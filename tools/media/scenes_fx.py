"""Celebration overlays (light on pure black, played with mix-blend-mode:screen)."""
import sys, math, random, cairo, time
sys.path.insert(0,'.')
from vkit import *
SY=['+','−','×','÷','=','π','Σ','∞','√','%','½','x²','7','3','9']
def out(name,w,h): return f'./out/fx/fx_{name}_{"portrait" if h>w else "landscape"}.mp4'

def confetti(w,h,fps=30,dur=3.0,seed=1):
    rnd=random.Random(seed); v=Video(out('confetti',w,h),w,h,fps,crf=18); c=v.ctx; S=min(w,h)/1080
    P=[]
    for side in (0,1):
        for i in range(110):
            ang=math.radians(rnd.uniform(-80,-35) if side==0 else rnd.uniform(-145,-100)); sp=rnd.uniform(900,1900)*S*(h/1920 if h>w else 0.75)**0.5
            P.append(dict(x=w*(0.08 if side==0 else 0.92),y=h*0.92,vx=sp*math.cos(ang),vy=sp*math.sin(ang),col=rnd.choice(CLUB+[PAL['gold']]),
                kind=rnd.random(),s=rnd.choice(SY),size=rnd.uniform(16,40)*S,rot=rnd.uniform(0,TAU),vr=rnd.uniform(-9,9),delay=rnd.uniform(0,0.25),flip=rnd.uniform(4,12)))
    for i in range(70):
        ang=rnd.uniform(0,TAU); sp=rnd.uniform(300,1300)*S
        P.append(dict(x=w/2,y=h*0.45,vx=sp*math.cos(ang),vy=sp*math.sin(ang)-300*S,col=rnd.choice(CLUB+[PAL['gold']]),kind=rnd.random(),s=rnd.choice(SY),
            size=rnd.uniform(18,46)*S,rot=rnd.uniform(0,TAU),vr=rnd.uniform(-6,6),delay=0.05+rnd.uniform(0,0.1),flip=rnd.uniform(4,12)))
    g=1500*S; N=int(dur*fps)
    for f in range(N):
        t=f/fps; c.set_source_rgb(0,0,0); c.paint()
        fl=clamp(1-t/0.35)
        if fl>0:
            gg=cairo.RadialGradient(w/2,h*0.45,0,w/2,h*0.45,min(w,h)*0.6); gg.add_color_stop_rgba(0,1,1,1,0.35*fl); gg.add_color_stop_rgba(1,1,1,1,0); c.set_source(gg); c.paint()
        for p in P:
            tt=t-p['delay']
            if tt<0: continue
            drag=math.exp(-1.6*tt); x=p['x']+p['vx']*(1-drag)/1.6; y=p['y']+p['vy']*(1-drag)/1.6+0.5*g*tt*tt*0.55
            a=clamp(1-(t-2.1)/0.9)
            if a<=0 or y>h+60: continue
            c.save(); c.translate(x,y); c.rotate(p['rot']+p['vr']*tt)
            if p['kind']<0.55:
                sx=abs(math.cos(p['flip']*tt))+0.15; c.scale(sx,1)
                c.set_source_rgba(*mix(p['col'],(1,1,1),0.15),a); c.rectangle(-p['size']*0.35,-p['size']*0.18,p['size']*0.7,p['size']*0.36); c.fill()
            else:
                text(c,p['s'],0,0,p['size'],mix(p['col'],(1,1,1),0.3),a)
            c.restore()
        v.push()
    return v.close()

def stars(w,h,fps=30,dur=2.6,seed=2):
    rnd=random.Random(seed); v=Video(out('stars',w,h),w,h,fps,crf=18); c=v.ctx; S=min(w,h)/1080; cx,cy=w/2,h*0.45
    rays=[dict(a=rnd.uniform(0,TAU),sp=rnd.uniform(500,1300)*S,r=rnd.uniform(8,22)*S,col=rnd.choice([PAL['gold'],PAL['orange'],PAL['pink'],(1,1,1)]),d=rnd.uniform(0,0.2)) for _ in range(60)]
    sp=[dict(x=rnd.uniform(0,w),y=rnd.uniform(0,h),d=rnd.uniform(0.2,1.6),r=rnd.uniform(4,12)*S) for _ in range(80)]
    def star(x,y,r,rot,inner=0.45):
        for i in range(11):
            rr=r if i%2==0 else r*inner; a=rot-TAU/4+math.pi*i/5
            (c.move_to if i==0 else c.line_to)(x+rr*math.cos(a),y+rr*math.sin(a))
        c.close_path()
    N=int(dur*fps)
    for f in range(N):
        t=f/fps; c.set_source_rgb(0,0,0); c.paint()
        k=ease_back(clamp(t/0.5)); a=clamp(1-(t-1.6)/0.8)
        R=150*S*k
        gg=cairo.RadialGradient(cx,cy,0,cx,cy,R*3); gg.add_color_stop_rgba(0,*PAL['gold'],0.45*a); gg.add_color_stop_rgba(1,*PAL['gold'],0); c.set_source(gg); c.paint()
        star(cx,cy,R,t*1.2); c.set_source_rgba(*mix(PAL['gold'],(1,1,1),0.3),0.95*a); c.fill()
        for q in range(3):
            u=clamp((t-q*0.18)/1.2)
            if 0<u<1:
                c.new_path(); c.arc(cx,cy,R*1.1+u*min(w,h)*0.55,0,TAU); glow_stroke(c,[PAL['gold'],PAL['pink'],PAL['orange']][q],4*S*(1-u),0.8*(1-u))
        for r in rays:
            tt=t-r['d']
            if tt<=0: continue
            d=r['sp']*(1-math.exp(-2.2*tt))/2.2; x=cx+d*math.cos(r['a']); y=cy+d*math.sin(r['a'])+120*S*tt*tt
            star(x,y,r['r']*(1-0.3*tt),tt*4+r['a']); c.set_source_rgba(*r['col'],a*0.95); c.fill()
        for s in sp:
            u=(t-s['d'])/0.5
            if 0<u<1:
                al=math.sin(math.pi*u); c.set_source_rgba(1,1,1,al*0.9); c.set_line_width(2*S)
                c.move_to(s['x']-s['r']*2,s['y']); c.line_to(s['x']+s['r']*2,s['y']); c.move_to(s['x'],s['y']-s['r']*2); c.line_to(s['x'],s['y']+s['r']*2); c.stroke()
        v.push()
    return v.close()

