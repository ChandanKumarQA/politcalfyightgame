'use strict';
/* Procedural 3D cartoon fighter: toon-shaded primitives with inverted-hull outlines + a pose animator. */
const Toon = (function () {
  let grad = null, outline = null;
  const geos = new Map();
  function gradMap() {
    if (!grad) {
      grad = new THREE.DataTexture(new Uint8Array([80, 160, 255]), 3, 1, THREE.RedFormat);
      grad.minFilter = grad.magFilter = THREE.NearestFilter;
      grad.generateMipmaps = false;
      grad.needsUpdate = true;
    }
    return grad;
  }
  function mat(color, extra) {
    return new THREE.MeshToonMaterial(Object.assign({ color: color, gradientMap: gradMap() }, extra || {}));
  }
  function outMat() {
    if (!outline) outline = new THREE.MeshBasicMaterial({ color: 0x1a0d06, side: THREE.BackSide });
    return outline;
  }
  function geo(key, make) {
    let g = geos.get(key);
    if (!g) { g = make(); geos.set(key, g); }
    return g;
  }
  return {
    mat, outMat, gradMap,
    sphere: (r, w, h) => geo('s' + r + ',' + w + ',' + h, () => new THREE.SphereGeometry(r, w || 20, h || 14)),
    cap: (r, l) => geo('c' + r + ',' + l, () => new THREE.CapsuleGeometry(r, l, 6, 14)),
    box: (x, y, z) => geo('b' + x + ',' + y + ',' + z, () => new THREE.BoxGeometry(x, y, z)),
    cyl: (rt, rb, h, seg) => geo('y' + rt + ',' + rb + ',' + h + ',' + seg, () => new THREE.CylinderGeometry(rt, rb, h, seg || 20)),
    cone: (r, h, seg) => geo('k' + r + ',' + h + ',' + seg, () => new THREE.ConeGeometry(r, h, seg || 14)),
    torus: (r, t) => geo('t' + r + ',' + t, () => new THREE.TorusGeometry(r, t, 8, 24))
  };
})();

