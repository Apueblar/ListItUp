document.addEventListener('DOMContentLoaded', function() {
    
    // Helper to get CSRF token
    function getCookie(name) {
        if (name === 'XSRF-TOKEN') {
            var meta = document.querySelector('meta[name="_csrf"]');
            if (meta && meta.content) return meta.content;
        }
        var value = "; " + document.cookie;
        var parts = value.split("; " + name + "=");
        if (parts.length === 2) return parts.pop().split(";").shift();
    }

    // Avatar Upload & Cropper Logic
    var avatarContainer = document.getElementById('avatar-container');
    var fileInput = document.getElementById('avatar-file-input');
    var profilePictureInput = document.getElementById('profilePictureInput');
    
    var cropperModal = document.getElementById('cropper-modal');
    var closeCropperModal = document.getElementById('close-cropper-modal');
    var btnCancelCrop = document.getElementById('btn-cancel-crop');
    var btnSaveCrop = document.getElementById('btn-save-crop');
    var cropperImage = document.getElementById('cropper-image');
    
    var cropper = null;

    if (avatarContainer && fileInput) {
        avatarContainer.addEventListener('click', function() {
            fileInput.value = ''; // Reset
            fileInput.click();
        });

        fileInput.addEventListener('change', function(e) {
            var files = e.target.files;
            if (files && files.length > 0) {
                var reader = new FileReader();
                reader.onload = function(event) {
                    cropperImage.src = event.target.result;
                    cropperModal.style.display = 'block';
                    
                    if (cropper) {
                        cropper.destroy();
                    }
                    cropper = new Cropper(cropperImage, {
                        aspectRatio: 1,
                        viewMode: 1,
                        background: false
                    });
                };
                reader.readAsDataURL(files[0]);
            }
        });
    }

    function hideCropperModal() {
        cropperModal.style.display = 'none';
        if (cropper) {
            cropper.destroy();
            cropper = null;
        }
    }

    if (closeCropperModal) closeCropperModal.addEventListener('click', hideCropperModal);
    if (btnCancelCrop) btnCancelCrop.addEventListener('click', hideCropperModal);
    if (cropperModal) {
        window.addEventListener('click', function(e) {
            if (e.target == cropperModal) hideCropperModal();
        });
    }

    if (btnSaveCrop) {
        btnSaveCrop.addEventListener('click', async function() {
            if (!cropper) return;
            btnSaveCrop.textContent = 'Saving...';
            btnSaveCrop.disabled = true;

            cropper.getCroppedCanvas({
                width: 400,
                height: 400
            }).toBlob(async function(blob) {
                var formData = new FormData();
                formData.append('file', blob, 'avatar.png');
                
                var xsrfToken = getCookie('XSRF-TOKEN');

                try {
                    // Upload image
                    var response = await fetch('/upload/image', {
                        method: 'POST',
                        body: formData,
                        headers: {
                            'X-XSRF-TOKEN': xsrfToken || ''
                        }
                    });

                    if (response.ok) {
                        var data = await response.json();
                        if (data.url) {
                            // Update avatar via backend API form submit
                            var updateForm = document.createElement('form');
                            updateForm.method = 'POST';
                            updateForm.action = '/profile/update-avatar';
                            
                            var csrfInput = document.createElement('input');
                            csrfInput.type = 'hidden';
                            csrfInput.name = '_csrf';
                            csrfInput.value = xsrfToken;
                            updateForm.appendChild(csrfInput);

                            var picInput = document.createElement('input');
                            picInput.type = 'hidden';
                            picInput.name = 'profilePicture';
                            picInput.value = data.url;
                            updateForm.appendChild(picInput);

                            document.body.appendChild(updateForm);
                            updateForm.submit();
                        }
                    } else {
                        alert('Failed to upload image.');
                    }
                } catch (err) {
                    console.error('Upload error:', err);
                    alert('Error uploading image.');
                } finally {
                    btnSaveCrop.textContent = 'Save Picture';
                    btnSaveCrop.disabled = false;
                    hideCropperModal();
                }
            }, 'image/png');
        });
    }

    // Follow Modal Logic
    var followLinks = document.querySelectorAll('.follower-count-link');
    var followModal = document.getElementById('follow-modal');
    var closeFollowModal = document.getElementById('close-follow-modal');
    var modalTitle = document.getElementById('follow-modal-title');
    var modalList = document.getElementById('follow-modal-list');

    if (followLinks.length > 0 && followModal) {
        followLinks.forEach(function(link) {
            link.addEventListener('click', async function() {
                var username = this.getAttribute('data-username');
                var type = this.getAttribute('data-type'); // 'followers' or 'following'
                
                modalTitle.textContent = type === 'followers' ? 'Followers' : 'Following';
                modalList.innerHTML = '<p>Loading...</p>';
                followModal.style.display = 'flex';

                try {
                    var response = await fetch('/users/' + username + '/' + type);
                    if (response.ok) {
                        var users = await response.json();
                        modalList.innerHTML = '';
                        if (users.length === 0) {
                            modalList.innerHTML = '<p>No users found.</p>';
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

                                modalList.appendChild(container);
                            });
                        }
                    } else {
                        modalList.innerHTML = '<p>Error loading users.</p>';
                    }
                } catch (err) {
                    console.error('Fetch error:', err);
                    modalList.innerHTML = '<p>Error loading users.</p>';
                }
            });
        });

        if (closeFollowModal) {
            closeFollowModal.addEventListener('click', function() {
                followModal.style.display = 'none';
            });
        }

        window.addEventListener('click', function(e) {
            if (e.target === followModal) {
                followModal.style.display = 'none';
            }
        });
    }
});
