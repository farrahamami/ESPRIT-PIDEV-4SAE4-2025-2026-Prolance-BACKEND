package com.esprit.reactionservice.services;

import com.esprit.reactionservice.clients.PublicationClient;
import com.esprit.reactionservice.clients.UserClient;
import com.esprit.reactionservice.dto.ReactionSummaryDTO;
import com.esprit.reactionservice.dto.ReactorDTO;
import com.esprit.reactionservice.dto.UserDTO;
import com.esprit.reactionservice.entities.Reaction;
import com.esprit.reactionservice.entities.TypeReaction;
import com.esprit.reactionservice.repositories.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReactionService {
    private final ReactionRepository reactionRepository;
    private final UserClient userClient;
    private final PublicationClient publicationClient;

    @Transactional
    public Optional<Reaction> toggleReaction(Integer publicationId, Integer userId, TypeReaction type) {
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new UserNotFoundException("User not found: " + userId);
        }

        try {
            publicationClient.getPublicationById(publicationId);
        } catch (Exception e) {
            throw new PublicationNotFoundException("Publication not found: " + publicationId);
        }

        Optional<Reaction> existing = reactionRepository.findByPublicationIdAndUserId(publicationId, userId);
        if (existing.isPresent()) {
            Reaction r = existing.get();
            if (r.getType() == type) {
                reactionRepository.delete(r);
                return Optional.empty();
            } else {
                r.setType(type);
                return Optional.of(reactionRepository.save(r));
            }
        } else {
            Reaction r = new Reaction();
            r.setUserId(userId);
            r.setPublicationId(publicationId);
            r.setType(type);
            return Optional.of(reactionRepository.save(r));
        }
    }

    public ReactionSummaryDTO getSummary(Integer publicationId, Integer userId) {
        List<Reaction> all = reactionRepository.findByPublicationId(publicationId);
        long likes = all.stream().filter(r -> r.getType() == TypeReaction.LIKE).count();
        long dislikes = all.stream().filter(r -> r.getType() == TypeReaction.DISLIKE).count();
        long hearts = all.stream().filter(r -> r.getType() == TypeReaction.HEART).count();
        TypeReaction userReaction = all.stream()
                .filter(r -> r.getUserId().equals(userId))
                .map(Reaction::getType)
                .findFirst()
                .orElse(null);

        List<ReactorDTO> reactors = all.stream().map(r -> {
            String name = "User " + r.getUserId();
            try {
                UserDTO dto = userClient.getUserById(r.getUserId());
                name = dto.getName() + " " + dto.getLastName();
            } catch (Exception ignored) {
                // User enrichment failed - continue with fallback name
            }
            return new ReactorDTO(r.getUserId(), name, r.getType());
        }).toList();

        return new ReactionSummaryDTO(likes, dislikes, hearts, userReaction, reactors);
    }
}

// Custom exceptions
class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

class PublicationNotFoundException extends RuntimeException {
    public PublicationNotFoundException(String message) {
        super(message);
    }
}