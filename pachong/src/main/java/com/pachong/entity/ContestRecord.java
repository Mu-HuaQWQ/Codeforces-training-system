package com.pachong.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contest_record")
public class ContestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "contest_name", length = 500)
    private String contestName;

    @Column(name = "contest_id", length = 100)
    private String contestId;

    @Column(name = "contest_rank")
    private Integer rank;

    @Column(name = "old_rating")
    private Integer oldRating;

    @Column(name = "new_rating")
    private Integer newRating;

    @Column(name = "solved_count")
    private Integer solvedCount;

    @Column(name = "contest_date")
    private LocalDateTime contestDate;

    public ContestRecord() {}

    public ContestRecord(Student student, String contestName, String contestId,
                         Integer rank, Integer oldRating, Integer newRating,
                         Integer solvedCount, LocalDateTime contestDate) {
        this.student = student;
        this.contestName = contestName;
        this.contestId = contestId;
        this.rank = rank;
        this.oldRating = oldRating;
        this.newRating = newRating;
        this.solvedCount = solvedCount;
        this.contestDate = contestDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getContestName() { return contestName; }
    public void setContestName(String contestName) { this.contestName = contestName; }
    public String getContestId() { return contestId; }
    public void setContestId(String contestId) { this.contestId = contestId; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public Integer getOldRating() { return oldRating; }
    public void setOldRating(Integer oldRating) { this.oldRating = oldRating; }
    public Integer getNewRating() { return newRating; }
    public void setNewRating(Integer newRating) { this.newRating = newRating; }
    public Integer getSolvedCount() { return solvedCount; }
    public void setSolvedCount(Integer solvedCount) { this.solvedCount = solvedCount; }
    public LocalDateTime getContestDate() { return contestDate; }
    public void setContestDate(LocalDateTime contestDate) { this.contestDate = contestDate; }
}
