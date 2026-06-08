/* =========================================================
   create-edit-list.js — List creation / editing page scripts
   Manages dynamic addition and removal of item rows.
   ========================================================= */

'use strict';

var itemIndex = 0;

/** Build a new item-row element and append it to the items container. */
function createItemRow() {
    var container = document.getElementById('items-container');
    if (!container) return;

    var div = document.createElement('div');
    div.className = 'item-row';
    div.setAttribute('draggable', 'true');

    div.innerHTML =
        '<div class="drag-handle" style="cursor: grab; display: inline-block; margin-right: 10px;">☰</div>' +
        '<button type="button" class="btn-remove-item" aria-label="Remove item">&times;</button>' +
        '<div class="item-row-field">' +
            '<label>Item Title</label>' +
            '<input type="text" name="items[' + itemIndex + '].title" placeholder="e.g. The Matrix" required>' +
        '</div>' +
        '<div class="item-row-field">' +
            '<label>Description</label>' +
            '<textarea name="items[' + itemIndex + '].description" rows="2" placeholder="Why is this on your list?"></textarea>' +
        '</div>' +
        '<div class="item-row-field">' +
            '<label>Photo (Optional)</label>' +
            '<div class="file-upload-zone">' +
                '<span class="drop-zone-text">Drag & Drop an image here or click to select</span>' +
                '<input type="file" accept="image/*" class="file-upload-input drop-zone-input" data-target="item-photo-' + itemIndex + '">' +
            '</div>' +
            '<div class="url-input-zone">' +
                '<span>OR provide a URL:</span>' +
                '<input type="text" name="items[' + itemIndex + '].photo" id="item-photo-' + itemIndex + '" placeholder="https://">' +
            '</div>' +
        '</div>' +
        '<div class="item-row-field">' +
            '<label>External URL (Optional)</label>' +
            '<input type="text" name="items[' + itemIndex + '].externalUrl" class="url-input" placeholder="https://">' +
        '</div>';

    attachItemEventListeners(div);
    container.appendChild(div);
    itemIndex++;
}

function attachItemEventListeners(div) {
    var removeBtn = div.querySelector('.btn-remove-item');
    if (removeBtn) {
        removeBtn.addEventListener('click', function () {
            div.remove();
        });
    }

    /* Support both dynamically-created (.url-input) and Thymeleaf-rendered ([name$=".externalUrl"]) inputs */
    var urlInput = div.querySelector('.url-input') || div.querySelector('input[name$=".externalUrl"]');
    if (urlInput && !urlInput.classList.contains('og-bound')) {
        urlInput.classList.add('og-bound');
        urlInput.addEventListener('blur', function () {
            var url = this.value.trim();
            if (!url || !url.startsWith('http')) return;
            fetch('/og?url=' + encodeURIComponent(url))
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    var titleInput = div.querySelector('input[name*=".title"]');
                    var descInput  = div.querySelector('textarea[name*=".description"]');
                    var photoInput = div.querySelector('input[name*=".photo"]:not([type="file"])') ||
                                     div.querySelector('input[id^="item-photo-"]');
                    if (data.title && titleInput && !titleInput.value.trim()) titleInput.value = data.title;
                    if (data.description && descInput && !descInput.value.trim()) descInput.value = data.description;
                    if (data.image && photoInput && !photoInput.value.trim()) photoInput.value = data.image;
                })
                .catch(function() { /* silently ignore */ });
        });
    }

    /* Drag-and-drop: only allow dragging from the handle to avoid breaking text selection */
    var dragHandle = div.querySelector('.drag-handle');
    if (dragHandle) {
        dragHandle.addEventListener('mousedown', function() {
            div.setAttribute('draggable', 'true');
        });
        dragHandle.addEventListener('mouseup', function() {
            div.setAttribute('draggable', 'false');
        });
        dragHandle.addEventListener('mouseleave', function() {
            div.setAttribute('draggable', 'false');
        });
    }

    /* Drag-and-drop: mark row as being dragged */
    div.addEventListener('dragstart', function(e) {
        div.classList.add('dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', '');
    });
    div.addEventListener('dragend', function() {
        div.classList.remove('dragging');
        div.setAttribute('draggable', 'false');
    });
    div.addEventListener('dragover', function(e) {
        e.preventDefault();
        var container = document.getElementById('items-container');
        var afterElement = getDragAfterElement(container, e.clientY);
        if (afterElement == null) container.appendChild(div);
        else container.insertBefore(div, afterElement);
    });
}

function getDragAfterElement(container, y) {
    var draggableElements = [...container.querySelectorAll('.item-row:not(.dragging)')];
    return draggableElements.reduce((closest, child) => {
        var box = child.getBoundingClientRect();
        var offset = y - box.top - box.height / 2;
        if (offset < 0 && offset > closest.offset) return { offset: offset, element: child };
        else return closest;
    }, { offset: Number.NEGATIVE_INFINITY }).element;
}

