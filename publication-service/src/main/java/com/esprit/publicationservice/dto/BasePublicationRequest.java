package com.esprit.publicationservice.dto;

import com.esprit.publicationservice.entities.TypePublication;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class BasePublicationRequest {
    private String titre;
    private String contenue;
    private TypePublication type;
    private Integer userId;
    private String titleColor;
    private String contentColor;
    private String titleFontSize;
}