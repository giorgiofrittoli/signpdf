package com.codebyte.signpdf.service;

import com.codebyte.signpdf.SignPDFCostant;
import com.codebyte.signpdf.SignPDFException;
import com.codebyte.signpdf.entities.CustomerSign;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.UUID;

@ApplicationScoped
public class SignPDFService {

    @Inject
    IExternalSignature iExternalSignature;

    @Inject
    Certificate[] certificates;

    @Transactional
    public File signPDF(String token, File pdf) throws SignPDFException {

        try{

            CustomerSign customerSign = CustomerSign.<CustomerSign>find("token", token).firstResultOptional().orElseThrow(NotFoundException::new);

            // Leggi il PDF
            PdfReader reader = new PdfReader(pdf);

            File pdfSigned = new File(FileUtils.getTempDirectory(), UUID.randomUUID() + "_" + pdf.getName());
            PdfSigner signer = new PdfSigner(reader, new FileOutputStream(pdfSigned), new StampingProperties());
            signer.setFieldName("Signature1");

            // Crea l'apparenza della firma
            signer.getSignatureAppearance().setReason("Test Firma").setLocation("Test Ragione").setReuseAppearance(false);

            // Provider di certificati
            IExternalDigest digest = new BouncyCastleDigest();

            // URL TSA
            ITSAClient tsaClient = new TSAClientBouncyCastle(SignPDFCostant.TSA_URL);

            // Firma il documento
            signer.signDetached(digest, iExternalSignature, certificates, null, null, tsaClient, 0, PdfSigner.CryptoStandard.CMS);

            customerSign.tot++;
            customerSign.persist();

            return pdfSigned;

        }catch (IOException | GeneralSecurityException e){
            throw new SignPDFException(e);
        }



    }
}
