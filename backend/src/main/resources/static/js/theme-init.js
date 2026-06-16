/* =========================================================
   theme-init.js — Prevents theme flash on load
   ========================================================= */
(function() {
    var theme = localStorage.getItem('theme');
    if (theme === 'light') {
        document.documentElement.classList.add('theme-light');
    }
})();
