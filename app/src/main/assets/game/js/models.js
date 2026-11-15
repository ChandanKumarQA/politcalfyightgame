/**
 * NETA FIGHT – 3D Character Models & Animation Rigging Engine
 * Procedural stylized 3D meshes with complete skeletal hierarchy!
 */
const CharacterRoster = [
  {
    id: 0,
    name: "MITRA MAN",
    title: "Balanced Power Fighter",
    party: "Team Vaada",
    avatar: "🧔🏻‍♂️",
    pwr: 80, spd: 70, def: 75,
    color: 0xff9933,
    skinColor: 0xdfa070,
    clothesColor: 0xffffff,
    vestColor: 0xe65100,
    hairColor: 0xeeeeee,
    hat: "none",
    glasses: true,
    beard: "full",
    superName: "MITRA CHAKRA",
    superDesc: "Summons a spinning flaming tricolor chakra that tears through opponent's defense!",
    quote: "Mitrooon! The verdict of the arena is absolute!"
  },
  {
    id: 1,
    name: "YATRA BOY",
    title: "Agile Speed Striker",
    party: "Team Yatra",
    avatar: "🏃🏻",
    pwr: 70, spd: 90, def: 65,
    color: 0x2e9bff,
    skinColor: 0xe8b88a,
    clothesColor: 0xffffff,
    vestColor: 0x1976d2,
    hairColor: 0x212121,
    hat: "none",
    glasses: false,
    beard: "stubble",
    superName: "ALOO SE SONA",
    superDesc: "Rolls out a high-tech golden machine that zaps enemy into pure gold ingots!",
    quote: "This is a 4,000 kilometer uninterrupted victory streak!"
  },
  {
    id: 2,
    name: "DEEDEE QUEEN",
    title: "Kick Specialist",
    party: "Trina Sena",
    avatar: "🧕🏻",
    pwr: 75, spd: 85, def: 65,
    color: 0x22b35e,
    skinColor: 0xd99b66,
    clothesColor: 0xf5f5f5,
    vestColor: 0x2e7d32,
    hairColor: 0x3e2723,
    hat: "shawl",
    glasses: false,
    beard: "none",
    superName: "KHELA HOBE BLAST",
    superDesc: "Unleashes an intense green sonic shockwave accompanied by flying footballs!",
    quote: "Khela hobe, and the match is mine!"
  },
  {
    id: 3,
    name: "TARIFF TAU",
    title: "Heavyweight Brawler",
    party: "MAGA Morcha",
    avatar: "👱🏻‍♂️",
    pwr: 95, spd: 50, def: 85,
    color: 0xe0262f,
    skinColor: 0xf2b58d,
    clothesColor: 0x212121,
    vestColor: 0xd50000,
    hairColor: 0xffd54f,
    hat: "blonde_comb",
    glasses: false,
    beard: "none",
    superName: "WALL SLAM",
    superDesc: "Summons a massive concrete border wall that collapses directly onto the rival!",
    quote: "Nobody builds combos better than me, believe me!"
  },
  {
    id: 4,
    name: "DOZER DADA",
    title: "Iron Defense Tank",
    party: "Bhagwa Front",
    avatar: "🧑🏻‍🦲",
    pwr: 85, spd: 55, def: 95,
    color: 0xf05a00,
    skinColor: 0xdea070,
    clothesColor: 0xff6d00,
    vestColor: 0xbf360c,
    hairColor: 0x212121,
    hat: "bald",
    glasses: false,
    beard: "none",
    superName: "BULLDOZER CHARGE",
    superDesc: "Spawns a roaring mini yellow bulldozer that sweeps across the arena floor!",
    quote: "All illegal attacks will be demolished on sight!"
  },
  {
    id: 5,
    name: "TOLL MASTER",
    title: "Highway Tactician",
    party: "Gati Morcha",
    avatar: "👨🏻‍🦳",
    pwr: 80, spd: 75, def: 75,
    color: 0xff9a1f,
    skinColor: 0xe2a878,
    clothesColor: 0xffffff,
    vestColor: 0x5d4037,
    hairColor: 0x757575,
    hat: "short_grey",
    glasses: true,
    beard: "none",
    superName: "FASTAG BLITZ",
    superDesc: "Automated high-speed barrier scanners bombard the opponent with supersonic tolls!",
    quote: "No vehicle or fighter passes through this arena without paying the toll!"
  },
  {
    id: 6,
    name: "RAIL RAJA",
    title: "Acrobatic Rusher",
    party: "Lal Paltan",
    avatar: "🧔🏽",
    pwr: 75, spd: 80, def: 70,
    color: 0x16a34a,
    skinColor: 0xbc7e4c,
    clothesColor: 0xffffff,
    vestColor: 0x33691e,
    hairColor: 0x212121,
    hat: "gamchha",
    glasses: false,
    beard: "mustache_curly",
    superName: "CHOO CHOO EXPRESS",
    superDesc: "A roaring steam locomotive rushes across the tracks tossing the enemy sky-high!",
    quote: "Samast janta ka dhyan akarsheet ho, Super Move aa raha hai!"
  },
  {
    id: 7,
    name: "GHAR MANTRI",
    title: "Counter Specialist",
    party: "Chanakya Dal",
    avatar: "👨🏻‍🦲",
    pwr: 85, spd: 65, def: 85,
    color: 0x90a4ae,
    skinColor: 0xdfa070,
    clothesColor: 0xffffff,
    vestColor: 0x37474f,
    hairColor: 0x616161,
    hat: "bald_sides",
    glasses: true,
    beard: "goatee",
    superName: "MASTER PLAN SHIELD",
    superDesc: "Constructs an impenetrable steel vault door that rebounds with shockwave force!",
    quote: "Everything is proceeding exactly according to the strategic master plan."
  },
  {
    id: 8,
    name: "FACT BHAI",
    title: "Combo Specialist",
    party: "Digital Sabha",
    avatar: "🧑🏻",
    pwr: 70, spd: 90, def: 70,
    color: 0x22d3ee,
    skinColor: 0xe8b88a,
    clothesColor: 0x212121,
    vestColor: 0x00838f,
    hairColor: 0x212121,
    hat: "headphones",
    glasses: false,
    beard: "stubble",
    superName: "FACT CHECK LASER",
    superDesc: "A cybernetic magnifying glass aims a high-voltage truth laser directly at the target!",
    quote: "Your claims have been fact-checked and permanently debunked!"
  },
  {
    id: 9,
    name: "CHAI BOSS",
    title: "The Supreme Master",
    party: "Ahimsa Front",
    avatar: "👑",
    pwr: 95, spd: 80, def: 90,
    color: 0xffd700,
    skinColor: 0xdca272,
    clothesColor: 0xfff8e1,
    vestColor: 0xff6f00,
    hairColor: 0xf5f5f5,
    hat: "round_glasses",
    glasses: true,
    beard: "mustache_white",
    superName: "SATYAGRAHA SHOCK",
    superDesc: "Supreme ancient combat shakti wave that reverberates through the entire arena!",
    quote: "Truth and strength triumph over everything in this arena!"
  }
];

