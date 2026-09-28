(() => {
  const store = window.TiketinStore;
  const role = document.body.dataset.dashboard;
  const current = store.requireRole(role);
  if (!current) return;
  const form = document.querySelector('#eventForm');
  const list = document.querySelector('#eventList');
  const el = (tag, className, value) => { const node = document.createElement(tag); if (className) node.className = className; if (value != null) node.textContent = value; return node; };
  const button = (label, className, action) => { const node = el('button', `button ${className}`, label); node.type = 'button'; node.addEventListener('click', action); return node; };

  function renderSummary() {
    const events = store.listEvents();
    const entries = role === 'admin'
      ? [['Akun', store.listUsers().length], ['Menunggu persetujuan', store.listApplications().filter((item) => item.status === 'pending').length], ['Event tayang', events.length]]
      : [['Event milikmu', events.filter((item) => item.ownerId === current.id).length], ['Sisa tiket', events.filter((item) => item.ownerId === current.id).reduce((sum, item) => sum + item.slots, 0)]];
    const summary = document.querySelector('#dashboardSummary'); summary.replaceChildren();
    entries.forEach(([label, value]) => { const item = el('div', 'summary-item'); item.append(el('strong', '', value), el('span', '', label)); summary.append(item); });
  }

  function renderEvents() {
    const users = store.listUsers();
    const events = store.listEvents().filter((item) => role === 'admin' || item.ownerId === current.id);
    list.replaceChildren(); document.querySelector('#eventCount').textContent = `${events.length} event`;
    if (!events.length) { list.append(el('p', 'empty-state', 'Belum ada event.')); return; }
    events.forEach((event) => {
      const row = el('article', 'management-row');
      const text = el('div', 'management-copy');
      text.append(el('h3', '', event.name), el('p', '', `${store.formatDate(event.date)} · ${store.formatPrice(event.price)} · ${event.slots} slot tersisa`));
      if (role === 'admin') text.append(el('small', '', `Pemilik: ${users.find((item) => item.id === event.ownerId)?.username || 'Tidak ditemukan'}`));
      const actions = el('div', 'row-actions');
      actions.append(button('Edit', 'button-quiet', () => editEvent(event.id)));
      if (role === 'admin') actions.append(button('Hapus', 'button-danger', () => {
        if (!confirm(`Hapus event “${event.name}”?`)) return;
        try { store.deleteEvent(event.id); resetForm(); render(); } catch (error) { alert(error.message); }
      }));
      row.append(text, actions); list.append(row);
    });
  }

  function editEvent(id) {
    const event = store.listEvents().find((item) => item.id === id);
    if (!event || (role !== 'admin' && event.ownerId !== current.id)) return;
    for (const key of ['name', 'date', 'price', 'slots', 'category', 'description']) form.elements[key].value = event[key];
    form.elements.eventId.value = id; form.hidden = false;
    document.querySelector('#eventFormTitle').textContent = `Edit ${event.name}`;
    if (role === 'organizer') { document.querySelector('#eventSubmit').textContent = 'Simpan perubahan'; document.querySelector('#cancelEdit').hidden = false; }
    form.scrollIntoView({ behavior: 'smooth', block: 'center' }); form.elements.name.focus();
  }

  function resetForm() {
    form.reset(); form.elements.eventId.value = ''; document.querySelector('#eventError').textContent = '';
    document.querySelector('#eventFormTitle').textContent = role === 'admin' ? 'Edit event' : 'Tambah event';
    if (role === 'admin') form.hidden = true;
    else { document.querySelector('#eventSubmit').textContent = 'Simpan event'; document.querySelector('#cancelEdit').hidden = true; }
  }

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const input = Object.fromEntries(new FormData(form));
    try {
      if (input.eventId) store.updateEvent(input.eventId, input);
      else store.createEvent(input);
      resetForm(); render();
    } catch (error) { document.querySelector('#eventError').textContent = error.message; }
  });
  document.querySelector('#cancelEdit').addEventListener('click', resetForm);

  function renderApplications() {
    const target = document.querySelector('#applicationList'); target.replaceChildren();
    const applications = store.listApplications().slice().reverse();
    document.querySelector('#applicationCount').textContent = `${applications.filter((item) => item.status === 'pending').length} menunggu`;
    if (!applications.length) { target.append(el('p', 'empty-state', 'Belum ada pengajuan.')); return; }
    applications.forEach((application) => {
      const row = el('article', 'application-row');
      const heading = el('div', 'application-heading');
      heading.append(el('h3', '', application.organization), el('span', `status status-${application.status}`, ({ pending: 'Menunggu', approved: 'Disetujui', rejected: 'Ditolak' })[application.status]));
      row.append(heading, el('p', '', `${application.fullName} · ${application.contact}`), el('p', '', `${application.initialEvent.name} · ${store.formatDate(application.initialEvent.date)} · ${store.formatPrice(application.initialEvent.price)} · ${application.initialEvent.slots} slot`));
      if (application.reason) row.append(el('p', 'rejection-reason', `Alasan: ${application.reason}`));
      if (application.status === 'pending') {
        const controls = el('div', 'application-controls');
        const label = el('label', '', 'Alasan penolakan');
        const reason = el('input'); reason.type = 'text'; reason.placeholder = 'Wajib diisi jika ditolak'; reason.id = `reason-${application.id}`; label.htmlFor = reason.id;
        const error = el('p', 'form-error'); error.setAttribute('role', 'alert');
        const actions = el('div', 'row-actions');
        actions.append(button('Setujui', 'button-primary', () => { try { store.decideApplication(application.id, 'approved'); render(); } catch (err) { error.textContent = err.message; } }), button('Tolak', 'button-danger', () => { try { store.decideApplication(application.id, 'rejected', reason.value); render(); } catch (err) { error.textContent = err.message; reason.focus(); } }));
        controls.append(label, reason, error, actions); row.append(controls);
      }
      target.append(row);
    });
  }

  function renderUsers() {
    const target = document.querySelector('#userList'); target.replaceChildren();
    const users = store.listUsers(); document.querySelector('#userCount').textContent = `${users.length} akun`;
    users.forEach((user) => { const row = el('div', 'management-row'); const copy = el('div', 'management-copy'); copy.append(el('h3', '', user.username), el('p', '', `${user.fullName} · ${user.organization || 'Tanpa organisasi'}`)); row.append(copy, el('span', 'status', user.role)); target.append(row); });
  }

  function render() { renderSummary(); renderEvents(); if (role === 'admin') { renderApplications(); renderUsers(); } }
  render();
})();
