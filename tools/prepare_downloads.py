#!/usr/bin/env python3
from pathlib import Path
import shutil

root=Path(__file__).resolve().parents[1]
target=root/'website/public/downloads';target.mkdir(parents=True,exist_ok=True)
for version in ('1.21.11','26.2','26.3'):
    source=root/'mod/versions'/version/'build/libs'/f'sol-sanctum-halloween-{version}-1.1.4.jar'
    if not source.is_file():raise SystemExit('Build the mod first: '+str(source))
    shutil.copy2(source,target/source.name)
    print(source.name)
