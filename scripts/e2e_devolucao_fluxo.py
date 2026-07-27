#!/usr/bin/env python3
"""E2E: fluxo PICKUP + SHIPPING com e-mail real."""
from __future__ import annotations

import datetime as dt
import json
import traceback
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

import pymysql
import requests

BASE = "http://localhost:8080/api/v1"
EMAIL = "jeffersonrdc@gmail.com"
ROOT = Path(__file__).resolve().parents[1]


def load_env() -> dict[str, str]:
    vals: dict[str, str] = {}
    for line in (ROOT / ".env").read_text(encoding="utf-8", errors="ignore").splitlines():
        s = line.strip()
        if not s or s.startswith("#") or "=" not in s:
            continue
        k, v = s.split("=", 1)
        v = v.strip().strip('"').strip("'")
        vals[k.strip()] = v
    return vals


def db():
    vals = load_env()
    return pymysql.connect(
        host=vals.get("DB_HOST", "localhost"),
        port=int(vals.get("DB_PORT", 3306)),
        user=vals.get("DB_USERNAME") or vals.get("DB_USER", "root"),
        password=vals.get("DB_PASSWORD") or vals.get("DB_PASS") or "",
        database=vals.get("DB_NAME", "achados_perdidos"),
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
    )


def req(method: str, path: str, body=None, token=None):
    url = BASE + path
    headers = {}
    data = None
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if body is not None:
        data = json.dumps(body).encode()
        headers["Content-Type"] = "application/json"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=90) as resp:
            raw = resp.read().decode()
            return resp.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            j = json.loads(raw)
        except Exception:
            j = {"raw": raw}
        return e.code, j


