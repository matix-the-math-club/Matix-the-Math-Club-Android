"""Render every course lesson.  usage: python3 render_course.py <out dir> <kbps> [workers]"""
import sys, time, os
from concurrent.futures import ProcessPoolExecutor
import curriculum as Cu
from pkit import render

def job(lid):
    l = Cu.get(lid); t = time.time()
    dst = os.path.join(OUT, lid + '.mp4')
    if os.path.exists(dst) and os.path.getsize(dst) > 0: return lid, 'skip', 0
    tmp = dst + '.part.mp4'
    d = render(tmp, Cu.build(l), Cu.cat_label(l), l['title'], Cu.ACC[l['grade']], KB)
    os.replace(tmp, dst)
    return lid, round(d, 1), round(time.time() - t)

OUT = sys.argv[1]; KB = int(sys.argv[2]); NW = int(sys.argv[3]) if len(sys.argv) > 3 else 5
if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    ids = sorted((l['id'] for l in Cu.LESSONS), key=lambda i: -sum(d for d, _ in Cu.build(Cu.get(i))))
    with ProcessPoolExecutor(NW) as ex:
        for r in ex.map(job, ids): print(*r, flush=True)
