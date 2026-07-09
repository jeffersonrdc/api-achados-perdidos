package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByNmPerfilIgnoreCaseAndFgExcluidoFalse(String nmPerfil);
}
