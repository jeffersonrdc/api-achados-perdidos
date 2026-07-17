package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "claim_historico")
public class ClaimHistorico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_ClaimHistorico") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "IDR_Claim", nullable = false) private Claim claim;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item") private Item item;
    @Column(name = "TP_Evento", nullable = false, length = 30) private String tpEvento;
    @Column(name = "TP_Solicitacao", length = 20) private String tpSolicitacao;
    @Column(name = "DS_Detalhe", columnDefinition = "TEXT") private String dsDetalhe;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Operador") private Usuario operador;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_EmailEnviado", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgEmailEnviado = false;
    @Column(name = "DS_EmailErro", length = 500) private String dsEmailErro;
    @Column(name = "DT_Historico", nullable = false) private LocalDateTime dtHistorico;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Claim getClaim() { return claim; } public void setClaim(Claim v) { this.claim = v; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public String getTpEvento() { return tpEvento; } public void setTpEvento(String v) { this.tpEvento = v; }
    public String getTpSolicitacao() { return tpSolicitacao; } public void setTpSolicitacao(String v) { this.tpSolicitacao = v; }
    public String getDsDetalhe() { return dsDetalhe; } public void setDsDetalhe(String v) { this.dsDetalhe = v; }
    public Usuario getOperador() { return operador; } public void setOperador(Usuario v) { this.operador = v; }
    public Boolean getFgEmailEnviado() { return fgEmailEnviado; } public void setFgEmailEnviado(Boolean v) { this.fgEmailEnviado = v; }
    public String getDsEmailErro() { return dsEmailErro; } public void setDsEmailErro(String v) { this.dsEmailErro = v; }
    public LocalDateTime getDtHistorico() { return dtHistorico; } public void setDtHistorico(LocalDateTime v) { this.dtHistorico = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
