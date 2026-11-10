package com.umc.pediupatrao.dto;

public record DashboardDTO(
    long totalPedidos,
    long pedidosFinalizados,
    long pedidosCancelados,
    double faturamento,
    double ticketMedio
){}
