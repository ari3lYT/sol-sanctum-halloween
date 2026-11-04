const BASE = import.meta.env.BASE_URL;
export const MOD_VERSION = '1.1.4';
export const releases = [
  { minecraft: '26.3', api: '0.165.0+26.3', size: '53,5 КБ' },
  { minecraft: '26.2', api: '0.161.0+26.2', size: '53,5 КБ' },
  { minecraft: '1.21.11', api: '0.141.6+1.21.11', size: '53,2 КБ' },
].map(release => ({
  ...release,
  modUrl: `${BASE}downloads/sol-sanctum-halloween-${release.minecraft}-${MOD_VERSION}.jar`,
  apiUrl: `https://maven.fabricmc.net/net/fabricmc/fabric-api/${release.api}/fabric-api-${release.api}.jar`,
}));
