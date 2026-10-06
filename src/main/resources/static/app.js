const $ = selector => document.querySelector(selector);
let token = localStorage.getItem('library-token');
let user = null;
let mode = 'login';
let section = 'books';
let page = 0;
let search = '';
let editing = null;
let items = [];
let requestVersion = 0;

function errorText(error) {
    return [error.message, ...Object.values(error.errors || {})].filter(Boolean).join('\n');
}

async function api(path, options = {}) {
    const headers = {'Content-Type': 'application/json', ...options.headers};
    if (token) headers.Authorization = 'Bearer ' + token;
    const response = await fetch('/api' + path, {...options, headers});
    if (response.status === 204) return null;
    const data = await response.json().catch(() => ({message: 'Сервер вернул некорректный ответ'}));
    if (!response.ok) {
        if (response.status === 401 && !path.startsWith('/auth/login') && !path.startsWith('/auth/register')) {
            logout();
            $('#auth-error').textContent = 'Сессия истекла. Войдите ещё раз.';
        }
        throw data;
    }
    return data;
}

function logout() {
    token = null;
    user = null;
    requestVersion++;
    localStorage.removeItem('library-token');
    $('#catalog-view').hidden = true;
    $('#auth-view').hidden = false;
    $('#editor').close();
    $('#password').value = '';
}

function authMode(value) {
    mode = value;
    $('#login-tab').classList.toggle('active', value === 'login');
    $('#register-tab').classList.toggle('active', value === 'register');
    $('#auth-title').textContent = value === 'login' ? 'С возвращением' : 'Стать читателем';
    $('#auth-caption').textContent = value === 'login' ? 'Войдите, чтобы открыть каталог.' : 'Пароль: 8–64 латинских символа без пробелов.';
    $('#auth-submit').textContent = value === 'login' ? 'Войти' : 'Создать аккаунт';
    $('#password').minLength = value === 'login' ? 1 : 8;
    $('#password').autocomplete = value === 'login' ? 'current-password' : 'new-password';
    $('#auth-error').textContent = '';
}

async function showCatalog() {
    $('#auth-view').hidden = true;
    $('#catalog-view').hidden = false;
    $('#account').textContent = user.email + ' · ' + (user.role === 'ADMIN' ? 'Администратор' : 'Читатель');
    $('#add').hidden = user.role !== 'ADMIN';
    await load();
}

function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

async function load() {
    const version = ++requestVersion;
    $('#notice').textContent = 'Загрузка…';
    $('#prev').disabled = true;
    $('#next').disabled = true;
    const params = new URLSearchParams({page, size: 9});
    if (section === 'books') params.set('search', search);
    try {
        const data = await api('/' + section + '?' + params);
        if (version !== requestVersion || !user) return;
        if (page > 0 && data.content.length === 0) {
            page--;
            return load();
        }
        items = data.content;
        $('#list').replaceChildren();
        if (!items.length) $('#list').append(element('div', 'empty', 'Ничего не найдено'));
        for (const item of items) {
            const card = element('article', 'card');
            card.append(element('span', 'number', '#' + String(item.id).padStart(3, '0')));
            card.append(element('h2', '', item.title || item.name));
            if (section === 'books') {
                card.append(element('p', '', item.author.name + ' · ' + item.publicationYear));
                card.append(element('p', 'muted small', 'ISBN ' + item.isbn));
                const tags = element('div', 'tags');
                item.categories.forEach(c => tags.append(element('span', 'tag', c.name)));
                card.append(tags);
            }
            if (user.role === 'ADMIN') {
                const actions = element('div', 'actions');
                const edit = element('button', 'quiet', 'Изменить');
                edit.onclick = () => openEditor(item);
                const remove = element('button', 'quiet danger', 'Удалить');
                remove.onclick = () => deleteItem(item, remove);
                actions.append(edit, remove);
                card.append(actions);
            }
            $('#list').append(card);
        }
        $('#total').textContent = 'Всего записей: ' + data.totalElements;
        $('#page-label').textContent = (page + 1) + ' / ' + Math.max(1, data.totalPages);
        $('#prev').disabled = page === 0;
        $('#next').disabled = page + 1 >= data.totalPages;
        $('#notice').textContent = '';
    } catch (error) {
        if (version !== requestVersion) return;
        $('#list').replaceChildren();
        $('#notice').textContent = errorText(error);
    }
}

function inputField(name, title, value, attributes = {}) {
    const label = element('label', '', title);
    const input = element('input');
    input.name = name;
    input.value = value ?? '';
    input.required = true;
    Object.assign(input, attributes);
    label.append(input);
    $('#fields').append(label);
}

