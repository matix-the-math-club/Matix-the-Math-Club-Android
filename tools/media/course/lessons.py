"""Matix "Math Basics" lessons (1.0.7): counting, place value, adding, subtracting, times tables, dividing,
time and shapes. Every frame is drawn from code in this file + pkit.py.
Preview stills:  python3 lessons.py stills add_intro 3,9,15
Render one:      python3 lessons.py render add_intro out 1500"""
import math, sys, os
from pkit import *

CATS = {'basics': 'Counting & place value', 'addsub': 'Adding & subtracting', 'muldiv': 'Times & dividing',
        'shapes': 'Time & shapes'}
CAT_ACC = {'basics': G, 'addsub': P, 'muldiv': B, 'shapes': O}


# =============================================================== shared cards
def intro(title, sub, acc):
    def f(fr, t):
        X = 3.6
        fr.pop(title, W / 2, 470, 96, WH, t, 0.2, X)
        fr.ktext(sub, W / 2, 590, 46, acc, t, 0.8, X)
        p = env(t, 0.5, X)
        fr.rect(W / 2 - 160 * eo(p), 650, 320 * eo(p), 8, acc, p, r=4)
    return (4.0, f)


def recap(lines, acc):
    def f(fr, t):
        fr.pop('Remember', W / 2, 300, 84, acc, t, 0.2)
        for i, ln in enumerate(lines):
            fr.ktext(ln, W / 2, 460 + i * 96, 50, WH, t, 0.9 + i * 0.6)
        for i in range(5):
            p = env(t, 2.4 + i * 0.12)
            fr.star(W / 2 - 200 + i * 100, 470 + len(lines) * 96 + 40, 30 * eback(clamp((t - 2.4 - i * 0.12) / 0.4)), AU, p, rot=t * 0.6)
    return (4.5 + 0.6 * len(lines), f)


def title_card(text, sub, acc, dur=2.6):
    def f(fr, t):
        X = dur - 0.4
        fr.pop(text, W / 2, 500, 80, acc, t, 0.1, X)
        if sub: fr.ktext(sub, W / 2, 610, 44, D, t, 0.5, X)
    return (dur, f)


def dots(fr, pts, col, t, t0, step=0.06, r=26, t1=None, a=1.0):
    for i, (x, y) in enumerate(pts):
        p = env(t, t0 + i * step, t1, 0.3)
        if p > 0: fr.disc(x, y, r * eback(clamp((t - t0 - i * step) / 0.35)), col, p * a)


# =============================================================== counting
def sc_count(n, col, per_row=5):
    dt = 0.55 if n <= 10 else 0.38
    dur = 2.0 + n * dt + 3.0
    def f(fr, t):
        X = dur - 0.45
        pts = grid_pos(n, 700, 540, per_row, 96)
        k = 0
        for i, (x, y) in enumerate(pts):
            ts = 1.0 + i * dt
            p = env(t, ts, X, 0.25)
            if p > 0:
                k = i + 1
                fr.disc(x, y, 34 * eback(clamp((t - ts) / 0.35)), col, p)
                fr.text(str(i + 1), x, y + 2, 30, BG, p)
        fr.rect(1300, 300, 440, 480, C['card'], env(t, 0.3, X), r=36)
        if k: fr.text(str(k), 1520, 520, 230, col if t < 1.0 + n * dt + 0.3 else AU, env(t, 0.6, X))
        fr.ktext('How many?', 1520, 700, 44, D, t, 0.5, 1.0 + n * dt)
        fr.ktext(str(n) + ' in all!', 1520, 700, 48, AU, t, 1.3 + n * dt, X)
    return (dur, f)


def sc_skip(step, count, col):
    top = step * count
    dur = 2.5 + count * 1.0 + 3.5
    def f(fr, t):
        X = dur - 0.45
        x0, x1, y = 160, 1760, 640
        a = env(t, 0.2, X)
        fr.line([(x0 - 30, y), (x1 + 30, y)], WH, a, 6)
        for v in range(0, top + 1):
            x = lerp(x0, x1, v / top)
            big = v % step == 0
            if top <= 40 or big:
                fr.line([(x, y - (18 if big else 10)), (x, y + (18 if big else 10))], WH, a * (1 if big else 0.5), 4 if big else 3)
            if big: fr.text(str(v), x, y + 55, 34, WH, a)
        for i in range(count):
            ts = 1.6 + i * 1.0
            xa, xb = lerp(x0, x1, i * step / top), lerp(x0, x1, (i + 1) * step / top)
            fr.hop(xa, xb, y - 10, 150, col, clamp((t - ts) / 0.55), env(t, ts, X, 0.1))
            fr.ktext('+' + str(step), (xa + xb) / 2, y - 190, 34, col, t, ts + 0.2, X)
        seq = [str(step * (i + 1)) for i in range(count)]
        shown = [s for i, s in enumerate(seq) if t >= 1.6 + i * 1.0 + 0.5]
        if shown:
            fr.text(',  '.join(shown), W / 2, 300, 54 if count <= 10 else 44, AU, env(t, 2.0, X))
        fr.ktext('Count by ' + str(step) + 's', W / 2, 190, 60, WH, t, 0.3, X)
    return (dur, f)


