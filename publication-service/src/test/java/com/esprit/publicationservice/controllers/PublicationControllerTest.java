package com.esprit.publicationservice.controllers;

import com.esprit.publicationservice.dto.UserBlockDTO;
import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.StatutPublication;
import com.esprit.publicationservice.entities.TypePublication;
import com.esprit.publicationservice.services.PublicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PublicationController.class)
@WithMockUser  // Désactive Spring Security pour tous les tests de cette classe
class PublicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicationService publicationService;

    private Publication makePublication(Integer id, Integer userId) {
        Publication p = new Publication();
        p.setId(id);
        p.setUserId(userId);
        p.setTitre("Titre de test");
        p.setContenue("Contenu de test");
        p.setType(TypePublication.ARTICLE);
        p.setStatut(StatutPublication.ACTIVE);
        return p;
    }

    @Nested
    @DisplayName("GET /api/publications")
    class GetAllTests {

        @Test
        @DisplayName("retourne 200 avec la liste des publications actives")
        void returns200WithPublicationList() throws Exception {
            Publication p = makePublication(1, 10);
            when(publicationService.getAllPublications()).thenReturn(List.of(p));

            mockMvc.perform(get("/api/publications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].titre").value("Titre de test"));
        }

        @Test
        @DisplayName("retourne 200 avec une liste vide si aucune publication")
        void returns200WithEmptyList() throws Exception {
            when(publicationService.getAllPublications()).thenReturn(List.of());

            mockMvc.perform(get("/api/publications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }


    @Nested
    @DisplayName("GET /api/publications/admin/all")
    class GetAllAdminTests {

        @Test
        @DisplayName("retourne 200 avec toutes les publications (admin)")
        void returns200WithAllPublications() throws Exception {
            Publication p1 = makePublication(1, 10);
            Publication p2 = makePublication(2, 11);
            p2.setStatut(StatutPublication.ARCHIVED);
            when(publicationService.getAllPublicationsAdmin()).thenReturn(List.of(p1, p2));

            mockMvc.perform(get("/api/publications/admin/all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }


    @Nested
    @DisplayName("GET /api/publications/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("retourne 200 avec la publication si elle existe")
        void returns200WhenFound() throws Exception {
            Publication p = makePublication(1, 10);
            when(publicationService.getPublicationById(1)).thenReturn(p);

            mockMvc.perform(get("/api/publications/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.titre").value("Titre de test"));
        }

        @Test
        @DisplayName("retourne 404 si la publication n'existe pas")
        void returns404WhenNotFound() throws Exception {
            when(publicationService.getPublicationById(99))
                    .thenThrow(new RuntimeException("Publication not found: 99"));

            mockMvc.perform(get("/api/publications/99"))
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("GET /api/publications/user/{userId}")
    class GetByUserTests {

        @Test
        @DisplayName("retourne 200 avec les publications de l'utilisateur")
        void returns200WithUserPublications() throws Exception {
            Publication p = makePublication(1, 5);
            when(publicationService.getPublicationsByUserId(5)).thenReturn(List.of(p));

            mockMvc.perform(get("/api/publications/user/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(5));
        }
    }


    @Nested
    @DisplayName("GET /api/publications/user/{userId}/archived")
    class GetArchivedByUserTests {

        @Test
        @DisplayName("retourne 200 avec les publications archivées de l'utilisateur")
        void returns200WithArchivedPublications() throws Exception {
            Publication p = makePublication(1, 5);
            p.setStatut(StatutPublication.ARCHIVED);
            when(publicationService.getArchivedByUserId(5)).thenReturn(List.of(p));

            mockMvc.perform(get("/api/publications/user/5/archived"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].statut").value("ARCHIVED"));
        }
    }


    @Nested
    @DisplayName("GET /api/publications/user/{userId}/block-status")
    class GetBlockStatusTests {

        @Test
        @DisplayName("retourne 200 avec blocked=true et warningCount=3")
        void returns200WithBlockedTrue() throws Exception {
            when(publicationService.isUserBlocked(1)).thenReturn(true);
            when(publicationService.getArchivedCount(1)).thenReturn(3L);

            mockMvc.perform(get("/api/publications/user/1/block-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blocked").value(true))
                    .andExpect(jsonPath("$.warningCount").value(3));
        }

        @Test
        @DisplayName("retourne 200 avec blocked=false et warningCount=1")
        void returns200WithBlockedFalse() throws Exception {
            when(publicationService.isUserBlocked(2)).thenReturn(false);
            when(publicationService.getArchivedCount(2)).thenReturn(1L);

            mockMvc.perform(get("/api/publications/user/2/block-status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blocked").value(false))
                    .andExpect(jsonPath("$.warningCount").value(1));
        }
    }


    @Nested
    @DisplayName("GET /api/publications/admin/blocked-users")
    class GetBlockedUsersTests {

        @Test
        @DisplayName("retourne 200 avec la liste des utilisateurs bloqués")
        void returns200WithBlockedUsersList() throws Exception {
            UserBlockDTO dto = new UserBlockDTO(1, "Jean", "Dupont", 3L);
            when(publicationService.getAllUsersBlockStatus()).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/publications/admin/blocked-users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(1))
                    .andExpect(jsonPath("$[0].blocked").value(true));
        }
    }


    @Nested
    @DisplayName("POST /api/publications/{id}/signaler")
    class SignalerTests {

        @Test
        @DisplayName("retourne 200 avec la publication signalée")
        void returns200OnSuccessfulSignalement() throws Exception {
            Publication p = makePublication(1, 10);
            when(publicationService.signalerPublication(1, 5, "Spam")).thenReturn(p);

            mockMvc.perform(post("/api/publications/1/signaler")
                            .param("userId", "5")
                            .param("raison", "Spam")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("retourne 409 si la publication est déjà signalée par cet utilisateur")
        void returns409WhenAlreadySignaled() throws Exception {
            when(publicationService.signalerPublication(1, 5, "Spam"))
                    .thenThrow(new IllegalStateException("Vous avez déjà signalé cette publication."));

            mockMvc.perform(post("/api/publications/1/signaler")
                            .param("userId", "5")
                            .param("raison", "Spam")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("retourne 409 si la publication n'est pas active")
        void returns409WhenPublicationNotActive() throws Exception {
            when(publicationService.signalerPublication(1, 5, ""))
                    .thenThrow(new IllegalStateException("Cette publication n'est pas active."));

            mockMvc.perform(post("/api/publications/1/signaler")
                            .param("userId", "5")
                            .with(csrf()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("retourne 404 si la publication est introuvable")
        void returns404WhenPublicationNotFound() throws Exception {
            when(publicationService.signalerPublication(99, 5, ""))
                    .thenThrow(new RuntimeException("Publication not found: 99"));

            mockMvc.perform(post("/api/publications/99/signaler")
                            .param("userId", "5")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("POST /api/publications/admin/users/{userId}/reactiver-compte")
    class ReactiverCompteTests {

        @Test
        @DisplayName("retourne 200 avec message de succès")
        void returns200OnSuccess() throws Exception {
            doNothing().when(publicationService).reactiverCompteUser(1);

            mockMvc.perform(post("/api/publications/admin/users/1/reactiver-compte")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("retourne 404 si l'utilisateur est introuvable")
        void returns404WhenUserNotFound() throws Exception {
            doThrow(new RuntimeException("User not found"))
                    .when(publicationService).reactiverCompteUser(99);

            mockMvc.perform(post("/api/publications/admin/users/99/reactiver-compte")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("DELETE /api/publications/{id}")
    class DeletePublicationTests {

        @Test
        @DisplayName("retourne 200 avec message 'Deleted' si succès")
        void returns200OnSuccess() throws Exception {
            doNothing().when(publicationService).deletePublication(1, 10);

            mockMvc.perform(delete("/api/publications/1")
                            .param("userId", "10")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Deleted"));
        }

        @Test
        @DisplayName("retourne 403 si l'utilisateur n'est pas le propriétaire")
        void returns403WhenNotOwner() throws Exception {
            doThrow(new RuntimeException("Not authorized"))
                    .when(publicationService).deletePublication(1, 99);

            mockMvc.perform(delete("/api/publications/1")
                            .param("userId", "99")
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }


    @Nested
    @DisplayName("DELETE /api/publications/admin/{id}")
    class AdminDeletePublicationTests {

        @Test
        @DisplayName("retourne 200 avec message 'Deleted' si succès")
        void returns200OnSuccess() throws Exception {
            doNothing().when(publicationService).adminDeletePublication(1);

            mockMvc.perform(delete("/api/publications/admin/1")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Deleted"));
        }

        @Test
        @DisplayName("retourne 404 si la publication est introuvable")
        void returns404WhenNotFound() throws Exception {
            doThrow(new RuntimeException("Publication not found"))
                    .when(publicationService).adminDeletePublication(99);

            mockMvc.perform(delete("/api/publications/admin/99")
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("GET /api/publications/type/{type}")
    class GetByTypeTests {

        @Test
        @DisplayName("retourne 200 avec les publications du type ARTICLE")
        void returns200WithArticlePublications() throws Exception {
            Publication p = makePublication(1, 10);
            p.setType(TypePublication.ARTICLE);
            when(publicationService.getPublicationsByType(TypePublication.ARTICLE))
                    .thenReturn(List.of(p));

            mockMvc.perform(get("/api/publications/type/ARTICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("ARTICLE"));
        }

        @Test
        @DisplayName("retourne 200 avec liste vide si aucun REVIEW")
        void returns200WithEmptyListForReview() throws Exception {
            when(publicationService.getPublicationsByType(TypePublication.REVIEW))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/publications/type/REVIEW"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}