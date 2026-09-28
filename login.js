const loginForm = document.querySelector('#loginForm');
const loginError = document.querySelector('#loginError');
const store = window.TiketinStore;

loginForm.addEventListener('submit', (event) => {
  event.preventDefault();

  loginError.textContent = '';
  try {
    const user = store.login(document.querySelector('#username').value, document.querySelector('#password').value);
    location.href = user.role !== 'admin' && store.hasPendingOrder() ? 'index.html#tickets' : store.homeFor(user);
  } catch (error) { loginError.textContent = error.message; }
});
