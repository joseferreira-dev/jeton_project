/**
 * Bloqueio do sistema
 */

import { API } from './config.js';

export function atualizarBotaoBloqueio() {
    const btn = document.getElementById('btnBloqueio');
    if (!btn) return;

    fetch(API.BLOQUEIO_STATUS)
        .then(response => response.json())
        .then(data => {
            if (data.bloqueado) {
                btn.innerHTML = '<i class="fa-solid fa-unlock-alt me-1"></i> Liberar Sistema';
                btn.classList.remove('btn-danger');
                btn.classList.add('btn-outline-light');
            } else {
                btn.innerHTML = '<i class="fa-solid fa-lock me-1"></i> Bloquear Sistema';
                btn.classList.remove('btn-outline-light');
                btn.classList.add('btn-danger', 'text-white');
            }
        })
        .catch(error => {
            console.error('Erro ao obter status do bloqueio:', error);
            btn.innerHTML = '<i class="fa-solid fa-lock me-1"></i> Bloquear/Desbloquear';
            btn.classList.remove('btn-outline-light', 'btn-outline-danger', 'btn-outline-success');
            btn.classList.add('btn-outline-warning');
        });
}

export function confirmarBloqueio() {
    const btn = document.getElementById('btnBloqueio');
    const isBloqueando = btn.innerHTML.includes('Bloquear');
    const modalElement = document.getElementById('modalConfirmacaoBloqueio');
    const modalBody = document.getElementById('textoConfirmacaoBloqueio');
    const confirmBtn = document.getElementById('btnConfirmarBloqueio');

    if (!modalElement || !modalBody || !confirmBtn) {
        const mensagem = isBloqueando
            ? 'Deseja BLOQUEAR o sistema? Os demais usuários ficarão impedidos de executar ações.'
            : 'Deseja LIBERAR o sistema? Todos os usuários voltarão a ter acesso normal.';
        if (!modalElement) {
            if (confirm(mensagem)) {
                // continua
            } else {
                showInfo('Ação cancelada', 'Cancelado');
            }
            return;
        }
    }

    if (isBloqueando) {
        modalBody.innerHTML = 'Deseja <strong>BLOQUEAR</strong> o sistema?<br>Os demais usuários ficarão impedidos de executar ações.';
        confirmBtn.innerHTML = '<i class="fa-solid fa-lock me-1"></i> Bloquear';
        confirmBtn.className = 'btn btn-danger px-4';
    } else {
        modalBody.innerHTML = 'Deseja <strong>LIBERAR</strong> o sistema?<br>Todos os usuários voltarão a ter acesso normal.';
        confirmBtn.innerHTML = '<i class="fa-solid fa-unlock-alt me-1"></i> Liberar';
        confirmBtn.className = 'btn btn-success px-4';
    }

    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

    newConfirmBtn.addEventListener('click', function (e) {
        e.preventDefault();
        const modal = bootstrap.Modal.getInstance(modalElement);
        if (modal) modal.hide();
        document.getElementById('formBloqueio').submit();
    });

    const modal = new bootstrap.Modal(modalElement);
    modal.show();
    return false;
}