    <footer class="site-footer">
        <div class="container">
            <p>© 2026 Tiketin. Buat momenmu jadi lebih dekat.</p>
        </div>
    </footer>

    <!-- Minimal Script untuk Interaktivitas Dropdown Akun -->
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const trigger = document.getElementById('accountTrigger');
            const menu = document.getElementById('accountMenu');
            const slot = document.getElementById('accountSlot');

            if (trigger && menu) {
                const toggle = (show) => {
                    menu.hidden = !show;
                    trigger.setAttribute('aria-expanded', String(show));
                };

                trigger.addEventListener('click', (e) => {
                    e.stopPropagation();
                    toggle(menu.hidden);
                });

                document.addEventListener('click', (e) => {
                    if (slot && !slot.contains(e.target)) {
                        toggle(false);
                    }
                });

                document.addEventListener('keydown', (e) => {
                    if (e.key === 'Escape' && !menu.hidden) {
                        toggle(false);
                        trigger.focus();
                    }
                });
            }
        });
    </script>
</body>
</html>
