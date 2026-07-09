package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "devolucao")
public class Devolucao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Devolucao") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Item", nullable = false) private Item item;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Claim") private Claim claim;
    @Column(name = "TP_Devolucao", nullable = false, length = 30) private String tpDevolucao;
    @Column(name = "DT_Devolucao", nullable = false) private LocalDateTime dtDevolucao;
    @Column(name = "NM_Recebedor", nullable = false, length = 150) private String nmRecebedor;
    @Column(name = "NR_CPF", length = 11) private String nrCpf;
    @Column(name = "DS_Observacao", columnDefinition = "TEXT") private String dsObservacao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Assinado", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAssinado = false;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Concluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgConcluido = false;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Item getItem(){return item;} public void setItem(Item item){this.item=item;}
    public Claim getClaim(){return claim;} public void setClaim(Claim claim){this.claim=claim;}
    public String getTpDevolucao(){return tpDevolucao;} public void setTpDevolucao(String tpDevolucao){this.tpDevolucao=tpDevolucao;}
    public LocalDateTime getDtDevolucao(){return dtDevolucao;} public void setDtDevolucao(LocalDateTime dtDevolucao){this.dtDevolucao=dtDevolucao;}
    public String getNmRecebedor(){return nmRecebedor;} public void setNmRecebedor(String nmRecebedor){this.nmRecebedor=nmRecebedor;}
    public String getNrCpf(){return nrCpf;} public void setNrCpf(String nrCpf){this.nrCpf=nrCpf;}
    public String getDsObservacao(){return dsObservacao;} public void setDsObservacao(String dsObservacao){this.dsObservacao=dsObservacao;}
    public Boolean getFgAssinado(){return fgAssinado;} public void setFgAssinado(Boolean fgAssinado){this.fgAssinado=fgAssinado;}
    public Boolean getFgConcluido(){return fgConcluido;} public void setFgConcluido(Boolean fgConcluido){this.fgConcluido=fgConcluido;}
    public LocalDateTime getDtCadastro(){return dtCadastro;} public void setDtCadastro(LocalDateTime dtCadastro){this.dtCadastro=dtCadastro;}
    public LocalDateTime getDtAlteracao(){return dtAlteracao;} public void setDtAlteracao(LocalDateTime dtAlteracao){this.dtAlteracao=dtAlteracao;}
    public Boolean getFgAtivo(){return fgAtivo;} public void setFgAtivo(Boolean fgAtivo){this.fgAtivo=fgAtivo;}
    public Boolean getFgExcluido(){return fgExcluido;} public void setFgExcluido(Boolean fgExcluido){this.fgExcluido=fgExcluido;}
}
