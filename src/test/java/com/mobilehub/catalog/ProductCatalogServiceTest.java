package com.mobilehub.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProductCatalogServiceTest {
    @Test
    void mapsProductsToTheStorefrontResponseShape() {
        ProductRepository repository = mock(ProductRepository.class);
        ProductInventoryRepository inventory = mock(ProductInventoryRepository.class);
        Product phone = new Product(
                "test-phone", "Test", "Phone", 25000, "8 GB", "256 GB",
                "6.5 inch OLED", "Test processor", "50 MP camera", "5,000 mAh",
                4.5, 2025, "https://example.com/phone.jpg", "https://example.com/fallback.jpg");
        when(repository.findAll()).thenReturn(List.of(phone));
        ProductCatalogService service = new ProductCatalogService(repository, inventory);

        ProductResponse response = service.findAll().get(0);

        assertEquals("test-phone", response.id());
        assertEquals("Phone", response.model());
        assertEquals(2, response.images().size());
        assertEquals("https://example.com/fallback.jpg", response.images().get(1));
        assertEquals(2, response.variants().size());
        assertEquals("12 GB", response.variants().get(1).ram());
        assertEquals("512 GB", response.variants().get(1).storage());
        assertEquals(32000, response.variants().get(1).price().intValue());
        assertEquals(3, response.colors().size());
        assertEquals(2, response.reviews().size());
        assertEquals("Mobiles", response.category());
        assertTrue(response.description().contains("Test Phone"));
    }

    @Test
    void mapsAccessoryProductsSafelyWithoutNumericRam() {
        ProductRepository repository = mock(ProductRepository.class);
        ProductInventoryRepository inventory = mock(ProductInventoryRepository.class);
        Product accessory = new Product(
                "test-headphones", "Sony", "WH-1000XM5", 29990, "N/A", "N/A",
                "Touch Controls", "V1 + QN1", "4 mics", "30 hrs",
                4.8, 2024, "https://example.com/headphones.jpg", "https://example.com/fallback.jpg",
                "Accessories");
        when(repository.findAll()).thenReturn(List.of(accessory));
        ProductCatalogService service = new ProductCatalogService(repository, inventory);

        ProductResponse response = service.findAll().get(0);

        assertEquals("test-headphones", response.id());
        assertEquals("Accessories", response.category());
        assertEquals(1, response.variants().size());
        assertEquals("Standard", response.variants().get(0).ram());
    }
}