def rings(w,h,fps=30,dur=2.6,seed=3):
    rnd=random.Random(seed); v=Video(out('rings',w,h),w,h,fps,crf=18); c=v.ctx; S=min(w,h)/1080; cx,cy=w/2,h*0.45
    shards=[dict(a=rnd.uniform(0,TAU),sp=rnd.uniform(400,1100)*S,n=rnd.choice((3,4,5,6)),r=rnd.uniform(14,36)*S,col=rnd.choice(CLUB),rot=rnd.uniform(-5,5)) for _ in range(46)]
    N=int(dur*fps)
    for f in range(N):
        t=f/fps; c.set_source_rgb(0,0,0); c.paint(); a=clamp(1-(t-1.7)/0.8)
        for q,(n,col) in enumerate(((3,PAL['pink']),(4,PAL['blue']),(5,PAL['green']),(6,PAL['orange']),(8,PAL['purple']))):
            u=clamp((t-q*0.09)/1.3)
            if u<=0: continue
            r=(60+u*700)*S*ease_out(u); rot=t*(1 if q%2 else -1)
            c.new_path()
            for i in range(n+1):
                an=rot+TAU*i/n-TAU/4; (c.move_to if i==0 else c.line_to)(cx+r*math.cos(an),cy+r*math.sin(an))
            glow_stroke(c,col,5*S*(1-u)+1,0.9*(1-u**2))
        for s in shards:
            d=s['sp']*(1-math.exp(-2*t))/2; x=cx+d*math.cos(s['a']); y=cy+d*math.sin(s['a'])
            c.new_path()
            for i in range(s['n']+1):
                an=s['rot']*t+TAU*i/s['n']; (c.move_to if i==0 else c.line_to)(x+s['r']*math.cos(an),y+s['r']*math.sin(an))
            glow_stroke(c,s['col'],2.5*S,a*0.9)
        k=ease_back(clamp((t-0.1)/0.5))
        if k>0:
            text_glow(c,'↑',cx,cy,170*S*k,PAL['green'],a)
        v.push()
    return v.close()

def badge(w,h,fps=30,dur=3.0,seed=4):
    rnd=random.Random(seed); v=Video(out('badge',w,h),w,h,fps,crf=18); c=v.ctx; S=min(w,h)/1080; cx,cy=w/2,h*0.45
    N=int(dur*fps)
    for f in range(N):
        t=f/fps; c.set_source_rgb(0,0,0); c.paint(); a=clamp(t/0.3)*clamp(1-(t-2.2)/0.8)
        for i in range(24):
            an=TAU*i/24+t*0.5; L=min(w,h)*0.9
            c.move_to(cx,cy); c.line_to(cx+L*math.cos(an-0.05),cy+L*math.sin(an-0.05)); c.line_to(cx+L*math.cos(an+0.05),cy+L*math.sin(an+0.05)); c.close_path()
            gg=cairo.RadialGradient(cx,cy,0,cx,cy,L); gg.add_color_stop_rgba(0,*PAL['gold'],0.35*a); gg.add_color_stop_rgba(1,*PAL['gold'],0); c.set_source(gg); c.fill()
        k=ease_back(clamp((t-0.15)/0.6)); R=170*S*k
        if R>1:
            c.new_path(); c.arc(cx,cy,R,0,TAU); glow_stroke(c,PAL['gold'],8*S,a)
            c.new_path(); c.arc(cx,cy,R*0.78,0,TAU); glow_stroke(c,PAL['orange'],4*S,a)
            text_glow(c,'%',cx,cy,R*1.1,PAL['pink'],a)
        for i in range(40):
            u=(t*0.9+i/40)%1; an=TAU*i/40*7.3; d=(R+40*S)+u*300*S
            glow_dot(c,cx+d*math.cos(an),cy+d*math.sin(an),3*S,CLUB[i%6],a*(1-u))
        v.push()
    return v.close()

if __name__=='__main__':
    t0=time.time()
    for fn in (confetti,stars,rings,badge):
        for (w,h) in ((1080,1920),(1920,1080)):
            print(fn.__name__,w,h,fn(w,h),round(time.time()-t0,1),flush=True)
