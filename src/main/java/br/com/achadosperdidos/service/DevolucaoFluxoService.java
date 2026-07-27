package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
import br.com.achadosperdidos.controller.dto.*;
import br.com.achadosperdidos.entity.*;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.*;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static br.com.achadosperdidos.service.DevolucaoStatusMachine.*;

@Service
public class DevolucaoFluxoService {

    public static final String ACAO_CHOOSE_METHOD = "CHOOSE_DELIVERY_METHOD";
    public static final String ACAO_CONFIRM_PICKUP = "CONFIRM_PICKUP_OPTION";
    public static final String ACAO_SHIPPING_ADDRESS = "SUBMIT_SHIPPING_ADDRESS";
    public static final String ACAO_PAYMENT_PROOF = "UPLOAD_PAYMENT_PROOF";
    public static final String ACAO_VIEW_TRACKING = "VIEW_TRACKING";

    private final DevolucaoRepository devolucaoRepository;
    private final ClaimRepository claimRepository;
    private final ClaimValidacaoRepository claimValidacaoRepository;
    private final ItemRepository itemRepository;
    private final LocalRepository localRepository;
    private final DevolucaoHistoricoRepository historicoRepository;
    private final DevolucaoPickupOpcaoRepository pickupOpcaoRepository;
    private final DevolucaoShippingEnderecoRepository enderecoRepository;
    private final DevolucaoShippingCotacaoRepository cotacaoRepository;
    private final DevolucaoShippingPostagemRepository postagemRepository;
    private final DevolucaoHistoricoService historicoService;
    private final DevolucaoTokenService tokenService;
    private final EmailService emailService;
    private final ArquivoService arquivoService;
    private final MatchService matchService;
    private final WorkflowService workflowService;
    private final AuditoriaContextService auditoriaContext;
    private final UsuarioContextService usuarioContextService;
    private final SignedResourceIdCodec idCodec;

    @Value("${app.portal.base-url:http://localhost:4300}")
    private String portalBaseUrl;

    public DevolucaoFluxoService(DevolucaoRepository devolucaoRepository,
                                 ClaimRepository claimRepository,
                                 ClaimValidacaoRepository claimValidacaoRepository,
                                 ItemRepository itemRepository,
                                 LocalRepository localRepository,
                                 DevolucaoHistoricoRepository historicoRepository,
                                 DevolucaoPickupOpcaoRepository pickupOpcaoRepository,
                                 DevolucaoShippingEnderecoRepository enderecoRepository,
                                 DevolucaoShippingCotacaoRepository cotacaoRepository,
                                 DevolucaoShippingPostagemRepository postagemRepository,
                                 DevolucaoHistoricoService historicoService,
                                 DevolucaoTokenService tokenService,
                                 EmailService emailService,
                                 ArquivoService arquivoService,
                                 MatchService matchService,
                                 WorkflowService workflowService,
                                 AuditoriaContextService auditoriaContext,
                                 UsuarioContextService usuarioContextService,
                                 SignedResourceIdCodec idCodec) {
        this.devolucaoRepository = devolucaoRepository;
        this.claimRepository = claimRepository;
        this.claimValidacaoRepository = claimValidacaoRepository;
        this.itemRepository = itemRepository;
        this.localRepository = localRepository;
        this.historicoRepository = historicoRepository;
        this.pickupOpcaoRepository = pickupOpcaoRepository;
        this.enderecoRepository = enderecoRepository;
        this.cotacaoRepository = cotacaoRepository;
        this.postagemRepository = postagemRepository;
        this.historicoService = historicoService;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.arquivoService = arquivoService;
        this.matchService = matchService;
        this.workflowService = workflowService;
        this.auditoriaContext = auditoriaContext;
        this.usuarioContextService = usuarioContextService;
        this.idCodec = idCodec;
    }

    @Transactional
    public Devolucao criarTicketPosAprovacao(Claim claim, Item item) {
        auditoriaContext.marcarContexto();
        var existente = devolucaoRepository.findByClaim_IdAndFgExcluidoFalse(claim.getId());
        if (existente.isPresent()) {
            Devolucao d = existente.get();
            if (DELIVERY_METHOD_PENDING.equals(d.getTpStatus())) {
                enviarEmailEscolherModalidade(d, claim);
            }
            return d;
        }

        Devolucao d = new Devolucao();
        d.setEvento(item.getEvento());
        d.setItem(item);
        d.setClaim(claim);
        String origem = claim.getTpClaim() != null ? claim.getTpClaim() : "RETIRADA";
        d.setTpClaimOrigem(origem);
        d.setTpDevolucao(origem);
        d.setTpMetodo(null);
        d.setTpStatus(DELIVERY_METHOD_PENDING);
        d.setNmRecebedor(claim.getNmNome());
        d.setNrCpf(claim.getNrCpf());
        d.setFgAssinado(false);
        d.setFgConcluido(false);
        d.setDtDevolucao(agora());
        d.setDtCadastro(agora());
        d.setFgAtivo(true);
        d.setFgExcluido(false);
        d = devolucaoRepository.save(d);

        int year = agora().getYear();
        d.setCdProtocolo(String.format(Locale.ROOT, "DEV-%d-%06d", year, d.getId()));
        d = devolucaoRepository.save(d);

        matchService.confirmarMatch(claim.getId(), item.getId());

        historicoService.registrar(d, CREATED, "Ticket criado",
                "Devolução criada após aprovação do pedido.", "SISTEMA", null, null, null);

        DevolucaoAcaoToken token = tokenService.gerar(d, ACAO_CHOOSE_METHOD, 15, false);
        EmailService.Resultado email = enviarEmailEscolherModalidade(d, claim, token);
        historicoService.registrar(d, DELIVERY_METHOD_PENDING, "Aguardando modalidade",
                "E-mail enviado para escolha de modalidade de devolução.",
                "SISTEMA", null, email, null);
        return d;
    }

