package com.esprit.commentaireservice.repositories;

import com.esprit.commentaireservice.entities.Commentaire;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

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
class CommentaireRepositoryTest {

    @Autowired
    private CommentaireRepository repository;

    private Commentaire saveRoot(Integer userId, Integer publicationId) {
        Commentaire c = new Commentaire();
        c.setUserId(userId);
        c.setPublicationId(publicationId);
        c.setContenue("Contenu test");
        return repository.saveAndFlush(c);
    }

    private Commentaire saveReply(Integer userId, Integer publicationId, Commentaire parent) {
        Commentaire c = new Commentaire();
        c.setUserId(userId);
        c.setPublicationId(publicationId);
        c.setContenue("Réponse test");
        c.setParent(parent);
        return repository.saveAndFlush(c);
    }

    @Nested
    @DisplayName("findByPublicationId()")
    class FindByPublicationIdTests {

        @Test
        @DisplayName("retourne uniquement les commentaires de la publication spécifiée")
        void returnsOnlyCommentairesForPublication() {
            saveRoot(1, 10);
            saveRoot(2, 10);
            saveRoot(3, 20);

            List<Commentaire> result = repository.findByPublicationId(10);

            assertThat(result)
                    .hasSize(2)
                    .allMatch(c -> c.getPublicationId().equals(10));
        }

        @Test
        @DisplayName("retourne liste vide si aucun commentaire pour cette publication")
        void returnsEmptyList_whenNoCommentairesForPublication() {
            List<Commentaire> result = repository.findByPublicationId(999);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByOrderByCreateAtDesc()")
    class FindAllOrderedTests {

        @Test
        @DisplayName("retourne tous les commentaires")
        void returnsAllCommentaires() {
            saveRoot(1, 10);
            saveRoot(2, 20);
            saveRoot(3, 30);

            List<Commentaire> result = repository.findAllByOrderByCreateAtDesc();

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("retourne liste vide si aucun commentaire")
        void returnsEmptyList_whenNoCommentaires() {
            List<Commentaire> result = repository.findAllByOrderByCreateAtDesc();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRootByPublicationIdOrderByPinned()")
    class FindRootByPublicationIdOrderByPinnedTests {

        @Test
        @DisplayName("retourne uniquement les commentaires racines (sans parent)")
        void returnsOnlyRootCommentaires() {
            Commentaire root = saveRoot(1, 10);
            saveReply(2, 10, root);

            List<Commentaire> result = repository.findRootByPublicationIdOrderByPinned(10);

            assertThat(result)
                    .hasSize(1)
                    .allMatch(c -> c.getParent() == null);
        }

        @Test
        @DisplayName("les commentaires épinglés apparaissent en premier")
        void returnsPinnedCommentairesFirst() {
            Commentaire notPinned = saveRoot(1, 10);
            notPinned.setPinned(false);
            repository.saveAndFlush(notPinned);

            Commentaire pinned = saveRoot(2, 10);
            pinned.setPinned(true);
            repository.saveAndFlush(pinned);

            List<Commentaire> result = repository.findRootByPublicationIdOrderByPinned(10);

            assertThat(result)
                    .hasSize(2)
                    .satisfies(list -> {
                        assertThat(list.get(0).isPinned()).isTrue();
                        assertThat(list.get(1).isPinned()).isFalse();
                    });
        }

        @Test
        @DisplayName("ignore les commentaires des autres publications")
        void ignoresCommentairesFromOtherPublications() {
            saveRoot(1, 10);
            saveRoot(2, 20);

            List<Commentaire> result = repository.findRootByPublicationIdOrderByPinned(10);

            assertThat(result)
                    .hasSize(1)
                    .allMatch(c -> c.getPublicationId().equals(10));
        }

        @Test
        @DisplayName("retourne liste vide si aucun commentaire racine pour cette publication")
        void returnsEmptyList_whenNoRootCommentaires() {
            List<Commentaire> result = repository.findRootByPublicationIdOrderByPinned(999);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("CRUD de base")
    class CrudTests {

        @Test
        @DisplayName("saveAndFlush() génère un id automatique")
        void savePersistsCommentaireAndGeneratesId() {
            Commentaire saved = saveRoot(1, 10);

            assertThat(saved.getId())
                    .isNotNull()
                    .isPositive();
        }

        @Test
        @DisplayName("findById() retourne le commentaire sauvegardé")
        void findByIdReturnsPersistedCommentaire() {
            Commentaire saved = saveRoot(1, 10);

            assertThat(repository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("delete() supprime le commentaire")
        void deleteRemovesCommentaire() {
            Commentaire saved = saveRoot(1, 10);
            repository.delete(saved);
            repository.flush();

            assertThat(repository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("createAt est renseignée automatiquement via @PrePersist")
        void createAtIsSetOnPersist() {
            Commentaire saved = saveRoot(1, 10);

            assertThat(saved.getCreateAt()).isNotNull();
        }

        @Test
        @DisplayName("isPinned vaut false par défaut")
        void isPinnedDefaultIsFalse() {
            Commentaire saved = saveRoot(1, 10);

            assertThat(saved.isPinned()).isFalse();
        }
    }
}