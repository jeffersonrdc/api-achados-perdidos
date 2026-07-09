package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "sla_registro")
public class SlaRegistro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SlaRegistro") private Long id;
    @Column(name = "TP_Entidade", nullable = false, length = 30) private String tpEntidade;
    @Column(name = "ID_Entidade", nullable = false) private Long idEntidade;
    @Column(name = "DT_Inicio", nullable = false) private LocalDateTime dtInicio;
    @Column(name = "DT_Limite", nullable = false) private LocalDateTime dtLimite;
    @Column(name = "DT_Conclusao") private LocalDateTime dtConclusao;
    @Column(name = "ST_Sla", nullable = false, length = 20) private String stSla;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTpEntidade(){return tpEntidade;} public void setTpEntidade(String tpEntidade){this.tpEntidade=tpEntidade;}
    public Long getIdEntidade(){return idEntidade;} public void setIdEntidade(Long idEntidade){this.idEntidade=idEntidade;}
    public LocalDateTime getDtInicio(){return dtInicio;} public void setDtInicio(LocalDateTime dtInicio){this.dtInicio=dtInicio;}
    public LocalDateTime getDtLimite(){return dtLimite;} public void setDtLimite(LocalDateTime dtLimite){this.dtLimite=dtLimite;}
    public LocalDateTime getDtConclusao(){return dtConclusao;} public void setDtConclusao(LocalDateTime dtConclusao){this.dtConclusao=dtConclusao;}
    public String getStSla(){return stSla;} public void setStSla(String stSla){this.stSla=stSla;}
    public Boolean getFgAtivo(){return fgAtivo;} public void setFgAtivo(Boolean fgAtivo){this.fgAtivo=fgAtivo;}
    public Boolean getFgExcluido(){return fgExcluido;} public void setFgExcluido(Boolean fgExcluido){this.fgExcluido=fgExcluido;}
}
