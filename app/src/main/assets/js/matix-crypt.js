/* =====================================================================
   Matix data scrambler — "end to end" scrambling of passwords and emails
   in Firebase.  The app scrambles on the way out and unscrambles on the
   way in, so the database only ever holds scrambled text.

   It is ONE fixed character-swap table for the whole database
   (b -> z, . -> h, @ -> d ... style).  The scrambled text never contains
   the characters Firebase forbids:  .  $  #  [  ]  /  newline / control chars
   (the swap table's output uses only letters, digits and safe symbols).

   - Characters in SRC swap 1:1 with the same position in DST.
   - The 8 characters that cannot be written as-is (. $ # [ ] / space ~)
     become  ~ + one safe character.
   - Control characters (tab, newline ...) become  ~~ + two hex swaps.
   - Scrambled values start with  mx1:  so old plain values are still
     understood and get upgraded automatically the next time they are read.
   ===================================================================== */
(function(W){
'use strict';
var PFX='mx1:',ESC='~';
var SRC='!"%&\'()*+,-0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ\\^_`abcdefghijklmnopqrstuvwxyz{|}';
var DST='&4;T"^O|v5:VLZwJWodX(,n=H\'m3U9yR%j!euCbK@rP+?phNiY8s)*}q`c>{1lfM-D2SE6gBt7FG0\\z<IaQxA_k';
var E_SRC='.$#[]/ ~';
var E_DST='ETo90\\6"';
var HEX='0123456789abcdef';
function enc(s){
  s=String(s==null?'':s);var out='';
  for(var i=0;i<s.length;i++){
    var c=s.charAt(i),k=SRC.indexOf(c),code=s.charCodeAt(i);
    if(k>=0){out+=DST.charAt(k);continue;}
    k=E_SRC.indexOf(c);
    if(k>=0){out+=ESC+E_DST.charAt(k);continue;}
    if(code<32||code===127){
      var h=HEX.charAt((code>>4)&15)+HEX.charAt(code&15);
      out+=ESC+ESC+DST.charAt(SRC.indexOf(h.charAt(0)))+DST.charAt(SRC.indexOf(h.charAt(1)));continue;
    }
    out+=c;
  }
  return PFX+out;
}
function dec(s){
  s=String(s==null?'':s);
  if(s.indexOf(PFX)!==0)return s;
  s=s.slice(PFX.length);var out='';
  for(var i=0;i<s.length;i++){
    var c=s.charAt(i);
    if(c===ESC){
      var n=s.charAt(i+1);
      if(n===ESC){
        var a=SRC.charAt(DST.indexOf(s.charAt(i+2))),b=SRC.charAt(DST.indexOf(s.charAt(i+3)));
        out+=String.fromCharCode(HEX.indexOf(a)*16+HEX.indexOf(b));i+=3;continue;
      }
      var k=E_DST.indexOf(n);out+=k>=0?E_SRC.charAt(k):n;i+=1;continue;
    }
    var j=DST.indexOf(c);out+=j>=0?SRC.charAt(j):c;
  }
  return out;
}
function isEnc(v){return typeof v==='string'&&v.indexOf(PFX)===0;}
W.mxEnc=enc;W.mxDec=dec;W.mxIsEnc=isEnc;

/* ---- which database fields get scrambled ---- */
var MEMBER_F=['password','email'],CODE_F=['email'];
var migrated={};
function recEnc(r,F){if(!r||typeof r!=='object')return r;F.forEach(function(f){if(typeof r[f]==='string')r[f]=enc(r[f]);});return r;}
function recDec(r,F,upPath,base){
  if(!r||typeof r!=='object')return r;
  var up=null;
  F.forEach(function(f){
    var v=r[f];if(typeof v!=='string')return;
    if(isEnc(v))r[f]=dec(v);
    else if(v&&upPath&&!migrated[upPath+'#'+f]){(up=up||{})[f]=enc(v);migrated[upPath+'#'+f]=1;}
  });
  if(up){try{fetch(base+upPath+'.json',{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify(up)})['catch'](function(){});}catch(e){}}
  return r;
}
function scalar(v,dir){return typeof v!=='string'?v:(dir==='enc'?enc(v):dec(v));}
function mapAll(obj,fn){if(obj&&typeof obj==='object')Object.keys(obj).forEach(function(k){fn(obj,k);});return obj;}

/* dir = 'enc' (going to Firebase) or 'dec' (coming back). Returns the converted data. */
W.mxCryptXform=function(base,path,data,dir){
  try{
    if(data==null)return data;
    var clean=String(path).split('?')[0].replace(/^\/+|\/+$/g,''),p=clean.split('/'),root=p[0];
    if(dir==='enc')data=JSON.parse(JSON.stringify(data));
    var E=dir==='enc';
    function R(r,F,up){return E?recEnc(r,F):recDec(r,F,up,base);}
    if(root==='members'){
      if(p.length===1)return mapAll(data,function(o,k){R(o[k],MEMBER_F,'/members/'+encodeURIComponent(k));});
      if(p.length===2)return R(data,MEMBER_F,'/members/'+p[1]);
      if(p.length===3&&MEMBER_F.indexOf(p[2])>=0)return scalar(data,dir);
    }else if(root==='authCodes'){
      if(p.length===1)return mapAll(data,function(o,k){R(o[k],CODE_F,null);});
      if(p.length===2)return R(data,CODE_F,null);
      if(p.length===3&&CODE_F.indexOf(p[2])>=0)return scalar(data,dir);
    }else if(root==='deletion_backups'){
      if(p.length===1)return mapAll(data,function(o,k){if(o[k]&&typeof o[k]==='object')R(o[k].member,MEMBER_F,null);});
      if(p.length===2){if(data&&typeof data==='object')R(data.member,MEMBER_F,null);return data;}
      if(p.length===3&&p[2]==='member')return R(data,MEMBER_F,null);
    }
  }catch(e){}
  return data;
};
})(window);
