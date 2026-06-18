(function () {
    var deleteBtn = document.getElementById('deleteAccountBtn');
    var dialog = document.getElementById('deleteAccountDialog');
    var confirmBtn = document.getElementById('deleteAccountConfirmBtn');
    var cancelBtn = document.getElementById('deleteAccountCancelBtn');
    var deleteForm = document.getElementById('deleteAccountForm');

    if (!deleteBtn || !dialog || !confirmBtn || !cancelBtn || !deleteForm) {
        return;
    }

    function openDialog() {
        dialog.hidden = false;
        cancelBtn.focus();
    }

    function closeDialog() {
        dialog.hidden = true;
        deleteBtn.focus();
    }

    deleteBtn.addEventListener('click', function () {
        if (deleteBtn.disabled) {
            return;
        }
        openDialog();
    });

    cancelBtn.addEventListener('click', closeDialog);

    dialog.querySelectorAll('[data-delete-dialog-dismiss]').forEach(function (element) {
        element.addEventListener('click', closeDialog);
    });

    confirmBtn.addEventListener('click', function () {
        deleteForm.submit();
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
