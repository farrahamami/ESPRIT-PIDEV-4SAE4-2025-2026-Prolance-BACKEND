package com.esprit.publicationservice.services;

import com.esprit.publicationservice.clients.UserClient;
import com.esprit.publicationservice.dto.UserDTO;
import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.StatutPublication;
import com.esprit.publicationservice.entities.TypePublication;
import com.esprit.publicationservice.repositories.PublicationRepository;
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

/**
 * Tests unitaires de PublicationService.
 * Toutes les dépendances (repository, userClient) sont mockées avec Mockito.
 * Spring n'est PAS démarré → exécution ultra-rapide.
 */
@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private PublicationService publicationService;

    private Publication makePublication(Integer id, Integer userId, StatutPublication statut) {
        Publication p = new Publication();
        p.setId(id);
        p.setUserId(userId);
        p.setTitre("Titre de test");
        p.setContenue("Contenu de test");
        p.setType(TypePublication.ARTICLE);
        p.setStatut(statut);
        p.setImages(new ArrayList<>());
        p.setPdfs(new ArrayList<>());
        p.setSignalements(new ArrayList<>());
        p.setSignalementRaisons(new ArrayList<>());
        return p;
    }

    private UserDTO makeUser(Integer id) {
        UserDTO u = new UserDTO();
        u.setId(id);
        u.setName("Jean");
        u.setLastName("Dupont");
        return u;
    }

    @Nested
    @DisplayName("isUserBlocked()")
    class IsUserBlockedTests {

        @Test
        @DisplayName("retourne true quand l'utilisateur a exactement 3 publications archivées")
        void returnsTrue_whenArchivedCountEqualsThreshold() {
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(3L);

            assertThat(publicationService.isUserBlocked(1)).isTrue();
        }

        @Test
        @DisplayName("retourne true quand l'utilisateur a plus de 3 publications archivées")
        void returnsTrue_whenArchivedCountAboveThreshold() {
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(5L);

            assertThat(publicationService.isUserBlocked(1)).isTrue();
        }

        @Test
        @DisplayName("retourne false quand l'utilisateur a 2 publications archivées")
        void returnsFalse_whenArchivedCountBelowThreshold() {
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(2L);

            assertThat(publicationService.isUserBlocked(1)).isFalse();
        }

        @Test
        @DisplayName("retourne false quand l'utilisateur n'a aucune publication archivée")
        void returnsFalse_whenNoArchivedPublications() {
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);

            assertThat(publicationService.isUserBlocked(1)).isFalse();
        }
    }


    @Nested
    @DisplayName("getArchivedCount()")
    class GetArchivedCountTests {

        @Test
        @DisplayName("retourne le bon nombre de publications archivées")
        void returnsCorrectCount() {
            when(publicationRepository.countArchivedByUserId(7)).thenReturn(2L);

            assertThat(publicationService.getArchivedCount(7)).isEqualTo(2L);
        }
    }


    @Nested
    @DisplayName("getPublicationById()")
    class GetPublicationByIdTests {

        @Test
        @DisplayName("retourne la publication enrichie avec les données user")
        void returnsEnrichedPublication() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(userClient.getUserById(10)).thenReturn(makeUser(10));

            Publication result = publicationService.getPublicationById(1);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getUser()).isNotNull();
            verify(userClient).getUserById(10);
        }

        @Test
        @DisplayName("lève RuntimeException si publication introuvable")
        void throwsRuntimeException_whenNotFound() {
            when(publicationRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicationService.getPublicationById(99))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Publication not found: 99");
        }
    }


    @Nested
    @DisplayName("getAllPublications()")
    class GetAllPublicationsTests {

        @Test
        @DisplayName("retourne uniquement les publications ACTIVE triées par date")
        void returnsOnlyActivePublications() {
            Publication p1 = makePublication(1, 10, StatutPublication.ACTIVE);
            Publication p2 = makePublication(2, 11, StatutPublication.ACTIVE);
            when(publicationRepository.findByStatutOrderByCreateAtDesc(StatutPublication.ACTIVE))
                    .thenReturn(List.of(p1, p2));
            when(userClient.getUserById(anyInt())).thenReturn(new UserDTO());

            List<Publication> result = publicationService.getAllPublications();

            assertThat(result).hasSize(2);
            verify(publicationRepository).findByStatutOrderByCreateAtDesc(StatutPublication.ACTIVE);
        }

        @Test
        @DisplayName("retourne une liste vide si aucune publication active")
        void returnsEmptyList_whenNoActivePublications() {
            when(publicationRepository.findByStatutOrderByCreateAtDesc(StatutPublication.ACTIVE))
                    .thenReturn(List.of());

            List<Publication> result = publicationService.getAllPublications();

            assertThat(result).isEmpty();
        }
    }


    @Nested
    @DisplayName("getPublicationsByUserId()")
    class GetPublicationsByUserIdTests {

        @Test
        @DisplayName("retourne toutes les publications de l'utilisateur")
        void returnsAllUserPublications() {
            Publication p = makePublication(1, 5, StatutPublication.ACTIVE);
            when(publicationRepository.findByUserId(5)).thenReturn(List.of(p));
            when(userClient.getUserById(5)).thenReturn(makeUser(5));

            List<Publication> result = publicationService.getPublicationsByUserId(5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(5);
        }
    }


    @Nested
    @DisplayName("getArchivedByUserId()")
    class GetArchivedByUserIdTests {

        @Test
        @DisplayName("retourne uniquement les publications ARCHIVED de l'utilisateur")
        void returnsOnlyArchivedPublications() {
            Publication archived = makePublication(1, 5, StatutPublication.ARCHIVED);
            when(publicationRepository.findByUserIdAndStatut(5, StatutPublication.ARCHIVED))
                    .thenReturn(List.of(archived));
            when(userClient.getUserById(5)).thenReturn(makeUser(5));

            List<Publication> result = publicationService.getArchivedByUserId(5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatut()).isEqualTo(StatutPublication.ARCHIVED);
        }
    }


    @Nested
    @DisplayName("signalerPublication()")
    class SignalerPublicationTests {

        @Test
        @DisplayName("ajoute le signalement et la raison à la publication")
        void addsSignalementAndRaison() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Publication result = publicationService.signalerPublication(1, 99, "Contenu inapproprié");

            assertThat(result.getSignalements()).containsExactly(99);
            assertThat(result.getSignalementRaisons()).containsExactly("Contenu inapproprié");
            assertThat(result.getStatut()).isEqualTo(StatutPublication.ACTIVE); // < 3, pas archivé
        }

        @Test
        @DisplayName("archive automatiquement la publication au 3ème signalement")
        void archivesPublication_onThirdSignalement() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            p.getSignalements().addAll(List.of(1, 2));         // déjà 2 signalements
            p.getSignalementRaisons().addAll(List.of("r1", "r2"));

            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Publication result = publicationService.signalerPublication(1, 3, "Spam");

            assertThat(result.getStatut()).isEqualTo(StatutPublication.ARCHIVED);
            assertThat(result.getArchivedAt()).isNotNull();
            assertThat(result.getSignalements()).hasSize(3);
        }

        @Test
        @DisplayName("utilise une raison vide si raison est null")
        void usesEmptyRaison_whenRaisonIsNull() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Publication result = publicationService.signalerPublication(1, 99, null);

            assertThat(result.getSignalementRaisons()).containsExactly("");
        }

        @Test
        @DisplayName("lève IllegalStateException si la publication n'est pas ACTIVE")
        void throwsIllegalStateException_whenPublicationNotActive() {
            Publication p = makePublication(1, 10, StatutPublication.ARCHIVED);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> publicationService.signalerPublication(1, 5, "raison"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("n'est pas active");
        }

        @Test
        @DisplayName("lève IllegalStateException si l'utilisateur a déjà signalé")
        void throwsIllegalStateException_whenAlreadySignaled() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            p.getSignalements().add(5); // user 5 a déjà signalé

            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> publicationService.signalerPublication(1, 5, "raison"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("déjà signalé");
        }

        @Test
        @DisplayName("lève RuntimeException si la publication est introuvable")
        void throwsRuntimeException_whenPublicationNotFound() {
            when(publicationRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicationService.signalerPublication(99, 1, "raison"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Publication not found: 99");
        }

        @Test
        @DisplayName("sauvegarde la publication après signalement")
        void savesPublicationAfterSignalement() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            publicationService.signalerPublication(1, 99, "raison");

            verify(publicationRepository).save(p);
        }
    }


    @Nested
    @DisplayName("deletePublication()")
    class DeletePublicationTests {

        @Test
        @DisplayName("supprime la publication si l'utilisateur est le propriétaire")
        void deletesPublication_whenOwner() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));

            publicationService.deletePublication(1, 10);

            verify(publicationRepository).delete(p);
        }

        @Test
        @DisplayName("lève RuntimeException si l'utilisateur n'est pas le propriétaire")
        void throwsRuntimeException_whenNotOwner() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> publicationService.deletePublication(1, 99))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Not authorized");

            verify(publicationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("lève RuntimeException si la publication est introuvable")
        void throwsRuntimeException_whenNotFound() {
            when(publicationRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicationService.deletePublication(99, 10))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Publication not found");
        }
    }


    @Nested
    @DisplayName("adminDeletePublication()")
    class AdminDeletePublicationTests {

        @Test
        @DisplayName("supprime la publication sans vérification de propriétaire")
        void deletesPublicationWithoutOwnerCheck() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));

            publicationService.adminDeletePublication(1);

            verify(publicationRepository).delete(p);
        }

        @Test
        @DisplayName("lève RuntimeException si la publication est introuvable")
        void throwsRuntimeException_whenNotFound() {
            when(publicationRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> publicationService.adminDeletePublication(99))
                    .isInstanceOf(RuntimeException.class);
        }
    }


    @Nested
    @DisplayName("reactiverCompteUser()")
    class ReactiverCompteUserTests {

        @Test
        @DisplayName("supprime les publications archivées et remet les signalements à zéro sur les actives")
        void deletesArchivedAndClearsSignalementsOnActive() {
            Publication archived = makePublication(1, 5, StatutPublication.ARCHIVED);
            Publication active   = makePublication(2, 5, StatutPublication.ACTIVE);
            active.getSignalements().addAll(List.of(1, 2));
            active.getSignalementRaisons().addAll(List.of("raison1", "raison2"));

            when(publicationRepository.findByUserId(5)).thenReturn(List.of(archived, active));

            publicationService.reactiverCompteUser(5);

            verify(publicationRepository).delete(archived);

            assertThat(active.getSignalements()).isEmpty();
            assertThat(active.getSignalementRaisons()).isEmpty();

            verify(publicationRepository).saveAll(List.of(active));
        }

        @Test
        @DisplayName("ne supprime rien si l'utilisateur n'a pas de publications archivées")
        void doesNothing_whenNoArchivedPublications() {
            Publication active = makePublication(1, 5, StatutPublication.ACTIVE);
            when(publicationRepository.findByUserId(5)).thenReturn(List.of(active));

            publicationService.reactiverCompteUser(5);

            verify(publicationRepository, never()).delete(any());
            verify(publicationRepository).saveAll(List.of(active));
        }

        @Test
        @DisplayName("fonctionne correctement si l'utilisateur n'a aucune publication")
        void worksCorrectly_whenNoPublications() {
            when(publicationRepository.findByUserId(5)).thenReturn(List.of());

            publicationService.reactiverCompteUser(5);

            verify(publicationRepository, never()).delete(any());
            verify(publicationRepository).saveAll(List.of());
        }
    }


    @Nested
    @DisplayName("createPublication() — validations métier")
    class CreatePublicationValidationTests {

        @Test
        @DisplayName("lève IllegalArgumentException si le titre est null")
        void throwsIllegalArgumentException_whenTitreIsNull() {
            assertThatThrownBy(() ->
                    publicationService.createPublication(null, "contenu", TypePublication.ARTICLE,
                            1, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le titre est vide")
        void throwsIllegalArgumentException_whenTitreIsBlank() {
            assertThatThrownBy(() ->
                    publicationService.createPublication("   ", "contenu", TypePublication.ARTICLE,
                            1, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est null")
        void throwsIllegalArgumentException_whenContenueIsNull() {
            assertThatThrownBy(() ->
                    publicationService.createPublication("titre", null, TypePublication.ARTICLE,
                            1, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content is required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est vide")
        void throwsIllegalArgumentException_whenContenueIsBlank() {
            assertThatThrownBy(() ->
                    publicationService.createPublication("titre", "   ", TypePublication.ARTICLE,
                            1, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content is required");
        }

        @Test
        @DisplayName("lève RuntimeException si l'utilisateur n'existe pas dans user-service")
        void throwsRuntimeException_whenUserNotFound() {
            when(userClient.getUserById(42)).thenThrow(new RuntimeException("User not found"));

            assertThatThrownBy(() ->
                    publicationService.createPublication("titre", "contenu", TypePublication.ARTICLE,
                            42, null, null, null, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found: 42");
        }

        @Test
        @DisplayName("lève IllegalStateException si l'utilisateur est bloqué")
        void throwsIllegalStateException_whenUserIsBlocked() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(3L);

            assertThatThrownBy(() ->
                    publicationService.createPublication("titre", "contenu", TypePublication.ARTICLE,
                            1, null, null, null, null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("BLOCKED");
        }
    }


    @Nested
    @DisplayName("updatePublication() — autorisation")
    class UpdatePublicationAuthorizationTests {

        @Test
        @DisplayName("lève RuntimeException si l'utilisateur n'est pas le propriétaire")
        void throwsRuntimeException_whenNotOwner() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));

            assertThatThrownBy(() ->
                    publicationService.updatePublication(1, "titre", "contenu",
                            TypePublication.ARTICLE, 99, null, null, null, null, null, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Not authorized");

            verify(publicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève RuntimeException si la publication est introuvable")
        void throwsRuntimeException_whenNotFound() {
            when(publicationRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    publicationService.updatePublication(99, "titre", "contenu",
                            TypePublication.ARTICLE, 1, null, null, null, null, null, null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Publication not found");
        }
    }
}