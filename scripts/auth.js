(() => {
    const cookieToken = () => document.cookie.split("; ").find((cookie) => cookie.startsWith("XSRF-TOKEN="))?.split("=")[1];

    async function request(url, options = {}) {
        const headers = { "Accept": "application/json", ...options.headers };
        if (options.body) headers["Content-Type"] = "application/json";
        if (options.method && options.method !== "GET") {
            const token = cookieToken();
            if (token) headers["X-XSRF-TOKEN"] = decodeURIComponent(token);
        }
        const response = await fetch(url, { ...options, headers, credentials: "same-origin" });
        if (!response.ok) {
            let message = `Request failed (${response.status}).`;
            try {
                const payload = await response.json();
                message = payload.message || payload.detail || message;
            } catch (error) {
                console.error("Could not read the account error response.", error);
            }
            const requestError = new Error(message);
            requestError.status = response.status;
            throw requestError;
        }
        return response.status === 204 ? null : response.json();
    }

    function showMessage(form, message, isError = false) {
        const target = form.querySelector("[data-auth-status]");
        if (!target) return;
        target.textContent = message;
        target.classList.toggle("is-error", isError);
        target.classList.toggle("is-success", !isError);
    }

    async function setupAccountNavigation() {
        const nav = document.querySelector(".site-nav");
        if (!nav) return;
        try {
            await request("/api/auth/csrf");
            const user = await request("/api/auth/me");
            const orderHistory = document.createElement("a");
            orderHistory.href = "Order-History.html";
            orderHistory.textContent = "My orders";
            nav.append(orderHistory);
            if (user.admin) {
                const manageProducts = document.createElement("a");
                manageProducts.href = "Admin-Products.html";
                manageProducts.textContent = "Manage products";
                nav.append(manageProducts);
            }
            const signOut = document.createElement("button");
            signOut.type = "button";
            signOut.className = "auth-sign-out";
            signOut.textContent = `Sign out (${user.firstName})`;
            signOut.addEventListener("click", async () => {
                try {
                    await request("/api/auth/logout", { method: "POST" });
                    window.location.reload();
                } catch (error) {
                    console.error("Could not sign out.", error);
                    window.alert(error.message);
                }
            });
            nav.append(signOut);
        } catch (error) {
            if (error.status !== 401) console.error("Could not load the signed-in account.", error);
        }
    }

    const loginForm = document.querySelector("[data-login-form]");
    if (loginForm) {
        if (new URLSearchParams(window.location.search).has("registered")) {
            showMessage(loginForm, "Account created. Sign in to continue.");
        }
        loginForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const submit = loginForm.querySelector('[type="submit"]');
            submit.disabled = true;
            try {
                await request("/api/auth/csrf");
                await request("/api/auth/login", {
                    method: "POST",
                    body: JSON.stringify({
                        email: loginForm.elements.email.value.trim(),
                        password: loginForm.elements.password.value
                    })
                });
                if (window.MobileHubCart) await window.MobileHubCart.migrateGuestCart();
                const next = new URLSearchParams(window.location.search).get("next");
                window.location.href = next && /^[A-Za-z0-9_-]+\.html$/.test(next)
                    ? next : "Home-Page.html";
            } catch (error) {
                showMessage(loginForm, error.message, true);
                submit.disabled = false;
            }
        });
    }

    const registerForm = document.querySelector("[data-register-form]");
    if (registerForm) {
        registerForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            if (registerForm.elements.password.value !== registerForm.elements.confirmPassword.value) {
                showMessage(registerForm, "Passwords do not match.", true);
                return;
            }
            const submit = registerForm.querySelector('[type="submit"]');
            submit.disabled = true;
            try {
                await request("/api/auth/csrf");
                const state = registerForm.elements.state.value;
                await request("/api/auth/register", {
                    method: "POST",
                    body: JSON.stringify({
                        firstName: registerForm.elements.fname.value.trim(),
                        lastName: registerForm.elements.lname.value.trim(),
                        email: registerForm.elements.email.value.trim(),
                        phone: registerForm.elements.phone.value.trim(),
                        password: registerForm.elements.password.value,
                        dateOfBirth: registerForm.elements.dateOfBirth.value,
                        state: state === "NA" ? "" : state,
                        address: registerForm.elements.address.value.trim()
                    })
                });
                window.location.href = "Login-Page.html?registered=1";
            } catch (error) {
                showMessage(registerForm, error.message, true);
                submit.disabled = false;
            }
        });
    }

    setupAccountNavigation();
})();
