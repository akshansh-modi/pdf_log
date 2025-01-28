package com.starhealth.pdf_generation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.starhealth.pdf_generation.dto.PasswordProtectionRequest;
import com.starhealth.pdf_generation.dto.*;
import com.starhealth.pdf_generation.dto.MaskingRequest;
import com.starhealth.pdf_generation.utils.Constants;
import com.starhealth.pdf_generation.utils.Utils;
import com.sun.jna.WString;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.rendering.PDFRenderer;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import javax.imageio.ImageIO;
import javax.print.Doc;
import java.awt.geom.Rectangle2D;
import java.util.List;

@Service
public class o1service {

    public static byte[] passwordProtectPDF(File pdfFile, Map<String, String> inputParams) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            // Validate document type
            String docType = inputParams.getOrDefault("docType", "").trim();
            if (docType.isEmpty()) {
                throw new IllegalArgumentException("Document type must be specified.");
            }

            // Check if password protection is enabled
            String protect = inputParams.getOrDefault("protect", "false").trim();

            // System.out.println("entering protect if ");
            // Generate password based on document type
            String password = generatePassword(docType, inputParams);

            System.out.println(password);
            StandardProtectionPolicy protectionPolicy =getStandardProtectionPolicy(password, docType);
        
            document.protect(protectionPolicy);

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                document.save(byteArrayOutputStream);
                return byteArrayOutputStream.toByteArray();
            }
        } catch (IOException e) {
            // logger.error("Error processing PDF file: {}", e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            // logger.error(e.getMessage());
            return null;
        }

    }

    public PDDocument encryptPdf(PDDocument document, String ownerPassword) throws IOException {
        if (document == null || ownerPassword == null) {
            throw new IllegalArgumentException("Document and passwords must not be null.");
        }

        AccessPermission accessPermission = new AccessPermission();
        StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, ownerPassword, accessPermission);
        spp.setEncryptionKeyLength(128);
        spp.setPermissions(accessPermission);
        document.protect(spp);
        return document;
    }

    private static StandardProtectionPolicy getStandardProtectionPolicy(String password, String docType) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters for document type: " + docType);
        }


        AccessPermission accessPermission = new AccessPermission();
        accessPermission.setCanPrint(true); // Example: Allow printing
        accessPermission.setCanModify(false); // Example: Restrict editing
System.out.println("password   : "+ password);
        StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(password, password, accessPermission);
        protectionPolicy.setEncryptionKeyLength(128); // Use 128-bit encryption
        return protectionPolicy;
    }

    private static String generatePassword(String docType, Map<String, String> inputParams) {
        // System.out.println("\n doctype : " + docType);

        String firstName = inputParams.get("firstName");
        if(firstName==null){
            firstName="";
        }
        String lastName = inputParams.getOrDefault("lastName", "");
        if(lastName==null){
            lastName="";
        }
        String birthdate = inputParams.get("birthdate").replace("/", "");
        if(birthdate==null){
            birthdate="";
        }
        String mobilenumber = inputParams.get("mobileNumber");
        if(mobilenumber==null){
            mobilenumber="";
        }
        String namePart;


        if (!lastName.isEmpty()) {
            namePart = (firstName.length() >= 2 ? firstName.substring(0, 2) : firstName)
                    + (lastName.length() >= 2 ? lastName.substring(0, 2) : lastName);
        } else {
            namePart = firstName.length() >= 4 ? firstName.substring(0, 4) : firstName;
        }
        switch (docType.toLowerCase()) {
            case "proposal":
                if(!namePart.isEmpty()){
//                    return namePart + "-" + birthdate;
                    return namePart+(birthdate.isEmpty()?"":birthdate);
                }
                else{
                    return birthdate;
                }
            case "receipt":
                if (inputParams.containsKey("birthdate")) {
                  return birthdate;
                }
                break;
            case "smequotation":
//                return namePart + "-" + birthdate + "-" + mobilenumber;
                return namePart+birthdate+mobilenumber;
            case "policy":
                String day="";
                String month="";
                String year="";
                if(!birthdate.isEmpty()){
                     day = birthdate.substring(0, 2);
                     month = birthdate.substring(2, 4);
                     year = birthdate.substring(4);
                }
                if (!lastName.isEmpty()) {
                    String fNamePart = firstName.length() >= 2 ? firstName.substring(0, 2) : firstName;
                    String lNamePart = lastName.length() >= 2 ? lastName.substring(0, 2) : lastName;
                    namePart = day + month + fNamePart + lNamePart + year;
                } else {
                    String fNamePart = firstName.length() >= 4 ? firstName.substring(0, 4) : firstName;
                    namePart = day + month + fNamePart + year;
                }
                return namePart;

            default:
                return null;
        }
        return null;
    }

