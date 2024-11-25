package com.devaxiom.pos.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "files_data")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FilesData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private String name;
    private String type;

    @Column(name = "data", nullable = false)
    private String filePath;

    @Column(name = "ownerId", nullable = false)
    private Long owner;
}
