package br.com.achadosperdidos.controller;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.pagination.ApiPage;
import br.com.achadosperdidos.service.DevolucaoFluxoService;
import br.com.achadosperdidos.service.DevolucaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devolucoes")
@Tag(name = "Devoluções", description = "Registro e acompanhamento de devoluções.")
@SecurityRequirement(name = "bearerAuth")
public class DevolucaoController {
    private final DevolucaoService devolucaoService;
    private final DevolucaoFluxoService devolucaoFluxoService;

    public DevolucaoController(DevolucaoService devolucaoService, DevolucaoFluxoService devolucaoFluxoService) {
        this.devolucaoService = devolucaoService;
        this.devolucaoFluxoService = devolucaoFluxoService;
    }

    @PostMapping
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Registrar devolução")
    public ResponseEntity<DevolucaoResponse> create(@Valid @RequestBody DevolucaoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucaoService.create(request));
    }

    @GetMapping
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Listar devoluções (paginado)",
            description = "Filtra opcionalmente por evento (`idEvento` assinado), busca livre (`q`), local, status, prioridade e data da devolução.")
    public ApiPage<DevolucaoResponse> findAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "ID assinado do evento") @RequestParam(required = false) String idEvento,
            @Parameter(description = "Busca livre por recebedor, item, protocolo ou local") @RequestParam(required = false) String q,
            @RequestParam(required = false) String local,
            @Parameter(description = "Status (código ou rótulo PT)") @RequestParam(required = false) String status,
            @Parameter(description = "Prioridade do item (ALTA/MEDIA/BAIXA)") @RequestParam(required = false) String tpPrioridade,
            @Parameter(description = "Data da devolução (yyyy-MM-dd ou dd/MM/yyyy)") @RequestParam(required = false) String data) {
        return devolucaoService.findAll(page, limit, idEvento, q, local, status, tpPrioridade, data);
    }

    @GetMapping("/resumo")
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Resumo/cards das devoluções do evento")
    public DevolucaoResumoResponse resumo(
            @Parameter(description = "ID assinado do evento") @RequestParam String idEvento,
            @Parameter(description = "Data da devolução (yyyy-MM-dd ou dd/MM/yyyy); sem valor = totais do evento")
            @RequestParam(required = false) String data) {
        return devolucaoService.resumo(idEvento, data);
    }

    @GetMapping("/filtros")
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Opções de filtro das devoluções do evento",
            description = "Status fixos + locais e prioridades presentes nas devoluções do evento.")
    public ColetaFiltrosResponse filtros(
            @Parameter(description = "ID assinado do evento") @RequestParam String idEvento) {
        return devolucaoService.filtros(idEvento);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Detalhe completo do ticket de devolução")
    public DevolucaoDetalheResponse detalhar(@PathVariable String id) {
        return devolucaoFluxoService.detalhar(id);
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Histórico (timeline) da devolução")
    public List<DevolucaoHistoricoItemResponse> historico(@PathVariable String id) {
        return devolucaoFluxoService.historico(id);
    }

    @PostMapping("/{id}/pickup/options")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Cadastrar opções de agenda (PICKUP)")
    public DevolucaoDetalheResponse cadastrarPickupOptions(
            @PathVariable String id, @Valid @RequestBody DevolucaoPickupOptionsRequest request) {
        return devolucaoFluxoService.cadastrarPickupOptions(id, request);
    }

    @PostMapping("/{id}/pickup/options/send")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Enviar e-mail com opções de agenda")
    public DevolucaoDetalheResponse enviarPickupOptions(@PathVariable String id) {
        return devolucaoFluxoService.enviarPickupOptions(id);
    }

    @PostMapping("/{id}/shipping/quote")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Registrar cotação de frete (SHIPPING)")
    public DevolucaoDetalheResponse registrarCotacao(
            @PathVariable String id, @Valid @RequestBody DevolucaoShippingQuoteRequest request) {
        return devolucaoFluxoService.registrarCotacao(id, request);
    }

    @PostMapping("/{id}/shipping/quote/send")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Enviar e-mail da cotação de frete")
    public DevolucaoDetalheResponse enviarCotacao(@PathVariable String id) {
        return devolucaoFluxoService.enviarCotacao(id);
    }

    @PostMapping("/{id}/shipping/posting")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Registrar postagem/rastreio (somente após pagamento)")
    public DevolucaoDetalheResponse registrarPostagem(
            @PathVariable String id, @Valid @RequestBody DevolucaoShippingPostingRequest request) {
        return devolucaoFluxoService.registrarPostagem(id, request);
    }

    @PostMapping("/{id}/shipping/posting/send")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Enviar e-mail de postagem/rastreio")
    public DevolucaoDetalheResponse enviarPostagem(@PathVariable String id) {
        return devolucaoFluxoService.enviarPostagem(id);
    }

    @PostMapping(value = "/{id}/termo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Upload do PDF do termo de devolução")
    public DevolucaoDetalheResponse uploadTermo(
            @PathVariable String id, @RequestParam("file") MultipartFile file) {
        return devolucaoFluxoService.uploadTermo(id, file);
    }

    @PostMapping("/{id}/concluir-presencial")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Concluir devolução presencial (sem assinatura)")
    public DevolucaoResponse concluirPresencial(
            @PathVariable String id, @RequestBody(required = false) DevolucaoConcluirPresencialRequest request) {
        return devolucaoFluxoService.concluirPresencial(id, request != null ? request : new DevolucaoConcluirPresencialRequest(null));
    }

    @PostMapping("/{id}/emails/resend")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Reenviar e-mail do fluxo de devolução")
    public Map<String, Object> reenviarEmail(
            @PathVariable String id, @RequestBody(required = false) DevolucaoEmailResendRequest request) {
        return devolucaoFluxoService.reenviarEmail(id, request != null ? request : new DevolucaoEmailResendRequest(null));
    }

    @PostMapping("/{id}/marcar-lidas")
    @PreAuthorize("@authz.pode('devolucao.listar')")
    @Operation(summary = "Marcar atualizações do solicitante como lidas (badge)")
    public Map<String, Object> marcarLidas(@PathVariable String id) {
        devolucaoFluxoService.marcarAtualizacoesLidas(id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/conferencia")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Persistir checkboxes de conferência presencial")
    public DevolucaoDetalheResponse salvarConferencia(
            @PathVariable String id, @Valid @RequestBody DevolucaoConferenciaRequest request) {
        return devolucaoFluxoService.salvarConferencia(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@authz.pode('devolucao.realizar')")
    @Operation(summary = "Atualizar status da devolução")
    public DevolucaoResponse atualizarStatus(
            @Parameter(description = "ID assinado da devolução") @PathVariable String id,
            @Valid @RequestBody DevolucaoStatusRequest request) {
        return devolucaoFluxoService.atualizarStatus(id, request);
    }
}
