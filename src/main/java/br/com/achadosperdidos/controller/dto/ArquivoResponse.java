package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

/**
 * Metadados do arquivo. {@code nmPath} é a chave opaca de storage (não é URL/bucket).
 * {@code tpStorage} indica o provedor físico (LOCAL|S3) deste registro.
 */
public record ArquivoResponse(
        String id,
        String idEvento,
        String tpEntidade,
        String idEntidade,
        String tpArquivo,
        String nmArquivo,
        String nmPath,
        String tpStorage,
        String tpMime,
        Boolean fgPrincipal,
        Long qtBytes,
        LocalDateTime dtCadastro
) {}
