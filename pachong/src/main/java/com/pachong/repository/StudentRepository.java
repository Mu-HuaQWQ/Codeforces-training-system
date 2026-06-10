package com.pachong.repository;

import com.pachong.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByHandleAndPlatform(String handle, String platform);
    boolean existsByHandleAndPlatform(String handle, String platform);
}
