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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final ReentrantLock LOCK = new ReentrantLock();

    public static void main(String[] args) throws IOException {
        var properties = new Properties();
        properties.load(Instances.class.getClassLoader().getResourceAsStream("config.properties"));

        var path = Path.of(properties.get("app.files.path").toString());
        var key = properties.get("instances.social.key").toString();


        Files.createDirectories(path);
        Files.deleteIfExists(path.resolve("instances.csv"));
        Files.deleteIfExists(path.resolve("languages.csv"));
        Files.deleteIfExists(path.resolve("prohibited_content.csv"));
        Files.deleteIfExists(path.resolve("categories.csv"));
        Files.deleteIfExists(path.resolve("blocks.csv"));

        var swInstance = new StringWriter();
        var swLanguages = new StringWriter();
        var swProhibitedContent = new StringWriter();
        var swCategories = new StringWriter();
        var swBlocks = new StringWriter();

        var csvInstances = CSVFormat.DEFAULT.builder()
                .setHeader("instance", "added_at", "updated_at", "checked_at", "uptime", "up", "dead", "version", "ipv6",
                        "https_score", "https_rank", "obs_score", "obs_rank", "users", "statuses", "connections", "open_registrations",
                        "topic")
                .build();

        var csvLanguages = CSVFormat.DEFAULT.builder()
                .setHeader("instance", "language")
                .build();

        var csvProhibitedContent = CSVFormat.DEFAULT.builder()
                .setHeader("instance", "prohibited_content")
                .build();

        var csvCategories = CSVFormat.DEFAULT.builder()
                .setHeader("instance", "category")
                .build();

        var csvBlocks = CSVFormat.DEFAULT.builder()
                .setHeader("instance", "domain", "severity", "comment")
                .build();

        try(final var httpClient = HttpClient.newHttpClient();
            final var printerInstances = new CSVPrinter(swInstance, csvInstances);
            final var printerLanguages = new CSVPrinter(swLanguages, csvLanguages);
            final var printerProhibitedContent = new CSVPrinter(swProhibitedContent, csvProhibitedContent);
            final var printerCategories = new CSVPrinter(swCategories, csvCategories);
            final var printerBlocks = new CSVPrinter(swBlocks, csvBlocks)) {

            String nextId = null;
            do {
                String url = "https://instances.social/api/1.0/instances/list?include_closed=false&include_dead=false&count=1000";

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

                ObjectMapper objectMapper = new ObjectMapper();
                var instances = objectMapper.readValue(response.body(), Instances.class);

                LOGGER.info("Writing CSV files...");
                writeCSV(instances.getInstances(), printerInstances, printerLanguages, printerProhibitedContent, printerCategories);

                LOGGER.info("Searching blocks...");
                instances.getInstances()
                        .parallelStream()
                        .forEach(proccessInstanceData(httpClient, objectMapper, printerBlocks));

                nextId = instances.getPagination().getNextId();
            } while (nextId != null && !nextId.isEmpty());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        LOGGER.info("Writing CSV files...");
        Files.write(path.resolve("instances.csv"), swInstance.toString().getBytes());
        Files.write(path.resolve("languages.csv"), swLanguages.toString().getBytes());
        Files.write(path.resolve("prohibited_content.csv"), swProhibitedContent.toString().getBytes());
        Files.write(path.resolve("categories.csv"), swCategories.toString().getBytes());
        Files.write(path.resolve("blocks.csv"), swBlocks.toString().getBytes());
    }

    private static Consumer<Instance> proccessInstanceData(HttpClient httpClient, ObjectMapper objectMapper, CSVPrinter printer) {
        return instance -> {
            try {
                LOGGER.info("Searching blocks for " + instance.getName());

                var req = HttpRequest.newBuilder()
                        .uri(new URI("https://" + instance.getName() + "/api/v1/instance/domain_blocks"))
                        .GET()
                        .timeout(Duration.ofSeconds(5))
                        .build();

                var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    LOGGER.warning("Error fetching blocks for " + instance.getName());
                    return;
                }

                LOCK.lock();
                try {
                    List<DomainModeration> domainsModeration = objectMapper.readValue(response.body(), new TypeReference<List<DomainModeration>>() {
                    });
                    for (var domainModeration : domainsModeration) {
                        printer.printRecord(instance.getName(), domainModeration.getDomain(), domainModeration.getSeverity(), domainModeration.getComment());
                    }
                }catch (Exception e) {
                    LOGGER.severe("Error processing blocks for " + instance.getName());
                    LOGGER.severe("Response: "+response.body());
                    e.printStackTrace();
                } finally {
                    LOCK.unlock();
                }
            } catch (ConnectException e) {
                LOGGER.warning("Connection Refused: " + instance.getName());
            } catch (HttpTimeoutException e) {
                LOGGER.warning("Connection Timeout: " + instance.getName());
            } catch (Exception e) {
                LOGGER.severe("Error fetching blocks for " + instance.getName());
                e.printStackTrace();
            }
        };
    }

    private static void writeCSV(List<Instance> instances, CSVPrinter printerInstances, CSVPrinter printerLanguages, CSVPrinter printerProhibitedContent, CSVPrinter printerCategories) throws IOException {
        LOGGER.info("Printing instances...");
        for (var instance : instances) {
            printerInstances.printRecord(instance.getName(), instance.getAddedAt(), instance.getUpdatedAt(), instance.getCheckedAt(),
                    instance.getUptime(), instance.isUp(), instance.isDead(), instance.getVersion(), instance.isIpv6(), instance.getHttpsScore(),
                    instance.getHttpsRank(), instance.getObsScore(), instance.getObsRank(), instance.getUsers(), instance.getStatuses(),
                    instance.getConnections(), instance.isOpenRegistrations(), instance.getInfo().getTopic());

            LOGGER.info("Printing languages for "+instance.getName());
            for(var language : instance.getInfo().getLanguages()) {
                printerLanguages.printRecord(instance.getName(), language);
            }

            LOGGER.info("Printing prohibited content for "+instance.getName());
            for(var prohibitedContent : instance.getInfo().getProhibitedContent()) {
                printerProhibitedContent.printRecord(instance.getName(), prohibitedContent);
            }

            LOGGER.info("Printing categories for "+instance.getName());
            for(var category : instance.getInfo().getCategories()) {
                printerCategories.printRecord(instance.getName(), category);
            }
        }
    }
}