# =============================================================== place value
def blocks(fr, x, y, h, t_, ones, tens, hundreds, a, cols=(PU, B, G), t0=0.0):
    """hundreds flats, tens rods, ones cubes laid out left-to-right starting at x (y = top)"""
    u = 16
    xx = x
    k = 0
    for i in range(hundreds):
        p = env(t_, t0 + k * 0.15, None, 0.3) * a; k += 1
        fr.rect(xx, y, u * 10, u * 10, cols[0], p, r=4)
        for j in range(1, 10):
            fr.line([(xx + j * u, y), (xx + j * u, y + u * 10)], BG, p * 0.5, 1)
            fr.line([(xx, y + j * u), (xx + u * 10, y + j * u)], BG, p * 0.5, 1)
        xx += u * 10 + 14
    for i in range(tens):
        p = env(t_, t0 + k * 0.15, None, 0.3) * a; k += 1
        fr.rect(xx, y, u, u * 10, cols[1], p, r=3)
        for j in range(1, 10): fr.line([(xx, y + j * u), (xx + u, y + j * u)], BG, p * 0.5, 1)
        xx += u + 10
    xx += 6
    for i in range(ones):
        p = env(t_, t0 + k * 0.15, None, 0.3) * a; k += 1
        fr.rect(xx + (i % 3) * (u + 6), y + u * 10 - (i // 3 + 1) * (u + 6), u, u, cols[2], p, r=3)


def sc_place(n):
    h, tn, o = n // 100, (n // 10) % 10, n % 10
    dur = 11.0
    def f(fr, t):
        X = dur - 0.45
        fr.pop(str(n), W / 2, 220, 140, WH, t, 0.2, X)
        cols = [('Hundreds', PU, h), ('Tens', B, tn), ('Ones', G, o)] if n >= 100 else [('Tens', B, tn), ('Ones', G, o)]
        cw = 520 if n >= 100 else 640
        sx = W / 2 - cw * len(cols) / 2
        for i, (nm, col, dg) in enumerate(cols):
            x = sx + i * cw
            a = env(t, 1.0 + i * 0.3, X)
            fr.rect(x + 12, 330, cw - 24, 560, C['card'], a, r=30, outline=col, ow=3)
            fr.text(nm, x + cw / 2, 380, 40, col, a)
            ts = 2.0 + i * 1.6
            if nm == 'Hundreds': blocks(fr, x + 40, 460, 0, t, 0, 0, dg, a, t0=ts)
            elif nm == 'Tens': blocks(fr, x + 40, 460, 0, t, 0, dg, 0, a, t0=ts)
            else: blocks(fr, x + 40, 460, 0, t, dg, 0, 0, a, t0=ts)
            fr.pop(str(dg), x + cw / 2, 800, 90, col, t, ts + 1.0, X)
        parts = []
        if n >= 100: parts += [(str(h * 100), PU), '+']
        parts += [(str(tn * 10), B), '+', (str(o), G)]
        fr.runs([(str(n), WH), '='] + parts, W / 2, 970, 56, t, 7.2, X)
    return (dur, f)


def sc_compare(a_, b_):
    dur = 8.5
    sym = '<' if a_ < b_ else ('>' if a_ > b_ else '=')
    word = {'<': 'is less than', '>': 'is greater than', '=': 'is equal to'}[sym]
    def f(fr, t):
        X = dur - 0.45
        mx = max(a_, b_)
        for i, (v, col, x) in enumerate([(a_, P, 560), (b_, B, 1360)]):
            fr.pop(str(v), x, 330, 130, col, t, 0.3 + i * 0.4, X)
            hgt = 380 * eo(clamp((t - 1.4) / 1.2)) * v / mx
            fr.rect(x - 90, 860 - hgt, 180, hgt, col, env(t, 1.4, X), r=14)
        fr.pop(sym, W / 2, 330, 190, AU, t, 3.2, X)
        fr.runs([(str(a_), P), (word, WH), (str(b_), B)], W / 2, 950, 52, t, 4.2, X)
    return (dur, f)


def sc_evenodd(n):
    dur = 4.0 + n * 0.25 + 3.0
    even = n % 2 == 0
    def f(fr, t):
        X = dur - 0.45
        fr.pop(str(n), W / 2, 230, 120, WH, t, 0.2, X)
        pairs = math.ceil(n / 2)
        sx = W / 2 - (pairs - 1) * 95 / 2
        for i in range(n):
            col_i, row = i // 2, i % 2
            x, y = sx + col_i * 95, 470 + row * 110
            ts = 1.0 + i * 0.22
            lone = (not even) and i == n - 1
            col = (R if lone and t > 2.4 + n * 0.25 else (G if row == 0 else B))
            p = env(t, ts, X, 0.3)
            fr.disc(x, y, 36 * eback(clamp((t - ts) / 0.35)), col, p)
            if row == 1 or lone:
                pp = env(t, 1.6 + n * 0.25 + col_i * 0.08, X, 0.3)
                if row == 1: fr.rect(x - 50, 410, 100, 220, None, pp, r=48, outline=AU, ow=4)
        msg = ('Everyone has a partner: EVEN' if even else 'One is left over: ODD')
        fr.ktext(msg, W / 2, 800, 60, G if even else R, t, 2.6 + n * 0.25, X)
        fr.ktext('Even numbers end in 0, 2, 4, 6, 8' if even else 'Odd numbers end in 1, 3, 5, 7, 9', W / 2, 900, 40, D, t, 3.2 + n * 0.25, X)
    return (dur, f)


def sc_round(n, base):
    lo = (n // base) * base; hi = lo + base
    near = lo if n - lo < hi - n else hi
    dur = 10.0
    def f(fr, t):
        X = dur - 0.45
        x0, x1, y = 260, 1660, 600
        a = env(t, 0.3, X)
        fr.line([(x0, y), (x1, y)], WH, a, 6)
        for v in range(11):
            x = lerp(x0, x1, v / 10)
            big = v in (0, 5, 10)
            fr.line([(x, y - (24 if big else 12)), (x, y + (24 if big else 12))], WH, a, 4)
            if v in (0, 10): fr.text(str(lo if v == 0 else hi), x, y + 70, 46, AU, a)
            elif base == 10: fr.text(str(lo + v), x, y + 60, 30, D, a)
        mid = lerp(x0, x1, 0.5)
        fr.ktext('half way: ' + str(lo + base // 2), mid, y + 130, 34, D, t, 1.4, X)
        xn = lerp(x0, x1, (n - lo) / base)
        p = env(t, 2.2, X)
        fr.disc(xn, y, 22, P, p)
        fr.ktext(str(n), xn, y - 70, 60, P, t, 2.3, X)
        xt = lerp(x0, x1, (near - lo) / base)
        q = clamp((t - 3.6) / 1.2)
        fr.hop(xn, xt, y - 30, 120, G, q, env(t, 3.6, X, 0.1))
        fr.ktext('closer to ' + str(near), W / 2, 250, 56, G, t, 5.0, X)
        fr.runs([(str(n), P), 'rounds to', (str(near), G)], W / 2, 900, 60, t, 6.0, X)
    return (dur, f)


# =============================================================== adding & subtracting
def sc_add(a_, b_):
    n = a_ + b_
    dur = 4.6 + n * 0.22 + 3.4
    def f(fr, t):
        X = dur - 0.45
        pa = grid_pos(a_, 520, 470, 5, 76); pb = grid_pos(b_, 1400, 470, 5, 76)
        final = grid_pos(n, W / 2, 470, 10, 76)
        m = eio(clamp((t - 2.8) / 1.1))
        for i in range(n):
            src = pa[i] if i < a_ else pb[i - a_]
            x, y = lerp(src[0], final[i][0], m), lerp(src[1], final[i][1], m)
            ts = 0.6 + (i if i < a_ else i - a_ + 0.0) * 0.07 + (0 if i < a_ else 0.8)
            p = env(t, ts, X, 0.3)
            fr.disc(x, y, 28 * eback(clamp((t - ts) / 0.35)), P if i < a_ else B, p)
            tc = 4.2 + i * 0.22
            if t > tc: fr.text(str(i + 1), x, y + 1, 24, BG, env(t, tc, X, 0.15))
        fr.text('+', W / 2, 470, 90, WH, env(t, 1.2, 2.9))
        fr.ktext(str(a_), 520, 250, 70, P, t, 0.8, 2.9)
        fr.ktext(str(b_), 1400, 250, 70, B, t, 1.4, 2.9)
        fr.runs([(str(a_), P), '+', (str(b_), B), '=', (str(n) if t > 4.2 + n * 0.22 else '?', AU)], W / 2, 830, 110, t, 1.8, X, 0.15)
    return (dur, f)


def sc_sub(a_, b_):
    c_ = a_ - b_
    dur = 6.0 + b_ * 0.3 + 3.5
    def f(fr, t):
        X = dur - 0.45
        pts = grid_pos(a_, W / 2, 470, 10 if a_ > 10 else 5, 80)
        for i, (x, y) in enumerate(pts):
            gone = i >= c_
            ts = 0.5 + i * 0.05
            p = env(t, ts, X, 0.3)
            if gone:
                k = (a_ - 1 - i)
                tx = 2.6 + k * 0.3
                fly = clamp((t - tx - 0.6) / 0.8)
                p *= 1 - fly * 0.82
                y -= 120 * eio(fly)
                fr.disc(x, y, 28, B, p)
                fr.cross(x, y, 18, R, clamp((t - tx) / 0.4), p)
            else:
                fr.disc(x, y, 28 * eback(clamp((t - ts) / 0.35)), B, p)
                tc = 3.6 + b_ * 0.3 + i * 0.15
                if t > tc: fr.text(str(i + 1), x, y + 1, 24, BG, env(t, tc, X, 0.15))
        fr.ktext('Start with ' + str(a_), W / 2, 220, 56, WH, t, 0.4, 2.4)
        fr.ktext('Take away ' + str(b_), W / 2, 220, 56, R, t, 2.6, X)
        done = 3.6 + b_ * 0.3 + c_ * 0.15
        fr.runs([(str(a_), B), '−', (str(b_), R), '=', (str(c_) if t > done else '?', AU)], W / 2, 830, 110, t, 1.4, X, 0.15)
    return (dur, f)


def sc_line(a_, b_, op='+', top=20):
    c_ = a_ + b_ if op == '+' else a_ - b_
    dur = 4.0 + b_ * 0.55 + 3.0
    def f(fr, t):
        X = dur - 0.45
        x0, x1, y = 130, 1790, 650
        a = env(t, 0.2, X)
        fr.line([(x0 - 30, y), (x1 + 30, y)], WH, a, 6)
        for v in range(top + 1):
            x = lerp(x0, x1, v / top)
            fr.line([(x, y - 14), (x, y + 14)], WH, a, 3)
            fr.text(str(v), x, y + 50, 30 if top <= 20 else 22, AU if v == c_ and t > 2.2 + b_ * 0.55 else WH, a)
        xa = lerp(x0, x1, a_ / top)
        fr.disc(xa, y, 20, P, env(t, 0.8, X))
        fr.ktext('start', xa, y - 230, 36, P, t, 0.9, X)
        d = 1 if op == '+' else -1
        for i in range(b_):
            ts = 1.8 + i * 0.55
            xs, xe = lerp(x0, x1, (a_ + d * i) / top), lerp(x0, x1, (a_ + d * (i + 1)) / top)
            fr.hop(xs, xe, y - 12, 90, G if op == '+' else R, clamp((t - ts) / 0.45), env(t, ts, X, 0.1), 5)
            fr.ktext(str(i + 1), (xs + xe) / 2, y - 125, 28, G if op == '+' else R, t, ts + 0.2, X)
        fr.disc(lerp(x0, x1, c_ / top), y, 22, AU, env(t, 2.0 + b_ * 0.55, X))
        fr.runs([(str(a_), P), '−' if op == '-' else '+', (str(b_), G if op == '+' else R), '=',
                 (str(c_) if t > 2.2 + b_ * 0.55 else '?', AU)], W / 2, 300, 100, t, 1.0, X, 0.15)
    return (dur, f)


def sc_maketen(a_, b_):
    n = a_ + b_
    give = 10 - a_
    rest = b_ - give
    dur = 12.0
    def frame10(fr, x, y, a):
        fr.rect(x, y, 5 * 100, 2 * 100, None, a, r=12, outline=WH, ow=4)
        for i in range(1, 5): fr.line([(x + i * 100, y), (x + i * 100, y + 200)], WH, a * 0.6, 3)
        fr.line([(x, y + 100), (x + 500, y + 100)], WH, a * 0.6, 3)
    def cell(x, y, i): return (x + 50 + (i % 5) * 100, y + 50 + (i // 5) * 100)
    def f(fr, t):
        X = dur - 0.45
        a = env(t, 0.2, X)
        F1, F2 = (300, 330), (1120, 330)
        frame10(fr, *F1, a); frame10(fr, *F2, a)
        for i in range(a_):
            x, y = cell(*F1, i); fr.disc(x, y, 36, P, env(t, 0.5 + i * 0.08, X))
        for j in range(b_):
            sx, sy = cell(*F2, j)
            if j < give:
                tx, ty = cell(*F1, a_ + j)
                m = eio(clamp((t - 3.0 - j * 0.35) / 0.8))
                x, y = lerp(sx, tx, m), lerp(sy, ty, m)
            else:
                x, y = cell(*F2, j - give) if t > 3.0 + give * 0.35 + 0.6 else (sx, sy)
                if 3.0 + give * 0.35 < t <= 3.0 + give * 0.35 + 0.6:
                    tx, ty = cell(*F2, j - give); m = eio(clamp((t - 3.0 - give * 0.35) / 0.6)); x, y = lerp(sx, tx, m), lerp(sy, ty, m)
            fr.disc(x, y, 36, B, env(t, 1.2 + j * 0.08, X))
        fr.ktext('Fill the ten first!', W / 2, 220, 56, AU, t, 2.4, X)
        fr.runs([(str(a_), P), '+', (str(b_), B), '=', ('10', AU), '+', (str(rest), B), '=', (str(n), G)], W / 2, 760, 88, t, 6.0, X, 0.3)
        fr.ktext('Ten and ' + str(rest) + ' more makes ' + str(n), W / 2, 900, 44, D, t, 9.0, X)
    return (dur, f)


def sc_double(n_):
    dur = 7.5 + n_ * 0.1
    def f(fr, t):
        X = dur - 0.45
        pl = grid_pos(n_, 620, 470, 5, 80); pr = grid_pos(n_, 1300, 470, 5, 80)
        dots(fr, pl, P, t, 0.4, 0.07, 30, X)
        m = clamp((t - 1.8) / 0.8)
        for i, (x, y) in enumerate(pr):
            fr.disc(lerp(pl[i][0], x, eio(m)), y, 30, B, env(t, 1.8, X))
        fr.line([(W / 2, 300), (W / 2, 300 + 340 * eo(clamp((t - 1.4) / 0.6)))], AU, env(t, 1.4, X), 4)
        fr.ktext('the same again', W / 2, 240, 40, AU, t, 1.6, X)
        fr.runs([(str(n_), P), '+', (str(n_), B), '=', (str(2 * n_), AU)], W / 2, 820, 110, t, 3.0, X, 0.2)
        fr.ktext('double ' + str(n_) + ' is ' + str(2 * n_), W / 2, 950, 46, D, t, 4.6, X)
    return (dur, f)


def sc_add9(a_):
    dur = 10.0
    def f(fr, t):
        X = dur - 0.45
        fr.runs([(str(a_), P), '+', ('9', G), '=', ('?', AU)], W / 2, 220, 100, t, 0.2, 3.0, 0.12)
        fr.runs([(str(a_), P), '+', ('10', G), '−', ('1', R), '=', (str(a_ + 9), AU)], W / 2, 220, 100, t, 3.2, X, 0.25)
        lo = (a_ // 10) * 10
        x0, x1, y = 160, 1760, 640
        a = env(t, 0.5, X)
        fr.line([(x0, y), (x1, y)], WH, a, 6)
        for v in range(0, 31):
            x = lerp(x0, x1, v / 30); val = lo + v
            fr.line([(x, y - 12), (x, y + 12)], WH, a, 3)
            if v % 2 == 0: fr.text(str(val), x, y + 46, 26, WH, a)
        def X_(v): return lerp(x0, x1, (v - lo) / 30)
        fr.disc(X_(a_), y, 18, P, env(t, 0.8, X))
        fr.hop(X_(a_), X_(a_ + 10), y - 12, 200, G, clamp((t - 4.0) / 0.9), env(t, 4.0, X, 0.1))
        fr.ktext('+10', (X_(a_) + X_(a_ + 10)) / 2, y - 250, 40, G, t, 4.4, X)
        fr.hop(X_(a_ + 10), X_(a_ + 9), y - 12, 70, R, clamp((t - 5.5) / 0.6), env(t, 5.5, X, 0.1))
        fr.ktext('−1', X_(a_ + 9.5), y - 110, 34, R, t, 5.8, X)
        fr.disc(X_(a_ + 9), y, 20, AU, env(t, 6.2, X))
        fr.ktext('Adding 9? Add 10, then take 1 away.', W / 2, 900, 46, D, t, 6.6, X)
    return (dur, f)


def sc_column(a_, b_, op='+'):
    n = max(len(str(a_)), len(str(b_))) + (1 if op == '+' else 0)
    da = [int(c) for c in str(a_).rjust(n, '0')]
    db = [int(c) for c in str(b_).rjust(n, '0')]
    res = [0] * n
    carry_in = [0] * n            # '+': carry arriving in column i
    hist = {i: [] for i in range(n)}  # '-': list of (step k, new value) for the top digit
    side = {}
    if op == '+':
        c = 0
        for i in range(n - 1, -1, -1):
            carry_in[i] = c
            s = da[i] + db[i] + c
            res[i] = s % 10; c = s // 10
            side[i] = (f'{da[i]} + {db[i]}' + (' + 1' if carry_in[i] else '') + f' = {s}', f'write {s % 10}, carry 1' if s >= 10 else '')
    else:
        top = da[:]
        for i in range(n - 1, -1, -1):
            k = n - 1 - i
            note = ''
            if top[i] < db[i]:
                j = i - 1
                while top[j] == 0: j -= 1
                top[j] -= 1; hist[j].append((k, top[j]))
                for m in range(j + 1, i): top[m] = 9; hist[m].append((k, 9))
                top[i] += 10; hist[i].append((k, top[i]))
                note = 'too small: borrow 10'
            res[i] = top[i] - db[i]
            side[i] = (f'{top[i]} \u2212 {db[i]} = {res[i]}', note)
    ans = a_ + b_ if op == '+' else a_ - b_
    lead_a, lead_b, lead_r = len(str(a_)), len(str(b_)), len(str(ans))
    step = 2.6
    dur = 3.0 + n * step + 3.2
    names = ['ones', 'tens', 'hundreds', 'thousands']
    def f(fr, t):
        X = dur - 0.45
        cw = 150; x0 = W / 2 - n * cw / 2 - 160; ya, yb, yr = 340, 510, 710
        def cx(i): return x0 + i * cw + cw / 2
        def kt(k): return 2.0 + k * step
        a = env(t, 0.2, X)
        cur = int((t - 2.0) // step) if t > 2.0 else -1
        if 0 <= cur < n:
            i = n - 1 - cur
            fr.rect(cx(i) - cw / 2 + 8, 200, cw - 16, 600, C['card'], env(t, kt(cur), X, 0.25), r=26, outline=AU, ow=3)
            fr.text(names[cur], cx(i), 850, 32, AU, a)
            s1, s2 = side[i]
            fr.ktext(s1, 1500, 430, 50, AU, t, kt(cur) + 0.4, kt(cur + 1) - 0.2)
            if s2: fr.ktext(s2, 1500, 510, 36, R, t, kt(cur) + 0.8, kt(cur + 1) - 0.2)
        for i in range(n):
            if i >= n - lead_a:
                changed = [v for k, v in hist[i] if t >= kt(k) + 0.3]
                fr.text(str(da[i]), cx(i), ya, 110, D if changed else WH, a)
                if hist[i]:
                    k0 = hist[i][0][0]
                    p = clamp((t - kt(k0) - 0.3) / 0.35)
                    if p > 0: fr.line([(cx(i) - 45, ya + 45), (cx(i) - 45 + 90 * p, ya + 45 - 90 * p)], R, a, 6)
                    for idx, (k, v) in enumerate(hist[i]):
                        nxt = hist[i][idx + 1][0] if idx + 1 < len(hist[i]) else None
                        fr.pop(str(v), cx(i) + 8, ya - 115, 52, R, t, kt(k) + 0.5, None if nxt is None else kt(nxt) + 0.4)
            if i >= n - lead_b: fr.text(str(db[i]), cx(i), yb, 110, WH, a)
            if op == '+' and carry_in[i]:
                fr.pop('1', cx(i), ya - 115, 52, R, t, kt(n - 1 - i - 1) + 1.0, X)
            if i >= n - lead_r: fr.pop(str(res[i]), cx(i), yr, 110, G, t, kt(n - 1 - i) + 1.3, X)
        fr.text('+' if op == '+' else '\u2212', x0 - 50, yb, 110, AU, a)
        fr.line([(x0 - 110, 610), (x0 + n * cw + 10, 610)], WH, a, 7)
        fr.runs([(str(a_), WH), '+' if op == '+' else '\u2212', (str(b_), WH), '=', (str(ans), G)], W / 2, 970, 64, t, 2.4 + n * step, X)
    return (dur, f)


def sc_family(a_, b_):
    c_ = a_ + b_
    dur = 10.0
    def f(fr, t):
        X = dur - 0.45
        cx, cy = 480, 560
        tri = [(cx, cy - 220), (cx - 230, cy + 170), (cx + 230, cy + 170)]
        fr.poly(tri, C['card'], env(t, 0.2, X), outline=AU, ow=5)
        fr.pop(str(c_), tri[0][0], tri[0][1] + 80, 90, AU, t, 0.5, X)
        fr.pop(str(a_), tri[1][0] + 80, tri[1][1] - 60, 80, P, t, 0.8, X)
        fr.pop(str(b_), tri[2][0] - 80, tri[2][1] - 60, 80, B, t, 1.1, X)
        eqs = [[(str(a_), P), '+', (str(b_), B), '=', (str(c_), AU)], [(str(b_), B), '+', (str(a_), P), '=', (str(c_), AU)],
               [(str(c_), AU), '−', (str(a_), P), '=', (str(b_), B)], [(str(c_), AU), '−', (str(b_), B), '=', (str(a_), P)]]
        for i, e in enumerate(eqs):
            fr.runs(e, 1300, 300 + i * 140, 80, t, 2.0 + i * 1.3, X, 0.08)
        fr.ktext('3 numbers, 4 facts', W / 2, 960, 46, D, t, 7.4, X)
    return (dur, f)


def sc_word(lines, eq, ans, icon_n=0, col=P):
    dur = 3.0 + len(lines) * 1.6 + 5.5
    def f(fr, t):
        X = dur - 0.45
        fr.rect(180, 170, W - 360, 120 + len(lines) * 76, C['card'], env(t, 0.2, X), r=34)
        for i, ln in enumerate(lines):
            fr.ktext(ln, W / 2, 240 + i * 76, 46, WH, t, 0.6 + i * 1.6, X)
        t2 = 1.4 + len(lines) * 1.6
        if icon_n:
            pts = grid_pos(icon_n, W / 2, 330 + len(lines) * 76 + 120, 10, 64)
            dots(fr, pts, col, t, t2, 0.05, 22, X)
        fr.runs(eq, W / 2, 820, 90, t, t2 + 1.2, X, 0.15)
        fr.ktext(ans, W / 2, 950, 52, AU, t, t2 + 3.0, X)
        fr.tick(W / 2 + fr.tw(ans, 52) / 2 + 70, 945, 50, G, t, t2 + 3.5, X)
    return (dur, f)


# =============================================================== times & dividing
def sc_groups(a_, b_):
    c_ = a_ * b_
    dur = 3.0 + a_ * 0.9 + 3.5
    def f(fr, t):
        X = dur - 0.45
        per = min(a_, 5)
        rows = math.ceil(a_ / per)
        gw = 300
        for g in range(a_):
            r, c = divmod(g, per)
            gx = W / 2 - (per - 1) * gw / 2 + c * gw
            gy = 400 + r * 300 - (rows - 1) * 150
            ts = 0.6 + g * 0.9
            p = env(t, ts, X, 0.3)
            fr.disc(gx, gy, 120 * eback(clamp((t - ts) / 0.4)), C['card'], p, outline=B, ow=4)
            pts = grid_pos(b_, gx, gy, 3 if b_ > 4 else 2, 54)
            dots(fr, pts, P, t, ts + 0.2, 0.06, 18, X)
            fr.ktext(str(b_ * (g + 1)), gx, gy + 150, 40, AU, t, ts + 0.5, X)
        fr.runs([(str(a_), B), 'groups of', (str(b_), P), '=', (str(c_), AU)], W / 2, 140 + 0 * 0, 60, t, 0.3, X)
        fr.runs([(str(a_), B), '×', (str(b_), P), '=', (str(c_), AU)], W / 2, 960, 90, t, 1.0 + a_ * 0.9, X)
    return (dur, f)


def sc_array(r_, c_):
    n = r_ * c_
    dur = 3.0 + r_ * 0.7 + 5.0
    def f(fr, t):
        X = dur - 0.45
        sp = min(90, 620 / max(r_, 1), 1000 / max(c_, 1))
        x0 = W / 2 - 250 - (c_ - 1) * sp / 2; y0 = 520 - (r_ - 1) * sp / 2
        for i in range(r_):
            ts = 0.6 + i * 0.7
            for j in range(c_):
                p = env(t, ts + j * 0.03, X, 0.25)
                fr.disc(x0 + j * sp, y0 + i * sp, sp * 0.34, B if i % 2 == 0 else PU, p)
            fr.ktext(str(c_ * (i + 1)), x0 + c_ * sp + 30, y0 + i * sp, 34, AU, t, ts + 0.3, X, anchor='lm')
        fr.ktext(str(r_) + ' rows', x0 - 70, 520, 36, D, t, 0.8, X, anchor='rm')
        fr.ktext(str(c_) + ' in each row', x0 + (c_ - 1) * sp / 2, y0 - 70, 36, D, t, 0.8, X)
        tx = 1500
        fr.runs([(str(r_), PU), '×', (str(c_), B), '=', (str(n), AU)], tx, 440, 84, t, 1.2 + r_ * 0.7, X)
        fr.runs([(str(c_), B), '×', (str(r_), PU), '=', (str(n), AU)], tx, 600, 84, t, 2.4 + r_ * 0.7, X)
        fr.ktext('turn it around: same answer', tx, 740, 34, D, t, 3.0 + r_ * 0.7, X)
    return (dur, f)


def sc_table(n_):
    dur = 2.0 + 10 * 1.15 + 3.0
    def f(fr, t):
        X = dur - 0.45
        k = int(clamp((t - 1.0) / 1.15, 0, 9.99)) + 1 if t > 1.0 else 0
        for i in range(10):
            col_x = 380 if i < 5 else 900
            y = 270 + (i % 5) * 130
            ts = 1.0 + i * 1.15
            c = AU if i + 1 == k and t < 1.0 + 10 * 1.15 else WH
            fr.ktext(f'{n_} × {i + 1} = {n_ * (i + 1)}', col_x, y, 58, c, t, ts, X)
        if k:
            sp = min(46, 520 / max(n_, 1))
            x0 = 1500 - (n_ - 1) * sp / 2
            y0 = 560 - (k - 1) * sp / 2
            for i in range(k):
                for j in range(n_):
                    fr.disc(x0 + j * sp, y0 + i * sp, sp * 0.36, B if i < k - 1 else AU, env(t, 1.0, X))
            fr.text(f'{n_} × {k}', 1500, 160, 52, AU, env(t, 1.0, X))
    return (dur, f)


def sc_table_tip(n_, tips):
    return title_card('The ' + str(n_) + ' times table', tips, CAT_ACC['muldiv'], 4.2)


def sc_share(total, k):
    q, rem = divmod(total, k)
    dur = 3.5 + total * 0.22 + 4.0
    def f(fr, t):
        X = dur - 0.45
        pile = grid_pos(total, W / 2, 230, 12, 56)
        px = [W / 2 - (k - 1) * 380 / 2 + i * 380 for i in range(k)] if k <= 5 else [W / 2 - (k - 1) * 290 / 2 + i * 290 for i in range(k)]
        for i in range(k):
            fr.disc(px[i], 640, 130, C['card'], env(t, 0.6 + i * 0.1, X), outline=G, ow=4)
        for i in range(total):
            ts = 1.6 + i * 0.22
            sx, sy = pile[i]
            if i < q * k:
                plate, slot = i % k, i // k
                tp = grid_pos(q, px[plate], 640, 3 if q > 4 else 2, 50)[slot]
                m = eio(clamp((t - ts) / 0.5))
                x, y = lerp(sx, tp[0], m), lerp(sy, tp[1], m)
                fr.disc(x, y, 18, P, env(t, 0.3, X))
            else:
                fr.disc(sx, sy, 18, R if t > 1.6 + q * k * 0.22 else P, env(t, 0.3, X))
        t2 = 1.8 + q * k * 0.22
        if rem: fr.ktext(str(rem) + ' left over', W / 2, 330, 40, R, t, t2, X)
        for i in range(k): fr.ktext(str(q), px[i], 820, 48, AU, t, t2 + 0.2, X)
        fr.runs([(str(total), P), '÷', (str(k), G), '=', (str(q), AU)] + ([('r ' + str(rem), R)] if rem else []), W / 2, 950, 86, t, t2 + 0.8, X)
    return (dur, f)


def sc_divgroups(total, size):
    q = total // size
    dur = 3.0 + q * 0.8 + 3.5
    def f(fr, t):
        X = dur - 0.45
        pts = grid_pos(total, W / 2, 480, size, 70)
        for i, (x, y) in enumerate(pts):
            fr.disc(x, y, 24, P, env(t, 0.4 + i * 0.03, X))
        for g in range(q):
            ts = 1.6 + g * 0.8
            y = pts[g * size][1]
            x0, x1 = pts[g * size][0], pts[g * size + size - 1][0]
            fr.rect(x0 - 40, y - 40, x1 - x0 + 80, 80, None, env(t, ts, X, 0.25), r=40, outline=G, ow=4)
            fr.ktext(str(g + 1), x1 + 90, y, 40, G, t, ts, X)
        fr.ktext('How many groups of ' + str(size) + '?', W / 2, 160, 54, WH, t, 0.4, X)
        fr.runs([(str(total), P), '÷', (str(size), G), '=', (str(q), AU)], W / 2, 950, 90, t, 1.8 + q * 0.8, X)
    return (dur, f)


def sc_mulcol(a_, b_):
    ds = [int(c) for c in str(a_)]
    n = len(ds)
    step = 2.8
    dur = 2.5 + n * step + 3.5
    carries = []; c = 0; out = []
    for d in reversed(ds):
        s = d * b_ + c; out.append(s % 10); carries.append(c); c = s // 10
    ans = a_ * b_
    def f(fr, t):
        X = dur - 0.45
        L = len(str(ans)); cw = 150
        x0 = W / 2 - L * cw / 2 - 200
        def cx(i): return x0 + i * cw + cw / 2  # i from left in an L-wide grid
        a = env(t, 0.2, X)
        for i, d in enumerate(ds): fr.text(str(d), cx(L - n + i), 330, 110, WH, a)
        fr.text(str(b_), cx(L - 1), 500, 110, WH, a)
        fr.text('×', cx(L - 1) - cw, 500, 100, AU, a)
        fr.line([(cx(0) - 80, 600), (cx(L - 1) + 80, 600)], WH, a, 7)
        cur = int((t - 1.8) // step) if t > 1.8 else -1
        for k in range(n):
            ts = 1.8 + k * step
            i = L - 1 - k
            last = k == n - 1
            if 0 <= cur and cur == k:
                fr.rect(cx(i) - cw / 2 + 8, 250, cw - 16, 520, C['card'], env(t, ts, ts + step - 0.3, 0.25), r=24, outline=AU, ow=3)
                s = ds[n - 1 - k] * b_ + carries[k]
                fr.ktext(f'{ds[n - 1 - k]} × {b_}' + (f' + {carries[k]}' if carries[k] else '') + f' = {s}', 1500, 420, 50, AU, t, ts + 0.3, ts + step - 0.3)
                if s >= 10 and not last: fr.ktext(f'write {s % 10}, carry {s // 10}', 1500, 500, 36, R, t, ts + 0.8, ts + step - 0.3)
            if last:
                fr.pop(str(ans // (10 ** k)), cx(i) - (len(str(ans // (10 ** k))) - 1) * cw / 2, 700, 110, G, t, ts + 1.3, X)
            else:
                fr.pop(str(out[k]), cx(i), 700, 110, G, t, ts + 1.3, X)
            if k + 1 < n and carries[k + 1]:
                fr.pop(str(carries[k + 1]), cx(i - 1), 220, 50, R, t, ts + 1.8, X)
        fr.runs([(str(a_), WH), '×', (str(b_), WH), '=', (str(ans), G)], W / 2, 960, 66, t, 2.2 + n * step, X)
    return (dur, f)


# =============================================================== time & shapes
def sc_clock(h, m):
    dur = 9.0
    def f(fr, t):
        X = dur - 0.45
        cx, cy, r = 640, 560, 330
        a = env(t, 0.2, X)
        fr.disc(cx, cy, r, C['card'], a, outline=WH, ow=8)
        for i in range(60):
            an = i / 60 * 2 * math.pi - math.pi / 2
            big = i % 5 == 0
            r0 = r - (40 if big else 20)
            fr.line([(cx + r0 * math.cos(an), cy + r0 * math.sin(an)), (cx + (r - 12) * math.cos(an), cy + (r - 12) * math.sin(an))], WH, a * (1 if big else 0.4), 5 if big else 2)
            if big:
                fr.text(str(i // 5 or 12), cx + (r - 80) * math.cos(an), cy + (r - 80) * math.sin(an), 46, WH, a)
        p = eio(clamp((t - 1.0) / 3.0))
        tot = (h % 12) * 60 + m
        cur = tot * p
        ma = (cur % 60) / 60 * 2 * math.pi - math.pi / 2
        ha = (cur / 60) / 12 * 2 * math.pi - math.pi / 2
        fr.line([(cx, cy), (cx + (r * 0.5) * math.cos(ha), cy + (r * 0.5) * math.sin(ha))], P, a, 18)
        fr.line([(cx, cy), (cx + (r * 0.78) * math.cos(ma), cy + (r * 0.78) * math.sin(ma))], B, a, 10)
        fr.disc(cx, cy, 18, AU, a)
        fr.ktext('short hand = hours', 1380, 360, 42, P, t, 1.2, X)
        fr.ktext('long hand = minutes', 1380, 440, 42, B, t, 1.6, X)
        hh = h if h else 12
        fr.pop(f'{hh}:{m:02d}', 1380, 640, 150, AU, t, 4.4, X)
        words = {0: "o'clock", 15: 'quarter past', 30: 'half past', 45: 'quarter to'}
        if m in words:
            w = f"{hh} o'clock" if m == 0 else (f'{words[m]} {hh}' if m != 45 else f'quarter to {hh % 12 + 1}')
        else:
            w = f'{m} minutes past {hh}'
        fr.ktext(w, 1380, 800, 50, WH, t, 5.2, X)
    return (dur, f)


def sc_shape(sides, name, col):
    dur = 3.0 + sides * 0.6 + 3.0
    def f(fr, t):
        X = dur - 0.45
        cx, cy, r = 640, 560, 300
        if sides == 0:
            p = clamp((t - 0.8) / 2.0)
            pts = [(cx + r * math.cos(-math.pi / 2 + 2 * math.pi * u / 80), cy + r * math.sin(-math.pi / 2 + 2 * math.pi * u / 80)) for u in range(int(80 * p) + 1)]
            fr.line(pts, col, env(t, 0.6, X), 10)
            fr.ktext('0 sides, 0 corners', 1380, 560, 54, WH, t, 3.0, X)
        else:
            vs = [(cx + r * math.cos(-math.pi / 2 + 2 * math.pi * i / sides + (math.pi / sides if sides == 4 else 0)),
                   cy + r * math.sin(-math.pi / 2 + 2 * math.pi * i / sides + (math.pi / sides if sides == 4 else 0))) for i in range(sides)]
            if t > 1.0 + sides * 0.6: fr.poly(vs, col, 0.25 * env(t, 1.0 + sides * 0.6, X))
            for i in range(sides):
                ts = 0.8 + i * 0.6
                a_, b_ = vs[i], vs[(i + 1) % sides]
                u = eo(clamp((t - ts) / 0.5))
                if u > 0:
                    fr.line([a_, (lerp(a_[0], b_[0], u), lerp(a_[1], b_[1], u))], col, env(t, ts, X, 0.1), 10)
                    mx, my = (a_[0] + b_[0]) / 2, (a_[1] + b_[1]) / 2
                    fr.ktext(str(i + 1), cx + (mx - cx) * 1.22, cy + (my - cy) * 1.22, 40, AU, t, ts + 0.3, X)
                fr.disc(vs[i][0], vs[i][1], 14, WH, env(t, ts, X))
            fr.ktext(f'{sides} sides', 1380, 520, 64, WH, t, 1.2 + sides * 0.6, X)
            fr.ktext(f'{sides} corners', 1380, 620, 64, WH, t, 1.6 + sides * 0.6, X)
        fr.pop(name, 1380, 360, 96, col, t, 0.4, X)
    return (dur, f)


def sc_perimeter(w_, h_):
    dur = 10.0
    per = 2 * (w_ + h_)
    def f(fr, t):
        X = dur - 0.45
        sc = min(900 / w_, 520 / h_)
        rw, rh = w_ * sc, h_ * sc
        x0, y0 = 560 - rw / 2, 540 - rh / 2
        corners = [(x0, y0), (x0 + rw, y0), (x0 + rw, y0 + rh), (x0, y0 + rh)]
        lens = [w_, h_, w_, h_]
        fr.rect(x0, y0, rw, rh, C['card'], env(t, 0.3, X))
        for i in range(4):
            ts = 1.0 + i * 1.0
            a_, b_ = corners[i], corners[(i + 1) % 4]
            u = eo(clamp((t - ts) / 0.8))
            if u > 0: fr.line([a_, (lerp(a_[0], b_[0], u), lerp(a_[1], b_[1], u))], O, env(t, ts, X, 0.1), 10)
            mx, my = (a_[0] + b_[0]) / 2, (a_[1] + b_[1]) / 2
            off = [(0, -50), (60, 0), (0, 50), (-60, 0)][i]
            fr.ktext(f'{lens[i]} cm', mx + off[0], my + off[1], 38, AU, t, ts + 0.3, X)
        fr.ktext('Perimeter = the distance all the way around', 1420, 300, 36, D, t, 0.6, X)
        fr.runs([str(w_), '+', str(h_), '+', str(w_), '+', str(h_)], 1420, 520, 64, t, 5.2, X, 0.1)
        fr.runs(['=', (f'{per} cm', AU)], 1420, 660, 84, t, 6.6, X)
    return (dur, f)


# =============================================================== the lessons
def L(id_, cat, title, sub, scenes_fn, recap_lines):
    return dict(id=id_, cat=cat, title=title, sub=sub, scenes=scenes_fn, recap=recap_lines)


def WP(lines, eq, ans, n=0, col=P): return lambda: sc_word(lines, eq, ans, n, col)


LESSONS = [
    # ---------------- counting & place value
    L('count_10', 'basics', 'Counting to 10', 'Point at each one and say the number',
      lambda: [sc_count(3, P), sc_count(5, B), sc_count(7, G), sc_count(10, AU)], ['Touch each thing once', 'The last number you say is how many']),
    L('count_20', 'basics', 'Counting to 20', 'Count in rows of ten',
      lambda: [sc_count(12, B, 10), sc_count(15, P, 10), sc_count(18, G, 10), sc_count(20, AU, 10)], ['10 and 2 more is 12', '2 full rows of 10 make 20']),
    L('skip_2', 'basics', 'Counting by 2s', '2, 4, 6, 8 ...', lambda: [sc_skip(2, 10, P), sc_evenodd(8)], ['Counting by 2s lands on even numbers']),
    L('skip_5', 'basics', 'Counting by 5s', '5, 10, 15, 20 ...', lambda: [sc_skip(5, 10, B), title_card('5s end in 5 or 0', '5, 10, 15, 20, 25 ...', B)], ['Counting by 5s ends in 5 or 0']),
    L('skip_10', 'basics', 'Counting by 10s', '10, 20, 30 ... 100', lambda: [sc_skip(10, 10, G), sc_skip(10, 5, AU)], ['Counting by 10s always ends in 0']),
    L('place_tens', 'basics', 'Tens and ones', 'What the digits in a number mean',
      lambda: [sc_place(23), sc_place(47), sc_place(60), sc_place(85)], ['The left digit counts tens', 'The right digit counts ones']),
    L('place_hundreds', 'basics', 'Hundreds, tens and ones', 'Three-digit numbers',
      lambda: [sc_place(124), sc_place(305), sc_place(472)], ['347 = 300 + 40 + 7', 'A 0 holds an empty place']),
    L('compare', 'basics', 'Bigger or smaller?', 'Using < and >',
      lambda: [sc_compare(3, 8), sc_compare(15, 9), sc_compare(42, 24), sc_compare(7, 7)], ['The open side faces the bigger number', '= means both sides are the same']),
    L('even_odd', 'basics', 'Even and odd', 'Can everyone have a partner?',
      lambda: [sc_evenodd(6), sc_evenodd(7), sc_evenodd(10), sc_evenodd(13)], ['Even: everyone has a partner', 'Odd: one is left over']),
    L('round_10', 'basics', 'Rounding to the nearest 10', 'Which ten is closer?',
      lambda: [sc_round(23, 10), sc_round(47, 10), sc_round(65, 10), sc_round(81, 10)], ['5 or more rounds up', '4 or less rounds down']),
    L('round_100', 'basics', 'Rounding to the nearest 100', 'Look at the tens digit',
      lambda: [sc_round(240, 100), sc_round(380, 100), sc_round(650, 100)], ['Tens digit 5 or more: round up']),
    # ---------------- adding & subtracting
    L('add_intro', 'addsub', 'What is adding?', 'Put groups together and count them all',
      lambda: [sc_add(2, 3), sc_add(4, 2), sc_add(3, 5), sc_add(6, 4)], ['Adding puts groups together', '+ means plus, = means makes']),
    L('add_more', 'addsub', 'Adding up to 20', 'Bigger groups, same idea',
      lambda: [sc_add(7, 5), sc_add(8, 6), sc_add(9, 9), sc_add(6, 7)], ['You can count on from the bigger number']),
    L('add_line', 'addsub', 'Adding on a number line', 'Start, then hop forward',
      lambda: [sc_line(3, 4), sc_line(8, 5), sc_line(11, 6), sc_line(9, 9)], ['Adding moves you to the right']),
    L('doubles', 'addsub', 'Doubles', '3 + 3, 4 + 4, 5 + 5 ...',
      lambda: [sc_double(2), sc_double(3), sc_double(5), sc_double(6), sc_double(8)], ['A double is a number added to itself']),
    L('make_ten', 'addsub', 'Make a ten', 'A clever way to add',
      lambda: [sc_maketen(8, 5), sc_maketen(9, 4), sc_maketen(7, 6)], ['Fill up to 10 first', 'Then add what is left']),
    L('add_9', 'addsub', 'Adding 9 quickly', 'Add 10, then take 1 away',
      lambda: [sc_add9(14), sc_add9(23), sc_add9(36)], ['+ 9 is the same as + 10 − 1']),
    L('sub_intro', 'addsub', 'What is subtracting?', 'Take some away and count what is left',
      lambda: [sc_sub(5, 2), sc_sub(8, 3), sc_sub(10, 4), sc_sub(12, 5)], ['Subtracting takes away', '− means minus']),
    L('sub_line', 'addsub', 'Subtracting on a number line', 'Start, then hop back',
      lambda: [sc_line(9, 4, '-'), sc_line(15, 6, '-'), sc_line(18, 9, '-'), sc_line(12, 7, '-')], ['Subtracting moves you to the left']),
    L('fact_family', 'addsub', 'Fact families', 'One triangle, four facts',
      lambda: [sc_family(3, 5), sc_family(6, 7), sc_family(4, 9)], ['Adding and subtracting undo each other']),
    L('col_add', 'addsub', 'Column adding', 'Add the ones, then the tens',
      lambda: [sc_column(23, 45), sc_column(51, 36), sc_column(142, 235)], ['Line up ones under ones', 'Always start on the right']),
    L('col_add_carry', 'addsub', 'Carrying', 'When a column makes 10 or more',
      lambda: [sc_column(38, 45), sc_column(67, 26), sc_column(276, 148)], ['10 ones become 1 ten', 'Carry it to the next column']),
    L('col_sub', 'addsub', 'Column subtracting', 'Ones first, then tens',
      lambda: [sc_column(68, 25, '-'), sc_column(97, 43, '-'), sc_column(586, 254, '-')], ['Top number minus bottom number', 'Start on the right']),
    L('col_sub_borrow', 'addsub', 'Borrowing', 'When the top digit is too small',
      lambda: [sc_column(52, 27, '-'), sc_column(83, 46, '-'), sc_column(431, 182, '-')], ['Borrow 1 ten = 10 ones', 'Cross out and write the new digit']),
    L('word_addsub', 'addsub', 'Story problems', 'Turn words into a number sentence',
      lambda: [WP(['Mia has 6 apples.', 'Her friend gives her 5 more.', 'How many apples now?'], [('6', P), '+', ('5', B), '=', ('11', AU)], '11 apples', 11, P)(),
               WP(['There are 14 birds on a fence.', '6 fly away.', 'How many are left?'], [('14', B), '−', ('6', R), '=', ('8', AU)], '8 birds', 8, B)(),
               WP(['Sam read 23 pages on Monday', 'and 35 pages on Tuesday.', 'How many pages in all?'], [('23', P), '+', ('35', B), '=', ('58', AU)], '58 pages')()],
      ['"more" / "in all": add', '"left" / "fly away": subtract']),
    # ---------------- times & dividing
    L('mul_groups', 'muldiv', 'What is multiplying?', 'Equal groups',
      lambda: [sc_groups(3, 2), sc_groups(4, 3), sc_groups(2, 5), sc_groups(5, 4)], ['× means groups of', 'It is quick adding']),
    L('mul_arrays', 'muldiv', 'Arrays', 'Rows and columns',
      lambda: [sc_array(3, 4), sc_array(2, 6), sc_array(5, 5), sc_array(4, 7)], ['rows × columns', 'You can swap the order']),
] + [
    L(f'times_{n}', 'muldiv', f'The {n} times table', tip, (lambda n=n, tip=tip: [sc_table_tip(n, tip), sc_table(n), sc_array(3, n)]), [rc])
    for n, tip, rc in [
        (2, 'Double the number', 'Times 2 = double'),
        (3, 'Count by 3s', '3, 6, 9, 12, 15 ...'),
        (4, 'Double, then double again', 'Times 4 = double double'),
        (5, 'Answers end in 5 or 0', 'Half of times 10'),
        (6, 'Times 3, then double', '6 × 7 = 42'),
        (7, 'The tricky one: practise!', '7 × 8 = 56'),
        (8, 'Double, double, double', '8 × 8 = 64'),
        (9, 'Digits add up to 9', 'Times 10, then take one away'),
        (10, 'Put a 0 on the end', '10 × anything ends in 0'),
    ]
] + [
    L('div_share', 'muldiv', 'What is dividing?', 'Share fairly',
      lambda: [sc_share(6, 2), sc_share(12, 3), sc_share(15, 5), sc_share(20, 4)], ['÷ means share equally', 'Every group gets the same']),
    L('div_groups', 'muldiv', 'Dividing into groups', 'How many groups can you make?',
      lambda: [sc_divgroups(12, 4), sc_divgroups(18, 6), sc_divgroups(20, 5)], ['Dividing undoes multiplying']),
    L('div_remainder', 'muldiv', 'Remainders', 'When it does not share evenly',
      lambda: [sc_share(7, 2), sc_share(14, 4), sc_share(17, 5)], ['The remainder is what is left over', 'It must be smaller than the groups']),
    L('mul_column', 'muldiv', 'Column multiplying', 'Big number × small number',
      lambda: [sc_mulcol(23, 3), sc_mulcol(47, 6), sc_mulcol(126, 4)], ['Multiply each digit, right to left', 'Add the carried number']),
    L('word_muldiv', 'muldiv', 'Times & share stories', 'Groups of and shared between',
      lambda: [WP(['There are 4 boxes.', 'Each box has 6 pencils.', 'How many pencils?'], [('4', B), '×', ('6', P), '=', ('24', AU)], '24 pencils', 24, P)(),
               WP(['18 sweets are shared', 'between 3 friends.', 'How many each?'], [('18', P), '÷', ('3', G), '=', ('6', AU)], '6 sweets each', 18, P)(),
               WP(['A spider has 8 legs.', 'How many legs on 5 spiders?'], [('5', B), '×', ('8', P), '=', ('40', AU)], '40 legs')()],
      ['"each" / "groups of": multiply', '"shared" / "split": divide']),
    # ---------------- time & shapes
    L('clock_hours', 'shapes', "Telling the time: o'clock", 'The short hand points to the hour',
      lambda: [sc_clock(3, 0), sc_clock(7, 0), sc_clock(12, 0), sc_clock(9, 0)], ["Long hand on 12 = o'clock"]),
    L('clock_half', 'shapes', 'Half past and quarter past', '30 minutes and 15 minutes',
      lambda: [sc_clock(2, 30), sc_clock(5, 15), sc_clock(8, 45), sc_clock(11, 30)], ['Half past: long hand on 6', 'Quarter past: long hand on 3']),
    L('clock_minutes', 'shapes', 'Reading minutes', 'Count the long hand in 5s',
      lambda: [sc_clock(4, 10), sc_clock(6, 25), sc_clock(1, 40), sc_clock(10, 55)], ['Each number on the clock is 5 minutes']),
    L('shapes_2d', 'shapes', 'Flat shapes', 'Count the sides and corners',
      lambda: [sc_shape(3, 'Triangle', P), sc_shape(4, 'Square', B), sc_shape(5, 'Pentagon', G), sc_shape(6, 'Hexagon', AU), sc_shape(0, 'Circle', PU)],
      ['Sides are straight lines', 'Corners are where sides meet']),
    L('perimeter', 'shapes', 'Perimeter', 'The distance all the way around',
      lambda: [sc_perimeter(6, 4), sc_perimeter(9, 3), sc_perimeter(5, 5)], ['Add every side', 'Rectangle: 2 × (long + short)']),
]


def build(lesson):
    acc = CAT_ACC[lesson['cat']]
    return [intro(lesson['title'], lesson['sub'], acc)] + lesson['scenes']() + [recap(lesson['recap'], acc)]


def get(id_):
    return [l for l in LESSONS if l['id'] == id_][0]


if __name__ == '__main__':
    mode, lid = sys.argv[1], sys.argv[2]
    l = get(lid)
    if mode == 'stills':
        os.makedirs('out', exist_ok=True)
        secs = [float(x) for x in sys.argv[3].split(',')]
        render(f'out/{lid}.mp4', build(l), CATS[l['cat']], l['title'], CAT_ACC[l['cat']], preview=secs)
    else:
        out = sys.argv[3]; kb = int(sys.argv[4])
        os.makedirs(out, exist_ok=True)
        print(render(f'{out}/{lid}.mp4', build(l), CATS[l['cat']], l['title'], CAT_ACC[l['cat']], kb))
