import { useEffect, useRef } from 'react';

const clamp = (value, min = 0, max = 1) => Math.min(max, Math.max(min, value));
const smoothstep = (value) => value * value * (3 - 2 * value);

const LIVE_IN_THRESHOLD = 0.22;
const ACTIVE_SWITCH_DELTA = 0.08;

const hasMaskChanged = (prevMask, nextMask) => {
  if (!Array.isArray(prevMask) || prevMask.length !== nextMask.length) return true;

  for (let index = 0; index < nextMask.length; index += 1) {
    if (prevMask[index] !== nextMask[index]) {
      return true;
    }
  }

  return false;
};

const applyFinalState = ({ sectionRef, motionRefs, cardCount }) => {
  const sectionEl = sectionRef.current;
  if (!sectionEl) return;

  sectionEl.style.setProperty('--stack-progress', '1');

  for (let index = 0; index < cardCount; index += 1) {
    const motionEl = motionRefs.current[index];

    if (!motionEl) continue;

    motionEl.style.opacity = '1';
    motionEl.style.transform = 'none';
    motionEl.style.setProperty('--reveal-progress', '1');
  }
};

const measureCardCenters = (sectionEl, cardRefs) => {
  const sectionRect = sectionEl.getBoundingClientRect();
  const sectionTop = sectionRect.top + window.scrollY;

  return cardRefs.current.map((cardEl) => {
    if (!cardEl) return 0;

    const cardRect = cardEl.getBoundingClientRect();
    return cardRect.top + window.scrollY - sectionTop + cardRect.height * 0.5;
  });
};