class FighterModel {
  constructor(charData, isP2 = false) {
    this.charData = charData;
    this.isP2 = isP2;
    this.group = new THREE.Group();
    this.bones = {};
    this.buildRig();
  }

  buildRig() {
    const cd = this.charData;
    const skinMat = new THREE.MeshLambertMaterial({ color: cd.skinColor });
    const clothesMat = new THREE.MeshLambertMaterial({ color: cd.clothesColor });
    const vestMat = new THREE.MeshLambertMaterial({ color: cd.vestColor });
    const hairMat = new THREE.MeshLambertMaterial({ color: cd.hairColor });
    const darkMat = new THREE.MeshLambertMaterial({ color: 0x111111 });
    const goldMat = new THREE.MeshLambertMaterial({ color: 0xffd700 });

    // Root node
    const root = new THREE.Group();
    this.group.add(root);
    this.bones.root = root;

    // Hips / Pelvis
    const hips = new THREE.Group();
    hips.position.y = 1.6;
    root.add(hips);
    this.bones.hips = hips;

    const hipsGeom = new THREE.CylinderGeometry(0.40, 0.36, 0.45, 14);
    const hipsMesh = new THREE.Mesh(hipsGeom, clothesMat);
    hips.add(hipsMesh);

    // Flared Indian Kurta Skirt / Drape (Hangs down over thighs to mid-thigh)
    const skirtGeom = new THREE.CylinderGeometry(0.42, 0.62, 0.72, 16, 1, true);
    const skirtMesh = new THREE.Mesh(skirtGeom, clothesMat);
    skirtMesh.position.y = -0.28;
    hips.add(skirtMesh);

    // Torso / Spine
    const torso = new THREE.Group();
    torso.position.y = 0.25;
    hips.add(torso);
    this.bones.torso = torso;

    const torsoGeom = new THREE.BoxGeometry(0.85, 0.95, 0.55);
    const torsoMesh = new THREE.Mesh(torsoGeom, clothesMat);
    torsoMesh.position.y = 0.48;
    torso.add(torsoMesh);

    // Vest / Waistcoat overlay
    const vestGeom = new THREE.BoxGeometry(0.88, 0.92, 0.58);
    const vestMesh = new THREE.Mesh(vestGeom, vestMat);
    vestMesh.position.y = 0.48;
    torso.add(vestMesh);

    // Mandarin / Nehru Collar
    const collarGeom = new THREE.CylinderGeometry(0.24, 0.25, 0.16, 14);
    const collarMesh = new THREE.Mesh(collarGeom, vestMat);
    collarMesh.position.y = 0.98;
    torso.add(collarMesh);

    // Neck & Head
    const neck = new THREE.Group();
    neck.position.y = 1.0;
    torso.add(neck);
    this.bones.neck = neck;

    const head = new THREE.Group();
    neck.add(head);
    this.bones.head = head;

    // 100% Procedural 3D Caricature Head & Face Sculpting (Pure 3D meshes, 0% image textures)
    this.buildProceduralHead(head, torso, cd, skinMat, hairMat, darkMat, goldMat, clothesMat, vestMat);

    // --- ARMS & SHOULDERS ---
    [-1, 1].forEach(side => {
      const shoulder = new THREE.Group();
      shoulder.position.set(side * 0.52, 0.82, 0);
      torso.add(shoulder);

      const upperArm = new THREE.Group();
      shoulder.add(upperArm);

      const upperMesh = new THREE.Mesh(new THREE.CylinderGeometry(0.14, 0.12, 0.52, 10), vestMat);
      upperMesh.position.y = -0.26;
      upperArm.add(upperMesh);

      const elbow = new THREE.Group();
      elbow.position.y = -0.52;
      upperArm.add(elbow);

      const foreMesh = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.11, 0.48, 10), skinMat);
      foreMesh.position.y = -0.24;
      elbow.add(foreMesh);

      // Hand / Fist
      const hand = new THREE.Mesh(new THREE.SphereGeometry(0.14, 10, 8), skinMat);
      hand.position.y = -0.52;
      elbow.add(hand);

      if (side === -1) {
        this.bones.leftShoulder = shoulder;
        this.bones.leftUpperArm = upperArm;
        this.bones.leftElbow = elbow;
        this.bones.leftHand = hand;
      } else {
        this.bones.rightShoulder = shoulder;
        this.bones.rightUpperArm = upperArm;
        this.bones.rightElbow = elbow;
        this.bones.rightHand = hand;
      }
    });

    // --- LEGS & FEET ---
    [-1, 1].forEach(side => {
      const hipJoint = new THREE.Group();
      hipJoint.position.set(side * 0.22, -0.2, 0);
      hips.add(hipJoint);

      const thigh = new THREE.Group();
      hipJoint.add(thigh);

      const thighMesh = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.14, 0.65, 10), clothesMat);
      thighMesh.position.y = -0.32;
      thigh.add(thighMesh);

      const knee = new THREE.Group();
      knee.position.y = -0.65;
      thigh.add(knee);

      const shinMesh = new THREE.Mesh(new THREE.CylinderGeometry(0.14, 0.12, 0.65, 10), clothesMat);
      shinMesh.position.y = -0.32;
      knee.add(shinMesh);

      // Churidar horizontal wrinkle rings
      [-0.44, -0.50, -0.56].forEach(ry => {
        const ring = new THREE.Mesh(new THREE.TorusGeometry(0.14, 0.024, 6, 12), clothesMat);
        ring.rotation.x = Math.PI / 2;
        ring.position.y = ry;
        knee.add(ring);
      });

      // Traditional Indian Mojari / Peshawari Footwear
      const shoeGroup = new THREE.Group();
      shoeGroup.position.set(0, -0.68, 0.06);
      knee.add(shoeGroup);

      const shoeColor = (cd.id === 0) ? 0xf5f5f5 : (cd.id === 1) ? 0x5d4037 : 0x3e2723;
      const shoeMat = new THREE.MeshLambertMaterial({ color: shoeColor });
      const soleMat = new THREE.MeshLambertMaterial({ color: 0x1a1a1a });

      const soleMesh = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.04, 0.48), soleMat);
      soleMesh.position.y = -0.06;
      shoeGroup.add(soleMesh);

      const shoeUpper = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.12, 0.44), shoeMat);
      shoeGroup.add(shoeUpper);

      // Pointed upturned toe tip
      const toeTip = new THREE.Mesh(new THREE.ConeGeometry(0.08, 0.16, 8), shoeMat);
      toeTip.rotation.x = Math.PI / 3;
      toeTip.position.set(0, 0.04, 0.26);
      shoeGroup.add(toeTip);

      if (side === -1) {
        this.bones.leftHip = hipJoint;
        this.bones.leftThigh = thigh;
        this.bones.leftKnee = knee;
      } else {
        this.bones.rightHip = hipJoint;
        this.bones.rightThigh = thigh;
        this.bones.rightKnee = knee;
      }
    });
  }

  // 100% Procedural 3D Head Sculptor (Zero Image Textures, pure Three.js meshes)
  buildProceduralHead(head, torso, cd, skinMat, hairMat, darkMat, goldMat, clothesMat, vestMat) {
    const whiteMat = new THREE.MeshLambertMaterial({ color: 0xffffff });
    const pupilMat = new THREE.MeshLambertMaterial({ color: 0x111111 });
    const lipMat = new THREE.MeshLambertMaterial({ color: 0xc87565 });
    const redMat = new THREE.MeshLambertMaterial({ color: 0xd50000 });
    const saffronMat = new THREE.MeshLambertMaterial({ color: 0xff6d00 });
    const silverWireMat = new THREE.MeshLambertMaterial({ color: 0xb0bec5 });
    const whiteHairMat = new THREE.MeshLambertMaterial({ color: 0xf5f5f5 });
    const greyHairMat = new THREE.MeshLambertMaterial({ color: 0x757575 });
    const darkHairMat = new THREE.MeshLambertMaterial({ color: 0x212121 });
    const blondeHairMat = new THREE.MeshLambertMaterial({ color: 0xf9cb60 });

    // 1. Skull / Cranium (Curved 3D Head Dome)
    const craniumGeom = new THREE.SphereGeometry(0.38, 16, 14);
    const cranium = new THREE.Mesh(craniumGeom, skinMat);
    cranium.position.set(0, 0.44, 0.0);
    cranium.scale.set(0.96, 1.10, 1.02);
    head.add(cranium);

    // 2. Jaw & Chin (Sculpted lower facial structure)
    const jawGeom = new THREE.CylinderGeometry(0.34, 0.24, 0.36, 12);
    const jaw = new THREE.Mesh(jawGeom, skinMat);
    jaw.position.set(0, 0.18, 0.05);
    head.add(jaw);

    const chinGeom = new THREE.SphereGeometry(0.13, 10, 8);
    const chin = new THREE.Mesh(chinGeom, skinMat);
    chin.position.set(0, 0.06, 0.18);
    chin.scale.set(1.0, 0.8, 1.0);
    head.add(chin);

    // 3. Cheekbones (Volumetric facial contours for 3D depth)
    [-0.20, 0.20].forEach(x => {
      const cheekGeom = new THREE.SphereGeometry(0.12, 10, 8);
      const cheek = new THREE.Mesh(cheekGeom, skinMat);
      cheek.position.set(x, 0.28, 0.20);
      cheek.scale.set(0.9, 0.8, 1.1);
      head.add(cheek);
    });

    // 4. Brow Ridge (Forehead shelf above eyes)
    const browRidgeGeom = new THREE.CylinderGeometry(0.045, 0.045, 0.46, 8);
    const browRidge = new THREE.Mesh(browRidgeGeom, skinMat);
    browRidge.rotation.z = Math.PI / 2;
    browRidge.position.set(0, 0.45, 0.28);
    browRidge.scale.set(1.0, 0.8, 1.0);
    head.add(browRidge);

    // 5. 3D Eyes (Sclera + Pupil + Specular Highlight Glint + Eyelid fold)
    [-0.13, 0.13].forEach(x => {
      // Eyeball
      const eyeGeom = new THREE.SphereGeometry(0.075, 12, 10);
      const eyeball = new THREE.Mesh(eyeGeom, whiteMat);
      eyeball.position.set(x, 0.38, 0.27);
      head.add(eyeball);

      // Pupil
      const pupilGeom = new THREE.SphereGeometry(0.040, 8, 8);
      const pupil = new THREE.Mesh(pupilGeom, pupilMat);
      pupil.position.set(x, 0.38, 0.33);
      pupil.scale.set(1.0, 1.0, 0.4);
      head.add(pupil);

      // Specular highlight glint
      const glintGeom = new THREE.SphereGeometry(0.013, 6, 6);
      const glint = new THREE.Mesh(glintGeom, whiteMat);
      glint.position.set(x + 0.015, 0.395, 0.355);
      head.add(glint);

      // Eyelid crease
      const lidGeom = new THREE.CylinderGeometry(0.02, 0.02, 0.14, 6);
      const lid = new THREE.Mesh(lidGeom, skinMat);
      lid.rotation.z = Math.PI / 2;
      lid.position.set(x, 0.44, 0.31);
      head.add(lid);
    });

    // 6. 3D Sculpted Nose (Bridge + Bulb Tip + Nostril Wings)
    const noseBridgeGeom = new THREE.CylinderGeometry(0.032, 0.050, 0.16, 8);
    const noseBridge = new THREE.Mesh(noseBridgeGeom, skinMat);
    noseBridge.position.set(0, 0.32, 0.34);
    noseBridge.rotation.x = -0.18;
    head.add(noseBridge);

    const noseTipGeom = new THREE.SphereGeometry(0.062, 10, 10);
    const noseTip = new THREE.Mesh(noseTipGeom, skinMat);
    noseTip.position.set(0, 0.23, 0.38);
    noseTip.scale.set(1.0, 0.9, 1.1);
    head.add(noseTip);

    [-0.055, 0.055].forEach(nx => {
      const wingGeom = new THREE.SphereGeometry(0.035, 8, 8);
      const wing = new THREE.Mesh(wingGeom, skinMat);
      wing.position.set(nx, 0.22, 0.35);
      head.add(wing);
    });

    // 7. 3D Lips & Mouth
    const upperLipGeom = new THREE.CylinderGeometry(0.022, 0.022, 0.16, 8);
    const upperLip = new THREE.Mesh(upperLipGeom, lipMat);
    upperLip.rotation.z = Math.PI / 2;
    upperLip.position.set(0, 0.155, 0.31);
    head.add(upperLip);

    const lowerLipGeom = new THREE.SphereGeometry(0.045, 8, 8);
    const lowerLip = new THREE.Mesh(lowerLipGeom, lipMat);
    lowerLip.position.set(0, 0.125, 0.30);
    lowerLip.scale.set(1.6, 0.65, 0.8);
    head.add(lowerLip);

    // 8. 3D Ears with Helix & Lobe
    [-0.37, 0.37].forEach((ex, eIdx) => {
      const earHelixGeom = new THREE.TorusGeometry(0.075, 0.022, 6, 12, Math.PI * 1.2);
      const earHelix = new THREE.Mesh(earHelixGeom, skinMat);
      earHelix.rotation.y = (eIdx === 0 ? -1 : 1) * Math.PI / 2;
      earHelix.rotation.x = -0.2;
      earHelix.position.set(ex, 0.34, 0.02);
      head.add(earHelix);

      const lobeGeom = new THREE.SphereGeometry(0.04, 6, 6);
      const lobe = new THREE.Mesh(lobeGeom, skinMat);
      lobe.position.set(ex * 0.95, 0.28, 0.02);
      head.add(lobe);
    });

    // --- CHARACTER-SPECIFIC 3D SCULPTING (0% IMAGES, 100% 3D MESHES) ---
    switch(cd.id) {
      case 0: { // MITRA MAN
        // Bushy white eyebrows
        [-0.13, 0.13].forEach(x => {
          const brow = new THREE.Mesh(new THREE.CylinderGeometry(0.026, 0.026, 0.15, 6), whiteHairMat);
          brow.rotation.z = Math.PI / 2 + (x > 0 ? -0.15 : 0.15);
          brow.position.set(x, 0.46, 0.32);
          head.add(brow);
        });

        // Combed white hair cap with side volume
        const hairCap = new THREE.Mesh(
          new THREE.SphereGeometry(0.40, 16, 12, 0, Math.PI * 2, 0, Math.PI * 0.52),
          whiteHairMat
        );
        hairCap.position.set(0, 0.54, -0.04);
        hairCap.rotation.x = -0.15;
        head.add(hairCap);

        // Side white hair puffs above ears
        [-0.32, 0.32].forEach(hx => {
          const puff = new THREE.Mesh(new THREE.SphereGeometry(0.16, 10, 8), whiteHairMat);
          puff.position.set(hx, 0.42, -0.06);
          puff.scale.set(0.9, 1.2, 1.3);
          head.add(puff);
        });
        const backHair = new THREE.Mesh(new THREE.SphereGeometry(0.24, 10, 8), whiteHairMat);
        backHair.position.set(0, 0.38, -0.26);
        backHair.scale.set(1.2, 1.2, 0.8);
        head.add(backHair);

        // Full sculpted white beard wrapping jawline
        const jawBeard = new THREE.Mesh(
          new THREE.TorusGeometry(0.30, 0.09, 8, 16, Math.PI),
          whiteHairMat
        );
        jawBeard.rotation.x = Math.PI / 2 + 0.35;
        jawBeard.position.set(0, 0.16, 0.10);
        head.add(jawBeard);

        // White chin beard block
        const chinBeard = new THREE.Mesh(new THREE.SphereGeometry(0.14, 10, 8), whiteHairMat);
        chinBeard.position.set(0, 0.05, 0.22);
        chinBeard.scale.set(1.2, 1.0, 1.0);
        head.add(chinBeard);

        // White curved mustache
        const stache = new THREE.Mesh(
          new THREE.TorusGeometry(0.12, 0.042, 8, 14, Math.PI * 0.8),
          whiteHairMat
        );
        stache.rotation.z = -Math.PI * 0.1;
        stache.rotation.x = 0.3;
        stache.position.set(0, 0.18, 0.35);
        head.add(stache);

        // Rectangular silver wireframe spectacles
        [-0.13, 0.13].forEach(gx => {
          const rim = new THREE.Mesh(new THREE.TorusGeometry(0.095, 0.015, 6, 14), silverWireMat);
          rim.position.set(gx, 0.39, 0.36);
          head.add(rim);
        });
        const bridge = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.08, 6), silverWireMat);
        bridge.rotation.z = Math.PI / 2;
        bridge.position.set(0, 0.39, 0.37);
        head.add(bridge);
        [-0.23, 0.23].forEach(tx => {
          const temple = new THREE.Mesh(new THREE.CylinderGeometry(0.01, 0.01, 0.32, 6), silverWireMat);
          temple.rotation.x = Math.PI / 2;
          temple.position.set(tx, 0.39, 0.18);
          head.add(temple);
        });
        break;
      }

      case 1: { // YATRA BOY
        // Dark thick youthful eyebrows
        [-0.13, 0.13].forEach(x => {
          const brow = new THREE.Mesh(new THREE.CylinderGeometry(0.028, 0.028, 0.16, 6), darkHairMat);
          brow.rotation.z = Math.PI / 2 + (x > 0 ? -0.1 : 0.1);
          brow.position.set(x, 0.46, 0.32);
          head.add(brow);
        });

        // Wavy dark hair dome
        const hairMop = new THREE.Mesh(new THREE.SphereGeometry(0.42, 16, 14), darkHairMat);
        hairMop.position.set(0, 0.52, -0.02);
        hairMop.scale.set(1.02, 1.05, 1.05);
        head.add(hairMop);

        // Swept front hair locks across forehead
        [[-0.12, 0.50, 0.32, 0.2], [-0.02, 0.49, 0.34, 0.1], [0.09, 0.48, 0.33, -0.15]].forEach(([lx, ly, lz, rot]) => {
          const lock = new THREE.Mesh(new THREE.ConeGeometry(0.07, 0.20, 6), darkHairMat);
          lock.rotation.z = Math.PI + rot;
          lock.rotation.x = -0.3;
          lock.position.set(lx, ly, lz);
          head.add(lock);
        });

        // Scruffy dark beard wrapping jaw and chin
        const beardWrap = new THREE.Mesh(
          new THREE.TorusGeometry(0.28, 0.07, 8, 14, Math.PI),
          darkHairMat
        );
        beardWrap.rotation.x = Math.PI / 2 + 0.32;
        beardWrap.position.set(0, 0.16, 0.10);
        head.add(beardWrap);

        const chinBeard = new THREE.Mesh(new THREE.SphereGeometry(0.13, 10, 8), darkHairMat);
        chinBeard.position.set(0, 0.05, 0.21);
        chinBeard.scale.set(1.1, 0.9, 0.9);
        head.add(chinBeard);

        const mustache = new THREE.Mesh(new THREE.CylinderGeometry(0.025, 0.025, 0.18, 6), darkHairMat);
        mustache.rotation.z = Math.PI / 2;
        mustache.position.set(0, 0.185, 0.34);
        head.add(mustache);
        break;
      }

      case 2: { // DEEDEE QUEEN
        // Dark parted hair peeking under saree
        const frontHair = new THREE.Mesh(new THREE.SphereGeometry(0.39, 14, 10, 0, Math.PI * 2, 0, Math.PI * 0.45), darkHairMat);
        frontHair.position.set(0, 0.52, -0.02);
        head.add(frontHair);

        // White Saree Pallu / Cloth Hood framing the head
        const sareeHood = new THREE.Mesh(
          new THREE.SphereGeometry(0.46, 18, 14, 0, Math.PI * 2, 0, Math.PI * 0.72),
          whiteMat
        );
        sareeHood.position.set(0, 0.46, -0.02);
        sareeHood.rotation.x = -0.15;
        head.add(sareeHood);

        // Saree green border trim around opening
        const greenBorderMat = new THREE.MeshLambertMaterial({ color: 0x2e7d32 });
        const sareeTrim = new THREE.Mesh(
          new THREE.TorusGeometry(0.43, 0.026, 8, 24, Math.PI * 1.3),
          greenBorderMat
        );
        sareeTrim.rotation.z = -Math.PI * 0.15;
        sareeTrim.rotation.x = 0.4;
        sareeTrim.position.set(0, 0.42, 0.18);
        head.add(sareeTrim);

        // Red Bindi at center of forehead
        const bindi = new THREE.Mesh(new THREE.SphereGeometry(0.035, 10, 10), redMat);
        bindi.position.set(0, 0.44, 0.33);
        bindi.scale.set(1.0, 1.0, 0.4);
        head.add(bindi);

        // Dark spectacles
        [-0.13, 0.13].forEach(gx => {
          const rim = new THREE.Mesh(new THREE.TorusGeometry(0.095, 0.016, 6, 14), darkMat);
          rim.position.set(gx, 0.38, 0.36);
          head.add(rim);
        });
        const bridge = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.08, 6), darkMat);
        bridge.rotation.z = Math.PI / 2;
        bridge.position.set(0, 0.38, 0.37);
        head.add(bridge);

        // Low chignon / hair bun behind neck
        const bun = new THREE.Mesh(new THREE.SphereGeometry(0.15, 10, 8), darkHairMat);
        bun.position.set(0, 0.20, -0.32);
        head.add(bun);
        break;
      }

      case 3: { // TARIFF TAU
        // Intense bushy golden eyebrows
        [-0.13, 0.13].forEach(x => {
          const brow = new THREE.Mesh(new THREE.CylinderGeometry(0.032, 0.032, 0.18, 6), blondeHairMat);
          brow.rotation.z = Math.PI / 2 + (x > 0 ? -0.25 : 0.25);
          brow.position.set(x, 0.47, 0.33);
          head.add(brow);
        });

        // Iconic golden swept-over quiff hair pompadour
        const quiff = new THREE.Mesh(new THREE.ConeGeometry(0.24, 0.58, 10), blondeHairMat);
        quiff.rotation.z = -Math.PI / 3;
        quiff.rotation.x = 0.4;
        quiff.position.set(0.08, 0.66, 0.14);
        head.add(quiff);

        const quiffBase = new THREE.Mesh(new THREE.SphereGeometry(0.42, 14, 12), blondeHairMat);
        quiffBase.position.set(0, 0.54, -0.04);
        quiffBase.scale.set(1.05, 1.15, 1.05);
        head.add(quiffBase);

        // Red silk necktie hanging from collar across torso
        const tie = new THREE.Mesh(new THREE.BoxGeometry(0.14, 0.75, 0.06), redMat);
        tie.position.set(0, 0.45, 0.32);
        torso.add(tie);
        break;
      }

      case 4: { // DOZER DADA
        // Sacred 3D Tilak on forehead
        const tilakRed = new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.16, 0.025), redMat);
        tilakRed.position.set(0, 0.44, 0.33);
        head.add(tilakRed);

        const tilakChandan = new THREE.Mesh(new THREE.BoxGeometry(0.09, 0.04, 0.02), saffronMat);
        tilakChandan.position.set(0, 0.39, 0.33);
        head.add(tilakChandan);

        // Sharp dark angled eyebrows
        [-0.13, 0.13].forEach(x => {
          const brow = new THREE.Mesh(new THREE.CylinderGeometry(0.025, 0.025, 0.16, 6), darkHairMat);
          brow.rotation.z = Math.PI / 2 + (x > 0 ? -0.2 : 0.2);
          brow.position.set(x, 0.46, 0.32);
          head.add(brow);
        });

        // Gold hoop Kundal earring in right ear
        const earring = new THREE.Mesh(new THREE.TorusGeometry(0.048, 0.014, 8, 14), goldMat);
        earring.position.set(0.36, 0.26, 0.02);
        head.add(earring);
        break;
      }

      case 5: { // TOLL MASTER
        // Salt-and-pepper cropped side-parted hair
        const greyHairCap = new THREE.Mesh(
          new THREE.SphereGeometry(0.41, 14, 12, 0, Math.PI * 2, 0, Math.PI * 0.55),
          greyHairMat
        );
        greyHairCap.position.set(0, 0.52, -0.04);
        greyHairCap.rotation.x = -0.15;
        head.add(greyHairCap);

        // Neat grey mustache
        const tollMustache = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 0.20, 6), greyHairMat);
        tollMustache.rotation.z = Math.PI / 2;
        tollMustache.position.set(0, 0.19, 0.35);
        head.add(tollMustache);

        // Wireframe spectacles
        [-0.13, 0.13].forEach(gx => {
          const rim = new THREE.Mesh(new THREE.TorusGeometry(0.095, 0.015, 6, 14), darkMat);
          rim.position.set(gx, 0.38, 0.36);
          head.add(rim);
        });
        const bridge = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.08, 6), darkMat);
        bridge.rotation.z = Math.PI / 2;
        bridge.position.set(0, 0.38, 0.37);
        head.add(bridge);
        break;
      }

      case 6: { // RAIL RAJA
        // Iconic twirled upturned handlebar mustache tips
        [-0.11, 0.11].forEach((mx, mIdx) => {
          const tip = new THREE.Mesh(
            new THREE.TorusGeometry(0.085, 0.032, 8, 14, Math.PI * 0.9),
            darkHairMat
          );
          tip.rotation.z = mIdx === 0 ? 0.3 : -0.3;
          tip.rotation.y = mIdx === 0 ? -0.4 : 0.4;
          tip.position.set(mx, 0.18, 0.35);
          head.add(tip);
        });

        // Grey side hair tufts
        [-0.34, 0.34].forEach(sx => {
          const tuft = new THREE.Mesh(new THREE.SphereGeometry(0.14, 8, 8), greyHairMat);
          tuft.position.set(sx, 0.38, -0.08);
          head.add(tuft);
        });

        // Round silver spectacles
        [-0.13, 0.13].forEach(gx => {
          const rim = new THREE.Mesh(new THREE.TorusGeometry(0.09, 0.015, 8, 14), silverWireMat);
          rim.position.set(gx, 0.38, 0.36);
          head.add(rim);
        });
        const bridge = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.08, 6), silverWireMat);
        bridge.rotation.z = Math.PI / 2;
        bridge.position.set(0, 0.38, 0.37);
        head.add(bridge);

        // Desi gamchha scarf draped across shoulders on torso
        const shawlGeom = new THREE.TorusGeometry(0.48, 0.12, 8, 16);
        const gamchhaMat = new THREE.MeshLambertMaterial({ color: 0xd50000 });
        const shawl = new THREE.Mesh(shawlGeom, gamchhaMat);
        shawl.position.set(0, 0.65, 0);
        shawl.rotation.x = Math.PI / 3;
        torso.add(shawl);
        break;
      }

      case 7: { // GHAR MANTRI
        // High receding bald forehead with salt-and-pepper fringe around sides and back
        const fringe = new THREE.Mesh(
          new THREE.TorusGeometry(0.38, 0.10, 8, 16, Math.PI * 1.2),
          greyHairMat
        );
        fringe.rotation.x = Math.PI / 2 + 0.2;
        fringe.position.set(0, 0.36, -0.10);
        head.add(fringe);

        // Full dense grey/white beard
        const fullBeard = new THREE.Mesh(
          new THREE.TorusGeometry(0.31, 0.09, 8, 16, Math.PI),
          greyHairMat
        );
        fullBeard.rotation.x = Math.PI / 2 + 0.35;
        fullBeard.position.set(0, 0.16, 0.10);
        head.add(fullBeard);

        const chinBeard = new THREE.Mesh(new THREE.SphereGeometry(0.14, 10, 8), greyHairMat);
        chinBeard.position.set(0, 0.05, 0.22);
        chinBeard.scale.set(1.2, 1.0, 1.0);
        head.add(chinBeard);

        const mustache = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 0.22, 6), greyHairMat);
        mustache.rotation.z = Math.PI / 2;
        mustache.position.set(0, 0.18, 0.35);
        head.add(mustache);

        // Heavy dark rectangular spectacles
        [-0.13, 0.13].forEach(gx => {
          const rim = new THREE.Mesh(new THREE.TorusGeometry(0.095, 0.018, 6, 14), darkMat);
          rim.position.set(gx, 0.38, 0.36);
          head.add(rim);
        });
        const bridge = new THREE.Mesh(new THREE.CylinderGeometry(0.014, 0.014, 0.08, 6), darkMat);
        bridge.rotation.z = Math.PI / 2;
        bridge.position.set(0, 0.38, 0.37);
        head.add(bridge);
        break;
      }

      case 8: { // FACT BHAI
        // Modern dark high-fade pompadour
        const pompadour = new THREE.Mesh(new THREE.BoxGeometry(0.44, 0.24, 0.44), darkHairMat);
        pompadour.position.set(0, 0.62, 0.02);
        pompadour.rotation.x = -0.15;
        head.add(pompadour);

        const hairBase = new THREE.Mesh(new THREE.SphereGeometry(0.40, 14, 12), darkHairMat);
        hairBase.position.set(0, 0.50, -0.04);
        head.add(hairBase);

        // Jaw stubble
        const stubble = new THREE.Mesh(new THREE.CylinderGeometry(0.025, 0.025, 0.18, 6), darkHairMat);
        stubble.rotation.z = Math.PI / 2;
        stubble.position.set(0, 0.18, 0.34);
        head.add(stubble);

        // Studio over-ear DJ headphones
        const band = new THREE.Mesh(new THREE.TorusGeometry(0.44, 0.04, 8, 16, Math.PI), darkMat);
        band.position.set(0, 0.42, 0);
        head.add(band);
        [-0.42, 0.42].forEach(hx => {
          const cup = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.12, 0.1, 10), silverWireMat);
          cup.rotation.z = Math.PI / 2;
          cup.position.set(hx, 0.36, 0);
          head.add(cup);
        });
        break;
      }

      case 9: { // CHAI BOSS (MBAPU)
        // Prominent rounded ears
        [-0.39, 0.39].forEach((ex, eIdx) => {
          const bigEar = new THREE.Mesh(new THREE.TorusGeometry(0.09, 0.024, 6, 12, Math.PI * 1.3), skinMat);
          bigEar.rotation.y = (eIdx === 0 ? -1 : 1) * Math.PI / 2;
          bigEar.rotation.x = -0.1;
          bigEar.position.set(ex, 0.36, 0.02);
          head.add(bigEar);
        });

        // Drooping white grandfatherly mustache
        const droopMustache = new THREE.Mesh(
          new THREE.TorusGeometry(0.11, 0.035, 8, 14, Math.PI * 0.7),
          whiteHairMat
        );
        droopMustache.rotation.z = -Math.PI * 0.15;
        droopMustache.rotation.x = 0.4;
        droopMustache.position.set(0, 0.19, 0.34);
        head.add(droopMustache);

        // Round nickel wire spectacles
        [-0.13, 0.13].forEach(gx => {
          const rim = new THREE.Mesh(new THREE.TorusGeometry(0.09, 0.014, 12, 16), silverWireMat);
          rim.position.set(gx, 0.38, 0.36);
          head.add(rim);
        });
        const bridge = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.08, 6), silverWireMat);
        bridge.rotation.z = Math.PI / 2;
        bridge.position.set(0, 0.38, 0.37);
        head.add(bridge);
        break;
      }

      default: {
        // Fallback generic 3D hair
        const fallbackHair = new THREE.Mesh(
          new THREE.SphereGeometry(0.42, 14, 12, 0, Math.PI * 2, 0, Math.PI * 0.58),
          hairMat
        );
        fallbackHair.position.set(0, 0.54, -0.06);
        fallbackHair.rotation.x = -0.2;
        head.add(fallbackHair);
        break;
      }
    }
  }

  // --- Procedural Skeletal Animation Blender ---
  updatePose(state, time, progress = 0) {
    const b = this.bones;
    const t = time;

    // Reset baseline joint rotations
    b.torso.rotation.set(0, 0, 0);
    b.torso.position.y = 0.25;
    b.head.rotation.set(0, 0, 0);
    b.leftUpperArm.rotation.set(0, 0, 0);
    b.rightUpperArm.rotation.set(0, 0, 0);
    b.leftElbow.rotation.set(0, 0, 0);
    b.rightElbow.rotation.set(0, 0, 0);
    b.leftThigh.rotation.set(0, 0, 0);
    b.rightThigh.rotation.set(0, 0, 0);
    b.leftKnee.rotation.set(0, 0, 0);
    b.rightKnee.rotation.set(0, 0, 0);

    switch(state) {
      case "IDLE": {
        const breath = Math.sin(t * 3.5) * 0.04;
        b.torso.position.y = 0.25 + breath;
        b.torso.rotation.y = 0.2;
        // Combat guard fists
        b.leftUpperArm.rotation.x = -0.8 + breath;
        b.leftUpperArm.rotation.z = 0.3;
        b.leftElbow.rotation.x = -1.2;
        b.rightUpperArm.rotation.x = -0.9 - breath;
        b.rightUpperArm.rotation.z = -0.2;
        b.rightElbow.rotation.x = -1.3;
        // Ready stance
        b.leftThigh.rotation.x = 0.15;
        b.rightThigh.rotation.x = -0.15;
        break;
      }

      case "WALK_FWD": {
        const stride = Math.sin(t * 8) * 0.55;
        b.torso.position.y = 0.25 + Math.abs(Math.sin(t * 8)) * 0.05;
        b.torso.rotation.x = 0.15;
        b.leftThigh.rotation.x = stride;
        b.rightThigh.rotation.x = -stride;
        b.leftKnee.rotation.x = stride > 0 ? stride * 0.8 : 0;
        b.rightKnee.rotation.x = stride < 0 ? -stride * 0.8 : 0;
        b.leftUpperArm.rotation.x = -stride * 0.8 - 0.5;
        b.rightUpperArm.rotation.x = stride * 0.8 - 0.5;
        break;
      }

      case "WALK_BACK": {
        const stride = Math.sin(t * 6) * 0.4;
        b.torso.position.y = 0.25 + Math.abs(Math.sin(t * 6)) * 0.04;
        b.leftThigh.rotation.x = -stride;
        b.rightThigh.rotation.x = stride;
        b.leftUpperArm.rotation.x = stride * 0.6 - 0.7;
        b.rightUpperArm.rotation.x = -stride * 0.6 - 0.7;
        break;
      }

      case "JUMP": {
        b.torso.position.y = 0.4;
        b.leftThigh.rotation.x = -0.8;
        b.rightThigh.rotation.x = -0.6;
        b.leftKnee.rotation.x = 1.2;
        b.rightKnee.rotation.x = 0.9;
        b.leftUpperArm.rotation.z = 0.8;
        b.rightUpperArm.rotation.z = -0.8;
        b.leftUpperArm.rotation.x = -0.5;
        b.rightUpperArm.rotation.x = -0.5;
        break;
      }

      case "CROUCH": {
        b.torso.position.y = -0.15;
        b.leftThigh.rotation.x = -1.2;
        b.rightThigh.rotation.x = -1.2;
        b.leftKnee.rotation.x = 1.5;
        b.rightKnee.rotation.x = 1.5;
        b.leftUpperArm.rotation.x = -1.2;
        b.rightUpperArm.rotation.x = -1.2;
        break;
      }

      case "PUNCH_1": { // Quick Left Jab
        const p = Math.sin(progress * Math.PI);
        b.torso.rotation.y = 0.3 * p;
        b.leftUpperArm.rotation.x = -1.55 * p;
        b.leftUpperArm.rotation.y = -0.2 * p;
        b.leftElbow.rotation.x = -0.1 * p;
        b.rightUpperArm.rotation.x = -0.9;
        b.rightElbow.rotation.x = -1.2;
        break;
      }

      case "PUNCH_2": { // Heavy Right Cross
        const p = Math.sin(progress * Math.PI);
        b.torso.rotation.y = -0.5 * p;
        b.rightUpperArm.rotation.x = -1.65 * p;
        b.rightUpperArm.rotation.y = 0.3 * p;
        b.rightElbow.rotation.x = -0.05;
        b.leftUpperArm.rotation.x = -1.0;
        b.leftElbow.rotation.x = -1.3;
        break;
      }

      case "KICK_1": { // Front Snap Kick
        const p = Math.sin(progress * Math.PI);
        b.torso.rotation.x = -0.25 * p;
        b.rightThigh.rotation.x = -1.6 * p;
        b.rightKnee.rotation.x = -0.1 * p;
        b.leftThigh.rotation.x = 0.2 * p;
        b.leftUpperArm.rotation.z = 0.6 * p;
        b.rightUpperArm.rotation.z = -0.6 * p;
        break;
      }

      case "KICK_2": { // High Roundhouse Kick
        const p = Math.sin(progress * Math.PI);
        b.torso.rotation.z = -0.4 * p;
        b.torso.rotation.y = 0.5 * p;
        b.rightThigh.rotation.x = -1.8 * p;
        b.rightThigh.rotation.z = 0.5 * p;
        b.rightKnee.rotation.x = 0.1;
        break;
      }

      case "DROPKICK": { // Flying Dropkick
        const p = Math.sin(progress * Math.PI);
        b.torso.rotation.x = -1.3 * p;
        b.leftThigh.rotation.x = -1.7 * p;
        b.rightThigh.rotation.x = -1.7 * p;
        b.leftKnee.rotation.x = 0.1;
        b.rightKnee.rotation.x = 0.1;
        break;
      }

      case "SWEEP": { // Low Sweep
        const p = Math.sin(progress * Math.PI);
        b.torso.position.y = -0.3;
        b.leftThigh.rotation.x = -1.3;
        b.leftKnee.rotation.x = 1.4;
        b.rightThigh.rotation.x = 0.8 * p;
        b.rightThigh.rotation.z = 1.2 * p;
        break;
      }

      case "GRAB": { // Grapple Lunge
        const p = Math.sin(progress * Math.PI);
        b.torso.rotation.x = 0.4 * p;
        b.leftUpperArm.rotation.x = -1.4 * p;
        b.rightUpperArm.rotation.x = -1.4 * p;
        b.leftUpperArm.rotation.z = 0.3 * p;
        b.rightUpperArm.rotation.z = -0.3 * p;
        break;
      }

      case "BLOCK": {
        b.torso.rotation.x = -0.1;
        b.leftUpperArm.rotation.x = -1.5;
        b.leftUpperArm.rotation.y = 0.4;
        b.leftElbow.rotation.x = -1.8;
        b.rightUpperArm.rotation.x = -1.5;
        b.rightUpperArm.rotation.y = -0.4;
        b.rightElbow.rotation.x = -1.8;
        break;
      }

      case "HIT": {
        const p = Math.sin(progress * Math.PI);
        b.torso.rotation.x = -0.5 * p;
        b.head.rotation.x = -0.4 * p;
        b.leftUpperArm.rotation.x = -0.4;
        b.rightUpperArm.rotation.x = -0.4;
        b.leftUpperArm.rotation.z = 0.6 * p;
        b.rightUpperArm.rotation.z = -0.6 * p;
        break;
      }

      case "KNOCKDOWN": {
        b.root.rotation.x = -Math.PI / 2;
        b.root.position.y = 0.3;
        b.head.rotation.x = 0.3;
        break;
      }

      case "GETUP": {
        const p = progress;
        b.root.rotation.x = (-Math.PI / 2) * (1 - p);
        b.root.position.y = 0.3 * (1 - p);
        break;
      }

      case "VICTORY": {
        const wave = Math.sin(t * 6) * 0.4;
        b.torso.position.y = 0.25 + Math.abs(Math.sin(t * 6)) * 0.15;
        b.leftUpperArm.rotation.z = 2.4 + wave;
        b.rightUpperArm.rotation.z = -2.4 - wave;
        b.head.rotation.y = Math.sin(t * 4) * 0.3;
        break;
      }

      case "DEFEAT": {
        b.torso.rotation.x = 0.4;
        b.head.rotation.x = 0.5;
        b.leftUpperArm.rotation.x = 0.2;
        b.rightUpperArm.rotation.x = 0.2;
        break;
      }

      case "SUPER": { // Dramatic Super Stance
        const p = Math.sin(progress * Math.PI);
        b.torso.position.y = 0.5 * p;
        b.torso.rotation.x = -0.3 * p;
        b.leftUpperArm.rotation.z = 1.4 * p;
        b.rightUpperArm.rotation.z = -1.4 * p;
        b.leftUpperArm.rotation.x = -1.2 * p;
        b.rightUpperArm.rotation.x = -1.2 * p;
        break;
      }
    }
  }
}
