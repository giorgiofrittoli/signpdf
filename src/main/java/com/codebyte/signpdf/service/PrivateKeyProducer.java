package com.codebyte.signpdf.service;

import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PrivateKeySignature;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayOutputStream;
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
import java.util.Base64;

@ApplicationScoped
public class PrivateKeyProducer {

    @ConfigProperty(name = "cert.filepath")
    String certFilepath;

    @ConfigProperty(name = "private.key.filepath")
    String privateKeyFilepath;

    IExternalSignature iExternalSignature;

    Certificate[] certificates;

    @Startup
    void init() throws IOException, NoSuchAlgorithmException, CertificateException, InvalidKeySpecException {

        // Aggiungi provider BouncyCastle
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream certIS = new FileInputStream(certFilepath)) {
            certificates = new Certificate[]{certFactory.generateCertificate(certIS)};
        }

        PrivateKey privateKey;
        try (InputStream keyIS = new FileInputStream(privateKeyFilepath)) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = keyIS.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] encodedPrivateKey = baos.toByteArray();

            // The encoded private key should be in PEM format, so we need to remove the PEM header and footer
            String privateKeyPem = new String(encodedPrivateKey);
            privateKeyPem = privateKeyPem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");

            // Decode the Base64 encoded private key
            byte[] decodedPrivateKey = Base64.getDecoder().decode(privateKeyPem);

            // Gestione della chiave privata
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
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
