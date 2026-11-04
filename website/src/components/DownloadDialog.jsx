import { useEffect, useRef, useState } from 'react';
import { CloseIcon, DownloadIcon, CheckIcon } from './Icons.jsx';
import { MOD_VERSION, releases } from '../releases.js';

export function DownloadDialog({ open, onClose }) {
  const dialog = useRef(null);
  const [selected, setSelected] = useState(null);
  useEffect(() => {
    if (open && !dialog.current.open) dialog.current.showModal();
    else if (!open && dialog.current.open) dialog.current.close();
  }, [open]);
  return <dialog className="download-dialog" ref={dialog} onClose={onClose} onClick={event => { if (event.target === dialog.current) onClose(); }} aria-labelledby="download-title">
    <div className="download-body">
      <button className="icon-button close-dialog" onClick={onClose} aria-label="Закрыть"><CloseIcon/></button>
      <h2 id="download-title">Скачать мод</h2>
      <p className="release-caption">Sol Sanctum | Makoto <span>{MOD_VERSION}</span></p>
      <fieldset className="version-list"><legend>Версия Minecraft</legend>
        {releases.map(release => <label className={`version-option ${selected?.minecraft === release.minecraft ? 'is-selected' : ''}`} key={release.minecraft}>
          <input type="radio" name="minecraft-version" value={release.minecraft} checked={selected?.minecraft === release.minecraft} onChange={() => setSelected(release)}/>
          <span className="version-number">{release.minecraft}</span>
          <span className="loader-label">Fabric</span>
          <span className="file-meta">{release.size} <span>· JAR</span></span>
          <span className="version-check"><CheckIcon/></span>
        </label>)}
      </fieldset>
      <div className="dependency"><div><span className="dependency-label">Нужен Fabric API</span><span className="dependency-version">{selected?.api || 'Для выбранной версии Minecraft'}</span></div>
        {selected && <a className="secondary-button" href={selected.apiUrl} download aria-label={`Скачать Fabric API для ${selected.minecraft}`}><DownloadIcon/><span>Fabric API</span></a>}
      </div>
      <p className="install-note">Положи мод и Fabric API в папку <code>mods</code> своего Fabric-профиля. При обновлении замени старый JAR мода.</p>
      <div className="download-actions">{selected ? <a className="primary-button download-submit" href={selected.modUrl} download><DownloadIcon/>Скачать для {selected.minecraft}</a> : <button className="primary-button download-submit" disabled>Выбери версию Minecraft</button>}</div>
    </div>
  </dialog>;
}
