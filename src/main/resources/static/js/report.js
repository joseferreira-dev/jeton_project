/**
 * Módulo de relatórios – Chart.js
 */

export function inicializarRelatorioGraficos() {
    if (typeof window._dadosRelatorio === 'undefined') {
        console.warn('Dados do relatório não encontrados');
        return;
    }

    const dadosRelatorio = window._dadosRelatorio;
    if (!dadosRelatorio || dadosRelatorio.length === 0) return;

    function obterPrimeiroESegundoNome(nomeCompleto) {
        if (!nomeCompleto) return "";
        const partes = nomeCompleto.trim().split(/\s+/);
        return partes.length > 1 ? partes[0] + " " + partes[1] : partes[0];
    }

    const labelsMedicos = dadosRelatorio.map(item => obterPrimeiroESegundoNome(item.conselheiro));
    const totaisAtividades = dadosRelatorio.map(item => Object.values(item.regras).reduce((a, b) => a + b, 0));

    const colunasRegras = Object.keys(dadosRelatorio[0].regras);
    const totaisPorRegra = colunasRegras.map(col =>
        dadosRelatorio.reduce((acc, item) => acc + (item.regras[col] || 0), 0)
    );

    new Chart(document.getElementById('chartConselheiros').getContext('2d'), {
        type: 'bar',
        data: {
            labels: labelsMedicos,
            datasets: [{
                label: 'Atividades Acumuladas',
                data: totaisAtividades,
                backgroundColor: '#004b84',
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                datalabels: {
                    anchor: 'end',
                    align: 'top',
                    color: '#004b84',
                    font: { weight: 'bold', size: 11 },
                    formatter: (value) => value > 0 ? value : ''
                }
            },
            scales: {
                x: { ticks: { minRotation: 90, maxRotation: 90, autoSkip: false } },
                y: { beginAtZero: true, ticks: { precision: 0 }, grace: '8%' }
            }
        }
    });

    new Chart(document.getElementById('chartRegras').getContext('2d'), {
        type: 'pie',
        data: {
            labels: colunasRegras,
            datasets: [{
                data: totaisPorRegra,
                backgroundColor: ['#004b84', '#0072c6', '#3a9ad9', '#74b9e7', '#aed4f1', '#ffc107', '#28a745'],
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'right', labels: { boxWidth: 12, font: { size: 11 } } },
                datalabels: {
                    color: '#ffffff',
                    font: { weight: 'bold', size: 12 },
                    textShadowColor: 'rgba(0, 0, 0, 0.5)',
                    textShadowBlur: 4,
                    formatter: (value) => value > 0 ? value : ''
                }
            }
        }
    });
}