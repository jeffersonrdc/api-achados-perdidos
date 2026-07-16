package br.com.achadosperdidos.service;

import br.com.achadosperdidos.entity.Cor;
import br.com.achadosperdidos.entity.Marca;
import br.com.achadosperdidos.entity.Modelo;
import br.com.achadosperdidos.repository.CorRepository;
import br.com.achadosperdidos.repository.MarcaRepository;
import br.com.achadosperdidos.repository.ModeloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogos globais (cor, marca, modelo) que alimentam os selects da tela de
 * Coleta/Edição de itens. O item grava os nomes escolhidos (texto), por isso
 * os endpoints retornam apenas os nomes ativos, já ordenados.
 */
@Service
public class CatalogoService {
    private final CorRepository corRepository;
    private final MarcaRepository marcaRepository;
    private final ModeloRepository modeloRepository;

    public CatalogoService(CorRepository corRepository, MarcaRepository marcaRepository,
                           ModeloRepository modeloRepository) {
        this.corRepository = corRepository;
        this.marcaRepository = marcaRepository;
        this.modeloRepository = modeloRepository;
    }

    @Transactional(readOnly = true)
    public List<String> listarCores() {
        return corRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmCorAsc()
                .stream().map(Cor::getNmCor).toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarMarcas() {
        return marcaRepository.findByFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmMarcaAsc()
                .stream().map(Marca::getNmMarca).toList();
    }

    /** Modelos de uma marca (por nome). Sem marca informada, retorna vazio (cascade). */
    @Transactional(readOnly = true)
    public List<String> listarModelos(String nmMarca) {
        if (nmMarca == null || nmMarca.isBlank()) return List.of();
        return modeloRepository
                .findByMarca_NmMarcaAndFgExcluidoFalseAndFgAtivoTrueOrderByOrOrdemAscNmModeloAsc(nmMarca.trim())
                .stream().map(Modelo::getNmModelo).toList();
    }
}
