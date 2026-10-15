const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const vm = require('node:vm');

const script = readFileSync(resolve(__dirname, '../../main/resources/static/js/speciality-form.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

function page(id) {
    const elements = [{name: 'name', value: 'surgery', disabled: false}, {name: 'submit', disabled: false}];
    if (id) elements.push({name: 'id', value: String(id), disabled: false});
    const form = {
        elements,
        action: id ? '/vets/specialities/' + id + '/edit' : '/vets/specialities/new',
        addEventListener(type, callback) { this[type] = callback; }
    };
    const feedback = {hidden: true, dataset: {
        saved: 'Saved', error: 'Save failed', networkError: 'Check the list before retrying'
    }};
    const requests = [];
    const navigations = [];
    let respond = async () => ({ok: true, status: id ? 200 : 201, json: async () => ({id: id || 7, name: 'surgery'})});
    vm.runInNewContext(script, {
        document: {getElementById: key => key === 'speciality-form' ? form : feedback},
        window: {location: {assign: url => navigations.push(url)}},
        FormData: class {
            constructor(form) { this.fields = form.elements.filter(element => !element.disabled); }
            get(name) { return this.fields.find(element => element.name === name)?.value ?? null; }
        },
        fetch: async (url, options) => {
            requests.push({url, ...options, payload: JSON.parse(options.body)});
            return respond();
        }
    });
    return {
        form, feedback, requests, navigations,
        respondWith(fn) { respond = fn; },
        submit() {
            let prevented = false;
            form.submit({preventDefault() { prevented = true; }});
            assert.equal(prevented, true);
            return flush();
        }
    };
}

test('creation posts a JSON name and opens the created speciality edit page', async () => {
    const ui = page();
    await ui.submit();
    assert.equal(ui.requests[0].url, '/vets/specialities/new');
    assert.equal(ui.requests[0].method, 'POST');
    assert.equal(ui.requests[0].headers['Content-Type'], 'application/json');
    assert.equal(ui.requests[0].headers.Accept, 'application/json');
    assert.deepEqual(ui.requests[0].payload, {name: 'surgery'});
    assert.deepEqual(ui.navigations, ['/vets/specialities/7/edit?saved=true']);
});

test('updates post the numeric ID and display success without navigating', async () => {
    const ui = page(7);
    await ui.submit();
    assert.equal(ui.requests[0].url, '/vets/specialities/7/edit');
    assert.deepEqual(ui.requests[0].payload, {id: 7, name: 'surgery'});
    assert.equal(ui.feedback.className, 'alert alert-success');
    assert.equal(ui.feedback.textContent, 'Saved');
    assert.equal(ui.feedback.hidden, false);
    assert.deepEqual(ui.navigations, []);
});

test('validation, duplicate and missing-record errors preserve the input and display plain text', async () => {
    for (const id of [undefined, 7]) {
        const ui = page(id);
        for (const status of [400, 404]) {
            ui.respondWith(async () => ({ok: false, status, json: async () => ({message: '<b>Cannot save</b>'})}));
            await ui.submit();
            assert.equal(ui.feedback.textContent, '<b>Cannot save</b>');
            assert.equal(ui.feedback.innerHTML, undefined);
            assert.equal(ui.feedback.className, 'alert alert-danger');
            assert.equal(ui.form.elements[0].value, 'surgery');
            assert.ok(ui.form.elements.every(element => !element.disabled));
            assert.deepEqual(ui.navigations, []);
        }
    }
});

test('network and non-JSON failures restore controls and permit retrying', async () => {
    const ui = page();
    ui.respondWith(async () => { throw new Error('offline'); });
    await ui.submit();
    assert.equal(ui.feedback.textContent, 'Check the list before retrying');
    assert.ok(ui.form.elements.every(element => !element.disabled));
    ui.respondWith(async () => ({ok: false, status: 500, json: async () => { throw new Error('HTML'); }}));
    await ui.submit();
    assert.equal(ui.feedback.textContent, 'Save failed');
    assert.equal(ui.requests.length, 2);
    assert.deepEqual(ui.navigations, []);
});

test('a malformed creation response never navigates to an invalid edit URL', async () => {
    const ui = page();
    ui.respondWith(async () => ({ok: true, status: 201, json: async () => ({})}));
    await ui.submit();
    assert.equal(ui.feedback.textContent, 'Check the list before retrying');
    assert.deepEqual(ui.navigations, []);
});

test('a pending request disables controls and prevents duplicate saves', async () => {
    const ui = page(7);
    let finish;
    ui.respondWith(() => new Promise(resolve => { finish = resolve; }));
    await ui.submit();
    assert.ok(ui.form.elements.every(element => element.disabled));
    await ui.submit();
    assert.equal(ui.requests.length, 1);
    finish({ok: true, status: 200, json: async () => ({id: 7, name: 'surgery'})});
    await flush();
    assert.ok(ui.form.elements.every(element => !element.disabled));
    assert.equal(ui.feedback.textContent, 'Saved');
});
