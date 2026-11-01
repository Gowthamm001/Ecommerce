(() => {
    const form = document.querySelector("[data-product-form]");
    if (!form) return;

    const table = document.querySelector("[data-admin-products]");
    const status = document.querySelector("[data-admin-status]");
    const currency = new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 0
    });
    const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#39;"
    })[character]);
    let products = [];
    let editingId = null;

    function showStatus(message, isError = false) {
        status.textContent = message;
        status.classList.toggle("is-error", isError);
        status.classList.toggle("is-success", !isError);
    }

    async function request(url, options = {}) {
        const headers = { Accept: "application/json", ...options.headers };
        if (options.body) headers["Content-Type"] = "application/json";
        if (options.method && options.method !== "GET") {
            const token = document.cookie.split("; ").find((cookie) => cookie.startsWith("XSRF-TOKEN="))?.split("=")[1];
            if (token) headers["X-XSRF-TOKEN"] = decodeURIComponent(token);
        }
        const response = await fetch(url, { ...options, headers, credentials: "same-origin" });
        if (!response.ok) {
            let message = `Request failed (${response.status}).`;
            try {
                const payload = await response.json();
                message = payload.message || payload.detail || message;
            } catch (error) {
                console.error("Could not read the product-management error response.", error);
            }
            throw new Error(message);
        }
        return response.status === 204 ? null : response.json();
    }

    function renderProducts() {
        const query = document.querySelector("[data-admin-search]").value.trim().toLowerCase();
        const visible = products.filter((product) =>
            `${product.id} ${product.brand} ${product.model}`.toLowerCase().includes(query));
        document.querySelector("[data-admin-product-count]").textContent =
            `${visible.length} ${visible.length === 1 ? "product" : "products"}`;
        if (visible.length === 0) {
            table.innerHTML = '<tr><td colspan="4">No products match your search.</td></tr>';
            return;
        }
        table.innerHTML = visible.map((product) =>
            `<tr><td><strong>${escapeHtml(product.brand)} ${escapeHtml(product.model)}</strong><small>${escapeHtml(product.id)}</small></td><td>${currency.format(product.price)}</td><td>${Number(product.stock)}</td><td><div class="management-row-actions"><button class="button button-outline" type="button" data-edit-product="${escapeHtml(product.id)}">Edit</button><button class="button button-outline" type="button" data-delete-product="${escapeHtml(product.id)}">Delete</button></div></td></tr>`
        ).join("");
    }

    async function loadProducts() {
        products = await request("/api/admin/products");
        renderProducts();
    }

    function resetForm() {
        editingId = null;
        form.reset();
        form.elements.stock.value = "0";
        form.elements.id.readOnly = false;
        document.querySelector("[data-product-form-title]").textContent = "Add a product";
        form.querySelector('[type="submit"]').textContent = "Save product";
    }

    function editProduct(product) {
        editingId = product.id;
        for (const field of form.elements) {
            if (field.name && Object.prototype.hasOwnProperty.call(product, field.name)) {
                field.value = product[field.name] ?? "";
            }
        }
        form.elements.id.readOnly = true;
        document.querySelector("[data-product-form-title]").textContent = `Edit ${product.model}`;
        form.querySelector('[type="submit"]').textContent = "Update product";
        form.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        const submit = form.querySelector('[type="submit"]');
        submit.disabled = true;
        const payload = Object.fromEntries(new FormData(form).entries());
        payload.price = Number(payload.price);
        payload.rating = Number(payload.rating);
        payload.year = Number(payload.year);
        payload.stock = Number(payload.stock);
        try {
            await request(editingId
                ? `/api/admin/products/${encodeURIComponent(editingId)}`
                : "/api/admin/products", {
                method: editingId ? "PUT" : "POST",
                body: JSON.stringify(payload)
            });
            showStatus(editingId ? "Product updated." : "Product added.");
            resetForm();
            await loadProducts();
        } catch (error) {
            console.error("Could not save product changes.", error);
            showStatus(error.message, true);
        } finally {
            submit.disabled = false;
        }
    });

    document.querySelectorAll("[data-product-form-reset]").forEach((button) =>
        button.addEventListener("click", resetForm));
    document.querySelector("[data-admin-search]").addEventListener("input", renderProducts);
    table.addEventListener("click", async (event) => {
        const button = event.target.closest("button");
        if (!button) return;
        if (button.dataset.editProduct) {
            const product = products.find((entry) => entry.id === button.dataset.editProduct);
            if (product) editProduct(product);
            return;
        }
        if (!button.dataset.deleteProduct) return;
        const product = products.find((entry) => entry.id === button.dataset.deleteProduct);
        if (!product || !window.confirm(`Delete ${product.brand} ${product.model}? Products in customer carts must be removed first. Order history will retain its product snapshot.`)) return;
        button.disabled = true;
        try {
            await request(`/api/admin/products/${encodeURIComponent(product.id)}`, { method: "DELETE" });
            showStatus("Product deleted.");
            await loadProducts();
            if (editingId === product.id) resetForm();
        } catch (error) {
            console.error("Could not delete the product.", error);
            showStatus(error.message, true);
            button.disabled = false;
        }
    });

    (async () => {
        try {
            await request("/api/auth/csrf");
            const account = await request("/api/auth/me");
            if (!account.admin) {
                throw new Error("You do not have permission to manage products.");
            }
            await loadProducts();
        } catch (error) {
            console.error("Could not open product management.", error);
            showStatus(error.message, true);
            table.innerHTML = `<tr><td colspan="4">${escapeHtml(error.message)} <a href="Login-Page.html">Sign in</a></td></tr>`;
        }
    })();
})();
