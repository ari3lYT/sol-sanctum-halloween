import { columns, formatNumber } from '../ranking.js';
import { SortIcon } from './Icons.jsx';
import { Avatar } from './Avatar.jsx';
export function Leaderboard({ players, sort, onSort, loading, error, searching, onRetry }) {
  return <section aria-labelledby="ranking-title" className="ranking">
    <h2 id="ranking-title">Рейтинг</h2>
    {error ? <div className="error" role="alert">Не удалось обновить рейтинг. <button onClick={onRetry}>Повторить</button></div> : null}
    <div className="table-scroll" tabIndex={0} role="region" aria-label="Рейтинг игроков. На небольшом экране таблицу можно прокрутить вправо.">
      <table aria-busy={loading}>
        <thead><tr>{columns.map(column => <th key={column.key} className={column.numeric ? 'number' : ''} scope="col" aria-sort={sort.key === column.key ? (sort.direction === 'desc' ? 'descending' : 'ascending') : 'none'}>
          <button className={sort.key === column.key ? 'column-button selected' : 'column-button'} onClick={() => onSort(column.key)} aria-label={`Сортировать: ${column.title}`}>
            <span>{column.title}</span><SortIcon direction={sort.key === column.key ? sort.direction : null}/>
          </button>
        </th>)}</tr></thead>
        <tbody>{players.length ? players.map(player => <tr key={player.uuid}>
          <td className={`rank ${player.rank === 1 ? 'first' : ''}`}>{player.rank}</td>
          <td><div className="player"><Avatar uuid={player.uuid}/><span>{player.name}</span></div></td>
          <td className={`number ${sort.key === 'kills' ? 'active-value' : ''}`}>{formatNumber(player.kills)}</td>
          <td className="number ordinary">{formatNumber(player.ordinary)}</td>
          <td className="number unique">{formatNumber(player.unique)}</td>
          <td className={`number ${sort.key === 'points' ? 'active-value' : ''}`}>{formatNumber(player.points)}</td>
        </tr>) : <tr><td colSpan={6} className="empty-state">{loading ? 'Загрузка…' : searching ? 'Игрок не найден' : error ? 'Рейтинг недоступен' : 'Пока нет результатов'}</td></tr>}</tbody>
      </table>
    </div>
  </section>;
}
