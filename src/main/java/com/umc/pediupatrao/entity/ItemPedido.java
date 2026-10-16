package com.umc.pediupatrao.entity;

public class ItemPedido {
    private String produtoId;
    private String nomeProduto;
    private int quantidade;
    private double precoUnitario;

    public ItemPedido() {}

    public ItemPedido(String produtoId, String nomeProduto, int quantidade, double precoUnitario) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }

    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(double precoUnitario) { this.precoUnitario = precoUnitario; }

    public double getSubtotal() {
        return precoUnitario * quantidade;
    }
}
