package com.codebyte.signpdf.resources;

import com.codebyte.signpdf.SignPDFException;
import com.codebyte.signpdf.service.SignPDFService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.io.File;

@Path("/api/private/sign-pdf")
public class SignPDFResource {

    @Inject
    SignPDFService signPDFService;

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
    public File signPDF(@HeaderParam("customer-auth") String token, @FormParam("pdf") File pdf) throws SignPDFException {
        return signPDFService.signPDF(token, pdf);
    }

}
