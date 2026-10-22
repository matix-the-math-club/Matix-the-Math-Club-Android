"""Lesson kit: kinetic-typography helpers for the Matix animated lessons (pycairo -> ffmpeg)."""
import cairo, math, random, subprocess, os
from vkit import PAL, mix, clamp, ease_out, ease_back, ease_in_out, smooth, _runs, TAU
W,H,FPS=1920,1080,30
FONT='Noto Sans'
C=dict(pink=PAL['pink'],blue=PAL['blue'],green=PAL['green'],orange=(0.976,0.451,0.086),purple=PAL['purple'],
       gold=(0.98,0.75,0.14),white=(1,1,1),dim=(0.72,0.74,0.82),red=(1.0,0.36,0.36),bg=(0.055,0.058,0.10),card=(0.11,0.115,0.17))
HERE=os.path.dirname(os.path.abspath(__file__))

# ---------------------------------------------------------------- text
def _face(c,f,bold=True): c.select_font_face(f,cairo.FONT_SLANT_NORMAL,cairo.FONT_WEIGHT_BOLD if bold else cairo.FONT_WEIGHT_NORMAL)
def str_w(c,s,size,bold=True):
    w=0
    for f,r in _runs(s,FONT):
        _face(c,f,bold); c.set_font_size(size); w+=c.text_extents(r)[4]
    return w
def str_draw(c,s,x,cy,size,col,a=1.0,bold=True):
    """draw s with its left edge at x, vertically centred on cy"""
    yb=cy+size*0.36
    for f,r in _runs(s,FONT):
        _face(c,f,bold); c.set_font_size(size)
        c.move_to(x,yb); c.set_source_rgba(*col,a); c.show_text(r); x+=c.text_extents(r)[4]
    return x
def seg_w(c,sg,size):
    k=sg[0]
    if k=='t': return str_w(c,sg[1],size,sg[3])
    if k=='f': fs=size*0.66; return max(str_w(c,sg[1],fs),str_w(c,sg[2],fs))+size*0.18
    if k=='g': return sg[1]*size
    return 0
def seg_draw(c,sg,x,cy,size,a):
    k=sg[0]
    if k=='t': str_draw(c,sg[1],x,cy,size,sg[2],a,sg[3])
    elif k=='f':
        fs=size*0.66; w=seg_w(c,sg,size); col=sg[3]
        nw=str_w(c,sg[1],fs); dw=str_w(c,sg[2],fs)
        str_draw(c,sg[1],x+(w-nw)/2,cy-size*0.40,fs,col,a)
        str_draw(c,sg[2],x+(w-dw)/2,cy+size*0.44,fs,col,a)
        c.set_source_rgba(*col,a); c.rectangle(x+size*0.04,cy+size*0.02-size*0.035,w-size*0.08,size*0.07); c.fill()
def norm(segs):
    """segments: 'text' | ('text',col) | ('text',col,False->regular) | F(n,d,col) | G(gap)"""
    out=[]
    for s in segs:
        if isinstance(s,str): out.append(('t',s,C['white'],True))
        elif s[0] in ('f','g'): out.append(s)
        elif len(s)==2: out.append(('t',s[0],s[1],True))
        else: out.append(('t',s[0],s[1],s[2]))
    return out
def F(n,d,col=None): return ('f',str(n),str(d),col or C['white'])
def G(g=0.3): return ('g',g)
def win(t,t0,t1=None,fi=0.45,fo=0.4):
    """0..1 visibility envelope"""
    p=clamp((t-t0)/fi)
    if t1 is not None: p*=1-clamp((t-t1)/fo)
    return p
def line(c,segs,cx,cy,size,t,t0,t1=None,stagger=0.07,style='up',align='center',fi=0.5):
    """kinetic text: every segment flies in after the previous one"""
    segs=norm(segs); ws=[seg_w(c,s,size) for s in segs]; tw=sum(ws)
    x=cx-tw/2 if align=='center' else (cx if align=='left' else cx-tw)
    out=1.0 if t1 is None else 1-clamp((t-t1)/0.4)
    if out<=0 or t<t0: return (x,tw)
    for i,(s,w) in enumerate(zip(segs,ws)):
        p=clamp((t-t0-i*stagger)/fi)
        if p>0:
            a=ease_out(p)*out
            if style=='up':
                seg_draw(c,s,x,cy+(1-ease_out(p))*size*0.55,size,a)
            elif style=='pop':
                k=ease_back(p); c.save(); c.translate(x+w/2,cy); c.scale(max(k,0.01),max(k,0.01)); seg_draw(c,s,-w/2,0,size,a); c.restore()
            else: seg_draw(c,s,x,cy,size,a)
        x+=w
    return (cx-tw/2 if align=='center' else cx, tw)
