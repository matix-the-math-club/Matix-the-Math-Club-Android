/* =====================================================================
   Matix schools in the app (1.2.0)
   - Sign-in screen: "🎒 I'm a student" for accounts a teacher made on
     matixthemathclub.com (username + password from the teacher).
   - Students find their homework in 🎮 Games → At home (the arcade
     reads /student_of/<user> and /homework/<class>).
   - Teachers, school administrators and owners get a 🍎 Teacher tab. Classes, student
     accounts, homework and Matix Present quizzes are made on the
     website, so the tab opens the Teacher page there in the browser.
   ===================================================================== */
(function(){
'use strict';
var D=document,W=window,FBU='https://matix-1d538-default-rtdb.firebaseio.com',SITE='https://www.matixthemathclub.com';
function $(id){return D.getElementById(id);}
function T(s){try{return typeof W.tr==='function'?W.tr(s):s;}catch(e){return s;}}
function E(s){return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}
function raw(p){return fetch(FBU+p+'.json',{cache:'no-store'}).then(function(r){return r.ok?r.json():null;})['catch'](function(){return null;});}
function me(){return W.ME||null;}
function owner(){try{return typeof W.isOwner==='function'&&W.isOwner();}catch(e){return false;}}

/* ---------- 🎒 I'm a student, on the sign-in screen ---------- */
function studentButton(){
  var sw=$('gSwap');if(!sw||$('gKid'))return;
  var b=D.createElement('button');b.type='button';b.id='gKid';b.className='gate-back mx-kid';b.textContent='🎒 '+T('I’m a student');
  sw.parentNode.insertBefore(b,sw.nextSibling);
  b.onclick=function(){
    var sl=$('gSwapA');if(sl&&/Sign in/.test(sl.textContent))sl.click();
    $('gTitle').textContent='🎒 '+T('I’m a student');
    $('gSub').textContent=T('Your teacher made your account. Type the username and password your teacher gave you.');
    $('gErr').textContent='';try{$('gUser').focus();}catch(e){}
  };
}

/* ---------- 🍎 Teacher tab ---------- */
var TEACH=false,ADMIN=false;
function addTab(){
  if(!W.STATIC||W.STATIC.some(function(s){return s.kind==='teacher';}))return;
  W.STATIC.push({kind:'teacher',icon:'🍎',name:'Teacher'});
  try{W.navPaint();}catch(e){}
}
function check(){
  var u=me();if(!u)return;
  if(owner()){TEACH=true;ADMIN=true;addTab();return;}
  raw('/teachers/'+encodeURIComponent(u)).then(function(t){if(t){TEACH=true;addTab();}});
  raw('/school_admins/'+encodeURIComponent(u)).then(function(a){if(a){ADMIN=true;addTab();}});
}
function page(c){
  c.innerHTML='<div class="mxs-app"><div class="mxs-hero"><div class="mxs-ic">🍎</div><h1>'+E(T('Teacher'))+'</h1>'
    +'<p>'+E(T('Make classes and student accounts, give homework with easy lessons, and run live quizzes on the smart board with Matix Present. It all happens on the Matix website.'))+'</p>'
    +'<a class="btn mxs-open" href="'+SITE+'/#/teacher" target="_blank" rel="noopener">🌐 '+E(T('Open the Teacher page on the website'))+'</a>'
    +'<a class="btn ghost mxs-open" href="'+SITE+'/#/teacher/classes" target="_blank" rel="noopener">🏫 '+E(T('My classes and homework'))+'</a>'
    +(ADMIN?'<a class="btn ghost mxs-open" href="'+SITE+'/#/school" target="_blank" rel="noopener">🏫 '+E(T('My school (administrators)'))+'</a>':'')
    +'<p class="mxs-small">'+E(T('Sign in there with the same username and password as in the app.'))+'</p></div>'
    +'<div class="mxs-tips"><div><b>🎒 '+E(T('Student accounts'))+'</b><span>'+E(T('Type your students’ first names and Matix makes their usernames and passwords. Print them for the class.'))+'</span></div>'
    +'<div><b>📝 '+E(T('Homework'))+'</b><span>'+E(T('Pick a topic: students get an easy lesson, then exercises. You see every score.'))+'</span></div>'
    +'<div><b>🎯 '+E(T('Matix Present'))+'</b><span>'+E(T('Live quizzes on the board. Students join from their phones with a Game PIN.'))+'</span></div></div></div>';
}
function wire(){
  studentButton();
  var rc=W.renderContent;
  if(typeof rc==='function'&&!rc.__mxs){
    W.renderContent=function(){
      var t=null;try{t=W.activeTab();}catch(e){}
      if(t&&t.kind==='teacher'){var c=$('content');page(c);try{W.highlightNav();}catch(e){}try{W.lxDockSync();}catch(e){}return;}
      return rc.apply(this,arguments);
    };
    W.renderContent.__mxs=true;
  }
  check();
  var last=me();setInterval(function(){if(me()!==last){last=me();if(last)check();}},3000);
}
var css=D.createElement('style');
css.textContent='.mx-kid{margin-top:10px;font-weight:800}'
 +'.mxs-app{max-width:760px;margin:0 auto;padding:16px}'
 +'.mxs-hero{text-align:center;padding:26px 18px;border-radius:24px;background:linear-gradient(135deg,rgba(239,68,68,.16),rgba(139,92,246,.16))}'
 +'.mxs-ic{font-size:3.2rem}.mxs-hero h1{margin:6px 0}.mxs-hero p{opacity:.85;line-height:1.5}'
 +'.mxs-open{display:flex;align-items:center;justify-content:center;min-height:52px;margin:10px auto 0;max-width:420px;font-size:1.02rem;text-decoration:none}'
 +'.mxs-small{font-size:.85rem;opacity:.7}'
 +'.mxs-tips{display:grid;gap:10px;margin-top:16px}.mxs-tips div{padding:14px;border-radius:16px;border:1px solid var(--border,rgba(0,0,0,.12))}.mxs-tips b{display:block;margin-bottom:4px}.mxs-tips span{opacity:.8}';
D.head.appendChild(css);
if(W.trAdd)W.trAdd({'I’m a student':'Je suis élève','Your teacher made your account. Type the username and password your teacher gave you.':'Ton enseignant a créé ton compte. Écris le nom d’utilisateur et le mot de passe qu’il t’a donnés.',
  'Teacher':'Enseignant','Open the Teacher page on the website':'Ouvrir la page Enseignant sur le site','My classes and homework':'Mes classes et devoirs','My school (administrators)':'Mon école (administrateurs)','Sign in there with the same username and password as in the app.':'Connecte-toi là-bas avec le même nom d’utilisateur et mot de passe que dans l’appli.',
  'Make classes and student accounts, give homework with easy lessons, and run live quizzes on the smart board with Matix Present. It all happens on the Matix website.':'Crée des classes et des comptes élèves, donne des devoirs avec des leçons faciles et lance des quiz en direct au tableau avec Matix Present. Tout se passe sur le site Matix.',
  'Student accounts':'Comptes élèves','Type your students’ first names and Matix makes their usernames and passwords. Print them for the class.':'Écris les prénoms de tes élèves et Matix crée leurs noms d’utilisateur et mots de passe. Imprime-les pour la classe.',
  'Homework':'Devoirs','Pick a topic: students get an easy lesson, then exercises. You see every score.':'Choisis un thème : les élèves ont une leçon facile, puis des exercices. Tu vois tous les scores.',
  'Live quizzes on the board. Students join from their phones with a Game PIN.':'Des quiz en direct au tableau. Les élèves rejoignent depuis leur téléphone avec un code PIN.'});
if(D.readyState==='loading')D.addEventListener('DOMContentLoaded',wire);else wire();
})();
