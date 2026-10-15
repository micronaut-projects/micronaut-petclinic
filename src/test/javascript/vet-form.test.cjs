const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const vm = require('node:vm');

const script = readFileSync(resolve(__dirname, '../../main/resources/static/js/vet-form.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

function page() {
    const field = (name, value) => ({name, value, disabled: false});
    function form(action, fields) {
        const elements = [...fields, field('submit', '')];
        elements.namedItem = name => elements.find(element => element.name === name);
        return {
            action, elements, dataset: {confirm: 'Delete this veterinarian?'},
            addEventListener(type, handler) { this[type] = handler; }
        };
    }
    const edit = form('/vets/7/edit', [field('id', '7'), field('firstName', 'Jane'), field('lastName', 'Vet'),
        field('specialityIds', '2'), field('specialityIds', '3'), field('etag', 'original')]);
    const deletion = form('/vets/7/delete', [field('etag', 'original')]);
    const feedback = {hidden: true, dataset: {
        saved: 'Saved', conflict: 'Reload latest details', missingEtag: 'Version required',
        error: 'Request failed', networkError: 'Check whether changes were saved'
    }};
    const message = {};
    const reload = {hidden: true};
    const nodes = {'vet-edit-form': edit, 'vet-delete-form': deletion, 'vet-feedback': feedback,
        'vet-feedback-message': message, 'vet-reload': reload};
    const requests = [];
    const navigations = [];
    let confirm = true;
    let respond = async () => ({ok: true, status: 200, json: async () => ({etag: 'saved'})});
    vm.runInNewContext(script, {
        document: {getElementById: id => nodes[id]},
        window: {confirm: () => confirm, location: {assign: url => navigations.push(url)}},
        FormData: class {
            constructor(form) { this.fields = form.elements.filter(field => !field.disabled); }
            get(name) { return this.fields.find(field => field.name === name)?.value ?? null; }
            getAll(name) { return this.fields.filter(field => field.name === name).map(field => field.value); }
        },
        fetch: async (url, options) => {
            requests.push({url, ...options, payload: options.body === undefined ? undefined : JSON.parse(options.body)});
            return respond();
        }
    });
    return {
        edit, deletion, feedback, message, reload, requests, navigations,
        respondWith(fn) { respond = fn; },
        confirmDeletion(value) { confirm = value; },
        submit(form) {
            let prevented = false;
            form.submit({preventDefault() { prevented = true; }});
            assert.equal(prevented, true);
            return flush();
        }
    };
}

test('save sends JSON with numeric speciality IDs and refreshes the ETag for later saves and deletes', async () => {
    const ui = page();
    await ui.submit(ui.edit);
    assert.equal(ui.requests[0].method, 'POST');
    assert.equal(ui.requests[0].headers['Content-Type'], 'application/json');
    assert.equal(ui.requests[0].headers.Accept, 'application/json');
    assert.deepEqual(ui.requests[0].payload, {
        id: 7, firstName: 'Jane', lastName: 'Vet', specialityIds: [2, 3], etag: 'original'
    });
    assert.equal(ui.edit.elements.namedItem('etag').value, 'saved');
    assert.equal(ui.deletion.elements.namedItem('etag').value, 'saved');
    assert.equal(ui.message.textContent, 'Saved');
    assert.equal(ui.feedback.hidden, false);
    assert.equal(ui.feedback.className, 'alert alert-success');
    await ui.submit(ui.edit);
    assert.equal(ui.requests[1].payload.etag, 'saved');
    ui.respondWith(async () => ({ok: true, status: 204, json: async () => { throw new Error('empty body'); }}));
    await ui.submit(ui.deletion);
    assert.equal(ui.requests[2].url, '/vets/7/delete');
    assert.equal(ui.requests[2].method, 'DELETE');
    assert.equal(ui.requests[2].headers['If-Match'], '"saved"');
    assert.equal(ui.requests[2].headers['Content-Type'], undefined);
    assert.equal(ui.requests[2].body, undefined);
    assert.deepEqual(ui.navigations, ['/vets']);
});

test('deselecting every speciality sends an empty array', async () => {
    const ui = page();
    ui.edit.elements.filter(field => field.name === 'specialityIds').forEach(field => { field.disabled = true; });
    await ui.submit(ui.edit);
    assert.deepEqual(ui.requests[0].payload.specialityIds, []);
    assert.equal(ui.edit.elements.namedItem('firstName').disabled, false);
    assert.equal(ui.edit.elements.namedItem('specialityIds').disabled, true);
});

for (const status of [412, 428]) {
    test(`${status} preserves inputs and the original ETag and offers a reload for both actions`, async () => {
        const ui = page();
        ui.respondWith(async () => ({ok: false, status, json: async () => ({message: 'server error'})}));
        for (const form of [ui.edit, ui.deletion]) {
            await ui.submit(form);
            assert.equal(ui.feedback.className, 'alert alert-warning');
            assert.equal(ui.reload.hidden, false);
            assert.equal(ui.edit.elements.namedItem('firstName').value, 'Jane');
            assert.equal(ui.edit.elements.namedItem('etag').value, 'original');
            assert.equal(ui.deletion.elements.namedItem('etag').value, 'original');
            assert.equal(ui.edit.elements.namedItem('submit').disabled, false);
            assert.deepEqual(ui.navigations, []);
        }
    });
}

test('validation and not-found errors display the server message without navigation', async () => {
    const ui = page();
    for (const status of [400, 404]) {
        ui.respondWith(async () => ({ok: false, status, json: async () => ({message: '<b>Invalid request</b>'})}));
        await ui.submit(ui.edit);
        assert.equal(ui.message.textContent, '<b>Invalid request</b>');
        assert.equal(ui.message.innerHTML, undefined);
        assert.equal(ui.feedback.className, 'alert alert-danger');
        assert.deepEqual(ui.navigations, []);
    }
});

test('network and non-JSON failures restore controls and allow a retry', async () => {
    const ui = page();
    ui.respondWith(async () => { throw new Error('offline'); });
    await ui.submit(ui.edit);
    assert.equal(ui.message.textContent, 'Check whether changes were saved');
    assert.equal(ui.edit.elements.namedItem('submit').disabled, false);
    ui.respondWith(async () => ({ok: false, status: 500, json: async () => { throw new Error('HTML response'); }}));
    await ui.submit(ui.edit);
    assert.equal(ui.message.textContent, 'Request failed');
    assert.equal(ui.requests.length, 2);
    assert.equal(ui.deletion.elements.namedItem('submit').disabled, false);
});

test('cancelled deletion sends no request', async () => {
    const ui = page();
    ui.confirmDeletion(false);
    await ui.submit(ui.deletion);
    assert.deepEqual(ui.requests, []);
    assert.deepEqual(ui.navigations, []);
});

test('a pending save disables both actions and prevents duplicate submissions', async () => {
    const ui = page();
    let finish;
    ui.respondWith(() => new Promise(resolve => { finish = resolve; }));
    await ui.submit(ui.edit);
    assert.equal(ui.edit.elements.namedItem('submit').disabled, true);
    assert.equal(ui.deletion.elements.namedItem('submit').disabled, true);
    await ui.submit(ui.deletion);
    await ui.submit(ui.edit);
    assert.equal(ui.requests.length, 1);
    finish({ok: true, status: 200, json: async () => ({etag: 'saved'})});
    await flush();
    assert.equal(ui.edit.elements.namedItem('submit').disabled, false);
    assert.equal(ui.deletion.elements.namedItem('submit').disabled, false);
});
