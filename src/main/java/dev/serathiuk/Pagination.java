package dev.serathiuk;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pagination {

    @JsonProperty("total")
    private int total;

    @JsonProperty("next_id")
    private String nextId;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getNextId() {
        return nextId;
    }

    public void setNextId(String nextId) {
        this.nextId = nextId;
    }
}
