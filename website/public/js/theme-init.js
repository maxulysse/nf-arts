function resolveTheme(saved) {
    if (saved === "dark" || saved === "light") return saved;
    return window.matchMedia("(prefers-color-scheme: dark)").matches
        ? "dark"
        : "light";
}

document.documentElement.setAttribute("data-bs-theme", resolveTheme(localStorage.getItem("theme")));
