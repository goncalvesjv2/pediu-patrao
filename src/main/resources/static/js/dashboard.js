const dashboard = document.getElementById("dashboard");

const formulario = document.getElementById("filtroDashboard");
const botaoFiltrar = document.getElementById("botaoFiltrar");
const botaoAtualizar = document.getElementById("botaoAtualizar");
const mensagem = document.getElementById("mensagemDashboard");

const formatadorMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL"});

function atualizarIndicadores(dados) {
    document.getElementById("totalPedidos").textContent = dados.totalPedidos;
    document.getElementById("pedidosFinalizados").textContent = dados.pedidosFinalizados;
    document.getElementById("pedidosCancelados").textContent = dados.pedidosCancelados;
    document.getElementById("faturamento").textContent = formatadorMoeda.format(dados.faturamento);
    document.getElementById("ticketMedio").textContent = formatadorMoeda.format(dados.ticketMedio);
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
        mensagem.className = "d-none";
        mensagem.textContent = "";

    } catch (erro) {
        mensagem.className = "alert alert-danger mb-3";
        mensagem.textContent = erro.message;
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