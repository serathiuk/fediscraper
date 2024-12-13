package dev.serathiuk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static final String BLANK = "";
    private static final List<String> UNKNOWN_LIST = List.of(BLANK);
    private static final List<DomainModeration> BLANK_DOMAIN_MODERATION;
    private static final String SUSPEND_STR = "suspend";;

    private static final ReentrantLock lock = new ReentrantLock();

    static {
        var domain = new DomainModeration();
        domain.setDomain(BLANK);
        domain.setSeverity(BLANK);
        domain.setComment(BLANK);
        BLANK_DOMAIN_MODERATION = List.of(domain);
    }

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

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



        var csvInstances = CSVFormat.DEFAULT.builder()
                .setHeader("instance", "added_at", "updated_at", "checked_at", "uptimwe", "up", "dead", "version", "ipv6",
                        "https_score", "https_rank", "obs_score", "obs_rank", "users", "statuses", "connections", "open_registrations",
                        "topic", "language", "prohibited_content", "category", "suspends", "silences", "comment")
                .build();

        var instancesList = new ArrayList<Instance>();

        try(final var httpClient = HttpClient.newHttpClient();
            final var swInstance = new FileWriter(path.resolve("full_data.csv").toFile());
            final var printerInstances = new CSVPrinter(swInstance, csvInstances)) {

            String nextId = null;
            do {
                String url = "https://instances.social/api/1.0/instances/list?include_closed=true&include_dead=true&min_users=1&count=5000";

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
            writeCSV(instancesList, printerInstances);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeCSV(List<Instance> instances, CSVPrinter printerInstances) throws IOException {
        LOGGER.info("Printing instances...");

        Map<String, Instance> mapInstances = instances.parallelStream()
                        .collect(Collectors.toMap(Instance::getName, instance -> instance));

        try(var executor = Executors.newFixedThreadPool(16)) {
            IntStream.range(0, instances.size()).forEach(i -> {
                executor.submit(() -> {
                    try {
                        processInstance(i, printerInstances, instances.get(i), mapInstances);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        }
    }

    private static void processInstance(int number, CSVPrinter printerInstances, Instance instance, Map<String, Instance> mapInstances) throws IOException {
        var languages_aux = instance.getInfo().getLanguages();
        var prohibitedContents_aux = instance.getInfo().getProhibitedContent();
        var categories_aux = instance.getInfo().getCategories();

        var languages = languages_aux != null && !languages_aux.isEmpty() ? languages_aux : UNKNOWN_LIST;
        var prohibitedContents = prohibitedContents_aux != null && !prohibitedContents_aux.isEmpty() ? prohibitedContents_aux : UNKNOWN_LIST;
        var categories = categories_aux != null && !categories_aux.isEmpty() ? categories_aux : UNKNOWN_LIST;
        List<DomainModeration> domainsModeration = null;

        if(number % 1000 == 0) {
            System.out.println("Running GC...");
            System.gc();
        }

        if(!instance.isDead() && instance.isUp()) {
            try (final var httpClient = HttpClient.newHttpClient();) {
                LOGGER.info(number+ "/"+mapInstances.size()+" - Searching blocks for " + instance.getName());

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
                            .filter(d -> d.getDomain() != null && !d.getDomain().trim().isEmpty())
                            .toList();
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
        } else {
            LOGGER.info(number+ "/"+mapInstances.size()+" - Instance is dead or down: " + instance.getName());
        }

        if(domainsModeration == null || domainsModeration.isEmpty()) {
            domainsModeration = BLANK_DOMAIN_MODERATION;
        }

        var blocks = BLANK;
        var silences = BLANK;
        for (var language : languages) {
            for (var prohibitedContent : prohibitedContents) {
                for (var category : categories) {
                    for (var domainModeration : domainsModeration) {
                        blocks = BLANK;
                        silences = BLANK;

                        if(SUSPEND_STR.equalsIgnoreCase(domainModeration.getSeverity())) {
                            blocks = domainModeration.getDomain();
                        } else {
                            silences = domainModeration.getDomain();
                        }


                        lock.lock();
                        try {

                            printerInstances.printRecord(instance.getName(), instance.getAddedAt(), instance.getUpdatedAt(), instance.getCheckedAt(),
                                    instance.getUptime(), instance.isUp(), instance.isDead(), instance.getVersion(), instance.isIpv6(), instance.getHttpsScore(),
                                    instance.getHttpsRank(), instance.getObsScore(), instance.getObsRank(), instance.getUsers(), instance.getStatuses(),
                                    instance.getConnections(), instance.isOpenRegistrations(), instance.getInfo().getTopic(), language, prohibitedContent, category,
                                    blocks, silences, domainModeration.getComment());
                        } finally {
                            lock.unlock();
                        }

                    }
                }
            }
        }
    }
}