(function () {
    function isEmpty(field) {
        return !field.value || !field.value.trim();
    }

    function clearClientError(form, fieldId) {
        var errorEl = form.querySelector('[data-client-error-for="' + fieldId + '"]');
        if (errorEl) {
            errorEl.textContent = '';
            errorEl.hidden = true;
        }
    }

    function showClientError(form, fieldId, message) {
        var errorEl = form.querySelector('[data-client-error-for="' + fieldId + '"]');
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.hidden = false;
        }
    }

    function initAuthForm(form, validators) {
        form.setAttribute('novalidate', 'novalidate');

        form.addEventListener('submit', function (event) {
            var isValid = true;

            Object.keys(validators).forEach(function (fieldId) {
                clearClientError(form, fieldId);
            });

            Object.keys(validators).forEach(function (fieldId) {
                var field = form.querySelector('#' + fieldId);
                if (!field) {
                    return;
                }

                var message = validators[fieldId](field);
                if (message) {
                    showClientError(form, fieldId, message);
                    isValid = false;
                }
            });

            if (!isValid) {
                event.preventDefault();
            }
        });

        form.addEventListener('input', function (event) {
            if (event.target.id && validators[event.target.id]) {
                clearClientError(form, event.target.id);
            }
        });

        form.addEventListener('change', function (event) {
            if (event.target.id && validators[event.target.id]) {
                clearClientError(form, event.target.id);
            }
        });
    }

    var registerForm = document.getElementById('register-form');
    if (registerForm) {
        initAuthForm(registerForm, {
            username: function (field) {
                return isEmpty(field) ? 'Please enter a username' : '';
            },
            password: function (field) {
                return isEmpty(field) ? 'Please enter a password' : '';
            },
            email: function (field) {
                return isEmpty(field) ? 'Please enter a valid email' : '';
            },
            region: function (field) {
                return isEmpty(field) ? 'Please select a city' : '';
            }
        });
    }

    var loginForm = document.getElementById('login-form');
    if (loginForm) {
        initAuthForm(loginForm, {
            username: function (field) {
                return isEmpty(field) ? 'Please enter a username' : '';
            },
            password: function (field) {
                return isEmpty(field) ? 'Please enter a password' : '';
            }
        });
    }

    var sendBookForm = document.getElementById('send-book-form');
    if (sendBookForm) {
        initAuthForm(sendBookForm, {
            receiverUsername: function (field) {
                return isEmpty(field) ? 'Recipient username is required' : '';
            },
            bookId: function (field) {
                return isEmpty(field) ? 'Please select a book' : '';
            },
            returnDeadline: function (field) {
                return isEmpty(field) ? 'Return deadline is required' : '';
            }
        });
    }

    var editTransferForm = document.getElementById('edit-transfer-form');
    if (editTransferForm) {
        initAuthForm(editTransferForm, {
            returnDeadline: function (field) {
                if (isEmpty(field)) {
                    return 'Return deadline is required';
                }

                var minDate = field.getAttribute('data-min-date');
                if (minDate && field.value < minDate) {
                    return 'Return deadline cannot be earlier than the current deadline';
                }

                return '';
            }
        });
    }
})();
