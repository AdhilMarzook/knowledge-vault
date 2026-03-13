package com.knowledgevault.controller;

import com.knowledgevault.model.*;
import com.knowledgevault.security.VaultUserDetailsService;
import com.knowledgevault.service.PlayerService;
import com.knowledgevault.service.QuestGenerationService;
import com.knowledgevault.service.provider.AiProviderRouter;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Validated
public class GameController {

    private static final Set<String> VALID_SKILLS = Set.of(
        "Science", "History", "Technology", "Philosophy",
        "Mathematics", "Literature", "Geography", "Politics"
    );

    private final PlayerService playerService;
    private final QuestGenerationService questGenerationService;
    private final VaultUserDetailsService userDetailsService;
    private final AiProviderRouter providerRouter;

    public GameController(
        PlayerService playerService,
        QuestGenerationService questGenerationService,
        VaultUserDetailsService userDetailsService,
        AiProviderRouter providerRouter
    ) {
        this.playerService          = playerService;
        this.questGenerationService = questGenerationService;
        this.userDetailsService     = userDetailsService;
        this.providerRouter         = providerRouter;
    }

    @GetMapping("/players/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Player> getMyPlayer(@AuthenticationPrincipal UserDetails userDetails) {
        String playerId = resolvePlayerId(userDetails);
        return playerService.getPlayer(playerId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Player>> getLeaderboard() {
        return ResponseEntity.ok(playerService.getLeaderboard());
    }

    @GetMapping("/quest")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Quest> generateQuest(
        @RequestParam @Pattern(regexp = "^[A-Za-z]+$") String skill,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (!VALID_SKILLS.contains(skill)) return ResponseEntity.badRequest().build();

        String playerId = resolvePlayerId(userDetails);
        return playerService.getPlayer(playerId).map(player -> {
            int skillLevel = player.getSkills().getOrDefault(skill, 1);
            Quest quest = questGenerationService.generateQuest(skill, skillLevel);
            playerService.storeQuest(quest);
            return ResponseEntity.ok(quest);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/answer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnswerResult> submitAnswer(
        @RequestBody AnswerRequest request,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        request.setPlayerId(resolvePlayerId(userDetails));
        if (request.getSelectedIndex() < 0 || request.getSelectedIndex() > 3) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(playerService.processAnswer(request));
    }

    @GetMapping("/providers/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> providerStatus() {
        return ResponseEntity.ok(Map.of(
            "activeProvider", providerRouter.getActiveProviderName(),
            "providers", providerRouter.getProviderStatus()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ONLINE",
            "aiProvider", providerRouter.getActiveProviderName()
        ));
    }

    private String resolvePlayerId(UserDetails userDetails) {
        var account = userDetailsService.findById(userDetails.getUsername());
        if (account == null) throw new RuntimeException("Account not found");
        return account.getPlayerId();
    }
}
