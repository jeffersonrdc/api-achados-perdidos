package br.com.achadosperdidos.controller.dto;

import java.util.List;

public record AuditoriaFiltrosResponse(
        List<Opcao> modulos,
        List<UsuarioOpcao> usuarios,
        Totais totais
) {
    public record Opcao(String valor, String label) {}
    public record UsuarioOpcao(String id, String nome, String login) {}
    public record Totais(long total, long criacoes, long alteracoes, long exclusoes) {}
}
