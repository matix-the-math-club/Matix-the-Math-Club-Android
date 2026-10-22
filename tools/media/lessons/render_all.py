import lessons as Ls, sys, time
from concurrent.futures import ProcessPoolExecutor
KB=int(sys.argv[1]) if len(sys.argv)>1 else 2300
def job(i):
    t=time.time(); r=Ls.go(i,'out',KB); return Ls.LESSONS[i][0],r,round(time.time()-t)
if __name__=='__main__':
    order=sorted(range(len(Ls.LESSONS)),key=lambda i:-Ls.LESSONS[i][4])
    with ProcessPoolExecutor(2) as ex:
        for res in ex.map(job,order): print(*res,flush=True)
    print('total s',sum(l[4] for l in Ls.LESSONS))
