package com.example.emp.controller;

import com.example.emp.model.Emp;
import com.example.emp.service.EmpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emps")
public class EmpController {

    @Autowired
    private EmpService empService;

    @GetMapping
    public ResponseEntity<List<Emp>> getAll() {
        return ResponseEntity.ok(empService.getAll());
    }

    @GetMapping("/{empno}")
    public ResponseEntity<Emp> getById(@PathVariable Integer empno) {
        Emp emp = empService.getById(empno);
        if (emp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(emp);
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody Emp emp) {
        empService.create(emp);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{empno}")
    public ResponseEntity<Void> update(@PathVariable Integer empno,
                                       @RequestBody Emp emp) {
        emp.setEmpno(empno);
        empService.update(emp);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{empno}")
    public ResponseEntity<Void> delete(@PathVariable Integer empno) {
        empService.delete(empno);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dept/{deptno}")
    public ResponseEntity<List<Emp>> getByDeptno(@PathVariable Integer deptno) {
        return ResponseEntity.ok(empService.getByDeptno(deptno));
    }
}
