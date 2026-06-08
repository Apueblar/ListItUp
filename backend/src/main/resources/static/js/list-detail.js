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
    if (name === 'XSRF-TOKEN') {
        var meta = document.querySelector('meta[name="_csrf"]');
        if (meta && meta.content) return meta.content;
    }
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length === 2) return parts.pop().split(";").shift();
}

async function toggleInteraction(endpoint, btnId, activeText, inactiveText) {
    var main   = document.querySelector('main[data-list-id]');
    var listId = main ? main.getAttribute('data-list-id') : null;
    if (!listId) return;

    var xsrfToken = getCookie('XSRF-TOKEN');
    var btn  = document.getElementById(btnId);
    
    // Optimistic toggle
    var wasActive = btn.classList.contains('btn-interaction--active');
    if (wasActive) {
        btn.textContent = inactiveText;
        btn.classList.remove('btn-interaction--active');
    } else {
        btn.textContent = activeText;
        btn.classList.add('btn-interaction--active');
    }

    try {
        var response = await fetch('/lists/' + listId + '/' + endpoint, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': xsrfToken || ''
            }
        });

        if (!response.ok) {
            // Revert on error
            if (wasActive) {
                btn.textContent = activeText;
                btn.classList.add('btn-interaction--active');
            } else {
                btn.textContent = inactiveText;
                btn.classList.remove('btn-interaction--active');
            }
            if (response.status === 401 || response.status === 403) {
                window.location.href = '/login';
            }
        }
    } catch (err) {
        console.error('Interaction error:', err);
        // Revert on error
        if (wasActive) {
            btn.textContent = activeText;
            btn.classList.add('btn-interaction--active');
        } else {
            btn.textContent = inactiveText;
            btn.classList.remove('btn-interaction--active');
        }
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

    var commentForm = document.getElementById('comment-form');
    if (commentForm) {
        commentForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            var input = document.getElementById('comment-input');
            var text = input.value.trim();
            if (!text) return;

            var main   = document.querySelector('main[data-list-id]');
            var listId = main ? main.getAttribute('data-list-id') : null;
            var xsrfToken = getCookie('XSRF-TOKEN');

            try {
                var response = await fetch('/lists/' + listId + '/comments', {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': xsrfToken || ''
                    },
                    body: JSON.stringify({ text: text })
                });

                if (response.ok) {
                    var data = await response.json();
                    input.value = '';
                    
                    var container = document.getElementById('comments-container');
                    var emptyMsg = document.getElementById('empty-comments-msg');
                    if (emptyMsg) emptyMsg.style.display = 'none';

                    var dateStr = new Date(data.createdAt).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });

                    var commentHtml = `
                        <div class="comment-card" id="comment-${data.commentId}">
                            <div class="comment-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                                <a class="comment-author" href="/users/${data.authorUsername}" style="text-decoration:none; font-weight: bold;">${data.authorUsername}</a>
                                <button class="btn-delete-comment" data-comment-id="${data.commentId}" style="background: none; border: none; color: #f87171; cursor: pointer; padding: 0;">🗑️</button>
                            </div>
                            <div class="comment-text">${data.text}</div>
                            <div class="comment-date">${dateStr}</div>
                        </div>
                    `;
                    container.insertAdjacentHTML('beforeend', commentHtml);
                } else if (response.status === 401 || response.status === 403) {
                    window.location.href = '/login';
                }
            } catch (err) {
                console.error('Comment error:', err);
            }
        });
    }

    var commentsContainer = document.getElementById('comments-container');
    if (commentsContainer) {
        commentsContainer.addEventListener('click', async function(e) {
            if (e.target.classList.contains('btn-delete-comment')) {
                var commentId = e.target.getAttribute('data-comment-id');
                if (!confirm('Delete this comment?')) return;

                var main   = document.querySelector('main[data-list-id]');
                var listId = main ? main.getAttribute('data-list-id') : null;
                var xsrfToken = getCookie('XSRF-TOKEN');

                try {
                    var response = await fetch('/lists/' + listId + '/comments/' + commentId, {
                        method: 'DELETE',
                        headers: { 
                            'X-XSRF-TOKEN': xsrfToken || ''
                        }
                    });

                    if (response.ok) {
                        var card = document.getElementById('comment-' + commentId);
                        if (card) card.remove();
                    } else {
                        alert('Failed to delete comment.');
                    }
                } catch (err) {
                    console.error('Delete comment error:', err);
                }
            }
        });
    }

    // Track clicks on external links
    document.querySelectorAll('.track-click').forEach(function(link) {
        link.addEventListener('click', function(e) {
            var main = document.querySelector('main[data-list-id]');
            var listId = main ? main.getAttribute('data-list-id') : null;
            var itemId = this.getAttribute('data-item-id');
            var xsrfToken = getCookie('XSRF-TOKEN');
            
            if (listId && itemId) {
                // Fire and forget
                fetch('/lists/' + listId + '/items/' + itemId + '/click', {
                    method: 'POST',
                    headers: {
                        'X-XSRF-TOKEN': xsrfToken || ''
                    }
                }).catch(err => console.error('Click tracking error:', err));
            }
        });
    });

    // Report logic
    var reportBtns = document.querySelectorAll('.btn-report-modal');
    var reportModal = document.getElementById('report-modal');
    var closeReportModal = document.querySelector('.close-modal');
    var reportForm = document.getElementById('report-form');

    if (reportBtns.length > 0 && reportModal) {
        reportBtns.forEach(function(btn) {
            btn.addEventListener('click', function() {
                var targetId = this.getAttribute('data-target-id');
                var targetType = this.getAttribute('data-target-type');
                document.getElementById('report-target-id').value = targetId;
                document.getElementById('report-target-type').value = targetType;
                reportModal.style.display = 'flex';
            });
        });

        if (closeReportModal) {
            closeReportModal.addEventListener('click', function() {
                reportModal.style.display = 'none';
            });
        }

        window.addEventListener('click', function(e) {
            if (e.target === reportModal) {
                reportModal.style.display = 'none';
            }
        });

        if (reportForm) {
            reportForm.addEventListener('submit', async function(e) {
                e.preventDefault();
                var xsrfToken = getCookie('XSRF-TOKEN');
                
                var targetId = document.getElementById('report-target-id').value;
                var targetType = document.getElementById('report-target-type').value;
                var reason = document.getElementById('report-reason').value;
                var details = document.getElementById('report-details').value;

                var payload = {
                    reason: reason,
                    details: details
                };

                if (targetType === 'LIST') payload.targetListId = targetId;
                if (targetType === 'ITEM') payload.targetItemId = targetId;
                if (targetType === 'COMMENT') payload.targetCommentId = targetId;

                try {
                    var response = await fetch('/reports', {
                        method: 'POST',
                        headers: { 
                            'Content-Type': 'application/json',
                            'X-XSRF-TOKEN': xsrfToken || ''
                        },
                        body: JSON.stringify(payload)
                    });

                    if (response.ok) {
                        showCustomAlert('Report submitted successfully. Thank you.');
                        reportModal.style.display = 'none';
                        reportForm.reset();
                    } else if (response.status === 401 || response.status === 403) {
                        window.location.href = '/login';
                    } else {
                        showCustomAlert('Failed to submit report. Please try again.');
                    }
                } catch (err) {
                    console.error('Report error:', err);
                    showCustomAlert('An error occurred. Please try again.');
                }
            });
        }
    }

    // Likes Modal Logic
    var likesLink = document.getElementById('likes-count-link');
    var likesModal = document.getElementById('likes-modal');
    var closeLikesModal = document.getElementById('close-likes-modal');
    var likesModalList = document.getElementById('likes-modal-list');

    if (likesLink && likesModal) {
        likesLink.addEventListener('click', async function() {
            var listId = this.getAttribute('data-list-id');
            likesModalList.innerHTML = '<p>Loading...</p>';
            likesModal.style.display = 'flex';

            try {
                var response = await fetch('/lists/' + listId + '/likes');
                if (response.ok) {
                    var users = await response.json();
                    likesModalList.innerHTML = '';
                    if (users.length === 0) {
                        likesModalList.innerHTML = '<p>No likes yet.</p>';
                    } else {
                        users.forEach(function(u) {
                            var container = document.createElement('div');
                            container.style.display = 'flex';
                            container.style.alignItems = 'center';
                            container.style.gap = '1rem';
                            container.style.padding = '0.5rem';
                            container.style.borderBottom = '1px solid var(--glass-border)';

                            if (u.profilePicture) {
                                var img = document.createElement('img');
                                img.src = u.profilePicture;
                                img.style.width = '40px';
                                img.style.height = '40px';
                                img.style.borderRadius = '50%';
                                img.style.objectFit = 'cover';
                                container.appendChild(img);
                            } else {
                                var span = document.createElement('span');
                                span.style.width = '40px';
                                span.style.height = '40px';
                                span.style.borderRadius = '50%';
                                span.style.background = 'var(--glass-bg)';
                                span.style.display = 'flex';
                                span.style.alignItems = 'center';
                                span.style.justifyContent = 'center';
                                span.style.border = '1px solid var(--glass-border)';
                                span.textContent = u.username.charAt(0).toUpperCase();
                                container.appendChild(span);
                            }

                            var link = document.createElement('a');
                            link.href = '/users/' + encodeURIComponent(u.username);
                            link.style.flex = '1';
                            link.style.textDecoration = 'none';
                            link.style.color = 'var(--text-primary)';
                            link.style.fontWeight = 'bold';
                            link.textContent = u.username;
                            container.appendChild(link);

                            likesModalList.appendChild(container);
                        });
                    }
                } else {
                    likesModalList.innerHTML = '<p>Error loading likes.</p>';
                }
            } catch (err) {
                console.error('Fetch error:', err);
                likesModalList.innerHTML = '<p>Error loading likes.</p>';
            }
        });

        if (closeLikesModal) {
            closeLikesModal.addEventListener('click', function() {
                likesModal.style.display = 'none';
            });
        }

        window.addEventListener('click', function(e) {
            if (e.target === likesModal) {
                likesModal.style.display = 'none';
            }
        });
    }

});
