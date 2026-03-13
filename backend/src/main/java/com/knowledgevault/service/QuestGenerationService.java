package com.knowledgevault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgevault.model.Quest;
import com.knowledgevault.service.provider.AiProviderRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuestGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestGenerationService.class);

    private final AiProviderRouter providerRouter;
    private final ObjectMapper objectMapper;

    private static final List<String> NPC_NAMES = List.of(
        "Overseer Vance", "Dr. Elara Mote", "Professor Grim", "The Archivist",
        "Scout Reyes", "Elder Thornton", "Hacker Zero", "Sage Miriam"
    );

    private static final String[] NPC_INTRO_TEMPLATES = {
        "Vault Dweller, %s is the key to survival.",
        "The old world prized %s above all else.",
        "Only the learned survive. Prove your %s.",
        "I've seen raiders fail this. Show me your %s.",
        "The wasteland will test your %s. Are you ready?",
        "Pre-war scholars died for this %s knowledge. Honor them."
    };

    public QuestGenerationService(AiProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
        this.objectMapper   = new ObjectMapper();
    }

    public Quest generateQuest(String skill, int skillLevel) {
        int difficulty = Math.min(10, Math.max(1, (skillLevel / 10) + 1));
        String prompt  = buildPrompt(skill, difficulty);

        try {
            String rawResponse = providerRouter.complete(prompt, 500);
            Quest quest = parseQuestFromResponse(rawResponse, skill, difficulty);
            quest.setGeneratedBy(providerRouter.getActiveProviderName());
            return quest;
        } catch (Exception e) {
            log.error("All AI providers failed for skill={} difficulty={}: {}", skill, difficulty, e.getMessage());
            return generateFallbackQuest(skill, difficulty);
        }
    }

    private String buildPrompt(String skill, int difficulty) {
        return String.format("""
            You are an AI quest generator for a Fallout-inspired knowledge RPG.
            Generate a multiple-choice question for the skill: %s
            Difficulty: %d/10 (1=trivial, 10=expert-only)

            Return ONLY valid JSON — no markdown, no preamble, no explanation outside the JSON:
            {
              "question": "The question text (max 200 chars)",
              "choices": ["Option A", "Option B", "Option C", "Option D"],
              "correctIndex": 0,
              "explanation": "Why the answer is correct (max 150 chars)"
            }

            Rules:
            - All 4 choices must be plausible, not obviously wrong
            - correctIndex is 0, 1, 2, or 3
            - Higher difficulty = more obscure, nuanced questions
            - Output pure JSON only, nothing else
            """, skill, difficulty);
    }

    private Quest parseQuestFromResponse(String raw, String skill, int difficulty) throws Exception {
        // Strip markdown code fences if any provider wraps the JSON
        String cleaned = raw.trim()
            .replaceAll("(?s)```json\\s*", "")
            .replaceAll("(?s)```\\s*", "")
            .trim();

        // Extract just the JSON object if there's surrounding text
        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        JsonNode data = objectMapper.readTree(cleaned);

        if (!data.has("question") || !data.has("choices") || !data.has("correctIndex")) {
            throw new IllegalArgumentException("Missing required fields in AI response");
        }

        List<String> choices = new ArrayList<>();
        data.path("choices").forEach(c -> choices.add(c.asText()));

        if (choices.size() != 4) {
            throw new IllegalArgumentException("Expected 4 choices, got " + choices.size());
        }

        int correctIndex = data.path("correctIndex").asInt();
        if (correctIndex < 0 || correctIndex > 3) {
            throw new IllegalArgumentException("Invalid correctIndex: " + correctIndex);
        }

        Quest quest = new Quest();
        quest.setId(UUID.randomUUID().toString());
        quest.setSkill(skill);
        quest.setDifficulty(difficulty);
        quest.setQuestion(data.path("question").asText());
        quest.setChoices(choices);
        quest.setCorrectIndex(correctIndex);
        quest.setExplanation(data.path("explanation").asText("No explanation provided."));
        quest.setXpReward(difficulty * 50);
        quest.setScoreReward(difficulty * 100);
        quest.setNpcName(NPC_NAMES.get(new Random().nextInt(NPC_NAMES.size())));
        quest.setNpcDialogue(buildNpcDialogue(skill));
        return quest;
    }

    private String buildNpcDialogue(String skill) {
        String template = NPC_INTRO_TEMPLATES[new Random().nextInt(NPC_INTRO_TEMPLATES.length)];
        return String.format(template, skill);
    }

    private Quest generateFallbackQuest(String skill, int difficulty) {
        Map<String, String[]> fallbacks = Map.of(
            "Science",     new String[]{"What is the atomic number of Carbon?",        "6","8","12","14",          "0","Carbon has 6 protons in its nucleus."},
            "History",     new String[]{"Which year did World War II end?",            "1943","1944","1945","1946","2","WWII ended in 1945 with Allied victory."},
            "Technology",  new String[]{"What does CPU stand for?",                    "Central Processing Unit","Core Processing Utility","Computer Power Unit","Central Power Utility","0","CPU = Central Processing Unit."},
            "Mathematics", new String[]{"What is Pi to 2 decimal places?",             "3.12","3.14","3.16","3.18","1","Pi is approximately 3.14159..."},
            "Philosophy",  new String[]{"Who wrote 'The Republic'?",                   "Aristotle","Socrates","Plato","Epicurus","2","Plato wrote The Republic around 380 BC."},
            "Literature",  new String[]{"Who wrote '1984'?",                           "Aldous Huxley","Ray Bradbury","George Orwell","H.G. Wells","2","George Orwell wrote 1984, published in 1949."},
            "Geography",   new String[]{"What is the capital of Australia?",           "Sydney","Melbourne","Brisbane","Canberra","3","Canberra has been Australia's capital since 1913."},
            "Politics",    new String[]{"How many permanent UN Security Council members are there?","3","4","5","6","2","The 5 permanent members are USA, UK, France, Russia, China."}
        );

        String[] q = fallbacks.getOrDefault(skill, fallbacks.get("Science"));

        Quest quest = new Quest();
        quest.setId(UUID.randomUUID().toString());
        quest.setSkill(skill);
        quest.setDifficulty(difficulty);
        quest.setQuestion(q[0]);
        quest.setChoices(List.of(q[1], q[2], q[3], q[4]));
        quest.setCorrectIndex(Integer.parseInt(q[5]));
        quest.setExplanation(q[6]);
        quest.setXpReward(difficulty * 50);
        quest.setScoreReward(difficulty * 100);
        quest.setNpcName("The Archivist");
        quest.setNpcDialogue("The AI terminals are offline. Answer from memory, Dweller.");
        quest.setGeneratedBy("Static Fallback");
        return quest;
    }
}
