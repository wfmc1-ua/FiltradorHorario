"""
Extrae texto con coordenadas del PDF del cuadrante y lo guarda como JSON.
El fixture resultante se usa en los golden tests (no requiere Android/PDFBox).
Ejecutar: python scripts/extract_pdf_fixture.py
"""
import json
import pdfplumber
from pathlib import Path

PDF = Path("app/src/test/resources/fixtures/cuadrante_plaza_mar_sem31.pdf")
OUT = Path("app/src/test/resources/fixtures/cuadrante_plaza_mar_sem31.json")

tokens = []
with pdfplumber.open(PDF) as pdf:
    for page_num, page in enumerate(pdf.pages, start=1):
        words = page.extract_words(
            x_tolerance=2,
            y_tolerance=2,
            keep_blank_chars=False,
            use_text_flow=False,
            extra_attrs=["fontname", "size"],
        )
        for w in words:
            tokens.append({
                "text": w["text"],
                "x": round(w["x0"], 2),
                "y": round(w["top"], 2),
                "page": page_num,
            })

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(tokens, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Extraidos {len(tokens)} tokens -> {OUT}")

# Imprime las primeras 60 filas para diagnóstico
print("\n--- MUESTRA (primeros 60 tokens) ---")
for t in tokens[:60]:
    print(f"  y={t['y']:6.1f}  x={t['x']:6.1f}  [{t['text']}]")
