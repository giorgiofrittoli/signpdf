package com.codebyte.signpdf.resources;

import com.codebyte.signpdf.entities.PDFSigned;
import com.codebyte.signpdf.resources.dto.NewSignRequest;
import com.codebyte.signpdf.signature.CreateSignature;
import com.codebyte.signpdf.config.SignPDFCostant;
import com.codebyte.signpdf.config.SignPDFException;
import com.codebyte.signpdf.entities.CustomerSign;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Path("/api/private/sign-pdf")
public class SignPDFResource {

    @Inject
    PrivateKey privateKey;

    @Inject
    Certificate[] certificates;

    @Inject
    Font font;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ResponseStatus(200)
    @Operation(summary = "Sign PDF", description = "Sign a PDF file")
    @APIResponse(
            responseCode = "200",
            description = "PDF firmato",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM)
    )
    @APIResponse(
            responseCode = "500",
            description = "Server error"
    )
    @Transactional
    public File signPDF(@HeaderParam("customer-auth") String token,
                        @Valid NewSignRequest newSignRequest) throws SignPDFException {

        try {

            // recupero il customer con chiave passata
            CustomerSign customerSign = CustomerSign.<CustomerSign>find("token", token)
                    .firstResultOptional()
                    .orElseThrow(NotFoundException::new);

            // id random che rappresenta il pdf firmato
            String pdfID = RandomStringUtils.random(13, true, true).toUpperCase();

            // creazione logo firma con informazioni pdf e firmatario
            BufferedImage image = new BufferedImage(250, 60, BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.getGraphics();
            g.drawImage(ImageIO.read(SignPDFResource.class.getResource("/base_logo.jpg")), 0, 0, null);
            g.setFont(font);
            g.setColor(Color.BLACK);
            g.drawString("Certified " + customerSign.name, 50, 10);
            g.drawString("Signer " + newSignRequest.getCaller(), 50, 20);
            g.drawString("Date " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("y-M-d H:m:s")), 50, 30);
            g.drawString("PDFId " + pdfID, 50, 40);
            g.dispose();

            // MD5 logo con info firma
            String md5Logo;
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", os);
                try (InputStream is = new ByteArrayInputStream(os.toByteArray())) {
                    md5Logo = DigestUtils.md5Hex(is);
                }
            }

            // inserimento logo e firma
            File signedPDF = new File(FileUtils.getTempDirectory(), UUID.randomUUID() + ".pdf");
            try (PDDocument doc = Loader.loadPDF(newSignRequest.getPdf())) {

                for (int i = 0; i < doc.getNumberOfPages(); i++) {

                    PDPage page = doc.getPage(i);
                    PDImageXObject pdImage = LosslessFactory.createFromImage(doc, image);

                    try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        float scale = 0.8f;
                        contentStream.drawImage(pdImage, 5, 790, pdImage.getWidth() * scale, pdImage.getHeight() * scale);
                    }
                }

                CreateSignature signature = new CreateSignature(privateKey, certificates);
                signature.signDetached(doc, signedPDF, customerSign.name, SignPDFCostant.TSA_URL);
            }

            // Persist info firma eseguita
            PDFSigned pdfSigned = new PDFSigned();
            pdfSigned.firmatario = newSignRequest.getFirmatario();
            pdfSigned.uuidFirma = UUID.randomUUID().toString();
            pdfSigned.cellulareOTP = newSignRequest.getCellulareOTP();
            pdfSigned.caller = newSignRequest.getCaller();
            pdfSigned.md5 = md5Logo;
            pdfSigned.uuidFirma = pdfID;
            pdfSigned.customerSign = customerSign;
            pdfSigned.persist();

            return signedPDF;

        } catch (IOException e) {
            throw new SignPDFException(e);
        }
    }

}
