"""Matix animated lessons - original kinetic-typography math explainers (no audio).
Every frame is drawn from code in this file + lkit.py. Run: python3 lessons.py [ids...]"""
import math, sys, os, cairo
from lkit import *
from vkit import lerp
P,B,Gn,O,Pu,Au,Wh,D,R=C['pink'],C['blue'],C['green'],C['orange'],C['purple'],C['gold'],C['white'],C['dim'],C['red']

def L(c,segs,y,t,t0,t1=None,size=64,x=W/2,align='center',style='up',stagger=0.06):
    return line(c,segs,x,y,size,t,t0,t1,stagger,style,align)
def frac2(c,n,d,cx,cy,size,t,t0,t1=None,cn=Wh,cd=Wh,cl=Wh):
    """big stacked fraction with its own colours, pops in"""
    p=win(t,t0,t1,0.5)
    if p<=0: return
    k=ease_back(clamp((t-t0)/0.5)); c.save(); c.translate(cx,cy); c.scale(k,k)
    nw,dw=str_w(c,str(n),size),str_w(c,str(d),size); w=max(nw,dw)+size*0.25
    str_draw(c,str(n),-nw/2,-size*0.62,size,cn,p); str_draw(c,str(d),-dw/2,size*0.66,size,cd,p)
    rrect(c,-w/2,-size*0.05,w,size*0.10,size*0.05); c.set_source_rgba(*cl,p); c.fill(); c.restore()
def wordfrac(c,top,bot,cx,cy,size,t,t0,t1=None,ct=P,cb=B):
    a=L(c,[(top,ct)],cy-size*0.75,t,t0,t1,size); b=L(c,[(bot,cb)],cy+size*0.8,t,t0+0.3,t1,size)
    q=ease_out(clamp((t-t0-0.2)/0.5))*(1 if t1 is None else 1-clamp((t-t1)/0.4))
    if q>0:
        w=max(a[1],b[1])+40; rrect(c,cx-w/2*q,cy-5,w*q,10,5); c.set_source_rgba(1,1,1,q); c.fill()
def vdash(c,x,y0,y1,t,t0,t1=None,col=Au):
    p=win(t,t0,t1,0.4)
    if p<=0: return
    c.set_source_rgba(*col,p); c.set_line_width(4); c.set_dash([14,10]); c.move_to(x,y0); c.line_to(x,y0+(y1-y0)*ease_out(p)); c.stroke(); c.set_dash([])
def num(c,s,cx,cy,size,col,a):
    str_draw(c,s,cx-str_w(c,s,size)/2,cy,size,col,a)
def seq(ts,i0,n,dt): return [(i,ts+dt*(i-i0),None) for i in range(i0,i0+n)]

# =========================================================== FRACTIONS
def fraction_intro(c,t):
    pie(c,520,580,260,4,t,3.2,[(0,4.3,P),(1,15.6,P),(2,17.0,P),(3,18.4,P)],t1=21.0)
    L(c,['This is'],300,t,3.8,14.6,56,x=1240)
    st=[(1,4.4,15.3),(2,15.6,16.8),(3,17.0,18.2),(4,18.4,21.0)]
    for n_,a,b in st: frac2(c,n_,4,1240,560,170,t,a,b,cn=P,cd=B)
    L(c,[('one quarter',D,False)],830,t,5.0,8.4,54,x=1240)
    L(c,[('1',P),' of ',('4',B),' equal parts'],830,t,8.7,14.6,62,x=1240)
    L(c,[('numerator',P)],420,t,10.2,14.6,46,x=1390,align='left'); L(c,[('parts we have',D,False)],472,t,10.5,14.6,36,x=1390,align='left')
    L(c,[('denominator',B)],660,t,11.6,14.6,46,x=1390,align='left'); L(c,[('equal parts in total',D,False)],712,t,11.9,14.6,36,x=1390,align='left')
    L(c,[('2',P),' of ',('4',B)],830,t,15.7,16.8,62,x=1240); L(c,[('3',P),' of ',('4',B)],830,t,17.1,18.2,62,x=1240)
    L(c,[('4',P),' of ',('4',B),' = ',('1 whole!',Au)],830,t,18.6,21.0,62,x=1240)
    L(c,[('A fraction means',D,False)],290,t,21.3,None,54)
    wordfrac(c,'parts you have','equal parts in total',W/2,560,80,t,21.8)
    tick(c,W/2,860,46,t,23.2)
