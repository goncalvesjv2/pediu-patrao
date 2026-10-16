package com.umc.pediupatrao.dto;
import java.util.List;

public class PedidoRequestDTO {
    private String clienteId;
    private List<ItemPedidoDTO> itens;

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public List<ItemPedidoDTO> getItens() { return itens; }
    public void setItens(List<ItemPedidoDTO> itens) { this.itens = itens; }
}
