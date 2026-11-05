package com.mobilehub.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id
    private String id;
    private String brand;
    private String model;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String ram;
    private String storage;
    @Column(length = 300)
    private String display;
    @Column(length = 200)
    private String processor;
    @Column(length = 300)
    private String camera;
    private String battery;
    private double rating;
    @Column(name = "launch_year", nullable = false)
    private int year;
    @Column(length = 1000)
    private String image;
    @Column(length = 1000)
    private String fallbackImage;
    @Column(name = "category", length = 100)
    private String category;

    protected Product() {
    }

    Product(String id, String brand, String model, int price, String ram, String storage,
            String display, String processor, String camera, String battery, double rating,
            int year, String image, String fallbackImage) {
        this(id, brand, model, BigDecimal.valueOf(price),
                BigDecimal.valueOf(Math.round(price * 1.12 / 100) * 100),
                ram, storage, display, processor, camera, battery, rating, year, image, fallbackImage, "Mobiles");
    }

    Product(String id, String brand, String model, int price, String ram, String storage,
            String display, String processor, String camera, String battery, double rating,
            int year, String image, String fallbackImage, String category) {
        this(id, brand, model, BigDecimal.valueOf(price),
                BigDecimal.valueOf(Math.round(price * 1.12 / 100) * 100),
                ram, storage, display, processor, camera, battery, rating, year, image, fallbackImage, category);
    }

    Product(String id, String brand, String model, BigDecimal price, String ram, String storage,
            String display, String processor, String camera, String battery, double rating,
            int year, String image, String fallbackImage) {
        this(id, brand, model, price, price, ram, storage, display, processor,
                camera, battery, rating, year, image, fallbackImage, "Mobiles");
    }

    Product(String id, String brand, String model, BigDecimal price, String ram, String storage,
            String display, String processor, String camera, String battery, double rating,
            int year, String image, String fallbackImage, String category) {
        this(id, brand, model, price, price, ram, storage, display, processor,
                camera, battery, rating, year, image, fallbackImage, category);
    }

    private Product(String id, String brand, String model, BigDecimal price, BigDecimal originalPrice,
                    String ram, String storage, String display, String processor, String camera,
                    String battery, double rating, int year, String image, String fallbackImage,
                    String category) {
        this.id = id;
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.originalPrice = originalPrice;
        this.ram = ram;
        this.storage = storage;
        this.display = display;
        this.processor = processor;
        this.camera = camera;
        this.battery = battery;
        this.rating = rating;
        this.year = year;
        this.image = image;
        this.fallbackImage = fallbackImage;
        this.category = (category == null || category.isBlank()) ? "Mobiles" : category.trim();
    }

    void update(String brand, String model, BigDecimal price, String ram, String storage,
                String display, String processor, String camera, String battery, double rating,
                int year, String image, String fallbackImage, String category) {
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.originalPrice = price;
        this.ram = ram;
        this.storage = storage;
        this.display = display;
        this.processor = processor;
        this.camera = camera;
        this.battery = battery;
        this.rating = rating;
        this.year = year;
        this.image = image;
        this.fallbackImage = fallbackImage;
        if (category != null && !category.isBlank()) {
            this.category = category.trim();
        }
    }

    void update(String brand, String model, BigDecimal price, String ram, String storage,
                String display, String processor, String camera, String battery, double rating,
                int year, String image, String fallbackImage) {
        update(brand, model, price, ram, storage, display, processor, camera, battery, rating,
                year, image, fallbackImage, this.category);
    }

    public String getId() { return id; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public String getRam() { return ram; }
    public String getStorage() { return storage; }
    public String getDisplay() { return display; }
    public String getProcessor() { return processor; }
    public String getCamera() { return camera; }
    public String getBattery() { return battery; }
    public double getRating() { return rating; }
    public int getYear() { return year; }
    public String getImage() { return image; }
    public String getFallbackImage() { return fallbackImage; }
    public String getCategory() { return (category == null || category.isBlank()) ? "Mobiles" : category; }
    public void setCategory(String category) { this.category = (category == null || category.isBlank()) ? "Mobiles" : category.trim(); }
}
