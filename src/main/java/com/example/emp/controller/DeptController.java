package com.example.emp.controller;

import com.example.emp.model.Dept;
import com.example.emp.model.Emp;
import com.example.emp.service.DeptService;
import com.example.emp.service.EmpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depts")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @Autowired
    private EmpService empService;

    @GetMapping
    public ResponseEntity<List<Dept>> getAll() {
        return ResponseEntity.ok(deptService.getAll());
    }

    @GetMapping("/{deptno}")
    public ResponseEntity<Dept> getById(@PathVariable Integer deptno) {
        Dept dept = deptService.getById(deptno);
        if (dept == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dept);
    }

    @GetMapping("/{deptno}/emps")
    public ResponseEntity<List<Emp>> getEmps(@PathVariable Integer deptno) {
        return ResponseEntity.ok(empService.getByDeptno(deptno));
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody Dept dept) {
        deptService.create(dept);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{deptno}")
    public ResponseEntity<Void> update(@PathVariable Integer deptno,
                                       @RequestBody Dept dept) {
        dept.setDeptno(deptno);
        deptService.update(dept);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deptno}")
    public ResponseEntity<Void> delete(@PathVariable Integer deptno) {
        deptService.delete(deptno);
        return ResponseEntity.ok().build();
    }
}
