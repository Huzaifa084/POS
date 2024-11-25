package com.devaxiom.pos.model;

import com.devaxiom.pos.enums.PaymentMethod;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Payment extends BaseEntity {

    private LocalDateTime paymentDate;

    @DecimalMin("0.0")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String referenceNumber;

    @ManyToOne
    @JoinColumn(name = "customerId")
    private Customer customer;
}

