(function () {
    var dialog = document.getElementById('deleteUserDialog');
    var confirmBtn = document.getElementById('deleteUserConfirmBtn');
    var cancelBtn = document.getElementById('deleteUserCancelBtn');
    var deleteButtons = document.querySelectorAll('[data-delete-user-btn]');

    if (!dialog || !confirmBtn || !cancelBtn || deleteButtons.length === 0) {
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
})();
