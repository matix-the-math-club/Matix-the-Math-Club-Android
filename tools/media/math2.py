from gallery import *
_KEEP=[]

def to_surface_premul(rgb,alpha):
    """rgb HxWx3 float 0..1 (straight), alpha HxW float 0..1 -> cairo surface (keeps buffer referenced)"""
    h,w=alpha.shape; a=np.clip(alpha,0,1)
    arr=np.empty((h,w,4),np.uint8)
    arr[...,0]=np.clip(rgb[...,2]*a*255,0,255); arr[...,1]=np.clip(rgb[...,1]*a*255,0,255); arr[...,2]=np.clip(rgb[...,0]*a*255,0,255); arr[...,3]=a*255
    s=surface_from_array(arr); _KEEP[:]=[arr]; return s

class ChaosGame(Scene):
    name='chaos_game'; dur=24.0; title='The chaos game'; sub='Jump halfway to a random corner, again and again — order appears out of randomness'
    sequential=True; needs_every_frame=True
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; self.rng=np.random.default_rng(5); M=80000
        cx,cy=w*0.5,h*0.53; R=500*S
        self.V=np.array([(cx+R*math.cos(-TAU/4+TAU*i/3),cy+R*math.sin(-TAU/4+TAU*i/3)) for i in range(3)])
        self.P=self.rng.uniform([cx-100,cy-100],[cx+100,cy+100],(M,2)); self.M=M
        self.acc=np.zeros((3,h*w),np.float32); self.fern=np.zeros(h*w,np.float32)
        self.F=self.rng.uniform(-0.5,0.5,(M,2)); self.frames=0; self.last=[]
        self.cols=np.array([PAL['pink'],PAL['blue'],PAL['green']],np.float32)
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; fps=self.fps
        if t<12:
            n=int(min(self.M,3*math.exp(t*1.15)))
            k=self.rng.integers(0,3,self.M); self.P=(self.P+self.V[k])/2
            idx=(self.P[:n,1].astype(int).clip(0,h-1))*w+self.P[:n,0].astype(int).clip(0,w-1)
            for v in range(3):
                sel=k[:n]==v; self.acc[v]+=np.bincount(idx[sel],minlength=w*h)
            tot=self.acc.sum(0); inten=1-np.exp(-tot/ (2.0+tot.mean()*3 if tot.mean()>0 else 1))
            inten=np.sqrt(np.clip(tot/ max(1.0,np.percentile(tot[tot>0],97) if (tot>0).any() else 1),0,1))
            rgb=(self.acc.T@self.cols)/np.maximum(tot,1)[:,None]
            rgb=rgb.reshape(h,w,3); fo=1-smooth((t-11.2)/0.8)
            c.set_source_surface(to_surface_premul(np.minimum(rgb*1.15+0.08,1),inten.reshape(h,w)*fo)); c.paint()
            for i,(x,y) in enumerate(self.V): glow_dot(c,x,y,9*S,tuple(self.cols[i]),fo)
            if n<40:
                self.last.append(tuple(self.P[0])); 
                for j in range(1,len(self.last)):
                    c.set_source_rgba(1,1,1,0.5*fo); c.set_line_width(1.5*S); c.move_to(*self.last[j-1]); c.line_to(*self.last[j]); c.stroke()
            text(c,f'{int(self.acc.sum()):,} jumps',w*0.86,h*0.14,34*S,(1,1,1),0.85*fo)
            text(c,'Sierpiński triangle',w*0.86,h*0.20,28*S,PAL['pink'],0.85*fo,bold=False)
        else:
            lt=t-12; n=int(min(self.M,200*math.exp(lt*1.2)))
            r=self.rng.random(self.M); x,y=self.F[:,0],self.F[:,1]
            nx=np.where(r<0.01,0,np.where(r<0.86,0.85*x+0.04*y,np.where(r<0.93,0.2*x-0.26*y,-0.15*x+0.28*y)))
            ny=np.where(r<0.01,0.16*y,np.where(r<0.86,-0.04*x+0.85*y+1.6,np.where(r<0.93,0.23*x+0.22*y+1.6,0.26*x+0.24*y+0.44)))
            self.F=np.stack([nx,ny],1)
            sc=95*S; X=(w*0.5+nx[:n]*sc).astype(int).clip(0,w-1); Y=(h*0.97-ny[:n]*sc).astype(int).clip(0,h-1)
            self.fern+=np.bincount(Y*w+X,minlength=w*h)
            tot=self.fern; ref=np.percentile(tot[tot>0],97) if (tot>0).any() else 1
            inten=np.sqrt(np.clip(tot/max(ref,1),0,1)).reshape(h,w)
            yy=np.linspace(0,1,h)[:,None]*np.ones((1,w))
            g=np.stack([0.243+0.6*(1-yy)*0.3,0.914-0.2*yy,0.561-0.3*yy],2)
            g=np.clip(g*0.7+0.3*np.array(PAL['olive'])*(yy[...,None]),0,1)
            fi=smooth(lt/0.6)
            c.set_source_surface(to_surface_premul(g,inten*fi)); c.paint()
            text(c,f'{int(self.fern.sum()):,} jumps',w*0.86,h*0.14,34*S,(1,1,1),0.85*fi)
            text(c,'Barnsley fern · 4 simple rules',w*0.86,h*0.20,28*S,PAL['green'],0.85*fi,bold=False)

