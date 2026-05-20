package com.example.emp.controller;

import com.example.emp.model.Emp;
import com.example.emp.service.EmpService;
import com.example.emp.service.ExcelService;
import com.example.emp.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emps")
public class EmpController {

    @Autowired
    private EmpService empService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ExcelService excelService;

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
    public ResponseEntity<?> create(@RequestBody Emp emp) {
        try {
            empService.create(emp);
            return ResponseEntity.ok().build();
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 사용 중인 사번입니다. 다른 사번을 입력해 주세요."));
        }
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

    @GetMapping("/export")
    public ResponseEntity<Map<String, String>> exportExcel() {
        try {
            String url = excelService.exportToS3();
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{empno}/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Integer empno,
            @RequestParam("file") MultipartFile file) {
        try {
            String url = s3Service.uploadPhoto(empno, file);
            empService.updatePhotoUrl(empno, url);
            return ResponseEntity.ok(Map.of("photoUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
