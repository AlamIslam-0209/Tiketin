(() => {
  const store = window.TiketinStore;
  const user = store.currentUser();
  if (!user) { location.replace('login.html'); return; }
  if (user.role === 'admin') { location.replace(store.homeFor(user)); return; }

  const METHOD_LABELS = { qris: 'QRIS', bca: 'Bank BCA', bri: 'Bank BRI', bsi: 'Bank BSI', mandiri: 'Bank Mandiri' };

  const today = new Date();
  const todayKey = [today.getFullYear(), String(today.getMonth() + 1).padStart(2, '0'), String(today.getDate()).padStart(2, '0')].join('-');
  const orders = store.listMyOrders();
  const groups = {
    upcoming: orders.filter((order) => order.eventDate >= todayKey),
    history: orders.filter((order) => order.eventDate < todayKey)
  };

  function element(tag, className, content) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (content !== undefined) node.textContent = content;
    return node;
  }

  function ticketCard(order, isHistory) {
    const card = element('article', 'my-ticket-card');
    const top = element('div', 'my-ticket-top');
    const category = element('span', 'my-ticket-category', order.category || 'Event');

    // Status badge: use order.status if available, else fallback to date-based
    const orderStatus = order.status || 'menunggu';
    let statusLabel, statusClass;
    if (orderStatus === 'dibatalkan') {
      statusLabel = 'Dibatalkan';
      statusClass = 'badge badge-dibatalkan';
    } else if (orderStatus === 'berhasil') {
      statusLabel = isHistory ? 'Selesai' : 'Berhasil';
      statusClass = 'badge badge-berhasil';
    } else {
      statusLabel = 'Menunggu';
      statusClass = 'badge badge-menunggu';
    }
    const status = element('span', statusClass, statusLabel);
    top.append(category, status);

    const title = element('h3', '', order.eventName);
    const details = element('dl', 'my-ticket-details');
    const methodLabel = METHOD_LABELS[order.paymentMethod] || order.paymentMethod || '-';
    const rows = [
      ['Tanggal acara', store.formatDate(order.eventDate)],
      ['Jumlah tiket', `${order.quantity} tiket`],
      ['Total harga', store.formatPrice(order.totalPrice)],
      ['Metode bayar', methodLabel],
      ['Dipesan', new Intl.DateTimeFormat('id-ID', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(order.purchasedAt))]
    ];
    rows.forEach(([label, value]) => {
      const row = element('div', 'my-ticket-detail');
      row.append(element('dt', '', label), element('dd', '', value));
      details.append(row);
    });
    card.append(top, title, details);
    return card;
  }

  function renderGroup(name, listId, countId, emptyText) {
    const list = document.getElementById(listId);
    const items = groups[name];
    document.getElementById(countId).textContent = `${items.length} pesanan`;
    if (!items.length) {
      const empty = element('div', 'my-ticket-empty');
      empty.append(element('p', '', emptyText));
      if (name === 'upcoming' && !orders.length) {
        const link = element('a', 'button button-primary', 'Jelajahi tiket →');
        link.href = 'index.html#tickets';
        empty.append(link);
      }
      list.append(empty);
      return;
    }
    items.forEach((order) => list.append(ticketCard(order, name === 'history')));
  }

  renderGroup('upcoming', 'upcomingList', 'upcomingCount', 'Belum ada tiket untuk acara mendatang.');
  renderGroup('history', 'historyList', 'historyCount', 'Belum ada riwayat tiket.');
})();
