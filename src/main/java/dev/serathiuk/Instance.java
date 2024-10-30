package dev.serathiuk;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.util.Properties;

public class Instance {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("added_at")
    private String addedAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("checked_at")
    private String checkedAt;

    @JsonProperty("uptime")
    private int uptime;

    @JsonProperty("up")
    private boolean up;

    @JsonProperty("dead")
    private boolean dead;

    @JsonProperty("version")
    private String version;

    @JsonProperty("ipv6")
    private boolean ipv6;

    @JsonProperty("https_score")
    private int httpsScore;

    @JsonProperty("https_rank")
    private String httpsRank;

    @JsonProperty("obs_score")
    private int obsScore;

    @JsonProperty("obs_rank")
    private String obsRank;

    @JsonProperty("users")
    private String users;

    @JsonProperty("statuses")
    private String statuses;

    @JsonProperty("connections")
    private String connections;

    @JsonProperty("open_registrations")
    private boolean openRegistrations;

    @JsonProperty("info")
    private Info info;

    @JsonProperty("thumbnail")
    private String thumbnail;

    @JsonProperty("thumbnail_proxy")
    private String thumbnailProxy;

    @JsonProperty("active_users")
    private int activeUsers;

    @JsonProperty("email")
    private String email;

    @JsonProperty("admin")
    private String admin;

    public static void main(String[] args) throws IOException {
        ;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(String checkedAt) {
        this.checkedAt = checkedAt;
    }

    public int getUptime() {
        return uptime;
    }

    public void setUptime(int uptime) {
        this.uptime = uptime;
    }

    public boolean isUp() {
        return up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isIpv6() {
        return ipv6;
    }

    public void setIpv6(boolean ipv6) {
        this.ipv6 = ipv6;
    }

    public int getHttpsScore() {
        return httpsScore;
    }

    public void setHttpsScore(int httpsScore) {
        this.httpsScore = httpsScore;
    }

    public String getHttpsRank() {
        return httpsRank;
    }

    public void setHttpsRank(String httpsRank) {
        this.httpsRank = httpsRank;
    }

    public int getObsScore() {
        return obsScore;
    }

    public void setObsScore(int obsScore) {
        this.obsScore = obsScore;
    }

    public String getObsRank() {
        return obsRank;
    }

    public void setObsRank(String obsRank) {
        this.obsRank = obsRank;
    }

    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }

    public String getStatuses() {
        return statuses;
    }

    public void setStatuses(String statuses) {
        this.statuses = statuses;
    }

    public String getConnections() {
        return connections;
    }

    public void setConnections(String connections) {
        this.connections = connections;
    }

    public boolean isOpenRegistrations() {
        return openRegistrations;
    }

    public void setOpenRegistrations(boolean openRegistrations) {
        this.openRegistrations = openRegistrations;
    }

    public Info getInfo() {
        return info != null ? info : new Info();
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getThumbnailProxy() {
        return thumbnailProxy;
    }

    public void setThumbnailProxy(String thumbnailProxy) {
        this.thumbnailProxy = thumbnailProxy;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
}


