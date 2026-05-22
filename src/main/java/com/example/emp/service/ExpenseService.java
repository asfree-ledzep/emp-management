package com.example.emp.service;

import com.example.emp.mapper.ExpenseMapper;
import com.example.emp.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

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
        log.info("[OCR] 영수증 분석 시작 - empno: {}, 파일: {}, 크기: {} bytes",
                empno, file.getOriginalFilename(), file.getSize());

        // 1. S3 업로드
        String receiptUrl = s3Service.uploadReceipt(empno, file);
        log.info("[OCR] S3 업로드 완료: {}", receiptUrl);

        // 2. Base64 변환
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        log.info("[OCR] Base64 변환 완료, 길이: {}", base64.length());

        // 3. Google Vision API 호출
        String ocrText = callVisionApi(base64);
        log.info("[OCR] Vision API 결과 텍스트 길이: {}", ocrText.length());
        if (!ocrText.isBlank()) {
            log.info("[OCR] 텍스트 앞 200자: {}", ocrText.substring(0, Math.min(200, ocrText.length())));
        }

        // 4. 금액·날짜 파싱
        Double amount      = parseAmount(ocrText);
        String expenseDate = parseDate(ocrText);
        log.info("[OCR] 파싱 결과 - 금액: {}, 날짜: {}", amount, expenseDate);

        return new OcrResultDto(receiptUrl, amount, expenseDate, ocrText);
    }

    @SuppressWarnings("unchecked")
    private String callVisionApi(String base64Image) {
        log.info("[Vision API] 호출 시작 - base64 길이: {}", base64Image.length());
        try {
            Map<String, Object> image   = Map.of("content", base64Image);
            // DOCUMENT_TEXT_DETECTION: 문서/영수증에 더 정확함
            Map<String, Object> feature = Map.of("type", "DOCUMENT_TEXT_DETECTION");
            Map<String, Object> request = Map.of("image", image, "features", List.of(feature));
            Map<String, Object> body    = Map.of("requests", List.of(request));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(VISION_URL + visionApiKey, entity, Map.class);

            log.info("[Vision API] 응답 HTTP 상태: {}", response.getStatusCode());

            if (response.getBody() == null) {
                log.warn("[Vision API] 응답 body가 null");
                return "";
            }

            // error 응답 처리
            if (response.getBody().containsKey("error")) {
                log.error("[Vision API] 오류 응답: {}", response.getBody().get("error"));
                return "";
            }

            List<Map<String, Object>> responses =
                    (List<Map<String, Object>>) response.getBody().get("responses");

            if (responses == null || responses.isEmpty()) {
                log.warn("[Vision API] responses 배열이 비어있음");
                return "";
            }

            Map<String, Object> first = responses.get(0);
            log.info("[Vision API] first 응답 키: {}", first.keySet());

            // API 레벨 오류 처리
            if (first.containsKey("error")) {
                log.error("[Vision API] 개별 응답 오류: {}", first.get("error"));
                return "";
            }

            // 1순위: fullTextAnnotation.text
            Map<String, Object> fullText = (Map<String, Object>) first.get("fullTextAnnotation");
            if (fullText != null) {
                String t = (String) fullText.get("text");
                if (t != null && !t.isBlank()) {
                    log.info("[Vision API] fullTextAnnotation 추출 성공, 길이: {}", t.length());
                    return t;
                }
            }

            // 2순위: textAnnotations[0].description (fallback)
            List<Map<String, Object>> textAnnotations =
                    (List<Map<String, Object>>) first.get("textAnnotations");
            if (textAnnotations != null && !textAnnotations.isEmpty()) {
                String t = (String) textAnnotations.get(0).get("description");
                if (t != null && !t.isBlank()) {
                    log.info("[Vision API] textAnnotations fallback 추출 성공, 길이: {}", t.length());
                    return t;
                }
            }

            log.warn("[Vision API] 텍스트 없음 - fullText: {}, textAnnotations 크기: {}",
                    fullText != null ? "존재" : "null",
                    textAnnotations != null ? textAnnotations.size() : 0);
            return "";
        } catch (HttpClientErrorException e) {
            log.error("[Vision API] HTTP 오류 {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "";
        } catch (Exception e) {
            log.error("[Vision API] 예외 발생: {}", e.getMessage(), e);
            return "";
        }
    }

    // 금액 최대 한도: 1억원 초과는 주문번호/계좌번호 등으로 간주
    private static final double MAX_AMOUNT = 100_000_000.0;

    // 금액 파싱 — 키워드 뒤 비숫자 최대 30자 허용 (줄바꿈, 공백, 콜론 등)
    // [\\d,.] : 콤마(5,000)와 점(5.000) 천단위 구분자 모두 허용
    private static final Pattern[] AMOUNT_PATTERNS = {
        Pattern.compile("합\\s*계[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("총\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("소\\s*계[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("청\\s*구\\s*금\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("결\\s*제\\s*금\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("실\\s*결\\s*제[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("승\\s*인\\s*금\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("판\\s*매\\s*금\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("받\\s*을\\s*금\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("지\\s*불\\s*금\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("청\\s*구\\s*액[^\\d\\n]{0,30}([\\d,.]+)"),
        Pattern.compile("금\\s*액[^\\d\\n]{0,20}([\\d,.]+)"),
        // ₩ / W 기호 패턴 (예: ₩ 575,000 / W575,000)
        Pattern.compile("[₩W]\\s*([\\d,.]+)"),
        Pattern.compile("[Tt][Oo][Tt][Aa][Ll][^\\d]{0,10}([\\d,.]+)"),
        Pattern.compile("[Aa][Mm][Oo][Uu][Nn][Tt][^\\d]{0,10}([\\d,.]+)"),
    };

    // 날짜처럼 보이는 패턴: "2003.02.10", "2023-10-02", "23/05/12" 등
    private static final Pattern DATE_LIKE =
        Pattern.compile("\\d{2,4}[.,/\\-]\\d{1,2}[.,/\\-]\\d{1,2}");

    /**
     * 금액 문자열 → Double 변환 (날짜 패턴이면 null 반환)
     * "5,000"  → 5000  (콤마 천단위)
     * "5.000"  → 5000  (점 천단위)
     * "2003.02.10" → null (날짜 패턴 → 제외)
     */
    private Double toAmountDoubleOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        // 날짜 패턴(yyyy.MM.dd / yy-MM-dd 등)이면 금액으로 사용 안 함
        if (DATE_LIKE.matcher(s).matches()) return null;
        try {
            return Double.parseDouble(s.replaceAll("[,.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseAmount(String text) {
        if (text == null || text.isBlank()) return null;

        // 정규화: 탭·연속 공백을 단일 공백으로 (개행은 유지)
        String norm = text.replaceAll("[ \\t]+", " ").trim();

        // 1순위: 키워드 패턴 매칭 (100 이상 1억 이하, 날짜 패턴 제외)
        for (Pattern p : AMOUNT_PATTERNS) {
            Matcher m = p.matcher(norm);
            while (m.find()) {
                Double v = toAmountDoubleOrNull(m.group(1));
                if (v != null && v >= 100 && v <= MAX_AMOUNT) return v;
            }
        }

        // 2순위: "숫자원" 패턴 중 최댓값 (예: 15,000원 / 15.000원), 1억 이하
        Matcher wonM = Pattern.compile("([\\d,.]+)\\s*원").matcher(norm);
        double maxWon = 0;
        while (wonM.find()) {
            Double v = toAmountDoubleOrNull(wonM.group(1));
            if (v != null && v > maxWon && v >= 100 && v <= MAX_AMOUNT) maxWon = v;
        }
        if (maxWon >= 100) return maxWon;

        // 3순위: 최후 폴백 — 1,000 이상 1억 이하 숫자 중 최댓값 (날짜 패턴 자동 제외)
        Matcher numM = Pattern.compile("([\\d,.]{4,})").matcher(norm);
        double maxNum = 0;
        while (numM.find()) {
            Double v = toAmountDoubleOrNull(numM.group(1));
            if (v != null && v > maxNum && v >= 1000 && v <= MAX_AMOUNT) maxNum = v;
        }
        return maxNum >= 1000 ? maxNum : null;
    }

    // 날짜 파싱 — 유효 범위 검증 후 반환, 실패 시 오늘 날짜 폴백
    private String parseDate(String text) {
        if (text == null || text.isBlank()) return LocalDate.now().toString();

        // 1순위: yyyy년 M월 d일 (가장 명확)
        Matcher m2 = Pattern.compile("(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일").matcher(text);
        while (m2.find()) {
            int year  = Integer.parseInt(m2.group(1));
            int month = Integer.parseInt(m2.group(2));
            int day   = Integer.parseInt(m2.group(3));
            if (year >= 2000 && year <= 2030 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                String c = String.format("%04d-%02d-%02d", year, month, day);
                if (isValidDate(c)) return c;
            }
        }

        // 2순위: yyyy-MM-dd / yyyy/MM/dd / yyyy.MM.dd (2010~2029)
        Matcher m1 = Pattern.compile("(20[12][0-9])[-/\\.](\\d{1,2})[-/\\.](\\d{1,2})").matcher(text);
        while (m1.find()) {
            int month = Integer.parseInt(m1.group(2));
            int day   = Integer.parseInt(m1.group(3));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                String c = String.format("%s-%02d-%02d", m1.group(1), month, day);
                if (isValidDate(c)) return c;
            }
        }

        // 3순위: yy-MM-dd / yy/MM/dd / yy.MM.dd
        Matcher m3 = Pattern.compile("(\\d{2})[-/\\.](\\d{2})[-/\\.](\\d{2})").matcher(text);
        while (m3.find()) {
            int month = Integer.parseInt(m3.group(2));
            int day   = Integer.parseInt(m3.group(3));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                String c = "20" + m3.group(1) + "-" + m3.group(2) + "-" + m3.group(3);
                if (isValidDate(c)) return c;
            }
        }

        return LocalDate.now().toString();
    }

    // 날짜 문자열 유효성 검사 (LocalDate.parse로 실제 검증)
    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────
    // OCR 해시 생성
    // ─────────────────────────────────────────
    /**
     * OCR 원문 → SHA-256 해시 (64자 hex 문자열)
     * 같은 영수증 = 같은 OCR 텍스트 = 같은 해시 → 중복 감지에 활용
     */
    public String computeOcrHash(String ocrRaw) {
        if (ocrRaw == null || ocrRaw.isBlank()) return null;
        try {
            // 공백/개행 정규화 후 해시 → 약간의 OCR 변동 흡수
            String normalized = ocrRaw.replaceAll("\\s+", " ").trim();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.warn("[OCR Hash] 해시 계산 실패: {}", e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────
    public void create(Expense expense) {
        // OCR 해시 세팅 (제출 시 ocrRaw가 있으면 해시 계산)
        if (expense.getOcrHash() == null && expense.getOcrRaw() != null) {
            expense.setOcrHash(computeOcrHash(expense.getOcrRaw()));
        }
        expenseMapper.insertExpense(expense);
    }

    /**
     * 중복 제출 확인
     * @return true = 이미 제출된 영수증
     */
    public boolean isDuplicate(Integer empno, String ocrHash) {
        if (ocrHash == null || ocrHash.isBlank()) return false;
        return expenseMapper.countByOcrHash(empno, ocrHash) > 0;
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
