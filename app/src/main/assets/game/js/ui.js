/**
 * NETA FIGHT – UI Manager, Controls & Input System
 * Connects Three.js, HUD, Touch Controls, Newsdesk & Android Native Bridge
 */
window.addEventListener("DOMContentLoaded", () => {
  // --- Three.js Setup ---
  const canvas = document.getElementById("game-canvas");
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.shadowMap.enabled = true;

  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x0a0502);

  const camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 100);
  camera.position.set(0, 3.2, 11);

  // Initialize Core Systems
  SoundSystem.init();
  const engine = new FightEngine(scene, camera);

  // UI State
  let selectedP1 = 0;
  let selectedP2 = 1;
  let selectedStage = 0;
  let selectedDiff = "normal";
  let selectingPlayer = 1; // 1 or 2
  let currentScreen = "main-menu";

  // --- Resize Handler ---
  function onWindowResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  }
  window.addEventListener("resize", onWindowResize);

  // --- Screen Navigation ---
  function showScreen(screenId) {
    currentScreen = screenId;
    document.querySelectorAll(".screen-layer").forEach(el => el.classList.add("hidden"));
    const target = document.getElementById(`screen-${screenId}`);
    if (target) target.classList.remove("hidden");

    if (screenId === "fight") {
      document.getElementById("fight-hud").classList.remove("hidden");
    } else {
      document.getElementById("fight-hud").classList.add("hidden");
      SoundSystem.stopMusic();
    }
  }

  // --- Announcements & Combo Floating Text ---
  engine.onAnnouncement = (text) => {
    const annText = document.getElementById("announcement-text");
    if (!annText) return;
    annText.textContent = text;
    annText.classList.add("show");
    setTimeout(() => {
      annText.classList.remove("show");
    }, 1100);
  };

  engine.onComboPopup = (text, worldX, worldY) => {
    const layer = document.getElementById("combo-layer");
    if (!layer) return;

    // Convert 3D world coordinates to screen pixels
    const pos = new THREE.Vector3(worldX, worldY, 0);
    pos.project(camera);
    const screenX = (pos.x * 0.5 + 0.5) * window.innerWidth;
    const screenY = (-(pos.y * 0.5) + 0.5) * window.innerHeight;

    const el = document.createElement("div");
    el.className = "floating-combo";
    el.textContent = text;
    el.style.left = `${screenX}px`;
    el.style.top = `${screenY}px`;
    layer.appendChild(el);

    setTimeout(() => el.remove(), 800);
  };

  // --- HUD Update Binding ---
  engine.onHudUpdate = (data) => {
    // Player 1 HP
    const p1HpFill = document.getElementById("p1-hp-fill");
    const p1HpTrail = document.getElementById("p1-hp-trail");
    if (p1HpFill && p1HpTrail) {
      p1HpFill.style.width = `${Math.max(0, data.p1Hp)}%`;
      setTimeout(() => { p1HpTrail.style.width = `${Math.max(0, data.p1Hp)}%`; }, 200);
      if (data.p1Hp < 25) p1HpFill.classList.add("hp-low");
      else p1HpFill.classList.remove("hp-low");
    }

    // Player 2 HP
    const p2HpFill = document.getElementById("p2-hp-fill");
    const p2HpTrail = document.getElementById("p2-hp-trail");
    if (p2HpFill && p2HpTrail) {
      p2HpFill.style.width = `${Math.max(0, data.p2Hp)}%`;
      setTimeout(() => { p2HpTrail.style.width = `${Math.max(0, data.p2Hp)}%`; }, 200);
      if (data.p2Hp < 25) p2HpFill.classList.add("hp-low");
      else p2HpFill.classList.remove("hp-low");
    }

    // Shakti Meters
    const p1ShaktiFill = document.getElementById("p1-shakti-fill");
    const p1ShaktiReady = document.getElementById("p1-shakti-ready");
    if (p1ShaktiFill && p1ShaktiReady) {
      p1ShaktiFill.style.width = `${data.p1Shakti}%`;
      if (data.p1Shakti >= 50) p1ShaktiReady.classList.add("ready");
      else p1ShaktiReady.classList.remove("ready");
    }

    const p2ShaktiFill = document.getElementById("p2-shakti-fill");
    const p2ShaktiReady = document.getElementById("p2-shakti-ready");
    if (p2ShaktiFill && p2ShaktiReady) {
      p2ShaktiFill.style.width = `${data.p2Shakti}%`;
      if (data.p2Shakti >= 50) p2ShaktiReady.classList.add("ready");
      else p2ShaktiReady.classList.remove("ready");
    }

    // Win Dots
    document.getElementById("p1-win-1")?.classList.toggle("won", data.p1Wins >= 1);
    document.getElementById("p1-win-2")?.classList.toggle("won", data.p1Wins >= 2);
    document.getElementById("p2-win-1")?.classList.toggle("won", data.p2Wins >= 1);
    document.getElementById("p2-win-2")?.classList.toggle("won", data.p2Wins >= 2);

    // Timer & Round
    const timerEl = document.getElementById("fight-timer");
    if (timerEl) {
      timerEl.textContent = data.timer;
      timerEl.classList.toggle("warning", data.timer <= 10);
    }
    const roundEl = document.getElementById("round-banner");
    if (roundEl) roundEl.textContent = `ROUND ${data.round}`;
  };

  // --- Match End & Result Screen ---
  engine.onMatchEnd = (winner, p1Data, p2Data) => {
    const isWin = winner === 1;
    const champ = isWin ? p1Data : p2Data;
    const runner = isWin ? p2Data : p1Data;

    document.getElementById("result-headline").textContent =
      `${champ.name} SCORES LANDSLIDE VICTORY OVER ${runner.name}!`;
    document.getElementById("result-stamp").textContent = isWin ? "YOU WIN! 🏆" : "DEFEAT! 💀";
    document.getElementById("result-story").textContent =
      `In an electrifying battle witnessed by thousands in the arena, ${champ.name} (${champ.party}) dominated the final round using signature special move '${champ.superName}'.`;
    document.getElementById("result-quote").textContent = `"${champ.quote}"`;

    showScreen("result");
    triggerNativeVibrate("heavy");
  };

  // --- Populate Roster Grid in Character Select ---
  function renderRoster() {
    const grid = document.getElementById("roster-grid");
    if (!grid) return;
    grid.innerHTML = "";

    CharacterRoster.forEach((char, idx) => {
      const tile = document.createElement("div");
      tile.className = "roster-tile";
      if (idx === selectedP1) tile.classList.add("selected-p1");
      if (idx === selectedP2) tile.classList.add("selected-p2");

      tile.innerHTML = `
        <span class="tile-icon">${char.avatar}</span>
        <span class="tile-name">${char.name}</span>
        ${idx === selectedP1 ? '<span class="tile-badge p1">1P</span>' : ''}
        ${idx === selectedP2 ? '<span class="tile-badge p2">2P</span>' : ''}
      `;

      tile.addEventListener("click", () => {
        SoundSystem.playPunch();
        if (selectingPlayer === 1) {
          selectedP1 = idx;
          updatePlate(1, char);
          selectingPlayer = 2;
          document.getElementById("select-step-prompt").textContent = "PICK CPU OPPONENT";
        } else {
          selectedP2 = idx;
          updatePlate(2, char);
          selectingPlayer = 1;
          document.getElementById("select-step-prompt").textContent = "READY FOR BATTLE!";
        }
        renderRoster();
      });

      grid.appendChild(tile);
    });

    updatePlate(1, CharacterRoster[selectedP1]);
    updatePlate(2, CharacterRoster[selectedP2]);
  }

  function updatePlate(pNum, char) {
    document.getElementById(`plate-avatar-p${pNum}`).textContent = char.avatar;
    document.getElementById(`plate-name-p${pNum}`).textContent = char.name;
    document.getElementById(`plate-party-p${pNum}`).textContent = char.party;
    document.getElementById(`plate-super-p${pNum}`).textContent = `SUPER: ${char.superName}`;
    document.getElementById(`plate-desc-p${pNum}`).textContent = char.superDesc;
    document.getElementById(`stat-p${pNum}-pwr`).style.width = `${char.pwr}%`;
    document.getElementById(`stat-p${pNum}-spd`).style.width = `${char.spd}%`;
    document.getElementById(`stat-p${pNum}-def`).style.width = `${char.def}%`;

    // Also update HUD portraits
    const hudAvatar = document.getElementById(`hud-p${pNum}-avatar`);
    const hudName = document.getElementById(`hud-p${pNum}-name`);
    if (hudAvatar) hudAvatar.textContent = char.avatar;
    if (hudName) hudName.textContent = char.name;
  }

  // --- Main Menu Buttons ---
  document.getElementById("btn-menu-play")?.addEventListener("click", () => {
    SoundSystem.resume();
    renderRoster();
    showScreen("character-select");
  });

  document.getElementById("btn-menu-friend")?.addEventListener("click", () => {
    SoundSystem.resume();
    showScreen("multiplayer");
  });

  document.getElementById("btn-menu-how")?.addEventListener("click", () => {
    showScreen("how-to-play");
  });

  document.getElementById("btn-menu-settings")?.addEventListener("click", () => {
    showScreen("settings");
  });

  document.getElementById("btn-menu-exit")?.addEventListener("click", () => {
    if (window.Android && window.Android.exitApp) {
      window.Android.exitApp();
    } else {
      alert("Exiting game...");
    }
  });

  // --- Back Buttons ---
  document.getElementById("btn-select-back")?.addEventListener("click", () => showScreen("main-menu"));
  document.getElementById("btn-multiplayer-back")?.addEventListener("click", () => showScreen("main-menu"));
  document.getElementById("btn-how-back")?.addEventListener("click", () => showScreen("main-menu"));
  document.getElementById("btn-settings-back")?.addEventListener("click", () => showScreen("main-menu"));

  // --- Stage & Difficulty Pills ---
  document.querySelectorAll("#stage-pills .pill").forEach(p => {
    p.addEventListener("click", (e) => {
      document.querySelectorAll("#stage-pills .pill").forEach(x => x.classList.remove("active"));
      p.classList.add("active");
      selectedStage = parseInt(p.getAttribute("data-stage") || "0");
      SoundSystem.playPunch();
    });
  });

  document.querySelectorAll("#diff-pills .pill").forEach(p => {
    p.addEventListener("click", (e) => {
      document.querySelectorAll("#diff-pills .pill").forEach(x => x.classList.remove("active"));
      p.classList.add("active");
      selectedDiff = p.getAttribute("data-diff") || "normal";
      SoundSystem.playPunch();
    });
  });

  // --- Start Match Button ---
  document.getElementById("btn-start-match")?.addEventListener("click", () => {
    showScreen("fight");
    engine.initMatch(selectedP1, selectedP2, selectedStage, "cpu", selectedDiff);
  });

  // --- Local 2-Player Button ---
  document.getElementById("btn-start-local-2p")?.addEventListener("click", () => {
    renderRoster();
    showScreen("character-select");
  });

  // --- Pause & Resume ---
  document.getElementById("btn-pause")?.addEventListener("click", () => {
    engine.paused = true;
    showScreen("pause");
  });

  document.getElementById("btn-pause-resume")?.addEventListener("click", () => {
    engine.paused = false;
    showScreen("fight");
  });

  document.getElementById("btn-pause-restart")?.addEventListener("click", () => {
    engine.paused = false;
    showScreen("fight");
    engine.startRound();
  });

  document.getElementById("btn-pause-quit")?.addEventListener("click", () => {
    engine.paused = false;
    engine.clearMatch();
    showScreen("main-menu");
  });

  // --- Result Rematch & Menu ---
  document.getElementById("btn-result-rematch")?.addEventListener("click", () => {
    showScreen("fight");
    engine.initMatch(selectedP1, selectedP2, selectedStage, "cpu", selectedDiff);
  });

  document.getElementById("btn-result-roster")?.addEventListener("click", () => {
    renderRoster();
    showScreen("character-select");
  });

  document.getElementById("btn-result-menu")?.addEventListener("click", () => {
    showScreen("main-menu");
  });

  // --- Settings Toggles ---
  document.getElementById("btn-toggle-sfx")?.addEventListener("click", (e) => {
    const active = !SoundSystem.getSfx();
    SoundSystem.setSfx(active);
    e.target.textContent = active ? "ON" : "OFF";
    e.target.classList.toggle("active", active);
  });

  document.getElementById("btn-toggle-music")?.addEventListener("click", (e) => {
    const active = !SoundSystem.getMusic();
    SoundSystem.setMusic(active);
    e.target.textContent = active ? "ON" : "OFF";
    e.target.classList.toggle("active", active);
  });

  // --- Parody News Ticker Dynamic Updates ---
  const headlines = [
    "PARLIAMENT ADJOURNED: Heavy dropkick reported in well of the house · EXIT POLLS PREDICT BOTH FIGHTERS WILL LOSE DEPOSIT ·",
    "SPECIAL REPORT: Opposition demands JPC probe into unblockable grab attack · WHOLESALE INFLATION IN CHAI PRICES REPORTED ·",
    "BREAKING: Flying dropkick declared 100% locally manufactured · RING-SIDE COW COMPLAINS ABOUT LACK OF FODDER ·",
    "ASTROLOGER CLAIMS: Retrograde Saturn responsible for missed low sweeps · SUPREME COURT ASKS FOR 15-MINUTE TEA BREAK ·"
  ];
  let headlineIdx = 0;
  setInterval(() => {
    headlineIdx = (headlineIdx + 1) % headlines.length;
    const ticker = document.getElementById("ticker-text");
    if (ticker) ticker.innerHTML = `<span>${headlines[headlineIdx]}</span>`;
  }, 14000);

  // --- Mobile Touch Controls ---
  const touchActions = document.getElementById("touch-actions");
  if (touchActions) {
    touchActions.addEventListener("touchstart", (e) => {
      const btn = e.target.closest(".action-btn");
      if (!btn || !engine.p1) return;
      e.preventDefault();
      const act = btn.getAttribute("data-act");
      engine.p1.triggerAction(act.toUpperCase());
      triggerNativeVibrate("light");
    }, { passive: false });
  }

  // Virtual Joystick
  const joystick = document.getElementById("touch-joystick");
  const knob = document.getElementById("joystick-knob");
  let joystickTouchId = null;
  let joyOriginX = 0;
  let joyOriginY = 0;

  if (joystick && knob) {
    joystick.addEventListener("touchstart", (e) => {
      e.preventDefault();
      const touch = e.changedTouches[0];
      joystickTouchId = touch.identifier;
      const rect = joystick.getBoundingClientRect();
      joyOriginX = rect.left + rect.width / 2;
      joyOriginY = rect.top + rect.height / 2;
    }, { passive: false });

    joystick.addEventListener("touchmove", (e) => {
      e.preventDefault();
      for (let i = 0; i < e.changedTouches.length; i++) {
        const touch = e.changedTouches[i];
        if (touch.identifier === joystickTouchId && engine.p1) {
          const dx = touch.clientX - joyOriginX;
          const dy = touch.clientY - joyOriginY;
          const dist = Math.min(40, Math.hypot(dx, dy));
          const angle = Math.atan2(dy, dx);
          const kx = Math.cos(angle) * dist;
          const ky = Math.sin(angle) * dist;
          knob.style.transform = `translate(${kx}px, ${ky}px)`;

          if (dx < -15) engine.p1.triggerAction("LEFT");
          else if (dx > 15) engine.p1.triggerAction("RIGHT");

          if (dy < -20) engine.p1.triggerAction("JUMP");
          else if (dy > 20) engine.p1.triggerAction("CROUCH");
        }
      }
    }, { passive: false });

    const resetJoy = (e) => {
      knob.style.transform = `translate(0px, 0px)`;
      joystickTouchId = null;
    };
    joystick.addEventListener("touchend", resetJoy);
    joystick.addEventListener("touchcancel", resetJoy);
  }

  // --- Keyboard Event Controls (Full PC & 2P Testing) ---
  const keys = {};
  window.addEventListener("keydown", (e) => {
    SoundSystem.resume();
    const k = e.key.toLowerCase();
    keys[k] = true;

    // Esc for Pause
    if (k === "escape" && currentScreen === "fight") {
      engine.paused = !engine.paused;
      showScreen(engine.paused ? "pause" : "fight");
      return;
    }

    if (!engine.p1) return;

    // Player 1 Keys: WASD + J (punch), K (kick), I (grab), L/Space (super)
    if (k === "a") engine.p1.triggerAction("LEFT");
    if (k === "d") engine.p1.triggerAction("RIGHT");
    if (k === "w") engine.p1.triggerAction("JUMP");
    if (k === "s") engine.p1.triggerAction("CROUCH");
    if (k === "j") engine.p1.triggerAction("PUNCH");
    if (k === "k") engine.p1.triggerAction("KICK");
    if (k === "i") engine.p1.triggerAction("GRAB");
    if (k === "l" || k === " ") engine.p1.triggerAction("SUPER");

    // Player 2 Keys (Local 2P): Arrow Keys + Numpad 1,2,4,5
    if (engine.p2 && engine.gameMode === "local_2p") {
      if (k === "arrowleft") engine.p2.triggerAction("LEFT");
      if (k === "arrowright") engine.p2.triggerAction("RIGHT");
      if (k === "arrowup") engine.p2.triggerAction("JUMP");
      if (k === "arrowdown") engine.p2.triggerAction("CROUCH");
      if (k === "1" || k === "u") engine.p2.triggerAction("PUNCH");
      if (k === "2" || k === "o") engine.p2.triggerAction("KICK");
      if (k === "4" || k === "p") engine.p2.triggerAction("GRAB");
      if (k === "5" || k === "enter") engine.p2.triggerAction("SUPER");
    }
  });

  window.addEventListener("keyup", (e) => {
    keys[e.key.toLowerCase()] = false;
  });

  // Native Android Vibration Bridge
  function triggerNativeVibrate(type = "light") {
    if (window.Android && window.Android.vibrate) {
      window.Android.vibrate(type);
    } else if (navigator.vibrate) {
      navigator.vibrate(type === "heavy" ? 120 : 35);
    }
  }

  // --- Main Animation Render Loop (60 FPS) ---
  let lastTime = performance.now();
  function animate() {
    requestAnimationFrame(animate);
    const now = performance.now();
    const dt = Math.min(0.1, (now - lastTime) / 1000);
    lastTime = now;

    // Continuous keyboard walking support
    if (currentScreen === "fight" && engine.matchActive && !engine.paused) {
      if (keys["a"]) engine.p1?.triggerAction("LEFT");
      if (keys["d"]) engine.p1?.triggerAction("RIGHT");
      if (engine.p2 && engine.gameMode === "local_2p") {
        if (keys["arrowleft"]) engine.p2.triggerAction("LEFT");
        if (keys["arrowright"]) engine.p2.triggerAction("RIGHT");
      }
    }

    engine.update(dt);
    renderer.render(scene, camera);
  }
  animate();

  // --- Native Android Match Starter Bridge ---
  window.initAndroidMatch = function(p1, p2, stage, mode, diff) {
    SoundSystem.resume();
    if (typeof p1 === "number" && p1 >= 0) selectedP1 = p1;
    if (typeof p2 === "number" && p2 >= 0) selectedP2 = p2;
    else selectedP2 = (selectedP1 + 1) % CharacterRoster.length;
    if (typeof stage === "number" && stage >= 0) selectedStage = stage;
    if (diff) selectedDiff = diff;
    showScreen("fight");
    engine.initMatch(selectedP1, selectedP2, selectedStage, mode || "cpu", selectedDiff);
  };

  try {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("autostart") || urlParams.has("p1")) {
      const p1 = parseInt(urlParams.get("p1") || "0");
      const p2 = parseInt(urlParams.get("p2") || "1");
      const stg = parseInt(urlParams.get("stage") || "0");
      const md = urlParams.get("mode") || "cpu";
      window.initAndroidMatch(p1, p2, stg, md);
    }
  } catch (e) {
    console.error(e);
  }

  // Show Main Menu on Start
  showScreen("main-menu");
  document.body.classList.remove("loading-state");
});
