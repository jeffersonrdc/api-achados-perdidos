package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity @Table(name = "item")
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Item") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Categoria", nullable = false) private Categoria categoria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Subcategoria") private Categoria subcategoria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Localizacao") private Localizacao localizacao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Status", nullable = false) private StatusItem status;
    @Column(name = "CD_Item", nullable = false, length = 50) private String cdItem;
    @Column(name = "NM_Titulo", nullable = false, length = 200) private String nmTitulo;
    @Column(name = "DS_Item", columnDefinition = "TEXT") private String dsItem;
    @Column(name = "NM_Marca", length = 100) private String nmMarca;
    @Column(name = "NM_Modelo", length = 100) private String nmModelo;
    @Column(name = "NM_Cor", length = 60) private String nmCor;
    @Column(name = "DT_Encontrado", nullable = false) private LocalDate dtEncontrado;
    @Column(name = "HR_Encontrado") private LocalTime hrEncontrado;
    @Column(name = "NM_LocalEncontrado", length = 200) private String nmLocalEncontrado;
    @Column(name = "VL_Estimado", precision = 12, scale = 2) private BigDecimal vlEstimado;
    @Column(name = "TP_Prioridade", length = 10) private String tpPrioridade;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Sensivel", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgSensivel = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Entregue", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgEntregue = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Descartado", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgDescartado = false;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Categoria getCategoria() { return categoria; } public void setCategoria(Categoria v) { this.categoria = v; }
    public Categoria getSubcategoria() { return subcategoria; } public void setSubcategoria(Categoria v) { this.subcategoria = v; }
    public Localizacao getLocalizacao() { return localizacao; } public void setLocalizacao(Localizacao v) { this.localizacao = v; }
    public StatusItem getStatus() { return status; } public void setStatus(StatusItem v) { this.status = v; }
    public String getCdItem() { return cdItem; } public void setCdItem(String v) { this.cdItem = v; }
    public String getNmTitulo() { return nmTitulo; } public void setNmTitulo(String v) { this.nmTitulo = v; }
    public String getDsItem() { return dsItem; } public void setDsItem(String v) { this.dsItem = v; }
    public String getNmMarca() { return nmMarca; } public void setNmMarca(String v) { this.nmMarca = v; }
    public String getNmModelo() { return nmModelo; } public void setNmModelo(String v) { this.nmModelo = v; }
    public String getNmCor() { return nmCor; } public void setNmCor(String v) { this.nmCor = v; }
    public LocalDate getDtEncontrado() { return dtEncontrado; } public void setDtEncontrado(LocalDate v) { this.dtEncontrado = v; }
    public LocalTime getHrEncontrado() { return hrEncontrado; } public void setHrEncontrado(LocalTime v) { this.hrEncontrado = v; }
    public String getNmLocalEncontrado() { return nmLocalEncontrado; } public void setNmLocalEncontrado(String v) { this.nmLocalEncontrado = v; }
    public BigDecimal getVlEstimado() { return vlEstimado; } public void setVlEstimado(BigDecimal v) { this.vlEstimado = v; }
    public String getTpPrioridade() { return tpPrioridade; } public void setTpPrioridade(String v) { this.tpPrioridade = v; }
    public Boolean getFgSensivel() { return fgSensivel; } public void setFgSensivel(Boolean v) { this.fgSensivel = v; }
    public Boolean getFgEntregue() { return fgEntregue; } public void setFgEntregue(Boolean v) { this.fgEntregue = v; }
    public Boolean getFgDescartado() { return fgDescartado; } public void setFgDescartado(Boolean v) { this.fgDescartado = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
