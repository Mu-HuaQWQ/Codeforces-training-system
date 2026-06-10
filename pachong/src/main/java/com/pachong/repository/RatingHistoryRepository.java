package com.pachong.repository;

import com.pachong.entity.RatingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RatingHistoryRepository extends JpaRepository<RatingHistory, Long> {
    List<RatingHistory> findByStudentIdOrderByRecordedAtAsc(Long studentId);
    RatingHistory findTopByStudentIdOrderByRecordedAtDesc(Long studentId);
}
