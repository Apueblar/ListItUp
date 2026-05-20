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

    /* ── Close dropdown when clicking outside ── */
    window.addEventListener('click', function (event) {
        var dropdown = document.querySelector('.user-dropdown');
        if (dropdown && !dropdown.contains(event.target)) {
            var menu = document.getElementById('avatarDropdownMenu');
            if (menu) {
                menu.classList.remove('show');
            }
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
});
