package com.esprit.commentaireservice.controllers;

import com.esprit.commentaireservice.entities.Commentaire;
import com.esprit.commentaireservice.services.CommentaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
public class CommentaireController {
    private final CommentaireService commentaireService;

    @GetMapping
    public ResponseEntity<List<Commentaire>> getAll() {
        return ResponseEntity.ok(commentaireService.getAllCommentaires());
    }

    @GetMapping("/publication/{publicationId}")
    public ResponseEntity<List<Commentaire>> getByPublication(@PathVariable Integer publicationId) {
        return ResponseEntity.ok(commentaireService.getByPublicationId(publicationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Commentaire> getById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(commentaireService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestParam String contenue,
                                         @RequestParam Integer publicationId,
                                         @RequestParam Integer userId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(commentaireService.create(contenue, publicationId, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{parentId}/reply")
    public ResponseEntity<Object> reply(@PathVariable Integer parentId,
                                        @RequestParam String contenue,
                                        @RequestParam Integer publicationId,
                                        @RequestParam Integer userId) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(commentaireService.reply(contenue, parentId, publicationId, userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Integer id,
                                         @RequestParam String contenue,
                                         @RequestParam Integer userId) {
        try {
            return ResponseEntity.ok(commentaireService.update(id, contenue, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id, @RequestParam Integer userId) {
        try {
            commentaireService.delete(id, userId);
            return ResponseEntity.ok("Deleted");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<Commentaire> pin(@PathVariable Integer id, @RequestParam Integer userId) {
        try {
            return ResponseEntity.ok(commentaireService.togglePin(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}