package com.umc.pediupatrao.dto;

import java.time.LocalDate;

public record DashboardVendasDTO(
        LocalDate data,
        long pedidosFinalizados,
        double faturamento
) {}