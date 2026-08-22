package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
import br.com.achadosperdidos.entity.Devolucao;
import br.com.achadosperdidos.entity.DevolucaoAcaoToken;
import br.com.achadosperdidos.exception.LinkExpiradoException;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.DevolucaoAcaoTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class DevolucaoTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DevolucaoAcaoTokenRepository tokenRepository;

    public DevolucaoTokenService(DevolucaoAcaoTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public DevolucaoAcaoToken gerar(Devolucao devolucao, String tpAcao, int ttlDays, boolean multiUso) {
        invalidarAtivos(devolucao, tpAcao);
        DevolucaoAcaoToken token = new DevolucaoAcaoToken();
        token.setDevolucao(devolucao);
        token.setTpAcao(tpAcao);
        token.setCdToken(gerarToken());
        token.setDtExpiracao(TimeConfig.now().plusDays(Math.max(1, ttlDays)));
        token.setFgAtivo(true);
        token.setFgMultiUso(multiUso);
        token.setDtCadastro(TimeConfig.now());
        token.setFgExcluido(false);
        return tokenRepository.save(token);
    }

    @Transactional
    public void invalidarAtivos(Devolucao devolucao, String tpAcao) {
        tokenRepository.invalidarAtivos(devolucao.getId(), tpAcao);
    }

    /**
     * Busca sem exceção — para quem trata "token inexistente" como resultado normal
     * (ex.: a tela pública, que responde "Link inválido." em vez de erro).
     *
     * <p>Existe porque capturar a {@link RecursoNaoEncontradoException} lançada por
     * {@link #resolver(String)} não funciona entre beans transacionais: o proxy marca a
     * transação compartilhada como <i>rollback-only</i> ao ver a exceção, e o commit do
     * chamador estoura {@code UnexpectedRollbackException} (HTTP 500) mesmo com o
     * {@code catch} no lugar.</p>
     */
    @Transactional(readOnly = true)
    public Optional<DevolucaoAcaoToken> buscar(String cdToken) {
        if (cdToken == null || cdToken.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findByCdTokenAndFgExcluidoFalse(cdToken.trim());
    }

    @Transactional(readOnly = true)
    public DevolucaoAcaoToken resolver(String cdToken) {
        return buscar(cdToken)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Link de devolução inválido."));
    }

    /** Valida token para ação mutável; lança 410 se expirado/usado/inativo. */
    @Transactional
    public DevolucaoAcaoToken resolverParaUso(String cdToken) {
        DevolucaoAcaoToken token = resolver(cdToken);
        if (isUsado(token) && !Boolean.TRUE.equals(token.getFgMultiUso())) {
            throw new LinkExpiradoException("Este link já foi utilizado.");
        }
        if (!Boolean.TRUE.equals(token.getFgAtivo())) {
            throw new LinkExpiradoException("Este link não é mais válido.");
        }
        if (token.getDtExpiracao() != null && token.getDtExpiracao().isBefore(TimeConfig.now())) {
            throw new LinkExpiradoException("Este link expirou.");
        }
        return token;
    }

    @Transactional
    public void marcarUsado(DevolucaoAcaoToken token) {
        if (Boolean.TRUE.equals(token.getFgMultiUso())) {
            token.setDtUsado(TimeConfig.now());
            tokenRepository.save(token);
            return;
        }
        token.setFgAtivo(false);
        token.setDtUsado(TimeConfig.now());
        tokenRepository.save(token);
    }

    public boolean isUsado(DevolucaoAcaoToken token) {
        return token.getDtUsado() != null && !Boolean.TRUE.equals(token.getFgMultiUso());
    }

    public boolean isExpirado(DevolucaoAcaoToken token) {
        return token.getDtExpiracao() != null && token.getDtExpiracao().isBefore(TimeConfig.now());
    }

    public String statusToken(DevolucaoAcaoToken token) {
        if (token == null) return "invalid";
        if (isUsado(token)) return "used";
        if (!Boolean.TRUE.equals(token.getFgAtivo()) || isExpirado(token)) return "expired";
        return "valid";
    }

    private static String gerarToken() {
        byte[] buf = new byte[32];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
