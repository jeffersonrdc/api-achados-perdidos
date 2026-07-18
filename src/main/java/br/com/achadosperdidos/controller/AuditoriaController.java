package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.AuditoriaResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auditoria")
@Tag(name = "Auditoria", description = "Consulta de trilhas de alteração operacional (A09). Aba Operações da tela /logs.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authz.pode('auditoria.consultar')")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @Operation(summary = "Listar registros de auditoria (paginado)",
            description = "Filtros opcionais por tabela, ação, usuário, IP e período. Retorna nmUsuario e nrIp quando a escrita publicou o contexto.")
    public ApiPage<AuditoriaResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String nmTabela,
            @RequestParam(required = false) String tpAcao,
            @Parameter(description = "ID assinado do usuário") @RequestParam(required = false) String idUsuario,
            @RequestParam(required = false) String nrIp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return auditoriaService.findAll(page, limit, nmTabela, tpAcao, idUsuario, nrIp, dataInicio, dataFim);
    }

    @GetMapping("/registro")
    @Operation(summary = "Listar auditoria por registro", description = "Filtra por tabela e ID numérico interno do registro.")
    public ApiPage<AuditoriaResponse> findByRegistro(@RequestParam String nmTabela,
                                                     @RequestParam Long idRegistro,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer limit) {
        return auditoriaService.findByRegistro(nmTabela, idRegistro, page, limit);
    }
}
