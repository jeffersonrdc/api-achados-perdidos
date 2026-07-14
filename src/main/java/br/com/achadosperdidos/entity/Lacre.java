package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "lacre")
public class Lacre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Lacre") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @Column(name = "NR_Lacre", nullable = false, length = 50) private String nrLacre;
    @Column(name = "NR_CodigoBarra", length = 100) private String nrCodigoBarra;
    @Column(name = "NR_QRCode", length = 200) private String nrQrCode;
    @Column(name = "DT_Lacre", nullable = false) private LocalDateTime dtLacre;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Violado", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgViolado = false;
    @Column(name = "DS_Observacao", length = 500) private String dsObservacao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public String getNrLacre() { return nrLacre; } public void setNrLacre(String v) { this.nrLacre = v; }
    public String getNrCodigoBarra() { return nrCodigoBarra; } public void setNrCodigoBarra(String v) { this.nrCodigoBarra = v; }
    public String getNrQrCode() { return nrQrCode; } public void setNrQrCode(String v) { this.nrQrCode = v; }
    public LocalDateTime getDtLacre() { return dtLacre; } public void setDtLacre(LocalDateTime v) { this.dtLacre = v; }
    public Boolean getFgViolado() { return fgViolado; } public void setFgViolado(Boolean v) { this.fgViolado = v; }
    public String getDsObservacao() { return dsObservacao; } public void setDsObservacao(String v) { this.dsObservacao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
