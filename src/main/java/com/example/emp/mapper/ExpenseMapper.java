package com.example.emp.mapper;

import com.example.emp.model.Expense;
import com.example.emp.model.ExpenseMonthlyStat;
import com.example.emp.model.ExpenseYearlyStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExpenseMapper {

    // 지출 내역
    void insertExpense(Expense expense);
    List<Expense> findByEmpno(Integer empno);
    List<Expense> findByMonth(@Param("year") int year, @Param("month") int month);
    void updateConfirm(@Param("expenseId") Long expenseId, @Param("confirmedBy") String confirmedBy);
    void deleteExpense(Long expenseId);

    // 중복 제출 방지 — OCR 해시로 동일 영수증 확인
    int countByOcrHash(@Param("empno") Integer empno, @Param("ocrHash") String ocrHash);

    // 월별 통계 생성 (DELETE + INSERT)
    void deleteMonthlyStats(@Param("year") int year, @Param("month") int month);
    void insertMonthlyStats(@Param("year") int year, @Param("month") int month);
    List<ExpenseMonthlyStat> findMonthlyStats(@Param("year") int year, @Param("month") int month);

    // 연별 통계 생성 (DELETE + INSERT)
    void deleteYearlyStats(@Param("year") int year);
    void insertYearlyStats(@Param("year") int year);
    List<ExpenseYearlyStat> findYearlyStats(@Param("year") int year);
}
