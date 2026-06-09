(function () {
    var body = document.body;
    if (!body) {
        return;
    }

    var isAuthenticated = body.dataset.authenticated === 'true';
    var loginUrl = body.dataset.loginUrl || '/login';

    document.querySelectorAll('[data-home-action]').forEach(function (link) {
        link.addEventListener('click', function (event) {
            if (!isAuthenticated) {
                event.preventDefault();
                window.location.href = loginUrl;
                return;
            }

            var targetUrl = link.getAttribute('href');
            if (!targetUrl || targetUrl === '#') {
                event.preventDefault();
            }
        });
    });
})();
