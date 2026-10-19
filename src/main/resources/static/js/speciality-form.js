(() => {
    const form = document.getElementById('speciality-form');
    if (!form) return;

    const feedback = document.getElementById('speciality-feedback');
    const savedAlert = document.getElementById('speciality-saved-alert');
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
                method: id ? 'PUT' : 'POST',
                headers: {'Content-Type': 'application/json', 'Accept': 'application/json'},
                body: JSON.stringify(payload)
            });
            const body = await response.json().catch(() => ({}));
            if (!response.ok) {
                showFeedback(response.status === 400 ? feedback.dataset.duplicate : feedback.dataset.error, 'danger');
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
            if (savedAlert) savedAlert.hidden = true;
            showFeedback(feedback.dataset.saved, 'success');
        } catch (error) {
            showFeedback(feedback.dataset.networkError, 'danger');
        } finally {
            controls.forEach((control, index) => { control.disabled = disabled[index]; });
            pending = false;
        }
    });
})();
