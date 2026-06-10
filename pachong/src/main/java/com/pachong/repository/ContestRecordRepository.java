package com.pachong.repository;

import com.pachong.entity.ContestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContestRecordRepository extends JpaRepository<ContestRecord, Long> {
    List<ContestRecord> findByStudentIdOrderByContestDateDesc(Long studentId);
    boolean existsByStudentIdAndContestName(Long studentId, String contestName);
}
