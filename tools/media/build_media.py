#!/usr/bin/env python3
"""Copy/encode rendered masters into app/src/main/assets/media within a size budget,
make posters + a highlight reel, write manifest.json and inject the gallery list into js/matix-media.js."""
import json, os, re, shutil, subprocess, sys, time
V='./out'
A='../../app/src/main/assets'
M=A+'/media'
TARGET_MEDIA=int(float(os.environ.get('TARGET_MEDIA','170e6')))
ONLY=os.environ.get('ONLY','')  # comma list of ids to (re)encode

# id, category, title, sub, master, poster time, reel time
G=[
 ('mandelbrot','fractals','Mandelbrot deep zoom','Zoom 640 million times into z → z² + c — the edge never runs out of new shapes',V+'/math_mandelbrot.mp4',20.5,19.0),
 ('fourier','waves','Fourier epicycles','Any drawing is just circles spinning on circles',V+'/gallery/math_fourier.mp4',21.0,18.0),
 ('julia','fractals','Julia sets in motion','Slide c around the Mandelbrot set and the Julia set of z² + c melts and re-forms',V+'/math_julia.mp4',6.0,4.0),
 ('lorenz','chaos','The Lorenz attractor','Three simple equations for weather — and the butterfly of chaos appears',V+'/gallery/math_lorenz.mp4',20.0,16.0),
 ('phyllotaxis','geometry','The golden angle','Turn 137.5° between seeds and a sunflower appears — Fibonacci spirals for free',V+'/gallery/math_phyllotaxis.mp4',15.0,12.0),
 ('chaos_game','fractals','The chaos game','Jump halfway to a random corner, again and again — order appears out of randomness',V+'/gallery/math_chaos_game.mp4',19.0,15.0),
 ('double_pendulum','chaos','Chaos: the double pendulum','Six pendulums start 0.0001 rad apart — within seconds they disagree completely',V+'/gallery/math_double_pendulum.mp4',18.0,14.0),
 ('cardioid','geometry','Times tables on a circle','Join n to k·n (mod 200): k = 2 makes a cardioid, then it keeps morphing',V+'/gallery/math_cardioid.mp4',9.0,14.0),
 ('newton','fractals',"Newton's fractal","Colour every start point by the root of z³ − 1 that Newton's method lands on",V+'/math_newton.mp4',5.0,3.0),
 ('galton','numbers','The Galton board','Each ball flips left or right 14 times — together they draw the bell curve',V+'/gallery/math_galton.mp4',22.0,18.0),
 ('lissajous','waves','Lissajous table','x = sin(a·t), y = sin(b·t) — every pair of frequencies makes its own curve',V+'/gallery/math_lissajous.mp4',18.0,14.0),
 ('primes','numbers','The Ulam spiral','Write 1, 2, 3… in a square spiral and light up the primes — diagonals appear',V+'/gallery/math_primes.mp4',17.0,14.0),
 ('unit_circle','waves','Sine & cosine','Walk round the unit circle — your height draws the sine wave',V+'/gallery/math_unit_circle.mp4',12.0,9.0),
 ('life','chaos',"Conway's Game of Life",'Two rules — born with 3 neighbours, survive with 2 or 3 — and gliders start to fly',V+'/gallery/math_life.mp4',16.0,12.0),
 ('tesseract','geometry','A 4-D hypercube','A tesseract turning through the fourth dimension, projected down to your screen',V+'/gallery/math_tesseract.mp4',11.0,8.0),
 ('monte_carlo','numbers','Finding π with random darts','Fraction of darts inside the quarter circle × 4  →  π',V+'/gallery/math_monte_carlo.mp4',17.0,13.0),
 ('pythagoras','geometry','Pythagoras, proved by moving','Same big square, same 4 triangles — so the leftover areas must match',V+'/gallery/math_pythagoras.mp4',16.0,12.0),
 ('koch_hilbert','fractals','Fractal curves','Koch snowflake: infinite edge, finite area. Hilbert curve: a line that fills a square',V+'/gallery/math_koch_hilbert.mp4',19.0,8.0),
 ('cellular','chaos','One-line universes','Each row follows a tiny rule from the row above — Rule 30, Rule 90, Rule 110',V+'/gallery/math_cellular.mp4',7.0,12.5),
]
# films whose bitrate is tuned to fill the budget (weight = relative bitrate share)
ADJ={'mandelbrot':1.25,'julia':1.1,'cardioid':1.0,'newton':0.9,'phyllotaxis':1.0}
CAP_KBPS=2600   # any other film above this is re-encoded down to it
MAX_KBPS=9000

