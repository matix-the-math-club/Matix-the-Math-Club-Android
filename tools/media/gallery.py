"""Math Motion gallery framework: Scene classes drawn with cairo (or numpy) over the brand dot-grid."""
import sys, math, random, cairo, time, os
import numpy as np
sys.path.insert(0,'.')
from vkit import *
OUT='./out/gallery/'; os.makedirs(OUT,exist_ok=True)
PREV='./prev/'

class Scene:
    name='scene'; dur=20.0; title=''; sub=''; crf=17; fps=30; bg_alpha=0.075; loop=False
    def setup(self,w,h): pass
    def draw(self,c,t): pass

def caption(c,w,h,title,sub,t,dur):
    a=smooth((t-0.4)/0.8)*(1-smooth((t-5.5)/1.0))
    if a<=0: return
    s=h/1080; x=70*s; y=h-120*s
    c.set_source_rgba(0,0,0,0.35*a); 
    text(c,title,x,y,44*s,(1,1,1),a,align='left')
    text(c,sub,x,y+52*s,24*s,(0.85,0.85,0.88),0.85*a,bold=False,align='left')
    c.set_source_rgba(*PAL['green'],a); c.rectangle(x,y-48*s,56*s*smooth((t-0.4)/0.8),5*s); c.fill()

def surface_from_array(arr):
    """arr HxWx4 uint8 premultiplied BGRA (C-contiguous)"""
    h,w=arr.shape[:2]
    return cairo.ImageSurface.create_for_data(arr,cairo.FORMAT_ARGB32,w,h,w*4)

def frame(scene,c,w,h,t,bg,vig):
    c.set_source_surface(bg); c.paint()
    c.save(); scene.draw(c,t); c.restore()
    c.set_source_surface(vig); c.paint()
    caption(c,w,h,scene.title,scene.sub,t,scene.dur)
    if not scene.loop:
        fi=1-smooth(t/0.5); fo=smooth((t-(scene.dur-0.6))/0.6); k=max(fi,fo)
        if k>0: c.set_source_rgba(*PAL['bg'],k); c.paint()

def render(scene,w=1920,h=1080,crf=None,fps=None):
    fps=fps or scene.fps
    path=OUT+'math_'+scene.name+'.mp4'
    v=Video(path,w,h,fps,crf=crf or scene.crf)
    bg=dot_grid(w,h,alpha=scene.bg_alpha); vig=vignette(w,h,0.5)
    scene.setup(w,h); N=int(round(scene.dur*fps)); t0=time.time()
    for f in range(N):
        frame(scene,v.ctx,w,h,f/fps,bg,vig); v.push()
    sz=v.close(); print(scene.name,sz,f'{time.time()-t0:.0f}s',flush=True); return sz

def preview(scene,times,w=1920,h=1080,scale=0.5):
    s=cairo.ImageSurface(cairo.FORMAT_ARGB32,w,h); c=cairo.Context(s)
    bg=dot_grid(w,h,alpha=scene.bg_alpha); vig=vignette(w,h,0.5); scene.setup(w,h)
    files=[]
    if getattr(scene,'sequential',False):
        # scenes with state must be stepped through every frame
        fps=scene.fps; want=set(int(round(t*fps)) for t in times)
        for f in range(int(max(times)*fps)+1):
            t=f/fps
            if f in want or getattr(scene,'needs_every_frame',False):
                frame(scene,c,w,h,t,bg,vig)
            if f in want:
                p=f'{PREV}{scene.name}_{f}.png'; s.write_to_png(p); files.append(p)
    else:
        for t in times:
            frame(scene,c,w,h,t,bg,vig); p=f'{PREV}{scene.name}_{int(t*100)}.png'; s.write_to_png(p); files.append(p)
    os.system(f"magick montage {' '.join(files)} -tile 3x -geometry {int(w*scale/2)}x{int(h*scale/2)}+2+2 {PREV}mont_{scene.name}.png")
    return f'{PREV}mont_{scene.name}.png'
