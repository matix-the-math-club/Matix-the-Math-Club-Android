"""Render every Math Basics lesson.  usage: python3 render_all.py <out dir> <kbps> [workers]"""
import sys, time, os
from concurrent.futures import ProcessPoolExecutor
import lessons as Ls
from pkit import render

def job(lid):
    l = Ls.get(lid); t = time.time()
    dst = os.path.join(OUT, lid + '.mp4')
    if os.path.exists(dst) and os.path.getsize(dst) > 0: return lid, 'skip', 0
    tmp = dst + '.part.mp4'
    d = render(tmp, Ls.build(l), Ls.CATS[l['cat']], l['title'], Ls.CAT_ACC[l['cat']], KB)
    os.replace(tmp, dst)
    return lid, round(d, 1), round(time.time() - t)

OUT = sys.argv[1]; KB = int(sys.argv[2]); NW = int(sys.argv[3]) if len(sys.argv) > 3 else 5
if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    ids = sorted((l['id'] for l in Ls.LESSONS), key=lambda i: -sum(d for d, _ in Ls.build(Ls.get(i))))
    with ProcessPoolExecutor(NW) as ex:
        for r in ex.map(job, ids): print(*r, flush=True)
