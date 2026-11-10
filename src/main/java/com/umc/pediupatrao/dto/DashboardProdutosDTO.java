package com.umc.pediupatrao.dto;

public record DashboardProdutosDTO(
        String produtoId,
        String nomeProduto,
        long quantidadeVendida
) {}