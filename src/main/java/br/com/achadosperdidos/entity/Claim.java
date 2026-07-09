package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity @Table(name = "claim")
public class Claim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID_Claim") private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Categoria", nullable = false) private Categoria categoria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Status", nullable = false) private StatusItem status;
    @Column(name = "NM_Nome", nullable = false, length = 150) private String nmNome;
    @Column(name = "NR_CPF", length = 11) private String nrCpf;
    @Column(name = "NM_Email", length = 150) private String nmEmail;
    @Column(name = "NR_Telefone", length = 20) private String nrTelefone;
    @Column(name = "NM_Objeto", nullable = false, length = 200) private String nmObjeto;
    @Column(name = "DS_Objeto", columnDefinition = "TEXT") private String dsObjeto;
    @Column(name = "NM_Marca", length = 100) private String nmMarca;
    @Column(name = "NM_Modelo", length = 100) private String nmModelo;
    @Column(name = "NM_Cor", length = 60) private String nmCor;
    @Column(name = "DT_Perdeu") private LocalDate dtPerdeu;
    @Column(name = "HR_Perdeu") private LocalTime hrPerdeu;
    @Column(name = "NM_Local", length = 200) private String nmLocal;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Categoria getCategoria() { return categoria; } public void setCategoria(Categoria v) { this.categoria = v; }
    public StatusItem getStatus() { return status; } public void setStatus(StatusItem v) { this.status = v; }
    public String getNmNome() { return nmNome; } public void setNmNome(String v) { this.nmNome = v; }
    public String getNrCpf() { return nrCpf; } public void setNrCpf(String v) { this.nrCpf = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getNrTelefone() { return nrTelefone; } public void setNrTelefone(String v) { this.nrTelefone = v; }
    public String getNmObjeto() { return nmObjeto; } public void setNmObjeto(String v) { this.nmObjeto = v; }
    public String getDsObjeto() { return dsObjeto; } public void setDsObjeto(String v) { this.dsObjeto = v; }
    public String getNmMarca() { return nmMarca; } public void setNmMarca(String v) { this.nmMarca = v; }
    public String getNmModelo() { return nmModelo; } public void setNmModelo(String v) { this.nmModelo = v; }
    public String getNmCor() { return nmCor; } public void setNmCor(String v) { this.nmCor = v; }
    public LocalDate getDtPerdeu() { return dtPerdeu; } public void setDtPerdeu(LocalDate v) { this.dtPerdeu = v; }
    public LocalTime getHrPerdeu() { return hrPerdeu; } public void setHrPerdeu(LocalTime v) { this.hrPerdeu = v; }
    public String getNmLocal() { return nmLocal; } public void setNmLocal(String v) { this.nmLocal = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
