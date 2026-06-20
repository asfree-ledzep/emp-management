package com.example.emp.service;

import com.example.emp.mapper.TodoMapper;
import com.example.emp.model.Todo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoService {

    @Autowired private TodoMapper todoMapper;

    public List<Todo> getMyTodos(Integer empno) {
        return todoMapper.findByEmpno(empno);
    }

    public Todo add(Integer empno, Todo req) {
        long id = todoMapper.nextVal();
        req.setTodoId(id);
        req.setEmpno(empno);
        if (req.getPriority() == null) req.setPriority("MEDIUM");
        if (req.getSortOrder() == null) req.setSortOrder(0);
        todoMapper.insert(req);
        return todoMapper.findById(id, empno);
    }

    public Todo edit(Integer empno, Long todoId, Todo req) {
        Todo existing = todoMapper.findById(todoId, empno);
        if (existing == null) throw new IllegalArgumentException("할일을 찾을 수 없습니다.");
        req.setTodoId(todoId);
        req.setEmpno(empno);
        todoMapper.update(req);
        return todoMapper.findById(todoId, empno);
    }

    public void toggle(Integer empno, Long todoId) {
        todoMapper.toggle(todoId, empno);
    }

    public void delete(Integer empno, Long todoId) {
        todoMapper.delete(todoId, empno);
    }

    public void reorder(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            todoMapper.updateOrder(ids.get(i), i);
        }
    }
}
