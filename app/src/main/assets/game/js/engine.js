/**
 * NETA FIGHT – Core 3D Fighting Engine
 * State machines, collision detection, combos, CPU AI & match director
 */
class FightEngine {
  constructor(scene, camera) {
    this.scene = scene;
    this.camera = camera;
    this.fighters = [];
    this.p1 = null;
    this.p2 = null;
    this.arenaManager = new ArenaManager(scene);

    // Match State
    this.matchActive = false;
    this.paused = false;
    this.round = 1;
    this.p1Wins = 0;
    this.p2Wins = 0;
    this.timer = 60;
    this.timerInterval = null;
    this.gameMode = "cpu"; // "cpu", "local_2p", "online"
    this.difficulty = "normal";

    // Visual Effects & Props
    this.activeProps = [];
    this.screenShake = 0;
    this.slowMo = 1.0;
    this.time = 0;

    // Callbacks
    this.onHudUpdate = null;
    this.onAnnouncement = null;
    this.onComboPopup = null;
    this.onMatchEnd = null;
  }

  initMatch(p1CharId, p2CharId, stageIdx = 0, mode = "cpu", difficulty = "normal") {
    this.clearMatch();
    this.gameMode = mode;
    this.difficulty = difficulty;
    this.arenaManager.loadArena(stageIdx);

    // Create Fighters
    const p1Data = CharacterRoster[p1CharId] || CharacterRoster[0];
    const p2Data = CharacterRoster[p2CharId] || CharacterRoster[1];

    this.p1 = new FighterEntity(p1Data, false, -4.5, this);
    this.p2 = new FighterEntity(p2Data, true, 4.5, this);
    this.p1.opponent = this.p2;
    this.p2.opponent = this.p1;

    this.fighters = [this.p1, this.p2];
    this.scene.add(this.p1.model.group);
    this.scene.add(this.p2.model.group);

    this.round = 1;
    this.p1Wins = 0;
    this.p2Wins = 0;
    this.startRound();
  }

  startRound() {
    this.timer = 60;
    this.p1.resetRound(-4.5, 1);
    this.p2.resetRound(4.5, -1);
    this.matchActive = false;
    this.clearProps();

    if (this.onAnnouncement) {
      this.onAnnouncement(`ROUND ${this.round}`);
    }
    SoundSystem.playRoundBell();

    setTimeout(() => {
      if (this.onAnnouncement) this.onAnnouncement("FIGHT!");
      this.matchActive = true;
      this.startTimer();
      SoundSystem.startMusic();
    }, 1200);

    this.updateHud();
  }

