/* =====================================================================
   MATIX SOCIAL (v1.0.7)
   1. Math Chat end-to-end encryption
      - every user gets an ECDH P-256 key pair made on their own device
        (WebCrypto). The PRIVATE key never leaves the device unless the user
        makes a passphrase-locked backup in My User.
      - the public key is published at /e2eKeys/<user>/pub
      - each message is sealed with AES-256-GCM using a key from
        ECDH(my private, their public) -> HKDF-SHA-256. Firebase only ever
        stores {iv, ct} + the two public keys used, never the text.
      - if the other person has not opened 1.0.7 yet (no public key) the
        message falls back to the old scrambler and the chat says so.
   2. Friends: requests, accept / decline, remove, friends list, "Friends"
      tab, and an add-friend button in Math Chat.
      /friends/<u>/<v> = {at}   (both sides, once accepted)
      /friendReq/<to>/<from> = {at}
   3. Bottom menu: "My User" is in it, and My User → Bottom menu lets you
      pick and order what the menu shows (saved on this device).
   ===================================================================== */
(function(){
'use strict';
var W=window,D=document;
function $(id){return D.getElementById(id);}
function T(s){try{return typeof W.tr==='function'?W.tr(s):s;}catch(e){return s;}}
function esc(s){return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
function me(){return W.ME||null;}
function toast(m,k){try{W.toast(m,k);}catch(e){}}
function lsG(k){try{return localStorage.getItem(k);}catch(e){return null;}}
function lsS(k,v){try{if(v==null)localStorage.removeItem(k);else localStorage.setItem(k,v);}catch(e){}}
var SUB=(W.crypto&&W.crypto.subtle)||null;
var TE=new TextEncoder(),TD=new TextDecoder();

/* ---------- base64url ---------- */
function b64(buf){var b=new Uint8Array(buf),s='';for(var i=0;i<b.length;i++)s+=String.fromCharCode(b[i]);return btoa(s).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');}
function unb64(s){s=String(s).replace(/-/g,'+').replace(/_/g,'/');while(s.length%4)s+='=';var r=atob(s),b=new Uint8Array(r.length);for(var i=0;i<r.length;i++)b[i]=r.charCodeAt(i);return b;}

/* =====================================================================
   1. END-TO-END ENCRYPTION
   ===================================================================== */
var EC={name:'ECDH',namedCurve:'P-256'};
var E2E={user:null,priv:null,pubC:null,ready:null,pubs:{},shared:{},plain:{}};
function compact(jwk){return jwk.x+'.'+jwk.y;}
function pubJwk(c){var p=String(c).split('.');return {kty:'EC',crv:'P-256',x:p[0],y:p[1],ext:true};}
function keyName(u){return 'mx_e2e_'+u;}

function loadKeys(u){
  if(!SUB||!u)return Promise.resolve(false);
  if(E2E.user===u&&E2E.ready)return E2E.ready;
  E2E.user=u;E2E.priv=null;E2E.pubC=null;E2E.shared={};
  E2E.ready=(async function(){
    var saved=null;try{saved=JSON.parse(lsG(keyName(u))||'null');}catch(e){}
    if(!saved||!saved.priv){
      var kp=await SUB.generateKey(EC,true,['deriveBits']);
      saved={priv:await SUB.exportKey('jwk',kp.privateKey),at:Date.now()};
      lsS(keyName(u),JSON.stringify(saved));
    }
    await useJwk(saved.priv);
    await publish(u);
    return true;
  })()['catch'](function(e){console.warn('e2e keys',e);E2E.ready=null;return false;});
  return E2E.ready;
}
async function useJwk(jwk){
  E2E.priv=await SUB.importKey('jwk',jwk,EC,true,['deriveBits']);
  E2E.pubC=compact(jwk);E2E.shared={};
}
function publish(u){
  return W.fbGet('/e2eKeys/'+u+'/pub').then(function(cur){
    if(cur!==E2E.pubC)return W.fbPut('/e2eKeys/'+u+'/pub',E2E.pubC).then(function(){W.fbPut('/e2eKeys/'+u+'/at',Date.now());});
  });
}
function theirPub(u){
  var c=E2E.pubs[u];
  if(c&&Date.now()-c.t<60000)return Promise.resolve(c.v);
  return W.fbGet('/e2eKeys/'+u+'/pub').then(function(v){v=(typeof v==='string'&&v.indexOf('.')>0)?v:null;E2E.pubs[u]={v:v,t:Date.now()};return v;});
}
async function aesFor(otherC){
  if(E2E.shared[otherC])return E2E.shared[otherC];
  var other=await SUB.importKey('jwk',pubJwk(otherC),EC,false,[]);
  var bits=await SUB.deriveBits({name:'ECDH',public:other},E2E.priv,256);
  var hk=await SUB.importKey('raw',bits,'HKDF',false,['deriveKey']);
  var info=TE.encode('matix-chat-v1|'+[E2E.pubC,otherC].sort().join('|'));
  var k=await SUB.deriveKey({name:'HKDF',hash:'SHA-256',salt:TE.encode('matix-e2e'),info:info},hk,{name:'AES-GCM',length:256},false,['encrypt','decrypt']);
  E2E.shared[otherC]=k;return k;
}
function aad(m){return TE.encode([m.from,m.to,m.type,m.at].join('|'));}
async function seal(msg,otherC){
  var k=await aesFor(otherC),iv=crypto.getRandomValues(new Uint8Array(12));
  var ct=await SUB.encrypt({name:'AES-GCM',iv:iv,additionalData:aad(msg)},k,TE.encode(msg.text));
  return {v:1,iv:b64(iv),ct:b64(ct),s:E2E.pubC,r:otherC};
}
async function open(m){
  var e=m.e2e;if(!e||!e.ct)return null;
  if(E2E.plain[e.ct]!==undefined)return E2E.plain[e.ct];
  if(!E2E.priv)return null;
  var other=e.s===E2E.pubC?e.r:(e.r===E2E.pubC?e.s:null);
  if(!other)return null;
  try{
    var k=await aesFor(other);
    var pt=await SUB.decrypt({name:'AES-GCM',iv:unb64(e.iv),additionalData:aad(m)},k,unb64(e.ct));
    return (E2E.plain[e.ct]=TD.decode(pt));
  }catch(x){return null;}
}
/* decrypt a list of messages in place: m.text = plain text, m._lock = true when it cannot be opened here */
async function openAll(arr){
  if(E2E.user!==me())await loadKeys(me());else if(E2E.ready)await E2E.ready;
  for(var i=0;i<arr.length;i++){
    var m=arr[i];if(!m||!m.e2e||m.deleted)continue;
    var p=await open(m);
    if(p==null){m._lock=true;m.text='';}else{m.text=p;m._lock=false;}
    m._sealed=true;
  }
  return arr;
}
async function safetyCode(a,b){
  var h=await SUB.digest('SHA-256',TE.encode([a,b].filter(Boolean).sort().join('|')));
  var x=Array.prototype.map.call(new Uint8Array(h).slice(0,15),function(n){return ('00'+n).slice(-3);}).join('');
  return x.match(/.{5}/g).join(' ');
}

/* ---- passphrase backup so you can move your chat key to a new phone ---- */
async function passKey(pass,salt){
  var base=await SUB.importKey('raw',TE.encode(pass),'PBKDF2',false,['deriveKey']);
  return SUB.deriveKey({name:'PBKDF2',salt:salt,iterations:310000,hash:'SHA-256'},base,{name:'AES-GCM',length:256},false,['encrypt','decrypt']);
}
async function backupKey(u,pass){
  var saved=JSON.parse(lsG(keyName(u))||'null');if(!saved)throw new Error('nokey');
  var salt=crypto.getRandomValues(new Uint8Array(16)),iv=crypto.getRandomValues(new Uint8Array(12));
  var k=await passKey(pass,salt);
  var ct=await SUB.encrypt({name:'AES-GCM',iv:iv},k,TE.encode(JSON.stringify(saved.priv)));
  await W.fbPut('/e2eKeys/'+u+'/backup',{salt:b64(salt),iv:b64(iv),ct:b64(ct),at:Date.now()});
}
async function restoreKey(u,pass){
  var bk=await W.fbGet('/e2eKeys/'+u+'/backup');if(!bk||!bk.ct)throw new Error('nobackup');
  var k=await passKey(pass,unb64(bk.salt)),pt;
  try{pt=await SUB.decrypt({name:'AES-GCM',iv:unb64(bk.iv)},k,unb64(bk.ct));}catch(e){throw new Error('badpass');}
  var jwk=JSON.parse(TD.decode(pt));
  lsS(keyName(u),JSON.stringify({priv:jwk,at:Date.now(),restored:true}));
  E2E.plain={};await useJwk(jwk);await publish(u);
}
W.MXE2E={load:loadKeys,openAll:openAll,backup:backupKey,restore:restoreKey,state:E2E};

/* ---- plug into Math Chat ---- */
function hookChat(){
  if(typeof W.mcSend!=='function'||W.mcSend.__e2e)return;
  var oldPreview=W.mcPreview;
  W.mcPreview=function(m){
    if(m&&m._lock)return '🔒 '+T('Encrypted message');
    return oldPreview(m);
  };
  W.mcSend=function(type,text){
    var MC=W.MC;if(!MC.who)return;
    var u=MC.who,cid=W.mcId(me(),u),at=Date.now();
    var msg={from:me(),to:u,type:type,text:text,at:at};
    var local={from:me(),to:u,type:type,text:text,at:at,_sealed:true};
    MC.msgs.push(local);W.mcPaint();
    Promise.all([loadKeys(me()),theirPub(u)]).then(async function(r){
      var body=msg,sealed=false;
      if(r[0]&&r[1]&&SUB){
        body={from:msg.from,to:msg.to,type:type,text:'',at:at,e2e:await seal(msg,r[1])};
        E2E.plain[body.e2e.ct]=text;sealed=true;
      }else local._sealed=false;
      await W.fbPost('/chats/'+cid+'/messages',body);
      W.fbPut('/chats/'+cid+'/typing/'+me(),0);
      W.mcRead();
      W.fbPost('/notifications/'+u,{type:'chat',title:'💬 New message from '+W.mcName(me()),
        body:sealed?('🔒 '+T('Encrypted message')):(type==='text'?(text.length>90?text.slice(0,90)+'…':text):(type==='sticker'?(text+'  (sticker)'):'📐 sent an equation')),
        at:at,from:me(),url:'/chat'});
      MC.last[u]=local;W.mcDrawList();
    })['catch'](function(){toast('Message did not send — check your internet','err');});
  };
  W.mcSend.__e2e=1;
  W.mcPull=function(first){
    var MC=W.MC,u=MC.who;if(!u)return;
    var cid=W.mcId(me(),u);
    W.fbGet('/chats/'+cid+'/messages').then(function(o){return openAll(W.mcArr(o));}).then(function(arr){
      if(MC.who!==u)return;
      var grew=arr.length!==MC.msgs.length;
      MC.msgs=arr;
      if(grew||first)W.mcPaint(first);
      if(arr.length){var last=arr[arr.length-1];MC.last[u]=last;if(last.from!==me()&&grew&&!first)W.mcRead();}
    });
    W.fbGet('/chatMeta/'+cid+'/read/'+u).then(function(r){MC.otherRead=r||0;});
    W.fbGet('/chats/'+cid+'/typing/'+u).then(function(t){
      MC.otherTyping=(typeof t==='number'&&t>1000000)?t:(t?Date.now():0);
      W.mcStatus();
    });
  };
  W.mcRefreshPreviews=function(){
    var MC=W.MC;
    W.mcPeople().slice(0,12).forEach(function(u){
      var cid=W.mcId(me(),u);
      W.fbGet('/chats/'+cid+'/messages').then(function(o){
        var arr=W.mcArr(o),last=arr.length?arr[arr.length-1]:null;
        return openAll(last?[last]:[]).then(function(){return arr;});
      }).then(function(arr){
        MC.last[u]=arr.length?arr[arr.length-1]:null;
        W.fbGet('/chatMeta/'+cid+'/read/'+me()).then(function(rd){
          rd=rd||0;var n=0;arr.forEach(function(m){if(m.from!==me()&&(m.at||0)>rd)n++;});
          MC.unread[u]=n;
          if(W.mcAlive()&&MC.who!==u)W.mcDrawList();
        });
      });
    });
  };
  W.mcDelete=function(k){
    var MC=W.MC;if(!MC.who||!k)return;
    W.fbPatch('/chats/'+W.mcId(me(),MC.who)+'/messages/'+k,{deleted:true,text:'',e2e:null}).then(function(){
      MC.msgs=MC.msgs.filter(function(x){return x._k!==k;});
      W.mcPaint(true);W.mcRefreshPreviews();
      toast('🗑️ '+T('Message deleted'));
    })['catch'](function(){toast(T('Could not delete — check your internet'),'err');});
  };
  var oldPaint=W.mcPaint;
  W.mcPaint=function(jump){
    var MC=W.MC;
    MC.msgs.forEach(function(m){if(m._lock)m.text='🔒 '+T("Can't open this message on this device — restore your chat key in My User");});
    var r=oldPaint.apply(this,arguments);
    var box=$('mcMsgs');if(!box||!MC.who)return r;
    var b=box.querySelector('.mc-enc'),who=MC.who;
    if(b&&E2E.pubs[who]){
      b.style.cursor='pointer';
      var pub=E2E.pubs[who]&&E2E.pubs[who].v;
      b.textContent=pub&&E2E.pubC?('🔒 '+T('End-to-end encrypted. Only you and')+' '+W.mcName(who)+' '+T('can read these messages. Tap to verify.'))
        :('🔓 '+W.mcName(who)+' '+T('has not updated Matix yet, so messages to them are only scrambled, not end-to-end encrypted.'));
      b.onclick=function(){
        safetyCode(E2E.pubC,pub).then(function(code){
          W.customConfirm?W.customConfirm(T('Safety code for this chat')+':\n\n'+code+'\n\n'+T('Compare it with')+' '+W.mcName(who)+T("'s phone. If it matches, nobody is in the middle."),function(){},{icon:'🔒',okText:'OK'}):alert(code);
        });
      };
    }
    MC.msgs.forEach(function(m){if(m._lock)m.text='';});
    return r;
  };
  /* add-friend button in the chat header */
  var oldOpen=W.mcOpen;
  W.mcOpen=function(u){
    var r=oldOpen.apply(this,arguments);
    theirPub(u).then(function(){if(W.MC.who===u)W.mcPaint();});
    var top=D.querySelector('#mcMain .mc-top');
    if(top&&!$('mcFriendBtn')){
      var b=D.createElement('button');b.className='mc-callbtn';b.id='mcFriendBtn';b.title=T('Friends');
      top.insertBefore(b,$('mcCallBtn'));
      friendBtn(b,u);
    }
    return r;
  };
}

/* sign in / join: make (or load) this device's chat key straight away */
function hookAuth(){
  ['doSignIn','doJoin'].forEach(function(n){
    var f=W[n];if(typeof f!=='function'||f.__e2e)return;
    W[n]=function(){return f.apply(this,arguments).then(function(r){if(r===true||r==='ok')setTimeout(function(){loadKeys(me());},300);return r;});};
    W[n].__e2e=1;
  });
}

/* =====================================================================
   2. FRIENDS
   ===================================================================== */
var FR={mine:{},inbox:{},sent:{},t:0};
function frLoad(force){
  var u=me();if(!u)return Promise.resolve(FR);
  if(!force&&Date.now()-FR.t<8000)return Promise.resolve(FR);
  return Promise.all([W.fbGet('/friends/'+u),W.fbGet('/friendReq/'+u),W.fbGet('/friendSent/'+u)]).then(function(r){
    FR.mine=r[0]||{};FR.inbox=r[1]||{};FR.sent=r[2]||{};FR.t=Date.now();return FR;
  });
}
function frState(v){return FR.mine[v]?'friend':(FR.inbox[v]?'incoming':(FR.sent[v]?'sent':'none'));}
function frAdd(v){
  var u=me(),at=Date.now();if(!u||!v||u===v)return Promise.resolve();
  if(FR.inbox[v])return frAccept(v);
  FR.sent[v]={at:at};
  return Promise.all([W.fbPut('/friendReq/'+v+'/'+u,{at:at}),W.fbPut('/friendSent/'+u+'/'+v,{at:at}),
    W.fbPost('/notifications/'+v,{type:'friend',title:'🤝 '+name(u)+' '+T('wants to be your friend'),body:T('Open Friends to accept.'),at:at,from:u})])
    .then(function(){toast('🤝 '+T('Friend request sent to')+' '+name(v),'ok');});
}
function frAccept(v){
  var u=me(),at=Date.now();
  FR.mine[v]={at:at};delete FR.inbox[v];
  return Promise.all([W.fbPut('/friends/'+u+'/'+v,{at:at}),W.fbPut('/friends/'+v+'/'+u,{at:at}),
    W.fbPut('/friendReq/'+u+'/'+v,null),W.fbPut('/friendSent/'+v+'/'+u,null),
    W.fbPut('/learn/friends/'+u+'/'+v,true),W.fbPut('/learn/friends/'+v+'/'+u,true),
    W.fbPost('/notifications/'+v,{type:'friend',title:'🎉 '+name(u)+' '+T('accepted your friend request'),body:'',at:at,from:u})])
    .then(function(){toast('🎉 '+T('You and')+' '+name(v)+' '+T('are now friends'),'ok');});
}
function frDecline(v){
  var u=me();delete FR.inbox[v];
  return Promise.all([W.fbPut('/friendReq/'+u+'/'+v,null),W.fbPut('/friendSent/'+v+'/'+u,null)]);
}
function frCancel(v){
  var u=me();delete FR.sent[v];
  return Promise.all([W.fbPut('/friendReq/'+v+'/'+u,null),W.fbPut('/friendSent/'+u+'/'+v,null)]);
}
function frRemove(v){
  var u=me();delete FR.mine[v];
  return Promise.all([W.fbPut('/friends/'+u+'/'+v,null),W.fbPut('/friends/'+v+'/'+u,null),
    W.fbPut('/learn/friends/'+u+'/'+v,null),W.fbPut('/learn/friends/'+v+'/'+u,null)]);
}
var PROF={};
function name(u){var p=PROF[u]||(W.MC&&W.MC.profiles&&W.MC.profiles[u]);return (p&&p.displayName)||(W.MC_DISPLAY&&W.MC_DISPLAY[u])||u;}
function avatar(u){
  var p=PROF[u]||{};
  var inner=p.profileImage||p.photo?('<img src="'+esc(p.profileImage||p.photo)+'" alt="">'):(p.emoji?esc(p.emoji):esc(name(u).charAt(0).toUpperCase()));
  return '<div class="mc-av"'+(p.color?' style="background:'+esc(p.color)+'"':'')+'>'+inner+'</div>';
}
function friendBtn(b,v){
  function paint(){
    var s=frState(v);
    b.textContent=s==='friend'?'🤝':(s==='incoming'?'✅':(s==='sent'?'⏳':'➕'));
    b.title=T(s==='friend'?'Friends':(s==='incoming'?'Accept friend request':(s==='sent'?'Request sent':'Add friend')));
  }
  frLoad().then(paint);paint();
  b.onclick=function(){
    var s=frState(v),p;
    if(s==='friend'){if(!confirm(T('Remove')+' '+name(v)+' '+T('from your friends?')))return;p=frRemove(v);}
    else if(s==='sent')p=frCancel(v);
    else p=frAdd(v);
    p.then(paint);paint();
  };
}
function renderFriends(c){
  var u=me();
  if(!u){c.innerHTML='<div class="empty" style="padding-top:110px"><div class="big">🤝</div>'+esc(T('Sign in to add friends.'))+'</div>';return;}
  c.innerHTML='<div class="wrap mxs-fr"><div class="page-title"><span class="emoji">🤝</span>'+esc(T('Friends'))+'</div>'
    +'<div class="page-sub">'+esc(T('Add club members as friends, accept requests and jump into an encrypted chat.'))+'</div>'
    +'<input class="mc-search" id="frQ" placeholder="'+esc(T('Search the club…'))+'" style="width:100%;margin:6px 0 14px">'
    +'<div id="frBody"><div class="u-loading">'+esc(T('Loading…'))+'</div></div></div>';
  var q='';
  Promise.all([frLoad(true),W.fbGet('/members'),W.fbGet('/profiles')]).then(function(r){
    var members=Object.keys(r[1]||{});(W.MC_CORE||[]).forEach(function(x){if(members.indexOf(x)<0)members.push(x);});
    PROF=r[2]||{};
    function row(v){
      var s=frState(v),btns;
      if(s==='friend')btns='<button class="btn sm" data-a="chat" data-u="'+esc(v)+'">💬 '+esc(T('Chat'))+'</button><button class="btn ghost sm" data-a="rm" data-u="'+esc(v)+'">'+esc(T('Remove'))+'</button>';
      else if(s==='incoming')btns='<button class="btn sm" data-a="ok" data-u="'+esc(v)+'">✅ '+esc(T('Accept'))+'</button><button class="btn ghost sm" data-a="no" data-u="'+esc(v)+'">'+esc(T('Decline'))+'</button>';
      else if(s==='sent')btns='<button class="btn ghost sm" data-a="cancel" data-u="'+esc(v)+'">⏳ '+esc(T('Cancel request'))+'</button>';
      else btns='<button class="btn sm" data-a="add" data-u="'+esc(v)+'">➕ '+esc(T('Add friend'))+'</button>';
      return '<div class="mxs-row">'+avatar(v)+'<div class="mxs-mid"><b>'+esc(name(v))+'</b><small>@'+esc(v)+'</small></div><div class="mxs-btns">'+btns+'</div></div>';
    }
    function sect(title,list,empty){
      return '<h3 class="mxa-h" style="margin:18px 0 8px">'+title+' <span class="mxs-n">'+list.length+'</span></h3>'
        +(list.length?list.map(row).join(''):'<div class="mxs-empty">'+esc(empty)+'</div>');
    }
    function draw(){
      var b=$('frBody');if(!b)return;
      var others=members.filter(function(v){return v!==u;});
      var match=function(v){return !q||v.indexOf(q)>-1||name(v).toLowerCase().indexOf(q)>-1;};
      var inc=Object.keys(FR.inbox).filter(match),fr=Object.keys(FR.mine).filter(match),sent=Object.keys(FR.sent).filter(match);
      var rest=others.filter(function(v){return frState(v)==='none'&&match(v);});
      b.innerHTML=(inc.length?sect('📨 '+esc(T('Friend requests')),inc,''):'')
        +sect('🤝 '+esc(T('Your friends')),fr,T('No friends yet — add someone below!'))
        +(sent.length?sect('⏳ '+esc(T('Waiting for them')),sent,''):'')
        +sect('👥 '+esc(T('Everyone in the club')),rest,T('Nobody matches that.'));
      b.querySelectorAll('button[data-a]').forEach(function(btn){
        btn.onclick=function(){
          var v=btn.dataset.u,a=btn.dataset.a,p;
          if(a==='chat'){W.openTab({kind:'chat',refId:v});return;}
          if(a==='add')p=frAdd(v);else if(a==='ok')p=frAccept(v);else if(a==='no')p=frDecline(v);
          else if(a==='cancel')p=frCancel(v);else if(a==='rm'){if(!confirm(T('Remove')+' '+name(v)+' '+T('from your friends?')))return;p=frRemove(v);}
          btn.disabled=true;Promise.resolve(p).then(draw);
        };
      });
    }
    $('frQ').oninput=function(){q=(this.value||'').toLowerCase().trim();draw();};
    draw();
  });
}
W.renderFriends=renderFriends;W.MXFriends={load:frLoad,add:frAdd,accept:frAccept,remove:frRemove,state:frState};

function hookTabs(){
  if(W.STATIC&&!W.findSection('friends'))W.STATIC.push({kind:'friends',icon:'🤝',name:'Friends'});
  var rc=W.renderContent;if(rc.__mxs)return;
  W.renderContent=function(){
    var t=W.activeTab&&W.activeTab();
    if(t&&t.kind==='friends'){var c=$('content');renderFriends(c);try{W.lxDockSync();}catch(e){}return;}
    if(t&&t.kind==='myuser'){var r0=rc.apply(this,arguments);setTimeout(addMyUserCards,0);return r0;}
    return rc.apply(this,arguments);
  };
  W.renderContent.__mxs=1;
}

/* =====================================================================
   3. BOTTOM MENU (customisable from My User)
   ===================================================================== */
var DOCK_KEY='matix_dock_items';
var DOCK_ALL=[
  {k:'home',i:'⌂',l:'Home'},{k:'learn',i:'🦉',l:'Learn'},{k:'ai',i:'✨',l:'Matix AI'},
  {k:'search',i:'🔍',l:'Search'},{k:'messages',i:'🔔',l:'Inbox'},{k:'myuser',i:'👤',l:'My User'},
  {k:'chat',i:'🧮',l:'Math Chat'},{k:'friends',i:'🤝',l:'Friends'},{k:'games',i:'🎮',l:'Games'},
  {k:'points',i:'⭐',l:'Points'},{k:'ideas',i:'💡',l:'Ideas'},{k:'translate',i:'🌐',l:'Translator'},
  {k:'discussions',i:'💬',l:'Discussions'},{k:'users',i:'👥',l:'Users'},{k:'theme',i:'🎨',l:'Theme'},
  {k:'bugs',i:'🐞',l:'Bugs'}
];
var DOCK_DEFAULT=['home','learn','games','messages','myuser'];
var DOCK_MAX=6;
function dockItems(){
  var v=null;try{v=JSON.parse(lsG(DOCK_KEY)||'null');}catch(e){}
  if(!Array.isArray(v)||!v.length)v=DOCK_DEFAULT.slice();
  return v.filter(function(k){return DOCK_ALL.some(function(a){return a.k===k;});}).slice(0,DOCK_MAX);
}
function dockBuild(){
  var dock=$('lbDock');if(!dock)return;
  dock.querySelectorAll('button[data-dock]').forEach(function(b){b.remove();});
  dockItems().forEach(function(k){
    var a=DOCK_ALL.filter(function(x){return x.k===k;})[0];
    var b=D.createElement('button');b.dataset.dock=k;
    b.innerHTML='<span>'+a.i+'</span><small>'+esc(T(a.l))+'</small>';
    b.onclick=function(){if(k==='search'){W.openPalette();return;}W.openTab({kind:k});};
    dock.appendChild(b);
  });
  try{W.lxDockSync();}catch(e){}
  try{W.mxBetaDecorate&&W.mxBetaDecorate();}catch(e){}
}
W.mxDockBuild=dockBuild;

function addMyUserCards(){
  var wrap=D.querySelector('.mxa-me');if(!wrap||$('muDock'))return;
  var s1=D.createElement('section');s1.className='card mxa-sec';s1.id='muDock';s1.style.marginTop='16px';
  var s2=D.createElement('section');s2.className='card mxa-sec';s2.id='muE2E';s2.style.marginTop='16px';
  var s3=D.createElement('section');s3.className='card mxa-sec';s3.id='muFriends';s3.style.marginTop='16px';
  wrap.appendChild(s1);wrap.appendChild(s3);wrap.appendChild(s2);
  drawDockCard();drawE2ECard();drawFriendsCard();
}
function drawDockCard(){
  var box=$('muDock');if(!box)return;
  var on=dockItems();
  var list=on.concat(DOCK_ALL.map(function(a){return a.k;}).filter(function(k){return on.indexOf(k)<0;}));
  box.innerHTML='<h3 class="mxa-h">🧭 '+esc(T('Bottom menu'))+'</h3><div class="mxa-sub">'
    +esc(T('Pick what shows in the mini bottom menu (up to'))+' '+DOCK_MAX+') '+esc(T('and use the arrows to change the order.'))+'</div>'
    +'<div class="mxs-dock">'+list.map(function(k,i){
      var a=DOCK_ALL.filter(function(x){return x.k===k;})[0],sel=on.indexOf(k)>-1;
      return '<div class="mxs-dk'+(sel?' on':'')+'"><label><input type="checkbox" data-k="'+k+'"'+(sel?' checked':'')+'> <span>'+a.i+'</span> '+esc(T(a.l))+'</label>'
        +(sel?'<span class="mxs-arr"><button type="button" data-up="'+k+'"'+(i===0?' disabled':'')+'>▲</button><button type="button" data-dn="'+k+'"'+(i===on.length-1?' disabled':'')+'>▼</button></span>':'')+'</div>';
    }).join('')+'</div>'
    +'<label class="mxs-dk on" style="margin-top:10px"><span><input type="checkbox" id="muDockAlways"'+((W.mxMenu&&W.mxMenu.always())?' checked':'')+'> 📌 '+esc(T('Always show the bottom menu'))+'</span></label>'
    +'<div class="mxa-btns" style="margin-top:12px"><button class="btn ghost sm" id="muDockReset">'+esc(T('Reset to default'))+'</button></div>';
  function save(v){lsS(DOCK_KEY,JSON.stringify(v));W.mxDockBuild();drawDockCard();}
  box.querySelectorAll('input[data-k]').forEach(function(cb){
    cb.onchange=function(){
      var v=dockItems(),k=cb.dataset.k;
      if(cb.checked){if(v.length>=DOCK_MAX){cb.checked=false;toast(T('The menu holds up to')+' '+DOCK_MAX+' '+T('items'),'err');return;}v.push(k);}
      else{if(v.length<=1){cb.checked=true;toast(T('Keep at least one item'),'err');return;}v=v.filter(function(x){return x!==k;});}
      save(v);
    };
  });
  box.querySelectorAll('button[data-up],button[data-dn]').forEach(function(b){
    b.onclick=function(){
      var v=dockItems(),k=b.dataset.up||b.dataset.dn,i=v.indexOf(k),j=b.dataset.up?i-1:i+1;
      if(i<0||j<0||j>=v.length)return;var t=v[i];v[i]=v[j];v[j]=t;save(v);
    };
  });
  $('muDockReset').onclick=function(){save(DOCK_DEFAULT.slice());toast(T('Bottom menu reset'),'ok');};
  $('muDockAlways').onchange=function(){if(W.mxMenu)W.mxMenu.setAlways(this.checked);};
}
function drawE2ECard(){
  var box=$('muE2E');if(!box)return;var u=me();
  if(!SUB){box.innerHTML='<h3 class="mxa-h">🔒 '+esc(T('Encrypted chat'))+'</h3><p class="mxa-note">'+esc(T('This device does not support encryption.'))+'</p>';return;}
  box.innerHTML='<h3 class="mxa-h">🔒 '+esc(T('Encrypted chat'))+'</h3><div class="mxa-sub">'+esc(T('Math Chat messages are end-to-end encrypted. Your private chat key lives only on this device.'))+'</div>'
    +'<div class="mxa-kv"><span>'+esc(T('Your key code'))+'</span><b id="muKeyCode">…</b></div>'
    +'<p class="mxa-note">'+esc(T('New phone? Back up your key with a passphrase here, then restore it on the new phone so you can read your old messages. Use a passphrase that is NOT your Matix password.'))+'</p>'
    +'<input id="muBkPass" type="password" placeholder="'+esc(T('Backup passphrase (8+ characters)'))+'" autocomplete="new-password">'
    +'<div class="mxa-btns" style="margin-top:10px"><button class="btn" id="muBk">💾 '+esc(T('Back up my key'))+'</button><button class="btn ghost" id="muRs">♻️ '+esc(T('Restore from backup'))+'</button></div><div class="err" id="muBkErr"></div>';
  loadKeys(u).then(function(){return safetyCode(E2E.pubC,null);}).then(function(c){if($('muKeyCode'))$('muKeyCode').textContent=c;});
  function pass(){var p=$('muBkPass').value||'';if(p.length<8){$('muBkErr').textContent=T('Use at least 8 characters.');return null;}$('muBkErr').textContent='';return p;}
  $('muBk').onclick=function(){var p=pass();if(!p)return;var b=this;b.disabled=true;
    loadKeys(u).then(function(){return backupKey(u,p);}).then(function(){toast('💾 '+T('Chat key backed up'),'ok');})
    ['catch'](function(){$('muBkErr').textContent=T('Could not back up. Check your connection.');}).then(function(){b.disabled=false;});};
  $('muRs').onclick=function(){var p=pass();if(!p)return;var b=this;b.disabled=true;
    restoreKey(u,p).then(function(){toast('♻️ '+T('Chat key restored'),'ok');drawE2ECard();})
    ['catch'](function(e){$('muBkErr').textContent=T(e.message==='nobackup'?'No backup found for your account yet.':(e.message==='badpass'?'That passphrase is wrong.':'Could not restore. Check your connection.'));})
    .then(function(){b.disabled=false;});};
}
function drawFriendsCard(){
  var box=$('muFriends');if(!box)return;
  frLoad(true).then(function(){
    var n=Object.keys(FR.mine).length,r=Object.keys(FR.inbox).length;
    box.innerHTML='<h3 class="mxa-h">🤝 '+esc(T('Friends'))+'</h3>'
      +'<div class="mxa-kv"><span>'+esc(T('Friends'))+'</span><b>'+n+'</b></div>'
      +'<div class="mxa-kv"><span>'+esc(T('Friend requests'))+'</span><b>'+r+'</b></div>'
      +'<div class="mxa-btns" style="margin-top:10px"><button class="btn" id="muFrOpen">🤝 '+esc(T('Open Friends'))+'</button></div>';
    $('muFrOpen').onclick=function(){W.openTab({kind:'friends'});};
  });
}

/* ---------- styles ---------- */
var css=D.createElement('style');
css.textContent='.mxs-row{display:flex;align-items:center;gap:12px;padding:10px 12px;border:1px solid var(--border);border-radius:14px;margin-bottom:8px;background:var(--card)}'
 +'.mxs-mid{flex:1;min-width:0;display:flex;flex-direction:column}.mxs-mid small{color:var(--muted)}'
 +'.mxs-btns{display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end}'
 +'.mxs-n{font-size:.75rem;color:var(--muted);font-weight:600}.mxs-empty{color:var(--muted);font-size:.88rem;padding:6px 2px}'
 +'.mxs-dock{display:flex;flex-direction:column;gap:6px;margin-top:10px}'
 +'.mxs-dk{display:flex;align-items:center;justify-content:space-between;padding:8px 12px;border:1px solid var(--border);border-radius:12px}'
 +'.mxs-dk.on{border-color:var(--mxa-accent,#5b8def)}.mxs-dk label{display:flex;align-items:center;gap:8px;cursor:pointer;flex:1}'
 +'.mxs-arr{display:flex;gap:4px}.mxs-arr button{width:30px;height:28px;border-radius:8px;border:1px solid var(--border);background:transparent;color:inherit}'
 +'.mxs-arr button:disabled{opacity:.3}'
 +'@media (max-width:520px){.mxs-row{flex-wrap:wrap}.mxs-btns{width:100%;justify-content:flex-start}}';
D.head.appendChild(css);

/* ---------- translations for the new strings ---------- */
(function(){
  var X={
   ar:{'Friends':'الأصدقاء','Add friend':'أضف صديقًا','Accept':'قبول','Decline':'رفض','Remove':'إزالة','Chat':'دردشة','Cancel request':'إلغاء الطلب','Friend requests':'طلبات الصداقة','Your friends':'أصدقاؤك','Waiting for them':'بانتظار الرد','Everyone in the club':'كل أعضاء النادي','Open Friends':'افتح الأصدقاء','Bottom menu':'القائمة السفلية','Reset to default':'إعادة الضبط','Encrypted chat':'دردشة مشفرة','Encrypted message':'رسالة مشفرة','Your key code':'رمز مفتاحك','Back up my key':'انسخ مفتاحي احتياطيًا','Restore from backup':'استعادة من النسخة الاحتياطية','Chat key backed up':'تم نسخ مفتاح الدردشة','Chat key restored':'تمت استعادة مفتاح الدردشة','My User':'حسابي','Inbox':'الوارد','Learn':'تعلّم','Counting & place value':'العد والقيمة المنزلية','Adding & subtracting':'الجمع والطرح','Times & dividing':'الضرب والقسمة','Time & shapes':'الوقت والأشكال','Sign in to add friends.':'سجّل الدخول لإضافة أصدقاء.','No friends yet — add someone below!':'لا أصدقاء بعد — أضف أحدًا بالأسفل!'},
   fr:{'Friends':'Amis','Add friend':'Ajouter en ami','Accept':'Accepter','Decline':'Refuser','Remove':'Retirer','Chat':'Discuter','Cancel request':'Annuler la demande','Friend requests':"Demandes d'ami",'Your friends':'Tes amis','Waiting for them':'En attente','Everyone in the club':'Tout le club','Open Friends':'Ouvrir Amis','Bottom menu':'Menu du bas','Reset to default':'Réinitialiser','Encrypted chat':'Chat chiffré','Encrypted message':'Message chiffré','Your key code':'Code de ta clé','Back up my key':'Sauvegarder ma clé','Restore from backup':'Restaurer la sauvegarde','Chat key backed up':'Clé sauvegardée','Chat key restored':'Clé restaurée','My User':'Mon compte','Inbox':'Boîte','Learn':'Apprendre','Counting & place value':'Compter et valeur de position','Adding & subtracting':'Additionner et soustraire','Times & dividing':'Multiplier et diviser','Time & shapes':"L'heure et les formes",'Sign in to add friends.':'Connecte-toi pour ajouter des amis.','No friends yet — add someone below!':"Pas encore d'amis — ajoutes-en un ci-dessous !"},
   es:{'Friends':'Amigos','Add friend':'Añadir amigo','Accept':'Aceptar','Decline':'Rechazar','Remove':'Quitar','Chat':'Chatear','Cancel request':'Cancelar solicitud','Friend requests':'Solicitudes de amistad','Your friends':'Tus amigos','Waiting for them':'Esperando respuesta','Everyone in the club':'Todo el club','Open Friends':'Abrir Amigos','Bottom menu':'Menú inferior','Reset to default':'Restablecer','Encrypted chat':'Chat cifrado','Encrypted message':'Mensaje cifrado','Your key code':'Código de tu clave','Back up my key':'Guardar mi clave','Restore from backup':'Restaurar copia','Chat key backed up':'Clave guardada','Chat key restored':'Clave restaurada','My User':'Mi cuenta','Inbox':'Bandeja','Learn':'Aprender','Counting & place value':'Contar y valor posicional','Adding & subtracting':'Sumar y restar','Times & dividing':'Multiplicar y dividir','Time & shapes':'La hora y las formas','Sign in to add friends.':'Inicia sesión para añadir amigos.','No friends yet — add someone below!':'Aún no tienes amigos: ¡añade a alguien abajo!'}
  };
  try{if(typeof W.I18N!=='object')return;Object.keys(X).forEach(function(l){var d=W.I18N[l]||(W.I18N[l]={});Object.keys(X[l]).forEach(function(k){if(d[k]===undefined)d[k]=X[l][k];});});}catch(e){}
})();

/* ---------- start ---------- */
function boot(){
  hookChat();hookAuth();hookTabs();W.mxDockBuild();
  if(me())loadKeys(me());
  /* friend requests show up as a red count on the Friends button */
  setInterval(function(){
    if(!me())return;
    frLoad().then(function(){
      var n=Object.keys(FR.inbox).length,b=D.querySelector('#lbDock button[data-dock="friends"] small');
      if(b)b.textContent=T('Friends')+(n?' ('+n+')':'');
    });
  },30000);
}
if(D.readyState==='loading')D.addEventListener('DOMContentLoaded',boot);else boot();
})();
