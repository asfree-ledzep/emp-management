package com.example.emp.service;

import com.example.emp.mapper.DeptMapper;
import com.example.emp.model.Dept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeptService {

    @Autowired
    private DeptMapper deptMapper;

    public List<Dept> getAll() {
        return deptMapper.findAll();
    }

    public Dept getById(Integer deptno) {
        return deptMapper.findById(deptno);
    }

    public void create(Dept dept) {
        deptMapper.insert(dept);
    }

    public void update(Dept dept) {
        deptMapper.update(dept);
    }

    public void delete(Integer deptno) {
        deptMapper.delete(deptno);
    }
}
