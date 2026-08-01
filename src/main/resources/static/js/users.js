(function () {
    setupDeleteDialog();
    setupRoleChangeDialog();

    function setupDeleteDialog() {
        var dialog = document.getElementById('deleteUserDialog');
        var confirmBtn = document.getElementById('deleteUserConfirmBtn');
        var cancelBtn = document.getElementById('deleteUserCancelBtn');
        var deleteButtons = document.querySelectorAll('[data-delete-user-btn]');

        if (!dialog || !confirmBtn || !cancelBtn) {
            return;
        }

        var pendingForm = null;
        var pendingButton = null;

        function openDialog(button, form) {
            pendingButton = button;
            pendingForm = form;
            dialog.hidden = false;
            cancelBtn.focus();
        }

        function closeDialog() {
            dialog.hidden = true;
            pendingForm = null;

            if (pendingButton) {
                pendingButton.focus();
                pendingButton = null;
            }
        }

        deleteButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                var form = button.closest('.user-delete-form');
                if (!form) {
                    return;
                }

                openDialog(button, form);
            });
        });

        cancelBtn.addEventListener('click', closeDialog);

        dialog.querySelectorAll('[data-delete-dialog-dismiss]').forEach(function (element) {
            element.addEventListener('click', closeDialog);
        });

        confirmBtn.addEventListener('click', function () {
            if (pendingForm) {
                pendingForm.submit();
            }
        });

        document.addEventListener('keydown', function (event) {
            if (dialog.hidden) {
                return;
            }

            if (event.key === 'Escape') {
                closeDialog();
            }
        });
    }

    function setupRoleChangeDialog() {
        var dialog = document.getElementById('roleChangeDialog');
        var message = document.getElementById('roleChangeDialogTitle');
        var confirmBtn = document.getElementById('roleChangeConfirmBtn');
        var cancelBtn = document.getElementById('roleChangeCancelBtn');
        var roleButtons = document.querySelectorAll('[data-role-change-btn]');

        if (!dialog || !message || !confirmBtn || !cancelBtn) {
            return;
        }

        var pendingForm = null;
        var pendingButton = null;

        var dialogMessages = {
            promote: 'Are you sure you want to promote this user to admin?',
            demote: 'Are you sure you want to demote this user to regular user?'
        };

        function openDialog(button, form, action) {
            pendingButton = button;
            pendingForm = form;
            message.textContent = dialogMessages[action] || 'Are you sure you want to change this user role?';
            dialog.hidden = false;
            cancelBtn.focus();
        }

        function closeDialog() {
            dialog.hidden = true;
            pendingForm = null;

            if (pendingButton) {
                pendingButton.focus();
                pendingButton = null;
            }
        }

        roleButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                var form = button.closest('.user-role-form');
                if (!form) {
                    return;
                }

                openDialog(button, form, button.getAttribute('data-role-action'));
            });
        });

        cancelBtn.addEventListener('click', closeDialog);

        dialog.querySelectorAll('[data-role-dialog-dismiss]').forEach(function (element) {
            element.addEventListener('click', closeDialog);
        });

        confirmBtn.addEventListener('click', function () {
            if (pendingForm) {
                pendingForm.submit();
            }
        });

        document.addEventListener('keydown', function (event) {
            if (dialog.hidden) {
                return;
            }

            if (event.key === 'Escape') {
                closeDialog();
            }
        });
    }
})();
