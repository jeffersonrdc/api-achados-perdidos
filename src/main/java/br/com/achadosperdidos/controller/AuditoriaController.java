package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.AuditoriaResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.AuditoriaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoria")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authz.pode('auditoria.consultar')")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ApiPage<AuditoriaResponse> findAll(@RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer limit) {
        return auditoriaService.findAll(page, limit);
    }

    @GetMapping("/registro")
    public ApiPage<AuditoriaResponse> findByRegistro(@RequestParam String nmTabela,
                                                     @RequestParam Long idRegistro,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer limit) {
        return auditoriaService.findByRegistro(nmTabela, idRegistro, page, limit);
    }
}
