/**
 * Modais e confirmações
 */

import { submitPost } from './csrf.js';

export function confirmarAcao(url, mensagem, isDesvalidar, corPersonalizada) {
    const modalElement = document.getElementById('modalConfirmacao');
    if (!modalElement) {
        if (confirm(mensagem)) {
            submitPost(url);
        }
        return;
    }

    const modal = new bootstrap.Modal(modalElement);
    const textoConfirmacao = document.getElementById('textoConfirmacao');
    const linkConfirmacao = document.getElementById('linkConfirmacao');
    const modalHeader = document.getElementById('modalConfirmacaoHeader');

    if (textoConfirmacao) textoConfirmacao.innerText = mensagem;
    if (linkConfirmacao) {
        linkConfirmacao.removeAttribute('href');
        linkConfirmacao.onclick = function (e) {
            e.preventDefault();
            submitPost(url);
            modal.hide();
        };
    }

    if (corPersonalizada) {
        if (linkConfirmacao) linkConfirmacao.className = `btn ${corPersonalizada} px-4`;
        if (modalHeader) {
            if (corPersonalizada === 'btn-danger')
                modalHeader.className = 'modal-header bg-danger text-white';
            else if (corPersonalizada === 'btn-warning')
                modalHeader.className = 'modal-header bg-warning text-white';
            else
                modalHeader.className = 'modal-header bg-success text-white';
        }
    } else {
        if (isDesvalidar) {
            if (linkConfirmacao) linkConfirmacao.className = "btn btn-warning px-4 text-white";
            if (modalHeader) modalHeader.className = "modal-header bg-warning text-white";
        } else {
            if (linkConfirmacao) linkConfirmacao.className = "btn btn-success px-4";
            if (modalHeader) modalHeader.className = "modal-header bg-success text-white";
        }
    }

    modal.show();
}

export function prepararExclusao(baseUrl, id, nome, campoExtra) {
    const modalElement = document.getElementById('modalExclusao');
    if (!modalElement) {
        if (confirm(`Deseja excluir permanentemente ${nome}?`)) {
            submitPost(baseUrl + id);
        }
        return;
    }

    const spanNome = document.getElementById('excluirNome');
    const spanExtra = document.getElementById('excluirExtra');
    if (spanNome) spanNome.innerText = nome;
    if (spanExtra && campoExtra) spanExtra.innerText = campoExtra;
    if (spanExtra && !campoExtra) spanExtra.innerText = '';

    const btnConfirmar = document.getElementById('btnConfirmarExclusao');
    if (btnConfirmar) {
        btnConfirmar.removeAttribute('href');
        btnConfirmar.onclick = function (e) {
            e.preventDefault();
            submitPost(baseUrl + id);
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) modal.hide();
        };
    }

    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}

export function verDetalhes(btn) {
    const id = btn.getAttribute('data-id');
    const turnoVal = btn.getAttribute('data-turno');
    const regraNome = btn.getAttribute('data-regranome');
    const regraDesc = btn.getAttribute('data-regradesc');
    const regraPontos = btn.getAttribute('data-regrapontos');
    const dataAtv = btn.getAttribute('data-dataatv');
    const dataReg = btn.getAttribute('data-datareg');

    const turnoTexto = turnoVal === 'M' ? 'Manhã' : (turnoVal === 'T' ? 'Tarde' : 'Noite');

    const html = `
        <div class="row g-3">
            <div class="col-md-12"><strong>Atividade Enquadrada:</strong><br>${regraNome} - ${regraDesc}</div>
            <div class="col-md-4"><strong>Data da Atividade:</strong><br>${dataAtv}</div>
            <div class="col-md-4"><strong>Turno:</strong><br>${turnoTexto}</div>
            <div class="col-md-4"><strong>Pontuação da Regra:</strong><br><span class="badge bg-primary">${regraPontos} pontos</span></div>
            <hr>
            <div class="col-md-6"><small class="text-muted"><strong>Registrado em:</strong> ${dataReg}</small></div>
            <div class="col-md-6 text-end"><small class="text-muted"><strong>ID Atividade:</strong> #${id}</small></div>
        </div>
    `;

    const modalBody = document.getElementById('detalhesConteudo');
    if (modalBody) {
        modalBody.innerHTML = html;
        const modal = new bootstrap.Modal(document.getElementById('modalDetalhes'));
        modal.show();
    }
}

export async function verComprovante(btn) {
    const id = btn.getAttribute('data-id');
    const nome = btn.getAttribute('data-nome');

    const titulo = document.getElementById('nomeComprovanteTitulo');
    if (titulo) titulo.innerText = nome;

    const modalBody = document.getElementById('modalComprovanteBody');
    if (!modalBody) return;

    modalBody.innerHTML = `
        <div class="text-center text-white">
            <i class="fa-solid fa-spinner fa-spin fa-3x"></i>
            <p class="mt-2">Carregando comprovante...</p>
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('modalComprovante'));
    modal.show();

    try {
        const response = await fetch(`/comprovantes/download/${id}`);
        if (!response.ok) throw new Error('Erro ao carregar arquivo');

        const contentType = response.headers.get('content-type');
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);

        if (contentType.startsWith('image/')) {
            const img = document.createElement('img');
            img.src = url;
            img.style.maxWidth = '100%';
            img.style.maxHeight = '100%';
            img.style.objectFit = 'contain';
            modalBody.innerHTML = '';
            modalBody.appendChild(img);
        } else if (contentType === 'application/pdf') {
            const iframe = document.createElement('iframe');
            iframe.src = url;
            iframe.width = '100%';
            iframe.height = '100%';
            iframe.style.border = 'none';
            modalBody.innerHTML = '';
            modalBody.appendChild(iframe);
        } else {
            modalBody.innerHTML = `
                <div class="alert alert-warning m-3">
                    <i class="fa-solid fa-triangle-exclamation me-2"></i>
                    Tipo de arquivo não suportado.
                </div>
            `;
        }
    } catch (error) {
        console.error('Erro ao carregar comprovante:', error);
        modalBody.innerHTML = `
            <div class="alert alert-danger m-3">
                <i class="fa-solid fa-circle-exclamation me-2"></i>
                Não foi possível carregar o comprovante. Tente novamente mais tarde.
            </div>
        `;
    }
}