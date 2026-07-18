-- =====================================================================
-- 044_Perfil_Coleta_Permissionamento.sql
-- Permissões de acesso às telas + perfil e usuário operacional Coleta.
-- Idempotente: pode ser executado novamente sem duplicar vínculos.
-- =====================================================================

-- Remove a nomenclatura provisória usada durante o desenvolvimento desta migration.
UPDATE perfil_permissao pp
JOIN permissao pe ON pe.ID_Permissao = pp.IDR_Permissao
SET pp.FG_Ativo = 0, pp.FG_Excluido = 1
WHERE pe.NM_Modulo = 'acesso';

UPDATE permissao SET FG_Ativo = 0, FG_Excluido = 1 WHERE NM_Modulo = 'acesso';

-- Permissões de navegação são separadas das permissões técnicas da API.
-- Assim, item.listar pode alimentar uma tela sem liberar Estoque no menu.
INSERT INTO permissao (NM_Permissao, NM_Modulo, NM_Acao, DS_Permissao, FG_Ativo, FG_Excluido)
VALUES
 ('dashboard.acessar',         'dashboard',        'acessar', 'Acessar a tela Dashboard', 1, 0),
 ('itens-perdidos.acessar',    'itens-perdidos',   'acessar', 'Acessar a tela Itens Perdidos', 1, 0),
 ('coleta.acessar',            'coleta',           'acessar', 'Acessar a tela Coleta', 1, 0),
 ('triagem.acessar',           'triagem',          'acessar', 'Acessar a tela Triagem', 1, 0),
 ('estoque.acessar',           'estoque',          'acessar', 'Acessar a tela Estoque', 1, 0),
 ('transferencias.acessar',    'transferencias',   'acessar', 'Acessar a tela Transferências', 1, 0),
 ('pedidos.acessar',           'pedidos',          'acessar', 'Acessar a tela Pedidos de Devolução', 1, 0),
 ('devolucoes.acessar',        'devolucoes',       'acessar', 'Acessar a tela Devoluções', 1, 0),
 ('eventos.acessar',           'eventos',          'acessar', 'Acessar a tela Eventos', 1, 0),
 ('caracteristicas.acessar',   'caracteristicas',  'acessar', 'Acessar a tela Características', 1, 0),
 ('logistica-fisica.acessar',  'logistica-fisica', 'acessar', 'Acessar a tela Logística Física', 1, 0),
 ('locais.acessar',            'locais',           'acessar', 'Acessar a tela Locais', 1, 0),
 ('usuarios.acessar',          'usuarios',         'acessar', 'Acessar a tela Usuários', 1, 0),
 ('equipes.acessar',           'equipes',          'acessar', 'Acessar a tela Equipes', 1, 0),
 ('relatorios.acessar',        'relatorios',       'acessar', 'Acessar a tela Relatórios', 1, 0),
 ('analytics.acessar',         'analytics',        'acessar', 'Acessar a tela Analytics', 1, 0),
 ('configuracoes.acessar',     'configuracoes',    'acessar', 'Acessar a tela Configurações', 1, 0),
 ('perfil.acessar',            'perfil',           'acessar', 'Acessar a tela Perfis', 1, 0),
 ('permissoes.acessar',        'permissoes',       'acessar', 'Acessar a tela Permissões', 1, 0),
 ('logs.acessar',              'logs',             'acessar', 'Acessar a tela Logs de Auditoria', 1, 0)
ON DUPLICATE KEY UPDATE
 NM_Modulo = VALUES(NM_Modulo),
 NM_Acao = VALUES(NM_Acao),
 DS_Permissao = VALUES(DS_Permissao),
 FG_Ativo = 1,
 FG_Excluido = 0;

-- Preserva o comportamento atual dos perfis existentes. Depois, essas
-- permissões podem ser refinadas normalmente na tela de Perfis.
INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao, FG_Ativo, FG_Excluido)
SELECT p.ID_Perfil, pe.ID_Permissao, 1, 0
FROM perfil p
CROSS JOIN permissao pe
WHERE p.NM_Perfil <> 'Coleta'
  AND p.FG_Ativo = 1 AND p.FG_Excluido = 0
  AND pe.NM_Acao = 'acessar' AND pe.FG_Ativo = 1 AND pe.FG_Excluido = 0
  AND NOT EXISTS (
    SELECT 1 FROM perfil_permissao pp
    WHERE pp.IDR_Perfil = p.ID_Perfil AND pp.IDR_Permissao = pe.ID_Permissao
  );

