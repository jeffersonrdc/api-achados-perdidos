package br.com.achadosperdidos.controller.dto;

import java.time.LocalDateTime;

public record ContatoResponse(
        String id,
        String idItem,
        String idClaim,
        String tpContato,
        String nmContato,
        String nrTelefone,
        String nmEmail,
        String dsResumo,
        LocalDateTime dtContato
) {}
