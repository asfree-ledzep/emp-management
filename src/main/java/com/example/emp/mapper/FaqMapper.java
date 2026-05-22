package com.example.emp.mapper;

import com.example.emp.model.Faq;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FaqMapper {
    List<Faq> findAll();
    Faq findById(Long faqId);
    void insert(Faq faq);
    void update(Faq faq);
    void delete(Long faqId);
}
