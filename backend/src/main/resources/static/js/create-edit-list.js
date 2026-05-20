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

    div.innerHTML =
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
            '<label>External URL (Optional)</label>' +
            '<input type="text" name="items[' + itemIndex + '].externalUrl" placeholder="https://">' +
        '</div>';

    div.querySelector('.btn-remove-item').addEventListener('click', function () {
        div.remove();
    });

    container.appendChild(div);
    itemIndex++;
}

document.addEventListener('DOMContentLoaded', function () {
    var addBtn = document.getElementById('add-item-btn');
    if (addBtn) {
        addBtn.addEventListener('click', createItemRow);
    }
    /* Start with one empty item row */
    createItemRow();
});
