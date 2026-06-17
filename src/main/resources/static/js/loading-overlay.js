/**
 * Overlay global com spinner e mensagem para operações longas.
 * Utiliza contador para permitir chamadas aninhadas.
 */

let overlayCount = 0;
let overlayElement = null;
let globalTimeout = null;

export function showLoading(message = 'Processando...') {
    if (!overlayElement) {
        overlayElement = document.createElement('div');
        overlayElement.id = 'globalLoadingOverlay';
        overlayElement.className = 'position-fixed top-0 start-0 w-100 h-100 d-flex flex-column justify-content-center align-items-center';
        overlayElement.style.backgroundColor = 'rgba(0, 0, 0, 0.6)';
        overlayElement.style.zIndex = '10000';
        overlayElement.style.display = 'none';
        overlayElement.innerHTML = `
            <div class="bg-white p-4 rounded-3 shadow text-center" style="min-width: 200px; max-width: 90%;">
                <div class="spinner-border text-success mb-2" style="width: 3rem; height: 3rem;" role="status">
                    <span class="visually-hidden">Carregando...</span>
                </div>
                <p class="mb-0 fw-bold" id="loadingMessage">${message}</p>
            </div>
        `;
        document.body.appendChild(overlayElement);
    }

    overlayCount++;
    overlayElement.style.display = 'flex';
    const msgElement = document.getElementById('loadingMessage');
    if (msgElement) {
        msgElement.textContent = message;
    }

    if (globalTimeout) clearTimeout(globalTimeout);
    globalTimeout = setTimeout(() => {
        hideLoading();
        console.warn('Loading overlay ocultado por timeout de segurança.');
    }, 30000);
}

export function hideLoading() {
    if (overlayCount > 0) {
        overlayCount--;
    }
    if (overlayCount === 0 && overlayElement) {
        overlayElement.style.display = 'none';
    }
}

export function fetchWithLoading(input, init = {}, message = 'Carregando dados...') {
    showLoading(message);
    return fetch(input, init)
        .finally(hideLoading);
}

export function submitFormWithLoading(form, message = 'Enviando dados...') {
    showLoading(message);
    form.submit();
    setTimeout(() => {
        hideLoading();
    }, 10000);
}