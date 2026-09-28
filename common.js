(() => {
  const store = window.TiketinStore;
  const slot = document.querySelector('#accountSlot');
  if (!slot) return;
  const user = store.currentUser();
  if (!user) {
    slot.innerHTML = '<a class="nav-link" href="login.html">Login</a>';
    return;
  }

  slot.innerHTML = `
    <button class="nav-link account-trigger" id="accountTrigger" type="button" aria-expanded="false" aria-controls="accountMenu">Akun <span aria-hidden="true">⌄</span></button>
    <div class="account-menu" id="accountMenu" hidden>
      <strong id="accountName"></strong>
      <span class="account-role" id="accountRole"></span>
      <div id="accountStatus"></div>
      <div id="accountActions"></div>
      <button class="account-action logout-action" id="logoutButton" type="button" href="login.html">Logout</button>
    </div>`;
  document.querySelector('#accountName').textContent = user.username;
  document.querySelector('#accountRole').textContent = user.role === 'admin' ? 'Admin' : user.role === 'organizer' ? 'Organizer' : 'User';
  const actions = document.querySelector('#accountActions');
  const status = document.querySelector('#accountStatus');
  if (user.role === 'user') {
    const application = store.latestApplication(user.id);
    if (application?.status === 'pending') status.textContent = 'Pengajuan organizer menunggu persetujuan.';
    if (application?.status === 'rejected') status.textContent = `Pengajuan ditolak: ${application.reason}`;
    if (!application || application.status === 'rejected') actions.innerHTML = '<a class="account-action" href="apply.html">Ajukan jadi organizer</a>';
  } else {
    actions.innerHTML = `<a class="account-action" href="${store.homeFor(user)}">Dashboard ${user.role === 'admin' ? 'admin' : 'organizer'}</a>`;
  }

  const trigger = document.querySelector('#accountTrigger');
  const menu = document.querySelector('#accountMenu');
  const setOpen = (open) => { menu.hidden = !open; trigger.setAttribute('aria-expanded', String(open)); };
  trigger.addEventListener('click', () => setOpen(menu.hidden));
  document.addEventListener('click', (event) => { if (!slot.contains(event.target)) setOpen(false); });
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape' && !menu.hidden) { setOpen(false); trigger.focus(); } });
  document.querySelector('#logoutButton').addEventListener('click', () => { store.logout(); location.href = 'login.html'; });
})();
