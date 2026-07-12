package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.EquipeUsuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipeUsuarioRepository extends JpaRepository<EquipeUsuario, Long> {
    @EntityGraph(attributePaths = {"usuario"})
    List<EquipeUsuario> findByEquipe_IdAndFgExcluidoFalseOrderByUsuario_NmUsuarioAsc(Long equipeId);
    Optional<EquipeUsuario> findByEquipe_IdAndUsuario_IdAndFgExcluidoFalse(Long equipeId, Long usuarioId);
    Optional<EquipeUsuario> findByEquipe_IdAndUsuario_Id(Long equipeId, Long usuarioId);
}