  startTimer() {
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => {
      if (!this.matchActive || this.paused) return;
      this.timer--;
      this.updateHud();
      if (this.timer <= 0) {
        this.endRound("time");
      }
    }, 1000);
  }

  endRound(reason = "ko") {
    this.matchActive = false;
    if (this.timerInterval) clearInterval(this.timerInterval);
    SoundSystem.stopMusic();

    let roundWinner = null;
    if (this.p1.hp > this.p2.hp) {
      roundWinner = 1;
      this.p1Wins++;
      this.p1.state = "VICTORY";
      this.p2.state = "DEFEAT";
    } else if (this.p2.hp > this.p1.hp) {
      roundWinner = 2;
      this.p2Wins++;
      this.p2.state = "VICTORY";
      this.p1.state = "DEFEAT";
    } else {
      // Draw - both get a point
      this.p1Wins++;
      this.p2Wins++;
    }

    const ann = reason === "time" ? "TIME UP!" : "K.O.!";
    if (this.onAnnouncement) this.onAnnouncement(ann);
    SoundSystem.playCheer();

    setTimeout(() => {
      // Check if match decided (First to 2 wins)
      if (this.p1Wins >= 2 || this.p2Wins >= 2) {
        const matchWinner = this.p1Wins >= 2 ? 1 : 2;
        if (this.onMatchEnd) {
          this.onMatchEnd(matchWinner, this.p1.charData, this.p2.charData);
        }
      } else {
        // Next round
        this.round++;
        this.startRound();
      }
    }, 2500);

    this.updateHud();
  }

  update(dt) {
    if (this.paused) return;
    const effectiveDt = dt * this.slowMo;
    this.time += effectiveDt;

    // Update Arena (crowd animation)
    this.arenaManager.update(this.time);

    // Update Fighters
    this.fighters.forEach(f => f.update(effectiveDt, this.time));

    // Update Active Super Props
    this.updateProps(effectiveDt);

    // Check Collisions
    if (this.matchActive) {
      this.checkCombatCollisions();
    }

    // Dynamic Camera Tracking & Shake
    this.updateCamera(effectiveDt);
  }

  checkCombatCollisions() {
    this.fighters.forEach(attacker => {
      if (!attacker.isAttacking || attacker.hitApplied) return;
      const defender = attacker.opponent;
      if (!defender || defender.hp <= 0) return;

      const hitboxes = attacker.getActiveHitboxes();
      const hurtboxes = defender.getHurtboxes();

      for (const hb of hitboxes) {
        for (const hurt of hurtboxes) {
          const dx = hb.x - hurt.x;
          const dy = hb.y - hurt.y;
          const dz = hb.z - hurt.z;
          const distSq = dx * dx + dy * dy + dz * dz;
          const radiusSum = hb.radius + hurt.radius;

          if (distSq < radiusSum * radiusSum) {
            this.handleHit(attacker, defender, hb);
            return;
          }
        }
      }
    });
  }

  handleHit(attacker, defender, hitbox) {
    attacker.hitApplied = true;

    // Grab is unblockable!
    const isGrab = hitbox.type === "grab";
    const isBlocking = defender.state === "BLOCK" && !isGrab;

    if (isBlocking) {
      // Blocked attack
      SoundSystem.playBlock();
      const chipDmg = hitbox.damage * 0.15;
      defender.hp = Math.max(0, defender.hp - chipDmg);
      defender.shakti = Math.min(100, defender.shakti + 12);
      attacker.shakti = Math.min(100, attacker.shakti + 6);
      this.triggerFlash(0.2);
      this.triggerShake(0.12, 0.15);
      if (this.onComboPopup) {
        this.onComboPopup("BLOCKED!", defender.x, defender.y + 2);
      }
    } else {
      // Clean hit
      if (hitbox.isKick) SoundSystem.playKick();
      else if (isGrab) SoundSystem.playGrab();
      else SoundSystem.playPunch();

      defender.hp = Math.max(0, defender.hp - hitbox.damage);
      attacker.shakti = Math.min(100, attacker.shakti + hitbox.damage * 0.7);
      defender.shakti = Math.min(100, defender.shakti + hitbox.damage * 0.5);

      this.triggerFlash(hitbox.isSuper ? 0.6 : 0.25);
      this.triggerShake(hitbox.isSuper ? 0.35 : 0.2, hitbox.damage * 0.02);

      // Apply Knockback & Hit/Knockdown state
      const dir = attacker.facing;
      defender.vx = dir * hitbox.knockback;

      if (hitbox.knockdown || defender.hp <= 0) {
        defender.state = "KNOCKDOWN";
        defender.stateTimer = 0;
        defender.stateDuration = 0.9;
        defender.vy = 4.5;
        if (this.onComboPopup) {
          this.onComboPopup(hitbox.isSuper ? "SUPER HIT!" : "KNOCKDOWN!", defender.x, defender.y + 2.4);
        }
      } else {
        defender.state = "HIT";
        defender.stateTimer = 0;
        defender.stateDuration = 0.35;
        attacker.comboCount++;
        if (attacker.comboCount >= 2 && this.onComboPopup) {
          this.onComboPopup(`COMBO x${attacker.comboCount}!`, defender.x, defender.y + 2.2);
        }
      }

      if (defender.hp <= 0) {
        this.endRound("ko");
      }
    }

    this.updateHud();
  }

  // Super Attacks Props Generator
  spawnSuperProp(fighter) {
    const p = new SuperProp(fighter, this);
    this.activeProps.push(p);
    this.scene.add(p.group);
    SoundSystem.playSuperCharge();
    this.triggerFlash(0.4);
  }

  updateProps(dt) {
    for (let i = this.activeProps.length - 1; i >= 0; i--) {
      const prop = this.activeProps[i];
      prop.update(dt);
      if (prop.isDead) {
        this.scene.remove(prop.group);
        this.activeProps.splice(i, 1);
      }
    }
  }

  clearProps() {
    this.activeProps.forEach(p => this.scene.remove(p.group));
    this.activeProps = [];
  }

  updateCamera(dt) {
    if (!this.p1 || !this.p2) return;
    const midX = (this.p1.x + this.p2.x) / 2;
    const dist = Math.abs(this.p1.x - this.p2.x);

    // Target Camera X & Z
    const targetX = midX;
    const targetZ = Math.max(9, Math.min(17, 8 + dist * 0.75));

    // Smooth Lerp
    this.camera.position.x += (targetX - this.camera.position.x) * 6 * dt;
    this.camera.position.z += (targetZ - this.camera.position.z) * 6 * dt;
    this.camera.position.y = 3.2;

    // Screen Shake
    if (this.screenShake > 0) {
      this.screenShake -= dt;
      this.camera.position.x += (Math.random() - 0.5) * this.shakeMagnitude;
      this.camera.position.y += (Math.random() - 0.5) * this.shakeMagnitude;
    }

    this.camera.lookAt(midX, 2.0, 0);
  }

  triggerShake(duration = 0.2, magnitude = 0.25) {
    this.screenShake = duration;
    this.shakeMagnitude = magnitude;
  }

  triggerFlash(intensity = 0.3) {
    const flashEl = document.getElementById("vfx-flash");
    if (flashEl) {
      flashEl.style.opacity = intensity;
      setTimeout(() => { flashEl.style.opacity = 0; }, 80);
    }
  }

  updateHud() {
    if (this.onHudUpdate && this.p1 && this.p2) {
      this.onHudUpdate({
        p1Hp: this.p1.hp,
        p2Hp: this.p2.hp,
        p1Shakti: this.p1.shakti,
        p2Shakti: this.p2.shakti,
        p1Wins: this.p1Wins,
        p2Wins: this.p2Wins,
        timer: this.timer,
        round: this.round
      });
    }
  }

  clearMatch() {
    this.matchActive = false;
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.fighters.forEach(f => this.scene.remove(f.model.group));
    this.fighters = [];
    this.clearProps();
  }
}

