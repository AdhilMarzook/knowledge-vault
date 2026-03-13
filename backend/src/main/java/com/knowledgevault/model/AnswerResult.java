package com.knowledgevault.model;

public class AnswerResult {
    private boolean correct;
    private String explanation;
    private int xpEarned;
    private int scoreEarned;
    private int newSkillLevel;
    private boolean leveledUp;
    private int newPlayerLevel;
    private String npcReaction;

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }
    public int getScoreEarned() { return scoreEarned; }
    public void setScoreEarned(int scoreEarned) { this.scoreEarned = scoreEarned; }
    public int getNewSkillLevel() { return newSkillLevel; }
    public void setNewSkillLevel(int newSkillLevel) { this.newSkillLevel = newSkillLevel; }
    public boolean isLeveledUp() { return leveledUp; }
    public void setLeveledUp(boolean leveledUp) { this.leveledUp = leveledUp; }
    public int getNewPlayerLevel() { return newPlayerLevel; }
    public void setNewPlayerLevel(int newPlayerLevel) { this.newPlayerLevel = newPlayerLevel; }
    public String getNpcReaction() { return npcReaction; }
    public void setNpcReaction(String npcReaction) { this.npcReaction = npcReaction; }
}
