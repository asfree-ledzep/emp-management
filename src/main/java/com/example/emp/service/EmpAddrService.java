package com.example.emp.service;

import com.example.emp.mapper.EmpAddrMapper;
import com.example.emp.model.EmpAddr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmpAddrService {

    @Autowired private EmpAddrMapper empAddrMapper;

    public EmpAddr getByEmpno(Integer empno) {
        return empAddrMapper.findByEmpno(empno);
    }

    public void save(EmpAddr addr) {
        empAddrMapper.upsert(addr);
    }
}
