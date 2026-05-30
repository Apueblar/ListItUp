document.addEventListener('DOMContentLoaded', function() {
    function getCsrfToken() {
        var meta = document.querySelector('meta[name="_csrf"]');
        if (meta && meta.content) return meta.content;
        var value = "; " + document.cookie;
        var parts = value.split("; XSRF-TOKEN=");
        if (parts.length === 2) return parts.pop().split(";").shift();
        return '';
    }
    var xsrfToken = getCsrfToken();
    var btns = document.querySelectorAll('.btn-like-feed');
    btns.forEach(function(btn) {
        btn.addEventListener('click', async function(e) {
            e.preventDefault();
            var listId = btn.getAttribute('data-list-id');
            var span = btn.querySelector('span');
            
            var wasActive = btn.classList.contains('btn-interaction--active');
            if (wasActive) {
                span.textContent = '🤍';
                btn.classList.remove('btn-interaction--active');
            } else {
                span.textContent = '❤️';
                btn.classList.add('btn-interaction--active');
            }

            try {
                var response = await fetch('/lists/' + listId + '/like', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': xsrfToken || ''
                    }
                });

                if (!response.ok) {
                    if (wasActive) {
                        span.textContent = '❤️';
                        btn.classList.add('btn-interaction--active');
                    } else {
                        span.textContent = '🤍';
                        btn.classList.remove('btn-interaction--active');
                    }
                    if (response.status === 401 || response.status === 403) {
                        window.location.href = '/oauth2/authorization/google';
                    }
                }
            } catch (err) {
                console.error(err);
                if (wasActive) {
                    span.textContent = '❤️';
                    btn.classList.add('btn-interaction--active');
                } else {
                    span.textContent = '🤍';
                    btn.classList.remove('btn-interaction--active');
                }
            }
        });
    });
});
