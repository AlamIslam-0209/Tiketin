    <footer class="site-footer">
        <div class="container">
            <p>© 2026 Tiketin. Buat momenmu jadi lebih dekat.</p>
        </div>
    </footer>

    <!-- Lightbox global untuk perbesar foto event -->
    <div class="lightbox-backdrop" id="imageLightboxModal" role="dialog" aria-modal="true" aria-labelledby="lightboxTitle" aria-hidden="true" onclick="closeImageLightbox()">
        <div class="lightbox-dialog" onclick="event.stopPropagation()">
            <button type="button" class="lightbox-close" id="lightboxCloseBtn" aria-label="Tutup perbesaran foto" onclick="closeImageLightbox()">×</button>
            <div class="lightbox-stage">
                <img id="lightboxImg" src="" alt="Perbesaran foto acara" />
            </div>
            <div class="lightbox-footer">
                <strong class="lightbox-caption" id="lightboxTitle">Foto Acara</strong>
                <span class="lightbox-hint">Tekan ESC atau klik luar gambar untuk menutup</span>
            </div>
        </div>
    </div>

    <script>
        function openImageLightbox(src, title) {
            const modal = document.getElementById('imageLightboxModal');
            const img = document.getElementById('lightboxImg');
            const caption = document.getElementById('lightboxTitle');
            if (!modal || !img || !src) return;

            img.src = src;
            img.alt = title ? `Foto acara ${title}` : 'Perbesaran foto acara';
            if (caption) caption.textContent = title || 'Foto Acara';

            modal.classList.add('is-open');
            modal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('lightbox-open');
        }

        function closeImageLightbox() {
            const modal = document.getElementById('imageLightboxModal');
            const img = document.getElementById('lightboxImg');
            if (!modal) return;
            modal.classList.remove('is-open');
            modal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('lightbox-open');
            if (img) img.src = '';
        }

        function syncUploadWidgetButtons(widget) {
            if (!widget) return;
            const input = widget.querySelector('[data-upload-input]');
            const clearBtn = widget.querySelector('[data-upload-clear]');
            const badge = widget.querySelector('[data-upload-badge]');
            const hasNewFile = Boolean(input && input.files && input.files.length > 0);
            const hasSaved = widget.dataset.hasSaved === '1';
            const isRequired = widget.dataset.required === '1';

            if (clearBtn) {
                if (hasSaved && !hasNewFile) {
                    clearBtn.hidden = true;
                } else {
                    clearBtn.hidden = false;
                    const labelSpan = clearBtn.querySelector('span');
                    if (labelSpan) {
                        labelSpan.textContent = (hasSaved && hasNewFile) ? 'Batal ganti' : 'Hapus';
                    }
                }
            }
            if (badge) {
                badge.textContent = isRequired ? 'Wajib 1 foto · Rasio 16:9 utuh' : 'Opsional ganti · Rasio 16:9 utuh';
            }
        }

        function setUploadWidgetSavedImage(widgetId, imageUrl, imageName, isRequiredWhenEmpty = false) {
            const widget = document.getElementById(widgetId);
            if (!widget) return;
            const input = widget.querySelector('[data-upload-input]');
            const dropzone = widget.querySelector('[data-upload-dropzone]');
            const preview = widget.querySelector('[data-upload-preview]');
            const img = widget.querySelector('[data-upload-img]');
            const filenameEl = widget.querySelector('[data-upload-filename]');
            const metaEl = widget.querySelector('[data-upload-meta]');
            const errorEl = widget.querySelector('[data-upload-error]');

            if (input) input.value = '';
            if (errorEl) errorEl.textContent = '';

            if (imageUrl) {
                widget.dataset.hasSaved = '1';
                widget.dataset.savedUrl = imageUrl;
                widget.dataset.savedName = imageName || 'Foto acara tersimpan';
                widget.dataset.required = '0';

                if (img) img.src = imageUrl;
                if (filenameEl) filenameEl.textContent = imageName || 'Foto acara tersimpan';
                if (metaEl) metaEl.textContent = 'Foto acara aktif tersimpan · klik Ganti Foto bila ingin memperbarui';
                if (dropzone) dropzone.hidden = true;
                if (preview) preview.hidden = false;
            } else {
                widget.dataset.hasSaved = '0';
                widget.dataset.savedUrl = '';
                widget.dataset.savedName = '';
                widget.dataset.required = isRequiredWhenEmpty ? '1' : '0';

                if (img) img.src = '';
                if (filenameEl) filenameEl.textContent = '';
                if (metaEl) metaEl.textContent = '';
                if (dropzone) dropzone.hidden = false;
                if (preview) preview.hidden = true;
            }
            syncUploadWidgetButtons(widget);
        }

        function resetUploadWidget(widgetId, isRequired = true) {
            setUploadWidgetSavedImage(widgetId, '', '', isRequired);
        }

        function initEventImageDropzones() {
            const widgets = document.querySelectorAll('[data-upload-widget]');
            const allowedExt = ['jpg', 'jpeg', 'png', 'webp', 'gif'];
            const maxBytes = 5 * 1024 * 1024;

            widgets.forEach(widget => {
                const input = widget.querySelector('[data-upload-input]');
                const dropzone = widget.querySelector('[data-upload-dropzone]');
                const pickBtn = widget.querySelector('[data-upload-pick]');
                const preview = widget.querySelector('[data-upload-preview]');
                const img = widget.querySelector('[data-upload-img]');
                const zoomBtn = widget.querySelector('[data-upload-zoom]');
                const filenameEl = widget.querySelector('[data-upload-filename]');
                const metaEl = widget.querySelector('[data-upload-meta]');
                const replaceBtn = widget.querySelector('[data-upload-replace]');
                const clearBtn = widget.querySelector('[data-upload-clear]');
                const errorEl = widget.querySelector('[data-upload-error]');

                if (!input || !dropzone || !preview || !img) return;
                syncUploadWidgetButtons(widget);

                const showError = (msg) => {
                    if (errorEl) errorEl.textContent = msg || '';
                };

                const applyFile = (file, fromDrop) => {
                    showError('');
                    if (!file) return;

                    const ext = (file.name.split('.').pop() || '').toLowerCase();
                    if (!allowedExt.includes(ext)) {
                        input.value = '';
                        showError('Format file tidak didukung. Gunakan JPG, JPEG, PNG, WEBP, atau GIF.');
                        return;
                    }
                    if (file.size <= 0 || file.size > maxBytes) {
                        input.value = '';
                        showError('Ukuran foto melebihi batas maksimal 5 MB.');
                        return;
                    }

                    if (fromDrop && typeof DataTransfer !== 'undefined') {
                        const dt = new DataTransfer();
                        dt.items.add(file);
                        input.files = dt.files;
                    }

                    const reader = new FileReader();
                    reader.onload = (ev) => {
                        img.src = ev.target.result;
                        if (filenameEl) filenameEl.textContent = file.name;
                        if (metaEl) metaEl.textContent = `${(file.size / 1024).toFixed(1)} KB · Siap diunggah & disimpan`;
                        dropzone.hidden = true;
                        preview.hidden = false;
                        syncUploadWidgetButtons(widget);
                    };
                    reader.readAsDataURL(file);
                };

                if (pickBtn) {
                    pickBtn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        input.click();
                    });
                }
                if (replaceBtn) {
                    replaceBtn.addEventListener('click', () => input.click());
                }
                dropzone.addEventListener('click', () => input.click());
                dropzone.addEventListener('keydown', (e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        input.click();
                    }
                });

                input.addEventListener('change', () => {
                    if (input.files && input.files[0]) {
                        applyFile(input.files[0], false);
                    }
                });

                [dropzone, preview].forEach(zone => {
                    zone.addEventListener('dragover', (e) => {
                        e.preventDefault();
                        dropzone.classList.add('is-dragover');
                    });
                    zone.addEventListener('dragleave', () => {
                        dropzone.classList.remove('is-dragover');
                    });
                    zone.addEventListener('drop', (e) => {
                        e.preventDefault();
                        dropzone.classList.remove('is-dragover');
                        if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files[0]) {
                            applyFile(e.dataTransfer.files[0], true);
                        }
                    });
                });

                if (clearBtn) {
                    clearBtn.addEventListener('click', () => {
                        showError('');
                        if (widget.dataset.hasSaved === '1') {
                            setUploadWidgetSavedImage(
                                widget.id,
                                widget.dataset.savedUrl,
                                widget.dataset.savedName,
                                false
                            );
                        } else {
                            input.value = '';
                            img.src = '';
                            if (filenameEl) filenameEl.textContent = '';
                            if (metaEl) metaEl.textContent = '';
                            dropzone.hidden = false;
                            preview.hidden = true;
                            syncUploadWidgetButtons(widget);
                        }
                    });
                }

                if (zoomBtn) {
                    zoomBtn.addEventListener('click', () => {
                        if (img.src) {
                            openImageLightbox(img.src, filenameEl ? filenameEl.textContent : 'Pratinjau Foto Acara');
                        }
                    });
                }

                const parentForm = widget.closest('form');
                if (parentForm) {
                    parentForm.addEventListener('submit', (e) => {
                        const isReq = widget.dataset.required === '1';
                        const hasSaved = widget.dataset.hasSaved === '1';
                        const hasNew = Boolean(input.files && input.files.length > 0);
                        if (isReq && !hasSaved && !hasNew) {
                            e.preventDefault();
                            showError('Foto acara (rasio 16:9) wajib diunggah sebelum menyimpan.');
                            widget.scrollIntoView({ behavior: 'smooth', block: 'center' });
                            if (pickBtn) pickBtn.focus();
                            else dropzone.focus();
                        }
                    });
                }
            });
        }

        document.addEventListener('DOMContentLoaded', () => {
            initEventImageDropzones();

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

            document.addEventListener('keydown', (e) => {
                const lightbox = document.getElementById('imageLightboxModal');
                if (e.key === 'Escape' && lightbox && lightbox.classList.contains('is-open')) {
                    closeImageLightbox();
                }
            });
        });
    </script>
</body>
</html>
