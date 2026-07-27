package br.com.achadosperdidos.service;

import br.com.achadosperdidos.config.TimeConfig;
import br.com.achadosperdidos.entity.Devolucao;
import br.com.achadosperdidos.entity.DevolucaoHistorico;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.DevolucaoHistoricoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevolucaoHistoricoService {
    private final DevolucaoHistoricoRepository historicoRepository;
    private final UsuarioContextService usuarioContextService;

    public DevolucaoHistoricoService(DevolucaoHistoricoRepository historicoRepository,
                                     UsuarioContextService usuarioContextService) {
        this.historicoRepository = historicoRepository;
        this.usuarioContextService = usuarioContextService;
    }

    @Transactional
    public DevolucaoHistorico registrar(Devolucao devolucao, String tpEvento, String titulo, String descricao,
                                        String atorType, Usuario operador, EmailService.Resultado emailResult,
                                        String metadataJson) {
        DevolucaoHistorico h = new DevolucaoHistorico();
        h.setDevolucao(devolucao);
        h.setTpEvento(tpEvento);
        h.setNmTitulo(titulo);
        h.setDsDescricao(descricao);
        h.setTpAtor(atorType);
        Usuario op = operador;
        if (op == null && "OPERADOR".equalsIgnoreCase(atorType)) {
            op = usuarioContextService.findUsuarioLogado().orElse(null);
        }
        h.setOperador(op);
        if (op != null) {
            h.setNmAtor(op.getNmUsuario());
        } else if ("SOLICITANTE".equalsIgnoreCase(atorType)) {
            h.setNmAtor(devolucao.getNmRecebedor());
        } else if ("SISTEMA".equalsIgnoreCase(atorType)) {
            h.setNmAtor("Sistema");
        }
        h.setFgEmailEnviado(emailResult != null && emailResult.enviado());
        h.setDsEmailErro(emailResult != null ? EmailService.truncarErro(emailResult.erro()) : null);
        h.setJsMetadata(metadataJson);
        h.setDtEvento(TimeConfig.now());
        h.setFgExcluido(false);
        return historicoRepository.save(h);
    }
}
