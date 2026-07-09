package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.ArquivoCreateRequest;
import br.com.achadosperdidos.controller.dto.ArquivoResponse;
import br.com.achadosperdidos.entity.Arquivo;
import br.com.achadosperdidos.repository.ArquivoRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArquivoService {
    private final ArquivoRepository arquivoRepository;
    private final SignedResourceIdCodec idCodec;

    public ArquivoService(ArquivoRepository arquivoRepository, SignedResourceIdCodec idCodec) {
        this.arquivoRepository = arquivoRepository;
        this.idCodec = idCodec;
    }

    @Transactional
    public ArquivoResponse create(ArquivoCreateRequest request) {
        Arquivo a = new Arquivo();
        String tpEntidade = request.tpEntidade().trim().toUpperCase();
        a.setTpEntidade(tpEntidade);
        a.setIdEntidade(idCodec.decodeEntidadeId(tpEntidade, request.idEntidade()));
        a.setTpArquivo(request.tpArquivo().trim().toUpperCase());
        a.setNmArquivo(request.nmArquivo());
        a.setNmPath(request.nmPath());
        a.setTpMime(request.tpMime());
        a.setFgPrincipal(Boolean.TRUE.equals(request.fgPrincipal()));
        a.setQtBytes(request.qtBytes());
        a.setDtCadastro(LocalDateTime.now());
        a.setFgAtivo(true);
        a.setFgExcluido(false);
        return toResponse(arquivoRepository.save(a));
    }

    @Transactional(readOnly = true)
    public List<ArquivoResponse> findByEntidade(String tpEntidade, String idEntidade) {
        String tipo = tpEntidade.toUpperCase();
        return arquivoRepository.findByTpEntidadeAndIdEntidadeAndFgExcluidoFalseOrderByDtCadastroDesc(
                        tipo, idCodec.decodeEntidadeId(tipo, idEntidade))
                .stream().map(this::toResponse).toList();
    }

    private ArquivoResponse toResponse(Arquivo a) {
        return new ArquivoResponse(
                idCodec.encodeArquivoId(a.getId()),
                a.getTpEntidade(),
                idCodec.encodeEntidadeId(a.getTpEntidade(), a.getIdEntidade()),
                a.getTpArquivo(),
                a.getNmArquivo(),
                a.getNmPath(),
                a.getTpMime(),
                a.getFgPrincipal(),
                a.getQtBytes(),
                a.getDtCadastro());
    }
}
