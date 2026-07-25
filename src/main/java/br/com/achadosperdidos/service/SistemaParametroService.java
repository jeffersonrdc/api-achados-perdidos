package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.SistemaParametro;
import br.com.achadosperdidos.repository.SistemaParametroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SistemaParametroService {
    public static final String ARQUIVO_STORAGE_PROVIDER = "ARQUIVO_STORAGE_PROVIDER";
    public static final String PORTAL_TELEFONE_CENTRAL = "PORTAL_TELEFONE_CENTRAL";
    public static final String PORTAL_WHATSAPP = "PORTAL_WHATSAPP";
    public static final String PORTAL_EMAIL_SUPORTE = "PORTAL_EMAIL_SUPORTE";

    private final SistemaParametroRepository repository;

    public SistemaParametroService(SistemaParametroRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String get(String chave, String padrao) {
        return repository.findById(chave)
                .map(SistemaParametro::getDsValor)
                .filter(v -> v != null && !v.isBlank())
                .orElse(padrao);
    }

    @Transactional
    public void set(String chave, String valor, String descricao) {
        SistemaParametro p = repository.findById(chave).orElseGet(SistemaParametro::new);
        if (p.getNmChave() == null) {
            p.setNmChave(chave);
            p.setDtCadastro(LocalDateTime.now());
            p.setDsDescricao(descricao);
        }
        p.setDsValor(valor);
        p.setDtAlteracao(LocalDateTime.now());
        if (descricao != null && !descricao.isBlank()) {
            p.setDsDescricao(descricao);
        }
        repository.save(p);
    }
}
