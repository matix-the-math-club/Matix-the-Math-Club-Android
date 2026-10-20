from gallery import *

def resample(pts,n):
    P=np.array(pts,float); seg=np.sqrt(((np.diff(P,axis=0))**2).sum(1)); L=np.concatenate([[0],np.cumsum(seg)])
    u=np.linspace(0,L[-1],n,endpoint=False); x=np.interp(u,L,P[:,0]); y=np.interp(u,L,P[:,1]); return x+1j*y

def glyph_contour(ch,size=600):
    s=cairo.ImageSurface(cairo.FORMAT_ARGB32,10,10); c=cairo.Context(s)
    c.select_font_face('Noto Sans',cairo.FONT_SLANT_NORMAL,cairo.FONT_WEIGHT_BOLD); c.set_font_size(size)
    c.move_to(0,0); c.text_path(ch); path=c.copy_path_flat(); subs=[]; cur=[]
    for kind,pts in path:
        if kind==cairo.PATH_MOVE_TO:
            if cur: subs.append(cur)
            cur=[pts]
        elif kind==cairo.PATH_LINE_TO: cur.append(pts)
        elif kind==cairo.PATH_CLOSE_PATH:
            if cur: cur.append(cur[0]); subs.append(cur); cur=[]
    if cur: subs.append(cur)
    best=max(subs,key=lambda q:len(q)); P=np.array(best); P-=P.mean(0); return [tuple(p) for p in P]

