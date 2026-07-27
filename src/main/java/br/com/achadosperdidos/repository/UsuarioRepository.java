package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    @EntityGraph(attributePaths = {"perfil"})
    Optional<Usuario> findWithPerfilByNmEmail(String nmEmail);
    @EntityGraph(attributePaths = {"perfil"})
    Optional<Usuario> findWithPerfilByNmLogin(String nmLogin);
    Optional<Usuario> findByNmEmail(String nmEmail);
    Optional<Usuario> findByNmLoginAndFgExcluidoFalse(String nmLogin);
    org.springframework.data.domain.Page<Usuario> findByFgExcluidoFalse(org.springframework.data.domain.Pageable pageable);
}
