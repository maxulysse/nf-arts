(function () {
    const saved = localStorage.getItem("theme");
    const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    const theme = saved === "dark" || saved === "light" || saved === "auto"
        ? saved
        : prefersDark ? "dark" : "light";
    document.documentElement.setAttribute("data-bs-theme", theme);
})();