/**
 * Fighter Entity Class
 */
class FighterEntity {
  constructor(charData, isP2, startX, engine) {
    this.charData = charData;
    this.isP2 = isP2;
    this.engine = engine;
    this.model = new FighterModel(charData, isP2);

    this.x = startX;
    this.y = 0;
    this.vx = 0;
    this.vy = 0;
    this.facing = isP2 ? -1 : 1;
    this.onGround = true;

    this.hp = 100;
    this.shakti = 0;
    this.state = "IDLE";
    this.stateTimer = 0;
    this.stateDuration = 0;
    this.hitApplied = false;
    this.comboCount = 0;
    this.comboBuffer = [];
    this.opponent = null;

    // AI Planning
    this.aiTimer = 0;
    this.aiPlan = "idle";
  }

  resetRound(x, facing) {
    this.x = x;
    this.y = 0;
    this.vx = 0;
    this.vy = 0;
    this.facing = facing;
    this.onGround = true;
    this.hp = 100;
    this.state = "IDLE";
    this.stateTimer = 0;
    this.stateDuration = 0;
    this.hitApplied = false;
    this.comboCount = 0;
  }

  get isAttacking() {
    return ["PUNCH_1", "PUNCH_2", "KICK_1", "KICK_2", "DROPKICK", "SWEEP", "GRAB", "SUPER"].includes(this.state);
  }

