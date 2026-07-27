package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.ClaimHistorico;
import br.com.achadosperdidos.entity.ClaimValidacao;
import br.com.achadosperdidos.entity.Item;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ClaimHistoricoRepository;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ClaimValidacaoRepository;
import br.com.achadosperdidos.repository.ItemRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluxo do pedido de devolução (claim): iniciar análise, solicitar informações,
 * aprovar (gera devolução) e reprovar. Cada ação muda o status do claim, grava
 * o histórico (claim_historico) e dispara o e-mail correspondente.
 */
@Service
public class ClaimWorkflowService {
    private final ClaimRepository claimRepository;
    private final ClaimHistoricoRepository historicoRepository;
    private final ClaimValidacaoRepository validacaoRepository;
    private final ItemRepository itemRepository;
    private final ClaimService claimService;
    private final StatusItemService statusItemService;
    private final EmailService emailService;
    private final DevolucaoFluxoService devolucaoFluxoService;
    private final WorkflowService workflowService;
    private final UsuarioContextService usuarioContextService;
    private final SignedResourceIdCodec idCodec;
    private final ClaimMensagemService claimMensagemService;

    public ClaimWorkflowService(ClaimRepository claimRepository, ClaimHistoricoRepository historicoRepository,
                                ClaimValidacaoRepository validacaoRepository, ItemRepository itemRepository,
                                ClaimService claimService, StatusItemService statusItemService,
                                EmailService emailService, DevolucaoFluxoService devolucaoFluxoService,
                                WorkflowService workflowService, UsuarioContextService usuarioContextService,
                                SignedResourceIdCodec idCodec, ClaimMensagemService claimMensagemService) {
        this.claimRepository = claimRepository;
        this.historicoRepository = historicoRepository;
        this.validacaoRepository = validacaoRepository;
        this.itemRepository = itemRepository;
        this.claimService = claimService;
        this.statusItemService = statusItemService;
        this.emailService = emailService;
        this.devolucaoFluxoService = devolucaoFluxoService;
        this.workflowService = workflowService;
        this.usuarioContextService = usuarioContextService;
        this.idCodec = idCodec;
        this.claimMensagemService = claimMensagemService;
    }

    @Transactional
    public ClaimResponse iniciarAnalise(String idClaim) {
        Claim claim = findClaim(idClaim);
        claim.setStatus(statusItemService.findByNomeOrDefault(null, "Claim em Análise"));
        claim.setDtAlteracao(LocalDateTime.now());
        claimRepository.save(claim);
        var email = emailService.enviar("CLAIM_ANALISE", claim.getNmEmail(), variaveis(claim, null));
        gravarHistorico(claim, null, "ANALISE", null, "Análise iniciada", email);
        return response(claim);
    }

    @Transactional
    public ClaimResponse solicitarInfo(String idClaim, ClaimSolicitarInfoRequest req) {
        return claimMensagemService.solicitarInfoComConversa(idClaim, req.tpSolicitacao(), req.dsDetalhe());
    }

    @Transactional
    public ClaimResponse aprovar(String idClaim, ClaimAprovarRequest req) {
        Claim claim = findClaim(idClaim);
        String justificativa = req.dsJustificativa().trim();
        Item item = itemRepository.findById(idCodec.decodeItemId(req.idItem()))
                .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
        if (!item.getEvento().getId().equals(claim.getEvento().getId())) {
            throw new IllegalArgumentException("Item e pedido pertencem a eventos diferentes.");
        }
        claim.setStatus(statusItemService.findByNomeOrDefault(null, "Claim Aprovado"));
        claim.setDsJustificativaAprovacao(justificativa);
        claim.setDtAlteracao(LocalDateTime.now());
        claimRepository.save(claim);

        // Registro formal do match claim<->item.
        ClaimValidacao validacao = new ClaimValidacao();
        validacao.setEvento(claim.getEvento());
        validacao.setClaim(claim);
        validacao.setItem(item);
        validacao.setStResultado("APROVADO");
        validacao.setDtValidacao(LocalDateTime.now());
        validacao.setDtCadastro(LocalDateTime.now());
        validacao.setFgExcluido(false);
        validacaoRepository.save(validacao);

        // Ticket do novo fluxo (PICKUP/SHIPPING) + e-mail de escolha de modalidade.
        devolucaoFluxoService.criarTicketPosAprovacao(claim, item);
        workflowService.transitarSePermitido(req.idItem(), "Aguardando retirada",
                "Pedido de devolução aprovado — item reservado para retirada.");

        var email = emailService.enviar("CLAIM_APROVACAO", claim.getNmEmail(), variaveis(claim, justificativa));
        gravarHistorico(claim, item, "APROVACAO", null, justificativa, email);
        return response(claim);
    }

