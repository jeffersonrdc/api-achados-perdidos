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

    @GetMapping("/painel/{tipo}")
    @Operation(summary = "Painel completo de relatório por tipo",
            description = "Substitui os mocks das telas Angular /relatorios. "
                    + "`tipo`: encontrados|devolvidos|estoque|pedidos|transferencias|produtividade|auditoria|lgpd. "
                    + "`dias`: 7|14|30|0 (0 = evento completo). `idEvento` assinado.")
    public Map<String, Object> painel(
            @Parameter(description = "Tipo do relatório", required = true) @PathVariable String tipo,
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "Janela em dias (0 = evento completo)") @RequestParam(defaultValue = "14") int dias) {
        return relatorioService.painel(tipo, idEvento, dias);
    }

    @GetMapping("/itens-por-categoria")
    @Operation(summary = "Itens agrupados por categoria")
    public List<Map<String, Object>> itensPorCategoria(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "Filtra pelo dia de cadastro (yyyy-MM-dd ou dd/MM/yyyy)")
            @RequestParam(required = false) String data) {
        return relatorioService.itensPorCategoria(idEvento, data);
    }

    @GetMapping("/itens-pendentes")
    @Operation(summary = "Itens pendentes de devolução")
    public List<Map<String, Object>> itensPendentes(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "Filtra pelo dia de cadastro (yyyy-MM-dd ou dd/MM/yyyy)")
            @RequestParam(required = false) String data) {
        return relatorioService.itensPendentes(idEvento, data);
    }

    @GetMapping("/itens-devolvidos")
    @Operation(summary = "Itens devolvidos")
    public List<Map<String, Object>> itensDevolvidos(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.itensDevolvidos(idEvento);
    }

    @GetMapping("/itens-por-localizacao")
    @Operation(summary = "Itens agrupados por localização")
    public List<Map<String, Object>> itensPorLocalizacao(
            @Parameter(description = "ID assinado do evento") @RequestParam(required = false) String idEvento,
            @Parameter(description = "Filtra pelo dia de cadastro (yyyy-MM-dd ou dd/MM/yyyy)")
            @RequestParam(required = false) String data) {
        return relatorioService.itensPorLocalizacao(idEvento, data);
    }

    @GetMapping("/tempo-devolucao")
    @Operation(summary = "Tempo médio de devolução")
    public List<Map<String, Object>> tempoDevolucao(@Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento) {
        return relatorioService.tempoDevolucao(idEvento);
    }

    @GetMapping("/claims-abertos")
    @Operation(summary = "Claims em aberto")
    public List<Map<String, Object>> claimsAbertos(
            @Parameter(description = "ID assinado do evento", required = true) @RequestParam String idEvento,
            @Parameter(description = "Filtra pelo dia de abertura (yyyy-MM-dd ou dd/MM/yyyy)")
            @RequestParam(required = false) String data) {
        return relatorioService.claimsAbertos(idEvento, data);
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
