package com.esprit.reactionservice.controllers;

import com.esprit.reactionservice.dto.ReactionSummaryDTO;
import com.esprit.reactionservice.entities.Reaction;
import com.esprit.reactionservice.entities.TypeReaction;
import com.esprit.reactionservice.services.ReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/reactions")
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;

    @PostMapping("/publication/{publicationId}")
    public ResponseEntity<Reaction> toggle(@PathVariable Integer publicationId,
                                           @RequestParam Integer userId,
                                           @RequestParam TypeReaction type) {
        Optional<Reaction> result = reactionService.toggleReaction(publicationId, userId, type);
        return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/publication/{publicationId}/summary")
    public ResponseEntity<ReactionSummaryDTO> summary(@PathVariable Integer publicationId,
                                                      @RequestParam Integer userId) {
        return ResponseEntity.ok(reactionService.getSummary(publicationId, userId));
    }
}