  update(dt, time) {
    this.stateTimer += dt;

    // CPU AI Decision Loop
    if (this.isP2 && this.engine.gameMode === "cpu" && this.engine.matchActive) {
      this.updateAI(dt);
    }

    // State Timeout & Transitions
    if (this.stateDuration > 0 && this.stateTimer >= this.stateDuration) {
      if (this.state === "KNOCKDOWN") {
        this.state = "GETUP";
        this.stateTimer = 0;
        this.stateDuration = 0.45;
      } else {
        this.state = "IDLE";
        this.stateDuration = 0;
        this.hitApplied = false;
        this.comboCount = 0;
      }
    }

    // Physics Movement & Gravity
    this.x += this.vx * dt;
    this.y += this.vy * dt;

    if (!this.onGround) {
      this.vy -= 18 * dt; // Gravity
      if (this.y <= 0) {
        this.y = 0;
        this.vy = 0;
        this.onGround = true;
        if (this.state === "JUMP" || this.state === "DROPKICK") {
          this.state = "IDLE";
          this.stateDuration = 0;
        }
      }
    }

    // Drag friction on ground
    if (this.onGround) {
      this.vx *= 0.82;
    }

    // Auto transition walking to IDLE when movement stops
    if (this.onGround && !this.isAttacking && this.state !== "BLOCK" && this.state !== "CROUCH" && this.state !== "KNOCKDOWN" && this.state !== "GETUP") {
      if (Math.abs(this.vx) < 0.3) {
        this.state = "IDLE";
      }
    }

    // Arena Boundaries (Kept within central fighting dais)
    this.x = Math.max(-8.5, Math.min(8.5, this.x));

    // Auto Face Opponent when grounded and not attacking
    if (this.opponent && this.onGround && !this.isAttacking && this.state !== "KNOCKDOWN" && this.state !== "GETUP") {
      this.facing = this.opponent.x >= this.x ? 1 : -1;
    }

    // Sync 3D Mesh
    this.model.group.position.set(this.x, this.y, 0);
    // Face each other in dynamic 3/4 3D fighting stance towards the camera
    this.model.group.rotation.y = this.facing > 0 ? (Math.PI * 0.42) : (-Math.PI * 0.42);

    // Update Bone Animation Pose
    const progress = this.stateDuration > 0 ? Math.min(1, this.stateTimer / this.stateDuration) : 0;
    this.model.updatePose(this.state, time, progress);
  }

  // Execute Player / AI Action
  triggerAction(act) {
    if (!this.engine.matchActive || this.hp <= 0) return;
    if (this.state === "KNOCKDOWN" || this.state === "GETUP" || this.state === "HIT") return;

    // Buffer combo action
    this.comboBuffer.push({ act, time: Date.now() });
    if (this.comboBuffer.length > 5) this.comboBuffer.shift();

    const combo = this.detectCombo();

    switch(combo || act) {
      case "COMBO_PPP":
        this.startAttack("PUNCH_2", 0.32, 14, 2.5, true);
        break;
      case "COMBO_PPK":
        this.startAttack("KICK_2", 0.45, 16, 4.0, true, true);
        break;
      case "COMBO_KK":
        this.startAttack("KICK_2", 0.4, 15, 3.5, true);
        break;
      case "PUNCH":
        this.startAttack(this.state === "PUNCH_1" ? "PUNCH_2" : "PUNCH_1", 0.28, 8, 1.8);
        break;
      case "KICK":
        if (!this.onGround) {
          this.startAttack("DROPKICK", 0.55, 14, 3.8, true, true);
        } else {
          this.startAttack("KICK_1", 0.36, 11, 2.4, true);
        }
        break;
      case "SWEEP":
        this.startAttack("SWEEP", 0.42, 10, 3.0, true, true);
        break;
      case "GRAB":
        if (Math.abs(this.x - this.opponent.x) < 2.0 && this.onGround) {
          this.startAttack("GRAB", 0.48, 18, 5.0, false, true);
        }
        break;
      case "BLOCK":
        this.state = "BLOCK";
        this.stateDuration = 0.4;
        this.stateTimer = 0;
        this.vx = 0;
        break;
      case "CROUCH":
        this.state = "CROUCH";
        this.stateDuration = 0.4;
        this.stateTimer = 0;
        this.vx = 0;
        break;
      case "JUMP":
        if (this.onGround) {
          this.vy = 8.5;
          this.onGround = false;
          this.state = "JUMP";
          this.stateDuration = 0;
        }
        break;
      case "SUPER":
        if (this.shakti >= 50) {
          this.shakti = 0;
          this.startAttack("SUPER", 0.75, 35, 6.0, false, true);
          this.engine.spawnSuperProp(this);
        }
        break;
      case "LEFT":
        this.vx = -4.8;
        this.state = this.facing < 0 ? "WALK_FWD" : "WALK_BACK";
        break;
      case "RIGHT":
        this.vx = 4.8;
        this.state = this.facing > 0 ? "WALK_FWD" : "WALK_BACK";
        break;
    }
  }

