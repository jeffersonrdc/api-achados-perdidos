package br.com.achadosperdidos.controller.dto;

public record UsuarioUpdateRequest(String nmUsuario, String nmEmail, String nmPerfil, Boolean fgAtivo) {}
