package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "contato")
public class Contato {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Contato") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item") private Item item;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Claim") private Claim claim;
    @Column(name = "TP_Contato", nullable = false, length = 30) private String tpContato;
    @Column(name = "NM_Contato", nullable = false, length = 150) private String nmContato;
    @Column(name = "NR_Telefone", length = 20) private String nrTelefone;
    @Column(name = "NM_Email", length = 150) private String nmEmail;
    @Column(name = "DS_Resumo", columnDefinition = "TEXT") private String dsResumo;
    @Column(name = "DT_Contato", nullable = false) private LocalDateTime dtContato;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Item getItem() { return item; } public void setItem(Item v) { this.item = v; }
    public Claim getClaim() { return claim; } public void setClaim(Claim v) { this.claim = v; }
    public String getTpContato() { return tpContato; } public void setTpContato(String v) { this.tpContato = v; }
    public String getNmContato() { return nmContato; } public void setNmContato(String v) { this.nmContato = v; }
    public String getNrTelefone() { return nrTelefone; } public void setNrTelefone(String v) { this.nrTelefone = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getDsResumo() { return dsResumo; } public void setDsResumo(String v) { this.dsResumo = v; }
    public LocalDateTime getDtContato() { return dtContato; } public void setDtContato(LocalDateTime v) { this.dtContato = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
