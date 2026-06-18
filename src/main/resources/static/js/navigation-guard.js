/**
 * Confirmação de navegação para formulários não salvos
 */

let isDirty = false;
let isSubmitting = false;

/**
 * Marca o formulário como "sujo" (modificado)
 */
export function markDirty() {
    isDirty = true;
}

/**
 * Limpa a marcação de "sujo" (formulário salvo ou submetido)
 */
export function clearDirty() {
    isDirty = false;
}

/**
 * Verifica se o formulário está sujo
 * @returns {boolean}
 */
export function isFormDirty() {
    return isDirty;
}

/**
 * Inicializa o guardião de navegação
 * @param {string} message - Mensagem personalizada (opcional)
 */
export function initNavigationGuard(message = 'Há alterações não salvas. Tem certeza que deseja sair?') {
    // ========== Evento de antes de fechar/recarregar a página ==========
    window.addEventListener('beforeunload', function (e) {
        if (isDirty && !isSubmitting) {
            // Para navegadores modernos, a mensagem personalizada não é mais exibida,
            // mas ainda é necessário retornar uma string para ativar o alerta.
            e.preventDefault();
            e.returnValue = message;
            return message;
        }
    });

    // ========== Interceptar cliques em links (navegação interna) ==========
    document.addEventListener('click', function (e) {
        const target = e.target.closest('a');
        if (!target) return;

        // Ignora links que:
        // - Abrem em nova aba (target="_blank")
        // - São âncoras internas (#)
        // - Têm atributo download
        // - São links de exclusão/confirmação (data-role)
        if (target.target === '_blank' ||
            target.getAttribute('href') === '#' ||
            target.hasAttribute('download') ||
            target.closest('[data-role="confirmar-acao"]') ||
            target.closest('[data-role="excluir"]') ||
            target.closest('[data-role="alternar-status"]')) {
            return;
        }

        const href = target.getAttribute('href');
        if (!href || href.startsWith('#')) return;

        if (isDirty && !isSubmitting) {
            e.preventDefault();
            e.stopPropagation();

            // Exibe um modal de confirmação
            showNavigationConfirm(message, href);
        }
    });

    // ========== Interceptar submissões de formulário ==========
    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (form && form.tagName === 'FORM') {
            // Marca como submetendo para evitar o alerta
            isSubmitting = true;
            // Limpa a marcação de sujo após um curto período (o suficiente para o submit)
            setTimeout(() => {
                isSubmitting = false;
                clearDirty();
            }, 500);
        }
    });
}

/**
 * Exibe um modal de confirmação personalizado
 * @param {string} message - Mensagem de confirmação
 * @param {string} href - URL para onde redirecionar
 */
function showNavigationConfirm(message, href) {
    // Verifica se o modal já existe
    let modalElement = document.getElementById('modalNavegacaoConfirm');

    if (!modalElement) {
        // Cria o modal dinamicamente
        const modalHtml = `
            <div class="modal fade" id="modalNavegacaoConfirm" tabindex="-1" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content shadow border-warning">
                        <div class="modal-header bg-warning text-dark">
                            <h5 class="modal-title">
                                <i class="fa-solid fa-triangle-exclamation me-2"></i> Alterações não salvas
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body p-4">
                            <p id="modalNavegacaoMensagem" class="mb-0">${message}</p>
                        </div>
                        <div class="modal-footer bg-light">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                                <i class="fa-solid fa-xmark me-1"></i> Ficar
                            </button>
                            <a href="#" id="modalNavegacaoConfirmar" class="btn btn-warning px-4">
                                <i class="fa-solid fa-arrow-right me-1"></i> Sair mesmo assim
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // Adiciona ao body
        const div = document.createElement('div');
        div.innerHTML = modalHtml;
        document.body.appendChild(div.firstElementChild);

        modalElement = document.getElementById('modalNavegacaoConfirm');
    }

    // Atualiza a mensagem
    const msgElement = document.getElementById('modalNavegacaoMensagem');
    if (msgElement) msgElement.textContent = message;

    // Atualiza o link de confirmação
    const confirmBtn = document.getElementById('modalNavegacaoConfirmar');
    if (confirmBtn) {
        confirmBtn.href = href;
        // Remove listeners antigos (se houver)
        const newConfirmBtn = confirmBtn.cloneNode(true);
        confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);
        newConfirmBtn.addEventListener('click', function (e) {
            // Limpa o dirty antes de navegar
            clearDirty();
            // Fecha o modal
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) modal.hide();
            // A navegação já será feita pelo href do link
        });
    }

    // Mostra o modal
    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}

/**
 * Detecta mudanças em campos de formulário e marca como sujo
 * @param {string|HTMLElement} formSelector - Seletor do formulário ou elemento
 */
export function watchFormChanges(formSelector = 'form') {
    const forms = typeof formSelector === 'string'
        ? document.querySelectorAll(formSelector)
        : [formSelector];

    forms.forEach(form => {
        if (!form) return;

        // Remove listeners antigos (evita duplicidade)
        form.removeEventListener('input', handleFormChange);
        form.removeEventListener('change', handleFormChange);
        form.removeEventListener('keyup', handleFormChange);

        form.addEventListener('input', handleFormChange);
        form.addEventListener('change', handleFormChange);
        form.addEventListener('keyup', handleFormChange);
    });
}

function handleFormChange(e) {
    const target = e.target;
    // Ignora campos que não devem disparar o alerta (ex: campos ocultos, botões)
    if (target.type === 'hidden' || target.type === 'submit' || target.type === 'button' ||
        target.tagName === 'BUTTON' || target.tagName === 'FIELDSET') {
        return;
    }

    // Marca como sujo
    markDirty();
}

/**
 * Reseta o estado (útil após salvar com sucesso)
 */
export function resetNavigationGuard() {
    clearDirty();
    isSubmitting = false;
}

// Expor globalmente para uso em outros scripts
window.resetNavigationGuard = resetNavigationGuard;
window.markDirty = markDirty;
window.clearDirty = clearDirty;