package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name = "marca")
public class Marca {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Marca") private Long id;
    @Column(name = "NM_Marca", nullable = false, length = 100) private String nmMarca;
    @Column(name = "OR_Ordem", nullable = false) private Integer orOrdem = 0;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    /**
     * Subcategorias em que a marca aparece no select (N:N via marca_subcategoria).
     * Vazio = marca genérica (visível em qualquer subcategoria), no mesmo espírito de modelo sem vínculo.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "marca_subcategoria",
            joinColumns = @JoinColumn(name = "IDR_Marca"),
            inverseJoinColumns = @JoinColumn(name = "IDR_Subcategoria"))
    private Set<Categoria> subcategorias = new HashSet<>();

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmMarca() { return nmMarca; } public void setNmMarca(String v) { this.nmMarca = v; }
    public Integer getOrOrdem() { return orOrdem; } public void setOrOrdem(Integer v) { this.orOrdem = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
    public Set<Categoria> getSubcategorias() { return subcategorias; }
    public void setSubcategorias(Set<Categoria> v) { this.subcategorias = v != null ? v : new HashSet<>(); }
}
