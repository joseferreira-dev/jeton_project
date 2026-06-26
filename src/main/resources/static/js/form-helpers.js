/**
 * Funções auxiliares para formulários
 * Inclui lógica para formulários de atividade (simples, lote criação, lote edição)
 */
import { API } from './config.js';

export function atualizarConselheiros(idParaSelecionar) {
    const gestaoId = document.getElementById('selectGestao')?.value;
    const selectConselheiro = document.getElementById('selectConselheiro');
    if (!selectConselheiro) return;

    if (!gestaoId) {
        selectConselheiro.innerHTML = '<option value="">Aguardando Gestão</option>';
        selectConselheiro.disabled = true;
        return;
    }

    fetch(`${API.CONSELHEIROS_POR_GESTAO}?gestaoId=${gestaoId}`)
        .then(response => response.json())
        .then(data => {
            selectConselheiro.innerHTML = '<option value="">Selecione o Médico</option>';
            if (data && data.length > 0) {
                data.forEach(c => {
                    const selected = (idParaSelecionar && idParaSelecionar == c.id) ? 'selected' : '';
                    selectConselheiro.innerHTML += `<option value="${c.id}" ${selected}>${c.nome}</option>`;
                });
                selectConselheiro.disabled = false;
            } else {
                selectConselheiro.innerHTML = '<option value="">Nenhum médico vinculado a esta gestão</option>';
                selectConselheiro.disabled = true;
            }
        })
        .catch(err => console.error("Erro ao carregar conselheiros:", err));
}

