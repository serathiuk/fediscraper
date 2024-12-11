package dev.serathiuk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class Info {

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("languages")
    private List<String> languages;

    @JsonProperty("prohibited_content")
    private List<String> prohibitedContent;

    @JsonProperty("categories")
    private List<String> categories;

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