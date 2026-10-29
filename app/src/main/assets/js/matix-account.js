/* =====================================================================
   MATIX ACCOUNT (v1.0.3)
   - email on every account (private: stored only under /members, never on profiles)
   - 6-digit codes by email: verify email, reset a forgotten password, change email
   - sign in with username OR email
   - members from before 1.0.3 are asked to connect + verify an email
   - "My User" tab: profile, email, password, sign-out
   - owner can only see member passwords after re-typing the owner password
   Email is sent with EmailJS (https://www.emailjs.com). The owner pastes the
   Service ID / Template ID / Public key in My User → Email sending, or fills MXA_EMAILJS below.
   ===================================================================== */
(function(){
'use strict';
var W=window,D=document;
var MXA_EMAILJS={serviceId:'',templateId:'',publicKey:''};   /* optional: hard-code here instead of /config/emailjs */
var CODE_MIN=15, MAX_TRIES=5, RESEND_MS=45000, PW_UNLOCK_MS=10*60000;

/* ---------- tiny helpers ---------- */
function $(id){return D.getElementById(id);}
function T(s){try{return typeof W.tr==='function'?W.tr(s):s;}catch(e){return s;}}
function esc(s){return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
function enc(s){return encodeURIComponent(s);}
function toast(m,k){try{W.toast(m,k);}catch(e){}}
function base(){return W.FB||'https://matix-1d538-default-rtdb.firebaseio.com';}
function fb(path,method,data){
  var o={method:method||'GET'};
  if(data!==undefined){o.headers={'Content-Type':'application/json'};o.body=JSON.stringify(data);}
  if(data!==undefined&&typeof W.mxCryptXform==='function'){data=W.mxCryptXform(base(),path,data,'enc');o.body=JSON.stringify(data);}
  return fetch(base()+path+'.json',o).then(function(r){if(!r.ok)throw new Error('db '+r.status);return r.json();}).then(function(d){return typeof W.mxCryptXform==='function'?W.mxCryptXform(base(),path,d,'dec'):d;});
}
var get=function(p){return fb(p);},put=function(p,d){return fb(p,'PUT',d);},patch=function(p,d){return fb(p,'PATCH',d);},del=function(p){return fb(p,'DELETE');};
function normU(u){return typeof W.normUser==='function'?W.normUser(u):String(u||'').toLowerCase().replace(/[^a-z0-9_]/g,'');}
function normE(e){return String(e||'').trim().toLowerCase();}
function validEmail(e){return /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/.test(e)&&e.length<=120;}
function maskEmail(e){e=String(e||'');var i=e.indexOf('@');if(i<1)return e;var n=e.slice(0,i);return n.slice(0,Math.min(2,n.length))+'•••'+e.slice(i);}
async function sha(s){
  var b=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(s));
  return Array.prototype.map.call(new Uint8Array(b),function(x){return ('0'+x.toString(16)).slice(-2);}).join('');
}
function emailKey(e){return sha('matix-email:'+normE(e));}
function newCode(){var a=new Uint32Array(1);crypto.getRandomValues(a);return String(a[0]%1000000).padStart(6,'0');}
function me(){return W.ME||normU(typeof W.getUser==='function'?W.getUser():'');}
function isOwnerNow(){try{return typeof W.isOwner==='function'&&W.isOwner();}catch(e){return false;}}

/* ---------- email sending (EmailJS) ---------- */
var CFG=null;
function loadCfg(force){
  if(CFG&&!force)return Promise.resolve(CFG);
  return get('/config/emailjs').then(function(v){v=v||{};
    CFG={serviceId:MXA_EMAILJS.serviceId||v.serviceId||'',templateId:MXA_EMAILJS.templateId||v.templateId||'',publicKey:MXA_EMAILJS.publicKey||v.publicKey||''};
    return CFG;
  })['catch'](function(){CFG=CFG||{serviceId:MXA_EMAILJS.serviceId,templateId:MXA_EMAILJS.templateId,publicKey:MXA_EMAILJS.publicKey};return CFG;});
}
function emailReady(){return !!(CFG&&CFG.serviceId&&CFG.templateId&&CFG.publicKey);}
var PURPOSE={
  verify:{subject:'Verify your Matix email',message:'Use this code to verify your email for Matix.'},
  reset:{subject:'Reset your Matix password',message:'Use this code to reset your Matix password. If you did not ask for this, you can ignore this email.'},
  change:{subject:'Confirm your new Matix email',message:'Use this code to confirm this new email address for your Matix account.'},
  test:{subject:'Matix email test',message:'Email sending works. This is a test code.'}
};
async function sendMail(to,user,code,purpose){
  await loadCfg();
  if(!emailReady())throw new Error('noservice');
  var p=PURPOSE[purpose]||PURPOSE.verify;
  var r=await fetch('https://api.emailjs.com/api/v1.0/email/send',{method:'POST',headers:{'Content-Type':'application/json'},
    body:JSON.stringify({service_id:CFG.serviceId,template_id:CFG.templateId,user_id:CFG.publicKey,
      template_params:{to_email:to,email:to,to_name:user,username:user,code:code,passcode:code,subject:p.subject,message:p.message,expires:String(CODE_MIN),app_name:'Matix'}})});
  if(!r.ok){var t='';try{t=await r.text();}catch(e){}throw new Error('send '+r.status+' '+t);}
  return true;
}

/* ---------- one-time codes (only a hash is stored) ---------- */
async function issueCode(u,purpose,to,extra){
  var path='/authCodes/'+enc(u);
  var cur=null;try{cur=await get(path);}catch(e){}
  if(cur&&cur.purpose===purpose&&cur.sentAt&&Date.now()-cur.sentAt<RESEND_MS&&(!extra||!extra.email||cur.email===extra.email))
    throw new Error('wait:'+Math.ceil((RESEND_MS-(Date.now()-cur.sentAt))/1000));
  var code=newCode();
  var rec={purpose:purpose,h:await sha(code+':'+u+':'+purpose),exp:Date.now()+CODE_MIN*60000,sentAt:Date.now(),tries:0};
  if(extra)for(var k in extra)rec[k]=extra[k];
  await put(path,rec);
  try{await sendMail(to,u,code,purpose);}catch(e){try{await del(path);}catch(_){}throw e;}
  return true;
}
async function checkCode(u,purpose,code){
  var path='/authCodes/'+enc(u);
  var c=await get(path);
  code=String(code||'').replace(/\D/g,'');
  if(!c||c.purpose!==purpose)return {err:'none'};
  if(Date.now()>c.exp){await del(path);return {err:'expired'};}
  if((c.tries||0)>=MAX_TRIES)return {err:'locked'};
  if(code.length!==6||await sha(code+':'+u+':'+purpose)!==c.h){await patch(path,{tries:(c.tries||0)+1});return {err:'wrong',left:MAX_TRIES-(c.tries||0)-1};}
  await del(path);return {ok:true,rec:c};
}
function codeErr(r){
  return r.err==='expired'?T('That code expired. Send a new one.'):r.err==='locked'?T('Too many wrong tries. Send a new code.'):
         r.err==='none'?T('No active code. Send a new one.'):T('Wrong code.')+(r.left>0?' '+T('Tries left:')+' '+r.left:'');
}
function sendErr(e){
  var m=String(e&&e.message||e);
  if(m.indexOf('wait:')===0)return T('Wait a moment before asking for another code')+' ('+m.slice(5)+'s).';
  if(m==='noservice')return T('Email sending is not set up yet. Ask the owner.');
  return T('Could not send the email. Check your connection and try again.');
}

/* ---------- account data ---------- */
async function lookupEmail(e){var u=await get('/emailIndex/'+await emailKey(e));return typeof u==='string'?u:null;}
async function emailFree(e,u){var o=await lookupEmail(e);return !o||o===u;}
async function attachEmail(u,e){
  e=normE(e);
  var m=await get('/members/'+enc(u))||{};
  if(m.email&&normE(m.email)!==e){try{var ok=await emailKey(m.email);var o=await get('/emailIndex/'+ok);if(o===u)await del('/emailIndex/'+ok);}catch(_){}}
  await put('/emailIndex/'+await emailKey(e),u);
  await patch('/members/'+enc(u),{email:e,emailVerified:true,emailVerifiedAt:Date.now()});
}
function passOk(u,p){return typeof W.verifyPassword==='function'?W.verifyPassword(u,p):Promise.resolve(false);}

/* ---------- shared card UI ---------- */
function codeInput(id){return '<input id="'+id+'" class="mxa-code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" placeholder="••••••">';}
function fld(label,id,type,ph,ac){return '<div class="mxa-fld"><label for="'+id+'">'+esc(T(label))+'</label><input id="'+id+'" type="'+(type||'text')+'"'+(ph?' placeholder="'+esc(ph)+'"':'')+(ac?' autocomplete="'+ac+'"':'')+'></div>';}
function busy(btn,on,txt){if(!btn)return;btn.disabled=!!on;if(txt!==undefined)btn.textContent=txt;}
function onEnter(ids,btnId){ids.forEach(function(i){var n=$(i);if(n)n.addEventListener('keydown',function(e){if(e.key==='Enter'){e.preventDefault();var b=$(btnId);if(b&&!b.disabled)b.click();}});});}

/* =====================================================================
   SIGN-IN SCREEN: email on create, username-or-email, forgot password
   ===================================================================== */
var gateCard=null;
function gateMain(){var a=$('gateAuth');return a?a.querySelector('.gate-card:not(#mxaGate)'):null;}
function patchGate(){
  var gu=$('gUser');if(!gu||$('gEmail'))return;
  var f=D.createElement('div');f.className='fld';f.id='gEmailFld';f.style.display='none';
  f.innerHTML='<label for="gEmail">'+esc(T('Email'))+'</label><input id="gEmail" type="email" autocomplete="email" placeholder="you@example.com">';
  gu.closest('.fld').after(f);
  var fg=D.createElement('div');fg.className='swap mxa-forgot';fg.id='gForgot';fg.innerHTML='<a id="gForgotA">'+esc(T('Forgot password?'))+'</a>';
  var er=$('gErr');if(er)er.after(fg);
  $('gForgotA').onclick=function(){showForgot();};
  gateCard=D.createElement('div');gateCard.className='gate-card mxa-card mxa-gcard';gateCard.id='mxaGate';gateCard.style.display='none';
  var main=gateMain();if(main)main.after(gateCard);
  $('gEmail').addEventListener('keydown',function(e){if(e.key==='Enter')$('gBtn').click();});
}
var _refreshGate=W.refreshGate;
W.refreshGate=function(){
  try{if(_refreshGate)_refreshGate();}catch(e){}
  patchGate();
  var join=W.authMode==='join';
  var lab=D.querySelector('label[for="gUser"]');if(lab)lab.textContent=T(join?'Username':'Username or email');
  var gu=$('gUser');if(gu)gu.placeholder=join?'yourname':T('yourname or you@example.com');
  if($('gEmailFld'))$('gEmailFld').style.display=join?'':'none';
  if($('gForgot'))$('gForgot').style.display=join?'none':'';
};
function showMainCard(){var m=gateMain();if(m)m.style.display='';if(gateCard)gateCard.style.display='none';}
function showGateCard(html){patchGate();var m=gateMain();if(m)m.style.display='none';gateCard.innerHTML=html;gateCard.style.display='';try{$('gate').scrollTop=0;}catch(e){}}

async function doJoinEmail(rawU,pass,email){
  var u=String(rawU||'').toLowerCase().replace(/[^a-z0-9_]/g,'');
  email=normE(email);
  if(!u||u.length<2)return 'short';
  if(!pass||pass.length<4)return 'shortpass';
  if(!validEmail(email))return 'email';
  if(W.VALID_MEMBERS&&W.VALID_MEMBERS[u])return 'reserved';
  var ex=await get('/members/'+enc(u));
  if(ex)return 'taken';
  if(!(await emailFree(email,u)))return 'emailtaken';
  await put('/members/'+enc(u),{password:pass,joinedAt:Date.now(),testUser:true,email:email,emailVerified:false});
  try{await put('/profiles/'+enc(u),{displayName:u,bio:'',updatedAt:Date.now()});}catch(e){}
  if(typeof W.setUser==='function')W.setUser(u);
  return 'ok';
}
async function onGateBtn(){
  var b=$('gBtn'),e=$('gErr');e.textContent='';b.disabled=true;
  try{
    var u=$('gUser').value,p=$('gPass').value;
    if(W.authMode==='join'){
      var r=await doJoinEmail(u,p,$('gEmail')?$('gEmail').value:'');
      if(r==='ok'){
        var uu=me();var em=normE($('gEmail').value);
        await loadCfg();
        if(emailReady()){try{await issueCode(uu,'verify',em,{email:em});LOCK_SENT=Date.now();}catch(x){}}
        W.enterApp();return;
      }
      e.textContent=T({short:'Username too short (min 2).',shortpass:'Password too short (min 4).',reserved:'That username is reserved.',taken:'That username is already taken.',
        email:'Enter a real email address.',emailtaken:'That email is already used by another account.'}[r]||'Could not create account.');
    }else{
      var id=String(u||'').trim();
      if(id.indexOf('@')>0){
        var found=null;try{found=await lookupEmail(id);}catch(x){}
        if(!found){e.textContent=T('No account uses that email.');b.disabled=false;return;}
        id=found;
      }
      var ok=await W.doSignIn(id,p);
      if(ok){W.enterApp();return;}
      e.textContent=T('Wrong username/email or password.');
    }
  }catch(err){e.textContent=T('Something went wrong. Check your connection and try again.');}
  b.disabled=false;
}

/* ---------- forgot password (username + email → code → new password) ---------- */
function showForgot(prefU){
  showGateCard('<div class="mxa-ic">🔑</div><h2>'+esc(T('Forgot your password?'))+'</h2>'
    +'<p class="mxa-sub">'+esc(T('Type your username and the email on your account. We will email you a 6-digit code.'))+'</p>'
    +fld('Username','fgUser','text','yourname','username')+fld('Email','fgEmail','email','you@example.com','email')
    +'<button class="mxa-btn" id="fgSend">'+esc(T('Send reset code'))+'</button><div class="mxa-err" id="fgErr"></div>'
    +'<button class="mxa-link" id="fgBack">← '+esc(T('Back to sign in'))+'</button>');
  if(prefU)$('fgUser').value=prefU;
  $('fgBack').onclick=showMainCard;
  onEnter(['fgUser','fgEmail'],'fgSend');
  $('fgSend').onclick=async function(){
    var b=this,er=$('fgErr');er.textContent='';
    var u=normU($('fgUser').value),em=normE($('fgEmail').value);
    if(!u){er.textContent=T('Type your username.');return;}
    if(!validEmail(em)){er.textContent=T('Enter a real email address.');return;}
    busy(b,true,T('Sending…'));
    try{
      var m=await get('/members/'+enc(u));
      if(!m||!m.email){er.textContent=T('This account has no email connected yet. Ask the owner for help.');busy(b,false,T('Send reset code'));return;}
      if(normE(m.email)!==em){er.textContent=T('That username and email do not match.');busy(b,false,T('Send reset code'));return;}
      await issueCode(u,'reset',em);
      showResetCode(u,em);
    }catch(x){er.textContent=sendErr(x);busy(b,false,T('Send reset code'));}
  };
}
function resetCodeHtml(em){
  return '<div class="mxa-ic">📬</div><h2>'+esc(T('Check your email'))+'</h2>'
    +'<p class="mxa-sub">'+esc(T('We sent a 6-digit code to'))+' <b>'+esc(maskEmail(em))+'</b>. '+esc(T('It works for 15 minutes.'))+'</p>'
    +'<div class="mxa-fld"><label for="rcCode">'+esc(T('Reset code'))+'</label>'+codeInput('rcCode')+'</div>'
    +fld('New password','rcPass','password','••••••••','new-password')+fld('Repeat new password','rcPass2','password','••••••••','new-password')
    +'<button class="mxa-btn" id="rcGo">'+esc(T('Change password'))+'</button><div class="mxa-err" id="rcErr"></div>'
    +'<button class="mxa-link" id="rcResend">'+esc(T('Send a new code'))+'</button>';
}
function wireResetCode(u,em,done,back){
  $('rcResend').onclick=async function(){try{await issueCode(u,'reset',em);toast(T('New code sent'),'ok');}catch(x){$('rcErr').textContent=sendErr(x);}};
  onEnter(['rcCode','rcPass','rcPass2'],'rcGo');
  $('rcGo').onclick=async function(){
    var b=this,er=$('rcErr');er.textContent='';
    var p1=$('rcPass').value,p2=$('rcPass2').value;
    if(!p1||p1.length<4){er.textContent=T('Password too short (min 4).');return;}
    if(p1!==p2){er.textContent=T('The two passwords are different.');return;}
    busy(b,true,T('Checking…'));
    try{
      var r=await checkCode(u,'reset',$('rcCode').value);
      if(!r.ok){er.textContent=codeErr(r);busy(b,false,T('Change password'));return;}
      await patch('/members/'+enc(u),{password:p1,emailVerified:true,passwordChangedAt:Date.now()});
      var rev=Date.now();try{await patch('/members/'+enc(u),{sessionRev:rev});}catch(_){}
      done(rev);
    }catch(x){er.textContent=T('Something went wrong. Check your connection and try again.');busy(b,false,T('Change password'));}
  };
}
function showResetCode(u,em){
  showGateCard(resetCodeHtml(em)+'<button class="mxa-link" id="rcBack">← '+esc(T('Back to sign in'))+'</button>');
  $('rcBack').onclick=showMainCard;
  wireResetCode(u,em,function(){
    showMainCard();
    if(W.authMode==='join'){W.authMode='signin';W.refreshGate();}
    $('gUser').value=u;$('gPass').value='';$('gErr').textContent='';
    try{$('gPass').focus();}catch(e){}
    toast(T('Password changed. Sign in with your new password.'),'ok');
  });
}

/* =====================================================================
   EMAIL LOCK: everyone needs a verified email to use the app
   ===================================================================== */
var LOCK_SENT=0,LOCK_ON=false;
function lockShell(html){
  var l=$('mxaLock');
  if(!l){l=D.createElement('div');l.id='mxaLock';l.setAttribute('role','dialog');l.setAttribute('aria-modal','true');D.body.appendChild(l);}
  l.innerHTML='<div class="mxa-card">'+html+'<button class="mxa-link mxa-out" id="lkOut">'+esc(T('Sign out'))+'</button></div>';
  D.documentElement.classList.add('mxa-locked');LOCK_ON=true;
  $('lkOut').onclick=function(){try{W.doSignOut();}catch(e){location.reload();}};
}
function unlock(){var l=$('mxaLock');if(l)l.remove();D.documentElement.classList.remove('mxa-locked');LOCK_ON=false;}
function lockConnect(u,isNew){
  lockShell('<div class="mxa-ic">✉️</div><h2>'+esc(T('Connect your email'))+'</h2>'
    +'<p class="mxa-sub">'+esc(T('Matix now uses email to keep accounts safe and to reset forgotten passwords. Your email stays private — other members never see it.'))+'</p>'
    +fld('Email','lkEmail','email','you@example.com','email')
    +'<button class="mxa-btn" id="lkSend">'+esc(T('Send code'))+'</button><div class="mxa-err" id="lkErr"></div>');
  onEnter(['lkEmail'],'lkSend');
  setTimeout(function(){try{$('lkEmail').focus();}catch(e){}},150);
  $('lkSend').onclick=async function(){
    var b=this,er=$('lkErr');er.textContent='';var em=normE($('lkEmail').value);
    if(!validEmail(em)){er.textContent=T('Enter a real email address.');return;}
    busy(b,true,T('Sending…'));
    try{
      if(!(await emailFree(em,u))){er.textContent=T('That email is already used by another account.');busy(b,false,T('Send code'));return;}
      await patch('/members/'+enc(u),{email:em,emailVerified:false});
      await issueCode(u,'verify',em,{email:em});LOCK_SENT=Date.now();
      lockCode(u,em);
    }catch(x){er.textContent=sendErr(x);busy(b,false,T('Send code'));}
  };
}
function lockCode(u,em){
  lockShell('<div class="mxa-ic">📬</div><h2>'+esc(T('Verify your email'))+'</h2>'
    +'<p class="mxa-sub">'+esc(T('We sent a 6-digit code to'))+' <b>'+esc(maskEmail(em))+'</b>. '+esc(T('Type it here to start using Matix.'))+'</p>'
    +'<div class="mxa-fld"><label for="lkCode">'+esc(T('Verification code'))+'</label>'+codeInput('lkCode')+'</div>'
    +'<button class="mxa-btn" id="lkGo">'+esc(T('Verify'))+'</button><div class="mxa-err" id="lkErr"></div>'
    +'<div class="mxa-row"><button class="mxa-link" id="lkResend">'+esc(T('Send a new code'))+'</button><button class="mxa-link" id="lkOther">'+esc(T('Use a different email'))+'</button></div>');
  onEnter(['lkCode'],'lkGo');
  setTimeout(function(){try{$('lkCode').focus();}catch(e){}},150);
  $('lkOther').onclick=function(){lockConnect(u);};
  $('lkResend').onclick=async function(){$('lkErr').textContent='';try{await issueCode(u,'verify',em,{email:em});toast(T('New code sent'),'ok');}catch(x){$('lkErr').textContent=sendErr(x);}};
  $('lkGo').onclick=async function(){
    var b=this,er=$('lkErr');er.textContent='';busy(b,true,T('Checking…'));
    try{
      var r=await checkCode(u,'verify',$('lkCode').value);
      if(!r.ok){er.textContent=codeErr(r);busy(b,false,T('Verify'));return;}
      var target=normE(r.rec.email||em);
      if(!(await emailFree(target,u))){er.textContent=T('That email is already used by another account.');busy(b,false,T('Verify'));return;}
      await attachEmail(u,target);
      unlock();toast(T('Email verified ✓'),'ok');
      try{if(W.ACTIVE!=null&&typeof W.renderContent==='function'){var t=W.activeTab&&W.activeTab();if(t&&t.kind==='myuser')W.renderContent();}}catch(e){}
    }catch(x){er.textContent=T('Something went wrong. Check your connection and try again.');busy(b,false,T('Verify'));}
  };
}
var checking=false;
async function emailCheck(){
  var u=me();if(!u||checking)return;checking=true;
  try{
    await loadCfg();
    var m;try{m=await get('/members/'+enc(u));}catch(e){return;}   /* offline: never lock anyone out */
    m=m||{};
    if(m.email&&m.emailVerified){unlock();return;}
    if(!emailReady()){
      if(isOwnerNow())setTimeout(function(){toast(T('Email sending is not set up — open My User → Email sending.'),'err');},2500);
      return;   /* codes cannot be delivered yet, so nobody is locked out */
    }
    if(m.email&&!m.emailVerified){
      if(!LOCK_SENT||Date.now()-LOCK_SENT>CODE_MIN*60000){
        var c=null;try{c=await get('/authCodes/'+enc(u));}catch(e){}
        if(!c||c.purpose!=='verify'||Date.now()>c.exp){try{await issueCode(u,'verify',m.email,{email:normE(m.email)});LOCK_SENT=Date.now();}catch(e){}}
      }
      lockCode(u,m.email);
    }else lockConnect(u);
  }finally{checking=false;}
}

/* =====================================================================
   MY USER tab
   ===================================================================== */
function renderMyUser(c){
  var u=me();
  c.innerHTML='<div class="wrap mxa-me"><div class="page-title"><span class="emoji">👤</span>'+esc(T('My User'))+'</div>'
    +'<div class="page-sub">'+esc(T('Your profile, email, password and sign-in — all in one place.'))+'</div>'
    +'<div class="mxa-grid"><section class="card mxa-sec" id="muProfile"><div class="u-loading">'+esc(T('Loading…'))+'</div></section>'
    +'<section class="card mxa-sec" id="muAccount"><div class="u-loading">'+esc(T('Loading…'))+'</div></section></div>'
    +(isOwnerNow()?'<section class="card mxa-sec" id="muMail" style="margin-top:16px"></section>':'')+'</div>';
  Promise.all([get('/profiles/'+enc(u)).catch(function(){return null;}),get('/members/'+enc(u)).catch(function(){return null;}),loadCfg()]).then(function(res){
    if(!$('muProfile'))return;
    drawProfile(u,res[0]||{});drawAccount(u,res[1]||{});
    if($('muMail'))drawMail();
  });
}
function drawProfile(u,pr){
  var box=$('muProfile');
  var st={emoji:pr.emoji||'',pic:(typeof W.profileImg==='function'?W.profileImg(pr):'')||''};
  box.innerHTML='<h3 class="mxa-h">🎨 '+esc(T('Profile'))+'</h3><div class="mxa-sub">'+esc(T('This is what the club sees on the Users page and next to your name.'))+'</div>'
    +'<div class="pf-prev"><div class="pf-av" id="muAv"></div><div><b id="muPrevName"></b><small class="sub">@'+esc(u)+'</small></div></div>'
    +'<label class="fl">'+esc(T('Display name'))+'</label><input id="muName" maxlength="28" value="'+esc(pr.displayName||'')+'" placeholder="'+esc(u)+'">'
    +'<label class="fl">'+esc(T('About you'))+'</label><textarea id="muBio" maxlength="180" placeholder="'+esc(T('one line about you'))+'">'+esc(pr.bio||'')+'</textarea>'
    +'<label class="fl">'+esc(T('Avatar emoji'))+'</label><div class="emoji-pick" id="muEmoji"></div>'
    +'<label class="fl">'+esc(T('Profile picture'))+'</label><input id="muFile" type="file" accept="image/*">'
    +'<button class="btn ghost sm" id="muDrop" style="margin-top:8px;'+(st.pic?'':'display:none')+'">'+esc(T('Remove my picture'))+'</button>'
    +'<label class="fl">'+esc(T('Favourite colour'))+'</label><input id="muColor" type="color" value="'+esc(pr.color||'#5b46c9')+'" style="height:40px;padding:3px">'
    +'<div class="err" id="muErr"></div><button class="btn block" id="muSave" style="margin-top:14px">'+esc(T('Save my profile'))+'</button>';
  function prev(){var a=$('muAv');if(!a)return;if(st.pic)a.innerHTML='<img src="'+esc(st.pic)+'" alt="">';else a.textContent=st.emoji||(W.initials?W.initials(u):u.slice(0,2).toUpperCase());
    $('muPrevName').textContent=$('muName').value||u;$('muDrop').style.display=st.pic?'':'none';}
  var ep=$('muEmoji');
  (W.PF_EMOJIS||['🧠','📐','🧮','🚀','🌟','🔥','🎯','🐱']).forEach(function(e){
    var b=D.createElement('button');b.className='em'+(e===st.emoji?' on':'');b.textContent=e;b.type='button';
    b.onclick=function(){st.emoji=st.emoji===e?'':e;ep.querySelectorAll('.em').forEach(function(n){n.classList.remove('on');});if(st.emoji)b.classList.add('on');prev();};
    ep.appendChild(b);
  });
  $('muName').oninput=prev;
  $('muFile').onchange=function(){var f=this.files&&this.files[0];if(!f)return;$('muErr').textContent=T('Shrinking your picture…');
    W.shrinkImage(f,256,function(d){if(!d){$('muErr').textContent=T('That image would not load. Try another one.');return;}st.pic=d;$('muErr').textContent='';prev();});};
  $('muDrop').onclick=function(){st.pic='';prev();toast(T('Picture removed — hit save'));};
  $('muSave').onclick=function(){
    var b=this;busy(b,true,T('Saving…'));
    patch('/profiles/'+enc(u),{displayName:($('muName').value||'').trim()||u,bio:($('muBio').value||'').trim(),emoji:st.emoji||null,profileImage:st.pic||null,color:$('muColor').value||null,updatedAt:Date.now()})
      .then(function(){busy(b,false,T('Save my profile'));toast(T('Profile saved'),'ok');try{W.loadAvatar();}catch(e){}try{W.USERS_CACHE=null;}catch(e){}})
      ['catch'](function(){busy(b,false,T('Save my profile'));$('muErr').textContent=T('Could not save. Check your connection and try again.');});
  };
  prev();
}
function drawAccount(u,m){
  var box=$('muAccount');var em=m.email||'',ver=!!m.emailVerified;
  box.innerHTML='<h3 class="mxa-h">🔐 '+esc(T('Account'))+'</h3>'
    +'<div class="mxa-kv"><span>'+esc(T('Username'))+'</span><b>@'+esc(u)+'</b></div>'
    +'<div class="mxa-kv"><span>'+esc(T('Email'))+'</span><b id="muEm">'+(em?esc(maskEmail(em)):'<i>'+esc(T('not connected'))+'</i>')+'</b>'
    +(em?'<button class="mxa-mini" id="muEmShow">'+esc(T('Show'))+'</button>':'')+'<span class="mxa-chip '+(ver?'ok':'no')+'">'+esc(T(ver?'Verified':'Not verified'))+'</span></div>'
    +'<p class="mxa-note">🔒 '+esc(T('Only you can see your email. It is never shown on your profile.'))+'</p>'
    +'<div class="mxa-btns"><button class="btn" id="muChEm">✉️ '+esc(T(em?'Change email':'Connect email'))+'</button>'
    +'<button class="btn" id="muChPw">🔑 '+esc(T('Change password'))+'</button>'
    +'<button class="btn ghost" id="muForgot">'+esc(T('Forgot password?'))+'</button></div>'
    +'<h3 class="mxa-h" style="margin-top:22px">🚪 '+esc(T('Sign out'))+'</h3>'
    +'<div class="mxa-btns"><button class="btn ghost" id="muOut">'+esc(T('Sign out'))+'</button><button class="btn ghost" id="muOutAll">'+esc(T('Sign out of all devices'))+'</button></div>';
  if($('muEmShow'))$('muEmShow').onclick=function(){var s=this.textContent===T('Show');$('muEm').textContent=s?em:maskEmail(em);this.textContent=T(s?'Hide':'Show');};
  $('muChEm').onclick=function(){dlgEmail(u,em);};
  $('muChPw').onclick=function(){dlgPassword(u);};
  $('muForgot').onclick=function(){dlgForgot(u,em);};
  $('muOut').onclick=function(){try{W.doSignOut();}catch(e){}};
  $('muOutAll').onclick=function(){try{W.doSignOutAll();}catch(e){}};
}
function drawMail(){
  var box=$('muMail');var c=CFG||{};
  box.innerHTML='<h3 class="mxa-h">📮 '+esc(T('Email sending (owner)'))+' <span class="mxa-chip '+(emailReady()?'ok':'no')+'">'+esc(T(emailReady()?'On':'Off'))+'</span></h3>'
    +'<p class="mxa-note">'+esc(T('Codes are emailed through a free EmailJS account. Paste its Service ID, Template ID and Public key. Until this is set, nobody is asked to verify.'))+'</p>'
    +'<div class="mxa-3"><div><label class="fl">Service ID</label><input id="mjS" value="'+esc(c.serviceId||'')+'" placeholder="service_xxxxxxx"></div>'
    +'<div><label class="fl">Template ID</label><input id="mjT" value="'+esc(c.templateId||'')+'" placeholder="template_xxxxxxx"></div>'
    +'<div><label class="fl">Public key</label><input id="mjK" value="'+esc(c.publicKey||'')+'" placeholder="xxxxxxxxxxxxxxx"></div></div>'
    +'<div class="mxa-btns" style="margin-top:12px"><button class="btn" id="mjSave">'+esc(T('Save'))+'</button><button class="btn ghost" id="mjTest">'+esc(T('Send me a test email'))+'</button></div><div class="err" id="mjErr"></div>';
  $('mjSave').onclick=function(){
    var v={serviceId:$('mjS').value.trim(),templateId:$('mjT').value.trim(),publicKey:$('mjK').value.trim(),updatedAt:Date.now(),by:me()};
    put('/config/emailjs',v).then(function(){return loadCfg(true);}).then(function(){toast(T('Saved'),'ok');drawMail();})['catch'](function(){$('mjErr').textContent=T('Could not save. Check your connection and try again.');});
  };
  $('mjTest').onclick=async function(){
    var b=this;$('mjErr').textContent='';busy(b,true,T('Sending…'));
    try{var m=await get('/members/'+enc(me()))||{};if(!m.email)throw new Error('noemail');await sendMail(m.email,me(),newCode(),'test');toast(T('Test email sent to')+' '+maskEmail(m.email),'ok');}
    catch(x){$('mjErr').textContent=String(x.message)==='noemail'?T('Connect your own email first (Account → Connect email).'):sendErr(x)+' '+String(x.message||'').slice(0,140);}
    busy(b,false,T('Send me a test email'));
  };
}

/* ---------- My User dialogs (reuse the app modal) ---------- */
function dlg(html){var mc=$('modalCard');mc.innerHTML='<button class="modal-x" id="mX">×</button>'+html;$('modal').classList.add('open');$('mX').onclick=function(){W.closeModal();};}
function refreshMe(){try{var t=W.activeTab&&W.activeTab();if(t&&t.kind==='myuser')W.renderContent();}catch(e){}}
function dlgEmail(u,cur){
  dlg('<h2>✉️ '+esc(T(cur?'Change email':'Connect email'))+'</h2><div class="mh-sub">'+esc(T('We will send a code to the new email to make sure it is yours.'))+'</div>'
    +(cur?'<label class="fl">'+esc(T('Current password'))+'</label><input id="deP" type="password" autocomplete="current-password">':'')
    +'<label class="fl">'+esc(T('New email'))+'</label><input id="deE" type="email" autocomplete="email" placeholder="you@example.com">'
    +'<div class="err" id="deErr"></div><button class="btn block" id="deGo" style="margin-top:14px">'+esc(T('Send code'))+'</button>');
  $('deGo').onclick=async function(){
    var b=this,er=$('deErr');er.textContent='';var em=normE($('deE').value);
    if(!validEmail(em)){er.textContent=T('Enter a real email address.');return;}
    if(cur&&normE(cur)===em){er.textContent=T('That is already your email.');return;}
    busy(b,true,T('Sending…'));
    try{
      if(cur&&!(await passOk(u,$('deP').value))){er.textContent=T('Wrong password.');busy(b,false,T('Send code'));return;}
      if(!(await emailFree(em,u))){er.textContent=T('That email is already used by another account.');busy(b,false,T('Send code'));return;}
      await issueCode(u,'change',em,{email:em});
      dlg('<h2>📬 '+esc(T('Check your email'))+'</h2><div class="mh-sub">'+esc(T('We sent a 6-digit code to'))+' <b>'+esc(maskEmail(em))+'</b>.</div>'
        +'<label class="fl">'+esc(T('Code'))+'</label>'+codeInput('deC')+'<div class="err" id="deErr2"></div>'
        +'<button class="btn block" id="deOk" style="margin-top:14px">'+esc(T('Confirm new email'))+'</button>');
      $('deOk').onclick=async function(){
        var b2=this,e2=$('deErr2');e2.textContent='';busy(b2,true,T('Checking…'));
        try{var r=await checkCode(u,'change',$('deC').value);
          if(!r.ok){e2.textContent=codeErr(r);busy(b2,false,T('Confirm new email'));return;}
          await attachEmail(u,r.rec.email||em);W.closeModal();toast(T('Email changed ✓'),'ok');refreshMe();
        }catch(x){e2.textContent=T('Something went wrong. Check your connection and try again.');busy(b2,false,T('Confirm new email'));}
      };
    }catch(x){er.textContent=sendErr(x);busy(b,false,T('Send code'));}
  };
}
function dlgPassword(u){
  dlg('<h2>🔑 '+esc(T('Change password'))+'</h2>'
    +'<label class="fl">'+esc(T('Current password'))+'</label><input id="dpO" type="password" autocomplete="current-password">'
    +'<label class="fl">'+esc(T('New password'))+'</label><input id="dpN" type="password" autocomplete="new-password">'
    +'<label class="fl">'+esc(T('Repeat new password'))+'</label><input id="dpN2" type="password" autocomplete="new-password">'
    +'<div class="err" id="dpErr"></div><button class="btn block" id="dpGo" style="margin-top:14px">'+esc(T('Change password'))+'</button>');
  $('dpGo').onclick=async function(){
    var b=this,er=$('dpErr');er.textContent='';var n1=$('dpN').value,n2=$('dpN2').value;
    if(!n1||n1.length<4){er.textContent=T('Password too short (min 4).');return;}
    if(n1!==n2){er.textContent=T('The two passwords are different.');return;}
    busy(b,true,T('Saving…'));
    try{
      if(!(await passOk(u,$('dpO').value))){er.textContent=T('Wrong password.');busy(b,false,T('Change password'));return;}
      await setPassword(u,n1);W.closeModal();toast(T('Password changed ✓ Other devices were signed out.'),'ok');
    }catch(x){er.textContent=T('Something went wrong. Check your connection and try again.');busy(b,false,T('Change password'));}
  };
}
async function setPassword(u,p){
  var rev=Date.now();
  await patch('/members/'+enc(u),{password:p,passwordChangedAt:rev,sessionRev:rev});
  try{W.setLocalSessionRev(u,rev);}catch(e){}
}
function dlgForgot(u,em){
  if(!em){W.customAlert(T('Connect an email first — the reset code is sent there.'),null,{icon:'✉️'});return;}
  issueCode(u,'reset',normE(em)).then(function(){
    dlg('<h2>🔑 '+esc(T('Reset your password'))+'</h2>'+resetCodeHtml(em).replace(/<div class="mxa-ic">.*?<\/div><h2>.*?<\/h2>/,''));
    wireResetCode(u,normE(em),function(rev){try{W.setLocalSessionRev(u,rev);}catch(e){}W.closeModal();toast(T('Password changed ✓ Other devices were signed out.'),'ok');});
  })['catch'](function(x){W.customAlert(sendErr(x),null,{icon:'📡'});});
}

/* =====================================================================
   OWNER: passwords stay hidden until the owner re-types their password
   ===================================================================== */
var PW_UNTIL=0;
function pwUnlocked(){return Date.now()<PW_UNTIL;}
W.mxaPwHtml=function(u,i,big){
  var txt=(i.testUser&&i.password)?null:(!i.testUser?(big?'opted out of saving':'🔒 opted out'):(big?'none saved':'🔒 none'));
  if(txt)return big?'<div class="card owner-note" style="margin-top:14px"><b style="font-size:.8rem;color:var(--muted)">🔑 '+esc(T('PASSWORD (OWNER ONLY)'))+'</b><div class="u-pw" style="margin-top:6px;display:inline-block">'+esc(txt)+'</div></div>':'<div class="u-pw">'+esc(txt)+'</div>';
  var inner='<span class="u-pw mxa-pw" data-mxa-pw="'+esc(u)+'" title="'+esc(T('Type your password to show'))+'">'+(pwUnlocked()?'🔑 '+esc(i.password):'🔑 •••••••• <em>'+esc(T('Show'))+'</em>')+'</span>';
  return big?'<div class="card owner-note" style="margin-top:14px"><b style="font-size:.8rem;color:var(--muted)">🔑 '+esc(T('PASSWORD (OWNER ONLY)'))+'</b><div style="margin-top:6px">'+inner+'</div></div>':'<div>'+inner+'</div>';
};
function paintPw(){
  var map=(W.USERS_CACHE&&W.USERS_CACHE.map)||{};
  D.querySelectorAll('[data-mxa-pw]').forEach(function(n){var i=map[n.getAttribute('data-mxa-pw')];if(!i)return;
    n.innerHTML=pwUnlocked()?'🔑 '+esc(i.password):'🔑 •••••••• <em>'+esc(T('Show'))+'</em>';});
}
function askOwnerPw(){
  W.customConfirm('<div style="text-align:left"><b>'+esc(T('Confirm it is you'))+'</b><p style="margin:6px 0 10px;font-size:.9rem;opacity:.8">'+esc(T('Type your owner password to show member passwords for 10 minutes.'))+'</p>'
    +'<input id="mxaOwnPw" type="password" autocomplete="current-password" style="width:100%;box-sizing:border-box" placeholder="••••••••"></div>',
    async function(){
      var v=($('mxaOwnPw')||{}).value||'';
      if(await passOk(me(),v)){PW_UNTIL=Date.now()+PW_UNLOCK_MS;paintPw();setTimeout(paintPw,PW_UNLOCK_MS+500);toast(T('Passwords visible for 10 minutes'),'ok');}
      else toast(T('Wrong password.'),'err');
    },{html:true,icon:'🔐',okText:T('Show passwords'),danger:false});
  setTimeout(function(){try{$('mxaOwnPw').focus();}catch(e){}},80);
}
D.addEventListener('click',function(e){
  var n=e.target&&e.target.closest&&e.target.closest('[data-mxa-pw]');
  if(!n)return;
  e.stopPropagation();e.preventDefault();
  if(!isOwnerNow())return;
  if(pwUnlocked()){PW_UNTIL=0;paintPw();return;}   /* tap again to hide */
  askOwnerPw();
},true);

/* =====================================================================
   wiring
   ===================================================================== */
W.renderMyUser=renderMyUser;
W.mxaEmailCheck=emailCheck;
var _enter=W.enterApp;
W.enterApp=function(){var r=_enter.apply(this,arguments);setTimeout(emailCheck,60);return r;};
function wireGate(){var b=$('gBtn');if(b)b.onclick=onGateBtn;patchGate();try{W.refreshGate();}catch(e){}}
wireGate();
if(me()&&$('app')&&$('app').style.display!=='none')setTimeout(emailCheck,300);
W.addEventListener('storage',function(){});
/* re-check when the app comes back to the foreground (e.g. after reading the email) */
D.addEventListener('visibilitychange',function(){if(!D.hidden&&me()&&LOCK_ON){/* keep lock; nothing to do */}});

/* ---------- translations (merged into the app's I18N table) ---------- */
var L={
ar:{'My User':'مستخدمي','Email':'البريد الإلكتروني','Username':'اسم المستخدم','Username or email':'اسم المستخدم أو البريد الإلكتروني','yourname or you@example.com':'اسمك أو you@example.com','Forgot password?':'نسيت كلمة المرور؟',
 'Enter a real email address.':'أدخل بريدًا إلكترونيًا صحيحًا.','That email is already used by another account.':'هذا البريد مستخدم في حساب آخر.','No account uses that email.':'لا يوجد حساب بهذا البريد.','Wrong username/email or password.':'اسم المستخدم/البريد أو كلمة المرور غير صحيحة.',
 'Something went wrong. Check your connection and try again.':'حدث خطأ. تحقّق من الاتصال وحاول مجددًا.','Forgot your password?':'نسيت كلمة المرور؟','Type your username and the email on your account. We will email you a 6-digit code.':'اكتب اسم المستخدم والبريد المرتبط بحسابك. سنرسل لك رمزًا من 6 أرقام.',
 'Send reset code':'أرسل رمز إعادة التعيين','Back to sign in':'العودة لتسجيل الدخول','Type your username.':'اكتب اسم المستخدم.','Sending…':'جارٍ الإرسال…','This account has no email connected yet. Ask the owner for help.':'لا يوجد بريد مرتبط بهذا الحساب بعد. اطلب المساعدة من المالك.',
 'That username and email do not match.':'اسم المستخدم والبريد غير متطابقين.','Check your email':'تحقّق من بريدك','We sent a 6-digit code to':'أرسلنا رمزًا من 6 أرقام إلى','It works for 15 minutes.':'صالح لمدة 15 دقيقة.','Reset code':'رمز إعادة التعيين',
 'New password':'كلمة المرور الجديدة','Repeat new password':'أعد كتابة كلمة المرور الجديدة','Change password':'تغيير كلمة المرور','Send a new code':'أرسل رمزًا جديدًا','New code sent':'تم إرسال رمز جديد','Password too short (min 4).':'كلمة المرور قصيرة جدًا (4 على الأقل).',
 'The two passwords are different.':'كلمتا المرور مختلفتان.','Checking…':'جارٍ التحقق…','Password changed. Sign in with your new password.':'تم تغيير كلمة المرور. سجّل الدخول بكلمة المرور الجديدة.','Sign out':'تسجيل الخروج',
 'Connect your email':'اربط بريدك الإلكتروني','Matix now uses email to keep accounts safe and to reset forgotten passwords. Your email stays private — other members never see it.':'يستخدم ماتيكس الآن البريد الإلكتروني لحماية الحسابات وإعادة تعيين كلمات المرور المنسية. بريدك يبقى خاصًا — لا يراه الأعضاء الآخرون.',
 'Send code':'أرسل الرمز','Verify your email':'تحقّق من بريدك الإلكتروني','Type it here to start using Matix.':'اكتبه هنا لتبدأ باستخدام ماتيكس.','Verification code':'رمز التحقق','Verify':'تحقّق','Use a different email':'استخدم بريدًا آخر','Email verified ✓':'تم التحقق من البريد ✓',
 'That code expired. Send a new one.':'انتهت صلاحية الرمز. أرسل رمزًا جديدًا.','Too many wrong tries. Send a new code.':'محاولات خاطئة كثيرة. أرسل رمزًا جديدًا.','No active code. Send a new one.':'لا يوجد رمز فعّال. أرسل رمزًا جديدًا.','Wrong code.':'رمز خاطئ.','Tries left:':'المحاولات المتبقية:',
 'Wait a moment before asking for another code':'انتظر قليلًا قبل طلب رمز آخر','Email sending is not set up yet. Ask the owner.':'إرسال البريد غير مُعدّ بعد. اسأل المالك.','Could not send the email. Check your connection and try again.':'تعذّر إرسال البريد. تحقّق من الاتصال وحاول مجددًا.',
 'Your profile, email, password and sign-in — all in one place.':'ملفك الشخصي وبريدك وكلمة المرور وتسجيل الدخول — في مكان واحد.','Loading…':'جارٍ التحميل…','Profile':'الملف الشخصي','Account':'الحساب','not connected':'غير مربوط','Show':'إظهار','Hide':'إخفاء','Verified':'تم التحقق','Not verified':'لم يتم التحقق',
 'Only you can see your email. It is never shown on your profile.':'أنت وحدك ترى بريدك. لا يظهر أبدًا في ملفك الشخصي.','Change email':'تغيير البريد','Connect email':'ربط البريد','Sign out of all devices':'تسجيل الخروج من كل الأجهزة','Current password':'كلمة المرور الحالية','New email':'البريد الجديد',
 'We will send a code to the new email to make sure it is yours.':'سنرسل رمزًا إلى البريد الجديد للتأكد من أنه لك.','That is already your email.':'هذا بريدك الحالي بالفعل.','Wrong password.':'كلمة مرور خاطئة.','Code':'الرمز','Confirm new email':'تأكيد البريد الجديد','Email changed ✓':'تم تغيير البريد ✓',
 'Password changed ✓ Other devices were signed out.':'تم تغيير كلمة المرور ✓ وتم تسجيل الخروج من الأجهزة الأخرى.','Connect an email first — the reset code is sent there.':'اربط بريدًا أولًا — يُرسل رمز إعادة التعيين إليه.','Reset your password':'إعادة تعيين كلمة المرور',
 'PASSWORD (OWNER ONLY)':'كلمة المرور (للمالك فقط)','Type your password to show':'اكتب كلمة مرورك للإظهار','Confirm it is you':'أكّد أنك أنت','Type your owner password to show member passwords for 10 minutes.':'اكتب كلمة مرور المالك لإظهار كلمات مرور الأعضاء لمدة 10 دقائق.','Show passwords':'إظهار كلمات المرور','Passwords visible for 10 minutes':'كلمات المرور ظاهرة لمدة 10 دقائق',
 'Email sending (owner)':'إرسال البريد (المالك)','Save':'حفظ','Saved':'تم الحفظ','Send me a test email':'أرسل لي بريدًا تجريبيًا','Test email sent to':'تم إرسال بريد تجريبي إلى','On':'مفعّل','Off':'متوقف','Save my profile':'احفظ ملفي','Profile saved':'تم حفظ الملف الشخصي','Display name':'الاسم الظاهر','About you':'نبذة عنك','Avatar emoji':'رمز الصورة','Profile picture':'صورة الملف الشخصي','Favourite colour':'اللون المفضل','Remove my picture':'احذف صورتي'},
fr:{'My User':'Mon compte','Email':'E-mail','Username':"Nom d'utilisateur",'Username or email':"Nom d'utilisateur ou e-mail",'yourname or you@example.com':'tonnom ou toi@exemple.com','Forgot password?':'Mot de passe oublié ?',
 'Enter a real email address.':'Entre une vraie adresse e-mail.','That email is already used by another account.':'Cet e-mail est déjà utilisé par un autre compte.','No account uses that email.':"Aucun compte n'utilise cet e-mail.",'Wrong username/email or password.':'Nom d’utilisateur/e-mail ou mot de passe incorrect.',
 'Something went wrong. Check your connection and try again.':'Un problème est survenu. Vérifie ta connexion et réessaie.','Forgot your password?':'Mot de passe oublié ?','Type your username and the email on your account. We will email you a 6-digit code.':"Tape ton nom d'utilisateur et l'e-mail de ton compte. Nous t'enverrons un code à 6 chiffres.",
 'Send reset code':'Envoyer le code','Back to sign in':'Retour à la connexion','Type your username.':"Tape ton nom d'utilisateur.",'Sending…':'Envoi…','This account has no email connected yet. Ask the owner for help.':"Ce compte n'a pas encore d'e-mail. Demande de l'aide au propriétaire.",
 'That username and email do not match.':"Ce nom d'utilisateur et cet e-mail ne correspondent pas.",'Check your email':'Regarde tes e-mails','We sent a 6-digit code to':'Nous avons envoyé un code à 6 chiffres à','It works for 15 minutes.':'Il est valable 15 minutes.','Reset code':'Code de réinitialisation',
 'New password':'Nouveau mot de passe','Repeat new password':'Répète le nouveau mot de passe','Change password':'Changer le mot de passe','Send a new code':'Envoyer un nouveau code','New code sent':'Nouveau code envoyé','Password too short (min 4).':'Mot de passe trop court (min. 4).',
 'The two passwords are different.':'Les deux mots de passe sont différents.','Checking…':'Vérification…','Password changed. Sign in with your new password.':'Mot de passe changé. Connecte-toi avec le nouveau.','Sign out':'Se déconnecter',
 'Connect your email':'Associe ton e-mail','Matix now uses email to keep accounts safe and to reset forgotten passwords. Your email stays private — other members never see it.':"Matix utilise maintenant l'e-mail pour protéger les comptes et réinitialiser les mots de passe oubliés. Ton e-mail reste privé — les autres membres ne le voient jamais.",
 'Send code':'Envoyer le code','Verify your email':'Vérifie ton e-mail','Type it here to start using Matix.':'Tape-le ici pour commencer à utiliser Matix.','Verification code':'Code de vérification','Verify':'Vérifier','Use a different email':'Utiliser un autre e-mail','Email verified ✓':'E-mail vérifié ✓',
 'That code expired. Send a new one.':'Ce code a expiré. Envoie-en un nouveau.','Too many wrong tries. Send a new code.':'Trop d’essais. Envoie un nouveau code.','No active code. Send a new one.':'Aucun code actif. Envoie-en un nouveau.','Wrong code.':'Code incorrect.','Tries left:':'Essais restants :',
 'Wait a moment before asking for another code':'Attends un peu avant de demander un autre code','Email sending is not set up yet. Ask the owner.':"L'envoi d'e-mails n'est pas encore configuré. Demande au propriétaire.",'Could not send the email. Check your connection and try again.':"Impossible d'envoyer l'e-mail. Vérifie ta connexion et réessaie.",
 'Your profile, email, password and sign-in — all in one place.':'Ton profil, ton e-mail, ton mot de passe et ta connexion — au même endroit.','Loading…':'Chargement…','Profile':'Profil','Account':'Compte','not connected':'non associé','Show':'Afficher','Hide':'Masquer','Verified':'Vérifié','Not verified':'Non vérifié',
 'Only you can see your email. It is never shown on your profile.':'Toi seul vois ton e-mail. Il n’apparaît jamais sur ton profil.','Change email':"Changer d'e-mail",'Connect email':'Associer un e-mail','Sign out of all devices':'Se déconnecter de tous les appareils','Current password':'Mot de passe actuel','New email':'Nouvel e-mail',
 'We will send a code to the new email to make sure it is yours.':"Nous enverrons un code au nouvel e-mail pour vérifier qu'il est à toi.",'That is already your email.':'C’est déjà ton e-mail.','Wrong password.':'Mot de passe incorrect.','Code':'Code','Confirm new email':'Confirmer le nouvel e-mail','Email changed ✓':'E-mail changé ✓',
 'Password changed ✓ Other devices were signed out.':'Mot de passe changé ✓ Les autres appareils ont été déconnectés.','Connect an email first — the reset code is sent there.':"Associe d'abord un e-mail — le code y est envoyé.",'Reset your password':'Réinitialiser le mot de passe',
 'PASSWORD (OWNER ONLY)':'MOT DE PASSE (PROPRIÉTAIRE)','Type your password to show':'Tape ton mot de passe pour afficher','Confirm it is you':"Confirme que c'est toi",'Type your owner password to show member passwords for 10 minutes.':'Tape ton mot de passe de propriétaire pour afficher les mots de passe pendant 10 minutes.','Show passwords':'Afficher les mots de passe','Passwords visible for 10 minutes':'Mots de passe visibles 10 minutes',
 'Email sending (owner)':"Envoi d'e-mails (propriétaire)",'Save':'Enregistrer','Saved':'Enregistré','Send me a test email':"M'envoyer un e-mail de test",'Test email sent to':'E-mail de test envoyé à','On':'Activé','Off':'Désactivé','Save my profile':'Enregistrer mon profil','Profile saved':'Profil enregistré','Display name':'Nom affiché','About you':'À propos de toi','Avatar emoji':'Emoji d’avatar','Profile picture':'Photo de profil','Favourite colour':'Couleur préférée','Remove my picture':'Supprimer ma photo'},
es:{'My User':'Mi usuario','Email':'Correo','Username':'Usuario','Username or email':'Usuario o correo','yourname or you@example.com':'tunombre o tu@ejemplo.com','Forgot password?':'¿Olvidaste la contraseña?',
 'Enter a real email address.':'Escribe un correo real.','That email is already used by another account.':'Ese correo ya lo usa otra cuenta.','No account uses that email.':'Ninguna cuenta usa ese correo.','Wrong username/email or password.':'Usuario/correo o contraseña incorrectos.',
 'Something went wrong. Check your connection and try again.':'Algo salió mal. Revisa tu conexión e inténtalo de nuevo.','Forgot your password?':'¿Olvidaste tu contraseña?','Type your username and the email on your account. We will email you a 6-digit code.':'Escribe tu usuario y el correo de tu cuenta. Te enviaremos un código de 6 dígitos.',
 'Send reset code':'Enviar código','Back to sign in':'Volver a iniciar sesión','Type your username.':'Escribe tu usuario.','Sending…':'Enviando…','This account has no email connected yet. Ask the owner for help.':'Esta cuenta aún no tiene correo. Pide ayuda al dueño.',
 'That username and email do not match.':'El usuario y el correo no coinciden.','Check your email':'Revisa tu correo','We sent a 6-digit code to':'Enviamos un código de 6 dígitos a','It works for 15 minutes.':'Sirve durante 15 minutos.','Reset code':'Código de restablecimiento',
 'New password':'Nueva contraseña','Repeat new password':'Repite la nueva contraseña','Change password':'Cambiar contraseña','Send a new code':'Enviar un código nuevo','New code sent':'Código nuevo enviado','Password too short (min 4).':'Contraseña demasiado corta (mín. 4).',
 'The two passwords are different.':'Las dos contraseñas son distintas.','Checking…':'Comprobando…','Password changed. Sign in with your new password.':'Contraseña cambiada. Inicia sesión con la nueva.','Sign out':'Cerrar sesión',
 'Connect your email':'Conecta tu correo','Matix now uses email to keep accounts safe and to reset forgotten passwords. Your email stays private — other members never see it.':'Matix ahora usa el correo para proteger las cuentas y restablecer contraseñas olvidadas. Tu correo es privado: los demás miembros nunca lo ven.',
 'Send code':'Enviar código','Verify your email':'Verifica tu correo','Type it here to start using Matix.':'Escríbelo aquí para empezar a usar Matix.','Verification code':'Código de verificación','Verify':'Verificar','Use a different email':'Usar otro correo','Email verified ✓':'Correo verificado ✓',
 'That code expired. Send a new one.':'Ese código caducó. Envía uno nuevo.','Too many wrong tries. Send a new code.':'Demasiados intentos. Envía un código nuevo.','No active code. Send a new one.':'No hay código activo. Envía uno nuevo.','Wrong code.':'Código incorrecto.','Tries left:':'Intentos restantes:',
 'Wait a moment before asking for another code':'Espera un momento antes de pedir otro código','Email sending is not set up yet. Ask the owner.':'El envío de correos aún no está configurado. Pregunta al dueño.','Could not send the email. Check your connection and try again.':'No se pudo enviar el correo. Revisa tu conexión e inténtalo de nuevo.',
 'Your profile, email, password and sign-in — all in one place.':'Tu perfil, correo, contraseña e inicio de sesión, todo en un lugar.','Loading…':'Cargando…','Profile':'Perfil','Account':'Cuenta','not connected':'sin conectar','Show':'Mostrar','Hide':'Ocultar','Verified':'Verificado','Not verified':'Sin verificar',
 'Only you can see your email. It is never shown on your profile.':'Solo tú ves tu correo. Nunca aparece en tu perfil.','Change email':'Cambiar correo','Connect email':'Conectar correo','Sign out of all devices':'Cerrar sesión en todos los dispositivos','Current password':'Contraseña actual','New email':'Correo nuevo',
 'We will send a code to the new email to make sure it is yours.':'Enviaremos un código al correo nuevo para comprobar que es tuyo.','That is already your email.':'Ese ya es tu correo.','Wrong password.':'Contraseña incorrecta.','Code':'Código','Confirm new email':'Confirmar correo nuevo','Email changed ✓':'Correo cambiado ✓',
 'Password changed ✓ Other devices were signed out.':'Contraseña cambiada ✓ Se cerró la sesión en otros dispositivos.','Connect an email first — the reset code is sent there.':'Conecta primero un correo: el código se envía allí.','Reset your password':'Restablecer contraseña',
 'PASSWORD (OWNER ONLY)':'CONTRASEÑA (SOLO DUEÑO)','Type your password to show':'Escribe tu contraseña para mostrar','Confirm it is you':'Confirma que eres tú','Type your owner password to show member passwords for 10 minutes.':'Escribe tu contraseña de dueño para ver las contraseñas durante 10 minutos.','Show passwords':'Mostrar contraseñas','Passwords visible for 10 minutes':'Contraseñas visibles 10 minutos',
 'Email sending (owner)':'Envío de correos (dueño)','Save':'Guardar','Saved':'Guardado','Send me a test email':'Enviarme un correo de prueba','Test email sent to':'Correo de prueba enviado a','On':'Activado','Off':'Desactivado','Save my profile':'Guardar mi perfil','Profile saved':'Perfil guardado','Display name':'Nombre visible','About you':'Sobre ti','Avatar emoji':'Emoji de avatar','Profile picture':'Foto de perfil','Favourite colour':'Color favorito','Remove my picture':'Quitar mi foto'}
};
var L2={ar:{"Email sending is not set up — open My User → Email sending.": "إرسال البريد غير مُعدّ — افتح مستخدمي ← إرسال البريد.", "This is what the club sees on the Users page and next to your name.": "هذا ما يراه أعضاء النادي في صفحة المستخدمين وبجانب اسمك.", "one line about you": "سطر واحد عنك", "Shrinking your picture…": "جارٍ تصغير صورتك…", "That image would not load. Try another one.": "تعذّر تحميل هذه الصورة. جرّب صورة أخرى.", "Picture removed — hit save": "تمت إزالة الصورة — اضغط حفظ", "Saving…": "جارٍ الحفظ…", "Could not save. Check your connection and try again.": "تعذّر الحفظ. تحقّق من اتصالك وحاول مجددًا.", "Codes are emailed through a free EmailJS account. Paste its Service ID, Template ID and Public key. Until this is set, nobody is asked to verify.": "تُرسل الرموز عبر حساب EmailJS مجاني. الصق Service ID وTemplate ID وPublic key. إلى أن يتم ذلك، لن يُطلب من أحد التحقق.", "Connect your own email first (Account → Connect email).": "اربط بريدك أولًا (الحساب ← ربط البريد)."},fr:{"Email sending is not set up — open My User → Email sending.": "L'envoi d'e-mails n'est pas configuré — ouvre Mon compte → Envoi d'e-mails.", "This is what the club sees on the Users page and next to your name.": "C'est ce que le club voit sur la page Utilisateurs et à côté de ton nom.", "one line about you": "une ligne sur toi", "Shrinking your picture…": "Réduction de ta photo…", "That image would not load. Try another one.": "Cette image ne se charge pas. Essaie-en une autre.", "Picture removed — hit save": "Photo retirée — enregistre", "Saving…": "Enregistrement…", "Could not save. Check your connection and try again.": "Impossible d'enregistrer. Vérifie ta connexion et réessaie.", "Codes are emailed through a free EmailJS account. Paste its Service ID, Template ID and Public key. Until this is set, nobody is asked to verify.": "Les codes sont envoyés via un compte EmailJS gratuit. Colle son Service ID, son Template ID et sa Public key. Tant que ce n'est pas fait, personne ne doit vérifier.", "Connect your own email first (Account → Connect email).": "Connecte d'abord ton propre e-mail (Compte → Connecter un e-mail)."},es:{"Email sending is not set up — open My User → Email sending.": "El envío de correos no está configurado — abre Mi usuario → Envío de correos.", "This is what the club sees on the Users page and next to your name.": "Esto es lo que el club ve en la página de Usuarios y junto a tu nombre.", "one line about you": "una línea sobre ti", "Shrinking your picture…": "Reduciendo tu foto…", "That image would not load. Try another one.": "Esa imagen no se pudo cargar. Prueba otra.", "Picture removed — hit save": "Foto quitada — pulsa guardar", "Saving…": "Guardando…", "Could not save. Check your connection and try again.": "No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.", "Codes are emailed through a free EmailJS account. Paste its Service ID, Template ID and Public key. Until this is set, nobody is asked to verify.": "Los códigos se envían con una cuenta gratuita de EmailJS. Pega su Service ID, Template ID y Public key. Hasta entonces, nadie tiene que verificar.", "Connect your own email first (Account → Connect email).": "Conecta primero tu propio correo (Cuenta → Conectar correo)."}};
Object.keys(L2).forEach(function(l){L[l]=L[l]||{};Object.keys(L2[l]).forEach(function(k){L[l][k]=L2[l][k];});});
function mergeL(){try{if(!W.I18N)return;Object.keys(L).forEach(function(l){var d=W.I18N[l]||(W.I18N[l]={});Object.keys(L[l]).forEach(function(k){if(d[k]===undefined)d[k]=L[l][k];});});}catch(e){}}
mergeL();
try{W.refreshGate();}catch(e){}
try{if(W.ME&&$('nav'))W.renderNav();}catch(e){}
})();
