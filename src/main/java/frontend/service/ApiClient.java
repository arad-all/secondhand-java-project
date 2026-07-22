package frontend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All HTTP communication with the backend lives here. Controllers should
 * never talk to the network directly — they call methods on this class
 * and work with the returned JsonNode.
 * <p>
 * The JWT (if the user is logged in) is attached automatically to every
 * request via {@link SessionManager}. This is harmless for the backend's
 * public endpoints and required for its protected ones, so there's no
 * need for callers to say which is which.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:8080";

    /**
     * Used on list endpoints to request enough results in one page for
     * this project's simple, non-paginated list views. The backend still
     * returns a proper Spring Data {@code Page}; we just ask for a big
     * enough single page instead of teaching the UI to page through it.
     */
    private static final String PAGE_SIZE_PARAM = "size=100";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ------------------------------------------------------------------
    // Auth
    // ------------------------------------------------------------------

    public JsonNode login(String username, String password) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        return sendJsonRequest("POST", "/api/auth/login", body.toString());
    }

    public JsonNode register(String fullName, String username, String password, String phoneNumber, String email) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("fullName", fullName);
        body.put("username", username);
        body.put("password", password);
        body.put("phoneNumber", phoneNumber);
        body.put("email", email);
        return sendJsonRequest("POST", "/api/auth/register", body.toString());
    }

    // ------------------------------------------------------------------
    // Advertisements
    // ------------------------------------------------------------------

    /** Plain public listing (GET /api/advertisements) — every ACTIVE ad, unfiltered. */
    public JsonNode getAdvertisements() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/advertisements?" + PAGE_SIZE_PARAM, null);
    }

    /**
     * Keyword/filter search (GET /api/advertisements/search). Any parameter
     * can be null/blank to skip that filter. Returns a Spring Data Page —
     * callers should read the "content" field for the list of results.
     *
     * @param sortField backend field name to sort by: "createdAt", "price", or "sellerRating"; null for default order
     * @param sortDir   "asc" or "desc"; ignored if sortField is null
     */
    public JsonNode searchAdvertisements(String keyword, Long categoryId, Long cityId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String sortField, String sortDir) throws IOException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            params.put("keyword", keyword.trim());
        }
        if (categoryId != null) {
            params.put("categoryId", String.valueOf(categoryId));
        }
        if (cityId != null) {
            params.put("cityId", String.valueOf(cityId));
        }
        if (minPrice != null) {
            params.put("minPrice", minPrice.toPlainString());
        }
        if (maxPrice != null) {
            params.put("maxPrice", maxPrice.toPlainString());
        }
        if (sortField != null && !sortField.isBlank()) {
            params.put("sort", sortField + "," + (sortDir == null ? "asc" : sortDir));
        }

        String query = buildQuery(params);
        String separator = query.isEmpty() ? "?" : "&";
        return sendJsonRequest("GET", "/api/advertisements/search" + query + separator + PAGE_SIZE_PARAM, null);
    }

    /** The logged-in caller's own advertisements, in any (or one specific) status. Returns a Page. */
    public JsonNode getMyAdvertisements(String statusFilter) throws IOException, InterruptedException {
        String query = (statusFilter == null || statusFilter.isBlank()) ? "" : "&status=" + statusFilter;
        return sendJsonRequest("GET", "/api/advertisements/my?" + PAGE_SIZE_PARAM + query, null);
    }

    /** Advertisements the logged-in caller has purchased (i.e. was recorded as buyer on). Returns a Page. */
    public JsonNode getPurchasedAdvertisements() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/advertisements/purchased?" + PAGE_SIZE_PARAM, null);
    }

    public JsonNode getAdvertisementById(Long id) throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/advertisements/" + id, null);
    }

    public JsonNode createAdvertisement(String title, String description, BigDecimal price, Long categoryId, Long cityId) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("title", title);
        body.put("description", description);
        body.put("price", price);
        body.put("categoryId", categoryId);
        body.put("cityId", cityId);
        return sendJsonRequest("POST", "/api/advertisements", body.toString());
    }

    /**
     * Partial update (PATCH /api/advertisements/{id}). Only non-null
     * arguments are sent, matching the backend's partial-update contract.
     */
    public JsonNode editAdvertisement(Long id, String title, String description, BigDecimal price,
                                      Long categoryId, Long cityId) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        if (title != null) {
            body.put("title", title);
        }
        if (description != null) {
            body.put("description", description);
        }
        if (price != null) {
            body.put("price", price);
        }
        if (categoryId != null) {
            body.put("categoryId", categoryId);
        }
        if (cityId != null) {
            body.put("cityId", cityId);
        }
        return sendJsonRequest("PATCH", "/api/advertisements/" + id, body.toString());
    }

    /** Marks the caller's own ACTIVE advertisement as SOLD, recording who bought it. */
    public JsonNode markAsSold(Long id, Long buyerId) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("buyerId", buyerId);
        return sendJsonRequest("PATCH", "/api/advertisements/" + id + "/sold", body.toString());
    }

    /** Soft-deletes an advertisement. Owner or admin only (enforced by the backend). */
    public void deleteAdvertisement(Long id) throws IOException, InterruptedException {
        sendJsonRequest("DELETE", "/api/advertisements/" + id, null);
    }

    // ------------------------------------------------------------------
    // Categories / cities (reference data, used to populate ComboBoxes)
    // ------------------------------------------------------------------

    public JsonNode getTopLevelCategories() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/categories", null);
    }

    public JsonNode getCategoryChildren(Long categoryId) throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/categories/" + categoryId + "/children", null);
    }

    /**
     * Every category in the tree, flattened via a depth-first traversal
     * (each category immediately followed by all of its descendants),
     * recursing to whatever depth the tree actually has. The backend
     * supports categories of unlimited depth — {@code Category} is
     * self-referencing, and {@code CategoryRepository} already walks the
     * full descendant chain for search filtering — so this must not stop
     * at one level either.
     * <p>
     * Each returned node gets a synthetic {@code depth} field added
     * (0 = top-level, 1 = its children, 2 = grandchildren, and so on) so
     * the UI can indent consistently no matter how deep the tree goes,
     * without every caller having to walk parentId chains itself.
     */
    public JsonNode getCategoriesFlattened() throws IOException, InterruptedException {
        ArrayNode all = objectMapper.createArrayNode();
        for (JsonNode topLevelCategory : getTopLevelCategories()) {
            collectCategoryAndDescendants(topLevelCategory, 0, all);
        }
        return all;
    }

    private void collectCategoryAndDescendants(JsonNode category, int depth, ArrayNode accumulator)
            throws IOException, InterruptedException {
        if (category instanceof ObjectNode categoryObject) {
            categoryObject.put("depth", depth);
        }
        accumulator.add(category);
        for (JsonNode child : getCategoryChildren(category.get("id").asLong())) {
            collectCategoryAndDescendants(child, depth + 1, accumulator);
        }
    }

    /** Admin-only (POST /api/categories). Pass parentId=null for a top-level category. */
    public JsonNode createCategory(String name, Long parentId) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        if (parentId != null) {
            body.put("parentId", parentId);
        }
        return sendJsonRequest("POST", "/api/categories", body.toString());
    }

    public JsonNode getCities() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/cities", null);
    }

    /** Admin-only (POST /api/cities). Province is optional. */
    public JsonNode createCity(String name, String province) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", name);
        if (province != null && !province.isBlank()) {
            body.put("province", province);
        }
        return sendJsonRequest("POST", "/api/cities", body.toString());
    }

    // ------------------------------------------------------------------
    // Favorites
    // ------------------------------------------------------------------

    public JsonNode getFavorites() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/favorites", null);
    }

    public JsonNode addFavorite(Long advertisementId) throws IOException, InterruptedException {
        return sendJsonRequest("POST", "/api/favorites/" + advertisementId, "");
    }

    public void removeFavorite(Long advertisementId) throws IOException, InterruptedException {
        sendJsonRequest("DELETE", "/api/favorites/" + advertisementId, null);
    }

    // ------------------------------------------------------------------
    // Users & ratings (seller profile, purchase history rating)
    // ------------------------------------------------------------------

    /** Public seller profile (username/full name/phone) by username — e.g. an ad's ownerUsername. */
    public JsonNode getUserByUsername(String username) throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/users/by-username/" + encode(username), null);
    }

    /** A seller's reputation: average score, total count, and every rating they've received. Public. */
    public JsonNode getSellerRatings(Long sellerId) throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/users/" + sellerId + "/ratings", null);
    }

    /** Rates the seller of a SOLD advertisement the caller bought. Score must be 1-5; comment is optional. */
    public JsonNode rateSeller(Long advertisementId, int score, String comment) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("score", score);
        body.put("comment", comment);
        return sendJsonRequest("POST", "/api/advertisements/" + advertisementId + "/ratings", body.toString());
    }

    // ------------------------------------------------------------------
    // Chat
    // ------------------------------------------------------------------

    /**
     * Sends a message to an advertisement's seller. Creates the
     * (advertisement, buyer) conversation on first contact and reuses it
     * on every later call for the same ad — the backend
     * (ChatService#messageSeller) is what guarantees there's never a
     * duplicate conversation, not this method. The returned message's
     * {@code conversationId} is how the caller finds out which
     * conversation to open next.
     */
    public JsonNode messageSeller(Long advertisementId, String content) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("content", content);
        return sendJsonRequest("POST", "/api/advertisements/" + advertisementId + "/messages", body.toString());
    }

    /** The caller's own conversations (as buyer or seller), newest activity first. */
    public JsonNode getMyConversations() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/conversations", null);
    }

    /** One conversation's full message thread, oldest first. Also marks the other party's messages as read. */
    public JsonNode getConversation(Long conversationId) throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/conversations/" + conversationId, null);
    }

    /** Replies within an existing conversation. Either participant — buyer or seller — may call this. */
    public JsonNode sendMessage(Long conversationId, String content) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("content", content);
        return sendJsonRequest("POST", "/api/conversations/" + conversationId + "/messages", body.toString());
    }

    // ------------------------------------------------------------------
    // Admin: advertisement moderation + user moderation
    // ------------------------------------------------------------------

    public JsonNode listPendingAdvertisements() throws IOException, InterruptedException {
        return sendJsonRequest("GET", "/api/admin/advertisements/pending?" + PAGE_SIZE_PARAM, null);
    }

    public JsonNode approveAdvertisement(Long id) throws IOException, InterruptedException {
        return sendJsonRequest("PATCH", "/api/admin/advertisements/" + id + "/approve", "");
    }

    public JsonNode rejectAdvertisement(Long id, String reason) throws IOException, InterruptedException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("reason", reason);
        return sendJsonRequest("PATCH", "/api/admin/advertisements/" + id + "/reject", body.toString());
    }

    public JsonNode listUsers(String statusFilter) throws IOException, InterruptedException {
        String query = (statusFilter == null || statusFilter.isBlank()) ? "" : "?status=" + statusFilter;
        return sendJsonRequest("GET", "/api/admin/users" + query, null);
    }

    public JsonNode blockUser(Long id) throws IOException, InterruptedException {
        return sendJsonRequest("PATCH", "/api/admin/users/" + id + "/block", "");
    }

    public JsonNode unblockUser(Long id) throws IOException, InterruptedException {
        return sendJsonRequest("PATCH", "/api/admin/users/" + id + "/unblock", "");
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private String buildQuery(Map<String, String> params) {
        if (params.isEmpty()) {
            return "";
        }
        return params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&", "?", ""));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Sends a JSON request and returns the parsed response body. Attaches
     * the current session's JWT (if any) as "Authorization: Bearer ..." —
     * harmless on public endpoints, required on protected ones.
     */
    private JsonNode sendJsonRequest(String method, String path, String jsonBody)
            throws IOException, InterruptedException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json");

        String token = SessionManager.getInstance().getToken();
        if (token != null && !token.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + token);
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
