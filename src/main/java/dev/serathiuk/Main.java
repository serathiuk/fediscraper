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
    public static final TypeReference<List<DomainModeration>> VALUE_TYPE_REF = new TypeReference<>() {
    };

    public static final Duration DURATION = Duration.ofSeconds(5);
    public static final HttpResponse.BodyHandler<String> RESPONSE_BODY_HANDLER = HttpResponse.BodyHandlers.ofString();
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

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

        try(final var httpClient = HttpClient.newHttpClient();
            final var swInstance = new FileWriter(path.resolve("full_data.csv").toFile());
            final var printerInstances = new CSVPrinter(swInstance, csvInstances)) {

            String nextId = null;
            do {
                String url = "https://instances.social/api/1.0/instances/list?include_closed=true&include_dead=false&min_users=1&count=1000";

                if(nextId != null && !nextId.isEmpty()) {
                    url += "&min_id=" + nextId;
                }

                LOGGER.info("Fetching instances.... Next Id: " + nextId);
                var req = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .GET()
                        .timeout(Duration.ofSeconds(10))
                        .header(AUTHORIZATION, BEARER +key)
                        .build();

                var response = httpClient.send(req, RESPONSE_BODY_HANDLER);

                var instances = objectMapper.readValue(response.body(), Instances.class);

                LOGGER.info("Writing CSV files...");
                writeCSV(instances.getInstances(), printerInstances);

                nextId = instances.getPagination().getNextId();

                swInstance.flush();
            } while (nextId != null && !nextId.isEmpty());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeCSV(List<Instance> instances, CSVPrinter printerInstances) throws IOException {
        LOGGER.info("Printing instances...");
        System.gc();

        instances.parallelStream().forEach(instance -> {
            try {
                processInstance(printerInstances, instance);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void processInstance(CSVPrinter printerInstances, Instance instance) throws IOException {
        var languages_aux = instance.getInfo().getLanguages();
        var prohibitedContents_aux = instance.getInfo().getProhibitedContent();
        var categories_aux = instance.getInfo().getCategories();

        var languages = languages_aux != null && !languages_aux.isEmpty() ? languages_aux : UNKNOWN_LIST;
        var prohibitedContents = prohibitedContents_aux != null && !prohibitedContents_aux.isEmpty() ? prohibitedContents_aux : UNKNOWN_LIST;
        var categories = categories_aux != null && !categories_aux.isEmpty() ? categories_aux : UNKNOWN_LIST;
        List<DomainModeration> domainsModeration = null;

        if(!instance.isDead()) {
            try (final var httpClient = HttpClient.newHttpClient();) {
                var req = HttpRequest.newBuilder()
                        .uri(new URI("https://" + instance.getName() + "/api/v1/instance/domain_blocks"))
                        .GET()
                        .timeout(DURATION)
                        .build();

                var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    return;
                }

                try {
                    domainsModeration = objectMapper.readValue(response.body(), VALUE_TYPE_REF)
                            .stream()
                            .filter(d -> d.getDomain() != null && !d.getDomain().trim().isEmpty())
                            .toList();
                } catch (Exception e) {
                    LOGGER.severe("Error processing blocks for " + instance.getName());
                    LOGGER.severe("Response: " + response.body());
                }
            } catch (ConnectException e) {
                LOGGER.warning("Connection Refused: " + instance.getName());
            } catch (HttpTimeoutException e) {
                LOGGER.warning("Connection Timeout: " + instance.getName());
            } catch (Exception e) {
                LOGGER.severe("Error fetching blocks for " + instance.getName());
            }
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