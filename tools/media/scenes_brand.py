import sys, math, random, cairo, time
sys.path.insert(0,'.')
from vkit import *
SYMS=['+','−','×','÷','=','√','π','Σ','∞','∫','%','Δ','θ','≈','≠','≤','≥','∏','φ','∂','x²','y','n!','e','i','λ','⅓','½','¾','7','3','9','1','0']

def intro(path,w,h,fps=30,dur=5.0,seed=7):
    rnd=random.Random(seed); v=Video(path,w,h,fps,crf=17)
    bg=dot_grid(w,h); vig=vignette(w,h,0.6); c=v.ctx; cx,cy=w/2,h*0.5; R=math.hypot(w,h)/2
    parts=[]
    for i in range(140):
        ang=rnd.uniform(0,TAU); sp=rnd.uniform(0.25,1.05)*R
        parts.append(dict(s=rnd.choice(SYMS),ang=ang,dist=sp,col=rnd.choice(CLUB),size=rnd.uniform(22,78)*(w/1080)**0.5,
                          rot=rnd.uniform(-2,2),delay=rnd.uniform(0,0.5),wob=rnd.uniform(0,TAU)))
    polys=[(3,0.20,PAL['pink'],1),(4,0.27,PAL['blue'],-1),(5,0.34,PAL['green'],1),(6,0.41,PAL['orange'],-1),(8,0.48,PAL['purple'],1)]
    N=int(dur*fps)
    for f in range(N):
        t=f/fps; c.set_source_surface(bg); c.paint()
        k=ease_out(t/1.2); g=cairo.RadialGradient(cx,cy,0,cx,cy,R*0.55*(0.4+0.6*k))
        pul=0.5+0.5*math.sin(t*2.4)
        g.add_color_stop_rgba(0,*PAL['brand'],0.38*k*(0.75+0.25*pul)); g.add_color_stop_rgba(0.5,*PAL['purple'],0.10*k); g.add_color_stop_rgba(1,0,0,0,0)
        c.set_source(g); c.paint()
        for j,(col,dirn) in enumerate(((PAL['green'],1),(PAL['pink'],-1))):
            kb=ease_out((t-0.2-j*0.4)/2.6)
            if kb<=0: continue
            c.save(); c.translate(cx,cy); c.rotate(dirn*(t*0.6+j)); c.scale(1.0,0.86)
            rr=min(w,h)*0.62*(0.15+0.85*kb)
            gb=cairo.RadialGradient(0,0,rr*0.2,0,0,rr); gb.add_color_stop_rgba(0,*col,0.0); gb.add_color_stop_rgba(0.7,*col,0.10*kb); gb.add_color_stop_rgba(1,*col,0)
            c.set_source(gb); c.arc(0,0,rr,0,TAU); c.fill(); c.restore()
        for j,(n,rad,col,dirn) in enumerate(polys):
            p=ease_in_out((t-0.15*j)/1.4)
            if p<=0: continue
            r=min(w,h)*rad; a0=dirn*t*0.35+j
            c.new_path()
            pts=[(cx+r*math.cos(a0+TAU*q/n),cy+r*math.sin(a0+TAU*q/n)) for q in range(n+1)]
            total=n*p
            c.move_to(*pts[0])
            for q in range(1,n+1):
                if q<=total: c.line_to(*pts[q])
                else:
                    fr=total-(q-1)
                    if fr>0: c.line_to(lerp(pts[q-1][0],pts[q][0],fr),lerp(pts[q-1][1],pts[q][1],fr))
                    break
            glow_stroke(c,col,2.2*(w/1080),a=0.55)
            for q in range(n):
                if q<total: glow_dot(c,*pts[q],3.2*(w/1080),col,0.8)
        for p in parts:
            tt=t-0.35-p['delay']
            if tt<=0: continue
            e=ease_out(tt/2.2); d=p['dist']*e + tt*18
            x=cx+d*math.cos(p['ang'])+6*math.sin(t*1.3+p['wob']); y=cy+d*math.sin(p['ang'])+6*math.cos(t*1.1+p['wob'])
            a=clamp(tt*3)*(0.35+0.65*(1-e*0.55))
            c.save(); c.translate(x,y); c.rotate(p['rot']*e+0.2*math.sin(t+p['wob']))
            text(c,p['s'],0,0,p['size'],p['col'],a*0.25); text(c,p['s'],0,0,p['size']*0.92,mix(p['col'],(1,1,1),0.35),a)
            c.restore()
        sw=(t-2.4)/1.0
        if 0<sw<1:
            xx=lerp(-w*0.3,w*1.3,ease_in_out(sw))
            lg=cairo.LinearGradient(xx-w*0.25,0,xx+w*0.25,0); lg.add_color_stop_rgba(0,1,1,1,0); lg.add_color_stop_rgba(0.5,1,1,1,0.07); lg.add_color_stop_rgba(1,1,1,1,0)
            c.set_source(lg); c.paint()
        cg=cairo.RadialGradient(cx,cy,0,cx,cy,min(w,h)*0.33); cg.add_color_stop_rgba(0,*PAL['bg'],0.55*ease_out(t/1.5)); cg.add_color_stop_rgba(1,*PAL['bg'],0)
        c.set_source(cg); c.paint()
        c.set_source_surface(vig); c.paint()
        if t<0.35: c.set_source_rgba(*PAL['bg'],1-t/0.35); c.paint()
        v.push()
    return v.close()

