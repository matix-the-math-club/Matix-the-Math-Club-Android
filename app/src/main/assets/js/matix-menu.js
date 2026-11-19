/* =====================================================================
   Matix menu (1.0.8) — a more accessible bottom menu
   - the bottom menu is always on screen (can be switched back to the
     ☰ button in More → "Always show the menu")
   - "More" opens a full menu: every section in big tiles, a search box,
     and quick settings: text size, high contrast, dark mode, reduce
     motion, bigger buttons, settings and customising the menu
   - real buttons with labels, aria-current on the open page, Esc to
     close, focus moves into the sheet and back
   ===================================================================== */
(function(){
'use strict';
var D=document,W=window;
function $(id){return D.getElementById(id);}
function T(s){try{return typeof W.tr==='function'?W.tr(s):s;}catch(e){return s;}}
function E(s){return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
function ls(k,v){try{if(v===undefined)return localStorage.getItem(k);if(v===null)localStorage.removeItem(k);else localStorage.setItem(k,v);}catch(e){return null;}}

var SECTIONS=[
  ['home','⌂','Home'],['learn','🦉','Math Learn'],['games','🎮','Games'],['ai','✨','Matix AI'],
  ['chat','🧮','Math Chat'],['friends','🤝','Friends'],['messages','🔔','Inbox'],['myuser','👤','My User'],
  ['points','⭐','Points'],['ideas','💡','Ideas'],['discussions','💬','Discussions'],['users','👥','Users'],
  ['translate','🌐','Translator'],['theme','🎨','Theme'],['bugs','🐞','Bugs']
];
var SIZES=[90,100,115,130];

/* ---------- preferences ---------- */
function always(){return ls('mx_dock_always')!=='0';}
function applyPrefs(){
  var b=D.body;
  b.classList.toggle('mx-dock-always',always());
  b.classList.toggle('mx-hc',ls('mx_hc')==='1');
  b.classList.toggle('mx-big',ls('mx_bigbtn')==='1');
  var ts=parseInt(ls('mx_textsize')||'100',10);if(SIZES.indexOf(ts)<0)ts=100;
  D.documentElement.style.fontSize=ts===100?'':(ts+'%');
}
function setAlways(on){ls('mx_dock_always',on?'1':'0');applyPrefs();}

/* ---------- dock: labels, current page, More button ---------- */
function decorate(){
  var dock=$('lbDock');if(!dock)return;
  dock.setAttribute('role','navigation');dock.setAttribute('aria-label',T('Main menu'));
  dock.querySelectorAll('button[data-dock]').forEach(function(b){
    var l=b.querySelector('small');b.type='button';
    if(l)b.setAttribute('aria-label',l.textContent);
  });
  var more=dock.querySelector('.mx-more');
  if(!more){
    more=D.createElement('button');more.type='button';more.className='mx-more';
    more.setAttribute('aria-haspopup','dialog');
    more.onclick=function(e){e.stopPropagation();openSheet(more);};
  }
  more.innerHTML='<span>☰</span><small>'+E(T('More'))+'</small>';more.setAttribute('aria-label',T('More: all sections and settings'));
  dock.appendChild(more);
  sync();
}
function sync(){
  var t=null;try{t=W.activeTab();}catch(e){}
  D.querySelectorAll('#lbDock button[data-dock]').forEach(function(b){
    if(t&&b.dataset.dock===t.kind)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current');
  });
}

/* ---------- the More sheet ---------- */
var lastFocus=null;
function openSheet(from){
  closeSheet(true);lastFocus=from||D.activeElement;
  var cur=null;try{cur=W.activeTab();}catch(e){}
  var ts=parseInt(ls('mx_textsize')||'100',10);
  var sh=D.createElement('div');sh.id='mxMore';sh.setAttribute('role','dialog');sh.setAttribute('aria-modal','true');sh.setAttribute('aria-label',T('Menu'));
  sh.innerHTML='<div class="mxmo-bg" data-close="1"></div><div class="mxmo-panel">'
    +'<div class="mxmo-head"><h2>'+E(T('Menu'))+'</h2><button type="button" class="mxmo-x" data-close="1" aria-label="'+E(T('Close menu'))+'">✕</button></div>'
    +'<input type="search" class="mxmo-q" id="mxmoQ" placeholder="'+E(T('Find a section…'))+'" aria-label="'+E(T('Find a section'))+'">'
    +'<div class="mxmo-grid" id="mxmoGrid">'+SECTIONS.map(function(s){
        return '<button type="button" class="mxmo-tile'+(cur&&cur.kind===s[0]?' on':'')+'" data-k="'+s[0]+'"'+(cur&&cur.kind===s[0]?' aria-current="page"':'')+'><span aria-hidden="true">'+s[1]+'</span>'+E(T(s[2]))+'</button>';
      }).join('')+'<button type="button" class="mxmo-tile" data-k="search"><span aria-hidden="true">🔍</span>'+E(T('Search'))+'</button></div>'
    +'<h3>'+E(T('Easier to use'))+'</h3>'
    +'<div class="mxmo-set"><span>🔡 '+E(T('Text size'))+'</span><div class="mxmo-seg" role="radiogroup" aria-label="'+E(T('Text size'))+'">'
      +SIZES.map(function(v){return '<button type="button" role="radio" aria-checked="'+(v===ts)+'" class="'+(v===ts?'on':'')+'" data-ts="'+v+'" style="font-size:'+(v/100)+'rem">A</button>';}).join('')+'</div></div>'
    +sw('hc','◑ '+T('High contrast'),ls('mx_hc')==='1')
    +sw('big','👆 '+T('Bigger buttons'),ls('mx_bigbtn')==='1')
    +sw('dark','🌙 '+T('Dark mode'),D.body.classList.contains('dark'))
    +sw('motion','🎞 '+T('Reduce motion'),D.body.classList.contains('no-motion'))
    +sw('always','🧭 '+T('Always show the menu'),always())
    +'<div class="mxmo-row"><button type="button" class="mxmo-btn" id="mxmoCustom">✏️ '+E(T('Customise the menu'))+'</button>'
    +'<button type="button" class="mxmo-btn" id="mxmoSet">⚙ '+E(T('All settings'))+'</button></div>'
    +'</div>';
  D.body.appendChild(sh);D.body.classList.add('mx-more-open');
  function sw(id,label,on){return '<button type="button" class="mxmo-set mxmo-sw" role="switch" aria-checked="'+on+'" data-sw="'+id+'"><span>'+E(label)+'</span><i class="'+(on?'on':'')+'"></i></button>';}
  sh.addEventListener('click',function(e){
    var t=e.target.closest('[data-close],[data-k],[data-ts],[data-sw]');if(!t)return;
    if(t.dataset.close){closeSheet();return;}
    if(t.dataset.k){var k=t.dataset.k;closeSheet();if(k==='search'){try{W.openPalette();}catch(x){}}else{try{W.openTab({kind:k});}catch(x){}}return;}
    if(t.dataset.ts){ls('mx_textsize',t.dataset.ts);applyPrefs();sh.querySelectorAll('[data-ts]').forEach(function(b){var on=b===t;b.classList.toggle('on',on);b.setAttribute('aria-checked',on);});return;}
    if(t.dataset.sw){
      var id=t.dataset.sw,on;
      if(id==='hc'){on=ls('mx_hc')!=='1';ls('mx_hc',on?'1':'0');}
      else if(id==='big'){on=ls('mx_bigbtn')!=='1';ls('mx_bigbtn',on?'1':'0');}
      else if(id==='always'){on=!always();setAlways(on);}
      else if(id==='dark'){var dt=$('darkToggle');if(dt)dt.click();on=D.body.classList.contains('dark');}
      else if(id==='motion'){var mb=$('tbMotionBtn');if(mb)mb.click();on=D.body.classList.contains('no-motion');}
      applyPrefs();
      t.setAttribute('aria-checked',on);var i=t.querySelector('i');if(i)i.classList.toggle('on',!!on);
    }
  });
  $('mxmoCustom').onclick=function(){closeSheet();try{W.openTab({kind:'myuser'});}catch(x){}setTimeout(function(){var c=$('muDock');if(c&&c.scrollIntoView)c.scrollIntoView({behavior:'smooth',block:'start'});},450);};
  $('mxmoSet').onclick=function(){closeSheet();var s=$('tbSettings');if(s)s.click();};
  $('mxmoQ').oninput=function(){var q=(this.value||'').toLowerCase().trim();sh.querySelectorAll('.mxmo-tile').forEach(function(b){b.hidden=!!q&&b.textContent.toLowerCase().indexOf(q)<0;});};
  sh.addEventListener('keydown',function(e){
    if(e.key==='Escape'){e.preventDefault();closeSheet();return;}
    if(e.key==='Tab'){var f=sh.querySelectorAll('button:not([hidden]),input');if(!f.length)return;var a=f[0],z=f[f.length-1];
      if(e.shiftKey&&D.activeElement===a){e.preventDefault();z.focus();}else if(!e.shiftKey&&D.activeElement===z){e.preventDefault();a.focus();}}
  });
  setTimeout(function(){var f=sh.querySelector('.mxmo-tile.on')||sh.querySelector('.mxmo-tile');try{f.focus();}catch(x){}},30);
}
function closeSheet(quiet){
  var sh=$('mxMore');if(!sh)return;sh.parentNode.removeChild(sh);D.body.classList.remove('mx-more-open');
  if(!quiet&&lastFocus&&lastFocus.focus)try{lastFocus.focus();}catch(e){}
}

/* ---------- translations for the 1.0.8 strings (course, arcade, menu) ---------- */
(function(){
  var X={
   ar:{'More':'المزيد','Menu':'القائمة','Main menu':'القائمة الرئيسية','Close menu':'إغلاق القائمة','Find a section…':'ابحث عن قسم…','Easier to use':'أسهل في الاستخدام','Text size':'حجم النص','High contrast':'تباين عالٍ','Bigger buttons':'أزرار أكبر','Always show the menu':'إظهار القائمة دائمًا','Customise the menu':'تخصيص القائمة','All settings':'كل الإعدادات','Video course':'دروس الفيديو','Practice':'تدريب','Continue':'تابع','lessons done':'دروس مكتملة','Grades':'الصفوف','Quick quiz':'اختبار سريع','Correct! 🎉':'صحيح! 🎉','Not quite — the answer is':'ليس تمامًا — الإجابة هي','Watch again':'شاهد مجددًا','Next lesson':'الدرس التالي','Done':'تم','Quiz':'اختبار','correct':'صحيحة','Matix Arcade':'صالة ألعاب ماتيكس','Difficulty':'المستوى','Best':'الأفضل','Play again':'العب مجددًا','Back to games':'العودة للألعاب','Game over':'انتهت اللعبة','New best score!':'رقم قياسي جديد!','Exit game':'الخروج من اللعبة','Paused':'متوقف مؤقتًا','College & beyond':'الجامعة وما بعدها','College':'الجامعة','Grade 1':'الصف 1','Grade 2':'الصف 2','Grade 3':'الصف 3','Grade 4':'الصف 4','Grade 5':'الصف 5','Grade 6':'الصف 6','Grade 7':'الصف 7','Grade 8':'الصف 8','Grade 9':'الصف 9','Grade 10':'الصف 10','Grade 11':'الصف 11','Grade 12':'الصف 12'},
   fr:{'More':'Plus','Menu':'Menu','Main menu':'Menu principal','Close menu':'Fermer le menu','Find a section…':'Trouver une section…','Easier to use':'Plus facile à utiliser','Text size':'Taille du texte','High contrast':'Contraste élevé','Bigger buttons':'Boutons plus grands','Always show the menu':'Toujours afficher le menu','Customise the menu':'Personnaliser le menu','All settings':'Tous les réglages','Video course':'Cours vidéo','Practice':'Entraînement','Continue':'Continuer','lessons done':'leçons terminées','Grades':'Niveaux','Quick quiz':'Quiz rapide','Correct! 🎉':'Correct ! 🎉','Not quite — the answer is':'Pas tout à fait — la réponse est','Watch again':'Revoir','Next lesson':'Leçon suivante','Done':'Terminé','Quiz':'Quiz','correct':'justes','Matix Arcade':'Arcade Matix','Difficulty':'Difficulté','Best':'Record','Play again':'Rejouer','Back to games':'Retour aux jeux','Game over':'Partie terminée','New best score!':'Nouveau record !','Exit game':'Quitter le jeu','Paused':'En pause','College & beyond':'Université et plus','College':'Université','Grade 1':'1re année','Grade 2':'2e année','Grade 3':'3e année','Grade 4':'4e année','Grade 5':'5e année','Grade 6':'6e année','Grade 7':'7e année','Grade 8':'8e année','Grade 9':'9e année','Grade 10':'10e année','Grade 11':'11e année','Grade 12':'12e année'},
   es:{'More':'Más','Menu':'Menú','Main menu':'Menú principal','Close menu':'Cerrar menú','Find a section…':'Buscar una sección…','Easier to use':'Más fácil de usar','Text size':'Tamaño del texto','High contrast':'Alto contraste','Bigger buttons':'Botones más grandes','Always show the menu':'Mostrar siempre el menú','Customise the menu':'Personalizar el menú','All settings':'Todos los ajustes','Video course':'Curso en vídeo','Practice':'Práctica','Continue':'Continuar','lessons done':'lecciones hechas','Grades':'Cursos','Quick quiz':'Quiz rápido','Correct! 🎉':'¡Correcto! 🎉','Not quite — the answer is':'Casi — la respuesta es','Watch again':'Ver otra vez','Next lesson':'Siguiente lección','Done':'Hecho','Quiz':'Quiz','correct':'correctas','Matix Arcade':'Arcade Matix','Difficulty':'Dificultad','Best':'Récord','Play again':'Jugar otra vez','Back to games':'Volver a los juegos','Game over':'Fin del juego','New best score!':'¡Nuevo récord!','Exit game':'Salir del juego','Paused':'En pausa','College & beyond':'Universidad y más','College':'Universidad','Grade 1':'1.º grado','Grade 2':'2.º grado','Grade 3':'3.º grado','Grade 4':'4.º grado','Grade 5':'5.º grado','Grade 6':'6.º grado','Grade 7':'7.º grado','Grade 8':'8.º grado','Grade 9':'9.º grado','Grade 10':'10.º grado','Grade 11':'11.º grado','Grade 12':'12.º grado'}
  };
  try{if(typeof W.I18N!=='object')return;Object.keys(X).forEach(function(l){var d=W.I18N[l]||(W.I18N[l]={});Object.keys(X[l]).forEach(function(k){if(d[k]===undefined)d[k]=X[l][k];});});}catch(e){}
})();

/* ---------- hook in ---------- */
function boot(){
  applyPrefs();
  var b=W.mxDockBuild;
  if(typeof b==='function'&&!b.__mxmenu){W.mxDockBuild=function(){var r=b.apply(this,arguments);decorate();return r;};W.mxDockBuild.__mxmenu=1;}
  var s=W.lxDockSync;
  if(typeof s==='function'&&!s.__mxmenu){W.lxDockSync=function(){var r=s.apply(this,arguments);sync();return r;};W.lxDockSync.__mxmenu=1;}
  decorate();
  W.mxMenu={open:openSheet,close:closeSheet,setAlways:setAlways,always:always};
}
if(D.readyState==='loading')D.addEventListener('DOMContentLoaded',boot);else boot();
})();
