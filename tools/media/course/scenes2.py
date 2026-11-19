"""More scene types for the Matix grade 1 -> college video course (1.0.8):
worked steps, graphs (curves, points, tangents, areas), fractions, percents, geometry, probability,
statistics, matrices, vectors, sequences, inequalities and the unit circle."""
import math
from pkit import *

CARD = C['card']


def fit(fr, s, size, maxw):
    """largest size <= size so that s fits in maxw"""
    w = fr.tw(s, size)
    return size if w <= maxw else max(20, size * maxw / w)


# ============================================================ worked steps
def sc_steps(lines, notes=None, title=None, size=74, hold=2.9):
    """one line of working at a time; the newest line glows gold, older lines dim.
    lines: list of str; notes: optional list (same length) of short side notes."""
    n = len(lines)
    notes = notes or [''] * n
    dur = 1.6 + n * hold + 2.6
    top = 250 if title else 190
    gap = min(130, (930 - top) / max(n, 1))
    def f(fr, t):
        X = dur - 0.45
        if title: fr.ktext(title, W / 2, 150, 54, AU, t, 0.1, X)
        has_notes = any(notes)
        cx = 760 if has_notes else W / 2
        maxw = 1180 if has_notes else 1650
        sz = min(fit(fr, ln, min(size, gap * 0.62), maxw) for ln in lines)
        for i, ln in enumerate(lines):
            ts = 0.8 + i * hold
            y = top + gap * i + gap / 2
            cur = t < 0.8 + (i + 1) * hold or i == n - 1
            col = AU if (cur and t >= ts) else WH
            fr.ktext(ln, cx, y, sz, col, t, ts, X)
            if notes[i]:
                fr.ktext(notes[i], 1420, y, 34, D, t, ts + 0.3, X, anchor='lm')
                p = env(t, ts + 0.3, X)
                fr.line([(1390, y - 22), (1390, y + 22)], B, p, 4)

    return (dur, f)


# ============================================================ graphs
class Axes:
    def __init__(self, xr, yr, box=(140, 170, 1240, 990)):
        self.x0, self.x1 = xr; self.y0, self.y1 = yr
        self.bx0, self.by0, self.bx1, self.by1 = box

    def X(self, x): return lerp(self.bx0, self.bx1, (x - self.x0) / (self.x1 - self.x0))
    def Y(self, y): return lerp(self.by1, self.by0, (y - self.y0) / (self.y1 - self.y0))

    def draw(self, fr, a, step=None, labels=True):
        sx = step[0] if step else nice(self.x1 - self.x0)
        sy = step[1] if step else nice(self.y1 - self.y0)
        fr.rect(self.bx0, self.by0, self.bx1 - self.bx0, self.by1 - self.by0, CARD, a * 0.7, r=18)
        v = math.ceil(self.x0 / sx) * sx
        while v <= self.x1 + 1e-9:
            x = self.X(v); fr.line([(x, self.by0), (x, self.by1)], C['line'], a * 0.6, 1)
            if labels and abs(v) > 1e-9 and self.y0 <= 0 <= self.y1:
                fr.text(fmt(v), x, min(self.Y(0) + 26, self.by1 - 14), 22, D, a)
            v += sx
        v = math.ceil(self.y0 / sy) * sy
        while v <= self.y1 + 1e-9:
            y = self.Y(v); fr.line([(self.bx0, y), (self.bx1, y)], C['line'], a * 0.6, 1)
            if labels and abs(v) > 1e-9 and self.x0 <= 0 <= self.x1:
                fr.text(fmt(v), max(self.X(0) - 12, self.bx0 + 30), y, 22, D, a, anchor='rm')
            v += sy
        if self.y0 <= 0 <= self.y1: fr.line([(self.bx0, self.Y(0)), (self.bx1, self.Y(0))], WH, a * 0.85, 3)
        if self.x0 <= 0 <= self.x1: fr.line([(self.X(0), self.by0), (self.X(0), self.by1)], WH, a * 0.85, 3)

    def curve(self, fr, fn, col, p, a, w=6, xa=None, xb=None):
        xa = self.x0 if xa is None else xa; xb = self.x1 if xb is None else xb
        n = 360; seg = []
        last = int(n * clamp(p))
        for i in range(last + 1):
            x = lerp(xa, xb, i / n)
            try: y = fn(x)
            except Exception: y = None
            if y is None or isinstance(y, complex) or math.isnan(y) or math.isinf(y) or y < self.y0 - (self.y1 - self.y0) or y > self.y1 + (self.y1 - self.y0):
                if len(seg) > 1: fr.line(seg, col, a, w)
                seg = []; continue
            yy = clamp(self.Y(y), self.by0, self.by1)
            if (y < self.y0 or y > self.y1):
                if seg: seg.append((self.X(x), yy))
                if len(seg) > 1: fr.line(seg, col, a, w)
                seg = []; continue
            seg.append((self.X(x), yy))
        if len(seg) > 1: fr.line(seg, col, a, w)


def nice(span):
    raw = span / 10
    m = 10 ** math.floor(math.log10(raw))
    for k in (1, 2, 5, 10):
        if raw <= k * m: return k * m
    return 10 * m


def fmt(v):
    if abs(v - round(v)) < 1e-9: return str(int(round(v)))
    return ('%.2f' % v).rstrip('0').rstrip('.')


