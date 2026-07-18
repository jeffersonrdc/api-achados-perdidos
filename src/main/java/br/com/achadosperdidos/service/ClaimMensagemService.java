package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.controller.dto.ClaimMensagemCreateRequest;
import br.com.achadosperdidos.controller.dto.ClaimMensagemResponse;
import br.com.achadosperdidos.controller.dto.ClaimResponse;
import br.com.achadosperdidos.controller.dto.PortalRespostaContextResponse;
import br.com.achadosperdidos.controller.dto.PortalRespostaSubmitResponse;
import br.com.achadosperdidos.entity.Claim;
import br.com.achadosperdidos.entity.ClaimHistorico;
import br.com.achadosperdidos.entity.ClaimMensagem;
import br.com.achadosperdidos.entity.ClaimRespostaToken;
import br.com.achadosperdidos.entity.EventoConfiguracao;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.exception.LinkExpiradoException;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.ClaimHistoricoRepository;
import br.com.achadosperdidos.repository.ClaimMensagemRepository;
import br.com.achadosperdidos.repository.ClaimRepository;
import br.com.achadosperdidos.repository.ClaimRespostaTokenRepository;
import br.com.achadosperdidos.repository.EventoConfiguracaoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversa operador/solicitante e magic link de resposta (/responder-email).
 * Cada envio do operador rota o token e invalida links anteriores.
 */
@Service
public class ClaimMensagemService {
    private static final int PRAZO_PADRAO_DIAS = 15;
    private static final int MAX_IMAGENS = 5;
    private static final long MAX_BYTES_IMAGEM = 5L * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClaimRepository claimRepository;
    private final ClaimMensagemRepository mensagemRepository;
    private final ClaimRespostaTokenRepository tokenRepository;
    private final ClaimHistoricoRepository historicoRepository;
    private final EventoConfiguracaoRepository eventoConfiguracaoRepository;
    private final ArquivoService arquivoService;
    private final EmailService emailService;
    private final StatusItemService statusItemService;
    private final ClaimService claimService;
    private final UsuarioContextService usuarioContextService;
    private final SignedResourceIdCodec idCodec;

    @Value("${app.portal.base-url:http://localhost:4300}")
    private String portalBaseUrl;

    public ClaimMensagemService(ClaimRepository claimRepository,
                                ClaimMensagemRepository mensagemRepository,
                                ClaimRespostaTokenRepository tokenRepository,
                                ClaimHistoricoRepository historicoRepository,
                                EventoConfiguracaoRepository eventoConfiguracaoRepository,
                                ArquivoService arquivoService,
                                EmailService emailService,
                                StatusItemService statusItemService,
                                ClaimService claimService,
                                UsuarioContextService usuarioContextService,
                                SignedResourceIdCodec idCodec) {
        this.claimRepository = claimRepository;
        this.mensagemRepository = mensagemRepository;
        this.tokenRepository = tokenRepository;
        this.historicoRepository = historicoRepository;
        this.eventoConfiguracaoRepository = eventoConfiguracaoRepository;
        this.arquivoService = arquivoService;
        this.emailService = emailService;
        this.statusItemService = statusItemService;
        this.claimService = claimService;
        this.usuarioContextService = usuarioContextService;
        this.idCodec = idCodec;
    }

    /** Lista a conversa e marca mensagens do solicitante como vistas pelo operador. */
    @Transactional
    public List<ClaimMensagemResponse> listar(String idClaim) {
        Long claimId = idCodec.decodeClaimId(idClaim);
        List<ClaimMensagemResponse> msgs = mensagemRepository.findByClaim_IdAndFgExcluidoFalseOrderByIdAsc(claimId)
                .stream().map(this::toResponse).toList();
        mensagemRepository.marcarLidasPeloOperador(claimId);
        return msgs;
    }

