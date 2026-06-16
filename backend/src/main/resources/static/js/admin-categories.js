document.addEventListener('DOMContentLoaded', function() {
    function toggleEditForm(id) {
        var form = document.getElementById('edit-form-' + id);
        var deleteForm = document.getElementById('delete-form-' + id);
        if (deleteForm) deleteForm.style.display = 'none';
        if (form) {
            form.style.display = form.style.display === 'none' ? 'block' : 'none';
        }
    }

    function toggleDeleteForm(id) {
        var form = document.getElementById('delete-form-' + id);
        var editForm = document.getElementById('edit-form-' + id);
        if (editForm) editForm.style.display = 'none';
        if (form) {
            form.style.display = form.style.display === 'none' ? 'block' : 'none';
        }
    }

    document.querySelectorAll('.edit-btn, .edit-cancel-btn').forEach(function(btn) {
        btn.addEventListener('click', function() {
            var id = btn.getAttribute('data-id');
            toggleEditForm(id);
        });
    });

    document.querySelectorAll('.delete-btn, .delete-cancel-btn').forEach(function(btn) {
        btn.addEventListener('click', function() {
            var id = btn.getAttribute('data-id');
            toggleDeleteForm(id);
        });
    });
});
