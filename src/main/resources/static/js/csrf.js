/**
 * CSRF - Gerenciamento do token e submissão POST
 */

let csrfToken = null;
let csrfHeader = null;

export function initCsrf(token, header) {
    csrfToken = token;
    csrfHeader = header;
}

export function submitPost(url) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = url;
    if (csrfToken) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = csrfToken;
        form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
}