async function allOptions(path) {
    let result = [], current = 0, data;
    do {
        data = await api(path + '?size=100&page=' + current++);
        result.push(...data.content);
    } while (current < data.totalPages);
    return result;
}

function selectField(name, title, options, selected, multiple) {
    const label = element('label', '', title);
    const select = element('select');
    select.name = name;
    select.required = true;
    select.multiple = multiple;
    for (const item of options) {
        const option = element('option', '', item.name);
        option.value = item.id;
        option.selected = selected.includes(item.id);
        select.append(option);
    }
    label.append(select);
    if (multiple) label.append(element('p', 'muted small', 'Несколько категорий: удерживайте Ctrl или Cmd.'));
    $('#fields').append(label);
}

async function openEditor(item = null) {
    editing = item;
    $('#fields').replaceChildren();
    $('#edit-error').textContent = '';
    $('#edit-title').textContent = item ? 'Редактирование' : 'Новая запись';
    $('#save').disabled = true;
    $('#editor').showModal();
    if (section !== 'books') {
        inputField('name', 'Название / имя', item?.name, {maxLength: section === 'authors' ? 120 : 80});
        $('#save').disabled = false;
        return;
    }
    inputField('title', 'Название книги', item?.title, {maxLength: 200});
    inputField('isbn', 'ISBN · 13 цифр', item?.isbn, {pattern: '[0-9]{13}', maxLength: 13});
    inputField('publicationYear', 'Год издания', item?.publicationYear, {type: 'number', min: 1450, max: new Date().getFullYear()});
    try {
        const [authors, categories] = await Promise.all([allOptions('/authors'), allOptions('/categories')]);
        if (!$('#editor').open) return;
        selectField('authorId', 'Автор', authors, [item?.author.id], false);
        selectField('categoryIds', 'Категории', categories, item?.categories.map(c => c.id) || [], true);
        if (!authors.length || !categories.length) throw {message: 'Сначала добавьте хотя бы одного автора и категорию.'};
        $('#save').disabled = false;
    } catch (error) {
        $('#edit-error').textContent = errorText(error);
    }
}

async function deleteItem(item, button) {
    if (!confirm('Удалить «' + (item.title || item.name) + '»?')) return;
    button.disabled = true;
    try {
        await api('/' + section + '/' + item.id, {method: 'DELETE'});
        await load();
    } catch (error) {
        $('#notice').textContent = errorText(error);
        button.disabled = false;
    }
}

$('#auth-form').onsubmit = async event => {
    event.preventDefault();
    $('#auth-submit').disabled = true;
    $('#auth-error').textContent = '';
    try {
        const data = await api('/auth/' + mode, {method: 'POST', body: JSON.stringify({
            email: $('#email').value.trim(), password: $('#password').value
        })});
        token = data.token;
        localStorage.setItem('library-token', token);
        user = data;
        $('#password').value = '';
        page = 0;
        await showCatalog();
    } catch (error) {
        $('#auth-error').textContent = errorText(error);
    } finally {
        $('#auth-submit').disabled = false;
    }
};

$('#edit-form').onsubmit = async event => {
    event.preventDefault();
    $('#save').disabled = true;
    $('#edit-error').textContent = '';
    const form = new FormData(event.target);
    const body = section === 'books' ? {
        title: form.get('title').trim(), isbn: form.get('isbn'),
        publicationYear: Number(form.get('publicationYear')), authorId: Number(form.get('authorId')),
        categoryIds: form.getAll('categoryIds').map(Number)
    } : {name: form.get('name').trim()};
    try {
        await api('/' + section + (editing ? '/' + editing.id : ''), {
            method: editing ? 'PUT' : 'POST', body: JSON.stringify(body)
        });
        $('#editor').close();
        await load();
    } catch (error) {
        $('#edit-error').textContent = errorText(error);
    } finally {
        $('#save').disabled = false;
    }
};

$('#login-tab').onclick = () => authMode('login');
$('#register-tab').onclick = () => authMode('register');
$('#logout').onclick = logout;
$('#add').onclick = () => openEditor();
$('#close-editor').onclick = () => $('#editor').close();
$('#prev').onclick = () => {page--; load();};
$('#next').onclick = () => {page++; load();};
$('#search-form').onsubmit = event => {event.preventDefault(); search = $('#search').value.trim(); page = 0; load();};
document.querySelectorAll('[data-section]').forEach(button => {
    button.onclick = () => {
        section = button.dataset.section;
        page = 0;
        document.querySelectorAll('[data-section]').forEach(b => b.classList.toggle('active', b === button));
        $('#search-form').hidden = section !== 'books';
        load();
    };
});
if (token) {
    api('/auth/me').then(data => {user = data; showCatalog();}).catch(error => {
        logout();
        $('#auth-error').textContent = errorText(error);
    });
}
