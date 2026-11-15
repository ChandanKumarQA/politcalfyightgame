'use strict';
/* Tiny WebAudio sound kit: synthesized sfx + a looping dhol-style beat. No audio files. */
const Sfx = (function () {
  let ctx = null, master = null, mus = null, noiseBuf = null;
  let muted = false, musicOn = true, timer = null, nextT = 0, step = 0, started = false;
  const BPM = 138;
  const SA = [146.83, 155.56, 185.0, 196.0, 220.0, 233.08, 277.18, 293.66, 369.99];

  function init() {
    if (ctx) return;
    try {
      ctx = new (window.AudioContext || window.webkitAudioContext)();
      master = ctx.createGain(); master.gain.value = muted ? 0 : 0.9; master.connect(ctx.destination);
      mus = ctx.createGain(); mus.gain.value = 0.2; mus.connect(master);
      const len = ctx.sampleRate;
      noiseBuf = ctx.createBuffer(1, len, ctx.sampleRate);
      const d = noiseBuf.getChannelData(0);
      for (let i = 0; i < len; i++) d[i] = Math.random() * 2 - 1;
    } catch (e) { ctx = null; }
  }
  function resume() {
    init();
    if (ctx && ctx.state === 'suspended') ctx.resume();
    if (ctx && !started) { started = true; if (musicOn) startMusic(); }
  }
  function toneAt(t, f, dur, type, vol, slide, dest) {
    if (!ctx) return;
    const o = ctx.createOscillator(), g = ctx.createGain();
    o.type = type || 'sine';
    o.frequency.setValueAtTime(f, t);
    if (slide) o.frequency.exponentialRampToValueAtTime(Math.max(20, slide), t + dur);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol, t + 0.008);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    o.connect(g); g.connect(dest || master);
    o.start(t); o.stop(t + dur + 0.05);
  }
  function noiseAt(t, dur, vol, freq, type, dest, q) {
    if (!ctx) return;
    const s = ctx.createBufferSource(); s.buffer = noiseBuf;
    const fl = ctx.createBiquadFilter(); fl.type = type || 'lowpass'; fl.frequency.value = freq || 1000;
    if (q) fl.Q.value = q;
    const g = ctx.createGain();
    g.gain.setValueAtTime(vol, t);
    g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    s.connect(fl); fl.connect(g); g.connect(dest || master);
    s.start(t, Math.random() * 0.5); s.stop(t + dur + 0.05);
  }
  const now = () => (ctx ? ctx.currentTime : 0);

  // ---- music ----
  function sched() {
    if (!ctx) return;
    const spb = 60 / BPM / 2; // 8th note
    while (nextT < ctx.currentTime + 0.25) {
      const s = step % 16, t = nextT;
      if ('x..x..x.x..x..x.'[s] === 'x') { toneAt(t, 150, 0.18, 'sine', 0.9, 45, mus); }
      if (s % 4 === 2) { noiseAt(t, 0.07, 0.35, 3500, 'bandpass', mus, 2); }
      if (s % 2 === 1) { noiseAt(t, 0.03, 0.12, 7000, 'highpass', mus); }
      if (s % 4 === 0) {
        const bn = [0, 0, 3, 0, 0, 4, 3, 1][(step >> 2) % 8];
        toneAt(t, SA[bn] / 2, spb * 3.4, 'triangle', 0.55, 0, mus);
      }
      if ((s % 2 === 0 && Math.random() < 0.55) || s === 15) {
        const n = SA[[0, 2, 3, 4, 5, 6, 7, 8][Math.floor(Math.random() * 8)]];
        toneAt(t, n, spb * 1.6, 'sawtooth', 0.12, 0, mus);
      }
      nextT += spb; step++;
    }
  }
  function startMusic() {
    init(); if (!ctx || timer) return;
    nextT = ctx.currentTime + 0.1; step = 0;
    timer = setInterval(sched, 60);
  }
  function stopMusic() { if (timer) { clearInterval(timer); timer = null; } }

  return {
    resume,
    musicOn() { return musicOn; },
    toggleMusic() { musicOn = !musicOn; if (musicOn) startMusic(); else stopMusic(); return musicOn; },
    toggleMute() { muted = !muted; if (master) master.gain.value = muted ? 0 : 0.9; return muted; },
    isMuted() { return muted; },
    pause() { stopMusic(); },
    unpause() { if (musicOn && started) startMusic(); },
    hit(heavy) {
      if (!ctx) return; const t = now();
      noiseAt(t, heavy ? 0.22 : 0.12, heavy ? 1.0 : 0.7, heavy ? 900 : 1500, 'lowpass');
      toneAt(t, heavy ? 140 : 190, heavy ? 0.22 : 0.12, 'square', 0.5, 50);
    },
    block() { if (!ctx) return; const t = now(); noiseAt(t, 0.08, 0.5, 2500, 'bandpass'); toneAt(t, 520, 0.08, 'triangle', 0.3, 300); },
    whoosh() { if (!ctx) return; const t = now(); noiseAt(t, 0.16, 0.25, 1800, 'bandpass', null, 0.8); },
    jump() { if (!ctx) return; toneAt(now(), 260, 0.14, 'sine', 0.25, 520); },
    land() { if (!ctx) return; noiseAt(now(), 0.07, 0.35, 500, 'lowpass'); },
    sup() {
      if (!ctx) return; const t = now();
      toneAt(t, 200, 0.5, 'sawtooth', 0.35, 1200);
      toneAt(t + 0.05, 400, 0.5, 'square', 0.18, 1600);
      noiseAt(t, 0.5, 0.4, 4000, 'highpass');
    },
    superReady() { if (!ctx) return; const t = now(); toneAt(t, 660, 0.12, 'triangle', 0.35); toneAt(t + 0.1, 880, 0.18, 'triangle', 0.35); },
    boom() { if (!ctx) return; const t = now(); noiseAt(t, 0.5, 1.0, 700, 'lowpass'); toneAt(t, 120, 0.5, 'sine', 0.9, 30); },
    ko() {
      if (!ctx) return; const t = now();
      noiseAt(t, 0.7, 1.0, 600, 'lowpass'); toneAt(t, 200, 0.8, 'sawtooth', 0.5, 40);
      toneAt(t + 0.4, 90, 0.6, 'sine', 0.8, 30);
    },
    bell() { if (!ctx) return; const t = now(); toneAt(t, 880, 0.9, 'sine', 0.35); toneAt(t, 1320, 0.7, 'sine', 0.2); },
    ui() { if (!ctx) return; toneAt(now(), 720, 0.07, 'triangle', 0.25, 960); },
    win() { if (!ctx) return; const t = now(); [523, 659, 784, 1046].forEach((f, i) => toneAt(t + i * 0.13, f, 0.3, 'triangle', 0.35)); },
    lose() { if (!ctx) return; const t = now(); [392, 330, 262, 196].forEach((f, i) => toneAt(t + i * 0.18, f, 0.35, 'sawtooth', 0.22)); }
  };
})();