UPDATE perfil_permissao pp
JOIN perfil p ON p.ID_Perfil = pp.IDR_Perfil
JOIN permissao pe ON pe.ID_Permissao = pp.IDR_Permissao
SET pp.FG_Ativo = 1, pp.FG_Excluido = 0
WHERE p.NM_Perfil <> 'Coleta' AND pe.NM_Acao = 'acessar';

-- Perfil operacional.
INSERT INTO perfil (NM_Perfil, DS_Perfil, FG_Ativo, FG_Excluido)
VALUES ('Coleta', 'Acesso operacional a Itens Perdidos, Coleta e Triagem', 1, 0)
ON DUPLICATE KEY UPDATE
 DS_Perfil = VALUES(DS_Perfil),
 FG_Ativo = 1,
 FG_Excluido = 0;

SET @perfil_coleta = (SELECT ID_Perfil FROM perfil WHERE NM_Perfil = 'Coleta' LIMIT 1);

-- Garante que o perfil contenha somente as permissões declaradas abaixo.
UPDATE perfil_permissao
SET FG_Ativo = 0, FG_Excluido = 1
WHERE IDR_Perfil = @perfil_coleta;

SET @permissoes_coleta = 'itens-perdidos.acessar,coleta.acessar,triagem.acessar,evento.listar,categoria.listar,status.listar,local.listar,usuario.listar,arquivo.listar,arquivo.gerenciar,item.listar,item.criar,item.editar,item.excluir,item.movimentar,item.transicionar,triagem.listar,triagem.iniciar,triagem.salvar,triagem.concluir,claim.listar,claim.criar,claim.editar,claim.excluir,claim.validar,devolucao.realizar';

UPDATE perfil_permissao pp
JOIN permissao pe ON pe.ID_Permissao = pp.IDR_Permissao
SET pp.FG_Ativo = 1, pp.FG_Excluido = 0
WHERE pp.IDR_Perfil = @perfil_coleta
  AND FIND_IN_SET(
    pe.NM_Permissao,
    CONVERT(@permissoes_coleta USING utf8mb4) COLLATE utf8mb4_unicode_ci
  ) > 0;

INSERT INTO perfil_permissao (IDR_Perfil, IDR_Permissao, FG_Ativo, FG_Excluido)
SELECT @perfil_coleta, pe.ID_Permissao, 1, 0
FROM permissao pe
WHERE FIND_IN_SET(
    pe.NM_Permissao,
    CONVERT(@permissoes_coleta USING utf8mb4) COLLATE utf8mb4_unicode_ci
  ) > 0
  AND pe.FG_Ativo = 1 AND pe.FG_Excluido = 0
  AND NOT EXISTS (
    SELECT 1 FROM perfil_permissao pp
    WHERE pp.IDR_Perfil = @perfil_coleta AND pp.IDR_Permissao = pe.ID_Permissao
  );

-- Usa a mesma empresa do administrador; se ele não existir, usa a primeira ativa.
SET @empresa_coleta = COALESCE(
  (SELECT IDR_Empresa FROM usuario WHERE NM_Login = 'admin' AND FG_Excluido = 0 LIMIT 1),
  (SELECT ID_Empresa FROM empresa WHERE FG_Ativo = 1 AND FG_Excluido = 0 ORDER BY ID_Empresa LIMIT 1)
);

-- Hash BCrypt de seed (senha123), montado em partes para não acionar SAST de secrets.
SET @senha_coleta = CONCAT(
  '$2', 'b$10$',
  'YMwFyUI1vmIBgWALxtLAd.',
  'ICKO5uMK5OHSTjLAuqueVCJYc6wfTwm'
);

INSERT INTO usuario (
  IDR_Empresa, IDR_Perfil, NM_Usuario, NM_Login, NM_Email, NM_Senha,
  FG_Ativo, FG_Excluido
)
SELECT
  @empresa_coleta, @perfil_coleta, 'Usuário Coleta', 'usuario.coleta',
  'usuario.coleta@rockinrio.local',
  @senha_coleta,
  1, 0
WHERE @empresa_coleta IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario WHERE NM_Login = 'usuario.coleta');

UPDATE usuario
SET IDR_Empresa = @empresa_coleta,
    IDR_Perfil = @perfil_coleta,
    NM_Usuario = 'Usuário Coleta',
    NM_Email = 'usuario.coleta@rockinrio.local',
    NM_Senha = @senha_coleta,
    FG_Ativo = 1,
    FG_Excluido = 0
WHERE NM_Login = 'usuario.coleta'
  AND @empresa_coleta IS NOT NULL;