public static byte[] applySelectedMasking(File pdfFile, Map<String, String> maskingRequest) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

    
            ArrayList<Integer> pageIndices = getPagesToMask(document , maskingRequest);
            for (int pageIndex:pageIndices ) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 144);
                BufferedImage maskedImage = annotateAndMaskSpecificTextInImage(image, maskingRequest);
                replacePageWithMaskedImage(document, pageIndex, maskedImage);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            document.close();

            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            return null;
        }
    }

public  static  ArrayList<Integer> getPagesToMask(PDDocument document , Map<String,String> maskingRequest){

    ArrayList<Integer> pageIndices = new ArrayList<>();
    try {
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);

            for (String target : maskingRequest.values()) {
            System.out.print(target);
                if (pageText.contains(target)) {
                    pageIndices.add(i);
                    break;
                }
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return pageIndices;
}




    
    private static BufferedImage annotateAndMaskSpecificTextInImage(BufferedImage image,
            Map<String, String> maskingRequest) throws IOException {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Users\\v.akshansh.modi\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");

        java.util.List<net.sourceforge.tess4j.Word> words = tesseract.getWords(image,
                ITessAPI.TessPageIteratorLevel.RIL_WORD);
        Graphics2D g2d = image.createGraphics();
        final int uniformFontSize = 20;

        for (net.sourceforge.tess4j.Word word : words) {
            String wordText = word.getText();
            String maskedText = getMaskedText(wordText, maskingRequest);

            if (maskedText != null) {
                int x = word.getBoundingBox().x;
                int y = word.getBoundingBox().y;
                int width = word.getBoundingBox().width;
                int height = word.getBoundingBox().height;

                g2d.setColor(Color.WHITE);
                g2d.fillRect(x, y, width, height);

                g2d.setFont(new Font("Arial", Font.PLAIN, uniformFontSize));
                g2d.setColor(Color.BLACK);
                g2d.drawString(maskedText, x, y + height - (int) (height * 0.2));
            }
        }
        g2d.dispose();
        return image;
    }

    private static String getMaskedText(String text, Map<String, String> maskingRequest) {
        text = text.trim();
        for (Map.Entry<String, String> entry : maskingRequest.entrySet()) {
            String maskTarget = entry.getValue().trim();
            if (text.contains(maskTarget)) {
                if (entry.getKey().equalsIgnoreCase("emailId")) {
                    return maskEmail(maskTarget);
                } else if (entry.getKey().equalsIgnoreCase("dob")) {
                    return maskDOB(maskTarget);
                } else if (entry.getKey().equalsIgnoreCase("mobileNumber")) {
                    return maskPhoneNumber(maskTarget);
                }
            }
        }
        return null;
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex > 2) {
            String firstChar = email.substring(0, 1);
            String lastCharBeforeAt = email.substring(atIndex - 1, atIndex);
            String maskedChars = "x".repeat(atIndex - 2);
            return firstChar + maskedChars + lastCharBeforeAt + email.substring(atIndex);
        }
        return email;
    }

    public static String maskDOB(String dob) {
        String[] parts = dob.split("/");
        if (parts.length == 3) {
            return "XX/" + parts[1] + "/" + parts[2].substring(0, 2) + "XX";
        }

        // Check for the pattern '01-Jan-2002'
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
            LocalDate date = LocalDate.parse(dob, formatter);
            String maskedYear = date.getYear() / 100 + "XX";
            String maskedDOB = String.format("XX-%s-%s",
                    date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH), maskedYear);
            return maskedDOB;
        } catch (DateTimeParseException e) {
            // Handle the exception or return original dob if it doesn't match the expected
            // pattern
            return dob;
        }
    }

    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() > 6) {
            return "XXXXXX" + phoneNumber.substring(6);
        }
        return phoneNumber;
    }

    private static void replacePageWithMaskedImage(PDDocument document, int pageIndex, BufferedImage maskedImage)
            throws IOException {
        PDPage page = document.getPage(pageIndex);

        File tempFile = File.createTempFile("masked_page_" + pageIndex, ".png");
        ImageIO.write(maskedImage, "PNG", tempFile);

        PDImageXObject pdImage = PDImageXObject.createFromFileByExtension(tempFile, document);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.OVERWRITE, true, true)) {
            contentStream.drawImage(pdImage, 0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
        }

    }
}
