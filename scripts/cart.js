(() => {
    const storageKey = "think-mart-cart";
    let authenticated = false;
    const currency = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    });
    const formatPrice = (amount) => currency.format(amount);
    const csrfToken = () => document.cookie.split("; ").find((cookie) => cookie.startsWith("XSRF-TOKEN="))?.split("=")[1];

    async function apiRequest(url, options = {}) {
        const headers = { "Accept": "application/json", ...options.headers };
        if (options.body) headers["Content-Type"] = "application/json";
        if (options.method && options.method !== "GET") {
            const token = csrfToken();
            if (token) headers["X-XSRF-TOKEN"] = decodeURIComponent(token);
        }
        const response = await fetch(url, { ...options, headers, credentials: "same-origin" });
        if (!response.ok) {
            let message = `Request failed (${response.status}).`;
            try {
                const payload = await response.json();
                message = payload.message || payload.detail || message;
            } catch (error) {
                console.error("Could not read the server error response.", error);
            }
            const requestError = new Error(message);
            requestError.status = response.status;
            throw requestError;
        }
        return response.status === 204 ? null : response.json();
    }

    function readLocalCart() {
        let saved;
        try {
            saved = localStorage.getItem(storageKey);
        } catch (error) {
            console.error("Could not access the saved shopping cart.", error);
            return [];
        }
        if (!saved) return [];
        try {
            const parsed = JSON.parse(saved);
            if (!Array.isArray(parsed)) throw new TypeError("Saved cart data is not a list.");
            return parsed.filter((item) => item && typeof item.id === "string" &&
                typeof item.name === "string" && Number.isFinite(Number(item.price)) &&
                Number(item.price) > 0 && Number.isFinite(Number(item.quantity)) &&
                Number(item.quantity) > 0).map((item) => ({
                id: item.id,
                productId: item.productId || item.id.split("::")[0],
                name: item.name,
                price: Number(item.price),
                image: typeof item.image === "string" ? item.image : "",
                fallback: typeof item.fallback === "string" ? item.fallback : "",
                variantRam: item.variantRam || "",
                variantStorage: item.variantStorage || "",
                color: item.color || "",
                quantity: Math.min(99, Math.max(1, Math.floor(Number(item.quantity))))
            }));
        } catch (error) {
            console.error("Could not read the saved shopping cart.", error);
            return [];
        }
    }

    function saveLocalCart(cart) {
        try {
            localStorage.setItem(storageKey, JSON.stringify(cart));
        } catch (error) {
            console.error("Could not save the shopping cart.", error);
            window.alert("Your cart could not be saved in this browser. Check your storage settings and try again.");
            return false;
        }
        renderCart();
        return true;
    }

    function placeholderImage(label) {
        const text = label.replace(/[&<>"']/g, "");
        const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="320" height="360" viewBox="0 0 320 360"><rect width="320" height="360" fill="#eef5fb"/><rect x="105" y="35" width="110" height="220" rx="18" fill="#173f68"/><rect x="111" y="42" width="98" height="205" rx="14" fill="#d9ebf8"/><text x="160" y="292" text-anchor="middle" fill="#173f68" font-family="Arial,sans-serif" font-size="14" font-weight="700">${text}</text></svg>`;
        return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
    }

    function showItems(items) {
        const itemList = document.querySelector("[data-cart-items]");
        if (!itemList) return;
        itemList.replaceChildren();
        if (items.length === 0) {
            const empty = document.createElement("div");
            empty.className = "empty-cart";
            empty.innerHTML = '<span aria-hidden="true">▢</span><h3>Your cart is empty.</h3><p>Explore the phones and add one when you find your match.</p><a class="button button-dark" href="Products.html">Explore phones</a>';
            itemList.append(empty);
            return;
        }

        items.forEach((item) => {
            const row = document.createElement("article");
            row.className = "cart-item";
            const image = document.createElement("img");
            image.src = item.image;
            image.alt = item.name;
            image.loading = "lazy";
            image.dataset.fallbackStep = "0";
            image.addEventListener("error", () => {
                if (image.dataset.fallbackStep === "0" && item.fallback && image.src !== item.fallback) {
                    image.dataset.fallbackStep = "1";
                    image.src = item.fallback;
                } else if (image.dataset.fallbackStep !== "2") {
                    image.dataset.fallbackStep = "2";
                    image.src = placeholderImage(item.name);
                } else {
                    image.classList.add("image-unavailable");
                }
            });

            const details = document.createElement("div");
            details.className = "cart-item-details";
            const name = document.createElement("h3");
            name.textContent = item.name;
            const unitPrice = document.createElement("p");
            unitPrice.textContent = `${formatPrice(item.price)} each`;
            const quantity = document.createElement("div");
            quantity.className = "quantity-control";
            quantity.setAttribute("aria-label", `Quantity for ${item.name}`);
            const decrease = document.createElement("button");
            decrease.type = "button";
            decrease.className = "quantity-button";
            decrease.dataset.quantityAdjust = item.id;
            decrease.dataset.quantity = Math.max(0, item.quantity - 1);
            decrease.setAttribute("aria-label", `Decrease quantity for ${item.name}`);
            decrease.textContent = "−";
            const quantityValue = document.createElement("span");
            quantityValue.textContent = item.quantity;
            const increase = document.createElement("button");
            increase.type = "button";
            increase.className = "quantity-button";
            increase.dataset.quantityAdjust = item.id;
            increase.dataset.quantity = Math.min(99, item.quantity + 1);
            increase.setAttribute("aria-label", `Increase quantity for ${item.name}`);
            increase.textContent = "+";
            quantity.append(decrease, quantityValue, increase);
            const remove = document.createElement("button");
            remove.className = "remove-item";
            remove.type = "button";
            remove.dataset.removeProduct = item.id;
            remove.textContent = "Remove";
            details.append(name, unitPrice, quantity, remove);
            const lineTotal = document.createElement("strong");
            lineTotal.className = "cart-line-total";
            lineTotal.textContent = formatPrice(item.price * item.quantity);
            row.append(image, details, lineTotal);
            itemList.append(row);
        });
    }

    function renderSummary(itemCount, subtotal, delivery, deliveryNote) {
        const badgeCount = document.querySelectorAll("[data-cart-count]");
        badgeCount.forEach((badge) => { badge.textContent = itemCount; });
        const itemCountLabel = document.querySelector("[data-cart-item-count]");
        const subtotalLabel = document.querySelector("[data-cart-subtotal]");
        const deliveryLabel = document.querySelector("[data-cart-delivery]");
        const totalLabel = document.querySelector("[data-cart-total]");
        const note = document.querySelector("[data-delivery-note]");
        const checkout = document.querySelector("[data-checkout]");
        if (itemCountLabel) itemCountLabel.textContent = `(${itemCount})`;
        if (subtotalLabel) subtotalLabel.textContent = formatPrice(subtotal);
        if (deliveryLabel) deliveryLabel.textContent = delivery === 0 && subtotal > 0 ? "Free" : formatPrice(delivery);
        if (totalLabel) totalLabel.textContent = formatPrice(subtotal + delivery);
        if (note) note.textContent = deliveryNote;
        if (checkout) checkout.disabled = itemCount === 0;
    }

    function renderCart(serverCart) {
        if (serverCart) {
            renderSummary(serverCart.itemCount, Number(serverCart.subtotal), Number(serverCart.delivery), serverCart.deliveryNote);
            showItems(serverCart.items);
            return;
        }
        const cart = readLocalCart();
        const count = cart.reduce((total, item) => total + item.quantity, 0);
        const subtotal = cart.reduce((total, item) => total + item.price * item.quantity, 0);
        const delivery = subtotal === 0 || subtotal >= 10000 ? 0 : 499;
        const note = subtotal === 0
            ? "Shipping is calculated at checkout."
            : delivery === 0
                ? "Your order qualifies for free delivery."
                : `Add ${formatPrice(10000 - subtotal)} more for free delivery.`;
        renderSummary(count, subtotal, delivery, note);
        showItems(cart);
    }

    async function refreshServerCart() {
        const cart = await apiRequest("/api/cart");
        renderCart(cart);
        return cart;
    }

    async function addProduct(productId, configuration = {}) {
        const product = (window.MobileHubProducts || []).find((entry) => entry.id === productId);
        if (!product) {
            console.error(`Cannot add unknown MobileHub product "${productId}" to the cart.`);
            return false;
        }
        const variantRam = configuration.variantRam || product.ram;
        const variantStorage = configuration.variantStorage || product.storage;
        const color = configuration.variantColor || (product.colors || ["Midnight"])[0];
        if (authenticated) {
            const cart = await apiRequest("/api/cart/items", {
                method: "POST",
                body: JSON.stringify({ productId, variantRam, variantStorage, color })
            });
            renderCart(cart);
            return true;
        }

        const price = Number(configuration.variantPrice) || product.price;
        const hasSelection = variantRam !== product.ram || variantStorage !== product.storage || color !== product.colors[0];
        const itemId = hasSelection ? `${product.id}::${variantRam}-${variantStorage}-${color}` : product.id;
        const name = hasSelection
            ? `${product.brand} ${product.model} · ${variantRam} / ${variantStorage} · ${color}`
            : `${product.brand} ${product.model}`;
        const cart = readLocalCart();
        const existing = cart.find((item) => item.id === itemId);
        const quantityForProduct = cart.filter((item) => item.productId === productId)
            .reduce((total, item) => total + item.quantity, 0);
        if (quantityForProduct >= Number(product.stock || 0)) {
            throw new Error("The requested quantity exceeds available stock.");
        }
        if (existing) {
            if (existing.quantity >= 99) throw new Error("Cart quantity cannot exceed 99.");
            existing.quantity += 1;
        } else {
            cart.push({
                id: itemId,
                productId,
                name,
                price,
                image: product.image,
                fallback: product.images[1],
                variantRam,
                variantStorage,
                color,
                quantity: 1
            });
        }
        return saveLocalCart(cart);
    }

    async function updateQuantity(itemId, quantity) {
        if (authenticated) {
            renderCart(await apiRequest(`/api/cart/items/${encodeURIComponent(itemId)}`, {
                method: "PUT",
                body: JSON.stringify({ quantity })
            }));
            return;
        }
        const cart = readLocalCart();
        const item = cart.find((entry) => entry.id === itemId);
        if (!item) return;
        if (quantity > 0) {
            const product = (window.MobileHubProducts || []).find((entry) => entry.id === item.productId);
            const otherQuantity = cart.filter((entry) => entry.productId === item.productId && entry.id !== itemId)
                .reduce((total, entry) => total + entry.quantity, 0);
            if (!product || quantity + otherQuantity > Number(product.stock || 0)) {
                throw new Error("The requested quantity exceeds available stock.");
            }
        }
        saveLocalCart(quantity < 1 ? cart.filter((entry) => entry.id !== itemId) : cart.map((entry) =>
            entry.id === itemId ? { ...entry, quantity: Math.min(99, quantity) } : entry));
    }

    async function migrateGuestCart() {
        const guestCart = readLocalCart();
        if (guestCart.length === 0) return;
        const serverCart = await apiRequest("/api/cart/merge", {
            method: "POST",
            body: JSON.stringify({
                items: guestCart.map((item) => ({
                    productId: item.productId,
                    variantRam: item.variantRam,
                    variantStorage: item.variantStorage,
                    color: item.color,
                    quantity: item.quantity
                }))
            })
        });
        try {
            localStorage.removeItem(storageKey);
        } catch (error) {
            console.error("Could not clear the transferred guest cart.", error);
            throw new Error("Your cart was transferred, but local guest-cart data could not be cleared. Please contact support before retrying.");
        }
        authenticated = true;
        renderCart(serverCart);
    }

    async function removeItem(itemId) {
        if (authenticated) {
            renderCart(await apiRequest(`/api/cart/items/${encodeURIComponent(itemId)}`, { method: "DELETE" }));
            return;
        }
        saveLocalCart(readLocalCart().filter((entry) => entry.id !== itemId));
    }

    document.addEventListener("click", async (event) => {
        const button = event.target.closest("button");
        if (!button) return;
        try {
            if (button.matches(".add-to-cart")) {
                const added = await addProduct(button.dataset.productId, button.dataset);
                if (!added) return;
                const originalText = button.textContent;
                button.textContent = "Added";
                window.setTimeout(() => { button.textContent = originalText; }, 1100);
            } else if (button.matches("[data-buy-now]")) {
                if (await addProduct(button.dataset.buyNow, button.dataset)) window.location.href = "Cart.html";
            } else if (button.matches("[data-quantity-adjust]")) {
                await updateQuantity(button.dataset.quantityAdjust, Number(button.dataset.quantity));
            } else if (button.matches("[data-remove-product]")) {
                await removeItem(button.dataset.removeProduct);
            }
        } catch (error) {
            console.error("Could not update the shopping cart.", error);
            window.alert(error.message);
        }
    });

    document.querySelector("[data-checkout]")?.addEventListener("click", async (event) => {
        if (!authenticated) {
            window.location.href = "Login-Page.html?next=Cart.html";
            return;
        }
        const button = event.currentTarget;
        button.disabled = true;
        try {
            const order = await apiRequest("/api/orders", { method: "POST" });
            window.location.href = `Order-History.html?placed=${encodeURIComponent(order.id)}`;
        } catch (error) {
            console.error("Could not place the order.", error);
            window.alert(error.message);
            button.disabled = false;
        }
    });

    window.addEventListener("storage", () => {
        if (!authenticated) renderCart();
    });
    window.MobileHubCart = { add: addProduct, migrateGuestCart };

    (async () => {
        try {
            await apiRequest("/api/auth/csrf");
        } catch (error) {
            console.error("Could not initialize account security tokens.", error);
            renderCart();
            return;
        }
        try {
            const user = await apiRequest("/api/auth/me");
            authenticated = Boolean(user && user.id);
        } catch (error) {
            if (error.status === 401) {
                authenticated = false;
                renderCart();
                return;
            }
            console.error("Could not load the signed-in account.", error);
            return;
        }
        try {
            await refreshServerCart();
        } catch (error) {
            console.error("Could not load the saved shopping cart.", error);
            const itemList = document.querySelector("[data-cart-items]");
            if (itemList) itemList.innerHTML = '<p class="no-results">Your saved cart could not be loaded. Refresh and try again.</p>';
        }
    })();
})();
