#!/usr/bin/env python3
"""Reconstruit les modules PC depuis les fragments base64 et lance JARVIX."""
import base64, zipfile, io, sys
from pathlib import Path
here = Path(__file__).resolve().parent
parts = sorted(here.glob("pc_core.b64.*"))
if not parts:
    print("Fragments pc_core.b64.* manquants")
    sys.exit(1)
data = base64.b64decode("".join(p.read_text().strip() for p in parts))
with zipfile.ZipFile(io.BytesIO(data)) as zf:
    zf.extractall(here)
print("Modules PC extraits.")
print("Lancement de l'interface...")
import jarvix_gui
jarvix_gui.main()
