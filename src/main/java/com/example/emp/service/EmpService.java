package com.example.emp.service;

import com.example.emp.mapper.EmpMapper;
import com.example.emp.model.Emp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmpService {

    private static final Logger log = LoggerFactory.getLogger(EmpService.class);

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private EmailService emailService;

    public List<Emp> getAll() {
        return empMapper.findAll();
    }

    public Emp getById(Integer empno) {
        return empMapper.findById(empno);
    }

    @Transactional
    public void create(Emp emp) {
        empMapper.insert(emp);
        try {
            emailService.sendCreateNotification(emp.getEmpno(), emp.getEname());
        } catch (Exception e) {
            log.warn("사원 등록 이메일 발송 실패 (등록은 완료): {}", e.getMessage());
        }
    }

    @Transactional
    public void update(Emp emp) {
        empMapper.update(emp);
    }

    @Transactional
    public void delete(Integer empno) {
        Emp emp = empMapper.findById(empno);
        empMapper.delete(empno);
        try {
            if (emp != null) {
                emailService.sendDeleteNotification(empno, emp.getEname());
            }
        } catch (Exception e) {
            log.warn("사원 삭제 이메일 발송 실패 (삭제는 완료): {}", e.getMessage());
        }
    }

    public List<Emp> getByDeptno(Integer deptno) {
        return empMapper.findByDeptno(deptno);
    }

    @Transactional
    public void updatePhotoUrl(Integer empno, String photoUrl) {
        empMapper.updatePhotoUrl(empno, photoUrl);
    }
}