class Galton(Scene):
    name='galton'; dur=26.0; title='The Galton board'; sub='Each ball flips left or right 14 times — together they draw the bell curve'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; self.S=S; rng=random.Random(8)
        self.rows=14; self.dx=50*S; self.dy=38*S; self.top=110*S; self.cx=w*0.5; self.br=6.5*S
        self.binY=self.top+self.dy*(self.rows+1)+20*S; self.floor=h-60*S
        self.balls=[]; t=0.6; arrivals={}
        while t<20.5:
            path=[rng.choice((-1,1)) for _ in range(self.rows)]; b=sum(1 for p in path if p>0)
            per=0.075; tland=t+per*(self.rows+1)
            arrivals.setdefault(b,0); slot=arrivals[b]; arrivals[b]+=1
            self.balls.append(dict(t=t,path=path,bin=b,slot=slot,per=per,col=CLUB[rng.randrange(6)]))
            t+=0.055 if t>3 else 0.16
        self.N=len(self.balls)
    def binx(self,b): return self.cx+(b-self.rows/2)*self.dx
    def slotpos(self,b,slot):
        per_row=4; r=slot//per_row; q=slot%per_row; s=self.br*1.9
        return self.binx(b)+(q-1.5)*s, self.floor-self.br-r*s*0.95
    def draw(self,c,t):
        w,h=self.w,self.h; S=self.S
        # pegs
        for r in range(self.rows):
            for k in range(r+1):
                x=self.cx+(k-r/2)*self.dx; y=self.top+self.dy*(r+1)
                c.set_source_rgba(1,1,1,0.55); c.arc(x,y,3.2*S,0,TAU); c.fill()
        # bins
        c.set_source_rgba(1,1,1,0.18); c.set_line_width(2*S)
        for b in range(self.rows+2):
            x=self.binx(b)-self.dx/2; c.move_to(x,self.binY); c.line_to(x,self.floor); 
        c.move_to(self.binx(0)-self.dx/2,self.floor); c.line_to(self.binx(self.rows)+self.dx/2,self.floor); c.stroke()
        # funnel
        c.set_source_rgba(1,1,1,0.3); c.move_to(self.cx-80*S,self.top-50*S); c.line_to(self.cx-12*S,self.top+8*S); c.move_to(self.cx+80*S,self.top-50*S); c.line_to(self.cx+12*S,self.top+8*S); c.stroke()
        counts=[0]*(self.rows+1); landed=0
        for bl in self.balls:
            lt=t-bl['t']
            if lt<0: continue
            step=lt/bl['per']
            if step<self.rows:
                i=int(step); fr=step-i; xoff=sum(bl['path'][:i])*self.dx/2
                x=self.cx+xoff+bl['path'][i]*self.dx/2*smooth(fr); y=self.top+self.dy*(i+fr)+self.dy*0.55-math.sin(math.pi*fr)*10*S
                glow_dot(c,x,y,self.br,bl['col'],halo=2.4)
            else:
                xb=self.binx(bl['bin']); sx,sy=self.slotpos(bl['bin'],bl['slot'])
                y0=self.top+self.dy*self.rows+self.dy*0.55; fall=(step-self.rows)*bl['per']
                yf=y0+0.5*2600*S*fall*fall
                if yf<sy:
                    x=lerp(xb,sx,clamp((yf-y0)/max(1,sy-y0))); glow_dot(c,x,yf,self.br,bl['col'],halo=2.4)
                else:
                    c.set_source_rgba(*mix(bl['col'],(1,1,1),0.25),0.95); c.arc(sx,sy,self.br,0,TAU); c.fill(); counts[bl['bin']]+=1; landed+=1
        text(c,f'{landed} balls',w*0.86,h*0.14,36*S,(1,1,1),0.85)
        e=smooth((t-21)/1.5)
        if e>0:
            n=self.rows; s=self.br*1.9*0.95/4  # height per ball
            c.new_path()
            for q in range(0,201):
                b=q/200*n; pdf=math.exp(-(b-n/2)**2/(2*n/4))/math.sqrt(TAU*n/4)
                y=self.floor-pdf*self.N*s; x=self.binx(b)
                (c.move_to if q==0 else c.line_to)(x,y)
            glow_stroke(c,PAL['gold'],3*S,e)
            text(c,'binomial  →  normal',w*0.86,h*0.20,30*S,PAL['gold'],e,bold=False)

