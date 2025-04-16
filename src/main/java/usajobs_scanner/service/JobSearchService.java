package usajobs_scanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class JobSearchService {
    private final WebClient.Builder webClientBuilder;
    private final Logger logger = LoggerFactory.getLogger(JobSearchService.class);

    @Value("${usajobs.user-agent}")
    private String userAgent;

    @Value("${usajobs.api-key}")
    private String apiKey;

    public void searchJobs(String[] keywords, int days) {
        WebClient client = webClientBuilder.baseUrl("https://data.usajobs.gov/api/search").build();

        String joinedKeyWords = String.join("+", keywords);
        String url = "?Keyword=" + URLEncoder.encode(joinedKeyWords, StandardCharsets.UTF_8);

        try (PrintWriter writer = new PrintWriter(new FileWriter("vacancies-usa-gov.csv"))) {
            writer.println("Title,Agency,Location,URL");

            JsonNode json = client.get()
                    .uri(url)
                    .header("User-Agent", userAgent)
                    .header("Authorization-Key", apiKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    // Convert the JSON response into a tree of Jackson objects - an asynchronous result that will be available later
                    .block(); // Wait for Mono result in synchronous mode - blocks the thread until it receives JsonNode

            if (json != null) {
                // Navigate the JSON tree: first to SearchResult, then to SearchResultItems (this is an array of vacancies).
                JsonNode jobs = json.path("SearchResult").path("SearchResultItems");

                LocalDate cutoffDate = LocalDate.now().minusDays(days);

                for (JsonNode job : jobs) {
                    /*
                    Inside each vacancy there is a MatchedObjectDescriptor block where all the fields are stored, such as:
                        - PositionTitle
                        - OrganizationName
                        - PositionLocationDisplay
                        - PositionURI
                    */
                    JsonNode fields = job.path("MatchedObjectDescriptor");

                    String dateStr = fields.path("PublicationStartDate").asText();

                    // 1. Create a template that takes into account date, time, and possibly fractions of a second.
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS");
                    // 2. Parse date-time.
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr, formatter);
                    // 3. We take only the date.
                    LocalDate publicationDate = dateTime.toLocalDate();

                    if (publicationDate.isBefore(cutoffDate)) {
                        continue;
                    }

                    String title = fields.path("PositionTitle").asText();
                    // asText() - converts the JSON field into a regular string (String), if the field is missing, it will return an empty string ("").
                    System.out.println("Title: " + title);
                    String agency = fields.path("OrganizationName").asText();
                    System.out.println("Agency: " + agency);
                    String location = fields.path("PositionLocationDisplay").asText();
                    System.out.println("Location: " + location);
                    String urlStr = fields.path("PositionURI").asText();
                    System.out.println("URL: " + urlStr);
                    System.out.println("-".repeat(50));

                    writer.printf("%s,%s,%s,%s%n", escapeCsv(title),
                            escapeCsv(agency), escapeCsv(location), escapeCsv(urlStr));
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred while executing the request or processing the data", e);
        }
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
