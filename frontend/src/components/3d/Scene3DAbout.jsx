import React, { useRef, useMemo, Suspense } from 'react';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import { 
  Float, 
  MeshDistortMaterial, 
  MeshWobbleMaterial,
  Sphere,
  Box,
  Torus,
  Icosahedron,
  Octahedron,
  Dodecahedron,
  Environment,
  Stars
} from '@react-three/drei';
import * as THREE from 'three';

// Performance-optimized floating shape component
const FloatingShape = ({ 
  position, 
  shape = 'sphere', 
  color = '#7c3aed', 
  scale = 1,
  speed = 1,
  distort = 0.3,
  floatIntensity = 1
}) => {
  const meshRef = useRef();
  
  // Slowly rotate based on time
  useFrame((state) => {
    if (meshRef.current) {
      meshRef.current.rotation.x = state.clock.elapsedTime * 0.1 * speed;
      meshRef.current.rotation.y = state.clock.elapsedTime * 0.15 * speed;
    }
  });

  const ShapeComponent = useMemo(() => {
    switch (shape) {
      case 'box': return Box;
      case 'torus': return Torus;
      case 'icosahedron': return Icosahedron;
      case 'octahedron': return Octahedron;
      case 'dodecahedron': return Dodecahedron;
      default: return Sphere;
    }
  }, [shape]);

  const args = useMemo(() => {
    switch (shape) {
      case 'box': return [1, 1, 1];
      case 'torus': return [1, 0.4, 16, 32];
      case 'icosahedron': return [1, 0];
      case 'octahedron': return [1, 0];
      case 'dodecahedron': return [1, 0];
      default: return [1, 32, 32];
    }
  }, [shape]);

  return (
    <Float 
      speed={speed} 
      rotationIntensity={0.5} 
      floatIntensity={floatIntensity}
      floatingRange={[-0.2, 0.2]}
    >
      <ShapeComponent ref={meshRef} args={args} position={position} scale={scale}>
        <MeshDistortMaterial
          color={color}
          attach="material"
          distort={distort}
          speed={2}
          roughness={0.2}
          metalness={0.8}
          transparent
          opacity={0.85}
        />
      </ShapeComponent>
    </Float>
  );
};

// Particle field with optimized instanced mesh
const ParticleField = ({ count = 100 }) => {
  const meshRef = useRef();
  const dummy = useMemo(() => new THREE.Object3D(), []);
  
  // Generate random positions once
  const particles = useMemo(() => {
    const temp = [];
    for (let i = 0; i < count; i++) {
      const x = (Math.random() - 0.5) * 30;
      const y = (Math.random() - 0.5) * 30;
      const z = (Math.random() - 0.5) * 15 - 5;
      const scale = Math.random() * 0.5 + 0.1;
      const speed = Math.random() * 0.5 + 0.5;
      temp.push({ x, y, z, scale, speed });
    }
    return temp;
  }, [count]);

  useFrame((state) => {
    if (!meshRef.current) return;
    
    particles.forEach((particle, i) => {
      const t = state.clock.elapsedTime * particle.speed;
      dummy.position.set(
        particle.x + Math.sin(t + i) * 0.5,
        particle.y + Math.cos(t + i * 0.5) * 0.5,
        particle.z
      );
      dummy.scale.setScalar(particle.scale);
      dummy.rotation.set(t * 0.5, t * 0.3, 0);
      dummy.updateMatrix();
      meshRef.current.setMatrixAt(i, dummy.matrix);
    });
    meshRef.current.instanceMatrix.needsUpdate = true;
  });

  return (
    <instancedMesh ref={meshRef} args={[null, null, count]}>
      <icosahedronGeometry args={[0.15, 0]} />
      <meshStandardMaterial 
        color="#a78bfa" 
        transparent 
        opacity={0.6}
        metalness={0.5}
        roughness={0.5}
      />
    </instancedMesh>
  );
};

