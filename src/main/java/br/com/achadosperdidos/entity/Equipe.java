package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipe")
public class Equipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Equipe") private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Evento", nullable = false) private Evento evento;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "IDR_Local") private Local local;
    @Column(name = "NM_Equipe", nullable = false, length = 150) private String nmEquipe;
    @Column(name = "TP_Equipe", nullable = false, length = 40) private String tpEquipe;
    @Column(name = "DS_Responsabilidade", length = 500) private String dsResponsabilidade;
    @Column(name = "DT_Cadastro", nullable = false) private LocalDateTime dtCadastro;
    @Column(name = "DT_Alteracao") private LocalDateTime dtAlteracao;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Evento getEvento() { return evento; } public void setEvento(Evento v) { this.evento = v; }
    public Local getLocal() { return local; } public void setLocal(Local v) { this.local = v; }
    public String getNmEquipe() { return nmEquipe; } public void setNmEquipe(String v) { this.nmEquipe = v; }
    public String getTpEquipe() { return tpEquipe; } public void setTpEquipe(String v) { this.tpEquipe = v; }
    public String getDsResponsabilidade() { return dsResponsabilidade; } public void setDsResponsabilidade(String v) { this.dsResponsabilidade = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; } public void setDtCadastro(LocalDateTime v) { this.dtCadastro = v; }
    public LocalDateTime getDtAlteracao() { return dtAlteracao; } public void setDtAlteracao(LocalDateTime v) { this.dtAlteracao = v; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
