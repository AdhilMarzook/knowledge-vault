package com.knowledgevault.service;

import com.knowledgevault.model.AnswerRequest;
import com.knowledgevault.model.AnswerResult;
import com.knowledgevault.model.Player;
import com.knowledgevault.model.Quest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerService {

    // In-memory store (replace with DB for production)
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, Quest> activeQuests = new ConcurrentHashMap<>();

    private static final List<String> NPC_CORRECT = List.of(
        "Impressive, Vault Dweller. Your mind is a weapon.",
        "Knowledge survives when everything else turns to dust.",
        "You've earned this. Don't waste it.",
        "Few answer that correctly. Fewer still remember why."
    );

    private static final List<String> NPC_WRONG = List.of(
        "The wasteland doesn't forgive ignorance. Study harder.",
        "A wrong answer today is a lesson for tomorrow... if you live.",
        "Even the mutants knew that one. Think.",
        "Disappointing. But not surprising. Try again."
    );

    public Player createPlayer(String username) {
        Player player = new Player();
        player.setId(UUID.randomUUID().toString());
        player.setUsername(username);
        players.put(player.getId(), player);
        return player;
    }

    public Optional<Player> getPlayer(String playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public List<Player> getLeaderboard() {
        return players.values().stream()
            .sorted((a, b) -> Long.compare(b.getTotalScore(), a.getTotalScore()))
            .limit(10)
            .toList();
    }

    public void storeQuest(Quest quest) {
        activeQuests.put(quest.getId(), quest);
    }

    public AnswerResult processAnswer(AnswerRequest request) {
        Player player = players.get(request.getPlayerId());
        Quest quest = activeQuests.get(request.getQuestId());

        if (player == null || quest == null) {
            throw new RuntimeException("Player or Quest not found");
        }

        boolean correct = request.getSelectedIndex() == quest.getCorrectIndex();
        AnswerResult result = new AnswerResult();
        result.setCorrect(correct);
        result.setExplanation(quest.getExplanation());

        if (correct) {
            // Award XP and score
            int xp = quest.getXpReward();
            int score = quest.getScoreReward();
            result.setXpEarned(xp);
            result.setScoreEarned(score);

            player.setTotalScore(player.getTotalScore() + score);
            player.setQuestsCompleted(player.getQuestsCompleted() + 1);
            player.setCurrentStreak(player.getCurrentStreak() + 1);

            // Level up skill
            String skill = quest.getSkill();
            int currentSkillLevel = player.getSkills().getOrDefault(skill, 1);
            int newSkillLevel = Math.min(100, currentSkillLevel + quest.getDifficulty());
            player.getSkills().put(skill, newSkillLevel);
            result.setNewSkillLevel(newSkillLevel);

            // Check player level up
            int newPlayerLevel = calculateLevel(player.getTotalScore());
            if (newPlayerLevel > player.getLevel()) {
                player.setLevel(newPlayerLevel);
                result.setLeveledUp(true);
            }
            result.setNewPlayerLevel(player.getLevel());

            result.setNpcReaction(NPC_CORRECT.get(new Random().nextInt(NPC_CORRECT.size())));
        } else {
            result.setXpEarned(0);
            result.setScoreEarned(0);
            result.setNewSkillLevel(player.getSkills().getOrDefault(quest.getSkill(), 1));
            result.setNewPlayerLevel(player.getLevel());
            player.setCurrentStreak(0);
            result.setNpcReaction(NPC_WRONG.get(new Random().nextInt(NPC_WRONG.size())));
        }

        player.setLastActive(LocalDateTime.now());
        activeQuests.remove(quest.getId());

        return result;
    }

    private int calculateLevel(long score) {
        // Each level requires progressively more score
        return (int) (1 + Math.floor(Math.sqrt(score / 500.0)));
    }
}
