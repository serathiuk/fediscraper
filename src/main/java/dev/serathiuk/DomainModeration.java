package dev.serathiuk;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DomainModeration {

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("digest")
    private String digest;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("severity_ex")
    private String severity_ex;

    @JsonProperty("comment")
    private String comment;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSeverity_ex() {
        return severity_ex;
    }

    public void setSeverity_ex(String severity_ex) {
        this.severity_ex = severity_ex;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
