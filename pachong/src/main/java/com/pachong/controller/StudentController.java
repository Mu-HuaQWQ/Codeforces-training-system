package com.pachong.controller;

import com.pachong.service.StudentService;
import com.pachong.service.StudentService.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/students")
    public List<StudentDto> listStudents() {
        return studentService.getAllStudents();
    }

    @PostMapping("/students")
    public ResponseEntity<StudentDto> addStudent(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String handle = body.get("handle");
        String platform = body.get("platform");
        if (name == null || handle == null || platform == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(studentService.addStudent(name, handle, platform));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentDetailDto> getStudentDetail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(studentService.getStudentDetail(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/crawl/refresh-all")
    public ResponseEntity<Map<String, Object>> refreshAll() {
        int count = studentService.refreshAllStudents();
        return ResponseEntity.ok(Map.of("refreshed", count));
    }
}
