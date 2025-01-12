package com.codebyte.signpdf.entities;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_sign")
public class CustomerSign extends PanacheEntity {

    @Column
    public String name;
    @Column
    public String token;

    @OneToMany(mappedBy = "customerSign", fetch = FetchType.LAZY)
    public List<PDFSigned> pdfSigned = new ArrayList<>();

}
