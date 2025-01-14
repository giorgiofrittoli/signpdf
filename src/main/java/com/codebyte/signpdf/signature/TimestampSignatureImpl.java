package com.codebyte.signpdf.signature;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;

import java.io.IOException;
import java.io.InputStream;

public class TimestampSignatureImpl implements SignatureInterface {
    private final TSAClient tsaClient;

    public TimestampSignatureImpl(TSAClient tsaClient) {
        super();
        this.tsaClient = tsaClient;
    }

    @Override
    public byte[] sign(InputStream paramInputStream) throws IOException {
        return tsaClient.getTimeStampToken(paramInputStream).getEncoded();
    }
}