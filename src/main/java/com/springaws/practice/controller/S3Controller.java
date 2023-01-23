package com.springaws.practice.controller;

import com.springaws.practice.model.S3PutObjectModel;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@ResponseBody
@RequestMapping("/s3")
@AllArgsConstructor
@Slf4j
public class S3Controller {

    private final S3Template s3Template;

    private final S3Client s3Client;

    private final S3OutputStreamProvider s3OutputStreamProvider;
    private final S3ObjectConverter s3ObjectConverter;

    @PostMapping("/create")
    public String createS3Bucket(@RequestBody String bucketName) {
        log.info("Creating bucket {}", bucketName);
        var bucket = s3Client.createBucket(CreateBucketRequest
                .builder()
                .bucket(bucketName)
                .build());

        s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());

        return bucket.location();
    }

    @PutMapping(value = "/put", consumes = {MULTIPART_FORM_DATA_VALUE},
            produces = {APPLICATION_JSON_VALUE})
    public S3Resource put(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("Putting object {} {}", file, file.getContentType());
        var bucketName = "testarpitarora1";
        var key = file.getName() + ".pdf";
        var object = file.getBytes();
        Assert.notNull(bucketName, "bucketName is required");
        Assert.notNull(key, "key is required");
        Assert.notNull(object, "object is required");
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder().bucket(bucketName).key(key).contentType(file.getContentType());
        this.s3Client.putObject((PutObjectRequest) requestBuilder.build(), software.amazon.awssdk.core.sync.RequestBody.fromBytes(object));//this.s3ObjectConverter.write(object));
        return new S3Resource(bucketName, key, this.s3Client, this.s3OutputStreamProvider);
    }

    @PostMapping(value = "/upload-from-file-path")
    public S3Resource uploadFromFilePath(@RequestBody String filePath) throws IOException {
        log.info("Putting object {}", filePath);
        var bucketName = "testarpitarora1";
        File file = new File(filePath);
        var key = file.getName();
        Assert.notNull(bucketName, "bucketName is required");
        Assert.notNull(key, "key is required");

        var response = s3Template.upload(bucketName, key, new FileInputStream(file));
        file.delete();
        return response;
    }


    @PostMapping(value = "/upload", consumes = {MULTIPART_FORM_DATA_VALUE})
    public S3Resource upload(@RequestBody S3PutObjectModel s3PutObjectModel) throws IOException {
        log.info("Putting object {}", s3PutObjectModel.getKey());
        var bucketName = "testarpitarora1";
        var key = System.currentTimeMillis() + "_" + s3PutObjectModel.getFile().getOriginalFilename();
        Assert.notNull(bucketName, "bucketName is required");
        Assert.notNull(key, "key is required");
        var file = convertMultipartFileToFile(s3PutObjectModel.getFile());
        var response = s3Template.upload(bucketName, key, new FileInputStream(file));
        file.delete();
        return response;
    }

    @GetMapping(value = "/download/{key}")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String key) throws IOException {
        log.info("Downloading object {} ", key);
        var bucketName = "testarpitarora1";
        Assert.notNull(key, "key is required");

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        var result = this.s3Client.getObject(getObjectRequest);
        var response = result.readAllBytes();
        var byteArrayResource = new ByteArrayResource(response);
        log.info("Successfully downloaded the file {}", key);
        return ResponseEntity.ok()
                .contentLength(response.length)
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment;filename=\"" + key + "\"")
                .body(byteArrayResource);
    }

    private File convertMultipartFileToFile(MultipartFile multipartFile) {
        var file = new File(multipartFile.getOriginalFilename());

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        } catch (IOException exception) {
            log.error(exception.getMessage());
        }
        return file;
    }
}
