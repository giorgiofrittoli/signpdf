package com.codebyte.signpdf.entities;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_sign")
public class CustomerSign extends PanacheEntity {

    @Column
    public String name;
    @Column
    public String token;

    @Column
    public int tot;

}
