// Matix fractal renderer: writes raw RGB24 frames to stdout.
// usage: fractal mode w h frames [ss]
//  mode 0 = Mandelbrot deep zoom, 1 = Julia morph loop, 2 = Newton z^3-1 rotating
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>
#include <pthread.h>
typedef struct {double r,g,b;} C3;
static C3 PALC[]={{0.043,0.043,0.047},{0.357,0.275,0.788},{0.545,0.361,0.965},{1.0,0.31,0.639},{0.984,0.573,0.235},{0.969,0.80,0.25},{0.243,0.914,0.561},{0.22,0.741,0.973},{0.357,0.275,0.788}};
#define NP 8
static int MODE,W,H,FR,SS; static int frame; static unsigned char *buf;
static double cx,cy,scale,rot,jr,ji,cyc,glow;
static C3 pal(double t){ t=t-floor(t); double x=t*NP; int i=(int)x; double f=x-i; if(i<0||i>=NP){i=0;f=0;} f=f*f*(3-2*f); C3 a=PALC[i],b=PALC[(i+1)%NP==0?1:(i+1)];
  if(i+1>=NP) b=PALC[1]; C3 o={a.r+(b.r-a.r)*f,a.g+(b.g-a.g)*f,a.b+(b.b-a.b)*f}; return o; }
static int maxit;
static C3 shade(double px,double py){
  double ca=cos(rot),sa=sin(rot);
  double x0=(px-W*0.5)/H*scale, y0=(py-H*0.5)/H*scale;
  double X=cx+x0*ca-y0*sa, Y=cy+x0*sa+y0*ca;
  C3 bg={0.043,0.043,0.047};
  if(MODE==2){ // Newton for z^3-1
    double zr=X,zi=Y; int n; for(n=0;n<60;n++){ double r2=zr*zr-zi*zi, i2=2*zr*zi; // z^2
      double z3r=r2*zr-i2*zi, z3i=r2*zi+i2*zr; double dr=3*r2, di=3*i2; double nr=z3r-1, ni=z3i; double den=dr*dr+di*di; if(den<1e-18) break;
      double qr=(nr*dr+ni*di)/den, qi=(ni*dr-nr*di)/den; zr-=qr; zi-=qi; if(qr*qr+qi*qi<1e-12) break; }
    double ang=atan2(zi,zr); int root = ang>1.0?1:(ang<-1.0?2:0);
    C3 rc[3]={{1.0,0.31,0.639},{0.22,0.741,0.973},{0.243,0.914,0.561}};
    double k=exp(-n*0.11); C3 c=rc[root]; double m=0.15+0.85*k; C3 o={bg.r+(c.r-bg.r)*m,bg.g+(c.g-bg.g)*m,bg.b+(c.b-bg.b)*m};
    // edges glow
    double e=fmod(n*0.5+cyc*6,1.0); o.r+=0.10*e*k; o.g+=0.10*e*k; o.b+=0.10*e*k; return o; }
  double zr,zi,cr,ci;
  if(MODE==0){ zr=0;zi=0;cr=X;ci=Y; double q=(X-0.25)*(X-0.25)+Y*Y; if(q*(q+(X-0.25))<0.25*Y*Y) return bg; if((X+1)*(X+1)+Y*Y<0.0625) return bg; }
  else { zr=X;zi=Y;cr=jr;ci=ji; }
  int n; double r2=0, dzr=1,dzi=0;
  double ozr=0,ozi=0; int per=0;
  for(n=0;n<maxit;n++){ double nzr=zr*zr-zi*zi+cr, nzi=2*zr*zi+ci;
    // derivative for distance estimate
    double ndr=2*(zr*dzr-zi*dzi)+(MODE==0?1:0), ndi=2*(zr*dzi+zi*dzr); dzr=ndr; dzi=ndi;
    zr=nzr; zi=nzi; r2=zr*zr+zi*zi; if(r2>1e6) break;
    if(zr==ozr && zi==ozi){ n=maxit; break; } if(++per>20){ per=0; ozr=zr; ozi=zi; } }
  if(n>=maxit){ if(MODE!=1) return bg; double an=atan2(zi,zr)/6.283185307; double mg=sqrt(zr*zr+zi*zi); C3 c=pal(an*2+cyc*0.5+mg*0.3); double k=0.28; C3 o={bg.r+(c.r-bg.r)*k,bg.g+(c.g-bg.g)*k,bg.b+(c.b-bg.b)*k}; return o; }
  double mu=n+1-log(log(sqrt(r2)))/log(2.0);
  double dist=sqrt(r2/(dzr*dzr+dzi*dzi))*0.5*log(r2); double pix=scale/H; double de=dist/pix; // pixels to boundary
  double t=sqrt(mu)*0.16+cyc; C3 c=pal(t);
  double edge=exp(-de*0.6); double fade=1.0-exp(-mu*0.06);
  double b=fade*(0.55+0.45*edge*glow);
  C3 o={bg.r+(c.r-bg.r)*b+0.35*edge*edge*glow,bg.g+(c.g-bg.g)*b+0.35*edge*edge*glow,bg.b+(c.b-bg.b)*b+0.35*edge*edge*glow};
  return o; }
