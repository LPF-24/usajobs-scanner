package usajobs_scanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class JobSearchService {
    private final WebClient.Builder webClientBuilder;

    @Value("${usajobs.user-agent}")
    private String userAgent;

    @Value("${usajobs.api-key}")
    private String apiKey;

    public void searchJobs(String keyword) {
        WebClient client = webClientBuilder.baseUrl("https://data.usajobs.gov/api/search").build();

        String url = "?Keyword=" + keyword;

        try {
            JsonNode json = client.get()
                    .uri(url)
                    .header("User-Agent", userAgent)
                    .header("Authorization-Key", apiKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (json != null) {
                JsonNode jobs = json.path("SearchResult").path("SearchResultItems");
                for (JsonNode job : jobs) {
                    JsonNode fields = job.path("MatchedObjectDescriptor");
                    System.out.println("Title: " + fields.path("PositionTitle").asText());
                    System.out.println("Agency: " + fields.path("OrganizationName").asText());
                    System.out.println("Location: " + fields.path("PositionLocationDisplay").asText());
                    System.out.println("URL: " + fields.path("PositionURI").asText());
                    System.out.println("-".repeat(50));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
