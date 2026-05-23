/* =========================================================
   app.js — Global application scripts for ListItUp
   Handles: navbar dropdown, username change confirmation & input cleaning.
   ========================================================= */

'use strict';

/** Toggle the user avatar dropdown menu visibility. */
function toggleDropdown() {
    var menu = document.getElementById('avatarDropdownMenu');
    if (menu) {
        menu.classList.toggle('show');
    }
}

/** Strip spaces and disallowed characters from any username input. */
function cleanUsernameInput(input) {
    var clean = input.value.replace(/\s+/g, '').replace(/[^a-zA-Z0-9_.]/g, '');
    if (input.value !== clean) {
        input.value = clean;
    }
}

document.addEventListener('DOMContentLoaded', function () {

    /* ── Avatar dropdown button ── */
    var avatarBtn = document.getElementById('avatarDropdownBtn');
    if (avatarBtn) {
        avatarBtn.addEventListener('click', toggleDropdown);
    }

    /* ── Close avatar dropdown when clicking outside ── */
    window.addEventListener('click', function (event) {
        var dropdown = document.querySelector('.user-dropdown');
        if (dropdown && !dropdown.contains(event.target)) {
            var menu = document.getElementById('avatarDropdownMenu');
            if (menu) menu.classList.remove('show');
        }
        /* Close notification dropdown when clicking outside */
        var notifWrapper = document.querySelector('.notif-wrapper');
        if (notifWrapper && !notifWrapper.contains(event.target)) {
            var notifDropdown = document.getElementById('notifDropdown');
            if (notifDropdown) notifDropdown.classList.remove('show');
        }
    });

    /* ── Username-change form: real-time cleaning + confirmation ── */
    var usernameForm = document.querySelector('.username-update-form');
    if (usernameForm) {
        var usernameInput = usernameForm.querySelector('input[name="username"]');

        if (usernameInput) {
            usernameInput.addEventListener('input', function () {
                cleanUsernameInput(this);
            });
        }

        usernameForm.addEventListener('submit', function (event) {
            var newName = usernameInput ? usernameInput.value.trim() : '';
            if (!window.confirm('Are you sure you want to change your username to "' + newName + '"?')) {
                event.preventDefault();
            }
        });
    }

    /* ── Notification Bell ── */
    var bellBtn      = document.getElementById('notifBellBtn');
    var notifDropdown = document.getElementById('notifDropdown');
    var notifBadge   = document.getElementById('notifBadge');
    var notifList    = document.getElementById('notifList');
    var markReadBtn  = document.getElementById('notifMarkRead');

    if (bellBtn && notifDropdown) {

        /** Render notification list items from JSON array */
        function renderNotifications(items) {
            if (!notifList) return;
            if (!items || items.length === 0) {
                notifList.innerHTML = '<li class="notif-empty">No notifications yet.</li>';
                return;
            }
            notifList.innerHTML = items.map(function (n) {
                var unreadClass = n.isRead ? '' : ' notif-unread';
                var link = n.linkUrl ? n.linkUrl : '#';
                return '<li class="notif-item' + unreadClass + '">' +
                    '<a href="' + link + '">' + escapeHtml(n.message) + '</a>' +
                '</li>';
            }).join('');
        }

        /** Update badge count */
        function updateBadge(items) {
            if (!notifBadge) return;
            var unread = (items || []).filter(function(n) { return !n.isRead; }).length;
            if (unread > 0) {
                notifBadge.textContent = unread > 9 ? '9+' : unread;
                notifBadge.style.display = 'flex';
            } else {
                notifBadge.style.display = 'none';
            }
        }

        /** Fetch notifications from API */
        function fetchNotifications() {
            fetch('/api/notifications', { credentials: 'same-origin' })
                .then(function(r) {
                    if (r.status === 401) return [];
                    return r.json();
                })
                .then(function(data) {
                    renderNotifications(data);
                    updateBadge(data);
                })
                .catch(function() {
                    if (notifList) notifList.innerHTML = '<li class="notif-empty">Could not load notifications.</li>';
                });
        }

        /** Simple HTML escaping */
        function escapeHtml(str) {
            return String(str)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;');
        }

        /* Toggle dropdown on bell click */
        bellBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            notifDropdown.classList.toggle('show');
            if (notifDropdown.classList.contains('show')) {
                fetchNotifications();
            }
        });

        /* Mark all read */
        if (markReadBtn) {
            markReadBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                var xsrf = getCsrfToken();
                fetch('/api/notifications/mark-read', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'X-XSRF-TOKEN': xsrf || '' }
                })
                .then(function() {
                    /* Visually clear unread styling */
                    document.querySelectorAll('.notif-unread').forEach(function(el) {
                        el.classList.remove('notif-unread');
                    });
                    if (notifBadge) notifBadge.style.display = 'none';
                })
                .catch(function() {});
            });
        }

        /* Fetch badge count on load (quietly) */
        fetchNotifications();
    }
});

/** Read CSRF token from cookie (used for POST requests). */
function getCsrfToken() {
    var meta = document.querySelector('meta[name="_csrf"]');
    if (meta && meta.content) return meta.content;
    var value = '; ' + document.cookie;
    var parts = value.split('; XSRF-TOKEN=');
    if (parts.length === 2) return parts.pop().split(';').shift();
    return '';
}
