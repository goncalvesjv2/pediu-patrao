package com.umc.pediupatrao.entity;

import jakarta.persistence.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "pedidos")
@Entity
public class Pedido {
    @Id
    private String id;
    private String clienteId;
    private List<ItemPedido> itens;
    private double valorTotal;
    private String responsavelId;
    private LocalDateTime horarioEntrada;
    private StatusPedido status;
    private double percentualDesconto = 0.0;
    private double valorDesconto = 0.0;
    private String responsavelSaidaId;
    private LocalDateTime horarioSaida;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemPedido> getItens() { return itens; }

    public void setItens(List<ItemPedido> itens) { this.itens = itens; }

    public double getValorTotal() { return valorTotal; }

    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public String getResponsavelId() { return responsavelId; }

    public void setResponsavelId(String responsavelId) { this.responsavelId = responsavelId; }

    public LocalDateTime getHorarioEntrada() { return horarioEntrada; }

    public void setHorarioEntrada(LocalDateTime horarioEntrada) { this.horarioEntrada = horarioEntrada; }

    public StatusPedido getStatus() { return status; }

    public void setStatus(StatusPedido status) { this.status = status; }

    public double getPercentualDesconto() {
        return percentualDesconto;
    }

    public void setPercentualDesconto(double percentualDesconto) {
        this.percentualDesconto = percentualDesconto;
    }

    public double getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(double valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public String getResponsavelSaidaId() { return responsavelSaidaId; }

    public void setResponsavelSaidaId(String responsavelSaidaId) { this.responsavelSaidaId = responsavelSaidaId; }

    public LocalDateTime getHorarioSaida() { return horarioSaida; }

    public void setHorarioSaida(LocalDateTime horarioSaida) { this.horarioSaida = horarioSaida; }
}