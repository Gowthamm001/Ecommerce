(() => {
    let products = window.MobileHubProducts || [];
    const currency = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    });
    const params = new URLSearchParams(window.location.search);
    const formatPrice = (amount) => currency.format(amount);
    const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#39;"
    })[character]);
    const brandLabel = (brand) => brand === "Xiaomi (Redmi)" ? "Redmi" : brand;
    const fallbackFor = (product) => {
        const label = `${product.brand} ${product.model}`.replace(/[&<>"']/g, "");
        const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="640" height="760" viewBox="0 0 640 760"><rect width="640" height="760" fill="#eef5fb"/><rect x="205" y="75" width="230" height="440" rx="34" fill="#173f68"/><rect x="217" y="90" width="206" height="410" rx="25" fill="#d9ebf8"/><circle cx="320" cy="484" r="6" fill="#173f68"/><text x="320" y="590" text-anchor="middle" fill="#173f68" font-family="Arial,sans-serif" font-size="24" font-weight="700">${label}</text><text x="320" y="630" text-anchor="middle" fill="#47708f" font-family="Arial,sans-serif" font-size="15">MobileHub product image unavailable</text></svg>`;
        return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
    };

    function imageMarkup(product, className = "") {
        const images = Array.isArray(product.images) ? product.images : [];
        return `<img class="${escapeHtml(className)}" src="${escapeHtml(product.image || images[0] || fallbackFor(product))}" data-fallback="${escapeHtml(images[1] || "")}" data-last-resort="${escapeHtml(fallbackFor(product))}" alt="${escapeHtml(`${product.brand} ${product.model}`)}" loading="lazy">`;
    }

    function cardMarkup(product) {
        const productUrl = `Product-Details.html?id=${encodeURIComponent(product.id)}`;
        const outOfStock = Number(product.stock) < 1;
        const category = product.category || "Mobiles";
        const hasRamStorage = product.ram && product.ram !== "N/A" && product.storage && product.storage !== "N/A";
        const specSummary = hasRamStorage
            ? `${escapeHtml(product.ram)} RAM · ${escapeHtml(product.storage)} · ${escapeHtml(product.display)}`
            : (product.display && product.display !== "N/A"
                ? escapeHtml(product.display)
                : `${escapeHtml(category)} · Year ${escapeHtml(product.year)}`);

        const validSpecs = [
            { label: "Processor", value: product.processor },
            { label: "Camera", value: product.camera },
            { label: "Battery", value: product.battery }
        ].filter((s) => s.value && s.value !== "N/A");

        const specsHtml = validSpecs.length > 0
            ? validSpecs.map((s) => `<div><dt>${escapeHtml(s.label)}</dt><dd>${escapeHtml(s.value)}</dd></div>`).join("")
            : `<div><dt>Category</dt><dd>${escapeHtml(category)}</dd></div><div><dt>Year</dt><dd>${escapeHtml(product.year)}</dd></div><div><dt>Rating</dt><dd>★ ${Number(product.rating).toFixed(1)} / 5</dd></div>`;

        return `<article class="phone-card" data-product-id="${escapeHtml(product.id)}">
            <div class="phone-card-image">
                <a href="${productUrl}" aria-label="View ${escapeHtml(product.brand)} ${escapeHtml(product.model)} details">
                    ${imageMarkup(product)}
                </a>
                <span class="phone-card-brand">${escapeHtml(brandLabel(product.brand))}</span>
                <span class="card-category-badge">${escapeHtml(category)}</span>
            </div>
            <div class="phone-card-content">
                <div class="phone-card-title-row"><h3>${escapeHtml(product.model)}</h3><span class="phone-rating">★ ${Number(product.rating).toFixed(1)}</span></div>
                <p class="phone-card-spec">${specSummary}</p>
                <dl class="spec-list">
                    ${specsHtml}
                </dl>
                <div class="phone-price"><strong>${formatPrice(product.price)}</strong></div>
                <p class="stock-status${outOfStock ? " is-out" : ""}">${outOfStock ? "Out of stock" : `${Number(product.stock)} in stock`}</p>
                <div class="phone-card-actions">
                    <a class="button button-outline" href="${productUrl}">View details</a>
                    <button class="button button-dark add-to-cart" type="button" data-product-id="${escapeHtml(product.id)}"${outOfStock ? " disabled" : ""}>${outOfStock ? "Unavailable" : "Add to cart"}</button>
                </div>
            </div>
        </article>`;
    }

    function renderGrid(container, list, emptyMessage = "No products match those filters.") {
        if (!container) return;
        if (list.length === 0) {
            container.innerHTML = `<div class="no-results"><strong>${escapeHtml(emptyMessage)}</strong><p>Try another search or browse all products.</p></div>`;
            return;
        }
        container.innerHTML = list.map(cardMarkup).join("");
        wireImageFallbacks(container);
    }

    function wireImageFallbacks(container) {
        container.querySelectorAll("img[data-fallback]").forEach((image) => {
            image.addEventListener("error", () => {
                if (image.dataset.fallback) {
                    image.src = image.dataset.fallback;
                    image.dataset.fallback = "";
                    return;
                }
                if (image.dataset.lastResort) {
                    image.src = image.dataset.lastResort;
                    image.dataset.lastResort = "";
                    return;
                }
                image.classList.add("image-unavailable");
            });
        });
    }

    function selectedProductList() {
        const search = document.querySelector("[data-catalog-search]")?.value.trim().toLowerCase() || "";
        const activeCatPill = document.querySelector("[data-category-pill].is-active")?.dataset.categoryPill;
        const categorySelect = document.querySelector("[data-filter-category]")?.value;
        const category = (activeCatPill !== undefined && activeCatPill !== "")
            ? activeCatPill
            : (categorySelect || "");
        const brand = document.querySelector("[data-filter-brand]")?.value || "";
        const minimumPrice = Number(document.querySelector("[data-filter-min-price]")?.value || 0);
        const maximumPriceInput = document.querySelector("[data-filter-max-price]")?.value || "";
        const maximumPrice = maximumPriceInput === "" ? Number.POSITIVE_INFINITY : Number(maximumPriceInput);
        const ram = document.querySelector("[data-filter-ram]")?.value || "";
        const storage = document.querySelector("[data-filter-storage]")?.value || "";
        const camera = Number(document.querySelector("[data-filter-camera]")?.value || 0);
        const sort = document.querySelector("[data-filter-sort]")?.value || "featured";

        const filtered = products.filter((product) => {
            const prodCat = product.category || "Mobiles";
            const matchesCat = !category || prodCat.toLowerCase() === category.toLowerCase();
            const matchesSearch = !search || `${product.brand} ${product.model} ${prodCat} ${product.ram || ""} ${product.storage || ""} ${product.processor || ""}`.toLowerCase().includes(search);
            const matchesBrand = !brand || product.brand === brand;
            const matchesRAM = !ram || (product.ram && parseInt(product.ram, 10) >= Number(ram));
            const storageCapacity = (product.storage && product.storage.toLowerCase().includes("tb"))
                ? parseFloat(product.storage) * 1024
                : (product.storage ? parseInt(product.storage, 10) : 0);
            const matchesStorage = !storage || storageCapacity >= Number(storage);
            const megapixels = Number((product.camera || "").match(/(\d+)\s*MP/i)?.[1] || 0);
            const matchesCamera = !camera || megapixels >= camera;
            const matchesPrice = Number(product.price) >= minimumPrice && Number(product.price) <= maximumPrice;
            return matchesCat && matchesSearch && matchesBrand && matchesRAM && matchesStorage && matchesCamera && matchesPrice;
        });

        const sorters = {
            "price-low": (a, b) => a.price - b.price,
            "price-high": (a, b) => b.price - a.price,
            newest: (a, b) => b.year - a.year || b.price - a.price,
            rated: (a, b) => b.rating - a.rating
        };
        return filtered.sort(sorters[sort] || (() => 0));
    }

    function initCatalog() {
        const grid = document.querySelector("[data-product-grid]");
        if (!grid) return;

        const categoryPills = document.querySelectorAll("[data-category-pill]");
        const categorySelect = document.querySelector("[data-filter-category]");
        const urlCategory = params.get("category");

        if (urlCategory) {
            if (categorySelect) categorySelect.value = urlCategory;
            categoryPills.forEach((pill) => {
                pill.classList.toggle("is-active", pill.dataset.categoryPill.toLowerCase() === urlCategory.toLowerCase());
            });
        }

        const brandFilter = document.querySelector("[data-filter-brand]");
        if (brandFilter) {
            [...new Set(products.map((product) => product.brand))]
                    .sort((left, right) => brandLabel(left).localeCompare(brandLabel(right)))
                .forEach((brand) => {
                        const option = document.createElement("option");
                        option.value = brand;
                        option.textContent = brandLabel(brand);
                        brandFilter.append(option);
                    });
            brandFilter.value = params.get("brand") || "";
        }

        const searchInput = document.querySelector("[data-catalog-search]");
        if (searchInput && params.has("q")) searchInput.value = params.get("q");
        const minimumPriceInput = document.querySelector("[data-filter-min-price]");
        const maximumPriceInput = document.querySelector("[data-filter-max-price]");
        if (minimumPriceInput && params.has("minPrice")) minimumPriceInput.value = params.get("minPrice");
        if (maximumPriceInput && params.has("maxPrice")) maximumPriceInput.value = params.get("maxPrice");
        if (maximumPriceInput && !params.has("maxPrice")) {
            const legacyPriceRanges = {
                "under-20000": { min: "", max: 19999 },
                "under-25000": { min: "", max: 24999 },
                "20000-50000": { min: 20000, max: 50000 },
                "50000-100000": { min: 50001, max: 100000 },
                "over-100000": { min: 100001, max: "" }
            };
            const legacyRange = legacyPriceRanges[params.get("price")];
            if (legacyRange) {
                if (minimumPriceInput) minimumPriceInput.value = legacyRange.min;
                maximumPriceInput.value = legacyRange.max;
            }
        }
        const sortFilter = document.querySelector("[data-filter-sort]");
        if (sortFilter && params.has("sort")) sortFilter.value = params.get("sort");

        const update = () => {
            const visible = selectedProductList();
            renderGrid(grid, visible);
            const count = document.querySelector("[data-results-count]");
            if (count) count.textContent = `${visible.length} ${visible.length === 1 ? "product" : "products"}`;
        };

        categoryPills.forEach((pill) => {
            pill.addEventListener("click", () => {
                categoryPills.forEach((p) => p.classList.remove("is-active"));
                pill.classList.add("is-active");
                if (categorySelect) categorySelect.value = pill.dataset.categoryPill;
                const url = new URL(window.location.href);
                if (pill.dataset.categoryPill) {
                    url.searchParams.set("category", pill.dataset.categoryPill);
                } else {
                    url.searchParams.delete("category");
                }
                window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
                update();
            });
        });

        if (categorySelect) {
            categorySelect.addEventListener("change", () => {
                const val = categorySelect.value;
                categoryPills.forEach((pill) => {
                    pill.classList.toggle("is-active", pill.dataset.categoryPill === val);
                });
                const url = new URL(window.location.href);
                if (val) {
                    url.searchParams.set("category", val);
                } else {
                    url.searchParams.delete("category");
                }
                window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
                update();
            });
        }

        document.querySelectorAll("[data-catalog-search], [data-filter-brand], [data-filter-min-price], [data-filter-max-price], [data-filter-ram], [data-filter-storage], [data-filter-camera], [data-filter-sort]")
            .forEach((control) => control.addEventListener(control.matches("input") ? "input" : "change", update));
        document.querySelector("[data-clear-filters]")?.addEventListener("click", () => {
            document.querySelectorAll("[data-catalog-search], [data-filter-brand], [data-filter-min-price], [data-filter-max-price], [data-filter-ram], [data-filter-storage], [data-filter-camera]").forEach((control) => { control.value = ""; });
            if (categorySelect) categorySelect.value = "";
            categoryPills.forEach((p, idx) => p.classList.toggle("is-active", idx === 0));
            document.querySelector("[data-filter-sort]").value = "featured";
            const url = new URL(window.location.href);
            ["q", "brand", "minPrice", "maxPrice", "price", "sort", "category"].forEach((key) => url.searchParams.delete(key));
            window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
            update();
        });
        update();
    }

    function initHome() {
        renderGrid(document.querySelector("[data-home-featured]"),
            [...products].filter((p) => (p.category || "Mobiles") === "Mobiles").sort((a, b) => b.rating - a.rating).slice(0, 4),
            "Featured mobiles are coming soon.");
        renderGrid(document.querySelector("[data-home-laptops]"),
            [...products].filter((p) => p.category === "Laptops").slice(0, 4),
            "Top laptops will be available soon.");
        renderGrid(document.querySelector("[data-home-electronics]"),
            [...products].filter((p) => p.category === "Electronics").slice(0, 4),
            "Electronics will be available soon.");
        renderGrid(document.querySelector("[data-home-accessories]"),
            [...products].filter((p) => p.category === "Accessories").slice(0, 4),
            "Accessories will be available soon.");
        renderGrid(document.querySelector("[data-home-trending]"), [...products]
            .sort((a, b) => b.year - a.year || b.rating - a.rating).slice(0, 4), "No trending products are available yet.");
        renderGrid(document.querySelector("[data-home-deals]"), [...products]
            .filter((product) => Number(product.price) <= 50000)
            .sort((a, b) => Number(a.price) - Number(b.price)).slice(0, 4), "No value picks are available yet.");

        const brandContainer = document.querySelector("[data-popular-brands]");
        if (brandContainer) {
            const brandOrder = ["Apple", "Samsung", "Dell", "Lenovo", "Sony", "OnePlus", "Xiaomi", "ASUS", "Logitech", "Google"];
            const availableBrands = new Set(products.map((product) => product.brand));
            const brandValues = {
                Xiaomi: "Xiaomi",
                Redmi: "Xiaomi (Redmi)"
            };
            const labels = brandOrder.filter((label) => availableBrands.has(brandValues[label] || label));
            brandContainer.innerHTML = labels.map((label) =>
                `<a class="popular-brand" href="Products.html?brand=${encodeURIComponent(brandValues[label] || label)}" data-brand-link="${escapeHtml(brandValues[label] || label)}"><span>${escapeHtml(label.slice(0, 1))}</span><strong>${escapeHtml(label)}</strong></a>`
            ).join("");
        }

        document.querySelectorAll("[data-category-card]").forEach((card) => {
            card.addEventListener("click", () => {
                const cat = card.dataset.categoryCard;
                window.location.href = `Products.html${cat ? `?category=${encodeURIComponent(cat)}` : ""}`;
            });
        });
    }

    function initDetails() {
        const target = document.querySelector("[data-product-details]");
        if (!target) return;
        const product = products.find((entry) => entry.id === params.get("id"));
        if (!product) {
            target.innerHTML = '<div class="no-results"><h1>We could not find that product.</h1><p>It may have been removed from the catalogue.</p><a class="button button-dark" href="Products.html">Browse products</a></div>';
            return;
        }
        const category = product.category || "Mobiles";
        const detailImages = Array.isArray(product.images) && product.images.length
            ? product.images
            : [product.image || fallbackFor(product)];
        const outOfStock = Number(product.stock) < 1;
        const vRam = product.ram || "Standard";
        const vStorage = product.storage || "Standard";

        const specRows = [
            { label: "Category", value: category },
            { label: "Brand", value: brandLabel(product.brand) },
            { label: "Display", value: product.display },
            { label: "RAM", value: product.ram },
            { label: "Storage", value: product.storage },
            { label: "Processor", value: product.processor },
            { label: "Camera", value: product.camera },
            { label: "Battery", value: product.battery },
            { label: "Launch year", value: product.year }
        ].filter((s) => s.value && s.value !== "N/A" && s.value !== "NA");

        const specsHtml = specRows.map((s) =>
            `<div><dt>${escapeHtml(s.label)}</dt><dd>${escapeHtml(s.value)}</dd></div>`
        ).join("");

        target.innerHTML = `<p class="detail-back"><a href="Products.html?category=${encodeURIComponent(category)}">← Back to ${escapeHtml(category)}</a> · <a href="Products.html">All products</a></p>
        <section class="detail-layout">
            <div class="detail-gallery">
                <div class="detail-main-image">${imageMarkup(product, "detail-image")}</div>
                <div class="detail-thumbnails">${detailImages.map((image, index) => {
                    const fallbackImage = detailImages[index === 0 ? 1 : 0] || "";
                    return `<button type="button" class="detail-thumbnail${index === 0 ? " is-active" : ""}" data-gallery-image="${escapeHtml(image)}" data-gallery-fallback="${escapeHtml(fallbackImage)}" aria-label="Show product image ${index + 1}">${imageMarkup({ ...product, image, images: [image, fallbackImage] }, "")}</button>`;
                }).join("")}</div>
            </div>
            <div class="detail-copy">
                <div class="detail-meta-top"><span class="card-category-badge">${escapeHtml(category)}</span><p class="eyebrow">${escapeHtml(brandLabel(product.brand))}</p></div>
                <h1>${escapeHtml(product.model)}</h1>
                <p class="detail-rating">★★★★★ <strong>${Number(product.rating).toFixed(1)}</strong> <span>Customer rating</span></p>
                <p class="detail-price"><strong>${formatPrice(product.price)}</strong></p>
                <p class="stock-status${outOfStock ? " is-out" : ""}">${outOfStock ? "Out of stock" : `${Number(product.stock)} in stock`}</p>
                <p class="detail-description">${escapeHtml(product.description)}</p>
                <div class="detail-actions"><button class="button button-dark add-to-cart" type="button" data-product-id="${escapeHtml(product.id)}" data-variant-ram="${escapeHtml(vRam)}" data-variant-storage="${escapeHtml(vStorage)}" data-variant-color="Midnight"${outOfStock ? " disabled" : ""}>Add to cart</button><button class="button button-coral" type="button" data-buy-now="${escapeHtml(product.id)}" data-variant-ram="${escapeHtml(vRam)}" data-variant-storage="${escapeHtml(vStorage)}" data-variant-color="Midnight"${outOfStock ? " disabled" : ""}>Buy now</button></div>
                <p class="detail-note">Price and specifications are shown from the current official product listing.</p>
            </div>
        </section>
        <section class="detail-specs"><h2>Specifications</h2><dl class="full-spec-list">
            ${specsHtml}
        </dl></section>
        <section class="review-section"><h2>Customer reviews</h2><div class="review-grid">${(product.reviews || []).map((review) => `<article class="review-card"><p class="review-stars">${"★".repeat(review.rating)}${"☆".repeat(5 - review.rating)}</p><p>${escapeHtml(review.text)}</p><strong>${escapeHtml(review.name)}</strong></article>`).join("")}</div></section>`;

        wireImageFallbacks(target);
        const mainImage = target.querySelector(".detail-main-image img");
        target.querySelectorAll("[data-gallery-image]").forEach((button) => {
            button.addEventListener("click", () => {
                target.querySelectorAll(".detail-thumbnail").forEach((item) => item.classList.remove("is-active"));
                button.classList.add("is-active");
                mainImage.src = button.dataset.galleryImage;
                mainImage.dataset.fallback = button.dataset.galleryFallback || "";
                mainImage.dataset.lastResort = fallbackFor(product);
                mainImage.classList.remove("image-unavailable");
            });
        });
        const description = document.querySelector("[data-detail-title]");
        if (description) description.textContent = `${product.model} (${category}) | MobileHub`;
    }

    document.querySelectorAll("[data-header-search]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            event.preventDefault();
            const query = form.querySelector("input").value.trim();
            window.location.href = `Products.html${query ? `?q=${encodeURIComponent(query)}` : ""}`;
        });
    });
    document.querySelectorAll("[data-brand-link]").forEach((link) => {
        link.addEventListener("click", (event) => {
            event.preventDefault();
            window.location.href = `Products.html?brand=${encodeURIComponent(link.dataset.brandLink)}`;
        });
    });
    document.querySelectorAll("[data-category-nav-link]").forEach((link) => {
        link.addEventListener("click", (event) => {
            event.preventDefault();
            const cat = link.dataset.categoryNavLink;
            window.location.href = `Products.html${cat ? `?category=${encodeURIComponent(cat)}` : ""}`;
        });
    });
    async function initializeStorefront() {
        try {
            const response = await fetch("/api/products");
            if (!response.ok) throw new Error(`Product catalogue request failed (${response.status}).`);
            products = await response.json();
            window.MobileHubProducts = products;
            initHome();
            initCatalog();
            initDetails();
        } catch (error) {
            console.error("Could not load the MobileHub product catalogue.", error);
            document.querySelectorAll("[data-home-featured], [data-home-trending], [data-home-deals], [data-popular-brands], [data-product-grid], [data-product-details]")
                .forEach((container) => {
                    container.innerHTML = '<div class="no-results"><strong>The catalogue is temporarily unavailable.</strong><p>Please refresh the page or try again later.</p></div>';
                });
        }
    }

    initializeStorefront();
})();
