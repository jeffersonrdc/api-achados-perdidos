package br.com.achadosperdidos.controller.dto;

public record EventoConfiguracaoUpdateRequest(
        Boolean fgRecebeObjetos,
        Boolean fgAceitaClaim,
        Boolean fgConsultaPublica,
        Boolean fgFotoObrigatoria,
        Boolean fgValidacaoObrigatoria,
        Integer qtMaxFotos,
        Integer qtDiasDescarte,
        Integer qtDiasEsperaAceitavel,
        Integer qtWallpapersDisponiveis
) {}