def sc_graph(curves, xr, yr, notes=(), marks=(), title=None, step=None, shade=None, extra=4.0):
    """curves: list of (fn, col, label). marks: list of (x, y, label, col[, 'open']). notes: right-hand lines.
    shade: (fn, a, b, col) area under fn from a to b."""
    nc, nn = len(curves), len(notes)
    dur = 2.0 + nc * 1.8 + max(0, nn - nc) * 1.4 + len(marks) * 0.8 + extra
    ax = Axes(xr, yr)
    def f(fr, t):
        X = dur - 0.45
        a = env(t, 0.1, X)
        ax.draw(fr, a, step)
        if title: fr.ktext(title, 1550, 160, 46, AU, t, 0.2, X)
        if shade:
            fn, xa, xb, col = shade
            q = clamp((t - (1.2 + nc * 1.8)) / 1.6)
            if q > 0:
                xe = lerp(xa, xb, eio(q)); pts = [(ax.X(xa), ax.Y(0))]
                for i in range(61):
                    x = lerp(xa, xe, i / 60); pts.append((ax.X(x), clamp(ax.Y(fn(x)), ax.by0, ax.by1)))
                pts.append((ax.X(xe), ax.Y(0)))
                fr.poly(pts, col, a * 0.45)
        for i, (fn, col, lab) in enumerate(curves):
            ts = 0.8 + i * 1.8
            ax.curve(fr, fn, col, eo(clamp((t - ts) / 1.5)), env(t, ts, X, 0.1))
            if lab: fr.ktext(lab, 1300, 260 + i * 70, 40, col, t, ts + 0.6, X, anchor='lm')
        base = 260 + nc * 70 + 20
        for i, ln in enumerate(notes):
            fr.ktext(ln, 1300, base + i * 66, 38, WH, t, 1.0 + nc * 1.8 + i * 1.4, X, anchor='lm')
        t0 = 1.0 + nc * 1.8 + 0.4
        for i, m in enumerate(marks):
            x, y, lab, col = m[:4]
            ts = t0 + i * 0.8
            p = env(t, ts, X)
            if p <= 0: continue
            r = 13 * eback(clamp((t - ts) / 0.4))
            if len(m) > 4 and m[4] == 'open': fr.disc(ax.X(x), ax.Y(y), r, BG, p, outline=col, ow=4)
            else: fr.disc(ax.X(x), ax.Y(y), r, col, p)
            if lab: fr.text(lab, ax.X(x) + 22, ax.Y(y) - 30, 30, col, p, anchor='lm')
    return (dur, f)


def sc_tangent(fn, dfn, xr, yr, x_from, x_to, label='slope', title=None):
    """a tangent line slides along the curve; its slope is read out live"""
    dur = 12.0
    ax = Axes(xr, yr)
    def f(fr, t):
        X = dur - 0.45
        a = env(t, 0.1, X)
        ax.draw(fr, a)
        ax.curve(fr, fn, B, eo(clamp((t - 0.5) / 1.4)), a)
        if title: fr.ktext(title, 1550, 160, 46, AU, t, 0.2, X)
        q = eio(clamp((t - 2.4) / 7.0))
        x0 = lerp(x_from, x_to, q); y0 = fn(x0); m = dfn(x0)
        p = env(t, 2.2, X)
        span = (ax.x1 - ax.x0) * 0.22
        pts = [(ax.X(x0 - span), clamp(ax.Y(y0 - m * span), ax.by0, ax.by1)), (ax.X(x0 + span), clamp(ax.Y(y0 + m * span), ax.by0, ax.by1))]
        fr.line(pts, P, p, 5)
        fr.disc(ax.X(x0), ax.Y(y0), 13, AU, p)
        fr.text(f'x = {x0:.1f}', 1300, 420, 46, WH, p, anchor='lm')
        fr.text(f'{label} = {m:.2f}', 1300, 500, 52, P, p, anchor='lm')
        fr.ktext('steep = big slope', 1300, 620, 34, D, t, 3.0, X, anchor='lm')
        fr.ktext('flat = slope 0', 1300, 680, 34, D, t, 3.4, X, anchor='lm')
    return (dur, f)


