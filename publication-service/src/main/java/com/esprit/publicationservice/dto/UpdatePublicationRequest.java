package com.esprit.publicationservice.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter @Setter
public class UpdatePublicationRequest extends BasePublicationRequest {
    private Integer id;
    private List<MultipartFile> newImages;
    private List<String> imagesToKeep;
    private List<MultipartFile> newPdfs;
    private List<String> pdfsToKeep;
}