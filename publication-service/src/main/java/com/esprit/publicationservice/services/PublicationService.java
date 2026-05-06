package com.esprit.publicationservice.services;

import com.esprit.publicationservice.clients.UserClient;
import com.esprit.publicationservice.dto.BasePublicationRequest;
import com.esprit.publicationservice.dto.CreatePublicationRequest;
import com.esprit.publicationservice.dto.UpdatePublicationRequest;
import com.esprit.publicationservice.dto.UserBlockDTO;
import com.esprit.publicationservice.dto.UserDTO;
import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.StatutPublication;
import com.esprit.publicationservice.entities.TypePublication;
import com.esprit.publicationservice.repositories.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicationService {

    private static final Logger logger = LoggerFactory.getLogger(PublicationService.class);

    private final PublicationRepository publicationRepository;
    private final UserClient userClient;

    private static final String UPLOAD_DIR           = "uploads/publications/";
    private static final int    SIGNALEMENT_THRESHOLD = 3;
    private static final long   BLOCK_THRESHOLD       = 3;
    private static final String PUBLICATION_NOT_FOUND = "Publication not found";

    // ========== CUSTOM EXCEPTIONS ==========

    public static class PublicationNotFoundException extends RuntimeException {
        public PublicationNotFoundException(String message) { super(message); }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }

    // ========== PUBLIC METHODS ==========

    public List<Publication> getAllPublications() {
        List<Publication> list = publicationRepository.findByStatutOrderByCreateAtDesc(StatutPublication.ACTIVE);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Publication> getAllPublicationsAdmin() {
        List<Publication> list = publicationRepository.findAllByOrderByCreateAtDesc();
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Publication> getPublicationsByType(TypePublication type) {
        List<Publication> list = publicationRepository.findByTypeAndStatutOrderByCreateAtDesc(type, StatutPublication.ACTIVE);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Publication> getPublicationsByUserId(Integer userId) {
        List<Publication> list = publicationRepository.findByUserId(userId);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Publication> getArchivedByUserId(Integer userId) {
        List<Publication> list = publicationRepository.findByUserIdAndStatut(userId, StatutPublication.ARCHIVED);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public Publication getPublicationById(Integer id) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND + ": " + id));
        enrichWithUser(p);
        return p;
    }

    public boolean isUserBlocked(Integer userId) {
        return publicationRepository.countArchivedByUserId(userId) >= BLOCK_THRESHOLD;
    }

    public long getArchivedCount(Integer userId) {
        return publicationRepository.countArchivedByUserId(userId);
    }

    public void reactiverCompteUser(Integer userId) {
        List<Publication> allUserPubs = publicationRepository.findByUserId(userId);

        List<Publication> archivedPubs = allUserPubs.stream()
                .filter(p -> p.getStatut() == StatutPublication.ARCHIVED)
                .toList();

        for (Publication p : archivedPubs) {
            p.getImages().forEach(this::deleteFile);
            p.getPdfs().forEach(this::deleteFile);
            publicationRepository.delete(p);
        }

        List<Publication> activePubs = allUserPubs.stream()
                .filter(p -> p.getStatut() == StatutPublication.ACTIVE)
                .toList();

        for (Publication p : activePubs) {
            p.getSignalements().clear();
            p.getSignalementRaisons().clear();
        }
        publicationRepository.saveAll(activePubs);
    }

    public List<UserBlockDTO> getAllUsersBlockStatus() {
        Map<Integer, Long> archivedCountByUser = publicationRepository
                .findAllByOrderByCreateAtDesc()
                .stream()
                .filter(p -> p.getStatut() == StatutPublication.ARCHIVED)
                .collect(Collectors.groupingBy(Publication::getUserId, Collectors.counting()));

        List<UserBlockDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : archivedCountByUser.entrySet()) {
            Integer uid      = entry.getKey();
            long    count    = entry.getValue();
            String  name     = "";
            String  lastName = "";
            try {
                UserDTO u = userClient.getUserById(uid);
                name     = u.getName()     != null ? u.getName()     : "";
                lastName = u.getLastName() != null ? u.getLastName() : "";
            } catch (Exception e) {
                logger.debug("Could not fetch user {}: {}", uid, e.getMessage());
            }
            result.add(new UserBlockDTO(uid, name, lastName, count));
        }

        result.sort(Comparator.comparingLong(UserBlockDTO::getArchivedCount).reversed());
        return result;
    }

    public Publication signalerPublication(Integer id, Integer userId, String raison) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND + ": " + id));

        if (p.getStatut() != StatutPublication.ACTIVE) {
            throw new IllegalStateException("Cette publication n'est pas active.");
        }
        if (p.getSignalements().contains(userId)) {
            throw new IllegalStateException("Vous avez déjà signalé cette publication.");
        }

        p.getSignalements().add(userId);
        p.getSignalementRaisons().add(raison != null ? raison : "");

        if (p.getSignalements().size() >= SIGNALEMENT_THRESHOLD) {
            p.setStatut(StatutPublication.ARCHIVED);
            p.setArchivedAt(java.time.LocalDateTime.now());
        }

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    public Publication createPublication(CreatePublicationRequest request) throws IOException {
        validateCreateInput(request);

        Publication p = buildPublication(request);
        p.setImages(saveFiles(request.getImages(), false));
        p.setPdfs(saveFiles(request.getPdfs(), true));

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    public Publication updatePublication(UpdatePublicationRequest request) throws IOException {
        Publication p = publicationRepository.findById(request.getId())
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND));

        if (!p.getUserId().equals(request.getUserId())) {
            throw new UserNotFoundException("Not authorized");
        }

        applyTextFields(p, request);
        p.setImages(updateFiles(p.getImages(), request.getImagesToKeep(), request.getNewImages(), false));
        p.setPdfs(updateFiles(p.getPdfs(), request.getPdfsToKeep(), request.getNewPdfs(), true));

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    public void deletePublication(Integer id, Integer userId) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND));
        if (!p.getUserId().equals(userId)) {
            throw new UserNotFoundException("Not authorized");
        }
        deletePublicationFiles(p);
        publicationRepository.delete(p);
    }

    public void adminDeletePublication(Integer id) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND));
        deletePublicationFiles(p);
        publicationRepository.delete(p);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateCreateInput(CreatePublicationRequest request) {
        String titre    = request.getTitre();
        String contenue = request.getContenue();
        Integer userId  = request.getUserId();

        if (titre == null || titre.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new UserNotFoundException("User not found: " + userId);
        }
        if (isUserBlocked(userId)) {
            throw new IllegalStateException(
                    "BLOCKED: Votre compte est bloqué suite à 3 posts signalés. Contactez l'administrateur."
            );
        }
        if (request.getType() == TypePublication.QUESTION) {
            List<MultipartFile> images = request.getImages();
            List<MultipartFile> pdfs   = request.getPdfs();
            boolean hasImages = images != null && images.stream().anyMatch(f -> !f.isEmpty());
            boolean hasPdfs   = pdfs   != null && pdfs.stream().anyMatch(f -> !f.isEmpty());
            if (hasImages || hasPdfs) {
                throw new IllegalArgumentException("No images/PDFs allowed for QUESTION type.");
            }
        }
    }

    private Publication buildPublication(BasePublicationRequest request) {
        Publication p = new Publication();
        p.setTitre(request.getTitre());
        p.setContenue(request.getContenue());
        p.setType(request.getType());
        p.setUserId(request.getUserId());
        p.setStatut(StatutPublication.ACTIVE);
        applyColorFields(p, request);
        return p;
    }

    private void applyTextFields(Publication p, BasePublicationRequest request) {
        String titre    = request.getTitre();
        String contenue = request.getContenue();
        if (titre    != null && !titre.trim().isEmpty())    p.setTitre(titre);
        if (contenue != null && !contenue.trim().isEmpty()) p.setContenue(contenue);
        if (request.getType() != null) p.setType(request.getType());
        applyColorFields(p, request);
    }

    private void applyColorFields(Publication p, BasePublicationRequest request) {
        if (request.getTitleColor()   != null) p.setTitleColor(request.getTitleColor());
        if (request.getContentColor() != null) p.setContentColor(request.getContentColor());
        if (request.getTitleFontSize() != null) p.setTitleFontSize(request.getTitleFontSize());
    }

    private void deletePublicationFiles(Publication p) {
        p.getImages().forEach(this::deleteFile);
        p.getPdfs().forEach(this::deleteFile);
    }

    private List<String> saveFiles(List<MultipartFile> files, boolean isPdf) throws IOException {
        List<String> names = new ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (!f.isEmpty()) names.add(saveFile(f, isPdf));
            }
        }
        return names;
    }

    private List<String> updateFiles(List<String> current, List<String> toKeep,
                                     List<MultipartFile> newFiles, boolean isPdf) throws IOException {
        for (String file : current) {
            if (toKeep == null || !toKeep.contains(file)) deleteFile(file);
        }
        List<String> updated = new ArrayList<>();
        if (toKeep != null) updated.addAll(toKeep);
        if (newFiles != null) {
            for (MultipartFile f : newFiles) {
                if (!f.isEmpty()) updated.add(saveFile(f, isPdf));
            }
        }
        return updated;
    }

    private void enrichWithUser(Publication p) {
        try {
            p.setUser(userClient.getUserById(p.getUserId()));
        } catch (Exception e) {
            logger.debug("Could not enrich publication {} with user data: {}", p.getId(), e.getMessage());
        }
    }

    private String saveFile(MultipartFile file, boolean isPdf) throws IOException {
        String ct = file.getContentType();
        if (isPdf  && (ct == null || !ct.equals("application/pdf"))) {
            throw new IllegalArgumentException("Only PDFs accepted");
        }
        if (!isPdf && (ct == null || !ct.startsWith("image/"))) {
            throw new IllegalArgumentException("Only images accepted");
        }
        String name = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path   path = Paths.get(UPLOAD_DIR);
        if (!Files.exists(path)) Files.createDirectories(path);
        Files.copy(file.getInputStream(), path.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return name;
    }

    private void deleteFile(String name) {
        try {
            Files.deleteIfExists(Paths.get(UPLOAD_DIR + name));
        } catch (IOException e) {
            logger.warn("Delete error for file {}: {}", name, e.getMessage());
        }
    }
}