def sh(cmd):
    r=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True)
    if r.returncode: print(r.stderr[-2000:]); raise SystemExit('cmd failed: '+' '.join(cmd[:8]))
    return r.stdout
def probe(f):
    j=json.loads(sh(['ffprobe','-v','error','-print_format','json','-show_format','-show_streams',f]))
    s=[x for x in j['streams'] if x['codec_type']=='video'][0]
    return float(j['format']['duration']),int(j['format'].get('bit_rate',0)),s['width'],s['height']
X264=['-c:v','libx264','-profile:v','high','-level','4.1','-pix_fmt','yuv420p','-g','60',
      '-colorspace','bt709','-color_primaries','bt709','-color_trc','bt709','-an','-movflags','+faststart']
def enc2(src,dst,kbps,vf=None):
    log='/tmp/x264_'+os.path.basename(dst)
    base=['ffmpeg','-y','-v','error','-i',src]+(['-vf',vf] if vf else [])
    sh(base+X264+['-preset','slow','-b:v',f'{kbps}k','-pass','1','-passlogfile',log,'-f','mp4','/dev/null'])
    sh(base+X264+['-preset','slow','-b:v',f'{kbps}k','-maxrate',f'{int(kbps*1.6)}k','-bufsize',f'{int(kbps*3)}k','-pass','2','-passlogfile',log,dst])
    for e in ('-0.log','-0.log.mbtree'):
        try: os.remove(log+e)
        except OSError: pass
def copy(src,dst):
    shutil.copyfile(src,dst)

os.makedirs(M,exist_ok=True)
for d in ('brand','hero','fx','gallery','posters'): os.makedirs(f'{M}/{d}',exist_ok=True)
t0=time.time()
fixed=0; files={}
def note(path):
    global fixed
    fixed+=os.path.getsize(path)
# ---- brand / hero / fx (copied as rendered) ----
for o in ('portrait','landscape'):
    for n in ('intro','welcome'):
        copy(f'{V}/{n}_{o}.mp4',f'{M}/brand/{n}_{o}.mp4'); note(f'{M}/brand/{n}_{o}.mp4')
    for n in ('confetti','stars','rings','badge'):
        copy(f'{V}/fx/fx_{n}_{o}.mp4',f'{M}/fx/fx_{n}_{o}.mp4'); note(f'{M}/fx/fx_{n}_{o}.mp4')
HERO=sorted(f[5:-4] for f in os.listdir(V+'/hero') if f.startswith('hero_') and f.endswith('.mp4'))
for h in HERO:
    p=f'{V}/hero/hero_{h}.mp4'
    if os.path.getsize(p)<10000: raise SystemExit('broken hero '+h)
    copy(p,f'{M}/hero/hero_{h}.mp4'); note(f'{M}/hero/hero_{h}.mp4')
# ---- posters ----
for gid,cat,title,sub,src,pt,rt in G:
    dst=f'{M}/posters/{gid}.jpg'
    sh(['ffmpeg','-y','-v','error','-ss',str(pt),'-i',src,'-frames:v','1','-vf','scale=960:540:flags=lanczos','-q:v','4',dst]); note(dst)
# ---- highlight reel ----
reel=f'{M}/gallery/reel.mp4'
if not os.path.exists(reel) or 'reel' in ONLY.split(',') or os.environ.get('REEL')=='1':
    cmd=['ffmpeg','-y','-v','error']; fc=[]; CL=2.4
    for i,(gid,cat,title,sub,src,pt,rt) in enumerate(G):
        cmd+=['-ss',str(rt),'-t',str(CL),'-i',src]
        fc.append(f'[{i}:v]scale=960:540:force_original_aspect_ratio=decrease:flags=lanczos,pad=960:540:(ow-iw)/2:(oh-ih)/2,setsar=1,fps=30,format=yuv420p,'
                  f'fade=t=in:st=0:d=0.3,fade=t=out:st={CL-0.3}:d=0.3[v{i}]')
    fc.append(''.join(f'[v{i}]' for i in range(len(G)))+f'concat=n={len(G)}:v=1:a=0[out]')
    cmd+=['-filter_complex',';'.join(fc),'-map','[out]']+X264+['-preset','slow','-crf','23','-maxrate','3000k','-bufsize','6000k',reel]
    sh(cmd)
