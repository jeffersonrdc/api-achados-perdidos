package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "item_historico")
public class ItemHistorico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_ItemHistorico") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_StatusAnterior") private StatusItem statusAnterior;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_StatusNovo", nullable = false) private StatusItem statusNovo;
    @Column(name = "DS_Historico", columnDefinition = "TEXT") private String dsHistorico;
    @Column(name = "DT_Historico", nullable = false) private LocalDateTime dtHistorico;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public StatusItem getStatusAnterior() { return statusAnterior; } public void setStatusAnterior(StatusItem v) { this.statusAnterior = v; }
    public StatusItem getStatusNovo() { return statusNovo; } public void setStatusNovo(StatusItem v) { this.statusNovo = v; }
    public String getDsHistorico() { return dsHistorico; } public void setDsHistorico(String v) { this.dsHistorico = v; }
    public LocalDateTime getDtHistorico() { return dtHistorico; } public void setDtHistorico(LocalDateTime v) { this.dtHistorico = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
