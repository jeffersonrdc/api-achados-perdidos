package br.com.achadosperdidos.repository;

import br.com.achadosperdidos.entity.Modelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModeloRepository extends JpaRepository<Modelo, Long> {
    List<Modelo> findByMarca_NmMarcaAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmModeloAsc(String nmMarca);
}
