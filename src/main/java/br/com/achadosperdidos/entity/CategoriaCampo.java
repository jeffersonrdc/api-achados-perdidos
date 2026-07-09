package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "categoria_campo")
public class CategoriaCampo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_CategoriaCampo") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Categoria", nullable = false) private Categoria categoria;
    @Column(name = "NM_Campo", nullable = false, length = 80) private String nmCampo;
    @Column(name = "DS_Label", nullable = false, length = 150) private String dsLabel;
    @Column(name = "TP_Campo", nullable = false, length = 30) private String tpCampo;
    @Column(name = "QT_Tamanho") private Integer qtTamanho;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Obrigatorio", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgObrigatorio = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Pesquisavel", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgPesquisavel = false;
    @Column(name = "OR_Exibicao", nullable = false) private Integer orExibicao = 0;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Categoria getCategoria() { return categoria; } public void setCategoria(Categoria c) { this.categoria = c; }
    public String getNmCampo() { return nmCampo; } public void setNmCampo(String v) { this.nmCampo = v; }
    public String getDsLabel() { return dsLabel; } public void setDsLabel(String v) { this.dsLabel = v; }
    public String getTpCampo() { return tpCampo; } public void setTpCampo(String v) { this.tpCampo = v; }
    public Integer getQtTamanho() { return qtTamanho; } public void setQtTamanho(Integer v) { this.qtTamanho = v; }
    public Boolean getFgObrigatorio() { return fgObrigatorio; } public void setFgObrigatorio(Boolean v) { this.fgObrigatorio = v; }
    public Boolean getFgPesquisavel() { return fgPesquisavel; } public void setFgPesquisavel(Boolean v) { this.fgPesquisavel = v; }
    public Integer getOrExibicao() { return orExibicao; } public void setOrExibicao(Integer v) { this.orExibicao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