    @Transactional
    public DevolucaoResponse criarRetornoDoClaim(String claimIdToken) {
        Claim claim = claimRepository.findById(idCodec.decodeClaimId(claimIdToken))
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido (claim) não encontrado."));
        Item item = resolverItemDoClaim(claim);
        String statusNome = claim.getStatus() != null ? claim.getStatus().getNmStatus() : "";
        String sn = statusNome == null ? "" : statusNome.toLowerCase(Locale.ROOT);
        if (!(sn.contains("aprov") || sn.contains("match") || sn.contains("retirada"))) {
            // Permite se já houver validação APROVADO
            boolean aprovado = claimValidacaoRepository
                    .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claim.getId(), "APROVADO")
                    .stream().findFirst().isPresent();
            if (!aprovado) {
                throw new IllegalArgumentException("O pedido precisa estar aprovado (ou com match confirmado) para gerar devolução.");
            }
        }
        Devolucao d = criarTicketPosAprovacao(claim, item);
        workflowService.transitarSePermitido(idCodec.encodeItemId(item.getId()), "Aguardando retirada",
                "Ticket de devolução criado — item reservado.");
        return toListResponse(d);
    }

    @Transactional(readOnly = true)
    public DevolucaoDetalheResponse detalhar(String idToken) {
        Devolucao d = findDevolucao(idToken);
        return toDetalhe(d);
    }

    @Transactional(readOnly = true)
    public List<DevolucaoHistoricoItemResponse> historico(String idToken) {
        Devolucao d = findDevolucao(idToken);
        return mapHistorico(d.getId());
    }

    @Transactional
    public DevolucaoDetalheResponse cadastrarPickupOptions(String idToken, DevolucaoPickupOptionsRequest payload) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        assertStatusIn(d, PICKUP_SELECTED, PICKUP_SCHEDULE_REQUESTED, PICKUP_OPTIONS_PREPARED,
                PICKUP_OPTIONS_SENT, PICKUP_CONFIRMATION_PENDING, PICKUP_OPTIONS_EXPIRED);

        for (DevolucaoPickupOpcao op : pickupOpcaoRepository
                .findByDevolucao_IdAndFgExcluidoFalseOrderByDtOpcaoAscHrInicioAsc(d.getId())) {
            if (!Boolean.TRUE.equals(op.getFgSelecionada())) {
                op.setFgExcluido(true);
                pickupOpcaoRepository.save(op);
            }
        }

        for (var item : payload.options()) {
            DevolucaoPickupOpcao op = new DevolucaoPickupOpcao();
            op.setDevolucao(d);
            op.setDtOpcao(item.date());
            op.setHrInicio(item.startTime());
            op.setHrFim(item.endTime());
            op.setNmLocal(item.pickupLocationName());
            op.setDtExpiracao(item.expiresAt());
            op.setDsNotas(item.notes());
            op.setFgSelecionada(false);
            op.setDtCadastro(agora());
            op.setFgExcluido(false);
            if (item.pickupLocationId() != null && !item.pickupLocationId().isBlank()) {
                Long localId = idCodec.decodeLocalId(item.pickupLocationId());
                Local local = localRepository.findById(localId).orElse(null);
                op.setLocal(local);
                if (op.getNmLocal() == null && local != null) {
                    op.setNmLocal(local.getNmLocal());
                }
            }
            pickupOpcaoRepository.save(op);
        }

        transitar(d, PICKUP_OPTIONS_PREPARED);
        historicoService.registrar(d, PICKUP_OPTIONS_PREPARED, "Opções de agenda cadastradas",
                payload.options().size() + " opção(ões) cadastrada(s).",
                "OPERADOR", usuarioLogado(), null, null);

        if (Boolean.TRUE.equals(payload.sendEmail())) {
            return enviarPickupOptions(idToken);
        }
        return toDetalhe(d);
    }

    @Transactional
    public DevolucaoDetalheResponse enviarPickupOptions(String idToken) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        assertStatusIn(d, PICKUP_OPTIONS_PREPARED, PICKUP_OPTIONS_SENT, PICKUP_CONFIRMATION_PENDING,
                PICKUP_OPTIONS_EXPIRED, PICKUP_SCHEDULE_REQUESTED);
        Claim claim = d.getClaim();
        if (claim == null || claim.getNmEmail() == null) {
            throw new IllegalArgumentException("Devolução sem e-mail do solicitante.");
        }
        DevolucaoAcaoToken token = tokenService.gerar(d, ACAO_CONFIRM_PICKUP, 7, false);
        EmailService.Resultado email = enviarEmail(d, claim, "DEVOLUCAO_PICKUP_OPCOES",
                montarLink("/devolucao/confirmar-agendamento", token.getCdToken()));
        if (!PICKUP_CONFIRMATION_PENDING.equals(d.getTpStatus())) {
            if (PICKUP_OPTIONS_PREPARED.equals(d.getTpStatus())
                    || PICKUP_OPTIONS_EXPIRED.equals(d.getTpStatus())
                    || PICKUP_SCHEDULE_REQUESTED.equals(d.getTpStatus())) {
                transitar(d, PICKUP_OPTIONS_SENT);
            }
            if (PICKUP_OPTIONS_SENT.equals(d.getTpStatus())) {
                transitar(d, PICKUP_CONFIRMATION_PENDING);
            }
        }
        historicoService.registrar(d, PICKUP_OPTIONS_SENT, "Opções de agenda enviadas",
                "E-mail com horários de retirada enviado.", "OPERADOR", usuarioLogado(), email, null);
        return toDetalhe(d);
    }

    @Transactional
    public DevolucaoDetalheResponse registrarCotacao(String idToken, DevolucaoShippingQuoteRequest payload) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        assertStatusIn(d, SHIPPING_SELECTED, SHIPPING_ADDRESS_PENDING, SHIPPING_QUOTE_PENDING,
                SHIPPING_QUOTE_SENT, PAYMENT_PROOF_PENDING);
        if (enderecoRepository.findByDevolucao_IdAndFgExcluidoFalse(d.getId()).isEmpty()) {
            throw new IllegalArgumentException("Informe o endereço antes da cotação.");
        }

        DevolucaoShippingCotacao c = new DevolucaoShippingCotacao();
        c.setDevolucao(d);
        c.setVlValor(payload.amount());
        c.setSgMoeda(payload.currency() != null && !payload.currency().isBlank() ? payload.currency() : "BRL");
        c.setQtDiasEntregaEstimados(payload.estimatedDeliveryDays());
        c.setQtDiasPrazoPostagem(payload.postingDeadlineDaysAfterPayment());
        c.setDsInstrucoesPagamento(payload.paymentInstructions().trim());
        c.setOperador(usuarioLogado());
        c.setDtInformada(agora());
        c.setFgExcluido(false);
        cotacaoRepository.save(c);

        if (SHIPPING_SELECTED.equals(d.getTpStatus()) || SHIPPING_ADDRESS_PENDING.equals(d.getTpStatus())) {
            if (SHIPPING_SELECTED.equals(d.getTpStatus())) {
                transitar(d, SHIPPING_ADDRESS_PENDING);
            }
            transitar(d, SHIPPING_QUOTE_PENDING);
        } else if (!SHIPPING_QUOTE_PENDING.equals(d.getTpStatus())
                && !SHIPPING_QUOTE_SENT.equals(d.getTpStatus())
                && !PAYMENT_PROOF_PENDING.equals(d.getTpStatus())) {
            d.setTpStatus(SHIPPING_QUOTE_PENDING);
            d.setDtAlteracao(agora());
            devolucaoRepository.save(d);
        }
        historicoService.registrar(d, SHIPPING_QUOTE_PENDING, "Cotação registrada",
                "Valor: " + payload.amount() + " " + c.getSgMoeda(),
                "OPERADOR", usuarioLogado(), null, null);

        if (Boolean.TRUE.equals(payload.sendEmail())) {
            return enviarCotacao(idToken);
        }
        return toDetalhe(d);
    }

    @Transactional
    public DevolucaoDetalheResponse enviarCotacao(String idToken) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        Claim claim = requireClaim(d);
        var cotacao = cotacaoRepository.findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtInformadaDesc(d.getId())
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma cotação cadastrada."));
        DevolucaoAcaoToken token = tokenService.gerar(d, ACAO_PAYMENT_PROOF, 15, true);
        Map<String, String> vars = variaveisBase(d, claim);
        vars.put("linkAcao", montarLink("/devolucao/comprovante-pagamento", token.getCdToken()));
        vars.put("valorFrete", cotacao.getVlValor().toPlainString());
        vars.put("instrucoesPagamento", cotacao.getDsInstrucoesPagamento());
        EmailService.Resultado email = emailService.enviar("DEVOLUCAO_SHIPPING_COTACAO", claim.getNmEmail(), vars);
        if (SHIPPING_QUOTE_PENDING.equals(d.getTpStatus()) || SHIPPING_QUOTE_SENT.equals(d.getTpStatus())) {
            transitar(d, SHIPPING_QUOTE_SENT);
            transitar(d, PAYMENT_PROOF_PENDING);
        } else if (!PAYMENT_PROOF_PENDING.equals(d.getTpStatus()) && !PAID_AWAITING_POSTING.equals(d.getTpStatus())) {
            d.setTpStatus(PAYMENT_PROOF_PENDING);
            d.setDtAlteracao(agora());
            devolucaoRepository.save(d);
        }
        historicoService.registrar(d, SHIPPING_QUOTE_SENT, "Cotação enviada",
                "E-mail com cotação e instruções de pagamento.", "OPERADOR", usuarioLogado(), email, null);
        return toDetalhe(d);
    }

    @Transactional
    public DevolucaoDetalheResponse registrarPostagem(String idToken, DevolucaoShippingPostingRequest payload) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        if (!PAID_AWAITING_POSTING.equals(d.getTpStatus())) {
            throw new IllegalArgumentException("Postagem só é permitida após comprovante (status PAID_AWAITING_POSTING).");
        }
        DevolucaoShippingPostagem p = new DevolucaoShippingPostagem();
        p.setDevolucao(d);
        p.setDtPostagem(payload.postingDate());
        p.setCdRastreio(payload.trackingCode().trim());
        p.setDsNotas(payload.notes());
        p.setOperador(usuarioLogado());
        p.setDtRegistro(agora());
        p.setFgExcluido(false);
        postagemRepository.save(p);
        transitar(d, POSTED);
        historicoService.registrar(d, POSTED, "Postagem registrada",
                "Rastreio: " + p.getCdRastreio(), "OPERADOR", usuarioLogado(), null, null);
        if (Boolean.TRUE.equals(payload.sendEmail())) {
            return enviarPostagem(idToken);
        }
        return toDetalhe(d);
    }

    @Transactional
    public DevolucaoDetalheResponse enviarPostagem(String idToken) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        Claim claim = requireClaim(d);
        var postagem = postagemRepository.findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtRegistroDesc(d.getId())
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma postagem cadastrada."));
        DevolucaoAcaoToken token = tokenService.gerar(d, ACAO_VIEW_TRACKING, 30, true);
        Map<String, String> vars = variaveisBase(d, claim);
        vars.put("linkAcao", montarLink("/devolucao/rastreio", token.getCdToken()));
        vars.put("rastreio", postagem.getCdRastreio());
        EmailService.Resultado email = emailService.enviar("DEVOLUCAO_POSTAGEM", claim.getNmEmail(), vars);
        if (POSTED.equals(d.getTpStatus())) {
            transitar(d, IN_TRANSIT);
        }
        historicoService.registrar(d, "DEVOLUCAO_POSTAGEM", "E-mail de postagem enviado",
                "Rastreio informado ao solicitante.", "OPERADOR", usuarioLogado(), email, null);
        return toDetalhe(d);
    }

    @Transactional
    public DevolucaoDetalheResponse uploadTermo(String idToken, MultipartFile file) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        assertStatusIn(d, READY_FOR_PICKUP, PICKUP_SCHEDULE_CONFIRMED, EM_CONFERENCIA,
                TERMO_GERADO, AGUARDANDO_RETIRADA, AGUARDANDO_ASSINATURA, ASSINADO);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo do termo é obrigatório.");
        }
        arquivoService.upload("DEVOLUCAO", idCodec.encodeDevolucaoId(d.getId()), "TERMO", file, false);
        if (!TERMO_GERADO.equals(d.getTpStatus())) {
            if (READY_FOR_PICKUP.equals(d.getTpStatus()) || PICKUP_SCHEDULE_CONFIRMED.equals(d.getTpStatus())
                    || AGUARDANDO_RETIRADA.equals(d.getTpStatus())) {
                try {
                    if (!EM_CONFERENCIA.equals(d.getTpStatus())) {
                        // READY → EM_CONFERENCIA → TERMO_GERADO (via caminho permitido)
                        if (READY_FOR_PICKUP.equals(d.getTpStatus()) || PICKUP_SCHEDULE_CONFIRMED.equals(d.getTpStatus())) {
                            if (PICKUP_SCHEDULE_CONFIRMED.equals(d.getTpStatus())) {
                                transitar(d, READY_FOR_PICKUP);
                            }
                            transitar(d, EM_CONFERENCIA);
                        } else if (AGUARDANDO_RETIRADA.equals(d.getTpStatus())) {
                            transitar(d, EM_CONFERENCIA);
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // segue para TERMO_GERADO se transição intermediária não for necessária
                }
            }
            if (!TERMO_GERADO.equals(d.getTpStatus())) {
                DevolucaoStatusMachine.assertCanTransition(d.getTpStatus(), TERMO_GERADO);
                d.setTpStatus(TERMO_GERADO);
                d.setDtAlteracao(agora());
                devolucaoRepository.save(d);
            }
        }
        historicoService.registrar(d, TERMO_GERADO, "Termo gerado",
                "PDF do termo de devolução anexado.", "OPERADOR", usuarioLogado(), null, null);
        return toDetalhe(d);
    }

    @Transactional
    public DevolucaoResponse concluirPresencial(String idToken, DevolucaoConcluirPresencialRequest req) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        assertStatusIn(d, EM_CONFERENCIA, TERMO_GERADO, READY_FOR_PICKUP, ASSINADO, AGUARDANDO_ASSINATURA);
        if (READY_FOR_PICKUP.equals(d.getTpStatus())) {
            transitar(d, EM_CONFERENCIA);
        }
        if (req != null && req.dsObservacao() != null && !req.dsObservacao().isBlank()) {
            d.setDsObservacao(req.dsObservacao().trim());
        }
        concluir(d, "Devolução presencial concluída.");
        Claim claim = d.getClaim();
        if (claim != null && claim.getNmEmail() != null) {
            EmailService.Resultado email = enviarEmail(d, claim, "DEVOLUCAO_CONCLUIDA", null);
            historicoService.registrar(d, COMPLETED, "Devolução concluída",
                    "Baixa presencial registrada.", "OPERADOR", usuarioLogado(), email, null);
        } else {
            historicoService.registrar(d, COMPLETED, "Devolução concluída",
                    "Baixa presencial registrada.", "OPERADOR", usuarioLogado(), null, null);
        }
        return toListResponse(d);
    }

    @Transactional
    public DevolucaoResponse atualizarStatus(String idToken, DevolucaoStatusRequest request) {
        auditoriaContext.marcarContexto();
        Devolucao d = findDevolucao(idToken);
        String destino = request.tpStatus().trim().toUpperCase(Locale.ROOT);
        if (CONCLUIDO.equals(destino)) destino = COMPLETED;
        DevolucaoStatusMachine.assertCanTransition(d.getTpStatus(), destino);
        if (request.dsObservacao() != null && !request.dsObservacao().isBlank()) {
            d.setDsObservacao(request.dsObservacao().trim());
        }
        if (ASSINADO.equals(destino) || COMPLETED.equals(destino) || CONCLUIDO.equals(destino)) {
            d.setFgAssinado(true);
        }
        d.setTpStatus(destino);
        d.setDtAlteracao(agora());
        if (COMPLETED.equals(destino)) {
            concluir(d, "Status atualizado para concluído.");
        } else {
            devolucaoRepository.save(d);
        }
        historicoService.registrar(d, destino, "Status atualizado",
                "Novo status: " + destino, "OPERADOR", usuarioLogado(), null, null);
        return toListResponse(d);
    }

    @Transactional
    public Map<String, Object> reenviarEmail(String idToken, DevolucaoEmailResendRequest req) {
        Devolucao d = findDevolucao(idToken);
        Claim claim = requireClaim(d);
        String tp = req != null && req.tpEvento() != null && !req.tpEvento().isBlank()
                ? req.tpEvento().trim().toUpperCase(Locale.ROOT)
                : sugerirEmailPorStatus(d.getTpStatus());
        EmailService.Resultado email = switch (tp) {
            case "DEVOLUCAO_ESCOLHER_MODALIDADE" -> {
                DevolucaoAcaoToken token = tokenService.gerar(d, ACAO_CHOOSE_METHOD, 15, false);
                yield enviarEmailEscolherModalidade(d, claim, token);
            }
            case "DEVOLUCAO_PICKUP_OPCOES" -> {
                enviarPickupOptions(idToken);
                yield new EmailService.Resultado(true, null);
            }
            case "DEVOLUCAO_SHIPPING_COTACAO" -> {
                enviarCotacao(idToken);
                yield new EmailService.Resultado(true, null);
            }
            case "DEVOLUCAO_POSTAGEM" -> {
                enviarPostagem(idToken);
                yield new EmailService.Resultado(true, null);
            }
            default -> enviarEmail(d, claim, tp, null);
        };
        historicoService.registrar(d, tp, "E-mail reenviado",
                "Evento: " + tp, "OPERADOR", usuarioLogado(), email, null);
        return Map.of("tpEvento", tp, "enviado", email.enviado(), "erro", email.erro() != null ? email.erro() : "");
    }

    // ---------------- Portal ----------------

    @Transactional(readOnly = true)
    public PortalDevolucaoContextResponse contexto(String cdToken) {
        DevolucaoAcaoToken token;
        try {
            token = tokenService.resolver(cdToken);
        } catch (RecursoNaoEncontradoException ex) {
            return new PortalDevolucaoContextResponse(
                    null, "", "", "", "invalid", null, null, true, false,
                    "Link inválido.", List.of(), null, null);
        }
        Devolucao d = token.getDevolucao();
        Claim claim = d.getClaim();
        Item item = d.getItem();
        String statusTok = tokenService.statusToken(token);
        boolean used = "used".equals(statusTok);
        boolean expired = "expired".equals(statusTok) || used;
        // multi-uso: used flag se já houve uso mas ainda válido
        if (Boolean.TRUE.equals(token.getFgMultiUso()) && token.getDtUsado() != null
                && "valid".equals(statusTok)) {
            used = false;
            expired = false;
        }

        List<PortalDevolucaoContextResponse.PickupOption> options = List.of();
        if (ACAO_CONFIRM_PICKUP.equals(token.getTpAcao())) {
            options = pickupOpcaoRepository
                    .findByDevolucao_IdAndFgExcluidoFalseOrderByDtOpcaoAscHrInicioAsc(d.getId())
                    .stream()
                    .filter(o -> !Boolean.TRUE.equals(o.getFgSelecionada()) || true)
                    .map(o -> new PortalDevolucaoContextResponse.PickupOption(
                            idCodec.encodeDevolucaoPickupOpcaoId(o.getId()),
                            o.getDtOpcao(), o.getHrInicio(), o.getHrFim(),
                            o.getNmLocal(), o.getDsNotas()))
                    .toList();
        }

        PortalDevolucaoContextResponse.ShippingQuote quote = null;
        if (ACAO_PAYMENT_PROOF.equals(token.getTpAcao()) || ACAO_VIEW_TRACKING.equals(token.getTpAcao())) {
            quote = cotacaoRepository.findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtInformadaDesc(d.getId())
                    .map(c -> new PortalDevolucaoContextResponse.ShippingQuote(
                            c.getVlValor(), c.getSgMoeda(), c.getQtDiasEntregaEstimados(),
                            c.getQtDiasPrazoPostagem(), c.getDsInstrucoesPagamento()))
                    .orElse(null);
        }

        PortalDevolucaoContextResponse.Tracking tracking = null;
        if (ACAO_VIEW_TRACKING.equals(token.getTpAcao()) || POSTED.equals(d.getTpStatus())
                || IN_TRANSIT.equals(d.getTpStatus()) || DELIVERED.equals(d.getTpStatus())) {
            tracking = postagemRepository.findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtRegistroDesc(d.getId())
                    .map(p -> new PortalDevolucaoContextResponse.Tracking(p.getDtPostagem(), p.getCdRastreio()))
                    .orElse(null);
        }

        return new PortalDevolucaoContextResponse(
                token.getTpAcao(),
                d.getCdProtocolo() != null ? d.getCdProtocolo() : "",
                item != null ? item.getNmTitulo() : (claim != null ? claim.getNmObjeto() : ""),
                d.getEvento() != null ? d.getEvento().getNmEvento() : "",
                statusTok,
                d.getTpMetodo(),
                token.getDtExpiracao(),
                expired,
                used,
                nextAction(d.getTpStatus()),
                options,
                quote,
                tracking);
    }

    @Transactional
    public Map<String, Object> modalidade(String cdToken, PortalDevolucaoModalidadeRequest req) {
        DevolucaoAcaoToken token = tokenService.resolverParaUso(cdToken);
        if (!ACAO_CHOOSE_METHOD.equals(token.getTpAcao())) {
            throw new IllegalArgumentException("Token não permite escolher modalidade.");
        }
        Devolucao d = token.getDevolucao();
        assertStatusIn(d, DELIVERY_METHOD_PENDING, CREATED);
        String method = req.method().trim().toUpperCase(Locale.ROOT);
        if (!"PICKUP".equals(method) && !"SHIPPING".equals(method)) {
            throw new IllegalArgumentException("Modalidade inválida. Use PICKUP ou SHIPPING.");
        }
        d.setTpMetodo(method);
        d.setTpDevolucao(method);
        if ("PICKUP".equals(method)) {
            transitar(d, PICKUP_SELECTED);
            historicoService.registrar(d, PICKUP_SELECTED, "Modalidade: retirada",
                    "Solicitante escolheu retirada presencial.", "SOLICITANTE", null, null, null);
        } else {
            transitar(d, SHIPPING_SELECTED);
            transitar(d, SHIPPING_ADDRESS_PENDING);
            DevolucaoAcaoToken addrToken = tokenService.gerar(d, ACAO_SHIPPING_ADDRESS, 15, false);
            Claim claim = d.getClaim();
            if (claim != null) {
                enviarEmail(d, claim, "DEVOLUCAO_OCORRENCIA",
                        montarLink("/devolucao/endereco", addrToken.getCdToken()));
            }
            historicoService.registrar(d, SHIPPING_ADDRESS_PENDING, "Modalidade: Correios",
                    "Solicitante escolheu envio pelos Correios.", "SOLICITANTE", null, null, null);
        }
        tokenService.marcarUsado(token);
        return Map.of("method", method, "tpStatus", d.getTpStatus(), "protocolo",
                d.getCdProtocolo() != null ? d.getCdProtocolo() : "");
    }

    @Transactional
    public Map<String, Object> pickupRequest(String cdToken) {
        // Front encadeia modalidade(PICKUP) + pickup/request no mesmo token.
        DevolucaoAcaoToken token = tokenService.resolver(cdToken);
        if (tokenService.isExpirado(token)) {
            throw new br.com.achadosperdidos.exception.LinkExpiradoException("Este link expirou.");
        }
        Devolucao d = token.getDevolucao();
        assertStatusIn(d, PICKUP_SELECTED, PICKUP_SCHEDULE_REQUESTED);
        if (PICKUP_SELECTED.equals(d.getTpStatus())) {
            transitar(d, PICKUP_SCHEDULE_REQUESTED);
        }
        historicoService.registrar(d, PICKUP_SCHEDULE_REQUESTED, "Agendamento solicitado",
                "Solicitante pediu opções de horário.", "SOLICITANTE", null, null, null);
        return Map.of("tpStatus", d.getTpStatus());
    }

    @Transactional
    public Map<String, Object> pickupConfirm(String cdToken, PortalDevolucaoPickupConfirmRequest req) {
        DevolucaoAcaoToken token = tokenService.resolverParaUso(cdToken);
        if (!ACAO_CONFIRM_PICKUP.equals(token.getTpAcao())) {
            throw new IllegalArgumentException("Token não permite confirmar agendamento.");
        }
        Devolucao d = token.getDevolucao();
        long optionId = idCodec.decodeDevolucaoPickupOpcaoId(req.optionId());
        DevolucaoPickupOpcao op = pickupOpcaoRepository
                .findByIdAndDevolucao_IdAndFgExcluidoFalse(optionId, d.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Opção de agenda não encontrada."));
        if (op.getDtExpiracao() != null && op.getDtExpiracao().isBefore(agora())) {
            transitar(d, PICKUP_OPTIONS_EXPIRED);
            throw new IllegalArgumentException("Esta opção de agenda expirou.");
        }
        for (DevolucaoPickupOpcao other : pickupOpcaoRepository
                .findByDevolucao_IdAndFgExcluidoFalseOrderByDtOpcaoAscHrInicioAsc(d.getId())) {
            other.setFgSelecionada(other.getId().equals(op.getId()));
            pickupOpcaoRepository.save(other);
        }
        transitar(d, PICKUP_SCHEDULE_CONFIRMED);
        transitar(d, READY_FOR_PICKUP);
        Claim claim = d.getClaim();
        EmailService.Resultado email = null;
        if (claim != null) {
            email = enviarEmail(d, claim, "DEVOLUCAO_PICKUP_CONFIRMADO", null);
        }
        historicoService.registrar(d, READY_FOR_PICKUP, "Agendamento confirmado",
                "Opção: " + op.getDtOpcao() + " " + op.getHrInicio() + "-" + op.getHrFim(),
                "SOLICITANTE", null, email, null);
        tokenService.marcarUsado(token);
        return Map.of("tpStatus", d.getTpStatus(), "optionId", req.optionId());
    }

    @Transactional
    public Map<String, Object> shippingAddress(String cdToken, PortalDevolucaoShippingAddressRequest req) {
        DevolucaoAcaoToken token = tokenService.resolverParaUso(cdToken);
        Devolucao d = token.getDevolucao();
        assertStatusIn(d, SHIPPING_SELECTED, SHIPPING_ADDRESS_PENDING, SHIPPING_QUOTE_PENDING);
        String cep = req.zipCode().replaceAll("\\D", "");
        if (cep.length() != 8) {
            throw new IllegalArgumentException("CEP inválido.");
        }
        DevolucaoShippingEndereco end = enderecoRepository.findByDevolucao_IdAndFgExcluidoFalse(d.getId())
                .orElseGet(DevolucaoShippingEndereco::new);
        end.setDevolucao(d);
        end.setNmDestinatario(req.recipientName().trim());
        end.setNrCep(cep);
        end.setNmLogradouro(req.street().trim());
        end.setNrNumero(req.number().trim());
        end.setDsComplemento(req.complement());
        end.setNmBairro(req.district().trim());
        end.setNmCidade(req.city().trim());
        end.setSgUf(req.state().trim().toUpperCase(Locale.ROOT));
        end.setNrTelefone(req.phone().trim());
        if (end.getDtCadastro() == null) end.setDtCadastro(agora());
        else end.setDtAlteracao(agora());
        end.setFgExcluido(false);
        enderecoRepository.save(end);

        if (SHIPPING_SELECTED.equals(d.getTpStatus()) || SHIPPING_ADDRESS_PENDING.equals(d.getTpStatus())) {
            if (SHIPPING_SELECTED.equals(d.getTpStatus())) {
                transitar(d, SHIPPING_ADDRESS_PENDING);
            }
            transitar(d, SHIPPING_QUOTE_PENDING);
        }
        historicoService.registrar(d, SHIPPING_QUOTE_PENDING, "Endereço informado",
                end.getNmCidade() + "/" + end.getSgUf(), "SOLICITANTE", null, null, null);
        if (ACAO_SHIPPING_ADDRESS.equals(token.getTpAcao()) || ACAO_CHOOSE_METHOD.equals(token.getTpAcao())) {
            tokenService.marcarUsado(token);
        }
        return Map.of("tpStatus", d.getTpStatus());
    }

    @Transactional
    public Map<String, Object> paymentProof(String cdToken, MultipartFile comprovante) {
        DevolucaoAcaoToken token = tokenService.resolverParaUso(cdToken);
        Devolucao d = token.getDevolucao();
        assertStatusIn(d, SHIPPING_QUOTE_SENT, PAYMENT_PROOF_PENDING, PAID_AWAITING_POSTING);
        if (comprovante == null || comprovante.isEmpty()) {
            throw new IllegalArgumentException("Comprovante é obrigatório.");
        }
        String mime = comprovante.getContentType() == null ? "" : comprovante.getContentType().toLowerCase(Locale.ROOT);
        if (!(mime.startsWith("image/") || mime.equals("application/pdf"))) {
            throw new IllegalArgumentException("Envie PDF, JPEG ou PNG.");
        }
        arquivoService.upload("DEVOLUCAO", idCodec.encodeDevolucaoId(d.getId()),
                "COMPROVANTE_PAGAMENTO", comprovante, false);
        if (!PAID_AWAITING_POSTING.equals(d.getTpStatus())) {
            if (SHIPPING_QUOTE_SENT.equals(d.getTpStatus())) {
                transitar(d, PAYMENT_PROOF_PENDING);
            }
            transitar(d, PAID_AWAITING_POSTING);
        }
        Claim claim = d.getClaim();
        EmailService.Resultado email = claim != null
                ? enviarEmail(d, claim, "DEVOLUCAO_PAGAMENTO_RECEBIDO", null) : null;
        historicoService.registrar(d, PAID_AWAITING_POSTING, "Comprovante recebido",
                "Aguardando postagem.", "SOLICITANTE", null, email, null);
        tokenService.marcarUsado(token);
        return Map.of("tpStatus", d.getTpStatus());
    }

    @Transactional(readOnly = true)
    public PortalDevolucaoContextResponse.Tracking tracking(String cdToken) {
        DevolucaoAcaoToken token = tokenService.resolver(cdToken);
        return postagemRepository
                .findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtRegistroDesc(token.getDevolucao().getId())
                .map(p -> new PortalDevolucaoContextResponse.Tracking(p.getDtPostagem(), p.getCdRastreio()))
                .orElse(new PortalDevolucaoContextResponse.Tracking(null, null));
    }

    // ---------------- helpers ----------------

    private Item resolverItemDoClaim(Claim claim) {
        return claimValidacaoRepository
                .findByClaim_IdAndStResultadoAndFgExcluidoFalseOrderByQtSimilaridadeDesc(claim.getId(), "APROVADO")
                .stream().findFirst()
                .map(ClaimValidacao::getItem)
                .or(() -> claimValidacaoRepository
                        .findByClaim_IdAndFgExcluidoFalseOrderByDtCadastroDesc(claim.getId())
                        .stream().findFirst().map(ClaimValidacao::getItem))
                .filter(i -> i != null && !Boolean.TRUE.equals(i.getFgExcluido()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Claim sem item vinculado. Confirme o match antes de criar a devolução."));
    }

    private Devolucao findDevolucao(String idToken) {
        return devolucaoRepository.findById(idCodec.decodeDevolucaoId(idToken))
                .filter(x -> !Boolean.TRUE.equals(x.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Devolução não encontrada."));
    }

    private void assertStatusIn(Devolucao d, String... statuses) {
        String atual = d.getTpStatus();
        for (String s : statuses) {
            if (s.equalsIgnoreCase(atual)) return;
        }
        throw new IllegalArgumentException(
                "Status atual (" + atual + ") não permite esta operação.");
    }

    private void transitar(Devolucao d, String destino) {
        if (destino.equals(d.getTpStatus())) return;
        DevolucaoStatusMachine.assertCanTransition(d.getTpStatus(), destino);
        d.setTpStatus(destino);
        d.setDtAlteracao(agora());
        devolucaoRepository.save(d);
    }

    private void concluir(Devolucao d, String motivo) {
        d.setTpStatus(COMPLETED);
        d.setFgConcluido(true);
        d.setDtConclusao(agora());
        d.setDtAlteracao(agora());
        Item item = d.getItem();
        item.setFgEntregue(true);
        item.setDtAlteracao(agora());
        itemRepository.save(item);
        workflowService.transitarSePermitido(idCodec.encodeItemId(item.getId()), "Devolvido", motivo);
        devolucaoRepository.save(d);
    }

    private EmailService.Resultado enviarEmailEscolherModalidade(Devolucao d, Claim claim) {
        DevolucaoAcaoToken token = tokenService.gerar(d, ACAO_CHOOSE_METHOD, 15, false);
        return enviarEmailEscolherModalidade(d, claim, token);
    }

    private EmailService.Resultado enviarEmailEscolherModalidade(Devolucao d, Claim claim, DevolucaoAcaoToken token) {
        if (claim == null || claim.getNmEmail() == null || claim.getNmEmail().isBlank()) {
            return new EmailService.Resultado(false, "E-mail do solicitante ausente.");
        }
        return enviarEmail(d, claim, "DEVOLUCAO_ESCOLHER_MODALIDADE",
                montarLink("/devolucao/escolher-modalidade", token.getCdToken()));
    }

    private EmailService.Resultado enviarEmail(Devolucao d, Claim claim, String tpEvento, String linkAcao) {
        Map<String, String> vars = variaveisBase(d, claim);
        if (linkAcao != null) vars.put("linkAcao", linkAcao);
        return emailService.enviar(tpEvento, claim.getNmEmail(), vars);
    }

    private Map<String, String> variaveisBase(Devolucao d, Claim claim) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nome", claim != null && claim.getNmNome() != null ? claim.getNmNome() : d.getNmRecebedor());
        vars.put("nomeSolicitante", vars.get("nome"));
        vars.put("protocolo", d.getCdProtocolo() != null ? d.getCdProtocolo() : "");
        vars.put("objeto", d.getItem() != null ? d.getItem().getNmTitulo()
                : (claim != null ? claim.getNmObjeto() : ""));
        vars.put("evento", d.getEvento() != null ? d.getEvento().getNmEvento() : "");
        vars.put("ano", d.getEvento() != null && d.getEvento().getDtInicio() != null
                ? String.valueOf(d.getEvento().getDtInicio().getYear()) : String.valueOf(agora().getYear()));
        vars.put("linkAcao", "");
        return vars;
    }

    private String montarLink(String path, String cdToken) {
        String base = portalBaseUrl == null ? "" : portalBaseUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String p = path.startsWith("/") ? path : "/" + path;
        return base + p + "?token=" + cdToken;
    }

    private String sugerirEmailPorStatus(String status) {
        return switch (status == null ? "" : status) {
            case DELIVERY_METHOD_PENDING, CREATED -> "DEVOLUCAO_ESCOLHER_MODALIDADE";
            case PICKUP_OPTIONS_PREPARED, PICKUP_OPTIONS_SENT, PICKUP_CONFIRMATION_PENDING -> "DEVOLUCAO_PICKUP_OPCOES";
            case SHIPPING_QUOTE_PENDING, SHIPPING_QUOTE_SENT, PAYMENT_PROOF_PENDING -> "DEVOLUCAO_SHIPPING_COTACAO";
            case POSTED, IN_TRANSIT -> "DEVOLUCAO_POSTAGEM";
            case COMPLETED, CONCLUIDO, DELIVERED -> "DEVOLUCAO_CONCLUIDA";
            default -> "DEVOLUCAO_OCORRENCIA";
        };
    }

    private Claim requireClaim(Devolucao d) {
        Claim claim = d.getClaim();
        if (claim == null) throw new IllegalArgumentException("Devolução sem claim vinculado.");
        return claim;
    }

    private Usuario usuarioLogado() {
        return usuarioContextService.findUsuarioLogado().orElse(null);
    }

    private LocalDateTime agora() {
        return TimeConfig.now();
    }

    private DevolucaoDetalheResponse toDetalhe(Devolucao d) {
        Item item = d.getItem();
        Claim claim = d.getClaim();
        List<DevolucaoPickupOptionResponse> options = pickupOpcaoRepository
                .findByDevolucao_IdAndFgExcluidoFalseOrderByDtOpcaoAscHrInicioAsc(d.getId())
                .stream()
                .map(o -> new DevolucaoPickupOptionResponse(
                        idCodec.encodeDevolucaoPickupOpcaoId(o.getId()),
                        o.getDtOpcao(), o.getHrInicio(), o.getHrFim(),
                        o.getNmLocal(), o.getDtExpiracao(), o.getDsNotas(), o.getFgSelecionada()))
                .toList();

        DevolucaoShippingAddressResponse addr = enderecoRepository
                .findByDevolucao_IdAndFgExcluidoFalse(d.getId())
                .map(e -> new DevolucaoShippingAddressResponse(
                        e.getNmDestinatario(), e.getNrCep(), e.getNmLogradouro(), e.getNrNumero(),
                        e.getDsComplemento(), e.getNmBairro(), e.getNmCidade(), e.getSgUf(), e.getNrTelefone()))
                .orElse(null);

        DevolucaoShippingQuoteResponse quote = cotacaoRepository
                .findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtInformadaDesc(d.getId())
                .map(c -> new DevolucaoShippingQuoteResponse(
                        c.getVlValor(), c.getSgMoeda(), c.getQtDiasEntregaEstimados(),
                        c.getQtDiasPrazoPostagem(), c.getDsInstrucoesPagamento(), c.getDtInformada()))
                .orElse(null);

        DevolucaoPaymentProofResponse proof = null;
        try {
            List<ArquivoResponse> arquivos = arquivoService.findByEntidade(
                    "DEVOLUCAO", idCodec.encodeDevolucaoId(d.getId()));
            proof = arquivos.stream()
                    .filter(a -> "COMPROVANTE_PAGAMENTO".equalsIgnoreCase(a.tpArquivo()))
                    .findFirst()
                    .map(a -> new DevolucaoPaymentProofResponse(
                            a.id(), a.nmArquivo(), a.dtCadastro(),
                            PAID_AWAITING_POSTING.equals(d.getTpStatus())
                                    || POSTED.equals(d.getTpStatus())
                                    || IN_TRANSIT.equals(d.getTpStatus())
                                    || DELIVERED.equals(d.getTpStatus())
                                    || COMPLETED.equals(d.getTpStatus())
                                    ? "RECEBIDO" : "ENVIADO"))
                    .orElse(null);
        } catch (RuntimeException ignored) {
            proof = null;
        }

        DevolucaoShippingPostingResponse posting = postagemRepository
                .findFirstByDevolucao_IdAndFgExcluidoFalseOrderByDtRegistroDesc(d.getId())
                .map(p -> new DevolucaoShippingPostingResponse(p.getDtPostagem(), p.getCdRastreio(), p.getDtRegistro()))
                .orElse(null);

        return new DevolucaoDetalheResponse(
                idCodec.encodeDevolucaoId(d.getId()),
                d.getCdProtocolo(),
                idCodec.encodeItemId(item.getId()),
                claim != null ? idCodec.encodeClaimId(claim.getId()) : null,
                d.getTpClaimOrigem() != null ? d.getTpClaimOrigem() : (claim != null ? claim.getTpClaim() : null),
                d.getTpMetodo(),
                d.getTpDevolucao(),
                d.getTpStatus(),
                d.getNmRecebedor(),
                claim != null ? claim.getNmEmail() : null,
                claim != null ? claim.getNrTelefone() : null,
                d.getNrCpf() != null ? d.getNrCpf() : (claim != null ? claim.getNrCpf() : null),
                item.getCdItem(),
                item.getNmTitulo(),
                item.getCategoria() != null ? item.getCategoria().getNmCategoria() : null,
                item.getNmLocalEncontrado(),
                d.getDsObservacao(),
                nextAction(d.getTpStatus()),
                allowedActions(d.getTpStatus()),
                options,
                addr,
                quote,
                proof,
                posting,
                mapHistorico(d.getId()));
    }

    private List<DevolucaoHistoricoItemResponse> mapHistorico(Long devolucaoId) {
        return historicoRepository.findByDevolucao_IdAndFgExcluidoFalseOrderByDtEventoDesc(devolucaoId)
                .stream()
                .map(h -> new DevolucaoHistoricoItemResponse(
                        idCodec.encodeDevolucaoHistoricoId(h.getId()),
                        h.getTpEvento(),
                        h.getNmTitulo(),
                        h.getDsDescricao(),
                        h.getTpAtor(),
                        h.getNmAtor(),
                        h.getDtEvento()))
                .toList();
    }

    private DevolucaoResponse toListResponse(Devolucao d) {
        Item item = d.getItem();
        Claim claim = d.getClaim();
        return new DevolucaoResponse(
                idCodec.encodeDevolucaoId(d.getId()),
                idCodec.encodeItemId(item.getId()),
                claim != null ? idCodec.encodeClaimId(claim.getId()) : null,
                item.getCdItem(),
                item.getNmTitulo(),
                item.getCategoria() != null ? item.getCategoria().getNmCategoria() : null,
                item.getNmLocalEncontrado(),
                d.getTpDevolucao(), d.getNmRecebedor(), d.getTpStatus(), d.getFgAssinado(), d.getFgConcluido(),
                d.getDtDevolucao(),
                d.getNrCpf() != null ? d.getNrCpf() : (claim != null ? claim.getNrCpf() : null),
                claim != null ? claim.getNmEmail() : null,
                claim != null ? claim.getNrTelefone() : null,
                item.getDtEncontrado(),
                d.getDsObservacao(),
                item.getTpPrioridade(),
                item.getFgSensivel(),
                d.getCdProtocolo(),
                d.getTpMetodo(),
                d.getTpClaimOrigem(),
                nextAction(d.getTpStatus()));
    }
}
