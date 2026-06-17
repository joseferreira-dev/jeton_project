/**
 * Utilitários gerais
 */

/**
 * Exibe spinner no botão e o desabilita
 * @param {HTMLElement} button
 * @param {boolean} loading
 */
function setButtonLoading(button, loading) {
    if (!button) return;
    if (loading) {
        if (!button.hasAttribute('data-original-text')) {
            button.setAttribute('data-original-text', button.innerHTML);
        }
        button.disabled = true;
        button.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Processando...';
    } else {
        button.disabled = false;
        const original = button.getAttribute('data-original-text');
        if (original) {
            button.innerHTML = original;
            button.removeAttribute('data-original-text');
        }
    }
}

/**
 * Converte string de data ISO para formato DD/MM/YYYY
 * @param {string} isoDate
 * @returns {string}
 */
function formatDateBr(isoDate) {
    if (!isoDate) return '';
    const parts = isoDate.split('-');
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

window.setButtonLoading = setButtonLoading;
window.formatDateBr = formatDateBr;