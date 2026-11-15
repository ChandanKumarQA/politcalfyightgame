'use strict';
/* Game engine: scene, combat rules (60 Hz fixed step), CPU AI, camera, effects. */
const W = { MINX: -6.4, MAXX: 6.4, GRAV: 0.0105, JUMPV: 0.19, WALK: 0.052 };

const MV = {
  P1: { id: 'P1', total: 16, from: 4, to: 8, dmg: 5, reach: 1.8, lvl: 'H', stun: 13, kb: 0.045 },
  P2: { id: 'P2', total: 16, from: 4, to: 8, dmg: 5, reach: 1.8, lvl: 'H', stun: 13, kb: 0.045 },
  P3: { id: 'P3', total: 24, from: 7, to: 11, dmg: 9, reach: 1.95, lvl: 'H', stun: 22, kb: 0.16 },
  KICK: { id: 'KICK', total: 24, from: 8, to: 13, dmg: 8, reach: 2.05, lvl: 'M', stun: 18, kb: 0.1 },
  KICK2: { id: 'KICK2', total: 26, from: 8, to: 13, dmg: 9, reach: 2.15, lvl: 'M', stun: 22, kb: 0.15 },
  SWEEP: { id: 'SWEEP', total: 30, from: 9, to: 15, dmg: 7, reach: 2.2, lvl: 'L', stun: 30, kb: 0.09 },
  UPPER: { id: 'UPPER', total: 24, from: 6, to: 12, dmg: 7, reach: 1.6, lvl: 'M', stun: 26, kb: 0.05, launch: 0.13 },
  AIRKICK: { id: 'AIRKICK', total: 999, from: 0, to: 999, dmg: 7, reach: 1.9, lvl: 'M', stun: 18, kb: 0.1 },
  GRAB: { id: 'GRAB', total: 34, from: 7, to: 9, dmg: 12, reach: 1.5, lvl: 'M', stun: 36, kb: 0.2, grab: true },
  SUPER: { id: 'SUPER', total: 54, from: 20, to: 21, dmg: 0, reach: 0, lvl: 'M', stun: 0, kb: 0, sup: true }
};
const PUNCH_CHAIN = [MV.P1, MV.P2, MV.P3];

class Fighter {
  constructor(def, isP1) {
    this.def = def; this.isP1 = isP1;
    this.rig = buildFighter(def);
    this.phase = Math.random() * 6;
    this.wins = 0;
    this.reset(0, 1);
  }
  reset(x, face) {
    this.x = x; this.y = 0; this.vx = 0; this.vy = 0; this.facing = face;
    this.hp = this.def.hp; this.meter = 0; this.st = 'IDLE';
    this.move = null; this.moveT = 0; this.hitDone = false; this.chain = 0;
    this.stun = 0; this.blocking = false; this.flash = 0; this.combo = 0; this.comboT = 0;
    this.walkPhase = 0; this.koT = 0; this.superReady = false;
    this.rig.yaw = face > 0 ? -0.42 : Math.PI + 0.42;
    this.rig.flash(false); this.rig._fl = false;
  }
  get onGround() { return this.y <= 0.0001; }
  get crouching() { return this.st === 'CROUCH' || (this.move && (this.move.id === 'SWEEP' || this.move.id === 'UPPER')); }
}

class Ai {
  constructor(level) { this.level = level; this.inp = Ai.blank(); this.plan = 0; this.planT = 0; this.blockT = 0; this.atkCd = 40; this.prevOp = false; }
  static blank() { return { left: false, right: false, up: false, down: false, punch: false, kick: false, grab: false, sup: false }; }
  think(me, op) {
    const i = this.inp = Ai.blank(), lv = this.level, r = Math.random;
    if (me.st === 'KO' || op.st === 'KO') return;
    const dx = op.x - me.x, d = Math.abs(dx), tw = dx > 0 ? 1 : -1;
    const toward = () => { if (tw > 0) i.right = true; else i.left = true; };
    const away = () => { if (tw > 0) i.left = true; else i.right = true; };
    if (this.atkCd > 0) this.atkCd--;
    const opAtk = op.st === 'ATTACK';
    if (opAtk && !this.prevOp && d < 2.9 && r() < Math.min(0.88, 0.18 + 0.27 * lv)) this.blockT = 18 + Math.floor(r() * 10);
    this.prevOp = opAtk;
    if (this.blockT > 0) { this.blockT--; if (me.onGround && me.st !== 'ATTACK') { away(); return; } }
    if (me.st === 'HIT' || me.st === 'BLOCK') return;
    if (this.planT-- <= 0) { this.plan = Math.floor(r() * 5); this.planT = Math.max(5, 14 + Math.floor(r() * 30) - lv * 7); }
    const free = me.st !== 'ATTACK' && me.onGround;
    switch (this.plan) {
      case 0: case 4: if (d > 1.7) toward(); else if (free) this.attack(d); break;
      case 1: if (d < 3.6) away(); else toward(); break;
      case 2: if (free && d > 2.6 && d < 6.5) i.up = true; toward(); break;
      default: if (d > 2.2 && r() < 0.05) toward();
    }
    if (!me.onGround && d < 2.4 && r() < 0.12) i.kick = true;
    if (me.meter >= 100 && d > 3.4 && free && r() < 0.04 + 0.03 * lv) i.sup = true;
    if (me.st === 'ATTACK' && me.hitDone && r() < Math.min(0.9, 0.25 + 0.2 * lv)) { if (r() < 0.6) i.punch = true; else i.kick = true; }
  }
  attack(d) {
    if (this.atkCd > 0) return;
    this.atkCd = Math.max(5, 26 - this.level * 6 + Math.floor(Math.random() * Math.max(4, 20 - this.level * 4)));
    const i = this.inp, x = Math.random() * 100;
    if (x < 38) i.punch = true;
    else if (x < 62) i.kick = true;
    else if (x < 74) { i.down = true; i.kick = true; }
    else if (x < 84) { i.down = true; i.punch = true; }
    else if (x < 94 && d < 1.7) i.grab = true;
    else i.punch = true;
  }
}

