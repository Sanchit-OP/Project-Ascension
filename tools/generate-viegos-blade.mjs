// SUPERSEDED (2026-10-07): Veigo's Blade's 3D model is now drawn by tools/generate-combat-models.py.
// Kept for history: it made the first, 373-cuboid Blockbench model (design/weapons/viegos-blade/).
// Running it would overwrite the new veigos_blade_3d.json and .png, so it refuses to.
//
// Authoring helper for the initial Blockbench model. No build/runtime dependency.
// Run from the repository root: node tools/generate-viegos-blade.mjs
// This regenerates the .bbmodel and game assets; preserve manual Blockbench edits first.
throw new Error('superseded by tools/generate-combat-models.py; see the comment at the top of this file');
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const artDir = path.join(root, 'design/weapons/viegos-blade');
const assets = path.join(root, 'modules/combat/src/main/resources/assets/ascension_combat');
fs.mkdirSync(artDir, { recursive: true });

// A small hand-authored material atlas: pale edges, oxidized steel, recesses and leather.
const palette = ['#8fd8ba', '#59ad8f', '#327763', '#214d42',
  '#183b32', '#102923', '#091c19', '#baffdf',
  '#15df99', '#08785b', '#55ffc0', '#203932',
  '#132d29', '#2d4d42', '#8dffca', '#689988'];
