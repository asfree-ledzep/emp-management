package com.example.emp.controller;

import com.example.emp.model.*;
import com.example.emp.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    // 관리자 여부 확인 헬퍼
    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ─── 사원 전용 ───

    // 1. OCR 분석 (S3 업로드 + Vision API) - DB 저장 안 함
    @PostMapping("/parse")
    public ResponseEntity<?> parseReceipt(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            OcrResultDto result = expenseService.parseReceipt(empno, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 2. 지출 등록 (DB 저장)
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Expense expense, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            expense.setEmpno(empno);
            expenseService.create(expense);
            return ResponseEntity.ok(Map.of("message", "지출이 등록되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 3. 내 지출 목록
    @GetMapping("/my")
    public ResponseEntity<?> getMyExpenses(Authentication auth) {
        Integer empno = Integer.parseInt(auth.getName());
        return ResponseEntity.ok(expenseService.findByEmpno(empno));
    }

    // ─── 관리자 전용 ───

    // 4. 월별 지출 목록
    @GetMapping
    public ResponseEntity<?> getByMonth(
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(expenseService.findByMonth(year, month));
    }

    // 5. 확인 처리
    @PatchMapping("/{expenseId}/confirm")
    public ResponseEntity<?> confirm(
            @PathVariable Long expenseId,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        expenseService.confirm(expenseId, auth.getName());
        return ResponseEntity.ok(Map.of("message", "확인 처리되었습니다."));
    }

    // 6. 삭제
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<?> delete(
            @PathVariable Long expenseId,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        expenseService.delete(expenseId);
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    // 7. 월별 통계 조회
    @GetMapping("/stats/monthly")
    public ResponseEntity<?> getMonthlyStats(
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(expenseService.findMonthlyStats(year, month));
    }

    // 8. 연별 통계 조회
    @GetMapping("/stats/yearly")
    public ResponseEntity<?> getYearlyStats(
            @RequestParam int year,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(expenseService.findYearlyStats(year));
    }

    // 9. 월별 통계 수동 생성
    @PostMapping("/stats/monthly/generate")
    public ResponseEntity<?> generateMonthly(
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        expenseService.generateMonthlyStats(year, month);
        return ResponseEntity.ok(Map.of("message", "월별 통계가 생성되었습니다."));
    }

    // 10. 연별 통계 수동 생성
    @PostMapping("/stats/yearly/generate")
    public ResponseEntity<?> generateYearly(
            @RequestParam int year,
            Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        expenseService.generateYearlyStats(year);
        return ResponseEntity.ok(Map.of("message", "연별 통계가 생성되었습니다."));
    }

    // 11. 지출내역 엑셀 다운로드
    @GetMapping("/excel/detail")
    public ResponseEntity<byte[]> downloadDetailExcel(
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) throws IOException {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        List<Expense> list = expenseService.findByMonth(year, month);
        byte[] bytes = expenseService.buildDetailExcel(list, year, month);
        return excelResponse(bytes, String.format("expense-detail-%d%02d.xlsx", year, month));
    }

    // 12. 월별통계 엑셀 다운로드
    @GetMapping("/excel/monthly")
    public ResponseEntity<byte[]> downloadMonthlyExcel(
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) throws IOException {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        List<ExpenseMonthlyStat> list = expenseService.findMonthlyStats(year, month);
        byte[] bytes = expenseService.buildMonthlyExcel(list, year, month);
        return excelResponse(bytes, String.format("expense-monthly-%d%02d.xlsx", year, month));
    }

    // 13. 연별통계 엑셀 다운로드
    @GetMapping("/excel/yearly")
    public ResponseEntity<byte[]> downloadYearlyExcel(
            @RequestParam int year,
            Authentication auth) throws IOException {
        if (!isAdmin(auth)) return ResponseEntity.status(403).build();
        List<ExpenseYearlyStat> list = expenseService.findYearlyStats(year);
        byte[] bytes = expenseService.buildYearlyExcel(list, year);
        return excelResponse(bytes, String.format("expense-yearly-%d.xlsx", year));
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }
}
