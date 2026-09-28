const store = window.TiketinStore;
const grid = document.querySelector('#ticketGrid');
const modal = document.querySelector('#orderModal');
const orderForm = document.querySelector('#orderForm');
const quantityInput = document.querySelector('#ticketQuantity');
const formError = document.querySelector('#formError');
const authWarning = document.querySelector('#authWarning');
const toast = document.querySelector('#toast');
let selectedId = null;
let lastTrigger = null;
let toastTimer;


function renderEvents() {
  const search = document.querySelector('#eventSearch').value.trim().toLowerCase();
  const categoryFilter = document.querySelector('#categoryFilter').value;
  const events = store.listEvents().filter((item) => item.name.toLowerCase().includes(search) && (!categoryFilter || item.category === categoryFilter));
  grid.replaceChildren();
  if (!events.length) { grid.innerHTML = '<p class="empty-state">Tidak ada event yang cocok.</p>'; return; }
  events.forEach((event) => {
    const card = document.createElement('article');
    card.className = 'ticket-card';
    const top = document.createElement('div'); top.className = 'card-topline';
    const icon = document.createElement('span'); icon.className = `category-icon ${['Konser', 'Wisata', 'Seminar'].includes(event.category) ? event.category.toLowerCase() : 'other'}`; icon.setAttribute('aria-hidden', 'true'); icon.textContent = ({ Konser: '♪', Wisata: '◇', Seminar: '▦' })[event.category] || '✦';
    const category = document.createElement('span'); category.className = 'category-label'; category.textContent = event.category;
    top.append(icon, category);
    const title = document.createElement('h3'); title.textContent = event.name;
    const description = document.createElement('p'); description.textContent = event.description || 'Temukan pengalaman seru di event ini.';
    const details = document.createElement('div'); details.className = 'ticket-details';
    const price = document.createElement('strong'); price.textContent = store.formatPrice(event.price);
    const slots = document.createElement('span'); slots.className = event.slots ? '' : 'sold-out'; slots.textContent = event.slots ? `${event.slots} tiket tersedia` : 'Tiket habis';
    details.append(price, slots);
    const footer = document.createElement('div'); footer.className = 'card-footer';
    const date = document.createElement('span'); date.className = 'event-meta'; date.textContent = store.formatDate(event.date);
    const button = document.createElement('button'); button.className = 'button button-card order-button'; button.type = 'button'; button.textContent = event.slots ? 'Pesan' : 'Habis'; button.disabled = !event.slots;
    button.addEventListener('click', () => openModal(event.id, button));
    footer.append(date, button); card.append(top, title, description, details, footer); grid.append(card);
  });
}

function openModal(id, trigger, previousQuantity = 1) {
  const event = store.listEvents().find((item) => item.id === id);
  if (!event || event.slots < 1) return;
  selectedId = id; lastTrigger = trigger;
  document.querySelector('#selectedEvent').textContent = event.name;
  document.querySelector('#selectedPrice').textContent = store.formatPrice(event.price);
  document.querySelector('#selectedSlots').textContent = `${event.slots} tiket tersedia`;
  quantityInput.max = String(Math.min(10, event.slots));
  quantityInput.value = String(Math.min(Math.max(1, previousQuantity), 10));
  formError.textContent = ''; authWarning.hidden = true;
  modal.inert = false;
  document.querySelectorAll('header, main, footer').forEach((item) => { item.inert = true; });
  modal.classList.add('is-open'); modal.setAttribute('aria-hidden', 'false');
  document.body.classList.add('modal-open'); quantityInput.focus();
}

function closeModal() {
  modal.classList.remove('is-open'); modal.setAttribute('aria-hidden', 'true');
  modal.inert = true;
  document.querySelectorAll('header, main, footer').forEach((item) => { item.inert = false; });
  document.body.classList.remove('modal-open');
  lastTrigger?.focus();
}

function showToast(message) {
  clearTimeout(toastTimer); toast.textContent = message; toast.classList.add('is-visible');
  toastTimer = setTimeout(() => toast.classList.remove('is-visible'), 4200);
}

document.querySelector('#closeModal').addEventListener('click', closeModal);
modal.addEventListener('click', (event) => { if (event.target === modal) closeModal(); });
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && modal.classList.contains('is-open')) closeModal();
  if (event.key === 'Tab' && modal.classList.contains('is-open')) {
    const focusables = [...modal.querySelectorAll('button:not([disabled]), input, a[href]')].filter((item) => item.offsetParent !== null);
    const first = focusables[0], last = focusables.at(-1);
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
});

orderForm.addEventListener('submit', (event) => {
  event.preventDefault();
  const quantity = Number(quantityInput.value);
  const selected = store.listEvents().find((item) => item.id === selectedId);
  if (!selected || !Number.isInteger(quantity) || quantity < 1 || quantity > 10 || quantity > selected.slots) {
    formError.textContent = `Masukkan 1 sampai ${Math.min(10, selected?.slots || 0)} tiket sesuai slot tersedia.`;
    authWarning.hidden = true; quantityInput.focus(); return;
  }
  formError.textContent = '';
  if (!store.currentUser()) {
    authWarning.hidden = false;
    store.setPendingOrder({ eventId: selectedId, quantity });
    authWarning.querySelector('a').focus(); return;
  }
  try { 
    store.book(selectedId, quantity);
    closeModal(); renderEvents();
    showToast(`${quantity} tiket untuk ${selected.name} berhasil dipesan.`);
  } catch (error) { formError.textContent = error.message; }
});

document.querySelector('#eventSearch').addEventListener('input', renderEvents);
document.querySelector('#categoryFilter').addEventListener('change', renderEvents);
renderEvents();
const pending = store.currentUser()?.role !== 'admin' ? store.takePendingOrder() : null;
if (pending) {
  const trigger = [...grid.querySelectorAll('.order-button')].find((button) => {
    const card = button.closest('.ticket-card');
    return card?.querySelector('h3')?.textContent === store.listEvents().find((item) => item.id === pending.eventId)?.name;
  });
  if (trigger) openModal(pending.eventId, trigger, pending.quantity);
}
