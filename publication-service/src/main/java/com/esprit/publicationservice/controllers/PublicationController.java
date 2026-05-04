package com.esprit.publicationservice.controllers;

import com.esprit.publicationservice.dto.UserBlockDTO;
import com.esprit.publicationservice.entities.Publication;
import com.esprit.publicationservice.entities.TypePublication;
import com.esprit.publicationservice.services.PublicationService;
import com.esprit.publicationservice.dto.CreatePublicationRequest;
import com.esprit.publicationservice.dto.UpdatePublicationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
public class PublicationController {

    private final PublicationService publicationService;

    @GetMapping
    public ResponseEntity<List<Publication>> getAll() {
        return ResponseEntity.ok(publicationService.getAllPublications());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Publication>> getAllAdmin() {
        return ResponseEntity.ok(publicationService.getAllPublicationsAdmin());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Publication>> getByType(@PathVariable TypePublication type) {
        return ResponseEntity.ok(publicationService.getPublicationsByType(type));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Publication>> getByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(publicationService.getPublicationsByUserId(userId));
    }

    @GetMapping("/user/{userId}/archived")
    public ResponseEntity<List<Publication>> getArchivedByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(publicationService.getArchivedByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Publication> getById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(publicationService.getPublicationById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/user/{userId}/block-status")
    public ResponseEntity<Map<String, Object>> getBlockStatus(@PathVariable Integer userId) {
        boolean blocked = publicationService.isUserBlocked(userId);
        long archivedCount = publicationService.getArchivedCount(userId);
        return ResponseEntity.ok(Map.of(
                "blocked", blocked,
                "warningCount", archivedCount
        ));
    }

    @GetMapping("/admin/blocked-users")
    public ResponseEntity<List<UserBlockDTO>> getBlockedUsers() {
        return ResponseEntity.ok(publicationService.getAllUsersBlockStatus());
    }

    @PostMapping("/admin/users/{userId}/reactiver-compte")
    public ResponseEntity<Map<String, String>> reactiverCompteUser(@PathVariable Integer userId) {
        try {
            publicationService.reactiverCompteUser(userId);
            return ResponseEntity.ok(Map.of("message", "Compte réactivé. Publications archivées supprimées."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/signaler")
    public ResponseEntity<Object> signaler(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            @RequestParam(value = "raison", required = false, defaultValue = "") String raison) {
        try {
            return ResponseEntity.ok(publicationService.signalerPublication(id, userId, raison));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> create(
            @RequestParam("titre") String titre,
            @RequestParam("contenue") String contenue,
            @RequestParam("type") TypePublication type,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "pdfs", required = false) List<MultipartFile> pdfs,
            @RequestParam(value = "titleColor", required = false, defaultValue = "#2d1f4e") String titleColor,
            @RequestParam(value = "contentColor", required = false, defaultValue = "#6b5e8e") String contentColor,
            @RequestParam(value = "titleFontSize", required = false, defaultValue = "1.1rem") String titleFontSize) {
        try {
            // Build the request object
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

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(publicationService.createPublication(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> update(
            @PathVariable Integer id,
            @RequestParam(value = "titre", required = false) String titre,
            @RequestParam(value = "contenue", required = false) String contenue,
            @RequestParam(value = "type", required = false) TypePublication type,
            @RequestParam("userId") Integer userId,
            @RequestParam(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "imagesToKeep", required = false) List<String> imagesToKeep,
            @RequestParam(value = "pdfs", required = false) List<MultipartFile> newPdfs,
            @RequestParam(value = "pdfsToKeep", required = false) List<String> pdfsToKeep,
            @RequestParam(value = "titleColor", required = false) String titleColor,
            @RequestParam(value = "contentColor", required = false) String contentColor,
            @RequestParam(value = "titleFontSize", required = false) String titleFontSize) {
        try {
            // Build the request object
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

            return ResponseEntity.ok(publicationService.updatePublication(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id, @RequestParam Integer userId) {
        try {
            publicationService.deletePublication(id, userId);
            return ResponseEntity.ok("Deleted");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> adminDelete(@PathVariable Integer id) {
        try {
            publicationService.adminDeletePublication(id);
            return ResponseEntity.ok("Deleted");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}