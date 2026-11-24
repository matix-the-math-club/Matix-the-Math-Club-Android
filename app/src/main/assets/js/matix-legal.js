/* =====================================================================
   Matix privacy links (1.1.1)
   Google Play requires a privacy policy link inside the app. It is shown:
   - on the sign-in / create-account screen
   - in Settings → About
   - in My User (with how to delete your account)
   - in the More menu
   Also: deleted-account backups are erased after 30 days, as the privacy
   policy says (runs when the owner opens the app).
   ===================================================================== */
(function(){
'use strict';
var D=document,W=window;
var URL_PRIVACY='https://matixthemathclub.com/privacy/';
var DELETE_MAIL='mailto:ghadimatix@gmail.com?subject=Delete%20my%20Matix%20account';
function $(id){return D.getElementById(id);}
function T(s){try{return typeof W.tr==='function'?W.tr(s):s;}catch(e){return s;}}
function link(txt){return '<a href="'+URL_PRIVACY+'" target="_blank" rel="noopener">'+T(txt)+'</a>';}

function gate(){
  var card=D.querySelector('#gateAuth .gate-card');
  if(!card||card.querySelector('.mx-legal'))return;
  var p=D.createElement('p');p.className='mx-legal';
  p.innerHTML=T('By signing in or creating an account you agree to our')+' '+link('Privacy Policy')+'.';
  var sw=$('gSwap');card.insertBefore(p,sw?sw.nextSibling:null);
}
function settings(){
  var i=$('tbInfo');if(!i||i.querySelector('.mx-legal-a'))return;
  var a=D.createElement('div');a.className='mx-legal-a';a.innerHTML=link('Privacy Policy');i.appendChild(a);
}
function myUser(){
  var wrap=D.querySelector('.mxa-me');if(!wrap||$('muPrivacy'))return;
  var s=D.createElement('section');s.className='card mxa-sec';s.id='muPrivacy';s.style.marginTop='16px';
  s.innerHTML='<h3 class="mxa-h">🔏 '+T('Privacy & your data')+'</h3>'
    +'<p class="mxa-note">'+T('Read how Matix uses your data, or ask us to delete your account and everything linked to it.')+'</p>'
    +'<div class="mxa-btns"><a class="btn" href="'+URL_PRIVACY+'" target="_blank" rel="noopener">'+T('Privacy Policy')+'</a>'
    +'<a class="btn ghost" href="'+DELETE_MAIL+'">'+T('Delete my account')+'</a></div>';
  wrap.appendChild(s);
}
function more(){
  var p=D.querySelector('#mxMore .mxmo-panel');if(!p||p.querySelector('.mx-legal-m'))return;
  var d=D.createElement('p');d.className='mx-legal-m';d.innerHTML=link('Privacy Policy');p.appendChild(d);
}
function all(){try{gate();settings();myUser();more();}catch(e){}}

/* erase deleted-account backups older than 30 days (owner only) */
var purged=false;
function purge(){
  if(purged||!W.ME)return;
  try{if(!(typeof W.isOwner==='function'&&W.isOwner()))return;}catch(e){return;}
  purged=true;
  if(typeof W.fbGet!=='function')return;
  W.fbGet('/deletion_backups').then(function(o){
    var cut=Date.now()-30*86400000;
    Object.keys(o||{}).forEach(function(u){var b=o[u];if(b&&b.at&&b.at<cut){W.fbPut('/deletion_backups/'+encodeURIComponent(u),null);}});
  });
}

var css=D.createElement('style');
css.textContent='.mx-legal{font-size:.78rem;color:var(--muted,#6b6b76);text-align:center;margin:10px 0 0;line-height:1.4}'
 +'.mx-legal a,.mx-legal-a a,.mx-legal-m a{color:#8b5cf6;font-weight:700}'
 +'.mx-legal-a{margin-top:6px;font-size:.85rem}.mx-legal-m{text-align:center;margin:14px 0 0;font-size:.9rem}';
D.head.appendChild(css);

function boot(){
  all();
  var tm=null;
  try{new MutationObserver(function(){if(tm)return;tm=setTimeout(function(){tm=null;all();purge();},150);}).observe(D.body,{childList:true,subtree:true});}catch(e){}
  setTimeout(purge,4000);
}
if(D.readyState==='loading')D.addEventListener('DOMContentLoaded',boot);else boot();
})();
