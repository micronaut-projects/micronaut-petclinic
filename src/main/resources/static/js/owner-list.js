/* Render the owner list from JSON and keep presentation state in the browser. */
(() => {
    const list = document.querySelector('[data-owner-list]');
    if (!list) {
        return;
    }
    const form = list.querySelector('[data-owner-controls]');
    const grid = list.querySelector('[data-owner-grid]');
    const empty = list.querySelector('[data-owner-empty]');
    const pagination = list.querySelector('[data-owner-pagination]');
    const template = list.querySelector('[data-owner-card-template]');
    const status = list.querySelector('[data-owner-status]');
    const errorBox = list.querySelector('[data-owner-error]');
    const submit = form.querySelector('[type="submit"]');
    const numbers = new Intl.NumberFormat(document.documentElement.lang || 'en');
    let request;
    let sequence = 0;
    let retryParams;

    function message(pattern, values) {
        for (const [key, value] of Object.entries(values)) {
            pattern = pattern.replaceAll(`__${key}__`, numbers.format(value));
        }
        return pattern;
    }

    function selectionFrom(params) {
        return { lastName: params.get('lastName') || '', page: params.get('page') || '0',
            size: params.get('size') || '5', sort: params.get('sort') || 'lastName',
            direction: (params.get('direction') || 'asc').toLowerCase() };
    }

    function pageUrl(data, page) {
        const url = new URL(form.action, window.location.origin);
        url.search = new URLSearchParams({ lastName: data.lastName, page, size: data.size,
            sort: data.sort, direction: data.direction });
        return url;
    }

    function card(owner) {
        const fragment = template.content.cloneNode(true);
        const text = (selector, value) => { fragment.querySelector(selector).textContent = value ?? ''; };
        text('[data-owner-initials]', `${owner.firstName?.charAt(0) || '?'}${owner.lastName?.charAt(0) || '?'}`);
        text('[data-owner-name]', `${owner.firstName || ''} ${owner.lastName || ''}`.trim());
        text('[data-owner-city]', owner.city);
        text('[data-owner-address]', owner.address);
        text('[data-owner-phone]', owner.telephone);
        fragment.querySelectorAll('[data-owner-link]').forEach(link => {
            link.href = `/owners/${encodeURIComponent(owner.id)}`;
        });
        const pets = owner.pets || [];
        fragment.querySelector('[data-owner-no-pets]').hidden = pets.length > 0;
        const badges = fragment.querySelector('[data-owner-pets]');
        badges.hidden = pets.length === 0;
        for (const name of pets) {
            const badge = document.createElement('span');
            badge.className = 'pet-badge';
            const icon = document.createElement('i');
            icon.className = 'fas fa-paw';
            icon.setAttribute('aria-hidden', 'true');
            badge.append(icon, document.createTextNode(` ${name}`));
            badges.append(badge);
        }
        return fragment;
    }

    function pageButton(data, delta, label, enabled) {
        const button = document.createElement('button');
        button.type = 'submit';
        button.setAttribute('form', form.id);
        button.name = 'page';
        button.value = String(data.page + delta);
        button.setAttribute('data-owner-page', delta < 0 ? 'previous' : 'next');
        button.className = 'btn btn-outline-primary';
        button.textContent = label;
        button.disabled = !enabled;
        return button;
    }

    function render(data) {
        const cards = document.createDocumentFragment();
        data.owners.forEach(owner => cards.append(card(owner)));
        grid.replaceChildren(cards);
        empty.hidden = data.owners.length > 0;
        const range = data.owners.length > 0
            ? message(list.dataset.rangeTemplate, { start: data.page * data.size + 1,
                end: Math.min(data.totalSize, data.page * data.size + data.owners.length), total: data.totalSize })
            : list.dataset.emptyMessage;
        list.querySelector('[data-owner-summary]').hidden = false;
        list.querySelector('[data-owner-range]').textContent = range;
        document.querySelector('[data-owner-total]').textContent = message(list.dataset.totalTemplate, { total: data.totalSize });
        const filter = list.querySelector('[data-owner-filter]');
        filter.hidden = !data.lastName;
        filter.textContent = `${list.dataset.lastNameLabel}: ${data.lastName}`;
        const pageNumber = document.createElement('span');
        pageNumber.className = 'owner-page-number';
        pageNumber.textContent = message(list.dataset.pageTemplate, { page: data.page + 1, pages: Math.max(1, data.totalPages) });
        pagination.replaceChildren(
            pageButton(data, -1, list.dataset.previousLabel, data.page > 0), pageNumber,
            pageButton(data, 1, list.dataset.nextLabel, data.page + 1 < data.totalPages));
        pagination.hidden = data.totalSize === 0;
        for (const field of ['lastName', 'sort', 'direction', 'size']) {
            form.elements.namedItem(field).value = data[field];
        }
    }

    async function load(params, pushHistory = true) {
        request?.abort();
        request = new AbortController();
        const current = ++sequence;
        const selection = selectionFrom(params);
        retryParams = new URLSearchParams(selection);
        errorBox.hidden = true;
        status.textContent = list.dataset.loadingMessage;
        status.hidden = false;
        list.setAttribute('aria-busy', 'true');
        list.classList.add('owner-list-loading');
        submit.disabled = true;
        try {
            const url = new URL(list.dataset.endpoint, window.location.origin);
            url.search = retryParams.toString();
            const response = await fetch(url, { headers: { Accept: 'application/json' }, signal: request.signal });
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const payload = await response.json();
            if (current !== sequence) {
                return;
            }
            const data = { ...payload, ...selection, page: Number(selection.page), size: Number(selection.size),
                totalPages: Math.ceil(payload.totalSize / Number(selection.size)) };
            render(data);
            const canonical = pageUrl(data, data.page);
            if (pushHistory && canonical.href !== window.location.href) {
                window.history.pushState(null, '', canonical.pathname + canonical.search);
            }
        } catch (failure) {
            if (failure.name === 'AbortError' || current !== sequence) {
                return;
            }
            list.querySelector('[data-owner-error-message]').textContent = list.dataset.failureMessage;
            errorBox.hidden = false;
        } finally {
            if (current === sequence) {
                status.hidden = true;
                list.removeAttribute('aria-busy');
                list.classList.remove('owner-list-loading');
                submit.disabled = false;
            }
        }
    }

    form.addEventListener('submit', event => {
        event.preventDefault();
        if (!form.reportValidity()) {
            return;
        }
        const params = new URLSearchParams(new FormData(form));
        params.set('page', event.submitter?.name === 'page' ? event.submitter.value : '0');
        load(params);
    });
    list.querySelector('[data-owner-retry]').addEventListener('click', () => {
        if (retryParams) {
            load(retryParams);
        }
    });
    window.addEventListener('popstate', () => load(new URLSearchParams(window.location.search), false));
    const initialParams = new URLSearchParams(window.location.search);
    const initialSelection = selectionFrom(initialParams);
    for (const field of ['lastName', 'sort', 'direction', 'size']) {
        form.elements.namedItem(field).value = initialSelection[field];
    }
    load(initialParams, false);
})();
