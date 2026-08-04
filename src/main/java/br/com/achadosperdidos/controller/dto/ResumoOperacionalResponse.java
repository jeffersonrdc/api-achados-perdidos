package br.com.achadosperdidos.controller.dto;

/** Contadores operacionais por evento (Telas 2/4/5 da especificacao). */
public record ResumoOperacionalResponse(
        String idEvento,
        String nmEvento,
        long total,
        long encontrados,
        long coletados,
        long aguardandoTriagem,
        long emTriagem,
        long emTransporteEstoque,
        long emEstoque,
        long comPedidoDevolucao,
        long aguardandoRetirada,
        long devolvidos,
        long finalizados,
        long descartados,
        long devolvidosHoje,
        long sensiveis,
        /** Downloads de wallpaper no portal público (card "Wallpapers Baixados"). */
        long wallpapersBaixados
) {}
