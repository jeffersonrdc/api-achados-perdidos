package br.com.achadosperdidos.controller.dto;

import java.util.List;

/** Define o conjunto de permissoes (nomes modulo.acao) de um perfil ou extras de um usuario. */
public record PermissoesRequest(List<String> permissoes) {}
