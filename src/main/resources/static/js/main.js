/**
 * Ponto de entrada do módulo ES
 */

import { initCsrf } from './csrf.js';
import { setButtonLoading, formatDateBr } from './utils.js';
import {
    confirmarAcao,
    prepararExclusao,
    verDetalhes,
    verComprovante
} from './modals.js';
import {
    atualizarConselheiros,
    atualizarRegrasPorData,
    exibirGuiaRegra,
    toggleCrm,
    atualizarTurnoVisual,
    inicializarFormularioAtividade,
    resetarFiltrosAtividadeForm,
    inicializarLoteCriacao,
    inicializarLoteEdicao
} from './form-helpers.js';
import {
    inicializarHomologacao,
    inicializarBotaoRelatorio,
    abrirModalAtividades,
    abrirRelatorioJeton
} from './jeton.js';
import { atualizarBotaoBloqueio, confirmarBloqueio } from './bloqueio.js';
import { inicializarRelatorioGraficos } from './report.js';
import { API } from './config.js';

document.addEventListener('DOMContentLoaded', function () {

    // ========== CSRF ==========
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    if (metaToken && metaHeader) {
        initCsrf(metaToken.content, metaHeader.content);
    }

    // ========== TOOLTIPS ==========
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[title]'));
    tooltipTriggerList.map(el => new bootstrap.Tooltip(el));

    // ========== MODAL CONFIRMAÇÃO GLOBAL ==========
    configurarModalConfirmacaoGlobal();

    // ========== DELEGAÇÃO DE EVENTOS POR DATA-ROLE ==========
    document.addEventListener('click', function (e) {
        const target = e.target.closest('[data-role]');
        if (!target) return;

        const role = target.getAttribute('data-role');

        switch (role) {
            case 'ver-detalhes':
                e.preventDefault();
                verDetalhes(target);
                break;
            case 'ver-comprovante':
                e.preventDefault();
                verComprovante(target);
                break;
            case 'abrir-relatorio':
                e.preventDefault();
                abrirRelatorioJeton(target);
                break;
            case 'abrir-atividades':
                e.preventDefault();
                abrirModalAtividades(target);
                break;
            case 'confirmar-acao':
                e.preventDefault();
                const url = target.getAttribute('data-url');
                const mensagem = target.getAttribute('data-mensagem') || 'Tem certeza?';
                const isDesvalidar = target.getAttribute('data-desvalidar') === 'true';
                const cor = target.getAttribute('data-cor');
                confirmarAcao(url, mensagem, isDesvalidar, cor);
                break;
            case 'excluir':
                e.preventDefault();
                const baseUrl = target.getAttribute('data-url');
                const id = target.getAttribute('data-id');
                const nome = target.getAttribute('data-nome');
                const extra = target.getAttribute('data-extra');
                if (baseUrl && id) {
                    prepararExclusao(baseUrl, id, nome, extra);
                }
                break;
            case 'resetar-filtros':
                e.preventDefault();
                resetarFiltrosAtividadeForm();
                break;
            case 'resetar-lote':
                e.preventDefault();
                if (typeof window._resetarLote === 'function') {
                    window._resetarLote();
                }
                break;
            default:
                console.warn('data-role não reconhecido:', role);
        }
    });

    // ========== DELEGAÇÃO PARA MUDANÇA EM SELECTS (onchange) ==========
    document.addEventListener('change', function (e) {
        const target = e.target;

        if (target.matches('[data-role="atualizar-conselheiros"]')) {
            const idParaSelecionar = document.getElementById('selectConselheiro')?.value || null;
            atualizarConselheiros(idParaSelecionar);
        }

        if (target.matches('[data-role="atualizar-regras"]')) {
            const idRegraAtual = document.getElementById('selectRegra')?.value || null;
            atualizarRegrasPorData(idRegraAtual);
            atualizarTurnoVisual();
        }
    });

    // Handler para bloqueio
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('[data-role="bloquear"]');
        if (btn) {
            e.preventDefault();
            const result = confirmarBloqueio();
        }
    });

    // Handler para toggle CRM
    document.addEventListener('change', function (e) {
        const target = e.target;
        if (target.matches('[data-role="toggle-crm"]')) {
            toggleCrm();
        }
    });

    // ========== INICIALIZADORES ESPECÍFICOS ==========
    inicializarHomologacao();
    inicializarBotaoRelatorio();
    inicializarFormularioAtividade();
    inicializarSpinnerFormularioAtividade();
    atualizarBotaoBloqueio();
    inicializarFiltroRegrasConjuntas();

    // Inicializa lote
    if (document.getElementById('formLote') && document.getElementById('selectGestao')) {
        if (document.querySelector('input[name="idComprovante"]')) {
            inicializarLoteEdicao();
        } else {
            inicializarLoteCriacao();
        }
    }

    // Toggle CRM
    if (document.getElementById('checkConselheiro')) {
        toggleCrm();
    }

    // Inicializa relatório Chart.js
    if (document.getElementById('chartConselheiros')) {
        inicializarRelatorioGraficos();
    }
});

