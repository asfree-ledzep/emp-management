package com.example.emp.service;

import com.example.emp.model.Emp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelService {

    @Autowired private EmpService empService;
    @Autowired private S3Service s3Service;

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String[] HEADERS =
            {"사번", "사원명", "직책", "상사사번", "입사일", "급여", "커미션", "부서번호"};

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