note(reel)
sh(['ffmpeg','-y','-v','error','-ss','19.6','-i',V+'/math_mandelbrot.mp4','-frames:v','1','-vf','scale=1280:720:flags=lanczos','-q:v','4',f'{M}/posters/reel.jpg']); note(f'{M}/posters/reel.jpg')
# ---- gallery films ----
info={}
for gid,cat,title,sub,src,pt,rt in G:
    d,br,w,h=probe(src); info[gid]=(d,br,w,h)
    if gid in ADJ: continue
    dst=f'{M}/gallery/{gid}.mp4'
    if br/1000>CAP_KBPS*1.08:
        if not os.path.exists(dst) or (ONLY and gid in ONLY.split(',')) or not ONLY: enc2(src,dst,CAP_KBPS)
    else: copy(src,dst)
    note(dst)
pool=TARGET_MEDIA-fixed
wsum=sum(info[g][0]*ADJ[g] for g in ADJ)
kb=pool*8/1000/wsum*0.985   # ~1.5% mp4 overhead
print(f'fixed {fixed/1e6:.1f} MB, pool {pool/1e6:.1f} MB, base {kb:.0f} kbps')
for g in ADJ:
    d,br,w,h=info[g]; src=[x for x in G if x[0]==g][0][4]
    k=int(min(MAX_KBPS,max(1500,kb*ADJ[g]),br/1000))
    dst=f'{M}/gallery/{g}.mp4'
    if ONLY and g not in ONLY.split(',') and os.path.exists(dst): print('keep',g); continue
    print(f'  {g}: {k} kbps ({d:.0f}s {w}x{h}, master {br/1e6:.1f} Mbps)',flush=True)
    if k>=br/1000*0.98: copy(src,dst)
    else: enc2(src,dst,k)
# ---- manifest ----
gal=[];
for gid,cat,title,sub,src,pt,rt in G:
    f=f'{M}/gallery/{gid}.mp4'; d,br,w,h=probe(f)
    gal.append({'id':gid,'cat':cat,'title':title,'sub':sub,'file':f'media/gallery/{gid}.mp4','poster':f'media/posters/{gid}.jpg',
                'dur':round(d,1),'w':w,'h':h,'bytes':os.path.getsize(f)})
rd=probe(reel)[0]
REEL={'file':'media/gallery/reel.mp4','poster':'media/posters/reel.jpg','dur':round(rd,1),'bytes':os.path.getsize(reel)}
tot=0
for root,_,fs in os.walk(M):
    for f in fs: tot+=os.path.getsize(os.path.join(root,f))
man={'name':'Matix media','version':'1.1.0','generated':time.strftime('%Y-%m-%d'),
     'note':'Every film is generated procedurally from math (Python + pycairo + C, encoded with x264). Muted H.264 MP4.',
     'gallery':gal,'reel':REEL,
     'hero':{h:f'media/hero/hero_{h}.mp4' for h in HERO},
     'fx':{n:{o:f'media/fx/fx_{n}_{o}.mp4' for o in ('portrait','landscape')} for n in ('confetti','stars','rings','badge')},
     'brand':{n:{o:f'media/brand/{n}_{o}.mp4' for o in ('portrait','landscape')} for n in ('intro','welcome')},
     'total_bytes':tot}
json.dump(man,open(M+'/manifest.json','w'),ensure_ascii=False,indent=1)
js=A+'/js/matix-media.js'; s=open(js,encoding='utf-8').read()
slim=[{k:v for k,v in g.items() if k!='bytes'} for g in gal]
block='/* @@GALLERY_BEGIN@@ (generated by tools/media/build_media.py) */\nvar GALLERY='+json.dumps(slim,ensure_ascii=False)+';\nvar REEL='+json.dumps({k:REEL[k] for k in ('file','poster','dur')})+';\n/* @@GALLERY_END@@ */'
s=re.sub(r'/\* @@GALLERY_BEGIN@@.*?@@GALLERY_END@@ \*/',lambda m:block,s,flags=re.S)
open(js,'w',encoding='utf-8').write(s)
print(f'media total {tot/1e6:.1f} MB in {time.time()-t0:.0f}s')
