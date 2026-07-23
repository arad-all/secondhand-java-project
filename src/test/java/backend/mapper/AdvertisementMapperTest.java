package backend.mapper;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.model.entity.Advertisement;
import backend.model.entity.AdvertisementImage;
import backend.model.entity.Category;
import backend.model.entity.City;
import backend.model.entity.User;
import backend.model.enums.AdvertisementStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvertisementMapperTest {

    private Advertisement createAdWithImages(List<AdvertisementImage> images) {
        Advertisement ad = new Advertisement();
        ad.setId(1L);
        ad.setTitle("Test Ad");
        ad.setDescription("Test Description");
        ad.setPrice(new BigDecimal("100.00"));
        ad.setSellerRating(4.5);
        ad.setStatus(AdvertisementStatus.ACTIVE);

        City city = new City();
        city.setName("Tehran");
        ad.setCity(city);

        Category category = new Category();
        category.setName("Electronics");
        ad.setCategory(category);

        User owner = new User();
        owner.setUsername("seller");
        ad.setOwner(owner);

        images.forEach(img -> img.setAdvertisement(ad));
        ad.setImages(images);

        return ad;
    }

    private AdvertisementImage createImage(String path, int displayOrder) {
        AdvertisementImage img = new AdvertisementImage();
        img.setImagePath(path);
        img.setDisplayOrder(displayOrder);
        return img;
    }

    // ---- toSummary tests ----

    @Test
    void toSummary_firstImageUrlIsNull_whenAdHasNoImages() {
        Advertisement ad = createAdWithImages(new ArrayList<>());

        AdvertisementSummaryResponse result = AdvertisementMapper.toSummary(ad);

        assertNull(result.firstImageUrl());
    }

    @Test
    void toSummary_firstImageUrlPointsToFirstByDisplayOrder() {
        List<AdvertisementImage> images = List.of(
                createImage("third.jpg", 2),
                createImage("first.jpg", 0),
                createImage("second.jpg", 1));
        Advertisement ad = createAdWithImages(images);

        AdvertisementSummaryResponse result = AdvertisementMapper.toSummary(ad);

        assertEquals("/api/advertisements/1/images/first.jpg", result.firstImageUrl());
    }

    @Test
    void toSummary_setsAllFieldsCorrectly() {
        Advertisement ad = createAdWithImages(new ArrayList<>());

        AdvertisementSummaryResponse result = AdvertisementMapper.toSummary(ad);

        assertEquals(1L, result.id());
        assertEquals("Test Ad", result.title());
        assertEquals(new BigDecimal("100.00"), result.price());
        assertEquals("Tehran", result.cityName());
        assertEquals("Electronics", result.categoryName());
        assertEquals("ACTIVE", result.status());
        assertEquals(4.5, result.sellerRating());
    }

    @Test
    void toSummary_handlesNullDisplayOrderGracefully() {
        AdvertisementImage img1 = createImage("first.jpg", 1);
        AdvertisementImage img2 = new AdvertisementImage();
        img2.setImagePath("second.jpg");
        img2.setDisplayOrder(null);
        img2.setAdvertisement(advertisementForImage());

        Advertisement ad = createAdWithImages(List.of(img1, img2));

        AdvertisementSummaryResponse result = AdvertisementMapper.toSummary(ad);

        assertNotNull(result.firstImageUrl());
        assertEquals("/api/advertisements/1/images/first.jpg", result.firstImageUrl());
    }

    /** Helper to satisfy the non-null advertisement reference when setting fields manually. */
    private Advertisement advertisementForImage() {
        Advertisement ad = new Advertisement();
        ad.setId(1L);
        return ad;
    }

    // ---- toDetail tests ----

    @Test
    void toDetail_imageUrlsAreSortedByDisplayOrder() {
        List<AdvertisementImage> images = List.of(
                createImage("z.jpg", 2),
                createImage("a.jpg", 0),
                createImage("m.jpg", 1));
        Advertisement ad = createAdWithImages(images);

        AdvertisementDetailResponse result = AdvertisementMapper.toDetail(ad);

        assertEquals(3, result.imageUrls().size());
        assertEquals("/api/advertisements/1/images/a.jpg", result.imageUrls().get(0));
        assertEquals("/api/advertisements/1/images/m.jpg", result.imageUrls().get(1));
        assertEquals("/api/advertisements/1/images/z.jpg", result.imageUrls().get(2));
    }

    @Test
    void toDetail_imageUrlsIsEmpty_whenNoImages() {
        Advertisement ad = createAdWithImages(new ArrayList<>());

        AdvertisementDetailResponse result = AdvertisementMapper.toDetail(ad);

        assertTrue(result.imageUrls().isEmpty());
    }

    @Test
    void toDetail_setsAllFields() {
        User buyer = new User();
        buyer.setUsername("buyer");
        Advertisement ad = createAdWithImages(new ArrayList<>());
        ad.setBuyer(buyer);
        ad.setAdminNote("Rejected: inappropriate content");

        AdvertisementDetailResponse result = AdvertisementMapper.toDetail(ad);

        assertEquals(1L, result.id());
        assertEquals("Test Ad", result.title());
        assertEquals("Test Description", result.description());
        assertEquals(new BigDecimal("100.00"), result.price());
        assertEquals("Tehran", result.cityName());
        assertEquals("Electronics", result.categoryName());
        assertEquals("ACTIVE", result.status());
        assertEquals("seller", result.ownerUsername());
        assertEquals("buyer", result.buyerUsername());
        assertEquals("Rejected: inappropriate content", result.adminNote());
    }

    @Test
    void toDetail_buyerUsernameIsNull_whenNoBuyer() {
        Advertisement ad = createAdWithImages(new ArrayList<>());

        AdvertisementDetailResponse result = AdvertisementMapper.toDetail(ad);

        assertNull(result.buyerUsername());
    }
}
