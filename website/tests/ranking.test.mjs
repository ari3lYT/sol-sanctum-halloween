import { test } from 'node:test';
import assert from 'node:assert/strict';
import { selectPlayers, nextSort, columns } from '../src/ranking.js';
const players=[{uuid:'a',name:'Alice',kills:4,ordinary:1,unique:1,points:4,rank:2},{uuid:'b',name:'Bob',kills:8,ordinary:2,unique:0,points:2,rank:1},{uuid:'c',name:'Carol',kills:3,ordinary:0,unique:3,points:9,rank:3}];
test('initial kills descending',()=>assert.deepEqual(selectPlayers(players,'',{key:'kills',direction:'desc'}).map(p=>p.name),['Bob','Alice','Carol']));
for(const column of columns)test(`sort ${column.key} in both directions`,()=>{
 const a=selectPlayers(players,'',{key:column.key,direction:'asc'}),b=selectPlayers(players,'',{key:column.key,direction:'desc'});
 assert.equal(a.length,3);assert.equal(b.length,3);assert.notDeepEqual(a.map(p=>p.uuid),b.map(p=>p.uuid));assert.deepEqual(players.map(p=>p.name),['Alice','Bob','Carol']);
});
test('search is case insensitive and preserves original rank',()=>{const r=selectPlayers(players,'  ALI ',{key:'kills',direction:'desc'});assert.equal(r[0].name,'Alice');assert.equal(r[0].rank,2);});
test('repeated header click reverses direction',()=>{assert.deepEqual(nextSort({key:'kills',direction:'desc'},'kills'),{key:'kills',direction:'asc'});assert.deepEqual(nextSort({key:'kills',direction:'desc'},'name'),{key:'name',direction:'asc'});});
