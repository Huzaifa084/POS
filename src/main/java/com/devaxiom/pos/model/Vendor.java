package com.devaxiom.pos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Vendor extends BaseEntity{

    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String address;

    @OneToMany(mappedBy = "vendor")
    private List<PurchaseOrder> purchaseOrders;

}

