'use strict';
/* Original, fictional cartoon archetypes. Not based on any real person. */
const ROSTER = [
  { id: 'kursi', name: 'KURSI KUMAR', title: 'Balanced Brawler', team: 'Team Sinhasan',
    spd: 1.0, pwr: 1.0, hp: 100, def: 1.0,
    skin: 0xd9a066, top: 0xf28c28, bottom: 0xf5f0e6, shoe: 0x3b2a1a, accent: 0xffd166,
    hair: 0x222222, hairStyle: 'none', hat: 'topi', hatColor: 0xf6efd8, stache: 1,
    sup: { name: 'KURSI THROW', desc: 'Hurls a giant golden chair across the arena.', color: 0xffc83d, shape: 'chair', dmg: 26 },
    quote: 'Kursi meri, fight meri!' },
  { id: 'jumla', name: 'JUMLA JEE', title: 'Speed Striker', team: 'Team Vaada',
    spd: 1.25, pwr: 0.9, hp: 92, def: 1.1,
    skin: 0xc68642, top: 0x1fa3a3, bottom: 0xeeeeee, shoe: 0x1b1b1b, accent: 0xffffff,
    hair: 0x111111, hairStyle: 'slick', hat: 'none', glasses: true,
    sup: { name: 'JUMLA JAAL', desc: 'Fires a flurry of rolled-up promises. Most of them explode.', color: 0xffffff, shape: 'scroll', dmg: 24 },
    quote: 'Achhe din aa gaye... bas aa hi rahe hain!' },
  { id: 'chai', name: 'CHAI CHACHA', title: 'Heavy Hitter', team: 'Team Tapri',
    spd: 0.82, pwr: 1.25, hp: 120, def: 0.9,
    skin: 0xb87a4b, top: 0x8b5a2b, bottom: 0x4a3322, shoe: 0x2a1a10, accent: 0xffffff,
    hair: 0x2b1b10, hairStyle: 'none', hat: 'none', stache: 2,
    sup: { name: 'GARAM CHAI', desc: 'Flings a scalding kulhad of extra-strong chai.', color: 0xc97b3a, shape: 'cup', dmg: 28 },
    quote: 'Pehle chai, phir ladai.' },
  { id: 'rally', name: 'RALLY RAJA', title: 'Power Orator', team: 'Team Jalsa',
    spd: 0.98, pwr: 1.15, hp: 102, def: 1.0,
    skin: 0xd9a066, top: 0xd62839, bottom: 0xf2e8cf, shoe: 0x3a1a1a, accent: 0xffd166,
    hair: 0x1a1a1a, hairStyle: 'none', hat: 'turban', hatColor: 0xe8b923, stache: 2, beard: true,
    sup: { name: 'LOUDSPEAKER BLAST', desc: 'Screams through a megaphone. Eardrums were harmed.', color: 0xff6b35, shape: 'mega', dmg: 27 },
    quote: 'Bhaiyon aur behno... zor se bolo!' },
  { id: 'toll', name: 'TOLL TAU', title: 'Iron Tank', team: 'Team Highway',
    spd: 0.75, pwr: 1.1, hp: 132, def: 0.78,
    skin: 0xe0b48a, top: 0x4a5568, bottom: 0x2d3748, shoe: 0x111111, accent: 0xfbbf24,
    hair: 0x9a9a9a, hairStyle: 'short', hat: 'none', glasses: true, stache: 1,
    sup: { name: 'TOLL BARRIER', desc: 'Drops a boom barrier. Pay up or get flattened.', color: 0xfbbf24, shape: 'barrier', dmg: 27 },
    quote: 'Rukiye. Toll bharna padega.' },
  { id: 'netri', name: 'NETRI NIMMI', title: 'Rushdown Queen', team: 'Team Dharna',
    spd: 1.12, pwr: 1.0, hp: 100, def: 1.0,
    skin: 0xe0ac83, top: 0x2563eb, bottom: 0x1e3a8a, shoe: 0xffffff, accent: 0xf472b6,
    hair: 0x1a1a1a, hairStyle: 'bun', hat: 'none',
    sup: { name: 'DHARNA SHOCK', desc: 'A sit-in so intense it shakes the arena.', color: 0x60a5fa, shape: 'orb', dmg: 25 },
    quote: 'Hum yahin baithenge!' },
  { id: 'uncle', name: 'WHATSAPP UNCLE', title: 'Forward Trickster', team: 'Team Forward',
    spd: 1.0, pwr: 1.05, hp: 105, def: 1.0,
    skin: 0xcb9462, top: 0x16a34a, bottom: 0xe5e7eb, shoe: 0x333333, accent: 0xfacc15,
    hair: 0x3b3b3b, hairStyle: 'none', hat: 'cap', hatColor: 0x16a34a, glasses: true, stache: 1,
    sup: { name: 'GOOD MORNING SPAM', desc: 'Sends 400 flower pics. Direct hit.', color: 0x4ade80, shape: 'phone', dmg: 26 },
    quote: 'Share karo, warna bura hoga!' },
  { id: 'meme', name: 'MEME MANTRI', title: 'Agile Troll', team: 'Team Viral',
    spd: 1.18, pwr: 1.0, hp: 96, def: 1.0,
    skin: 0xd2a679, top: 0x7c3aed, bottom: 0x1f2937, shoe: 0xfacc15, accent: 0xf43f5e,
    hair: 0x0f0f0f, hairStyle: 'spiky', hat: 'none', glasses: true,
    sup: { name: 'VIRAL BLAST', desc: 'A cursed meme so spicy it hurts.', color: 0xa78bfa, shape: 'orb', dmg: 25 },
    quote: 'Ratio + L + fight kar.' },
  { id: 'laddoo', name: 'LADDOO LALA', title: 'Grapple Master', team: 'Team Mithai',
    spd: 0.9, pwr: 1.2, hp: 112, def: 0.92,
    skin: 0xd8a070, top: 0xfbbf24, bottom: 0xffffff, shoe: 0x7c2d12, accent: 0xdc2626,
    hair: 0x2a1a0a, hairStyle: 'none', hat: 'none', stache: 2,
    sup: { name: 'LADDOO BARRAGE', desc: 'Rapid-fire motichoor laddoos. Sweet pain.', color: 0xf59e0b, shape: 'laddoo', dmg: 27 },
    quote: 'Mithai khao, mooh band rakho!' },
  { id: 'reel', name: 'REEL RANI', title: 'Influencer Menace', team: 'Team Trending',
    spd: 1.08, pwr: 0.95, hp: 98, def: 1.0,
    skin: 0xe8b88a, top: 0xec4899, bottom: 0x831843, shoe: 0xffffff, accent: 0x22d3ee,
    hair: 0x5b2a86, hairStyle: 'afro', hat: 'none', glasses: true,
    sup: { name: 'SELFIE FLASH', desc: 'Blinds the opponent with 12 filters at once.', color: 0xf9a8d4, shape: 'phone', dmg: 25 },
    quote: 'Like, share, subscribe... and lose.' },
  { id: 'boss', name: 'MEGA MANTRI', title: 'BOSS · Final Form', team: 'Team Kursi Forever',
    spd: 0.95, pwr: 1.3, hp: 150, def: 0.75, scale: 1.18, boss: true,
    skin: 0xc9905c, top: 0x111827, bottom: 0x111827, shoe: 0xfbbf24, accent: 0xfbbf24,
    hair: 0xe5e7eb, hairStyle: 'none', hat: 'crown', hatColor: 0xfbbf24, stache: 2, beard: true,
    sup: { name: 'KURSI KA KHEL', desc: 'Rains golden chairs from the heavens.', color: 0xfbbf24, shape: 'chair', dmg: 32 },
    quote: 'Yeh kursi kabhi nahi chhodunga!' },
];

