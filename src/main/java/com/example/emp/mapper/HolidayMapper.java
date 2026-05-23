package com.example.emp.mapper;

import com.example.emp.model.Holiday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HolidayMapper {
    List<Holiday> findByYear(@Param("year") int year);
    List<Holiday> findByYearMonth(@Param("year") int year, @Param("month") int month);
    void insert(Holiday holiday);
    void delete(Long holidayId);
}
