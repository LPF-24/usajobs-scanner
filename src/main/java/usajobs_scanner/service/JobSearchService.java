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

@Service
@RequiredArgsConstructor
public class JobSearchService {
    private final WebClient.Builder webClientBuilder;
    private final Logger logger = LoggerFactory.getLogger(JobSearchService.class);

    @Value("${usajobs.user-agent}")
    private String userAgent;

    @Value("${usajobs.api-key}")
    private String apiKey;

    public void searchJobs(String[] keywords) {
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
                    .bodyToMono(JsonNode.class) //преобразуй JSON-ответ в дерево объектов Jackson -
                    // асинхронный результат, который будет доступен позже
                    .block(); //Ожидаем результат Mono в синхронном режиме
                    //блокирует поток, пока не получим JsonNode

            if (json != null) {
                //Переход по дереву JSON: сначала к SearchResult, затем к SearchResultItems (это массив вакансий)
                JsonNode jobs = json.path("SearchResult").path("SearchResultItems");
                for (JsonNode job : jobs) {
                    //Внутри каждой вакансии есть блок MatchedObjectDescriptor, где хранятся все поля, такие как:
                    //PositionTitle
                    //OrganizationName
                    //PositionLocationDisplay
                    //PositionURI
                    JsonNode fields = job.path("MatchedObjectDescriptor");
                    String title = fields.path("PositionTitle").asText();
                    //asText() - преобразует поле JSON в обычную строку (String), если поле отсутствует — вернёт пустую строку ("")
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
