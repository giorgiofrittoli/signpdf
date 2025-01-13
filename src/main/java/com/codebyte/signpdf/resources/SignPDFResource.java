package com.codebyte.signpdf.resources;

import com.codebyte.signpdf.entities.PDFSigned;
import com.codebyte.signpdf.resources.dto.NewSignRequest;
import com.codebyte.signpdf.signature.CreateSignature;
import com.codebyte.signpdf.config.SignPDFCostant;
import com.codebyte.signpdf.config.SignPDFException;
import com.codebyte.signpdf.entities.CustomerSign;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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

            CustomerSign customerSign = CustomerSign.<CustomerSign>find("token", token)
                    .withLock(LockModeType.PESSIMISTIC_WRITE)
                    .firstResultOptional()
                    .orElseThrow(NotFoundException::new);

            File pdfWithLogo = new File(FileUtils.getTempDirectory(), UUID.randomUUID() + ".pdf");

            String pdfID = RandomStringUtils.random(13, true, true).toUpperCase();

            BufferedImage image = new BufferedImage(250, 70, BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.getGraphics();
            g.drawImage(ImageIO.read(SignPDFResource.class.getResource("/cob.jpg")), 0, 0, null);
            g.setFont(g.getFont().deriveFont(10f));
            g.setColor(Color.BLACK);
            g.drawString("Certified Firmapdf.net", 55, 10);
            g.drawString("Signer " + newSignRequest.getCaller(), 55, 20);
            g.drawString("Date " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("y-M-d H:m:s")), 55, 30);
            g.drawString("PDFId " + pdfID, 55, 40);
            g.dispose();

            File logoForSign = new File("/Users/giorgio/Downloads/", UUID.randomUUID() + ".png");
            ImageIO.write(image, "png", logoForSign);

            String md5Logo;
            try (InputStream is = Files.newInputStream(logoForSign.toPath())) {
                md5Logo = DigestUtils.md5Hex(is);
            }

            try (PDDocument doc = Loader.loadPDF(newSignRequest.getPdf())) {

                for (int i = 0; i < doc.getNumberOfPages(); i++) {
                    //we will add the image to the first page.
                    PDPage page = doc.getPage(i);
                    // createFromFile is the easiest way with an image file
                    // if you already have the image in a BufferedImage,
                    // call LosslessFactory.createFromImage() instead
                    PDImageXObject pdImage = PDImageXObject.createFromFile(logoForSign.getAbsolutePath(), doc);

                    try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                        // contentStream.drawImage(ximage, 20, 20 );
                        // better method inspired by http://stackoverflow.com/a/22318681/535646
                        // reduce this value if the image is too large
                        float scale = 1f;
                        contentStream.drawImage(pdImage, 0, 770, 250, 70);
                    }
                }

                doc.save(pdfWithLogo);
            }

            File signedPDF = new File(FileUtils.getTempDirectory(), UUID.randomUUID() + ".pdf");

            CreateSignature signature = new CreateSignature(privateKey, certificates);
            signature.signDetached(pdfWithLogo, signedPDF, SignPDFCostant.TSA_URL);

            PDFSigned pdfSigned = new PDFSigned();
            pdfSigned.firmatario = newSignRequest.getFirmatario();
            pdfSigned.uuidFirma = UUID.randomUUID().toString();
            pdfSigned.cellulareOTP = newSignRequest.getCellulareOTP();
            pdfSigned.caller = newSignRequest.getCaller();
            pdfSigned.md5 = md5Logo;
            pdfSigned.uuidFirma = pdfID.toString();
            pdfSigned.customerSign = customerSign;
            pdfSigned.persist();

            return signedPDF;

        } catch (IOException e) {
            throw new SignPDFException(e);
        }
    }

}
