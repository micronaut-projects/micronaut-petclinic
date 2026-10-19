(() => {
    const editForm = document.getElementById('vet-edit-form');
    const deleteForm = document.getElementById('vet-delete-form');
    if (!editForm || !deleteForm) return;

    const feedback = document.getElementById('vet-feedback');
    const message = document.getElementById('vet-feedback-message');
    const reload = document.getElementById('vet-reload');
    let pending = false;

    function showFeedback(text, kind, needsReload = false) {
        message.textContent = text;
        feedback.className = 'alert alert-' + kind;
        feedback.hidden = false;
        reload.hidden = !needsReload;
    }

    async function submit(form, deleting) {
        if (pending) return;
        if (deleting && !window.confirm(deleteForm.dataset.confirm)) return;

        const fields = new FormData(form);
        const request = deleting ? {
            method: 'DELETE',
            headers: {'If-Match': '"' + fields.get('etag') + '"', 'Accept': 'application/json'}
        } : {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
            body: JSON.stringify({
                id: Number(fields.get('id')),
                firstName: fields.get('firstName'),
                lastName: fields.get('lastName'),
                specialityIds: fields.getAll('specialityIds').map(Number),
                etag: fields.get('etag')
            })
        };
        pending = true;
        const controls = [...editForm.elements, ...deleteForm.elements];
        const disabled = controls.map(control => control.disabled);
        controls.forEach(control => { control.disabled = true; });
        feedback.hidden = true;

        try {
            const response = await fetch(form.action, request);
            const body = await response.json().catch(() => ({}));
            if (!response.ok) {
                if (response.status === 412) {
                    showFeedback(feedback.dataset.conflict, 'warning', true);
                } else if (response.status === 428) {
                    showFeedback(feedback.dataset.missingEtag, 'warning', true);
                } else {
                    showFeedback(feedback.dataset.error, 'danger');
                }
                return;
            }
            if (deleting) {
                window.location.assign('/vets');
                return;
            }
            if (!body.etag) {
                showFeedback(feedback.dataset.missingEtag, 'warning', true);
                return;
            }
            // Both subsequent saves and deletes must use the version just saved.
            editForm.elements.namedItem('etag').value = body.etag;
            deleteForm.elements.namedItem('etag').value = body.etag;
            showFeedback(feedback.dataset.saved, 'success');
        } catch (error) {
            showFeedback(feedback.dataset.networkError, 'danger');
        } finally {
            controls.forEach((control, index) => { control.disabled = disabled[index]; });
            pending = false;
        }
    }

    editForm.addEventListener('submit', event => {
        event.preventDefault();
        submit(editForm, false);
    });
    deleteForm.addEventListener('submit', event => {
        event.preventDefault();
        submit(deleteForm, true);
    });
})();