function handleFileUpload(e) {
    if (!e.target.classList.contains('file-upload-input')) return;
    var file = e.target.files[0];
    if (!file) return;

    var targetId = e.target.getAttribute('data-target');
    var targetInput = document.getElementById(targetId);
    if (!targetInput) return;

    targetInput.value = "Uploading...";

    var formData = new FormData();
    formData.append('file', file);

    var xsrfToken = getCookie('XSRF-TOKEN');

    fetch('/upload/image', {
        method: 'POST',
        headers: {
            'X-XSRF-TOKEN': xsrfToken || ''
        },
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.url) {
            targetInput.value = data.url;
        } else {
            targetInput.value = "";
            alert(data.error || "Upload failed");
        }
    })
    .catch(err => {
        console.error(err);
        targetInput.value = "";
        alert("Upload failed");
    })
    .finally(() => {
        // Clear the file input so the same file can be selected again
        e.target.value = "";
    });
}

function getCookie(name) {
    if (name === 'XSRF-TOKEN') {
        var meta = document.querySelector('meta[name="_csrf"]');
        if (meta && meta.content) return meta.content;
    }
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length === 2) return parts.pop().split(";").shift();
}

document.addEventListener('DOMContentLoaded', function () {
    document.body.addEventListener('change', handleFileUpload);
    
    // Add visual feedback for drag and drop file uploads
    document.body.addEventListener('dragover', function(e) {
        if (e.target.classList.contains('drop-zone-input')) {
            var zone = e.target.closest('.file-upload-zone');
            if (zone) zone.classList.add('drag-over');
        }
    });
    document.body.addEventListener('dragleave', function(e) {
        if (e.target.classList.contains('drop-zone-input')) {
            var zone = e.target.closest('.file-upload-zone');
            if (zone) zone.classList.remove('drag-over');
        }
    });
    document.body.addEventListener('drop', function(e) {
        if (e.target.classList.contains('drop-zone-input')) {
            var zone = e.target.closest('.file-upload-zone');
            if (zone) zone.classList.remove('drag-over');
        }
    });

    var addBtn = document.getElementById('add-item-btn');
    if (addBtn) {
        addBtn.addEventListener('click', createItemRow);
    }
    var existingItems = document.querySelectorAll('.item-row');
    if (existingItems.length > 0) {
        existingItems.forEach(attachItemEventListeners);
        itemIndex = existingItems.length;
    } else {
        createItemRow();
    }

    // Category Proposal logic
    var proposeLink = document.getElementById('propose-category-link');
    var proposeContainer = document.getElementById('propose-category-container');
    var submitProposalBtn = document.getElementById('submit-proposal-btn');
    var proposedCategoryName = document.getElementById('proposedCategoryName');
    var proposalFeedback = document.getElementById('proposal-feedback');

    if (proposeLink && proposeContainer) {
        proposeLink.addEventListener('click', function() {
            if (proposeContainer.style.display === 'none') {
                proposeContainer.style.display = 'block';
            } else {
                proposeContainer.style.display = 'none';
            }
        });
    }

    var categorySelect = document.getElementById('category');
    if (categorySelect && proposeContainer) {
        categorySelect.addEventListener('change', function() {
            var selectedText = categorySelect.options[categorySelect.selectedIndex].text;
            if (selectedText.toLowerCase() === 'other') {
                proposeContainer.style.display = 'block';
            } else {
                proposeContainer.style.display = 'none';
            }
        });
    }

    if (submitProposalBtn) {
        submitProposalBtn.addEventListener('click', function() {
            var name = proposedCategoryName.value.trim();
            if (!name) return;

            var xsrfToken = getCookie('XSRF-TOKEN');
            var formData = new URLSearchParams();
            formData.append('proposedName', name);

            fetch('/api/categories/propose', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-XSRF-TOKEN': xsrfToken || ''
                },
                body: formData.toString()
            })
            .then(res => res.json())
            .then(data => {
                if (data.message) {
                    proposalFeedback.style.display = 'block';
                    proposalFeedback.style.color = 'var(--success-color, #10b981)';
                    proposalFeedback.innerText = data.message;
                    proposedCategoryName.value = '';
                } else if (data.error) {
                    proposalFeedback.style.display = 'block';
                    proposalFeedback.style.color = 'var(--danger-color, #ef4444)';
                    proposalFeedback.innerText = data.error;
                }
            })
            .catch(err => {
                proposalFeedback.style.display = 'block';
                proposalFeedback.style.color = 'var(--danger-color, #ef4444)';
                proposalFeedback.innerText = "Error submitting proposal";
            });
        });
    }

    var form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function() {
            var rows = document.querySelectorAll('.item-row');
            rows.forEach(function(row, index) {
                var inputs = row.querySelectorAll('input, textarea, select');
                inputs.forEach(function(input) {
                    if (input.name) {
                        // Replace items[X] with items[index]
                        input.name = input.name.replace(/items\[\d+\]/, 'items[' + index + ']');
                    }
                });
                var fileInputs = row.querySelectorAll('.file-upload-input');
                fileInputs.forEach(function(fi) {
                    var targetId = fi.getAttribute('data-target');
                    if (targetId && targetId.startsWith('item-photo-')) {
                        fi.setAttribute('data-target', 'item-photo-' + index);
                    }
                });
                var urlInputs = row.querySelectorAll('[id^="item-photo-"]');
                urlInputs.forEach(function(ui) {
                    ui.id = 'item-photo-' + index;
                });
            });
        });
    }
});