class Primes(Scene):
    name='primes'; dur=22.0; title='The Ulam spiral'; sub='Write 1, 2, 3… in a square spiral and light up the primes — diagonals appear'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; self.cell=10*S; nx=int(w/self.cell)+2; ny=int(h/self.cell)+2; N=nx*ny; self.N=N
        sieve=np.ones(N+2,bool); sieve[:2]=False
        for i in range(2,int(N**0.5)+2):
            if sieve[i]: sieve[i*i::i]=False
        self.isp=sieve
        xs=np.zeros(N+1,int); ys=np.zeros(N+1,int); x=y=0; d=0; dirs=[(1,0),(0,-1),(-1,0),(0,1)]; n=1; seg=1
        xs[1]=0; ys[1]=0
        while n<N:
            for _ in range(2):
                for _ in range(seg):
                    if n>=N: break
                    x+=dirs[d][0]; y+=dirs[d][1]; n+=1; xs[n]=x; ys[n]=y
                d=(d+1)%4
            seg+=1
        self.X=w/2+xs*self.cell; self.Y=h/2+ys*self.cell
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; cell=self.cell
        n=int(1+(self.N-1)*ease_in_out(t/15.0)**1.6)
        ids=np.arange(1,n+1); pr=self.isp[1:n+1]
        comp=ids[~pr]; prs=ids[pr]
        c.set_source_rgba(1,1,1,0.10)
        for k in comp[::1]:
            c.rectangle(self.X[k]-1*S,self.Y[k]-1*S,2*S,2*S)
        c.fill()
        for colk,(m,col) in enumerate(((1,PAL['pink']),(5,PAL['blue']),(2,PAL['gold']),(3,PAL['gold']))):
            sel=prs[(prs%6)==m]
            c.set_source_rgba(*col,0.95)
            for k in sel: c.rectangle(self.X[k]-cell*0.38,self.Y[k]-cell*0.38,cell*0.76,cell*0.76)
            c.fill()
        # glowing head
        glow_dot(c,self.X[n],self.Y[n],5*S,(1,1,1))
        np_=int(pr.sum())
        c.set_source_rgba(*PAL['bg'],0.75); c.rectangle(w*0.86-200*S,h*0.09,400*S,140*S); c.fill()
        text(c,f'{n:,} numbers',w*0.86,h*0.13,34*S,(1,1,1),0.9)
        text(c,f'{np_:,} primes',w*0.86,h*0.185,30*S,PAL['pink'],0.9,bold=False)
        e=smooth((t-16)/1.2)
        if e>0:
            c.set_source_rgba(*PAL['bg'],0.75*e); c.rectangle(w*0.5-520*S,h*0.885,1040*S,70*S); c.fill()
            text(c,'Primes line up on diagonals — and nobody fully knows why',w*0.5,h*0.92,30*S,(1,1,1),e)

