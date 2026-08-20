-- =====================================================================
-- 076_Modulos_E_Fluxo_Triagem.sql
-- Parâmetros globais: módulos do painel (antes da permissão) e
-- exigência de triagem antes do estoque/portal.
-- Idempotente.
-- =====================================================================

INSERT INTO sistema_parametro (NM_Chave, DS_Valor, DS_Descricao)
VALUES (
  'FLUXO_TRIAGEM_OBRIGATORIA',
  'true',
  'Se true, novos itens entram na fila de triagem. Se false, vão direto ao estoque e ao portal.'
)
ON DUPLICATE KEY UPDATE
  DS_Descricao = VALUES(DS_Descricao);

INSERT INTO sistema_parametro (NM_Chave, DS_Valor, DS_Descricao)
VALUES
 ('MODULO_DASHBOARD', 'true', 'Módulo Dashboard habilitado'),
 ('MODULO_ITENS_PERDIDOS', 'true', 'Módulo Comunicações de Perda habilitado'),
 ('MODULO_ITENS', 'true', 'Módulo Registro de Achados habilitado'),
 ('MODULO_TRIAGEM', 'true', 'Módulo Triagem habilitado'),
 ('MODULO_ESTOQUE', 'true', 'Módulo Estoque habilitado'),
 ('MODULO_TRANSFERENCIAS', 'true', 'Módulo Transferências habilitado'),
 ('MODULO_PEDIDOS', 'true', 'Módulo Pedidos de Devolução habilitado'),
 ('MODULO_DEVOLUCAO_RAPIDA', 'true', 'Módulo Devolução Rápida habilitado'),
 ('MODULO_DEVOLUCOES', 'true', 'Módulo Devoluções habilitado'),
 ('MODULO_EVENTOS', 'true', 'Módulo Eventos habilitado'),
 ('MODULO_CARACTERISTICAS', 'true', 'Módulo Características habilitado'),
 ('MODULO_LOGISTICA_FISICA', 'true', 'Módulo Logística Física habilitado'),
 ('MODULO_LOCAIS', 'true', 'Módulo Locais habilitado'),
 ('MODULO_USUARIOS', 'true', 'Módulo Usuários habilitado'),
 ('MODULO_EQUIPES', 'true', 'Módulo Equipes habilitado'),
 ('MODULO_RELATORIOS', 'true', 'Módulo Relatórios habilitado'),
 ('MODULO_ANALYTICS', 'true', 'Módulo Analytics habilitado'),
 ('MODULO_CONFIGURACOES', 'true', 'Módulo Configurações habilitado (sempre on)'),
 ('MODULO_PERFIL', 'true', 'Módulo Perfis habilitado (sempre on)'),
 ('MODULO_PERMISSOES', 'true', 'Módulo Permissões habilitado (sempre on)'),
 ('MODULO_LOGS', 'true', 'Módulo Logs de Auditoria habilitado')
ON DUPLICATE KEY UPDATE
  DS_Descricao = VALUES(DS_Descricao);
