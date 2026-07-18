package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.EmailConfigRequest;
import br.com.achadosperdidos.controller.dto.EmailConfigResponse;
import br.com.achadosperdidos.controller.dto.EmailParametroResponse;
import br.com.achadosperdidos.controller.dto.EmailParametroUpdateRequest;
import br.com.achadosperdidos.controller.dto.SmtpTesteResponse;
import br.com.achadosperdidos.entity.EmailConfig;
import br.com.achadosperdidos.entity.EmailParametro;
import br.com.achadosperdidos.exception.RecursoNaoEncontradoException;
import br.com.achadosperdidos.repository.EmailConfigRepository;
import br.com.achadosperdidos.repository.EmailParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** CRUD das contas SMTP (email_config) e dos parâmetros de envio (email_parametro). */
@Service
public class ClaimConfigService {
    private final EmailConfigRepository configRepository;
    private final EmailParametroRepository parametroRepository;
    private final EmailService emailService;

    public ClaimConfigService(EmailConfigRepository configRepository, EmailParametroRepository parametroRepository,
                              EmailService emailService) {
        this.configRepository = configRepository;
        this.parametroRepository = parametroRepository;
        this.emailService = emailService;
    }

    // ---- Contas SMTP ----
    @Transactional(readOnly = true)
    public List<EmailConfigResponse> listarConfigs() {
        return configRepository.findByFgExcluidoFalseOrderByNmConfigAsc().stream().map(this::toConfigResponse).toList();
    }

    @Transactional
    public EmailConfigResponse criarConfig(EmailConfigRequest req) {
        EmailConfig c = new EmailConfig();
        aplicar(c, req, true);
        c.setDtCadastro(LocalDateTime.now());
        c.setFgExcluido(false);
        return toConfigResponse(configRepository.save(c));
    }

    @Transactional
    public EmailConfigResponse atualizarConfig(String id, EmailConfigRequest req) {
        EmailConfig c = findConfig(id);
        aplicar(c, req, false);
        c.setDtAlteracao(LocalDateTime.now());
        return toConfigResponse(configRepository.save(c));
    }

    @Transactional
    public void excluirConfig(String id) {
        EmailConfig c = findConfig(id);
        c.setFgExcluido(true);
        c.setFgAtivo(false);
        c.setDtAlteracao(LocalDateTime.now());
        configRepository.save(c);
    }

    @Transactional(readOnly = true)
    public SmtpTesteResponse testarConexao(String id, EmailConfigRequest req) {
        EmailConfig teste = new EmailConfig();
        aplicar(teste, req, true);
        if ((req.nmSenha() == null || req.nmSenha().isBlank()) && id != null && !id.isBlank()) {
            teste.setNmSenha(findConfig(id).getNmSenha());
        }
        EmailService.Resultado resultado = emailService.testarConexao(teste);
        return resultado.enviado()
                ? new SmtpTesteResponse(true, "Conexão SMTP realizada com sucesso.")
                : new SmtpTesteResponse(false, resultado.erro());
    }

    private void aplicar(EmailConfig c, EmailConfigRequest req, boolean novo) {
        c.setNmConfig(req.nmConfig().trim());
        c.setNmHost(req.nmHost());
        c.setNrPorta(req.nrPorta());
        c.setNmUsuario(req.nmUsuario());
        // Só sobrescreve a senha quando informada (mantém a atual em edições sem senha).
        if (req.nmSenha() != null && !req.nmSenha().isBlank()) c.setNmSenha(req.nmSenha());
        c.setNmRemetente(req.nmRemetente());
        c.setNmRemetenteNome(req.nmRemetenteNome());
        c.setFgTls(req.fgTls() == null ? Boolean.TRUE : req.fgTls());
        c.setFgAtivo(req.fgAtivo() == null ? Boolean.TRUE : req.fgAtivo());
    }

    private EmailConfig findConfig(String id) {
        Long pk;
        try { pk = Long.parseLong(id.trim()); } catch (NumberFormatException e) {
            throw new RecursoNaoEncontradoException("Conta de e-mail não encontrada.");
        }
        return configRepository.findById(pk)
                .filter(c -> !Boolean.TRUE.equals(c.getFgExcluido()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta de e-mail não encontrada."));
    }

    private EmailConfigResponse toConfigResponse(EmailConfig c) {
        return new EmailConfigResponse(
                String.valueOf(c.getId()), c.getNmConfig(), c.getNmHost(), c.getNrPorta(), c.getNmUsuario(),
                c.getNmRemetente(), c.getNmRemetenteNome(), c.getFgTls(), c.getFgAtivo(),
                c.getNmSenha() != null && !c.getNmSenha().isBlank());
    }

    // ---- Parâmetros (propósito -> conta) ----
    @Transactional(readOnly = true)
    public List<EmailParametroResponse> listarParametros() {
        return parametroRepository.findAllByOrderByTpEventoAsc().stream().map(this::toParametroResponse).toList();
    }

    @Transactional
    public List<EmailParametroResponse> salvarParametros(List<EmailParametroUpdateRequest> reqs) {
        for (EmailParametroUpdateRequest req : reqs) {
            EmailParametro p = parametroRepository.findByTpEvento(req.tpEvento()).orElse(null);
            if (p == null) continue; // ignora chaves desconhecidas
            EmailConfig config = null;
            if (req.idEmailConfig() != null && !req.idEmailConfig().isBlank()) {
                config = findConfig(req.idEmailConfig());
            }
            p.setEmailConfig(config);
            if (req.nmAssunto() != null && !req.nmAssunto().isBlank()) p.setNmAssunto(req.nmAssunto());
            p.setDtAlteracao(LocalDateTime.now());
            parametroRepository.save(p);
        }
        return listarParametros();
    }

    private EmailParametroResponse toParametroResponse(EmailParametro p) {
        EmailConfig c = p.getEmailConfig();
        return new EmailParametroResponse(
                p.getTpEvento(),
                c != null ? String.valueOf(c.getId()) : null,
                c != null ? c.getNmConfig() : null,
                p.getNmTemplate(),
                p.getNmAssunto());
    }
}
