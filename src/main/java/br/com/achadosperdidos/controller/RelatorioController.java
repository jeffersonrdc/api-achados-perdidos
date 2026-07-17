package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios", description = "Relatórios operacionais por evento.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authz.pode('relatorio.visualizar')")
public class RelatorioController {
    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/itens-por-categoria")
    @Operation(summary = "Itens agrupados por categoria")
    public List<Map<String, Object>> itensPorCategoria(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.itensPorCategoria(idEvento);
    }

    @GetMapping("/itens-pendentes")
    @Operation(summary = "Itens pendentes de devolução")
    public List<Map<String, Object>> itensPendentes(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.itensPendentes(idEvento);
    }

    @GetMapping("/itens-devolvidos")
    @Operation(summary = "Itens devolvidos")
    public List<Map<String, Object>> itensDevolvidos(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.itensDevolvidos(idEvento);
    }

    @GetMapping("/itens-por-localizacao")
    @Operation(summary = "Itens agrupados por localização")
    public List<Map<String, Object>> itensPorLocalizacao() {
        return relatorioService.itensPorLocalizacao();
    }

    @GetMapping("/tempo-devolucao")
    @Operation(summary = "Tempo médio de devolução")
    public List<Map<String, Object>> tempoDevolucao(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.tempoDevolucao(idEvento);
    }

    @GetMapping("/claims-abertos")
    @Operation(summary = "Claims em aberto")
    public List<Map<String, Object>> claimsAbertos(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.claimsAbertos(idEvento);
    }

    @GetMapping("/sla-estourado")
    @Operation(summary = "Itens com SLA estourado")
    public List<Map<String, Object>> slaEstourado(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.slaEstourado(idEvento);
    }

    @GetMapping("/auditoria")
    @Operation(summary = "Relatório de auditoria do evento")
    public List<Map<String, Object>> auditoria(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.auditoria(idEvento);
    }
}
