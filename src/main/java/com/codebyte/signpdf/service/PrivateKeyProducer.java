package com.codebyte.signpdf.service;

import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PrivateKeySignature;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

@ApplicationScoped
public class PrivateKeyProducer {

    IExternalSignature iExternalSignature;

    Certificate[] certificates;

    @Startup
    void init() throws IOException, NoSuchAlgorithmException, CertificateException, InvalidKeySpecException {

        // Aggiungi provider BouncyCastle
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream certIS = PrivateKeyProducer.class.getResourceAsStream("/example.com.crt")) {
            certificates = new Certificate[]{certFactory.generateCertificate(certIS)};
        }

        PrivateKey privateKey;
        try (InputStream keyIS = PrivateKeyProducer.class.getResourceAsStream("/example.com.pkcs8.key")) {
            // Gestione della chiave privata
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] keyBytes = keyIS.readAllBytes();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            privateKey = keyFactory.generatePrivate(keySpec);
        }

        // Crea un PrivateKeyEntry con il certificato e la chiave privata
        KeyStore.PrivateKeyEntry pKey = new KeyStore.PrivateKeyEntry(privateKey, certificates);
        iExternalSignature = new PrivateKeySignature(pKey.getPrivateKey(), DigestAlgorithms.SHA256, BouncyCastleProvider.PROVIDER_NAME);
    }

    @Produces
    public IExternalSignature getSignature() {
        return iExternalSignature;
    }

    @Produces
    public Certificate[] getCertificates() {
        return certificates;
    }

}
