"""Tiny motion-graphics kit: pycairo frames piped into ffmpeg (H.264, yuv420p, faststart)."""
import cairo, math, random, subprocess, os, numpy as np
TAU = math.tau
PAL = dict(bg=(0.043,0.043,0.047), green=(0.243,0.914,0.561), pink=(1.0,0.31,0.639), orange=(0.984,0.573,0.235),
           blue=(0.22,0.741,0.973), purple=(0.545,0.361,0.965), olive=(0.788,0.827,0.337), gold=(0.969,0.651,0.0),
           cream=(0.957,0.941,0.906), white=(1,1,1), brand=(0.357,0.275,0.788), red=(0.898,0.392,0.345))
CLUB = [PAL[k] for k in ('pink','purple','blue','orange','green','olive')]
def hexrgb(h): h=h.lstrip('#'); return tuple(int(h[i:i+2],16)/255 for i in (0,2,4))
def lerp(a,b,t): return a+(b-a)*t
def mix(c1,c2,t): return tuple(lerp(a,b,t) for a,b in zip(c1,c2))
def clamp(x,a=0.0,b=1.0): return a if x<a else b if x>b else x
def ease_out(t): t=clamp(t); return 1-(1-t)**3
def ease_in_out(t): t=clamp(t); return 4*t*t*t if t<.5 else 1-(-2*t+2)**3/2
def ease_back(t,s=1.70158): t=clamp(t); t-=1; return t*t*((s+1)*t+s)+1
def smooth(t): t=clamp(t); return t*t*(3-2*t)
def hsv(h,s,v):
    import colorsys; return colorsys.hsv_to_rgb(h%1.0,s,v)

class Video:
    def __init__(self, path, w, h, fps=30, crf=18, preset='medium', maxrate=None, gop=None, audio=None, tune=None, level=None):
        self.w,self.h,self.fps,self.path=w,h,fps,path
        self.surface=cairo.ImageSurface(cairo.FORMAT_ARGB32,w,h)
        assert self.surface.get_stride()==w*4
        self.ctx=cairo.Context(self.surface)
        lvl = level or ('5.1' if w*h>1920*1088 or fps>30 else '4.1')
        cmd=['ffmpeg','-y','-loglevel','error','-f','rawvideo','-pix_fmt','bgra','-s',f'{w}x{h}','-r',str(fps),'-i','-']
        if audio: cmd+=['-i',audio]
        cmd+=['-vf','scale=out_color_matrix=bt709:out_range=tv,format=yuv420p',
              '-c:v','libx264','-preset',preset,'-crf',str(crf),'-profile:v','high','-level',lvl,
              '-colorspace','bt709','-color_primaries','bt709','-color_trc','bt709',
              '-g',str(gop or fps*2),'-threads','2']
        if tune: cmd+=['-tune',tune]
        if maxrate: cmd+=['-maxrate',maxrate,'-bufsize',str(int(maxrate.rstrip('M'))*2)+'M']
        if audio: cmd+=['-c:a','aac','-b:a','192k','-shortest']
        else: cmd+=['-an']
        cmd+=['-movflags','+faststart',path]
        self.proc=subprocess.Popen(cmd,stdin=subprocess.PIPE)
        self.n=0
    def push(self):
        self.surface.flush()
        self.proc.stdin.write(self.surface.get_data())
        self.n+=1
    def push_array(self, arr):
        """arr: HxWx3 uint8 RGB or HxWx4 BGRA"""
        if arr.shape[2]==3:
            bgra=np.empty((self.h,self.w,4),np.uint8); bgra[...,0]=arr[...,2]; bgra[...,1]=arr[...,1]; bgra[...,2]=arr[...,0]; bgra[...,3]=255
            arr=bgra
        self.proc.stdin.write(np.ascontiguousarray(arr).tobytes()); self.n+=1
    def close(self):
        self.proc.stdin.close(); self.proc.wait()
        return os.path.getsize(self.path)

def surface_array(surface):
    """numpy view (H,W,4) BGRA of a cairo surface"""
    surface.flush()
    return np.ndarray((surface.get_height(),surface.get_width(),4),np.uint8,buffer=surface.get_data())