class Fourier(Scene):
    name='fourier'; dur=27.0; title='Fourier epicycles'; sub='Any drawing is just circles spinning on circles'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; N=512
        heart=[(16*math.sin(t)**3*22, -(13*math.cos(t)-5*math.cos(2*t)-2*math.cos(3*t)-math.cos(4*t))*22) for t in np.linspace(0,TAU,800)]
        star=[]
        for i in range(11):
            r=330 if i%2==0 else 140; a=-TAU/4+math.pi*i/5; star.append((r*math.cos(a),r*math.sin(a)))
        shapes=[glyph_contour('π',760),heart,star,glyph_contour('Ω',720)]
        self.shapes=[]
        for sh in shapes:
            z=resample(sh,N)*S*0.95; X=np.fft.fft(z)/N; k=np.fft.fftfreq(N,1/N)
            order=np.argsort(-np.abs(X)); self.shapes.append((X[order],k[order]))
        self.seg=self.dur/4; self.cols=[PAL['pink'],PAL['green'],PAL['gold'],PAL['blue']]
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; i=min(3,int(t//self.seg)); lt=t-i*self.seg; X,k=self.shapes[i]
        draw_t=self.seg-1.4; u=clamp(lt/draw_t); fade=1-smooth((lt-(self.seg-0.7))/0.6)
        cx,cy=w*0.58,h*0.47; M=len(X); nshow=150
        def pos(uu,upto=None):
            terms=X[:upto]*np.exp(1j*TAU*k[:upto]*uu); return np.cumsum(terms)
        # trace
        us=np.linspace(0,u,max(2,int(u*900)))
        tr=np.array([pos(uu)[-1] for uu in us])
        c.new_path()
        for j,z in enumerate(tr): (c.move_to if j==0 else c.line_to)(cx+z.real,cy+z.imag)
        glow_stroke(c,self.cols[i],3.2*S,0.95*fade)
        # circles + arms
        chain=pos(u,nshow); prev=0j; c.set_line_width(1.1*S)
        for j in range(min(nshow,M)):
            r=abs(X[j]); z=chain[j]
            if r*1.0>0.6 and j<60:
                c.set_source_rgba(1,1,1,0.22*fade); c.arc(cx+prev.real,cy+prev.imag,r,0,TAU); c.stroke()
            c.set_source_rgba(1,1,1,(0.75 if j<40 else 0.3)*fade); c.move_to(cx+prev.real,cy+prev.imag); c.line_to(cx+z.real,cy+z.imag); c.stroke()
            prev=z
        glow_dot(c,cx+prev.real,cy+prev.imag,5*S,self.cols[i],fade)
        text(c,f'{min(nshow,M)} circles',w*0.86,h*0.12,26*S,(1,1,1),0.55*fade,bold=False)
        text(c,'z(t) = Σ cₖ·e^(2πikt)',w*0.86,h*0.17,30*S,mix(self.cols[i],(1,1,1),0.4),0.8*fade)

class Lissajous(Scene):
    name='lissajous'; dur=24.0; title='Lissajous table'; sub='x = sin(a·t),  y = sin(b·t) — every pair of frequencies makes its own curve'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; self.cell=176*S; self.nx,self.ny=8,4
        self.ox=(w-(self.nx+1)*self.cell)/2; self.oy=(h-(self.ny+1)*self.cell)/2-30*S
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; cell=self.cell; r=cell*0.38; per=12.0
        loop=int(t//per); lt=t-loop*per; ang=TAU*lt/per*1.0-TAU/4
        fade=1-smooth((lt-(per-0.8))/0.7)
        cols=[PAL['pink'],PAL['purple'],PAL['blue'],PAL['green'],PAL['olive'],PAL['orange'],PAL['gold'],PAL['red']]
        cxs=[self.ox+cell*(i+1.5) for i in range(self.nx)]; cys=[self.oy+cell*(j+1.5) for j in range(self.ny)]
        hx=self.ox+cell*0.5; hy=self.oy+cell*0.5
        c.set_line_width(1.6*S)
        for i,cx in enumerate(cxs):
            a=ang*(i+1); px,py=cx+r*math.cos(a),hy+r*math.sin(a)
            c.new_path(); c.arc(cx,hy,r,0,TAU); glow_stroke(c,cols[i],1.5*S,0.6)
            c.set_source_rgba(1,1,1,0.10); c.move_to(px,py); c.line_to(px,h); c.stroke()
            glow_dot(c,px,py,4.5*S,cols[i])
            text(c,str(i+1),cx,hy,22*S,(1,1,1),0.35)
        for j,cy in enumerate(cys):
            a=ang*(j+1); px,py=hx+r*math.cos(a),cy+r*math.sin(a)
            c.new_path(); c.arc(hx,cy,r,0,TAU); glow_stroke(c,cols[(j+3)%8],1.5*S,0.6)
            c.set_source_rgba(1,1,1,0.10); c.move_to(px,py); c.line_to(w,py); c.stroke()
            glow_dot(c,px,py,4.5*S,cols[(j+3)%8])
            text(c,str(j+1),hx,cy,22*S,(1,1,1),0.35)
        steps=max(2,int((lt/per)*360))
        for j,cy in enumerate(cys):
            for i,cx in enumerate(cxs):
                c.new_path()
                for q in range(steps+1):
                    a=-TAU/4+TAU*(q/360)
                    x=cx+r*math.cos(a*(i+1)); y=cy+r*math.sin(a*(j+1))
                    (c.move_to if q==0 else c.line_to)(x,y)
                col=mix(cols[i],cols[(j+3)%8],0.5)
                glow_stroke(c,col,1.5*S,0.85*fade,passes=((3,0.12),(1,0.9)))
                a=ang; glow_dot(c,cx+r*math.cos(a*(i+1)),cy+r*math.sin(a*(j+1)),3*S,(1,1,1),fade)

class UnitCircle(Scene):
    name='unit_circle'; dur=18.0; title='Sine & cosine'; sub='Walk round the unit circle — your height draws the sine wave'
    def setup(self,w,h): self.w,self.h=w,h
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; cx,cy=430*S*(w/h)/(16/9),h*0.52; R=240*S; per=6.0
        th=TAU*t/per; x0=cx+R*1.55; wav=w-x0-90*S
        # axes
        c.set_line_width(1.5*S); c.set_source_rgba(1,1,1,0.18)
        c.move_to(cx-R*1.3,cy); c.line_to(w-60*S,cy); c.move_to(cx,cy-R*1.3); c.line_to(cx,cy+R*1.3); c.stroke()
        for k in range(1,5):
            x=x0+wav*k/4; c.move_to(x,cy-8*S); c.line_to(x,cy+8*S); c.stroke()
            text(c,['π/2','π','3π/2','2π'][k-1],x,cy+30*S,22*S,(1,1,1),0.45,bold=False)
        c.new_path(); c.arc(cx,cy,R,0,TAU); glow_stroke(c,(1,1,1),1.6*S,0.5)
        px,py=cx+R*math.cos(th),cy-R*math.sin(th)
        # angle arc
        c.new_path(); c.arc_negative(cx,cy,R*0.22,0,-(th%TAU)); glow_stroke(c,PAL['gold'],2*S,0.9)
        # triangle
        c.set_line_width(3*S)
        c.set_source_rgba(*PAL['blue'],0.95); c.move_to(cx,cy); c.line_to(px,cy); c.stroke()
        c.set_source_rgba(*PAL['pink'],0.95); c.move_to(px,cy); c.line_to(px,py); c.stroke()
        c.move_to(cx,cy); c.line_to(px,py); glow_stroke(c,(1,1,1),2.5*S,0.9)
        # projection line to the wave
        c.set_source_rgba(*PAL['pink'],0.35); c.set_line_width(1.5*S); c.set_dash([6*S,6*S]); c.move_to(px,py); c.line_to(x0,py); c.stroke(); c.set_dash([])
        # sine wave (history scrolls)
        c.new_path(); n=400
        for q in range(n+1):
            u=q/n; a=th-TAU*u  # oldest at right
            x=x0+wav*u; y=cy-R*math.sin(a)
            (c.move_to if q==0 else c.line_to)(x,y)
        glow_stroke(c,PAL['pink'],3*S,0.95)
        c.new_path()
        for q in range(n+1):
            u=q/n; a=th-TAU*u; x=x0+wav*u; y=cy-R*math.cos(a)
            (c.move_to if q==0 else c.line_to)(x,y)
        glow_stroke(c,PAL['blue'],2*S,0.45)
        glow_dot(c,px,py,7*S,(1,1,1)); glow_dot(c,x0,py,7*S,PAL['pink'])
        deg=math.degrees(th)%360
        text(c,f'θ = {deg:5.1f}°',cx,70*S,34*S,PAL['gold'],0.95)
        text(c,f'sin θ = {math.sin(th):+.3f}',cx,116*S,28*S,PAL['pink'],0.95,bold=False)
        text(c,f'cos θ = {math.cos(th):+.3f}',cx,156*S,28*S,PAL['blue'],0.95,bold=False)

class Pythagoras(Scene):
    name='pythagoras'; dur=22.0; title='Pythagoras, proved by moving'; sub='Same big square, same 4 triangles — so the leftover areas must match'
    def setup(self,w,h):
        self.w,self.h=w,h; S=h/1080; a,b=3,4; self.a,self.b=a,b; Sd=a+b; self.u=600*S/Sd; self.ox=w*0.30-Sd*self.u/2; self.oy=h*0.46-Sd*self.u/2
        T1=[(0,0),(a,0),(0,b)]; T2=[(Sd,0),(Sd,a),(a,0)]; T3=[(Sd,Sd),(Sd-a,Sd),(Sd,a)]; T4=[(0,Sd),(0,b),(b,Sd)]
        U1=[(0,a),(a,a),(0,Sd)]; U2=T2; U3=[(a,Sd),(0,Sd),(a,a)]; U4=[(a,a),(a,0),(Sd,a)]
        self.A=[T1,T2,T3,T4]; self.B=[U1,U2,U3,U4]; self.cols=[PAL['pink'],PAL['purple'],PAL['blue'],PAL['orange']]
    def P(self,x,y): return self.ox+x*self.u,self.oy+y*self.u
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; a,b=self.a,self.b; Sd=a+b
        # timeline: 0-3 build, 3-6 show c², 6-9 move -> a²+b², 9-12 show, 12-15 back, 15-18 forward, 18-22 equation
        def m(t0,t1): return ease_in_out((t-t0)/(t1-t0))
        k = m(6,9) - m(12,15) + m(15,18)
        build=smooth(t/2.5)
        x0,y0=self.P(0,0); x1,y1=self.P(Sd,Sd)
        c.set_line_width(3*S); c.rectangle(x0,y0,x1-x0,y1-y0); glow_stroke(c,(1,1,1),2.4*S,0.7*build)
        # leftover areas
        ca=(1-k)*smooth((t-2.5)/1.0); ab=k
        if ca>0.01:
            pts=[self.P(a,0),self.P(Sd,a),self.P(Sd-a,Sd),self.P(0,Sd-a)]
            c.new_path(); c.move_to(*pts[0]); [c.line_to(*p) for p in pts[1:]]; c.close_path()
            c.set_source_rgba(*PAL['green'],0.22*ca); c.fill_preserve(); glow_stroke(c,PAL['green'],2*S,0.9*ca)
            cxx,cyy=self.P(Sd/2,Sd/2); text_glow(c,'c²',cxx,cyy,96*S,PAL['green'],ca)
        if ab>0.01:
            for (xx,yy,s,lab,col) in ((0,0,a,'a²',PAL['gold']),(a,a,b,'b²',PAL['blue'])):
                p0=self.P(xx,yy); p1=self.P(xx+s,yy+s)
                c.rectangle(p0[0],p0[1],p1[0]-p0[0],p1[1]-p0[1]); c.set_source_rgba(*col,0.22*ab); c.fill_preserve(); glow_stroke(c,col,2*S,0.9*ab)
                text_glow(c,lab,(p0[0]+p1[0])/2,(p0[1]+p1[1])/2,(70 if s==a else 96)*S,col,ab)
        for i,(TA,TB) in enumerate(zip(self.A,self.B)):
            kk=clamp(k*1.3-i*0.1) if 0<k<1 else k
            kk=ease_in_out(kk)
            pts=[self.P(lerp(pa[0],pb[0],kk),lerp(pa[1],pb[1],kk)) for pa,pb in zip(TA,TB)]
            app=smooth((t-0.3-i*0.4)/0.8)
            c.new_path(); c.move_to(*pts[0]); c.line_to(*pts[1]); c.line_to(*pts[2]); c.close_path()
            c.set_source_rgba(*self.cols[i],0.55*app); c.fill_preserve(); c.set_source_rgba(*mix(self.cols[i],(1,1,1),0.5),0.9*app); c.set_line_width(2*S); c.stroke()
        # right side explanation
        rx=w*0.70; s1=smooth((t-1)/1)
        text(c,'4 triangles + c²',rx,h*0.30,46*S,PAL['green'],s1*(1-k*0.6))
        text(c,'=',rx,h*0.38,46*S,(1,1,1),s1)
        text(c,'4 triangles + a² + b²',rx,h*0.46,46*S,PAL['gold'],smooth((t-8)/1)*(0.4+0.6*k))
        e=smooth((t-18)/1.2)
        if e>0:
            text_glow(c,'a² + b² = c²',rx,h*0.64,92*S,(1,1,1),e)
            text(c,'3² + 4² = 9 + 16 = 25 = 5²',rx,h*0.73,32*S,(0.85,0.85,0.9),e,bold=False)

class Phyllotaxis(Scene):
    name='phyllotaxis'; dur=22.0; title='The golden angle'; sub='Turn 137.5° between seeds and a sunflower appears — Fibonacci spirals for free'
    def setup(self,w,h): self.w,self.h=w,h
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; cx,cy=w*0.5,h*0.48; GA=math.radians(137.50776405)
        n=int(2400*ease_in_out(t/15.0)); rot=t*0.05; scale=12.0*S
        hl=smooth((t-15)/2.5)
        for i in range(1,n+1):
            r=scale*math.sqrt(i); a=i*GA+rot
            x,y=cx+r*math.cos(a),cy+r*math.sin(a)
            age=clamp((n-i)/120)
            arm=i%34; arm2=i%21
            base=CLUB[(i//7)%6] if hl<1 else None
            col=mix(mix(PAL['gold'],PAL['orange'],(i%13)/13),PAL['pink'] if arm%2==0 else PAL['green'],hl*0.85) if True else base
            sz=(2.2+2.6*math.sqrt(i)/50)*S*(0.6+0.4*ease_back(age))
            c.set_source_rgba(*col,0.35+0.6*age); c.arc(x,y,sz,0,TAU); c.fill()
        if n>0:
            r=scale*math.sqrt(n); a=n*GA+rot; glow_dot(c,cx+r*math.cos(a),cy+r*math.sin(a),6*S,(1,1,1))
        text(c,f'n = {n}',w*0.86,h*0.14,34*S,(1,1,1),0.8)
        text(c,'θ = n × 137.5°',w*0.86,h*0.20,30*S,PAL['gold'],0.8,bold=False)
        text(c,'r = √n',w*0.86,h*0.255,30*S,PAL['gold'],0.8,bold=False)
        if hl>0:
            text(c,'34 spirals one way · 21 the other',w*0.5,h*0.93,30*S,(1,1,1),hl)

class Cardioid(Scene):
    name='cardioid'; dur=26.0; title='Times tables on a circle'; sub='Join n to k·n (mod 200): k = 2 makes a cardioid, then it keeps morphing'
    def setup(self,w,h): self.w,self.h=w,h
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; cx,cy=w*0.5,h*0.48; R=430*S; N=200
        k=2+ (t/self.dur)**1.15*49
        c.set_line_width(max(1.25*S,1.1))
        for i in range(N):
            a1=TAU*i/N-math.pi; a2=TAU*((i*k)%N)/N-math.pi
            col=hsv(0.88+i/N*0.75,0.65,1.0)
            c.set_source_rgba(*col,0.55); c.move_to(cx+R*math.cos(a1),cy+R*math.sin(a1)); c.line_to(cx+R*math.cos(a2),cy+R*math.sin(a2)); c.stroke()
        c.new_path(); c.arc(cx,cy,R,0,TAU); glow_stroke(c,(1,1,1),1.5*S,0.4)
        text_glow(c,f'k = {k:.2f}',w*0.86,h*0.14,44*S,PAL['gold'],0.95)

class Tesseract(Scene):
    name='tesseract'; dur=20.0; title='A 4-D hypercube'; sub='A tesseract turning through the fourth dimension, projected down to your screen'; loop=False
    def setup(self,w,h):
        self.w,self.h=w,h; import itertools
        self.V=np.array(list(itertools.product((-1,1),repeat=4)),float)
        self.E=[(i,j) for i in range(16) for j in range(i+1,16) if np.sum(self.V[i]!=self.V[j])==1]
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080; a=TAU*t/self.dur*2; b=TAU*t/self.dur
        def rot(i,j,th):
            M=np.eye(4); M[i,i]=M[j,j]=math.cos(th); M[i,j]=-math.sin(th); M[j,i]=math.sin(th); return M
        M=rot(0,3,a)@rot(1,2,b*0.5)@rot(2,3,b)@rot(0,1,0.3)
        P=self.V@M.T
        d4=3.0; k4=1/(d4-P[:,3]); P3=P[:,:3]*k4[:,None]
        d3=4.0; k3=1/(d3-P3[:,2]); P2=P3[:,:2]*k3[:,None]
        sc=1350*S; xs=w*0.5+P2[:,0]*sc; ys=h*0.47+P2[:,1]*sc
        for (i,j) in self.E:
            depth=(P[i,3]+P[j,3])/2; col=mix(PAL['blue'],PAL['pink'],clamp(depth*0.5+0.5))
            c.move_to(xs[i],ys[i]); c.line_to(xs[j],ys[j]); glow_stroke(c,col,3*S,0.85)
        for i in range(16):
            glow_dot(c,xs[i],ys[i],(5+4*clamp(P[i,3]*0.5+0.5))*S,mix(PAL['blue'],PAL['pink'],clamp(P[i,3]*0.5+0.5)))
        text(c,'16 corners · 32 edges · 24 squares · 8 cubes',w*0.5,h*0.92,30*S,(1,1,1),0.75,bold=False)

class KochHilbert(Scene):
    name='koch_hilbert'; dur=26.0; title='Fractal curves'; sub='Koch snowflake: infinite edge, finite area. Hilbert curve: a line that fills a square'
    def setup(self,w,h):
        self.w,self.h=w,h
        def hil(order):
            def d2xy(n,d):
                x=y=0; s=1; tt=d
                while s<n:
                    rx=1&(tt//2); ry=1&(tt^rx)
                    if ry==0:
                        if rx==1: x=s-1-x; y=s-1-y
                        x,y=y,x
                    x+=s*rx; y+=s*ry; tt//=4; s*=2
                return x,y
            n=2**order; return [d2xy(n,d) for d in range(n*n)],n
        self.H=[hil(o) for o in range(1,7)]
    def koch(self,pts,depth,bump):
        for _ in range(depth):
            new=[]
            for i in range(len(pts)-1):
                (x1,y1),(x2,y2)=pts[i],pts[i+1]; dx,dy=(x2-x1)/3,(y2-y1)/3
                a=(x1+dx,y1+dy); b=(x1+2*dx,y1+2*dy); mx,my=(x1+x2)/2,(y1+y2)/2
                px,py=mx+dy*math.sqrt(3)/2*1.0*(-1)*(-1), my-dx*math.sqrt(3)/2
                # outward peak (counter-clockwise polygon)
                px=mx-(dy)*math.sqrt(3)/2; py=my+(dx)*math.sqrt(3)/2
                new+= [pts[i],a,(lerp(mx,px,1),lerp(my,py,1)),b]
            new.append(pts[-1]); pts=new
        return pts
    def draw(self,c,t):
        w,h=self.w,self.h; S=h/1080
        if t<13:
            cx,cy=w*0.5,h*0.50; R=380*S
            tri=[(cx+R*math.cos(-TAU/4-TAU*i/3),cy+R*math.sin(-TAU/4-TAU*i/3)) for i in range(4)]
            lev=min(5,t/2.2); d=int(lev); fr=ease_in_out(lev-d)
            base=self.koch(tri,d,1)
            if d<5 and fr>0:
                new=[]
                for i in range(len(base)-1):
                    (x1,y1),(x2,y2)=base[i],base[i+1]; dx,dy=(x2-x1)/3,(y2-y1)/3; mx,my=(x1+x2)/2,(y1+y2)/2
                    px=mx-dy*math.sqrt(3)/2; py=my+dx*math.sqrt(3)/2
                    new+=[base[i],(x1+dx,y1+dy),(lerp(mx,px,fr),lerp(my,py,fr)),(x1+2*dx,y1+2*dy)]
                new.append(base[-1]); base=new
            c.new_path(); c.move_to(*base[0]); [c.line_to(*p) for p in base[1:]]; c.close_path()
            fo=1-smooth((t-12.2)/0.8)
            c.set_source_rgba(*PAL['blue'],0.10*fo); c.fill_preserve(); glow_stroke(c,PAL['blue'],2.2*S,0.95*fo)
            text(c,f'step {min(5,int(lev))}',w*0.86,h*0.14,36*S,(1,1,1),0.8*fo)
            text(c,f'{3*4**min(5,int(lev))} edges',w*0.86,h*0.20,30*S,PAL['blue'],0.8*fo,bold=False)
        else:
            lt=t-13; o=min(5,int(lt/2.0)); fr=clamp((lt-o*2.0)/1.6)
            if lt>=12: o=5; fr=1
            pts,n=self.H[o]; side=820*S; cell=side/n; ox=w*0.5-side/2+cell/2; oy=h*0.5-side/2+cell/2
            m=max(2,int(len(pts)*ease_in_out(fr)))
            fo=1-smooth((lt-12.2)/0.8)
            c.new_path()
            for i in range(m):
                x,y=pts[i]; (c.move_to if i==0 else c.line_to)(ox+x*cell,oy+(n-1-y)*cell)
            lw=max(1.4,6-o)*S
            glow_stroke(c,PAL['green'],lw,0.95*fo,passes=((4,0.08),(2,0.2),(1,1.0)))
            x,y=pts[m-1]; glow_dot(c,ox+x*cell,oy+(n-1-y)*cell,6*S,(1,1,1),fo)
            text(c,f'order {o+1}',w*0.86,h*0.14,36*S,(1,1,1),0.8*fo)
            text(c,f'{len(pts)} cells visited',w*0.86,h*0.20,30*S,PAL['green'],0.8*fo,bold=False)

SCENES=[Fourier,Lissajous,UnitCircle,Pythagoras,Phyllotaxis,Cardioid,Tesseract,KochHilbert]
if __name__=='__main__':
    args=sys.argv[1:]
    mode='render'
    if args and args[0]=='preview': mode='preview'; args=args[1:]
    for cls in SCENES:
        if args and cls.name not in args: continue
        sc=cls()
        if mode=='preview': print(preview(sc,[1.0,3.0,sc.dur*0.3,sc.dur*0.5,sc.dur*0.7,sc.dur*0.9]))
        else: render(sc)
