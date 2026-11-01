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
        return `<article class="phone-card" data-product-id="${escapeHtml(product.id)}">
            <div class="phone-card-image">
                <a href="${productUrl}" aria-label="View ${escapeHtml(product.brand)} ${escapeHtml(product.model)} details">
                    ${imageMarkup(product)}
                </a>
                <span class="phone-card-brand">${escapeHtml(brandLabel(product.brand))}</span>
            </div>
            <div class="phone-card-content">
                <div class="phone-card-title-row"><h3>${escapeHtml(product.model)}</h3><span class="phone-rating">★ ${Number(product.rating).toFixed(1)}</span></div>
                <p class="phone-card-spec">${escapeHtml(product.ram)} RAM · ${escapeHtml(product.storage)} storage · ${escapeHtml(product.display)}</p>
                <dl class="spec-list">
                    <div><dt>Processor</dt><dd>${escapeHtml(product.processor)}</dd></div>
                    <div><dt>Camera</dt><dd>${escapeHtml(product.camera)}</dd></div>
                    <div><dt>Battery</dt><dd>${escapeHtml(product.battery)}</dd></div>
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

    function renderGrid(container, list, emptyMessage = "No phones match those filters.") {
        if (!container) return;
        if (list.length === 0) {
            container.innerHTML = `<div class="no-results"><strong>${escapeHtml(emptyMessage)}</strong><p>Try another search or browse all phones.</p></div>`;
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
        const brand = document.querySelector("[data-filter-brand]")?.value || "";
        const minimumPrice = Number(document.querySelector("[data-filter-min-price]")?.value || 0);
        const maximumPriceInput = document.querySelector("[data-filter-max-price]")?.value || "";
        const maximumPrice = maximumPriceInput === "" ? Number.POSITIVE_INFINITY : Number(maximumPriceInput);
        const ram = document.querySelector("[data-filter-ram]")?.value || "";
        const storage = document.querySelector("[data-filter-storage]")?.value || "";
        const camera = Number(document.querySelector("[data-filter-camera]")?.value || 0);
        const sort = document.querySelector("[data-filter-sort]")?.value || "featured";

        const filtered = products.filter((product) => {
            const matchesSearch = !search || `${product.brand} ${product.model} ${product.ram} ${product.storage}`.toLowerCase().includes(search);
            const matchesBrand = !brand || product.brand === brand;
            const matchesRAM = !ram || parseInt(product.ram, 10) >= Number(ram);
            const storageCapacity = product.storage.toLowerCase().includes("tb")
                ? parseFloat(product.storage) * 1024
                : parseInt(product.storage, 10);
            const matchesStorage = !storage || storageCapacity >= Number(storage);
            const megapixels = Number(product.camera.match(/(\d+)\s*MP/i)?.[1] || 0);
            const matchesCamera = !camera || megapixels >= camera;
            const matchesPrice = Number(product.price) >= minimumPrice && Number(product.price) <= maximumPrice;
            return matchesSearch && matchesBrand && matchesRAM && matchesStorage && matchesCamera && matchesPrice;
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
            if (count) count.textContent = `${visible.length} ${visible.length === 1 ? "phone" : "phones"}`;
        };
        document.querySelectorAll("[data-catalog-search], [data-filter-brand], [data-filter-min-price], [data-filter-max-price], [data-filter-ram], [data-filter-storage], [data-filter-camera], [data-filter-sort]")
            .forEach((control) => control.addEventListener(control.matches("input") ? "input" : "change", update));
        document.querySelector("[data-clear-filters]")?.addEventListener("click", () => {
            document.querySelectorAll("[data-catalog-search], [data-filter-brand], [data-filter-min-price], [data-filter-max-price], [data-filter-ram], [data-filter-storage], [data-filter-camera]").forEach((control) => { control.value = ""; });
            document.querySelector("[data-filter-sort]").value = "featured";
            const url = new URL(window.location.href);
            ["q", "brand", "minPrice", "maxPrice", "price", "sort"].forEach((key) => url.searchParams.delete(key));
            window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
            update();
        });
        update();
    }

    function initHome() {
        renderGrid(document.querySelector("[data-home-featured]"), [...products].sort((a, b) => b.rating - a.rating).slice(0, 4),
            "Featured phones are coming soon.");
        renderGrid(document.querySelector("[data-home-trending]"), [...products]
            .sort((a, b) => b.year - a.year || b.rating - a.rating).slice(0, 4), "No trending phones are available yet.");
        renderGrid(document.querySelector("[data-home-deals]"), [...products]
            .filter((product) => Number(product.price) <= 50000)
            .sort((a, b) => Number(a.price) - Number(b.price)).slice(0, 4), "No value picks are available yet.");

        const brandContainer = document.querySelector("[data-popular-brands]");
        if (brandContainer) {
            const brandOrder = ["Apple", "Samsung", "OnePlus", "Xiaomi", "Redmi", "Realme", "Vivo", "Oppo", "iQOO", "Google", "Motorola", "Nothing"];
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
    }

    function initDetails() {
        const target = document.querySelector("[data-product-details]");
        if (!target) return;
        const product = products.find((entry) => entry.id === params.get("id"));
        if (!product) {
            target.innerHTML = '<div class="no-results"><h1>We could not find that phone.</h1><p>It may have been removed from the catalogue.</p><a class="button button-dark" href="Products.html">Browse phones</a></div>';
            return;
        }
        const detailImages = Array.isArray(product.images) && product.images.length
            ? product.images
            : [product.image || fallbackFor(product)];
        const outOfStock = Number(product.stock) < 1;
        target.innerHTML = `<p class="detail-back"><a href="Products.html">← Back to all phones</a></p>
        <section class="detail-layout">
            <div class="detail-gallery">
                <div class="detail-main-image">${imageMarkup(product, "detail-image")}</div>
                <div class="detail-thumbnails">${detailImages.map((image, index) => {
                    const fallbackImage = detailImages[index === 0 ? 1 : 0] || "";
                    return `<button type="button" class="detail-thumbnail${index === 0 ? " is-active" : ""}" data-gallery-image="${escapeHtml(image)}" data-gallery-fallback="${escapeHtml(fallbackImage)}" aria-label="Show product image ${index + 1}">${imageMarkup({ ...product, image, images: [image, fallbackImage] }, "")}</button>`;
                }).join("")}</div>
            </div>
            <div class="detail-copy">
                <p class="eyebrow">${escapeHtml(brandLabel(product.brand))}</p><h1>${escapeHtml(product.model)}</h1>
                <p class="detail-rating">★★★★★ <strong>${Number(product.rating).toFixed(1)}</strong> <span>Customer rating</span></p>
                <p class="detail-price"><strong>${formatPrice(product.price)}</strong></p>
                <p class="stock-status${outOfStock ? " is-out" : ""}">${outOfStock ? "Out of stock" : `${Number(product.stock)} in stock`}</p>
                <p class="detail-description">${escapeHtml(product.description)}</p>
                <div class="detail-actions"><button class="button button-dark add-to-cart" type="button" data-product-id="${escapeHtml(product.id)}" data-variant-ram="${escapeHtml(product.ram)}" data-variant-storage="${escapeHtml(product.storage)}" data-variant-color="Midnight"${outOfStock ? " disabled" : ""}>Add to cart</button><button class="button button-coral" type="button" data-buy-now="${escapeHtml(product.id)}" data-variant-ram="${escapeHtml(product.ram)}" data-variant-storage="${escapeHtml(product.storage)}" data-variant-color="Midnight"${outOfStock ? " disabled" : ""}>Buy now</button></div>
                <p class="detail-note">Price and base specifications are shown from the current product listing.</p>
            </div>
        </section>
        <section class="detail-specs"><h2>Full specifications</h2><dl class="full-spec-list">
            <div><dt>Brand</dt><dd>${escapeHtml(brandLabel(product.brand))}</dd></div><div><dt>Display</dt><dd>${escapeHtml(product.display)}</dd></div>
            <div><dt>RAM</dt><dd>${escapeHtml(product.ram)}</dd></div><div><dt>Storage</dt><dd>${escapeHtml(product.storage)}</dd></div>
            <div><dt>Processor</dt><dd>${escapeHtml(product.processor)}</dd></div><div><dt>Camera</dt><dd>${escapeHtml(product.camera)}</dd></div>
            <div><dt>Battery</dt><dd>${escapeHtml(product.battery)}</dd></div><div><dt>Launch year</dt><dd>${escapeHtml(product.year)}</dd></div>
        </dl></section>
        <section class="review-section"><h2>Customer reviews</h2><div class="review-grid">${product.reviews.map((review) => `<article class="review-card"><p class="review-stars">${"★".repeat(review.rating)}${"☆".repeat(5 - review.rating)}</p><p>${escapeHtml(review.text)}</p><strong>${escapeHtml(review.name)}</strong></article>`).join("")}</div></section>`;

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
        if (description) description.textContent = `${product.model} specifications and price | MobileHub`;
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
