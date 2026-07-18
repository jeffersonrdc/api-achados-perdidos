package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EvolucaoPontoResponse;
import br.com.achadosperdidos.controller.dto.ResumoOperacionalResponse;
import br.com.achadosperdidos.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Indicadores operacionais por evento. Permissão: `analytics.visualizar`.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authz.pode('analytics.visualizar')")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/eventos/{idEvento}/painel")
    @Operation(summary = "Painel completo de analytics do evento",
            description = "Substitui os mocks das telas Angular /analytics. "
                    + "`dias`: 7|14|30|0 (0 = evento completo). KPIs com comparação ao período anterior, "
                    + "taxas, tempos, incidência, horários, SLA, gargalos e previsão por média móvel.")
    public Map<String, Object> painel(
            @Parameter(description = "ID assinado do evento (`s2.*`)", required = true)
            @PathVariable String idEvento,
            @Parameter(description = "Janela em dias (0 = evento completo)")
            @RequestParam(defaultValue = "14") int dias) {
        return analyticsService.painel(idEvento, dias);
    }

    @GetMapping("/eventos/{idEvento}/resumo")
    @Operation(summary = "Resumo operacional do evento",
            description = "Totais de itens, claims, devoluções e indicadores do evento. "
                    + "`idEvento` deve ser o token assinado retornado pela API (não o ID numérico).")
    public ResumoOperacionalResponse resumoOperacional(
            @Parameter(description = "ID assinado do evento (`s2.*`)", required = true)
            @PathVariable String idEvento) {
        return analyticsService.resumoOperacional(idEvento);
    }

    @GetMapping("/eventos/{idEvento}/evolucao")
    @Operation(summary = "Evolução temporal de indicadores",
            description = "Série diária de métricas do evento para o número de dias informado.")
    public List<EvolucaoPontoResponse> evolucao(
            @Parameter(description = "ID assinado do evento (`s2.*`)", required = true)
            @PathVariable String idEvento,
            @Parameter(description = "Quantidade de dias da série (default 14)")
            @RequestParam(defaultValue = "14") int dias) {
        return analyticsService.evolucao(idEvento, dias);
    }
}
