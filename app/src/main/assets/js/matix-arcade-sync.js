/* =====================================================================
   Matix Arcade sync (1.1.1)
   The arcade (games, class games, smart-board exercises) lives on the
   website: https://www.matixthemathclub.com/assets/arcade.js
   The app ships a copy (tools/sync-arcade.sh copies it at build time).
   On start, the app reads /arcade/version.json from the website. If the
   website has a newer arcade, the app loads it from there, so games added
   on the website show up in the app without a Play update. Offline, or if
   anything fails, the built-in copy keeps working.
   Best scores sync through the club database inside the arcade itself.
   ===================================================================== */
(function(){
'use strict';
var D=document,W=window,SITE='https://www.matixthemathclub.com';
function local(){return (W.mxArcade&&W.mxArcade.version)||0;}
function refresh(){
  var c=D.getElementById('content');
  if(c&&c.querySelector('.mxr-arcade')&&!D.getElementById('mxgOv')&&typeof W.renderGames==='function'){try{W.renderGames(c);}catch(e){}}
}
function load(v){
  if(!v||!(+v.version>local()))return;
  var l=D.createElement('link');l.rel='stylesheet';l.href=SITE+(v.css||'/assets/arcade.css')+'?v='+v.version;D.head.appendChild(l);
  var s=D.createElement('script');s.src=SITE+(v.js||'/assets/arcade.js')+'?v='+v.version;s.async=true;
  s.onload=function(){refresh();};
  s.onerror=function(){if(l.parentNode)l.parentNode.removeChild(l);};
  D.body.appendChild(s);
}
function check(){
  if(typeof fetch!=='function')return;
  var ctl=typeof AbortController==='function'?new AbortController():null;
  var to=setTimeout(function(){if(ctl)ctl.abort();},6000);
  fetch(SITE+'/arcade/version.json',{cache:'no-store',signal:ctl?ctl.signal:undefined})
    .then(function(r){return r.ok?r.json():null;})
    .then(function(v){clearTimeout(to);load(v);})
    ['catch'](function(){clearTimeout(to);});
}
if(D.readyState==='complete')setTimeout(check,1500);else W.addEventListener('load',function(){setTimeout(check,1500);});
})();
