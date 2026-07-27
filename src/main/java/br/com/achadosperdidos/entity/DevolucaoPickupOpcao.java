package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "devolucao_pickup_opcao")
public class DevolucaoPickupOpcao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_DevolucaoPickupOpcao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "IDR_Devolucao", nullable = false)
    private Devolucao devolucao;

    @Column(name = "DT_Opcao", nullable = false)
    private LocalDate dtOpcao;

    @Column(name = "HR_Inicio", nullable = false)
    private LocalTime hrInicio;

    @Column(name = "HR_Fim", nullable = false)
    private LocalTime hrFim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Local")
    private Local local;

    @Column(name = "NM_Local", length = 150)
    private String nmLocal;

    @Column(name = "DT_Expiracao")
    private LocalDateTime dtExpiracao;

    @Column(name = "DS_Notas", length = 500)
    private String dsNotas;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Selecionada", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgSelecionada = false;

    @Column(name = "DT_Cadastro", nullable = false)
    private LocalDateTime dtCadastro;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean fgExcluido = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }
    public LocalDate getDtOpcao() { return dtOpcao; }
    public void setDtOpcao(LocalDate dtOpcao) { this.dtOpcao = dtOpcao; }
    public LocalTime getHrInicio() { return hrInicio; }
    public void setHrInicio(LocalTime hrInicio) { this.hrInicio = hrInicio; }
    public LocalTime getHrFim() { return hrFim; }
    public void setHrFim(LocalTime hrFim) { this.hrFim = hrFim; }
    public Local getLocal() { return local; }
    public void setLocal(Local local) { this.local = local; }
    public String getNmLocal() { return nmLocal; }
    public void setNmLocal(String nmLocal) { this.nmLocal = nmLocal; }
    public LocalDateTime getDtExpiracao() { return dtExpiracao; }
    public void setDtExpiracao(LocalDateTime dtExpiracao) { this.dtExpiracao = dtExpiracao; }
    public String getDsNotas() { return dsNotas; }
    public void setDsNotas(String dsNotas) { this.dsNotas = dsNotas; }
    public Boolean getFgSelecionada() { return fgSelecionada; }
    public void setFgSelecionada(Boolean fgSelecionada) { this.fgSelecionada = fgSelecionada; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public void setDtCadastro(LocalDateTime dtCadastro) { this.dtCadastro = dtCadastro; }
    public Boolean getFgExcluido() { return fgExcluido; }
    public void setFgExcluido(Boolean fgExcluido) { this.fgExcluido = fgExcluido; }
}
