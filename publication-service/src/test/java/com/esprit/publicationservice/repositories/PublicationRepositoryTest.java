package com.esprit.publicationservice.repositories;

import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.StatutPublication;
import com.esprit.publicationservice.entities.TypePublication;
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
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;NON_KEYWORDS=TYPE,VALUE",
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
class PublicationRepositoryTest {

    @Autowired
    private PublicationRepository repository;

    private Publication save(Integer userId, StatutPublication statut, TypePublication type) {
        Publication p = new Publication();
        p.setUserId(userId);
        p.setTitre("Titre test");
        p.setContenue("Contenu test");
        p.setType(type);
        p.setStatut(statut);
        return repository.saveAndFlush(p);
    }

    private Publication save(Integer userId, StatutPublication statut) {
        return save(userId, statut, TypePublication.ARTICLE);
    }

    @Nested
    @DisplayName("countArchivedByUserId()")
    class CountArchivedByUserIdTests {

        @Test
        @DisplayName("retourne 0 si aucune publication archivée")
        void returnsZero_whenNoArchivedPublications() {
            save(1, StatutPublication.ACTIVE);
            assertThat(repository.countArchivedByUserId(1)).isEqualTo(0L);
        }

        @Test
        @DisplayName("compte uniquement les ARCHIVED")
        void countsOnlyArchivedPublications() {
            save(1, StatutPublication.ARCHIVED);
            save(1, StatutPublication.ARCHIVED);
            save(1, StatutPublication.ACTIVE);
            assertThat(repository.countArchivedByUserId(1)).isEqualTo(2L);
        }

        @Test
        @DisplayName("ignore les ARCHIVED des autres utilisateurs")
        void ignoresOtherUsersArchivedPublications() {
            save(1, StatutPublication.ARCHIVED);
            save(2, StatutPublication.ARCHIVED);
            assertThat(repository.countArchivedByUserId(1)).isEqualTo(1L);
        }

        @Test
        @DisplayName("retourne 3 quand le seuil de blocage est atteint")
        void returnsThree_whenBlockingThresholdReached() {
            save(1, StatutPublication.ARCHIVED);
            save(1, StatutPublication.ARCHIVED);
            save(1, StatutPublication.ARCHIVED);
            assertThat(repository.countArchivedByUserId(1)).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("findByStatutOrderByCreateAtDesc()")
    class FindByStatutOrderByCreateAtDescTests {

        @Test
        @DisplayName("retourne uniquement les ACTIVE")
        void returnsOnlyActivePublications() {
            save(1, StatutPublication.ACTIVE);
            save(2, StatutPublication.ARCHIVED);
            save(3, StatutPublication.ACTIVE);
            List<Publication> result = repository.findByStatutOrderByCreateAtDesc(StatutPublication.ACTIVE);
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(p -> p.getStatut() == StatutPublication.ACTIVE);
        }

        @Test
        @DisplayName("retourne liste vide si aucune ACTIVE")
        void returnsEmptyList_whenNoActivePublications() {
            save(1, StatutPublication.ARCHIVED);
            assertThat(repository.findByStatutOrderByCreateAtDesc(StatutPublication.ACTIVE)).isEmpty();
        }

        @Test
        @DisplayName("retourne uniquement les ARCHIVED")
        void returnsOnlyArchivedPublications() {
            save(1, StatutPublication.ACTIVE);
            save(2, StatutPublication.ARCHIVED);
            List<Publication> result = repository.findByStatutOrderByCreateAtDesc(StatutPublication.ARCHIVED);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatut()).isEqualTo(StatutPublication.ARCHIVED);
        }
    }

    @Nested
    @DisplayName("findByTypeAndStatutOrderByCreateAtDesc()")
    class FindByTypeAndStatutTests {

