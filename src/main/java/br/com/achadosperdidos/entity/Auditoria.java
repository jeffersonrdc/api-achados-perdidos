package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "auditoria")
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Auditoria") private Long id;
    @Column(name = "NM_Tabela", nullable = false, length = 80) private String nmTabela;
    @Column(name = "ID_Registro", nullable = false) private Long idRegistro;
    @Column(name = "TP_Acao", nullable = false, length = 20) private String tpAcao;
    @Column(name = "DS_Antes", columnDefinition = "JSON") private String dsAntes;
    @Column(name = "DS_Depois", columnDefinition = "JSON") private String dsDepois;
    @Column(name = "IDR_Usuario") private Long idUsuario;
    @Column(name = "DT_Auditoria", nullable = false) private LocalDateTime dtAuditoria;
    @Column(name = "NR_IP", length = 45) private String nrIp;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmTabela() { return nmTabela; } public void setNmTabela(String v) { this.nmTabela = v; }
    public Long getIdRegistro() { return idRegistro; } public void setIdRegistro(Long v) { this.idRegistro = v; }
    public String getTpAcao() { return tpAcao; } public void setTpAcao(String v) { this.tpAcao = v; }
    public String getDsAntes() { return dsAntes; } public void setDsAntes(String v) { this.dsAntes = v; }
    public String getDsDepois() { return dsDepois; } public void setDsDepois(String v) { this.dsDepois = v; }
    public Long getIdUsuario() { return idUsuario; } public void setIdUsuario(Long v) { this.idUsuario = v; }
    public LocalDateTime getDtAuditoria() { return dtAuditoria; } public void setDtAuditoria(LocalDateTime v) { this.dtAuditoria = v; }
    public String getNrIp() { return nrIp; } public void setNrIp(String v) { this.nrIp = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
