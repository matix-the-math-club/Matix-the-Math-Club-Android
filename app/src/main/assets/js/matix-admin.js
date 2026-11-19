/* =====================================================================
   Matix owner tools (1.0.9) — Promote and Delete on the Users page.
   Only the owner sees them. Every user card and profile gets:
     ⬆ Promote: user → member → admin → manager (and ⬇ to step back)
     🗑 Delete: backs everything up to /deletion_backups/<user> first,
       then removes the account. The owner can undo it from Messages
       ("↩ Revert deletion"), exactly like the existing restore flow.
   Owners can never be promoted, demoted or deleted.
   ===================================================================== */
(function(){
'use strict';
var D=document,W=window;
var LADDER=['user','member','admin','manager'];
var ICON={user:'🙂',member:'🎓',admin:'⭐',manager:'🛡️'};
function $(id){return D.getElementById(id);}
function esc(s){return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
function owner(){try{return typeof W.isOwner==='function'&&W.isOwner();}catch(e){return false;}}
function me(){return W.ME||null;}
function isOwnerUser(u){return (W.OWNERS||['ghadi']).indexOf(u)>-1||ROLES[u]==='owner';}
function toast(m,k){try{W.toast(m,k);}catch(e){}}
function ask(msg,ok,opts){if(typeof W.customConfirm==='function')W.customConfirm(msg,ok,opts||{});else if(confirm(msg))ok();}
var enc=encodeURIComponent;
var ROLES={},rolesAt=0;
function loadRoles(force){
  if(!force&&Date.now()-rolesAt<15000)return Promise.resolve(ROLES);
  return W.fbGet('/roles').then(function(r){ROLES=r||{};rolesAt=Date.now();return ROLES;});
}
function roleOf(u){var r=String(ROLES[u]||'').toLowerCase();if(LADDER.indexOf(r)>-1)return r;return (W.MC_CORE||[]).indexOf(u)>-1?'member':'user';}

function setRole(u,role){
  return W.fbPut('/roles/'+enc(u),role).then(function(){
    ROLES[u]=role;
    W.fbPost('/notifications/'+enc(u),{type:'role',title:ICON[role]+' You are now '+(role==='admin'?'an':'a')+' '+role,body:'@'+me()+' changed your role in the club.',at:Date.now(),from:me()});
    toast(ICON[role]+' @'+u+' is now '+role,'ok');
  });
}
function deleteUser(u){
  var p=['/members/','/profiles/','/roles/','/profile_comments/','/follows/'].map(function(x){return W.fbGet(x+enc(u));});
  return Promise.all(p).then(function(r){
    var backup={member:r[0]||null,profile:r[1]||null,role:r[2]||null,comments:r[3]||null,follows:r[4]||null,at:Date.now(),by:me()};
    return W.fbPut('/deletion_backups/'+enc(u),backup).then(function(saved){
      if(saved==null)throw new Error('backup failed');
      return Promise.all([W.fbPut('/deletions/'+enc(u),{at:Date.now(),by:me()}),
        W.fbPut('/members/'+enc(u),null),W.fbPut('/profiles/'+enc(u),null),W.fbPut('/roles/'+enc(u),null),
        W.fbPut('/profile_comments/'+enc(u),null),W.fbPut('/follows/'+enc(u),null),W.fbPut('/friends/'+enc(u),null)]);
    });
  }).then(function(){delete ROLES[u];toast('🗑 @'+u+' deleted. You can undo it from Messages.','ok');});
}

function controls(u,big){
  var r=roleOf(u),i=LADDER.indexOf(r);
  return '<div class="mxadm'+(big?' big':'')+'" data-adm="'+esc(u)+'" role="group" aria-label="Owner tools for @'+esc(u)+'">'
    +'<span class="mxadm-role">'+ICON[r]+' '+esc(r)+'</span>'
    +(i<LADDER.length-1?'<button type="button" data-act="up" aria-label="Promote @'+esc(u)+' to '+LADDER[i+1]+'">⬆ Promote</button>':'')
    +(i>0?'<button type="button" data-act="down" aria-label="Demote @'+esc(u)+' to '+LADDER[i-1]+'">⬇</button>':'')
    +'<button type="button" data-act="del" class="del" aria-label="Delete @'+esc(u)+'">🗑 Delete</button></div>';
}
function wire(box){
  box.addEventListener('click',function(e){
    var b=e.target.closest('button[data-act]');if(!b)return;
    e.stopPropagation();e.preventDefault();
    var u=box.dataset.adm,act=b.dataset.act,i=LADDER.indexOf(roleOf(u));
    if(act==='up'||act==='down'){
      var nr=LADDER[i+(act==='up'?1:-1)];if(!nr)return;
      ask((act==='up'?'Promote':'Demote')+' @'+u+' to '+nr+'?',function(){b.disabled=true;setRole(u,nr).then(refresh);},{icon:ICON[nr],okText:act==='up'?'Promote':'Demote'});
    }else if(act==='del'){
      ask('Delete @'+u+'? Their account, profile, role, comments and follows are removed. A backup is kept, so you can undo it from Messages.',function(){
        b.disabled=true;b.textContent='Deleting…';
        deleteUser(u).then(function(){try{W.USERS_CACHE=null;}catch(x){}if(W.usersView)W.usersView={name:'list'};try{W.renderContent();}catch(x){}})
          ['catch'](function(){toast('Could not delete — check your connection','err');b.disabled=false;b.textContent='🗑 Delete';});
      },{icon:'🗑️',okText:'Delete'});
    }
  });
}
function refresh(){D.querySelectorAll('.mxadm').forEach(function(n){n.remove();});decorate();}
function decorate(){
  if(!owner())return;
  var t=null;try{t=W.activeTab();}catch(e){}
  if(!t||t.kind!=='users')return;
  loadRoles().then(function(){
    D.querySelectorAll('.u-card[data-u]').forEach(function(card){
      var u=card.dataset.u;if(!u||card.querySelector('.mxadm')||u===me()||isOwnerUser(u))return;
      var d=D.createElement('div');d.innerHTML=controls(u,false);var box=d.firstChild;
      var view=card.querySelector('.u-view');card.insertBefore(box,view||null);wire(box);
    });
    var hero=D.querySelector('.u-hero');
    if(hero&&!hero.querySelector('.mxadm')){
      var nm=hero.querySelector('.u-hero-name'),u=nm?nm.textContent.replace(/^@/,'').trim():'';
      if(u&&u!==me()&&!isOwnerUser(u)){var d2=D.createElement('div');d2.innerHTML=controls(u,true);var bx=d2.firstChild;hero.appendChild(bx);wire(bx);}
    }
  });
}

var css=D.createElement('style');
css.textContent='.mxadm{display:flex;flex-wrap:wrap;align-items:center;gap:6px;margin:8px 0 2px}'
 +'.mxadm.big{margin-top:12px}'
 +'.mxadm-role{font-size:.72rem;font-weight:800;padding:4px 9px;border-radius:99px;background:rgba(139,92,246,.14);color:#6d28d9;text-transform:capitalize}'
 +'body.dark .mxadm-role{color:#c4b5fd}'
 +'.mxadm button{min-height:36px;padding:6px 11px;border-radius:10px;border:1px solid var(--border,rgba(0,0,0,.15));background:var(--card,#fff);color:inherit;font:700 .78rem/1 inherit;font-family:inherit;cursor:pointer}'
 +'.mxadm button:hover{border-color:#8b5cf6}'
 +'.mxadm button.del{color:#dc2626;border-color:rgba(220,38,38,.35)}'
 +'.mxadm button:focus-visible{outline:3px solid #fbbf24;outline-offset:2px}'
 +'.mxadm button:disabled{opacity:.5}';
D.head.appendChild(css);

function boot(){
  var c=$('content');if(!c)return;
  var tm=null;
  try{new MutationObserver(function(){if(tm)return;tm=setTimeout(function(){tm=null;decorate();},120);}).observe(c,{childList:true,subtree:true});}catch(e){}
}
if(D.readyState==='loading')D.addEventListener('DOMContentLoaded',boot);else boot();
W.mxAdmin={setRole:setRole,deleteUser:deleteUser};
})();
