/**
 * Funcionalidades específicas do módulo Jeton
 */

import { setButtonLoading } from './utils.js';

export function inicializarHomologacao() {
    const form = document.getElementById('formProcessamento');
    if (!form) return;

    const btnCalcular = form.querySelector('button[type="submit"]:not([formaction])');
    if (btnCalcular) {
        form.addEventListener('submit', function (e) {
            const clickedButton = document.activeElement;
            if (clickedButton && clickedButton.type === 'submit' && clickedButton === btnCalcular) {
                setButtonLoading(btnCalcular, true);
            }
        });
    }

    const btnHomologar = document.getElementById('btnHomologar');
    if (!btnHomologar) return;

    btnHomologar.removeAttribute('onclick');
    btnHomologar.addEventListener('click', function (e) {
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
    });

    const btnConfirmar = document.getElementById('btnConfirmarHomologacao');
    if (btnConfirmar) {
        btnConfirmar.addEventListener('click', function () {
            const modalEl = document.getElementById('modalHomologacao');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();

            setButtonLoading(btnHomologar, true);
            form.action = '/jeton/fechar-definitivo';
            form.method = 'post';
            form.submit();
        });
    }
}

export function inicializarBotaoRelatorio() {
    const btnBaixar = document.getElementById('btnBaixarRelatorio');
    if (!btnBaixar) return;

    btnBaixar.removeEventListener('click', window._baixarHandler);
    const handler = function () {
        const selectGestao = document.querySelector('select[name="idGestao"]');
        const selectMes = document.querySelector('select[name="mes"]');
        const inputAno = document.querySelector('input[name="ano"]');
        const idGestao = selectGestao ? selectGestao.value : '';
        const mes = selectMes ? selectMes.value : '';
        const ano = inputAno ? inputAno.value : '';

        if (!idGestao || !mes || !ano) {
            const modalAlerta = document.getElementById('modalAlertaValidacao');
            if (modalAlerta) {
                const modal = new bootstrap.Modal(modalAlerta);
                modal.show();
            } else {
                alert('Preencha todos os campos (Gestão, Mês e Ano).');
            }
            return;
        }

        const modalFormatos = document.getElementById('modalFormatosRelatorio');
        if (modalFormatos) {
            const modal = new bootstrap.Modal(modalFormatos);
            modal.show();
        }
    };
    btnBaixar.addEventListener('click', handler);
    window._baixarHandler = handler;

    const btnExcel = document.getElementById('btnExcelRelatorio');
    const btnPdf = document.getElementById('btnPdfRelatorio');

    if (btnExcel) {
        btnExcel.removeEventListener('click', window._excelHandler);
        const excelHandler = function () {
            const selectGestao = document.querySelector('select[name="idGestao"]');
            const selectMes = document.querySelector('select[name="mes"]');
            const inputAno = document.querySelector('input[name="ano"]');
            const idGestao = selectGestao ? selectGestao.value : '';
            const mes = selectMes ? selectMes.value : '';
            const ano = inputAno ? inputAno.value : '';
            if (idGestao && mes && ano) {
                window.location.href = `/jetons/relatorio?idGestao=${idGestao}&mes=${mes}&ano=${ano}&formato=excel`;
            }
            const modal = bootstrap.Modal.getInstance(document.getElementById('modalFormatosRelatorio'));
            if (modal) modal.hide();
        };
        btnExcel.addEventListener('click', excelHandler);
        window._excelHandler = excelHandler;
    }

    if (btnPdf) {
        btnPdf.removeEventListener('click', window._pdfHandler);
        btnPdf.disabled = false;
        const pdfHandler = function () {
            const selectGestao = document.querySelector('select[name="idGestao"]');
            const selectMes = document.querySelector('select[name="mes"]');
            const inputAno = document.querySelector('input[name="ano"]');
            const idGestao = selectGestao ? selectGestao.value : '';
            const mes = selectMes ? selectMes.value : '';
            const ano = inputAno ? inputAno.value : '';
            if (idGestao && mes && ano) {
                window.location.href = `/jetons/relatorio?idGestao=${idGestao}&mes=${mes}&ano=${ano}&formato=pdf`;
            }
            const modal = bootstrap.Modal.getInstance(document.getElementById('modalFormatosRelatorio'));
            if (modal) modal.hide();
        };
        btnPdf.addEventListener('click', pdfHandler);
        window._pdfHandler = pdfHandler;
    }
}

export function abrirModalAtividades(btn) {
    const idPessoa = btn.getAttribute('data-pessoa');
    const idGestao = btn.getAttribute('data-gestao');
    const mes = btn.getAttribute('data-mes');
    const ano = btn.getAttribute('data-ano');

    const tbody = document.getElementById('corpoTabelaAtividades');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="3" class="text-center py-3"><i class="fa-solid fa-spinner fa-spin me-2"></i>Buscando composição de pontos...</td></tr>';

    const modal = new bootstrap.Modal(document.getElementById('modalAtividades'));
    modal.show();

    fetch(`/api/jetons/atividades/conselheiro/${idPessoa}/gestao/${idGestao}/mes/${mes}/ano/${ano}`)
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

export function abrirRelatorioJeton(btn) {
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

    fetch(`/api/jetons/relatorio-conselheiro/${idPessoa}/gestao/${idGestao}/mes/${mes}/ano/${ano}`)
        .then(response => response.json())
        .then(dados => {
            document.getElementById('saldoExistente').innerText = dados.saldoExistente || 0;
            document.getElementById('saldoAtividades').innerText = dados.saldoAtividades || 0;
            document.getElementById('saldoUtilizado').innerText = dados.saldoUtilizado || 0;
            document.getElementById('saldoFuturo').innerText = dados.saldoFuturo || 0;

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