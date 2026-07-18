package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "claim_mensagem")
public class ClaimMensagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ClaimMensagem")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Claim", nullable = false)
    private Claim claim;

    @Column(name = "TP_Autor", nullable = false, length = 20)
    private String tpAutor;

    @Column(name = "DS_Mensagem", nullable = false, columnDefinition = "TEXT")
    private String dsMensagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Operador")
    private Usuario operador;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_EmailEnviado", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgEmailEnviado = false;

    /** true = operador já viu (mensagens do operador nascem lidas; do solicitante, não). */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_LidaOperador", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgLidaOperador = false;

    @Column(name = "DS_EmailErro", length = 500)
    private String dsEmailErro;

    @Column(name = "DT_Mensagem", nullable = false)
    private LocalDateTime dtMensagem;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Claim getClaim() { return claim; }
    public void setClaim(Claim claim) { this.claim = claim; }
    public String getTpAutor() { return tpAutor; }
    public void setTpAutor(String tpAutor) { this.tpAutor = tpAutor; }
    public String getDsMensagem() { return dsMensagem; }
    public void setDsMensagem(String dsMensagem) { this.dsMensagem = dsMensagem; }
    public Usuario getOperador() { return operador; }
    public void setOperador(Usuario operador) { this.operador = operador; }
    public Boolean getFgEmailEnviado() { return fgEmailEnviado; }
    public void setFgEmailEnviado(Boolean fgEmailEnviado) { this.fgEmailEnviado = fgEmailEnviado; }
    public Boolean getFgLidaOperador() { return fgLidaOperador; }
    public void setFgLidaOperador(Boolean fgLidaOperador) { this.fgLidaOperador = fgLidaOperador; }
    public String getDsEmailErro() { return dsEmailErro; }
    public void setDsEmailErro(String dsEmailErro) { this.dsEmailErro = dsEmailErro; }
    public LocalDateTime getDtMensagem() { return dtMensagem; }
    public void setDtMensagem(LocalDateTime dtMensagem) { this.dtMensagem = dtMensagem; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
