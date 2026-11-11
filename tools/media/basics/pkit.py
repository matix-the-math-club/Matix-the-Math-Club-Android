"""Pillow kit for the Matix "Math Basics" lessons (1.0.7).
Frames are drawn at 2x (3840x2160) with Pillow and box-filtered down to 1920x1080, then piped to ffmpeg.
No pycairo needed: Python 3.9+, Pillow, numpy-free, and an ffmpeg with libx264 (imageio-ffmpeg works)."""
import math, os, subprocess
from PIL import Image, ImageDraw, ImageFont

S = 2
W, H, FPS = 1920, 1080, 30
BG = (14, 15, 26)
C = dict(pink=(255, 79, 163), blue=(56, 189, 248), green=(52, 211, 153), orange=(249, 115, 22),
         purple=(139, 92, 246), gold=(250, 191, 36), white=(255, 255, 255), dim=(170, 176, 199),
         red=(255, 92, 92), card=(28, 30, 46), line=(60, 64, 90))
P, B, G, O, PU, AU, WH, D, R = (C[k] for k in ('pink', 'blue', 'green', 'orange', 'purple', 'gold', 'white', 'dim', 'red'))
FONT_DIR = '/System/Library/Fonts/Supplemental/'
FONT_FILES = {'r': 'Arial Rounded Bold.ttf', 'b': 'Arial Bold.ttf', 'n': 'Arial.ttf'}
_fonts = {}


def font(size, kind='r'):
    k = (int(size), kind)
    if k not in _fonts:
        _fonts[k] = ImageFont.truetype(os.environ.get('MX_FONT_DIR', FONT_DIR) + FONT_FILES[kind], int(size * S))
    return _fonts[k]


def fix(s):
    return str(s).replace('−', '–')  # Arial Rounded has no U+2212


def clamp(x, a=0.0, b=1.0): return a if x < a else (b if x > b else x)
def eo(x): x = clamp(x); return 1 - (1 - x) ** 3
def eio(x): x = clamp(x); return 3 * x * x - 2 * x * x * x
def eback(x):
    x = clamp(x); c = 1.70158
    return 1 + (c + 1) * (x - 1) ** 3 + c * (x - 1) ** 2
def lerp(a, b, t): return a + (b - a) * t
def mix(a, b, t): t = clamp(t); return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
def env(t, t0, t1=None, fi=0.4, fo=0.35):
    p = clamp((t - t0) / fi) if fi > 0 else (1.0 if t >= t0 else 0.0)
    if t1 is not None: p *= 1 - clamp((t - t1) / fo)
    return p


