package frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * All HTTP communication with the backend lives here. Controllers should
 * never talk to the network directly — they call methods on this class
 * and work with the returned JsonNode.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode login(String username, String password) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        return sendJsonRequest("POST", "/api/auth/login", body.toString(), false);
    }

    public JsonNode register(String fullName, String username, String password, String phoneNumber, String email) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("fullName", fullName);
        body.put("username", username);
        body.put("password", password);
        body.put("phoneNumber", phoneNumber);
        body.put("email", email);
        return sendJsonRequest("POST", "/api/auth/register", body.toString(), false);
    }

    public JsonNode getAdvertisements() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/advertisements", null, false);
    }

    public JsonNode getAdvertisementById(Long id) throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/advertisements/" + id, null, false);
    }

    public JsonNode createAdvertisement(String title, String description, BigDecimal price, Long categoryId, Long cityId) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("title", title);
        body.put("description", description);
        body.put("price", price);
        body.put("categoryId", categoryId);
        body.put("cityId", cityId);
        return sendJsonRequest("POST", "/api/advertisements", body.toString(), true);
    }

    /**
     * Sends a JSON request and returns the parsed response body.
     *
     * @param includeAuth whether to attach the current session's JWT
     *                    (as "Authorization: Bearer ...") if one exists.
     */
    private JsonNode sendJsonRequest(String method, String path, String jsonBody, boolean includeAuth)
            throws IOException, InterruptedException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json");

        if (includeAuth) {
            String token = SessionManager.getInstance().getToken();
            if (token != null && !token.isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }
        }

        HttpRequest.BodyPublisher bodyPublisher = (jsonBody == null)
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(jsonBody);

        requestBuilder.method(method, bodyPublisher);

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        JsonNode responseJson = (responseBody == null || responseBody.isBlank())
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(responseBody);

        if (response.statusCode() >= 400) {
            String message = responseJson.has("message")
                    ? responseJson.get("message").asText()
                    : "Request failed with status " + response.statusCode();
            throw new IOException(message);
        }

        return responseJson;
    }
}