    /** Mensagem do operador via aba Conversa: persiste, e-mail, renova token. */
    @Transactional
    public ClaimMensagemResponse enviarOperador(String idClaim, ClaimMensagemCreateRequest req) {
        Claim claim = findClaim(idClaim);
        String texto = req.dsMensagem().trim();
        claim.setStatus(statusItemService.findByNomeOrDefault(null, "Claim Aguardando Info"));
        claim.setDtAlteracao(agora());
        claimRepository.save(claim);

        ClaimMensagem msg = criarMensagemOperador(claim, texto);
        ClaimRespostaToken token = rotacionarToken(claim, msg);
        EmailService.Resultado email = enviarEmailSolicitacao(claim, texto, token, "informações adicionais");
        atualizarResultadoEmail(msg, email);
        gravarHistorico(claim, "SOLICITACAO_INFO", "PERGUNTA", texto, email);
        return toResponse(msg);
    }

    /**
     * Usado por solicitar-info: cria mensagem + token + e-mail com o mesmo CTA.
     * Retorna o claim atualizado.
     */
    @Transactional
    public ClaimResponse solicitarInfoComConversa(String idClaim, String tpSolicitacao, String detalhe) {
        Claim claim = findClaim(idClaim);
        String tpSolic = tpSolicitacao.trim().toUpperCase();
        String texto = detalhe.trim();
        claim.setStatus(statusItemService.findByNomeOrDefault(null, "Claim Aguardando Info"));
        claim.setDtAlteracao(agora());
        claimRepository.save(claim);

        ClaimMensagem msg = criarMensagemOperador(claim, texto);
        ClaimRespostaToken token = rotacionarToken(claim, msg);
        String tipo = "IMAGEM".equals(tpSolic) ? "imagens de comprovação" : "informações adicionais";
        EmailService.Resultado email = enviarEmailSolicitacao(claim, texto, token, tipo);
        atualizarResultadoEmail(msg, email);
        gravarHistorico(claim, "SOLICITACAO_INFO", tpSolic, texto, email);
        return claimService.findById(idCodec.encodeClaimId(claim.getId()));
    }

    @Transactional(readOnly = true)
    public PortalRespostaContextResponse contextoPublico(String cdToken) {
        ClaimRespostaToken token = encontrarToken(cdToken);
        Claim claim = token.getClaim();
        boolean used = isUsado(token);
        boolean expiredByDate = isExpiradoPorPrazo(token);
        boolean expired = used || expiredByDate || !Boolean.TRUE.equals(token.getFgAtivo());
        String pergunta = token.getMensagem() != null ? token.getMensagem().getDsMensagem() : "";
        int prazo = resolverPrazoDias(claim.getEvento().getId());
        return new PortalRespostaContextResponse(
                claim.getCdClaim() != null ? claim.getCdClaim() : "",
                claim.getNmObjeto() != null ? claim.getNmObjeto() : "",
                claim.getEvento() != null ? claim.getEvento().getNmEvento() : "",
                pergunta,
                token.getDtExpiracao(),
                expired,
                used,
                prazo);
    }

