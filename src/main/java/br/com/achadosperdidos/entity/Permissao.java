package br.com.achadosperdidos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "permissao")
public class Permissao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Permissao") private Long id;
    @Column(name = "NM_Permissao", nullable = false, length = 100) private String nmPermissao;
    @Column(name = "NM_Modulo", length = 50) private String nmModulo;
    @Column(name = "NM_Acao", length = 50) private String nmAcao;
    @Column(name = "DS_Permissao", length = 500) private String dsPermissao;
    @Column(name = "DT_Cadastro", nullable = false, insertable = false, updatable = false) private LocalDateTime dtCadastro;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Ativo", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgAtivo = true;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "FG_Excluido", nullable = false, columnDefinition = "TINYINT(1)") private Boolean fgExcluido = false;

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getNmPermissao() { return nmPermissao; } public void setNmPermissao(String v) { this.nmPermissao = v; }
    public String getNmModulo() { return nmModulo; } public void setNmModulo(String v) { this.nmModulo = v; }
    public String getNmAcao() { return nmAcao; } public void setNmAcao(String v) { this.nmAcao = v; }
    public String getDsPermissao() { return dsPermissao; } public void setDsPermissao(String v) { this.dsPermissao = v; }
    public LocalDateTime getDtCadastro() { return dtCadastro; }
    public Boolean getFgAtivo() { return fgAtivo; } public void setFgAtivo(Boolean v) { this.fgAtivo = v; }
    public Boolean getFgExcluido() { return fgExcluido; } public void setFgExcluido(Boolean v) { this.fgExcluido = v; }
}
