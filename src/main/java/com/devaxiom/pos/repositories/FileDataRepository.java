package com.devaxiom.pos.repositories;

import com.devaxiom.pos.model.FilesData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileDataRepository extends JpaRepository<FilesData, Long> {

    Optional<FilesData> findByName(String fileName);
}
