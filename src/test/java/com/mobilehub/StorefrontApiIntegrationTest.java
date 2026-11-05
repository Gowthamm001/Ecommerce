package com.mobilehub;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockCookie;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import com.jayway.jsonpath.JsonPath;
import com.mobilehub.catalog.ProductInventory;
import com.mobilehub.catalog.ProductInventoryRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StorefrontApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductInventoryRepository inventory;

    @Test
    void exposesSeededCatalogAndProtectsCustomerCart() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/pages/Home-Page.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html")));

        mockMvc.perform(get("/scripts/auth.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/auth/login")));

        mockMvc.perform(get("/styles/storefront.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(41)))
                .andExpect(jsonPath("$[?(@.id == 'apple-iphone-17')].variants", hasSize(1)));

        mockMvc.perform(get("/api/products").param("category", "Mobiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(29)));

        mockMvc.perform(get("/api/products").param("category", "Laptops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        mockMvc.perform(get("/api/products").param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        mockMvc.perform(get("/api/products").param("category", "Accessories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registersSignsInAndPersistsTheCustomerCart() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "Customer",
                                  "email": "customer@example.test",
                                  "phone": "9876543210",
                                  "password": "secure-test-password",
                                  "dateOfBirth": "1995-01-01",
                                  "state": "Karnataka",
                                  "address": "Test address"
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"customer@example.test","password":"secure-test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("ACCESS_TOKEN", true))
                .andReturn();
        String jwt = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE)
                .split(";", 2)[0].substring("ACCESS_TOKEN=".length());
        String jwtHeader = new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]), StandardCharsets.UTF_8);
        assertEquals("HS256", JsonPath.read(jwtHeader, "$.alg"));
        assertNull(loginResult.getRequest().getSession(false));
        MockCookie jwtCookie = new MockCookie("ACCESS_TOKEN", jwt);
        ProductInventory phoneStock = inventory.findById("apple-iphone-17").orElseThrow();
        phoneStock.setStock(10);
        inventory.save(phoneStock);

        mockMvc.perform(get("/api/auth/me").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@example.test"));

        mockMvc.perform(post("/api/cart/items")
                        .with(csrf())
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"apple-iphone-17","variantRam":"8 GB","variantStorage":"256 GB","color":"Midnight"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.subtotal").value(82900));

        mockMvc.perform(get("/api/cart").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/orders").with(csrf()).cookie(jwtCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.subtotal").value(82900));

        assertEquals(9, inventory.findById("apple-iphone-17").orElseThrow().getStock());
        mockMvc.perform(get("/api/cart").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(0));
        mockMvc.perform(get("/api/orders").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].items[0].productName").value("Apple iPhone 17"));
    }

    @Test
    void mergesGuestCartAndLeavesItUnchangedWhenCheckoutStockIsInsufficient() throws Exception {
        MockCookie jwtCookie = registerAndSignIn("checkout-" + UUID.randomUUID() + "@example.test");
        ProductInventory phoneStock = inventory.findById("google-pixel-9").orElseThrow();
        phoneStock.setStock(2);
        inventory.save(phoneStock);

        mockMvc.perform(post("/api/orders").with(csrf()).cookie(jwtCookie))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/cart/merge")
                        .with(csrf())
                        .cookie(jwtCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":"google-pixel-9","variantRam":"","variantStorage":"","color":"","quantity":2}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.subtotal").value(119998));

        phoneStock.setStock(1);
        inventory.save(phoneStock);
        mockMvc.perform(post("/api/orders").with(csrf()).cookie(jwtCookie))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/cart").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2));
        mockMvc.perform(get("/api/orders").cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        assertEquals(1, inventory.findById("google-pixel-9").orElseThrow().getStock());
    }

    @Test
    void restrictsProductManagementToConfiguredAdminsAndSupportsCrudAndSearch() throws Exception {
        MockCookie customerCookie = registerAndSignIn("customer-" + UUID.randomUUID() + "@example.test");
        mockMvc.perform(get("/api/admin/products").cookie(customerCookie))
                .andExpect(status().isForbidden());

        MockCookie adminCookie = registerAndSignIn("admin@example.test");
        mockMvc.perform(get("/api/admin/products").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(41)));

        String product = """
                {
                  "id":"integration-test-phone",
                  "brand":"Test",
                  "model":"Integration Phone",
                  "price":25000,
                  "ram":"8 GB",
                  "storage":"256 GB",
                  "display":"6.5 inch OLED",
                  "processor":"Test processor",
                  "camera":"50 MP camera",
                  "battery":"5,000 mAh",
                  "rating":4.5,
                  "year":2025,
                  "image":"https://example.test/phone.jpg",
                  "fallbackImage":"https://example.test/fallback.jpg",
                  "stock":0
                }
                """;
        mockMvc.perform(post("/api/admin/products")
                        .with(csrf())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(product))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.fallbackImage").value("https://example.test/fallback.jpg"));

        mockMvc.perform(get("/api/products").param("q", "Integration Phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("integration-test-phone"));

        mockMvc.perform(post("/api/admin/products")
                        .with(csrf())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(product.replace("\"price\":25000", "\"price\":0")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/admin/products/integration-test-phone")
                        .with(csrf())
                        .cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(product.replace("\"price\":25000", "\"price\":27000")
                                .replace("\"stock\":0", "\"stock\":5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(27000))
                .andExpect(jsonPath("$.stock").value(5));

        mockMvc.perform(post("/api/cart/items")
                        .with(csrf())
                        .cookie(customerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"integration-test-phone","variantRam":"8 GB","variantStorage":"256 GB","color":"Midnight"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(1));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                                "/api/admin/products/integration-test-phone")
                        .with(csrf())
                        .cookie(adminCookie))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/orders").with(csrf()).cookie(customerCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].productName").value("Test Integration Phone"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                                "/api/admin/products/integration-test-phone")
                        .with(csrf())
                        .cookie(adminCookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/orders").cookie(customerCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].items[0].productName").value("Test Integration Phone"));
        mockMvc.perform(get("/api/products/integration-test-phone"))
                .andExpect(status().isNotFound());
    }

    private MockCookie registerAndSignIn(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Integration",
                                  "lastName":"Test",
                                  "email":"%s",
                                  "phone":"9876543210",
                                  "password":"secure-test-password",
                                  "dateOfBirth":"1995-01-01",
                                  "state":"Karnataka",
                                  "address":"Integration test address"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secure-test-password"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return new MockCookie("ACCESS_TOKEN",
                loginResult.getResponse().getCookie("ACCESS_TOKEN").getValue());
    }
}
