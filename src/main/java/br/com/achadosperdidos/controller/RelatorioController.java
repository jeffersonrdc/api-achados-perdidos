package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.service.RelatorioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authz.pode('relatorio.visualizar')")
public class RelatorioController {
    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/itens-por-categoria")
    public List<Map<String, Object>> itensPorCategoria(@RequestParam String idEvento) {
        return relatorioService.itensPorCategoria(idEvento);
    }

    @GetMapping("/itens-pendentes")
    public List<Map<String, Object>> itensPendentes(@RequestParam String idEvento) {
        return relatorioService.itensPendentes(idEvento);
    }

    @GetMapping("/itens-devolvidos")
    public List<Map<String, Object>> itensDevolvidos(@RequestParam String idEvento) {
        return relatorioService.itensDevolvidos(idEvento);
    }

    @GetMapping("/itens-por-localizacao")
    public List<Map<String, Object>> itensPorLocalizacao() {
        return relatorioService.itensPorLocalizacao();
    }

    @GetMapping("/tempo-devolucao")
    public List<Map<String, Object>> tempoDevolucao(@RequestParam String idEvento) {
        return relatorioService.tempoDevolucao(idEvento);
    }

    @GetMapping("/claims-abertos")
    public List<Map<String, Object>> claimsAbertos(@RequestParam String idEvento) {
        return relatorioService.claimsAbertos(idEvento);
    }

    @GetMapping("/sla-estourado")
    public List<Map<String, Object>> slaEstourado(@RequestParam String idEvento) {
        return relatorioService.slaEstourado(idEvento);
    }

    @GetMapping("/auditoria")
    public List<Map<String, Object>> auditoria(@RequestParam String idEvento) {
        return relatorioService.auditoria(idEvento);
    }
}