const rgb = palette.map(c => [1, 3, 5].map(i => parseInt(c.slice(i, i + 2), 16)));
const size = 64;
const pixels = Buffer.alloc(size * (1 + size * 4));
for (let y = 0; y < size; y++) {
  for (let x = 0; x < size; x++) {
    const p = y * (size * 4 + 1) + 1 + x * 4;
    const tile = Math.floor(y / 16) * 4 + Math.floor(x / 16);
    const noise = ((x * 17 + y * 29 + x * y * 3) % 11) - 5;
    const scratch = (x % 16 === 5 && y % 7 < 3) ? 5 : 0;
    rgb[tile].forEach((c, i) => pixels[p + i] = Math.max(0, Math.min(255, c + noise + scratch)));
    pixels[p + 3] = 255;
  }
}
function crc32(b) {
  let c = 0xffffffff;
  for (const v of b) { c ^= v; for (let j = 0; j < 8; j++) c = (c >>> 1) ^ ((c & 1) ? 0xedb88320 : 0); }
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const t = Buffer.from(type), n = Buffer.alloc(4), crc = Buffer.alloc(4);
  n.writeUInt32BE(data.length); crc.writeUInt32BE(crc32(Buffer.concat([t, data])));
  return Buffer.concat([n, t, data, crc]);
}
const header = Buffer.alloc(13);
header.writeUInt32BE(size, 0); header.writeUInt32BE(size, 4); header[8] = 8; header[9] = 6;
const png = Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),
  chunk('IHDR', header), chunk('IDAT', zlib.deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
fs.writeFileSync(path.join(assets, 'textures/item/veigos_blade_3d.png'), png);

const elements = [], groups = new Map();
function uuid(s) {
  const h = crypto.createHash('sha256').update(s).digest('hex');
  return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-a${h.slice(17,20)}-${h.slice(20,32)}`;
}
function uv(tile) {
  const x = (tile % 4) * 16, y = Math.floor(tile / 4) * 16;
  return [x + 2, y + 2, x + 14, y + 14];
}
function box(group, name, from, to, material, side = material, rotation) {
  const id = uuid(name + elements.length);
  const faces = Object.fromEntries(['north','south','east','west','up','down'].map(face => [face,
    { uv: uv(face === 'north' || face === 'south' ? material : side), texture: 0 }]));
  const e = { name, type: 'cube', uuid: id, from, to, autouv: 0, color: 4,
    origin: [8, 8, 8], rotation: [0,0,0], box_uv: false, faces };
  if (rotation) { e.origin = rotation.origin; e.rotation[2] = rotation.angle; }
  elements.push(e);
  if (!groups.has(group)) groups.set(group, []);
  groups.get(group).push(id);
}
function inside(x, y, poly) {
  let hit = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const [a,b] = poly[i], [c,d] = poly[j];
    if ((b > y) !== (d > y) && x < (c-a)*(y-b)/(d-b)+a) hit = !hit;
  }
  return hit;
}
// Merge equal adjacent cells into cuboids. Beveled thickness and metal borders are real
// geometry, with discrete steps suitable for the vanilla Java model format.
function profile(group, poly, { step = 0.125, depth = 0.34, edge = 0, fill = 2, blade = false } = {}) {
  const x0 = Math.floor(Math.min(...poly.map(p=>p[0])) / step);
  const y0 = Math.floor(Math.min(...poly.map(p=>p[1])) / step);
  const w = Math.ceil(Math.max(...poly.map(p=>p[0])) / step) - x0;
  const h = Math.ceil(Math.max(...poly.map(p=>p[1])) / step) - y0;
  const mask = Array.from({length:h}, (_,y)=>Array.from({length:w}, (_,x)=>
    inside((x+x0+.5)*step, (y+y0+.5)*step, poly)));
  const has = (x,y)=>mask[y]?.[x] ?? false;
  const cells = mask.map((row,y)=>row.map((v,x)=> {
    if(!v) return null;
    const border = !has(x-1,y) || !has(x+1,y) || !has(x,y-1) || !has(x,y+1);
    const xx = Math.abs((x+x0+.5)*step);
    let mat = border ? edge : fill, d = border ? depth*.58 : depth;
    if(blade && !border) {
      mat = xx < .13 ? 9 : xx < .26 ? 10 : 8;
      d = xx < .26 ? depth*1.2 : depth;
    }
    return `${mat}:${d.toFixed(4)}`;
  }));
  for(let y=0;y<h;y++) for(let x=0;x<w;x++) {
    const key=cells[y][x]; if(key===null) continue;
    let ww=1, hh=1;
    while(x+ww<w && cells[y][x+ww]===key) ww++;
    outer: while(y+hh<h) {
      for(let dx=0;dx<ww;dx++) if(cells[y+hh][x+dx]!==key) break outer;
      hh++;
    }
    for(let dy=0;dy<hh;dy++) for(let dx=0;dx<ww;dx++) cells[y+dy][x+dx]=null;
    const [mat,d]=key.split(':').map(Number);
    box(group, `${group} ${groups.get(group)?.length ?? 0}`,
      [8+(x+x0)*step*(blade ? 1.6 : 1),(y+y0)*step,8-d],
      [8+(x+x0+ww)*step*(blade ? 1.6 : 1),(y+y0+hh)*step,8+d],mat,blade ? 9 : 3);
  }
}
const mirror = p=>p.map(([x,y])=>[-x,y]);

// Overall length 32 units; two-thirds is the narrow, straight blade.
profile('Blade', [[0,32],[-.375,30.5],[-.625,27],[-.625,15.2],[-.875,14.5],
  [-.875,13.7],[-.625,13.4],[-.875,12.8],[0,12.45],[.875,12.8],
  [.625,13.4],[.875,13.7],[.875,14.5],[.625,15.2],[.625,27],[.375,30.5]],
  {depth:.24, edge:7, blade:true});

const guard = [[.4,6.95],[1.45,7.15],[2.45,7.55],[2.95,8.15],[3.2,8.6],
  [3.8,8.8],[4.4,8.6],[4.85,8.1],[4.25,8.2],[3.85,7.9],[3.65,7.35],
  [3.7,6.8],[3.95,6.25],[4.35,6.05],[4.85,6.15],[4.5,5.6],[3.9,5.3],
  [3.25,5.4],[2.9,5.85],[2.65,6.3],[1.5,6.5],[.4,6.45]];
profile('Great guard right', guard, {depth:.34, fill:2});
profile('Great guard left', mirror(guard), {depth:.34, fill:2});
profile('Guard heart', [[0,8],[-.5,7.45],[-1.2,6.8],[-.5,6.15],[0,5.65],
  [.5,6.15],[1.2,6.8],[.5,7.45]], {depth:.48,fill:3});

// Open crescent pommel and its reinforced stem.
profile('Pommel', [[-1.2,3.1],[-1.65,2.6],[-1.8,1.9],[-1.6,.95],[-1,.4],
  [0,.2],[1,.4],[1.6,.95],[1.8,1.9],[1.65,2.6],[1.2,3.1],[1.2,2.4],
  [.85,1.8],[.45,1.65],[.4,2.3],[-.4,2.3],[-.45,1.65],[-.85,1.8],[-1.2,2.4]],
  {depth:.36,fill:2});
box('Lower hilt','Hilt spine',[7.7,2.1,7.73],[8.3,6.25,8.27],2,3);
box('Lower hilt','Hilt inset',[7.9,2.5,7.70],[8.1,5.7,8.30],4,4);
for(const sign of [-1,1]) profile('Hilt thorns',
  [[sign*.25,4.55],[sign*.7,4.1],[sign*.7,4.65],[sign*.35,5.1]], {depth:.25,fill:2});

// The long black leather ricasso between the two guards is a defining feature.
box('Wrapped grip','Leather core',[7.68,7.7,7.7],[8.32,11.9,8.3],6,5);
for(let i=0;i<8;i++) {
  const y=7.85+i*.5;
  box('Wrapped grip',`Spiral binding ${i}`,[7.61,y,7.64],[8.39,y+.16,8.36],13,5,
    {origin:[8,y+.08,8],angle:22.5});
}
box('Wrapped grip','Upper ferrule',[7.57,11.72,7.6],[8.43,12.1,8.4],1,3);
box('Wrapped grip','Lower ferrule',[7.57,7.6,7.6],[8.43,7.95,8.4],1,3);

const small = [[.3,12.4],[.9,12.6],[1.15,13.15],[1.6,13.5],[1.95,13.45],
  [1.55,13.05],[1.45,12.6],[1.5,12.15],[1.95,11.75],[1.55,11.85],
  [1.15,12.1],[.3,12.05]];
profile('Blade guard',small,{depth:.25,fill:3});
profile('Blade guard',mirror(small),{depth:.25,fill:3});
profile('Blade collar',[[0,13.1],[-.55,12.55],[0,12],[.55,12.55]],{depth:.33,fill:2});

// Raised diamond sockets on both faces give the central guard a readable focal point.
for (const s of [-1,1]) {
  const z=8+s*.5;
  box('Guard jewels',`Socket ${s}`,[7.63,6.43,z-.055],[8.37,7.17,z+.055],1,3,
    {origin:[8,6.8,z],angle:45});
  box('Guard jewels',`Black diamond ${s}`,[7.77,6.57,z+s*.07-.035],
    [8.23,7.03,z+s*.07+.035],6,5,{origin:[8,6.8,z],angle:45});
  box('Guard jewels',`Soul glint ${s}`,[7.925,6.8,z+s*.12-.02],
    [8.075,6.95,z+s*.12+.02],14,10,{origin:[8,6.875,z],angle:45});
}

const display = {
  thirdperson_righthand:{rotation:[0,-90,0],translation:[0,0,1.5],scale:[1.3,1.3,1.3]},
  thirdperson_lefthand:{rotation:[0,90,0],translation:[0,0,1.5],scale:[1.3,1.3,1.3]},
  firstperson_righthand:{rotation:[0,-25,35],translation:[1,-3,-3],scale:[1,1,1]},
  firstperson_lefthand:{rotation:[0,-25,35],translation:[1,-3,-3],scale:[1,1,1]},
  gui:{rotation:[0,0,-40],translation:[-3.0,-3.6,0],scale:[.55,.55,.55]},
  ground:{rotation:[0,0,0],translation:[0,4,0],scale:[.4,.4,.4]},
  fixed:{rotation:[0,0,-40],translation:[-3.6,-4.2,0],scale:[.65,.65,.65]},
  head:{rotation:[0,0,0],translation:[0,0,0],scale:[1,1,1]}
};
const textureId=uuid('veigos_blade_3d');
const bbmodel = {
  meta:{format_version:'4.10',model_format:'java_block',box_uv:false},
  name:'Veigos Blade', model_identifier:'veigos_blade', visible_box:[1,1,0],
  resolution:{width:size,height:size}, elements,
  outliner:[...groups].map(([name,children])=>({name,uuid:uuid(name),origin:[8,8,8],
    rotation:[0,0,0],color:4,export:true,isOpen:false,visibility:true,autouv:0,children})),
  textures:[{name:'veigos_blade_3d.png',id:'0',uuid:textureId,mode:'bitmap',saved:true,
    namespace:'ascension_combat',folder:'item',particle:true,render_mode:'default',
    width:size,height:size,uv_width:size,uv_height:size,
    source:'data:image/png;base64,'+png.toString('base64')}],
  display, front_gui_light:true, ambientocclusion:false
};
const game = {
  credit:'Made for Project Ascension; editable Blockbench source in design/weapons/viegos-blade',
  texture_size:[size,size], ambientocclusion:false, gui_light:'front',
  textures:{0:'ascension_combat:item/veigos_blade_3d',particle:'ascension_combat:item/veigos_blade_3d'},
  elements:elements.map(e=>{
    const result={name:e.name,from:e.from,to:e.to,faces:Object.fromEntries(
      Object.entries(e.faces).map(([f,v])=>[f,{uv:v.uv.map(n=>n*16/size),texture:'#0'}]))};
    if(e.name.startsWith('Blade ') && /^Blade \d+$/.test(e.name)) {
      // NeoForge 1.21.1 baked-model emissivity, no shader or custom renderer required.
      result.neoforge_data={block_light:15,sky_light:15,ambient_occlusion:false};
      result.shade=false;
    }
    if(e.name.startsWith('Soul glint')) {
      result.neoforge_data={block_light:15,sky_light:15,ambient_occlusion:false};
      result.shade=false;
    }
    if(e.rotation[2]) result.rotation={angle:e.rotation[2],axis:'z',origin:e.origin};
    return result;
  }), display,
  groups:[...groups].map(([name,children])=>({name,origin:[8,8,8],color:4,
    children:children.map(id=>elements.findIndex(e=>e.uuid===id))}))
};
for(const e of game.elements) {
  if(e.from.some((n,i)=>n>=e.to[i] || n< -16 || e.to[i]>32)) throw new Error('Invalid cuboid '+e.name);
}
fs.writeFileSync(path.join(artDir,'veigos_blade.bbmodel'),JSON.stringify(bbmodel,null,2)+'\n');
fs.writeFileSync(path.join(assets,'models/item/veigos_blade_3d.json'),JSON.stringify(game,null,2)+'\n');
console.log(`Created ${elements.length} cuboids in ${groups.size} groups; 64x64 material atlas.`);
