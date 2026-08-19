"""Aplica 074 no MySQL remoto do .env e replica o schema/dados para localhost.

Senhas só via env do subprocess (MYSQL_PWD), nunca na linha de comando.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path


import pymysql


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    p = Path(__file__).resolve().parents[1] / ".env"
    for raw in p.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def mysql_bin(name: str) -> str:
    if shutil.which(name):
        return name
    wb = Path(r"C:\Program Files\MySQL\MySQL Workbench 8.0 CE") / f"{name}.exe"
    if wb.is_file():
        return str(wb)
    raise SystemExit(f"{name} não encontrado no PATH nem no MySQL Workbench")


def apply_sql(conn: pymysql.connections.Connection, sql: str) -> None:
    with conn.cursor() as cur:
        for stmt in sql.split(";"):
            s = stmt.strip()
            if not s or s.startswith("--"):
                continue
            cur.execute(s)
    conn.commit()


def main() -> None:
    env = load_env()
    sql_path = Path(__file__).with_name("074_Arquivo_Somente_S3.sql")
    sql = sql_path.read_text(encoding="utf-8")

    remote = pymysql.connect(
        host=env["DB_HOST"],
        port=int(env.get("DB_PORT") or 3306),
        user=env["DB_USERNAME"],
        password=env["DB_PASSWORD"],
        database=env["DB_NAME"],
        charset="utf8mb4",
        autocommit=False,
    )
    apply_sql(remote, sql)
    with remote.cursor() as cur:
        cur.execute(
            "SELECT TP_Storage, COUNT(*) FROM arquivo WHERE FG_Excluido=0 GROUP BY TP_Storage"
        )
        rows = cur.fetchall()
        cur.execute(
            "SELECT DS_Valor FROM sistema_parametro WHERE NM_Chave=%s",
            ("ARQUIVO_STORAGE_PROVIDER",),
        )
        param = cur.fetchone()
    remote.close()
    print("remoto arquivo por TP_Storage (ativos):", rows)
    print("remoto ARQUIVO_STORAGE_PROVIDER:", param[0] if param else None)

    dump_env = os.environ.copy()
    dump_env["MYSQL_PWD"] = env["DB_PASSWORD"]
    dump_cmd = [
        mysql_bin("mysqldump"),
        "-h", env["DB_HOST"],
        "-P", env.get("DB_PORT") or "3306",
        "-u", env["DB_USERNAME"],
        "--single-transaction",
        "--routines",
        "--default-character-set=utf8mb4",
        env["DB_NAME"],
    ]
    with tempfile.NamedTemporaryFile(suffix=".sql", delete=False) as tmp:
        dump_path = Path(tmp.name)
    with dump_path.open("wb") as out:
        r = subprocess.run(dump_cmd, env=dump_env, stdout=out, stderr=subprocess.PIPE)
    if r.returncode != 0:
        dump_path.unlink(missing_ok=True)
        sys.stderr.write(r.stderr.decode("utf-8", errors="replace"))
        raise SystemExit("mysqldump remoto falhou")
    print("dump temporario bytes", dump_path.stat().st_size)


    local_user = os.environ.get("LOCAL_DB_USER", "root")
    local_pwd = os.environ.get("LOCAL_DB_PASSWORD", "")
    local_env = os.environ.copy()
    local_env["MYSQL_PWD"] = local_pwd
    create = subprocess.run(
        [mysql_bin("mysql"), "-h", "127.0.0.1", "-u", local_user, "-e",
         f"CREATE DATABASE IF NOT EXISTS `{env['DB_NAME']}` DEFAULT CHARACTER SET utf8mb4"],
        env=local_env,
        stderr=subprocess.PIPE,
    )
    if create.returncode != 0:
        sys.stderr.write(create.stderr.decode("utf-8", errors="replace"))
        raise SystemExit("mysql local CREATE DATABASE falhou (defina LOCAL_DB_USER/LOCAL_DB_PASSWORD)")
    try:
        with dump_path.open("rb") as inp:
            load = subprocess.run(
                [mysql_bin("mysql"), "-h", "127.0.0.1", "-u", local_user, env["DB_NAME"]],
                env=local_env,
                stdin=inp,
                stderr=subprocess.PIPE,
            )
    finally:
        dump_path.unlink(missing_ok=True)
    if load.returncode != 0:
        sys.stderr.write(load.stderr.decode("utf-8", errors="replace"))
        raise SystemExit("mysql local import falhou")
    print("localhost alinhado com o remoto")


if __name__ == "__main__":
    main()
