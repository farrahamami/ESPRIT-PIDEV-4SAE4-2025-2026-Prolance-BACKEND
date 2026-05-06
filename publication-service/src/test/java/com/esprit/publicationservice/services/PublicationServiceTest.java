package com.esprit.publicationservice.services;

import com.esprit.publicationservice.clients.UserClient;
import com.esprit.publicationservice.dto.CreatePublicationRequest;
import com.esprit.publicationservice.dto.UpdatePublicationRequest;
import com.esprit.publicationservice.dto.UserBlockDTO;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicationServiceTest {

    @Mock
    private PublicationRepository publicationRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private PublicationService publicationService;

    // ========== HELPERS ==========

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

    private CreatePublicationRequest createRequest(String titre, String contenue, TypePublication type,
                                                   Integer userId, List<MultipartFile> images,
                                                   List<MultipartFile> pdfs, String titleColor,
                                                   String contentColor, String titleFontSize) {
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setTitre(titre);
        request.setContenue(contenue);
        request.setType(type);
        request.setUserId(userId);
        request.setImages(images);
        request.setPdfs(pdfs);
        request.setTitleColor(titleColor);
        request.setContentColor(contentColor);
        request.setTitleFontSize(titleFontSize);
        return request;
    }

    private UpdatePublicationRequest updateRequest(Integer id, String titre, String contenue, TypePublication type,
                                                   Integer userId, List<MultipartFile> newImages,
                                                   List<String> imagesToKeep, List<MultipartFile> newPdfs,
                                                   List<String> pdfsToKeep, String titleColor,
                                                   String contentColor, String titleFontSize) {
        UpdatePublicationRequest request = new UpdatePublicationRequest();
        request.setId(id);
        request.setTitre(titre);
        request.setContenue(contenue);
        request.setType(type);
        request.setUserId(userId);
        request.setNewImages(newImages);
        request.setImagesToKeep(imagesToKeep);
        request.setNewPdfs(newPdfs);
        request.setPdfsToKeep(pdfsToKeep);
        request.setTitleColor(titleColor);
        request.setContentColor(contentColor);
        request.setTitleFontSize(titleFontSize);
        return request;
    }

    // ========== TESTS ==========

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

        @Test
        @DisplayName("retourne la publication même si userClient échoue (enrichWithUser silencieux)")
        void returnsPublication_whenUserClientFails() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(userClient.getUserById(10)).thenThrow(new RuntimeException("User service down"));

            Publication result = publicationService.getPublicationById(1);

            assertThat(result).isNotNull();
            assertThat(result.getUser()).isNull();
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
    @DisplayName("getAllPublicationsAdmin()")
    class GetAllPublicationsAdminTests {

        @Test
        @DisplayName("retourne toutes les publications (actives et archivées)")
        void returnsAllPublicationsRegardlessOfStatus() {
            Publication active   = makePublication(1, 10, StatutPublication.ACTIVE);
            Publication archived = makePublication(2, 11, StatutPublication.ARCHIVED);
            when(publicationRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of(active, archived));
            when(userClient.getUserById(anyInt())).thenReturn(new UserDTO());

            List<Publication> result = publicationService.getAllPublicationsAdmin();

            assertThat(result).hasSize(2);
            verify(publicationRepository).findAllByOrderByCreateAtDesc();
        }

        @Test
        @DisplayName("retourne liste vide si aucune publication en base")
        void returnsEmptyList_whenNoPublications() {
            when(publicationRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of());

            List<Publication> result = publicationService.getAllPublicationsAdmin();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPublicationsByType()")
    class GetPublicationsByTypeTests {

        @Test
        @DisplayName("retourne les publications du type ARTICLE")
        void returnsPublicationsOfGivenType() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            p.setType(TypePublication.ARTICLE);
            when(publicationRepository.findByTypeAndStatutOrderByCreateAtDesc(TypePublication.ARTICLE, StatutPublication.ACTIVE))
                    .thenReturn(List.of(p));
            when(userClient.getUserById(10)).thenReturn(makeUser(10));

            List<Publication> result = publicationService.getPublicationsByType(TypePublication.ARTICLE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(TypePublication.ARTICLE);
        }

        @Test
        @DisplayName("retourne liste vide si aucune publication du type donné")
        void returnsEmptyList_whenNoPublicationOfType() {
            when(publicationRepository.findByTypeAndStatutOrderByCreateAtDesc(TypePublication.REVIEW, StatutPublication.ACTIVE))
                    .thenReturn(List.of());

            List<Publication> result = publicationService.getPublicationsByType(TypePublication.REVIEW);

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
    @DisplayName("getAllUsersBlockStatus()")
    class GetAllUsersBlockStatusTests {

        @Test
        @DisplayName("retourne les DTOs triés par archivedCount décroissant avec données user")
        void returnsSortedBlockStatusWithUserData() {
            Publication a1     = makePublication(1, 10, StatutPublication.ARCHIVED);
            Publication a2     = makePublication(2, 10, StatutPublication.ARCHIVED);
            Publication a3     = makePublication(3, 11, StatutPublication.ARCHIVED);
            Publication active = makePublication(4, 10, StatutPublication.ACTIVE);

            when(publicationRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of(a1, a2, a3, active));
            when(userClient.getUserById(10)).thenReturn(makeUser(10));
            when(userClient.getUserById(11)).thenReturn(makeUser(11));

            List<UserBlockDTO> result = publicationService.getAllUsersBlockStatus();

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getUserId()).isEqualTo(10);
            assertThat(result.get(0).getArchivedCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("retourne liste vide si aucune publication archivée")
        void returnsEmptyList_whenNoArchivedPublications() {
            Publication active = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of(active));

            List<UserBlockDTO> result = publicationService.getAllUsersBlockStatus();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("name/lastName restent vides si userClient lève une exception")
        void handlesUserClientFailureGracefully() {
            Publication archived = makePublication(1, 10, StatutPublication.ARCHIVED);
            when(publicationRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of(archived));
            when(userClient.getUserById(10)).thenThrow(new RuntimeException("service down"));

            List<UserBlockDTO> result = publicationService.getAllUsersBlockStatus();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEmpty();
            assertThat(result.get(0).getLastName()).isEmpty();
        }

        @Test
        @DisplayName("retourne liste vide si aucune publication en base")
        void returnsEmptyList_whenNoPublications() {
            when(publicationRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of());

            List<UserBlockDTO> result = publicationService.getAllUsersBlockStatus();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("gère un user dont getName() retourne null")
        void handlesNullUserNameGracefully() {
            Publication archived = makePublication(1, 10, StatutPublication.ARCHIVED);
            when(publicationRepository.findAllByOrderByCreateAtDesc()).thenReturn(List.of(archived));
            UserDTO userWithNullName = new UserDTO();
            userWithNullName.setId(10);
            userWithNullName.setName(null);
            userWithNullName.setLastName(null);
            when(userClient.getUserById(10)).thenReturn(userWithNullName);

            List<UserBlockDTO> result = publicationService.getAllUsersBlockStatus();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEmpty();
            assertThat(result.get(0).getLastName()).isEmpty();
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
            assertThat(result.getStatut()).isEqualTo(StatutPublication.ACTIVE);
        }

        @Test
        @DisplayName("archive automatiquement la publication au 3ème signalement")
        void archivesPublication_onThirdSignalement() {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            p.getSignalements().addAll(List.of(1, 2));
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
            p.getSignalements().add(5);
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
    @DisplayName("createPublication() — chemin heureux")
    class CreatePublicationHappyPathTests {

        @Test
        @DisplayName("crée et retourne une publication ACTIVE sans images ni PDFs")
        void createsPublicationSuccessfully() throws IOException {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);
            when(publicationRepository.save(any())).thenAnswer(inv -> {
                Publication saved = inv.getArgument(0);
                saved.setId(42);
                return saved;
            });

            CreatePublicationRequest request = createRequest(
                    "Titre", "Contenu", TypePublication.ARTICLE,
                    1, null, null,
                    "#ffffff", "#000000", "1rem"
            );

            Publication result = publicationService.createPublication(request);

            assertThat(result).isNotNull();
            assertThat(result.getStatut()).isEqualTo(StatutPublication.ACTIVE);
            assertThat(result.getTitre()).isEqualTo("Titre");
            assertThat(result.getTitleColor()).isEqualTo("#ffffff");
            assertThat(result.getContentColor()).isEqualTo("#000000");
            assertThat(result.getTitleFontSize()).isEqualTo("1rem");
            verify(publicationRepository).save(any());
        }

        @Test
        @DisplayName("crée une publication de type QUESTION sans images ni PDFs")
        void createsQuestionPublicationSuccessfully() throws IOException {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreatePublicationRequest request = createRequest(
                    "Titre question", "Contenu question", TypePublication.QUESTION,
                    1, null, null,
                    null, null, null
            );

            Publication result = publicationService.createPublication(request);

            assertThat(result.getType()).isEqualTo(TypePublication.QUESTION);
        }

        @Test
        @DisplayName("crée une publication QUESTION avec des listes vides (non null)")
        void createsQuestionWithEmptyFileLists() throws IOException {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Listes non-null mais ne contenant que des fichiers vides
            MultipartFile emptyFile = new MockMultipartFile("f", new byte[0]);
            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.QUESTION,
                    1, List.of(emptyFile), List.of(emptyFile),
                    null, null, null
            );

            Publication result = publicationService.createPublication(request);

            assertThat(result.getType()).isEqualTo(TypePublication.QUESTION);
        }

        @Test
        @DisplayName("lève IllegalArgumentException si type QUESTION avec images non vides")
        void throwsIllegalArgumentException_whenQuestionWithImages() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);

            MultipartFile image = new MockMultipartFile("img", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.QUESTION,
                    1, List.of(image), null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No images/PDFs allowed for QUESTION type");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si type QUESTION avec PDFs non vides")
        void throwsIllegalArgumentException_whenQuestionWithPdfs() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);

            MultipartFile pdf = new MockMultipartFile("pdf", "test.pdf", "application/pdf", new byte[]{1, 2, 3});

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.QUESTION,
                    1, null, List.of(pdf), null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No images/PDFs allowed for QUESTION type");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si l'image uploadée n'est pas du type image")
        void throwsIllegalArgumentException_whenImageHasWrongContentType() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);

            MultipartFile wrongFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.ARTICLE,
                    1, List.of(wrongFile), null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only images accepted");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le PDF uploadé n'est pas application/pdf")
        void throwsIllegalArgumentException_whenPdfHasWrongContentType() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);

            MultipartFile wrongFile = new MockMultipartFile("file", "img.jpg", "image/jpeg", new byte[]{1, 2, 3});

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.ARTICLE,
                    1, null, List.of(wrongFile), null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only PDFs accepted");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si l'image a un contentType null")
        void throwsIllegalArgumentException_whenImageContentTypeIsNull() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);

            MultipartFile noContentType = new MockMultipartFile("file", "file.bin", null, new byte[]{1, 2, 3});

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.ARTICLE,
                    1, List.of(noContentType), null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only images accepted");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le PDF a un contentType null")
        void throwsIllegalArgumentException_whenPdfContentTypeIsNull() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(0L);

            MultipartFile noContentType = new MockMultipartFile("file", "file.bin", null, new byte[]{1, 2, 3});

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.ARTICLE,
                    1, null, List.of(noContentType), null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only PDFs accepted");
        }
    }

    @Nested
    @DisplayName("createPublication() — validations métier")
    class CreatePublicationValidationTests {

        @Test
        @DisplayName("lève IllegalArgumentException si le titre est null")
        void throwsIllegalArgumentException_whenTitreIsNull() {
            CreatePublicationRequest request = createRequest(
                    null, "contenu", TypePublication.ARTICLE,
                    1, null, null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le titre est vide")
        void throwsIllegalArgumentException_whenTitreIsBlank() {
            CreatePublicationRequest request = createRequest(
                    "   ", "contenu", TypePublication.ARTICLE,
                    1, null, null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Title is required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est null")
        void throwsIllegalArgumentException_whenContenueIsNull() {
            CreatePublicationRequest request = createRequest(
                    "titre", null, TypePublication.ARTICLE,
                    1, null, null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content is required");
        }

        @Test
        @DisplayName("lève IllegalArgumentException si le contenu est vide")
        void throwsIllegalArgumentException_whenContenueIsBlank() {
            CreatePublicationRequest request = createRequest(
                    "titre", "   ", TypePublication.ARTICLE,
                    1, null, null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content is required");
        }

        @Test
        @DisplayName("lève RuntimeException si l'utilisateur n'existe pas dans user-service")
        void throwsRuntimeException_whenUserNotFound() {
            when(userClient.getUserById(42)).thenThrow(new RuntimeException("User not found"));

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.ARTICLE,
                    42, null, null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found: 42");
        }

        @Test
        @DisplayName("lève IllegalStateException si l'utilisateur est bloqué")
        void throwsIllegalStateException_whenUserIsBlocked() {
            when(userClient.getUserById(1)).thenReturn(makeUser(1));
            when(publicationRepository.countArchivedByUserId(1)).thenReturn(3L);

            CreatePublicationRequest request = createRequest(
                    "titre", "contenu", TypePublication.ARTICLE,
                    1, null, null, null, null, null
            );

            assertThatThrownBy(() -> publicationService.createPublication(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("BLOCKED");
        }
    }

    @Nested
    @DisplayName("updatePublication() — chemin heureux")
    class UpdatePublicationHappyPathTests {

        @Test
        @DisplayName("met à jour les champs texte et retourne la publication modifiée")
        void updatesTextFieldsSuccessfully() throws IOException {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePublicationRequest request = updateRequest(
                    1, "Nouveau titre", "Nouveau contenu",
                    TypePublication.REVIEW, 10,
                    null, null, null, null,
                    "#ff0000", "#00ff00", "1.2rem"
            );

            Publication result = publicationService.updatePublication(request);

            assertThat(result.getTitre()).isEqualTo("Nouveau titre");
            assertThat(result.getContenue()).isEqualTo("Nouveau contenu");
            assertThat(result.getType()).isEqualTo(TypePublication.REVIEW);
            assertThat(result.getTitleColor()).isEqualTo("#ff0000");
            verify(publicationRepository).save(any());
        }

        @Test
        @DisplayName("ne modifie pas le titre si null")
        void doesNotChangeTitre_whenNull() throws IOException {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePublicationRequest request = updateRequest(
                    1, null, null,
                    null, 10,
                    null, null, null, null,
                    null, null, null
            );

            Publication result = publicationService.updatePublication(request);

            assertThat(result.getTitre()).isEqualTo("Titre de test");
        }

        @Test
        @DisplayName("ne modifie pas le titre si vide (blank)")
        void doesNotChangeTitre_whenBlank() throws IOException {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePublicationRequest request = updateRequest(
                    1, "   ", "   ",
                    null, 10,
                    null, null, null, null,
                    null, null, null
            );

            Publication result = publicationService.updatePublication(request);

            assertThat(result.getTitre()).isEqualTo("Titre de test");
            assertThat(result.getContenue()).isEqualTo("Contenu de test");
        }

        @Test
        @DisplayName("conserve les fichiers existants et ajoute les nouveaux si toKeep non null")
        void keepsExistingFilesAndAddsNew() throws IOException {
            Publication p = makePublication(1, 10, StatutPublication.ACTIVE);
            p.getImages().add("existing_image.jpg");

            when(publicationRepository.findById(1)).thenReturn(Optional.of(p));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePublicationRequest request = updateRequest(
                    1, "titre", "contenu",
                    TypePublication.ARTICLE, 10,
                    null, List.of("existing_image.jpg"),
                    null, null,
                    null, null, null
            );

            Publication result = publicationService.updatePublication(request);

            assertThat(result.getImages()).contains("existing_image.jpg");
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

            UpdatePublicationRequest request = updateRequest(
                    1, "titre", "contenu",
                    TypePublication.ARTICLE, 99,
                    null, null, null, null,
                    null, null, null
            );

            assertThatThrownBy(() -> publicationService.updatePublication(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Not authorized");

            verify(publicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("lève RuntimeException si la publication est introuvable")
        void throwsRuntimeException_whenNotFound() {
            when(publicationRepository.findById(99)).thenReturn(Optional.empty());

            UpdatePublicationRequest request = updateRequest(
                    99, "titre", "contenu",
                    TypePublication.ARTICLE, 1,
                    null, null, null, null,
                    null, null, null
            );

            assertThatThrownBy(() -> publicationService.updatePublication(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Publication not found");
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
}