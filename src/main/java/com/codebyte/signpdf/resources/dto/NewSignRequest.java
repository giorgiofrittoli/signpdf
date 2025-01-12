package com.codebyte.signpdf.resources.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class NewSignRequest {

    @FormParam("pdf")
    @NotNull
    private File pdf;
    @FormParam("firmatario")
    @NotEmpty
    private String firmatario;
    @FormParam("cellulareOtp")
    @NotEmpty
    private String cellulareOTP;
    @FormParam("caller")
    @NotEmpty
    private String caller;
}
