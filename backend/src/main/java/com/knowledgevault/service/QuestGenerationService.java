package com.knowledgevault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgevault.model.Quest;
import com.knowledgevault.service.provider.ProviderOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Generates AI-powered quests via the multi-provider orchestrator.
 * Provider priority: Groq → Gemini → OpenRouter → Mistral → Claude (paid)
 * Falls back to a built-in static question if ALL providers fail.
 */
@Service
public class QuestGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestGenerationService.class);

    private final ProviderOrchestrator orchestrator;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<String> NPC_NAMES = List.of(
        "Overseer Vance", "Dr. Elara Mote", "Professor Grim", "The Archivist",
        "Scout Reyes", "Elder Thornton", "Hacker Zero", "Sage Miriam"
    );

    private static final String[] NPC_INTRO_TEMPLATES = {
        "Vault Dweller, %s is the key to survival.",
        "The old world prized %s above all else.",
        "Only the learned survive. Prove your %s.",
        "I've seen raiders fail this. Show me your %s.",
        "In the wasteland, %s separates the living from the dead.",
    };

    public QuestGenerationService(ProviderOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Quest generateQuest(String skill, int skillLevel) {
        int difficulty = Math.min(10, Math.max(1, (skillLevel / 10) + 1));
        String prompt  = buildPrompt(skill, difficulty);

        try {
            ProviderOrchestrator.ProviderResponse response = orchestrator.complete(prompt);
            Quest quest = parseQuest(response.text(), skill, difficulty);
            quest.setGeneratedBy(response.usedProvider());
            attachNpc(quest, skill);
            return quest;

        } catch (ProviderOrchestrator.AllProvidersFailedException e) {
            log.error("All AI providers failed — serving static fallback: {}", e.getMessage());
            return staticFallback(skill, difficulty);

        } catch (Exception e) {
            log.error("Unexpected error during quest generation: {}", e.getMessage(), e);
            return staticFallback(skill, difficulty);
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private String buildPrompt(String skill, int difficulty) {
        return """
            You are a quest generator for a Fallout-inspired knowledge RPG.
            Generate ONE multiple-choice question for the skill: %s
            Difficulty: %d/10  (1=trivial, 10=expert/obscure)

            Respond with ONLY valid JSON — no markdown, no preamble:
            {
              "question": "...",
              "choices": ["A", "B", "C", "D"],
              "correctIndex": 0,
              "explanation": "..."
            }

            Rules:
            - All 4 choices must be plausible but only one correct
            - correctIndex is 0–3
            - question: max 200 characters
            - explanation: max 150 characters
            - Higher difficulty = rarer knowledge, more nuance
            """.formatted(skill, difficulty);
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private Quest parseQuest(String raw, String skill, int difficulty) throws Exception {
        // Strip markdown code fences if any model wraps the JSON
        String cleaned = raw.strip()
            .replaceAll("(?s)^```json\\s*", "")
            .replaceAll("(?s)^```\\s*",     "")
            .replaceAll("(?s)```$",          "")
            .strip();

        JsonNode data = mapper.readTree(cleaned);

        Quest quest = new Quest();
        quest.setId(UUID.randomUUID().toString());
        quest.setSkill(skill);
        quest.setDifficulty(difficulty);
        quest.setQuestion(data.path("question").asText());

        List<String> choices = new ArrayList<>();
        data.path("choices").forEach(c -> choices.add(c.asText()));
        quest.setChoices(choices);
        quest.setCorrectIndex(data.path("correctIndex").asInt());
        quest.setExplanation(data.path("explanation").asText());
        quest.setXpReward(difficulty * 50);
        quest.setScoreReward(difficulty * 100);
        return quest;
    }

    // ── NPC ───────────────────────────────────────────────────────────────────

    private void attachNpc(Quest quest, String skill) {
        String name = NPC_NAMES.get(new Random().nextInt(NPC_NAMES.size()));
        String template = NPC_INTRO_TEMPLATES[new Random().nextInt(NPC_INTRO_TEMPLATES.length)];
        quest.setNpcName(name);
        quest.setNpcDialogue(String.format(template, skill));
    }

    // ── Static fallback ───────────────────────────────────────────────────────

    private Quest staticFallback(String skill, int difficulty) {
        Quest q = new Quest();
        q.setId(UUID.randomUUID().toString());
        q.setSkill(skill);
        q.setDifficulty(difficulty);
        q.setQuestion("What year did the World Wide Web become publicly available?");
        q.setChoices(List.of("1983", "1991", "1995", "2001"));
        q.setCorrectIndex(1);
        q.setExplanation("Tim Berners-Lee made the World Wide Web public in 1991.");
        q.setXpReward(difficulty * 50);
        q.setScoreReward(difficulty * 100);
        q.setNpcName("The Archivist");
        q.setNpcDialogue("Knowledge is the only currency that matters out here.");
        q.setGeneratedBy("STATIC_FALLBACK");
        return q;
    }
}