class MonteCarlo(Scene):
    name='monte_carlo'; dur=22.0; title='Finding π with random darts'; sub='Fraction of darts inside the quarter circle × 4  →  π'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; rng=np.random.default_rng(31415); self.M=60000
        self.pts=rng.random((self.M,2)); self.ins=(self.pts**2).sum(1)<=1
        self.cum=np.cumsum(self.ins); self.side=820*S; self.ox=150*S; self.oy=h*0.5-self.side/2
    def count(self,t): return int(min(self.M,1+ (t/19.0)**2.4*self.M))
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; sd=self.side; ox,oy=self.ox,self.oy
        n=self.count(t); P=self.pts[:n]; I=self.ins[:n]
        c.rectangle(ox,oy,sd,sd); glow_stroke(c,(1,1,1),2*S,0.6)
        c.new_path(); c.arc(ox,oy+sd,sd,-TAU/4,0); glow_stroke(c,PAL['gold'],2.5*S,0.9)
        r=max(1.4,4.5-math.log10(n+1))*S
        for sel,col in ((I,PAL['pink']),(~I,PAL['blue'])):
            Q=P[sel]; c.set_source_rgba(*col,0.85)
            for x,y in Q: c.rectangle(ox+x*sd-r/2,oy+(1-y)*sd-r/2,r,r)
            c.fill()
        for x,y in P[max(0,n-6):n]: glow_dot(c,ox+x*sd,oy+(1-y)*sd,5*S,(1,1,1))
        est=4*self.cum[n-1]/n
        rx=w*0.73
        text_glow(c,f'π ≈ {est:.5f}',rx,h*0.22,84*S,PAL['gold'])
        text(c,f'{int(self.cum[n-1]):,} inside  /  {n:,} darts  × 4',rx,h*0.31,30*S,(1,1,1),0.85,bold=False)
        text(c,f'real π = 3.14159…',rx,h*0.36,26*S,(0.7,0.7,0.75),0.8,bold=False)
        # convergence plot
        gx,gy,gw,gh=w*0.56,h*0.48,w*0.36,h*0.36
        c.set_source_rgba(1,1,1,0.15); c.rectangle(gx,gy,gw,gh); c.set_line_width(1.2*S); c.stroke()
        piy=gy+gh/2; c.set_dash([8*S,6*S]); c.set_source_rgba(*PAL['gold'],0.7); c.move_to(gx,piy); c.line_to(gx+gw,piy); c.stroke(); c.set_dash([])
        if n>2:
            c.new_path(); L=math.log10(self.M)
            ks=np.unique(np.logspace(0,math.log10(n),300).astype(int).clip(1,n))
            for j,k in enumerate(ks):
                e=4*self.cum[k-1]/k; x=gx+gw*math.log10(k)/L; y=piy-(e-math.pi)*gh*0.9
                y=min(gy+gh,max(gy,y)); (c.move_to if j==0 else c.line_to)(x,y)
            glow_stroke(c,PAL['pink'],2.2*S,0.95)
        text(c,'more darts  →  closer to π',gx+gw/2,gy+gh+30*S,24*S,(1,1,1),0.6,bold=False)