def text(c,s,cx,cy,size,col,t,t0,t1=None,style='up',align='center',bold=True):
    return line(c,[(s,col,bold)],cx,cy,size,t,t0,t1,0,style,align)

# ---------------------------------------------------------------- shapes
def rrect(c,x,y,w,h,r):
    c.new_sub_path(); c.arc(x+w-r,y+r,r,-TAU/4,0); c.arc(x+w-r,y+h-r,r,0,TAU/4); c.arc(x+r,y+h-r,r,TAU/4,TAU/2); c.arc(x+r,y+r,r,TAU/2,TAU*3/4); c.close_path()
def card(c,x,y,w,h,t,t0,t1=None,col=None,a=0.92,r=34,edge=None):
    p=win(t,t0,t1)
    if p<=0: return 0
    k=0.94+0.06*ease_out(p)
    c.save(); c.translate(x+w/2,y+h/2); c.scale(k,k); c.translate(-w/2,-h/2)
    rrect(c,0,0,w,h,r); c.set_source_rgba(*(col or C['card']),a*p); c.fill_preserve()
    c.set_source_rgba(*(edge or (1,1,1)),0.10*p); c.set_line_width(2); c.stroke(); c.restore()
    return p
def pie(c,cx,cy,r,n,t,t0,fills=(),t1=None,col=C['pink'],lw=6):
    """circle cut into n equal slices; fills = [(slice, start_time, colour)]"""
    p=win(t,t0,t1,0.8)
    if p<=0: return
    a0=-TAU/4
    for i,ts,fc in fills:
        q=clamp((t-ts)/0.45)
        if q>0:
            c.move_to(cx,cy); c.arc(cx,cy,r*(0.9+0.1*ease_back(q)),a0+TAU*i/n,a0+TAU*(i+1)/n); c.close_path()
            c.set_source_rgba(*(fc or col),0.95*ease_out(q)*p); c.fill()
    c.set_line_width(lw); c.set_source_rgba(1,1,1,0.9*p)
    c.new_path(); c.arc(cx,cy,r,a0,a0+TAU*ease_in_out(clamp((t-t0)/0.8))); c.stroke()
    for i in range(n):
        q=clamp((t-t0-0.5-i*0.08)/0.35)
        if q>0:
            an=a0+TAU*i/n; c.move_to(cx,cy); c.line_to(cx+r*q*math.cos(an),cy+r*q*math.sin(an)); c.stroke()
def bar(c,x,y,w,h,n,t,t0,fills=(),t1=None,col=C['blue'],lw=5,label=None):
    p=win(t,t0,t1,0.6)
    if p<=0: return
    cw=w/n
    for i,ts,fc in fills:
        q=clamp((t-ts)/0.4)
        if q>0:
            c.rectangle(x+i*cw,y+h*(1-ease_out(q)),cw,h*ease_out(q)); c.set_source_rgba(*(fc or col),0.92*p); c.fill()
    c.set_line_width(lw); c.set_source_rgba(1,1,1,0.9*p)
    c.rectangle(x,y,w*ease_in_out(clamp((t-t0)/0.6)),h); c.stroke()
    for i in range(1,n):
        q=clamp((t-t0-0.4-i*0.05)/0.3)
        if q>0: c.move_to(x+i*cw,y); c.line_to(x+i*cw,y+h*q); c.stroke()
def grid(c,x,y,cell,cols,rows,t,t0,k_fn=None,t1=None,col=C['green'],gap=4,order='row'):
    p=win(t,t0,t1,0.6)
    if p<=0: return
    k=k_fn(t) if k_fn else 0
    for j in range(rows):
        for i in range(cols):
            idx=j*cols+i if order=='row' else i*rows+j
            q=clamp((t-t0-0.008*idx)/0.3)
            if q<=0: continue
            filled=idx<k
            rrect(c,x+i*cell+gap/2,y+j*cell+gap/2,cell-gap,cell-gap,min(8,cell*0.18))
            if filled: c.set_source_rgba(*col,0.95*p*q)
            else: c.set_source_rgba(1,1,1,0.10*p*q)
            c.fill()
