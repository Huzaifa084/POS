package com.devaxiom.pos.services;

import com.devaxiom.pos.exceptions.ResourceNotFoundException;
import com.devaxiom.pos.model.FilesData;
import com.devaxiom.pos.repositories.FileDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class FileDataService {
    private final FileDataRepository fileDataRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public FileDataService(FileDataRepository fileDataRepository) {
        this.fileDataRepository = fileDataRepository;
    }

    @Transactional
    public String uploadFile(MultipartFile file, Long username) throws IOException {
        if (file.isEmpty())
            throw new IOException("Cannot upload an empty file.");

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.contains(".."))
            throw new IOException(String.format("Invalid file name: %s", originalFileName));

        validateFile(file);

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String cleanedFileName = StringUtils.cleanPath(originalFileName);

        String uniqueFileName = UUID.randomUUID().toString() + "_" + cleanedFileName;
        Path filePath = uploadPath.resolve(uniqueFileName).normalize();

        if (!filePath.startsWith(uploadPath))
            throw new IOException("Cannot store file outside the current directory.");

        file.transferTo(filePath);
        log.info("User {} uploaded file: {}", username, uniqueFileName);

        FilesData filesData = FilesData.builder()
                .name(uniqueFileName)
                .type(file.getContentType())
                .filePath(filePath.toString())
                .owner(username)
                .build();

        fileDataRepository.save(filesData);

        return uniqueFileName;
    }

    public Resource loadFileAsResource(String fileName) throws IOException {
        Optional<FilesData> filesDataOpt = fileDataRepository.findByName(fileName);
        if (filesDataOpt.isEmpty())
            throw new ResourceNotFoundException(String.format("File not found: %s", fileName));

        FilesData filesData = filesDataOpt.get();
        Path filePath = Paths.get(filesData.getFilePath()).normalize();

        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new IOException("Could not read file: " + fileName, e);
        }

        if (!resource.exists() || !resource.isReadable())
            throw new IOException(String.format("Could not read file: %s", fileName));

        return resource;
    }

    private void validateFile(MultipartFile file) throws IOException {
        List<String> allowedMimeTypes = Arrays.asList("application/pdf", "image/png", "image/jpeg", "audio/mpeg");
        if (!allowedMimeTypes.contains(file.getContentType()))
            throw new IOException("Unsupported file type.");

        long maxFileSize = 10 * 1024 * 1024; // 10 MB
        if (file.getSize() > maxFileSize)
            throw new IOException("File size exceeds the maximum limit.");
    }

    public boolean isUserAuthorized(String fileName, Long username) {
        log.info("Checking if user {} is authorized to access file: {}", username, fileName);
        Optional<FilesData> filesDataOpt = fileDataRepository.findByName(fileName);
        if (filesDataOpt.isEmpty()) {
            log.warn("File not found: {}", fileName);
            throw new ResourceNotFoundException("File not found.");
        }
        FilesData filesData = filesDataOpt.get();
        log.info("File owner: {}", filesData.getOwner());
        log.info("User: {}", username);
        return Objects.equals(filesData.getOwner(), username);
    }

}
