package com.esprit.commentaireservice.services;

import com.esprit.commentaireservice.clients.PublicationClient;
import com.esprit.commentaireservice.clients.UserClient;
import com.esprit.commentaireservice.dto.PublicationDTO;
import com.esprit.commentaireservice.entities.Commentaire;
import com.esprit.commentaireservice.repositories.CommentaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentaireService {
    private final CommentaireRepository commentaireRepository;
    private final UserClient userClient;
    private final PublicationClient publicationClient;

    public List<Commentaire> getAllCommentaires() {
        List<Commentaire> list = commentaireRepository.findAllByOrderByCreateAtDesc();
        list.forEach(this::enrichWithUser);
        return list;
    }

    public List<Commentaire> getByPublicationId(Integer publicationId) {
        List<Commentaire> list = commentaireRepository.findRootByPublicationIdOrderByPinned(publicationId);
        list.forEach(this::enrichWithUser);
        return list;
    }

    public Commentaire getById(Integer id) {
        return commentaireRepository.findById(id)
                .orElseThrow(() -> new CommentaireNotFoundException("Commentaire not found: " + id));
    }

    public Commentaire create(String contenue, Integer publicationId, Integer userId) {
        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Content required");
        }

        try {
            publicationClient.getPublicationById(publicationId);
        } catch (Exception e) {
            throw new PublicationNotFoundException("Publication not found: " + publicationId);
        }

        Commentaire c = new Commentaire();
        c.setContenue(contenue);
        c.setUserId(userId);
        c.setPublicationId(publicationId);

        Commentaire saved = commentaireRepository.save(c);
        enrichWithUser(saved);
        return saved;
    }

    public Commentaire reply(String contenue, Integer parentId, Integer publicationId, Integer userId) {
        if (contenue == null || contenue.trim().isEmpty()) {
            throw new IllegalArgumentException("Content required");
        }

        Commentaire parent = getById(parentId);

        Commentaire c = new Commentaire();
        c.setContenue(contenue);
        c.setUserId(userId);
        c.setPublicationId(publicationId);
        c.setParent(parent);

        Commentaire saved = commentaireRepository.save(c);
        enrichWithUser(saved);
        return saved;
    }

    public Commentaire update(Integer id, String contenue, Integer userId) {
        Commentaire c = getById(id);

        if (!c.getUserId().equals(userId)) {
            throw new UnauthorizedCommentaireException("Not authorized to update this commentaire");
        }

        if (contenue != null && !contenue.trim().isEmpty()) {
            c.setContenue(contenue);
        }

        return commentaireRepository.save(c);
    }

    public void delete(Integer id, Integer userId) {
        Commentaire c = getById(id);

        if (!c.getUserId().equals(userId)) {
            throw new UnauthorizedCommentaireException("Not authorized to delete this commentaire");
        }

        commentaireRepository.delete(c);
    }

    public Commentaire togglePin(Integer id, Integer userId) {
        Commentaire c = getById(id);

        try {
            PublicationDTO pub = publicationClient.getPublicationById(c.getPublicationId());
            if (!pub.getUserId().equals(userId)) {
                throw new UnauthorizedCommentaireException("Only publication owner can pin");
            }
        } catch (UnauthorizedCommentaireException e) {
            throw e;
        } catch (Exception e) {
            throw new PublicationValidationException("Error checking publication: " + e.getMessage());
        }

        c.setPinned(!c.isPinned());
        return commentaireRepository.save(c);
    }

    private void enrichWithUser(Commentaire c) {
        try {
            c.setUser(userClient.getUserById(c.getUserId()));
        } catch (Exception ignored) {
            // User enrichment failed - continue without user data
        }

        if (c.getReplies() != null && !c.getReplies().isEmpty()) {
            c.getReplies().forEach(reply -> {
                try {
                    reply.setUser(userClient.getUserById(reply.getUserId()));
                } catch (Exception ignored) {
                    // Reply user enrichment failed - continue without user data
                }
            });
        }
    }
}

// Custom exceptions
class CommentaireNotFoundException extends RuntimeException {
    public CommentaireNotFoundException(String message) {
        super(message);
    }
}

class PublicationNotFoundException extends RuntimeException {
    public PublicationNotFoundException(String message) {
        super(message);
    }
}

class UnauthorizedCommentaireException extends RuntimeException {
    public UnauthorizedCommentaireException(String message) {
        super(message);
    }
}

class PublicationValidationException extends RuntimeException {
    public PublicationValidationException(String message) {
        super(message);
    }
}