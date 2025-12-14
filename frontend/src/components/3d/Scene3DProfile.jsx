import React, { useRef, useMemo, Suspense } from 'react';
import { Canvas, useFrame, useThree } from '@react-three/fiber';
import { 
  Float, 
  MeshDistortMaterial,
  Sphere,
  Icosahedron,
  Octahedron,
  Environment,
  Stars,
  Trail
} from '@react-three/drei';
import * as THREE from 'three';

// Orbiting security icon representation
const SecurityOrb = ({ radius = 3, speed = 0.5, offset = 0, color = '#7c3aed' }) => {
  const meshRef = useRef();
  
  useFrame((state) => {
    const t = state.clock.elapsedTime * speed + offset;
    if (meshRef.current) {
      meshRef.current.position.x = Math.cos(t) * radius;
      meshRef.current.position.z = Math.sin(t) * radius;
      meshRef.current.position.y = Math.sin(t * 2) * 0.5;
    }
  });

  return (
    <Trail
      width={1}
      length={6}
      color={color}
      attenuation={(t) => t * t}
    >
      <mesh ref={meshRef}>
        <icosahedronGeometry args={[0.2, 0]} />
        <meshStandardMaterial 
          color={color} 
          emissive={color}
          emissiveIntensity={0.5}
          metalness={0.8}
          roughness={0.2}
        />
      </mesh>
    </Trail>
  );
};

// Shield shape for security theme
const Shield3D = ({ position = [0, 0, 0], scale = 1 }) => {
  const meshRef = useRef();
  
  useFrame((state) => {
    if (meshRef.current) {
      meshRef.current.rotation.y = Math.sin(state.clock.elapsedTime * 0.5) * 0.2;
      meshRef.current.position.y = position[1] + Math.sin(state.clock.elapsedTime * 0.8) * 0.1;
    }
  });

  // Create shield shape
  const shieldShape = useMemo(() => {
    const shape = new THREE.Shape();
    shape.moveTo(0, 1.2);
    shape.quadraticCurveTo(0.8, 1, 0.8, 0.3);
    shape.quadraticCurveTo(0.8, -0.5, 0, -1);
    shape.quadraticCurveTo(-0.8, -0.5, -0.8, 0.3);
    shape.quadraticCurveTo(-0.8, 1, 0, 1.2);
    return shape;
  }, []);

  const extrudeSettings = useMemo(() => ({
    steps: 1,
    depth: 0.15,
    bevelEnabled: true,
    bevelThickness: 0.05,
    bevelSize: 0.05,
    bevelSegments: 3
  }), []);

  return (
    <Float speed={2} rotationIntensity={0.3} floatIntensity={0.5}>
      <mesh 
        ref={meshRef} 
        position={position} 
        scale={scale}
        rotation={[0, 0, 0]}
      >
        <extrudeGeometry args={[shieldShape, extrudeSettings]} />
        <MeshDistortMaterial
          color="#7c3aed"
          distort={0.1}
          speed={2}
          roughness={0.2}
          metalness={0.9}
          transparent
          opacity={0.9}
        />
      </mesh>
    </Float>
  );
};

// Lock icon 3D representation
const Lock3D = ({ position = [0, 0, 0], scale = 0.5 }) => {
  const groupRef = useRef();
  
  useFrame((state) => {
    if (groupRef.current) {
      groupRef.current.rotation.y = state.clock.elapsedTime * 0.3;
    }
  });

  return (
    <Float speed={1.5} rotationIntensity={0.2} floatIntensity={0.3}>
      <group ref={groupRef} position={position} scale={scale}>
        {/* Lock body */}
        <mesh position={[0, -0.3, 0]}>
          <boxGeometry args={[1, 0.8, 0.4]} />
          <meshStandardMaterial 
            color="#6366f1" 
            metalness={0.8} 
            roughness={0.2}
          />
        </mesh>
        {/* Lock shackle */}
        <mesh position={[0, 0.3, 0]}>
          <torusGeometry args={[0.35, 0.1, 16, 32, Math.PI]} />
          <meshStandardMaterial 
            color="#8b5cf6" 
            metalness={0.9} 
            roughness={0.1}
          />
        </mesh>
      </group>
    </Float>
  );
};

