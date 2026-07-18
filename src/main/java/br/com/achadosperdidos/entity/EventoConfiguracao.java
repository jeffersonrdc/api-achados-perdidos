package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "evento_configuracao")
public class EventoConfiguracao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_EventoConfiguracao") private Long id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false, unique = true) private Evento evento;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_RecebeObjetos", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgRecebeObjetos = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_AceitaClaim", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAceitaClaim = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_ConsultaPublica", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgConsultaPublica = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_FotoObrigatoria", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgFotoObrigatoria = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_ValidacaoObrigatoria", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgValidacaoObrigatoria = true;
    @Column(name = "QT_MaxFotos", nullable = false) private Integer qtMaxFotos = 10;
    @Column(name = "QT_DiasDescarte", nullable = false) private Integer qtDiasDescarte = 180;
    @Column(name = "QT_DiasEsperaAceitavel", nullable = false) private Integer qtDiasEsperaAceitavel = 15;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Boolean getFgRecebeObjetos() { return fgRecebeObjetos; } public void setFgRecebeObjetos(Boolean v) { this.fgRecebeObjetos = v; }
    public Boolean getFgAceitaClaim() { return fgAceitaClaim; } public void setFgAceitaClaim(Boolean v) { this.fgAceitaClaim = v; }
    public Boolean getFgConsultaPublica() { return fgConsultaPublica; } public void setFgConsultaPublica(Boolean v) { this.fgConsultaPublica = v; }
    public Boolean getFgFotoObrigatoria() { return fgFotoObrigatoria; } public void setFgFotoObrigatoria(Boolean v) { this.fgFotoObrigatoria = v; }
    public Boolean getFgValidacaoObrigatoria() { return fgValidacaoObrigatoria; } public void setFgValidacaoObrigatoria(Boolean v) { this.fgValidacaoObrigatoria = v; }
    public Integer getQtMaxFotos() { return qtMaxFotos; } public void setQtMaxFotos(Integer v) { this.qtMaxFotos = v; }
    public Integer getQtDiasDescarte() { return qtDiasDescarte; } public void setQtDiasDescarte(Integer v) { this.qtDiasDescarte = v; }
    public Integer getQtDiasEsperaAceitavel() { return qtDiasEsperaAceitavel; } public void setQtDiasEsperaAceitavel(Integer v) { this.qtDiasEsperaAceitavel = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
