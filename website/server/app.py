#!/usr/bin/env python3
"""Sol Sanctum event snapshots. Standard library only; public totals never expose upload keys."""
import argparse
import hashlib
import hmac
import json
import mimetypes
import os
import re
import secrets
import sqlite3
import threading
import time
from collections import defaultdict, deque
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit

UUID = re.compile(r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$')
PROFILE = re.compile(r'^[0-9a-f]{64}$')
NAME = re.compile(r'^[A-Za-z0-9_]{3,16}$')
RESOURCE = re.compile(r'^[a-z0-9_.-]{1,32}:[a-z0-9_./-]{1,80}$')
VERSION = re.compile(r'^[A-Za-z0-9_.+\- ]{1,48}$')
EXCLUDED = {'minecraft:zombified_piglin', 'minecraft:player'}
LIMIT = 2_000_000_000

class Invalid(ValueError): pass
class Unauthorized(ValueError): pass

def integer(value, name, upper=LIMIT):
    if type(value) is not int or value < 0 or value > upper:
        raise Invalid(f'Invalid {name}')
    return value

def validate(payload):
    if not isinstance(payload, dict) or payload.get('schema') != 1:
        raise Invalid('Invalid schema')
    result = {}
    for key, pattern in [('installationId', UUID), ('playerUuid', UUID), ('profileId', PROFILE), ('playerName', NAME), ('minecraftVersion', VERSION), ('modVersion', VERSION)]:
        value = payload.get(key)
        if not isinstance(value, str) or not pattern.fullmatch(value): raise Invalid(f'Invalid {key}')
        result[key] = value
    result['sequence'] = integer(payload.get('sequence'), 'sequence', 9_000_000_000_000_000)
    stats = payload.get('stats')
    if not isinstance(stats, dict): raise Invalid('Missing stats')
    mobs = stats.get('mobKills')
    if not isinstance(mobs, dict) or len(mobs) > 512: raise Invalid('Invalid mobKills')
    result['mobKills'] = {}
    for mob, count in mobs.items():
        if not isinstance(mob, str) or not RESOURCE.fullmatch(mob): raise Invalid('Invalid mob type')
        n = integer(count, 'mob count')
        if mob not in EXCLUDED and n > 0: result['mobKills'][mob] = n
    result['kills'] = integer(sum(result['mobKills'].values()), 'total kills')
    result['ordinary'] = integer(stats.get('ordinaryCandies'), 'ordinary')
    result['unique'] = integer(stats.get('uniqueCandies'), 'unique')
    if result['ordinary'] + result['unique'] > result['kills']: raise Invalid('More candies than kills')
    result['startedAt'] = integer(stats.get('startedAt', 0), 'startedAt', int(time.time() * 1000) + 86400000)
    return result

class Store:
    def __init__(self, path):
        self.path = str(path)
        Path(path).parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as db:
            db.execute('PRAGMA journal_mode=WAL')
            db.executescript('''
                CREATE TABLE IF NOT EXISTS installations (
                    installation_id TEXT PRIMARY KEY,
                    key_hash TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                );
                CREATE TABLE IF NOT EXISTS reports (
                    installation_id TEXT NOT NULL REFERENCES installations(installation_id),
                    profile_id TEXT NOT NULL,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    sequence INTEGER NOT NULL,
                    kills INTEGER NOT NULL,
                    ordinary INTEGER NOT NULL,
                    unique_count INTEGER NOT NULL,
                    mob_kills TEXT NOT NULL,
                    minecraft_version TEXT NOT NULL,
                    mod_version TEXT NOT NULL,
                    started_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    PRIMARY KEY(installation_id,profile_id,player_uuid)
                );
                CREATE INDEX IF NOT EXISTS reports_player ON reports(player_uuid,profile_id,updated_at);
            ''')
    @contextmanager
    def connect(self):
        db = sqlite3.connect(self.path, timeout=10)
        db.row_factory = sqlite3.Row
        db.execute('PRAGMA foreign_keys=ON')
        try:
            yield db
            db.commit()
        except Exception:
            db.rollback()
            raise
        finally:
            db.close()
    def ingest(self, payload, key):
        data = validate(payload)
        if not isinstance(key, str) or not re.fullmatch(r'[A-Za-z0-9_-]{32,128}', key): raise Unauthorized('Invalid upload key')
        digest = hashlib.sha256(key.encode()).hexdigest()
        now = time.time_ns() // 1_000_000
        with self.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            found = db.execute('SELECT key_hash FROM installations WHERE installation_id=?', (data['installationId'],)).fetchone()
            if found and not hmac.compare_digest(found['key_hash'], digest): raise Unauthorized('Invalid upload key')
            if not found:
                db.execute('INSERT INTO installations VALUES(?,?,?)', (data['installationId'], digest, now))
            cursor = db.execute('''INSERT INTO reports VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(installation_id,profile_id,player_uuid) DO UPDATE SET
                  player_name=excluded.player_name, sequence=excluded.sequence, kills=excluded.kills,
                  ordinary=excluded.ordinary, unique_count=excluded.unique_count, mob_kills=excluded.mob_kills,
                  minecraft_version=excluded.minecraft_version, mod_version=excluded.mod_version,
                  started_at=excluded.started_at, updated_at=excluded.updated_at
                WHERE excluded.sequence > reports.sequence''',
                (data['installationId'], data['profileId'], data['playerUuid'], data['playerName'], data['sequence'],
                 data['kills'], data['ordinary'], data['unique'], json.dumps(data['mobKills'], separators=(',', ':')),
                 data['minecraftVersion'], data['modVersion'], data['startedAt'], now))
            return {'ok': True, 'accepted': bool(cursor.rowcount), 'sequence': data['sequence']}
    def leaderboard(self):
        # One current snapshot per account/server profile, even if the same folder was copied to another device.
        with self.connect() as db:
            rows = db.execute('''WITH latest AS (
                SELECT *, ROW_NUMBER() OVER(PARTITION BY player_uuid,profile_id ORDER BY updated_at DESC,sequence DESC,installation_id) AS n
                FROM reports
              ) SELECT player_uuid, SUM(kills) AS kills, SUM(ordinary) AS ordinary, SUM(unique_count) AS unique_count,
                       MAX(updated_at) AS updated_at
                FROM latest WHERE n=1 GROUP BY player_uuid
                ORDER BY kills DESC, (SUM(ordinary)+3*SUM(unique_count)) DESC, player_uuid LIMIT 10000''').fetchall()
            names = {row['player_uuid']: row['player_name'] for row in db.execute('''SELECT player_uuid,player_name FROM (
                SELECT player_uuid,player_name,ROW_NUMBER() OVER(PARTITION BY player_uuid ORDER BY updated_at DESC) AS n FROM reports
              ) WHERE n=1''')}
        players = [{'uuid': row['player_uuid'], 'name': names[row['player_uuid']], 'kills': row['kills'],
                    'ordinary': row['ordinary'], 'unique': row['unique_count'], 'points': row['ordinary'] + 3 * row['unique_count'],
                    'updatedAt': row['updated_at']} for row in rows]
        players.sort(key=lambda p: (-p['kills'], -p['points'], p['name'].lower(), p['uuid']))
        for rank, player in enumerate(players, 1): player['rank'] = rank
        return {'players': players, 'weights': {'ordinary': 1, 'unique': 3}}

class RateLimiter:
    def __init__(self): self.lock = threading.Lock(); self.requests = defaultdict(deque)
    def allow(self, address):
        now = time.monotonic()
        with self.lock:
            if len(self.requests) > 10000:
                self.requests = defaultdict(deque, {key: values for key, values in self.requests.items() if values and values[-1] > now - 60})
            queue = self.requests[address]
            while queue and queue[0] <= now - 60: queue.popleft()
            if len(queue) >= 120: return False
            queue.append(now); return True

class AppServer(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, address, store, public, origins):
        self.store = store; self.public = Path(public).resolve(); self.origins = set(origins); self.limiter = RateLimiter()
        super().__init__(address, Handler)

class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'
    server_version = 'SolSanctum'
    sys_version = ''
    def setup(self):
        super().setup(); self.connection.settimeout(15)
    def log_message(self, *_): pass
    def send_data(self, status, body, content_type='application/json; charset=utf-8', headers=None):
        self.send_response(status)
        self.send_header('Content-Type', content_type); self.send_header('Content-Length', str(len(body)))
        self.send_header('X-Content-Type-Options', 'nosniff'); self.send_header('Cache-Control', 'no-store')
        for key, value in (headers or {}).items(): self.send_header(key, value)
        self.end_headers()
        if self.command != 'HEAD': self.wfile.write(body)
    def json(self, status, data, headers=None): self.send_data(status, json.dumps(data, ensure_ascii=False, separators=(',', ':')).encode(), headers=headers)
    def do_HEAD(self): self.do_GET()
    def do_GET(self):
        path = unquote(urlsplit(self.path).path)
        if path == '/halloween/api/health': return self.json(200, {'ok': True, 'version': '1.1.3'})
        if path == '/halloween/api/leaderboard': return self.json(200, self.server.store.leaderboard())
        if path in ['/halloween', '/halloween/']: relative = 'index.html'
        elif path.startswith('/halloween/assets/') or path.startswith('/halloween/downloads/'):
            relative = path[len('/halloween/'):]
        else: return self.json(404, {'error': 'Not found'})
        candidate = (self.server.public / relative).resolve()
        if not candidate.is_relative_to(self.server.public) or not candidate.is_file(): return self.json(404, {'error': 'Not found'})
        kind = mimetypes.guess_type(candidate.name)[0] or 'application/octet-stream'
        self.send_response(200); self.send_header('Content-Type', kind); self.send_header('Content-Length', str(candidate.stat().st_size))
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Content-Security-Policy', "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")
        self.send_header('Cache-Control', 'no-cache' if relative == 'index.html' else 'public, max-age=3600')
        if relative.startswith('downloads/'):
            self.send_header('Content-Disposition', f'attachment; filename="{candidate.name}"')
        self.end_headers()
        if self.command != 'HEAD':
            with candidate.open('rb') as stream:
                while chunk := stream.read(131072): self.wfile.write(chunk)
    def do_POST(self):
        if urlsplit(self.path).path != '/halloween/api/ingest':
            self.close_connection = True; return self.json(404, {'error': 'Not found'})
        if self.headers.get('Origin') and self.headers['Origin'] not in self.server.origins:
            self.close_connection = True; return self.json(403, {'error': 'Origin not allowed'})
        if self.headers.get_content_type() != 'application/json' or self.headers.get('Transfer-Encoding'):
            self.close_connection = True; return self.json(415, {'error': 'Use application/json'})
        try:
            size = int(self.headers.get('Content-Length', '0'))
            if size <= 0 or size > 65536: raise Invalid('Request too large')
            address = self.headers.get('X-Real-IP', self.client_address[0])
            if not self.server.limiter.allow(address):
                self.close_connection = True; return self.json(429, {'error': 'Too many requests'}, {'Retry-After': '60'})
            raw = self.rfile.read(size)
            if len(raw) != size: raise Invalid('Incomplete request')
            payload = json.loads(raw)
            authorization = self.headers.get('Authorization', '')
            if not authorization.startswith('Bearer '): raise Unauthorized('Missing upload key')
            response = self.server.store.ingest(payload, authorization[7:])
            return self.json(200, response)
        except Unauthorized as error: return self.json(401, {'error': str(error)})
        except (Invalid, ValueError, UnicodeError, RecursionError) as error:
            self.close_connection = True; return self.json(400, {'error': str(error) if isinstance(error, Invalid) else 'Invalid JSON'})
        except sqlite3.Error: return self.json(503, {'error': 'Temporary storage error'}, {'Retry-After': '10'})

if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--host', default='127.0.0.1'); parser.add_argument('--port', type=int, default=18126)
    parser.add_argument('--db', default=os.environ.get('ELYSIUM_DB', 'data/halloween.sqlite3')); parser.add_argument('--public', default='dist')
    args = parser.parse_args(); server = AppServer((args.host, args.port), Store(args.db), args.public, [origin.strip() for origin in os.environ.get('ALLOWED_ORIGINS', 'http://127.0.0.1:5186').split(',') if origin.strip()])
    print(f'Sol Sanctum listening on {args.host}:{args.port}', flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally: server.server_close()
