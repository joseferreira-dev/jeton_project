/**
 * CSRF - Gerenciamento do token e submissão POST
 */

// Inicializa como variáveis globais
window.csrfToken = null;
window.csrfHeader = null;

/**
 * Submete uma requisição POST para a URL informada, incluindo o token CSRF
 * @param {string} url - URL de destino
 */
function submitPost(url) {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = url;
    if (window.csrfToken) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = window.csrfToken;
        form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
}

window.submitPost = submitPost;