class Frame:
    def __init__(self, base):
        self.im = base.copy()
        self.d = ImageDraw.Draw(self.im)

    def col(self, col, a):
        return mix(BG, col, a)

    # ---------- text
    def tw(self, s, size, kind='r'):
        return font(size, kind).getlength(fix(s)) / S

    def text(self, s, x, y, size, col=WH, a=1.0, anchor='mm', kind='r'):
        if a <= 0.01: return
        self.d.text((x * S, y * S), fix(s), fill=self.col(col, a), font=font(size, kind), anchor=anchor)

    def ktext(self, s, x, y, size, col, t, t0, t1=None, anchor='mm', kind='r', rise=34):
        p = env(t, t0, t1)
        if p <= 0: return
        self.text(s, x, y + rise * (1 - eo(p)), size, col, p, anchor, kind)

    def runs(self, parts, cx, y, size, t, t0, t1=None, stagger=0.12, align='center'):
        """parts: list of str | (str, col). Each part flies in after the previous one."""
        parts = [(p, WH) if isinstance(p, str) else p for p in parts]
        gap = size * 0.18
        ws = [self.tw(s, size) for s, _ in parts]
        total = sum(ws) + gap * (len(parts) - 1)
        x = cx - total / 2 if align == 'center' else (cx if align == 'left' else cx - total)
        for i, ((s, col), w) in enumerate(zip(parts, ws)):
            self.ktext(s, x + w / 2, y, size, col, t, t0 + i * stagger, t1)
            x += w + gap
        return total

    def pop(self, s, x, y, size, col, t, t0, t1=None):
        p = env(t, t0, t1, 0.35)
        if p <= 0: return
        k = eback(clamp((t - t0) / 0.45))
        self.text(s, x, y, max(4, size * k), col, p)

    # ---------- shapes
    def disc(self, x, y, r, col, a=1.0, outline=None, ow=3):
        if a <= 0.01 or r <= 0: return
        self.d.ellipse([(x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S], fill=self.col(col, a) if col else None,
                       outline=self.col(outline, a) if outline else None, width=int(ow * S))

    def rect(self, x, y, w, h, col, a=1.0, r=0, outline=None, ow=3):
        if a <= 0.01 or w <= 0 or h <= 0: return
        box = [x * S, y * S, (x + w) * S, (y + h) * S]
        f = self.col(col, a) if col else None
        o = self.col(outline, a) if outline else None
        if r > 0: self.d.rounded_rectangle(box, radius=r * S, fill=f, outline=o, width=int(ow * S))
        else: self.d.rectangle(box, fill=f, outline=o, width=int(ow * S))

    def line(self, pts, col, a=1.0, w=5):
        if a <= 0.01 or len(pts) < 2: return
        self.d.line([(x * S, y * S) for x, y in pts], fill=self.col(col, a), width=int(w * S), joint='curve')

    def poly(self, pts, col, a=1.0, outline=None, ow=3):
        if a <= 0.01: return
        self.d.polygon([(x * S, y * S) for x, y in pts], fill=self.col(col, a) if col else None,
                       outline=self.col(outline, a) if outline else None, width=int(ow * S))

    def hop(self, x0, x1, y, h, col, p, a=1.0, w=6, head=True):
        """arc from x0 to x1 above y, drawn up to fraction p"""
        if p <= 0: return
        n = 28
        pts = []
        for i in range(int(n * p) + 1):
            u = i / n
            pts.append((lerp(x0, x1, u), y - h * 4 * u * (1 - u)))
        self.line(pts, col, a, w)
        if head and p >= 0.999:
            d = 1 if x1 > x0 else -1
            self.poly([(x1, y), (x1 - 16 * d, y - 22), (x1 + 6 * d, y - 22)], col, a)

    def tick(self, x, y, s, col, t, t0, t1=None):
        """animated check mark"""
        p = env(t, t0, t1, 0.5)
        if p <= 0: return
        q = eo(clamp((t - t0) / 0.5))
        a = (x - s * 0.5, y); b = (x - s * 0.1, y + s * 0.4); c = (x + s * 0.6, y - s * 0.45)
        if q < 0.4:
            u = q / 0.4; self.line([a, (lerp(a[0], b[0], u), lerp(a[1], b[1], u))], col, p, s * 0.16)
        else:
            u = (q - 0.4) / 0.6; self.line([a, b, (lerp(b[0], c[0], u), lerp(b[1], c[1], u))], col, p, s * 0.16)

    def star(self, x, y, r, col, a=1.0, rot=0.0):
        pts = []
        for i in range(10):
            rr = r if i % 2 == 0 else r * 0.45
            an = -math.pi / 2 + i * math.pi / 5 + rot
            pts.append((x + rr * math.cos(an), y + rr * math.sin(an)))
        self.poly(pts, col, a)

    def cross(self, x, y, s, col, p, a=1.0):
        if p <= 0: return
        q1 = clamp(p * 2); q2 = clamp(p * 2 - 1)
        self.line([(x - s, y - s), (x - s + 2 * s * q1, y - s + 2 * s * q1)], col, a, 7)
        if q2 > 0: self.line([(x + s, y - s), (x + s - 2 * s * q2, y - s + 2 * s * q2)], col, a, 7)

    def out(self):
        return self.im.reduce(S).tobytes()


def background(cat_label, title, accent):
    im = Image.new('RGB', (W * S, H * S), BG)
    d = ImageDraw.Draw(im)
    for y in range(0, H * S, 8):  # soft vertical gradient
        k = y / (H * S)
        d.rectangle([0, y, W * S, y + 8], fill=mix(BG, (24, 22, 44), k * 0.9))
    for gx in range(60, W, 80):  # faint dot grid
        for gy in range(60, H, 80):
            d.ellipse([(gx - 2) * S, (gy - 2) * S, (gx + 2) * S, (gy + 2) * S], fill=(30, 32, 50))
    f = font(26)
    lab = fix(cat_label.upper() + '  -  ' + title)
    tw = f.getlength(lab) / S
    d.rounded_rectangle([40 * S, 32 * S, (40 + tw + 44) * S, 80 * S], radius=24 * S, fill=(26, 28, 44), outline=mix(BG, accent, 0.7), width=2 * S)
    d.text(((62 + tw / 2) * S, 56 * S), lab, fill=(200, 204, 224), font=f, anchor='mm')
    d.text(((W - 46) * S, (H - 44) * S), 'matix', fill=(249, 115, 22), font=font(40), anchor='rm')
    return im


# ---------------------------------------------------------------- layouts
def grid_pos(n, cx, cy, per_row=5, sp=74, group_gap=24):
    """dots in rows of `per_row`, every 5 a slightly bigger gap (easy to count)"""
    rows = max(1, math.ceil(n / per_row))
    cols = min(n, per_row)
    def gx(i): return i * sp + (i // 5) * group_gap
    wd = gx(cols - 1) if cols else 0
    out = []
    for i in range(n):
        r, c = divmod(i, per_row)
        out.append((cx - wd / 2 + gx(c), cy - (rows - 1) * sp / 2 + r * sp))
    return out


# ---------------------------------------------------------------- encode
def ffmpeg_exe():
    e = os.environ.get('FFMPEG')
    if e: return e
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        return 'ffmpeg'


def render(path, scenes, cat_label, title, accent, kbps=1500, preview=None):
    """scenes: list of (duration, fn(frame, t)). Writes an H.264 mp4 (muted, faststart)."""
    base = background(cat_label, title, accent)
    total = sum(d for d, _ in scenes)
    nf = int(round(total * FPS))
    if preview is not None:  # list of seconds -> png stills
        for s in preview:
            fr = _frame(base, scenes, s, total, accent)
            Image.frombytes('RGB', (W, H), fr).save(path.replace('.mp4', f'_{s:05.1f}.png'))
        return total
    cmd = [ffmpeg_exe(), '-y', '-v', 'error', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-s', f'{W}x{H}', '-r', str(FPS), '-i', '-',
           '-c:v', 'libx264', '-preset', os.environ.get('X264_PRESET', 'medium'), '-profile:v', 'high', '-level', '4.1',
           '-pix_fmt', 'yuv420p', '-g', '60', '-b:v', f'{kbps}k',
           # constant bitrate: simple animations would otherwise come out far below the size budget
           '-minrate', f'{kbps}k', '-maxrate', f'{kbps}k', '-bufsize', f'{kbps * 2}k', '-x264-params', 'nal-hrd=cbr',
           '-colorspace', 'bt709', '-color_primaries', 'bt709', '-color_trc', 'bt709', '-an', '-movflags', '+faststart', path]
    pr = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    for i in range(nf):
        pr.stdin.write(_frame(base, scenes, i / FPS, total, accent))
    pr.stdin.close()
    if pr.wait(): raise SystemExit('ffmpeg failed for ' + path)
    return total


def _frame(base, scenes, t, total, accent):
    fr = Frame(base)
    acc = 0.0
    for d, fn in scenes:
        if t < acc + d or (d, fn) == scenes[-1]:
            fn(fr, t - acc)
            break
        acc += d
    # progress bar
    fr.rect(0, H - 8, W * clamp(t / total), 8, accent, 0.9)
    return fr.out()
