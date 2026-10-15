(() => {
    const form = document.getElementById('speciality-form');
    if (!form) return;

    const feedback = document.getElementById('speciality-feedback');
    let pending = false;

    function showFeedback(text, kind) {
        feedback.textContent = text;
        feedback.className = 'alert alert-' + kind;
        feedback.hidden = false;
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (pending) return;

        const fields = new FormData(form);
        const payload = {name: fields.get('name')};
        const id = fields.get('id');
        if (id) payload.id = Number(id);
        pending = true;
        const controls = [...form.elements];
        const disabled = controls.map(control => control.disabled);
        controls.forEach(control => { control.disabled = true; });
        feedback.hidden = true;

        try {
            const response = await fetch(form.action, {
                method: 'POST',
                headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
                body: JSON.stringify(payload)
            });
            const body = await response.json().catch(() => ({}));
            if (!response.ok) {
                showFeedback(body.message || feedback.dataset.error, 'danger');
                return;
            }
            if (!id) {
                if (!Number.isInteger(body.id) || body.id <= 0) {
                    showFeedback(feedback.dataset.networkError, 'danger');
                    return;
                }
                window.location.assign('/vets/specialities/' + body.id + '/edit?saved=true');
                return;
            }
            showFeedback(feedback.dataset.saved, 'success');
        } catch (error) {
            showFeedback(feedback.dataset.networkError, 'danger');
        } finally {
            controls.forEach((control, index) => { control.disabled = disabled[index]; });
            pending = false;
        }
    });
})();
