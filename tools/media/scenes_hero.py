"""Hero-card loops: 1920x480, 10 s, seamless, light-on-black (shown with mix-blend-mode:screen)."""
import sys, math, random, cairo, time
sys.path.insert(0,'.')
from vkit import *
W,H,FPS,T=1920,480,30,10.0
N=int(T*FPS)
WH=(1,1,1)
def new(name,crf=19):
    v=Video(f'./out/hero/hero_{name}.mp4',W,H,FPS,crf=crf); return v,v.ctx
def clear(c): c.set_source_rgb(0,0,0); c.paint()
def fade_edges(c):
    # soften the left side where the card text sits
    g=cairo.LinearGradient(0,0,W*0.55,0); g.add_color_stop_rgba(0,0,0,0,0.78); g.add_color_stop_rgba(1,0,0,0,0)
    c.set_source(g); c.rectangle(0,0,W*0.55,H); c.fill()

def hero_games():
    v,c=new('games'); cell=24; cols,rows=W//cell+1,H//cell+1
    import numpy as np
    g=np.zeros((rows,cols),np.uint8)
    def put(pat,ox,oy):
        for y,line in enumerate(pat):
            for x,ch in enumerate(line):
                if ch=='O' and 0<=oy+y<rows and 0<=ox+x<cols: g[oy+y,ox+x]=1
    pulsar=["..OOO...OOO..",".............","O....O.O....O","O....O.O....O","O....O.O....O","..OOO...OOO..",".............","..OOO...OOO..","O....O.O....O","O....O.O....O","O....O.O....O",".............","..OOO...OOO.."]
    penta=["..O....O..","OO.OOOO.OO","..O....O.."]
    blinker=["OOO"]; toad=[".OOO","OOO."]; beacon=["OO..","OO..","..OO","..OO"]
    for pat,x,y in ((pulsar,46,4),(penta,64,9),(blinker,30,4),(blinker,34,15),(blinker,60,2),(blinker,77,16),(toad,24,9),(toad,72,3),(beacon,38,8),(beacon,26,15)): put(pat,x,y)
    def step(a):
        n=sum(np.roll(np.roll(a,i,0),j,1) for i in (-1,0,1) for j in (-1,0,1) if (i,j)!=(0,0))
        return ((n==3)|((a==1)&(n==2))).astype(np.uint8)
    gens=[g]
    for i in range(30): gens.append(step(gens[-1]))
    assert (gens[30]==gens[0]).all(), 'not periodic'
    for f in range(N):
        clear(c); gi=f//10; fr=(f%10)/10.0
        A=gens[gi]; B=gens[(gi+1)%30]
        for y in range(rows):
            for x in range(cols):
                a=A[y,x]; b=B[y,x]
                if not a and not b:
                    continue
                k=a*(1-smooth(fr*1.6))+b*smooth(fr*1.6) if a!=b else 1.0
                if k<=0.01: continue
                col=CLUB[(x*7+y*3)//9%6]
                s=cell*(0.25+0.55*k); cx,cy=x*cell+cell/2,y*cell+cell/2
                c.set_source_rgba(*mix(col,WH,0.5),0.75*k); c.rectangle(cx-s/2,cy-s/2,s,s); c.fill()
                c.set_source_rgba(*col,0.18*k); c.rectangle(cx-s,cy-s,2*s,2*s); c.fill()
        # faint grid
        c.set_source_rgba(1,1,1,0.035); c.set_line_width(1)
        for x in range(0,W,cell): c.move_to(x+.5,0); c.line_to(x+.5,H)
        for y in range(0,H,cell): c.move_to(0,y+.5); c.line_to(W,y+.5)
        c.stroke(); fade_edges(c); v.push()
    return v.close()

def hero_learn():
    v,c=new('learn')
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        # compass & straightedge construction loop: equilateral triangle, circumcircle, hexagon flower
        cx,cy=W*0.72,H*0.5; R=150
        p=(t%T)/T
        stages=[(0.00,0.16),(0.10,0.26),(0.22,0.40),(0.36,0.55),(0.50,0.70),(0.62,0.80)]
        alpha_all=1.0 if p<0.85 else 1-smooth((p-0.85)/0.12)
        def prog(i): a,b=stages[i]; return smooth((p-a)/(b-a))
        c.set_line_width(2.2)
        A=(cx-R*0.5,cy+R*0.43); B=(cx+R*0.5,cy+R*0.43)
        # base segment
        k=prog(0); c.set_source_rgba(1,1,1,0.85*alpha_all); c.move_to(*A); c.line_to(lerp(A[0],B[0],k),B[1]); c.stroke()
        # two circles
        for i,(P,Q) in enumerate(((A,B),(B,A))):
            k=prog(1+i)
            if k>0:
                r=math.dist(P,Q); a0=math.atan2(Q[1]-P[1],Q[0]-P[0])
                c.new_path(); c.arc(P[0],P[1],r,a0,a0+TAU*k); glow_stroke(c,CLUB[i],1.6,0.55*alpha_all)
        Cp=(cx,cy+R*0.43-R*0.866)
        k=prog(3)
        if k>0:
            c.set_source_rgba(1,1,1,0.85*alpha_all); c.move_to(*A); c.line_to(lerp(A[0],Cp[0],k),lerp(A[1],Cp[1],k)); c.stroke()
            c.move_to(*B); c.line_to(lerp(B[0],Cp[0],k),lerp(B[1],Cp[1],k)); c.stroke()
        k=prog(4)
        if k>0:
            G=(cx,(A[1]+B[1]+Cp[1])/3); r=math.dist(G,A)
            c.new_path(); c.arc(G[0],G[1],r,-TAU/4,-TAU/4+TAU*k); glow_stroke(c,PAL['olive'],1.6,0.6*alpha_all)
        k=prog(5)
        if k>0:
            G=(cx,(A[1]+B[1]+Cp[1])/3); r=math.dist(G,A)
            for q in range(6):
                a=TAU*q/6+ph*0.0-TAU/4; px,py=G[0]+r*math.cos(a),G[1]+r*math.sin(a)
                c.new_path(); c.arc(px,py,r,0,TAU*k); glow_stroke(c,CLUB[q],1.0,0.28*alpha_all)
        for P in (A,B,Cp): glow_dot(c,*P,4,WH,0.9*alpha_all*prog(0))
        # floating formulas
        for i,(s,x,y) in enumerate((("a²+b²=c²",0.40,0.25),("πr²",0.92,0.22),("60°",0.52,0.78),("√3/2",0.90,0.80),("Σ",0.34,0.62))):
            text(c,s,W*x+18*math.sin(ph+i),H*y+10*math.cos(ph*1+i*2),34,mix(CLUB[i],WH,0.5),0.55,bold=True)
        fade_edges(c); v.push()
    return v.close()

def hero_ai():
    v,c=new('ai'); rnd=random.Random(3)
    layers=[4,7,9,7,3]; xs=[W*(0.40+0.13*i) for i in range(len(layers))]
    nodes=[[(xs[i],H*(j+1)/(n+1)) for j in range(n)] for i,n in enumerate(layers)]
    edges=[(i,a,b) for i in range(len(layers)-1) for a in range(layers[i]) for b in range(layers[i+1])]
    pulses=[(rnd.randrange(len(edges)),rnd.uniform(0,1),rnd.choice((1,2))) for _ in range(90)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T; c.set_line_width(1.2)
        for (i,a,b) in edges:
            (x1,y1),(x2,y2)=nodes[i][a],nodes[i+1][b]
            c.set_source_rgba(1,1,1,0.07); c.move_to(x1,y1); c.line_to(x2,y2); c.stroke()
        for (e,off,sp) in pulses:
            i,a,b=edges[e]; (x1,y1),(x2,y2)=nodes[i][a],nodes[i+1][b]
            u=(t/T*sp+off)%1.0; x,y=lerp(x1,x2,u),lerp(y1,y2,u)
            glow_dot(c,x,y,2.6,CLUB[e%6],0.9*math.sin(math.pi*u))
        for i,L in enumerate(nodes):
            for j,(x,y) in enumerate(L):
                act=0.5+0.5*math.sin(ph*2-i*1.1+j*0.7)
                glow_dot(c,x,y,7+3*act,mix(PAL['purple'],PAL['pink'],act),0.55+0.45*act,halo=3)
        fade_edges(c); v.push()
    return v.close()

def hero_ideas():
    v,c=new('ideas'); rnd=random.Random(5)
    sparks=[dict(x=rnd.uniform(W*0.3,W),sp=rnd.choice((1,2)),off=rnd.uniform(0,1),sz=rnd.uniform(10,26),col=rnd.choice(CLUB),s=rnd.choice(['+','×','?','!','∑','π','√','∞'])) for _ in range(70)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        cx,cy=W*0.78,H*0.48
        for k in range(24):
            a=TAU*k/24+ph/6; r1=70; r2=150+40*math.sin(ph*2+k*1.7)
            c.move_to(cx+r1*math.cos(a),cy+r1*math.sin(a)); c.line_to(cx+r2*math.cos(a),cy+r2*math.sin(a))
            glow_stroke(c,CLUB[k%6],2.0,0.35)
        glow_dot(c,cx,cy,34+4*math.sin(ph*4),PAL['gold'],0.8,halo=3.2)
        for p in sparks:
            u=(t/T*p['sp']+p['off'])%1.0; y=H+30-u*(H+60); x=p['x']+20*math.sin(ph*p['sp']+p['off']*9)
            text(c,p['s'],x,y,p['sz'],mix(p['col'],WH,0.4),0.75*math.sin(math.pi*u))
        fade_edges(c); v.push()
    return v.close()

def star_path(c,x,y,r,n=5,inner=0.45,rot=0):
    for i in range(2*n+1):
        rr=r if i%2==0 else r*inner; a=rot-TAU/4+math.pi*i/n
        (c.move_to if i==0 else c.line_to)(x+rr*math.cos(a),y+rr*math.sin(a))
    c.close_path()

def hero_points():
    v,c=new('points'); rnd=random.Random(9)
    stars=[dict(x=rnd.uniform(W*0.3,W),y=rnd.uniform(0,H),r=rnd.uniform(5,16),off=rnd.uniform(0,1),k=rnd.choice((1,2,3))) for _ in range(80)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        for s in stars:
            tw=0.5+0.5*math.sin(ph*s['k']+s['off']*TAU)
            star_path(c,s['x'],s['y']+8*math.sin(ph+s['off']*7),s['r']*(0.6+0.4*tw),rot=ph*0.5+s['off'])
            c.set_source_rgba(*mix(PAL['gold'],WH,0.4),0.25+0.6*tw); c.fill()
        cx,cy=W*0.80,H*0.5
        for j,(n,m,r,col) in enumerate(((7,3,170,PAL['gold']),(5,2,110,PAL['orange']))):
            c.new_path(); rot=ph*(1 if j==0 else -1)/n
            pts=[(cx+r*math.cos(rot+TAU*i/n-TAU/4),cy+r*math.sin(rot+TAU*i/n-TAU/4)) for i in range(n)]
            i=0; c.move_to(*pts[0])
            for _ in range(n): i=(i+m)%n; c.line_to(*pts[i])
            c.close_path(); glow_stroke(c,col,2.2,0.6)
        # rising bar chart
        for i in range(8):
            hgt=60+90*(0.5+0.5*math.sin(ph+i*0.8)); x=W*0.40+i*46
            c.set_source_rgba(1,1,1,0.13); c.rectangle(x,H-30-hgt,30,hgt); c.fill()
        fade_edges(c); v.push()
    return v.close()

def hero_messages():
    v,c=new('messages')
    centers=[(W*0.62,H*0.55,0.0),(W*0.86,H*0.35,0.33),(W*0.78,H*0.8,0.66)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        for (x,y,off) in centers:
            for k in range(5):
                u=((t/T)*2+off+k/5)%1.0; r=20+u*360
                c.new_path(); c.arc(x,y,r,0,TAU); glow_stroke(c,CLUB[k],1.6,0.55*(1-u)**1.5)
        # paper plane along lemniscate
        for j in range(2):
            u=ph+j*math.pi; a=W*0.18; px=W*0.74+a*math.cos(u)/(1+math.sin(u)**2); py=H*0.5+a*math.sin(u)*math.cos(u)/(1+math.sin(u)**2)
            du=0.02; qx=W*0.74+a*math.cos(u+du)/(1+math.sin(u+du)**2); qy=H*0.5+a*math.sin(u+du)*math.cos(u+du)/(1+math.sin(u+du)**2)
            ang=math.atan2(qy-py,qx-px)
            c.save(); c.translate(px,py); c.rotate(ang)
            c.move_to(22,0); c.line_to(-16,-13); c.line_to(-8,0); c.line_to(-16,13); c.close_path(); c.set_source_rgba(1,1,1,0.9); c.fill(); c.restore()
            for q in range(40):
                uu=u-q*0.03; x=W*0.74+a*math.cos(uu)/(1+math.sin(uu)**2); y=H*0.5+a*math.sin(uu)*math.cos(uu)/(1+math.sin(uu)**2)
                c.set_source_rgba(1,1,1,0.35*(1-q/40)); c.arc(x,y,2,0,TAU); c.fill()
        fade_edges(c); v.push()
    return v.close()

def bubble(c,x,y,w,h,r=18):
    c.new_path(); c.arc(x+w-r,y+r,r,-TAU/4,0); c.arc(x+w-r,y+h-r,r,0,TAU/4); c.line_to(x+30,y+h); c.line_to(x+12,y+h+18); c.line_to(x+16,y+h); c.arc(x+r,y+h-r,r,TAU/4,TAU/2); c.arc(x+r,y+r,r,TAU/2,3*TAU/4); c.close_path()

def hero_discussions():
    v,c=new('discussions'); rnd=random.Random(12)
    bubs=[dict(x=rnd.uniform(W*0.38,W*0.95),y=rnd.uniform(H*0.1,H*0.7),w=rnd.uniform(110,220),off=rnd.uniform(0,1),col=rnd.choice(CLUB),s=rnd.choice(['2+2=4','why π?','x = 7','√49 = 7','nice!','a/b','∞ > 10⁹','proof?'])) for _ in range(9)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        for b in bubs:
            u=(t/T+b['off'])%1.0; a=math.sin(math.pi*u); y=b['y']+40-80*u
            bubble(c,b['x'],y,b['w'],56); glow_stroke(c,b['col'],1.8,0.6*a)
            c.set_source_rgba(*b['col'],0.08*a); bubble(c,b['x'],y,b['w'],56); c.fill()
            text(c,b['s'],b['x']+b['w']/2,y+28,24,mix(b['col'],WH,0.6),0.85*a)
        for k in range(3):
            x=W*0.5+k*26; y=H*0.85
            c.set_source_rgba(1,1,1,0.25+0.6*max(0,math.sin(ph*4-k*0.9))); c.arc(x,y,7,0,TAU); c.fill()
        fade_edges(c); v.push()
    return v.close()

def hero_users():
    v,c=new('users'); rnd=random.Random(21)
    nodes=[dict(x=rnd.uniform(W*0.3,W),y=rnd.uniform(20,H-20),a=rnd.uniform(10,50),k=rnd.choice((1,2)),ph=rnd.uniform(0,TAU),col=rnd.choice(CLUB),r=rnd.uniform(3,7)) for _ in range(60)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        P=[(n['x']+n['a']*math.cos(ph*n['k']+n['ph']),n['y']+n['a']*0.6*math.sin(ph*n['k']+n['ph'])) for n in nodes]
        c.set_line_width(1.3)
        for i in range(len(P)):
            for j in range(i+1,len(P)):
                d=math.dist(P[i],P[j])
                if d<200: c.set_source_rgba(1,1,1,0.22*(1-d/200)); c.move_to(*P[i]); c.line_to(*P[j]); c.stroke()
        for n,(x,y) in zip(nodes,P):
            glow_dot(c,x,y,n['r'],n['col'],0.9)
            if n['r']>6:  # avatar ring
                c.new_path(); c.arc(x,y,n['r']*2.6,0,TAU); glow_stroke(c,n['col'],1.2,0.5)
        fade_edges(c); v.push()
    return v.close()

def hero_bugs():
    v,c=new('bugs'); rnd=random.Random(30); colw=28
    cols=[dict(x=x,sp=rnd.choice((1,2,3)),off=rnd.uniform(0,1),len=rnd.randint(6,14),seq=[rnd.choice('01') for _ in range(40)]) for x in range(int(W*0.3),W,colw)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        for cl in cols:
            head=((t/T*cl['sp']+cl['off'])%1.0)*(H+cl['len']*26)
            for k in range(cl['len']):
                y=head-k*26
                if -20<y<H+20:
                    a=(1-k/cl['len'])
                    col=PAL['green'] if k else WH
                    text(c,cl['seq'][(k+int(cl['off']*40))%40],cl['x'],y,22,col,0.85*a,font='Noto Sans Mono')
        # magnifier
        cx,cy=W*0.78+60*math.cos(ph),H*0.5+40*math.sin(2*ph)
        c.new_path(); c.arc(cx,cy,62,0,TAU); glow_stroke(c,PAL['orange'],4,0.9)
        c.move_to(cx+44,cy+44); c.line_to(cx+100,cy+100); glow_stroke(c,PAL['orange'],8,0.9)
        fade_edges(c); v.push()
    return v.close()

def hero_changelog():
    v,c=new('changelog')
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        # commit graph scrolling left, period = spacing*k
        sp=120; off=(t/T)*sp*4
        lanes=[H*0.3,H*0.5,H*0.7]
        for li,y in enumerate(lanes):
            c.set_line_width(3); c.set_source_rgba(*CLUB[li*2],0.5); c.move_to(W*0.3,y); c.line_to(W,y); c.stroke()
        for i in range(-2,20):
            x=W*0.3+i*sp-off%sp
            idx=i+int(off//sp)
            li=idx%3
            y=lanes[li]
            if idx%4==0:
                c.set_line_width(2.5); c.set_source_rgba(1,1,1,0.4); c.move_to(x,y); c.curve_to(x+sp*0.5,y,x+sp*0.5,lanes[(li+1)%3],x+sp,lanes[(li+1)%3]); c.stroke()
            glow_dot(c,x,y,8,CLUB[(idx)%6],0.9,halo=3)
            text(c,f'v1.{idx%10}',x,y-28,18,(1,1,1),0.5,font='Noto Sans Mono')
        fade_edges(c); v.push()
    return v.close()

def hero_private():
    v,c=new('private')
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T; cx,cy=W*0.78,H*0.5
        for k,(r,n,col,d) in enumerate(((70,12,PAL['purple'],1),(115,24,PAL['blue'],-1),(160,36,PAL['pink'],1),(205,48,PAL['green'],-1))):
            for i in range(n):
                a=TAU*i/n+d*ph/(k+2)*(1 if k%2 else 1)
                if (i%3)!=0: continue
                c.new_path(); c.arc(cx,cy,r,a,a+TAU/n*2.2); glow_stroke(c,col,3,0.55)
        # keyhole
        c.set_source_rgba(1,1,1,0.85); c.arc(cx,cy-10,16,0,TAU); c.fill(); c.move_to(cx-9,cy); c.line_to(cx+9,cy); c.line_to(cx+14,cy+34); c.line_to(cx-14,cy+34); c.close_path(); c.fill()
        for i in range(40):
            x=W*0.35+(i*97)%int(W*0.4); y=(i*53)%H
            text(c,'•'*((i%4)+3),x,y,20,(1,1,1),0.2+0.2*math.sin(ph+i))
        fade_edges(c); v.push()
    return v.close()

def hero_waves(name='page',cols=None):
    v,c=new(name); cols=cols or CLUB
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        for k in range(7):
            c.new_path()
            for x in range(int(W*0.25),W+10,8):
                u=x/W; y=H*0.5+H*0.28*math.sin(u*TAU*(1+k*0.35)+ph*(1+(k%3))+k)*math.sin(math.pi*u)*(0.5+0.5*math.cos(ph+k))
                (c.move_to if x==int(W*0.25) else c.line_to)(x,y)
            glow_stroke(c,cols[k%len(cols)],1.8,0.55)
        fade_edges(c); v.push()
    return v.close()

def hero_translate():
    v,c=new('translate'); rnd=random.Random(4)
    glyphs=['A','ع','É','ب','ç','ß','Ω','ض','?','ñ','ق','ê','ž','ي','Z','ح']
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T; cx,cy,R=W*0.78,H*0.5,190
        rot=ph/4; tilt=0.38
        c.set_line_width(1.6)
        for lat in range(-60,90,30):
            la=math.radians(lat); c.new_path(); first=True
            for lo in range(0,361,6):
                L=math.radians(lo)+rot; x=math.cos(la)*math.sin(L); y=math.sin(la); z=math.cos(la)*math.cos(L)
                y2=y*math.cos(tilt)-z*math.sin(tilt)
                (c.move_to if first else c.line_to)(cx+R*x,cy-R*y2); first=False
            glow_stroke(c,PAL['blue'],1.3,0.45)
        for lo in range(0,180,20):
            c.new_path(); first=True
            for lat in range(-90,271,6):
                la=math.radians(lat); L=math.radians(lo)+rot
                x=math.cos(la)*math.sin(L); y=math.sin(la); z=math.cos(la)*math.cos(L)
                y2=y*math.cos(tilt)-z*math.sin(tilt)
                (c.move_to if first else c.line_to)(cx+R*x,cy-R*y2); first=False
            glow_stroke(c,PAL['green'],1.3,0.35)
        for i,g in enumerate(glyphs):
            a=ph/2+TAU*i/len(glyphs); rx,ry=330,120
            x=cx+rx*math.cos(a); y=cy+ry*math.sin(a); depth=0.5+0.5*math.sin(a)
            text(c,g,x,y,24+18*depth,mix(CLUB[i%6],WH,0.5),0.3+0.6*depth)
        fade_edges(c); v.push()
    return v.close()

def hero_theme():
    v,c=new('theme')
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T; cx,cy=W*0.78,H*0.5
        for i in range(36):
            a=TAU*i/36+ph/6; col=CLUB[[0,1,2,3][i%4]] if True else None
            col=[PAL['pink'],PAL['purple'],PAL['blue'],PAL['orange']][i%4]
            r1=60; r2=200+30*math.sin(ph*2+i*0.5)
            c.move_to(cx+r1*math.cos(a),cy+r1*math.sin(a)); c.line_to(cx+r2*math.cos(a+0.25),cy+r2*math.sin(a+0.25))
            glow_stroke(c,col,3,0.5)
        for i,col in enumerate((PAL['pink'],PAL['purple'],PAL['blue'],PAL['orange'])):
            a=ph+TAU*i/4; x=W*0.48+i*90; y=H*0.5+40*math.sin(a)
            glow_dot(c,x,y,22,col,0.9,halo=2.5)
        fade_edges(c); v.push()
    return v.close()

def hero_home():
    v,c=new('home'); rnd=random.Random(2)
    SY=['+','−','×','÷','=','√','π','Σ','∞','∫','%','θ','φ','x²','½']
    ps=[dict(x=rnd.uniform(0,W),y=rnd.uniform(0,H),s=rnd.choice(SY),k=rnd.choice((1,2)),a=rnd.uniform(10,40),ph=rnd.uniform(0,TAU),sz=rnd.uniform(18,44),col=rnd.choice(CLUB)) for _ in range(70)]
    for f in range(N):
        clear(c); t=f/FPS; ph=TAU*t/T
        for p in ps:
            x=p['x']+p['a']*math.sin(ph*p['k']+p['ph']); y=p['y']+p['a']*0.7*math.cos(ph*p['k']+p['ph'])
            text(c,p['s'],x,y,p['sz'],mix(p['col'],WH,0.35),0.5+0.3*math.sin(ph+p['ph']))
        v.push()
    return v.close()

ALL=dict(games=hero_games,learn=hero_learn,ai=hero_ai,ideas=hero_ideas,points=hero_points,messages=hero_messages,discussions=hero_discussions,
         users=hero_users,bugs=hero_bugs,changelog=hero_changelog,private=hero_private,page=lambda:hero_waves('page'),translate=hero_translate,theme=hero_theme,home=hero_home)
if __name__=='__main__':
    names=sys.argv[1:] or list(ALL)
    for n in names:
        t0=time.time(); sz=ALL[n](); print(n,sz,round(time.time()-t0,1),flush=True)
