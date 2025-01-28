package com.starhealth.pdf_generation.dto;

import java.io.File;
import java.util.Map;

public class o1Request {

    private File pdfFile;
    private boolean masking;
    private boolean passwordProtection;
    private PasswordProtectionRequest passwordProtectionRequest;
    private MaskingRequest maskingRequest;

    public File getPdfFile() {
        return pdfFile;
    }

    public void setPdfFile(File pdfFile) {
        this.pdfFile = pdfFile;
    }

    public boolean isMasking() {
        return masking;
    }

    public void setMasking(boolean masking) {
        this.masking = masking;
    }

    public boolean isPasswordProtection() {
        return passwordProtection;
    }

    public void setPasswordProtection(boolean passwordProtection) {
        this.passwordProtection = passwordProtection;
    }

    public PasswordProtectionRequest getPasswordProtectionRequest() {
        return passwordProtectionRequest;
    }

    public void setPasswordProtectionRequest(PasswordProtectionRequest passwordProtectionRequest) {
        this.passwordProtectionRequest = passwordProtectionRequest;
    }

    public MaskingRequest getMaskingRequest() {
        return maskingRequest;
    }

    public void setMaskingRequest(MaskingRequest maskingRequest) {
        this.maskingRequest = maskingRequest;
    }

    public static class PasswordProtectionRequest {
        private String docType;
        private String lastName;
        private String firstName;
        private String birthdate;

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getBirthdate() {
            return birthdate;
        }

        public void setBirthdate(String birthdate) {
            this.birthdate = birthdate;
        }
    }

    public static class MaskingRequest {
        private String emailID;
        private String mobileNumber;
        private String dob;

        public String getEmailID() {
            return emailID;
        }

        public void setEmailID(String emailID) {
            this.emailID = emailID;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public void setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
        }

        public String getDob() {
            return dob;
        }

        public void setDob(String dob) {
            this.dob = dob;
        }
    }
}
