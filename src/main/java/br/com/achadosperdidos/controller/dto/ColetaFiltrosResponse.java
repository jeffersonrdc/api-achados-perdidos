package br.com.achadosperdidos.controller.dto;

import java.util.List;

/** Opções para os filtros e selects da tela de Coleta de Itens (/itens). */
public record ColetaFiltrosResponse(
        List<CategoriaArvore> categorias,
        List<Opcao> status,
        List<String> locais,
        List<String> prioridades
) {
    public record CategoriaArvore(String id, String nome, List<Opcao> subcategorias) {}
    public record Opcao(String id, String nome) {}
}