const DEF_MODS = { grav: 0.0105, speed: 1, dmg: 1, enemyDmg: 1, time: 60, m1: 0, m2: 0, bighead: false, mini: false };
const Engine = {
  scr: 'boot',          // boot | preview | intro | fight | roundend | result | pause
  frame: 0, t: 0, slow: 1, hitstop: 0, shake: 0,
  stage: 0, level: 1,
  keys: { left: false, right: false, up: false, down: false },
  edge: { punch: false, kick: false, grab: false, sup: false },
  fighters: [], projs: [], parts: [], stars: [], rings: [],
  round: 1, timer: 0, introT: 0, endT: 0, banner: '', paused: false,
  _acc: 0, _last: 0, mods: Object.assign({}, DEF_MODS),

  init(canvas) {
    const r = this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'high-performance' });
    r.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
    const sc = this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(38, 16 / 9, 0.1, 200);
    this.camera.position.set(0, 2.6, 10);
    this.hemi = new THREE.HemisphereLight(0xffffff, 0x444444, 1);
    this.dir = new THREE.DirectionalLight(0xffffff, 1);
    sc.add(this.hemi, this.dir, this.dir.target);
    this.blobTex = Arenas.blob();
    this.blobGeo = new THREE.PlaneGeometry(2.4, 1.3);
    this.blobGeo.rotateX(-Math.PI / 2);
    this.starTex = Arenas.canvasTex(128, 128, (c) => {
      c.translate(64, 64); c.beginPath();
      for (let i = 0; i < 16; i++) { const a = (i / 16) * Math.PI * 2, rr = i % 2 ? 26 : 60; c.lineTo(Math.cos(a) * rr, Math.sin(a) * rr); }
      c.closePath(); c.fillStyle = '#ffe34d'; c.fill(); c.lineWidth = 7; c.strokeStyle = '#e63b2e'; c.stroke();
    });
    // pools
    const sg = new THREE.OctahedronGeometry(0.1, 0);
    for (let i = 0; i < 46; i++) {
      const m = new THREE.Mesh(sg, new THREE.MeshBasicMaterial({ color: 0xffe34d, transparent: true }));
      m.visible = false; sc.add(m); this.parts.push({ m, vx: 0, vy: 0, vz: 0, life: 0, max: 1, grav: 0 });
    }
    for (let i = 0; i < 4; i++) {
      const s = new THREE.Sprite(new THREE.SpriteMaterial({ map: this.starTex, transparent: true, depthTest: false }));
      s.visible = false; s.renderOrder = 10; sc.add(s); this.stars.push({ s, life: 0 });
    }
    for (let i = 0; i < 4; i++) {
      const m = new THREE.Mesh(new THREE.RingGeometry(0.85, 1, 40), new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, side: THREE.DoubleSide, depthWrite: false }));
      m.visible = false; sc.add(m); this.rings.push({ m, life: 0, max: 20, big: 6 });
    }
    window.addEventListener('resize', () => this.resize());
    this.resize();
    this.setStage(0);
  },

  resize() {
    const w = window.innerWidth, h = window.innerHeight;
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h; this.camera.updateProjectionMatrix();
  },

  setStage(i) {
    this.stage = i;
    if (this.arena) this.scene.remove(this.arena.group);
    this.arena = Arenas.get(i);
    this.scene.add(this.arena.group);
    this.scene.background = new THREE.Color(this.arena.bg);
    const f = this.arena.fog; this.scene.fog = new THREE.Fog(f[0], f[1], f[2]);
    const L = this.arena.light;
    this.hemi.color.setHex(L.sky); this.hemi.groundColor.setHex(L.ground); this.hemi.intensity = L.hi;
    this.dir.color.setHex(L.dir); this.dir.intensity = L.di; this.dir.position.set(L.pos[0], L.pos[1], L.pos[2]);
  },

  /* ---------- fighters / matches ---------- */
  setFighters(d1, d2) {
    this.fighters.forEach((f) => { this.scene.remove(f.rig.root); if (f.blob) this.scene.remove(f.blob); });
    this.p1 = new Fighter(d1, true); this.p2 = new Fighter(d2, false);
    this.fighters = [this.p1, this.p2];
    this.fighters.forEach((f) => {
      f.blob = new THREE.Mesh(this.blobGeo, new THREE.MeshBasicMaterial({ map: this.blobTex, transparent: true, depthWrite: false }));
      f.blob.position.y = 0.02;
      this.scene.add(f.rig.root, f.blob);
    });
  },
  preview(i1, i2, x) {
    this.setFighters(ROSTER[i1], ROSTER[i2]);
    const sp = x || 2.7;
    this.p1.reset(-sp, 1); this.p2.reset(sp, -1);
    this.projs.forEach((p) => this.scene.remove(p.mesh)); this.projs = [];
    this.scr = 'preview';
  },
  swapPreview(side, idx) {
    const f = side === 1 ? this.p1 : this.p2;
    const x = f.x, face = f.facing, wins = 0;
    this.scene.remove(f.rig.root); this.scene.remove(f.blob);
    const nf = new Fighter(ROSTER[idx], side === 1);
    nf.reset(x, face); nf.blob = f.blob; nf.blob.visible = true;
    this.scene.add(nf.rig.root, nf.blob);
    if (side === 1) { this.p1 = nf; this.fighters[0] = nf; } else { this.p2 = nf; this.fighters[1] = nf; }
    nf.st = 'ATTACK'; nf.move = MV.P3; nf.moveT = 8; // little greeting pose
    nf.st = 'IDLE'; nf.move = null;
  },

  startMatch(i1, i2, level, stage) {
    this.mods = Object.assign({}, DEF_MODS); this.level = level; this.setStage(stage);
    this.setFighters(ROSTER[i1], ROSTER[i2]);
    this.ai = new Ai(level); this.round = 1; this.p1.wins = 0; this.p2.wins = 0;
    this.startRound();
  },
  startRound() {
    this.p1.reset(-3.2, 1); this.p2.reset(3.2, -1); this.p1.meter = this.mods.m1; this.p2.meter = this.mods.m2;
    this.projs.forEach((p) => this.scene.remove(p.mesh)); this.projs = [];
    this.timer = this.mods.time * 60; this.hitstop = 0; this.slow = 1;
    this.introT = 125; this.clearInput(); this.scr = 'intro';
    this._bell = false; this.camJump = true;
    UI.onRound && UI.onRound(this.round);
  },
  endRound() {
    const ko = this.p1.hp <= 0 || this.p2.hp <= 0;
    this.banner = ko ? 'K.O.!' : 'TIME UP!';
    let winner = null;
    if (this.p1.hp > this.p2.hp) winner = this.p1; else if (this.p2.hp > this.p1.hp) winner = this.p2;
    if (winner) { winner.wins++; }
    this.roundWinner = winner;
    this.endT = 190; this.scr = 'roundend';
    if (!ko) { this.slow = 1; }
    UI.onBanner && UI.onBanner(this.banner);
  },
  afterRound() {
    if (this.p1.wins >= 2 || this.p2.wins >= 2) {
      this.scr = 'result';
      UI.onResult && UI.onResult(this.p1.wins >= 2);
    } else { this.round++; this.startRound(); }
  },
  startLevel(info, i1) {
    this.mods = Object.assign({}, DEF_MODS, info.mods);
    let d1 = ROSTER[i1];
    if (this.mods.mini) d1 = Object.assign({}, d1, { scale: (d1.scale || 1) * 0.75 });
    this.level = info.ai; this.setStage(info.stage); this.setFighters(d1, info.def);
    if (this.mods.bighead) this.fighters.forEach((f) => { f.rig.head.scale.setScalar(1.55); f.rig.head.position.y += 0.3; });
    this.ai = new Ai(info.ai); this.round = 1; this.p1.wins = 0; this.p2.wins = 0;
    this.startRound();
  },
  rematch() { this.p1.wins = 0; this.p2.wins = 0; this.round = 1; this.startRound(); },

  /* ---------- input ---------- */
  clearInput() { for (const k in this.keys) this.keys[k] = false; for (const k in this.edge) this.edge[k] = false; },
  press(k) {
    if (k in this.keys) this.keys[k] = true;
    else if (k in this.edge) { this.edge[k] = true; }
  },
  release(k) { if (k in this.keys) this.keys[k] = false; },

  /* ---------- main loop ---------- */
  frameLoop(ms) {
    const dt = Math.min(0.1, (ms - (this._last || ms)) / 1000);
    this._last = ms;
    if (!this.paused) {
      this._acc += dt * 60 * this.slow * (this.mods.speed || 1);
      let n = 0;
      while (this._acc >= 1 && n < 5) { this.update(); this._acc -= 1; n++; }
      if (n === 5) this._acc = 0;
    }
    this.render();
  },

  update() {
    this.frame++; this.t = this.frame / 60;
    if (this.shake > 0) this.shake = Math.max(0, this.shake - 0.025);
    this.arena && this.arena.update(this.t);
    this.updateFx();

    const s = this.scr;
    if (s === 'fight') this.updateFight();
    else if (s === 'intro') {
      this.introT--;
      if (this.introT === 55 && !this._bell) { this._bell = true; Sfx.bell(); }
      if (this.introT <= 0) { this.scr = 'fight'; this.clearInput(); }
      this.fighters.forEach((f) => { f.st = 'IDLE'; this.physics(f); });
    } else if (s === 'roundend') {
      this.endT--;
      const w = this.roundWinner;
      this.fighters.forEach((f) => {
        if (f.st === 'KO') { f.koT++; f.vx *= 0.92; this.physics(f); }
        else if (w === f && this.endT < 150) { f.st = 'WIN'; }
        else { f.vx = 0; this.physics(f); }
      });
      if (this.endT === 150 && this.slow < 1) this.slow = 1;
      if (this.endT <= 0) this.afterRound();
    } else if (s === 'result') {
      this.fighters.forEach((f) => { if (f.st === 'KO') { f.koT++; this.physics(f); } });
    } else if (s === 'preview' || s === 'boot') {
      this.fighters.forEach((f) => { f.st = 'IDLE'; });
    }
    this.fighters.forEach((f) => { animateFighter(f, this.frame); f.blob.position.x = f.x; f.blob.position.z = 0; const sc = Math.max(0.3, 1 - f.y * 0.25); f.blob.scale.set(sc, 1, sc); f.blob.material.opacity = Math.max(0.25, 1 - f.y * 0.3); });
    this.updateCamera();
  },

  updateFight() {
    if (this.hitstop > 0) { this.hitstop--; return; }
    this.timer--;
    const k = this.keys, in1 = { left: k.left, right: k.right, up: k.up, down: k.down, punch: this.edge.punch, kick: this.edge.kick, grab: this.edge.grab, sup: this.edge.sup };
    for (const e in this.edge) this.edge[e] = false;
    this.ai.think(this.p2, this.p1);
    this.step(this.p1, this.p2, in1);
    this.step(this.p2, this.p1, this.ai.inp);
    this.updateProj();
    this.pushApart();
    [this.p1, this.p2].forEach((f) => { const rdy = f.meter >= 100; if (rdy && !f.superReady && f.isP1) Sfx.superReady(); f.superReady = rdy; });
    if (this.p1.hp <= 0 || this.p2.hp <= 0 || this.timer <= 0) this.endRound();
  },

  /* ---------- fighter state machine ---------- */
  step(f, o, inp) {
    const sp = W.WALK * f.def.spd;
    if (f.flash > 0) f.flash--;
    if (f.comboT > 0 && --f.comboT === 0) f.combo = 0;
    if (f.st === 'KO') { f.koT++; f.vx *= 0.92; this.physics(f); return; }
    if (f.st === 'HIT' || f.st === 'BLOCK') {
      f.stun--; f.vx *= 0.86; f.blocking = false;
      if (f.stun <= 0 && f.onGround) { f.st = 'IDLE'; f.vx = 0; }
      else if (f.stun <= 0) { f.st = 'IDLE'; }
      this.physics(f); return;
    }
    if (f.st === 'ATTACK') { f.blocking = false; this.updateAttack(f, o, inp); this.physics(f); return; }

    if (f.onGround && f.vy === 0) f.facing = o.x > f.x ? 1 : -1;
    if (f.onGround) {
      const back = (f.facing === 1 && inp.left) || (f.facing === -1 && inp.right);
      const dir = (inp.right ? 1 : 0) - (inp.left ? 1 : 0);
      f.blocking = back;
      let m = null;
      if (inp.sup && f.meter >= 100) m = MV.SUPER;
      else if (inp.grab || (inp.punch && inp.kick)) m = MV.GRAB;
      else if (inp.punch) m = inp.down ? MV.UPPER : MV.P1;
      else if (inp.kick) m = inp.down ? MV.SWEEP : MV.KICK;
      if (m) this.begin(f, m, true);
      else if (inp.up) { f.vy = W.JUMPV; f.vx = dir * sp; f.st = 'JUMP'; f.blocking = false; if (f.isP1) Sfx.jump(); }
      else if (inp.down) { f.st = 'CROUCH'; f.vx = 0; }
      else {
        f.vx = dir * sp * (back ? 0.65 : 1);
        f.st = dir !== 0 ? 'WALK' : 'IDLE';
        if (dir !== 0) f.walkPhase += 0.2 * f.def.spd + 0.08;
      }
    } else {
      f.st = 'JUMP'; f.blocking = false;
      if (inp.kick || inp.punch) this.begin(f, MV.AIRKICK, true);
    }
    this.physics(f);
  },

  begin(f, m, resetChain) {
    f.move = m; f.moveT = 0; f.hitDone = false; f.st = 'ATTACK'; f.blocking = false;
    if (resetChain) f.chain = 0;
    if (m.id !== 'AIRKICK') f.vx = 0;
    if (m.sup) { f.meter = 0; this.flashScreen(f.def.sup.color); Sfx.sup(); }
    else Sfx.whoosh();
  },

  updateAttack(f, o, inp) {
    const m = f.move;
    if (!m) { f.st = 'IDLE'; return; }
    f.moveT++;
    if (m.sup) {
      if (f.moveT === m.from) this.spawnSuper(f);
      if (f.moveT < m.from) f.vx = 0;
    } else if (!f.hitDone && f.moveT >= m.from && f.moveT <= m.to && this.inRange(f, o, m)) {
      f.hitDone = true; this.resolve(f, o, m);
    }
    if (f.hitDone && f.chain < 2 && (m.id === 'P1' || m.id === 'P2' || m.id === 'KICK')) {
      if (inp.punch) { f.chain++; this.begin(f, PUNCH_CHAIN[Math.min(f.chain, 2)], false); return; }
      if (inp.kick) { f.chain++; this.begin(f, MV.KICK2, false); return; }
    }
    if (m.id !== 'AIRKICK' && f.moveT >= m.total) { f.st = 'IDLE'; f.move = null; }
  },

  inRange(f, o, m) {
    if (o.st === 'KO') return false;
    const dx = (o.x - f.x) * f.facing;
    if (dx < -0.4 || dx > m.reach) return false;
    if (Math.abs(f.y - o.y) > 1.5) return false;
    if (m.lvl === 'H' && o.crouching && o.onGround) return false;
    if (m.lvl === 'L' && !o.onGround) return false;
    if (m.grab && !o.onGround) return false;
    return true;
  },

  resolve(f, o, m) {
    let dmg = m.dmg * f.def.pwr * Math.max(0.5, 1 - 0.08 * f.combo) * this.mods.dmg * (f.isP1 ? 1 : this.mods.enemyDmg);
    const hp = this.chest(o);
    if (m.grab) {
      o.hp -= dmg * o.def.def; o.st = 'HIT'; o.stun = m.stun; o.move = null;
      o.vx = f.facing * 0.14; o.vy = 0.15; f.meter = Math.min(100, f.meter + 14); o.meter = Math.min(100, o.meter + 8);
      f.combo++; f.comboT = 70; this.pop('PATAK!', hp, '#ff5a36'); this.burst(hp, 0xff8a3d, 18); this.starAt(hp, 2.4);
      this.shake = 0.5; this.hitstop = 9; Sfx.hit(true); UI.vibe && UI.vibe(30);
      if (o.hp <= 0) this.ko(o, f.facing);
      return;
    }
    if (o.blocking && o.onGround) {
      o.hp = Math.max(1, o.hp - dmg * 0.12); o.st = 'BLOCK'; o.stun = 10;
      o.vx = f.facing * (m.kb * 5 + 0.03); f.meter = Math.min(100, f.meter + 3);
      this.pop('BLOCK', hp, '#9ad1ff'); this.burst(hp, 0x9ad1ff, 6); this.hitstop = 3; Sfx.block();
      return;
    }
    const heavy = m.id === 'P3' || m.id === 'KICK2' || m.id === 'SWEEP' || m.id === 'UPPER';
    o.hp -= dmg * o.def.def; f.meter = Math.min(100, f.meter + dmg * 1.3); o.meter = Math.min(100, o.meter + dmg * 0.8);
    o.st = 'HIT'; o.stun = m.stun; o.move = null; o.vx = f.facing * m.kb;
    if (m.id === 'SWEEP') o.vy = 0.1;
    if (m.launch) o.vy = m.launch;
    o.flash = 6; f.combo++; f.comboT = 70;
    this.pop(HIT_WORDS[Math.floor(Math.random() * HIT_WORDS.length)], hp, '#ffe066');
    this.burst(hp, heavy ? 0xffa23a : 0xffe34d, heavy ? 16 : 9); this.starAt(hp, heavy ? 2.4 : 1.6);
    this.hitstop = heavy ? 9 : 5; this.shake = heavy ? 0.45 : 0.2; Sfx.hit(heavy);
    if (heavy && UI.vibe) UI.vibe(18);
    if (o.hp <= 0) this.ko(o, f.facing);
  },
  chest(o) { return new THREE.Vector3(o.x, o.y + 1.9 * (o.def.scale || 1) * (o.crouching ? 0.65 : 1), 0.4); },

  ko(o, dir) {
    o.hp = 0; o.st = 'KO'; o.move = null; o.koT = 0; o.vx = dir * 0.2; o.vy = 0.17; this.hitstop = 22; this.shake = 0.9;
    this.slow = 0.35; this.ringAt(new THREE.Vector3(o.x, 1.5, 0.5), 0xffffff, 8); Sfx.ko();
    setTimeout(() => { this.slow = 1; }, 1200);
  },

  physics(f) {
    f.x += f.vx;
    if (f.x < W.MINX) f.x = W.MINX; if (f.x > W.MAXX) f.x = W.MAXX;
    if (f.y > 0 || f.vy > 0) {
      f.vy -= this.mods.grav; f.y += f.vy;
      if (f.y <= 0) {
        f.y = 0;
        if (f.st === 'KO' && f.vy < -0.05) { f.vy = -f.vy * 0.3; f.y = 0.001; this.dust(f.x); Sfx.land(); }
        else {
          f.vy = 0;
          if (f.st === 'JUMP') { f.st = 'IDLE'; f.vx = 0; this.dust(f.x); if (f.isP1) Sfx.land(); }
          if (f.move && f.move.id === 'AIRKICK') { f.move = null; f.st = 'IDLE'; f.vx = 0; }
          if (f.st === 'HIT' && f.stun <= 0) f.st = 'IDLE';
        }
      }
    }
  },

  pushApart() {
    const a = this.p1, b = this.p2;
    if (!a.onGround || !b.onGround) return;
    const d = b.x - a.x, minD = 1.05;
    if (Math.abs(d) < minD) {
      const push = (minD - Math.abs(d)) / 2, s = d >= 0 ? 1 : -1;
      a.x = Math.max(W.MINX, Math.min(W.MAXX, a.x - s * push));
      b.x = Math.max(W.MINX, Math.min(W.MAXX, b.x + s * push));
    }
  },

  /* ---------- super projectiles ---------- */
  spawnSuper(f) {
    const sup = f.def.sup, mesh = this.superMesh(sup), dir = f.facing;
    mesh.position.set(f.x + dir * 1.4, 1.9, 0.3); this.scene.add(mesh);
    this.projs.push({ owner: f, x: f.x + dir * 1.4, y: 1.9, vx: dir * 0.2, mesh, dmg: sup.dmg * f.def.pwr * this.mods.dmg * (f.isP1 ? 1 : this.mods.enemyDmg), life: 110, shape: sup.shape, color: sup.color, dir: dir });
    this.ringAt(new THREE.Vector3(f.x + dir * 1.2, 1.9, 0.3), sup.color, 5);
    this.shake = 0.6; this.pop(sup.name + '!', new THREE.Vector3(f.x, 3.8, 0.5), '#ffc83d');
  },
  superMesh(sup) {
    const g = new THREE.Group(), c = sup.color;
    const M = (col) => Toon.mat(col), glow = new THREE.MeshBasicMaterial({ color: c, transparent: true, opacity: 0.4, blending: THREE.AdditiveBlending, depthWrite: false });
    const add = (geo, col, x, y, z, rx, ry, rz) => { const m = new THREE.Mesh(geo, M(col)); m.position.set(x || 0, y || 0, z || 0); if (rx || ry || rz) m.rotation.set(rx || 0, ry || 0, rz || 0); g.add(m); const ol = new THREE.Mesh(geo, Toon.outMat()); ol.scale.setScalar(1.1); m.add(ol); return m; };
    switch (sup.shape) {
      case 'chair': add(Toon.box(1.1, 0.2, 1.0), 0xfbbf24, 0, 0, 0); add(Toon.box(1.1, 1.1, 0.18), 0xfbbf24, -0.0, 0.6, -0.45); [[-.45, -.4], [.45, -.4], [-.45, .4], [.45, .4]].forEach((p) => add(Toon.box(0.14, 0.7, 0.14), 0xd97706, p[0], -0.4, p[1])); g.scale.setScalar(1.15); break;
      case 'cup': add(Toon.cyl(0.55, 0.38, 0.9, 16), 0xc2410c, 0, 0, 0); add(Toon.cyl(0.52, 0.52, 0.06, 16), 0xb45309, 0, 0.42, 0); add(Toon.torus(0.3, 0.07), 0xc2410c, 0.6, 0, 0); break;
      case 'scroll': for (let i = 0; i < 3; i++) { add(Toon.cyl(0.18, 0.18, 1.0, 12), 0xfffaf0, 0, 0, 0, 0, 0, Math.PI / 2).position.set(0, (i - 1) * 0.5, (i - 1) * 0.2); add(Toon.cyl(0.2, 0.2, 0.1, 12), 0xef4444, 0.5, (i - 1) * 0.5, (i - 1) * 0.2, 0, 0, Math.PI / 2); } break;
      case 'mega': add(Toon.cone(0.8, 1.4, 16), 0xff6b35, 0, 0, 0, 0, 0, -Math.PI / 2); add(Toon.cyl(0.25, 0.25, 0.7, 10), 0x333333, -0.9, 0, 0, 0, 0, Math.PI / 2); break;
      case 'barrier': add(Toon.box(2.2, 0.28, 0.28), 0xffffff, 0, 0, 0); for (let i = 0; i < 4; i++) add(Toon.box(0.3, 0.3, 0.3), 0xef4444, -0.8 + i * 0.52, 0, 0.0); add(Toon.cyl(0.18, 0.18, 1.4, 10), 0x555555, -1.1, -0.5, 0); break;
      case 'phone': add(Toon.box(0.7, 1.3, 0.14), 0x1f2937, 0, 0, 0); { const sc = new THREE.Mesh(Toon.box(0.58, 1.1, 0.04), new THREE.MeshBasicMaterial({ color: c })); sc.position.z = 0.09; g.add(sc); } g.scale.setScalar(1.3); break;
      case 'laddoo': for (let i = 0; i < 4; i++) add(Toon.sphere(0.34, 14, 10), 0xf59e0b, -i * 0.5, Math.sin(i * 2) * 0.35, Math.cos(i * 2) * 0.3); break;
      default: add(Toon.sphere(0.6, 16, 12), c, 0, 0, 0);
    }
    const halo = new THREE.Mesh(new THREE.SphereGeometry(1.2, 14, 10), glow); g.add(halo);
    return g;
  },
  updateProj() {
    for (let i = this.projs.length - 1; i >= 0; i--) {
      const pr = this.projs[i]; pr.x += pr.vx; pr.life--;
      pr.mesh.position.x = pr.x; pr.mesh.rotation.x += 0.18; pr.mesh.rotation.z += pr.shape === 'mega' ? 0 : 0.04;
      if (this.frame % 2 === 0) this.spark(new THREE.Vector3(pr.x, pr.y + (Math.random() - 0.5), 0.3), pr.color, 0.4);
      const tgt = pr.owner === this.p1 ? this.p2 : this.p1;
      let kill = pr.life <= 0 || Math.abs(pr.x) > 16;
      if (!kill && tgt.st !== 'KO' && Math.abs(pr.x - tgt.x) < 1.0 && tgt.y < 2.6) {
        kill = true; const dir = pr.vx > 0 ? 1 : -1, hp = this.chest(tgt);
        if (tgt.blocking && tgt.onGround) {
          tgt.hp = Math.max(1, tgt.hp - pr.dmg * 0.2); tgt.st = 'BLOCK'; tgt.stun = 14; tgt.vx = dir * 0.12;
          this.pop('BLOCK', hp, '#9ad1ff'); Sfx.block();
        } else {
          tgt.hp -= pr.dmg * tgt.def.def; tgt.meter = Math.min(100, tgt.meter + 10); tgt.st = 'HIT'; tgt.stun = 34; tgt.move = null;
          tgt.vx = dir * 0.2; tgt.vy = 0.16; tgt.flash = 8; pr.owner.combo++; pr.owner.comboT = 70;
          this.pop('BOOM!', hp, '#ff7a3d'); this.burst(hp, pr.color, 26); this.starAt(hp, 3.2); this.ringAt(hp, pr.color, 7);
          this.shake = 0.8; this.hitstop = 10; Sfx.boom(); UI.vibe && UI.vibe(40);
          if (tgt.hp <= 0) this.ko(tgt, dir);
        }
      }
      if (kill) { this.scene.remove(pr.mesh); this.projs.splice(i, 1); }
    }
  },

  /* ---------- effects ---------- */
  flashScreen(color) { UI.flash && UI.flash(color); },
  pop(text, v3, color) { UI.pop && UI.pop(text, v3, color); },
  spark(p, color, life) {
    const q = this.parts.find((x) => !x.m.visible); if (!q) return;
    q.m.visible = true; q.m.position.copy(p); q.m.material.color.setHex(color); q.m.material.opacity = 1; q.m.scale.setScalar(1.3);
    q.vx = (Math.random() - 0.5) * 0.02; q.vy = (Math.random() - 0.5) * 0.02; q.vz = 0; q.life = q.max = (life || 0.5) * 60; q.grav = 0;
  },
  burst(p, color, n) {
    for (let i = 0; i < n; i++) {
      const q = this.parts.find((x) => !x.m.visible); if (!q) return;
      q.m.visible = true; q.m.position.copy(p); q.m.material.color.setHex(i % 3 ? color : 0xffffff); q.m.material.opacity = 1; q.m.scale.setScalar(0.9 + Math.random());
      const a = Math.random() * Math.PI * 2, s = 0.06 + Math.random() * 0.12;
      q.vx = Math.cos(a) * s; q.vy = Math.sin(a) * s + 0.03; q.vz = (Math.random() - 0.3) * 0.08; q.life = q.max = 22 + Math.random() * 14; q.grav = 0.004;
    }
  },
  dust(x) { for (let i = 0; i < 5; i++) { const q = this.parts.find((z) => !z.m.visible); if (!q) return; q.m.visible = true; q.m.position.set(x, 0.1, 0.4); q.m.material.color.setHex(0xd9c7a0); q.m.material.opacity = 1; q.m.scale.setScalar(1.6); q.vx = (Math.random() - 0.5) * 0.09; q.vy = 0.02 + Math.random() * 0.03; q.vz = Math.random() * 0.03; q.life = q.max = 18; q.grav = 0; } },
  starAt(p, size) { const s = this.stars.find((x) => x.life <= 0); if (!s) return; s.life = 14; s.size = size; s.s.visible = true; s.s.position.copy(p); s.s.position.z += 0.6; s.s.material.rotation = Math.random() * 6; },
  ringAt(p, color, big) { const r = this.rings.find((x) => x.life <= 0); if (!r) return; r.life = r.max = 22; r.big = big; r.m.visible = true; r.m.position.copy(p); r.m.material.color.setHex(color); r.m.lookAt(p.x, p.y, p.z + 10); },
  updateFx() {
    for (const q of this.parts) {
      if (!q.m.visible) continue;
      q.life--; q.m.position.x += q.vx; q.m.position.y += q.vy; q.m.position.z += q.vz; q.vy -= q.grav;
      q.m.material.opacity = Math.max(0, q.life / q.max); q.m.rotation.x += 0.2; q.m.rotation.y += 0.15;
      if (q.life <= 0) q.m.visible = false;
    }
    for (const s of this.stars) {
      if (s.life <= 0) continue; s.life--;
      const k = 1 - s.life / 14; s.s.scale.setScalar(s.size * (0.5 + k * 1.2)); s.s.material.opacity = 1 - k * k;
      if (s.life <= 0) s.s.visible = false;
    }
    for (const r of this.rings) {
      if (r.life <= 0) continue; r.life--;
      const k = 1 - r.life / r.max; r.m.scale.setScalar(0.3 + k * r.big); r.m.material.opacity = 1 - k;
      if (r.life <= 0) r.m.visible = false;
    }
  },

  /* ---------- camera ---------- */
  updateCamera() {
    const cam = this.camera, a = this.p1, b = this.p2;
    if (!a || !b) return;
    let tx, ty, tz, ly;
    const aspect = cam.aspect, th = Math.tan(THREE.MathUtils.degToRad(cam.fov / 2)) * aspect;
    if (this.scr === 'preview' || this.scr === 'boot') {
      const sw = Math.sin(this.t * 0.35) * 0.9;
      const wide = this._previewWide;
      tx = sw; ty = wide ? 2.8 : 2.35; tz = Math.max(wide ? 10.5 : 9.5, ((Math.abs(a.x - b.x) / 2) + 3.2) / th); ly = wide ? 1.55 : 0.95;
      this.camLook = this.camLook || new THREE.Vector3(0, ly, 0);
      cam.position.x += (tx - cam.position.x) * 0.06; cam.position.y += (ty - cam.position.y) * 0.06; cam.position.z += (tz - cam.position.z) * 0.06;
      this.camLook.x += (sw * 0.3 - this.camLook.x) * 0.06; this.camLook.y += (ly - this.camLook.y) * 0.06;
      cam.lookAt(this.camLook.x, this.camLook.y, 0);
      return;
    }
    const mid = (a.x + b.x) / 2, dx = Math.abs(a.x - b.x);
    tz = Math.max(8.6, (dx / 2 + 3.4) / th); tz = Math.min(tz, 19);
    ty = 2.5 + (tz - 8.6) * 0.06; tx = mid * 0.9; ly = 1.55 + Math.max(a.y, b.y) * 0.2;
    if (this.scr === 'roundend' && this.roundWinner) { tx = this.roundWinner.x; tz = 7.5; ty = 2.6; }
    this.camLook = this.camLook || new THREE.Vector3(mid, ly, 0);
    const k = this.camJump ? 1 : 0.1; this.camJump = false;
    cam.position.x += (tx - cam.position.x) * k; cam.position.y += (ty - cam.position.y) * k; cam.position.z += (tz - cam.position.z) * k;
    this.camLook.x += (tx - this.camLook.x) * k; this.camLook.y += (ly - this.camLook.y) * k;
    cam.lookAt(this.camLook.x, this.camLook.y, 0);
    if (this.shake > 0) { cam.position.x += (Math.random() - 0.5) * this.shake * 0.5; cam.position.y += (Math.random() - 0.5) * this.shake * 0.4; }
  },

  render() {
    this.renderer.render(this.scene, this.camera);
    UI.afterRender && UI.afterRender();
  },

  project(v3) {
    const v = v3.clone().project(this.camera);
    return { x: (v.x * 0.5 + 0.5) * window.innerWidth, y: (-v.y * 0.5 + 0.5) * window.innerHeight };
  }
};
