/* Cloud Vault client: theme, CSRF, real upload progress (actual bytes). */
(function () {
  'use strict';

  // ---- Theme (light / dark / system) ----
  var root = document.documentElement;
  function applyTheme(t) {
    if (t === 'system' || !t) {
      root.removeAttribute('data-theme');
      var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
      root.style.colorScheme = prefersDark ? 'dark' : 'light';
    } else {
      root.setAttribute('data-theme', t);
      root.style.colorScheme = t;
    }
  }
  var stored = localStorage.getItem('cv-theme') || 'system';
  applyTheme(stored);
  var toggle = document.getElementById('theme-toggle');
  if (toggle) {
    toggle.addEventListener('click', function () {
      var current = localStorage.getItem('cv-theme') || 'system';
      var isDark = current === 'dark' || (current === 'system' &&
        window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches);
      var next = isDark ? 'light' : 'dark';
      localStorage.setItem('cv-theme', next);
      applyTheme(next);
    });
  }

  // ---- CSRF helpers for XHR/fetch ----
  function readCookie(name) {
    var m = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[1]) : '';
  }
  window.cloudVaultCsrf = function () {
    var header = (document.querySelector('meta[name="csrf-header"]') || {}).content || 'X-XSRF-TOKEN';
    return { header: header, token: readCookie('XSRF-TOKEN') };
  };

  // ---- Destructive action confirmation ----
  document.addEventListener('submit', function (e) {
    var form = e.target;
    if (form.hasAttribute('data-confirm') && !window.confirm(form.getAttribute('data-confirm'))) {
      e.preventDefault();
    }
  });

  // ---- Upload with real byte progress ----
  var fileInput = document.getElementById('upload-input');
  var uploadList = document.getElementById('upload-list');
  if (fileInput) {
    var folderId = fileInput.getAttribute('data-folder-id');
    fileInput.addEventListener('change', function () {
      Array.prototype.forEach.call(fileInput.files, uploadOne);
      fileInput.value = '';
    });
    var zone = document.getElementById('upload-zone');
    if (zone) {
      ['dragover', 'dragenter'].forEach(function (ev) {
        zone.addEventListener(ev, function (e) { e.preventDefault(); zone.classList.add('drag'); });
      });
      ['dragleave', 'drop'].forEach(function (ev) {
        zone.addEventListener(ev, function (e) { e.preventDefault(); zone.classList.remove('drag'); });
      });
      zone.addEventListener('drop', function (e) {
        Array.prototype.forEach.call(e.dataTransfer.files, uploadOne);
      });
    }
  }

  function human(bytes) {
    var units = ['B', 'KB', 'MB', 'GB', 'TB'];
    var i = 0, v = bytes;
    while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
    return (i === 0 ? v : v.toFixed(1)) + ' ' + units[i];
  }

  function uploadOne(file) {
    if (!uploadList) return;
    var li = document.createElement('li');
    var label = document.createElement('span');
    label.textContent = file.name + ' — ';
    var pct = document.createElement('span');
    pct.textContent = '0% (0 / ' + human(file.size) + ')';
    var bar = document.createElement('div');
    bar.className = 'progress';
    var fill = document.createElement('div');
    bar.appendChild(fill);
    li.appendChild(label); li.appendChild(pct); li.appendChild(bar);
    uploadList.appendChild(li);

    var xhr = new XMLHttpRequest();
    var folder = fileInput ? fileInput.getAttribute('data-folder-id') : '';
    var url = '/app/upload' + (folder ? '?folderId=' + encodeURIComponent(folder) : '');
    xhr.open('POST', url);
    var csrf = window.cloudVaultCsrf();
    if (csrf.token) xhr.setRequestHeader(csrf.header, csrf.token);
    xhr.upload.addEventListener('progress', function (e) {
      if (e.lengthComputable) {
        var done = e.loaded, total = e.total;
        fill.style.width = Math.round((done / total) * 100) + '%';
        pct.textContent = Math.round((done / total) * 100) + '% (' + human(done) + ' / ' + human(total) + ')';
      }
    });
    xhr.addEventListener('load', function () {
      if (xhr.status >= 200 && xhr.status < 300) {
        pct.textContent = 'done (' + human(file.size) + ')';
        fill.style.width = '100%';
        setTimeout(function () { window.location.reload(); }, 500);
      } else {
        var msg = 'failed';
        try {
          var body = JSON.parse(xhr.responseText);
          msg = body.detail || body.message || body.error || msg;
        } catch (ignored) { /* non-JSON error body */ }
        pct.textContent = 'failed: ' + msg;
        li.style.color = 'var(--danger)';
      }
    });
    xhr.addEventListener('error', function () {
      pct.textContent = 'failed: network error';
      li.style.color = 'var(--danger)';
    });
    xhr.send(file);
  }
})();
