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

    private static final String[] MULTI_CATEGORY_ROWS = {
        "apple-macbook-pro-14-m4~Apple~MacBook Pro 14 (M4)~169900~16 GB~512 GB~14.2″ Liquid Retina XDR, 120 Hz~Apple M4 Pro~12 MP Center Stage camera~Up to 24 hrs battery~4.9~2024~photo-1517336714731-489689fd1ca8~photo-1517336714731-489689fd1ca8~Laptops",
        "dell-xps-15~Dell~XPS 15 OLED~149999~16 GB~1 TB~15.6″ 3.5K OLED Touch, 60 Hz~Intel Core Ultra 7 155H~FHD 1080p Webcam~86 Wh battery~4.7~2024~photo-1593642632823-8f785ba67e45~photo-1593642632823-8f785ba67e45~Laptops",
        "lenovo-legion-pro-7~Lenovo~Legion Pro 7i Gen 9~199999~32 GB~1 TB~16″ WQXGA 240 Hz IPS~Intel Core i9-14900HX~1080p FHD with E-shutter~99.9 Wh battery~4.8~2024~photo-1603302576837-37561b2e2302~photo-1603302576837-37561b2e2302~Laptops",
        "asus-zenbook-14-oled~ASUS~Zenbook 14 OLED~89990~16 GB~1 TB~14″ 3K 120 Hz OLED~Intel Core Ultra 5 125H~FHD IR camera with ALS~75 Wh battery~4.6~2024~photo-1496181133206-80ce9b88a853~photo-1496181133206-80ce9b88a853~Laptops",
        "sony-wh-1000xm5~Sony~WH-1000XM5 ANC Headphones~29990~N/A~N/A~Touch Controls~Integrated Processor V1 + QN1~4 beamforming mics~30 hrs battery~4.8~2024~photo-1505740420928-5e560c06d30e~photo-1505740420928-5e560c06d30e~Electronics",
        "apple-ipad-air-m2~Apple~iPad Air 11″ (M2)~59900~8 GB~128 GB~11″ Liquid Retina True Tone~Apple M2 8-core CPU~12 MP Wide | 12 MP front~28.93 Wh battery~4.8~2024~photo-1544244015-0df4b3ffc6b0~photo-1544244015-0df4b3ffc6b0~Electronics",
        "samsung-viewfinity-s9~Samsung~ViewFinity S9 5K Monitor~119999~N/A~N/A~27″ 5K IPS Matte Display~Intelligent Picture Optimization~4K SlimFit Camera~100W Power Delivery~4.7~2024~photo-1527443224154-c4a3942d3acf~photo-1527443224154-c4a3942d3acf~Electronics",
        "apple-watch-series-10~Apple~Apple Watch Series 10~46900~N/A~64 GB~Wide-angle Always-On OLED~Apple S10 SiP~Optical heart sensor~18 hrs battery~4.7~2024~photo-1508685096489-7aacd43bd3b1~photo-1508685096489-7aacd43bd3b1~Electronics",
        "anker-prime-65w-charger~Anker~Prime 65W GaN Fast Charger~4499~N/A~N/A~Foldable Pins, Ultra-compact~GaNPrime MultiProtect~N/A~65W High-Speed Output~4.8~2024~photo-1583863788434-e58a36330cf0~photo-1583863788434-e58a36330cf0~Accessories",
        "logitech-mx-master-3s~Logitech~MX Master 3S Wireless Mouse~8995~N/A~N/A~Ergonomic Quiet Clicks~Darkfield 8000 DPI Sensor~N/A~70 days on full charge~4.9~2024~photo-1527864550417-7fd91fc51a46~photo-1527864550417-7fd91fc51a46~Accessories",
        "apple-magsafe-wallet~Apple~iPhone FineWoven Wallet with MagSafe~5900~N/A~N/A~Shielded Microtwill~Built-in MagSafe Support~N/A~Holds 3 cards securely~4.6~2024~photo-1607604276583-eef5d076aa5f~photo-1607604276583-eef5d076aa5f~Accessories",
        "spigen-ultra-hybrid-case~Spigen~Ultra Hybrid MagFit Clear Case~1899~N/A~N/A~Crystal Clear Hybrid Backing~Air Cushion Drop Defense~N/A~Drop-tested Protection~4.7~2025~photo-1601784551446-20c9e07cdbdb~photo-1601784551446-20c9e07cdbdb~Accessories"
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
        products.backfillNullCategories();

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
                        Integer.parseInt(fields[11]), image, fallback, "Mobiles"));
            }
        }

        for (String row : MULTI_CATEGORY_ROWS) {
            String[] fields = row.split("~", -1);
            if (fields.length != 15) {
                throw new IllegalStateException("Invalid multi-category seed row for " + fields[0]);
            }
            if (!products.existsById(fields[0])) {
                String image = imageUrl(fields[12]);
                String fallback = imageUrl(fields[13]);
                products.save(new Product(fields[0], fields[1], fields[2],
                        Integer.parseInt(fields[3]), fields[4], fields[5], fields[6],
                        fields[7], fields[8], fields[9], Double.parseDouble(fields[10]),
                        Integer.parseInt(fields[11]), image, fallback, fields[14]));
            }
        }

        List<Product> catalog = products.findAll();
        Map<String, ProductInventory> knownInventory = inventory.findAllById(
                catalog.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.toMap(ProductInventory::getProductId, Function.identity()));
        inventory.saveAll(catalog.stream()
                .filter(product -> !knownInventory.containsKey(product.getId()))
                .map(product -> new ProductInventory(product, 15))
                .toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return findAll("", "");
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(String query) {
        return findAll(query, "");
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(String query, String category) {
        String term = query == null ? "" : query.trim();
        String cat = category == null ? "" : category.trim();

        List<Product> catalog;
        if (cat.isEmpty()) {
            catalog = term.isEmpty()
                    ? products.findAll()
                    : products.findByIdContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
                            term, term, term);
        } else {
            catalog = term.isEmpty()
                    ? products.findByCategoryIgnoreCase(cat)
                    : products.searchByCategoryAndTerm(cat, term);
        }

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
        String ram = product.getRam() == null ? "N/A" : product.getRam().trim();
        String storage = product.getStorage() == null ? "N/A" : product.getStorage().trim();
        List<ProductResponse.Variant> variants;

        if (ram.matches("^\\d+\\s*GB.*")) {
            int baseRam = Integer.parseInt(ram.split("\\s+")[0]);
            String nextRam = baseRam < 12 ? "12 GB" : (baseRam < 16 ? "16 GB" : (baseRam < 32 ? "32 GB" : "64 GB"));
            String nextStorage = "512 GB".equalsIgnoreCase(storage) ? "1 TB"
                    : ("1 TB".equalsIgnoreCase(storage) ? "2 TB" : "512 GB");
            BigDecimal nextPrice = product.getPrice().add(BigDecimal.valueOf(7000));
            variants = List.of(
                    new ProductResponse.Variant(ram, storage, product.getPrice()),
                    new ProductResponse.Variant(nextRam, nextStorage, nextPrice));
        } else {
            variants = List.of(new ProductResponse.Variant(
                    ram.isBlank() || "N/A".equalsIgnoreCase(ram) ? "Standard" : ram,
                    storage.isBlank() || "N/A".equalsIgnoreCase(storage) ? "Standard" : storage,
                    product.getPrice()));
        }

        String category = product.getCategory();
        String description;
        if ("Mobiles".equalsIgnoreCase(category)) {
            description = product.getBrand() + " " + product.getModel()
                    + " pairs a vivid display with fast, dependable performance and a versatile camera system. "
                    + "Specifications and sample INR pricing are provided for demonstration and should be confirmed with the retailer before purchase.";
        } else if ("Laptops".equalsIgnoreCase(category)) {
            description = product.getBrand() + " " + product.getModel()
                    + " offers premium productivity, brilliant display clarity, and power efficiency for everyday computing, creative work, and gaming.";
        } else if ("Electronics".equalsIgnoreCase(category)) {
            description = product.getBrand() + " " + product.getModel()
                    + " delivers industry-leading audio-visual performance, smart connectivity, and lasting reliability.";
        } else {
            description = product.getBrand() + " " + product.getModel()
                    + " delivers quality craftsmanship, universal compatibility, and everyday convenience.";
        }

        return new ProductResponse(
                product.getId(), product.getBrand(), product.getModel(), product.getPrice(),
                product.getOriginalPrice(), stock, product.getRam(), product.getStorage(), product.getDisplay(),
                product.getProcessor(), product.getCamera(), product.getBattery(), product.getRating(),
                product.getYear(), product.getImage(), product.getFallbackImage(),
                List.of(product.getImage(), product.getFallbackImage()),
                variants,
                List.of("Midnight", "Silver", "Blue"), description,
                List.of(new ProductResponse.Review("Aarav M.", 5, "The display is beautiful and everyday performance feels smooth."),
                        new ProductResponse.Review("Priya S.", 4, "A lovely upgrade with great build quality and detail.")),
                category);
    }

    private String imageUrl(String source) {
        return source.startsWith("photo-")
                ? "https://images.unsplash.com/" + source + "?auto=format&fit=crop&w=900&q=85"
                : "https://fdn2.gsmarena.com/vv/bigpic/" + source + ".jpg";
    }
}
