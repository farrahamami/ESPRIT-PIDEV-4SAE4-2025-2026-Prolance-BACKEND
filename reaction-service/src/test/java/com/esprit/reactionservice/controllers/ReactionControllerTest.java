package com.esprit.reactionservice.controllers;

import com.esprit.reactionservice.dto.ReactorDTO;
import com.esprit.reactionservice.dto.ReactionSummaryDTO;
import com.esprit.reactionservice.entities.Reaction;
import com.esprit.reactionservice.entities.TypeReaction;
import com.esprit.reactionservice.services.ReactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReactionController.class)
@WithMockUser
class ReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReactionService reactionService;

    private Reaction makeReaction(Integer id, Integer userId, Integer publicationId, TypeReaction type) {
        Reaction r = new Reaction();
        r.setId(id);
        r.setUserId(userId);
        r.setPublicationId(publicationId);
        r.setType(type);
        return r;
    }

    private ReactionSummaryDTO makeSummary(long likes, long dislikes, long hearts,
                                           TypeReaction userReaction) {
        ReactionSummaryDTO dto = new ReactionSummaryDTO();
        dto.setLIKE(likes);
        dto.setDISLIKE(dislikes);
        dto.setHEART(hearts);
        dto.setUserReaction(userReaction);
        dto.setReactors(List.of());
        return dto;
    }

    @Nested
    @DisplayName("POST /api/reactions/publication/{publicationId}")
    class ToggleReactionTests {

        @Test
        @DisplayName("retourne 200 avec la réaction créée ou modifiée")
        void returns200WithReaction_whenReactionCreatedOrChanged() throws Exception {
            Reaction r = makeReaction(1, 1, 10, TypeReaction.LIKE);
            when(reactionService.toggleReaction(10, 1, TypeReaction.LIKE))
                    .thenReturn(Optional.of(r));

            mockMvc.perform(post("/api/reactions/publication/10")
                            .param("userId", "1")
                            .param("type", "LIKE")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.type").value("LIKE"));
        }

        @Test
        @DisplayName("retourne 204 No Content quand la réaction est supprimée (toggle off)")
        void returns204_whenReactionIsRemoved() throws Exception {
            when(reactionService.toggleReaction(10, 1, TypeReaction.LIKE))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/reactions/publication/10")
                            .param("userId", "1")
                            .param("type", "LIKE")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("retourne 200 avec une réaction DISLIKE")
        void returns200WithDislikeReaction() throws Exception {
            Reaction r = makeReaction(2, 1, 10, TypeReaction.DISLIKE);
            when(reactionService.toggleReaction(10, 1, TypeReaction.DISLIKE))
                    .thenReturn(Optional.of(r));

            mockMvc.perform(post("/api/reactions/publication/10")
                            .param("userId", "1")
                            .param("type", "DISLIKE")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("DISLIKE"));
        }

        @Test
        @DisplayName("retourne 200 avec une réaction HEART")
        void returns200WithHeartReaction() throws Exception {
            Reaction r = makeReaction(3, 1, 10, TypeReaction.HEART);
            when(reactionService.toggleReaction(10, 1, TypeReaction.HEART))
                    .thenReturn(Optional.of(r));

            mockMvc.perform(post("/api/reactions/publication/10")
                            .param("userId", "1")
                            .param("type", "HEART")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("HEART"));
        }

        @Test
        @DisplayName("retourne 204 No Content quand DISLIKE est supprimé par toggle")
        void returns204_whenDislikeIsRemoved() throws Exception {
            when(reactionService.toggleReaction(10, 1, TypeReaction.DISLIKE))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/reactions/publication/10")
                            .param("userId", "1")
                            .param("type", "DISLIKE")
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/reactions/publication/{publicationId}/summary")
    class GetSummaryTests {

        @Test
        @DisplayName("retourne 200 avec le résumé des réactions")
        void returns200WithSummary() throws Exception {
            ReactionSummaryDTO summary = makeSummary(3, 1, 2, TypeReaction.LIKE);
            when(reactionService.getSummary(10, 1)).thenReturn(summary);

            mockMvc.perform(get("/api/reactions/publication/10/summary")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.LIKE").value(3))
                    .andExpect(jsonPath("$.DISLIKE").value(1))
                    .andExpect(jsonPath("$.HEART").value(2))
                    .andExpect(jsonPath("$.userReaction").value("LIKE"));
        }

        @Test
        @DisplayName("retourne 200 avec compteurs à zéro si aucune réaction")
        void returns200WithZeroCounts_whenNoReactions() throws Exception {
            ReactionSummaryDTO summary = makeSummary(0, 0, 0, null);
            when(reactionService.getSummary(10, 1)).thenReturn(summary);

            mockMvc.perform(get("/api/reactions/publication/10/summary")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.LIKE").value(0))
                    .andExpect(jsonPath("$.DISLIKE").value(0))
                    .andExpect(jsonPath("$.HEART").value(0))
                    .andExpect(jsonPath("$.userReaction").doesNotExist());
        }

        @Test
        @DisplayName("retourne 200 avec la liste des reactors")
        void returns200WithReactorsList() throws Exception {
            ReactionSummaryDTO summary = new ReactionSummaryDTO();
            summary.setLIKE(1);
            summary.setDISLIKE(0);
            summary.setHEART(0);
            summary.setUserReaction(TypeReaction.LIKE);
            summary.setReactors(List.of(new ReactorDTO(1, "Jean Dupont", TypeReaction.LIKE)));
            when(reactionService.getSummary(10, 1)).thenReturn(summary);

            mockMvc.perform(get("/api/reactions/publication/10/summary")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reactors").isArray())
                    .andExpect(jsonPath("$.reactors[0].userId").value(1))
                    .andExpect(jsonPath("$.reactors[0].userName").value("Jean Dupont"))
                    .andExpect(jsonPath("$.reactors[0].type").value("LIKE"));
        }

        @Test
        @DisplayName("retourne 200 avec userReaction null si l'utilisateur n'a pas réagi")
        void returns200WithNullUserReaction_whenUserHasNoReaction() throws Exception {
            ReactionSummaryDTO summary = makeSummary(2, 0, 0, null);
            when(reactionService.getSummary(10, 99)).thenReturn(summary);

            mockMvc.perform(get("/api/reactions/publication/10/summary")
                            .param("userId", "99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userReaction").doesNotExist());
        }
    }
}