def welcome_loop(path,w,h,fps=30,dur=12.0,seed=11):
    rnd=random.Random(seed); v=Video(path,w,h,fps,crf=17)
    bg=dot_grid(w,h); vig=vignette(w,h,0.65); c=v.ctx; T=dur; S=(w*h/(1080*1920))**0.5
    parts=[]
    for i in range(int(110*S)+30):
        depth=rnd.choice((0.35,0.6,1.0))
        parts.append(dict(s=rnd.choice(SYMS),x=rnd.uniform(0,w),y=rnd.uniform(0,h),ax=rnd.uniform(10,60)*depth*S,ay=rnd.uniform(10,70)*depth*S,
          kx=rnd.choice((1,2)),ky=rnd.choice((1,2,3)),ph=rnd.uniform(0,TAU),ph2=rnd.uniform(0,TAU),col=rnd.choice(CLUB),
          size=rnd.uniform(18,46)*depth*1.6*S+10,depth=depth,rot=rnd.uniform(-0.5,0.5)))
    parts.sort(key=lambda p:p['depth'])
    blobs=[(PAL['purple'],0.30,0.25,0.42),(PAL['green'],0.75,0.35,0.36),(PAL['pink'],0.25,0.78,0.40),(PAL['blue'],0.80,0.80,0.34),(PAL['orange'],0.55,0.55,0.28)]
    nodes=[dict(x=rnd.uniform(0,w),y=rnd.uniform(0,h),a=rnd.uniform(20,80)*S,ph=rnd.uniform(0,TAU),k=rnd.choice((1,2))) for _ in range(int(46*S)+12)]
    N=int(dur*fps)
    for f in range(N):
        t=f/fps; ph=TAU*t/T
        c.set_source_surface(bg); c.paint()
        for j,(col,bx,by,rad) in enumerate(blobs):
            x=w*bx+w*0.10*math.cos(ph+j*1.3); y=h*by+h*0.06*math.sin(ph*1+j*2.1)
            r=min(w,h)*rad*(1+0.12*math.sin(ph*2+j))
            g=cairo.RadialGradient(x,y,0,x,y,r); g.add_color_stop_rgba(0,*col,0.22); g.add_color_stop_rgba(0.6,*col,0.07); g.add_color_stop_rgba(1,*col,0)
            c.set_source(g); c.arc(x,y,r,0,TAU); c.fill()
        P=[(n['x']+n['a']*math.cos(ph*n['k']+n['ph']),n['y']+n['a']*math.sin(ph*n['k']+n['ph']*1.7)) for n in nodes]
        lim=230*S+60
        c.set_line_width(1.2*S+0.4)
        for i in range(len(P)):
            for j in range(i+1,len(P)):
                d=math.hypot(P[i][0]-P[j][0],P[i][1]-P[j][1])
                if d<lim:
                    c.set_source_rgba(1,1,1,0.10*(1-d/lim)); c.move_to(*P[i]); c.line_to(*P[j]); c.stroke()
        for i,(x,y) in enumerate(P): glow_dot(c,x,y,2.2*S+1,CLUB[i%6],0.7)
        for j,(n,rad,col) in enumerate(((6,0.30,PAL['green']),(4,0.22,PAL['pink']),(3,0.14,PAL['blue']))):
            cx,cy=w*0.5,h*0.5; r=min(w,h)*rad; a0=(TAU/n)*(t/T)*(1 if j%2==0 else -1)
            c.new_path()
            for q in range(n+1):
                x=cx+r*math.cos(a0+TAU*q/n); y=cy+r*math.sin(a0+TAU*q/n)
                (c.move_to if q==0 else c.line_to)(x,y)
            glow_stroke(c,col,1.6*S+0.6,a=0.22)
        for p in parts:
            x=p['x']+p['ax']*math.sin(ph*p['kx']+p['ph']); y=p['y']+p['ay']*math.sin(ph*p['ky']+p['ph2'])
            a=(0.22+0.5*p['depth'])*(0.75+0.25*math.sin(ph*2+p['ph']))
            c.save(); c.translate(x,y); c.rotate(p['rot']*math.sin(ph+p['ph']))
            text(c,p['s'],0,0,p['size'],mix(p['col'],(1,1,1),0.25),a)
            c.restore()
        c.set_source_surface(vig); c.paint()
        v.push()
    return v.close()

if __name__=='__main__':
    which=sys.argv[1]; out='./out/'
    t0=time.time()
    if which=='intro':
        print('intro_portrait',intro(out+'intro_portrait.mp4',1080,1920)); print('intro_landscape',intro(out+'intro_landscape.mp4',1920,1080))
    if which=='welcome':
        print('welcome_portrait',welcome_loop(out+'welcome_portrait.mp4',1080,1920)); print('welcome_landscape',welcome_loop(out+'welcome_landscape.mp4',1920,1080))
    if which=='test':
        print(intro(out+'test.mp4',1080,1920,dur=1.0))
    print('secs',round(time.time()-t0,1))
