/**
 * CSRF - Gerenciamento do token e submissão POST
 */

// Variáveis globais (expostas para compatibilidade)
let csrfToken = null;
let csrfHeader = null;

/**
 * Submete uma requisição POST para a URL informada, incluindo o token CSRF
 * @param {string} url - URL de destino
 */
function submitPost(url) {
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

window.submitPost = submitPost;