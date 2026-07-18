package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.AuthEventResponse;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.AuthEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "Logs de Acesso",
        description = "Trilha de autenticação (login, bloqueio, refresh, logout). Aba Acessos da tela /logs. "
                + "Permissão: `logs.consultar`.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@authz.pode('logs.consultar') or @authz.pode('auditoria.consultar')")
public class LogsController {

    private final AuthEventService authEventService;

    public LogsController(AuthEventService authEventService) {
        this.authEventService = authEventService;
    }

    @GetMapping("/acessos")
    @Operation(summary = "Listar eventos de acesso (paginado)",
            description = """
                    Eventos: LOGIN_SUCESSO, LOGIN_CREDENCIAL_INVALIDA, LOGIN_RATE_LIMIT_IP,
                    LOGIN_RATE_LIMIT_CONTA, REFRESH_SUCESSO, REFRESH_INVALIDO, LOGOUT.
                    Identificador aparece mascarado; senha/JWT nunca são persistidos.
                    """)
    public ApiPage<AuthEventResponse> listarAcessos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String tpEvento,
            @RequestParam(required = false) String tpResultado,
            @Parameter(description = "ID assinado do usuário") @RequestParam(required = false) String idUsuario,
            @RequestParam(required = false) String nrIp,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return authEventService.listar(page, limit, tpEvento, tpResultado, idUsuario, nrIp, dataInicio, dataFim);
    }
}
