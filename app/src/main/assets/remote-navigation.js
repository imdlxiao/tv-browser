/* Author: imdlxiao. ES5 fallback for old TV WebViews; no privileged JavaScript interface. */
(function () {
  if (window.__tvBrowserNavigate) return;
  var style = document.createElement('style');
  style.textContent = ':focus{outline:4px solid #50dcb0!important;outline-offset:3px!important}';
  document.head.appendChild(style);
  function controls() {
    var dialogs = document.querySelectorAll('dialog[open]');
    var root = dialogs.length ? dialogs[dialogs.length - 1] : document;
    return Array.prototype.filter.call(root.querySelectorAll('button,a[href],input,select,textarea,summary,[tabindex],[role="button"]'), function (node) {
      var rect = node.getBoundingClientRect();
      return !node.disabled && node.tabIndex >= 0 && rect.width > 0 && rect.height > 0 && getComputedStyle(node).visibility !== 'hidden';
    });
  }
  window.__tvBrowserNavigate = function (key) {
    if (window.MemoirTV && typeof window.MemoirTV.handleKey === 'function') return window.MemoirTV.handleKey(key);
    var active = document.activeElement, list = controls(), rect, best, score = Infinity;
    if (key.indexOf('Media') === 0) {
      var video = document.querySelector('video');
      if (!video) return 'native';
      if (key === 'MediaFastForward' && isFinite(video.duration)) video.currentTime = Math.min(video.duration, video.currentTime + 10);
      else if (key === 'MediaRewind') video.currentTime = Math.max(0, video.currentTime - 10);
      else if (key === 'MediaPause' || (key === 'MediaPlayPause' && !video.paused)) video.pause();
      else { var promise = video.play(); if (promise) promise.catch(function () {}); }
      return 'handled';
    }
    if (key === 'Back') {
      var dialogs = document.querySelectorAll('dialog[open]');
      if (dialogs.length && dialogs[dialogs.length - 1].close) { dialogs[dialogs.length - 1].close(); return 'handled'; }
      return 'unhandled';
    }
    if (key === 'Focus' || list.indexOf(active) < 0) {
      if (list.length) { list[0].focus(); return 'handled'; }
    }
    if (key === 'Focus') return 'handled';
    var horizontal = key === 'ArrowLeft' || key === 'ArrowRight';
    if (active && /INPUT|TEXTAREA/.test(active.tagName) && horizontal) return 'native';
    if (active && active.tagName === 'SELECT' && horizontal) {
      active.selectedIndex = Math.max(0, Math.min(active.options.length - 1, active.selectedIndex + (key === 'ArrowLeft' ? -1 : 1)));
      active.dispatchEvent(new Event('change', { bubbles: true })); return 'handled';
    }
    rect = active ? active.getBoundingClientRect() : { left:0, top:0, width:0, height:0 };
    var sign = key === 'ArrowLeft' || key === 'ArrowUp' ? -1 : 1;
    list.forEach(function (node) {
      if (node === active) return;
      var r = node.getBoundingClientRect(), dx = r.left + r.width / 2 - rect.left - rect.width / 2, dy = r.top + r.height / 2 - rect.top - rect.height / 2;
      var along = (horizontal ? dx : dy) * sign, cross = Math.abs(horizontal ? dy : dx), rank = along + 3 * cross;
      if (along > 4 && rank < score) { best = node; score = rank; }
    });
    if (best) { best.focus(); best.scrollIntoView({ block:'nearest', inline:'nearest' }); return 'handled'; }
    if (key === 'ArrowUp' && window.pageYOffset <= 0) return 'toolbar';
    if (!horizontal) window.scrollBy(0, sign * window.innerHeight * .6);
    return 'handled';
  };
}());
