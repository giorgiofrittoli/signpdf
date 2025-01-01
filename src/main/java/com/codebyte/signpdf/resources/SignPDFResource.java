package com.codebyte.signpdf.resources;

import com.codebyte.signpdf.signature.CreateSignature;
import com.codebyte.signpdf.config.SignPDFCostant;
import com.codebyte.signpdf.config.SignPDFException;
import com.codebyte.signpdf.entities.CustomerSign;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
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
                        @FormParam("pdf") File pdf) throws SignPDFException {

        try {

            CustomerSign customerSign = CustomerSign.<CustomerSign>find("token", token)
                    .withLock(LockModeType.PESSIMISTIC_WRITE)
                    .firstResultOptional()
                    .orElseThrow(NotFoundException::new);

            File signedPDF = new File(FileUtils.getTempDirectory(), UUID.randomUUID() + ".pdf");

            CreateSignature signature = new CreateSignature(privateKey, certificates);
            signature.signDetached(pdf, signedPDF, SignPDFCostant.TSA_URL);

            customerSign.tot++;
            customerSign.persist();

            return signedPDF;

        } catch (IOException e) {
            throw new SignPDFException(e);
        }
    }

}