typedef struct {int y0,y1,st;} Job;
static void *work(void*a){ Job*j=(Job*)a; for(int y=j->y0;y<H;y+=j->st){ unsigned char*row=buf+(size_t)y*W*3; for(int x=0;x<W;x++){ double r=0,g=0,b=0;
  for(int s=0;s<SS;s++){ double ox=SS==1?0.5:(s==0?0.25:s==1?0.75:s==2?0.25:0.75), oy=SS==1?0.5:(s==0?0.25:s==1?0.25:s==2?0.75:0.75); if(SS==2){ox=s?0.75:0.25; oy=s?0.25:0.75;}
    C3 c=shade(x+ox,y+oy); r+=c.r;g+=c.g;b+=c.b; }
  r/=SS; g/=SS; b/=SS; if(r>1)r=1; if(g>1)g=1; if(b>1)b=1;
  row[x*3]=(unsigned char)(pow(r,1.0)*255+0.5); row[x*3+1]=(unsigned char)(g*255+0.5); row[x*3+2]=(unsigned char)(b*255+0.5);} } return 0; }
int main(int argc,char**argv){ MODE=atoi(argv[1]); W=atoi(argv[2]); H=atoi(argv[3]); FR=atoi(argv[4]); SS=argc>5?atoi(argv[5]):1; int NT=argc>6?atoi(argv[6]):2; int START=argc>7?atoi(argv[7]):0; int CNT=argc>8?atoi(argv[8]):FR;
  buf=malloc((size_t)W*H*3);
  for(frame=START;frame<START+CNT && frame<FR;frame++){ double u=(double)frame/FR; double T=(double)frame/30.0; glow=1.0;
    if(MODE==0){ // zoom into seahorse valley spiral
      cx=-0.743643887037158704752191506114774; cy=0.131825904205311970493132056385139;
      double z0=log(3.2), z1=log(5.0e-9); double e=u<0.06? u*u/0.12 : u-0.03; e=e/(0.97); // ease-in
      scale=exp(z0+(z1-z0)*e); rot=0.35*u*6.283; double depth=log2(3.2/scale); maxit=(int)(300+depth*depth*3.2); cyc=T*0.12;
      // start centred on whole set, drift to target
      double k=u<0.12?1.0-(u/0.12)*(u/0.12)*(3-2*(u/0.12)):0; cx=cx*(1-k)+(-0.6)*k; cy=cy*(1-k)+0.0*k; }
    else if(MODE==1){ double a=6.283185307*u; double m=1.075+0.035*sin(2*a); double lr=m*cos(a), li=m*sin(a); /* c = l/2 - l^2/4 just outside the main cardioid */ jr=lr/2-(lr*lr-li*li)/4; ji=li/2-(2*lr*li)/4; cx=0;cy=0; scale=2.5; rot=-a*0.5; maxit=900; cyc=u*2.0; }
    else { cx=0;cy=0; scale=2.6+0.6*sin(6.283185307*u); rot=6.283185307*u/3.0; cyc=u; maxit=60; }
    pthread_t th[8]; Job jb[8]; for(int i=0;i<NT;i++){ jb[i].y0=i; jb[i].st=NT; pthread_create(&th[i],0,work,&jb[i]); }
    for(int i=0;i<NT;i++) pthread_join(th[i],0);
    fwrite(buf,1,(size_t)W*H*3,stdout); fflush(stdout); }
  return 0; }
