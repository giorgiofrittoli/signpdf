package com.codebyte.signpdf.producers;

import com.codebyte.signpdf.resources.SignPDFResource;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.awt.*;
import java.io.IOException;

@ApplicationScoped
public class FontProducer {

    Font font;

    @Startup
    void init() throws IOException, FontFormatException {
        font = Font.createFont(Font.TRUETYPE_FONT, SignPDFResource.class.getResourceAsStream("/DejaVuSansCondensed.ttf"))
                .deriveFont(10f);
    }

    @Produces
    public Font getFont() {
        return font;
    }

}
