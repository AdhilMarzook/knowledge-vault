package com.knowledgevault.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Player {
    private String id;
    private String username;
    private int level;
    private long totalScore;
    private int questsCompleted;
    private Map<String, Integer> skills; // skill name -> level (1-100)
    private int currentStreak;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;

    public Player() {
        this.skills = new HashMap<>();
        this.skills.put("Science", 1);
        this.skills.put("History", 1);
        this.skills.put("Technology", 1);
        this.skills.put("Philosophy", 1);
        this.skills.put("Mathematics", 1);
        this.skills.put("Literature", 1);
        this.skills.put("Geography", 1);
        this.skills.put("Politics", 1);
        this.level = 1;
        this.totalScore = 0;
        this.questsCompleted = 0;
        this.currentStreak = 0;
        this.createdAt = LocalDateTime.now();
        this.lastActive = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public long getTotalScore() { return totalScore; }
    public void setTotalScore(long totalScore) { this.totalScore = totalScore; }
    public int getQuestsCompleted() { return questsCompleted; }
    public void setQuestsCompleted(int questsCompleted) { this.questsCompleted = questsCompleted; }
    public Map<String, Integer> getSkills() { return skills; }
    public void setSkills(Map<String, Integer> skills) { this.skills = skills; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
}
