package dev.serathiuk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final  ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        var properties = new Properties();
        properties.load(Instances.class.getClassLoader().getResourceAsStream("config.properties"));

        var path = Path.of(properties.get("app.files.path").toString());
        var key = properties.get("instances.social.key").toString();

        System.out.println("Path: "+path);
        System.out.println("Key: "+key);

        Files.createDirectories(path);
        Files.deleteIfExists(path.resolve("full_data.csv"));

        var swInstance = new StringWriter();

        var csvInstances = CSVFormat.DEFAULT.builder()
                .setHeader("instance", "added_at", "updated_at", "checked_at", "uptimwe", "up", "dead", "version", "ipv6",
                        "https_score", "https_rank", "obs_score", "obs_rank", "users", "statuses", "connections", "open_registrations",
                        "topic", "up", "dead", "language", "prohibited_content", "category", "moderated_domain", "moderation_severitiy", "moderation_comment")
                .build();

        var instancesList = new ArrayList<Instance>();
        try(final var httpClient = HttpClient.newHttpClient();
            final var printerInstances = new CSVPrinter(swInstance, csvInstances)) {

            String nextId = null;
            do {
                String url = "https://instances.social/api/1.0/instances/list?include_closed=true&include_dead=true&count=1000";

                if(nextId != null && !nextId.isEmpty()) {
                    url += "&min_id=" + nextId;
                }

                LOGGER.info("Fetching instances.... Next Id: " + nextId);
                var req = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer "+key)
                        .build();

                var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                var instances = objectMapper.readValue(response.body(), Instances.class);
                instancesList.addAll(instances.getInstances());



                nextId = instances.getPagination().getNextId();
            } while (nextId != null && !nextId.isEmpty());

            LOGGER.info("Writing CSV files...");
            writeCSV(instancesList, httpClient, printerInstances);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Writing CSV files...");
        Files.write(path.resolve("full_data.csv"), swInstance.toString().getBytes());
    }

    private static void writeCSV(List<Instance> instances, HttpClient httpClient, CSVPrinter printerInstances) throws IOException {
        LOGGER.info("Printing instances...");

        Map<String, Instance> mapInstances = instances.parallelStream()
                        .collect(Collectors.toMap(Instance::getName, instance -> instance));

        instances.parallelStream().forEach(instance -> {

            try {
                processInstance(printerInstances, instance, httpClient, mapInstances);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void processInstance(CSVPrinter printerInstances, Instance instance, HttpClient httpClient, Map<String, Instance> mapInstances) throws IOException {
        var languages = instance.getInfo().getLanguages() != null ? instance.getInfo().getLanguages() : List.of("unknown");
        var prohibitedContents = instance.getInfo().getProhibitedContent() != null ? instance.getInfo().getProhibitedContent() : List.of("unknown");
        var categories = instance.getInfo().getCategories() != null ? instance.getInfo().getCategories() : List.of("unknown");
        List<DomainModeration> domainsModeration = List.of();

        if(!instance.isDead() && instance.isUp()) {
            try {
                LOGGER.info("Searching blocks for " + instance.getName());

                var req = HttpRequest.newBuilder()
                        .uri(new URI("https://" + instance.getName() + "/api/v1/instance/domain_blocks"))
                        .GET()
                        .timeout(Duration.ofSeconds(3))
                        .build();

                var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    LOGGER.warning("Error fetching blocks for " + instance.getName());
                    return;
                }

                try {
                    domainsModeration = objectMapper.readValue(response.body(), new TypeReference<List<DomainModeration>>() {})
                            .stream()
                            .filter(domainModeration -> mapInstances.containsKey(domainModeration.getDomain()))
                            .collect(Collectors.toList());

                } catch (Exception e) {
                    LOGGER.severe("Error processing blocks for " + instance.getName());
                    LOGGER.severe("Response: " + response.body());
                    e.printStackTrace();
                }
            } catch (ConnectException e) {
                LOGGER.warning("Connection Refused: " + instance.getName());
            } catch (HttpTimeoutException e) {
                LOGGER.warning("Connection Timeout: " + instance.getName());
            } catch (Exception e) {
                LOGGER.severe("Error fetching blocks for " + instance.getName());
                e.printStackTrace();
            }
        }

        if(domainsModeration.isEmpty()) {
            var domain = new DomainModeration();
            domain.setDomain("none");
            domain.setSeverity("unknown");
            domain.setComment("none");
            domain.setSeverity_ex("unknown");
            domainsModeration = List.of(domain);
        }

        LOCK.lock();
        try {
            for (var language : languages) {
                for (var prohibitedContent : prohibitedContents) {
                    for (var category : categories) {
                        for (var domainModeration : domainsModeration) {
                            printerInstances.printRecord(instance.getName(), instance.getAddedAt(), instance.getUpdatedAt(), instance.getCheckedAt(),
                                    instance.getUptime(), instance.isUp(), instance.isDead(), instance.getVersion(), instance.isIpv6(), instance.getHttpsScore(),
                                    instance.getHttpsRank(), instance.getObsScore(), instance.getObsRank(), instance.getUsers(), instance.getStatuses(),
                                    instance.getConnections(), instance.isOpenRegistrations(), instance.getInfo().getTopic(), instance.isUp(),
                                    instance.isDead(), language, prohibitedContent, category,
                                    domainModeration.getDomain(), domainModeration.getSeverity(), domainModeration.getComment());
                        }
                    }
                }
            }
        } finally {
            LOCK.unlock();
        }
    }
}