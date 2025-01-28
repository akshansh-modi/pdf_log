package com.starhealth.pdf_generation.controller;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starhealth.pdf_generation.dto.o1Request;
import com.starhealth.pdf_generation.service.PdfService;
import com.starhealth.pdf_generation.service.o1service;
import com.starhealth.pdf_generation.validation.ValidUnifiedRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
@ValidUnifiedRequest
@RestController
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping(value = "/getMaskAndProtectPdfUsingPdfInput", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ByteArrayResource> processPDF(@RequestPart("file") MultipartFile file,
                                                        @RequestPart("request") String requestJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            o1Request request = objectMapper.readValue(requestJson, o1Request.class);

            File pdfFile = convertMultiPartToFile(file);
            byte[] maskedPdfBytes = null;
            byte[] outputBytes = null;

            // Apply masking if requested
            if (request.isMasking()) {
                logWithTimestamp("Entering masking");
                maskedPdfBytes = o1service.applyMasking(pdfFile, buildMaskingParams(request.getMaskingRequest()));
                outputBytes = maskedPdfBytes;
                logWithTimestamp("Masking completed");
            }

            // If masking is applied, use masked PDF for password protection
            if (maskedPdfBytes != null && request.isPasswordProtection()) {
                logWithTimestamp("Entering pdf protection");
                File maskedPdfFile = convertByteArrayToFile(maskedPdfBytes, "masked_temp.pdf");
                outputBytes = o1service.passwordProtectPDF(maskedPdfFile, buildProtectionParams(request.getPasswordProtectionRequest()));
                logWithTimestamp("Pdf protection completed");
            }
            // If masking not requested but password protection is requested
            else if (!request.isMasking() && request.isPasswordProtection()) {
                logWithTimestamp("Entering pdf protection");
                outputBytes = o1service.passwordProtectPDF(pdfFile, buildProtectionParams(request.getPasswordProtectionRequest()));
                logWithTimestamp("Pdf protection completed");
            }

            // No processing if neither masking nor password protection is requested
            if (outputBytes == null) {
                outputBytes = maskedPdfBytes != null ? maskedPdfBytes : convertFileToByteArray(pdfFile);
            }

            ByteArrayResource resource = new ByteArrayResource(outputBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "processed_" + file.getOriginalFilename() + ".pdf");
            return new ResponseEntity<>(resource, headers, HttpStatus.OK);
        } catch (Exception e) {
            logWithTimestamp("Error processing request: " + e.getMessage());
            return new ResponseEntity<>(new ByteArrayResource(("Error processing request: " + e.getMessage()).getBytes()), HttpStatus.BAD_REQUEST);
        }
    }

    private void logWithTimestamp(String message) {
        String logMessage = new Date() + ": " + message;
        System.out.println(logMessage);
        try (FileWriter fw = new FileWriter("logs.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(logMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("temp", null);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    private File convertByteArrayToFile(byte[] data, String fileName) throws IOException {
        File file = File.createTempFile(fileName, null);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
        return file;
    }

    private byte[] convertFileToByteArray(File file) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private Map<String, String> buildMaskingParams(o1Request.MaskingRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("emailID", request.getEmailID());
        params.put("mobileNumber", request.getMobileNumber());
        params.put("dob", request.getDob());
        return params;
    }

    private Map<String, String> buildProtectionParams(o1Request.PasswordProtectionRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("docType", request.getDocType());
        params.put("lastName", request.getLastName());
        params.put("firstName", request.getFirstName());
        params.put("birthdate", request.getBirthdate());
        return params;
    }
}
