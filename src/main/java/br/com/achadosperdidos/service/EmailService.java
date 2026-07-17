package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.EmailConfig;
import br.com.achadosperdidos.entity.EmailParametro;
import br.com.achadosperdidos.repository.EmailParametroRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Properties;

/**
 * Envio de e-mail do fluxo de claims. Para cada propósito (TP_Evento) busca o
 * parâmetro -> conta SMTP e envia. Se não houver conta configurada (dev), NÃO
 * quebra: retorna um resultado "não enviado" para ser registrado no histórico.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailParametroRepository parametroRepository;
    private final EmailTemplateService templateService;

    public EmailService(EmailParametroRepository parametroRepository, EmailTemplateService templateService) {
        this.parametroRepository = parametroRepository;
        this.templateService = templateService;
    }

    /** Resultado do envio: enviado=false quando cai no fallback (dev) ou falha. */
    public record Resultado(boolean enviado, String erro) {}

    @Transactional(readOnly = true)
    public Resultado enviar(String tpEvento, String destinatario, Map<String, String> variaveis) {
        EmailParametro parametro = parametroRepository.findByTpEvento(tpEvento).orElse(null);
        if (parametro == null) {
            return naoEnviado("Sem parâmetro de e-mail para " + tpEvento);
        }
        if (destinatario == null || destinatario.isBlank()) {
            return naoEnviado("Solicitante sem e-mail cadastrado");
        }
        EmailConfig config = parametro.getEmailConfig();
        if (config == null || config.getNmHost() == null || config.getNmHost().isBlank()
                || !Boolean.TRUE.equals(config.getFgAtivo())) {
            return naoEnviado("SMTP não configurado para " + tpEvento + " (e-mail não enviado)");
        }

        String assunto = parametro.getNmAssunto() != null ? parametro.getNmAssunto() : "Atualização do seu pedido";
        try {
            String html = templateService.render(parametro.getNmTemplate(), variaveis);
            JavaMailSenderImpl sender = montarSender(config);
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            String from = config.getNmRemetente() != null && !config.getNmRemetente().isBlank()
                    ? config.getNmRemetente() : config.getNmUsuario();
            if (config.getNmRemetenteNome() != null && !config.getNmRemetenteNome().isBlank()) {
                helper.setFrom(from, config.getNmRemetenteNome());
            } else {
                helper.setFrom(from);
            }
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(html, true);
            sender.send(msg);
            log.info("E-mail '{}' enviado para {}", tpEvento, destinatario);
            return new Resultado(true, null);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail '{}' para {}: {}", tpEvento, destinatario, e.getMessage());
            return naoEnviado("Falha no envio: " + e.getMessage());
        }
    }

    private JavaMailSenderImpl montarSender(EmailConfig config) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getNmHost());
        sender.setPort(config.getNrPorta() != null ? config.getNrPorta() : 587);
        if (config.getNmUsuario() != null && !config.getNmUsuario().isBlank()) sender.setUsername(config.getNmUsuario());
        if (config.getNmSenha() != null && !config.getNmSenha().isBlank()) sender.setPassword(config.getNmSenha());
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", config.getNmUsuario() != null && !config.getNmUsuario().isBlank() ? "true" : "false");
        props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(config.getFgTls())));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private Resultado naoEnviado(String motivo) {
        log.info("E-mail não enviado: {}", motivo);
        return new Resultado(false, motivo);
    }

    /** Trunca o motivo do erro para caber na coluna DS_EmailErro. */
    public static String truncarErro(String erro) {
        if (erro == null) return null;
        return erro.length() > 500 ? erro.substring(0, 500) : erro;
    }
}
