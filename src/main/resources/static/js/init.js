/**
 * Inicialização global – registra event listeners e chama inits
 */

document.addEventListener('DOMContentLoaded', function () {

    // 1. Captura tokens CSRF (já definidos em csrf.js)
    const metaToken = document.querySelector('meta[name="_csrf"]');
    const metaHeader = document.querySelector('meta[name="_csrf_header"]');
    if (metaToken) window.csrfToken = metaToken.content;
    if (metaHeader) window.csrfHeader = metaHeader.content;

    // 2. Tooltips do Bootstrap
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[title]'));
    tooltipTriggerList.map(el => new bootstrap.Tooltip(el));

    // 3. Configura modal de confirmação global (btn-confirm)
    configurarModalConfirmacaoGlobal();

    // 4. Event delegation para botões de exclusão (.btn-excluir)
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('.btn-excluir');
        if (!btn) return;
        e.preventDefault();
        const baseUrl = btn.getAttribute('data-url');
        const id = btn.getAttribute('data-id');
        const nome = btn.getAttribute('data-nome');
        const extra = btn.getAttribute('data-extra');
        if (baseUrl && id) {
            prepararExclusao(baseUrl, id, nome, extra);
        }
    });

    // 5. Event delegation para alternar status (.btn-alternar-status)
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('.btn-alternar-status');
        if (!btn) return;
        e.preventDefault();
        const url = btn.getAttribute('data-url');
        const mensagem = btn.getAttribute('data-mensagem');
        const isDesvalidar = btn.getAttribute('data-desvalidar') === 'true';
        if (url) {
            confirmarAcao(url, mensagem, isDesvalidar);
        }
    });

    // 6. Event delegation para ver detalhes do log (.btn-ver-log)
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('.btn-ver-log');
        if (!btn) return;
        e.preventDefault();
        const texto = btn.getAttribute('data-texto');
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
    });

    // 7. Inicializações específicas
    inicializarHomologacao();
    inicializarBotaoRelatorio();
    inicializarFiltroRegrasConjuntas();    // (função ainda em app.js? moveremos depois)
    inicializarFormularioAtividade();
    inicializarSpinnerFormularioAtividade();
    atualizarBotaoBloqueio();

    // 8. Toggle CRM no formulário de usuário
    if (document.getElementById('checkConselheiro')) {
        toggleCrm();
    }

});

// =========================================================================
// FUNÇÕES AUXILIARES (ainda não movidas para módulo)
// =========================================================================

/**
 * Configura o modal de confirmação global (btn-confirm)
 */
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

/**
 * Inicializa o spinner no formulário de atividade
 */
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

/**
 * Inicializa o filtro de Regras Conjuntas (a ser movido)
 */
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
        const url = resolucaoId ? `/regras/regras-por-resolucao?resolucaoId=${resolucaoId}` : '/regras/regras-por-resolucao';
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