import { useCallback, useEffect, useRef, useState } from 'react';
import { Leaderboard } from './components/Leaderboard.jsx';
import { SearchIcon, RefreshIcon } from './components/Icons.jsx';
import { RulesDialog } from './components/RulesDialog.jsx';
import { DownloadDialog } from './components/DownloadDialog.jsx';
import { nextSort, selectPlayers } from './ranking.js';
const BASE = import.meta.env.BASE_URL;
export default function App() {
  const [players, setPlayers] = useState([]), [search, setSearch] = useState('');
  const [sort, setSort] = useState({ key: 'kills', direction: 'desc' });
  const [loading, setLoading] = useState(true), [error, setError] = useState(false), [rules, setRules] = useState(false);
  const [downloads, setDownloads] = useState(false);
  const showDownloads = () => { setRules(false); setDownloads(true); };
  const request = useRef(null);
  const refresh = useCallback(async () => {
    request.current?.abort();
    const controller = new AbortController(); request.current = controller;
    setLoading(true);
    try {
      const response = await fetch(`${BASE}api/leaderboard`, { signal: controller.signal, credentials: 'omit', cache: 'no-store' });
      if (!response.ok) throw new Error('Leaderboard unavailable');
      const data = await response.json();
      if (!Array.isArray(data.players)) throw new Error('Invalid leaderboard');
      setPlayers(data.players); setError(false);
    } catch (problem) { if (problem.name !== 'AbortError') setError(true); }
    finally { if (!controller.signal.aborted) setLoading(false); }
  }, []);
  useEffect(() => {
    refresh(); const timer = setInterval(() => { if (document.visibilityState === 'visible') refresh(); }, 15000);
    return () => { clearInterval(timer); request.current?.abort(); };
  }, [refresh]);
  const selected = selectPlayers(players, search, sort);
  return <>
    <header className="site-header"><div className="header-inner"><a className="wordmark" href={BASE} aria-label="Sol Sanctum | Makoto">Sol Sanctum <span className="wordmark-divider">|</span> Makoto</a><nav aria-label="Главное меню"><button className="text-button rules-link" onClick={() => setRules(true)}>Правила</button><button className="primary-button" onClick={showDownloads}>Скачать мод</button></nav></div></header>
    <main>
      <section className="hero" aria-labelledby="page-title"><img src={`${BASE}assets/hero.png`} className="hero-art" alt="" width="2048" height="683" fetchPriority="high"/><div className="hero-inner"><h1 id="page-title">Хэллоуин<br/>в Sol Sanctum</h1><p>Обычная конфета — 1 очко. Уникальная — 3.</p></div></section>
      <div className="main-content"><div className="toolbar"><label className="search"><SearchIcon/><input type="search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Найти игрока" aria-label="Найти игрока" autoComplete="off" spellCheck="false"/></label><button className={`refresh-button ${loading ? 'loading' : ''}`} onClick={refresh} disabled={loading} aria-label="Обновить"><RefreshIcon/><span>Обновить</span></button></div>
        <Leaderboard players={selected} sort={sort} onSort={key => setSort(previous => nextSort(previous, key))} loading={loading} error={error} searching={Boolean(search.trim())} onRetry={refresh}/>
      </div>
    </main>
    <footer><div className="footer-inner"><span>Sol Sanctum | Makoto</span><button className="text-button" onClick={() => setRules(true)}>Как установить</button></div></footer>
    <RulesDialog open={rules} onClose={() => setRules(false)} onDownload={showDownloads}/>
    <DownloadDialog open={downloads} onClose={() => setDownloads(false)}/>
  </>;
}
