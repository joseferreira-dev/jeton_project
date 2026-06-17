/**
 * Funções auxiliares para formulários
 */

// =========================================================================
// FORMULÁRIO DE ATIVIDADES
// =========================================================================

/**
 * Inicializa o formulário de atividades
 */
function inicializarFormularioAtividade() {
    const selectGestao = document.getElementById('selectGestao');
    const selectConselheiro = document.getElementById('selectConselheiro');
    const dataAtividade = document.getElementById('dataAtividade');

    if (!selectGestao) return;

    if (selectGestao.value) {
        const idConselheiroAtual = selectConselheiro ? selectConselheiro.value : null;
        atualizarConselheiros(idConselheiroAtual);
    }

    if (dataAtividade && dataAtividade.value) {
        const selectRegra = document.getElementById('selectRegra');
        const idRegraAtual = selectRegra ? selectRegra.value : null;
        atualizarRegrasPorData(idRegraAtual);
    }

    if (dataAtividade) {
        dataAtividade.addEventListener('change', atualizarTurnoVisual);
        if (dataAtividade.value) atualizarTurnoVisual();
    }
}

/**
 * Atualiza a lista de conselheiros com base na gestão
 * @param {string|null} idParaSelecionar
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

    fetch(`/api/conselheiros/conselheiros-por-gestao?gestaoId=${gestaoId}`)
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
 * Busca regras por data e preenche select de regras e normativas
 * @param {string|null} idRegraParaSelecionar
 */
function atualizarRegrasPorData(idRegraParaSelecionar) {
    const dataValue = document.getElementById('dataAtividade')?.value;
    const selectRegra = document.getElementById('selectRegra');
    if (!dataValue || !selectRegra) return;

    selectRegra.innerHTML = '<option value="">Carregando regras da época...</option>';

    fetch(`/api/regras/regras-por-data?data=${dataValue}`)
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
 * Exibe o guia da regra selecionada
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
 * Atualiza o campo visual de turno
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

// =========================================================================
// FORMULÁRIO DE USUÁRIO (CRM toggle)
// =========================================================================

/**
 * Toggle para exibir campo CRM
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

window.atualizarConselheiros = atualizarConselheiros;
window.atualizarRegrasPorData = atualizarRegrasPorData;
window.exibirGuiaRegra = exibirGuiaRegra;
window.toggleCrm = toggleCrm;
window.atualizarTurnoVisual = atualizarTurnoVisual;
window.inicializarFormularioAtividade = inicializarFormularioAtividade;