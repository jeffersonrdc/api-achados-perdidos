package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_event")
public class AuthEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_AuthEvent")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Usuario")
    private Usuario usuario;

    @Column(name = "TP_Evento", nullable = false, length = 40)
    private String tpEvento;

    @Column(name = "TP_Resultado", nullable = false, length = 20)
    private String tpResultado;

    @Column(name = "CD_Motivo", length = 80)
    private String cdMotivo;

    @Column(name = "DS_IdentificadorMascarado", length = 120)
    private String dsIdentificadorMascarado;

    @Column(name = "NR_IP", length = 45)
    private String nrIp;

    @Column(name = "NM_Dispositivo", length = 150)
    private String nmDispositivo;

    @Column(name = "NM_Navegador", length = 150)
    private String nmNavegador;

    @Column(name = "DT_Evento", nullable = false)
    private LocalDateTime dtEvento;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTpEvento() { return tpEvento; }
    public void setTpEvento(String tpEvento) { this.tpEvento = tpEvento; }
    public String getTpResultado() { return tpResultado; }
    public void setTpResultado(String tpResultado) { this.tpResultado = tpResultado; }
    public String getCdMotivo() { return cdMotivo; }
    public void setCdMotivo(String cdMotivo) { this.cdMotivo = cdMotivo; }
    public String getDsIdentificadorMascarado() { return dsIdentificadorMascarado; }
    public void setDsIdentificadorMascarado(String dsIdentificadorMascarado) { this.dsIdentificadorMascarado = dsIdentificadorMascarado; }
    public String getNrIp() { return nrIp; }
    public void setNrIp(String nrIp) { this.nrIp = nrIp; }
    public String getNmDispositivo() { return nmDispositivo; }
    public void setNmDispositivo(String nmDispositivo) { this.nmDispositivo = nmDispositivo; }
    public String getNmNavegador() { return nmNavegador; }
    public void setNmNavegador(String nmNavegador) { this.nmNavegador = nmNavegador; }
    public LocalDateTime getDtEvento() { return dtEvento; }
    public void setDtEvento(LocalDateTime dtEvento) { this.dtEvento = dtEvento; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
