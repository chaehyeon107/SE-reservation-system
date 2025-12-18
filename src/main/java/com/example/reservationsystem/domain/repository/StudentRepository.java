package com.example.reservationsystem.domain.repository;

import com.example.reservationsystem.domain.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(Long studentId);

}
