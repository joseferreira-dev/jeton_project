/**
 * Bloqueio do sistema
 */

import { API } from './config.js';

export function atualizarBotaoBloqueio() {
    // Seleciona todos os botões com data-role="bloquear"
    const botoes = document.querySelectorAll('[data-role="bloquear"]');
    if (botoes.length === 0) return;

    fetch(API.BLOQUEIO_STATUS)
        .then(response => response.json())
        .then(data => {
            const isBloqueado = data.bloqueado;
            botoes.forEach(btn => {
                if (isBloqueado) {
                    btn.innerHTML = '<i class="fa-solid fa-unlock-alt me-1"></i> Liberar Sistema';
                    btn.classList.remove('btn-danger');
                    btn.classList.add('btn-outline-light');
                } else {
                    btn.innerHTML = '<i class="fa-solid fa-lock me-1"></i> Bloquear Sistema';
                    btn.classList.remove('btn-outline-light');
                    btn.classList.add('btn-danger');
                }
            });
        })
        .catch(error => {
            console.error('Erro ao obter status do bloqueio:', error);
            botoes.forEach(btn => {
                btn.innerHTML = '<i class="fa-solid fa-lock me-1"></i> Bloquear/Desbloquear';
                btn.classList.remove('btn-outline-light', 'btn-danger');
                btn.classList.add('btn-outline-warning');
            });
        });
}

export function confirmarBloqueio(btn) {
    // Se o botão não foi passado, tenta encontrar o primeiro (fallback)
    if (!btn) {
        btn = document.querySelector('[data-role="bloquear"]');
        if (!btn) return;
    }

    const isBloqueando = btn.innerHTML.includes('Bloquear');
    const modalElement = document.getElementById('modalConfirmacaoBloqueio');
    const modalBody = document.getElementById('textoConfirmacaoBloqueio');
    const confirmBtn = document.getElementById('btnConfirmarBloqueio');

    if (!modalElement || !modalBody || !confirmBtn) {
        // Fallback para confirm() simples
        const mensagem = isBloqueando
            ? 'Deseja BLOQUEAR o sistema? Os demais usuários ficarão impedidos de executar ações.'
            : 'Deseja LIBERAR o sistema? Todos os usuários voltarão a ter acesso normal.';
        if (confirm(mensagem)) {
            const form = btn.closest('form');
            if (form) form.submit();
        }
        return;
    }

    // Configura o modal conforme a ação
    if (isBloqueando) {
        modalBody.innerHTML = 'Deseja <strong>BLOQUEAR</strong> o sistema?<br>Os demais usuários ficarão impedidos de executar ações.';
        confirmBtn.innerHTML = '<i class="fa-solid fa-lock me-1"></i> Bloquear';
        confirmBtn.className = 'btn btn-danger px-4';
    } else {
        modalBody.innerHTML = 'Deseja <strong>LIBERAR</strong> o sistema?<br>Todos os usuários voltarão a ter acesso normal.';
        confirmBtn.innerHTML = '<i class="fa-solid fa-unlock-alt me-1"></i> Liberar';
        confirmBtn.className = 'btn btn-success px-4';
    }

    // Substitui o evento do botão de confirmação
    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

    newConfirmBtn.addEventListener('click', function (e) {
        e.preventDefault();
        const modal = bootstrap.Modal.getInstance(modalElement);
        if (modal) modal.hide();

        // Submete o formulário que contém o botão clicado
        const form = btn.closest('form');
        if (form) {
            // Adiciona o motivo (se existir) – você pode manter a lógica anterior
            const motivoInput = document.querySelector('input[name="motivo"]');
            if (motivoInput && isBloqueando) {
                // se já existe, mantém
            }
            form.submit();
        }
    });

    const modal = new bootstrap.Modal(modalElement);
    modal.show();
    return false;
}