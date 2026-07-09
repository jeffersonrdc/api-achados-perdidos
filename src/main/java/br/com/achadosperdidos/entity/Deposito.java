package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposito")
public class Deposito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Deposito") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @Column(name = "NM_Deposito", nullable = false, length = 150) private String nmDeposito;
    @Column(name = "DS_Deposito", length = 500) private String dsDeposito;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Principal", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgPrincipal = false;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }
    public String getNmDeposito() { return nmDeposito; }
    public void setNmDeposito(String nmDeposito) { this.nmDeposito = nmDeposito; }
    public String getDsDeposito() { return dsDeposito; }
    public void setDsDeposito(String dsDeposito) { this.dsDeposito = dsDeposito; }
    public Boolean getFgPrincipal() { return fgPrincipal; }
    public void setFgPrincipal(Boolean fgPrincipal) { this.fgPrincipal = fgPrincipal; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public void setDtCadastro(LocalDateTime dtCadastro) { this.dtCadastro = dtCadastro; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; }
    public void setDtAlteracao(LocalDateTime dtAlteracao) { this.dtAlteracao = dtAlteracao; }
    public Boolean getFgAtivo() { return fgAtivo; }
    public void setFgAtivo(Boolean fgAtivo) { this.fgAtivo = fgAtivo; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
