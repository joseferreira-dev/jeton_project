/**
 * Funcionalidades específicas do formulário de atividades do conselheiro
 */

import { showLoading, hideLoading } from './loading-overlay.js';

export function initAtividadeForm() {
    const dataInput = document.getElementById('dataAtividade');
    const selectRegra = document.getElementById('selectRegra');
    const turnoVisual = document.getElementById('turnoVisual');
    const loadingDiv = document.getElementById('regrasLoading');
    const errorDiv = document.getElementById('regrasError');
    const currentRegraId = document.getElementById('currentRegraId')?.value || '';

    if (!dataInput || !selectRegra) return;

    function exibirGuiaRegra() {
        const id = selectRegra?.value;
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

    async function carregarRegrasPorData() {
        const data = dataInput.value;
        if (!data) {
            selectRegra.innerHTML = '<option value="">Selecione a data para carregar as regras</option>';
            selectRegra.disabled = true;
            return;
        }

        loadingDiv.style.display = 'block';
        errorDiv.style.display = 'none';

        try {
            const response = await fetch(`/api/regras/regras-por-data?data=${data}`);
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const result = await response.json();
            if (result.erro) {
                throw new Error(result.erro);
            }

            selectRegra.innerHTML = '<option value="">Selecione a Regra</option>';

            if (result.regras && result.regras.length > 0) {
                window.regrasCache = result.regras;
                result.regras.forEach(r => {
                    const option = document.createElement('option');
                    option.value = r.id;
                    option.text = `${r.nome} (${r.pontos} pts)`;
                    if (currentRegraId && currentRegraId == r.id) {
                        option.selected = true;
                    }
                    selectRegra.appendChild(option);
                });
                selectRegra.disabled = false;

                if (selectRegra.value) {
                    setTimeout(exibirGuiaRegra, 100);
                }
            } else {
                selectRegra.innerHTML = '<option value="">Nenhuma regra disponível para esta data</option>';
                selectRegra.disabled = true;
                const box = document.getElementById('boxGuia');
                if (box) box.style.display = 'none';
            }
        } catch (err) {
            console.error('Erro ao carregar regras:', err);
            errorDiv.innerText = 'Erro ao carregar regras. Tente novamente mais tarde.';
            errorDiv.style.display = 'block';
            selectRegra.innerHTML = '<option value="">Erro ao carregar regras</option>';
            selectRegra.disabled = true;
            const box = document.getElementById('boxGuia');
            if (box) box.style.display = 'none';
        } finally {
            loadingDiv.style.display = 'none';
        }
    }

    function atualizarTurno() {
        const dataHora = dataInput.value;
        if (dataHora) {
            const hora = new Date(dataHora).getHours();
            let turno = '';
            if (hora >= 6 && hora < 12) turno = 'Manhã (M)';
            else if (hora >= 12 && hora < 18) turno = 'Tarde (T)';
            else turno = 'Noite (N)';
            turnoVisual.value = turno;
        } else {
            turnoVisual.value = 'Automático';
        }
    }

    dataInput.addEventListener('change', () => {
        carregarRegrasPorData();
        atualizarTurno();
    });

    selectRegra.addEventListener('change', exibirGuiaRegra);

    if (dataInput.value) {
        carregarRegrasPorData();
        atualizarTurno();
    }
}

document.addEventListener('DOMContentLoaded', function () {
    if (document.getElementById('formAtividade') && document.getElementById('dataAtividade')) {
        initAtividadeForm();
    }
});