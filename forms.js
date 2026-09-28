(() => {
  const store = window.TiketinStore;
  const register = document.querySelector('#registerForm');
  if (register) register.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      store.register(Object.fromEntries(new FormData(register)));
      location.href = 'index.html#home';
    } catch (error) { document.querySelector('#registerError').textContent = error.message; }
  });

  const application = document.querySelector('#applicationForm');
  if (!application) return;
  const user = store.requireRole('user');
  if (!user) return;
  document.querySelector('#fullName').value = user.fullName || '';
  const previous = store.latestApplication(user.id);
  const notice = document.querySelector('#applicationNotice');
  if (previous?.status === 'pending') {
    notice.hidden = false; notice.textContent = 'Pengajuanmu sedang ditinjau admin.'; application.hidden = true;
  } else if (previous?.status === 'rejected') {
    notice.hidden = false; notice.textContent = `Pengajuan sebelumnya ditolak: ${previous.reason} Kamu dapat memperbaikinya dan mengajukan ulang.`;
    const old = previous.initialEvent;
    for (const key of ['fullName', 'contact', 'organization']) application.elements[key].value = previous[key];
    for (const key of ['name', 'date', 'price', 'slots', 'category', 'description']) application.elements[key].value = old[key];
  }
  application.addEventListener('submit', (event) => {
    event.preventDefault();
    try {
      store.apply(Object.fromEntries(new FormData(application)));
      location.reload();
    } catch (error) { document.querySelector('#applicationError').textContent = error.message; }
  });
})();