  detectCombo() {
    const now = Date.now();
    const recent = this.comboBuffer.filter(c => now - c.time < 1200).map(c => c.act);
    const str = recent.join(",");
    if (str.endsWith("punch,punch,punch")) return "COMBO_PPP";
    if (str.endsWith("punch,punch,kick")) return "COMBO_PPK";
    if (str.endsWith("kick,kick")) return "COMBO_KK";
    return null;
  }

  startAttack(state, duration, dmg, knockback, isKick = false, knockdown = false) {
    this.state = state;
    this.stateDuration = duration;
    this.stateTimer = 0;
    this.hitApplied = false;
    this.currentAttackData = {
      damage: dmg,
      knockback,
      isKick,
      knockdown,
      isSuper: state === "SUPER",
      type: state.toLowerCase()
    };
  }

  getActiveHitboxes() {
    const d = this.currentAttackData;
    if (!d) return [];
    // Active during middle 50% of the attack animation
    const progress = this.stateTimer / this.stateDuration;
    if (progress < 0.25 || progress > 0.75) return [];

    const reach = (d.isKick ? 1.4 : 1.1) * this.facing;
    const height = d.type === "sweep" ? 0.3 : (d.isKick ? 1.4 : 1.7);
    return [{
      x: this.x + reach,
      y: this.y + height,
      z: 0,
      radius: 0.65,
      ...d
    }];
  }

  getHurtboxes() {
    return [
      { x: this.x, y: this.y + 0.6, z: 0, radius: 0.45 }, // Legs
      { x: this.x, y: this.y + 1.4, z: 0, radius: 0.55 }, // Torso
      { x: this.x, y: this.y + 2.2, z: 0, radius: 0.42 }  // Head
    ];
  }

  // --- CPU AI Engine ---
  updateAI(dt) {
    if (!this.opponent || !this.engine.matchActive || this.hp <= 0) return;
    if (this.state === "KNOCKDOWN" || this.state === "GETUP" || this.state === "HIT") return;

    this.aiTimer -= dt;
    const opp = this.opponent;
    const diff = this.engine.difficulty;
    const dist = Math.abs(this.x - opp.x);
    // Direction directly TOWARDS opponent (-1 = left, +1 = right)
    const toOpp = (opp.x < this.x) ? -1 : 1;

    // 1. Reactive Defense: Block when player attacks in striking distance
    if (opp.isAttacking && dist < 2.5 && !this.isAttacking) {
      const blockChance = diff === "hard" ? 0.70 : (diff === "normal" ? 0.40 : 0.15);
      if (Math.random() < blockChance && this.state !== "BLOCK") {
        this.triggerAction("BLOCK");
        this.aiTimer = 0.25;
        return;
      }
    }

    // 2. Tactical Decision Timer
    if (this.aiTimer <= 0) {
      this.aiTimer = diff === "hard" ? (0.12 + Math.random() * 0.12) : 
                     (diff === "normal" ? (0.18 + Math.random() * 0.16) : (0.28 + Math.random() * 0.22));

      const rand = Math.random();

      // A. Super Attack: Unleash Super when Shakti is ready!
      if (this.shakti >= 50 && dist < 7.5 && rand < (diff === "hard" ? 0.65 : 0.40)) {
        this.triggerAction("SUPER");
        this.aiTimer = 0.8;
        return;
      }

      // B. Close Combat Range (dist <= 2.2): Attack with punch/kick/sweep combos!
      if (dist <= 2.2) {
        if (opp.state === "BLOCK" && rand < 0.65) {
          // Break guard with grab!
          this.triggerAction("GRAB");
        } else if (rand < 0.35) {
          // Punch combo
          this.triggerAction("PUNCH");
          setTimeout(() => {
            if (this.engine.matchActive && Math.abs(this.x - opp.x) <= 2.2) {
              this.triggerAction(Math.random() < 0.5 ? "PUNCH" : "KICK");
            }
          }, 140);
        } else if (rand < 0.65) {
          // Kick combo
          this.triggerAction("KICK");
          setTimeout(() => {
            if (this.engine.matchActive && Math.abs(this.x - opp.x) <= 2.2) {
              this.triggerAction("KICK");
            }
          }, 160);
        } else if (rand < 0.85) {
          // Low trip sweep
          this.triggerAction("SWEEP");
        } else {
          // Unblockable Grab
          this.triggerAction("GRAB");
        }
        return;
      }

      // C. Mid Range (2.2 < dist <= 5.0): Jump Attack or Slide Sweep
      if (dist <= 5.0) {
        if (rand < 0.35 && diff !== "easy" && this.onGround) {
          // Flying Jump Kick forward into opponent
          this.triggerAction("JUMP");
          this.vx = toOpp * 5.5;
          setTimeout(() => {
            if (this.engine.matchActive) this.triggerAction("KICK");
          }, 180);
          return;
        } else if (rand < 0.55 && this.onGround) {
          // Slide / Sweep attack
          this.vx = toOpp * 5.8;
          this.triggerAction("SWEEP");
          return;
        }
      }
    }

    // 3. Continuous Advance: ALWAYS walk/charge forward towards opponent to fight!
    if (!this.isAttacking && this.state !== "BLOCK" && this.onGround) {
      if (dist > 1.9) {
        const speed = diff === "hard" ? 5.2 : (diff === "normal" ? 4.5 : 3.8);
        this.vx = toOpp * speed;
        this.state = "WALK_FWD";
      } else {
        this.vx *= 0.5;
      }
    }
  }
}

