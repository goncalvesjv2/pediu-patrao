package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {
    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ProdutoService produtoService;

    private Produto criarProduto(String id, String nome) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setTipo("Pizza");
        produto.setPreco(45.0);
        produto.setAtivo(true);

        return produto;
    }

    @Test
    void deveCadastrarNovoProduto() {
        Produto produto = criarProduto(null, "Pizza Calabresa");
        when(produtoRepository.save(produto)).thenReturn(produto);
        Produto resultado = produtoService.novoProduto(produto);
        assertSame(produto, resultado);
        assertEquals("Pizza Calabresa", resultado.getNome());
        verify(produtoRepository).save(produto);
    }

    @Test
    void deveInformarFalhaAoCadastrarProduto() {
        Produto produto = criarProduto(null, "Pizza Calabresa");
        IllegalStateException falha = new IllegalStateException("Falha ao salvar produto");
        when(produtoRepository.save(produto)).thenThrow(falha);
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> produtoService.novoProduto(produto));
        assertSame(falha, erro);
    }

    @Test
    void deveListarProdutosCadastrados() {
        Produto pizza = criarProduto("1", "Pizza Calabresa");
        Produto mussarela = criarProduto("2", "Pizza Mussarela");
        when(produtoRepository.findAll()).thenReturn(List.of(pizza, mussarela));
        List<Produto> resultado = produtoService.listarProdutos();
        assertEquals(2, resultado.size());
        assertEquals(List.of(pizza, mussarela), resultado);
        verify(produtoRepository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaProdutos() {
        when(produtoRepository.findAll()).thenReturn(List.of());
        List<Produto> resultado = produtoService.listarProdutos();
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveExcluirProdutoPorId() {
        produtoService.excluir("1");
        verify(produtoRepository).deleteById("1");
    }

    @Test
    void deveInformarFalhaAoExcluirProduto() {
        IllegalStateException falha = new IllegalStateException("Falha ao excluir produto");
        doThrow(falha).when(produtoRepository).deleteById("1");
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> produtoService.excluir("1"));
        assertSame(falha, erro);
    }

    @Test
    void deveSalvarProdutoSemId() {
        Produto produto = criarProduto(null, "Pizza Calabresa");
        when(produtoRepository.save(produto)).thenReturn(produto);
        Produto resultado = produtoService.salvar(produto);
        assertSame(produto, resultado);
        verify(produtoRepository).save(produto);
        verify(produtoRepository, never()).existsById(anyString());
    }

    @Test
    void deveAtualizarProdutoExistente() {
        Produto produto = criarProduto("1", "Pizza Calabresa");
        produto.setPreco(50.0);

        when(produtoRepository.existsById("1")).thenReturn(true);
        when(produtoRepository.save(produto)).thenReturn(produto);

        Produto resultado = produtoService.salvar(produto);

        assertEquals("Pizza Calabresa", resultado.getNome());
        assertEquals(50.0, resultado.getPreco());

        verify(produtoRepository).existsById("1");
        verify(produtoRepository).save(produto);
    }

    @Test
    void deveImpedirAtualizacaoDeProdutoInexistente() {
        Produto produto = criarProduto("99", "Pizza Calabresa");

        when(produtoRepository.existsById("99")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> produtoService.salvar(produto));

        verify(produtoRepository).existsById("99");
        verify(produtoRepository, never()).save(any(Produto.class));
    }

    @Test
    void deveInformarFalhaAoSalvarProdutoNovo() {
        Produto produto = criarProduto(null, "Pizza Calabresa");

        IllegalStateException falha = new IllegalStateException("Falha ao salvar produto");

        when(produtoRepository.save(produto)).thenThrow(falha);
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> produtoService.salvar(produto));

        assertSame(falha, erro);
        verify(produtoRepository, never()).existsById(anyString());
    }

    @Test
    void deveInformarFalhaAoAtualizarProduto() {
        Produto produto = criarProduto("1", "Pizza Calabresa");

        IllegalStateException falha = new IllegalStateException("Falha ao atualizar produto");

        when(produtoRepository.existsById("1")).thenReturn(true);
        when(produtoRepository.save(produto)).thenThrow(falha);

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> produtoService.salvar(produto));

        assertSame(falha, erro);
        verify(produtoRepository).existsById("1");
        verify(produtoRepository).save(produto);
    }
}
