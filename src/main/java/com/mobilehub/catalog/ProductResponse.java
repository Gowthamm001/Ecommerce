package com.mobilehub.catalog;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        String id,
        String brand,
        String model,
        BigDecimal price,
        BigDecimal originalPrice,
        int stock,
        String ram,
        String storage,
        String display,
        String processor,
        String camera,
        String battery,
        double rating,
        int year,
        String image,
        String fallbackImage,
        List<String> images,
        List<Variant> variants,
        List<String> colors,
        String description,
        List<Review> reviews,
        String category) {

    public record Variant(String ram, String storage, BigDecimal price) { }
    public record Review(String name, int rating, String text) { }
}