def arrow(c,x0,y0,x1,y1,t,t0,t1=None,col=C['gold'],lw=6,bend=0.0,head=22):
    p=win(t,t0,t1,0.01)
    q=ease_in_out(clamp((t-t0)/0.6))
    if p<=0 or q<=0: return
    mx,my=(x0+x1)/2-(y1-y0)*bend,(y0+y1)/2+(x1-x0)*bend
    pts=[]
    for i in range(41):
        u=q*i/40; pts.append(((1-u)**2*x0+2*(1-u)*u*mx+u*u*x1,(1-u)**2*y0+2*(1-u)*u*my+u*u*y1))
    c.set_line_width(lw); c.set_line_cap(cairo.LINE_CAP_ROUND); c.set_source_rgba(*col,p)
    c.move_to(*pts[0])
    for pt in pts[1:]: c.line_to(*pt)
    c.stroke()
    (ax,ay),(bx,by)=pts[-2],pts[-1]; an=math.atan2(by-ay,bx-ax)
    c.move_to(bx,by); c.line_to(bx-head*math.cos(an-0.45),by-head*math.sin(an-0.45))
    c.move_to(bx,by); c.line_to(bx-head*math.cos(an+0.45),by-head*math.sin(an+0.45)); c.stroke()
def underline(c,x,y,w,t,t0,t1=None,col=C['gold'],lw=8):
    q=ease_in_out(clamp((t-t0)/0.4))*(1 if t1 is None else 1-clamp((t-t1)/0.4))
    if q<=0: return
    c.set_line_width(lw); c.set_line_cap(cairo.LINE_CAP_ROUND); c.set_source_rgba(*col,0.95)
    c.move_to(x,y); c.curve_to(x+w*0.3,y+6,x+w*0.6,y-4,x+w*q,y+2); c.stroke()
def tick(c,cx,cy,s,t,t0,t1=None,col=C['green']):
    q=clamp((t-t0)/0.4)
    o=1 if t1 is None else 1-clamp((t-t1)/0.4)
    if q<=0 or o<=0: return
    q=q*o if o<1 else q
    c.save(); c.translate(cx,cy); k=ease_back(q); c.scale(k,k)
    c.arc(0,0,s,0,TAU); c.set_source_rgba(*col,0.95); c.fill()
    c.set_source_rgb(0.05,0.06,0.1); c.set_line_width(s*0.22); c.set_line_cap(cairo.LINE_CAP_ROUND)
    c.move_to(-s*0.45,0); c.line_to(-s*0.1,s*0.35); c.line_to(s*0.5,-s*0.35); c.stroke(); c.restore()
def cross(c,cx,cy,s,t,t0,t1=None,col=C['red']):
    q=win(t,t0,t1,0.3)
    if q<=0: return
    c.set_source_rgba(*col,q); c.set_line_width(s*0.22); c.set_line_cap(cairo.LINE_CAP_ROUND)
    c.move_to(cx-s,cy-s); c.line_to(cx+s,cy+s); c.move_to(cx+s,cy-s); c.line_to(cx-s,cy+s); c.stroke()

# ---------------------------------------------------------------- frame furniture
_BG=None; _WM=None
SYMS=['+','−','×','÷','=','%','½','¼','π','√','x','y','∑','²','∞','7','3','0.5']
def _bg():
    global _BG,_WM
    if _BG: return _BG
    s=cairo.ImageSurface(cairo.FORMAT_ARGB32,W,H); c=cairo.Context(s)
    c.set_source_rgb(*C['bg']); c.paint()
    c.set_source_rgba(1,1,1,0.045)
    for y in range(30,H,44):
        for x in range(30,W,44): c.arc(x,y,1.6,0,TAU); c.fill()
    g=cairo.RadialGradient(W/2,H/2,H*0.3,W/2,H/2,W*0.75); g.add_color_stop_rgba(0,0,0,0,0); g.add_color_stop_rgba(1,0,0,0,0.55)
    vg=cairo.ImageSurface(cairo.FORMAT_ARGB32,W,H); vc=cairo.Context(vg); vc.set_source(g); vc.paint()
    _WM=cairo.ImageSurface.create_from_png(os.path.join(HERE,'wordmark.png'))
    _BG=(s,vg); return _BG
