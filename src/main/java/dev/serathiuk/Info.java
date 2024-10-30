package dev.serathiuk;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class Info {

    @JsonProperty("short_description")
    private String shortDescription;

    @JsonProperty("full_description")
    private String fullDescription;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("languages")
    private List<String> languages;

    @JsonProperty("other_languages_accepted")
    private boolean otherLanguagesAccepted;

    @JsonProperty("federates_with")
    private String federatesWith;

    @JsonProperty("prohibited_content")
    private List<String> prohibitedContent;

    @JsonProperty("categories")
    private List<String> categories;

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public List<String> getLanguages() {
        return languages != null ? languages : List.of();
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public boolean isOtherLanguagesAccepted() {
        return otherLanguagesAccepted;
    }

    public void setOtherLanguagesAccepted(boolean otherLanguagesAccepted) {
        this.otherLanguagesAccepted = otherLanguagesAccepted;
    }

    public String getFederatesWith() {
        return federatesWith;
    }

    public void setFederatesWith(String federatesWith) {
        this.federatesWith = federatesWith;
    }

    public List<String> getProhibitedContent() {
        return prohibitedContent != null ? prohibitedContent : List.of();
    }

    public void setProhibitedContent(List<String> prohibitedContent) {
        this.prohibitedContent = prohibitedContent;
    }

    public List<String> getCategories() {
        return categories != null ? categories : List.of();
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}