export function SearchIcon() { return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg>; }
export function RefreshIcon() { return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M20 9a8 8 0 1 0-1 8M20 3v6h-6"/></svg>; }
export function SortIcon({ direction }) { return <svg className="sort-icon" viewBox="0 0 12 18" aria-hidden="true"><path d="m6 2 5 6H1z" opacity={!direction || direction === 'asc' ? 1 : .24}/><path d="m6 16 5-6H1z" opacity={!direction || direction === 'desc' ? 1 : .24}/></svg>; }
export function CloseIcon() { return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18"/></svg>; }
export function DownloadIcon() { return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M12 3v12m-5-5 5 5 5-5M4 16v4h16v-4"/></svg>; }
export function CheckIcon() { return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="m5 12 4 4L19 6"/></svg>; }
