package com.esprit.commentaireservice.controllers;

import com.esprit.commentaireservice.entities.Commentaire;
import com.esprit.commentaireservice.services.CommentaireService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentaireController.class)
@WithMockUser
class CommentaireControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentaireService commentaireService;

    private Commentaire makeCommentaire(Integer id, Integer userId, Integer publicationId) {
        Commentaire c = new Commentaire();
        c.setId(id);
        c.setUserId(userId);
        c.setPublicationId(publicationId);
        c.setContenue("Contenu de test");
        c.setPinned(false);
        c.setReplies(new ArrayList<>());
        return c;
    }

    @Nested
    @DisplayName("GET /api/commentaires")
    class GetAllTests {

        @Test
        @DisplayName("retourne 200 avec la liste de tous les commentaires")
        void returns200WithCommentaireList() throws Exception {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireService.getAllCommentaires()).thenReturn(List.of(c));

            mockMvc.perform(get("/api/commentaires"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].contenue").value("Contenu de test"));
        }

        @Test
        @DisplayName("retourne 200 avec une liste vide si aucun commentaire")
        void returns200WithEmptyList() throws Exception {
            when(commentaireService.getAllCommentaires()).thenReturn(List.of());

            mockMvc.perform(get("/api/commentaires"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/commentaires/publication/{publicationId}")
    class GetByPublicationTests {

        @Test
        @DisplayName("retourne 200 avec les commentaires de la publication")
        void returns200WithCommentairesForPublication() throws Exception {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireService.getByPublicationId(5)).thenReturn(List.of(c));

            mockMvc.perform(get("/api/commentaires/publication/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].publicationId").value(5));
        }

        @Test
        @DisplayName("retourne 200 avec liste vide si aucun commentaire pour cette publication")
        void returns200WithEmptyList_whenNoCommentaires() throws Exception {
            when(commentaireService.getByPublicationId(99)).thenReturn(List.of());

            mockMvc.perform(get("/api/commentaires/publication/99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/commentaires/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("retourne 200 avec le commentaire si trouvé")
        void returns200WhenFound() throws Exception {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireService.getById(1)).thenReturn(c);

            mockMvc.perform(get("/api/commentaires/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.contenue").value("Contenu de test"));
        }

        @Test
        @DisplayName("retourne 404 si le commentaire n'existe pas")
        void returns404WhenNotFound() throws Exception {
            when(commentaireService.getById(99))
                    .thenThrow(new RuntimeException("Commentaire not found: 99"));

            mockMvc.perform(get("/api/commentaires/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/commentaires")
    class CreateCommentaireTests {

        @Test
        @DisplayName("retourne 201 avec le commentaire créé")
        void returns201OnSuccessfulCreate() throws Exception {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireService.create("Mon commentaire", 5, 10)).thenReturn(c);

            mockMvc.perform(post("/api/commentaires")
                            .param("contenue", "Mon commentaire")
                            .param("publicationId", "5")
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("retourne 400 si le contenu est invalide (IllegalArgumentException)")
        void returns400WhenContenueIsInvalid() throws Exception {
            when(commentaireService.create(eq(""), anyInt(), anyInt()))
                    .thenThrow(new IllegalArgumentException("Content required"));

            mockMvc.perform(post("/api/commentaires")
                            .param("contenue", "")
                            .param("publicationId", "5")
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retourne 500 si une erreur inattendue se produit")
        void returns500OnUnexpectedError() throws Exception {
            when(commentaireService.create(anyString(), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Publication not found: 999"));

            mockMvc.perform(post("/api/commentaires")
                            .param("contenue", "Mon commentaire")
                            .param("publicationId", "999")
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/commentaires/{parentId}/reply")
    class ReplyTests {

        @Test
        @DisplayName("retourne 201 avec la réponse créée")
        void returns201OnSuccessfulReply() throws Exception {
            Commentaire reply = makeCommentaire(2, 20, 5);
            when(commentaireService.reply("Ma réponse", 1, 5, 20)).thenReturn(reply);

            mockMvc.perform(post("/api/commentaires/1/reply")
                            .param("contenue", "Ma réponse")
                            .param("publicationId", "5")
                            .param("userId", "20")
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2));
        }

        @Test
        @DisplayName("retourne 500 si le commentaire parent n'existe pas")
        void returns500WhenParentNotFound() throws Exception {
            when(commentaireService.reply(anyString(), eq(99), anyInt(), anyInt()))
                    .thenThrow(new RuntimeException("Commentaire not found: 99"));

            mockMvc.perform(post("/api/commentaires/99/reply")
                            .param("contenue", "réponse")
                            .param("publicationId", "5")
                            .param("userId", "20")
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/commentaires/{id}")
    class UpdateCommentaireTests {

        @Test
        @DisplayName("retourne 200 avec le commentaire mis à jour")
        void returns200OnSuccessfulUpdate() throws Exception {
            Commentaire updated = makeCommentaire(1, 10, 5);
            updated.setContenue("Nouveau contenu");
            when(commentaireService.update(1, "Nouveau contenu", 10)).thenReturn(updated);

            mockMvc.perform(put("/api/commentaires/1")
                            .param("contenue", "Nouveau contenu")
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenue").value("Nouveau contenu"));
        }

        @Test
        @DisplayName("retourne 403 si l'utilisateur n'est pas le propriétaire")
        void returns403WhenNotOwner() throws Exception {
            when(commentaireService.update(1, "contenu", 99))
                    .thenThrow(new RuntimeException("Not authorized"));

            mockMvc.perform(put("/api/commentaires/1")
                            .param("contenue", "contenu")
                            .param("userId", "99")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/commentaires/{id}")
    class DeleteCommentaireTests {

        @Test
        @DisplayName("retourne 200 avec message 'Deleted' si succès")
        void returns200OnSuccess() throws Exception {
            doNothing().when(commentaireService).delete(1, 10);

            mockMvc.perform(delete("/api/commentaires/1")
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Deleted"));
        }

        @Test
        @DisplayName("retourne 403 si l'utilisateur n'est pas le propriétaire")
        void returns403WhenNotOwner() throws Exception {
            doThrow(new RuntimeException("Not authorized"))
                    .when(commentaireService).delete(1, 99);

            mockMvc.perform(delete("/api/commentaires/1")
                            .param("userId", "99")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("retourne 403 si le commentaire n'existe pas (RuntimeException)")
        void returns403WhenNotFound() throws Exception {
            doThrow(new RuntimeException("Commentaire not found: 99"))
                    .when(commentaireService).delete(99, 10);

            mockMvc.perform(delete("/api/commentaires/99")
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/commentaires/{id}/pin")
    class TogglePinTests {

        @Test
        @DisplayName("retourne 200 avec le commentaire épinglé")
        void returns200OnSuccessfulPin() throws Exception {
            Commentaire pinned = makeCommentaire(1, 10, 5);
            pinned.setPinned(true);
            when(commentaireService.togglePin(1, 20)).thenReturn(pinned);

            mockMvc.perform(put("/api/commentaires/1/pin")
                            .param("userId", "20")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pinned").value(true));
        }

        @Test
        @DisplayName("retourne 403 si l'utilisateur n'est pas le propriétaire de la publication")
        void returns403WhenNotPublicationOwner() throws Exception {
            when(commentaireService.togglePin(1, 99))
                    .thenThrow(new RuntimeException("Only publication owner can pin"));

            mockMvc.perform(put("/api/commentaires/1/pin")
                            .param("userId", "99")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("retourne 403 si le commentaire est introuvable")
        void returns403WhenNotFound() throws Exception {
            when(commentaireService.togglePin(99, 20))
                    .thenThrow(new RuntimeException("Commentaire not found: 99"));

            mockMvc.perform(put("/api/commentaires/99/pin")
                            .param("userId", "20")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}