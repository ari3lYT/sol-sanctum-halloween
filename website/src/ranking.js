const collator = new Intl.Collator('ru', { numeric: true, sensitivity: 'base' });
export const columns = [
  { key: 'rank', title: '№', numeric: true },
  { key: 'name', title: 'Игрок' },
  { key: 'kills', title: 'Убийства', numeric: true },
  { key: 'ordinary', title: 'Обычные', numeric: true },
  { key: 'unique', title: 'Уникальные', numeric: true },
  { key: 'points', title: 'Очки', numeric: true },
];
export function nextSort(current, key) {
  return { key, direction: current.key === key ? (current.direction === 'desc' ? 'asc' : 'desc') : (key === 'name' || key === 'rank' ? 'asc' : 'desc') };
}
export function selectPlayers(players, search, sort) {
  const term = search.trim().toLowerCase();
  const result = term ? players.filter(p => p.name.toLowerCase().includes(term)) : [...players];
  const sign = sort.direction === 'asc' ? 1 : -1;
  return result.sort((a, b) => {
    const difference = sort.key === 'name' ? collator.compare(a.name, b.name) : a[sort.key] - b[sort.key];
    return difference * sign || b.kills - a.kills || b.points - a.points || collator.compare(a.name, b.name) || a.uuid.localeCompare(b.uuid);
  });
}
export const formatNumber = new Intl.NumberFormat('ru-RU').format;
