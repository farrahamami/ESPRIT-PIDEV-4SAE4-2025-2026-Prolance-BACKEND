package com.esprit.publicationservice.services;

import com.esprit.publicationservice.clients.UserClient;
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

@Service
@RequiredArgsConstructor
public class PublicationService {

    private static final Logger logger = LoggerFactory.getLogger(PublicationService.class);

    private final PublicationRepository publicationRepository;
    private final UserClient userClient;
    private static final String UPLOAD_DIR = "uploads/publications/";
    private static final int SIGNALEMENT_THRESHOLD = 3;
    private static final long BLOCK_THRESHOLD = 3;
    private static final String PUBLICATION_NOT_FOUND = "Publication not found";

    // Custom exceptions
    public static class PublicationNotFoundException extends RuntimeException {
        public PublicationNotFoundException(String message) { super(message); }
    }
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }

    // ========== REQUEST DTO CLASSES ==========

    public static class CreatePublicationRequest {
        private String titre;
        private String contenue;
        private TypePublication type;
        private Integer userId;
        private List<MultipartFile> images;
        private List<MultipartFile> pdfs;
        private String titleColor;
        private String contentColor;
        private String titleFontSize;

        // Getters and Setters
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public String getContenue() { return contenue; }
        public void setContenue(String contenue) { this.contenue = contenue; }
        public TypePublication getType() { return type; }
        public void setType(TypePublication type) { this.type = type; }
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public List<MultipartFile> getImages() { return images; }
        public void setImages(List<MultipartFile> images) { this.images = images; }
        public List<MultipartFile> getPdfs() { return pdfs; }
        public void setPdfs(List<MultipartFile> pdfs) { this.pdfs = pdfs; }
        public String getTitleColor() { return titleColor; }
        public void setTitleColor(String titleColor) { this.titleColor = titleColor; }
        public String getContentColor() { return contentColor; }
        public void setContentColor(String contentColor) { this.contentColor = contentColor; }
        public String getTitleFontSize() { return titleFontSize; }
        public void setTitleFontSize(String titleFontSize) { this.titleFontSize = titleFontSize; }
    }

    public static class UpdatePublicationRequest {
        private Integer id;
        private String titre;
        private String contenue;
        private TypePublication type;
        private Integer userId;
        private List<MultipartFile> newImages;
        private List<String> imagesToKeep;
        private List<MultipartFile> newPdfs;
        private List<String> pdfsToKeep;
        private String titleColor;
        private String contentColor;
        private String titleFontSize;

        // Getters and Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public String getContenue() { return contenue; }
        public void setContenue(String contenue) { this.contenue = contenue; }
        public TypePublication getType() { return type; }
        public void setType(TypePublication type) { this.type = type; }
        public Integer getUserId() { return userId; }
        public void setUserId(Integer userId) { this.userId = userId; }
        public List<MultipartFile> getNewImages() { return newImages; }
        public void setNewImages(List<MultipartFile> newImages) { this.newImages = newImages; }
        public List<String> getImagesToKeep() { return imagesToKeep; }
        public void setImagesToKeep(List<String> imagesToKeep) { this.imagesToKeep = imagesToKeep; }
        public List<MultipartFile> getNewPdfs() { return newPdfs; }
        public void setNewPdfs(List<MultipartFile> newPdfs) { this.newPdfs = newPdfs; }
        public List<String> getPdfsToKeep() { return pdfsToKeep; }
        public void setPdfsToKeep(List<String> pdfsToKeep) { this.pdfsToKeep = pdfsToKeep; }
        public String getTitleColor() { return titleColor; }
        public void setTitleColor(String titleColor) { this.titleColor = titleColor; }
        public String getContentColor() { return contentColor; }
        public void setContentColor(String contentColor) { this.contentColor = contentColor; }
        public String getTitleFontSize() { return titleFontSize; }
        public void setTitleFontSize(String titleFontSize) { this.titleFontSize = titleFontSize; }
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
                .collect(java.util.stream.Collectors.groupingBy(Publication::getUserId, java.util.stream.Collectors.counting()));

        List<UserBlockDTO> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : archivedCountByUser.entrySet()) {
            Integer uid = entry.getKey();
            long count  = entry.getValue();
            String name = "";
            String lastName = "";
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

    // FIXED: Now uses parameter object (1 parameter instead of 9)
    public Publication createPublication(CreatePublicationRequest request) throws IOException {
        validateCreateInput(request.getTitre(), request.getContenue(), request.getUserId(),
                request.getType(), request.getImages(), request.getPdfs());

        Publication p = buildPublication(request.getTitre(), request.getContenue(), request.getType(),
                request.getUserId(), request.getTitleColor(),
                request.getContentColor(), request.getTitleFontSize());
        p.setImages(saveFiles(request.getImages(), false));
        p.setPdfs(saveFiles(request.getPdfs(), true));

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    // FIXED: Now uses parameter object (1 parameter instead of 12)
    public Publication updatePublication(UpdatePublicationRequest request) throws IOException {
        Publication p = publicationRepository.findById(request.getId())
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND));

        if (!p.getUserId().equals(request.getUserId()))
            throw new UserNotFoundException("Not authorized");

        applyTextFields(p, request.getTitre(), request.getContenue(), request.getType(),
                request.getTitleColor(), request.getContentColor(), request.getTitleFontSize());
        p.setImages(updateFiles(p.getImages(), request.getImagesToKeep(), request.getNewImages(), false));
        p.setPdfs(updateFiles(p.getPdfs(), request.getPdfsToKeep(), request.getNewPdfs(), true));

        Publication saved = publicationRepository.save(p);
        enrichWithUser(saved);
        return saved;
    }

    public void deletePublication(Integer id, Integer userId) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND));
        if (!p.getUserId().equals(userId)) throw new UserNotFoundException("Not authorized");
        p.getImages().forEach(this::deleteFile);
        p.getPdfs().forEach(this::deleteFile);
        publicationRepository.delete(p);
    }

    public void adminDeletePublication(Integer id) {
        Publication p = publicationRepository.findById(id)
                .orElseThrow(() -> new PublicationNotFoundException(PUBLICATION_NOT_FOUND));
        p.getImages().forEach(this::deleteFile);
        p.getPdfs().forEach(this::deleteFile);
        publicationRepository.delete(p);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateCreateInput(String titre, String contenue, Integer userId,
                                     TypePublication type, List<MultipartFile> images, List<MultipartFile> pdfs) {
        if (titre == null || titre.trim().isEmpty()) throw new IllegalArgumentException("Title is required");
        if (contenue == null || contenue.trim().isEmpty()) throw new IllegalArgumentException("Content is required");
        try {
            userClient.getUserById(userId);
        } catch (Exception e) {
            throw new UserNotFoundException("User not found: " + userId);
        }
        if (isUserBlocked(userId)) {
            throw new IllegalStateException("BLOCKED: Votre compte est bloqué suite à 3 posts signalés. Contactez l'administrateur.");
        }
        if (type == TypePublication.QUESTION) {
            boolean hasImages = images != null && images.stream().anyMatch(f -> !f.isEmpty());
            boolean hasPdfs   = pdfs   != null && pdfs.stream().anyMatch(f -> !f.isEmpty());
            if (hasImages || hasPdfs) throw new IllegalArgumentException("No images/PDFs allowed for QUESTION type.");
        }
    }

    private Publication buildPublication(String titre, String contenue, TypePublication type,
                                         Integer userId, String titleColor, String contentColor, String titleFontSize) {
        Publication p = new Publication();
        p.setTitre(titre);
        p.setContenue(contenue);
        p.setType(type);
        p.setUserId(userId);
        p.setStatut(StatutPublication.ACTIVE);
        if (titleColor    != null) p.setTitleColor(titleColor);
        if (contentColor  != null) p.setContentColor(contentColor);
        if (titleFontSize != null) p.setTitleFontSize(titleFontSize);
        return p;
    }

    private void applyTextFields(Publication p, String titre, String contenue, TypePublication type,
                                 String titleColor, String contentColor, String titleFontSize) {
        if (titre    != null && !titre.trim().isEmpty())    p.setTitre(titre);
        if (contenue != null && !contenue.trim().isEmpty()) p.setContenue(contenue);
        if (type         != null) p.setType(type);
        if (titleColor   != null) p.setTitleColor(titleColor);
        if (contentColor != null) p.setContentColor(contentColor);
        if (titleFontSize != null) p.setTitleFontSize(titleFontSize);
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
        if (isPdf  && (ct == null || !ct.equals("application/pdf"))) throw new IllegalArgumentException("Only PDFs accepted");
        if (!isPdf && (ct == null || !ct.startsWith("image/")))      throw new IllegalArgumentException("Only images accepted");
        String name = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR);
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