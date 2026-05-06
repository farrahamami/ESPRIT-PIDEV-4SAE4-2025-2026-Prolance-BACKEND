package com.esprit.publicationservice.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter @Setter
public class CreatePublicationRequest extends BasePublicationRequest {
    private List<MultipartFile> images;
    private List<MultipartFile> pdfs;
}