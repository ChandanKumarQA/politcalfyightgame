/**
 * NETA FIGHT – 3D Arenas Engine
 * 3 Detailed 3D Environments with dynamic lighting, props & animated crowds
 */
class ArenaManager {
  constructor(scene) {
    this.scene = scene;
    this.currentArenaGroup = null;
    this.crowdSpectators = [];
    this.lights = [];
  }

  clear() {
    if (this.currentArenaGroup) {
      this.scene.remove(this.currentArenaGroup);
      this.currentArenaGroup = null;
    }
    this.lights.forEach(l => this.scene.remove(l));
    this.lights = [];
    this.crowdSpectators = [];
  }

  loadArena(arenaIndex) {
    this.clear();
    const group = new THREE.Group();
    this.currentArenaGroup = group;
    this.scene.add(group);

    switch(arenaIndex) {
      case 0:
        this.buildSansadArena(group);
        break;
      case 1:
        this.buildJantarArena(group);
        break;
      case 2:
      default:
        this.buildDesiStreetArena(group);
        break;
    }
  }

  // --- ARENA 1: SANSAD ARENA (Parliament Hall) ---
  buildSansadArena(group) {
    // Ambient & Directional Lighting
    const ambLight = new THREE.AmbientLight(0xfff3e0, 0.7);
    const dirLight = new THREE.DirectionalLight(0xfffaed, 0.9);
    dirLight.position.set(0, 18, 12);
    this.scene.add(ambLight, dirLight);
    this.lights.push(ambLight, dirLight);

    // Red Carpet Floor with ceremonial circular rings
    const floorGeom = new THREE.CylinderGeometry(24, 24, 0.5, 36);
    const floorMat = new THREE.MeshLambertMaterial({ color: 0x8b0000 });
    const floor = new THREE.Mesh(floorGeom, floorMat);
    floor.position.y = -0.25;
    group.add(floor);

    // Concentric Lotus Mandala Dais Rings (Royal Arena Center Dais)
    const mandalaColors = [0xffd700, 0xe65100, 0x1565c0, 0xc2185b, 0xffb300];
    const ringRadii = [[2, 2.8], [2.8, 3.8], [3.8, 5.0], [5.0, 6.4], [6.4, 7.8]];
    ringRadii.forEach(([inner, outer], idx) => {
      const mGeom = new THREE.RingGeometry(inner, outer, 36);
      const mMat = new THREE.MeshBasicMaterial({ color: mandalaColors[idx % mandalaColors.length], side: THREE.DoubleSide });
      const mMesh = new THREE.Mesh(mGeom, mMat);
      mMesh.rotation.x = -Math.PI / 2;
      mMesh.position.y = 0.01 + idx * 0.002;
      group.add(mMesh);
    });

    // Circular Diya Oil Lamps around dais perimeter
    const numDiyas = 20;
    const diyaGeom = new THREE.CylinderGeometry(0.18, 0.12, 0.12, 10);
    const diyaMat = new THREE.MeshLambertMaterial({ color: 0x8d6e63 });
    const flameGeom = new THREE.ConeGeometry(0.08, 0.18, 8);
    const flameMat = new THREE.MeshBasicMaterial({ color: 0xffa000 });
    for (let i = 0; i < numDiyas; i++) {
      const ang = (i / numDiyas) * Math.PI * 2;
      const dx = Math.sin(ang) * 8.0;
      const dz = Math.cos(ang) * 8.0;
      const dMesh = new THREE.Mesh(diyaGeom, diyaMat);
      dMesh.position.set(dx, 0.06, dz);
      group.add(dMesh);
      const fMesh = new THREE.Mesh(flameGeom, flameMat);
      fMesh.position.set(dx, 0.18, dz);
      group.add(fMesh);
    }

    // Classical Semicircular Pillars in background
    const colGeom = new THREE.CylinderGeometry(0.55, 0.6, 9, 16);
    const colMat = new THREE.MeshLambertMaterial({ color: 0xf5f5f5 });
    const numCols = 12;
    for (let i = 0; i < numCols; i++) {
      const angle = (i / (numCols - 1)) * Math.PI - Math.PI / 2;
      const x = Math.sin(angle) * 16;
      const z = -Math.cos(angle) * 12 - 4;
      const col = new THREE.Mesh(colGeom, colMat);
      col.position.set(x, 4.5, z);
      group.add(col);

      // Capital & Base
      const cap = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.5, 1.4), colMat);
      cap.position.set(x, 9, z);
      group.add(cap);
    }

    // Wooden Speaker's Podium in back center
    const podiumGeom = new THREE.BoxGeometry(3.5, 2.5, 1.8);
    const woodMat = new THREE.MeshLambertMaterial({ color: 0x3e2723 });
    const podium = new THREE.Mesh(podiumGeom, woodMat);
    podium.position.set(0, 1.25, -7);
    group.add(podium);

    // Central Ashoka / Tricolor Back Wall
    const wallGeom = new THREE.CylinderGeometry(20, 20, 12, 32, 1, true, -Math.PI / 2, Math.PI);
    const wallMat = new THREE.MeshLambertMaterial({ color: 0x2b1d0c, side: THREE.BackSide });
    const wall = new THREE.Mesh(wallGeom, wallMat);
    wall.position.set(0, 6, -3);
    group.add(wall);

    // Tiered Audience Galleries with 3D Spectators
    this.createAudienceTier(group, 0, -8, 14, 28);
  }

  // --- ARENA 2: JANTAR ARENA (Protest Ground) ---
  buildJantarArena(group) {
    // Warm Dusk Lighting
    const ambLight = new THREE.AmbientLight(0xffab91, 0.65);
    const sunsetLight = new THREE.DirectionalLight(0xff7043, 1.1);
    sunsetLight.position.set(-10, 12, 8);
    this.scene.add(ambLight, sunsetLight);
    this.lights.push(ambLight, sunsetLight);

    // Stage Ground (Concrete Plinth)
    const floorGeom = new THREE.BoxGeometry(38, 0.6, 26);
    const concreteMat = new THREE.MeshLambertMaterial({ color: 0x616161 });
    const floor = new THREE.Mesh(floorGeom, concreteMat);
    floor.position.y = -0.3;
    group.add(floor);

    // Metal Police Barricades with Yellow/Black stripes
    const barricadeGeom = new THREE.BoxGeometry(4.2, 1.6, 0.25);
    const barMat = new THREE.MeshLambertMaterial({ color: 0xfbc02d });
    [-12, -7, -2, 3, 8, 13].forEach(x => {
      const b = new THREE.Mesh(barricadeGeom, barMat);
      b.position.set(x, 0.8, -6);
      group.add(b);

      // Warning stripes
      const stripe = new THREE.Mesh(new THREE.BoxGeometry(4.2, 0.3, 0.28), new THREE.MeshBasicMaterial({ color: 0x212121 }));
      stripe.position.set(x, 0.8, -6);
      group.add(stripe);
    });

    // Loudspeaker Towers & Truss
    [-14, 14].forEach(x => {
      const truss = new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.2, 10, 8), new THREE.MeshLambertMaterial({ color: 0x9e9e9e }));
      truss.position.set(x, 5, -5.5);
      group.add(truss);

      // 3 Cone Loudspeakers
      [6, 7.5, 9].forEach(y => {
        const cone = new THREE.Mesh(new THREE.ConeGeometry(0.4, 0.9, 10), new THREE.MeshLambertMaterial({ color: 0x424242 }));
        cone.rotation.z = Math.PI / 2 * (x > 0 ? 1 : -1);
        cone.position.set(x + (x > 0 ? -0.4 : 0.4), y, -5.5);
        group.add(cone);
      });
    });

    // Protest Banners with Funny Satire Slogans
    const bannerGeom = new THREE.BoxGeometry(10, 2.2, 0.1);
    const bannerMat = new THREE.MeshLambertMaterial({ color: 0xd32f2f });
    const banner = new THREE.Mesh(bannerGeom, bannerMat);
    banner.position.set(0, 7.5, -6.5);
    group.add(banner);

    // Crowd with Waving Flags in background
    this.createAudienceTier(group, 0, -8, 15, 32);
  }

  // --- ARENA 3: DESI STREET (Evening Indian Market) ---
  buildDesiStreetArena(group) {
    // Vibrant Evening Street Lighting
    const ambLight = new THREE.AmbientLight(0xffe0b2, 0.6);
    const streetLight = new THREE.DirectionalLight(0xffcc80, 0.95);
    streetLight.position.set(8, 15, 6);
    this.scene.add(ambLight, streetLight);
    this.lights.push(ambLight, streetLight);

    // Asphalt Road with white dashed divider
    const roadGeom = new THREE.BoxGeometry(40, 0.5, 24);
    const roadMat = new THREE.MeshLambertMaterial({ color: 0x37474f });
    const road = new THREE.Mesh(roadGeom, roadMat);
    road.position.y = -0.25;
    group.add(road);

    // Road Divider Dashes
    const dashGeom = new THREE.BoxGeometry(2, 0.05, 0.3);
    const whiteMat = new THREE.MeshBasicMaterial({ color: 0xffffff });
    for (let x = -16; x <= 16; x += 4) {
      const dash = new THREE.Mesh(dashGeom, whiteMat);
      dash.position.set(x, 0.02, 0);
      group.add(dash);
    }

    // Sidewalk Kerb
    const kerbGeom = new THREE.BoxGeometry(40, 0.4, 4);
    const kerbMat = new THREE.MeshLambertMaterial({ color: 0x78909c });
    const kerb = new THREE.Mesh(kerbGeom, kerbMat);
    kerb.position.set(0, 0.15, -6.5);
    group.add(kerb);

    // Colorful Shopfronts along the street
    const shops = [
      { name: "CHAI & SAMOSA", color: 0xd84315, x: -12, w: 7 },
      { name: "GUPTA SWEETS", color: 0xf57f17, x: -4, w: 8 },
      { name: "SUPER LAUNDRY", color: 0x1976d2, x: 5, w: 8 },
      { name: "PAAN BHANDAR", color: 0x2e7d32, x: 13, w: 6 }
    ];

    shops.forEach(s => {
      const shopGeom = new THREE.BoxGeometry(s.w, 5.5, 4);
      const sMat = new THREE.MeshLambertMaterial({ color: s.color });
      const shop = new THREE.Mesh(shopGeom, sMat);
      shop.position.set(s.x, 2.9, -9.5);
      group.add(shop);

      // Awning / Chajja
      const awningGeom = new THREE.BoxGeometry(s.w + 0.6, 0.3, 2.2);
      const awMat = new THREE.MeshLambertMaterial({ color: 0xffffff });
      const awning = new THREE.Mesh(awningGeom, awMat);
      awning.position.set(s.x, 4.8, -7.5);
      awning.rotation.x = 0.25;
      group.add(awning);
    });

    // 3D Auto Rickshaw parked on the side
    this.createAutoRickshaw(group, 13, 0.1, -4.5);

    // Street Lamps with warm lantern glow
    [-14, 0, 14].forEach(x => {
      const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.14, 7, 8), new THREE.MeshLambertMaterial({ color: 0x212121 }));
      pole.position.set(x, 3.5, -5.5);
      group.add(pole);

      const lamp = new THREE.Mesh(new THREE.SphereGeometry(0.35, 10, 10), new THREE.MeshBasicMaterial({ color: 0xfff59d }));
      lamp.position.set(x, 7.2, -5.2);
      group.add(lamp);

      const pointLight = new THREE.PointLight(0xfff59d, 0.6, 12);
      pointLight.position.set(x, 7.2, -5.2);
      this.scene.add(pointLight);
      this.lights.push(pointLight);
    });

    // Festive Fairy Lights string overhead
    const wireGeom = new THREE.CylinderGeometry(0.02, 0.02, 36, 12);
    const wire = new THREE.Mesh(wireGeom, new THREE.MeshBasicMaterial({ color: 0x333333 }));
    wire.rotation.z = Math.PI / 2;
    wire.position.set(0, 6.8, -5.5);
    group.add(wire);

    // Colorful light bulbs
    const bulbColors = [0xff1744, 0x00e676, 0xffea00, 0x2979ff, 0xff9100];
    for (let x = -16; x <= 16; x += 1.2) {
      const bColor = bulbColors[Math.floor(Math.random() * bulbColors.length)];
      const bulb = new THREE.Mesh(new THREE.SphereGeometry(0.12, 6, 6), new THREE.MeshBasicMaterial({ color: bColor }));
      bulb.position.set(x, 6.75 - Math.sin((x + 16) / 32 * Math.PI) * 0.4, -5.5);
      group.add(bulb);
    }

    // Street Audience
    this.createAudienceTier(group, 0, -6.8, 12, 26);
  }

  // --- Dynamic Animated Audience ---
  createAudienceTier(group, centerX, centerZ, width, count) {
    const crowdGroup = new THREE.Group();
    group.add(crowdGroup);

    const colors = [0xff7043, 0xffca28, 0x42a5f5, 0x66bb6a, 0xab47bc, 0xffffff, 0x263238];
    for (let i = 0; i < count; i++) {
      const row = Math.floor(i / (count / 2));
      const col = i % (count / 2);
      const x = (col / (count / 2 - 1) - 0.5) * width * 1.8 + (Math.random() * 0.6 - 0.3);
      const z = centerZ - row * 2.2 - (Math.random() * 0.5);
      const y = 0.5 + row * 1.1;

      // Stylized cartoon spectator (Body + Head)
      const spectator = new THREE.Group();
      spectator.position.set(x, y, z);

      const color = colors[i % colors.length];
      const body = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.32, 1.1, 8), new THREE.MeshLambertMaterial({ color }));
      body.position.y = 0.55;
      spectator.add(body);

      const head = new THREE.Mesh(new THREE.SphereGeometry(0.25, 8, 8), new THREE.MeshLambertMaterial({ color: 0xdfa070 }));
      head.position.y = 1.35;
      spectator.add(head);

      crowdGroup.add(spectator);
      this.crowdSpectators.push({
        group: spectator,
        phase: Math.random() * Math.PI * 2,
        baseY: y
      });
    }
  }

  // --- 3D Auto Rickshaw Prop ---
  createAutoRickshaw(group, x, y, z) {
    const auto = new THREE.Group();
    auto.position.set(x, y, z);
    auto.rotation.y = -Math.PI / 6;
    group.add(auto);

    // Green Lower Body
    const lowerBody = new THREE.Mesh(new THREE.BoxGeometry(2.4, 1.1, 1.4), new THREE.MeshLambertMaterial({ color: 0x2e7d32 }));
    lowerBody.position.y = 0.75;
    auto.add(lowerBody);

    // Yellow Canopy Roof
    const roof = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.9, 1.4), new THREE.MeshLambertMaterial({ color: 0xfbc02d }));
    roof.position.y = 1.7;
    auto.add(roof);

    // Windshield
    const glass = new THREE.Mesh(new THREE.PlaneGeometry(1.2, 0.6), new THREE.MeshBasicMaterial({ color: 0x81d4fa, side: THREE.DoubleSide }));
    glass.position.set(-1.21, 1.5, 0);
    glass.rotation.y = Math.PI / 2;
    auto.add(glass);

    // 3 Black Wheels
    const wheelGeom = new THREE.CylinderGeometry(0.3, 0.3, 0.2, 10);
    const wheelMat = new THREE.MeshLambertMaterial({ color: 0x111111 });
    // Front wheel
    const w1 = new THREE.Mesh(wheelGeom, wheelMat);
    w1.rotation.x = Math.PI / 2;
    w1.position.set(-0.9, 0.3, 0);
    auto.add(w1);
    // Rear wheels
    [-0.7, 0.7].forEach(zOff => {
      const w2 = new THREE.Mesh(wheelGeom, wheelMat);
      w2.rotation.x = Math.PI / 2;
      w2.position.set(0.9, 0.3, zOff);
      auto.add(w2);
    });
  }

  // Update animated audience spectators (cheering / bobbing)
  update(time) {
    this.crowdSpectators.forEach(s => {
      const bounce = Math.sin(time * 5 + s.phase);
      s.group.position.y = s.baseY + Math.max(0, bounce * 0.2);
      s.group.rotation.y = Math.sin(time * 2 + s.phase) * 0.15;
    });
  }
}
