package com.esprit.reactionservice.repositories;

import com.esprit.reactionservice.entities.Reaction;
import com.esprit.reactionservice.entities.TypeReaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class ReactionRepositoryTest {

    @Autowired
    private ReactionRepository repository;

    private Reaction save(Integer userId, Integer publicationId, TypeReaction type) {
        Reaction r = new Reaction();
        r.setUserId(userId);
        r.setPublicationId(publicationId);
        r.setType(type);
        return repository.saveAndFlush(r);
    }

    @Nested
    @DisplayName("findByPublicationId()")
    class FindByPublicationIdTests {

        @Test
        @DisplayName("retourne toutes les réactions d'une publication")
        void returnsAllReactionsForPublication() {
            save(1, 10, TypeReaction.LIKE);
            save(2, 10, TypeReaction.DISLIKE);
            save(3, 20, TypeReaction.HEART);

            List<Reaction> result = repository.findByPublicationId(10);

            assertThat(result)
                    .hasSize(2)
                    .allMatch(r -> r.getPublicationId().equals(10));
        }

        @Test
        @DisplayName("retourne liste vide si aucune réaction pour cette publication")
        void returnsEmptyList_whenNoReactionsForPublication() {
            List<Reaction> result = repository.findByPublicationId(999);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("retourne les réactions de types différents pour la même publication")
        void returnsMixedTypes_forSamePublication() {
            save(1, 10, TypeReaction.LIKE);
            save(2, 10, TypeReaction.HEART);
            save(3, 10, TypeReaction.DISLIKE);

            List<Reaction> result = repository.findByPublicationId(10);

            assertThat(result)
                    .hasSize(3)
                    .extracting(Reaction::getType)
                    .containsExactlyInAnyOrder(TypeReaction.LIKE, TypeReaction.HEART, TypeReaction.DISLIKE);
        }
    }

    @Nested
    @DisplayName("findByPublicationIdAndUserId()")
    class FindByPublicationIdAndUserIdTests {

        @Test
        @DisplayName("retourne la réaction de l'utilisateur sur la publication")
        void returnsReaction_whenExists() {
            save(1, 10, TypeReaction.LIKE);

            Optional<Reaction> result = repository.findByPublicationIdAndUserId(10, 1);

            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(r -> {
                        assertThat(r.getUserId()).isEqualTo(1);
                        assertThat(r.getPublicationId()).isEqualTo(10);
                        assertThat(r.getType()).isEqualTo(TypeReaction.LIKE);
                    });
        }

        @Test
        @DisplayName("retourne Optional.empty() si l'utilisateur n'a pas réagi")
        void returnsEmpty_whenNoReaction() {
            save(1, 10, TypeReaction.LIKE);

            Optional<Reaction> result = repository.findByPublicationIdAndUserId(10, 99);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("retourne Optional.empty() si la publication n'existe pas")
        void returnsEmpty_whenPublicationNotFound() {
            Optional<Reaction> result = repository.findByPublicationIdAndUserId(999, 1);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ne confond pas les couples (userId, publicationId) de publications différentes")
        void doesNotConfuse_differentPublications() {
            save(1, 10, TypeReaction.LIKE);
            save(1, 20, TypeReaction.DISLIKE);

            Optional<Reaction> result = repository.findByPublicationIdAndUserId(10, 1);

            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(r -> assertThat(r.getType()).isEqualTo(TypeReaction.LIKE));
        }
    }

    @Nested
    @DisplayName("countByPublicationIdAndType()")
    class CountByPublicationIdAndTypeTests {

        @Test
        @DisplayName("retourne le bon nombre de LIKE pour une publication")
        void returnsCorrectLikeCount() {
            save(1, 10, TypeReaction.LIKE);
            save(2, 10, TypeReaction.LIKE);
            save(3, 10, TypeReaction.DISLIKE);

            long likeCount = repository.countByPublicationIdAndType(10, TypeReaction.LIKE);
            assertThat(likeCount).isEqualTo(2);
        }

        @Test
        @DisplayName("retourne 0 si aucune réaction du type donné")
        void returnsZero_whenNoReactionOfType() {
            save(1, 10, TypeReaction.LIKE);

            long heartCount = repository.countByPublicationIdAndType(10, TypeReaction.HEART);
            assertThat(heartCount).isZero();
        }

        @Test
        @DisplayName("ignore les réactions des autres publications")
        void ignoresOtherPublications() {
            save(1, 10, TypeReaction.LIKE);
            save(2, 20, TypeReaction.LIKE);

            long likeCount = repository.countByPublicationIdAndType(10, TypeReaction.LIKE);
            assertThat(likeCount).isEqualTo(1);
        }

        @Test
        @DisplayName("retourne 0 si la publication n'existe pas")
        void returnsZero_whenPublicationNotFound() {
            long likeCount = repository.countByPublicationIdAndType(999, TypeReaction.LIKE);
            assertThat(likeCount).isZero();
        }
    }

    @Nested
    @DisplayName("CRUD de base")
    class CrudTests {

        @Test
        @DisplayName("saveAndFlush() génère un id automatique")
        void savePersistsReactionAndGeneratesId() {
            Reaction saved = save(1, 10, TypeReaction.LIKE);

            assertThat(saved.getId())
                    .isNotNull()
                    .isPositive();
        }

        @Test
        @DisplayName("findById() retourne la réaction sauvegardée")
        void findByIdReturnsPersistedReaction() {
            Reaction saved = save(1, 10, TypeReaction.LIKE);

            assertThat(repository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("delete() supprime la réaction")
        void deleteRemovesReaction() {
            Reaction saved = save(1, 10, TypeReaction.LIKE);
            repository.delete(saved);
            repository.flush();

            assertThat(repository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("createdAt est renseignée automatiquement via @PrePersist")
        void createdAtIsSetOnPersist() {
            Reaction saved = save(1, 10, TypeReaction.LIKE);

            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("save() permet de changer le type d'une réaction existante")
        void updateTypeOfExistingReaction() {
            Reaction saved = save(1, 10, TypeReaction.LIKE);
            saved.setType(TypeReaction.HEART);
            repository.saveAndFlush(saved);

            Reaction updated = repository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getType()).isEqualTo(TypeReaction.HEART);
        }
    }
}