function buildFighter(def) {
  const S = def.scale || 1;
  const cache = new Map(), matList = [];
  const M = (c) => {
    let m = cache.get(c);
    if (!m) { m = Toon.mat(c); cache.set(c, m); matList.push(m); }
    return m;
  };
  // part(): a toon mesh with an outline hull as a child
  function part(geo, color, parent, o) {
    o = o || {};
    const m = new THREE.Mesh(geo, o.mat || M(color));
    if (o.p) m.position.set(o.p[0], o.p[1], o.p[2]);
    if (o.r) m.rotation.set(o.r[0], o.r[1], o.r[2]);
    if (o.s) m.scale.set(o.s[0], o.s[1], o.s[2]);
    if (o.outline !== false) {
      const ol = new THREE.Mesh(geo, Toon.outMat());
      const k = o.os || 1.09;
      ol.scale.set(k, k, k);
      m.add(ol);
    }
    if (parent) parent.add(m);
    return m;
  }
  const grp = (parent, x, y, z) => { const g = new THREE.Group(); g.position.set(x || 0, y || 0, z || 0); if (parent) parent.add(g); return g; };

  const root = new THREE.Group();
  const body = grp(root); body.scale.setScalar(S);
  const hips = grp(body, 0, 0.99, 0);

  // ---------- torso ----------
  const torso = grp(hips);
  part(Toon.cap(0.34, 0.2), def.top, torso, { p: [0, 0.42, 0], s: [0.86, 1, 1.08] });
  if (def.skirt !== false) part(Toon.cyl(0.33, 0.5, 0.5, 20), def.top, torso, { p: [0, -0.1, 0], s: [0.9, 1, 1.1], os: 1.07 });
  part(Toon.torus(0.37, 0.055), def.accent, torso, { p: [0, 0.04, 0], r: [Math.PI / 2, 0, 0], s: [0.9, 1.1, 1], outline: false });
  part(Toon.torus(0.3, 0.07), def.accent, torso, { p: [0, 0.84, 0], r: [Math.PI / 2, 0, 0], s: [0.9, 1.1, 1], outline: false });

  // ---------- head ----------
  const head = grp(torso, 0, 1.3, 0);
  const R = 0.58;
  part(Toon.sphere(R, 28, 20), def.skin, head, { s: [1, 0.96, 1.02], os: 1.06 });
  [-1, 1].forEach((sd) => part(Toon.sphere(0.12, 12, 10), def.skin, head, { p: [-0.02, -0.02, sd * 0.57], s: [0.6, 1, 0.7], os: 1.12 }));
  // eyes
  const eyes = grp(head), exes = grp(head), brows = [];
  const pupils = [];
  [-1, 1].forEach((sd) => {
    part(Toon.sphere(0.135, 14, 12), 0xffffff, eyes, { p: [0.47, 0.07, sd * 0.2], s: [0.62, 1, 0.9], os: 1.08 });
    pupils.push(part(Toon.sphere(0.075, 10, 8), 0x120a06, eyes, { p: [0.54, 0.06, sd * 0.2], s: [0.5, 1, 0.9], outline: false }));
    const b = part(Toon.box(0.04, 0.045, 0.2), def.hair || 0x222222, head, { p: [0.5, 0.25, sd * 0.2], r: [sd * -0.22, 0, 0], outline: false });
    brows.push(b);
    // X eyes (KO)
    [0.7, -0.7].forEach((a) => part(Toon.box(0.03, 0.2, 0.04), 0x120a06, exes, { p: [0.55, 0.07, sd * 0.2], r: [a, 0, 0], outline: false }));
  });
  exes.visible = false;
  part(Toon.sphere(0.095, 12, 10), def.skin, head, { p: [0.58, -0.08, 0], s: [0.9, 0.9, 0.9], os: 1.12 });
  const mouth = part(Toon.sphere(0.1, 12, 8), 0x3a0d0d, head, { p: [0.55, -0.29, 0], s: [0.25, 0.28, 1.5], outline: false });

  if (def.glasses) {
    [-1, 1].forEach((sd) => part(Toon.torus(0.14, 0.022), 0x111111, head, { p: [0.56, 0.07, sd * 0.2], r: [0, Math.PI / 2, 0], outline: false }));
    part(Toon.box(0.03, 0.03, 0.14), 0x111111, head, { p: [0.57, 0.1, 0], outline: false });
  }
  if (def.stache) {
    const t = def.stache === 2 ? 0.075 : 0.05;
    [-1, 1].forEach((sd) => part(Toon.cap(t, 0.17), def.hair, head, { p: [0.56, -0.17, sd * 0.1], r: [sd * 1.2, 0, 0], outline: false }));
  }
  if (def.beard) {
    const bg = new THREE.SphereGeometry(0.6, 20, 12, 0, Math.PI * 2, Math.PI * 0.64, Math.PI * 0.3);
    part(bg, def.hair, head, { p: [0.03, -0.02, 0], s: [1, 1, 1.02], outline: false });
  }
  // hair
  const hairMat = M(def.hair);
  const cap = (r, tilt, thetaEnd) => {
    const g = new THREE.SphereGeometry(r, 24, 14, 0, Math.PI * 2, 0, thetaEnd || Math.PI * 0.55);
    return part(g, def.hair, head, { r: [0, 0, tilt], outline: false });
  };
  switch (def.hairStyle) {
    case 'short': cap(R * 1.06, 0.38); break;
    case 'slick': cap(R * 1.07, 0.5); part(Toon.sphere(0.3, 12, 10), def.hair, head, { p: [-0.35, 0.3, 0], s: [1, 0.9, 1.2], outline: false }); break;
    case 'bun': cap(R * 1.06, 0.42); part(Toon.sphere(0.22, 12, 10), def.hair, head, { p: [-0.32, 0.62, 0] }); break;
    case 'afro': cap(R * 1.25, 0.3, Math.PI * 0.62); break;
    case 'spiky': {
      cap(R * 1.05, 0.38);
      for (let i = 0; i < 7; i++) {
        const a = (i / 7) * Math.PI * 2;
        part(Toon.cone(0.16, 0.5, 8), def.hair, head, { p: [Math.cos(a) * 0.3 - 0.08, 0.6 + Math.sin(i * 1.7) * 0.03, Math.sin(a) * 0.38], r: [Math.sin(a) * 0.5, 0, -Math.cos(a) * 0.5 - 0.1] });
      }
      break;
    }
  }
  // hats
  const hc = def.hatColor || 0xffffff;
  if (def.hat === 'topi') {
    part(Toon.cyl(0.46, 0.5, 0.34, 20), hc, head, { p: [-0.02, 0.56, 0], r: [0, 0, 0.18], s: [0.82, 1, 1.12] });
  } else if (def.hat === 'turban') {
    part(Toon.sphere(R * 1.1, 20, 12), hc, head, { p: [-0.04, 0.16, 0], s: [1, 0.9, 1.05] });
    for (let i = 0; i < 3; i++) part(Toon.torus(0.5 - i * 0.04, 0.11), i % 2 ? 0xffffff : hc, head, { p: [-0.02, 0.34 + i * 0.13, 0], r: [Math.PI / 2 + 0.2, 0.0, 0], s: [1, 1.1, 1] });
    part(Toon.sphere(0.2, 12, 10), hc, head, { p: [-0.05, 0.88, 0], s: [1, 0.8, 1] });
  } else if (def.hat === 'cap') {
    const g = new THREE.SphereGeometry(R * 1.08, 22, 12, 0, Math.PI * 2, 0, Math.PI * 0.5);
    part(g, hc, head, { r: [0, 0, 0.2], outline: false });
    part(Toon.cyl(0.4, 0.4, 0.06, 16), hc, head, { p: [0.56, 0.28, 0], s: [1, 1, 1.1], r: [0, 0, 0.2] });
  } else if (def.hat === 'crown') {
    part(Toon.cyl(0.46, 0.5, 0.22, 18), hc, head, { p: [-0.02, 0.62, 0] });
    for (let i = 0; i < 6; i++) {
      const a = (i / 6) * Math.PI * 2;
      part(Toon.cone(0.09, 0.26, 6), 0xef4444, head, { p: [-0.02 + Math.cos(a) * 0.42, 0.85, Math.sin(a) * 0.42], outline: true, os: 1.15 });
    }
  }

  // ---------- arms (N = near camera side z+, F = far) ----------
  function arm(side) {
    const sh = grp(torso, 0, 0.64, side * 0.46);
    part(Toon.cap(0.125, 0.22), def.top, sh, { p: [0, -0.2, 0], os: 1.14 });
    const el = grp(sh, 0, -0.4, 0);
    part(Toon.cap(0.105, 0.2), def.skin, el, { p: [0, -0.18, 0], os: 1.14 });
    part(Toon.sphere(0.175, 14, 12), def.skin, el, { p: [0, -0.42, 0], os: 1.13 });
    return { sh, el };
  }
  const aN = arm(1), aF = arm(-1);

  // ---------- legs ----------
  function leg(side) {
    const hp = grp(hips, 0, 0, side * 0.2);
    part(Toon.cap(0.16, 0.2), def.bottom, hp, { p: [0, -0.22, 0], os: 1.12 });
    const kn = grp(hp, 0, -0.42, 0);
    part(Toon.cap(0.14, 0.2), def.bottom, kn, { p: [0, -0.21, 0], os: 1.12 });
    part(Toon.sphere(0.19, 14, 10), def.shoe, kn, { p: [0.07, -0.44, 0], s: [1.55, 0.7, 1.1], os: 1.1 });
    return { hp, kn };
  }
  const lN = leg(1), lF = leg(-1);

  // ---------- aura (super charge) ----------
  const auraMat = new THREE.MeshBasicMaterial({ color: def.sup.color, transparent: true, opacity: 0.35, blending: THREE.AdditiveBlending, depthWrite: false });
  const aura = new THREE.Mesh(new THREE.SphereGeometry(1.5, 20, 14), auraMat);
  aura.position.y = 1.5; aura.visible = false; body.add(aura);

  const rig = {
    root, body, hips, torso, head, aN, aF, lN, lF, mouth, pupils, brows, eyes, exes, aura, auraMat,
    cur: { lean: 0.1, tilt: 0, shN: 0.9, elN: 1.7, shF: 0.5, elF: 1.9, hipN: 0.28, knN: -0.4, hipF: -0.32, knF: -0.45, yaw: 0, bodyZ: 0, bodyY: 0, squash: 1 },
    yaw: 0,
    flash(v) {
      for (let i = 0; i < matList.length; i++) {
        matList[i].emissive.setHex(v ? 0xffffff : 0x000000);
        matList[i].emissiveIntensity = v ? 0.75 : 0;
      }
    },
    setExpr(e) {
      const hit = e === 'hit', ko = e === 'ko', win = e === 'win', ang = e === 'angry';
      eyes.visible = !ko; exes.visible = ko;
      mouth.scale.set(hit || ko ? 0.4 : 0.25, hit || ko ? 1.0 : win ? 0.6 : 0.28, win ? 2.4 : hit || ko ? 1.1 : 1.5);
      mouth.position.y = win ? -0.27 : -0.29;
      brows[0].rotation.x = (hit ? 0.35 : ang ? 0.5 : -0.22) * -1;
      brows[1].rotation.x = (hit ? -0.35 : ang ? -0.5 : 0.22) * -1;
    }
  };
  rig.setExpr('normal');
  return rig;
}

