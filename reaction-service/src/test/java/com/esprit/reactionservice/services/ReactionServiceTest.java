package com.esprit.reactionservice.services;

import com.esprit.reactionservice.clients.PublicationClient;
import com.esprit.reactionservice.clients.UserClient;
import com.esprit.reactionservice.dto.ReactionSummaryDTO;
import com.esprit.reactionservice.dto.UserDTO;
import com.esprit.reactionservice.entities.Reaction;
import com.esprit.reactionservice.entities.TypeReaction;
import com.esprit.reactionservice.repositories.ReactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private ReactionRepository reactionRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private PublicationClient publicationClient;

    @InjectMocks
    private ReactionService reactionService;

    private Reaction makeReaction(Integer id, Integer userId, Integer publicationId, TypeReaction type) {
        Reaction r = new Reaction();
        r.setId(id);
        r.setUserId(userId);
        r.setPublicationId(publicationId);
        r.setType(type);
        return r;
    }

    private UserDTO makeUser(Integer id) {
        UserDTO u = new UserDTO();
        u.setId(id);
        u.setName("Jean");
        u.setLastName("Dupont");
        return u;
    }

    @Nested
    @DisplayName("toggleReaction() — nouvelle réaction")
    class ToggleReaction_NewTests {

        @Test
        @DisplayName("crée une nouvelle réaction si aucune n'existe pour cet utilisateur/publication")
        void createsNewReaction_whenNoneExists() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(10)).thenReturn(Map.of("id", 10));
            when(reactionRepository.findByPublicationIdAndUserId(10, 1)).thenReturn(Optional.empty());
            when(reactionRepository.save(any())).thenAnswer(inv -> {
                Reaction r = inv.getArgument(0);
                r.setId(1);
                return r;
            });

            Optional<Reaction> result = reactionService.toggleReaction(10, 1, TypeReaction.LIKE);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(TypeReaction.LIKE);
            assertThat(result.get().getUserId()).isEqualTo(1);
            assertThat(result.get().getPublicationId()).isEqualTo(10);
            verify(reactionRepository).save(any());
        }

        @Test
        @DisplayName("crée une réaction DISLIKE si aucune n'existe")
        void createsDislike_whenNoneExists() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(10)).thenReturn(Map.of("id", 10));
            when(reactionRepository.findByPublicationIdAndUserId(10, 1)).thenReturn(Optional.empty());
            when(reactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Optional<Reaction> result = reactionService.toggleReaction(10, 1, TypeReaction.DISLIKE);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(TypeReaction.DISLIKE);
        }

        @Test
        @DisplayName("crée une réaction HEART si aucune n'existe")
        void createsHeart_whenNoneExists() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(10)).thenReturn(Map.of("id", 10));
            when(reactionRepository.findByPublicationIdAndUserId(10, 1)).thenReturn(Optional.empty());
            when(reactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Optional<Reaction> result = reactionService.toggleReaction(10, 1, TypeReaction.HEART);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(TypeReaction.HEART);
        }
    }

    @Nested
    @DisplayName("toggleReaction() — suppression (même type)")
    class ToggleReaction_DeleteTests {

        @Test
        @DisplayName("supprime la réaction et retourne Optional.empty() si même type déjà présent")
        void deletesReaction_whenSameTypeExists() {
            Reaction existing = makeReaction(1, 1, 10, TypeReaction.LIKE);
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(10)).thenReturn(Map.of("id", 10));
            when(reactionRepository.findByPublicationIdAndUserId(10, 1)).thenReturn(Optional.of(existing));

            Optional<Reaction> result = reactionService.toggleReaction(10, 1, TypeReaction.LIKE);

            assertThat(result).isEmpty();
            verify(reactionRepository).delete(existing);
            verify(reactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("supprime un DISLIKE existant si le même type est envoyé")
        void deletesDislike_whenSameTypeExists() {
            Reaction existing = makeReaction(1, 1, 10, TypeReaction.DISLIKE);
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(10)).thenReturn(Map.of("id", 10));
            when(reactionRepository.findByPublicationIdAndUserId(10, 1)).thenReturn(Optional.of(existing));

            Optional<Reaction> result = reactionService.toggleReaction(10, 1, TypeReaction.DISLIKE);

            assertThat(result).isEmpty();
            verify(reactionRepository).delete(existing);
        }
    }

    @Nested
    @DisplayName("toggleReaction() — changement de type")
    class ToggleReaction_ChangeTypeTests {

        @Test
        @DisplayName("change le type de LIKE vers DISLIKE si une réaction de type différent existe")
        void changesType_fromLikeToDislike() {
            Reaction existing = makeReaction(1, 1, 10, TypeReaction.LIKE);
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(10)).thenReturn(Map.of("id", 10));
            when(reactionRepository.findByPublicationIdAndUserId(10, 1)).thenReturn(Optional.of(existing));
            when(reactionRepository.save(existing)).thenReturn(existing);

            Optional<Reaction> result = reactionService.toggleReaction(10, 1, TypeReaction.DISLIKE);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(TypeReaction.DISLIKE);
            verify(reactionRepository).save(existing);
            verify(reactionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("change le type de DISLIKE vers HEART")
        void changesType_fromDislikeToHeart() {
            Reaction existing = makeReaction(1, 1, 10, TypeReaction.DISLIKE);
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(10)).thenReturn(Map.of("id", 10));
            when(reactionRepository.findByPublicationIdAndUserId(10, 1)).thenReturn(Optional.of(existing));
            when(reactionRepository.save(existing)).thenReturn(existing);

            Optional<Reaction> result = reactionService.toggleReaction(10, 1, TypeReaction.HEART);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(TypeReaction.HEART);
        }
    }

    @Nested
    @DisplayName("toggleReaction() — validation clients")
    class ToggleReaction_ValidationTests {

        @Test
        @DisplayName("lève UserNotFoundException si l'utilisateur n'existe pas dans user-service")
        void throwsUserNotFoundException_whenUserNotFound() {
            when(userClient.getUserById(99)).thenThrow(new RuntimeException("feign error"));

            assertThatThrownBy(() -> reactionService.toggleReaction(10, 99, TypeReaction.LIKE))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found: 99");

            verify(reactionRepository, never()).findByPublicationIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("lève PublicationNotFoundException si la publication n'existe pas dans publication-service")
        void throwsPublicationNotFoundException_whenPublicationNotFound() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationClient.getPublicationById(99)).thenThrow(new RuntimeException("feign error"));

            assertThatThrownBy(() -> reactionService.toggleReaction(99, 1, TypeReaction.LIKE))
                    .isInstanceOf(PublicationNotFoundException.class)
                    .hasMessageContaining("Publication not found: 99");

            verify(reactionRepository, never()).findByPublicationIdAndUserId(any(), any());
        }
    }

    @Nested
    @DisplayName("getSummary()")
    class GetSummaryTests {

        @Test
        @DisplayName("retourne le bon comptage de LIKE, DISLIKE, HEART")
        void returnsCorrectCounts() {
            List<Reaction> reactions = List.of(
                    makeReaction(1, 1, 10, TypeReaction.LIKE),
                    makeReaction(2, 2, 10, TypeReaction.LIKE),
                    makeReaction(3, 3, 10, TypeReaction.DISLIKE),
                    makeReaction(4, 4, 10, TypeReaction.HEART)
            );
            when(reactionRepository.findByPublicationId(10)).thenReturn(reactions);
            when(userClient.getUserById(anyInt())).thenReturn(makeUser(1));

            ReactionSummaryDTO result = reactionService.getSummary(10, 1);

            assertThat(result.getLIKE()).isEqualTo(2);
            assertThat(result.getDISLIKE()).isEqualTo(1);
            assertThat(result.getHEART()).isEqualTo(1);
        }

        @Test
        @DisplayName("retourne la réaction de l'utilisateur courant (userReaction)")
        void returnsCurrentUserReaction() {
            List<Reaction> reactions = List.of(
                    makeReaction(1, 5, 10, TypeReaction.HEART),
                    makeReaction(2, 6, 10, TypeReaction.LIKE)
            );
            when(reactionRepository.findByPublicationId(10)).thenReturn(reactions);
            when(userClient.getUserById(anyInt())).thenReturn(makeUser(1));

            ReactionSummaryDTO result = reactionService.getSummary(10, 5);

            assertThat(result.getUserReaction()).isEqualTo(TypeReaction.HEART);
        }

        @Test
        @DisplayName("retourne userReaction null si l'utilisateur n'a pas réagi")
        void returnsNullUserReaction_whenUserHasNoReaction() {
            List<Reaction> reactions = List.of(
                    makeReaction(1, 2, 10, TypeReaction.LIKE)
            );
            when(reactionRepository.findByPublicationId(10)).thenReturn(reactions);
            when(userClient.getUserById(anyInt())).thenReturn(makeUser(1));

            ReactionSummaryDTO result = reactionService.getSummary(10, 99);

            assertThat(result.getUserReaction()).isNull();
        }

        @Test
        @DisplayName("retourne des compteurs à zéro et liste vide si aucune réaction")
        void returnsZeroCountsAndEmptyReactors_whenNoReactions() {
            when(reactionRepository.findByPublicationId(10)).thenReturn(List.of());

            ReactionSummaryDTO result = reactionService.getSummary(10, 1);

            assertThat(result.getLIKE()).isZero();
            assertThat(result.getDISLIKE()).isZero();
            assertThat(result.getHEART()).isZero();
            assertThat(result.getUserReaction()).isNull();
            assertThat(result.getReactors()).isEmpty();
        }

        @Test
        @DisplayName("la liste reactors contient un ReactorDTO par réaction")
        void returnsOneReactorPerReaction() {
            List<Reaction> reactions = List.of(
                    makeReaction(1, 1, 10, TypeReaction.LIKE),
                    makeReaction(2, 2, 10, TypeReaction.DISLIKE)
            );
            when(reactionRepository.findByPublicationId(10)).thenReturn(reactions);
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(userClient.getUserById(2)).thenReturn(makeUser(2));

            ReactionSummaryDTO result = reactionService.getSummary(10, 1);

            assertThat(result.getReactors()).hasSize(2);
        }

        @Test
        @DisplayName("utilise 'User {id}' comme nom si userClient échoue (enrichissement silencieux)")
        void usesFallbackName_whenUserClientFails() {
            List<Reaction> reactions = List.of(
                    makeReaction(1, 7, 10, TypeReaction.LIKE)
            );
            when(reactionRepository.findByPublicationId(10)).thenReturn(reactions);
            when(userClient.getUserById(7)).thenThrow(new RuntimeException("service down"));

            ReactionSummaryDTO result = reactionService.getSummary(10, 99);

            assertThat(result.getReactors()).hasSize(1);
            assertThat(result.getReactors().get(0).getUserName()).isEqualTo("User 7");
        }

        @Test
        @DisplayName("chaque ReactorDTO contient le bon userId et le bon type de réaction")
        void reactorDtoHasCorrectUserIdAndType() {
            List<Reaction> reactions = List.of(
                    makeReaction(1, 3, 10, TypeReaction.HEART)
            );
            when(reactionRepository.findByPublicationId(10)).thenReturn(reactions);
            when(userClient.getUserById(3)).thenReturn(makeUser(3));

            ReactionSummaryDTO result = reactionService.getSummary(10, 99);

            assertThat(result.getReactors().get(0).getUserId()).isEqualTo(3);
            assertThat(result.getReactors().get(0).getType()).isEqualTo(TypeReaction.HEART);
        }
    }
}