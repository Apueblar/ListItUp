document.addEventListener('DOMContentLoaded', function() {
    var btn = document.getElementById('btn-follow');
    if (btn) {
        btn.addEventListener('click', async function() {
            var username = btn.getAttribute('data-username');
            var isFollowing = btn.classList.contains('btn-interaction--active');
            var method = isFollowing ? 'DELETE' : 'POST';
            var endpoint = '/users/' + username + '/follow';
            
            function getCookie(name) {
                var value = "; " + document.cookie;
                var parts = value.split("; " + name + "=");
                if (parts.length === 2) return parts.pop().split(";").shift();
            }
            var xsrfToken = getCookie('XSRF-TOKEN');

            // Optimistic UI update
            if (isFollowing) {
                btn.classList.remove('btn-interaction--active');
                btn.textContent = 'Follow';
            } else {
                btn.classList.add('btn-interaction--active');
                btn.textContent = 'Following';
            }

            try {
                var response = await fetch(endpoint, {
                    method: method,
                    headers: {
                        'X-XSRF-TOKEN': xsrfToken || ''
                    }
                });
                
                if (!response.ok) {
                    throw new Error('Failed');
                }
            } catch(e) {
                // Revert on error
                if (isFollowing) {
                    btn.classList.add('btn-interaction--active');
                    btn.textContent = 'Following';
                } else {
                    btn.classList.remove('btn-interaction--active');
                    btn.textContent = 'Follow';
                }
            }
        });
    }
});
