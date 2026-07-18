package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ClaimMensagemResponse(
        String id,
        String tpAutor,
        String dsMensagem,
        String nmOperador,
        Boolean fgEmailEnviado,
        String dsEmailErro,
        LocalDateTime dtMensagem,
        List<ArquivoResponse> anexos
) {}
