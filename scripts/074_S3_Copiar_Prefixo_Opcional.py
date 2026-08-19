"""Copia objetos S3 do prefixo acidental '# opcional/' para a key do banco (NM_Path).

Não imprime credenciais. Uso:
  python scripts/074_S3_Copiar_Prefixo_Opcional.py
(variáveis lidas de api/.env)
"""
from __future__ import annotations

from pathlib import Path

import boto3
from botocore.exceptions import ClientError


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


def main() -> None:
    env = load_env()
    bucket = env["APP_S3_BUCKET"]
    s3 = boto3.client(
        "s3",
        region_name=env.get("APP_S3_REGION") or "us-east-1",
        aws_access_key_id=env["AWS_ACCESS_KEY_ID"],
        aws_secret_access_key=env["AWS_SECRET_ACCESS_KEY"],
    )
    prefixo = "# opcional/"
    copiados = 0
    ja = 0
    token = None
    while True:
        kw: dict = {"Bucket": bucket, "Prefix": prefixo, "MaxKeys": 500}
        if token:
            kw["ContinuationToken"] = token
        resp = s3.list_objects_v2(**kw)
        for obj in resp.get("Contents") or []:
            src = obj["Key"]
            dest = src[len(prefixo) :] if src.startswith(prefixo) else src
            if not dest or dest.endswith("/"):
                continue
            try:
                s3.head_object(Bucket=bucket, Key=dest)
                ja += 1
                continue
            except ClientError:
                pass
            s3.copy_object(
                Bucket=bucket,
                Key=dest,
                CopySource={"Bucket": bucket, "Key": src},
            )
            copiados += 1
            print("copied", dest)
        if not resp.get("IsTruncated"):
            break
        token = resp.get("NextContinuationToken")
    print("ok copiados", copiados, "ja existiam", ja)


if __name__ == "__main__":
    main()
