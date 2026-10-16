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
    private String status;

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

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}