/**
 * Super Attack Cinematic 3D Props Entity
 */
class SuperProp {
  constructor(fighter, engine) {
    this.fighter = fighter;
    this.engine = engine;
    this.group = new THREE.Group();
    this.isDead = false;
    this.life = 0;
    this.maxLife = 2.4;
    this.buildProp();
  }

  buildProp() {
    const f = this.fighter;
    this.group.position.set(f.x + f.facing * 1.5, f.y + 1.5, 0);

    switch(f.charData.id) {
      case 0: // Mitra Chakra (Flaming Spinning Wheel)
        const chakra = new THREE.Mesh(
          new THREE.TorusGeometry(1.2, 0.15, 12, 32),
          new THREE.MeshBasicMaterial({ color: 0xff9933 })
        );
        this.group.add(chakra);
        break;
      case 1: // Aloo Se Sona Machine
        const machine = new THREE.Mesh(
          new THREE.BoxGeometry(1.8, 1.8, 1.4),
          new THREE.MeshLambertMaterial({ color: 0xffd700 })
        );
        this.group.add(machine);
        break;
      case 4: // Bulldozer Charge
        const dozer = new THREE.Mesh(
          new THREE.BoxGeometry(2.5, 1.4, 1.8),
          new THREE.MeshLambertMaterial({ color: 0xffeb3b })
        );
        this.group.add(dozer);
        break;
      default: // Energy Sphere Blast
        const sphere = new THREE.Mesh(
          new THREE.SphereGeometry(1.1, 16, 16),
          new THREE.MeshBasicMaterial({ color: f.charData.color })
        );
        this.group.add(sphere);
        break;
    }
  }

  update(dt) {
    this.life += dt;
    this.group.position.x += this.fighter.facing * 9.5 * dt;
    this.group.rotation.z += 12 * dt;

    // Check hit on opponent
    const opp = this.fighter.opponent;
    if (opp && !this.hasHit) {
      const dist = Math.abs(this.group.position.x - opp.x);
      if (dist < 1.2 && Math.abs(this.group.position.y - (opp.y + 1.2)) < 1.5) {
        this.hasHit = true;
        this.engine.handleHit(this.fighter, opp, {
          damage: 35,
          knockback: 6.5,
          knockdown: true,
          isSuper: true,
          type: "super"
        });
        SoundSystem.playSuperBlast();
      }
    }

    if (this.life >= this.maxLife) {
      this.isDead = true;
    }
  }
}
