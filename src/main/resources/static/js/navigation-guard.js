/**
 * Confirmação de navegação para formulários não salvos
 */

let isDirty = false;
let isSubmitting = false;

export function markDirty() {
    isDirty = true;
}

export function clearDirty() {
    isDirty = false;
}

export function isFormDirty() {
    return isDirty;
}

function isIgnored(element) {
    if (element.hasAttribute('data-guard-ignore')) {
        return true;
    }
    const form = element.closest('form');
    if (form && form.hasAttribute('data-guard-ignore')) {
        return true;
    }
    return false;
}

export function initNavigationGuard(message = 'Há alterações não salvas. Tem certeza que deseja sair?') {
    window.addEventListener('beforeunload', function (e) {
        if (isDirty && !isSubmitting) {
            e.preventDefault();
            e.returnValue = message;
            return message;
        }
    });

    document.addEventListener('click', function (e) {
        const target = e.target.closest('a');
        if (!target) return;

        // Ignora links que:
        // - Abrem em nova aba (target="_blank")
        // - São âncoras internas (#)
        // - Têm atributo download
        // - São links de exclusão/confirmação (data-role)
        // - Estão dentro de um formulário com data-guard-ignore
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
            showNavigationConfirm(message, href);
        }
    });

    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (form && form.tagName === 'FORM') {
            if (isIgnored(form)) {
                return;
            }
            isSubmitting = true;
            setTimeout(() => {
                isSubmitting = false;
                clearDirty();
            }, 500);
        }
    });
}

function showNavigationConfirm(message, href) {
    let modalElement = document.getElementById('modalNavegacaoConfirm');

    if (!modalElement) {
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

        const div = document.createElement('div');
        div.innerHTML = modalHtml;
        document.body.appendChild(div.firstElementChild);
        modalElement = document.getElementById('modalNavegacaoConfirm');
    }

    const msgElement = document.getElementById('modalNavegacaoMensagem');
    if (msgElement) msgElement.textContent = message;

    const confirmBtn = document.getElementById('modalNavegacaoConfirmar');
    if (confirmBtn) {
        confirmBtn.href = href;
        const newConfirmBtn = confirmBtn.cloneNode(true);
        confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);
        newConfirmBtn.addEventListener('click', function (e) {
            clearDirty();
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) modal.hide();
        });
    }

    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}

export function watchFormChanges(formSelector = 'form') {
    const forms = typeof formSelector === 'string'
        ? document.querySelectorAll(formSelector)
        : [formSelector];

    forms.forEach(form => {
        if (!form) return;

        if (isIgnored(form)) {
            return;
        }

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
    if (target.type === 'hidden' || target.type === 'submit' || target.type === 'button' ||
        target.tagName === 'BUTTON' || target.tagName === 'FIELDSET') {
        return;
    }

    if (isIgnored(target)) {
        return;
    }

    markDirty();
}

export function ignoreForm(form) {
    if (form) {
        form.setAttribute('data-guard-ignore', 'true');
        form.removeEventListener('input', handleFormChange);
        form.removeEventListener('change', handleFormChange);
        form.removeEventListener('keyup', handleFormChange);
    }
}

export function resetNavigationGuard() {
    clearDirty();
    isSubmitting = false;
}

window.resetNavigationGuard = resetNavigationGuard;
window.markDirty = markDirty;
window.clearDirty = clearDirty;
window.ignoreForm = ignoreForm;