import { initCsrf } from './csrf.js';
import { formatDateBr } from './utils.js';
import { confirmarAcao, prepararExclusao, verDetalhes, verComprovante } from './modals.js';
import {
    atualizarConselheiros, atualizarRegrasPorData, exibirGuiaRegra, toggleCrm, atualizarTurnoVisual,
    inicializarFormularioAtividade, resetarFiltrosAtividadeForm, inicializarLoteCriacao, inicializarLoteEdicao
} from './form-helpers.js';
import { inicializarHomologacao, inicializarBotaoRelatorio, abrirModalAtividades, abrirRelatorioJeton } from './jeton.js';
import { atualizarBotaoBloqueio, confirmarBloqueio } from './bloqueio.js';
import { inicializarRelatorioGraficos } from './report.js';
import { showToast, showSuccess, showError, showWarning, showInfo, initFlashToasts } from './toast.js';
import { API } from './config.js';
import { showLoading, hideLoading, fetchWithLoading } from './loading-overlay.js';
import { initValidation, validateForm } from './validation.js';
import { initNavigationGuard, watchFormChanges, resetNavigationGuard } from './navigation-guard.js';
import { initMasks, getRawValue } from './masks.js';
import { initFileUploads } from './file-upload.js';
import { initAtividadeForm } from './conselheiro-atividade.js';

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
            case 'alternar-status':
                e.preventDefault();
                const urlAlt = target.getAttribute('data-url');
                const mensagemAlt = target.getAttribute('data-mensagem') || 'Deseja alterar o status?';
                const isDesvalidarAlt = target.getAttribute('data-desvalidar') === 'true';
                confirmarAcao(urlAlt, mensagemAlt, isDesvalidarAlt);
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
            case 'ver-log':
                e.preventDefault();
                const texto = target.getAttribute('data-texto');
                const pre = document.getElementById('logDetalhesTexto');
                if (pre) {
                    try {
                        const obj = JSON.parse(texto);
                        pre.textContent = JSON.stringify(obj, null, 2);
                    } catch (err) {
                        pre.textContent = texto;
                    }
                    const modal = new bootstrap.Modal(document.getElementById('modalLogDetalhes'));
                    modal.show();
                }
                break;
            default:
                console.warn('data-role não reconhecido:', role);
        }
    });

    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (form.hasAttribute('data-loading')) {
            const message = form.getAttribute('data-loading-message') || 'Processando...';
            showLoading(message);
            setTimeout(() => {
                if (document.body.contains(form) && document.getElementById('globalLoadingOverlay')) {
                    hideLoading();
                }
            }, 3000);
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
    atualizarBotaoBloqueio();
    inicializarFiltroRegrasConjuntas();
    initFlashToasts();
    initMasks();
    initFileUploads();

    // Inicializa formulário de atividade do conselheiro (se existir)
    if (document.getElementById('formAtividade') && document.getElementById('dataAtividade')) {
        initAtividadeForm();
    }

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

    // ========== VALIDAÇÃO DE FORMULÁRIO ==========
    initValidation();

    // Adiciona validação antes do submit para formulários com data-validate
    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (form.querySelector('[data-validate]')) {
            if (!validateForm(form)) {
                e.preventDefault();
                // Foca no primeiro campo inválido
                const firstInvalid = form.querySelector('.is-invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
                // Exibe um toast de aviso (opcional)
                if (typeof showWarning === 'function') {
                    showWarning('Corrija os campos destacados antes de continuar.', 'Validação');
                }
                return false;
            }
        }
    });

    // ========== NAVEGAÇÃO COM CONFIRMAÇÃO ==========
    // Inicializa o guardião de navegação
    initNavigationGuard('Há alterações não salvas. Tem certeza que deseja sair?');

    // Observa mudanças em todos os formulários
    watchFormChanges();
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

function inicializarFiltroRegrasConjuntas() {
    const selectResolucao = document.getElementById('selectResolucaoFiltro');
    const selectRegras = document.getElementById('selectRegras');
    const hiddenIds = document.getElementById('regrasSelecionadasIds');

    if (!selectResolucao || !selectRegras) {
        console.warn('Elementos do filtro de regras conjuntas não encontrados.');
        return;
    }

    // 1. Ordenar opções do select de resoluções por valor (ID) decrescente
    const placeholder = selectResolucao.querySelector('option[value=""]');
    const options = Array.from(selectResolucao.options).filter(opt => opt.value !== '');
    options.sort((a, b) => parseInt(b.value) - parseInt(a.value));

    // Limpa e reinsere as opções ordenadas
    selectResolucao.innerHTML = '';
    if (placeholder) {
        selectResolucao.appendChild(placeholder);
    }
    options.forEach(opt => selectResolucao.appendChild(opt));

    // 2. Se nenhuma opção estiver selecionada, seleciona a primeira (mais recente)
    if (!selectResolucao.value) {
        const firstOption = selectResolucao.querySelector('option:not([disabled])');
        if (firstOption && firstOption.value) {
            firstOption.selected = true;
        }
    }

    function parseIdsString(str) {
        if (!str || str.trim() === '') return [];
        return str.split(',').map(id => parseInt(id.trim())).filter(id => !isNaN(id));
    }

    function carregarRegras(resolucaoId, idsParaSelecionar) {
        const url = resolucaoId
            ? `${API.REGRAS_POR_RESOLUCAO}/${resolucaoId}`
            : API.REGRAS_POR_RESOLUCAO;

        selectRegras.innerHTML = '<option value="" disabled>Carregando regras...</option>';
        selectRegras.disabled = true;

        fetch(url)
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(data => {
                selectRegras.innerHTML = '';
                if (!data || data.length === 0) {
                    const option = document.createElement('option');
                    option.text = 'Nenhuma regra encontrada';
                    option.disabled = true;
                    selectRegras.appendChild(option);
                    selectRegras.disabled = true;
                    return;
                }

                data.forEach(regra => {
                    const option = document.createElement('option');
                    option.value = regra.id;
                    const revogado = regra.inRevogado === 'S' ? ' [REVOGADA]' : '';
                    option.text = `${regra.nome} (${regra.pontos} pts)${revogado}`;
                    selectRegras.appendChild(option);
                });

                if (idsParaSelecionar && idsParaSelecionar.length > 0) {
                    Array.from(selectRegras.options).forEach(opt => {
                        if (idsParaSelecionar.includes(parseInt(opt.value))) {
                            opt.selected = true;
                        }
                    });
                }

                selectRegras.disabled = false;
            })
            .catch(error => {
                console.error('Erro ao carregar regras:', error);
                selectRegras.innerHTML = '<option value="" disabled>Erro ao carregar regras</option>';
                selectRegras.disabled = true;
                if (typeof showError === 'function') {
                    showError('Não foi possível carregar as regras. Tente novamente.', 'Erro');
                }
            });
    }

    // 3. Evento de mudança da resolução
    selectResolucao.addEventListener('change', function () {
        const resolucaoId = this.value ? parseInt(this.value) : null;
        const selectedIds = Array.from(selectRegras.selectedOptions).map(opt => parseInt(opt.value));
        carregarRegras(resolucaoId, selectedIds);
    });

    // 4. Carregamento inicial com a resolução já selecionada
    let idsIniciais = [];
    if (hiddenIds && hiddenIds.value) {
        idsIniciais = parseIdsString(hiddenIds.value);
    }
    const resolucaoInicial = selectResolucao.value ? parseInt(selectResolucao.value) : null;
    carregarRegras(resolucaoInicial, idsIniciais);
}

window.showLoading = showLoading;
window.hideLoading = hideLoading;
window.fetchWithLoading = fetchWithLoading;