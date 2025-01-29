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
import java.time.format.TextStyle;
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
            
            String protect = inputParams.getOrDefault("protect", "false").trim();
            
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
        accessPermission.setCanPrint(true); 
        accessPermission.setCanModify(false);
        System.out.println("password   : "+ password);
        StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(password, password, accessPermission);
        protectionPolicy.setEncryptionKeyLength(128); // Use 128-bit encryption
        return protectionPolicy;
    }

    private static String generatePassword(String docType, Map<String, String> inputParams) {
   

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

    public static byte[] applySelectedMasking(File pdfFile, Map<String, List<String>> maskingRequests) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            ArrayList<Integer> pageIndices = getPagesToMask(document, maskingRequests);
            for (int pageIndex : pageIndices) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, 144);
                BufferedImage maskedImage = annotateAndMaskSpecificTextInImage(image, maskingRequests);
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

    public static ArrayList<Integer> getPagesToMask(PDDocument document, Map<String, List<String>> maskingRequests) {
        ArrayList<Integer> pageIndices = new ArrayList<>();
        try {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);

                for (List<String> targets : maskingRequests.values()) {
                    for (String target : targets) {
                        System.out.println("Checking target: " + target);
                        if (pageText.contains(target)) {
                            System.out.println("Found target on page: " + i);
                            pageIndices.add(i);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pageIndices;
    }

    private static BufferedImage annotateAndMaskSpecificTextInImage(BufferedImage image, Map<String, List<String>> maskingRequests) throws IOException {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:/Users/v.akshansh.modi/AppData/Local/Programs/Tesseract-OCR/tessdata");

        java.util.List<net.sourceforge.tess4j.Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        Graphics2D g2d = image.createGraphics();

        for (net.sourceforge.tess4j.Word word : words) {
            String wordText = word.getText().trim();
            String maskedText = getMaskedText(wordText, maskingRequests);

            if (maskedText != null) {
                // Get bounding box dimensions
                int x = word.getBoundingBox().x;
                int y = word.getBoundingBox().y;
                int width = word.getBoundingBox().width;
                int height = word.getBoundingBox().height;

                // Mask the original text with a white rectangle
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x, y, width, height);

                // Dynamically calculate font size slightly larger than bounding box height
                float fontSize = height * 1.1f; // Scale factor to make text slightly larger
                Font currentFont = g2d.getFont();

                // Detect if the original text was bold (if such metadata is available)
                //boolean isBold = detectIfBold(word); // Implement this method based on your OCR library or metadata
                boolean isBold=false;
                // Set font style based on detected boldness
                Font scaledFont = currentFont.deriveFont(isBold ? Font.BOLD : Font.PLAIN, fontSize);
                g2d.setFont(scaledFont);

                // Use FontMetrics to adjust text positioning
                FontMetrics metrics = g2d.getFontMetrics(scaledFont);
                int textHeight = metrics.getAscent();

                // Draw the masked text at the top-left corner of the bounding box
                g2d.setColor(Color.BLACK);
                g2d.drawString(maskedText, x, y + textHeight - metrics.getDescent());

                System.out.println("Masked word: " + wordText + " with " + maskedText + " (Bold: " + isBold + ")");
            }
        }
        g2d.dispose();
        return image;
    }

    private static String getMaskedText(String text, Map<String, List<String>> maskingRequests) {
        for (Map.Entry<String, List<String>> entry : maskingRequests.entrySet()) {
            for (String maskTarget : entry.getValue()) {
                maskTarget = maskTarget.trim();
                if (text.contains(maskTarget)) {
                    switch (entry.getKey().toLowerCase()) {
                        case "emailids":
                            return maskEmail(maskTarget);
                        case "dobs":
                            return maskDOB(maskTarget);
                        case "mobilenumbers":
                            return maskPhoneNumber(maskTarget);
                    }
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
        try {
            // Define formatters for various patterns
            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("dd/MMM/yyyy", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
                    DateTimeFormatter.ofPattern("dd/MMM/yyyy", Locale.ENGLISH)
            };

            for (DateTimeFormatter formatter : formatters) {
                try {
                    LocalDate date = LocalDate.parse(dob, formatter);

                    // Masking logic for YYYY/MM/DD or YYYY-MM-DD
                    if (formatter.toString().contains("yyyy")) {
                        String maskedYear = date.getYear() % 100 + "XX";
                        return String.format("%s/%02d/XX",
                                maskedYear, date.getMonthValue());
                    }

                    // Masking logic for DD-MMM-YYYY or DD/MMM/YYYY
                    String maskedYear = (date.getYear() / 100) + "XX";
                    return String.format("XX-%s-%s",
                            date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), maskedYear);
                } catch (DateTimeParseException ignored) {
                }
            }

            // If date is in the format DD/JUL/YYYY
            if (dob.matches("\\d{2}/[A-Z]{3}/\\d{4}")) {
                String[] parts = dob.split("/");
                String maskedYear = parts[2].substring(2, 4) + "XX";
                return String.format("XX/%s/%s", parts[1], maskedYear);
            }

            // If date parsing fails, handle alternative format
            String[] parts = dob.split("[-/]");
            if (parts.length == 3) {
                return "XX/" + parts[1] + "/" + parts[2].substring(0, 2) + "XX";
            }
            return dob;

        } catch (Exception e) {
            return dob;
        }
    }


    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() > 6) {
            return "XXXXXX" + phoneNumber.substring(6);
        }
        return phoneNumber;
    }

    private static void replacePageWithMaskedImage(PDDocument document, int pageIndex, BufferedImage maskedImage) throws IOException {
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

