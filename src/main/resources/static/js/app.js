/**
 * JETON - Funções JavaScript Globais
 */

// =========================================================================
// INICIALIZAÇÃO GLOBAL
// =========================================================================

document.addEventListener('DOMContentLoaded', function () {
    // Inicializa todos os tooltips do Bootstrap
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[title]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Configura o modal de confirmação global (já existe no layout.html)
    configurarModalConfirmacaoGlobal();

    // Delegação de eventos para botões de exclusão (classe .btn-excluir)
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

    // Alternar status (ativar/inativar)
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

    inicializarHomologacao();
});

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

// =========================================================================
// FUNÇÕES DE CONFIRMAÇÃO E EXCLUSÃO (genéricas)
// =========================================================================

/**
 * Exibe modal de confirmação para ações comuns (validar, desvalidar, etc.)
 * @param {string} url - URL para onde redirecionar após confirmação
 * @param {string} mensagem - Texto da mensagem de confirmação
 * @param {boolean} isDesvalidar - Se true, muda o estilo do botão para warning
 */
function confirmarAcao(url, mensagem, isDesvalidar, corPersonalizada) {
    const modalElement = document.getElementById('modalConfirmacao');
    if (!modalElement) {
        if (confirm(mensagem)) window.location.href = url;
        return;
    }

    const modal = new bootstrap.Modal(modalElement);
    const textoConfirmacao = document.getElementById('textoConfirmacao');
    const linkConfirmacao = document.getElementById('linkConfirmacao');
    const modalHeader = document.getElementById('modalConfirmacaoHeader');

    if (textoConfirmacao) textoConfirmacao.innerText = mensagem;
    if (linkConfirmacao) linkConfirmacao.setAttribute('href', url);

    // Define classes com base nos parâmetros
    if (corPersonalizada) {
        // Usa a cor personalizada (ex: 'btn-danger')
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
        // Comportamento original
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

/**
 * Prepara a exclusão de um registro (genérico)
 * @param {string} baseUrl - URL base do recurso (ex: '/conselheiros/excluir/')
 * @param {number|string} id - ID do registro
 * @param {string} nome - Nome descritivo (exibido no modal)
 * @param {string} campoExtra - Campo adicional opcional (ex: CRM, gestão)
 */
function prepararExclusao(baseUrl, id, nome, campoExtra) {
    const modalElement = document.getElementById('modalExclusao');
    if (!modalElement) {
        // Fallback
        if (confirm(`Deseja excluir permanentemente ${nome}?`)) {
            window.location.href = baseUrl + id;
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
        btnConfirmar.setAttribute('href', baseUrl + id);
    }

    const modal = new bootstrap.Modal(modalElement);
    modal.show();
}

// =========================================================================
// FUNÇÕES PARA MODAIS DE DETALHES E COMPROVANTES
// =========================================================================

/**
 * Exibe modal com detalhes de uma atividade (usado em listas)
 * @param {HTMLElement} btn - Botão que contém os data attributes
 */
function verDetalhes(btn) {
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

/**
 * Carrega e exibe um comprovante em modal (iframe para PDF, imagem para img)
 * @param {HTMLElement} btn - Botão com data-id e data-nome
 */
async function verComprovante(btn) {
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
                    Tipo de arquivo não suportado para visualização direta.
                    <a href="${url}" download target="_blank" class="alert-link">Clique aqui para baixar</a>.
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

// =========================================================================
// FUNÇÕES ESPECÍFICAS PARA JETON
// =========================================================================

function inicializarHomologacao() {
    const form = document.getElementById('formProcessamento');
    const btnHomologar = document.getElementById('btnHomologar');
    if (!btnHomologar || !form) return;

    btnHomologar.removeAttribute('onclick');

    let confirmado = false;

    const handleClick = function (e) {
        if (confirmado) {
            confirmado = false;
            return;
        }
        e.preventDefault();

        const selectGestao = form.querySelector('select[name="idGestao"]');
        const selectMes = form.querySelector('select[name="mes"]');
        const inputAno = form.querySelector('input[name="ano"]');

        if (!selectGestao.value || !selectMes.value || !inputAno.value) {
            const modalAlerta = new bootstrap.Modal(document.getElementById('modalAlertaValidacao'));
            modalAlerta.show();
            return;
        }

        const modalHomologacao = new bootstrap.Modal(document.getElementById('modalHomologacao'));
        modalHomologacao.show();
    };

    btnHomologar.addEventListener('click', handleClick);

    const btnConfirmar = document.getElementById('btnConfirmarHomologacao');
    if (btnConfirmar) {
        btnConfirmar.addEventListener('click', function () {
            const modalEl = document.getElementById('modalHomologacao');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();

            confirmado = true;
            btnHomologar.click();
        });
    }
}

/**
 * Abre modal com atividades que compõem um jeton (histórico)
 * @param {HTMLElement} btn - Botão com data-pessoa, data-gestao, data-mes, data-ano
 */
function abrirModalAtividades(btn) {
    const idPessoa = btn.getAttribute('data-pessoa');
    const idGestao = btn.getAttribute('data-gestao');
    const mes = btn.getAttribute('data-mes');
    const ano = btn.getAttribute('data-ano');

    const tbody = document.getElementById('corpoTabelaAtividades');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3"><i class="fa-solid fa-spinner fa-spin me-2"></i>Buscando composição de pontos...</td></tr>';

    const modal = new bootstrap.Modal(document.getElementById('modalAtividades'));
    modal.show();

    fetch(`/jeton/atividades/conselheiro/${idPessoa}/gestao/${idGestao}/mes/${mes}/ano/${ano}`)
        .then(response => response.json())
        .then(dados => {
            tbody.innerHTML = '';
            if (dados.length === 0) {
                tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3 text-muted">Este registro originou-se exclusivamente de saldos remanescentes de meses anteriores.</td></tr>';
                return;
            }
            dados.forEach(at => {
                tbody.innerHTML += `
                    <tr>
                        <td class="ps-3 fw-semibold text-dark">${at.regra}</td>
                        <td class="text-center"><span class="badge bg-secondary">${at.data.split('-').reverse().join('/')}</span></td>
                        <td class="text-center pe-3 fw-bold text-primary">${at.qtd}</td>
                    </tr>
                `;
            });
        })
        .catch(error => {
            tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3 text-danger">Erro ao carregar as atividades.</td></tr>';
        });
}

/**
 * Abre modal com relatório detalhado do jeton (origem dos pontos)
 * @param {HTMLElement} btn - Botão com data-pessoa, data-gestao, data-mes, data-ano, data-nome
 */
function abrirRelatorioJeton(btn) {
    const idPessoa = btn.getAttribute('data-pessoa');
    const idGestao = btn.getAttribute('data-gestao');
    const mes = btn.getAttribute('data-mes');
    const ano = btn.getAttribute('data-ano');
    const nome = btn.getAttribute('data-nome');

    const nomeEl = document.getElementById('nomeConselheiroRelatorio');
    const competenciaEl = document.getElementById('competenciaRelatorio');
    if (nomeEl) nomeEl.innerText = nome || 'Conselheiro';
    if (competenciaEl) competenciaEl.innerText = `Competência: ${mes}/${ano}`;

    const tbody = document.getElementById('corpoTabelaRelatorio');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3"><i class="fa-solid fa-spinner fa-spin me-2"></i>Carregando dados...</td></tr>';
    }

    const modal = new bootstrap.Modal(document.getElementById('modalRelatorio'));
    modal.show();

    fetch(`/jeton/relatorio-conselheiro/${idPessoa}/gestao/${idGestao}/mes/${mes}/ano/${ano}`)
        .then(response => response.json())
        .then(dados => {
            const saldoAnterior = document.getElementById('saldoAnterior');
            const pontosAcumulados = document.getElementById('pontosAcumulados');
            const saldoFuturo = document.getElementById('saldoFuturo');
            if (saldoAnterior) saldoAnterior.innerText = dados.saldoAnterior || 0;
            if (pontosAcumulados) pontosAcumulados.innerText = dados.pontosAcumuladosMes || 0;
            if (saldoFuturo) saldoFuturo.innerText = dados.saldoFuturo || 0;

            if (tbody) {
                tbody.innerHTML = '';
                if (dados.atividades && dados.atividades.length > 0) {
                    dados.atividades.forEach(at => {
                        tbody.innerHTML += `
                            <tr>
                                <td class="fw-semibold text-dark">${at.regra}</td>
                                <td class="text-center"><span class="badge bg-secondary">${at.data.split('-').reverse().join('/')}</span></td>
                                <td class="text-center fw-bold text-primary">${at.qtd}</td>
                            </tr>
                        `;
                    });
                } else {
                    tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3 text-muted">Nenhuma atividade registrada no mês.</td></tr>';
                }
            }
        })
        .catch(error => {
            if (tbody) {
                tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3 text-danger">Erro ao carregar os dados do relatório.</td></tr>';
            }
        });
}

// =========================================================================
// FUNÇÕES AUXILIARES PARA FORMULÁRIOS (atividade conselhal)
// =========================================================================

/**
 * Atualiza a lista de conselheiros com base na gestão selecionada
 * @param {string|null} idParaSelecionar - ID do conselheiro a ser pré-selecionado (opcional)
 */
function atualizarConselheiros(idParaSelecionar) {
    const gestaoId = document.getElementById('selectGestao')?.value;
    const selectConselheiro = document.getElementById('selectConselheiro');
    if (!selectConselheiro) return;

    if (!gestaoId) {
        selectConselheiro.innerHTML = '<option value="">-- Aguardando Gestão --</option>';
        selectConselheiro.disabled = true;
        return;
    }

    fetch(`/atividades/api/conselheiros-por-gestao?gestaoId=${gestaoId}`)
        .then(response => response.json())
        .then(data => {
            selectConselheiro.innerHTML = '<option value="">-- Selecione o Médico --</option>';
            if (data && data.length > 0) {
                data.forEach(c => {
                    const selected = (idParaSelecionar && idParaSelecionar == c.id) ? 'selected' : '';
                    selectConselheiro.innerHTML += `<option value="${c.id}" ${selected}>${c.nome}</option>`;
                });
                selectConselheiro.disabled = false;
            } else {
                selectConselheiro.innerHTML = '<option value="">-- Nenhum médico vinculado a esta gestão --</option>';
                selectConselheiro.disabled = true;
            }
        })
        .catch(err => console.error("Erro ao carregar conselheiros:", err));
}

/**
 * Busca regras por data e preenche o select de regras e os campos de normativas
 * @param {string|null} idRegraParaSelecionar - ID da regra a ser pré-selecionada (opcional)
 */
function atualizarRegrasPorData(idRegraParaSelecionar) {
    const dataValue = document.getElementById('dataAtividade')?.value;
    const selectRegra = document.getElementById('selectRegra');
    if (!dataValue || !selectRegra) return;

    selectRegra.innerHTML = '<option value="">Carregando regras da época...</option>';

    fetch(`/atividades/api/regras-por-data?data=${dataValue}`)
        .then(response => response.json())
        .then(data => {
            const nomeResolucao = document.getElementById('nomeResolucaoVisual');
            const idResolucaoHidden = document.getElementById('idResolucaoHidden');
            const nomePortaria = document.getElementById('nomePortariaVisual');
            const idPortariaHidden = document.getElementById('idPortariaHidden');

            if (nomeResolucao) nomeResolucao.value = data.nomeResolucao || "Nenhuma encontrada";
            if (idResolucaoHidden) idResolucaoHidden.value = data.idResolucao || "";
            if (nomePortaria) nomePortaria.value = data.nomePortaria || "Nenhuma encontrada";
            if (idPortariaHidden) idPortariaHidden.value = data.idPortaria || "";

            selectRegra.innerHTML = '<option value="">-- Selecione a Regra Enquadrada --</option>';

            if (data.regras && data.regras.length > 0) {
                window.regrasCache = data.regras;
                data.regras.forEach(r => {
                    const selected = (idRegraParaSelecionar && idRegraParaSelecionar == r.id) ? 'selected' : '';
                    selectRegra.innerHTML += `<option value="${r.id}" ${selected}>${r.nome} (${r.pontos} pts)</option>`;
                });
                selectRegra.disabled = false;
                if (idRegraParaSelecionar) {
                    setTimeout(exibirGuiaRegra, 100);
                }
            } else {
                selectRegra.innerHTML = '<option value="">Nenhuma regra cadastrada para as normativas deste período</option>';
                selectRegra.disabled = true;
            }
        })
        .catch(error => {
            console.error('Erro ao buscar regras por data:', error);
            selectRegra.innerHTML = '<option value="">Erro ao carregar regras</option>';
        });
}

/**
 * Exibe o guia da regra selecionada (descrição e pontos)
 */
function exibirGuiaRegra() {
    const id = document.getElementById('selectRegra')?.value;
    const box = document.getElementById('boxGuia');
    if (!box) return;

    if (id && window.regrasCache) {
        const regra = window.regrasCache.find(r => r.id == id);
        if (regra) {
            const descricaoSpan = document.getElementById('guiaDescricao');
            const pontosSpan = document.getElementById('guiaPontos');
            if (descricaoSpan) descricaoSpan.innerText = regra.descricao || '';
            if (pontosSpan) pontosSpan.innerText = regra.pontos;
            box.style.display = 'block';
        } else {
            box.style.display = 'none';
        }
    } else {
        box.style.display = 'none';
    }
}

/**
 * Atualiza o campo visual de turno baseado na data/hora
 */
function atualizarTurnoVisual() {
    const inputData = document.getElementById('dataAtividade');
    const turnoVisual = document.getElementById('turnoVisual');
    if (!inputData || !turnoVisual) return;

    const dataHora = inputData.value;
    if (dataHora) {
        const hora = new Date(dataHora).getHours();
        let turnoTexto = "Automático";
        let cor = "initial";
        if (hora >= 6 && hora < 12) {
            turnoTexto = "Manhã (M)";
            cor = "#0d6efd";
        } else if (hora >= 12 && hora < 18) {
            turnoTexto = "Tarde (T)";
            cor = "#fd7e14";
        } else {
            turnoTexto = "Noite (N)";
            cor = "#6c757d";
        }
        turnoVisual.value = turnoTexto;
        turnoVisual.style.color = cor;
        turnoVisual.style.fontWeight = "bold";
    } else {
        turnoVisual.value = "Automático (via Horário)";
        turnoVisual.style.color = "initial";
        turnoVisual.style.fontWeight = "normal";
    }
}

/**
 * Toggle para exibir campo CRM no formulário de usuário (quando é conselheiro)
 */
function toggleCrm() {
    const checkbox = document.getElementById('checkConselheiro');
    const divCrm = document.getElementById('divCrm');
    const inputCrm = document.getElementById('inputCrm');
    if (!checkbox || !divCrm || !inputCrm) return;

    if (checkbox.checked) {
        divCrm.style.display = 'block';
        inputCrm.required = true;
    } else {
        divCrm.style.display = 'none';
        inputCrm.required = false;
        inputCrm.value = '';
    }
}

// =========================================================================
// EXPORTA FUNÇÕES PARA O ESCOPO GLOBAL (para uso em onclick, etc.)
// =========================================================================

window.confirmarAcao = confirmarAcao;
window.prepararExclusao = prepararExclusao;
window.verDetalhes = verDetalhes;
window.verComprovante = verComprovante;
window.inicializarHomologacao = inicializarHomologacao;
window.abrirModalAtividades = abrirModalAtividades;
window.abrirRelatorioJeton = abrirRelatorioJeton;
window.atualizarConselheiros = atualizarConselheiros;
window.atualizarRegrasPorData = atualizarRegrasPorData;
window.exibirGuiaRegra = exibirGuiaRegra;
window.toggleCrm = toggleCrm;
window.atualizarTurnoVisual = atualizarTurnoVisual;