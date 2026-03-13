package com.knowledgevault.model;

import java.util.List;

public class Quest {
    private String id;
    private String skill;
    private int difficulty;
    private String question;
    private List<String> choices;
    private int correctIndex;
    private String explanation;
    private int xpReward;
    private int scoreReward;
    private String npcName;
    private String npcDialogue;
    private String generatedBy; // which AI provider generated this quest

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public List<String> getChoices() { return choices; }
    public void setChoices(List<String> choices) { this.choices = choices; }
    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }
    public int getScoreReward() { return scoreReward; }
    public void setScoreReward(int scoreReward) { this.scoreReward = scoreReward; }
    public String getNpcName() { return npcName; }
    public void setNpcName(String npcName) { this.npcName = npcName; }
    public String getNpcDialogue() { return npcDialogue; }
    public void setNpcDialogue(String npcDialogue) { this.npcDialogue = npcDialogue; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
}
