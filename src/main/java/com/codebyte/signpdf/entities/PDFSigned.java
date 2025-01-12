package com.codebyte.signpdf.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "pdf_signed")
public class PDFSigned extends PanacheEntity {

    @Column
    public String firmatario;
    @Column(name = "uuid_firma")
    public String uuidFirma;
    @Column(name = "ts_firma")
    @CreationTimestamp
    public String tsFirma;
    @Column(name = "cellulare_otp")
    public String cellulareOTP;
    @Column
    public String md5;
    @Column
    public String caller;

    @ManyToOne
    public CustomerSign customerSign;

}
