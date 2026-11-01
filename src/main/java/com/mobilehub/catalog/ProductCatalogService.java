package com.mobilehub.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCatalogService implements CommandLineRunner {
    private static final String[] PHONE_ROWS = {
        "apple-iphone-17~Apple~iPhone 17~82900~8 GB~256 GB~6.3″ Super Retina XDR OLED~A19~48 MP + 48 MP | 18 MP front~3,692 mAh~4.8~2025~apple-iphone-17~photo-1592750475338-74b7b21085ab",
        "apple-iphone-17-pro~Apple~iPhone 17 Pro~134900~12 GB~256 GB~6.3″ Super Retina XDR OLED, 120 Hz~A19 Pro~48 MP triple camera | 18 MP front~4,252 mAh~4.9~2025~apple-iphone-17-pro~photo-1605236453806-6ff36851218e",
        "apple-iphone-16~Apple~iPhone 16~69900~8 GB~128 GB~6.1″ Super Retina XDR OLED~A18~48 MP + 12 MP | 12 MP front~3,561 mAh~4.7~2024~apple-iphone-16~photo-1598327105666-5b89351aff97",
        "samsung-galaxy-s25-ultra~Samsung~Galaxy S25 Ultra~129999~12 GB~256 GB~6.9″ Dynamic AMOLED 2X, 120 Hz~Snapdragon 8 Elite~200 MP quad camera | 12 MP front~5,000 mAh~4.9~2025~photo-1610945265064-0e34e5519bbf~photo-1610945265064-0e34e5519bbf",
        "samsung-galaxy-s25~Samsung~Galaxy S25~80999~12 GB~256 GB~6.2″ Dynamic AMOLED 2X, 120 Hz~Snapdragon 8 Elite~50 MP triple camera | 12 MP front~4,000 mAh~4.8~2025~photo-1610945264803-c22b62d2a7b3~photo-1610945264803-c22b62d2a7b3",
        "samsung-galaxy-a56~Samsung~Galaxy A56~41999~8 GB~256 GB~6.7″ Super AMOLED, 120 Hz~Exynos 1580~50 MP triple camera | 12 MP front~5,000 mAh~4.5~2025~photo-1601593346740-925612772716~photo-1601593346740-925612772716",
        "oneplus-13~OnePlus~OnePlus 13~69999~12 GB~256 GB~6.82″ LTPO AMOLED, 120 Hz~Snapdragon 8 Elite~50 MP triple camera | 32 MP front~6,000 mAh~4.8~2025~oneplus-13~photo-1606041011872-596597976b25",
        "oneplus-13r~OnePlus~OnePlus 13R~42999~12 GB~256 GB~6.78″ LTPO AMOLED, 120 Hz~Snapdragon 8 Gen 3~50 MP triple camera | 16 MP front~6,000 mAh~4.7~2025~oneplus-13r~photo-1605236453806-6ff36851218e",
        "oneplus-nord-5~OnePlus~OnePlus Nord 5~31999~8 GB~256 GB~6.83″ AMOLED, 144 Hz~Snapdragon 8s Gen 3~50 MP dual camera | 50 MP front~6,800 mAh~4.5~2025~photo-1546054454-aa26e2b734c7~photo-1546054454-aa26e2b734c7",
        "xiaomi-15~Xiaomi~Xiaomi 15~64999~12 GB~512 GB~6.36″ CrystalRes AMOLED, 120 Hz~Snapdragon 8 Elite~50 MP Leica triple camera | 32 MP front~5,240 mAh~4.7~2025~xiaomi-15~photo-1603891128711-11b4b03bb138",
        "redmi-note-14-pro-plus~Xiaomi (Redmi)~Redmi Note 14 Pro+~30999~8 GB~256 GB~6.67″ AMOLED, 120 Hz~Snapdragon 7s Gen 3~200 MP triple camera | 20 MP front~6,200 mAh~4.5~2025~photo-1621330396173-e41b1cafd17f~photo-1621330396173-e41b1cafd17f",
        "redmi-14c~Xiaomi (Redmi)~Redmi 14C~10999~6 GB~128 GB~6.88″ LCD, 120 Hz~MediaTek Helio G81 Ultra~50 MP dual camera | 13 MP front~5,160 mAh~4.2~2024~xiaomi-redmi-14c~photo-1605787020600-b9ebd5df1d07",
        "realme-gt-7-pro~Realme~Realme GT 7 Pro~59999~12 GB~256 GB~6.78″ Eco² OLED Plus, 120 Hz~Snapdragon 8 Elite~50 MP triple camera | 16 MP front~5,800 mAh~4.7~2024~photo-1598327105666-5b89351aff97~photo-1598327105666-5b89351aff97",
        "realme-14-pro-plus~Realme~Realme 14 Pro+~29999~8 GB~256 GB~6.83″ OLED, 120 Hz~Snapdragon 7s Gen 3~50 MP triple camera | 32 MP front~6,000 mAh~4.5~2025~photo-1523206489230-c012c64b2b48~photo-1523206489230-c012c64b2b48",
        "realme-p3~Realme~Realme P3~16999~8 GB~128 GB~6.67″ AMOLED, 120 Hz~Snapdragon 6 Gen 4~50 MP dual camera | 16 MP front~6,000 mAh~4.3~2025~photo-1533228100845-08145b01de14~photo-1533228100845-08145b01de14",
        "vivo-x200~Vivo~Vivo X200~65999~12 GB~256 GB~6.67″ LTPO AMOLED, 120 Hz~MediaTek Dimensity 9400~50 MP Zeiss triple camera | 32 MP front~5,800 mAh~4.8~2024~vivo-x200~photo-1603891128711-11b4b03bb138",
        "vivo-v50~Vivo~Vivo V50~34999~8 GB~256 GB~6.77″ AMOLED, 120 Hz~Snapdragon 7 Gen 3~50 MP Zeiss dual camera | 50 MP front~6,000 mAh~4.6~2025~vivo-v50~photo-1512499617640-c74ae3a79d37",
        "vivo-t4~Vivo~Vivo T4~21999~8 GB~128 GB~6.77″ AMOLED, 120 Hz~Snapdragon 7s Gen 3~50 MP dual camera | 32 MP front~7,300 mAh~4.4~2025~vivo-t4~photo-1605787020600-b9ebd5df1d07",
        "oppo-find-x8~Oppo~Oppo Find X8~69999~12 GB~256 GB~6.59″ AMOLED, 120 Hz~MediaTek Dimensity 9400~50 MP Hasselblad triple camera | 32 MP front~5,630 mAh~4.7~2024~oppo-find-x8~photo-1606041011872-596597976b25",
        "oppo-reno-13-pro~Oppo~Oppo Reno 13 Pro~49999~12 GB~256 GB~6.83″ AMOLED, 120 Hz~MediaTek Dimensity 8350~50 MP triple camera | 50 MP front~5,800 mAh~4.6~2025~photo-1585060544812-6b45742d762f~photo-1585060544812-6b45742d762f",
        "oppo-k13~Oppo~Oppo K13~17999~8 GB~128 GB~6.67″ AMOLED, 120 Hz~Snapdragon 6 Gen 4~50 MP dual camera | 16 MP front~7,000 mAh~4.3~2025~photo-1565849904461-04a58ad377e0~photo-1565849904461-04a58ad377e0",
        "motorola-edge-60-pro~Motorola~Motorola Edge 60 Pro~29999~8 GB~256 GB~6.7″ pOLED, 120 Hz~MediaTek Dimensity 8350 Extreme~50 MP triple camera | 50 MP front~6,000 mAh~4.6~2025~motorola-edge-60-pro~photo-1603891128711-11b4b03bb138",
        "motorola-edge-50-fusion~Motorola~Motorola Edge 50 Fusion~22999~8 GB~128 GB~6.7″ pOLED, 144 Hz~Snapdragon 7s Gen 2~50 MP dual camera | 32 MP front~5,000 mAh~4.5~2024~motorola-edge-50-fusion~photo-1512499617640-c74ae3a79d37",
        "nothing-phone-3~Nothing~Nothing Phone (3)~79999~12 GB~256 GB~6.7″ AMOLED, 120 Hz~Snapdragon 8s Gen 4~50 MP triple camera | 50 MP front~5,500 mAh~4.7~2025~nothing-phone-3~photo-1605787020600-b9ebd5df1d07",
        "nothing-phone-3a~Nothing~Nothing Phone (3a)~24999~8 GB~128 GB~6.77″ AMOLED, 120 Hz~Snapdragon 7s Gen 3~50 MP triple camera | 32 MP front~5,000 mAh~4.5~2025~nothing-phone-3a~photo-1606041011872-596597976b25",
        "iqoo-13~iQOO~iQOO 13~54999~12 GB~256 GB~6.82″ 2K LTPO AMOLED, 144 Hz~Snapdragon 8 Elite~50 MP triple camera | 32 MP front~6,000 mAh~4.8~2024~vivo-iqoo-13~photo-1598327105666-5b89351aff97",
        "iqoo-neo-10~iQOO~iQOO Neo 10~31999~8 GB~256 GB~6.78″ AMOLED, 144 Hz~Snapdragon 8s Gen 4~50 MP dual camera | 32 MP front~7,000 mAh~4.6~2025~vivo-iqoo-neo-10~photo-1605236453806-6ff36851218e",
        "google-pixel-9~Google~Google Pixel 9~59999~12 GB~256 GB~6.3″ Actua OLED, 120 Hz~Google Tensor G4~50 MP + 48 MP | 10.5 MP front~4,700 mAh~4.7~2024~photo-1574944985070-8f3ebc6b79d2~photo-1574944985070-8f3ebc6b79d2",
        "google-pixel-9-pro~Google~Google Pixel 9 Pro~109999~16 GB~256 GB~6.3″ Super Actua LTPO OLED, 120 Hz~Google Tensor G4~50 MP triple camera | 42 MP front~4,700 mAh~4.8~2024~photo-1592750475338-74b7b21085ab~photo-1592750475338-74b7b21085ab"
    };

    private final ProductRepository products;
    private final ProductInventoryRepository inventory;

    public ProductCatalogService(ProductRepository products, ProductInventoryRepository inventory) {
        this.products = products;
        this.inventory = inventory;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String row : PHONE_ROWS) {
            String[] fields = row.split("~", -1);
            if (fields.length != 14) {
                throw new IllegalStateException("Invalid product seed row for " + fields[0]);
            }
            if (!products.existsById(fields[0])) {
                String image = imageUrl(fields[12]);
                String fallback = imageUrl(fields[13]);
                products.save(new Product(fields[0], fields[1], fields[2],
                        Integer.parseInt(fields[3]), fields[4], fields[5], fields[6],
                        fields[7], fields[8], fields[9], Double.parseDouble(fields[10]),
                        Integer.parseInt(fields[11]), image, fallback));
            }
        }
        List<Product> catalog = products.findAll();
        Map<String, ProductInventory> knownInventory = inventory.findAllById(
                catalog.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.toMap(ProductInventory::getProductId, Function.identity()));
        inventory.saveAll(catalog.stream()
                .filter(product -> !knownInventory.containsKey(product.getId()))
                .map(product -> new ProductInventory(product, 0))
                .toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return findAll("");
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(String query) {
        String term = query == null ? "" : query.trim();
        List<Product> catalog = term.isEmpty()
                ? products.findAll()
                : products.findByIdContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
                        term, term, term);
        Map<String, Integer> stockByProduct = inventory.findByProductIdIn(
                        catalog.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.toMap(ProductInventory::getProductId, ProductInventory::getStock));
        return catalog.stream().map(product -> toResponse(product,
                stockByProduct.getOrDefault(product.getId(), 0))).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(String id) {
        return products.findById(id).map(product -> toResponse(product,
                inventory.findById(id).map(ProductInventory::getStock).orElse(0))).orElse(null);
    }

    private ProductResponse toResponse(Product product, int stock) {
        int baseRam = Integer.parseInt(product.getRam().split(" ")[0]);
        String nextRam = baseRam < 12 ? "12 GB" : "16 GB";
        String nextStorage = product.getStorage().equals("512 GB") ? "1 TB" : "512 GB";
        BigDecimal nextPrice = product.getPrice().add(BigDecimal.valueOf(7000));
        String description = product.getBrand() + " " + product.getModel()
                + " pairs a vivid display with fast, dependable performance and a versatile camera system. "
                + "Specifications and sample INR pricing are provided for demonstration and should be confirmed with the retailer before purchase.";
        return new ProductResponse(
                product.getId(), product.getBrand(), product.getModel(), product.getPrice(),
                product.getOriginalPrice(), stock, product.getRam(), product.getStorage(), product.getDisplay(),
                product.getProcessor(), product.getCamera(), product.getBattery(), product.getRating(),
                product.getYear(), product.getImage(), product.getFallbackImage(),
                List.of(product.getImage(), product.getFallbackImage()),
                List.of(new ProductResponse.Variant(product.getRam(), product.getStorage(), product.getPrice()),
                        new ProductResponse.Variant(nextRam, nextStorage, nextPrice)),
                List.of("Midnight", "Silver", "Blue"), description,
                List.of(new ProductResponse.Review("Aarav M.", 5, "The display is beautiful and everyday performance feels smooth."),
                        new ProductResponse.Review("Priya S.", 4, "A lovely upgrade with a camera that captures great detail.")));
    }

    private String imageUrl(String source) {
        return source.startsWith("photo-")
                ? "https://images.unsplash.com/" + source + "?auto=format&fit=crop&w=900&q=85"
                : "https://fdn2.gsmarena.com/vv/bigpic/" + source + ".jpg";
    }
}