        @Test
        @DisplayName("retourne publications du type et statut spécifiés")
        void returnsPublicationsMatchingTypeAndStatut() {
            save(1, StatutPublication.ACTIVE,   TypePublication.ARTICLE);
            save(2, StatutPublication.ACTIVE,   TypePublication.QUESTION);
            save(3, StatutPublication.ARCHIVED, TypePublication.ARTICLE);
            List<Publication> result = repository.findByTypeAndStatutOrderByCreateAtDesc(
                    TypePublication.ARTICLE, StatutPublication.ACTIVE);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(TypePublication.ARTICLE);
        }

        @Test
        @DisplayName("retourne liste vide si aucune correspondance")
        void returnsEmptyList_whenNoMatch() {
            save(1, StatutPublication.ARCHIVED, TypePublication.ARTICLE);
            assertThat(repository.findByTypeAndStatutOrderByCreateAtDesc(
                    TypePublication.ARTICLE, StatutPublication.ACTIVE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId()")
    class FindByUserIdTests {

        @Test
        @DisplayName("retourne toutes les publications d'un utilisateur")
        void returnsAllPublicationsForUser() {
            save(1, StatutPublication.ACTIVE);
            save(1, StatutPublication.ARCHIVED);
            save(2, StatutPublication.ACTIVE);
            List<Publication> result = repository.findByUserId(1);
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(p -> p.getUserId().equals(1));
        }

        @Test
        @DisplayName("retourne liste vide si utilisateur sans publication")
        void returnsEmptyList_whenUserHasNoPublications() {
            assertThat(repository.findByUserId(999)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndStatut()")
    class FindByUserIdAndStatutTests {

        @Test
        @DisplayName("retourne uniquement les ARCHIVED de l'utilisateur")
        void returnsOnlyArchivedPublicationsForUser() {
            save(1, StatutPublication.ARCHIVED);
            save(1, StatutPublication.ACTIVE);
            save(2, StatutPublication.ARCHIVED);
            List<Publication> result = repository.findByUserIdAndStatut(1, StatutPublication.ARCHIVED);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(1);
        }

        @Test
        @DisplayName("retourne liste vide si aucune ARCHIVED pour cet utilisateur")
        void returnsEmptyList_whenNoArchivedPublicationsForUser() {
            save(1, StatutPublication.ACTIVE);
            assertThat(repository.findByUserIdAndStatut(1, StatutPublication.ARCHIVED)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByOrderByCreateAtDesc()")
    class FindAllOrderedTests {

        @Test
        @DisplayName("retourne toutes les publications")
        void returnsAllPublications() {
            save(1, StatutPublication.ACTIVE);
            save(2, StatutPublication.ARCHIVED);
            save(3, StatutPublication.ACTIVE);
            assertThat(repository.findAllByOrderByCreateAtDesc()).hasSize(3);
        }

        @Test
        @DisplayName("retourne liste vide si aucune publication")
        void returnsEmptyList_whenNoPublications() {
            assertThat(repository.findAllByOrderByCreateAtDesc()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CRUD de base")
    class CrudTests {

        @Test
        @DisplayName("saveAndFlush() génère un id")
        void savePersistsPublicationAndGeneratesId() {
            Publication p = save(1, StatutPublication.ACTIVE);
            assertThat(p.getId()).isNotNull().isPositive();
        }

        @Test
        @DisplayName("findById() retourne la publication sauvegardée")
        void findByIdReturnsPersistedPublication() {
            Publication saved = save(1, StatutPublication.ACTIVE);
            assertThat(repository.findById(saved.getId())).isPresent();
        }

        @Test
        @DisplayName("delete() supprime la publication")
        void deleteRemovesPublication() {
            Publication saved = save(1, StatutPublication.ACTIVE);
            repository.delete(saved);
            repository.flush();
            assertThat(repository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("createAt est renseignée automatiquement")
        void createAtIsSetOnPersist() {
            Publication saved = save(1, StatutPublication.ACTIVE);
            assertThat(saved.getCreateAt()).isNotNull();
        }
    }
}