def main():
    today = dt.date.today().isoformat()
    now = dt.datetime.now().strftime("%H:%M:%S")
    results = {}

    st, login = req("POST", "/auth/login", {"identificador": "admin", "senha": "senha123"})
    assert st == 200 and "accessToken" in login, login
    token = login["accessToken"]
    print("LOGIN OK")

    st, evs = req("GET", "/eventos?page=1&limit=5", token=token)
    evento = (evs.get("content") or evs)[0]
    id_evento = evento["id"]
    print("EVENTO", evento.get("nmEvento"))

    st, cats = req("GET", f"/categorias?idEvento={urllib.parse.quote(id_evento)}&page=1&limit=50", token=token)
    cc = cats.get("content") or []
    cat = next((c for c in cc if "eletr" in (c.get("nmCategoria") or "").lower()), cc[0])
    id_cat = cat["id"]
    print("CATEGORIA", cat.get("nmCategoria"))

    st, statuses = req("GET", "/status-itens?page=1&limit=100", token=token)
    ss = statuses.get("content") if isinstance(statuses, dict) else statuses
    ss = ss or []
    id_estoque = None
    for s in ss:
        if (s.get("nmStatus") or "") == "Em estoque":
            id_estoque = s["id"]
            break
    if not id_estoque:
        for s in ss:
            if "estoque" in (s.get("nmStatus") or "").lower() and "transporte" not in (
                s.get("nmStatus") or ""
            ).lower():
                id_estoque = s["id"]
                break
    print("STATUS ESTOQUE", id_estoque)

    def create_item(titulo: str):
        body = {
            "idEvento": id_evento,
            "idCategoria": id_cat,
            "nmTitulo": titulo,
            "dsItem": f"Item de teste E2E {titulo}",
            "nmMarca": "Apple",
            "nmModelo": "iPhone 13",
            "nmCor": "Azul",
            "nmEstado": "Bom",
            "dtEncontrado": today,
            "hrEncontrado": now,
            "nmLocalEncontrado": "Palco Mundo",
            "tpPrioridade": "MEDIA",
            "fgSensivel": False,
        }
        if id_estoque:
            body["idStatus"] = id_estoque
        st, item = req("POST", "/itens", body, token=token)
        print("CREATE ITEM", st, item.get("id") or item)
        assert st in (200, 201), item
        return item

    def create_claim(nome_obj: str, sufixo: str):
        body = {
            "idEvento": id_evento,
            "idCategoria": id_cat,
            "tpClaim": "RETIRADA",
            "nmStatus": "Claim Aberto",
            "nmNome": f"Jefferson Teste {sufixo}",
            "nrCpf": "52998224725",
            "nmEmail": EMAIL,
            "nrTelefone": "21999990000",
            "nmObjeto": nome_obj,
            "dsObjeto": f"Teste E2E fluxo {sufixo}",
            "nmMarca": "Apple",
            "nmModelo": "iPhone 13",
            "nmCor": "Azul",
            "nmEstado": "Bom",
            "dtPerdeu": today,
            "hrPerdeu": "10:00:00",
            "nmLocal": "Palco Mundo",
            "tpPrioridade": "MEDIA",
        }
        st, claim = req("POST", "/claims", body, token=token)
        print("CREATE CLAIM", sufixo, st, claim.get("id") or claim)
        assert st in (200, 201), claim
        return claim

    def analisar(claim_id: str):
        st, r = req("POST", f"/claims/{claim_id}/analise", {}, token=token)
        print("ANALISE", st, r.get("nmStatus") or r)

    def aprovar(claim_id: str, item_id: str):
        st, r = req(
            "POST",
            f"/claims/{claim_id}/aprovar",
            {"idItem": item_id, "dsJustificativa": "Teste E2E automatizado — match confirmado."},
            token=token,
        )
        print("APROVAR", st, r.get("id") or r)
        assert st == 200, r
        return r

    def choose_token_for_protocol(protocolo: str | None = None):
        conn = db()
        cur = conn.cursor()
        if protocolo:
            cur.execute(
                """
                SELECT t.CD_Token, d.CD_Protocolo, d.TP_Status
                FROM devolucao_acao_token t
                JOIN devolucao d ON d.ID_Devolucao = t.IDR_Devolucao
                WHERE d.CD_Protocolo=%s AND t.TP_Acao='CHOOSE_DELIVERY_METHOD'
                  AND t.FG_Excluido=0 AND t.DT_Usado IS NULL
                ORDER BY t.ID_DevolucaoAcaoToken DESC LIMIT 1
                """,
                (protocolo,),
            )
        else:
            cur.execute(
                """
                SELECT t.CD_Token, d.CD_Protocolo, d.TP_Status, d.ID_Devolucao
                FROM devolucao_acao_token t
                JOIN devolucao d ON d.ID_Devolucao = t.IDR_Devolucao
                JOIN claim c ON c.ID_Claim = d.IDR_Claim
                WHERE c.NM_Email=%s AND t.TP_Acao='CHOOSE_DELIVERY_METHOD'
                  AND t.FG_Excluido=0 AND t.DT_Usado IS NULL AND t.DT_Expiracao > NOW()
                ORDER BY t.ID_DevolucaoAcaoToken DESC LIMIT 1
                """,
                (EMAIL,),
            )
        row = cur.fetchone()
        conn.close()
        return row

    def find_dev_by_claim(claim_id: str):
        st, det_list = req(
            "GET",
            f"/devolucoes?idEvento={urllib.parse.quote(id_evento)}&page=1&limit=100",
            token=token,
        )
        for d in det_list.get("content") or []:
            if d.get("idClaim") == claim_id:
                return d
        return None

    def token_by_acao(protocolo: str, acao: str):
        conn = db()
        cur = conn.cursor()
        cur.execute(
            """
            SELECT t.CD_Token FROM devolucao_acao_token t
            JOIN devolucao d ON d.ID_Devolucao = t.IDR_Devolucao
            WHERE d.CD_Protocolo=%s AND t.TP_Acao=%s AND t.FG_Excluido=0
            ORDER BY t.ID_DevolucaoAcaoToken DESC LIMIT 1
            """,
            (protocolo, acao),
        )
        row = cur.fetchone()
        conn.close()
        return row["CD_Token"] if row else None

    # ---------- PICKUP ----------
    print("\n===== FLOW PICKUP =====")
    item_p = create_item("E2E Pickup Fone Azul v2")
    claim_p = create_claim("E2E Pickup Fone Azul v2", "PICKUP")
    analisar(claim_p["id"])
    aprovar(claim_p["id"], item_p["id"])

    dev_pick = find_dev_by_claim(claim_p["id"])
    assert dev_pick, "devolucao pickup nao encontrada"
    proto_pick = dev_pick.get("protocol")
    print("DEV PICKUP", dev_pick["id"], proto_pick, dev_pick.get("tpStatus"))

    row = choose_token_for_protocol(proto_pick) or choose_token_for_protocol()
    assert row, "token modalidade pickup ausente"
    tok_pick = row["CD_Token"]
    print("TOKEN PICKUP", tok_pick[:20], "...")

    st, ctx = req("GET", f"/portal/devolucoes/{tok_pick}")
    print("CONTEXTO", st, {k: ctx.get(k) for k in ["actionType", "protocolo", "status", "expired", "used"]})

    st, r = req("POST", f"/portal/devolucoes/{tok_pick}/modalidade", {"method": "PICKUP"})
    print("MODALIDADE PICKUP", st, r)
    st, r = req("POST", f"/portal/devolucoes/{tok_pick}/pickup/request", {})
    print("PICKUP REQUEST", st, r)

    tomorrow = (dt.date.today() + dt.timedelta(days=1)).isoformat()
    st, r = req(
        "POST",
        f"/devolucoes/{dev_pick['id']}/pickup/options",
        {
            "sendEmail": True,
            "options": [
                {
                    "date": tomorrow,
                    "startTime": "10:00:00",
                    "endTime": "12:00:00",
                    "pickupLocationName": "Achados e Perdidos — Portão A",
                    "notes": "Levar documento com foto",
                },
                {
                    "date": tomorrow,
                    "startTime": "14:00:00",
                    "endTime": "16:00:00",
                    "pickupLocationName": "Achados e Perdidos — Portão A",
                    "notes": "Levar documento com foto",
                },
            ],
        },
        token=token,
    )
    print("PICKUP OPTIONS", st, r.get("tpStatus") if isinstance(r, dict) else r)

    tok_conf = token_by_acao(proto_pick, "CONFIRM_PICKUP_OPTION")
    assert tok_conf, "token confirmacao ausente"
    st, ctx = req("GET", f"/portal/devolucoes/{tok_conf}")
    opts = ctx.get("pickupOptions") or []
    print("CTX CONFIRM", st, "options", len(opts))
    assert opts, ctx
    st, r = req("POST", f"/portal/devolucoes/{tok_conf}/pickup/confirm", {"optionId": opts[0]["id"]})
    print("CONFIRM", st, r)

    st, r = req(
        "PUT",
        f"/devolucoes/{dev_pick['id']}/status",
        {"tpStatus": "EM_CONFERENCIA", "dsObservacao": "E2E conferencia"},
        token=token,
    )
    print("EM_CONFERENCIA", st, r.get("tpStatus") if isinstance(r, dict) else r)

    pdf = b"%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n"
    rr = requests.post(
        f"{BASE}/devolucoes/{dev_pick['id']}/termo",
        headers={"Authorization": f"Bearer {token}"},
        files={"file": ("termo.pdf", pdf, "application/pdf")},
        timeout=90,
    )
    print("TERMO", rr.status_code, rr.text[:180])
    st, r = req(
        "POST",
        f"/devolucoes/{dev_pick['id']}/concluir-presencial",
        {"dsObservacao": "E2E baixa OK"},
        token=token,
    )
    print("CONCLUIR", st, r.get("tpStatus") if isinstance(r, dict) else r)
    results["pickup"] = {
        "protocol": proto_pick,
        "devolucaoId": dev_pick["id"],
        "status_final": r.get("tpStatus") if isinstance(r, dict) else r,
        "http": st,
        "modalidade_link": f"http://localhost:4300/devolucao/escolher-modalidade?token={tok_pick}",
    }

    # ---------- SHIPPING ----------
    print("\n===== FLOW SHIPPING =====")
    item_s = create_item("E2E Shipping Fone Azul v2")
    claim_s = create_claim("E2E Shipping Fone Azul v2", "SHIPPING")
    analisar(claim_s["id"])
    aprovar(claim_s["id"], item_s["id"])

    dev_ship = find_dev_by_claim(claim_s["id"])
    assert dev_ship, "devolucao shipping nao encontrada"
    proto_ship = dev_ship.get("protocol")
    print("DEV SHIP", dev_ship["id"], proto_ship, dev_ship.get("tpStatus"))

    row = choose_token_for_protocol(proto_ship)
    assert row, "token modalidade shipping ausente"
    tok_ship = row["CD_Token"]

    st, r = req("POST", f"/portal/devolucoes/{tok_ship}/modalidade", {"method": "SHIPPING"})
    print("MODALIDADE SHIP", st, r)

    tok_addr = token_by_acao(proto_ship, "SUBMIT_SHIPPING_ADDRESS")
    assert tok_addr, "token endereco ausente"
    st, r = req(
        "POST",
        f"/portal/devolucoes/{tok_addr}/shipping/address",
        {
            "recipientName": "Jefferson Teste",
            "zipCode": "22041080",
            "street": "Av Atlantica",
            "number": "1000",
            "complement": "Apto 101",
            "district": "Copacabana",
            "city": "Rio de Janeiro",
            "state": "RJ",
            "phone": "21999990000",
        },
    )
    print("ADDRESS", st, r)

    st, r = req(
        "POST",
        f"/devolucoes/{dev_ship['id']}/shipping/quote",
        {
            "amount": 45.90,
            "currency": "BRL",
            "estimatedDeliveryDays": 5,
            "postingDeadlineDaysAfterPayment": 2,
            "paymentInstructions": "PIX chave jeffersonrdc@gmail.com — frete R$ 45,90 (teste E2E)",
            "sendEmail": True,
        },
        token=token,
    )
    print("QUOTE", st, r.get("tpStatus") if isinstance(r, dict) else r)

    tok_pay = token_by_acao(proto_ship, "UPLOAD_PAYMENT_PROOF")
    assert tok_pay, "token pagamento ausente"
    rr = requests.post(
        f"{BASE}/portal/devolucoes/{tok_pay}/shipping/payment-proof",
        files={"comprovante": ("comprovante.pdf", b"%PDF-1.4 proof\n%%EOF\n", "application/pdf")},
        timeout=90,
    )
    print("PAYMENT", rr.status_code, rr.text[:180])

    st, r = req(
        "POST",
        f"/devolucoes/{dev_ship['id']}/shipping/posting",
        {
            "postingDate": today,
            "trackingCode": "BR123456789BR",
            "sendEmail": True,
            "notes": "E2E postagem",
        },
        token=token,
    )
    print("POSTING", st, r.get("tpStatus") if isinstance(r, dict) else r)

    tok_tr = token_by_acao(proto_ship, "VIEW_TRACKING")
    if tok_tr:
        st_tr, tr = req("GET", f"/portal/devolucoes/{tok_tr}/tracking")
        print("TRACKING", st_tr, tr)
        track_link = f"http://localhost:4300/devolucao/rastreio?token={tok_tr}"
    else:
        track_link = None

    results["shipping"] = {
        "protocol": proto_ship,
        "devolucaoId": dev_ship["id"],
        "status_final": r.get("tpStatus") if isinstance(r, dict) else r,
        "http": st,
        "tracking": "BR123456789BR",
        "modalidade_link": f"http://localhost:4300/devolucao/escolher-modalidade?token={tok_ship}",
        "rastreio_link": track_link,
    }

    # historico e-mails
    conn = db()
    cur = conn.cursor()
    cur.execute(
        """
        SELECT d.CD_Protocolo, h.TP_Evento, h.NM_Titulo, h.FG_EmailEnviado, h.DS_EmailErro
        FROM devolucao_historico h
        JOIN devolucao d ON d.ID_Devolucao = h.IDR_Devolucao
        WHERE d.CD_Protocolo IN (%s, %s)
        ORDER BY h.DT_Cadastro
        """,
        (proto_pick, proto_ship),
    )
    hist = cur.fetchall()
    conn.close()
    print("\n===== HISTORICO / EMAILS =====")
    for h in hist:
        print(
            h["CD_Protocolo"],
            h["TP_Evento"],
            "email=",
            bool(h["FG_EmailEnviado"]),
            "err=",
            h["DS_EmailErro"],
        )

    print("\n===== RESULTADO =====")
    print(json.dumps(results, indent=2, ensure_ascii=False))
    print(f"\nE-mails devem ter chegado em {EMAIL}")
    print("Se SMTP falhar, use os links modality/rastreio acima (tokens no resultado).")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        traceback.print_exc()
        raise
