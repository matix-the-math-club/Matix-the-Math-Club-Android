/* =====================================================================
   Matix SVG layer
   1) Every icon/logo that used to be an inline SVG string now lives in its
      own file under assets/svg/. mxSvgTag(path, cls) returns the cached
      markup, or a same-size placeholder that is swapped for the real <svg>
      as soon as the file has loaded (CSS that targets "svg" keeps working).
   Only Matix's own artwork is bundled - no third-party image packs.
   ===================================================================== */
(function(){
  'use strict';
  var CACHE = Object.create(null), PENDING = Object.create(null);
  var PRELOAD = ["svg/brand/matix-tile.svg", "svg/brand/profile-badge.svg", "svg/social/discord.svg", "svg/social/github.svg", "svg/social/globe.svg", "svg/social/instagram.svg", "svg/social/link.svg", "svg/social/mail.svg", "svg/social/reddit.svg", "svg/social/user.svg", "svg/social/x.svg", "svg/social/youtube.svg", "svg/learn/abacus.svg", "svg/learn/arrowL.svg", "svg/learn/bolt.svg", "svg/learn/book.svg", "svg/learn/book2.svg", "svg/learn/books.svg", "svg/learn/box.svg", "svg/learn/brain.svg", "svg/learn/bulb.svg", "svg/learn/calendar.svg", "svg/learn/cart.svg", "svg/learn/chart.svg", "svg/learn/check.svg", "svg/learn/compass.svg", "svg/learn/cross.svg", "svg/learn/crown.svg", "svg/learn/divide.svg", "svg/learn/drop.svg", "svg/learn/equals.svg", "svg/learn/flame.svg", "svg/learn/fraction.svg", "svg/learn/gem.svg", "svg/learn/gift.svg", "svg/learn/graph.svg", "svg/learn/heart.svg", "svg/learn/heartOff.svg", "svg/learn/key.svg", "svg/learn/lock.svg", "svg/learn/map.svg", "svg/learn/medal.svg", "svg/learn/minus.svg", "svg/learn/num123.svg", "svg/learn/owl.svg", "svg/learn/page.svg", "svg/learn/pencil.svg", "svg/learn/people.svg", "svg/learn/pizza.svg", "svg/learn/plus.svg", "svg/learn/plusUser.svg", "svg/learn/protractor.svg", "svg/learn/puzzle.svg", "svg/learn/ruler.svg", "svg/learn/scales.svg", "svg/learn/seed.svg", "svg/learn/snow.svg", "svg/learn/sparkle.svg", "svg/learn/star.svg", "svg/learn/target.svg", "svg/learn/times.svg", "svg/learn/trash.svg", "svg/learn/trophy.svg"];
  function esc(v){ return String(v).replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/</g,'&lt;'); }
  function clean(txt){
    var i = txt.indexOf('<svg'); if(i > 0) txt = txt.slice(i);
    return txt.replace(/<!--[\s\S]*?-->/g,'').trim();
  }
  function withClass(markup, cls){
    if(!cls) return markup;
    if(/^<svg[^>]*\sclass="/.test(markup)) return markup.replace(/^(<svg[^>]*\sclass=")/, '$1'+esc(cls)+' ');
    return markup.replace(/^<svg/, '<svg class="'+esc(cls)+'"');
  }
  function hydrateAll(root){
    var list = (root || document).querySelectorAll ? (root || document).querySelectorAll('.mx-svgf[data-svg]') : [];
    for(var i=0;i<list.length;i++) hydrate(list[i]);
  }
  function hydrate(ph){
    var src = ph.getAttribute('data-svg'), markup = CACHE[src];
    if(!markup){ load(src); return; }
    if(!ph.parentNode) return;
    var t = document.createElement('template');
    t.innerHTML = withClass(markup, ph.getAttribute('data-cls') || '');
    var svg = t.content.firstElementChild;
    if(svg) ph.parentNode.replaceChild(svg, ph);
  }
  function load(src){
    if(CACHE[src] || PENDING[src]) return PENDING[src];
    PENDING[src] = fetch(src).then(function(r){ if(!r.ok) throw new Error(r.status); return r.text(); })
      .then(function(txt){ CACHE[src] = clean(txt); delete PENDING[src];
        var ph = document.querySelectorAll('.mx-svgf[data-svg="'+src+'"]');
        for(var i=0;i<ph.length;i++) hydrate(ph[i]);
      })['catch'](function(){ delete PENDING[src]; });
    return PENDING[src];
  }
  window.mxSvgTag = function(src, cls){
    if(CACHE[src]) return withClass(CACHE[src], cls);
    load(src);
    return '<span class="mx-svgf'+(cls?' '+esc(cls):'')+'" data-svg="'+esc(src)+'" data-cls="'+esc(cls||'')+'" aria-hidden="true"></span>';
  };
  window.mxSvgLoad = load;
  window.mxSvgReady = function(){ return Promise.all(PRELOAD.map(load)); };
  PRELOAD.forEach(load);

  /* hydrate SVG placeholders that are added later by the app */
  var mo = new MutationObserver(function(muts){
    for(var i=0;i<muts.length;i++){
      var add = muts[i].addedNodes;
      for(var j=0;j<add.length;j++){
        var a = add[j];
        if(a.nodeType !== 1) continue;
        if(a.classList && a.classList.contains('mx-svgf')) hydrate(a);
        else if(a.querySelector && a.querySelector('.mx-svgf')) hydrateAll(a);
      }
    }
  });
  mo.observe(document.documentElement, {childList:true, subtree:true});
  function first(){ hydrateAll(document); }
  if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', first); else first();
})();
