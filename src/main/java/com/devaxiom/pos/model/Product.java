package com.devaxiom.pos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
public class Product extends BaseEntity{

    private String name;
    private String description;
    private String category;
    private BigDecimal unitPrice;
    private Integer quantityInStock;
    private Integer reorderLevel;

    @OneToMany(mappedBy = "product")
    private List<PurchaseOrderItem> purchaseOrderItems;

    @OneToMany(mappedBy = "product")
    private List<SaleItem> saleItems;
}
