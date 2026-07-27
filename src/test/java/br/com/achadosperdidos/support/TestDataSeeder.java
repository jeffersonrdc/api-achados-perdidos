package br.com.achadosperdidos.support;

import br.com.achadosperdidos.entity.*;
import br.com.achadosperdidos.repository.*;
import br.com.achadosperdidos.security.SignedResourceIdCodec;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@TestComponent
public class TestDataSeeder {

    public record SeedData(
            String idCategoria,
            String idStatusRecebido,
            String idStatusClaimAberto,
            String idStatusEmEstoque
    ) {}

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final StatusItemRepository statusItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final SignedResourceIdCodec idCodec;

    public TestDataSeeder(PerfilRepository perfilRepository,
                          UsuarioRepository usuarioRepository,
                          CategoriaRepository categoriaRepository,
                          StatusItemRepository statusItemRepository,
                          PasswordEncoder passwordEncoder,
                          SignedResourceIdCodec idCodec) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.statusItemRepository = statusItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.idCodec = idCodec;
    }

    public SeedData seed() {
        LocalDateTime now = LocalDateTime.now();

        Perfil admin = savePerfil("Administrador", now);
        Perfil atendente = savePerfil("Atendente", now);
        savePerfil("Operador", now);
        savePerfil("Consulta", now);
        savePerfil("Participante", now);

        Usuario usuarioAdmin = new Usuario();
        usuarioAdmin.setPerfil(admin);
        usuarioAdmin.setNmUsuario("Administrador");
        usuarioAdmin.setNmLogin("admin");
        usuarioAdmin.setNmEmail("admin@teste.com");
        usuarioAdmin.setNmSenha(passwordEncoder.encode("admin123"));
        usuarioAdmin.setDtCadastro(now);
        usuarioAdmin.setFgAtivo(true);
        usuarioAdmin.setFgExcluido(false);
        usuarioRepository.save(usuarioAdmin);

        Usuario usuarioAtendente = new Usuario();
        usuarioAtendente.setPerfil(atendente);
        usuarioAtendente.setNmUsuario("Atendente");
        usuarioAtendente.setNmLogin("atendente");
        usuarioAtendente.setNmEmail("atendente@teste.com");
        usuarioAtendente.setNmSenha(passwordEncoder.encode("atendente123"));
        usuarioAtendente.setDtCadastro(now);
        usuarioAtendente.setFgAtivo(true);
        usuarioAtendente.setFgExcluido(false);
        usuarioRepository.save(usuarioAtendente);

        Categoria categoria = new Categoria();
        categoria.setNmCategoria("Celular");
        categoria.setDsCategoria("Telefones e smartphones");
        categoria.setOrOrdem(1);
        categoria.setDtCadastro(now);
        categoria.setFgAtivo(true);
        categoria.setFgExcluido(false);
        categoria = categoriaRepository.save(categoria);

        StatusItem recebido = saveStatus("Recebido", 1, now);
        StatusItem claimAberto = saveStatus("Claim Aberto", 2, now);
        // "Em estoque" faz parte de STATUS_PORTAL: itens só aparecem no catálogo
        // público depois da triagem, ao chegarem ao estoque.
        StatusItem emEstoque = saveStatus("Em estoque", 3, now);
        // Status do motor de match (script 052) — usados ao criar/atualizar claim PERDA.
        saveStatus("Aguardando Match", 91, now);
        saveStatus("Match", 92, now);

        return new SeedData(
                idCodec.encodeCategoriaId(categoria.getId()),
                idCodec.encodeStatusId(recebido.getId()),
                idCodec.encodeStatusId(claimAberto.getId()),
                idCodec.encodeStatusId(emEstoque.getId())
        );
    }

    private Perfil savePerfil(String nome, LocalDateTime now) {
        Perfil perfil = new Perfil();
        perfil.setNmPerfil(nome);
        perfil.setDsPerfil(nome);
        perfil.setDtCadastro(now);
        perfil.setFgAtivo(true);
        perfil.setFgExcluido(false);
        return perfilRepository.save(perfil);
    }

    private StatusItem saveStatus(String nome, int ordem, LocalDateTime now) {
        StatusItem status = new StatusItem();
        status.setNmStatus(nome);
        status.setOrOrdem(ordem);
        status.setFgFinal(false);
        status.setFgAtivo(true);
        status.setFgExcluido(false);
        return statusItemRepository.save(status);
    }
}
