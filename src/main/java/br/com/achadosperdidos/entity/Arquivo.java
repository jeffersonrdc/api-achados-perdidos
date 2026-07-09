package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "arquivo")
public class Arquivo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Arquivo") private Long id;
    @Column(name = "TP_Entidade", nullable = false, length = 30) private String tpEntidade;
    @Column(name = "ID_Entidade", nullable = false) private Long idEntidade;
    @Column(name = "TP_Arquivo", nullable = false, length = 30) private String tpArquivo;
    @Column(name = "NM_Arquivo", nullable = false, length = 255) private String nmArquivo;
    @Column(name = "NM_Path", nullable = false, length = 500) private String nmPath;
    @Column(name = "TP_Mime", length = 100) private String tpMime;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Principal", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgPrincipal = false;
    @Column(name = "QT_Bytes") private Long qtBytes;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getTpEntidade(){return tpEntidade;} public void setTpEntidade(String tpEntidade){this.tpEntidade=tpEntidade;}
    public Long getIdEntidade(){return idEntidade;} public void setIdEntidade(Long idEntidade){this.idEntidade=idEntidade;}
    public String getTpArquivo(){return tpArquivo;} public void setTpArquivo(String tpArquivo){this.tpArquivo=tpArquivo;}
    public String getNmArquivo(){return nmArquivo;} public void setNmArquivo(String nmArquivo){this.nmArquivo=nmArquivo;}
    public String getNmPath(){return nmPath;} public void setNmPath(String nmPath){this.nmPath=nmPath;}
    public String getTpMime(){return tpMime;} public void setTpMime(String tpMime){this.tpMime=tpMime;}
    public Boolean getFgPrincipal(){return fgPrincipal;} public void setFgPrincipal(Boolean fgPrincipal){this.fgPrincipal=fgPrincipal;}
    public Long getQtBytes(){return qtBytes;} public void setQtBytes(Long qtBytes){this.qtBytes=qtBytes;}
    public LocalDateTime getDtCadastro(){return dtCadastro;} public void setDtCadastro(LocalDateTime dtCadastro){this.dtCadastro=dtCadastro;}
    public LocalDateTime getDtAlteracao(){return dtAlteracao;} public void setDtAlteracao(LocalDateTime dtAlteracao){this.dtAlteracao=dtAlteracao;}
    public Boolean getFgAtivo(){return fgAtivo;} public void setFgAtivo(Boolean fgAtivo){this.fgAtivo=fgAtivo;}
    public Boolean getFgExcluido(){return fgExcluido;} public void setFgExcluido(Boolean fgExcluido){this.fgExcluido=fgExcluido;}
}
