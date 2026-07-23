package backend.service;

import backend.controller.dto.AdvertisementDetailResponse;
import backend.controller.dto.AdvertisementSummaryResponse;
import backend.controller.dto.CreateAdvertisementRequest;
import backend.controller.dto.UpdateAdvertisementRequest;
import backend.exception.ForbiddenActionException;
import backend.exception.InvalidStateTransitionException;
import backend.exception.ResourceNotFoundException;
import backend.mapper.AdvertisementMapper;
import backend.model.entity.Advertisement;
import backend.model.entity.AdvertisementImage;
import backend.model.entity.Category;
import backend.model.entity.City;
import backend.model.entity.User;
import backend.model.enums.AccountStatus;
import backend.model.enums.AdvertisementStatus;
import backend.repository.AdvertisementImageRepository;
import backend.repository.AdvertisementRepository;
import backend.repository.CategoryRepository;
import backend.repository.CityRepository;
import backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceTest {

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdvertisementImageRepository advertisementImageRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AdvertisementService advertisementService;

    private User owner;
    private User buyer;
    private User admin;
    private Category category;
    private City city;
    private Advertisement activeAd;
    private Advertisement pendingAd;
    private Advertisement soldAd;
    private Advertisement deletedAd;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setStatus(AccountStatus.ACTIVE);

        buyer = new User();
        buyer.setId(2L);
        buyer.setUsername("buyer");

        admin = new User();
        admin.setId(3L);
        admin.setUsername("admin");

        category = new Category();
        category.setId(10L);
        category.setName("Electronics");

        city = new City();
        city.setId(20L);
        city.setName("Tehran");

        activeAd = createAd(1L, "Active Ad", AdvertisementStatus.ACTIVE, owner, null);
        pendingAd = createAd(2L, "Pending Ad", AdvertisementStatus.PENDING_REVIEW, owner, null);
        soldAd = createAd(3L, "Sold Ad", AdvertisementStatus.SOLD, owner, buyer);
        deletedAd = createAd(4L, "Deleted Ad", AdvertisementStatus.DELETED, owner, null);
    }

    private Advertisement createAd(Long id, String title, AdvertisementStatus status, User owner, User buyer) {
        Advertisement ad = new Advertisement();
        ad.setId(id);
        ad.setTitle(title);
        ad.setDescription("Description of " + title);
        ad.setPrice(new BigDecimal("100.00"));
        ad.setStatus(status);
        ad.setOwner(owner);
        ad.setBuyer(buyer);
        ad.setCategory(category);
        ad.setCity(city);
        ad.setImages(new ArrayList<>());
        return ad;
    }

    // ---- getActiveAdvertisements ----

    @Test
    void getActiveAdvertisements_delegatesToRepositoryAndMaps() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Advertisement> page = new PageImpl<>(List.of(activeAd));
        when(advertisementRepository.findByStatusAndOwnerStatus(
                AdvertisementStatus.ACTIVE, AccountStatus.ACTIVE, pageable)).thenReturn(page);

        Page<AdvertisementSummaryResponse> result = advertisementService.getActiveAdvertisements(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Active Ad", result.getContent().get(0).title());
    }

    // ---- getById visibility logic ----

    @Test
    void getById_returnsActiveAd_toAnonymousUser() {
        when(advertisementRepository.findByIdWithImages(1L)).thenReturn(Optional.of(activeAd));

        AdvertisementDetailResponse result = advertisementService.getById(1L, null, false);

        assertEquals("Active Ad", result.title());
    }

    @Test
    void getById_returnsActiveAd_toAnyAuthenticatedUser() {
        when(advertisementRepository.findByIdWithImages(1L)).thenReturn(Optional.of(activeAd));

        AdvertisementDetailResponse result = advertisementService.getById(1L, 99L, false);

        assertEquals("Active Ad", result.title());
    }

    @Test
    void getById_throws404_whenAdNotFound() {
        when(advertisementRepository.findByIdWithImages(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> advertisementService.getById(99L, null, false));
    }

    @Test
    void getById_showsNonActiveAd_toOwner() {
        when(advertisementRepository.findByIdWithImages(2L)).thenReturn(Optional.of(pendingAd));

        AdvertisementDetailResponse result = advertisementService.getById(2L, 1L, false);

        assertEquals("Pending Ad", result.title());
    }

    @Test
    void getById_showsSoldAd_toBuyer() {
        when(advertisementRepository.findByIdWithImages(3L)).thenReturn(Optional.of(soldAd));

        AdvertisementDetailResponse result = advertisementService.getById(3L, 2L, false);

        assertEquals("Sold Ad", result.title());
    }

    @Test
    void getById_showsNonActiveAd_toAdmin() {
        when(advertisementRepository.findByIdWithImages(2L)).thenReturn(Optional.of(pendingAd));

        AdvertisementDetailResponse result = advertisementService.getById(2L, 99L, true);

        assertEquals("Pending Ad", result.title());
    }

    @Test
    void getById_hidesNonActiveAd_fromNonOwnerNonBuyer() {
        when(advertisementRepository.findByIdWithImages(2L)).thenReturn(Optional.of(pendingAd));

        assertThrows(ResourceNotFoundException.class,
                () -> advertisementService.getById(2L, 99L, false));
    }

    @Test
    void getById_hidesActiveAd_whenOwnerIsBlocked_fromAnonymousUser() {
        owner.setStatus(AccountStatus.BLOCKED);
        when(advertisementRepository.findByIdWithImages(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(ResourceNotFoundException.class,
                () -> advertisementService.getById(1L, null, false));
    }

    @Test
    void getById_showsBlockedOwnersAd_toAdmin() {
        owner.setStatus(AccountStatus.BLOCKED);
        when(advertisementRepository.findByIdWithImages(1L)).thenReturn(Optional.of(activeAd));

        AdvertisementDetailResponse result = advertisementService.getById(1L, 3L, true);

        assertEquals("Active Ad", result.title());
    }

    // ---- search ----

    @Test
    void search_overridesStatusToActive_forNonAdmin() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Advertisement> page = new PageImpl<>(List.of(activeAd));
        when(advertisementRepository.search(eq(AdvertisementStatus.ACTIVE), eq(AccountStatus.ACTIVE),
                any(), any(), any(), any(), any(), eq(pageable))).thenReturn(page);

        Page<AdvertisementSummaryResponse> result = advertisementService.search(
                null, null, null, null, null,
                AdvertisementStatus.PENDING_REVIEW, false, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void search_usesRequestedStatus_forAdmin() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Advertisement> page = new PageImpl<>(List.of(pendingAd));
        when(advertisementRepository.search(eq(AdvertisementStatus.PENDING_REVIEW), eq(null),
                any(), any(), any(), any(), any(), eq(pageable))).thenReturn(page);

        Page<AdvertisementSummaryResponse> result = advertisementService.search(
                null, null, null, null, null,
                AdvertisementStatus.PENDING_REVIEW, true, pageable);

        assertEquals(1, result.getTotalElements());
    }

    // ---- create ----

    @Test
    void create_createsAdAndReturnsDetail() {
        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                "New Ad", "Description", new BigDecimal("50.00"), 10L, 20L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(cityRepository.findById(20L)).thenReturn(Optional.of(city));
        when(advertisementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdvertisementDetailResponse result = advertisementService.create(request, 1L);

        assertEquals("New Ad", result.title());
        assertEquals("Description", result.description());
        assertEquals(new BigDecimal("50.00"), result.price());
        assertEquals("owner", result.ownerUsername());
        // Should start as PENDING_REVIEW
        assertEquals("PENDING_REVIEW", result.status());
    }

    @Test
    void create_throws_whenCategoryNotFound() {
        CreateAdvertisementRequest request = new CreateAdvertisementRequest(
                "New Ad", null, new BigDecimal("50.00"), 999L, 20L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> advertisementService.create(request, 1L));
    }

    // ---- markAsSold ----

    @Test
    void markAsSold_happyPath() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));
        when(userRepository.findById(2L)).thenReturn(Optional.of(buyer));

        AdvertisementDetailResponse result = advertisementService.markAsSold(1L, 1L, 2L);

        assertEquals("SOLD", result.status());
        assertEquals("buyer", result.buyerUsername());
    }

    @Test
    void markAsSold_throws_whenNotOwner() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(ForbiddenActionException.class,
                () -> advertisementService.markAsSold(1L, 99L, 2L));
    }

    @Test
    void markAsSold_throws_whenNotActive() {
        when(advertisementRepository.findById(2L)).thenReturn(Optional.of(pendingAd));

        assertThrows(InvalidStateTransitionException.class,
                () -> advertisementService.markAsSold(2L, 1L, 2L));
    }

    @Test
    void markAsSold_throws_whenBuyerEqualsOwner() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(ForbiddenActionException.class,
                () -> advertisementService.markAsSold(1L, 1L, 1L));
    }

    // ---- approve ----

    @Test
    void approve_transitionsFromPendingToActive_andClearsAdminNote() {
        pendingAd.setAdminNote("Some old note");
        when(advertisementRepository.findById(2L)).thenReturn(Optional.of(pendingAd));

        AdvertisementDetailResponse result = advertisementService.approve(2L);

        assertEquals("ACTIVE", result.status());
        assertNull(result.adminNote());
    }

    @Test
    void approve_throws_whenNotPending() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(InvalidStateTransitionException.class,
                () -> advertisementService.approve(1L));
    }

    // ---- reject ----

    @Test
    void reject_setsReasonAndTransitionsToRejected() {
        when(advertisementRepository.findById(2L)).thenReturn(Optional.of(pendingAd));

        AdvertisementDetailResponse result = advertisementService.reject(2L, "Inappropriate content");

        assertEquals("REJECTED", result.status());
        assertEquals("Inappropriate content", result.adminNote());
    }

    @Test
    void reject_throws_whenNotPending() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(InvalidStateTransitionException.class,
                () -> advertisementService.reject(1L, "reason"));
    }

    // ---- deleteAdvertisement ----

    @Test
    void deleteAdvertisement_ownerCanDeleteActiveAd() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        advertisementService.deleteAdvertisement(1L, 1L, false);

        assertEquals(AdvertisementStatus.DELETED, activeAd.getStatus());
    }

    @Test
    void deleteAdvertisement_adminCanDeleteOthersAd() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        advertisementService.deleteAdvertisement(1L, 3L, true);

        assertEquals(AdvertisementStatus.DELETED, activeAd.getStatus());
    }

    @Test
    void deleteAdvertisement_throws_whenNotOwnerNorAdmin() {
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(ForbiddenActionException.class,
                () -> advertisementService.deleteAdvertisement(1L, 99L, false));
    }

    @Test
    void deleteAdvertisement_throws_whenStatusNotDeletable() {
        when(advertisementRepository.findById(3L)).thenReturn(Optional.of(soldAd));

        assertThrows(InvalidStateTransitionException.class,
                () -> advertisementService.deleteAdvertisement(3L, 1L, false));
    }

    @Test
    void deleteAdvertisement_throws_whenAlreadyDeleted() {
        when(advertisementRepository.findById(4L)).thenReturn(Optional.of(deletedAd));

        assertThrows(InvalidStateTransitionException.class,
                () -> advertisementService.deleteAdvertisement(4L, 1L, false));
    }

    // ---- editAdvertisement ----

    @Test
    void editAdvertisement_updatesFields_partialUpdate() {
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                "Updated Title", null, null, null, null);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        AdvertisementDetailResponse result = advertisementService.editAdvertisement(1L, request, 1L, false);

        assertEquals("Updated Title", result.title());
        assertEquals("Description of Active Ad", result.description());
    }

    @Test
    void editAdvertisement_updatesCategoryAndCity_whenProvided() {
        Category newCategory = new Category();
        newCategory.setId(11L);
        newCategory.setName("Books");
        City newCity = new City();
        newCity.setId(21L);
        newCity.setName("Shiraz");

        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                null, null, null, 11L, 21L);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));
        when(categoryRepository.findById(11L)).thenReturn(Optional.of(newCategory));
        when(cityRepository.findById(21L)).thenReturn(Optional.of(newCity));

        AdvertisementDetailResponse result = advertisementService.editAdvertisement(1L, request, 1L, false);

        assertEquals("Books", result.categoryName());
        assertEquals("Shiraz", result.cityName());
    }

    @Test
    void editAdvertisement_throws_whenNotOwner() {
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                "Hacked", null, null, null, null);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(ForbiddenActionException.class,
                () -> advertisementService.editAdvertisement(1L, request, 99L, false));
    }

    @Test
    void editAdvertisement_adminCanEdit() {
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                "Admin Edit", null, null, null, null);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        AdvertisementDetailResponse result = advertisementService.editAdvertisement(1L, request, 3L, true);

        assertEquals("Admin Edit", result.title());
    }

    @Test
    void editAdvertisement_throws_whenNotActive() {
        UpdateAdvertisementRequest request = new UpdateAdvertisementRequest(
                "Edit", null, null, null, null);
        when(advertisementRepository.findById(2L)).thenReturn(Optional.of(pendingAd));

        assertThrows(InvalidStateTransitionException.class,
                () -> advertisementService.editAdvertisement(2L, request, 1L, false));
    }

    // ---- addImages ----

    @Test
    void addImages_addsImagesAndReturnsUpdatedDetail() {
        MultipartFile file = mock(MultipartFile.class);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));
        when(advertisementImageRepository.countByAdvertisementId(1L)).thenReturn(0L);
        when(fileStorageService.store(eq(1L), eq(file))).thenReturn("uuid.jpg");

        AdvertisementDetailResponse result = advertisementService.addImages(1L, List.of(file), 1L);

        assertEquals("Active Ad", result.title());
        assertEquals(1, activeAd.getImages().size());
        assertEquals("uuid.jpg", activeAd.getImages().get(0).getImagePath());
    }

    @Test
    void addImages_throws_whenNoFiles() {
        assertThrows(backend.exception.InvalidFileException.class,
                () -> advertisementService.addImages(1L, null, 1L));
        assertThrows(backend.exception.InvalidFileException.class,
                () -> advertisementService.addImages(1L, List.of(), 1L));
    }

    @Test
    void addImages_throws_whenExceedsMax() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));
        when(advertisementImageRepository.countByAdvertisementId(1L)).thenReturn(7L);

        assertThrows(backend.exception.InvalidFileException.class,
                () -> advertisementService.addImages(1L, List.of(file1, file2), 1L));
    }

    @Test
    void addImages_rollsBackOnFailure() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));
        when(advertisementImageRepository.countByAdvertisementId(1L)).thenReturn(0L);
        when(fileStorageService.store(eq(1L), eq(file1))).thenReturn("ok.jpg");
        when(fileStorageService.store(eq(1L), eq(file2))).thenThrow(new RuntimeException("Disk full"));

        assertThrows(RuntimeException.class,
                () -> advertisementService.addImages(1L, List.of(file1, file2), 1L));

        // Should have cleaned up the first file
        verify(fileStorageService).delete(1L, "ok.jpg");
    }

    @Test
    void addImages_throws_whenNotOwner() {
        MultipartFile file = mock(MultipartFile.class);
        when(advertisementRepository.findById(1L)).thenReturn(Optional.of(activeAd));

        assertThrows(ForbiddenActionException.class,
                () -> advertisementService.addImages(1L, List.of(file), 99L));
    }

    // ---- getMyAdvertisements ----

    @Test
    void getMyAdvertisements_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Advertisement> page = new PageImpl<>(List.of(pendingAd));
        when(advertisementRepository.findByOwnerIdAndStatus(1L, AdvertisementStatus.PENDING_REVIEW, pageable))
                .thenReturn(page);

        Page<AdvertisementSummaryResponse> result = advertisementService.getMyAdvertisements(
                1L, AdvertisementStatus.PENDING_REVIEW, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Pending Ad", result.getContent().get(0).title());
    }

    @Test
    void getMyAdvertisements_withoutStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Advertisement> page = new PageImpl<>(List.of(activeAd, pendingAd, soldAd));
        when(advertisementRepository.findByOwnerId(1L, pageable)).thenReturn(page);

        Page<AdvertisementSummaryResponse> result = advertisementService.getMyAdvertisements(
                1L, null, pageable);

        assertEquals(3, result.getTotalElements());
    }

    // ---- getPurchasedAdvertisements ----

    @Test
    void getPurchasedAdvertisements_returnsBuyersAds() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Advertisement> page = new PageImpl<>(List.of(soldAd));
        when(advertisementRepository.findByBuyerId(2L, pageable)).thenReturn(page);

        Page<AdvertisementSummaryResponse> result = advertisementService.getPurchasedAdvertisements(2L, pageable);

        assertEquals(1, result.getTotalElements());
    }

    // ---- loadImage ----

    @Test
    void loadImage_returnsResource_whenImageExists() {
        AdvertisementImage img = new AdvertisementImage();
        img.setImagePath("photo.jpg");
        when(advertisementImageRepository.findByAdvertisementIdAndImagePath(1L, "photo.jpg"))
                .thenReturn(Optional.of(img));
        org.springframework.core.io.Resource resource = mock(org.springframework.core.io.Resource.class);
        when(fileStorageService.load(1L, "photo.jpg")).thenReturn(resource);

        org.springframework.core.io.Resource result = advertisementService.loadImage(1L, "photo.jpg");

        assertSame(resource, result);
    }

    @Test
    void loadImage_throws404_whenImageNotInDatabase() {
        when(advertisementImageRepository.findByAdvertisementIdAndImagePath(1L, "missing.jpg"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> advertisementService.loadImage(1L, "missing.jpg"));
    }

    // ---- listPendingReview ----

    @Test
    void listPendingReview_returnsOnlyPendingAds() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Advertisement> page = new PageImpl<>(List.of(pendingAd));
        when(advertisementRepository.findByStatus(AdvertisementStatus.PENDING_REVIEW, pageable))
                .thenReturn(page);

        Page<AdvertisementSummaryResponse> result = advertisementService.listPendingReview(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("PENDING_REVIEW", result.getContent().get(0).status());
    }
}
