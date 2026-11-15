'use strict';
/* Three stylised 3D arenas built entirely from primitives + canvas textures. */
const Arenas = (function () {
  const T = Toon;

  function canvasTex(w, h, fn, rep) {
    const c = document.createElement('canvas'); c.width = w; c.height = h;
    fn(c.getContext('2d'), w, h);
    const t = new THREE.CanvasTexture(c);
    t.colorSpace = THREE.SRGBColorSpace;
    t.anisotropy = 4;
    if (rep) { t.wrapS = t.wrapT = THREE.RepeatWrapping; t.repeat.set(rep[0], rep[1]); }
    return t;
  }
  function rnd(seed) { let s = seed; return () => ((s = (s * 16807) % 2147483647) - 1) / 2147483646; }
  function box(parent, w, h, d, color, x, y, z, m) {
    const mesh = new THREE.Mesh(T.box(w, h, d), m || T.mat(color));
    mesh.position.set(x, y, z); parent.add(mesh); return mesh;
  }
  function skyDome(top, mid, bot, r) {
    const t = canvasTex(8, 256, (g, w, h) => {
      const gr = g.createLinearGradient(0, 0, 0, h);
      gr.addColorStop(0, top); gr.addColorStop(0.55, mid); gr.addColorStop(1, bot);
      g.fillStyle = gr; g.fillRect(0, 0, w, h);
    });
    const m = new THREE.Mesh(new THREE.SphereGeometry(r || 90, 24, 16), new THREE.MeshBasicMaterial({ map: t, side: THREE.BackSide, fog: false }));
    return m;
  }
  function blobTexture() {
    return canvasTex(64, 64, (g, w, h) => {
      const gr = g.createRadialGradient(32, 32, 2, 32, 32, 30);
      gr.addColorStop(0, 'rgba(0,0,0,0.55)'); gr.addColorStop(1, 'rgba(0,0,0,0)');
      g.fillStyle = gr; g.fillRect(0, 0, 64, 64);
    });
  }

  // instanced bobbing crowd. pts: [{x,y,z,ph}]
  function crowd(parent, pts, palette, seed, scale) {
    const R = rnd(seed || 7), n = pts.length, sc = scale || 1;
    const heads = new THREE.InstancedMesh(new THREE.SphereGeometry(0.36, 12, 10), T.mat(0xffffff), n);
    const bodies = new THREE.InstancedMesh(new THREE.CapsuleGeometry(0.34, 0.55, 3, 10), T.mat(0xffffff), n);
    const skins = [0xd9a066, 0xc68642, 0xb87a4b, 0xe0ac83, 0xcb9462, 0xe8b88a];
    const col = new THREE.Color();
    for (let i = 0; i < n; i++) {
      heads.setColorAt(i, col.setHex(skins[Math.floor(R() * skins.length)]));
      bodies.setColorAt(i, col.setHex(palette[Math.floor(R() * palette.length)]));
    }
    parent.add(heads, bodies);
    const d = new THREE.Object3D();
    return function (t) {
      for (let i = 0; i < n; i++) {
        const p = pts[i], b = Math.max(0, Math.sin(t * 3.2 + p.ph)) * 0.22;
        d.position.set(p.x, p.y + b, p.z); d.scale.setScalar(sc); d.rotation.set(0, 0, Math.sin(t * 2 + p.ph) * 0.05);
        d.updateMatrix(); bodies.setMatrixAt(i, d.matrix);
        d.position.set(p.x, p.y + b + 0.78 * sc, p.z); d.updateMatrix(); heads.setMatrixAt(i, d.matrix);
      }
      heads.instanceMatrix.needsUpdate = true; bodies.instanceMatrix.needsUpdate = true;
    };
  }

  // ------------------------------------------------------------------ SABHA HALL
  function hall() {
    const g = new THREE.Group(), R = rnd(11);
    const floorT = canvasTex(256, 256, (c, w, h) => {
      for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) {
        c.fillStyle = (x + y) % 2 ? '#e8d3a8' : '#a9764a'; c.fillRect(x * 32, y * 32, 32, 32);
      }
      c.strokeStyle = 'rgba(60,30,10,.35)'; c.lineWidth = 2;
      for (let i = 0; i <= 8; i++) { c.beginPath(); c.moveTo(i * 32, 0); c.lineTo(i * 32, 256); c.stroke(); c.beginPath(); c.moveTo(0, i * 32); c.lineTo(256, i * 32); c.stroke(); }
    }, [14, 9]);
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(50, 36), T.mat(0xffffff, { map: floorT }));
    floor.rotation.x = -Math.PI / 2; floor.position.set(0, 0, -2); g.add(floor);

    // wooden slat wall (curved)
    const wallT = canvasTex(512, 64, (c, w, h) => {
      for (let i = 0; i < 64; i++) {
        const l = 30 + (i % 3) * 4 + R() * 4;
        c.fillStyle = 'hsl(24,55%,' + l + '%)'; c.fillRect(i * 8, 0, 8, h);
        c.fillStyle = 'rgba(0,0,0,.35)'; c.fillRect(i * 8, 0, 1.5, h);
      }
    }, [5, 1]);
    const wall = new THREE.Mesh(new THREE.CylinderGeometry(19, 19, 12, 64, 1, true), T.mat(0xffffff, { map: wallT, side: THREE.BackSide }));
    wall.position.set(0, 6, 3); g.add(wall);

    // pillars arc
    for (let i = 0; i < 15; i++) {
      const a = (i / 14 - 0.5) * 2.2;
      const x = Math.sin(a) * 17, z = 4 - Math.cos(a) * 17;
      const p = new THREE.Group(); p.position.set(x, 0, z); p.rotation.y = -a; g.add(p);
      const m = T.mat(0xc8935a);
      const col = new THREE.Mesh(T.cyl(0.55, 0.65, 9, 16), m); col.position.y = 4.5; p.add(col);
      const b = new THREE.Mesh(T.box(1.6, 0.5, 1.6), T.mat(0x7a4a26)); b.position.y = 0.25; p.add(b);
      const cp = new THREE.Mesh(T.box(1.6, 0.5, 1.6), T.mat(0x7a4a26)); cp.position.y = 9.1; p.add(cp);
    }
    // gallery tiers (stepped box segments along an arc) with crowd
    const pts = [];
    for (let tier = 0; tier < 3; tier++) {
      const rad = 12.5 + tier * 2.0, y = 1.0 + tier * 1.35;
      const segs = 22;
      for (let i = 0; i < segs; i++) {
        const a = (i / (segs - 1) - 0.5) * 2.4;
        const sb = new THREE.Mesh(T.box(2.4, y, 2.1), T.mat(tier % 2 ? 0x6b4428 : 0x5b3a22));
        sb.position.set(Math.sin(a) * rad, y / 2, 3 - Math.cos(a) * rad); sb.rotation.y = -a; g.add(sb);
      }
      const n = 40 + tier * 6;
      for (let i = 0; i < n; i++) {
        const a = (i / (n - 1) - 0.5) * 2.3;
        pts.push({ x: Math.sin(a) * rad, y: y, z: 3 - Math.cos(a) * rad, ph: R() * 6 });
      }
    }
    const upd = crowd(g, pts, [0xffffff, 0xf5f0e6, 0xf28c28, 0x16a34a, 0x2563eb, 0xd62839, 0xfbbf24], 5, 1);

    // emblem: golden disc with a throne-chair silhouette
    const em = new THREE.Group(); em.position.set(0, 6.8, -12.7); g.add(em);
    const disc = new THREE.Mesh(T.cyl(2.3, 2.3, 0.3, 40), T.mat(0xfbbf24)); disc.rotation.x = Math.PI / 2; em.add(disc);
    const rim = new THREE.Mesh(T.torus(2.3, 0.14), T.mat(0x7a4a26)); rim.position.z = 0.1; em.add(rim);
    const dk = T.mat(0x5a2d12);
    [[0, 0.1, 1.9, 0.35], [0, -0.35, 1.5, 0.3]].forEach((q) => { const m = new THREE.Mesh(T.box(q[2], q[3], 0.3), dk); m.position.set(q[0], q[1], 0.3); em.add(m); });
    const back = new THREE.Mesh(T.box(1.5, 1.7, 0.3), dk); back.position.set(0, 1.0, 0.3); em.add(back);
    [-0.65, 0.65].forEach((x) => { const l = new THREE.Mesh(T.box(0.22, 1.2, 0.3), dk); l.position.set(x, -1.0, 0.3); em.add(l); });
    // hanging lamps
    const lampM = new THREE.MeshBasicMaterial({ color: 0xffe08a });
    const lamps = [];
    for (let i = 0; i < 7; i++) {
      const x = (i - 3) * 5.2, z = -3 - Math.abs(i - 3) * 0.6;
      const l = new THREE.Group(); l.position.set(x, 8.5, z); g.add(l);
      const chain = new THREE.Mesh(T.cyl(0.03, 0.03, 3, 6), T.mat(0x222222)); chain.position.y = 1.5; l.add(chain);
      const bulb = new THREE.Mesh(T.sphere(0.5, 14, 10), lampM); l.add(bulb);
      const shade = new THREE.Mesh(T.cone(0.9, 0.7, 14), T.mat(0xb45309)); shade.position.y = 0.55; l.add(shade);
      lamps.push(l);
    }
    // floor diyas
    for (let i = 0; i < 9; i++) {
      const x = -9 + i * 2.25, z = 1.8;
      const d = new THREE.Mesh(T.cyl(0.22, 0.14, 0.14, 10), T.mat(0xb45309)); d.position.set(x, 0.07, z + 2.2); g.add(d);
      const fl = new THREE.Mesh(T.cone(0.09, 0.32, 8), new THREE.MeshBasicMaterial({ color: 0xffb020 })); fl.position.set(x, 0.3, z + 2.2); g.add(fl);
    }
    return {
      group: g, bg: 0x1c0a08, fog: [0x2a120a, 24, 60],
      light: { sky: 0xffe2b8, ground: 0x6a3a1e, hi: 0.95, dir: 0xfff1d6, di: 1.0, pos: [-4, 10, 8] },
      update(t) { upd(t); lamps.forEach((l, i) => { l.rotation.z = Math.sin(t * 0.8 + i) * 0.04; }); }
    };
  }

  // ------------------------------------------------------------------ RALLY MAIDAN
  function rally() {
    const g = new THREE.Group(), R = rnd(23);
    g.add(skyDome('#1a0b3d', '#7a2d6b', '#ff8a3d'));
    // stars
    const sp = []; for (let i = 0; i < 160; i++) { const a = R() * Math.PI * 2, e = 0.25 + R() * 1.0; sp.push(Math.cos(a) * Math.cos(e) * 85, Math.sin(e) * 85, -Math.abs(Math.sin(a)) * Math.cos(e) * 85 - 10); }
    const sg = new THREE.BufferGeometry(); sg.setAttribute('position', new THREE.Float32BufferAttribute(sp, 3));
    g.add(new THREE.Points(sg, new THREE.PointsMaterial({ color: 0xffffff, size: 0.6, fog: false })));
    const moon = new THREE.Mesh(new THREE.SphereGeometry(3, 20, 14), new THREE.MeshBasicMaterial({ color: 0xfff1b8, fog: false })); moon.position.set(22, 22, -70); g.add(moon);

    const dirt = canvasTex(256, 256, (c, w, h) => {
      c.fillStyle = '#6b4a2e'; c.fillRect(0, 0, w, h);
      for (let i = 0; i < 900; i++) { c.fillStyle = 'rgba(' + (R() > 0.5 ? '255,220,170' : '30,15,5') + ',' + (R() * 0.12) + ')'; c.fillRect(R() * w, R() * h, 2 + R() * 6, 2 + R() * 4); }
    }, [12, 8]);
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(70, 44), T.mat(0xffffff, { map: dirt }));
    floor.rotation.x = -Math.PI / 2; floor.position.set(0, 0, -4); g.add(floor);
    // carpet strip for fighters
    const carpet = new THREE.Mesh(new THREE.PlaneGeometry(16, 3.2), T.mat(0xb3261e));
    carpet.rotation.x = -Math.PI / 2; carpet.position.set(0, 0.01, 0); g.add(carpet);
    const trim = new THREE.Mesh(new THREE.PlaneGeometry(16.4, 3.6), T.mat(0xfbbf24));
    trim.rotation.x = -Math.PI / 2; trim.position.set(0, 0.005, 0); g.add(trim);

    // banner stage
    const bt = canvasTex(1024, 256, (c, w, h) => {
      const gr = c.createLinearGradient(0, 0, w, 0); gr.addColorStop(0, '#d62839'); gr.addColorStop(1, '#8c1d2a');
      c.fillStyle = gr; c.fillRect(0, 0, w, h);
      c.strokeStyle = '#fbbf24'; c.lineWidth = 14; c.strokeRect(10, 10, w - 20, h - 20);
      c.fillStyle = '#fff'; c.font = '120px Bangers, Impact, sans-serif'; c.textAlign = 'center'; c.textBaseline = 'middle';
      c.strokeStyle = '#1a0d06'; c.lineWidth = 12; c.lineJoin = 'round';
      c.strokeText('CHAI · SAMOSA · ZINDABAD', w / 2, h / 2 + 6); c.fillText('CHAI · SAMOSA · ZINDABAD', w / 2, h / 2 + 6);
    });
    const banner = new THREE.Mesh(new THREE.PlaneGeometry(14, 3.5), new THREE.MeshBasicMaterial({ map: bt }));
    banner.position.set(0, 4.7, -9.5); g.add(banner);
    // stage platform + truss
    box(g, 20, 1.2, 5, 0, 0, 0.6, -11.5, T.mat(0x7c4a2a));
    [-9, 9].forEach((x) => { box(g, 0.5, 9, 0.5, 0x333333, x, 4.5, -9.5, T.mat(0x2b2b30)); });
    box(g, 19, 0.5, 0.5, 0, 0, 9.2, -9.5, T.mat(0x2b2b30));
    // speakers
    [-11, 11].forEach((x) => { box(g, 2.2, 4.2, 1.6, 0, 0, 2.1, -9, T.mat(0x1c1c22)).position.x = x; [1.2, 3].forEach((y) => { const sp2 = new THREE.Mesh(T.cyl(0.7, 0.7, 0.2, 18), T.mat(0x3f3f48)); sp2.rotation.x = Math.PI / 2; sp2.position.set(x, y, -8.1); g.add(sp2); }); });
    // spotlights
    const beams = [];
    const bm = new THREE.MeshBasicMaterial({ color: 0xfff1b8, transparent: true, opacity: 0.12, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide });
    for (let i = 0; i < 4; i++) {
      const b = new THREE.Mesh(new THREE.ConeGeometry(2.4, 11, 18, 1, true), bm);
      const pv = new THREE.Group(); pv.position.set(-7.5 + i * 5, 9.2, -9.4);
      b.position.y = -5.5; pv.add(b); g.add(pv); beams.push(pv);
    }
    // bunting
    const flags = [], fc = [0xfbbf24, 0x22c55e, 0x38bdf8, 0xf472b6, 0xef4444];
    for (let line = 0; line < 2; line++) for (let i = 0; i < 26; i++) {
      const x = -16 + i * 1.28, y = 7.6 - Math.sin((i / 25) * Math.PI) * -0.0 - line * 1.6 - Math.sin((i / 25) * Math.PI) * 0.9;
      const f = new THREE.Mesh(new THREE.ConeGeometry(0.34, 0.7, 3), new THREE.MeshBasicMaterial({ color: fc[(i + line) % 5], side: THREE.DoubleSide }));
      f.rotation.z = Math.PI; f.position.set(x, y, -6 - line * 1.5); g.add(f); flags.push(f);
    }
    // crowd rows
    const pts = [];
    for (let row = 0; row < 4; row++) for (let i = 0; i < 34; i++) pts.push({ x: -17 + i * 1.05 + (row % 2) * 0.5 + R() * 0.2, y: 0.0 + row * 0.12, z: -3.6 - row * 1.15, ph: R() * 6 });
    const upd = crowd(g, pts, [0xffffff, 0xf2e8cf, 0xf28c28, 0x16a34a, 0x2563eb, 0x7c3aed, 0xfacc15, 0xef4444], 9, 1.0);
    // signs
    const sc = [0xfbbf24, 0x22c55e, 0x60a5fa, 0xf472b6];
    for (let i = 0; i < 12; i++) {
      const x = -15 + i * 2.7 + R(), z = -4.8 - R() * 2.2;
      const s = new THREE.Group(); s.position.set(x, 2.3, z); g.add(s);
      box(s, 0.07, 1.6, 0.07, 0, 0, 0, 0, T.mat(0x6b4a2e));
      box(s, 1.2, 0.8, 0.06, 0, 0, 0.9, 0, new THREE.MeshBasicMaterial({ color: sc[i % 4] }));
      flags.push(s);
    }
    // fireworks
    const fw = [];
    for (let k = 0; k < 3; k++) {
      const N = 40, pos = new Float32Array(N * 3), vel = [];
      for (let i = 0; i < N; i++) { const a = R() * Math.PI * 2, b = Math.acos(2 * R() - 1), s = 2 + R() * 3; vel.push([Math.sin(b) * Math.cos(a) * s, Math.cos(b) * s, Math.sin(b) * Math.sin(a) * s]); }
      const geo = new THREE.BufferGeometry(); geo.setAttribute('position', new THREE.BufferAttribute(pos, 3));
      const mt = new THREE.PointsMaterial({ color: [0xffd166, 0x7ee787, 0xff6b9d][k], size: 0.7, transparent: true, opacity: 1, blending: THREE.AdditiveBlending, depthWrite: false, fog: false });
      const p = new THREE.Points(geo, mt); p.frustumCulled = false; g.add(p);
      fw.push({ p, geo, mt, vel, N, cx: -10 + k * 10, cy: 13 + k * 2, cz: -35, off: k * 2.1 });
    }
    return {
      group: g, bg: 0x2b1055, fog: [0x7a2d6b, 30, 90],
      light: { sky: 0xffc2a0, ground: 0x4a2a30, hi: 0.9, dir: 0xffe1b0, di: 0.95, pos: [-5, 9, 9] },
      update(t) {
        upd(t);
        beams.forEach((b, i) => { b.rotation.z = Math.sin(t * 0.7 + i * 1.3) * 0.5; b.rotation.x = Math.cos(t * 0.5 + i) * 0.15; });
        flags.forEach((f, i) => { f.rotation.x = Math.sin(t * 3 + i) * 0.12; });
        fw.forEach((w) => {
          const lt = ((t + w.off) % 4.2) / 4.2, pa = w.geo.attributes.position;
          for (let i = 0; i < w.N; i++) pa.setXYZ(i, w.cx + w.vel[i][0] * lt * 4, w.cy + w.vel[i][1] * lt * 4 - lt * lt * 8, w.cz + w.vel[i][2] * lt * 4);
          pa.needsUpdate = true; w.mt.opacity = lt < 0.12 ? 0 : Math.max(0, 1 - lt);
        });
      }
    };
  }

  // ------------------------------------------------------------------ DESI SADAK
  function road() {
    const g = new THREE.Group(), R = rnd(31);
    g.add(skyDome('#4aa8ea', '#a9d8f5', '#fdeccb'));
    const sun = new THREE.Mesh(new THREE.SphereGeometry(4, 20, 14), new THREE.MeshBasicMaterial({ color: 0xfff3b0, fog: false })); sun.position.set(-30, 28, -70); g.add(sun);
    const clouds = [];
    for (let i = 0; i < 7; i++) {
      const c = new THREE.Group(); c.position.set(-40 + i * 14, 20 + R() * 8, -60 - R() * 15);
      for (let j = 0; j < 4; j++) { const s = new THREE.Mesh(T.sphere(2 + R() * 2, 12, 10), new THREE.MeshBasicMaterial({ color: 0xffffff, fog: false })); s.position.set(j * 2.6, R(), 0); s.scale.y = 0.7; c.add(s); }
      g.add(c); clouds.push(c);
    }
    const asph = canvasTex(256, 256, (c, w, h) => {
      c.fillStyle = '#4b4b52'; c.fillRect(0, 0, w, h);
      for (let i = 0; i < 1400; i++) { c.fillStyle = 'rgba(255,255,255,' + R() * 0.07 + ')'; c.fillRect(R() * w, R() * h, 2, 2); }
      c.fillStyle = '#e8dc9a'; for (let i = 0; i < 2; i++) c.fillRect(i * 128 + 10, 118, 80, 10);
    }, [14, 6]);
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(70, 30), T.mat(0xffffff, { map: asph }));
    floor.rotation.x = -Math.PI / 2; floor.position.set(0, 0, 4); g.add(floor);
    const walk = new THREE.Mesh(new THREE.BoxGeometry(60, 0.35, 5), T.mat(0x9a8f86)); walk.position.set(0, 0.17, -8.2); g.add(walk);
    const curb = new THREE.Mesh(new THREE.BoxGeometry(60, 0.4, 0.4), T.mat(0xe6dccb)); curb.position.set(0, 0.2, -5.6); g.add(curb);
    const ground2 = new THREE.Mesh(new THREE.PlaneGeometry(80, 30), T.mat(0x7c746b)); ground2.rotation.x = -Math.PI / 2; ground2.position.set(0, -0.01, -20); g.add(ground2);

    // buildings
    const pal = ['#f2b8a2', '#f6e0a4', '#a9d6c7', '#b9c4f0', '#f0a8b8', '#e6c8f0'];
    for (let i = 0; i < 9; i++) {
      const h = 7 + R() * 9, w = 4.4 + R() * 1.4, x = -23 + i * 5.8, col = pal[i % pal.length];
      const wt = canvasTex(128, 256, (c, ww, hh) => {
        c.fillStyle = col; c.fillRect(0, 0, ww, hh);
        for (let y = 0; y < 6; y++) for (let xx = 0; xx < 3; xx++) {
          c.fillStyle = '#3b4a5a'; c.fillRect(14 + xx * 38, 14 + y * 40, 24, 28);
          c.fillStyle = R() > 0.7 ? '#ffe9a0' : '#6d8aa8'; c.fillRect(16 + xx * 38, 16 + y * 40, 20, 24);
          c.fillStyle = '#222'; c.fillRect(12 + xx * 38, 42 + y * 40, 28, 4);
        }
      });
      const b = new THREE.Mesh(new THREE.BoxGeometry(w, h, 4), T.mat(0xffffff, { map: wt })); b.position.set(x, h / 2, -12 - R() * 1.5); g.add(b);
      if (R() > 0.5) { const tank = new THREE.Mesh(T.cyl(0.7, 0.7, 1, 12), T.mat(0x2b6cb0)); tank.position.set(x, h + 0.5, b.position.z); g.add(tank); }
      const sg = new THREE.Mesh(new THREE.BoxGeometry(w * 0.8, 0.9, 0.2), T.mat([0xef4444, 0x22c55e, 0xf59e0b, 0x3b82f6][i % 4])); sg.position.set(x, 3.6, b.position.z + 2.1); g.add(sg);
    }
    // poles + wires
    const poleX = [-14, -4, 6, 16];
    poleX.forEach((x) => {
      const p = new THREE.Mesh(T.cyl(0.14, 0.18, 8.5, 8), T.mat(0x5a5a60)); p.position.set(x, 4.25, -6.5); g.add(p);
      const arm = new THREE.Mesh(T.box(2.6, 0.18, 0.18), T.mat(0x5a5a60)); arm.position.set(x, 8, -6.5); g.add(arm);
    });
    const wm = new THREE.LineBasicMaterial({ color: 0x1a1a1a });
    for (let w = 0; w < 3; w++) for (let i = 0; i < poleX.length - 1; i++) {
      const pts = []; for (let s = 0; s <= 12; s++) { const u = s / 12; pts.push(new THREE.Vector3(poleX[i] + (poleX[i + 1] - poleX[i]) * u + (w - 1) * 0.9, 8 - Math.sin(u * Math.PI) * 0.9, -6.5)); }
      g.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(pts), wm));
    }
    // chai tapri
    const tp = new THREE.Group(); tp.position.set(-8, 0.35, -7.5); g.add(tp);
    box(tp, 3, 1.3, 1.5, 0, 0, 0.65, 0, T.mat(0x8b5a2b));
    box(tp, 3.6, 0.15, 2.2, 0, 0, 2.6, 0.1, T.mat(0xd62839));
    [-1.6, 1.6].forEach((x) => box(tp, 0.1, 2, 0.1, 0, 0, 1.6, 0.8, T.mat(0x4a3322)).position.x = x);
    const ket = new THREE.Mesh(T.cyl(0.35, 0.45, 0.6, 14), T.mat(0xb8bcc4)); ket.position.set(-0.6, 1.6, 0); tp.add(ket);
    // cow
    const cow = new THREE.Group(); cow.position.set(8.5, 0.35, -7.0); cow.rotation.y = -0.5; g.add(cow);
    const cw = T.mat(0xf0e4cc), cb = T.mat(0x5a3a22);
    box(cow, 2.2, 1.1, 1.0, 0, 0, 1.4, 0, cw); box(cow, 0.7, 0.55, 0.8, 0, 0, 1.55, 0, cb).position.set(-0.3, 1.5, 0.1);
    box(cow, 0.8, 0.8, 0.7, 0, 0, 1.75, 0, cw).position.set(1.45, 1.75, 0);
    box(cow, 0.35, 0.3, 0.5, 0, 0, 1.5, 0, T.mat(0xe8a0a0)).position.set(1.9, 1.55, 0);
    [[-0.8, 0.35], [-0.8, -0.35], [0.8, 0.35], [0.8, -0.35]].forEach((p) => box(cow, 0.22, 0.9, 0.22, 0, 0, 0.45, 0, cw).position.set(p[0], 0.45, p[1]));
    const tail = new THREE.Group(); tail.position.set(-1.1, 1.8, 0); cow.add(tail);
    box(tail, 0.1, 0.9, 0.1, 0, 0, -0.45, 0, cb);
    // auto-rickshaw (drives back and forth)
    const auto = new THREE.Group(); g.add(auto);
    box(auto, 2.4, 0.9, 1.3, 0, 0, 0.8, 0, T.mat(0x1f8a3a));
    box(auto, 1.5, 0.12, 1.4, 0, 0, 1.9, 0, T.mat(0x111111)).position.x = -0.3;
    box(auto, 1.0, 0.9, 1.2, 0, 0, 1.4, 0, T.mat(0xfbbf24)).position.x = 0.9;
    box(auto, 0.1, 1.0, 1.3, 0, 0, 1.4, 0, T.mat(0xfacc15)).position.x = -1.3;
    [[0.9, 0.5], [-0.9, 0.5], [-0.9, -0.5]].forEach((p) => { const w = new THREE.Mesh(T.cyl(0.35, 0.35, 0.22, 14), T.mat(0x111111)); w.rotation.x = Math.PI / 2; w.position.set(p[0], 0.35, p[1]); auto.add(w); });
    // onlookers
    const pts = []; for (let i = 0; i < 28; i++) pts.push({ x: -20 + i * 1.5 + R(), y: 0.35, z: -9.5 - R() * 1.2, ph: R() * 6 });
    const upd = crowd(g, pts, [0xffffff, 0xf28c28, 0x2563eb, 0x16a34a, 0xd62839, 0xfacc15], 3, 0.95);
    return {
      group: g, bg: 0x8fc8f0, fog: [0xcfe8f5, 35, 95],
      light: { sky: 0xdff1ff, ground: 0x8a7a68, hi: 1.05, dir: 0xfff3d0, di: 1.05, pos: [-6, 12, 8] },
      update(t) {
        upd(t);
        clouds.forEach((c, i) => { c.position.x += 0.004 * (1 + i * 0.1); if (c.position.x > 50) c.position.x = -50; });
        tail.rotation.z = Math.sin(t * 3) * 0.4;
        const ax = ((t * 2.2) % 56) - 28; auto.position.set(ax, 0, -4.3); auto.rotation.y = 0;
      }
    };
  }

  const built = [];
  const builders = [hall, rally, road];
  return {
    blob: blobTexture,
    get(i) { if (!built[i]) built[i] = builders[i](); return built[i]; },
    canvasTex
  };
})();