// Gradient background plane
const GradientBackground = () => {
  const meshRef = useRef();
  
  const gradientMaterial = useMemo(() => {
    return new THREE.ShaderMaterial({
      uniforms: {
        uTime: { value: 0 },
        uColor1: { value: new THREE.Color('#ede9fe') },
        uColor2: { value: new THREE.Color('#ddd6fe') },
        uColor3: { value: new THREE.Color('#c4b5fd') },
      },
      vertexShader: `
        varying vec2 vUv;
        void main() {
          vUv = uv;
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform float uTime;
        uniform vec3 uColor1;
        uniform vec3 uColor2;
        uniform vec3 uColor3;
        varying vec2 vUv;
        
        void main() {
          float mixFactor = vUv.y + sin(vUv.x * 3.0 + uTime * 0.2) * 0.1;
          vec3 color = mix(uColor1, uColor2, mixFactor);
          color = mix(color, uColor3, smoothstep(0.5, 1.0, mixFactor));
          gl_FragColor = vec4(color, 1.0);
        }
      `,
    });
  }, []);

  useFrame((state) => {
    gradientMaterial.uniforms.uTime.value = state.clock.elapsedTime;
  });

  return (
    <mesh ref={meshRef} position={[0, 0, -15]} material={gradientMaterial}>
      <planeGeometry args={[50, 50]} />
    </mesh>
  );
};

// Connection lines between shapes
const ConnectionLines = ({ points }) => {
  const lineRef = useRef();
  
  const lineGeometry = useMemo(() => {
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(points.length * 3);
    points.forEach((point, i) => {
      positions[i * 3] = point[0];
      positions[i * 3 + 1] = point[1];
      positions[i * 3 + 2] = point[2];
    });
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    return geometry;
  }, [points]);

  return (
    <line ref={lineRef} geometry={lineGeometry}>
      <lineBasicMaterial color="#7c3aed" transparent opacity={0.3} />
    </line>
  );
};

// Adaptive performance component
const AdaptivePerformance = () => {
  const { gl } = useThree();
  
  React.useEffect(() => {
    // Reduce pixel ratio on lower-end devices
    const pixelRatio = Math.min(window.devicePixelRatio, 2);
    gl.setPixelRatio(pixelRatio);
  }, [gl]);
  
  return null;
};

// Main 3D Scene Component
const Scene3DAbout = ({ 
  className = '', 
  style = {},
  reducedMotion = false,
  isActive = true  // New prop to control render loop
}) => {
  // Check for reduced motion preference
  const prefersReducedMotion = useMemo(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches || reducedMotion;
  }, [reducedMotion]);

  const shapes = useMemo(() => [
    { position: [-4, 2, -2], shape: 'icosahedron', color: '#7c3aed', scale: 0.8, speed: 0.8 },
    { position: [4, -1, -3], shape: 'dodecahedron', color: '#6366f1', scale: 0.6, speed: 1.2 },
    { position: [-3, -2, -1], shape: 'octahedron', color: '#8b5cf6', scale: 0.5, speed: 1 },
    { position: [3, 3, -4], shape: 'torus', color: '#a78bfa', scale: 0.4, speed: 0.6 },
    { position: [0, -3, -2], shape: 'sphere', color: '#7c3aed', scale: 0.7, speed: 0.9 },
    { position: [-5, 0, -5], shape: 'icosahedron', color: '#6366f1', scale: 0.5, speed: 0.7 },
    { position: [5, 1, -4], shape: 'sphere', color: '#8b5cf6', scale: 0.4, speed: 1.1 },
  ], []);

  if (prefersReducedMotion) {
    // Return a static gradient for reduced motion preference
    return (
      <div 
        className={className}
        style={{
          ...style,
          background: 'linear-gradient(135deg, #ede9fe 0%, #ddd6fe 50%, #c4b5fd 100%)',
        }}
      />
    );
  }

  return (
    <div className={className} style={{ ...style, position: 'relative' }}>
      <Canvas
        camera={{ position: [0, 0, 8], fov: 50 }}
        dpr={[1, 2]}
        performance={{ min: 0.5 }}
        frameloop={isActive ? 'always' : 'demand'}
        gl={{ 
          antialias: true,
          alpha: true,
          powerPreference: 'high-performance'
        }}
        style={{ background: 'transparent' }}
      >
        <Suspense fallback={null}>
          <AdaptivePerformance />
          <GradientBackground />
          
          {/* Ambient and directional lighting */}
          <ambientLight intensity={0.4} />
          <directionalLight position={[10, 10, 5]} intensity={1} color="#ffffff" />
          <pointLight position={[-10, -10, -5]} intensity={0.5} color="#7c3aed" />
          
          {/* Floating geometric shapes */}
          {shapes.map((shape, index) => (
            <FloatingShape key={index} {...shape} />
          ))}
          
          {/* Particle field */}
          <ParticleField count={80} />
          
          {/* Stars in background */}
          <Stars 
            radius={50} 
            depth={50} 
            count={1000} 
            factor={4} 
            saturation={0} 
            fade 
            speed={0.5}
          />
        </Suspense>
      </Canvas>
    </div>
  );
};

export default Scene3DAbout;
