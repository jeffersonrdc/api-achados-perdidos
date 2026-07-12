package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "local")
public class Local {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Local") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Responsavel") private Usuario responsavel;
    @Column(name = "NM_Local", nullable = false, length = 150) private String nmLocal;
    @Column(name = "TP_Local", nullable = false, length = 40) private String tpLocal;
    @Column(name = "VL_Latitude", precision = 10, scale = 7) private BigDecimal vlLatitude;
    @Column(name = "VL_Longitude", precision = 10, scale = 7) private BigDecimal vlLongitude;
    @Column(name = "NM_Horario", length = 120) private String nmHorario;
    @Column(name = "DS_Observacao", length = 500) private String dsObservacao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Usuario getResponsavel() { return responsavel; } public void setResponsavel(Usuario v) { this.responsavel = v; }
    public String getNmLocal() { return nmLocal; } public void setNmLocal(String v) { this.nmLocal = v; }
    public String getTpLocal() { return tpLocal; } public void setTpLocal(String v) { this.tpLocal = v; }
    public BigDecimal getVlLatitude() { return vlLatitude; } public void setVlLatitude(BigDecimal v) { this.vlLatitude = v; }
    public BigDecimal getVlLongitude() { return vlLongitude; } public void setVlLongitude(BigDecimal v) { this.vlLongitude = v; }
    public String getNmHorario() { return nmHorario; } public void setNmHorario(String v) { this.nmHorario = v; }
    public String getDsObservacao() { return dsObservacao; } public void setDsObservacao(String v) { this.dsObservacao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
