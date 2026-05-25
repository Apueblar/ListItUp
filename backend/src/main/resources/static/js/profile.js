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

    // Avatar Upload Logic
    var btnUploadAvatar = document.getElementById('btn-upload-avatar');
    var fileInput = document.getElementById('avatar-file-input');
    var avatarPreview = document.getElementById('avatar-preview');
    var profilePictureInput = document.getElementById('profilePictureInput');

    if (btnUploadAvatar && fileInput) {
        btnUploadAvatar.addEventListener('click', function() {
            fileInput.click();
        });

        fileInput.addEventListener('change', async function() {
            if (!this.files || this.files.length === 0) return;
            var file = this.files[0];
            var formData = new FormData();
            formData.append('file', file);
            
            var xsrfToken = getCookie('XSRF-TOKEN');

            try {
                btnUploadAvatar.textContent = 'Uploading...';
                btnUploadAvatar.disabled = true;

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
                        profilePictureInput.value = data.url;
                        if (avatarPreview) {
                            avatarPreview.src = data.url;
                        } else {
                            // Create preview if it didn't exist
                            var newImg = document.createElement('img');
                            newImg.src = data.url;
                            newImg.id = 'avatar-preview';
                            newImg.style = 'width:40px; height:40px; border-radius:50%; object-fit:cover;';
                            profilePictureInput.parentNode.insertBefore(newImg, btnUploadAvatar);
                        }
                    }
                } else {
                    alert('Failed to upload image.');
                }
            } catch (err) {
                console.error('Upload error:', err);
                alert('Error uploading image.');
            } finally {
                btnUploadAvatar.textContent = 'Upload Image';
                btnUploadAvatar.disabled = false;
            }
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
                                var avatarHtml = u.profilePicture ? 
                                    `<img src="${u.profilePicture}" style="width:40px; height:40px; border-radius:50%; object-fit:cover;">` :
                                    `<span style="width:40px; height:40px; border-radius:50%; background:var(--glass-bg); display:flex; align-items:center; justify-content:center; border:1px solid var(--glass-border);">${u.username.charAt(0).toUpperCase()}</span>`;
                                
                                var userHtml = `
                                    <div style="display:flex; align-items:center; gap:1rem; padding:0.5rem; border-bottom:1px solid var(--glass-border);">
                                        ${avatarHtml}
                                        <a href="/users/${u.username}" style="flex:1; text-decoration:none; color:var(--text-primary); font-weight:bold;">${u.username}</a>
                                    </div>
                                `;
                                modalList.insertAdjacentHTML('beforeend', userHtml);
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
