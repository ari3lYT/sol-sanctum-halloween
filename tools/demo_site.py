#!/usr/bin/env python3
"""Real site/API with temporary, fictional player records; no production access."""
import importlib.util
from pathlib import Path
import secrets
import tempfile

root=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('event_app',root/'website/server/app.py')
app=importlib.util.module_from_spec(spec);spec.loader.exec_module(app)

if __name__=='__main__':
    with tempfile.TemporaryDirectory() as directory:
        store=app.Store(Path(directory)/'demo.sqlite3')
        for i,(name,kills,ordinary,unique) in enumerate([('DemoBuilder',420,81,12),('DemoExplorer',315,67,9),('DemoFarmer',268,54,8),('DemoHelper',192,42,6),('DemoMiner',146,31,3)],1):
            payload={'schema':1,'installationId':f'10000000-0000-0000-0000-{i:012d}',
                     'playerUuid':f'00000000-0000-0000-0000-{i:012d}','playerName':name,
                     'profileId':f'{i:064x}','sequence':1,'minecraftVersion':'26.3','modVersion':'1.1.4',
                     'stats':{'mobKills':{'minecraft:zombie':kills},'ordinaryCandies':ordinary,
                              'uniqueCandies':unique,'startedAt':0}}
            store.ingest(payload,secrets.token_urlsafe(32))
        server=app.AppServer(('127.0.0.1',18126),store,root/'website/dist',['http://127.0.0.1:18126'])
        print('Synthetic demo: http://127.0.0.1:18126/halloween/',flush=True)
        server.serve_forever()
