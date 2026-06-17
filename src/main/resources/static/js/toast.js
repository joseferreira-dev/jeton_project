/**
 * toast.js - Sistema de notificações Toast (Bootstrap 5)
 * Substitui alert() e mensagens flash com recarga de página
 */

// ============================================================================
// CONFIGURAÇÕES PADRÃO
// ============================================================================

const DEFAULTS = {
    delay: 5000,               // tempo em ms antes de desaparecer
    position: 'top-end',       // top-start, top-center, top-end, bottom-start, bottom-center, bottom-end
    animation: true,
};

// ============================================================================
// CORES POR TIPO
// ============================================================================

const TYPE_CONFIG = {
    success: {
        bg: 'bg-success',
        text: 'text-white',
        icon: 'fa-check-circle',
        title: 'Sucesso',
    },
    error: {
        bg: 'bg-danger',
        text: 'text-white',
        icon: 'fa-circle-exclamation',
        title: 'Erro',
    },
    warning: {
        bg: 'bg-warning',
        text: 'text-dark',
        icon: 'fa-triangle-exclamation',
        title: 'Atenção',
    },
    info: {
        bg: 'bg-info',
        text: 'text-dark',
        icon: 'fa-circle-info',
        title: 'Informação',
    },
};

// ============================================================================
// FUNÇÃO PRINCIPAL
// ============================================================================

/**
 * Exibe um toast na tela
 * @param {string} message - Mensagem principal
 * @param {string} type - 'success' | 'error' | 'warning' | 'info'
 * @param {string} title - Título opcional (usa o padrão do tipo se não informado)
 * @param {number} delay - Tempo em ms (padrão: 5000)
 */
export function showToast(message, type = 'info', title = null, delay = null) {
    const config = TYPE_CONFIG[type] || TYPE_CONFIG.info;
    const toastTitle = title || config.title;
    const autohide = delay !== 0;
    const delayValue = delay !== null ? delay : DEFAULTS.delay;

    // Cria o elemento do toast
    const toastHtml = `
        <div class="toast align-items-center border-0 ${config.bg} ${config.text}" role="alert"
             aria-live="assertive" aria-atomic="true"
             data-bs-delay="${delayValue}"
             data-bs-autohide="${autohide}">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fa-solid ${config.icon} me-2"></i>
                    <strong>${toastTitle}</strong> — ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Fechar"></button>
            </div>
        </div>
    `;

    // Adiciona ao container
    const container = getOrCreateContainer();
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = toastHtml;
    const toastElement = tempDiv.firstElementChild;
    container.appendChild(toastElement);

    // Inicializa e mostra
    const toast = new bootstrap.Toast(toastElement);
    toast.show();

    // Remove do DOM após esconder
    toastElement.addEventListener('hidden.bs.toast', () => {
        if (toastElement.parentNode) {
            toastElement.remove();
        }
    });
}

// ============================================================================
// HELPERS PARA TIPOS ESPECÍFICOS
// ============================================================================

export function showSuccess(message, title = null, delay = null) {
    showToast(message, 'success', title, delay);
}

export function showError(message, title = null, delay = null) {
    showToast(message, 'error', title, delay);
}

export function showWarning(message, title = null, delay = null) {
    showToast(message, 'warning', title, delay);
}

export function showInfo(message, title = null, delay = null) {
    showToast(message, 'info', title, delay);
}

// ============================================================================
// FUNÇÕES AUXILIARES
// ============================================================================

/**
 * Obtém ou cria o container global de toasts
 * @returns {HTMLElement}
 */
function getOrCreateContainer() {
    const containerId = 'toastContainer';
    let container = document.getElementById(containerId);
    if (!container) {
        container = document.createElement('div');
        container.id = containerId;
        container.className = 'toast-container position-fixed';
        // Posicionamento (top-right)
        container.style.bottom = '20px';
        container.style.right = '20px';
        container.style.zIndex = '9999';
        container.style.maxWidth = '400px';
        document.body.appendChild(container);
    }
    return container;
}

// ============================================================================
// INICIALIZAÇÃO: CONVERTE MENSAGENS FLASH EM TOASTS
// ============================================================================

/**
 * Verifica se há mensagens flash no layout e as exibe como toasts.
 * Deve ser chamada após o DOM carregar.
 */
export function initFlashToasts() {
    // Busca os elementos que contêm mensagens flash
    const flashSuccess = document.getElementById('flashSuccess');
    const flashError = document.getElementById('flashError');
    const flashWarning = document.getElementById('flashWarning');
    const flashInfo = document.getElementById('flashInfo');

    if (flashSuccess && flashSuccess.textContent.trim()) {
        showSuccess(flashSuccess.textContent.trim());
        flashSuccess.remove(); // remove para não aparecer duas vezes
    }
    if (flashError && flashError.textContent.trim()) {
        showError(flashError.textContent.trim());
        flashError.remove();
    }
    if (flashWarning && flashWarning.textContent.trim()) {
        showWarning(flashWarning.textContent.trim());
        flashWarning.remove();
    }
    if (flashInfo && flashInfo.textContent.trim()) {
        showInfo(flashInfo.textContent.trim());
        flashInfo.remove();
    }
}