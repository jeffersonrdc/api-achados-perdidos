-- =====================================================================
-- 075_Remover_Permissoes_Crianca_Lacre.sql
-- Remove do catálogo e dos vínculos de perfil/usuário as permissões
-- de crianças e lacres (módulos fora do escopo atual do produto).
-- Idempotente.
-- =====================================================================

UPDATE perfil_permissao pp
JOIN permissao pe ON pe.ID_Permissao = pp.IDR_Permissao
SET pp.FG_Ativo = 0,
    pp.FG_Excluido = 1,
    pp.DT_Alteracao = NOW()
WHERE pe.NM_Modulo IN ('crianca', 'lacre')
   OR pe.NM_Permissao IN (
        'crianca.listar', 'crianca.gerenciar',
        'lacre.listar', 'lacre.gerenciar'
      );

UPDATE usuario_permissao up
JOIN permissao pe ON pe.ID_Permissao = up.IDR_Permissao
SET up.FG_Ativo = 0,
    up.FG_Excluido = 1,
    up.DT_Alteracao = NOW()
WHERE pe.NM_Modulo IN ('crianca', 'lacre')
   OR pe.NM_Permissao IN (
        'crianca.listar', 'crianca.gerenciar',
        'lacre.listar', 'lacre.gerenciar'
      );

UPDATE permissao
SET FG_Ativo = 0,
    FG_Excluido = 1
WHERE NM_Modulo IN ('crianca', 'lacre')
   OR NM_Permissao IN (
        'crianca.listar', 'crianca.gerenciar',
        'lacre.listar', 'lacre.gerenciar'
      );