    @Transactional
    public PortalRespostaSubmitResponse responderPublico(String cdToken, String dsMensagem, List<MultipartFile> imagens) {
        ClaimRespostaToken token = encontrarToken(cdToken);
        if (isExpiradoOuInativo(token)) {
            throw new LinkExpiradoException(
                    "Este link expirou ou não é mais válido. Aguarde um novo e-mail da equipe ou entre em contato.");
        }
        String texto = dsMensagem == null ? "" : dsMensagem.trim();
        if (texto.isBlank()) {
            throw new IllegalArgumentException("Informe a mensagem de resposta.");
        }
        List<MultipartFile> files = imagens == null ? List.of() : imagens.stream()
                .filter(f -> f != null && !f.isEmpty()).toList();
        if (files.size() > MAX_IMAGENS) {
            throw new IllegalArgumentException("Envie no máximo " + MAX_IMAGENS + " imagens.");
        }
        for (MultipartFile f : files) {
            if (f.getSize() > MAX_BYTES_IMAGEM) {
                throw new IllegalArgumentException("Cada imagem deve ter no máximo 5 MB.");
            }
            String mime = f.getContentType() == null ? "" : f.getContentType().toLowerCase();
            if (!mime.startsWith("image/jpeg") && !mime.equals("image/png") && !mime.equals("image/pjpeg")) {
                throw new IllegalArgumentException("Envie apenas imagens JPEG ou PNG.");
            }
        }

        Claim claim = token.getClaim();
        ClaimMensagem msg = new ClaimMensagem();
        msg.setClaim(claim);
        msg.setTpAutor("SOLICITANTE");
        msg.setDsMensagem(texto);
        msg.setOperador(null);
        msg.setFgEmailEnviado(false);
        msg.setFgLidaOperador(false);
        msg.setDtMensagem(agora());
        msg.setFgExcluido(false);
        msg = mensagemRepository.save(msg);

        for (MultipartFile f : files) {
            arquivoService.upload("CLAIM_MENSAGEM", idCodec.encodeClaimMensagemId(msg.getId()), "IMAGEM", f, false);
        }

        token.setFgAtivo(false);
        token.setDtUsado(agora());
        tokenRepository.save(token);

        claim.setStatus(statusItemService.findByNomeOrDefault(null, "Claim em Análise"));
        claim.setDtAlteracao(agora());
        claimRepository.save(claim);

        gravarHistorico(claim, "RESPOSTA_INFO", null, texto, null);

        return new PortalRespostaSubmitResponse(
                claim.getCdClaim() != null ? claim.getCdClaim() : "",
                "Resposta recebida com sucesso. A equipe continuará a análise.");
    }

    // ------------------------------------------------------------------

    private ClaimMensagem criarMensagemOperador(Claim claim, String texto) {
        ClaimMensagem msg = new ClaimMensagem();
        msg.setClaim(claim);
        msg.setTpAutor("OPERADOR");
        msg.setDsMensagem(texto);
        msg.setOperador(usuarioLogadoOuNulo());
        msg.setFgEmailEnviado(false);
        msg.setFgLidaOperador(true);
        msg.setDtMensagem(agora());
        msg.setFgExcluido(false);
        return mensagemRepository.save(msg);
    }

    private ClaimRespostaToken rotacionarToken(Claim claim, ClaimMensagem msg) {
        // Resolver lazy associations ANTES do UPDATE (@Modifying), que pode flushar o contexto.
        Long eventoId = claim.getEvento().getId();
        int prazo = resolverPrazoDias(eventoId);
        tokenRepository.invalidarAtivosDoClaim(claim.getId());
        ClaimRespostaToken token = new ClaimRespostaToken();
        token.setClaim(claim);
        token.setMensagem(msg);
        token.setCdToken(gerarToken());
        token.setDtExpiracao(agora().plusDays(prazo));
        token.setFgAtivo(true);
        token.setDtCadastro(agora());
        token.setFgExcluido(false);
        return tokenRepository.save(token);
    }

    private EmailService.Resultado enviarEmailSolicitacao(Claim claim, String detalhe,
                                                          ClaimRespostaToken token, String tipoSolicitacao) {
        // Materializa associações lazy usadas nas variáveis do template.
        String nmEvento = claim.getEvento() != null ? claim.getEvento().getNmEvento() : "";
        Integer ano = claim.getEvento() != null && claim.getEvento().getDtInicio() != null
                ? claim.getEvento().getDtInicio().getYear() : null;
        int prazo = claim.getEvento() != null ? resolverPrazoDias(claim.getEvento().getId()) : PRAZO_PADRAO_DIAS;

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("nomeSolicitante", claim.getNmNome() != null ? claim.getNmNome() : "");
        vars.put("objeto", claim.getNmObjeto() != null ? claim.getNmObjeto() : "");
        vars.put("evento", nmEvento);
        vars.put("ano", ano != null ? String.valueOf(ano) : "");
        vars.put("protocolo", claim.getCdClaim() != null ? claim.getCdClaim() : "");
        vars.put("detalhe", detalhe != null ? detalhe : "");
        vars.put("motivo", "");
        vars.put("tipoSolicitacao", tipoSolicitacao);
        vars.put("linkResposta", montarLinkResposta(token.getCdToken()));
        vars.put("prazoDias", String.valueOf(prazo));
        return emailService.enviar("CLAIM_SOLICITACAO_INFO", claim.getNmEmail(), vars);
    }