class Look:
    def __init__(self,seed,accent):
        r=random.Random(seed); self.accent=accent
        self.blobs=[(r.uniform(0,W),r.uniform(0,H),r.uniform(380,620),r.choice([C['pink'],C['purple'],C['blue'],C['orange'],C['green']]),r.uniform(0,TAU),r.uniform(0.05,0.12)) for _ in range(4)]
        self.blobs[0]=(W*0.85,H*0.2,620,accent,0.3,0.08)
        self.syms=[(r.uniform(0,W),r.uniform(0,H),r.randint(40,120),r.choice(SYMS),r.uniform(-0.4,0.4),r.uniform(8,22)) for _ in range(22)]
    def back(self,c,t):
        s,_=_bg(); c.set_source_surface(s,0,0); c.paint()
        for x,y,rad,col,ph,sp in self.blobs:
            bx=x+math.sin(t*sp+ph)*140; by=y+math.cos(t*sp*1.3+ph)*90
            g=cairo.RadialGradient(bx,by,0,bx,by,rad); g.add_color_stop_rgba(0,*col,0.16); g.add_color_stop_rgba(1,*col,0)
            c.set_source(g); c.arc(bx,by,rad,0,TAU); c.fill()
        for x,y,sz,ch,rot,sp in self.syms:
            yy=(y-t*sp)%(H+200)-100
            c.save(); c.translate(x,yy); c.rotate(rot); str_draw(c,ch,0,0,sz,(1,1,1),0.035); c.restore()
    def front(self,c,t,dur,cat,title):
        _,vg=_bg(); c.set_source_surface(vg,0,0); c.paint()
        a=clamp((t-2.9)/0.5)
        if a>0:
            # chip with category + lesson title
            c.set_font_size(26); w=str_w(c,cat.upper(),24)+64
            rrect(c,64,54,w,52,26); c.set_source_rgba(*self.accent,0.22*a); c.fill()
            c.arc(92,80,7,0,TAU); c.set_source_rgba(*self.accent,a); c.fill()
            str_draw(c,cat.upper(),110,80,24,mix(self.accent,(1,1,1),0.55),a)
            str_draw(c,title,64+w+22,80,30,(1,1,1),0.75*a)
        c.save(); sc=0.30; c.translate(W-64-_WM.get_width()*sc,58); c.scale(sc,sc); c.set_source_surface(_WM,0,0); c.paint_with_alpha(0.85); c.restore()
        # progress
        rrect(c,64,H-44,W-128,8,4); c.set_source_rgba(1,1,1,0.10); c.fill()
        rrect(c,64,H-44,max(8,(W-128)*clamp(t/dur)),8,4); c.set_source_rgba(*self.accent,0.9); c.fill()
    def title_card(self,c,t,cat,title,sub,num):
        if t>3.4: return
        p=win(t,0.15,2.6,0.6,0.5)
        line(c,[(f'{cat.upper()}  ·  LESSON {num}',mix(self.accent,(1,1,1),0.4))],W/2,H/2-150,32,t,0.1,2.6,0,'up')
        line(c,[(w+' ',C['white']) for w in title.split(' ')],W/2,H/2-30,112,t,0.3,2.6,0.08,'pop')
        line(c,[(sub,C['dim'],False)],W/2,H/2+110,40,t,0.9,2.6,0,'up')
        if p>0:
            q=ease_out(clamp((t-0.7)/0.6)); c.set_source_rgba(*self.accent,p); rrect(c,W/2-90*q,H/2+190,180*q,10,5); c.fill()

# ---------------------------------------------------------------- render
def render(path,dur,draw,look,cat,title,sub,num,kbps=None,crf=16,t_only=None):
    s=cairo.ImageSurface(cairo.FORMAT_ARGB32,W,H); c=cairo.Context(s)
    def frame(t):
        look.back(c,t); look.title_card(c,t,cat,title,sub,num)
        c.save(); draw(c,t); c.restore()
        look.front(c,t,dur,cat,title)
        fo=clamp((t-(dur-0.5))/0.5)
        if fo>0: c.set_source_rgba(*C['bg'],fo); c.paint()
    if t_only is not None:
        for tt in t_only:
            frame(tt); s.write_to_png(path.replace('.mp4',f'_{tt:05.1f}.png'))
        return
    vf='scale=out_color_matrix=bt709:out_range=tv,format=yuv420p,noise=c0s=7:c0f=t+u'
    rate=(['-b:v',f'{kbps}k','-maxrate',f'{int(kbps*1.5)}k','-bufsize',f'{int(kbps*3)}k'] if kbps else ['-crf',str(crf)])
    cmd=['ffmpeg','-y','-loglevel','error','-f','rawvideo','-pix_fmt','bgra','-s',f'{W}x{H}','-r',str(FPS),'-i','-','-vf',vf,
         '-c:v','libx264','-preset','medium']+rate+['-profile:v','high','-level','4.1','-colorspace','bt709','-color_primaries','bt709','-color_trc','bt709',
         '-g','60','-threads','1','-an','-movflags','+faststart',path]
    pr=subprocess.Popen(cmd,stdin=subprocess.PIPE)
    for f in range(int(round(dur*FPS))):
        frame(f/FPS); s.flush(); pr.stdin.write(s.get_data())
    pr.stdin.close(); pr.wait()
    return os.path.getsize(path)