// Floating key
const Key3D = ({ position = [0, 0, 0], scale = 0.4 }) => {
  const groupRef = useRef();
  
  useFrame((state) => {
    if (groupRef.current) {
      groupRef.current.rotation.z = Math.sin(state.clock.elapsedTime) * 0.3;
      groupRef.current.rotation.y = state.clock.elapsedTime * 0.2;
    }
  });

  return (
    <Float speed={2} rotationIntensity={0.4} floatIntensity={0.5}>
      <group ref={groupRef} position={position} scale={scale}>
        {/* Key head (ring) */}
        <mesh position={[0, 0.8, 0]}>
          <torusGeometry args={[0.4, 0.12, 16, 32]} />
          <meshStandardMaterial 
            color="#a78bfa" 
            metalness={0.9} 
            roughness={0.1}
          />
        </mesh>
        {/* Key shaft */}
        <mesh position={[0, -0.2, 0]}>
          <boxGeometry args={[0.15, 1.2, 0.08]} />
          <meshStandardMaterial 
            color="#a78bfa" 
            metalness={0.9} 
            roughness={0.1}
          />
        </mesh>
        {/* Key teeth */}
        <mesh position={[0.15, -0.6, 0]}>
          <boxGeometry args={[0.2, 0.12, 0.08]} />
          <meshStandardMaterial 
            color="#a78bfa" 
            metalness={0.9} 
            roughness={0.1}
          />
        </mesh>
        <mesh position={[0.12, -0.8, 0]}>
          <boxGeometry args={[0.15, 0.1, 0.08]} />
          <meshStandardMaterial 
            color="#a78bfa" 
            metalness={0.9} 
            roughness={0.1}
          />
        </mesh>
      </group>
    </Float>
  );
};

// Particle ring around central element
const ParticleRing = ({ count = 50, radius = 2.5 }) => {
  const meshRef = useRef();
  const dummy = useMemo(() => new THREE.Object3D(), []);
  
  const particles = useMemo(() => {
    const temp = [];
    for (let i = 0; i < count; i++) {
      const angle = (i / count) * Math.PI * 2;
      const r = radius + (Math.random() - 0.5) * 0.5;
      temp.push({
        angle,
        radius: r,
        speed: Math.random() * 0.5 + 0.5,
        yOffset: (Math.random() - 0.5) * 0.5,
        scale: Math.random() * 0.3 + 0.1
      });
    }
    return temp;
  }, [count, radius]);

  useFrame((state) => {
    if (!meshRef.current) return;
    
    particles.forEach((particle, i) => {
      const t = state.clock.elapsedTime * particle.speed;
      const angle = particle.angle + t * 0.2;
      dummy.position.set(
        Math.cos(angle) * particle.radius,
        particle.yOffset + Math.sin(t * 2) * 0.2,
        Math.sin(angle) * particle.radius
      );
      dummy.scale.setScalar(particle.scale);
      dummy.updateMatrix();
      meshRef.current.setMatrixAt(i, dummy.matrix);
    });
    meshRef.current.instanceMatrix.needsUpdate = true;
  });

  return (
    <instancedMesh ref={meshRef} args={[null, null, count]}>
      <sphereGeometry args={[0.08, 8, 8]} />
      <meshStandardMaterial 
        color="#7c3aed" 
        emissive="#7c3aed"
        emissiveIntensity={0.3}
        transparent 
        opacity={0.7}
      />
    </instancedMesh>
  );
};

// Adaptive performance
const AdaptivePerformance = () => {
  const { gl } = useThree();
  
  React.useEffect(() => {
    const pixelRatio = Math.min(window.devicePixelRatio, 2);
    gl.setPixelRatio(pixelRatio);
  }, [gl]);
  
  return null;
};