def equivalent(c,t):
    X,Wd,Hh=200,860,110
    for k,(n_,y,ts) in enumerate([(2,300,4.0),(4,470,5.6),(8,640,7.2)]):
        bar(c,X,y,Wd,Hh,n_,t,ts-0.6,[(i,ts+0.12*i,[P,B,Gn][k]) for i in range(n_//2)],t1=13.4)
        frac2(c,n_//2,n_,1220,y+Hh/2,62,t,ts+0.2,13.4,cn=[P,B,Gn][k],cd=Wh)
        if k<2: L(c,['='],y+Hh/2+85,t,ts+1.2,13.4,52,x=X-50)
    vdash(c,X+Wd/2,260,790,t,9.0,13.4)
    L(c,['Same amount!'],880,t,9.4,13.4,64,x=1500); tick(c,1220,880,36,t,9.9,13.4)
    # rule
    xs=[520,960,1400]
    for k,(n_,d_) in enumerate([(1,2),(2,4),(4,8)]): frac2(c,n_,d_,xs[k],560,150,t,14.0+k*2.2,21.6,cn=[P,B,Gn][k],cd=[P,B,Gn][k])
    for k in range(2):
        L(c,['='],560,t,14.6+k*2.2,21.6,120,x=(xs[k]+xs[k+1])/2)
        ts=15.0+k*2.2
        arrow(c,xs[k]+20,370,xs[k+1]-20,370,t,ts,21.6,bend=-0.25); L(c,[('× 2',Au)],290,t,ts+0.3,21.6,48,x=(xs[k]+xs[k+1])/2)
        arrow(c,xs[k]+20,760,xs[k+1]-20,760,t,ts+0.4,21.6,bend=0.25); L(c,[('× 2',Au)],850,t,ts+0.7,21.6,48,x=(xs[k]+xs[k+1])/2)
    L(c,['Multiply top and bottom by the ',('same',Au),' number'],950,t,19.0,21.6,48)
    L(c,[('Equivalent fractions',Au)],430,t,22.0,None,96,style='pop')
    L(c,[('same amount, different numbers',D,False)],580,t,22.6,None,56)
    tick(c,W/2,740,44,t,23.4)
def add_same(c,t):
    X,Wd,Hh=200,820,110
    bar(c,X,300,Wd,Hh,5,t,3.4,[(0,4.0,P)],t1=13.6)
    bar(c,X,470,Wd,Hh,5,t,4.8,[(0,5.4,B),(1,5.6,B)],t1=13.6)
    bar(c,X,690,Wd,Hh,5,t,7.0,[(0,8.0,P),(1,8.5,B),(2,8.7,B)],t1=13.6)
    frac2(c,1,5,1200,355,62,t,4.2,13.6,cn=P); L(c,['+'],440,t,5.0,13.6,56,x=X-50); frac2(c,2,5,1200,525,62,t,5.8,13.6,cn=B)
    L(c,['='],635,t,7.4,13.6,56,x=X-50); frac2(c,3,5,1200,745,62,t,9.0,13.6,cn=Au)
    L(c,[('1',P),' + ',('2',B),' = ',('3',Au),' fifths'],500,t,9.8,13.6,60,x=1640)
    L(c,[('Same bottom?',Au)],300,t,14.0,None,84,style='pop')
    L(c,['Add the tops, keep the bottom'],430,t,14.7,None,60)
    L(c,[F('1 + 2',5,Wh),'  =  ',F(3,5,Au)],690,t,16.0,None,150,stagger=0.5)
    L(c,[('Never add the bottoms: 1/5 + 2/5 is not 3/10',D,False)],930,t,18.0,None,40)
def add_diff(c,t):
    L(c,[F(1,2,P),'  +  ',F(1,3,B),'  =  ',('?',Au)],330,t,3.4,9.8,150,stagger=0.25)
    L(c,['The pieces are ',('different sizes',R)],590,t,5.2,9.8,56)
    bar(c,320,680,560,110,2,t,5.6,[(0,6.0,P)],t1=9.8); bar(c,1040,680,560,110,3,t,5.9,[(0,6.3,B)],t1=9.8)
    L(c,[('Make them the same size first!',Au)],900,t,7.6,9.8,56)
    L(c,['Common bottom: ',('6',Au)],215,t,10.2,20.2,56)
    rows=[(330,2,1,P,11.6),(500,3,1,B,12.8)]
    for y,n_,f_,col,ts in rows:
        bar(c,560,y,820,100,n_,t,ts-1.2,[(0,ts-0.9,col)],t1=ts)
        bar(c,560,y,820,100,6,t,ts-0.1,[(i,ts-0.1,col) for i in range(6//n_)],t1=20.2)
        frac2(c,1,n_,370,y+50,60,t,ts-1.0,20.2,cn=col,cd=col)
        frac2(c,6//n_,6,1570,y+50,60,t,ts+0.4,20.2,cn=col,cd=col)
        L(c,[(f'× {6//n_}',Au)],y+50,t,ts+0.2,20.2,38,x=1460)
    bar(c,560,720,820,110,6,t,14.4,[(0,15.0,P),(1,15.1,P),(2,15.2,P),(3,15.7,B),(4,15.8,B)],t1=20.2)
    L(c,['total'],775,t,14.6,20.2,48,x=370); frac2(c,5,6,1570,775,66,t,16.4,20.2,cn=Au,cd=Au)
    L(c,[('3 sixths + 2 sixths = 5 sixths',D,False)],940,t,17.0,20.2,44)
    L(c,[F(1,2,P),' + ',F(1,3,B),' = ',F(3,6,P),' + ',F(2,6,B),' = ',F(5,6,Au)],460,t,20.6,None,130,stagger=0.35)
    L(c,['Find a ',('common bottom',Au),', then add the tops'],760,t,23.0,None,56)
def multiply(c,t):
    x0,y0,cw,ch=230,290,220,150
    p=win(t,3.6,13.4,0.6)
    if p>0:
        for i in range(3):
            for j in range(4):
                a=0
                inb=i<2 and t>5.0; inp=j<3 and t>7.0
                rrect(c,x0+i*cw+4,y0+j*ch+4,cw-8,ch-8,10)
                if inb and inp and t>9.0: col,a=Pu,0.95*clamp((t-9.0)/0.5)+0.0
                elif inb and inp: col,a=mix(B,P,0.5),0.55
                elif inb: col,a=B,0.45*clamp((t-5.0)/0.5)
                elif inp: col,a=P,0.45*clamp((t-7.0)/0.5)
                else: col,a=(1,1,1),0.08
                c.set_source_rgba(*col,a*p); c.fill()
        k=0
        for j in range(3):
            for i in range(2):
                k+=1; q=clamp((t-9.4-0.25*k)/0.3)
                if q>0: num(c,str(k),x0+i*cw+cw/2,y0+j*ch+ch/2,64,Wh,q*p)
        c.set_source_rgba(1,1,1,0.9*p); c.set_line_width(5); c.rectangle(x0,y0,cw*3,ch*4); c.stroke()
    L(c,[F(2,3,B),'  of the square'],380,t,5.0,13.4,90,x=1380)
    L(c,[F(3,4,P),'  of that'],560,t,7.0,13.4,90,x=1380)
    L(c,['Overlap: ',('6',Pu),' of ',('12',Wh),' squares'],760,t,11.2,13.4,56,x=1380)
    L(c,[F(2,3,B),' × ',F(3,4,P),'  =  ',F('2 × 3','3 × 4',Au)],320,t,13.8,None,120,stagger=0.3)
    L(c,['=  ',F(6,12,Pu),'  =  ',F(1,2,Gn)],580,t,16.0,None,120,stagger=0.5)
    L(c,['Multiply the ',('tops',Au),', multiply the ',('bottoms',Au)],820,t,18.2,None,56)
    L(c,[('No common bottom needed!',D,False)],920,t,19.6,None,42)
def simplify(c,t):
    bar(c,360,300,1200,120,8,t,3.4,[(i,4.0+0.12*i,O) for i in range(6)],t1=13.2)
    frac2(c,6,8,210,360,64,t,4.8,13.2,cn=O)
    bar(c,360,540,1200,120,4,t,6.6,[(i,7.2+0.15*i,Gn) for i in range(3)],t1=13.2)
    frac2(c,3,4,210,600,64,t,8.0,13.2,cn=Gn)
    vdash(c,360+900,260,700,t,8.8,13.2)
    L(c,['Same amount, ',('smaller numbers',Au)],830,t,9.4,13.2,64); tick(c,W/2,940,36,t,10.4,13.2)
    L(c,[F(6,8,O),'  =  ',F('6 ÷ 2','8 ÷ 2',Au),'  =  ',F(3,4,Gn)],320,t,13.6,None,130,stagger=0.5)
    L(c,['Divide top and bottom by the ',('same',Au),' number'],590,t,15.6,None,58)
    L(c,[('2 is the biggest number that divides both 6 and 8',D,False)],690,t,16.8,None,44)
    L(c,[F(3,4,Gn),'  is in ',('simplest form',Gn)],850,t,18.6,None,80); tick(c,1440,850,36,t,19.6)

# =========================================================== PERCENTS & DECIMALS
def percent_intro(c,t):
    def k(t):
        if t<5.5: return 0
        if t<8.5: return int(25*(t-5.5)/3.0)+1 if t<8.5 else 25
        if t<18.8: return 25
        if t<20.0: return 25+int(25*(t-18.8)/1.2)
        if t<21.6: return 50
        return min(100,50+int(50*(t-21.6)/1.2))
    kk=lambda t: min(k(t),25) if 5.5<=t<8.5 else k(t)
    grid(c,200,250,66,10,10,t,4.0,kk,col=P)
    L(c,[('Percent means',D,False)],300,t,3.4,12.2,56,x=1360); L(c,[('out of 100',Au)],400,t,3.8,12.2,96,x=1360,style='pop')
    if 5.5<=t<12.6:
        a=win(t,5.5,12.2); s=f'{kk(t)} out of 100'; num(c,s,1360,570,72,Wh,a)
    L(c,['=  ',('25%',P)],740,t,8.8,12.2,160,x=1360,style='pop')
    L(c,['25%  =  ',F(25,100),'  =  ',F(1,4,P)],570,t,12.6,18.4,96,x=1360,stagger=0.4)
    L(c,[('a quarter of the grid',D,False)],790,t,14.0,18.4,52,x=1360)
    L(c,[('50%',P),' = half'],570,t,18.8,21.3,100,x=1360,style='pop')
    L(c,[('100%',Au),' = all of it!'],570,t,21.6,None,100,x=1360,style='pop'); tick(c,1360,760,44,t,22.6)
def percent_of(c,t):
    L(c,[('20%',P),' of ',('50',B),' = ',('?',Au)],290,t,3.4,15.0,120,stagger=0.2)
    L(c,[('20%',P),' of ',('50',B),' = ',('10',Au)],290,t,15.2,19.0,120,style='pop')
    L(c,['20%  =  ',F(20,100),'  =  ',F(1,5,P)],560,t,5.0,9.8,96,stagger=0.4)
    L(c,[('So find one fifth of 50',D,False)],790,t,7.2,9.8,58)
    bar(c,310,480,1300,150,5,t,10.2,[(0,14.0,P)],t1=19.0)
    p=win(t,11.0,19.0)
    if p>0:
        c.set_source_rgba(*B,p); c.set_line_width(5); c.move_to(310,445); c.line_to(310,425); c.line_to(1610,425); c.line_to(1610,445); c.stroke()
        rrect(c,900,395,120,60,30); c.set_source_rgba(*C['bg'],p); c.fill(); num(c,'50',960,425,48,B,p)
    for i in range(5):
        q=win(t,12.4+0.25*i,19.0)
        if q>0: num(c,'10',310+130+260*i,555,64,Wh,q)
    L(c,['50 ÷ 5 = ',('10',Au),' in each part'],790,t,12.2,14.3,64)
    L(c,['one part = ',('20%',P),' = ',('10',Au)],790,t,14.6,19.0,64); tick(c,1440,790,34,t,15.4,19.0)
    L(c,[('Shortcut',Au)],330,t,19.4,None,72,style='pop')
    L(c,['50 × 20 ÷ 100 = ',('10',Au)],520,t,20.0,None,96)
    L(c,[('multiply by the percent, then divide by 100',D,False)],700,t,21.0,None,48)
def frac_dec_pct(c,t):
    L(c,[('Three ways to write the same amount',D,False)],280,t,3.4,10.8,56)
    xs=[520,960,1400]
    frac2(c,1,4,xs[0],520,130,t,4.0,10.8,cn=P,cd=P)
    L(c,['='],520,t,4.8,10.8,120,x=740); L(c,[('0.25',B)],520,t,5.2,10.8,150,x=xs[1],style='pop')
    L(c,['='],520,t,6.0,10.8,120,x=1180); L(c,[('25%',Gn)],520,t,6.4,10.8,150,x=xs[2],style='pop')
    for x,s,ts in zip(xs,['fraction','decimal','percent'],[7.4,7.7,8.0]): L(c,[(s,D,False)],790,t,ts,10.8,44,x=x)
    L(c,[F(1,4,P),'   →   1 ÷ 4 = ',('0.25',B)],380,t,11.2,17.4,90,stagger=0.3)
    L(c,[('0.25',B),'   →   0.25 × 100 = ',('25%',Gn)],610,t,13.4,17.4,90,stagger=0.3)
    L(c,[('divide to get the decimal, × 100 to get the percent',D,False)],820,t,15.2,17.4,46)
    hx=[470,800,1160,1500]
    for x,s in zip(hx,['','fraction','decimal','percent']):
        if s: L(c,[(s,D,False)],230,t,17.8,None,40,x=x)
    rows=[(1,2,'0.5','50%',2,1),(1,4,'0.25','25%',4,1),(3,4,'0.75','75%',4,3),(1,10,'0.1','10%',10,1)]
    for r_,(n_,d_,dec,pc,pn,pf) in enumerate(rows):
        y=350+r_*165; ts=18.2+r_*1.2
        pie(c,hx[0],y,62,pn,t,ts,[(i,ts+0.5,P) for i in range(pf)],lw=4)
        frac2(c,n_,d_,hx[1],y,52,t,ts+0.3,cn=P,cd=P)
        L(c,[(dec,B)],y,t,ts+0.5,None,74,x=hx[2]); L(c,[(pc,Gn)],y,t,ts+0.7,None,74,x=hx[3])
def decimals(c,t):
    L(c,[('3',P),('.',Wh),('7',B),('5',Gn)],310,t,3.4,11.8,200,stagger=0.2,style='pop')
    for x,dg,lab,col,ts in [(600,'3','ones',P,5.0),(960,'7','tenths',B,5.6),(1320,'5','hundredths',Gn,6.2)]:
        if card(c,x-150,500,300,240,t,ts,11.8)>0:
            a=win(t,ts,11.8); num(c,dg,x,590,110,col,a); num(c,lab,x,690,38,D,a)
    p=win(t,5.3,11.8)
    if p>0: c.set_source_rgba(1,1,1,p); c.arc(780,690,10,0,TAU); c.fill()
    L(c,[('3',P),'  +  ',F(7,10,B),'  +  ',F(5,100,Gn)],880,t,7.6,11.8,90,stagger=0.4)
    L(c,['3.75 = ',('3',P),' and ',F(75,100,Au)],360,t,12.2,None,100,stagger=0.3)
    L(c,['Like money: ',('$3.75',Au)],590,t,14.4,None,72)
    L(c,[('3 dollars and 75 cents',D,False)],700,t,15.2,None,52)
    L(c,['Each place to the right is ',('10× smaller',Au)],880,t,17.4,None,48)

# =========================================================== NUMBERS
NX=lambda v: 960+v*140
def numline(c,t,t0,y=560):
    p=win(t,t0,None,0.8)
    if p<=0: return
    c.set_source_rgba(1,1,1,0.9*p); c.set_line_width(5); c.move_to(NX(-5.6),y); c.line_to(NX(-5.6)+(NX(5.6)-NX(-5.6))*ease_out(p),y); c.stroke()
    for v in range(-5,6):
        q=clamp((t-t0-0.3-0.05*(v+5))/0.3)
        if q<=0: continue
        c.set_source_rgba(1,1,1,q); c.set_line_width(4); c.move_to(NX(v),y-16); c.line_to(NX(v),y+16); c.stroke()
        s=('−'+str(-v)) if v<0 else str(v); num(c,s,NX(v),y+60,42,R if v<0 else (Wh if v==0 else B),q)
def negatives(c,t):
    y=560; numline(c,t,3.4)
    L(c,['Start at ',('−3',P),', add ',('5',Au),': move ',('right',Gn)],270,t,4.4,13.4,60)
    for i in range(5): arrow(c,NX(-3+i)+10,y-22,NX(-2+i)-10,y-22,t,5.8+0.7*i,13.4,col=Gn,bend=-0.8,head=18)
    for i in range(5): L(c,[('+1',Gn)],y-130,t,6.0+0.7*i,13.4,34,x=(NX(-3+i)+NX(-2+i))/2)
    L(c,[('−3',P),' + ',('5',Au),' = ',('2',Gn)],850,t,9.6,13.4,110,style='pop')
    L(c,['Now subtract ',('4',Au),': move ',('left',R)],270,t,13.8,20.8,60)
    for i in range(4): arrow(c,NX(2-i)-10,y+22,NX(1-i)+10,y+22,t,15.0+0.7*i,20.8,col=R,bend=-0.8,head=18)
    L(c,[('2',Gn),' − ',('4',Au),' = ',('−2',R)],850,t,18.0,20.8,110,style='pop')
    # moving dot
    if t>4.4:
        if t<5.8: v=-3
        elif t<9.4: v=-3+min(5,(t-5.8)/0.7)
        elif t<15.0: v=2
        elif t<17.8: v=2-min(4,(t-15.0)/0.7)
        else: v=-2
        fr=v-math.floor(v); hop=math.sin(fr*math.pi)*(-60 if (t<15) else 60)
        q=ease_back(clamp((t-4.4)/0.4)); glow=P if t<15 else Au
        c.set_source_rgba(*glow,0.25); c.arc(NX(v),y+hop,30*q,0,TAU); c.fill()
        c.set_source_rgba(*glow,1); c.arc(NX(v),y+hop,17*q,0,TAU); c.fill()
    L(c,[('add',Gn),' → move right      ',('subtract',R),' → move left'],270,t,21.2,None,60)
    L(c,[('Numbers left of zero are negative',D,False)],850,t,22.0,None,52)
def order_ops(c,t):
    cx,y,sz=960,300,150
    L(c,['2 + ','3 × 4'],y,t,3.4,14.0,sz,stagger=0.3)
    w1,w2=str_w(c,'2 + ',sz),str_w(c,'3 × 4',sz); x0=cx-(w1+w2)/2
    underline(c,x0+w1,y+95,w2,t,10.0,14.0,col=Gn)
    L(c,[('What is the answer?',D,False)],460,t,4.6,7.2,54)
    L(c,[('Left to right?',R)],520,t,5.8,14.0,56,x=560)
    L(c,['2 + 3 = 5'],640,t,6.6,14.0,64,x=560); L(c,['5 × 4 = ',('20',R)],750,t,7.6,14.0,64,x=560); cross(c,560,880,34,t,8.6,14.0)
    L(c,[('× before +',Gn)],520,t,9.6,14.0,56,x=1360)
    L(c,['3 × 4 = 12'],640,t,10.4,14.0,64,x=1360); L(c,['2 + 12 = ',('14',Gn)],750,t,11.2,14.0,64,x=1360); tick(c,1360,880,36,t,12.2,14.0)
    L(c,[('The order of operations',Au)],240,t,14.4,None,72,style='pop')
    items=[('1','Brackets','( )'),('2','Powers','x²'),('3','× and ÷','left to right'),('4','+ and −','left to right')]
    for i,(n_,a,b) in enumerate(items):
        yy=370+i*130; ts=15.0+i*0.8
        if card(c,520,yy-52,880,104,t,ts)>0:
            L(c,[(n_,[P,B,Gn,O][i])],yy,t,ts,None,56,x=580,align='left'); L(c,[a],yy,t,ts+0.1,None,56,x=660,align='left')
            L(c,[(b,D,False)],yy,t,ts+0.2,None,44,x=1360,align='right')
    L(c,['So  2 + 3 × 4 = ',('14',Gn)],930,t,19.4,None,52)
def blocks(c,x,y,n,s,col,a):
    for i in range(n):
        rrect(c,x+(i%2)*(s+8),y+(i//2%2)*(s+8)-(i//4)*(s*0.55),s,s,10); c.set_source_rgba(*col,a*(0.95-0.25*(i//4))); c.fill()
def powers(c,t):
    L(c,['2³',' = ','2 × 2 × 2',' = ',('8',Au)],290,t,3.4,12.0,130,stagger=0.9,style='pop')
    L(c,[('2',P),' is the base,  ',('3',Au),' says how many 2s to multiply'],450,t,6.0,12.0,48)
    for x,n_,lab,ts,col in [(470,2,'2',7.0,P),(910,4,'2 × 2 = 4',8.2,B),(1380,8,'2 × 2 × 2 = 8',9.4,Au)]:
        p=win(t,ts,12.0)
        if p>0:
            q=ease_back(clamp((t-ts)/0.5)); c.save(); c.translate(x,690); c.scale(q,q)
            if n_==2:
                for i in range(2): rrect(c,-39,-78+i*86,78,78,10); c.set_source_rgba(*col,0.95*p); c.fill()
            elif n_==4: blocks(c,-82,-82,4,78,col,p)
            else: blocks(c,-185,-82,4,78,col,p); blocks(c,21,-82,4,78,col,p)
            c.restore(); L(c,[(lab,D,False)],880,t,ts+0.2,12.0,44,x=x)
    for i,(a,b) in enumerate([('10²','10 × 10 = 100'),('5²','5 × 5 = 25'),('3³','3 × 3 × 3 = 27')]):
        L(c,[(a,[B,Gn,O][i]),'  =  ',b],320+i*140,t,12.4+i*1.4,None,80)
    L(c,[('Careful: 2³ is 8, not 2 × 3 = 6',R)],770,t,17.0,None,52)
    L(c,['Powers are ',('repeated multiplication',Au)],900,t,18.8,None,58)
def roots(c,t):
    def k(t): return 0 if t<4.5 else min(49,int(49*(t-4.5)/3.0)+1)
    grid(c,260,290,80,7,7,t,3.6,k,t1=13.0,col=Gn)
    L(c,['49 squares'],380,t,4.6,13.0,72,x=1330)
    L(c,['make a ',('7 × 7',Au),' square'],500,t,8.0,13.0,64,x=1330)
    L(c,[('7',Au)],905,t,9.2,13.0,56,x=540); L(c,[('7',Au)],570,t,9.4,13.0,56,x=215)
    L(c,['√49 = ',('7',Au)],720,t,10.0,13.0,150,x=1330,style='pop')
    L(c,[('The square root asks:',D,False)],320,t,13.4,None,56)
    L(c,['what number × ',('itself',Au),' = 49?'],440,t,14.0,None,72)
    L(c,['7 × 7 = 49   →   √49 = ',('7',Au)],600,t,15.6,None,76)
    L(c,['√16 = ',('4',Gn),'        √81 = ',('9',Gn),'        √100 = ',('10',Gn)],800,t,17.6,None,62,stagger=0.5)

# =========================================================== ALGEBRA
def scale(c,t,t0,t1,lit,right):
    p=win(t,t0,t1,0.6)
    if p<=0: return
    wob=math.sin((t-9.8)*6)*math.exp(-(t-9.8)*1.4)*0.02 if t>9.8 else 0
    c.save(); c.translate(960,330); c.rotate(wob)
    c.set_source_rgba(1,1,1,0.9*p); rrect(c,-600,-8,1200,16,8); c.fill()
    for sx in (-580,580):
        c.set_line_width(3); c.move_to(sx,0); c.line_to(sx-190,270); c.move_to(sx,0); c.line_to(sx+190,270); c.stroke()
        rrect(c,sx-230,262,460,18,9); c.fill()
    c.restore()
    c.set_source_rgba(1,1,1,0.9*p); c.move_to(960,330); c.line_to(920,410); c.line_to(1000,410); c.close_path(); c.fill()
    rrect(c,952,400,16,260,6); c.fill(); rrect(c,860,650,200,18,9); c.fill()
    for (x,y,kind,ts,te) in lit+right:
        q=clamp((t-ts)/0.4); o=1-clamp((t-te)/0.6) if te else 1
        if q<=0 or o<=0: continue
        lift=(1-o)*120; a=p*ease_out(q)*o; yy=y-lift
        if kind=='x':
            rrect(c,x-48,yy-48,96,96,16); c.set_source_rgba(*O,a); c.fill(); num(c,'x',x,yy-4,64,C['bg'],a)
        else:
            c.arc(x,yy,24,0,TAU); c.set_source_rgba(*B,a); c.fill()
def solve_one(c,t):
    left=[(250,530,'x',4.0,None)]+[(330+i*56,553,'o',4.2+0.1*i,9.8) for i in range(5)]
    right=[(1400+i*56,553 if j==0 else 500,'o',4.4+0.05*(i+6*j),(9.8 if j==1 and i<5 else None)) for j in range(2) for i in range(6)]
    scale(c,t,3.4,19.0,left,right)
    L(c,[('Keep both sides balanced',D,False)],180,t,6.4,8.8,48)
    L(c,['Take ',('5',B),' away from ',('both',Au),' sides'],180,t,9.0,15.0,48)
    L(c,[('Check: put 7 back in',D,False)],180,t,15.4,19.0,48)
    L(c,[('x',O),' + 5 = 12'],820,t,5.4,10.2,110)
    L(c,[('x',O),' + 5 ',('− 5',Au),' = 12 ',('− 5',Au)],820,t,10.4,12.8,100)
    L(c,[('x',O),' = ',('7',Au)],820,t,13.0,15.2,140,style='pop')
    L(c,['7 + 5 = 12'],820,t,15.6,19.0,110); tick(c,1280,820,40,t,16.6,19.0)
    L(c,['Do the ',('same thing',Au),' to both sides'],300,t,19.4,None,72,style='pop')
    L(c,[('x',O),' + 5 = 12'],470,t,20.2,None,80)
    L(c,[('− 5          − 5',Au)],570,t,20.8,None,56)
    L(c,[('x',O),' = ',('7',Gn)],690,t,21.4,None,100,style='pop')
    L(c,[('The goal: get x on its own',D,False)],880,t,22.6,None,50)
def solve_two(c,t):
    ex=820; nx=1240
    L(c,['3',('x',O),' − 4 = 11'],300,t,3.4,15.8,110,x=ex)
    L(c,[('+ 4 to both sides',Au)],400,t,5.0,15.8,46,x=nx,align='left'); arrow(c,ex+20,350,ex+20,450,t,5.2,15.8,bend=-0.6,head=16)
    L(c,['3',('x',O),' = 15'],500,t,6.4,15.8,110,x=ex)
    L(c,[('÷ 3 on both sides',Au)],600,t,8.0,15.8,46,x=nx,align='left'); arrow(c,ex+20,550,ex+20,650,t,8.2,15.8,bend=-0.6,head=16)
    L(c,[('x',O),' = ',('5',Gn)],700,t,9.4,15.8,120,x=ex,style='pop')
    L(c,['Check:  3 × 5 − 4 = 15 − 4 = ',('11',Gn)],890,t,11.4,15.8,56); tick(c,1580,890,32,t,13.2,15.8)
    L(c,[('Undo in reverse order',Au)],300,t,16.2,None,76,style='pop')
    L(c,['Last step done: ',('− 4',P),'  →  undo first with ',('+ 4',Gn)],470,t,17.2,None,52)
    L(c,['First step done: ',('× 3',P),'  →  undo last with ',('÷ 3',Gn)],590,t,18.6,None,52)
    L(c,[('Like getting dressed: socks go on first, but come off last',D,False)],800,t,20.4,None,44)

# =========================================================== GEOMETRY
def area(c,t):
    x0,y0,s=230,300,110
    def k(t): return 0 if t<4.6 else min(24,int(24*(t-4.6)/3.2)+1)
    p=win(t,3.6,None,0.6)
    if p>0:
        n=k(t)
        for j in range(4):
            for i in range(6):
                idx=j*6+i; rrect(c,x0+i*s+4,y0+j*s+4,s-8,s-8,10)
                c.set_source_rgba(*(B if idx<n else (1,1,1)),(0.85 if idx<n else 0.08)*p*(1-0.45*clamp((t-14)/0.6))); c.fill()
                if idx<n and t<14.2: num(c,str(idx+1),x0+i*s+s/2,y0+j*s+s/2,40,Wh,p*(1-clamp((t-13.6)/0.5)))
    L(c,[('6',Au)],y0+4*s+50,t,8.3,13.8,56,x=x0+3*s); L(c,[('4',Au)],y0+2*s,t,8.5,13.8,56,x=x0-45)
    L(c,['Area = the space ',('inside',B)],330,t,3.8,13.8,56,x=1400)
    if 4.6<=t<14.2: num(c,f'{k(t)} squares',1400,470,72,Wh,win(t,4.6,13.8))
    L(c,['6 × 4 = ',('24',Au)],610,t,8.8,13.8,90,x=1400)
    L(c,['Area = 24 ',('square units',Au)],740,t,9.8,13.8,56,x=1400)
    # perimeter trace
    if t>14.4:
        q=clamp((t-14.6)/4.0); per=2*(6*s+4*s); d=per*q
        pts=[(x0,y0),(x0+6*s,y0),(x0+6*s,y0+4*s),(x0,y0+4*s),(x0,y0)]
        c.set_source_rgba(*O,1); c.set_line_width(10); c.set_line_cap(cairo.LINE_CAP_ROUND); c.move_to(*pts[0])
        for (ax,ay),(bx,by) in zip(pts,pts[1:]):
            sl=math.hypot(bx-ax,by-ay)
            if d<=0: break
            u=min(1,d/sl); c.line_to(ax+(bx-ax)*u,ay+(by-ay)*u); d-=sl
        c.stroke()
        if q<1: num(c,str(int(20*q)),1400,470,1,Wh,0)
    L(c,['Perimeter = the distance ',('around',O)],330,t,14.2,None,56,x=1400)
    L(c,['6 + 4 + 6 + 4'],480,t,14.8,None,80,x=1400,stagger=0)
    L(c,['= ',('20 units',O)],610,t,19.0,None,90,x=1400,style='pop')
    L(c,[('Area: inside  ·  Perimeter: around',D,False)],880,t,20.6,None,46)
TA=(400,820); TB=(1160,820); _ac=760*math.sin(math.radians(60))/math.sin(math.radians(70))
TC=(TA[0]+_ac*math.cos(math.radians(50)),TA[1]-_ac*math.sin(math.radians(50)))
def _wedge(V,N1,N2):
    a1=math.atan2(N1[1]-V[1],N1[0]-V[0]); a2=math.atan2(N2[1]-V[1],N2[0]-V[0])
    sp=(a2-a1)%TAU
    if sp>math.pi: a1,sp=a2,TAU-sp
    return a1,sp
WEDGES=[(TA,_wedge(TA,TB,TC),'50°',P),(TC,_wedge(TC,TA,TB),'70°',Gn),(TB,_wedge(TB,TA,TC),'60°',B)]
def triangle_angles(c,t):
    p=win(t,3.4,12.4,0.4)
    if p>0:
        q=clamp((t-3.4)/1.2); pts=[TA,TB,TC,TA]; c.set_source_rgba(1,1,1,0.9*p); c.set_line_width(6); c.set_line_join(cairo.LINE_JOIN_ROUND)
        per=sum(math.dist(a,b) for a,b in zip(pts,pts[1:])); d=per*q; c.move_to(*TA)
        for a,b in zip(pts,pts[1:]):
            sl=math.dist(a,b)
            if d<=0: break
            u=min(1,d/sl); c.line_to(a[0]+(b[0]-a[0])*u,a[1]+(b[1]-a[1])*u); d-=sl
        c.stroke()
    tgt=0.0; P0=(960,760)
    for i,(V,(a1,sp),lab,col) in enumerate(WEDGES):
        ts=5.0+0.8*i
        if t<=ts: continue
        q=ease_back(clamp((t-ts)/0.5))
        m=ease_in_out(clamp((t-12.6-0.3*i)/1.6))
        cx=lerp(V[0],P0[0],m); cy=lerp(V[1],P0[1],m)
        ta=math.pi+tgt; ang=lerp(a1,ta+ (0 if abs(ta-a1)<=math.pi else (-TAU if ta>a1 else TAU)),m) if True else a1
        r=(120+110*m)*q
        c.move_to(cx,cy); c.arc(cx,cy,r,ang,ang+sp); c.close_path(); c.set_source_rgba(*col,0.85); c.fill()
        mid=ang+sp/2; lr=r+46-m*(r*0.45+46)
        num(c,lab,cx+lr*math.cos(mid),cy+lr*math.sin(mid),40 if m<0.5 else 44,Wh if m>0.5 else col,1)
        tgt+=sp
    p2=win(t,14.2,None,0.6)
    if p2>0:
        c.set_source_rgba(1,1,1,p2); c.set_line_width(6); c.move_to(960-420*p2,760); c.line_to(960+420*p2,760); c.stroke()
    L(c,['Add the angles:'],380,t,7.4,12.0,52,x=1540)
    L(c,['50 + 60 + 70'],490,t,8.0,12.0,70,x=1540)
    L(c,['= ',('180°',Au)],620,t,9.0,12.0,110,x=1540,style='pop')
    L(c,['Tear off the corners...'],300,t,12.8,15.8,56)
    L(c,['...they make a ',('straight line',Au),' = 180°'],300,t,16.0,None,60)
    L(c,[('Every triangle: the 3 angles add up to 180°',D,False)],880,t,18.2,None,50)
def pythagoras(c,t):
    u=70; Rx,Ry=680,650; A=(Rx,Ry-3*u); Bp=(Rx+4*u,Ry)
    def sq(o,e1,e2,n,ts,colf,lab,lt):
        p=win(t,ts,None,0.5)
        if p<=0: return
        kk=min(n*n,int(n*n*clamp((t-ts-0.5)/1.6)+(1 if t>ts+0.5 else 0)))
        for j in range(n):
            for i in range(n):
                idx=j*n+i
                pts=[(o[0]+e1[0]*(i+a)+e2[0]*(j+b),o[1]+e1[1]*(i+a)+e2[1]*(j+b)) for a,b in ((0.06,0.06),(0.94,0.06),(0.94,0.94),(0.06,0.94))]
                c.move_to(*pts[0]); [c.line_to(*q) for q in pts[1:]]; c.close_path()
                c.set_source_rgba(*(colf(idx) if idx<kk else (1,1,1)),(0.85 if idx<kk else 0.08)*p); c.fill()
        cx=o[0]+(e1[0]+e2[0])*n/2; cy=o[1]+(e1[1]+e2[1])*n/2
        a=win(t,lt,None,0.4)
        if a>0:
            c.set_source_rgba(*C['bg'],0.7*a); c.arc(cx,cy,48,0,TAU); c.fill(); num(c,lab,cx,cy,56,Wh,a)
    sq((Rx-3*u,A[1]),(u,0),(0,u),3,6.0,lambda i:P,'9',7.8)
    sq((Rx,Ry),(u,0),(0,u),4,8.2,lambda i:B,'16',10.0)
    e1=((Bp[0]-A[0])/5,(Bp[1]-A[1])/5); e2=(3*u/5,-4*u/5)
    sq(A,e1,(e2[0],e2[1]),5,10.6,lambda i:P if i<9 else B,'25',12.6)
    p=win(t,3.4,None,0.6)
    if p>0:
        c.set_source_rgba(1,1,1,p); c.set_line_width(6); c.set_line_join(cairo.LINE_JOIN_ROUND)
        c.move_to(Rx,Ry); c.line_to(*A); c.line_to(*Bp); c.close_path(); c.stroke()
        c.set_line_width(3); c.rectangle(Rx,Ry-30,30,30); c.stroke()
    X=1555
    L(c,['A ',('right',Au),' triangle'],300,t,4.0,16.6,56,x=X)
    L(c,[('sides 3, 4 and 5',D,False)],380,t,4.6,16.6,44,x=X)
    L(c,[('3² = 9',P)],480,t,6.4,16.6,64,x=X); L(c,[('4² = 16',B)],580,t,8.6,16.6,64,x=X); L(c,[('5² = 25',Au)],680,t,11.0,16.6,64,x=X)
    L(c,[('9',P),' + ',('16',B),' = ',('25',Au)],820,t,13.4,16.6,80,x=X); tick(c,X,930,32,t,14.6,16.6)
    L(c,[("Pythagoras' theorem",Au)],320,t,17.0,None,56,x=X,style='pop')
    L(c,[('a²',P),' + ',('b²',B),' = ',('c²',Au)],480,t,17.6,None,90,x=X)
    L(c,[('works for every right triangle',D,False)],620,t,18.8,None,40,x=X)
    L(c,[('c = the longest side',D,False)],700,t,19.6,None,40,x=X)

# =========================================================== catalogue
LESSONS=[
 ('fraction_intro','fractions','What is a fraction?','1 of 4 equal parts is one quarter',26,fraction_intro,P),
 ('equivalent','fractions','Equivalent fractions','1/2 = 2/4 = 4/8',25,equivalent,P),
 ('add_same','fractions','Adding fractions','Same bottom: add the tops',22,add_same,P),
 ('add_diff','fractions','Different bottoms','Find a common denominator',27,add_diff,P),
 ('multiply','fractions','Multiplying fractions','Tops times tops, bottoms times bottoms',24,multiply,P),
 ('simplify','fractions','Simplifying fractions','6/8 becomes 3/4',23,simplify,P),
 ('percent_intro','percent','What is a percent?','Percent means out of 100',25,percent_intro,Gn),
 ('percent_of','percent','Percent of a number','What is 20% of 50?',24,percent_of,Gn),
 ('frac_dec_pct','percent','Fractions, decimals, percents','Three ways to say the same amount',26,frac_dec_pct,Gn),
 ('decimals','percent','Decimal place value','What does 3.75 really mean?',23,decimals,Gn),
 ('negatives','numbers','Negative numbers','Moving along the number line',26,negatives,B),
 ('order_ops','numbers','Order of operations','Why 2 + 3 × 4 is 14',24,order_ops,B),
 ('powers','numbers','Powers','2³ means 2 × 2 × 2',23,powers,B),
 ('roots','numbers','Square roots','Why √49 is 7',23,roots,B),
 ('solve_one','algebra','Solving for x','Keep the equation balanced',26,solve_one,O),
 ('solve_two','algebra','Two-step equations','Undo in reverse order',25,solve_two,O),
 ('area','geometry','Area and perimeter','Inside and around a rectangle',25,area,Pu),
 ('triangle_angles','geometry','Angles in a triangle','They always add up to 180°',25,triangle_angles,Pu),
 ('pythagoras','geometry','The Pythagorean theorem','3² + 4² = 5²',25,pythagoras,Pu),
]
CATN=dict(fractions='Fractions',percent='Percents & decimals',numbers='Numbers',algebra='Algebra',geometry='Geometry')
def go(i,out,kbps=None,t_only=None):
    lid,cat,title,sub,dur,fn,acc=LESSONS[i]
    return render(os.path.join(out,lid+'.mp4'),dur,fn,Look(i*7+3,acc),CATN[cat],title,sub,i+1,kbps=kbps,t_only=t_only)
if __name__=='__main__':
    out=sys.argv[1]; ids=sys.argv[2:]
    kb=int(os.environ.get('KBPS','0')) or None
    ts=os.environ.get('STILLS')
    for i,l in enumerate(LESSONS):
        if ids and l[0] not in ids: continue
        r=go(i,out,kb,[float(x) for x in ts.split(',')] if ts else None)
        print(l[0],r,flush=True)