export const useStackedReveal = ({
  sectionRef,
  cardRefs,
  motionRefs,
  cardCount,
  disabled,
  onStepChange,
  onLiveMaskChange,
  onActiveIndexChange,
}) => {
  const liveMaskRef = useRef(Array.from({ length: cardCount }, () => false));
  const activeIndexRef = useRef(0);
  const progressStepRef = useRef(1);

  useEffect(() => {
    liveMaskRef.current = Array.from({ length: cardCount }, () => false);
    activeIndexRef.current = 0;
    progressStepRef.current = 1;
  }, [cardCount]);

  useEffect(() => {
    const sectionEl = sectionRef.current;
    if (!sectionEl) return undefined;

    if (disabled) {
      const allLiveMask = Array.from({ length: cardCount }, () => true);
      const disabledStep = cardCount > 0 ? 1 : 0;

      applyFinalState({ sectionRef, motionRefs, cardCount });

      liveMaskRef.current = allLiveMask;
      activeIndexRef.current = cardCount > 0 ? 0 : -1;
      progressStepRef.current = disabledStep;

      onStepChange?.(disabledStep);
      onLiveMaskChange?.(allLiveMask);
      onActiveIndexChange?.(cardCount > 0 ? 0 : -1);

      return undefined;
    }

    let rafId = 0;
    let measureRafId = 0;
    let isInViewportRange = true;
    let cardCenters = [];
    let previousProgress = -1;
    let previousReveals = Array.from({ length: cardCount }, () => -1);

    const publishStep = (nextStep) => {
      if (nextStep === progressStepRef.current) return;
      progressStepRef.current = nextStep;
      onStepChange?.(nextStep);
    };

    const publishActiveIndex = (nextActiveIndex) => {
      if (nextActiveIndex === activeIndexRef.current) return;
      activeIndexRef.current = nextActiveIndex;
      onActiveIndexChange?.(nextActiveIndex);
    };

    const publishLiveMask = (nextMask) => {
      if (!hasMaskChanged(liveMaskRef.current, nextMask)) return;
      liveMaskRef.current = nextMask;
      onLiveMaskChange?.(nextMask);
    };

    const runMeasure = () => {
      cardCenters = measureCardCenters(sectionEl, cardRefs);
    };

    const updateProgress = (forceWrite = false) => {
      rafId = 0;

      const viewportHeight = window.innerHeight || 1;
      const sectionRect = sectionEl.getBoundingClientRect();

      if (sectionRect.bottom < -viewportHeight * 0.45 || sectionRect.top > viewportHeight * 1.35) {
        return;
      }

      if (cardCenters.length !== cardCount) {
        runMeasure();
      }

      const startTrigger = viewportHeight * 0.9;
      const totalDistance = sectionRect.height + viewportHeight * 0.72;
      const nextProgress = clamp((startTrigger - sectionRect.top) / Math.max(1, totalDistance));
      const progressDelta = forceWrite ? 1 : Math.abs(nextProgress - previousProgress);

      sectionEl.style.setProperty('--stack-progress', nextProgress.toFixed(4));

      const nextStep = Math.max(1, Math.min(cardCount, Math.round(nextProgress * cardCount + 0.5)));
      publishStep(nextStep);

      const revealStartY = viewportHeight * 0.92;
      const revealEndY = viewportHeight * 0.58;
      const revealDistance = Math.max(1, revealStartY - revealEndY);

      const nextLiveMask = Array.from({ length: cardCount }, () => false);
      const frameReveals = Array.from({ length: cardCount }, () => 0);

      let strongestIndex = 0;
      let strongestReveal = -1;

      for (let index = 0; index < cardCount; index += 1) {
        const cardEl = cardRefs.current[index];
        const motionEl = motionRefs.current[index];
        if (!cardEl || !motionEl) continue;

        const cardCenterOffset = cardCenters[index] ?? 0;
        const cardCenterY = sectionRect.top + cardCenterOffset;

        const rawReveal = clamp((revealStartY - cardCenterY) / revealDistance);
        const reveal = smoothstep(rawReveal);
        const depth = Math.max(0, 1 - reveal);
        frameReveals[index] = reveal;

        const side = index % 2 === 0 ? -1 : 1;
        const floatWave = Math.sin((nextProgress * 3.2 + index * 0.35) * Math.PI) * (depth * 1.6);
        const translateX = depth * side * (58 + index * 9);
        const translateY = depth * (70 + index * 10) + floatWave * 0.8;
        const scale = 0.93 + reveal * 0.07;
        const rotateX = depth * 6;
        const rotateZ = depth * side * 1.8;
        const revealDelta = Math.abs(reveal - (previousReveals[index] ?? -1));
        const shouldWriteMotion = forceWrite || progressDelta > 0.001 || revealDelta > 0.001;

        if (shouldWriteMotion) {
          motionEl.style.opacity = `${0.3 + reveal * 0.7}`;
          motionEl.style.transform = `perspective(1300px) translate3d(${translateX.toFixed(2)}px, ${translateY.toFixed(2)}px, 0) scale(${scale.toFixed(4)}) rotateX(${rotateX.toFixed(3)}deg) rotateZ(${rotateZ.toFixed(3)}deg)`;

          motionEl.style.setProperty('--reveal-progress', reveal.toFixed(4));
          previousReveals[index] = reveal;
        }

        const wasLive = liveMaskRef.current[index] ?? false;
        const isLive = wasLive || reveal > LIVE_IN_THRESHOLD;
        nextLiveMask[index] = isLive;

        if (reveal > strongestReveal) {
          strongestReveal = reveal;
          strongestIndex = index;
        }
      }

      const currentActiveIndex = activeIndexRef.current;
      const currentReveal =
        currentActiveIndex >= 0
          ? frameReveals[currentActiveIndex] ?? previousReveals[currentActiveIndex] ?? 0
          : 0;

      const nextActiveIndex =
        strongestIndex !== currentActiveIndex && strongestReveal <= currentReveal + ACTIVE_SWITCH_DELTA
          ? currentActiveIndex
          : strongestIndex;

      publishActiveIndex(nextActiveIndex);
      publishLiveMask(nextLiveMask);
      previousProgress = nextProgress;
    };

    const scheduleProgressUpdate = () => {
      if (!isInViewportRange || rafId) return;
      rafId = window.requestAnimationFrame(() => updateProgress(false));
    };

    const scheduleReMeasure = () => {
      if (measureRafId) return;

      measureRafId = window.requestAnimationFrame(() => {
        measureRafId = 0;
        runMeasure();
        updateProgress(true);
      });
    };

    let viewportObserver;
    if (typeof IntersectionObserver !== 'undefined') {
      viewportObserver = new IntersectionObserver(
        ([entry]) => {
          isInViewportRange = entry.isIntersecting;
          if (isInViewportRange) {
            scheduleReMeasure();
          }
        },
        {
          threshold: 0,
          rootMargin: '45% 0px 45% 0px',
        }
      );

      viewportObserver.observe(sectionEl);
    }

    let resizeObserver;
    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(scheduleReMeasure);
      resizeObserver.observe(sectionEl);

      cardRefs.current.forEach((cardEl) => {
        if (cardEl) {
          resizeObserver.observe(cardEl);
        }
      });
    }

    window.addEventListener('scroll', scheduleProgressUpdate, { passive: true });
    window.addEventListener('resize', scheduleReMeasure);
    window.addEventListener('orientationchange', scheduleReMeasure);

    scheduleReMeasure();

    if (document.fonts?.ready) {
      document.fonts.ready.then(() => {
        scheduleReMeasure();
      });
    }

    return () => {
      window.removeEventListener('scroll', scheduleProgressUpdate);
      window.removeEventListener('resize', scheduleReMeasure);
      window.removeEventListener('orientationchange', scheduleReMeasure);

      viewportObserver?.disconnect();
      resizeObserver?.disconnect();

      if (rafId) {
        window.cancelAnimationFrame(rafId);
      }

      if (measureRafId) {
        window.cancelAnimationFrame(measureRafId);
      }
    };
  }, [
    sectionRef,
    cardRefs,
    motionRefs,
    cardCount,
    disabled,
    onStepChange,
    onLiveMaskChange,
    onActiveIndexChange,
  ]);
};
