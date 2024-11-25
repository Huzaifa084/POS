package com.devaxiom.pos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class AuditLog extends BaseEntity {

    private LocalDateTime timestamp;

    private String action;

    private String entity;

    private Long entityId;

    private String details;

    @ManyToOne
    @JoinColumn(name = "userId")
    private Users user;
}