/* ---- Pose animation. f = logic fighter. Angles: limbs + = swing forward; lean + = forward ---- */
const ATK_ARC = { P1: 1, P2: 1, P3: 1 };

function atkProg(f) {
  const m = f.move;
  if (!m) return 0;
  if (m.id === 'AIRKICK') return 1;
  if (f.moveT < m.from) return f.moveT / m.from;
  if (f.moveT <= m.to) return 1;
  return Math.max(0, 1 - (f.moveT - m.to) / (m.total - m.to));
}

function poseTarget(f, t) {
  const P = {
    lean: 0.12, tilt: 0.0, shN: 0.95, elN: 1.7, shF: 0.55, elF: 1.95,
    hipN: 0.3, knN: -0.45, hipF: -0.32, knF: -0.5, yaw: 0, bodyZ: 0, bodyY: 0, squash: 1, k: 0.35
  };
  const bob = Math.sin(t * 0.11 + f.phase);
  const ease = (x) => x * x * (3 - 2 * x);
  switch (f.st) {
    case 'IDLE':
      P.lean += bob * 0.03; P.hipN += bob * 0.05; P.hipF -= bob * 0.03;
      P.shN += bob * 0.06; P.shF += bob * 0.05;
      break;
    case 'WALK': {
      const w = f.walkPhase, dir = f.vx * f.facing >= 0 ? 1 : -1;
      P.hipN = 0.15 + Math.sin(w) * 0.75 * dir; P.knN = -0.35 - Math.max(0, -Math.sin(w) * dir) * 0.9;
      P.hipF = 0.15 - Math.sin(w) * 0.75 * dir; P.knF = -0.35 - Math.max(0, Math.sin(w) * dir) * 0.9;
      P.lean = 0.2; P.shN += Math.sin(w) * 0.2; P.shF -= Math.sin(w) * 0.2;
      break;
    }
    case 'JUMP':
      P.hipN = 0.9; P.knN = -1.5; P.hipF = 0.25; P.knF = -1.1;
      P.shN = 1.5; P.elN = 0.5; P.shF = 1.3; P.elF = 0.6; P.lean = 0.05;
      break;
    case 'CROUCH':
      P.hipN = 1.15; P.knN = -1.25; P.hipF = 0.95; P.knF = -1.15;
      P.lean = 0.35; P.shN = 1.0; P.shF = 0.7; P.elN = 1.9; P.elF = 2.0;
      break;
    case 'BLOCK':
      P.shN = 1.25; P.elN = 2.35; P.shF = 1.35; P.elF = 2.4; P.lean = 0.0; P.hipN = 0.4; P.knN = -0.7; P.hipF = -0.4; P.knF = -0.7; P.tilt = 0.15;
      break;
    case 'HIT':
      P.lean = -0.4; P.tilt = -0.35; P.shN = -0.5; P.elN = 0.6; P.shF = -0.8; P.elF = 0.5;
      P.hipN = 0.45; P.knN = -0.6; P.hipF = -0.5; P.knF = -0.4; P.k = 0.6;
      break;
    case 'KO': {
      const a = Math.min(1, f.koT / 22);
      P.bodyZ = ease(a) * 1.5; P.bodyY = ease(a) * 0.5; P.k = 0.5;
      P.shN = 2.2; P.elN = 0.4; P.shF = 1.8; P.elF = 0.3; P.hipN = 0.2; P.knN = -0.2; P.hipF = -0.1; P.knF = -0.3; P.lean = 0; P.tilt = -0.3;
      break;
    }
    case 'WIN':
      P.shN = 2.9 + Math.sin(t * 0.3) * 0.15; P.elN = 0.2; P.shF = 2.7 - Math.sin(t * 0.3) * 0.15; P.elF = 0.3;
      P.lean = -0.05; P.hipN = 0.2; P.knN = -0.3; P.hipF = -0.2; P.knF = -0.3;
      P.bodyY = Math.abs(Math.sin(t * 0.15)) * 0.35; P.k = 0.4;
      break;
    case 'ATTACK': {
      const m = f.move, pr = atkProg(f), e = ease(pr);
      P.k = 0.75;
      switch (m.id) {
        case 'P1': P.shN = 0.7 + 0.9 * e; P.elN = 1.9 - 1.85 * e; P.lean = 0.12 + 0.28 * e; P.yaw = -0.35 * e; P.hipN = 0.5; P.knN = -0.7; P.hipF = -0.6; P.knF = -0.5; break;
        case 'P2': P.shF = 0.7 + 0.9 * e; P.elF = 1.9 - 1.85 * e; P.lean = 0.12 + 0.28 * e; P.yaw = 0.35 * e; P.hipN = 0.5; P.knN = -0.7; P.hipF = -0.6; P.knF = -0.5; break;
        case 'P3': P.shN = 2.9 - 1.6 * e; P.elN = 0.25; P.shF = 2.5 - 1.5 * e; P.elF = 0.4; P.lean = -0.15 + 0.7 * e; P.yaw = -0.5 * e; P.hipN = 0.7; P.knN = -0.8; P.hipF = -0.7; P.knF = -0.5; break;
        case 'KICK': P.hipN = 0.2 + 1.3 * e; P.knN = -0.7 + 0.6 * e; P.lean = -0.25 * e; P.hipF = -0.1; P.knF = -0.3; P.shN = 0.6; P.shF = 1.0; break;
        case 'KICK2': P.hipN = 0.2 + 1.65 * e; P.knN = -0.8 + 0.7 * e; P.lean = -0.5 * e; P.yaw = 0.6 * e; P.hipF = -0.1; P.knF = -0.3; P.shN = 0.3; P.shF = 1.6; break;
        case 'SWEEP': P.hipN = 0.6 + 0.9 * e; P.knN = -0.9 + 0.85 * e; P.hipF = 1.15; P.knF = -1.4; P.lean = -0.25 * e + 0.25; P.shN = 0.7; P.shF = 1.0; break;
        case 'UPPER': P.shN = 0.4 + 2.55 * e; P.elN = 1.6 - 1.4 * e; P.lean = -0.25 * e; P.hipN = 0.6; P.knN = -0.8; P.hipF = -0.5; P.knF = -0.6; P.bodyY = 0.12 * e; break;
        case 'AIRKICK': P.hipN = 1.35; P.knN = -0.15; P.hipF = 0.3; P.knF = -1.3; P.lean = -0.35; P.shN = 1.5; P.shF = -0.3; P.elN = 0.5; break;
        case 'GRAB': P.shN = 0.8 + 0.75 * e; P.shF = 0.8 + 0.75 * e; P.elN = 0.3; P.elF = 0.3; P.lean = 0.25 + 0.2 * e; P.hipN = 0.6; P.knN = -0.7; P.hipF = -0.6; P.knF = -0.5; break;
        case 'SUPER': {
          const ch = Math.min(1, f.moveT / m.from);
          if (f.moveT < m.from) { P.shN = -0.4; P.elN = 1.2; P.shF = -0.6; P.elF = 1.4; P.lean = -0.3 * ch; P.hipN = 0.5; P.knN = -0.7; P.hipF = -0.5; P.knF = -0.6; P.bodyY = 0; P.squash = 1 - 0.06 * ch; }
          else { const r = Math.min(1, (f.moveT - m.from) / 4); P.shN = 1.55 * r - 0.4 * (1 - r); P.shF = 1.45 * r - 0.6 * (1 - r); P.elN = 0.1; P.elF = 0.15; P.lean = 0.45 * r - 0.3 * (1 - r); P.hipN = 0.6; P.knN = -0.7; P.hipF = -0.6; P.knF = -0.5; }
          break;
        }
      }
      break;
    }
  }
  return P;
}

