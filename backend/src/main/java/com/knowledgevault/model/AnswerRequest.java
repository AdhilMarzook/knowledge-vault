package com.knowledgevault.model;

public class AnswerRequest {
    private String questId;
    private String playerId;
    private int selectedIndex;

    public String getQuestId() { return questId; }
    public void setQuestId(String questId) { this.questId = questId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public int getSelectedIndex() { return selectedIndex; }
    public void setSelectedIndex(int selectedIndex) { this.selectedIndex = selectedIndex; }
}
