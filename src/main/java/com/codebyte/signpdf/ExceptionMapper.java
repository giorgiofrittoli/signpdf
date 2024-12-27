package com.codebyte.signpdf;

import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ExceptionMapper {

    @ServerExceptionMapper
    public Response toResponse(SignPDFException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }

}
