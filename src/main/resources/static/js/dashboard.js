const dashboard = document.getElementById("dashboard");

const formulario = document.getElementById("filtroDashboard");
const botaoFiltrar = document.getElementById("botaoFiltrar");
const botaoAtualizar = document.getElementById("botaoAtualizar");
const mensagem = document.getElementById("mensagemDashboard");
const mensagemGraficos = document.getElementById("mensagemGraficos");
let graficoFaturamento = null;
let graficoPedidos = null;

const formatadorMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL"});

function atualizarIndicadores(dados) {
    document.getElementById("totalPedidos").textContent = dados.totalPedidos;
    document.getElementById("pedidosFinalizados").textContent = dados.pedidosFinalizados;
    document.getElementById("pedidosCancelados").textContent = dados.pedidosCancelados;
    document.getElementById("faturamento").textContent = formatadorMoeda.format(dados.faturamento);
    document.getElementById("ticketMedio").textContent = formatadorMoeda.format(dados.ticketMedio);
}

async function carregarGraficos(inicio, fim) {
    const url = new URL(`${dashboard.dataset.apiUrl}/vendas`, window.location.origin);

    if (inicio) {
        url.searchParams.set("inicio", inicio);
    }

    if (fim) {
        url.searchParams.set("fim", fim);
    }

    try {
        const resposta = await fetch(url, {
            credentials: "same-origin",
            headers: {
                "Accept": "application/json"
            },
            cache: "no-store"
        });

        if (!resposta.ok) {
            const erro = await resposta.json().catch(() => ({}));
            throw new Error(erro.erro || "Não foi possível consultar as vendas.");
        }

        const vendas = await resposta.json();

        if (!Array.isArray(vendas)) {
            throw new Error("Formato de dados inválido.");
        }

        if (typeof Chart === "undefined") {
            throw new Error("A biblioteca Chart.js não foi carregada.");
        }

        const datas = vendas.map(venda => {
            const [ano, mes, dia] = venda.data.split("-");
            return `${dia}/${mes}/${ano}`;
        });


        const faturamentos = vendas.map(venda => venda.faturamento);
        const pedidos = vendas.map(venda => venda.pedidosFinalizados);

        if (graficoFaturamento !== null) {
            graficoFaturamento.destroy();
        }

        if (graficoPedidos !== null) {
            graficoPedidos.destroy();
        }

        graficoFaturamento = new Chart(
            document.getElementById("graficoFaturamento"),
            {
                type: "bar",

                data: {
                    labels: datas,

                    datasets: [{
                        label: "Faturamento",
                        data: faturamentos,
                        backgroundColor: "#6c757d"
                    }]
                },

                options: {
                    responsive: true,
                    maintainAspectRatio: false,

                    plugins: {
                        legend: {
                            display: false
                        },

                        tooltip: {
                            callbacks: {
                                label: contexto => formatadorMoeda.format(contexto.parsed.y)
                            }
                        }
                    },

                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            }
        );

        graficoPedidos = new Chart(
            document.getElementById("graficoPedidos"),
            {
                type: "line",

                data: {
                    labels: datas,

                    datasets: [{
                        label: "Pedidos finalizados",
                        data: pedidos,
                        borderColor: "#212529",
                        backgroundColor: "#212529",
                        tension: 0.3
                    }]
                },

                options: {
                    responsive: true,
                    maintainAspectRatio: false,

                    plugins: {
                        legend: {
                            display: false
                        }
                    },

                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                precision: 0
                            }
                        }
                    }
                }
            }
        );

        const semVendas = vendas.every(
            venda => venda.pedidosFinalizados === 0
        );

        if (semVendas) {
            mensagemGraficos.className = "alert alert-info mb-3";
            mensagemGraficos.textContent = "Nenhum pedido finalizado encontrado no período.";
        } else {
            mensagemGraficos.className = "d-none";
            mensagemGraficos.textContent = "";
        }
    } catch (erro) {
        mensagemGraficos.className = "alert alert-danger mb-3";

        if (erro instanceof TypeError) {
            mensagemGraficos.textContent = "Não foi possível conectar ao servidor.";
        } else {
            mensagemGraficos.textContent = erro.message;
        }
    }
}

async function carregarDashboard() {
    const inicio = document.getElementById("inicio").value;
    const fim = document.getElementById("fim").value;
    const url = new URL(dashboard.dataset.apiUrl, window.location.origin);

    if (inicio) {
        url.searchParams.set("inicio", inicio);
    }

    if (fim) {
        url.searchParams.set("fim", fim);
    }

    botaoFiltrar.disabled = true;
    botaoAtualizar.disabled = true;
    mensagem.className = "alert alert-info mb-3";
    mensagem.textContent = "Carregando indicadores...";
    mensagemGraficos.className = "d-none";
    mensagemGraficos.textContent = "";

    try {
        const resposta = await fetch(url, {
            credentials: "same-origin",
            headers: {
                "Accept": "application/json"
            },
            cache: "no-store"
        });

        const tipoResposta = resposta.headers.get("content-type") || "";

        if (!tipoResposta.includes("application/json")) {
            throw new Error("Não foi possível obter os dados. Verifique sua sessão.");
        }

        const dados = await resposta.json();

        if (!resposta.ok) {
            throw new Error(dados.erro || "Erro ao consultar o dashboard.");
        }

        atualizarIndicadores(dados);
        await carregarGraficos(inicio, fim);

        mensagem.className = "d-none";
        mensagem.textContent = "";

    } catch (erro) {
        mensagem.className = "alert alert-danger mb-3";
        if (erro instanceof TypeError) {
            mensagem.textContent = "Não foi possível conectar ao servidor."
        } else {
            mensagem.textContent = erro.message;
        }
    } finally {
        botaoFiltrar.disabled = false;
        botaoAtualizar.disabled = false;
    }
}

formulario.addEventListener("submit", function (event) {
    event.preventDefault();
    carregarDashboard();
});

botaoAtualizar.addEventListener("click", carregarDashboard);
carregarDashboard();