import { useEffect, useRef } from 'react';
import { CloseIcon } from './Icons.jsx';
export function RulesDialog({ open, onClose, onDownload }) {
  const dialog = useRef(null);
  useEffect(() => { if (open && !dialog.current.open) dialog.current.showModal(); else if (!open && dialog.current.open) dialog.current.close(); }, [open]);
  return <dialog ref={dialog} onClose={onClose} onClick={event => { if (event.target === dialog.current) onClose(); }} aria-labelledby="rules-title">
    <div className="dialog-body"><button className="icon-button close-dialog" onClick={onClose} aria-label="Закрыть"><CloseIcon/></button>
      <h2 id="rules-title">Правила</h2>
      <ul><li>Моб с тыквой на голове — одна обычная конфета и 1 очко.</li><li>Моб со светильником Джека — одна уникальная конфета и 3 очка.</li><li>Мобы без тыквы увеличивают только счётчик убийств. Свинозомби не учитываются.</li></ul>
      <h3>Как установить</h3><ol className="install-steps">
        <li>Установи <a href="https://fabricmc.net/use/installer/" target="_blank" rel="noopener noreferrer">Fabric Loader</a> 0.20.1 или новее для своей версии Minecraft. В стороннем лаунчере выбери Fabric в настройках игрового профиля.</li>
        <li>Нажми «Скачать мод», выбери версию Minecraft и скачай мод и подходящий Fabric API.</li>
        <li>Положи оба файла <code>.jar</code> в папку <code>mods</code> этого игрового профиля. Если папки нет — создай её. При обновлении замени старый JAR мода новым.</li>
        <li>Запусти Fabric-профиль и зайди на сервер. Окно статистики — <kbd>K</kbd>, показать или скрыть оверлей — <kbd>H</kbd>.</li>
      </ol>
      <p>Результат появится в таблице после входа на сервер с модом. Клавиши и положение оверлея меняются в настройках мода.</p>
      <button className="primary-button" onClick={onDownload}>Скачать мод</button>
    </div>
  </dialog>;
}