def dot_grid(w,h,step=22,alpha=0.075,bg=PAL['bg'],r=1.0,color=(1,1,1)):
    s=cairo.ImageSurface(cairo.FORMAT_ARGB32,w,h); c=cairo.Context(s)
    c.set_source_rgb(*bg); c.paint()
    c.set_source_rgba(*color,alpha)
    for y in range(step//2,h,step):
        for x in range(step//2,w,step):
            c.arc(x,y,r,0,TAU); c.fill()
    return s

def vignette(w,h,strength=0.55):
    s=cairo.ImageSurface(cairo.FORMAT_ARGB32,w,h); c=cairo.Context(s)
    g=cairo.RadialGradient(w/2,h/2,min(w,h)*0.25,w/2,h/2,math.hypot(w,h)*0.6)
    g.add_color_stop_rgba(0,0,0,0,0); g.add_color_stop_rgba(1,0,0,0,strength)
    c.set_source(g); c.paint(); return s

def glow_dot(c,x,y,r,col,a=1.0,halo=4.0):
    g=cairo.RadialGradient(x,y,0,x,y,r*halo)
    g.add_color_stop_rgba(0,*col,0.55*a); g.add_color_stop_rgba(0.25,*col,0.18*a); g.add_color_stop_rgba(1,*col,0)
    c.set_source(g); c.arc(x,y,r*halo,0,TAU); c.fill()
    c.set_source_rgba(*mix(col,(1,1,1),0.35),a); c.arc(x,y,r,0,TAU); c.fill()

def glow_stroke(c,col,width,a=1.0,passes=((6,0.06),(3,0.14),(1.6,0.35),(1,1.0))):
    """stroke the current path several times for a neon glow"""
    path=c.copy_path()
    c.set_line_cap(cairo.LINE_CAP_ROUND); c.set_line_join(cairo.LINE_JOIN_ROUND)
    for k,(m,al) in enumerate(passes):
        c.new_path(); c.append_path(path)
        cc = mix(col,(1,1,1),0.45) if k==len(passes)-1 else col
        c.set_source_rgba(*cc,al*a); c.set_line_width(width*m); c.stroke()
    c.new_path()

_CMAPS={}
def _cmap(fam):
    if fam not in _CMAPS:
        import subprocess
        from fontTools.ttLib import TTFont
        f=subprocess.run(['fc-match','-f','%{file}',fam+':style=Bold'],capture_output=True,text=True).stdout
        try: _CMAPS[fam]=set(TTFont(f,fontNumber=0).getBestCmap())
        except Exception: _CMAPS[fam]=set()
    return _CMAPS[fam]
FALLBACKS=['Arial','Noto Sans Arabic','Cambria','Liberation Sans']
def _runs(s,font):
    """split s into (font,text) runs so glyphs missing from the main font come from a fallback"""
    out=[]
    for ch in s:
        f=font
        if ord(ch)>127 and ord(ch) not in _cmap(font):
            for fb in FALLBACKS:
                if ord(ch) in _cmap(fb): f=fb; break
        if out and out[-1][0]==f: out[-1][1]+=ch
        else: out.append([f,ch])
    return out
def text(c,s,x,y,size,col=(1,1,1),a=1.0,font='Noto Sans',bold=True,align='center',italic=False):
    sl=cairo.FONT_SLANT_ITALIC if italic else cairo.FONT_SLANT_NORMAL; wt=cairo.FONT_WEIGHT_BOLD if bold else cairo.FONT_WEIGHT_NORMAL
    runs=_runs(s,font); c.set_font_size(size)
    # overall metrics: width from advances, vertical centre from the main font's caps
    tw=0; widths=[]
    for f,r in runs:
        c.select_font_face(f,sl,wt); c.set_font_size(size); xa=c.text_extents(r)[4]; widths.append(xa); tw+=xa
    c.select_font_face(font,sl,wt); c.set_font_size(size)
    xb,yb,_,th,_,_=c.text_extents(s if len(runs)==1 else 'Hx')
    if len(runs)==1:
        xb,yb,tw0,th,xa,ya=c.text_extents(s)
    if align=='center': x0=x-tw/2
    elif align=='right': x0=x-tw
    else: x0=x
    yy=y-yb-th/2
    c.set_source_rgba(*col,a)
    for (f,r),wd in zip(runs,widths):
        c.select_font_face(f,sl,wt); c.set_font_size(size); c.move_to(x0,yy); c.show_text(r); x0+=wd
    c.new_path()
    return tw

def text_glow(c,s,x,y,size,col,a=1.0,font='Noto Sans',bold=True,align='center'):
    for k,(dx,al) in enumerate(((6,0.05),(3,0.09))):
        for ang in range(0,360,60):
            text(c,s,x+dx*math.cos(math.radians(ang)),y+dx*math.sin(math.radians(ang)),size,col,al*a,font,bold,align)
    text(c,s,x,y,size,mix(col,(1,1,1),0.5),a,font,bold,align)

# ---- tiny SVG path parser (M L H V C Q Z, absolute + relative) for brand marks ----
import re
def svg_path(c,d,sx=1,sy=1,ox=0,oy=0):
    toks=re.findall(r'[MLHVCQZmlhvcqz]|-?[0-9]*\.?[0-9]+(?:e-?\d+)?',d)
    i=0; cmd=None; x=y=0; start=(0,0)
    def num():
        nonlocal i; v=float(toks[i]); i+=1; return v
    P=lambda px,py:(ox+px*sx,oy+py*sy)
    while i<len(toks):
        t=toks[i]
        if re.match(r'[A-Za-z]',t): cmd=t; i+=1
        if cmd in 'Zz': c.close_path(); x,y=start; continue
        if cmd in 'Mm':
            nx,ny=num(),num()
            if cmd=='m': nx+=x; ny+=y
            x,y=nx,ny; start=(x,y); c.move_to(*P(x,y)); cmd='L' if cmd=='M' else 'l'
        elif cmd in 'Ll':
            nx,ny=num(),num()
            if cmd=='l': nx+=x; ny+=y
            x,y=nx,ny; c.line_to(*P(x,y))
        elif cmd in 'Hh':
            nx=num(); x = x+nx if cmd=='h' else nx; c.line_to(*P(x,y))
        elif cmd in 'Vv':
            ny=num(); y = y+ny if cmd=='v' else ny; c.line_to(*P(x,y))
        elif cmd in 'Cc':
            a=[num() for _ in range(6)]
            if cmd=='c': a=[a[0]+x,a[1]+y,a[2]+x,a[3]+y,a[4]+x,a[5]+y]
            c.curve_to(*P(a[0],a[1]),*P(a[2],a[3]),*P(a[4],a[5])); x,y=a[4],a[5]
        elif cmd in 'Qq':
            a=[num() for _ in range(4)]
            if cmd=='q': a=[a[0]+x,a[1]+y,a[2]+x,a[3]+y]
            x0,y0=x,y; qx,qy,ex,ey=a
            c.curve_to(*P(x0+2/3*(qx-x0),y0+2/3*(qy-y0)),*P(ex+2/3*(qx-ex),ey+2/3*(qy-ey)),*P(ex,ey)); x,y=ex,ey

MATIX_PARTS = [  # from matix-logo.svg (viewBox -8 -12 345 94)
    ('path','M0 70V0h13l19 43 19-43h13v70H51V26L37 58H27L13 26v44z','ink'),
    ('path','M72 70 97 0h12l25 70h-14l-5.4-15H91.4L86 70zM95.3 44h15.4L103 22.4z','ink'),
    ('path','M142 0h62v13h-24.5v57h-13V13H142z','ink'),
    ('rect',(217,28,14,40,4),'ink'),
    ('times',(217,6,231,20),'pink'),
    ('slash',(275.3,64.1,304.7,5.9),'ink'),
    ('circle',(265,12,12),'pink'),
    ('circle',(315,58,12),'orange'),
]
def draw_matix(c,cx,cy,width,ink=(1,1,1),reveal=None,alpha=1.0):
    """draw the MATIX wordmark centred at cx,cy. reveal(i)->(progress 0..1) per part for staggered entrance"""
    s=width/345.0; ox=cx-(345/2-8)*s; oy=cy-(94/2-12)*s
    cols=dict(ink=ink,pink=PAL['pink'],orange=PAL['orange'])
    for i,(kind,data,col) in enumerate(MATIX_PARTS):
        p=1.0 if reveal is None else clamp(reveal(i))
        if p<=0: continue
        e=ease_back(p); a=alpha*clamp(p*1.6)
        # spring each part up from below
        c.save(); c.translate(0,(1-e)*40*s)
        colr=cols[col]
        if kind=='path':
            c.new_path(); svg_path(c,data,s,s,ox,oy); c.set_fill_rule(cairo.FILL_RULE_EVEN_ODD); c.set_source_rgba(*colr,a); c.fill()
        elif kind=='rect':
            x,y,w,h,r=data; x=ox+x*s; y=oy+y*s; w*=s; h*=s; r*=s
            c.new_path(); c.arc(x+w-r,y+r,r,-TAU/4,0); c.arc(x+w-r,y+h-r,r,0,TAU/4); c.arc(x+r,y+h-r,r,TAU/4,TAU/2); c.arc(x+r,y+r,r,TAU/2,3*TAU/4); c.close_path()
            c.set_source_rgba(*colr,a); c.fill()
        elif kind=='times':
            x1,y1,x2,y2=data; c.set_line_cap(cairo.LINE_CAP_ROUND); c.set_line_width(7*s); c.set_source_rgba(*colr,a)
            c.move_to(ox+x1*s,oy+y1*s); c.line_to(ox+x2*s,oy+y2*s); c.move_to(ox+x2*s,oy+y1*s); c.line_to(ox+x1*s,oy+y2*s); c.stroke()
        elif kind=='slash':
            x1,y1,x2,y2=data; c.set_line_cap(cairo.LINE_CAP_ROUND); c.set_line_width(11*s); c.set_source_rgba(*colr,a)
            c.move_to(ox+x1*s,oy+y1*s); c.line_to(ox+x2*s,oy+y2*s); c.stroke()
        elif kind=='circle':
            x,y,r=data; c.set_source_rgba(*colr,a); c.arc(ox+x*s,oy+y*s,r*s,0,TAU); c.fill()
        c.restore()
