package dev.serathiuk;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Instances {

    @JsonProperty("instances")
    private List<Instance> instances;

    @JsonProperty("pagination")
    private Pagination pagination;

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
