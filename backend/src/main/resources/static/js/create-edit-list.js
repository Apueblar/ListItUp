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
            '<div style="display: flex; gap: 0.5rem; align-items: center;">' +
                '<input type="text" name="items[' + itemIndex + '].photo" id="item-photo-' + itemIndex + '" placeholder="https://" style="flex: 1;">' +
                '<span style="font-weight: bold;">OR</span>' +
                '<input type="file" accept="image/*" class="file-upload-input" data-target="item-photo-' + itemIndex + '">' +
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

    /* Drag-and-drop: mark row as being dragged */
    div.addEventListener('dragstart', function(e) {
        div.classList.add('dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/plain', '');
    });
    div.addEventListener('dragend', function() {
        div.classList.remove('dragging');
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
    });
}

function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length === 2) return parts.pop().split(";").shift();
}

document.addEventListener('DOMContentLoaded', function () {
    document.body.addEventListener('change', handleFileUpload);
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
});
