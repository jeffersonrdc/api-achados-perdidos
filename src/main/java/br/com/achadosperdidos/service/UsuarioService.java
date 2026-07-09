package br.com.achadosperdidos.service;

import br.com.achadosperdidos.controller.dto.UsuarioResumoResponse;
import br.com.achadosperdidos.entity.Usuario;
import br.com.achadosperdidos.repository.UsuarioRepository;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final SignedResourceIdCodec idCodec;
    public UsuarioService(UsuarioRepository usuarioRepository, SignedResourceIdCodec idCodec) {
        this.usuarioRepository = usuarioRepository; this.idCodec = idCodec;
    }
    @Transactional(readOnly = true)
    public UsuarioResumoResponse toResumo(Usuario usuario) {
        return new UsuarioResumoResponse(
                idCodec.encodeUsuarioId(usuario.getId()),
                usuario.getNmUsuario(),
                usuario.getNmEmail(),
                usuario.getNmLogin(),
                usuario.getPerfil().getNmPerfil());
    }
    @Transactional(readOnly = true)
    public UsuarioResumoResponse findResumoByEmail(String email) {
        return usuarioRepository.findWithPerfilByNmEmail(email).map(this::toResumo)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }
    @Transactional(readOnly = true)
    public UsuarioResumoResponse findResumoByIdentificador(String identificador) {
        return usuarioRepository.findWithPerfilByNmEmail(identificador)
                .or(() -> usuarioRepository.findWithPerfilByNmLogin(identificador))
                .map(this::toResumo)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
    }
}
