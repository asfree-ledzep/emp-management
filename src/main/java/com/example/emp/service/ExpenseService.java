package com.example.emp.service;

import com.example.emp.mapper.ExpenseMapper;
import com.example.emp.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

@Service
public class ExpenseService {

    @Autowired private ExpenseMapper expenseMapper;
    @Autowired private S3Service s3Service;

    @Value("${google.vision.api-key}")
    private String visionApiKey;

    private static final String VISION_URL =
            "https://vision.googleapis.com/v1/images:annotate?key=";

    private final RestTemplate restTemplate = new RestTemplate();

    // ─────────────────────────────────────────
    // OCR: 영수증 이미지 → S3 업로드 + Google Vision 분석
    // ─────────────────────────────────────────
    public OcrResultDto parseReceipt(Integer empno, MultipartFile file) throws IOException {
        // 1. S3 업로드
        String receiptUrl = s3Service.uploadReceipt(empno, file);

        // 2. Base64 변환
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());

        // 3. Google Vision API 호출
        String ocrText = callVisionApi(base64);

        // 4. 금액·날짜 파싱
        Double amount      = parseAmount(ocrText);
        String expenseDate = parseDate(ocrText);

        return new OcrResultDto(receiptUrl, amount, expenseDate, ocrText);
    }

    @SuppressWarnings("unchecked")
    private String callVisionApi(String base64Image) {
        try {
            Map<String, Object> image    = Map.of("content", base64Image);
            Map<String, Object> feature  = Map.of("type", "TEXT_DETECTION");
            Map<String, Object> request  = Map.of("image", image, "features", List.of(feature));
            Map<String, Object> body     = Map.of("requests", List.of(request));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(VISION_URL + visionApiKey, entity, Map.class);

            List<Map<String, Object>> responses =
                    (List<Map<String, Object>>) response.getBody().get("responses");
            Map<String, Object> firstResponse = responses.get(0);
            Map<String, Object> fullText =
                    (Map<String, Object>) firstResponse.get("fullTextAnnotation");

            if (fullText == null) return "";
            return (String) fullText.get("text");
        } catch (Exception e) {
            return "";
        }
    }

    // 금액 파싱 — 키워드+금액 패턴 (우선순위 순)
    private static final Pattern[] AMOUNT_PATTERNS = {
        Pattern.compile("합\\s*계\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("총\\s*액\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("소\\s*계\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("결\\s*제\\s*금\\s*액\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("실\\s*결\\s*제\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("승\\s*인\\s*금\\s*액\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("청\\s*구\\s*금\\s*액\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("받\\s*을\\s*금\\s*액\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("지\\s*불\\s*금\\s*액\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("금\\s*액\\s*[:\\s：]?\\s*([\\d,]+)"),
        Pattern.compile("[Tt][Oo][Tt][Aa][Ll]\\s*[:\\s]?\\s*([\\d,]+)"),
        Pattern.compile("[Aa][Mm][Oo][Uu][Nn][Tt]\\s*[:\\s]?\\s*([\\d,]+)"),
    };

    private Double parseAmount(String text) {
        if (text == null || text.isBlank()) return null;

        // 정규화: 탭·연속 공백을 단일 공백으로 (개행은 유지)
        String norm = text.replaceAll("[ \\t]+", " ").trim();

        // 1순위: 키워드 패턴 매칭
        for (Pattern p : AMOUNT_PATTERNS) {
            Matcher m = p.matcher(norm);
            if (m.find()) {
                try {
                    double v = Double.parseDouble(m.group(1).replace(",", ""));
                    if (v >= 100) return v;
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2순위: "숫자원" 패턴 중 최댓값 (예: 15,000원 / 15000원)
        Matcher wonM = Pattern.compile("([\\d,]+)\\s*원").matcher(norm);
        double maxWon = 0;
        while (wonM.find()) {
            try {
                double v = Double.parseDouble(wonM.group(1).replace(",", ""));
                if (v > maxWon && v >= 100) maxWon = v;
            } catch (NumberFormatException ignored) {}
        }
        if (maxWon >= 100) return maxWon;

        // 3순위: 1,000 이상인 숫자 중 최댓값 (최후 폴백)
        Matcher numM = Pattern.compile("([\\d,]{4,})").matcher(norm);
        double maxNum = 0;
        while (numM.find()) {
            try {
                double v = Double.parseDouble(numM.group(1).replace(",", ""));
                if (v > maxNum && v >= 1000) maxNum = v;
            } catch (NumberFormatException ignored) {}
        }
        return maxNum >= 1000 ? maxNum : null;
    }

    // 날짜 파싱
    private String parseDate(String text) {
        if (text == null || text.isBlank()) return LocalDate.now().toString();

        // yyyy-MM-dd / yyyy/MM/dd / yyyy.MM.dd
        Matcher m1 = Pattern.compile("(202[0-9])[-/\\.](\\d{1,2})[-/\\.](\\d{1,2})").matcher(text);
        if (m1.find()) return String.format("%s-%02d-%02d",
                m1.group(1), Integer.parseInt(m1.group(2)), Integer.parseInt(m1.group(3)));

        // yyyy년 M월 d일
        Matcher m2 = Pattern.compile("(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일").matcher(text);
        if (m2.find()) return String.format("%s-%02d-%02d",
                m2.group(1), Integer.parseInt(m2.group(2)), Integer.parseInt(m2.group(3)));

        // yy-MM-dd
        Matcher m3 = Pattern.compile("(\\d{2})[-/\\.](\\d{2})[-/\\.](\\d{2})").matcher(text);
        if (m3.find()) return "20" + m3.group(1) + "-" + m3.group(2) + "-" + m3.group(3);

        return LocalDate.now().toString();
    }

    // ─────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────
    public void create(Expense expense) {
        expenseMapper.insertExpense(expense);
    }

    public List<Expense> findByEmpno(Integer empno) {
        return expenseMapper.findByEmpno(empno);
    }

    public List<Expense> findByMonth(int year, int month) {
        return expenseMapper.findByMonth(year, month);
    }

    public void confirm(Long expenseId, String confirmedBy) {
        expenseMapper.updateConfirm(expenseId, confirmedBy);
    }

    public void delete(Long expenseId) {
        expenseMapper.deleteExpense(expenseId);
    }

    // ─────────────────────────────────────────
    // 통계 생성
    // ─────────────────────────────────────────
    @Transactional
    public void generateMonthlyStats(int year, int month) {
        expenseMapper.deleteMonthlyStats(year, month);
        expenseMapper.insertMonthlyStats(year, month);
    }

    @Transactional
    public void generateYearlyStats(int year) {
        expenseMapper.deleteYearlyStats(year);
        expenseMapper.insertYearlyStats(year);
    }

    public List<ExpenseMonthlyStat> findMonthlyStats(int year, int month) {
        return expenseMapper.findMonthlyStats(year, month);
    }

    public List<ExpenseYearlyStat> findYearlyStats(int year) {
        return expenseMapper.findYearlyStats(year);
    }

    // ─────────────────────────────────────────
    // Excel 생성
    // ─────────────────────────────────────────
    public byte[] buildDetailExcel(List<Expense> list, int year, int month) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(year + "년 " + month + "월 지출내역");
            CellStyle hStyle = headerStyle(wb);

            String[] headers = {"사번","사원명","날짜","금액","카테고리","설명","상태","확인자"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hStyle);
            }

            int rowNum = 1;
            double total = 0;
            for (Expense e : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(e.getEmpno() != null ? e.getEmpno() : 0);
                row.createCell(1).setCellValue(e.getEname() != null ? e.getEname() : "");
                row.createCell(2).setCellValue(e.getExpenseDate() != null ? e.getExpenseDate().toString() : "");
                row.createCell(3).setCellValue(e.getAmount() != null ? e.getAmount() : 0);
                row.createCell(4).setCellValue(e.getCategory() != null ? e.getCategory() : "");
                row.createCell(5).setCellValue(e.getDescription() != null ? e.getDescription() : "");
                row.createCell(6).setCellValue("CONFIRMED".equals(e.getStatus()) ? "확인완료" : "미확인");
                row.createCell(7).setCellValue(e.getConfirmedBy() != null ? e.getConfirmedBy() : "");
                if (e.getAmount() != null) total += e.getAmount();
            }

            // 합계 행
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(2).setCellValue("합계");
            totalRow.createCell(3).setCellValue(total);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            return toBytes(wb);
        }
    }

    public byte[] buildMonthlyExcel(List<ExpenseMonthlyStat> list, int year, int month) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(year + "년 " + month + "월 통계");
            CellStyle hStyle = headerStyle(wb);

            String[] headers = {"사번","사원명","총 지출금액","건수"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hStyle);
            }

            int rowNum = 1;
            for (ExpenseMonthlyStat s : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getEmpno() != null ? s.getEmpno() : 0);
                row.createCell(1).setCellValue(s.getEname() != null ? s.getEname() : "");
                row.createCell(2).setCellValue(s.getTotalAmount() != null ? s.getTotalAmount() : 0);
                row.createCell(3).setCellValue(s.getExpenseCount() != null ? s.getExpenseCount() : 0);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            return toBytes(wb);
        }
    }

    public byte[] buildYearlyExcel(List<ExpenseYearlyStat> list, int year) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(year + "년 연간통계");
            CellStyle hStyle = headerStyle(wb);

            String[] headers = {"사번","사원명","연간 총 지출금액","건수"};
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(headers[i]); c.setCellStyle(hStyle);
            }

            int rowNum = 1;
            for (ExpenseYearlyStat s : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(s.getEmpno() != null ? s.getEmpno() : 0);
                row.createCell(1).setCellValue(s.getEname() != null ? s.getEname() : "");
                row.createCell(2).setCellValue(s.getTotalAmount() != null ? s.getTotalAmount() : 0);
                row.createCell(3).setCellValue(s.getExpenseCount() != null ? s.getExpenseCount() : 0);
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            return toBytes(wb);
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        return baos.toByteArray();
    }
}
