import importlib.util
import json
import secrets
import tempfile
import threading
import unittest
import urllib.request
import urllib.error
from pathlib import Path
spec=importlib.util.spec_from_file_location('app',Path(__file__).parents[1]/'server/app.py');app=importlib.util.module_from_spec(spec);spec.loader.exec_module(app)

def payload(player='00000000-0000-0000-0000-000000000001',installation='10000000-0000-0000-0000-000000000001',sequence=1,profile='a'*64):
 return {'schema':1,'installationId':installation,'playerUuid':player,'playerName':'PlayerOne','profileId':profile,'sequence':sequence,'minecraftVersion':'26.3','modVersion':'1.1.0','stats':{'mobKills':{'minecraft:zombie':3,'minecraft:skeleton':2,'minecraft:zombified_piglin':10},'ordinaryCandies':1,'uniqueCandies':1,'startedAt':1000}}

class APITest(unittest.TestCase):
 def setUp(self):
  self.directory=tempfile.TemporaryDirectory();self.store=app.Store(Path(self.directory.name)/'db.sqlite3');self.key=secrets.token_urlsafe(32)
 def tearDown(self):self.directory.cleanup()
 def test_weighted_points_exclusion_and_no_secrets(self):
  self.store.ingest(payload(),self.key);data=self.store.leaderboard();self.assertEqual(data['players'][0]['kills'],5);self.assertEqual(data['players'][0]['points'],4);self.assertNotIn(self.key,json.dumps(data));self.assertNotIn('installationId',json.dumps(data))
 def test_duplicate_and_stale_snapshots_never_add(self):
  self.assertTrue(self.store.ingest(payload(sequence=2),self.key)['accepted']);self.assertFalse(self.store.ingest(payload(sequence=2),self.key)['accepted']);self.assertFalse(self.store.ingest(payload(sequence=1),self.key)['accepted']);self.assertEqual(self.store.leaderboard()['players'][0]['kills'],5)
 def test_key_is_bound_to_installation(self):
  self.store.ingest(payload(),self.key)
  with self.assertRaises(app.Unauthorized):self.store.ingest(payload(sequence=2),secrets.token_urlsafe(32))
 def test_update_replaces_instead_of_summing(self):
  self.store.ingest(payload(),self.key);p=payload(sequence=2);p['stats']['mobKills']['minecraft:zombie']=4;self.store.ingest(p,self.key);self.assertEqual(self.store.leaderboard()['players'][0]['kills'],6)
 def test_two_devices_same_profile_not_double_counted(self):
  self.store.ingest(payload(),self.key);p=payload(installation='10000000-0000-0000-0000-000000000002');self.store.ingest(p,'x'*40);self.assertEqual(self.store.leaderboard()['players'][0]['kills'],5)
 def test_different_profiles_accumulate(self):
  self.store.ingest(payload(),self.key);self.store.ingest(payload(profile='b'*64),self.key);self.assertEqual(self.store.leaderboard()['players'][0]['kills'],10)
 def test_order_is_kills_first(self):
  self.store.ingest(payload(),self.key);p=payload(player='00000000-0000-0000-0000-000000000002');p['playerName']='PlayerTwo';p['stats']['mobKills']['minecraft:zombie']=10;self.store.ingest(p,self.key);self.assertEqual(self.store.leaderboard()['players'][0]['name'],'PlayerTwo')
 def test_invalid_counts_and_names_rejected(self):
  for field,value in [('playerName','<script>'),('profileId','../x'),('sequence',True)]:
   p=payload();p[field]=value
   with self.assertRaises(app.Invalid):self.store.ingest(p,self.key)
  p=payload();p['stats']['ordinaryCandies']=99
  with self.assertRaises(app.Invalid):self.store.ingest(p,self.key)
 def test_persists_after_reopening(self):
  self.store.ingest(payload(),self.key);new=app.Store(Path(self.directory.name)/'db.sqlite3');self.assertEqual(new.leaderboard()['players'][0]['points'],4)
 def test_http_contract(self):
  public=Path(self.directory.name)/'dist';public.mkdir();(public/'index.html').write_text('<h1>Elysium</h1>');server=app.AppServer(('127.0.0.1',0),self.store,public,['http://localhost']);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start();base=f'http://127.0.0.1:{server.server_port}/halloween'
  try:
   request=urllib.request.Request(base+'/api/ingest',json.dumps(payload()).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+self.key})
   self.assertEqual(urllib.request.urlopen(request).status,200)
   data=json.load(urllib.request.urlopen(base+'/api/leaderboard'));self.assertEqual(data['players'][0]['kills'],5)
   bad=urllib.request.Request(base+'/api/ingest',json.dumps(payload()).encode(),headers={'Content-Type':'application/json','Authorization':'Bearer '+self.key,'Origin':'https://evil.example'})
   with self.assertRaises(urllib.error.HTTPError) as failure:urllib.request.urlopen(bad)
   self.assertEqual(failure.exception.code,403)
   with self.assertRaises(urllib.error.HTTPError):urllib.request.urlopen(base+'/assets/../../server/app.py')
  finally:server.shutdown();server.server_close();thread.join()

if __name__=='__main__':unittest.main()
