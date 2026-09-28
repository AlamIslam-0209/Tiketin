if (window.gsap && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
  const tl = gsap.timeline();
  tl.from('header', {
    y: -100,
    duration: 0.8,
    opacity: 0,
    ease: 'power2.out'
  });

  tl.from('judul', {
    s
  })
}

