package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "triagem")
public class Triagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Triagem") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Operador") private Usuario operador;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_LocalizacaoInicial") private Localizacao localizacaoInicial;
    @Column(name = "NM_Estado", length = 60) private String nmEstado;
    @Column(name = "DS_Tags", length = 500) private String dsTags;
    @Column(name = "DS_Observacao", length = 1000) private String dsObservacao;
    @Column(name = "DS_SugestaoIa", length = 300) private String dsSugestaoIa;
    @Column(name = "VL_ConfiancaIa", precision = 5, scale = 2) private BigDecimal vlConfiancaIa;
    @Column(name = "TP_Status", nullable = false, length = 30) private String tpStatus = "EM_ANDAMENTO";
    @Column(name = "DT_Inicio") private LocalDateTime dtInicio;
    @Column(name = "DT_Conclusao") private LocalDateTime dtConclusao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public Usuario getOperador() { return operador; } public void setOperador(Usuario v) { this.operador = v; }
    public Localizacao getLocalizacaoInicial() { return localizacaoInicial; } public void setLocalizacaoInicial(Localizacao v) { this.localizacaoInicial = v; }
    public String getNmEstado() { return nmEstado; } public void setNmEstado(String v) { this.nmEstado = v; }
    public String getDsTags() { return dsTags; } public void setDsTags(String v) { this.dsTags = v; }
    public String getDsObservacao() { return dsObservacao; } public void setDsObservacao(String v) { this.dsObservacao = v; }
    public String getDsSugestaoIa() { return dsSugestaoIa; } public void setDsSugestaoIa(String v) { this.dsSugestaoIa = v; }
    public BigDecimal getVlConfiancaIa() { return vlConfiancaIa; } public void setVlConfiancaIa(BigDecimal v) { this.vlConfiancaIa = v; }
    public String getTpStatus() { return tpStatus; } public void setTpStatus(String v) { this.tpStatus = v; }
    public LocalDateTime getDtInicio() { return dtInicio; } public void setDtInicio(LocalDateTime v) { this.dtInicio = v; }
    public LocalDateTime getDtConclusao() { return dtConclusao; } public void setDtConclusao(LocalDateTime v) { this.dtConclusao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