function animateFighter(f, t) {
  const rig = f.rig, c = rig.cur, P = poseTarget(f, t), k = P.k;
  for (const key in c) c[key] += (P[key] - c[key]) * k;

  // face camera-ish with smooth turning
  const wantYaw = f.facing > 0 ? -0.42 : Math.PI + 0.42;
  let d = wantYaw - rig.yaw;
  while (d > Math.PI) d -= Math.PI * 2;
  while (d < -Math.PI) d += Math.PI * 2;
  rig.yaw += d * 0.35;
  rig.root.rotation.y = rig.yaw + c.yaw;
  rig.root.position.set(f.x, f.y, 0);

  rig.torso.rotation.z = -c.lean;
  rig.head.rotation.z = -c.tilt;
  rig.aN.sh.rotation.z = c.shN; rig.aN.el.rotation.z = c.elN;
  rig.aF.sh.rotation.z = c.shF; rig.aF.el.rotation.z = c.elF;
  rig.lN.hp.rotation.z = c.hipN; rig.lN.kn.rotation.z = c.knN;
  rig.lF.hp.rotation.z = c.hipF; rig.lF.kn.rotation.z = c.knF;
  rig.body.rotation.z = c.bodyZ;
  rig.body.position.y = c.bodyY;

  // keep the lowest foot on the floor when grounded
  if (f.y <= 0.001 && f.st !== 'KO') {
    const ext = (a, kk) => 0.42 * Math.cos(a) + 0.44 * Math.cos(a + kk) + 0.13;
    rig.hips.position.y = Math.max(0.45, Math.max(ext(c.hipN, c.knN), ext(c.hipF, c.knF)));
  } else {
    rig.hips.position.y = 0.99;
  }
  rig.hips.scale.y = c.squash;

  // super aura
  const charging = f.st === 'ATTACK' && f.move && f.move.id === 'SUPER' && f.moveT < 32;
  rig.aura.visible = charging || f.meter >= 100;
  if (rig.aura.visible) {
    const pulse = 1 + Math.sin(t * 0.5) * 0.08;
    rig.aura.scale.setScalar(pulse * (charging ? 1.15 : 0.82));
    rig.auraMat.opacity = charging ? 0.42 : 0.07 + Math.sin(t * 0.3) * 0.025;
  }
  const flash = f.flash > 3;
  if (flash !== rig._fl) { rig.flash(flash); rig._fl = flash; }
  const ex = f.st === 'KO' ? 'ko' : f.st === 'HIT' ? 'hit' : f.st === 'WIN' ? 'win' : (f.st === 'ATTACK' ? 'angry' : 'normal');
  if (ex !== rig._ex) { rig.setExpr(ex); rig._ex = ex; }
}
