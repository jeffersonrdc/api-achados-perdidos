package br.com.achadosperdidos.controller.dto;

import java.time.LocalDate;

public record PortalItemCatalogoResponse(
        String id,
        String nmTitulo,
        String nmCategoria,
        String nmMarca,
        String nmModelo,
        String nmCor,
        LocalDate dtEncontrado,
        String nmLocalEncontrado,
        /** ID assinado da foto principal — baixar em GET /portal/arquivos/{id}/download */
        String idFotoPrincipal
) {}
