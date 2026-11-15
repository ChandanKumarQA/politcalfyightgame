'use strict';
/* DOM screens, input (touch + keyboard), portraits, HUD. */
const $ = (id) => document.getElementById(id);
const Q = new URLSearchParams(location.search);

const UI = {
  screen: 'title', mode: 'vs', lv: 1, selLv: 1, page: 0, save: null, info: null, sel1: 0, sel2: 1, phase: 0, stage: 0, level: 1, portraits: [], touch: false,
  _hud: {},

  init() {
    Engine.init($('gl'));
    this.touch = Q.has('touch') || ('ontouchstart' in window) || (navigator.maxTouchPoints > 0);
    this.makePortraits();
    this.buildSelect();
    this.loadSave(); this.buildLevels();
    this.bindInput();
    Engine.preview(0, 3, 4.2); Engine._previewWide = true;
    this.show('title');
    requestAnimationFrame(function loop(ms) { Engine.frameLoop(ms); requestAnimationFrame(loop); });
    setTimeout(() => $('loading').classList.add('done'), 150);

    // Auto-start from URL query params (Native Intent launch)
    if (Q.has('autostart') && Q.get('autostart') === '1') {
      const p1 = parseInt(Q.get('p1')) || 0;
      const p2 = parseInt(Q.get('p2')) || 1;
      const stage = parseInt(Q.get('stage')) || 0;
      this.sel1 = p1 % ROSTER.length;
      this.sel2 = p2 % ROSTER.length;
      this.stage = stage % STAGE_NAMES.length;
      setTimeout(() => this.startFight(), 220);
    }
  },

  show(name) {
    this.screen = name;
    ['title', 'select', 'levels', 'pause', 'result'].forEach((s) => $(s).classList.toggle('on', s === name));
    const fight = name === 'fight' || name === 'pause' || name === 'result';
    $('hud').classList.toggle('off', !fight);
    $('pad').classList.toggle('off', !(name === 'fight' && this.touch));
    $('keys').classList.toggle('off', !(name === 'fight' && !this.touch));
  },

  /* ----- portraits rendered once with a throw-away WebGL context ----- */
  makePortraits() {
    try {
      const cv = document.createElement('canvas'); cv.width = cv.height = 160;
      const r = new THREE.WebGLRenderer({ canvas: cv, antialias: true, alpha: true, preserveDrawingBuffer: true });
      r.setSize(160, 160, false); r.setClearColor(0x000000, 0);
      const sc = new THREE.Scene(), cam = new THREE.PerspectiveCamera(26, 1, 0.1, 50);
      sc.add(new THREE.HemisphereLight(0xffffff, 0x665544, 1.0)); const dl = new THREE.DirectionalLight(0xffffff, 1.0); dl.position.set(3, 5, 6); sc.add(dl);
      const out = document.createElement('canvas'); out.width = out.height = 160; const oc = out.getContext('2d');
      ROSTER.forEach((d) => {
        const rig = buildFighter(d); const S = d.scale || 1;
        rig.root.rotation.y = -1.2; sc.add(rig.root);
        const cy = 2.3 * S;
        cam.position.set(0.6, cy + 0.15, 4.6 * S); cam.lookAt(0, cy - 0.15, 0);
        r.render(sc, cam);
        oc.clearRect(0, 0, 160, 160); oc.drawImage(cv, 0, 0);
        this.portraits.push(out.toDataURL('image/png'));
        sc.remove(rig.root);
      });
      r.dispose(); if (r.forceContextLoss) r.forceContextLoss();
    } catch (e) { console.warn('portraits failed', e); }
  },

  buildSelect() {
    const rs = $('roster'); rs.innerHTML = '';
    ROSTER.forEach((d, i) => {
      const b = document.createElement('button'); b.className = 'card' + (d.boss ? ' boss' : ''); b.dataset.i = i;
      b.innerHTML = (this.portraits[i] ? '<img alt="" src="' + this.portraits[i] + '">' : '') + '<div class="cn">' + d.name.split(' ')[0] + '</div><span class="badge b1">1P</span><span class="badge b2">CPU</span>';
      b.addEventListener('click', () => this.pick(i));
      rs.appendChild(b);
    });
    const rb = document.createElement('button'); rb.className = 'card'; rb.innerHTML = '<div class="q">?</div><div class="cn">RANDOM</div>';
    rb.addEventListener('click', () => this.pick(Math.floor(Math.random() * ROSTER.length)));
    rs.appendChild(rb);
    $('stageBtns').innerHTML = STAGE_NAMES.map((n, i) => '<button class="opt" data-s="' + i + '">' + n + '</button>').join('');
    $('cpuBtns').innerHTML = LEVEL_NAMES.map((n, i) => '<button class="opt" data-l="' + i + '">' + n + '</button>').join('');
    $('stageBtns').addEventListener('click', (e) => { const s = e.target.dataset.s; if (s != null) { this.setStage(+s); Sfx.ui(); } });
    $('cpuBtns').addEventListener('click', (e) => { const l = e.target.dataset.l; if (l != null) { this.level = +l; this.refreshSelect(); Sfx.ui(); } });
    this.refreshSelect();
  },
  setStage(i) { this.stage = i; Engine.setStage(i); this.refreshSelect(); },
  pick(i) {
    Sfx.ui();
    if (this.phase === 0 || this.mode === 'story') { this.sel1 = i; Engine.swapPreview(1, i); } else { this.sel2 = i; Engine.swapPreview(2, i); }
    this.phase = this.mode === 'story' ? 0 : 1 - this.phase; this.refreshSelect();
  },
  info(d, who) {
    const bar = (v) => '<div class="b"><i style="width:' + Math.max(8, Math.min(100, v)) + '%"></i></div>';
    return '<div class="who">' + who + '</div><div class="big">' + d.name + '</div><div class="team">' + d.team + ' · ' + d.title + '</div>' +
      '<div class="sup">SUPER · <i>' + d.sup.name + '</i></div><div class="desc">' + d.sup.desc + '</div>' +
      '<div class="stats"><span>PWR</span>' + bar(d.pwr * 70) + '<span>SPD</span>' + bar(d.spd * 70) + '<span>DEF</span>' + bar((1 / d.def) * 55 + d.hp / 4) + '</div>';
  },
  refreshSelect() {
    $('info1').innerHTML = this.info(ROSTER[this.sel1], '1P · YOU');
    $('info2').innerHTML = this.info(ROSTER[this.sel2], 'CPU');
    $('pickLabel').textContent = this.mode === 'story' ? 'PICK YOUR FIGHTER' : this.phase === 0 ? 'PICK YOUR NETA  (1P)' : 'PICK CPU OPPONENT';
    document.querySelectorAll('#roster .card').forEach((c) => { const i = +c.dataset.i; c.classList.toggle('s1', i === this.sel1); c.classList.toggle('s2', i === this.sel2); });
    document.querySelectorAll('#stageBtns .opt').forEach((b) => b.classList.toggle('on', +b.dataset.s === this.stage));
    document.querySelectorAll('#cpuBtns .opt').forEach((b) => b.classList.toggle('on', +b.dataset.l === this.level));
  },

  /* ----- flow ----- */
  goSelect(story) {
    if (story !== undefined) this.mode = story ? 'story' : 'vs';
    $('select').classList.toggle('story', this.mode === 'story');
    $('fightBtn').textContent = this.mode === 'story' ? 'LEVELS ›' : 'FIGHT!';
    Sfx.resume(); Sfx.ui();
    Engine.preview(this.sel1, this.sel2, 2.7); Engine._previewWide = false; Engine.camLook = null;
    Engine.setStage(this.stage);
    this.show('select'); this.refreshSelect();
  },
  goTitle() { this.mode = 'vs'; $('select').classList.remove('story'); Engine.paused = false; Engine.preview(this.sel1, this.sel2, 4.2); Engine._previewWide = true; this.show('title'); },
  startFight() {
    if (this.mode === 'story') { this.goLevels(); return; }
    Sfx.resume(); Sfx.ui(); Engine.paused = false;
    this.hudInit();
    this.show('fight');
    Engine.startMatch(this.sel1, this.sel2, this.level, this.stage);
  },
  hudInit() {
    $('n1').textContent = ROSTER[this.sel1].name; $('n2').textContent = this.mode === 'story' && this.info ? this.info.def.name : ROSTER[this.sel2].name;
    $('lvTag').textContent = this.mode === 'story' && this.info ? 'LEVEL ' + this.info.n + ' · ' + this.info.tier + (this.info.modNames.length ? ' · ' + this.info.modNames.join(' + ') : '') : '';
    this._hud = {}; $('c1').classList.remove('show'); $('c2').classList.remove('show'); $('banner').className = '';
  },
  onRound(n) {
    const b = $('banner'); b.className = ''; void b.offsetWidth;
    b.textContent = 'ROUND ' + n; b.classList.add('show');
    setTimeout(() => { if (Engine.scr === 'intro') { b.className = ''; void b.offsetWidth; b.textContent = 'FIGHT!'; b.classList.add('show'); } }, 1050);
  },
  onBanner(t) { const b = $('banner'); b.className = ''; void b.offsetWidth; b.textContent = t; b.classList.add('show', 'long'); },
  onResult(win) {
    setTimeout(() => {
      if (Engine.scr !== 'result') return;
      const story = this.mode === 'story', ratio = Engine.p1.hp / Engine.p1.def.hp;
      const st = win ? (ratio > 0.66 ? 3 : ratio > 0.33 ? 2 : 1) : 0;
      $('resTitle').textContent = win ? (story ? 'LEVEL ' + this.lv + ' CLEAR!' : 'YOU WIN!') : 'YOU LOSE!'; $('resTitle').className = win ? '' : 'lose';
      $('resStars').textContent = story && win ? '★'.repeat(st) + '☆'.repeat(3 - st) : '';
      $('resHead').textContent = HEADLINES[Math.floor(Math.random() * HEADLINES.length)];
      $('nextBtn').style.display = story && win && this.lv < LEVEL_COUNT ? '' : 'none';
      $('rematchBtn').textContent = story ? 'RETRY' : 'REMATCH';
      $('charBtn').textContent = story ? 'LEVELS' : 'CHARACTER SELECT';
      if (story && win) { if ((this.save.stars[this.lv] || 0) < st) this.save.stars[this.lv] = st; this.save.last = Math.min(LEVEL_COUNT, this.lv + 1); this.writeSave(); }
      this.show('result'); win ? Sfx.win() : Sfx.lose();
    }, 1200);
  },
  /* ----- 500 levels ----- */
  loadSave() {
    try { this.save = JSON.parse(localStorage.getItem('kc3d_save') || 'null'); } catch (e) { this.save = null; }
    if (!this.save || !this.save.stars) this.save = { stars: {}, last: 1 };
  },
  writeSave() { try { localStorage.setItem('kc3d_save', JSON.stringify(this.save)); } catch (e) { } },
  buildLevels() {
    const tabs = $('lvTabs'); tabs.innerHTML = '';
    for (let p = 0; p < 10; p++) {
      const b = document.createElement('button'); b.className = 'tab'; b.textContent = (p * 50 + 1) + '–' + (p * 50 + 50); b.dataset.p = p;
      b.addEventListener('click', () => { this.setPage(p); Sfx.ui(); }); tabs.appendChild(b);
    }
    $('lvGrid').addEventListener('click', (e) => {
      const c = e.target.closest('.lv'); if (!c) return; const n = +c.dataset.n;
      if (n === this.selLv) this.startLevel(n); else { this.selectLevel(n); Sfx.ui(); }
    });
  },
  setPage(p) {
    this.page = p;
    document.querySelectorAll('#lvTabs .tab').forEach((t) => t.classList.toggle('on', +t.dataset.p === p));
    let h = '';
    for (let i = 1; i <= 50; i++) {
      const n = p * 50 + i, st = this.save.stars[n] || 0;
      h += '<button class="lv' + (n % 10 === 0 ? ' boss' : '') + (n === this.selLv ? ' sel' : '') + '" data-n="' + n + '">' + n + '<small><b>' + '★'.repeat(st) + '</b>' + '☆'.repeat(3 - st) + '</small></button>';
    }
    $('lvGrid').innerHTML = h; $('lvGrid').scrollTop = 0;
  },
  selectLevel(n) {
    this.selLv = n;
    const pg = Math.floor((n - 1) / 50); if (pg !== this.page || !$('lvGrid').children.length) this.setPage(pg);
    document.querySelectorAll('#lvGrid .lv').forEach((c) => c.classList.toggle('sel', +c.dataset.n === n));
    const i = levelInfo(n), d = i.def;
    $('lvText').innerHTML = 'LEVEL ' + n + ' · <i>' + d.name + '</i>' + (i.boss ? ' 👑 BOSS' : '') + ' · ' + STAGE_NAMES[i.stage] +
      '<span>' + i.tier + ' · HP ' + d.hp + ' · POWER ×' + d.pwr.toFixed(2) + ' · CPU SKILL ' + (i.ai / 3 * 100).toFixed(0) + '%' + (i.modNames.length ? ' · TWIST: ' + i.modNames.join(' + ') : '') + '</span>';
  },
  goLevels() {
    Sfx.resume(); Sfx.ui(); Engine.paused = false; this.mode = 'story';
    Engine.preview(this.sel1, this.sel2, 4.2); Engine._previewWide = true; Engine.camLook = null;
    this.show('levels'); this.selLv = Math.min(LEVEL_COUNT, Math.max(1, this.save.last || 1)); this.page = -1; this.selectLevel(this.selLv);
  },
  startLevel(n) {
    n = Math.max(1, Math.min(LEVEL_COUNT, n)); this.mode = 'story'; this.lv = n; this.selLv = n; this.info = levelInfo(n);
    this.save.last = n; this.writeSave();
    Sfx.resume(); Sfx.ui(); Engine.paused = false;
    this.hudInit(); this.show('fight'); Engine.startLevel(this.info, this.sel1);
  },
  pause(on) {
    if (this.screen !== 'fight' && this.screen !== 'pause') return;
    Engine.paused = on; this.show(on ? 'pause' : 'fight'); Sfx.ui();
    if (on) { Sfx.pause(); $('musicBtn').textContent = 'MUSIC: ' + (Sfx.musicOn() ? 'ON' : 'OFF'); } else Sfx.unpause();
  },
  back() {
    switch (this.screen) {
      case 'fight': this.pause(true); return true;
      case 'pause': this.pause(false); return true;
      case 'result': this.mode === 'story' ? this.goLevels() : this.goSelect(); return true;
      case 'levels': this.goSelect(true); return true;
      case 'select': this.goTitle(); return true;
      default: if (window.Native && Native.quit) Native.quit(); return false;
    }
  },
  vibe(ms) { try { if (window.Native && Native.vibrate) Native.vibrate(ms); else if (navigator.vibrate) navigator.vibrate(ms); } catch (e) { } },

  /* ----- effects bridge ----- */
  flash(color) { const f = $('flash'); f.style.background = '#' + color.toString(16).padStart(6, '0'); f.classList.remove('go'); void f.offsetWidth; f.classList.add('go'); },
  pop(text, v3, color) {
    const p = Engine.project(v3), d = document.createElement('div');
    d.className = 'pop'; d.textContent = text; d.style.left = p.x + 'px'; d.style.top = p.y + 'px'; d.style.setProperty('--c', color || '#ffe066');
    $('pops').appendChild(d); setTimeout(() => d.remove(), 900);
  },
  afterRender() {
    if (this.screen !== 'fight' && this.screen !== 'result' && this.screen !== 'pause') return;
    const a = Engine.p1, b = Engine.p2, H = this._hud;
    const set = (k, v, fn) => { if (H[k] !== v) { H[k] = v; fn(v); } };
    set('h1', Math.round(a.hp / a.def.hp * 1000), (v) => { $('h1').style.width = v / 10 + '%'; $('g1').style.width = v / 10 + '%'; });
    set('h2', Math.round(b.hp / b.def.hp * 1000), (v) => { $('h2').style.width = v / 10 + '%'; $('g2').style.width = v / 10 + '%'; });
    set('m1', Math.round(a.meter), (v) => { $('m1').style.width = v + '%'; $('m1').parentNode.classList.toggle('full', v >= 100); $('pad').querySelector('.sup').classList.toggle('ready', v >= 100); });
    set('m2', Math.round(b.meter), (v) => { $('m2').style.width = v + '%'; $('m2').parentNode.classList.toggle('full', v >= 100); });
    const secs = Math.max(0, Math.ceil(Engine.timer / 60));
    set('t', secs, (v) => { $('timer').textContent = v; $('timer').classList.toggle('low', v <= 10); });
    set('w', a.wins * 10 + b.wins, () => {
      $('pips1').innerHTML = [0, 1].map((i) => '<div class="pip' + (i < a.wins ? ' w' : '') + '"></div>').join('');
      $('pips2').innerHTML = [0, 1].map((i) => '<div class="pip' + (i < b.wins ? ' w' : '') + '"></div>').join('');
    });
    set('c1', a.combo >= 2 ? a.combo : 0, (v) => { const e = $('c1'); e.textContent = v + ' HITS!'; e.classList.toggle('show', v > 0); });
    set('c2', b.combo >= 2 ? b.combo : 0, (v) => { const e = $('c2'); e.textContent = v + ' HITS!'; e.classList.toggle('show', v > 0); });
  },

  /* ----- input ----- */
  bindInput() {
    $('vsBtn').addEventListener('click', () => this.goSelect(false));
    $('lvBtn').addEventListener('click', () => this.goSelect(true));
    $('lvBack').addEventListener('click', () => this.goSelect(true));
    $('lvStart').addEventListener('click', () => this.startLevel(this.selLv));
    $('nextBtn').addEventListener('click', () => this.startLevel(this.lv + 1));
    $('backBtn').addEventListener('click', () => this.goTitle());
    $('fightBtn').addEventListener('click', () => this.startFight());
    $('pauseBtn').addEventListener('click', () => this.pause(true));
    $('resumeBtn').addEventListener('click', () => this.pause(false));
    $('quitBtn').addEventListener('click', () => { Engine.paused = false; Sfx.unpause(); this.mode === 'story' ? this.goLevels() : this.goSelect(); });
    $('musicBtn').addEventListener('click', () => { const on = Sfx.toggleMusic(); $('musicBtn').textContent = 'MUSIC: ' + (on ? 'ON' : 'OFF'); });
    $('rematchBtn').addEventListener('click', () => { this.hudInit(); this.show('fight'); Engine.rematch(); Sfx.ui(); });
    $('charBtn').addEventListener('click', () => this.mode === 'story' ? this.goLevels() : this.goSelect());

    document.querySelectorAll('#pad [data-k]').forEach((el) => {
      const k = el.dataset.k;
      const dn = (e) => { e.preventDefault(); el.classList.add('on'); Engine.press(k); Sfx.resume(); };
      const up = (e) => { e.preventDefault(); el.classList.remove('on'); Engine.release(k); };
      el.addEventListener('pointerdown', dn); el.addEventListener('pointerup', up); el.addEventListener('pointercancel', up); el.addEventListener('pointerleave', up);
    });

    const map = { ArrowLeft: 'left', a: 'left', A: 'left', ArrowRight: 'right', d: 'right', D: 'right', ArrowUp: 'up', w: 'up', W: 'up', ArrowDown: 'down', s: 'down', S: 'down',
      j: 'punch', J: 'punch', k: 'kick', K: 'kick', i: 'grab', I: 'grab', l: 'sup', L: 'sup', ' ': 'sup' };
    window.addEventListener('keydown', (e) => {
      Sfx.resume();
      if (e.key === 'Escape' || e.key === 'p' || e.key === 'P') { this.back(); e.preventDefault(); return; }
      if (e.key === 'm' || e.key === 'M') { Sfx.toggleMusic(); return; }
      if (e.key === 'n' || e.key === 'N') { Sfx.toggleMute(); return; }
      if (this.screen === 'title' && (e.key === 'Enter' || e.key === ' ')) { this.goSelect(false); return; }
      if (this.screen === 'levels' && e.key === 'Enter') { this.startLevel(this.selLv); return; }
      if (this.screen === 'select') {
        if (e.key === 'Enter') this.startFight();
        else if (e.key === 't' || e.key === 'T') this.setStage((this.stage + 1) % 3);
        else if (e.key >= '1' && e.key <= '3') { this.level = +e.key - 1; this.refreshSelect(); }
        return;
      }
      if (this.screen === 'result' && e.key === 'Enter') { ($('nextBtn').style.display !== 'none' ? $('nextBtn') : $('rematchBtn')).click(); return; }
      const k = map[e.key];
      if (k && this.screen === 'fight') { if (!e.repeat || k in Engine.keys) Engine.press(k); e.preventDefault(); }
    });
    window.addEventListener('keyup', (e) => { const k = map[e.key]; if (k) Engine.release(k); });
    window.addEventListener('blur', () => { Engine.clearInput(); if (this.screen === 'fight') this.pause(true); });
    document.addEventListener('visibilitychange', () => { if (document.hidden && this.screen === 'fight') this.pause(true); });
    document.addEventListener('contextmenu', (e) => e.preventDefault());
    window.onNativeBack = () => this.back();
    window.onNativePause = () => { if (this.screen === 'fight') this.pause(true); Sfx.pause(); };
    window.onNativeResume = () => { if (this.screen !== 'pause') Sfx.unpause(); };
    window.initAndroidMatch = (p1, p2, stage) => {
      if (typeof p1 === 'number') this.sel1 = p1 % ROSTER.length;
      if (typeof p2 === 'number') this.sel2 = p2 % ROSTER.length;
      if (typeof stage === 'number') this.stage = stage % STAGE_NAMES.length;
      this.startFight();
    };
  }
};

window.addEventListener('load', () => {
  // the heading font must be ready before canvas textures use it
  const go = () => UI.init();
  if (document.fonts && document.fonts.load) document.fonts.load('40px Bangers').then(go, go); else go();
});
