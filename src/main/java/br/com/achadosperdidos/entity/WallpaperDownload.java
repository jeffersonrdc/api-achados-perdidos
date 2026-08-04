package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/** Registro de um download de wallpaper feito no portal público (/wallpaper). */
@Entity @Table(name = "wallpaper_download")
public class WallpaperDownload {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_WallpaperDownload") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Arquivo") private Arquivo arquivo;
    @Column(name = "NM_Origem", nullable = false, length = 30) private String nmOrigem = "PORTAL";
    @Column(name = "NR_Ip", length = 45) private String nrIp;
    @Column(name = "DS_UserAgent", length = 300) private String dsUserAgent;
    @Column(name = "DT_Download", nullable = false) private LocalDateTime dtDownload;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Arquivo getArquivo() { return arquivo; } public void setArquivo(Arquivo v) { this.arquivo = v; }
    public String getNmOrigem() { return nmOrigem; } public void setNmOrigem(String v) { this.nmOrigem = v; }
    public String getNrIp() { return nrIp; } public void setNrIp(String v) { this.nrIp = v; }
    public String getDsUserAgent() { return dsUserAgent; } public void setDsUserAgent(String v) { this.dsUserAgent = v; }
    public LocalDateTime getDtDownload() { return dtDownload; } public void setDtDownload(LocalDateTime v) { this.dtDownload = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
