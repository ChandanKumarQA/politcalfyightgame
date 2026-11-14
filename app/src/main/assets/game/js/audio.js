/**
 * NETA FIGHT – Procedural Web Audio Engine
 * Zero external files, 100% synthesized arcade sounds and dynamic Bhangra battle music!
 */
const SoundSystem = (function() {
  let ctx = null;
  let sfxEnabled = true;
  let musicEnabled = true;
  let musicTimer = null;
  let musicStep = 0;
  let masterGain = null;

  function init() {
    if (ctx) return;
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      ctx = new AudioCtx();
      masterGain = ctx.createGain();
      masterGain.gain.setValueAtTime(0.8, ctx.currentTime);
      masterGain.connect(ctx.destination);
    } catch (e) {
      console.warn("Web Audio not supported", e);
    }
  }

  function resume() {
    if (ctx && ctx.state === 'suspended') {
      ctx.resume();
    }
  }

  // --- Sound Effects Synthesizers ---
  function playPunch() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;
    
    // Air whoosh
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(320, t);
    osc.frequency.exponentialRampToValueAtTime(80, t + 0.12);
    gain.gain.setValueAtTime(0.7, t);
    gain.gain.exponentialRampToValueAtTime(0.01, t + 0.12);
    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(t);
    osc.stop(t + 0.13);

    // Impact thud
    const osc2 = ctx.createOscillator();
    const gain2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(140, t + 0.04);
    osc2.frequency.exponentialRampToValueAtTime(40, t + 0.15);
    gain2.gain.setValueAtTime(0.8, t + 0.04);
    gain2.gain.exponentialRampToValueAtTime(0.01, t + 0.16);
    osc2.connect(gain2);
    gain2.connect(masterGain);
    osc2.start(t + 0.04);
    osc2.stop(t + 0.17);
  }

  function playKick() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(220, t);
    osc.frequency.exponentialRampToValueAtTime(30, t + 0.22);
    gain.gain.setValueAtTime(0.9, t);
    gain.gain.exponentialRampToValueAtTime(0.01, t + 0.22);
    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(t);
    osc.stop(t + 0.23);
  }

  function playBlock() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;

    // Metallic shield ring
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'triangle';
    osc.frequency.setValueAtTime(880, t);
    osc.frequency.exponentialRampToValueAtTime(440, t + 0.18);
    gain.gain.setValueAtTime(0.6, t);
    gain.gain.exponentialRampToValueAtTime(0.01, t + 0.18);
    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(t);
    osc.stop(t + 0.19);
  }

  function playGrab() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;

    // Fast whoosh + heavy ground slam
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(180, t);
    osc.frequency.exponentialRampToValueAtTime(50, t + 0.3);
    gain.gain.setValueAtTime(0.7, t);
    gain.gain.exponentialRampToValueAtTime(0.01, t + 0.3);
    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(t);
    osc.stop(t + 0.31);
  }

  function playSuperCharge() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;

    // Rising energy oscillation
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sawtooth';
    osc.frequency.setValueAtTime(110, t);
    osc.frequency.exponentialRampToValueAtTime(880, t + 0.6);
    gain.gain.setValueAtTime(0.1, t);
    gain.gain.linearRampToValueAtTime(0.8, t + 0.5);
    gain.gain.exponentialRampToValueAtTime(0.01, t + 0.65);
    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(t);
    osc.stop(t + 0.66);
  }

  function playSuperBlast() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;

    // Deep explosive rumble
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(120, t);
    osc.frequency.exponentialRampToValueAtTime(20, t + 0.6);
    gain.gain.setValueAtTime(1.0, t);
    gain.gain.exponentialRampToValueAtTime(0.01, t + 0.6);
    osc.connect(gain);
    gain.connect(masterGain);
    osc.start(t);
    osc.stop(t + 0.61);

    // Noise burst simulation
    const node = ctx.createBufferSource();
    const buffer = ctx.createBuffer(1, ctx.sampleRate * 0.4, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < buffer.length; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.exp(-i / (ctx.sampleRate * 0.08));
    }
    node.buffer = buffer;
    const filter = ctx.createBiquadFilter();
    filter.type = 'lowpass';
    filter.frequency.setValueAtTime(1200, t);
    node.connect(filter);
    filter.connect(masterGain);
    node.start(t);
  }

  function playRoundBell() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;
    [1046.5, 1318.5, 1567.98].forEach((f, idx) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(f, t + idx * 0.1);
      gain.gain.setValueAtTime(0.6, t + idx * 0.1);
      gain.gain.exponentialRampToValueAtTime(0.01, t + idx * 0.1 + 0.4);
      osc.connect(gain);
      gain.connect(masterGain);
      osc.start(t + idx * 0.1);
      osc.stop(t + idx * 0.1 + 0.45);
    });
  }

  function playCheer() {
    if (!sfxEnabled || !ctx) return;
    resume();
    const t = ctx.currentTime;
    const node = ctx.createBufferSource();
    const buffer = ctx.createBuffer(1, ctx.sampleRate * 1.2, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < buffer.length; i++) {
      data[i] = (Math.random() * 2 - 1) * Math.sin((i / buffer.length) * Math.PI);
    }
    node.buffer = buffer;
    const filter = ctx.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(1100, t);
    filter.Q.setValueAtTime(2, t);
    const gain = ctx.createGain();
    gain.gain.setValueAtTime(0.4, t);
    node.connect(filter);
    filter.connect(gain);
    gain.connect(masterGain);
    node.start(t);
  }

  // --- Dynamic Bhangra-Arcade Battle Music Loop ---
  // A punchy 130 BPM 8-bit desi groove
  const bassNotes = [110, 110, 130.8, 146.8, 164.8, 146.8, 130.8, 98];
  const leadNotes = [440, 523.25, 587.33, 659.25, 587.33, 523.25, 440, 392];

  function playBeat() {
    if (!musicEnabled || !ctx) return;
    const t = ctx.currentTime;
    const step = musicStep % 16;
    musicStep++;

    // Dhol Kick on beats 0, 4, 8, 12, with offbeat syncopations at 3, 7, 11, 14
    if (step % 4 === 0 || step === 3 || step === 11 || step === 14) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(140, t);
      osc.frequency.exponentialRampToValueAtTime(45, t + 0.1);
      gain.gain.setValueAtTime(0.4, t);
      gain.gain.exponentialRampToValueAtTime(0.01, t + 0.1);
      osc.connect(gain);
      gain.connect(masterGain);
      osc.start(t);
      osc.stop(t + 0.11);
    }

    // Snare / Rim snap on beats 4 and 12
    if (step === 4 || step === 12) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(260, t);
      gain.gain.setValueAtTime(0.25, t);
      gain.gain.exponentialRampToValueAtTime(0.01, t + 0.08);
      osc.connect(gain);
      gain.connect(masterGain);
      osc.start(t);
      osc.stop(t + 0.09);
    }

    // Bass Synth
    if (step % 2 === 0) {
      const noteIdx = Math.floor(step / 2) % bassNotes.length;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(bassNotes[noteIdx], t);
      gain.gain.setValueAtTime(0.18, t);
      gain.gain.exponentialRampToValueAtTime(0.01, t + 0.18);
      osc.connect(gain);
      gain.connect(masterGain);
      osc.start(t);
      osc.stop(t + 0.19);
    }

    // Lead melody fill
    if (step % 4 === 2) {
      const leadIdx = Math.floor(step / 2) % leadNotes.length;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'square';
      osc.frequency.setValueAtTime(leadNotes[leadIdx], t);
      gain.gain.setValueAtTime(0.1, t);
      gain.gain.exponentialRampToValueAtTime(0.01, t + 0.12);
      osc.connect(gain);
      gain.connect(masterGain);
      osc.start(t);
      osc.stop(t + 0.13);
    }
  }

  function startMusic() {
    init();
    resume();
    if (musicTimer) clearInterval(musicTimer);
    musicStep = 0;
    // 130 BPM = 16th note every ~115ms
    musicTimer = setInterval(playBeat, 115);
  }

  function stopMusic() {
    if (musicTimer) {
      clearInterval(musicTimer);
      musicTimer = null;
    }
  }

  return {
    init,
    resume,
    playPunch,
    playKick,
    playBlock,
    playGrab,
    playSuperCharge,
    playSuperBlast,
    playRoundBell,
    playCheer,
    startMusic,
    stopMusic,
    setSfx(enabled) { sfxEnabled = enabled; },
    setMusic(enabled) {
      musicEnabled = enabled;
      if (!enabled) stopMusic();
      else startMusic();
    },
    getSfx() { return sfxEnabled; },
    getMusic() { return musicEnabled; }
  };
})();
