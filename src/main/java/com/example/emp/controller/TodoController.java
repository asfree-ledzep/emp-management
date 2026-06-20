package com.example.emp.controller;

import com.example.emp.model.Todo;
import com.example.emp.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todo")
public class TodoController {

    @Autowired private TodoService todoService;

    @GetMapping
    public ResponseEntity<?> list(Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(todoService.getMyTodos(empno));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(List.of()); // 관리자: 빈 목록
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Todo req, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(todoService.add(empno, req));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{todoId}")
    public ResponseEntity<?> edit(@PathVariable Long todoId,
                                  @RequestBody Todo req, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            return ResponseEntity.ok(todoService.edit(empno, todoId, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{todoId}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long todoId, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            todoService.toggle(empno, todoId);
            return ResponseEntity.ok(Map.of("message", "상태가 변경되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{todoId}")
    public ResponseEntity<?> delete(@PathVariable Long todoId, Authentication auth) {
        try {
            Integer empno = Integer.parseInt(auth.getName());
            todoService.delete(empno, todoId);
            return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/reorder")
    public ResponseEntity<?> reorder(@RequestBody List<Long> ids) {
        try {
            todoService.reorder(ids);
            return ResponseEntity.ok(Map.of("message", "순서가 저장되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