    @Transactional
    public ClaimResponse reprovar(String idClaim, ClaimReprovarRequest req) {
        Claim claim = findClaim(idClaim);
        String justificativa = req.dsJustificativa().trim();
        Item item = null;
        if (req.idItem() != null && !req.idItem().isBlank()) {
            item = itemRepository.findById(idCodec.decodeItemId(req.idItem()))
                    .filter(i -> !Boolean.TRUE.equals(i.getFgExcluido())).orElse(null);
        }
        claim.setStatus(statusItemService.findByNomeOrDefault(null, "Claim Rejeitado"));
        claim.setDsJustificativaReprovacao(justificativa);
        claim.setDtAlteracao(LocalDateTime.now());
        claimRepository.save(claim);
        Map<String, String> vars = variaveis(claim, justificativa);
        vars.put("motivo", justificativa);
        var email = emailService.enviar("CLAIM_REPROVACAO", claim.getNmEmail(), vars);
        gravarHistorico(claim, item, "REPROVACAO", null, justificativa, email);
        return response(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimHistoricoResponse> historico(String idClaim) {
        Long claimId = idCodec.decodeClaimId(idClaim);
        return historicoRepository.findByClaim_IdAndFgExcluidoFalseOrderByDtHistoricoDesc(claimId)
                .stream().map(this::toHistoricoResponse).toList();
    }

    @Transactional(readOnly = true)
    public ItemClaimsResumoResponse resumoItem(String idItem) {
        Long itemId = idCodec.decodeItemId(idItem);
        long pedidos = historicoRepository.countPedidosDistintosPorItem(itemId);
        long reprovacoes = historicoRepository.countByItem_IdAndTpEventoAndFgExcluidoFalse(itemId, "REPROVACAO");
        boolean aprovado = historicoRepository.countByItem_IdAndTpEventoAndFgExcluidoFalse(itemId, "APROVACAO") > 0;
        return new ItemClaimsResumoResponse(pedidos, reprovacoes, aprovado);
    }

    // ------------------------------------------------------------------

    private void gravarHistorico(Claim claim, Item item, String tpEvento, String tpSolicitacao,
                                 String detalhe, EmailService.Resultado email) {
        ClaimHistorico h = new ClaimHistorico();
        h.setClaim(claim);
        h.setItem(item);
        h.setTpEvento(tpEvento);
        h.setTpSolicitacao(tpSolicitacao);
        h.setDsDetalhe(detalhe);
        h.setOperador(usuarioLogadoOuNulo());
        h.setFgEmailEnviado(email != null && email.enviado());
        h.setDsEmailErro(email != null ? EmailService.truncarErro(email.erro()) : null);
        h.setDtHistorico(LocalDateTime.now());
        h.setFgExcluido(false);
        historicoRepository.save(h);
    }

    private Map<String, String> variaveis(Claim claim, String detalhe) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nomeSolicitante", claim.getNmNome() != null ? claim.getNmNome() : "");
        vars.put("objeto", claim.getNmObjeto() != null ? claim.getNmObjeto() : "");
        vars.put("evento", claim.getEvento() != null ? claim.getEvento().getNmEvento() : "");
        vars.put("ano", claim.getEvento() != null && claim.getEvento().getDtInicio() != null
                ? String.valueOf(claim.getEvento().getDtInicio().getYear()) : "");
        vars.put("protocolo", claim.getCdClaim() != null ? claim.getCdClaim() : "");
        vars.put("detalhe", detalhe != null ? detalhe : "");
        vars.put("motivo", "");
        vars.put("tipoSolicitacao", "informações adicionais");
        return vars;
    }

    private Claim findClaim(String idClaim) {
        return claimRepository.findById(idCodec.decodeClaimId(idClaim))
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido (claim) não encontrado."));
    }

    private ClaimResponse response(Claim claim) {
        return claimService.findById(idCodec.encodeClaimId(claim.getId()));
    }

    private ClaimHistoricoResponse toHistoricoResponse(ClaimHistorico h) {
        return new ClaimHistoricoResponse(
                String.valueOf(h.getId()),
                h.getTpEvento(),
                h.getTpSolicitacao(),
                h.getDsDetalhe(),
                h.getItem() != null ? h.getItem().getCdItem() : null,
                h.getOperador() != null ? h.getOperador().getNmUsuario() : null,
                h.getFgEmailEnviado(),
                h.getDsEmailErro(),
                h.getDtHistorico());
    }

    private Usuario usuarioLogadoOuNulo() {
        try {
            return usuarioContextService.requireUsuarioLogado();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
