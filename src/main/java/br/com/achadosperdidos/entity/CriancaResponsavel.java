package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "crianca_responsavel")
public class CriancaResponsavel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CriancaResponsavel") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Crianca", nullable = false) private Crianca crianca;
    @Column(name = "NM_Responsavel", nullable = false, length = 150) private String nmResponsavel;
    @Column(name = "NR_CPF", length = 11) private String nrCpf;
    @Column(name = "NR_RG", length = 20) private String nrRg;
    @Column(name = "NM_Email", length = 150) private String nmEmail;
    @Column(name = "NR_Telefone", length = 20) private String nrTelefone;
    @Column(name = "DS_Parentesco", length = 80) private String dsParentesco;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Principal", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgPrincipal = false;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Crianca getCrianca(){return crianca;} public void setCrianca(Crianca crianca){this.crianca=crianca;}
    public String getNmResponsavel(){return nmResponsavel;} public void setNmResponsavel(String nmResponsavel){this.nmResponsavel=nmResponsavel;}
    public String getNrCpf(){return nrCpf;} public void setNrCpf(String nrCpf){this.nrCpf=nrCpf;}
    public String getNrRg(){return nrRg;} public void setNrRg(String nrRg){this.nrRg=nrRg;}
    public String getNmEmail(){return nmEmail;} public void setNmEmail(String nmEmail){this.nmEmail=nmEmail;}
    public String getNrTelefone(){return nrTelefone;} public void setNrTelefone(String nrTelefone){this.nrTelefone=nrTelefone;}
    public String getDsParentesco(){return dsParentesco;} public void setDsParentesco(String dsParentesco){this.dsParentesco=dsParentesco;}
    public Boolean getFgPrincipal(){return fgPrincipal;} public void setFgPrincipal(Boolean fgPrincipal){this.fgPrincipal=fgPrincipal;}
    public LocalDateTime getDtCadastro(){return dtCadastro;} public void setDtCadastro(LocalDateTime dtCadastro){this.dtCadastro=dtCadastro;}
    public LocalDateTime getDtAlteracao(){return dtAlteracao;} public void setDtAlteracao(LocalDateTime dtAlteracao){this.dtAlteracao=dtAlteracao;}
    public Boolean getFgAtivo(){return fgAtivo;} public void setFgAtivo(Boolean fgAtivo){this.fgAtivo=fgAtivo;}
    public Boolean getFgExcluido(){return fgExcluido;} public void setFgExcluido(Boolean fgExcluido){this.fgExcluido=fgExcluido;}
}
