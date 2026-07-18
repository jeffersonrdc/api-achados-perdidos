package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "tag")
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Tag") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Subcategoria", nullable = false)
    private Categoria subcategoria;
    @Column(name = "NM_Tag", nullable = false, length = 100) private String nmTag;
    @Column(name = "DS_Tag", length = 255) private String dsTag;
    @Column(name = "OR_Ordem", nullable = false) private Integer orOrdem = 0;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Categoria getSubcategoria() { return subcategoria; } public void setSubcategoria(Categoria v) { this.subcategoria = v; }
    public String getNmTag() { return nmTag; } public void setNmTag(String v) { this.nmTag = v; }
    public String getDsTag() { return dsTag; } public void setDsTag(String v) { this.dsTag = v; }
    public Integer getOrOrdem() { return orOrdem; } public void setOrOrdem(Integer v) { this.orOrdem = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
