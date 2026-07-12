package br.com.achadosperdidos.controller.dto;

import java.util.List;

public record PerfilDetalheResponse(String id, String nmPerfil, String dsPerfil, Boolean fgAtivo,
                                    List<PermissaoResponse> permissoes) {}
