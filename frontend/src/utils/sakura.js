export const createSakura = (width, height) => {
  const depth = 0.1 + Math.random() * 0.8;
  
  return {
    x: Math.random() * width,
    y: -100 - Math.random() * 200,
    rotation: Math.random() * 360,
    scale: 0.8 + Math.random() * 0.7,
    speed: 0.4 + Math.random() * 0.6,
    swing: 0.5 + Math.random(),
    swingSpeed: 0.003 + Math.random() * 0.007,
    swingDirection: Math.random() > 0.5 ? 1 : -1,
    opacity: 0.8 + Math.random() * 0.2,
    rotationSpeed: (Math.random() - 0.5) * 0.8,
    depth: depth,
    swingAmplitude: 1 + Math.random() * 3,
    delay: Math.random() * 5000,
    isBeingPushed: false,
    pushX: 0,
    pushY: 0
    
  }
}

export const updateSakura = (sakura, width, height) => {
  
  if (sakura.delay > 0) {
    return {...sakura, delay: sakura.delay - 16}
  }
  
  const updated = { ...sakura }
  const depthSpeedFactor = 0.2 + updated.depth * 0.8;
  const depthOpacityFactor = 0.4 + updated.depth * 0.6;
  
  updated.y += updated.speed * depthSpeedFactor * (1 + updated.y / height * 0.2);
  updated.x += Math.sin(updated.swing) * updated.swingDirection * updated.swingAmplitude * depthSpeedFactor;
  updated.swing += updated.swingSpeed;
  updated.rotation += updated.rotationSpeed * depthSpeedFactor;
  
  updated.opacity = (0.8 + Math.sin(Date.now() * 0.001) * 0.1) * 
                   depthOpacityFactor * 
                   (1 - Math.max(0, (updated.y - height * 0.8) / (height * 0.2)));
  
  updated.scale = 0.8 + Math.sin(Date.now() * 0.001 * updated.swingSpeed) * (0.4 * updated.depth);
  
  
  return updated
}