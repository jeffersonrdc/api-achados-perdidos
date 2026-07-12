package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.DashboardEventoResponse;
import br.com.achadosperdidos.controller.dto.DashboardSlaPendenteResponse;
import br.com.achadosperdidos.controller.dto.DashboardSlaResumoResponse;
import br.com.achadosperdidos.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }
    @GetMapping("/eventos") @PreAuthorize("@authz.pode('dashboard.visualizar')")
    public List<DashboardEventoResponse> resumoEventos() { return dashboardService.listarResumoEventos(); }

    @GetMapping("/sla/pendentes") @PreAuthorize("@authz.pode('dashboard.visualizar')")
    public List<DashboardSlaPendenteResponse> slaPendentes() { return dashboardService.listarSlaPendentes(); }

    @GetMapping("/sla/resumo") @PreAuthorize("@authz.pode('dashboard.visualizar')")
    public List<DashboardSlaResumoResponse> slaResumo() { return dashboardService.listarSlaResumo(); }
}
