/* =========================================================
   list-detail.js — List detail page interactive scripts
   The list UUID is read from data-list-id on the <main> element,
   so no Thymeleaf inline JS is needed.
   ========================================================= */

'use strict';

/**
 * POST to /lists/{listId}/{endpoint} and toggle the button appearance.
 * @param {string} endpoint      e.g. 'like', 'save', 'pin'
 * @param {string} btnId         The button element's id
 * @param {string} activeText    Text to show when the action is active
 * @param {string} inactiveText  Text to show when the action is inactive
 */
/** Helper to get a cookie value by name */
function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length === 2) return parts.pop().split(";").shift();
}

async function toggleInteraction(endpoint, btnId, activeText, inactiveText) {
    var main   = document.querySelector('main[data-list-id]');
    var listId = main ? main.getAttribute('data-list-id') : null;
    if (!listId) return;

    var xsrfToken = getCookie('XSRF-TOKEN');

    try {
        var response = await fetch('/lists/' + listId + '/' + endpoint, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': xsrfToken || ''
            }
        });

        if (response.ok) {
            var data = await response.json();
            var btn  = document.getElementById(btnId);
            if (data.status && data.status.indexOf('un') === 0) {
                btn.textContent = inactiveText;
                btn.classList.remove('btn-interaction--active');
            } else {
                btn.textContent = activeText;
                btn.classList.add('btn-interaction--active');
            }
        } else if (response.status === 401 || response.status === 403) {
            window.location.href = '/oauth2/authorization/google';
        }
    } catch (err) {
        console.error('Interaction error:', err);
    }
}

document.addEventListener('DOMContentLoaded', function () {
    var btnLike = document.getElementById('btn-like');
    var btnSave = document.getElementById('btn-save');
    var btnPin  = document.getElementById('btn-pin');

    if (btnLike) {
        btnLike.addEventListener('click', function () {
            toggleInteraction('like', 'btn-like', '❤️ Liked', '🤍 Like');
        });
    }

    if (btnSave) {
        btnSave.addEventListener('click', function () {
            toggleInteraction('save', 'btn-save', '🏷️ Saved', '🔖 Save');
        });
    }

    if (btnPin) {
        btnPin.addEventListener('click', function () {
            toggleInteraction('pin', 'btn-pin', '📌 Pinned', '📌 Pin');
        });
    }
});
