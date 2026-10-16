// Alternar sidebar em telas pequenas
document.querySelector('.toggle-btn').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('active');
});

// Alternar submenu
function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}


document.addEventListener("DOMContentLoaded", () => {
    const produtoForm = document.getElementById("produtoForm");

    if (!produtoForm) return;

    produtoForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const produto = {
            nome: document.getElementById("nome").value.trim(),
            tipo: document.getElementById("tipo").value,
            preco: Number(document.getElementById("preco").value),
            ativo: document.getElementById("ativo").checked
        };

        const cookieCsrf = document.cookie
            .split("; ")
            .find(cookie => cookie.startsWith("XSRF-TOKEN="));

        const headers = {
            "Content-Type": "application/json"
        };

        if (cookieCsrf) {
            headers["X-XSRF-TOKEN"] = decodeURIComponent(
                cookieCsrf.substring("XSRF-TOKEN=".length)
            );
        }

        try {
            const response = await fetch("/api/produtos", {
                method: "POST",
                headers,
                body: JSON.stringify(produto)
            });

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            alert("Produto cadastrado com sucesso!");
            window.location.reload();
        } catch (error) {
            console.error("Erro ao cadastrar produto:", error);
            alert("Não foi possível cadastrar o produto. Verifique o console.");
        }
    });
});


document.addEventListener("click", async (event) => {
    const botao = event.target.closest(".btn-excluir");

    if (!botao) return;

    const id = botao.dataset.id;

    if (!confirm("Tem certeza que deseja excluir este produto?")) {
        return;
    }

    const cookieCsrf = document.cookie
        .split("; ")
        .find(cookie => cookie.startsWith("XSRF-TOKEN="));

    const headers = {};

    if (cookieCsrf) {
        headers["X-XSRF-TOKEN"] = decodeURIComponent(
            cookieCsrf.substring("XSRF-TOKEN=".length)
        );
    }

    try {
        const response = await fetch(`/api/produtos/${id}`, {
            method: "DELETE",
            headers
        });

        if (!response.ok) {
            throw new Error(`Erro HTTP: ${response.status}`);
        }

        alert("Produto excluído com sucesso!");
        window.location.reload();
    } catch (error) {
        console.error("Erro ao excluir produto:", error);
        alert("Não foi possível excluir o produto. Verifique o console.");
    }
});
