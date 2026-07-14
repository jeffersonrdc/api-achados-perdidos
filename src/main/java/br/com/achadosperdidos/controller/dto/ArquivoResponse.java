package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record ArquivoResponse(String id, String idEvento, String tpEntidade, String idEntidade, String tpArquivo, String nmArquivo, String nmPath, String tpMime, Boolean fgPrincipal, Long qtBytes, LocalDateTime dtCadastro) {}
