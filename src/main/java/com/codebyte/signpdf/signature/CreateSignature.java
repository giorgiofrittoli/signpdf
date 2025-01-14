/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codebyte.signpdf.signature;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Calendar;

public class CreateSignature extends CreateSignatureBase {


    public CreateSignature(PrivateKey privateKey, Certificate[] certificateChain) {
        super(privateKey, certificateChain);
    }

    public void signDetached(PDDocument document, File outFile, String signedBy, String tsaUrl) throws IOException {

        try (FileOutputStream fos = new FileOutputStream(outFile)) {

            setTsaUrl(tsaUrl);

            // call SigUtils.checkCrossReferenceTable(document) if Adobe complains
            // and read https://stackoverflow.com/a/71293901/535646
            // and https://issues.apache.org/jira/browse/PDFBOX-5382

            int accessPermissions = SigUtils.getMDPPermission(document);
            if (accessPermissions == 1) {
                throw new IllegalStateException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
            }

            // create signature dictionary
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signedBy);
            // the signing date, needed for valid signature
            signature.setSignDate(Calendar.getInstance());

            // Optional: certify
            if (accessPermissions == 0) {
                SigUtils.setMDPPermission(document, signature, 2);
            }

            SignatureOptions signatureOptions = new SignatureOptions();
            // Size can vary, but should be enough for purpose.
            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
            // register signature dictionary and sign interface
            document.addSignature(signature, this, signatureOptions);
            // write incremental (only for signing purpose)
            document.saveIncremental(fos);

        }
    }

    public void signWTimestamp(File inFile, File outFile, String tsaUrl) throws IOException, NoSuchAlgorithmException {
        if (inFile == null || !inFile.exists()) {
            throw new FileNotFoundException("Document for signing does not exist");
        }

        setTsaUrl(tsaUrl);

        // sign
        try (FileOutputStream fos = new FileOutputStream(outFile);
             PDDocument doc = Loader.loadPDF(inFile)) {

            // call SigUtils.checkCrossReferenceTable(document) if Adobe complains
            // and read https://stackoverflow.com/a/71293901/535646
            // and https://issues.apache.org/jira/browse/PDFBOX-5382

            int accessPermissions = SigUtils.getMDPPermission(doc);
            if (accessPermissions == 1) {
                throw new IllegalStateException("No changes to the document are permitted due to DocMDP transform parameters dictionary");
            }

            // create signature dictionary
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setSubFilter(COSName.getPDFName("ETSI.RFC3161"));
            signature.setSignDate(Calendar.getInstance());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            TSAClient tsaClient = new TSAClient(new URL(this.getTsaUrl()), null, null, digest);

            doc.addSignature(signature, new TimestampSignatureImpl(tsaClient));
            doc.saveIncremental(fos);
        }
    }
}
