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
    title: "The Final Power Boss",
    party: "Royal Morcha",
    avatar: "👑",
    pwr: 95, spd: 80, def: 90,
    color: 0xffd700,
    skinColor: 0xdca272,
    clothesColor: 0xfff8e1,
    vestColor: 0xff6f00,
    hairColor: 0xf5f5f5,
    hat: "turban_gold",
    glasses: false,
    beard: "royal_white",
    superName: "KADAK CHAI SPLASH",
    superDesc: "A giant boiling brass kettle erupts with a volcano of spicy chai steam and cups!",
    quote: "Only the strongest can handle the asli kadak swaad of power!"
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

    const hipsGeom = new THREE.CylinderGeometry(0.38, 0.35, 0.45, 12);
    const hipsMesh = new THREE.Mesh(hipsGeom, clothesMat);
    hips.add(hipsMesh);

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
    const vestGeom = new THREE.BoxGeometry(0.88, 0.88, 0.58);
    const vestMesh = new THREE.Mesh(vestGeom, vestMat);
    vestMesh.position.y = 0.48;
    torso.add(vestMesh);

    // Neck & Head
    const neck = new THREE.Group();
    neck.position.y = 1.0;
    torso.add(neck);
    this.bones.neck = neck;

    const head = new THREE.Group();
    neck.add(head);
    this.bones.head = head;

    const headGeom = new THREE.SphereGeometry(0.42, 16, 14);
    const headMesh = new THREE.Mesh(headGeom, skinMat);
    headMesh.position.y = 0.35;
    head.add(headMesh);

    // Cartoon Nose
    const noseGeom = new THREE.SphereGeometry(0.1, 8, 8);
    const noseMesh = new THREE.Mesh(noseGeom, skinMat);
    noseMesh.position.set(0, 0.32, 0.42);
    head.add(noseMesh);

    // Eyes
    const eyeGeom = new THREE.SphereGeometry(0.08, 8, 8);
    const pupilGeom = new THREE.SphereGeometry(0.04, 8, 8);
    const whiteMat = new THREE.MeshBasicMaterial({ color: 0xffffff });

    [-0.14, 0.14].forEach(xOff => {
      const eyeWhite = new THREE.Mesh(eyeGeom, whiteMat);
      eyeWhite.position.set(xOff, 0.4, 0.38);
      head.add(eyeWhite);

      const pupil = new THREE.Mesh(pupilGeom, darkMat);
      pupil.position.set(xOff, 0.4, 0.44);
      head.add(pupil);
    });

    // Spectacles / Glasses
    if (cd.glasses) {
      const rimGeom = new THREE.TorusGeometry(0.12, 0.025, 8, 16);
      [-0.14, 0.14].forEach(xOff => {
        const rim = new THREE.Mesh(rimGeom, darkMat);
        rim.position.set(xOff, 0.4, 0.41);
        head.add(rim);
      });
      const bridgeGeom = new THREE.CylinderGeometry(0.02, 0.02, 0.1, 6);
      const bridge = new THREE.Mesh(bridgeGeom, darkMat);
      bridge.rotation.z = Math.PI / 2;
      bridge.position.set(0, 0.4, 0.42);
      head.add(bridge);
    }

    // Beard / Mustache
    if (cd.beard === "full" || cd.beard === "royal_white") {
      const beardGeom = new THREE.CylinderGeometry(0.32, 0.18, 0.35, 10);
      const beardMat = (cd.beard === "royal_white") ? new THREE.MeshLambertMaterial({ color: 0xffffff }) : hairMat;
      const beard = new THREE.Mesh(beardGeom, beardMat);
      beard.position.set(0, 0.14, 0.3);
      beard.rotation.x = 0.3;
      head.add(beard);
    } else if (cd.beard === "mustache_curly") {
      const stacheGeom = new THREE.TorusGeometry(0.16, 0.04, 6, 12, Math.PI);
      const stache = new THREE.Mesh(stacheGeom, darkMat);
      stache.position.set(0, 0.24, 0.42);
      stache.rotation.z = Math.PI;
      head.add(stache);
    }

    // Hair / Hats
    if (cd.hat === "turban_gold") {
      const turbanGeom = new THREE.CylinderGeometry(0.52, 0.44, 0.4, 14);
      const turban = new THREE.Mesh(turbanGeom, goldMat);
      turban.position.set(0, 0.65, 0.05);
      head.add(turban);
      const gemGeom = new THREE.SphereGeometry(0.08, 8, 8);
      const gem = new THREE.Mesh(gemGeom, new THREE.MeshLambertMaterial({ color: 0xff1744 }));
      gem.position.set(0, 0.65, 0.46);
      head.add(gem);
    } else if (cd.hat === "gamchha") {
      const shawlGeom = new THREE.TorusGeometry(0.48, 0.12, 8, 16);
      const gamchhaMat = new THREE.MeshLambertMaterial({ color: 0xd50000 });
      const shawl = new THREE.Mesh(shawlGeom, gamchhaMat);
      shawl.position.set(0, 0.05, 0);
      shawl.rotation.x = Math.PI / 3;
      torso.add(shawl);
    } else if (cd.hat === "headphones") {
      const bandGeom = new THREE.TorusGeometry(0.44, 0.04, 8, 16, Math.PI);
      const band = new THREE.Mesh(bandGeom, darkMat);
      band.position.set(0, 0.4, 0);
      head.add(band);
      [-0.42, 0.42].forEach(x => {
        const cup = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.12, 0.1, 10), darkMat);
        cup.rotation.z = Math.PI / 2;
        cup.position.set(x, 0.38, 0);
        head.add(cup);
      });
    } else if (cd.hat !== "bald") {
      const hairCap = new THREE.Mesh(new THREE.SphereGeometry(0.44, 14, 10, 0, Math.PI * 2, 0, Math.PI * 0.55), hairMat);
      hairCap.position.y = 0.38;
      head.add(hairCap);
    }

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

      // Foot / Shoe
      const footMesh = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.14, 0.38), darkMat);
      footMesh.position.set(0, -0.68, 0.08);
      knee.add(footMesh);

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
