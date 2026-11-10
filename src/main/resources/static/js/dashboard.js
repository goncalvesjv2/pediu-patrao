const dashboard = document.getElementById("dashboard");

const formulario = document.getElementById("filtroDashboard");
const botaoFiltrar = document.getElementById("botaoFiltrar");
const botaoAtualizar = document.getElementById("botaoAtualizar");
const mensagem = document.getElementById("mensagemDashboard");
const mensagemGraficos = document.getElementById("mensagemGraficos");
let graficoFaturamento = null;
let graficoPedidos = null;
const mensagemStatus = document.getElementById("mensagemStatus");
let graficoStatus = null;
const mensagemProdutos = document.getElementById("mensagemProdutos");
const tabelaProdutos = document.getElementById("tabelaProdutos");

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
            document.getElementById("graficoFaturamento"), {
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
            document.getElementById("graficoPedidos"), {
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

async function carregarSituacaoPedidos(inicio, fim) {
    const url = new URL(
        `${dashboard.dataset.apiUrl}/status`,
        window.location.origin
    );

    if (inicio) {
        url.searchParams.set("inicio", inicio);
    }

    if (fim) {
        url.searchParams.set("fim", fim);
    }

    mensagemStatus.className = "d-none";
    mensagemStatus.textContent = "";

    try {
        const resposta = await fetch(url, {
            credentials: "same-origin",
            headers: {
                "Accept": "application/json"
            },
            cache: "no-store"
        });

        const tipoResposta =
            resposta.headers.get("content-type") || "";

        if (!tipoResposta.includes("application/json")) {
            throw new Error(
                "Não foi possível obter os dados. Verifique sua sessão."
            );
        }

        const dados = await resposta.json();

        if (!resposta.ok) {
            throw new Error(
                dados.erro || "Erro ao consultar a situação dos pedidos."
            );
        }

        if (!Array.isArray(dados)) {
            throw new Error("Formato de dados inválido.");
        }

        if (typeof Chart === "undefined") {
            throw new Error("A biblioteca Chart.js não foi carregada.");
        }

        const canvas = document.getElementById("graficoStatus");

        if (!canvas) {
            throw new Error("O elemento do gráfico não foi encontrado.");
        }

        const nomesStatus = {
            RECEBIDO: "Recebido",
            EM_PREPARO: "Em preparo",
            PRONTO: "Pronto",
            SAIU_PARA_ENTREGA: "Saiu para entrega",
            FINALIZADO: "Finalizado",
            CANCELADO: "Cancelado"
        };

        const coresStatus = {
            RECEBIDO: "#0d6efd",
            EM_PREPARO: "#ffc107",
            PRONTO: "#17a2b8",
            SAIU_PARA_ENTREGA: "#6f42c1",
            FINALIZADO: "#198754",
            CANCELADO: "#dc3545"
        };

        const nomes = dados.map(item =>
            nomesStatus[item.status] || item.status
        );

        const quantidades = dados.map(item =>
            item.quantidade
        );

        const cores = dados.map(item =>
            coresStatus[item.status] || "#6c757d"
        );

        if (graficoStatus !== null) {
            graficoStatus.destroy();
            graficoStatus = null;
        }

        graficoStatus = new Chart(canvas, {
            type: "doughnut",
            data: {
                labels: nomes,
                datasets: [{
                    label: "Quantidade de pedidos",
                    data: quantidades,
                    backgroundColor: cores,
                    borderColor: "#ffffff",
                    borderWidth: 2
                }]
            },

            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: "60%",
                plugins: {
                    legend: {
                        display: true,
                        position: "bottom"
                    },

                    tooltip: {
                        callbacks: {
                            label: contexto => {
                                return `${contexto.label}: ${contexto.parsed} pedido(s)`;
                            }
                        }
                    }
                }
            }
        });

        const semPedidos = dados.every(
            item => item.quantidade === 0
        );

        if (semPedidos) {
            mensagemStatus.className = "alert alert-info mb-3";
            mensagemStatus.textContent = "Nenhum pedido encontrado no período.";

        } else {
            mensagemStatus.className = "d-none";
            mensagemStatus.textContent = "";
        }

    } catch (erro) {
        if (graficoStatus !== null) {
            graficoStatus.destroy();
            graficoStatus = null;
        }

        mensagemStatus.className = "alert alert-danger mb-3";

        if (erro instanceof TypeError) {
            mensagemStatus.textContent = "Não foi possível conectar ao servidor.";
        } else {
            mensagemStatus.textContent = erro.message;
        }
    }
}

async function carregarProdutosMaisVendidos(inicio, fim) {
    const url = new URL(
        `${dashboard.dataset.apiUrl}/produtos`,
        window.location.origin
    );

    if (inicio) {
        url.searchParams.set("inicio", inicio);
    }

    if (fim) {
        url.searchParams.set("fim", fim);
    }

    tabelaProdutos.replaceChildren();

    mensagemProdutos.className = "alert alert-info mb-3";
    mensagemProdutos.textContent = "Carregando produtos...";

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

        const produtos = await resposta.json();

        if (!resposta.ok) {
            throw new Error(produtos.erro || "Erro ao consultar os produtos mais vendidos.");
        }

        if (!Array.isArray(produtos)) {
            throw new Error("Formato de dados inválido.");
        }

        mensagemProdutos.className = "d-none";
        mensagemProdutos.textContent = "";

        if (produtos.length === 0) {
            const linha = document.createElement("tr");
            const coluna = document.createElement("td");
            coluna.colSpan = 3;
            coluna.className = "text-center text-muted py-4";
            coluna.textContent = "Nenhum produto vendido no período selecionado.";
            linha.appendChild(coluna);
            tabelaProdutos.appendChild(linha);
            return;
        }

        produtos.forEach((produto, indice) => {
            const linha = document.createElement("tr");

            const colunaPosicao = document.createElement("td");
            colunaPosicao.textContent = `${indice + 1}º`;

            const colunaNome = document.createElement("td");
            colunaNome.textContent = produto.nomeProduto;
            const colunaQuantidade = document.createElement("td");
            colunaQuantidade.textContent = Number(produto.quantidadeVendida).toLocaleString("pt-BR");

            linha.appendChild(colunaPosicao);
            linha.appendChild(colunaNome);
            linha.appendChild(colunaQuantidade);

            tabelaProdutos.appendChild(linha);
        });

    } catch (erro) {
        mensagemProdutos.className = "alert alert-danger mb-3";

        if (erro instanceof TypeError) {
            mensagemProdutos.textContent = "Não foi possível conectar ao servidor.";
        } else {
            mensagemProdutos.textContent = erro.message;
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
        await carregarSituacaoPedidos(inicio, fim);
        await carregarProdutosMaisVendidos(inicio, fim);

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