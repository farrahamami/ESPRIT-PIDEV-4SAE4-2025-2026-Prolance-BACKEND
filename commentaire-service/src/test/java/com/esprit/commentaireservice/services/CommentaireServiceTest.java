package com.esprit.commentaireservice.services;

import com.esprit.commentaireservice.clients.PublicationClient;
import com.esprit.commentaireservice.clients.UserClient;
import com.esprit.commentaireservice.dto.PublicationDTO;
import com.esprit.commentaireservice.entities.Commentaire;
import com.esprit.commentaireservice.repositories.CommentaireRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentaireServiceTest {

    @Mock
    private CommentaireRepository commentaireRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private PublicationClient publicationClient;

    @InjectMocks
    private CommentaireService commentaireService;

    private Commentaire makeCommentaire(Integer id, Integer userId, Integer publicationId) {
        Commentaire c = new Commentaire();
        c.setId(id);
        c.setUserId(userId);
        c.setPublicationId(publicationId);
        c.setContenue("Contenu de test");
        c.setReplies(new ArrayList<>());
        return c;
    }

    private PublicationDTO makePublication(Integer id, Integer userId) {
        PublicationDTO p = new PublicationDTO();
        p.setId(id);
        p.setUserId(userId);
        return p;
    }

    @Nested
    @DisplayName("getAllCommentaires()")
    class GetAllCommentairesTests {

        @Test
        @DisplayName("retourne la liste de tous les commentaires enrichis avec l'utilisateur")
        void returnsAllCommentairesEnrichedWithUser() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of(c));
            when(userClient.getUserById(10)).thenReturn(new com.esprit.commentaireservice.dto.UserDTO());

            List<Commentaire> result = commentaireService.getAllCommentaires();

            assertThat(result).hasSize(1);
            verify(commentaireRepository).findAllByOrderByCreateAtDesc();
            verify(userClient).getUserById(10);
        }

        @Test
        @DisplayName("retourne une liste vide si aucun commentaire")
        void returnsEmptyList_whenNoCommentaires() {
            when(commentaireRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of());

            List<Commentaire> result = commentaireService.getAllCommentaires();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ne lève pas d'exception si userClient échoue (enrichissement silencieux)")
        void doesNotThrow_whenUserClientFails() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of(c));
            when(userClient.getUserById(10)).thenThrow(new RuntimeException("user-service unavailable"));

            List<Commentaire> result = commentaireService.getAllCommentaires();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUser()).isNull();
        }
    }

    @Nested
    @DisplayName("getByPublicationId()")
    class GetByPublicationIdTests {

        @Test
        @DisplayName("retourne les commentaires racines d'une publication, enrichis")
        void returnsRootCommentairesForPublication() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findRootByPublicationIdOrderByPinned(5)).thenReturn(List.of(c));
            when(userClient.getUserById(10)).thenReturn(new com.esprit.commentaireservice.dto.UserDTO());

            List<Commentaire> result = commentaireService.getByPublicationId(5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPublicationId()).isEqualTo(5);
            verify(commentaireRepository).findRootByPublicationIdOrderByPinned(5);
        }

        @Test
        @DisplayName("retourne liste vide si aucun commentaire pour cette publication")
        void returnsEmptyList_whenNoCommentairesForPublication() {
            when(commentaireRepository.findRootByPublicationIdOrderByPinned(99)).thenReturn(List.of());

            List<Commentaire> result = commentaireService.getByPublicationId(99);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("retourne le commentaire si trouvé")
        void returnsCommentaire_whenFound() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));

            Commentaire result = commentaireService.getById(1);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("lève CommentaireNotFoundException si commentaire introuvable")
        void throwsCommentaireNotFoundException_whenNotFound() {
            when(commentaireRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentaireService.getById(99))
                    .isInstanceOf(CommentaireNotFoundException.class)
                    .hasMessageContaining("Commentaire not found: 99");
        }
    }

    @Nested
    @DisplayName("create()")
    class CreateCommentaireTests {

        @Test
        @DisplayName("crée et retourne un commentaire valide avec enrichissement user")
        void createsAndReturnsCommentaire_whenValid() {
            when(publicationClient.getPublicationById(5)).thenReturn(makePublication(5, 99));
            when(commentaireRepository.save(any())).thenAnswer(inv -> {
                Commentaire c = inv.getArgument(0);
                c.setId(1);
                c.setReplies(new ArrayList<>());
                return c;
            });
            when(userClient.getUserById(10)).thenReturn(new com.esprit.commentaireservice.dto.UserDTO());

            Commentaire result = commentaireService.create("Mon commentaire", 5, 10);

            assertThat(result).isNotNull();
            assertThat(result.getContenue()).isEqualTo("Mon commentaire");
            assertThat(result.getPublicationId()).isEqualTo(5);
            assertThat(result.getUserId()).isEqualTo(10);
            verify(commentaireRepository).save(any());
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est null")
        void throwsIllegalArgumentException_whenContenueIsNull() {
            assertThatThrownBy(() -> commentaireService.create(null, 5, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est vide")
        void throwsIllegalArgumentException_whenContenueIsBlank() {
            assertThatThrownBy(() -> commentaireService.create("   ", 5, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content required");
        }

        @Test
        @DisplayName("lève PublicationNotFoundException si la publication n'existe pas")
        void throwsPublicationNotFoundException_whenPublicationNotFound() {
            when(publicationClient.getPublicationById(99)).thenThrow(new RuntimeException("not found"));

            assertThatThrownBy(() -> commentaireService.create("contenu", 99, 10))
                    .isInstanceOf(PublicationNotFoundException.class)
                    .hasMessageContaining("Publication not found: 99");
        }
    }

    @Nested
    @DisplayName("reply()")
    class ReplyTests {

        @Test
        @DisplayName("crée une réponse valide avec le parent correctement assigné")
        void createsReply_withParentAssigned() {
            Commentaire parent = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(parent));
            when(commentaireRepository.save(any())).thenAnswer(inv -> {
                Commentaire c = inv.getArgument(0);
                c.setId(2);
                c.setReplies(new ArrayList<>());
                return c;
            });
            when(userClient.getUserById(20)).thenReturn(new com.esprit.commentaireservice.dto.UserDTO());

            Commentaire result = commentaireService.reply("Ma réponse", 1, 5, 20);

            assertThat(result).isNotNull();
            assertThat(result.getParent()).isEqualTo(parent);
            assertThat(result.getContenue()).isEqualTo("Ma réponse");
            verify(commentaireRepository).save(any());
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est null")
        void throwsIllegalArgumentException_whenContenueIsNull() {
            assertThatThrownBy(() -> commentaireService.reply(null, 1, 5, 20))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est vide")
        void throwsIllegalArgumentException_whenContenueIsBlank() {
            assertThatThrownBy(() -> commentaireService.reply("  ", 1, 5, 20))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content required");
        }

        @Test
        @DisplayName("lève CommentaireNotFoundException si le commentaire parent est introuvable")
        void throwsCommentaireNotFoundException_whenParentNotFound() {
            when(commentaireRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentaireService.reply("réponse", 99, 5, 20))
                    .isInstanceOf(CommentaireNotFoundException.class)
                    .hasMessageContaining("Commentaire not found: 99");
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateCommentaireTests {

        @Test
        @DisplayName("met à jour le contenu si l'utilisateur est le propriétaire")
        void updatesContenue_whenOwner() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));
            when(commentaireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Commentaire result = commentaireService.update(1, "Nouveau contenu", 10);

            assertThat(result.getContenue()).isEqualTo("Nouveau contenu");
            verify(commentaireRepository).save(c);
        }

        @Test
        @DisplayName("lève UnauthorizedCommentaireException si l'utilisateur n'est pas le propriétaire")
        void throwsUnauthorizedCommentaireException_whenNotOwner() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> commentaireService.update(1, "Nouveau contenu", 99))
                    .isInstanceOf(UnauthorizedCommentaireException.class)
                    .hasMessageContaining("Not authorized to update this commentaire");

            verify(commentaireRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève CommentaireNotFoundException si le commentaire est introuvable")
        void throwsCommentaireNotFoundException_whenNotFound() {
            when(commentaireRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentaireService.update(99, "contenu", 10))
                    .isInstanceOf(CommentaireNotFoundException.class)
                    .hasMessageContaining("Commentaire not found: 99");
        }

        @Test
        @DisplayName("ne modifie pas le contenu si le nouveau contenu est null")
        void doesNotChangeContenue_whenNewContenueIsNull() {
            Commentaire c = makeCommentaire(1, 10, 5);
            c.setContenue("Contenu original");
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));
            when(commentaireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Commentaire result = commentaireService.update(1, null, 10);

            assertThat(result.getContenue()).isEqualTo("Contenu original");
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteCommentaireTests {

        @Test
        @DisplayName("supprime le commentaire si l'utilisateur est le propriétaire")
        void deletesCommentaire_whenOwner() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));

            commentaireService.delete(1, 10);

            verify(commentaireRepository).delete(c);
        }

        @Test
        @DisplayName("lève UnauthorizedCommentaireException si l'utilisateur n'est pas le propriétaire")
        void throwsUnauthorizedCommentaireException_whenNotOwner() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> commentaireService.delete(1, 99))
                    .isInstanceOf(UnauthorizedCommentaireException.class)
                    .hasMessageContaining("Not authorized to delete this commentaire");

            verify(commentaireRepository, never()).delete(any());
        }

        @Test
        @DisplayName("lève CommentaireNotFoundException si le commentaire est introuvable")
        void throwsCommentaireNotFoundException_whenNotFound() {
            when(commentaireRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentaireService.delete(99, 10))
                    .isInstanceOf(CommentaireNotFoundException.class)
                    .hasMessageContaining("Commentaire not found: 99");
        }
    }

    @Nested
    @DisplayName("togglePin()")
    class TogglePinTests {

        @Test
        @DisplayName("passe isPinned de false à true si l'utilisateur est le propriétaire de la publication")
        void pinsCommentaire_whenPublicationOwner() {
            Commentaire c = makeCommentaire(1, 10, 5);
            c.setPinned(false);
            PublicationDTO pub = makePublication(5, 20);

            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));
            when(publicationClient.getPublicationById(5)).thenReturn(pub);
            when(commentaireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Commentaire result = commentaireService.togglePin(1, 20);

            assertThat(result.isPinned()).isTrue();
            verify(commentaireRepository).save(c);
        }

        @Test
        @DisplayName("passe isPinned de true à false (toggle)")
        void unpinsCommentaire_whenAlreadyPinned() {
            Commentaire c = makeCommentaire(1, 10, 5);
            c.setPinned(true);
            PublicationDTO pub = makePublication(5, 20);

            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));
            when(publicationClient.getPublicationById(5)).thenReturn(pub);
            when(commentaireRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Commentaire result = commentaireService.togglePin(1, 20);

            assertThat(result.isPinned()).isFalse();
        }

        @Test
        @DisplayName("lève UnauthorizedCommentaireException si l'utilisateur n'est pas le propriétaire de la publication")
        void throwsUnauthorizedCommentaireException_whenNotPublicationOwner() {
            Commentaire c = makeCommentaire(1, 10, 5);
            PublicationDTO pub = makePublication(5, 20);

            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));
            when(publicationClient.getPublicationById(5)).thenReturn(pub);

            assertThatThrownBy(() -> commentaireService.togglePin(1, 99))
                    .isInstanceOf(UnauthorizedCommentaireException.class)
                    .hasMessageContaining("Only publication owner can pin");

            verify(commentaireRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève CommentaireNotFoundException si le commentaire est introuvable")
        void throwsCommentaireNotFoundException_whenCommentaireNotFound() {
            when(commentaireRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentaireService.togglePin(99, 20))
                    .isInstanceOf(CommentaireNotFoundException.class)
                    .hasMessageContaining("Commentaire not found: 99");
        }

        @Test
        @DisplayName("lève PublicationValidationException si publicationClient échoue")
        void throwsPublicationValidationException_whenPublicationClientFails() {
            Commentaire c = makeCommentaire(1, 10, 5);
            when(commentaireRepository.findById(1)).thenReturn(Optional.of(c));
            when(publicationClient.getPublicationById(5)).thenThrow(new RuntimeException("service down"));

            assertThatThrownBy(() -> commentaireService.togglePin(1, 20))
                    .isInstanceOf(PublicationValidationException.class);
        }
    }
}