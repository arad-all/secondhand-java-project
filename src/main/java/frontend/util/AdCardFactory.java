package frontend.util;

import com.fasterxml.jackson.databind.JsonNode;
import frontend.service.ApiClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Builds a small "product card" node for one advertisement summary. Used
 * by every page that shows advertisements in a grid (browse, my
 * advertisements, favorites, purchase history) so the look of a card and
 * the logic for resolving its thumbnail only exists in one place.
 * <p>
 * Deliberately a plain static factory returning a {@link VBox} rather
 * than a custom {@code Control} subclass — nothing here needs a real
 * component (CSS skin, properties, etc.), just a Node to drop into a
 * {@code FlowPane}.
 */
public final class AdCardFactory {

    private static final double CARD_WIDTH = 210;
    private static final double IMAGE_HEIGHT = 135;

    private AdCardFactory() {
    }

    /**
     * @param ad     one advertisement summary (id/title/price/cityName/status/firstImageUrl)
     * @param onOpen called with the advertisement's id when the card is clicked
     */
    public static VBox create(JsonNode ad, ApiClient apiClient, Consumer<Long> onOpen) {
        long id = ad.path("id").asLong();

        StackPane imageArea = buildImageArea(ad, apiClient);

        Label title = new Label(ad.path("title").asText(""));
        title.getStyleClass().add("ad-card-title");
        title.setWrapText(true);
        title.setMaxHeight(34);

        Label price = new Label(ad.path("price").asText(""));
        price.getStyleClass().add("ad-card-price");

        String city = ad.path("cityName").asText(null);
        Label meta = new Label((city == null || city.isBlank()) ? " " : city);
        meta.getStyleClass().add("ad-card-meta");

        VBox info = new VBox(4, title, price, meta);

        // Seller rating row — added only when the backend provides a non-null rating.
        Double sellerRating = ad.hasNonNull("sellerRating") ? ad.get("sellerRating").asDouble() : null;
        if (sellerRating != null && sellerRating > 0) {
            Label starLabel = new Label("★");
            starLabel.getStyleClass().add("ad-card-rating-star");            Label ratingValueLabel = new Label(String.format(Locale.US, "%.1f", sellerRating));
            ratingValueLabel.getStyleClass().add("ad-card-rating-text");
            HBox ratingRow = new HBox(2, starLabel, ratingValueLabel);
            info.getChildren().add(ratingRow);
        }
        info.setPadding(new Insets(8, 10, 10, 10));

        VBox card = new VBox(imageArea, info);
        card.getStyleClass().add("ad-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setOnMouseClicked(event -> onOpen.accept(id));

        return card;
    }

    private static StackPane buildImageArea(JsonNode ad, ApiClient apiClient) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("ad-card-image");
        pane.setPrefSize(CARD_WIDTH, IMAGE_HEIGHT);
        pane.setMinSize(CARD_WIDTH, IMAGE_HEIGHT);
        pane.setMaxSize(CARD_WIDTH, IMAGE_HEIGHT);

        String imageUrl = apiClient.resolveImageUrl(
                ad.hasNonNull("firstImageUrl") ? ad.get("firstImageUrl").asText() : null);

        if (imageUrl != null) {
            ImageView imageView = new ImageView(new Image(imageUrl, CARD_WIDTH, IMAGE_HEIGHT, false, true, true));
            imageView.setFitWidth(CARD_WIDTH);
            imageView.setFitHeight(IMAGE_HEIGHT);
            imageView.setPreserveRatio(false);

            Rectangle clip = new Rectangle(CARD_WIDTH, IMAGE_HEIGHT);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            imageView.setClip(clip);

            pane.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("No Image");
            placeholder.getStyleClass().add("subtle-label");
            pane.getChildren().add(placeholder);
        }

        String status = ad.path("status").asText("");
        if (!status.isBlank() && !"ACTIVE".equals(status)) {
            Label badge = new Label(status.replace('_', ' '));
            badge.getStyleClass().addAll("status-badge", "status-" + status.toLowerCase(Locale.ROOT));
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(6));
            pane.getChildren().add(badge);
        }

        return pane;
    }
}
