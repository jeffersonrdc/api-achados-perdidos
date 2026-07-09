package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crianca")
public class Crianca {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Crianca") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @Column(name = "NM_Crianca", nullable = false, length = 150) private String nmCrianca;
    @Column(name = "DT_Nascimento") private LocalDate dtNascimento;
    @Column(name = "NM_Foto", length = 500) private String nmFoto;
    @Column(name = "NR_Pulseira", length = 50) private String nrPulseira;
    @Column(name = "NR_QRCode", length = 200) private String nrQrCode;
    @Column(name = "DS_Observacao", columnDefinition = "TEXT") private String dsObservacao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Evento getEvento(){return evento;} public void setEvento(Evento evento){this.evento=evento;}
    public String getNmCrianca(){return nmCrianca;} public void setNmCrianca(String nmCrianca){this.nmCrianca=nmCrianca;}
    public LocalDate getDtNascimento(){return dtNascimento;} public void setDtNascimento(LocalDate dtNascimento){this.dtNascimento=dtNascimento;}
    public String getNmFoto(){return nmFoto;} public void setNmFoto(String nmFoto){this.nmFoto=nmFoto;}
    public String getNrPulseira(){return nrPulseira;} public void setNrPulseira(String nrPulseira){this.nrPulseira=nrPulseira;}
    public String getNrQrCode(){return nrQrCode;} public void setNrQrCode(String nrQrCode){this.nrQrCode=nrQrCode;}
    public String getDsObservacao(){return dsObservacao;} public void setDsObservacao(String dsObservacao){this.dsObservacao=dsObservacao;}
    public LocalDateTime getDtCadastro(){return dtCadastro;} public void setDtCadastro(LocalDateTime dtCadastro){this.dtCadastro=dtCadastro;}
    public LocalDateTime getDtAlteracao(){return dtAlteracao;} public void setDtAlteracao(LocalDateTime dtAlteracao){this.dtAlteracao=dtAlteracao;}
    public Boolean getFgAtivo(){return fgAtivo;} public void setFgAtivo(Boolean fgAtivo){this.fgAtivo=fgAtivo;}
    public Boolean getFgExcluido(){return fgExcluido;} public void setFgExcluido(Boolean fgExcluido){this.fgExcluido=fgExcluido;}
}
