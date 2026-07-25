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
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Subcategoria") private Categoria subcategoria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Status", nullable = false) private StatusItem status;
    /** PERDA = relato de objeto perdido; RETIRADA = solicitação sobre item encontrado. */
    @Column(name = "TP_Claim", nullable = false, length = 20) private String tpClaim = "PERDA";
    @Column(name = "CD_Claim", length = 30) private String cdClaim;
    @Column(name = "NM_Nome", nullable = false, length = 150) private String nmNome;
    @Column(name = "NR_CPF", length = 11) private String nrCpf;
    @Column(name = "NM_Email", length = 150) private String nmEmail;
    @Column(name = "NR_Telefone", length = 20) private String nrTelefone;
    @Column(name = "NM_ContatoConfianca", length = 150) private String nmContatoConfianca;
    @Column(name = "NR_TelefoneConfianca", length = 20) private String nrTelefoneConfianca;
    @Column(name = "DS_RelacaoContatoConfianca", length = 80) private String dsRelacaoContatoConfianca;
    @Column(name = "NM_Objeto", nullable = false, length = 200) private String nmObjeto;
    @Column(name = "DS_Objeto", columnDefinition = "TEXT") private String dsObjeto;
    /** Descrição do wallpaper / tela de bloqueio (útil para celulares). */
    @Column(name = "DS_Wallpaper", length = 300) private String dsWallpaper;
    /** Detalhes que só o proprietário saberia (validação de retirada). */
    @Column(name = "DS_DetalhesOcultos", columnDefinition = "TEXT") private String dsDetalhesOcultos;
    @Column(name = "NM_Marca", length = 100) private String nmMarca;
    @Column(name = "NM_Modelo", length = 100) private String nmModelo;
    @Column(name = "NM_Cor", length = 60) private String nmCor;
    @Column(name = "NM_Estado", length = 60) private String nmEstado;
    @Column(name = "DS_Tags", columnDefinition = "TEXT") private String dsTags;
    @Column(name = "DS_JustificativaAprovacao", length = 1000) private String dsJustificativaAprovacao;
    @Column(name = "DS_JustificativaReprovacao", length = 1000) private String dsJustificativaReprovacao;
    @Column(name = "TP_Prioridade", length = 20) private String tpPrioridade;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Sensivel", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgSensivel = false;
    @Column(name = "DT_Perdeu") private LocalDate dtPerdeu;
    @Column(name = "HR_Perdeu") private LocalTime hrPerdeu;
    @Column(name = "NM_Local", length = 200) private String nmLocal;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "IDR_Local") private Local local;
    /** Operador do painel que registrou/atualizou o relato. */
    @Column(name = "NM_Operador", length = 150) private String nmOperador;
    /** Observação interna do operador (não exibida no portal). */
    @Column(name = "DS_Observacao", columnDefinition = "TEXT") private String dsObservacao;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT) @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Categoria getCategoria() { return categoria; } public void setCategoria(Categoria v) { this.categoria = v; }
    public Categoria getSubcategoria() { return subcategoria; } public void setSubcategoria(Categoria v) { this.subcategoria = v; }
    public StatusItem getStatus() { return status; } public void setStatus(StatusItem v) { this.status = v; }
    public String getTpClaim() { return tpClaim; } public void setTpClaim(String v) { this.tpClaim = v; }
    public String getCdClaim() { return cdClaim; } public void setCdClaim(String v) { this.cdClaim = v; }
    public String getNmNome() { return nmNome; } public void setNmNome(String v) { this.nmNome = v; }
    public String getNrCpf() { return nrCpf; } public void setNrCpf(String v) { this.nrCpf = v; }
    public String getNmEmail() { return nmEmail; } public void setNmEmail(String v) { this.nmEmail = v; }
    public String getNrTelefone() { return nrTelefone; } public void setNrTelefone(String v) { this.nrTelefone = v; }
    public String getNmContatoConfianca() { return nmContatoConfianca; } public void setNmContatoConfianca(String v) { this.nmContatoConfianca = v; }
    public String getNrTelefoneConfianca() { return nrTelefoneConfianca; } public void setNrTelefoneConfianca(String v) { this.nrTelefoneConfianca = v; }
    public String getDsRelacaoContatoConfianca() { return dsRelacaoContatoConfianca; } public void setDsRelacaoContatoConfianca(String v) { this.dsRelacaoContatoConfianca = v; }
    public String getNmObjeto() { return nmObjeto; } public void setNmObjeto(String v) { this.nmObjeto = v; }
    public String getDsObjeto() { return dsObjeto; } public void setDsObjeto(String v) { this.dsObjeto = v; }
    public String getDsWallpaper() { return dsWallpaper; } public void setDsWallpaper(String v) { this.dsWallpaper = v; }
    public String getDsDetalhesOcultos() { return dsDetalhesOcultos; } public void setDsDetalhesOcultos(String v) { this.dsDetalhesOcultos = v; }
    public String getNmMarca() { return nmMarca; } public void setNmMarca(String v) { this.nmMarca = v; }
    public String getNmModelo() { return nmModelo; } public void setNmModelo(String v) { this.nmModelo = v; }
    public String getNmCor() { return nmCor; } public void setNmCor(String v) { this.nmCor = v; }
    public String getNmEstado() { return nmEstado; } public void setNmEstado(String v) { this.nmEstado = v; }
    public String getDsTags() { return dsTags; } public void setDsTags(String v) { this.dsTags = v; }
    public String getDsJustificativaAprovacao() { return dsJustificativaAprovacao; } public void setDsJustificativaAprovacao(String v) { this.dsJustificativaAprovacao = v; }
    public String getDsJustificativaReprovacao() { return dsJustificativaReprovacao; } public void setDsJustificativaReprovacao(String v) { this.dsJustificativaReprovacao = v; }
    public String getTpPrioridade() { return tpPrioridade; } public void setTpPrioridade(String v) { this.tpPrioridade = v; }
    public Boolean getFgSensivel() { return fgSensivel; } public void setFgSensivel(Boolean v) { this.fgSensivel = v; }
    public LocalDate getDtPerdeu() { return dtPerdeu; } public void setDtPerdeu(LocalDate v) { this.dtPerdeu = v; }
    public LocalTime getHrPerdeu() { return hrPerdeu; } public void setHrPerdeu(LocalTime v) { this.hrPerdeu = v; }
    public String getNmLocal() { return nmLocal; } public void setNmLocal(String v) { this.nmLocal = v; }
    public Local getLocal() { return local; } public void setLocal(Local v) { this.local = v; }
    public String getNmOperador() { return nmOperador; } public void setNmOperador(String v) { this.nmOperador = v; }
    public String getDsObservacao() { return dsObservacao; } public void setDsObservacao(String v) { this.dsObservacao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
