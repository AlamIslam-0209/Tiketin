/* Data demo Tiketin. Ganti modul ini dengan API saat backend tersedia. */
(() => {
  const KEY = 'tiketinDemoV2';
  const SESSION = 'tiketinSessionV2';
  const PENDING_ORDER = 'tiketinPendingOrderV2';

  const seed = () => ({
    users: [
      { id: 'admin-1', username: 'admin', password: 'admin123', role: 'admin', fullName: 'Admin Tiketin' },
      { id: 'org-1', username: 'senjasuara', password: 'organizer123', role: 'organizer', fullName: 'Tim Senja Suara', organization: 'Senja Suara' },
      { id: 'org-2', username: 'jelajah', password: 'organizer123', role: 'organizer', fullName: 'Tim Jelajah', organization: 'Jelajah Nusantara' },
      { id: 'user-1', username: 'pengunjung', password: 'user123', role: 'user', fullName: 'Pengunjung Tiketin' }
    ],
    applications: [],
    events: [
      { id: 'event-1', ownerId: 'org-1', name: 'Senja Suara Festival', date: '2026-10-12', price: 175000, slots: 120, category: 'Konser', description: 'Musik, matahari terbenam, dan suasana hangat di tepi kota.' },
      { id: 'event-2', ownerId: 'org-1', name: 'Ruang Bunyi Live', date: '2026-10-18', price: 95000, slots: 80, category: 'Konser', description: 'Panggung intim untuk suara baru dan cerita yang dekat.' },
      { id: 'event-3', ownerId: 'org-2', name: 'Jelajah Pulau Kecil', date: '2026-10-24', price: 250000, slots: 45, category: 'Wisata', description: 'Seharian menikmati laut biru, kuliner lokal, dan udara segar.' },
      { id: 'event-4', ownerId: 'org-2', name: 'Langkah di Kota Lama', date: '2026-11-01', price: 75000, slots: 60, category: 'Wisata', description: 'Tur santai mengenal sudut bersejarah dengan cerita lokal.' },
      { id: 'event-5', ownerId: 'org-1', name: 'Ide Jadi Aksi', date: '2026-11-08', price: 50000, slots: 100, category: 'Seminar', description: 'Obrolan praktis untuk mengubah ide kecil menjadi langkah nyata.' },
      { id: 'event-6', ownerId: 'org-2', name: 'Future Makers Meetup', date: '2026-11-15', price: 85000, slots: 75, category: 'Seminar', description: 'Bertemu, belajar, dan bertukar perspektif dengan para kreator.' }
    ]
  });

  function read() {
    const raw = localStorage.getItem(KEY);
    if (!raw) {
      const data = seed();
      localStorage.setItem(KEY, JSON.stringify(data));
      return data;
    }
    try { return JSON.parse(raw); }
    catch { const data = seed(); localStorage.setItem(KEY, JSON.stringify(data)); return data; }
  }

  const save = (data) => localStorage.setItem(KEY, JSON.stringify(data));
  const uid = () => crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`;
  const publicUser = (user) => user && (({ password, ...safe }) => safe)(user);
  const currentUser = () => {
    const id = localStorage.getItem(SESSION);
    return publicUser(read().users.find((user) => user.id === id)) || null;
  };
  const requireRole = (role) => {
    const user = currentUser();
    if (!user || user.role !== role) {
      location.replace(user ? homeFor(user) : 'login.html');
      return null;
    }
    return user;
  };
  const homeFor = (user) => user.role === 'admin' ? 'admin.html' : user.role === 'organizer' ? 'organizer.html' : 'index.html#home';
  const latestApplication = (userId) => read().applications.filter((item) => item.userId === userId).at(-1) || null;
  const eventInput = (input) => ({
    name: input.name.trim(), date: input.date, price: Number(input.price), slots: Number(input.slots),
    category: input.category || 'Lainnya', description: (input.description || '').trim()
  });
  const validateEvent = (event) => {
    if (!event.name || !event.date || !Number.isInteger(event.price) || event.price < 0 || !Number.isInteger(event.slots) || event.slots < 0) {
      throw new Error('Lengkapi data event dengan harga dan slot yang valid.');
    }
  };

  window.TiketinStore = {
    read, currentUser, requireRole, homeFor, latestApplication,
    formatPrice: (value) => new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(value),
    formatDate: (value) => new Intl.DateTimeFormat('id-ID', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(`${value}T12:00:00`)),
    listEvents: () => read().events,
    listUsers: () => read().users.map(publicUser),
    listApplications: () => read().applications,
    login(username, password) {
      const user = read().users.find((item) => item.username.toLowerCase() === username.trim().toLowerCase() && item.password === password);
      if (!user) throw new Error('Username atau password salah.');
      localStorage.setItem(SESSION, user.id);
      return publicUser(user);
    },
    register(input) {
      const data = read();
      const username = input.username.trim();
      if (username.length < 3 || input.password.length < 6 || !input.fullName.trim()) throw new Error('Isi nama, username minimal 3 karakter, dan password minimal 6 karakter.');
      if (data.users.some((item) => item.username.toLowerCase() === username.toLowerCase())) throw new Error('Username sudah dipakai.');
      const user = { id: uid(), username, password: input.password, role: 'user', fullName: input.fullName.trim() };
      data.users.push(user); save(data); localStorage.setItem(SESSION, user.id);
      return publicUser(user);
    },
    logout() { localStorage.removeItem(SESSION); sessionStorage.removeItem(PENDING_ORDER); },
    hasPendingOrder() { return sessionStorage.getItem(PENDING_ORDER) !== null; },
    setPendingOrder(value) { sessionStorage.setItem(PENDING_ORDER, JSON.stringify(value)); },
    takePendingOrder() { const value = sessionStorage.getItem(PENDING_ORDER); sessionStorage.removeItem(PENDING_ORDER); return value ? JSON.parse(value) : null; },
    apply(input) {
      const user = currentUser();
      if (!user || user.role !== 'user') throw new Error('Hanya user biasa yang dapat mengajukan organizer.');
      const previous = latestApplication(user.id);
      if (previous && previous.status !== 'rejected') throw new Error('Pengajuanmu sudah dikirim.');
      const initialEvent = eventInput(input);
      validateEvent(initialEvent);
      if (initialEvent.slots < 1) throw new Error('Event pertama perlu memiliki minimal 1 slot tiket.');
      if (!input.fullName.trim() || !input.contact.trim() || !input.organization.trim()) throw new Error('Lengkapi identitas dan nama organisasi.');
      const data = read();
      const application = { id: uid(), userId: user.id, fullName: input.fullName.trim(), contact: input.contact.trim(), organization: input.organization.trim(), initialEvent, status: 'pending', reason: '' };
      data.applications.push(application); save(data); return application;
    },
    decideApplication(id, status, reason = '') {
      if (currentUser()?.role !== 'admin') throw new Error('Akses ditolak.');
      const data = read();
      const application = data.applications.find((item) => item.id === id);
      if (!application || application.status !== 'pending') throw new Error('Pengajuan tidak tersedia.');
      if (status !== 'approved' && status !== 'rejected') throw new Error('Keputusan tidak valid.');
      if (status === 'rejected' && !reason.trim()) throw new Error('Isi alasan penolakan.');
      application.status = status; application.reason = reason.trim();
      if (status === 'approved') {
        const user = data.users.find((item) => item.id === application.userId);
        user.role = 'organizer'; user.fullName = application.fullName; user.organization = application.organization;
        data.events.push({ id: uid(), ownerId: user.id, ...application.initialEvent });
      }
      save(data); return application;
    },
    createEvent(input) {
      const user = currentUser();
      if (!user || user.role !== 'organizer') throw new Error('Akses ditolak.');
      const event = eventInput(input); validateEvent(event);
      const data = read(); data.events.push({ id: uid(), ownerId: user.id, ...event }); save(data);
    },
    updateEvent(id, input) {
      const user = currentUser();
      const data = read(); const event = data.events.find((item) => item.id === id);
      if (!event || !user || (user.role !== 'admin' && (user.role !== 'organizer' || event.ownerId !== user.id))) throw new Error('Event tidak dapat diubah.');
      const next = eventInput(input); validateEvent(next);
      Object.assign(event, next); save(data);
    },
    deleteEvent(id) {
      if (currentUser()?.role !== 'admin') throw new Error('Akses ditolak.');
      const data = read(); data.events = data.events.filter((item) => item.id !== id); save(data);
    },
    book(id, quantity) {
      const user = currentUser();
      if (!user || user.role === 'admin') throw new Error('Login sebagai user atau organizer untuk memesan.');
      const data = read(); const event = data.events.find((item) => item.id === id);
      if (!event || !Number.isInteger(quantity) || quantity < 1 || quantity > 10 || quantity > event.slots) throw new Error('Jumlah tiket melebihi batas atau slot tersedia.');
      event.slots -= quantity; save(data); return event;
    }
  };
})();
