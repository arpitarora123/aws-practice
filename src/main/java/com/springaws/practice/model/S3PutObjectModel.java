package com.springaws.practice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class S3PutObjectModel implements Serializable {

    private String bucketName;

    @NotEmpty(message = "Key/File Name can't be empty")
    private String key;
    private MultipartFile file;

}