export function atualizarRegrasPorData(idRegraParaSelecionar) {
    const dataValue = document.getElementById('dataAtividade')?.value;
    const selectRegra = document.getElementById('selectRegra');
    if (!dataValue || !selectRegra) return;

    selectRegra.innerHTML = '<option value="">Carregando regras da época...</option>';

    fetch(`${API.REGRAS_POR_DATA}?data=${dataValue}`)
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

            selectRegra.innerHTML = '<option value="">Selecione a Regra Enquadrada</option>';
            if (data.regras && data.regras.length > 0) {
                window.regrasCache = data.regras;
                data.regras.forEach(r => {
                    const selected = (idRegraParaSelecionar && idRegraParaSelecionar == r.id) ? 'selected' : '';
                    selectRegra.innerHTML += `<option value="${r.id}" ${selected}>${r.nome} (${r.pontos} pts)</option>`;
                });
                selectRegra.disabled = false;
                if (idRegraParaSelecionar) {
                    setTimeout(() => exibirGuiaRegra(), 100);
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

export function exibirGuiaRegra() {
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
            return;
        }
    }
    box.style.display = 'none';
}

export function atualizarTurnoVisual() {
    const inputData = document.getElementById('dataAtividade');
    const turnoVisual = document.getElementById('turnoVisual');
    if (!inputData || !turnoVisual) return;

    const dataHora = inputData.value;
    if (dataHora) {
        const hora = new Date(dataHora).getHours();
        let turnoTexto = "Automático";
        let cor = "initial";
        if (hora >= 6 && hora < 12) { turnoTexto = "Manhã (M)"; cor = "#0d6efd"; }
        else if (hora >= 12 && hora < 18) { turnoTexto = "Tarde (T)"; cor = "#fd7e14"; }
        else { turnoTexto = "Noite (N)"; cor = "#6c757d"; }
        turnoVisual.value = turnoTexto;
        turnoVisual.style.color = cor;
        turnoVisual.style.fontWeight = "bold";
    } else {
        turnoVisual.value = "Automático (via Horário)";
        turnoVisual.style.color = "initial";
        turnoVisual.style.fontWeight = "normal";
    }
}

export function toggleCrm() {
    const checkbox = document.getElementById('checkConselheiro');
    const divCrm = document.getElementById('divCrm');
    const inputCrm = document.getElementById('inputCrm');
    if (!checkbox || !divCrm || !inputCrm) return;

    if (checkbox.checked) {
        divCrm.style.display = 'block';
        inputCrm.required = true;
        inputCrm.dataset.validate = "crm required";
    } else {
        divCrm.style.display = 'none';
        inputCrm.required = false;
        inputCrm.value = '';
        inputCrm.dataset.validate = "crm";
    }
}

export function inicializarFormularioAtividade() {
    const selectGestao = document.getElementById('selectGestao');
    const selectConselheiro = document.getElementById('selectConselheiro');
    const dataAtividade = document.getElementById('dataAtividade');
    const selectRegra = document.getElementById('selectRegra');

    if (!selectGestao) return;

    if (selectGestao.value) {
        const idConselheiroAtual = selectConselheiro ? selectConselheiro.value : null;
        atualizarConselheiros(idConselheiroAtual);
    }

    if (dataAtividade && dataAtividade.value) {
        const idRegraAtual = selectRegra ? selectRegra.value : null;
        atualizarRegrasPorData(idRegraAtual);
    }

    if (dataAtividade) {
        dataAtividade.addEventListener('change', function () {
            atualizarTurnoVisual();
            const idRegraAtual = selectRegra ? selectRegra.value : null;
            atualizarRegrasPorData(idRegraAtual);
        });
        if (dataAtividade.value) atualizarTurnoVisual();
    }

    if (selectRegra) {
        selectRegra.addEventListener('change', exibirGuiaRegra);

        if (selectRegra.value) {
            setTimeout(exibirGuiaRegra, 100);
        }
    }
}

export function resetarFiltrosAtividadeForm() {
    const nomeResolucao = document.getElementById('nomeResolucaoVisual');
    const idResolucaoHidden = document.getElementById('idResolucaoHidden');
    const nomePortaria = document.getElementById('nomePortariaVisual');
    const idPortariaHidden = document.getElementById('idPortariaHidden');
    const selectRegra = document.getElementById('selectRegra');

    if (nomeResolucao) nomeResolucao.value = '';
    if (idResolucaoHidden) idResolucaoHidden.value = '';
    if (nomePortaria) nomePortaria.value = '';
    if (idPortariaHidden) idPortariaHidden.value = '';

    if (selectRegra) {
        selectRegra.innerHTML = '<option value="">Escolha a documentação acima para desbloquear as regras</option>';
        selectRegra.disabled = true;
    }
    const boxGuia = document.getElementById('boxGuia');
    if (boxGuia) boxGuia.style.display = 'none';
}

export function inicializarLoteCriacao() {
    const selectGestao = document.getElementById('selectGestao');
    const selectConselheiros = document.getElementById('selectConselheiros');
    const listaSelecionadosDiv = document.getElementById('listaSelecionados');
    const dataAtividade = document.getElementById('dataAtividade');
    const selectRegra = document.getElementById('selectRegra');

    if (!selectGestao) return;

    function atualizarListaSelecionados() {
        const selectedOptions = Array.from(selectConselheiros.selectedOptions);
        if (selectedOptions.length === 0) {
            listaSelecionadosDiv.innerHTML = '<span class="text-muted">Nenhum selecionado</span>';
            return;
        }
        let html = '<ul class="list-unstyled mb-0">';
        selectedOptions.forEach(opt => {
            html += `<li><i class="fa-solid fa-user-check text-success me-1"></i> ${opt.text}</li>`;
        });
        html += '</ul>';
        listaSelecionadosDiv.innerHTML = html;
    }

    function carregarConselheiros() {
        const gestaoId = selectGestao.value;
        if (!gestaoId) {
            selectConselheiros.innerHTML = '<option value="">Selecione a gestão primeiro</option>';
            selectConselheiros.disabled = true;
            atualizarListaSelecionados();
            return;
        }
        fetch(`${API.CONSELHEIROS_POR_GESTAO}?gestaoId=${gestaoId}`)
            .then(response => response.json())
            .then(data => {
                selectConselheiros.innerHTML = '';
                if (data && data.length > 0) {
                    data.forEach(c => {
                        const option = document.createElement('option');
                        option.value = c.id;
                        option.text = c.nome;
                        selectConselheiros.appendChild(option);
                    });
                    selectConselheiros.disabled = false;
                } else {
                    selectConselheiros.innerHTML = '<option value="">Nenhum conselheiro vinculado</option>';
                    selectConselheiros.disabled = true;
                }
                atualizarListaSelecionados();
            })
            .catch(err => console.error("Erro ao carregar conselheiros:", err));
    }

    function carregarRegrasPorData() {
        const data = dataAtividade.value;
        if (!data) {
            selectRegra.innerHTML = '<option value="">Selecione a data</option>';
            selectRegra.disabled = true;
            document.getElementById('nomeResolucaoVisual').value = '';
            document.getElementById('idResolucaoHidden').value = '';
            document.getElementById('nomePortariaVisual').value = '';
            document.getElementById('idPortariaHidden').value = '';
            document.getElementById('boxGuia').style.display = 'none';
            return;
        }

        selectRegra.innerHTML = '<option value="">Carregando regras...</option>';
        fetch(`/api/regras/regras-por-data?data=${data}`)
            .then(response => response.json())
            .then(result => {
                document.getElementById('nomeResolucaoVisual').value = result.nomeResolucao || "Nenhuma encontrada";
                document.getElementById('idResolucaoHidden').value = result.idResolucao || "";
                document.getElementById('nomePortariaVisual').value = result.nomePortaria || "Nenhuma encontrada";
                document.getElementById('idPortariaHidden').value = result.idPortaria || "";

                selectRegra.innerHTML = '<option value="">Selecione a Regra Enquadrada</option>';
                if (result.regras && result.regras.length > 0) {
                    window.regrasCache = result.regras;
                    result.regras.forEach(r => {
                        const option = document.createElement('option');
                        option.value = r.id;
                        option.text = `${r.nome} (${r.pontos} pts)`;
                        selectRegra.appendChild(option);
                    });
                    selectRegra.disabled = false;
                    exibirGuiaRegra();
                } else {
                    selectRegra.innerHTML = '<option value="">Nenhuma regra cadastrada para este período</option>';
                    selectRegra.disabled = true;
                    document.getElementById('boxGuia').style.display = 'none';
                }
            })
            .catch(error => {
                console.error('Erro ao buscar regras por data:', error);
                selectRegra.innerHTML = '<option value="">Erro ao carregar regras</option>';
                selectRegra.disabled = true;
            });
    }

    function atualizarTurno() {
        const dataHora = dataAtividade.value;
        const turnoVisual = document.getElementById('turnoVisual');
        if (!turnoVisual) return;
        if (dataHora) {
            const hora = new Date(dataHora).getHours();
            let turnoTexto = "Automático";
            let cor = "initial";
            if (hora >= 6 && hora < 12) { turnoTexto = "Manhã (M)"; cor = "#0d6efd"; }
            else if (hora >= 12 && hora < 18) { turnoTexto = "Tarde (T)"; cor = "#fd7e14"; }
            else { turnoTexto = "Noite (N)"; cor = "#6c757d"; }
            turnoVisual.value = turnoTexto;
            turnoVisual.style.color = cor;
        } else {
            turnoVisual.value = "Automático (via Horário)";
            turnoVisual.style.color = "initial";
        }
    }

    function resetarFiltros() {
        document.getElementById('nomeResolucaoVisual').value = '';
        document.getElementById('idResolucaoHidden').value = '';
        document.getElementById('nomePortariaVisual').value = '';
        document.getElementById('idPortariaHidden').value = '';
        selectRegra.innerHTML = '<option value="">Escolha a documentação acima para desbloquear as regras</option>';
        selectRegra.disabled = true;
        document.getElementById('boxGuia').style.display = 'none';
    }

    selectGestao.addEventListener('change', carregarConselheiros);
    selectConselheiros.addEventListener('change', atualizarListaSelecionados);
    dataAtividade.addEventListener('change', () => {
        carregarRegrasPorData();
        atualizarTurno();
    });
    selectRegra.addEventListener('change', exibirGuiaRegra);

    if (selectGestao.value) carregarConselheiros();
    if (dataAtividade.value) {
        carregarRegrasPorData();
        atualizarTurno();
    }

    window._resetarLote = resetarFiltros;
}

export function inicializarLoteEdicao() {
    const config = window.loteConfig || { idsAtuais: [], idRegraAtual: null };
    const idsAtuaisLote = config.idsAtuais || [];
    const idRegraAtual = config.idRegraAtual || null;

    const selectGestao = document.getElementById('selectGestao');
    const selectConselheiros = document.getElementById('selectConselheiros');
    const listaSelecionadosDiv = document.getElementById('listaSelecionados');
    const dataAtividade = document.getElementById('dataAtividade');
    const selectRegra = document.getElementById('selectRegra');

    if (!selectGestao) return;

    function atualizarListaSelecionados() {
        const selectedOptions = Array.from(selectConselheiros.selectedOptions);
        if (selectedOptions.length === 0) {
            listaSelecionadosDiv.innerHTML = '<span class="text-muted">Nenhum selecionado</span>';
            return;
        }
        let html = '<ul class="list-unstyled mb-0">';
        selectedOptions.forEach(opt => {
            html += `<li><i class="fa-solid fa-user-check text-success me-1"></i> ${opt.text}</li>`;
        });
        html += '</ul>';
        listaSelecionadosDiv.innerHTML = html;
    }

    function carregarConselheiros() {
        const gestaoId = selectGestao.value;
        if (!gestaoId) {
            selectConselheiros.innerHTML = '<option value="">Selecione a gestão primeiro</option>';
            selectConselheiros.disabled = true;
            atualizarListaSelecionados();
            return;
        }
        fetch(`${API.CONSELHEIROS_POR_GESTAO}?gestaoId=${gestaoId}`)
            .then(response => response.json())
            .then(data => {
                selectConselheiros.innerHTML = '';
                if (data && data.length > 0) {
                    data.forEach(c => {
                        const option = document.createElement('option');
                        option.value = c.id;
                        option.text = c.nome;
                        if (idsAtuaisLote.includes(c.id)) {
                            option.selected = true;
                        }
                        selectConselheiros.appendChild(option);
                    });
                    selectConselheiros.disabled = false;
                } else {
                    selectConselheiros.innerHTML = '<option value="">Nenhum conselheiro vinculado</option>';
                    selectConselheiros.disabled = true;
                }
                atualizarListaSelecionados();
            })
            .catch(err => console.error("Erro ao carregar conselheiros:", err));
    }

    function resetarFiltros() {
        document.getElementById('nomeResolucaoVisual').value = '';
        document.getElementById('idResolucaoHidden').value = '';
        document.getElementById('nomePortariaVisual').value = '';
        document.getElementById('idPortariaHidden').value = '';
        selectRegra.innerHTML = '<option value="">Escolha a documentação acima para desbloquear as regras</option>';
        selectRegra.disabled = true;
        document.getElementById('boxGuia').style.display = 'none';
    }

    selectGestao.addEventListener('change', carregarConselheiros);
    selectConselheiros.addEventListener('change', atualizarListaSelecionados);

    dataAtividade.addEventListener('change', function () {
        if (typeof window.atualizarRegrasPorData === 'function') {
            window.atualizarRegrasPorData(idRegraAtual);
        }
        if (typeof window.atualizarTurnoVisual === 'function') {
            window.atualizarTurnoVisual();
        }
    });

    if (selectGestao.value) carregarConselheiros();
    if (dataAtividade.value) {
        if (typeof window.atualizarRegrasPorData === 'function') {
            window.atualizarRegrasPorData(idRegraAtual);
        }
        if (typeof window.atualizarTurnoVisual === 'function') {
            window.atualizarTurnoVisual();
        }
    }

    window._resetarLote = resetarFiltros;
}