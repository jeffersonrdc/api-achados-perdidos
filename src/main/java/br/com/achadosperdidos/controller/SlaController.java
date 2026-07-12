package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.SlaRegistroResponse;
import br.com.achadosperdidos.controller.dto.SlaRegraCreateRequest;
import br.com.achadosperdidos.controller.dto.SlaRegraResponse;
import br.com.achadosperdidos.service.SlaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sla")
@Tag(name = "SLA")
@SecurityRequirement(name = "bearerAuth")
public class SlaController {
    private final SlaService slaService;

    public SlaController(SlaService slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/pendentes")
    @PreAuthorize("@authz.pode('sla.listar')")
    public List<SlaRegistroResponse> listarPendentes() {
        return slaService.listarPendentes();
    }

    @PostMapping("/regras")
    @PreAuthorize("@authz.pode('sla.gerenciar')")
    public ResponseEntity<SlaRegraResponse> createRegra(@Valid @RequestBody SlaRegraCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slaService.createRegra(request));
    }

    @GetMapping("/regras")
    @PreAuthorize("@authz.pode('sla.listar')")
    public List<SlaRegraResponse> listarRegras(@RequestParam(required = false) String idEvento) {
        return slaService.listarRegras(idEvento);
    }
}