// Main Profile 3D Scene
const Scene3DProfile = ({ 
  className = '', 
  style = {},
  variant = 'full' // 'full', 'minimal', 'security'
}) => {
  const prefersReducedMotion = useMemo(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  }, []);

  if (prefersReducedMotion) {
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
        camera={{ position: [0, 0, 6], fov: 50 }}
        dpr={[1, 2]}
        performance={{ min: 0.5 }}
        gl={{ 
          antialias: true,
          alpha: true,
          powerPreference: 'high-performance'
        }}
        style={{ background: 'transparent' }}
      >
        <Suspense fallback={null}>
          <AdaptivePerformance />
          
          {/* Lighting */}
          <ambientLight intensity={0.4} />
          <directionalLight position={[10, 10, 5]} intensity={1} />
          <pointLight position={[-5, 5, 5]} intensity={0.8} color="#7c3aed" />
          <pointLight position={[5, -5, 5]} intensity={0.5} color="#6366f1" />
          
          {variant === 'security' ? (
            <>
              {/* Security-themed scene */}
              <Shield3D position={[0, 0, 0]} scale={1.2} />
              <Lock3D position={[-2.5, 1, -1]} scale={0.6} />
              <Key3D position={[2.5, -0.5, -1]} scale={0.5} />
              <ParticleRing count={40} radius={2.5} />
              
              {/* Orbiting elements */}
              <SecurityOrb radius={3} speed={0.4} offset={0} color="#7c3aed" />
              <SecurityOrb radius={3} speed={0.4} offset={Math.PI} color="#6366f1" />
              <SecurityOrb radius={2.2} speed={0.6} offset={Math.PI / 2} color="#8b5cf6" />
            </>
          ) : variant === 'minimal' ? (
            <>
              {/* Minimal floating shapes */}
              <Float speed={2} rotationIntensity={0.5} floatIntensity={0.5}>
                <Icosahedron args={[0.8, 0]} position={[-2, 0, 0]}>
                  <MeshDistortMaterial
                    color="#7c3aed"
                    distort={0.3}
                    speed={2}
                    roughness={0.2}
                    metalness={0.8}
                    transparent
                    opacity={0.8}
                  />
                </Icosahedron>
              </Float>
              <Float speed={1.5} rotationIntensity={0.3} floatIntensity={0.4}>
                <Octahedron args={[0.6, 0]} position={[2, 0.5, -1]}>
                  <MeshDistortMaterial
                    color="#6366f1"
                    distort={0.2}
                    speed={3}
                    roughness={0.2}
                    metalness={0.8}
                    transparent
                    opacity={0.8}
                  />
                </Octahedron>
              </Float>
            </>
          ) : (
            <>
              {/* Full scene with all elements */}
              <Shield3D position={[0, 0.5, 0]} scale={1} />
              <Lock3D position={[-3, 1.5, -2]} scale={0.5} />
              <Key3D position={[3, -1, -2]} scale={0.4} />
              <ParticleRing count={60} radius={3} />
              
              <Float speed={1.5} rotationIntensity={0.3} floatIntensity={0.4}>
                <Sphere args={[0.3, 16, 16]} position={[-2, -1.5, 0]}>
                  <meshStandardMaterial 
                    color="#a78bfa" 
                    metalness={0.9} 
                    roughness={0.1}
                    transparent
                    opacity={0.8}
                  />
                </Sphere>
              </Float>
              
              <SecurityOrb radius={3.5} speed={0.3} offset={0} color="#7c3aed" />
              <SecurityOrb radius={3.5} speed={0.3} offset={Math.PI * 2/3} color="#6366f1" />
              <SecurityOrb radius={3.5} speed={0.3} offset={Math.PI * 4/3} color="#8b5cf6" />
            </>
          )}
          
          {/* Background stars */}
          <Stars 
            radius={30} 
            depth={30} 
            count={500} 
            factor={3} 
            saturation={0} 
            fade 
            speed={0.3}
          />
        </Suspense>
      </Canvas>
    </div>
  );
};

export default Scene3DProfile;