def sc_riemann(fn, a_, b_, xr, yr, ns=(4, 8, 24), exact=None, title=None):
    """rectangles under a curve get thinner until they become the exact area"""
    dur = 3.0 + len(ns) * 2.6 + 4.0
    ax = Axes(xr, yr)
    def f(fr, t):
        X = dur - 0.45
        al = env(t, 0.1, X)
        ax.draw(fr, al)
        if title: fr.ktext(title, 1550, 160, 46, AU, t, 0.2, X)
        k = int((t - 2.0) // 2.6) if t > 2.0 else -1
        if 0 <= k < len(ns):
            n = ns[k]; w = (b_ - a_) / n; tot = 0
            for i in range(n):
                x = a_ + i * w; h = fn(x + w / 2); tot += h * w
                fr.rect(ax.X(x), ax.Y(max(h, 0)), ax.X(x + w) - ax.X(x) - 2, abs(ax.Y(h) - ax.Y(0)), G, al * 0.55, outline=G, ow=2)
            fr.text(f'{n} rectangles ≈ {tot:.3f}', 1300, 420, 40, G, al, anchor='lm')
        elif k >= len(ns):
            pts = [(ax.X(a_), ax.Y(0))] + [(ax.X(lerp(a_, b_, i / 60)), ax.Y(fn(lerp(a_, b_, i / 60)))) for i in range(61)] + [(ax.X(b_), ax.Y(0))]
            fr.poly(pts, G, al * 0.5)
            if exact: fr.pop(exact, 1500, 520, 56, AU, t, 2.0 + len(ns) * 2.6, X)
        ax.curve(fr, fn, B, eo(clamp((t - 0.5) / 1.3)), al)
        fr.ktext('thinner rectangles → exact area', 1300, 700, 32, D, t, 2.2, X, anchor='lm')
    return (dur, f)


def sc_unitcircle():
    dur = 14.0
    def f(fr, t):
        X = dur - 0.45
        a = env(t, 0.1, X)
        cx, cy, r = 520, 560, 300
        fr.line([(cx - r - 60, cy), (cx + r + 60, cy)], WH, a * 0.7, 3); fr.line([(cx, cy - r - 60), (cx, cy + r + 60)], WH, a * 0.7, 3)
        pts = [(cx + r * math.cos(2 * math.pi * i / 120), cy + r * math.sin(2 * math.pi * i / 120)) for i in range(121)]
        fr.line(pts, D, a, 4)
        th = 2 * math.pi * clamp((t - 1.5) / 10.0)
        px, py = cx + r * math.cos(th), cy - r * math.sin(th)
        fr.line([(cx, cy), (px, py)], AU, a, 5)
        fr.line([(px, py), (px, cy)], P, a, 6); fr.line([(cx, cy), (px, cy)], B, a, 6)
        fr.disc(px, py, 14, AU, a)
        gx0, gx1, gy = 1000, 1840, 560
        fr.line([(gx0, gy), (gx1, gy)], WH, a * 0.7, 3)
        n = int(120 * th / (2 * math.pi))
        sp = [(lerp(gx0, gx1, i / 120), gy - r * math.sin(2 * math.pi * i / 120)) for i in range(n + 1)]
        if len(sp) > 1: fr.line(sp, P, a, 5)
        fr.text(f'θ = {math.degrees(th):.0f}°', 1000, 220, 44, AU, a, anchor='lm')
        fr.text(f'sin θ = {math.sin(th):+.2f}', 1000, 880, 40, P, a, anchor='lm')
        fr.text(f'cos θ = {math.cos(th):+.2f}', 1450, 880, 40, B, a, anchor='lm')
        fr.ktext('the height of the point draws the sine wave', W / 2, 990, 34, D, t, 2.0, X)
    return (dur, f)


# ============================================================ fractions & percents
def wedge(fr, cx, cy, r, a0, a1, col, a):
    pts = [(cx, cy)] + [(cx + r * math.cos(lerp(a0, a1, i / 24)), cy + r * math.sin(lerp(a0, a1, i / 24))) for i in range(25)]
    fr.poly(pts, col, a)


def pie(fr, cx, cy, r, n, d, col, t, t0, X):
    a = env(t, t0, X)
    fr.disc(cx, cy, r, CARD, a)
    for i in range(d):
        a0 = -math.pi / 2 + 2 * math.pi * i / d; a1 = a0 + 2 * math.pi / d
        if i < n:
            q = env(t, t0 + 0.6 + i * 0.25, X, 0.3)
            wedge(fr, cx, cy, r, a0, a1, col, q)
        fr.line([(cx, cy), (cx + r * math.cos(a0), cy + r * math.sin(a0))], BG, a, 5)
    fr.disc(cx, cy, r, None, a, outline=WH, ow=4)


def sc_pie(n, d, words=None):
    dur = 9.0
    def f(fr, t):
        X = dur - 0.45
        pie(fr, 560, 560, 300, n, d, P, t, 0.3, X)
        fr.pop(f'{n}', 1300, 430, 130, P, t, 1.2 + n * 0.25, X)
        fr.rect(1200, 535, 200, 12, WH, env(t, 1.4 + n * 0.25, X), r=6)
        fr.pop(f'{d}', 1300, 650, 130, B, t, 1.6 + n * 0.25, X)
        fr.ktext('parts shaded', 1460, 430, 38, P, t, 2.6 + n * 0.25, X, anchor='lm')
        fr.ktext('equal parts', 1460, 650, 38, B, t, 3.0 + n * 0.25, X, anchor='lm')
        if words: fr.ktext(words, 1300, 850, 50, AU, t, 4.0 + n * 0.25, X)
    return (dur, f)


def sc_fracbars(rows, caption=None):
    """rows: list of (n, d); bars stacked so equal lengths line up"""
    dur = 3.0 + len(rows) * 1.4 + 3.5
    cols = [P, B, G, AU, PU]
    def f(fr, t):
        X = dur - 0.45
        x0, wd = 420, 1000
        for k, (n, d) in enumerate(rows):
            y = 260 + k * 150; ts = 0.6 + k * 1.4
            a = env(t, ts, X)
            for i in range(d):
                fr.rect(x0 + wd * i / d + 3, y, wd / d - 6, 100, cols[k % 5] if i < n else CARD, a * (1 if i < n else 0.9), r=10)
            fr.text(f'{n}/{d}', x0 - 90, y + 50, 56, cols[k % 5], a)
        if caption: fr.ktext(caption, W / 2, 260 + len(rows) * 150 + 60, 52, AU, t, 1.0 + len(rows) * 1.4, X)
    return (dur, f)


def sc_percent(p, of=None):
    dur = 10.0
    def f(fr, t):
        X = dur - 0.45
        s = 66; x0, y0 = 260, 220
        k = int(p * clamp((t - 1.0) / 3.0))
        for i in range(100):
            r, c = divmod(i, 10)
            fr.rect(x0 + c * s, y0 + r * s, s - 6, s - 6, G if i < k else CARD, env(t, 0.2, X), r=8)
        fr.pop(f'{p}%', 1380, 360, 150, G, t, 4.2, X)
        fr.ktext(f'{p} out of 100', 1380, 500, 48, WH, t, 4.6, X)
        if of is not None:
            fr.ktext(f'{p}% of {of} = {p}/100 × {of}', 1380, 660, 46, D, t, 5.6, X)
            fr.pop(f'= {fmt(p * of / 100)}', 1380, 760, 80, AU, t, 6.8, X)
    return (dur, f)


# ============================================================ geometry
def sc_angle(deg):
    dur = 8.0
    kind = 'acute' if deg < 90 else ('right' if deg == 90 else ('obtuse' if deg < 180 else 'straight'))
    def f(fr, t):
        X = dur - 0.45
        cx, cy, L_ = 640, 760, 480
        a = env(t, 0.2, X)
        fr.line([(cx, cy), (cx + L_, cy)], WH, a, 7)
        th = math.radians(deg) * eio(clamp((t - 0.8) / 2.0))
        fr.line([(cx, cy), (cx + L_ * math.cos(th), cy - L_ * math.sin(th))], B, a, 7)
        pts = [(cx + 120 * math.cos(-u * th / 30), cy + 120 * math.sin(-u * th / 30)) for u in range(31)]
        if th > 0.01: fr.line(pts, AU, a, 5)
        fr.text(f'{deg * clamp((t - 0.8) / 2.0):.0f}°', cx + 190 * math.cos(-th / 2), cy + 190 * math.sin(-th / 2), 48, AU, a)
        fr.pop(kind + ' angle', 1460, 420, 70, AU, t, 3.2, X)
        fr.ktext({'acute': 'less than 90°', 'right': 'exactly 90° (a square corner)', 'obtuse': 'between 90° and 180°', 'straight': 'exactly 180°'}[kind], 1460, 530, 40, D, t, 3.8, X)
    return (dur, f)


def sc_pyth(a_, b_):
    c_ = math.hypot(a_, b_)
    dur = 13.0
    def f(fr, t):
        X = dur - 0.45
        u = 300 / max(a_, b_)
        ox, oy = 600, 700
        A = (ox, oy); Bp = (ox + b_ * u, oy); Cp = (ox, oy - a_ * u)
        al = env(t, 0.2, X)
        sqa = [(ox, oy), (ox, oy - a_ * u), (ox - a_ * u, oy - a_ * u), (ox - a_ * u, oy)]
        sqb = [(ox, oy), (ox + b_ * u, oy), (ox + b_ * u, oy + b_ * u), (ox, oy + b_ * u)]
        dx, dy = Bp[0] - Cp[0], Bp[1] - Cp[1]
        sqc = [Cp, Bp, (Bp[0] + dy, Bp[1] - dx), (Cp[0] + dy, Cp[1] - dx)]
        fr.poly(sqa, P, env(t, 1.6, X) * 0.55); fr.poly(sqb, B, env(t, 2.4, X) * 0.55); fr.poly(sqc, G, env(t, 3.2, X) * 0.55)
        fr.poly([A, Bp, Cp], CARD, al, outline=WH, ow=5)
        fr.rect(ox, oy - 30, 30, 30, None, al, outline=WH, ow=3)
        fr.text(f'a = {a_}', ox + 18, oy - a_ * u / 2, 34, P, al, anchor='lm')
        fr.text(f'b = {b_}', ox + b_ * u / 2, oy + 30, 34, B, al)
        fr.text(f'{a_}² = {a_ * a_}', ox - a_ * u / 2, oy - a_ * u / 2, 38, WH, env(t, 1.8, X))
        fr.text(f'{b_}² = {b_ * b_}', ox + b_ * u / 2, oy + b_ * u / 2, 38, WH, env(t, 2.6, X))
        fr.text(f'c² = {a_ * a_ + b_ * b_}', (Cp[0] + Bp[0] + dy) / 2, (Cp[1] + Bp[1] - dx) / 2, 38, WH, env(t, 3.4, X))
        fr.runs([('a²', P), '+', ('b²', B), '=', ('c²', G)], 1480, 300, 70, t, 4.4, X)
        fr.runs([(str(a_ * a_), P), '+', (str(b_ * b_), B), '=', (str(a_ * a_ + b_ * b_), G)], 1480, 440, 64, t, 6.0, X)
        fr.pop(f'c = √{a_ * a_ + b_ * b_} = {fmt(round(c_, 2))}', 1480, 580, 56, AU, t, 7.6, X)
    return (dur, f)


def sc_trig(opp, adj):
    hyp = math.hypot(opp, adj)
    dur = 13.0
    def f(fr, t):
        X = dur - 0.45
        u = 560 / max(opp, adj)
        ox, oy = 240, 860
        A = (ox, oy); Bp = (ox + adj * u, oy); Cp = (ox + adj * u, oy - opp * u)
        al = env(t, 0.2, X)
        fr.poly([A, Bp, Cp], CARD, al, outline=WH, ow=5)
        fr.rect(Bp[0] - 30, oy - 30, 30, 30, None, al, outline=WH, ow=3)
        pts = [(ox + 90 * math.cos(-k * math.atan2(opp, adj) / 20), oy + 90 * math.sin(-k * math.atan2(opp, adj) / 20)) for k in range(21)]
        fr.line(pts, AU, al, 4); fr.text('θ', ox + 130, oy - 30, 40, AU, al)
        fr.text(f'opposite {opp}', Cp[0] + 20, (Bp[1] + Cp[1]) / 2, 34, P, env(t, 1.0, X), anchor='lm')
        fr.text(f'adjacent {adj}', (A[0] + Bp[0]) / 2, oy + 36, 34, B, env(t, 1.6, X))
        fr.text(f'hypotenuse {fmt(round(hyp, 2))}', (A[0] + Cp[0]) / 2 - 40, (A[1] + Cp[1]) / 2 - 40, 34, G, env(t, 2.2, X), anchor='rm')
        rows = [('sin θ = opp / hyp', f'= {opp}/{fmt(round(hyp, 2))} = {opp / hyp:.2f}', P),
                ('cos θ = adj / hyp', f'= {adj}/{fmt(round(hyp, 2))} = {adj / hyp:.2f}', B),
                ('tan θ = opp / adj', f'= {opp}/{adj} = {opp / adj:.2f}', G)]
        for i, (r1, r2, col) in enumerate(rows):
            fr.ktext(r1, 1100, 280 + i * 170, 50, col, t, 3.2 + i * 1.6, X, anchor='lm')
            fr.ktext(r2, 1140, 345 + i * 170, 40, WH, t, 3.8 + i * 1.6, X, anchor='lm')
        fr.ktext('SOH  CAH  TOA', 1400, 900, 56, AU, t, 8.4, X)
    return (dur, f)


def sc_circle(r_):
    dur = 12.0
    def f(fr, t):
        X = dur - 0.45
        cx, cy, R = 560, 560, 300
        al = env(t, 0.2, X)
        q = clamp((t - 0.6) / 1.6)
        pts = [(cx + R * math.cos(2 * math.pi * i / 120 * q), cy + R * math.sin(2 * math.pi * i / 120 * q)) for i in range(121)]
        fr.disc(cx, cy, R, CARD, al * 0.6)
        fr.line(pts, B, al, 7)
        fr.line([(cx, cy), (cx + R, cy)], AU, env(t, 2.2, X), 5)
        fr.text(f'r = {r_}', cx + R / 2, cy - 34, 40, AU, env(t, 2.4, X))
        fr.disc(cx, cy, 8, WH, al)
        fr.runs([('C', B), '=', '2πr', '=', f'2 × π × {r_}'], 1400, 300, 52, t, 3.4, X)
        fr.pop(f'≈ {2 * math.pi * r_:.1f}', 1400, 400, 64, B, t, 4.8, X)
        fr.runs([('A', G), '=', 'πr²', '=', f'π × {r_}²'], 1400, 600, 52, t, 6.2, X)
        fr.pop(f'≈ {math.pi * r_ * r_:.1f}', 1400, 700, 64, G, t, 7.6, X)
        fr.ktext('π ≈ 3.14', 1400, 880, 40, D, t, 8.4, X)
    return (dur, f)


def sc_area_rect(w_, h_, tri=False):
    dur = 11.0
    def f(fr, t):
        X = dur - 0.45
        u = min(900 / w_, 560 / h_); x0, y0 = 560 - w_ * u / 2, 560 - h_ * u / 2
        al = env(t, 0.2, X)
        for i in range(w_ * h_):
            r, c = divmod(i, w_)
            ts = 0.8 + i * (3.0 / (w_ * h_))
            fr.rect(x0 + c * u + 3, y0 + r * u + 3, u - 6, u - 6, CARD if tri else B, env(t, ts, X, 0.2), r=6)
        if tri:
            fr.poly([(x0, y0 + h_ * u), (x0 + w_ * u, y0 + h_ * u), (x0, y0)], G, env(t, 4.0, X) * 0.65, outline=WH, ow=5)
        else:
            fr.rect(x0, y0, w_ * u, h_ * u, None, al, outline=WH, ow=5)
        fr.text(f'{w_}', x0 + w_ * u / 2, y0 + h_ * u + 40, 42, AU, al)
        fr.text(f'{h_}', x0 - 40, y0 + h_ * u / 2, 42, AU, al)
        if tri:
            fr.runs(['Area', '=', '½ × base × height'], 1450, 380, 46, t, 4.6, X)
            fr.runs(['=', f'½ × {w_} × {h_}', '=', (fmt(w_ * h_ / 2), G)], 1450, 500, 56, t, 6.0, X)
            fr.ktext('half of the rectangle', 1450, 640, 36, D, t, 7.0, X)
        else:
            fr.runs(['Area', '=', 'length × width'], 1450, 380, 46, t, 4.2, X)
            fr.runs([f'{w_} × {h_}', '=', (str(w_ * h_), B), 'squares'], 1450, 500, 56, t, 5.6, X)
    return (dur, f)


def sc_box(l, w_, h_):
    dur = 3.0 + h_ * 1.2 + 4.5
    def f(fr, t):
        X = dur - 0.45
        u = 70; ox, oy = 560, 760
        def P3(x, y, z):  # isometric
            return (ox + (x - y) * u * 0.87, oy + (x + y) * u * 0.5 - z * u)
        for z in range(h_):
            a = env(t, 0.8 + z * 1.2, X, 0.4)
            if a <= 0: continue
            for y in range(w_ - 1, -1, -1):
                for x in range(l - 1, -1, -1):
                    top = [P3(x, y, z + 1), P3(x + 1, y, z + 1), P3(x + 1, y + 1, z + 1), P3(x, y + 1, z + 1)]
                    left = [P3(x, y + 1, z), P3(x + 1, y + 1, z), P3(x + 1, y + 1, z + 1), P3(x, y + 1, z + 1)]
                    right = [P3(x + 1, y, z), P3(x + 1, y + 1, z), P3(x + 1, y + 1, z + 1), P3(x + 1, y, z + 1)]
                    fr.poly(left, mix(B, BG, 0.35), a, outline=BG, ow=2)
                    fr.poly(right, mix(B, BG, 0.15), a, outline=BG, ow=2)
                    fr.poly(top, B, a, outline=BG, ow=2)
            fr.ktext(f'layer {z + 1}: {l * w_}', 1450, 260 + z * 60, 36, D, t, 1.0 + z * 1.2, X)
        tz = 1.6 + h_ * 1.2
        fr.runs(['V', '=', 'l × w × h'], 1450, 260 + h_ * 60 + 80, 48, t, tz, X)
        fr.runs([f'{l} × {w_} × {h_}', '=', (str(l * w_ * h_), AU)], 1450, 260 + h_ * 60 + 180, 56, t, tz + 1.2, X)
        fr.ktext('cubic units', 1450, 260 + h_ * 60 + 260, 36, D, t, tz + 2.0, X)
    return (dur, f)


# ============================================================ number lines, inequalities, ratios
def sc_iline(a_, b_, op='+', lo=-10, hi=10):
    """integer number line with negatives"""
    c_ = a_ + b_ if op == '+' else a_ - b_
    steps_ = abs(b_)
    d = (1 if b_ >= 0 else -1) * (1 if op == '+' else -1)
    dur = 4.0 + steps_ * 0.5 + 3.5
    def f(fr, t):
        X = dur - 0.45
        x0, x1, y = 140, 1780, 650
        a = env(t, 0.2, X)
        fr.line([(x0 - 30, y), (x1 + 30, y)], WH, a, 6)
        for v in range(lo, hi + 1):
            x = lerp(x0, x1, (v - lo) / (hi - lo))
            fr.line([(x, y - 14), (x, y + 14)], WH, a, 3)
            fr.text(str(v), x, y + 48, 26, R if v < 0 else (G if v > 0 else WH), a)
        def X_(v): return lerp(x0, x1, (v - lo) / (hi - lo))
        fr.disc(X_(a_), y, 18, P, env(t, 0.8, X))
        for i in range(steps_):
            ts = 1.8 + i * 0.5
            fr.hop(X_(a_ + d * i), X_(a_ + d * (i + 1)), y - 12, 80, G if d > 0 else R, clamp((t - ts) / 0.4), env(t, ts, X, 0.1), 5)
        fr.disc(X_(c_), y, 20, AU, env(t, 2.0 + steps_ * 0.5, X))
        sb = f'({b_})' if b_ < 0 else str(b_)
        fr.runs([(str(a_), P), '−' if op == '-' else '+', (sb, G if d > 0 else R), '=', (str(c_) if t > 2.0 + steps_ * 0.5 else '?', AU)], W / 2, 300, 96, t, 1.0, X, 0.15)
        fr.ktext('right = bigger,  left = smaller', W / 2, 860, 38, D, t, 2.4, X)
    return (dur, f)


def sc_ineq(v, kind, var='x'):
    """kind: '<', '<=', '>', '>='"""
    dur = 9.0
    closed = '=' in kind
    right = '>' in kind
    sym = {'<': '<', '<=': '≤', '>': '>', '>=': '≥'}[kind]
    def f(fr, t):
        X = dur - 0.45
        lo, hi = v - 8, v + 8
        x0, x1, y = 160, 1760, 620
        a = env(t, 0.2, X)
        fr.line([(x0 - 30, y), (x1 + 30, y)], WH, a, 6)
        def X_(u): return lerp(x0, x1, (u - lo) / (hi - lo))
        for u in range(lo, hi + 1):
            fr.line([(X_(u), y - 12), (X_(u), y + 12)], WH, a, 3); fr.text(str(u), X_(u), y + 46, 26, WH, a)
        q = eio(clamp((t - 2.0) / 1.6))
        end = lerp(X_(v), x1 + 20 if right else x0 - 20, q)
        fr.line([(X_(v), y), (end, y)], G, env(t, 2.0, X), 14)
        fr.disc(X_(v), y, 22, G if closed else BG, env(t, 1.4, X), outline=G, ow=6)
        fr.pop(f'{var} {sym} {v}', W / 2, 300, 110, AU, t, 0.4, X)
        fr.ktext(('filled dot: ' + str(v) + ' is included') if closed else ('open dot: ' + str(v) + ' is NOT included'), W / 2, 820, 42, D, t, 3.8, X)
    return (dur, f)


def sc_ratio(a_, b_, k=3):
    dur = 3.0 + k * 1.4 + 3.5
    def f(fr, t):
        X = dur - 0.45
        for g in range(k):
            ts = 0.6 + g * 1.4
            y = 300 + g * 150
            for i in range(a_): fr.disc(260 + i * 70, y, 26, P, env(t, ts + i * 0.05, X))
            for i in range(b_): fr.disc(260 + (a_ + i) * 70 + 40, y, 26, B, env(t, ts + 0.3 + i * 0.05, X))
            fr.ktext(f'{a_ * (g + 1)} : {b_ * (g + 1)}', 1500, y, 60, AU, t, ts + 0.6, X)
        fr.ktext('same ratio, just more groups', W / 2, 300 + k * 150 + 60, 44, D, t, 1.0 + k * 1.4, X)
    return (dur, f)


def sc_seq(terms, ops, title=None):
    """terms: list of numbers/str, ops: list of labels between them (e.g. '+3' or '×2')"""
    n = len(terms)
    dur = 2.0 + n * 0.9 + 3.5
    def f(fr, t):
        X = dur - 0.45
        sp = min(260, 1600 / n); x0 = W / 2 - (n - 1) * sp / 2
        if title: fr.ktext(title, W / 2, 230, 56, AU, t, 0.2, X)
        for i, v in enumerate(terms):
            ts = 0.8 + i * 0.9
            fr.rect(x0 + i * sp - sp * 0.38, 480, sp * 0.76, 140, CARD, env(t, ts, X), r=22, outline=B, ow=3)
            fr.pop(str(v), x0 + i * sp, 550, min(64, sp * 0.3), WH, t, ts, X)
            if i < n - 1 and i < len(ops):
                fr.hop(x0 + i * sp, x0 + (i + 1) * sp, 470, 80, G, clamp((t - ts - 0.4) / 0.4), env(t, ts + 0.4, X, 0.1), 4)
                fr.ktext(ops[i], x0 + (i + 0.5) * sp, 370, 36, G, t, ts + 0.5, X)
    return (dur, f)


# ============================================================ statistics & probability
def sc_bars(vals, labels=None, show=('mean', 'median', 'mode'), title=None):
    n = len(vals)
    srt = sorted(vals)
    mean = sum(vals) / n
    med = (srt[n // 2] if n % 2 else (srt[n // 2 - 1] + srt[n // 2]) / 2)
    mode = max(set(vals), key=vals.count)
    dur = 3.0 + n * 0.3 + len(show) * 2.2 + 2.5
    def f(fr, t):
        X = dur - 0.45
        mx = max(vals); bw = 900 / n; x0 = 180; y0 = 900
        if title: fr.ktext(title, W / 2, 150, 50, AU, t, 0.1, X)
        for i, v in enumerate(vals):
            ts = 0.6 + i * 0.3; h = 580 * v / mx * eo(clamp((t - ts) / 0.6))
            hl = 'mode' in show and v == mode and t > 3.0 + n * 0.3 + 4.4
            fr.rect(x0 + i * bw + 8, y0 - h, bw - 16, h, PU if hl else B, env(t, ts, X), r=10)
            fr.text(str(v), x0 + i * bw + bw / 2, y0 - h - 30, 34, WH, env(t, ts + 0.3, X))
            if labels: fr.text(labels[i], x0 + i * bw + bw / 2, y0 + 34, 26, D, env(t, ts, X))
        t1 = 2.0 + n * 0.3
        rows = []
        if 'mean' in show: rows.append(('mean (average)', f'{"+".join(map(str, vals))} ÷ {n} = {fmt(round(mean, 2))}', AU))
        if 'median' in show: rows.append(('median (middle)', f'sorted: {" ".join(map(str, srt))} → {fmt(med)}', G))
        if 'mode' in show: rows.append(('mode (most often)', f'{mode}', PU))
        for i, (a_, b_, col) in enumerate(rows):
            fr.ktext(a_, 1180, 300 + i * 190, 44, col, t, t1 + i * 2.2, X, anchor='lm')
            fr.ktext(b_, 1180, 360 + i * 190, fit(fr, b_, 34, 700), WH, t, t1 + i * 2.2 + 0.4, X, anchor='lm')
        if 'mean' in show:
            ym = y0 - 580 * mean / mx
            fr.line([(x0, ym), (x0 + 900, ym)], AU, env(t, t1 + 0.6, X), 4)
    return (dur, f)


def sc_dice(target):
    hits = [(a, b) for a in range(1, 7) for b in range(1, 7) if a + b == target]
    dur = 11.0
    def f(fr, t):
        X = dur - 0.45
        s = 105; x0, y0 = 220, 250
        for a in range(1, 7):
            fr.text(str(a), x0 + (a - 1) * s + s / 2, y0 - 40, 38, P, env(t, 0.2, X))
            fr.text(str(a), x0 - 40, y0 + (a - 1) * s + s / 2, 38, B, env(t, 0.2, X))
        for a in range(1, 7):
            for b in range(1, 7):
                hit = (a, b) in hits and t > 3.0
                fr.rect(x0 + (a - 1) * s + 4, y0 + (b - 1) * s + 4, s - 8, s - 8, G if hit else CARD, env(t, 0.4 + (a + b) * 0.08, X), r=12)
                fr.text(str(a + b), x0 + (a - 1) * s + s / 2, y0 + (b - 1) * s + s / 2, 34, BG if hit else WH, env(t, 0.4 + (a + b) * 0.08, X))
        fr.ktext('two dice: 36 outcomes', 1400, 300, 46, WH, t, 1.6, X)
        fr.ktext(f'sum of {target}: {len(hits)} ways', 1400, 420, 46, G, t, 3.2, X)
        fr.runs(['P', '=', (f'{len(hits)}/36', G), '≈', (f'{len(hits) / 36:.2f}', AU)], 1400, 580, 60, t, 4.6, X)
        fr.ktext('probability = ways it can happen ÷ all outcomes', 1400, 760, 30, D, t, 6.0, X)
    return (dur, f)


# ============================================================ matrices & vectors
def sc_matmul(A, B_):
    Cm = [[sum(A[i][k] * B_[k][j] for k in range(2)) for j in range(2)] for i in range(2)]
    dur = 3.0 + 4 * 2.2 + 3.0
    def mat(fr, M, x, y, col, a, hi_row=None, hi_col=None, show=None):
        cw, ch = 110, 100
        fr.line([(x - 10, y - 20), (x - 30, y - 20), (x - 30, y + 2 * ch - 20), (x - 10, y + 2 * ch - 20)], WH, a, 5)
        fr.line([(x + 2 * cw + 10, y - 20), (x + 2 * cw + 30, y - 20), (x + 2 * cw + 30, y + 2 * ch - 20), (x + 2 * cw + 10, y + 2 * ch - 20)], WH, a, 5)
        for i in range(2):
            for j in range(2):
                if show is not None and not show(i, j): continue
                h = (hi_row == i) or (hi_col == j)
                if h: fr.rect(x + j * cw + 6, y + i * ch - 16, cw - 12, ch - 12, AU, a * 0.25, r=12)
                fr.text(str(M[i][j]), x + j * cw + cw / 2, y + i * ch + ch / 2 - 22, 52, AU if h else col, a)
    def f(fr, t):
        X = dur - 0.45
        a = env(t, 0.2, X)
        k = int((t - 2.0) // 2.2) if t > 2.0 else -1
        i, j = (divmod(k, 2) if 0 <= k < 4 else (None, None))
        mat(fr, A, 160, 380, P, a, hi_row=i)
        fr.text('×', 520, 470, 70, WH, a)
        mat(fr, B_, 620, 380, B, a, hi_col=j)
        fr.text('=', 990, 470, 70, WH, a)
        mat(fr, Cm, 1090, 380, G, a, show=lambda r, c: t > 2.0 + (r * 2 + c) * 2.2 + 1.2)
        if i is not None:
            s = f'{A[i][0]}×{B_[0][j]} + {A[i][1]}×{B_[1][j]} = {Cm[i][j]}'
            fr.ktext(f'row {i + 1} · column {j + 1}:', W / 2, 760, 40, D, t, 2.0 + k * 2.2, 2.0 + (k + 1) * 2.2 - 0.2)
            fr.ktext(s, W / 2, 840, 54, AU, t, 2.3 + k * 2.2, 2.0 + (k + 1) * 2.2 - 0.2)
        fr.ktext('row times column, then add', W / 2, 220, 48, WH, t, 0.4, X)
    return (dur, f)


def sc_det(M):
    a, b, c, d = M[0][0], M[0][1], M[1][0], M[1][1]
    return sc_steps([f'| {a}  {b} |', f'| {c}  {d} |', f'det = ad − bc', f'= {a}×{d} − {b}×{c}', f'= {a * d} − {b * c} = {a * d - b * c}'],
                    ['', '', 'down-right minus up-right', '', 'area scale of the matrix'])


def sc_vectors(u, v):
    w = (u[0] + v[0], u[1] + v[1])
    ax = Axes((-1, 9), (-1, 7), box=(140, 170, 1180, 990))
    dur = 12.0
    def arrow(fr, p0, p1, col, p, a):
        if p <= 0: return
        x0, y0 = ax.X(p0[0]), ax.Y(p0[1]); x1, y1 = ax.X(p1[0]), ax.Y(p1[1])
        xe, ye = lerp(x0, x1, p), lerp(y0, y1, p)
        fr.line([(x0, y0), (xe, ye)], col, a, 7)
        if p >= 0.99:
            ang = math.atan2(y1 - y0, x1 - x0)
            fr.poly([(x1, y1), (x1 - 30 * math.cos(ang - 0.4), y1 - 30 * math.sin(ang - 0.4)), (x1 - 30 * math.cos(ang + 0.4), y1 - 30 * math.sin(ang + 0.4))], col, a)
    def f(fr, t):
        X = dur - 0.45
        a = env(t, 0.1, X)
        ax.draw(fr, a, (1, 1))
        arrow(fr, (0, 0), u, P, clamp((t - 0.8) / 0.8), a)
        arrow(fr, u, w, B, clamp((t - 2.2) / 0.8), a)
        arrow(fr, (0, 0), w, G, clamp((t - 4.0) / 1.0), a)
        fr.ktext(f'u = ({u[0]}, {u[1]})', 1260, 300, 46, P, t, 1.0, X, anchor='lm')
        fr.ktext(f'v = ({v[0]}, {v[1]})', 1260, 380, 46, B, t, 2.4, X, anchor='lm')
        fr.ktext(f'u + v = ({w[0]}, {w[1]})', 1260, 480, 50, G, t, 4.4, X, anchor='lm')
        fr.ktext('tip to tail', 1260, 560, 34, D, t, 4.8, X, anchor='lm')
        fr.ktext(f'u · v = {u[0]}×{v[0]} + {u[1]}×{v[1]} = {u[0] * v[0] + u[1] * v[1]}', 1260, 700, 36, AU, t, 6.4, X, anchor='lm')
        fr.ktext(f'|u| = √({u[0]}² + {u[1]}²) = {math.hypot(*u):.2f}', 1260, 780, 36, WH, t, 7.6, X, anchor='lm')
    return (dur, f)


def sc_square(n):
    dur = 9.0
    def f(fr, t):
        X = dur - 0.45
        s = min(70, 560 / n); x0, y0 = 560 - n * s / 2, 560 - n * s / 2
        for i in range(n * n):
            r, c = divmod(i, n)
            fr.rect(x0 + c * s + 2, y0 + r * s + 2, s - 4, s - 4, PU, env(t, 0.5 + i * (2.0 / (n * n)), X, 0.2), r=4)
        fr.text(str(n), x0 + n * s / 2, y0 + n * s + 40, 44, AU, env(t, 2.6, X))
        fr.text(str(n), x0 - 40, y0 + n * s / 2, 44, AU, env(t, 2.6, X))
        fr.runs([f'{n}²', '=', f'{n} × {n}', '=', (str(n * n), PU)], 1440, 400, 64, t, 3.0, X)
        fr.runs([f'√{n * n}', '=', (str(n), AU)], 1440, 560, 80, t, 4.8, X)
        fr.ktext('the square root is the side of the square', 1440, 700, 32, D, t, 5.6, X)
    return (dur, f)


# ============================================================ your turn
def sc_try(q, a, acc=AU, wait=4.0):
    """asks a question, counts down so the viewer can pause and think, then shows the answer"""
    dur = 2.4 + wait + 3.2
    def f(fr, t):
        X = dur - 0.45
        fr.pop('Your turn!', W / 2, 220, 84, acc, t, 0.1, X)
        fr.rect(260, 330, W - 520, 220, CARD, env(t, 0.4, X), r=34)
        fr.ktext(q, W / 2, 440, fit(fr, q, 60, W - 640), WH, t, 0.6, X)
        tw0 = 1.6
        p = clamp((t - tw0) / wait)
        if 0 < p < 1:
            pts = [(W / 2 + 70 * math.cos(-math.pi / 2 + 2 * math.pi * u / 60 * (1 - p)), 700 + 70 * math.sin(-math.pi / 2 + 2 * math.pi * u / 60 * (1 - p))) for u in range(61)]
            fr.line(pts, acc, 1.0, 10)
            fr.text(str(int(math.ceil(wait * (1 - p)))), W / 2, 700, 64, WH)
            fr.ktext('pause and think…', W / 2, 840, 36, D, t, tw0, tw0 + wait)
        fr.pop(a, W / 2, 700, 110, G, t, tw0 + wait + 0.1, X)
        fr.tick(W / 2 + fr.tw(a, 110) / 2 + 90, 700, 60, G, t, tw0 + wait + 0.5, X)
    return (dur, f)
