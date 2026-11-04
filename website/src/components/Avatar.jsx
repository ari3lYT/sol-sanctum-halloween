export function Avatar({ uuid }) {
  const seed = [...uuid].reduce((a, c) => ((a << 5) - a + c.charCodeAt(0)) | 0, 0) >>> 0;
  const colors = ['#634535', '#342b43', '#b66837', '#79727d', '#334e45', '#6c3d54'];
  const hair = colors[seed % colors.length];
  const skin = ['#d1a183', '#b77d61', '#ebbea2'][seed % 3];
  const eyes = ['#88bcd2', '#9c9fd3', '#83b68d'][seed % 3];
  return <svg className="avatar" viewBox="0 0 8 8" shapeRendering="crispEdges" aria-hidden="true"><rect width="8" height="8" fill={hair}/><path d="M1 3h6v4H1z" fill={skin}/><path d="M0 0h8v3H5V2H2v2H0z" fill={hair}/><path d="M1 4h2v1H1zm4 0h2v1H5z" fill="#f7eee4"/><path d="M2 4h1v1H2zm3 0h1v1H5z" fill={eyes}/><path d="M3 6h2v1H3z" fill="#9d6b58"/><path d="M0 7h8v1H0z" fill={hair}/></svg>;
}