class Life(Scene):
    name='life'; dur=26.0; title="Conway's Game of Life"; sub='Two rules — be born with 3 neighbours, survive with 2 or 3 — and gliders start to fly'
    def setup(self,w,h):
        self.w,self.h=w,h; self.cs=8; cs=self.cs; self.gw=w//cs; self.gh=h//cs; gw,gh=self.gw,self.gh
        g=np.zeros((gh,gw),np.uint8)
        gun=["........................O...........","......................O.O...........","............OO......OO............OO","...........O...O....OO............OO","OO........O.....O...OO..............","OO........O...O.OO....O.O...........","..........O.....O.......O...........","...........O...O....................","............OO......................"]
        def put(p,ox,oy,flip=False):
            for y,l in enumerate(p):
                for x,ch in enumerate(l):
                    if ch=='O':
                        xx=ox+(len(l)-1-x if flip else x); g[(oy+y)%gh,(ox+x if not flip else xx)%gw]=1
        put(gun,4,4); put(gun,gw-44,gh-16,flip=True)
        rng=np.random.default_rng(3)
        soup=(rng.random((40,60))<0.35).astype(np.uint8); g[50:90,90:150]|=soup
        rp=[".OO","OO.",".O."]; put(rp,200,20); put(rp,40,100)
        self.gens=[]; self.ages=[]; a=g; age=np.zeros_like(g,np.uint16); trail=np.zeros(g.shape,np.float32); self.trails=[]
        ngen=int(self.dur*15)+2
        for i in range(ngen):
            self.gens.append(a); self.ages.append(age.copy()); self.trails.append(trail.astype(np.float16))
            n=sum(np.roll(np.roll(a,i2,0),j2,1) for i2 in (-1,0,1) for j2 in (-1,0,1) if (i2,j2)!=(0,0))
            b=((n==3)|((a==1)&(n==2))).astype(np.uint8)
            trail=np.maximum(trail*0.86,(a&(1-b)).astype(np.float32))
            age=np.where(b==1,np.minimum(age+1,60),0).astype(np.uint16); a=b
        m=np.zeros((cs,cs),np.float32); m[1:cs-1,1:cs-1]=1; self.mask=m
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; cs=self.cs
        gi=int(t*15); A=self.gens[gi].astype(np.float32); age=self.ages[gi].astype(np.float32); tr=self.trails[gi].astype(np.float32)
        k=np.clip(age/25,0,1)[...,None]
        young=np.array(PAL['green']); mid=np.array(PAL['blue']); old=np.array(PAL['purple'])
        col=np.where(k<0.5, young*(1-2*k)+mid*(2*k), mid*(2-2*k)+old*(2*k-1))
        col=np.where(A[...,None]>0,col,np.array(PAL['pink'])[None,None,:]*np.ones_like(col))
        al=np.maximum(A,tr*0.45)
        big=np.kron(al,self.mask); bigc=np.repeat(np.repeat(col,cs,0),cs,1)
        hh,ww=big.shape
        canvas_a=np.zeros((h,w),np.float32); canvas_c=np.zeros((h,w,3),np.float32)
        canvas_a[:hh,:ww]=big; canvas_c[:hh,:ww]=bigc
        c.set_source_surface(to_surface_premul(canvas_c,canvas_a)); c.paint()
        c.set_source_rgba(*PAL['bg'],0.75); c.rectangle(w*0.86-170*S,h*0.09,340*S,130*S); c.fill()
        text(c,f'generation {gi}',w*0.86,h*0.13,34*S,(1,1,1),0.9)
        text(c,f'{int(A.sum())} alive',w*0.86,h*0.185,30*S,PAL['green'],0.9,bold=False)