    private String montarLinkResposta(String cdToken) {
        String base = portalBaseUrl == null ? "" : portalBaseUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/responder-email?token=" + cdToken;
    }

    private int resolverPrazoDias(Long eventoId) {
        return eventoConfiguracaoRepository.findByEvento_IdAndFgExcluidoFalse(eventoId)
                .map(EventoConfiguracao::getQtDiasEsperaAceitavel)
                .filter(v -> v != null && v > 0)
                .orElse(PRAZO_PADRAO_DIAS);
    }

    private ClaimRespostaToken encontrarToken(String cdToken) {
        if (cdToken == null || cdToken.isBlank()) {
            throw new RecursoNaoEncontradoException("Link de resposta inválido.");
        }
        return tokenRepository.findByCdTokenAndFgExcluidoFalse(cdToken.trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de resposta inválido."));
    }

    private boolean isExpiradoOuInativo(ClaimRespostaToken token) {
        if (isUsado(token)) return true;
        if (!Boolean.TRUE.equals(token.getFgAtivo())) return true;
        return isExpiradoPorPrazo(token);
    }

    /** Token já foi usado para enviar uma resposta. */
    private boolean isUsado(ClaimRespostaToken token) {
        return token.getDtUsado() != null;
    }

    private boolean isExpiradoPorPrazo(ClaimRespostaToken token) {
        return token.getDtExpiracao() != null && token.getDtExpiracao().isBefore(agora());
    }

    /** Agora em America/Sao_Paulo (Brasília) — evita gravar UTC (+3h). */
    private LocalDateTime agora() {
        return TimeConfig.now();
    }

    private void atualizarResultadoEmail(ClaimMensagem msg, EmailService.Resultado email) {
        msg.setFgEmailEnviado(email != null && email.enviado());
        msg.setDsEmailErro(email != null ? EmailService.truncarErro(email.erro()) : null);
        mensagemRepository.save(msg);
    }

    private void gravarHistorico(Claim claim, String tpEvento, String tpSolicitacao,
                                 String detalhe, EmailService.Resultado email) {
        ClaimHistorico h = new ClaimHistorico();
        h.setClaim(claim);
        h.setTpEvento(tpEvento);
        h.setTpSolicitacao(tpSolicitacao);
        h.setDsDetalhe(detalhe);
        h.setOperador(usuarioLogadoOuNulo());
        h.setFgEmailEnviado(email != null && email.enviado());
        h.setDsEmailErro(email != null ? EmailService.truncarErro(email.erro()) : null);
        h.setDtHistorico(agora());
        h.setFgExcluido(false);
        historicoRepository.save(h);
    }

    private ClaimMensagemResponse toResponse(ClaimMensagem m) {
        List<ArquivoResponse> anexos = List.of();
        try {
            anexos = arquivoService.findByEntidade(
                    "CLAIM_MENSAGEM", idCodec.encodeClaimMensagemId(m.getId()));
        } catch (RuntimeException ex) {
            // Mantém a thread mesmo se um anexo falhar ao carregar.
            anexos = List.of();
        }
        return new ClaimMensagemResponse(
                idCodec.encodeClaimMensagemId(m.getId()),
                m.getTpAutor(),
                m.getDsMensagem(),
                m.getOperador() != null ? m.getOperador().getNmUsuario() : null,
                m.getFgEmailEnviado(),
                m.getDsEmailErro(),
                m.getDtMensagem(),
                anexos);
    }

    private Claim findClaim(String idClaim) {
        return claimRepository.findById(idCodec.decodeClaimId(idClaim))
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido (claim) não encontrado."));
    }

    private Usuario usuarioLogadoOuNulo() {
        return usuarioContextService.findUsuarioLogado().orElse(null);
    }

    private static String gerarToken() {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
