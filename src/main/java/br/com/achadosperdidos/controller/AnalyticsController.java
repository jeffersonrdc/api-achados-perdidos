package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.EvolucaoPontoResponse;
import br.com.achadosperdidos.controller.dto.ResumoOperacionalResponse;
import br.com.achadosperdidos.service.AnalyticsService;

import java.util.List;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authz.pode('analytics.visualizar')")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/eventos/{idEvento}/resumo")
    public ResumoOperacionalResponse resumoOperacional(@PathVariable String idEvento) {
        return analyticsService.resumoOperacional(idEvento);
    }

    @GetMapping("/eventos/{idEvento}/evolucao")
    public List<EvolucaoPontoResponse> evolucao(@PathVariable String idEvento,
                                                @RequestParam(defaultValue = "14") int dias) {
        return analyticsService.evolucao(idEvento, dias);
    }
}