const STAGE_NAMES = ['SABHA HALL', 'RALLY MAIDAN', 'DESI SADAK'];
const LEVEL_NAMES = ['EASY', 'NORMAL', 'HARD'];
const HIT_WORDS = ['THOK!', 'DHISHUM!', 'OYE!', 'ARRE!', 'BAM!', 'WAH!', 'THAAP!'];
const HEADLINES = [
  'Sources say the chai vendor demanded compensation for broken cups.',
  'A stray cow paused the fight, then ate a placard.',
  'Exit polls, as usual, were completely wrong.',
  'Parliament adjourned after a heated debate over samosa size.',
  'Referee fined for blowing the whistle without a licence.',
  'Opposition walks out; ruling side walks out to follow them.',
  'Fact-check pending: forwarded as received.',
  'Committee formed to study why the committee was formed.',
  'Auto-rickshaw driver declares neutrality; meter still running.'
];

/* ================= 500 generated levels ================= */
const LEVEL_COUNT = 500;
const TIERS = ['NEWBIE', 'LOCAL', 'MOHALLA', 'TOWN', 'DISTRICT', 'STATE', 'ZONAL', 'REGIONAL', 'NATIONAL', 'LEGENDARY'];
const MOD_NAMES = {
  lowgrav: 'LOW GRAVITY', bighead: 'BIG HEADS', turbo: 'TURBO SPEED', glass: 'GLASS CANNONS', fullsuper: 'FULL SUPER START',
  rage: 'RAGE ENEMY', iron: 'IRON ENEMY', sprint: 'SPRINT (30s)', giant: 'GIANT ENEMY', mini: 'MINI FIGHTERS', slowmo: 'SLOW-MO'
};
function mulberry(a) {
  return function () { a |= 0; a = (a + 0x6D2B79F5) | 0; let t = Math.imul(a ^ (a >>> 15), 1 | a); t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
}
function shiftHue(hex, dh, ds, dl) {
  const c = new THREE.Color(hex), h = {}; c.getHSL(h);
  c.setHSL(((h.h + dh) % 1 + 1) % 1, Math.min(1, Math.max(0, h.s + ds)), Math.min(0.85, Math.max(0.1, h.l + dl)));
  return c.getHex();
}
const SKINS = [0xd9a066, 0xc68642, 0xb87a4b, 0xe0ac83, 0xcb9462, 0xe8b88a, 0x9c6b43];

/* Fully deterministic: level n always gives the same enemy, arena, difficulty and twists. */
function levelInfo(n) {
  const R = mulberry(n * 7919 + 13), boss = n % 10 === 0, tier = Math.min(9, Math.floor((n - 1) / 50));
  const base = boss ? ROSTER[10] : ROSTER[(n * 3 + Math.floor(R() * 3)) % 10];
  const d = Object.assign({}, base);
  d.top = shiftHue(base.top, R(), (R() - 0.5) * 0.3, (R() - 0.5) * 0.2);
  d.bottom = shiftHue(base.bottom, R(), (R() - 0.5) * 0.3, (R() - 0.5) * 0.2);
  d.accent = shiftHue(base.accent, R(), 0, 0);
  d.shoe = shiftHue(base.shoe, R(), 0, 0);
  if (!boss) {
    d.skin = SKINS[Math.floor(R() * SKINS.length)];
    d.glasses = R() < 0.4; d.stache = Math.floor(R() * 3);
    if (!base.hat || base.hat === 'none') { const hs = ['none', 'short', 'spiky', 'bun', 'afro', 'slick']; d.hairStyle = hs[Math.floor(R() * hs.length)]; d.hair = shiftHue(base.hair, R(), 0.1, 0.1); }
    if (base.hatColor) d.hatColor = shiftHue(base.hatColor, R(), 0, 0);
  }
  // modifiers
  const pool = Object.keys(MOD_NAMES), mods = [];
  let count = n < 5 ? 0 : n < 100 ? (R() < 0.6 ? 1 : 0) : n < 300 ? 1 + (R() < 0.5 ? 1 : 0) : 1 + Math.floor(R() * 3);
  if (boss && count < 1) count = 1;
  while (mods.length < count) { const m = pool[Math.floor(R() * pool.length)]; if (mods.indexOf(m) < 0) mods.push(m); }
  const M = { grav: 0.0105, speed: 1, dmg: 1, enemyDmg: 1, time: 60, m1: 0, m2: 0, bighead: false, mini: false };
  // enemy scaling by level
  let hp = base.hp * (0.75 + n * 0.0045) * (boss ? 1.2 : 1);
  d.pwr = base.pwr * (0.85 + n * 0.0014); d.spd = base.spd * (0.92 + n * 0.0005); d.def = base.def * (1 - n * 0.0005);
  d.scale = base.scale || 1;
  if (boss) d.scale = Math.min(1.5, 1.18 + n / 1500);
  mods.forEach((m) => {
    if (m === 'lowgrav') M.grav = 0.0055; else if (m === 'bighead') M.bighead = true; else if (m === 'turbo') M.speed = 1.3;
    else if (m === 'glass') M.dmg = 1.7; else if (m === 'fullsuper') { M.m1 = 100; M.m2 = 100; } else if (m === 'rage') M.enemyDmg = 1.4;
    else if (m === 'iron') hp *= 1.5; else if (m === 'sprint') M.time = 30; else if (m === 'giant') { d.scale *= 1.3; hp *= 1.2; }
    else if (m === 'mini') { M.mini = true; d.scale *= 0.75; } else if (m === 'slowmo') M.speed = 0.75;
  });
  d.hp = Math.round(hp);
  d.title = TIERS[tier] + ' · LV ' + n;
  return { n: n, boss: boss, def: d, stage: Math.floor(R() * 3), ai: 3 * Math.pow((n - 1) / (LEVEL_COUNT - 1), 0.8), mods: M, modNames: mods.map((m) => MOD_NAMES[m]), tier: TIERS[tier] };
}
