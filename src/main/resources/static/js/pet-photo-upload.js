/* Submit independently of the pet details form and keep upload failures inline. */
document.querySelectorAll('[data-pet-photo-upload]').forEach(form => {
    const error = form.parentElement.querySelector('[data-upload-error]');
    const button = form.querySelector('button[type="submit"]');

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!form.reportValidity() || button.disabled) {
            return;
        }

        error.hidden = true;
        error.textContent = '';
        button.disabled = true;
        form.setAttribute('aria-busy', 'true');
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: new FormData(form),
                headers: { Accept: 'application/json' }
            });
            if (response.ok) {
                window.location.assign(form.dataset.successUrl);
                return;
            }
            const messages = {
                400: form.dataset.invalidMessage,
                413: form.dataset.sizeMessage,
                409: form.dataset.conflictMessage
            };
            error.textContent = messages[response.status] || form.dataset.failureMessage;
            error.hidden = false;
            error.focus();
        } catch (failure) {
            error.textContent = form.dataset.failureMessage;
            error.hidden = false;
            error.focus();
        } finally {
            button.disabled = false;
            form.removeAttribute('aria-busy');
        }
    });
});
