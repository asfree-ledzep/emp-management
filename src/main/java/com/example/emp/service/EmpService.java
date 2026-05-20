package com.example.emp.service;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.Emp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmpService {

    @Autowired
    private EmpMapper empMapper;

    public List<Emp> getAll() {
        return empMapper.findAll();
    }

    public Emp getById(Integer empno) {
        return empMapper.findById(empno);
    }

    @Transactional
    public void create(Emp emp) {
        empMapper.insert(emp);
    }

    @Transactional
    public void update(Emp emp) {
        empMapper.update(emp);
    }

    @Transactional
    public void delete(Integer empno) {
        empMapper.delete(empno);
    }

    public List<Emp> getByDeptno(Integer deptno) {
        return empMapper.findByDeptno(deptno);
    }

    @Transactional
    public void updatePhotoUrl(Integer empno, String photoUrl) {
        empMapper.updatePhotoUrl(empno, photoUrl);
    }
}