// =========================================================================
// FUNÇÕES AUXILIARES DE INIT (mantidas aqui)
// =========================================================================

function configurarModalConfirmacaoGlobal() {
    const botoesConfirm = document.querySelectorAll('.btn-confirm');
    if (botoesConfirm.length === 0) return;

    const modalElement = document.getElementById('modalConfirmacaoGen');
    if (!modalElement) return;

    const modalConfirmacao = new bootstrap.Modal(modalElement);
    const btnAcao = document.getElementById('btnConfirmarAcaoGen');
    const textoAcao = document.getElementById('textoConfirmacaoGen');

    let formAtual = null;
    let urlAtual = null;

    botoesConfirm.forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();

            const message = this.getAttribute('data-mensagem') || 'Tem certeza que deseja realizar esta ação?';
            const cor = this.getAttribute('data-cor') || 'btn-danger';
            const icone = this.getAttribute('data-icone') || '';

            textoAcao.innerHTML = message;
            btnAcao.className = 'btn px-4 ' + cor;
            btnAcao.innerHTML = icone ? `<i class="${icone} me-1"></i> Confirmar` : 'Confirmar';

            if (this.tagName.toLowerCase() === 'a') {
                urlAtual = this.getAttribute('href');
                formAtual = null;
            } else {
                formAtual = this.closest('form');
                urlAtual = null;
                if (formAtual && !formAtual.checkValidity()) {
                    formAtual.reportValidity();
                    return false;
                }
            }

            modalConfirmacao.show();
        });
    });

    btnAcao.addEventListener('click', function (e) {
        e.preventDefault();
        this.classList.add('disabled');
        this.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-1"></i> Processando...';

        if (formAtual) {
            HTMLFormElement.prototype.submit.call(formAtual);
        } else if (urlAtual) {
            window.location.href = urlAtual;
        }
    });
}

function inicializarSpinnerFormularioAtividade() {
    const form = document.getElementById('formAtividade');
    if (!form) return;
    const btnSubmit = form.querySelector('button[type="submit"]');
    if (btnSubmit) {
        form.addEventListener('submit', function () {
            setButtonLoading(btnSubmit, true);
        });
    }
}

function inicializarFiltroRegrasConjuntas() {
    const selectResolucao = document.getElementById('selectResolucaoFiltro');
    const selectRegras = document.getElementById('selectRegras');
    const hiddenIds = document.getElementById('regrasSelecionadasIds');

    if (!selectResolucao || !selectRegras) return;

    function parseIdsString(str) {
        if (!str || str.trim() === '') return [];
        return str.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
    }

    function carregarRegras(resolucaoId, idsParaSelecionar) {
        const url = resolucaoId
            ? `${API.REGRAS_POR_RESOLUCAO}?resolucaoId=${resolucaoId}`
            : API.REGRAS_POR_RESOLUCAO;
        fetch(url)
            .then(response => response.json())
            .then(data => {
                const selectedValues = Array.from(selectRegras.selectedOptions).map(opt => opt.value);
                selectRegras.innerHTML = '';
                if (data.length === 0) {
                    const option = document.createElement('option');
                    option.text = 'Nenhuma regra encontrada';
                    option.disabled = true;
                    selectRegras.appendChild(option);
                } else {
                    data.forEach(regra => {
                        const option = document.createElement('option');
                        option.value = regra.id;
                        option.text = `${regra.nome} (${regra.pontos} pts) ${regra.revogado === 'S' ? '[REVOGADA]' : ''}`;
                        selectRegras.appendChild(option);
                    });
                }
                if (idsParaSelecionar && idsParaSelecionar.length > 0) {
                    idsParaSelecionar.forEach(id => {
                        const option = Array.from(selectRegras.options).find(opt => opt.value == id);
                        if (option) option.selected = true;
                    });
                } else if (selectedValues.length > 0) {
                    selectedValues.forEach(val => {
                        const option = Array.from(selectRegras.options).find(opt => opt.value == val);
                        if (option) option.selected = true;
                    });
                }
            })
            .catch(err => console.error('Erro ao carregar regras:', err));
    }

    selectResolucao.addEventListener('change', function () {
        carregarRegras(this.value);
    });

    let idsIniciais = [];
    if (hiddenIds && hiddenIds.value) {
        idsIniciais = parseIdsString(hiddenIds.value);
    }
    carregarRegras(null, idsIniciais);
}