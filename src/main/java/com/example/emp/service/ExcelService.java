package com.example.emp.service;

import com.example.emp.model.Emp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ExcelService {

    @Autowired private EmpService empService;
    @Autowired private S3Service s3Service;

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String[] HEADERS =
            {"사번", "사원명", "직책", "상사사번", "입사일", "급여", "커미션", "부서번호"};

    // ── 엑셀 일괄 업로드 → DB 등록 ──────────────────────────────
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        List<Emp>   parsed  = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row)) continue;
                try {
                    Emp emp = rowToEmp(row);
                    if (emp != null) parsed.add(emp);
                } catch (Exception e) {
                    parseErrors.add((i + 1) + "행: " + e.getMessage());
                }
            }
        }

        int success = 0;
        List<String> failDetails = new ArrayList<>(parseErrors);
        for (Emp emp : parsed) {
            try {
                empService.create(emp);
                success++;
            } catch (DuplicateKeyException e) {
                failDetails.add("사번 " + emp.getEmpno() + " (" + emp.getEname() + ") — 중복");
            } catch (Exception e) {
                failDetails.add("사번 " + emp.getEmpno() + " — " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total",   parsed.size());
        result.put("success", success);
        result.put("failed",  parsed.size() - success + parseErrors.size());
        result.put("errors",  failDetails);
        return result;
    }

    // 빈 행 여부 확인
    private boolean isBlankRow(Row row) {
        for (int c = 0; c < 8; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    // 행 → Emp 변환
    private Emp rowToEmp(Row row) {
        Integer empno = getCellInt(row.getCell(0));
        if (empno == null) throw new IllegalArgumentException("사번(A열) 누락");
        String  ename    = getCellString(row.getCell(1));
        String  job      = getCellString(row.getCell(2));
        Integer mgr      = getCellInt(row.getCell(3));
        LocalDate hiredate = getCellDate(row.getCell(4));
        Double  sal      = getCellDouble(row.getCell(5));
        Double  comm     = getCellDouble(row.getCell(6));
        Integer deptno   = getCellInt(row.getCell(7));

        return Emp.builder()
                .empno(empno)
                .ename(ename.isBlank()  ? null : ename)
                .job(job.isBlank()      ? null : job)
                .mgr(mgr != null && mgr == 0 ? null : mgr)
                .hiredate(hiredate)
                .sal(sal)
                .comm(comm != null && comm == 0 ? null : comm)
                .deptno(deptno)
                .build();
    }

    private static final Pattern DATE_SEP = Pattern.compile("[./]");

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getStringCellValue().trim();
            default -> "";
        };
    }

    private Double getCellDouble(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().trim().replace(",", "");
            if (s.isBlank()) return null;
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Integer getCellInt(Cell cell) {
        Double d = getCellDouble(cell);
        return (d != null && d != 0) ? d.intValue() : null;
    }

    private LocalDate getCellDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String s = getCellString(cell).trim();
            if (s.isBlank()) return null;
            return LocalDate.parse(DATE_SEP.matcher(s).replaceAll("-"));
        } catch (Exception e) {
            return null;
        }
    }

    // ── 엑셀 내보내기 ────────────────────────────────────────────
    public String exportToS3() throws IOException {
        List<Emp> emps = empService.getAll();
        byte[] bytes = buildExcel(emps);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String key = "exports/emp-list-" + timestamp + ".xlsx";

        return s3Service.uploadBytes(key, bytes, CONTENT_TYPE);
    }

    private byte[] buildExcel(List<Emp> emps) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("사원목록");

            // 헤더 스타일
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // 헤더 행
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행
            int rowNum = 1;
            for (Emp emp : emps) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp.getEmpno()   != null ? emp.getEmpno()   : 0);
                row.createCell(1).setCellValue(emp.getEname()   != null ? emp.getEname()   : "");
                row.createCell(2).setCellValue(emp.getJob()     != null ? emp.getJob()     : "");
                row.createCell(3).setCellValue(emp.getMgr()     != null ? emp.getMgr()     : 0);
                row.createCell(4).setCellValue(emp.getHiredate()!= null ? emp.getHiredate().toString() : "");
                row.createCell(5).setCellValue(emp.getSal()     != null ? emp.getSal()     : 0.0);
                row.createCell(6).setCellValue(emp.getComm()    != null ? emp.getComm()    : 0.0);
                row.createCell(7).setCellValue(emp.getDeptno()  != null ? emp.getDeptno()  : 0);
            }

            // 열 너비 자동 조정
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        }
    }
}
