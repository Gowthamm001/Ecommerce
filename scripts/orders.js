(() => {
    const container = document.querySelector("[data-order-history]");
    if (!container) return;
    const status = document.querySelector("[data-order-status]");
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

    async function loadOrders() {
        const response = await fetch("/api/orders", { credentials: "same-origin", headers: { Accept: "application/json" } });
        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = "Login-Page.html?next=Order-History.html";
                return;
            }
            throw new Error(`Could not load order history (${response.status}).`);
        }
        const orders = await response.json();
        if (orders.length === 0) {
            container.innerHTML = '<div class="empty-cart"><h2>No orders yet</h2><p>Your placed orders will appear here.</p><a class="button button-dark" href="Products.html">Browse phones</a></div>';
            return;
        }
        container.innerHTML = orders.map((order) => {
            const placed = new Date(order.placedAt).toLocaleString("en-IN", {
                dateStyle: "medium",
                timeStyle: "short"
            });
            const rows = order.items.map((item) =>
                `<li><span><strong>${escapeHtml(item.productName)}</strong><small>${escapeHtml(item.variantRam)} · ${escapeHtml(item.variantStorage)} · ${escapeHtml(item.color)} × ${Number(item.quantity)}</small></span><strong>${currency.format(item.lineTotal)}</strong></li>`
            ).join("");
            return `<article class="order-history-card"><header><div><p class="eyebrow">ORDER #${escapeHtml(order.id)}</p><h2>${escapeHtml(placed)}</h2></div><span class="order-status">${escapeHtml(order.status)}</span></header><ul>${rows}</ul><div class="order-history-total"><span>Subtotal</span><strong>${currency.format(order.subtotal)}</strong></div><div class="order-history-total"><span>Delivery</span><strong>${currency.format(order.delivery)}</strong></div><div class="order-history-total is-grand-total"><span>Total</span><strong>${currency.format(order.total)}</strong></div></article>`;
        }).join("");
    }

    const placed = new URLSearchParams(window.location.search).get("placed");
    if (placed) status.textContent = `Order #${escapeHtml(placed)} was placed successfully.`;
    loadOrders().catch((error) => {
        console.error("Could not load order history.", error);
        container.innerHTML = '<div class="no-results"><strong>Order history is temporarily unavailable.</strong><p>Please refresh and try again.</p></div>';
    });
})();