class RuleCA(Scene):
    name='cellular'; dur=24.0; title='One-line universes'; sub='Each row follows a tiny rule from the row above — Rule 30, Rule 90, Rule 110'
    def setup(self,w,h):
        self.w,self.h=w,h; self.cs=6; self.cols=w//self.cs; self.rowsv=h//self.cs; self.data={}
        for rule in (30,90,110):
            rows=600; a=np.zeros(self.cols,np.uint8)
            if rule==110: a[-2]=1
            else: a[self.cols//2]=1
            out=[a]
            tbl=np.array([(rule>>i)&1 for i in range(8)],np.uint8)
            for i in range(rows):
                l=np.roll(a,1); r=np.roll(a,-1); a=tbl[(l<<2)|(a<<1)|r]; out.append(a)
            self.data[rule]=np.array(out)
        xs=np.linspace(0,1,self.cols)
        self.grad=np.array([np.interp(xs*5,np.arange(6),[cc[k] for cc in CLUB]) for k in range(3)]).T
        m=np.zeros((self.cs,self.cs),np.float32); m[0:self.cs-1,0:self.cs-1]=1; self.mask=m
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; seg=8.0; i=min(2,int(t//seg)); lt=t-i*seg; rule=(30,90,110)[i]
        D=self.data[rule]; r=int(lt*45)+1; r0=max(0,r-self.rowsv); V=D[r0:r].astype(np.float32)
        fade=smooth(lt/0.4)*(1-smooth((lt-(seg-0.5))/0.5))
        age=np.linspace(0.45,1,V.shape[0])[:,None]
        al=V*age*fade
        big=np.kron(al,self.mask); hh,ww=big.shape
        canvas_a=np.zeros((h,w),np.float32); canvas_a[:hh,:ww]=big
        col=np.repeat(self.grad[None,:,:],1,0); bigc=np.repeat(np.repeat(np.broadcast_to(col,(V.shape[0],self.cols,3)),self.cs,0),self.cs,1)
        canvas_c=np.zeros((h,w,3),np.float32); canvas_c[:hh,:ww]=bigc[:hh,:ww]
        c.set_source_surface(to_surface_premul(np.minimum(canvas_c*0.9+0.15,1),canvas_a)); c.paint()
        bits=format(rule,'08b')
        c.set_source_rgba(*PAL['bg'],0.8); c.rectangle(w*0.86-190*S,h*0.08,380*S,150*S); c.fill()
        text_glow(c,f'Rule {rule}',w*0.86,h*0.13,48*S,PAL['gold'],fade)
        text(c,bits,w*0.86,h*0.19,30*S,(1,1,1),0.8*fade,font='Noto Sans Mono')

class Pendulum(Scene):
    name='double_pendulum'; dur=24.0; title='Chaos: the double pendulum'; sub='Six pendulums start 0.0001 rad apart — within seconds they disagree completely'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; g=9.81; L1=L2=1.0; m1=m2=1.0
        def deriv(s):
            t1,w1,t2,w2=s; d=t2-t1; den1=(m1+m2)*L1-m2*L1*math.cos(d)**2; den2=(L2/L1)*den1
            a1=(m2*L1*w1*w1*math.sin(d)*math.cos(d)+m2*g*math.sin(t2)*math.cos(d)+m2*L2*w2*w2*math.sin(d)-(m1+m2)*g*math.sin(t1))/den1
            a2=(-m2*L2*w2*w2*math.sin(d)*math.cos(d)+(m1+m2)*(g*math.sin(t1)*math.cos(d)-L1*w1*w1*math.sin(d)-g*math.sin(t2)))/den2
            return np.array([w1,a1,w2,a2])
        self.K=6; F=int(self.dur*self.fps)+2; sub=24; dt=1/(self.fps*sub)*1.0
        self.traj=np.zeros((self.K,F,4))
        for k in range(self.K):
            s=np.array([math.radians(120)+k*1e-4,0.0,math.radians(-10),0.0])
            for f in range(F):
                self.traj[k,f]=s
                for _ in range(sub):
                    k1=deriv(s); k2=deriv(s+dt/2*k1); k3=deriv(s+dt/2*k2); k4=deriv(s+dt*k3); s=s+dt/6*(k1+2*k2+2*k3+k4)
        self.L=230*S; self.px,self.py=w*0.5,h*0.40
        self.colsk=[PAL['pink'],PAL['orange'],PAL['gold'],PAL['green'],PAL['blue'],PAL['purple']]
    def pos(self,k,f):
        t1,_,t2,_=self.traj[k,f]; x1=self.px+self.L*math.sin(t1); y1=self.py+self.L*math.cos(t1)
        return x1,y1,x1+self.L*math.sin(t2),y1+self.L*math.cos(t2)
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; f=int(t*self.fps); TR=75
        for k in range(self.K):
            col=self.colsk[k]
            for seg in range(0,TR,5):
                a0=max(0,f-TR+seg); a1=min(f,a0+6)
                if a1<=a0: continue
                c.new_path()
                for q in range(a0,a1+1):
                    _,_,x2,y2=self.pos(k,q); (c.move_to if q==a0 else c.line_to)(x2,y2)
                al=(seg+5)/TR
                c.set_source_rgba(*col,0.85*al); c.set_line_width(2.6*S*al+0.6); c.stroke()
        for k in range(self.K):
            x1,y1,x2,y2=self.pos(k,f); col=self.colsk[k]
            c.set_source_rgba(1,1,1,0.35); c.set_line_width(2.5*S); c.move_to(self.px,self.py); c.line_to(x1,y1); c.line_to(x2,y2); c.stroke()
            glow_dot(c,x1,y1,6*S,col,0.8); glow_dot(c,x2,y2,9*S,col)
        c.set_source_rgba(1,1,1,0.9); c.arc(self.px,self.py,6*S,0,TAU); c.fill()
        text(c,f't = {t:4.1f} s',w*0.86,h*0.13,34*S,(1,1,1),0.85)
        spread=np.std([self.traj[k,f,2] for k in range(self.K)])
        text(c,f'spread = {math.degrees(spread):6.2f}°',w*0.86,h*0.185,30*S,PAL['pink'],0.85,bold=False)

class Lorenz(Scene):
    name='lorenz'; dur=26.0; title='The Lorenz attractor'; sub='Three simple equations for weather — and the butterfly of chaos appears'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; self.K=8; F=int(self.dur*self.fps)+2; sub=6; dt=0.0021
        s0=np.array([1.0,1.0,20.0]); self.P=np.zeros((self.K,F,3))
        def d(v): x,y,z=v[...,0],v[...,1],v[...,2]; return np.stack([10*(y-x),x*(28-z)-y,x*y-8/3*z],-1)
        v=np.array([s0+np.array([k*0.03,0,0]) for k in range(self.K)])
        for f in range(F):
            self.P[:,f]=v
            for _ in range(sub):
                k1=d(v); k2=d(v+dt/2*k1); k3=d(v+dt/2*k2); k4=d(v+dt*k3); v=v+dt/6*(k1+2*k2+2*k3+k4)
        self.cols=[PAL['pink'],PAL['orange'],PAL['gold'],PAL['olive'],PAL['green'],PAL['blue'],PAL['purple'],PAL['red']]
        self.sc=17*S
    def proj(self,p,ang):
        x,y,z=p[...,0],p[...,1],p[...,2]; xr=x*math.cos(ang)-y*math.sin(ang); dep=x*math.sin(ang)+y*math.cos(ang)
        return self.w*0.5+xr*self.sc, self.h*0.53-(z-25)*self.sc, dep
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; f=int(t*self.fps); ang=0.6+TAU*t/self.dur*0.6; TR=420
        for k in range(self.K):
            a0=max(0,f-TR); seg=self.P[k,a0:f+1]
            if len(seg)<2: continue
            X,Y,D=self.proj(seg,ang); col=self.cols[k]; n=len(X)
            for ch in range(0,n-1,30):
                e=min(n,ch+31); al=(ch+30)/n if n>30 else 1
                c.new_path(); c.move_to(X[ch],Y[ch])
                for q in range(ch+1,e): c.line_to(X[q],Y[q])
                c.set_source_rgba(*col,0.15*al); c.set_line_width(6*S); c.stroke_preserve()
                c.set_source_rgba(*mix(col,(1,1,1),0.3),0.8*al); c.set_line_width(1.8*S); c.stroke()
            glow_dot(c,X[-1],Y[-1],5*S,col)
        text(c,'dx/dt = 10(y − x)',w*0.86,h*0.12,28*S,(1,1,1),0.8,bold=False)
        text(c,'dy/dt = x(28 − z) − y',w*0.86,h*0.165,28*S,(1,1,1),0.8,bold=False)
        text(c,'dz/dt = xy − 8z/3',w*0.86,h*0.21,28*S,(1,1,1),0.8,bold=False)

SCENES=[ChaosGame,Galton,Primes,MonteCarlo,Life,RuleCA,Pendulum,Lorenz]
if __name__=='__main__':
    args=sys.argv[1:]; mode='render'
    if args and args[0]=='preview': mode='preview'; args=args[1:]
    for cls in SCENES:
        if args and cls.name not in args: continue
        sc=cls()
        if mode=='preview': print(preview(sc,[1.0,3.0,sc.dur*0.3,sc.dur*0.5,sc.dur*0.7,sc.dur*0.9]),flush=True)
        else: render(sc)
