"""Draws the art for the Matix arcade games (1.0.8) with Pillow: shaded sprites + backgrounds.
usage: python3 make_sprites.py <project root>   -> app/src/main/assets/games/img/*.png|jpg
Everything is drawn at 4x and downsampled, so edges stay smooth."""
import math, os, random, sys
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else '.')
OUT = ROOT + '/app/src/main/assets/games/img'
os.makedirs(OUT, exist_ok=True)
K = 4
random.seed(7)


def canvas(w, h): return Image.new('RGBA', (w * K, h * K), (0, 0, 0, 0))
def save(im, name, w, h):
    im = im.resize((w, h), Image.LANCZOS)
    if name.endswith('.jpg'): im.convert('RGB').save(f'{OUT}/{name}', quality=84, optimize=True)
    else: im.save(f'{OUT}/{name}', optimize=True)
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# ------------------------------------------------------------------ space
def space_bg():
    W_, H_ = 1280, 720
    im = Image.new('RGB', (W_, H_), (8, 8, 22))
    d = ImageDraw.Draw(im)
    for y in range(H_):
        d.line([(0, y), (W_, y)], fill=mix((10, 8, 30), (22, 12, 48), y / H_))
    neb = Image.new('RGBA', (W_, H_), (0, 0, 0, 0)); nd = ImageDraw.Draw(neb)
    for _ in range(26):
        x, y, r = random.randint(0, W_), random.randint(0, H_), random.randint(80, 260)
        c = random.choice([(139, 92, 246), (236, 72, 153), (56, 189, 248)])
        nd.ellipse([x - r, y - r, x + r, y + r], fill=c + (26,))
    neb = neb.filter(ImageFilter.GaussianBlur(60))
    im = Image.alpha_composite(im.convert('RGBA'), neb)
    d = ImageDraw.Draw(im)
    for _ in range(420):
        x, y = random.random() * W_, random.random() * H_
        r = random.choice([0.6, 0.8, 1, 1.2, 1.8])
        b = random.randint(150, 255)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(b, b, 255, 255))
    planet = Image.new('RGBA', (W_, H_), (0, 0, 0, 0)); pd = ImageDraw.Draw(planet)
    for i in range(80):
        t = i / 80; r = 120 * (1 - t * 0.9)
        pd.ellipse([1080 - r - 30 * t, 140 - r - 30 * t, 1080 + r - 30 * t, 140 + r - 30 * t], fill=mix((60, 30, 120), (250, 160, 90), t ** 1.4) + (255,))
    im = Image.alpha_composite(im, planet)
    im.convert('RGB').save(f'{OUT}/bg_space.jpg', quality=82, optimize=True)


def ship():
    w, h = 120, 150; im = canvas(w, h); d = ImageDraw.Draw(im)
    s = K
    # flame
    for i in range(30):
        t = i / 30
        d.ellipse([(60 - 16 * (1 - t)) * s, (118 + 26 * (1 - t)) * s, (60 + 16 * (1 - t)) * s, (150 - 4 * t) * s], fill=mix((255, 90, 30), (255, 240, 160), t) + (255,))
    # fins
    d.polygon([(30 * s, 90 * s), (8 * s, 128 * s), (40 * s, 118 * s)], fill=(220, 50, 90))
    d.polygon([(90 * s, 90 * s), (112 * s, 128 * s), (80 * s, 118 * s)], fill=(220, 50, 90))
    # body
    for i in range(40):
        t = i / 40
        d.ellipse([(30 + 14 * t) * s, (10 + 6 * t) * s, (90 - 18 * t) * s, (128 - 6 * t) * s], fill=mix((170, 175, 200), (255, 255, 255), t))
    d.polygon([(60 * s, 2 * s), (36 * s, 40 * s), (84 * s, 40 * s)], fill=(236, 72, 153))
    # window
    d.ellipse([46 * s, 50 * s, 74 * s, 78 * s], fill=(40, 50, 90))
    d.ellipse([49 * s, 53 * s, 71 * s, 75 * s], fill=(56, 189, 248))
    d.ellipse([53 * s, 56 * s, 61 * s, 64 * s], fill=(220, 245, 255))
    d.rectangle([52 * s, 112 * s, 68 * s, 124 * s], fill=(90, 95, 120))
    save(im, 'ship.png', w, h)


def asteroid(n):
    w = h = 160; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    random.seed(100 + n)
    pts = []
    for i in range(14):
        a = 2 * math.pi * i / 14; r = random.uniform(62, 76)
        pts.append(((80 + r * math.cos(a)) * s, (80 + r * math.sin(a)) * s))
    base = [(120, 100, 85), (110, 110, 125), (135, 105, 80)][n % 3]
    d.polygon(pts, fill=mix(base, (0, 0, 0), 0.35))
    sh = Image.new('L', im.size, 0); sd = ImageDraw.Draw(sh)
    sd.polygon(pts, fill=255)
    hl = canvas(w, h); hd = ImageDraw.Draw(hl)
    for i in range(40):
        t = i / 40; r = 80 * (1 - t * 0.85)
        hd.ellipse([(66 - r) * s, (62 - r) * s, (66 + r) * s, (62 + r) * s], fill=mix(base, (235, 225, 210), t ** 1.5) + (255,))
    im.paste(hl, (0, 0), Image.composite(hl, Image.new('RGBA', im.size, (0, 0, 0, 0)), sh).split()[3])
    d = ImageDraw.Draw(im)
    for _ in range(6):
        cx, cy, r = random.uniform(40, 120), random.uniform(40, 120), random.uniform(8, 16)
        if math.hypot(cx - 80, cy - 80) + r > 66: continue
        d.ellipse([(cx - r) * s, (cy - r) * s, (cx + r) * s, (cy + r) * s], fill=mix(base, (0, 0, 0), 0.45))
        d.ellipse([(cx - r + 3) * s, (cy - r + 3) * s, (cx + r - 1) * s, (cy + r - 1) * s], fill=mix(base, (0, 0, 0), 0.2))
    save(im, f'asteroid{n}.png', w, h)


# ------------------------------------------------------------------ sky
def sky_bg():
    W_, H_ = 1280, 720
    im = Image.new('RGBA', (W_, H_))
    d = ImageDraw.Draw(im)
    for y in range(H_):
        d.line([(0, y), (W_, y)], fill=mix((86, 170, 250), (200, 235, 255), y / H_) + (255,))
    sun = Image.new('RGBA', (W_, H_), (0, 0, 0, 0)); sdr = ImageDraw.Draw(sun)
    sdr.ellipse([1020, 40, 1180, 200], fill=(255, 236, 140, 255))
    glow = sun.filter(ImageFilter.GaussianBlur(40))
    im = Image.alpha_composite(Image.alpha_composite(im, glow), sun)
    cl = Image.new('RGBA', (W_, H_), (0, 0, 0, 0)); cd = ImageDraw.Draw(cl)
    for cx, cy, sc in [(180, 130, 1.0), (560, 90, 0.8), (860, 200, 1.1), (330, 300, 0.7)]:
        for dx, dy, r in [(-60, 10, 40), (-20, -10, 55), (30, 0, 48), (70, 15, 36)]:
            cd.ellipse([cx + dx * sc - r * sc, cy + dy * sc - r * sc, cx + dx * sc + r * sc, cy + dy * sc + r * sc], fill=(255, 255, 255, 235))
    im = Image.alpha_composite(im, cl.filter(ImageFilter.GaussianBlur(2)))
    d = ImageDraw.Draw(im)
    for layer, col, base in [(0, (96, 180, 110), 560), (1, (70, 150, 85), 610), (2, (52, 125, 68), 660)]:
        pts = [(0, H_)]
        for x in range(0, W_ + 40, 40):
            pts.append((x, base + 40 * math.sin(x / (160 + layer * 50) + layer * 2)))
        pts.append((W_, H_)); d.polygon(pts, fill=col + (255,))
    im.convert('RGB').save(f'{OUT}/bg_sky.jpg', quality=82, optimize=True)


def balloon(name, col):
    w, h = 130, 190; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    d.line([(65 * s, 150 * s), (60 * s, 168 * s), (70 * s, 178 * s), (64 * s, 190 * s)], fill=(90, 90, 90), width=2 * s)
    d.polygon([(58 * s, 154 * s), (72 * s, 154 * s), (65 * s, 142 * s)], fill=mix(col, (0, 0, 0), 0.3))
    for i in range(50):
        t = i / 50
        rx, ry = 58 * (1 - t * 0.9), 72 * (1 - t * 0.9)
        cx, cy = 65 - 18 * t, 74 - 24 * t
        d.ellipse([(cx - rx) * s, (cy - ry) * s, (cx + rx) * s, (cy + ry) * s], fill=mix(mix(col, (0, 0, 0), 0.25), mix(col, (255, 255, 255), 0.55), t ** 1.3))
    d.ellipse([38 * s, 30 * s, 52 * s, 52 * s], fill=(255, 255, 255, 170))
    save(im, f'balloon_{name}.png', w, h)


# ------------------------------------------------------------------ fruit + market
def fruit(name):
    w = h = 140; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    def ball(c1, c2, box):
        x0, y0, x1, y1 = box
        for i in range(50):
            t = i / 50
            cx = (x0 + x1) / 2 - (x1 - x0) * 0.16 * t; cy = (y0 + y1) / 2 - (y1 - y0) * 0.16 * t
            rx, ry = (x1 - x0) / 2 * (1 - t * 0.9), (y1 - y0) / 2 * (1 - t * 0.9)
            d.ellipse([(cx - rx) * s, (cy - ry) * s, (cx + rx) * s, (cy + ry) * s], fill=mix(c1, c2, t ** 1.2))
    if name == 'apple':
        ball((170, 20, 30), (255, 120, 110), (14, 26, 126, 132))
        d.line([(70 * s, 34 * s), (76 * s, 8 * s)], fill=(100, 60, 30), width=6 * s)
        d.ellipse([78 * s, 8 * s, 112 * s, 26 * s], fill=(70, 160, 60))
    elif name == 'orange':
        ball((220, 110, 10), (255, 200, 90), (14, 14, 126, 126))
        for _ in range(40):
            x, y = random.uniform(30, 110), random.uniform(30, 110)
            if math.hypot(x - 70, y - 70) < 52: d.ellipse([(x - 1) * s, (y - 1) * s, (x + 1) * s, (y + 1) * s], fill=(210, 110, 20))
        d.ellipse([62 * s, 10 * s, 78 * s, 22 * s], fill=(70, 140, 50))
    elif name == 'watermelon':
        d.pieslice([6 * s, -40 * s, 134 * s, 120 * s], 20, 160, fill=(40, 130, 50))
        d.pieslice([14 * s, -32 * s, 126 * s, 108 * s], 20, 160, fill=(235, 245, 210))
        d.pieslice([20 * s, -26 * s, 120 * s, 100 * s], 20, 160, fill=(240, 70, 90))
        for a in range(35, 150, 22):
            r = 40; x, y = 70 + r * math.cos(math.radians(a)), 37 + r * math.sin(math.radians(a))
            d.ellipse([(x - 3) * s, (y - 5) * s, (x + 3) * s, (y + 5) * s], fill=(30, 20, 20))
    elif name == 'lemon':
        ball((220, 190, 20), (255, 245, 150), (10, 30, 130, 112))
        d.ellipse([4 * s, 62 * s, 18 * s, 78 * s], fill=(220, 190, 20)); d.ellipse([122 * s, 62 * s, 136 * s, 78 * s], fill=(220, 190, 20))
    elif name == 'grapes':
        for (x, y) in [(50, 40), (78, 40), (36, 64), (64, 64), (92, 64), (50, 88), (78, 88), (64, 112)]:
            ball((90, 30, 130), (190, 130, 230), (x - 17, y - 17, x + 17, y + 17))
        d.line([(64 * s, 24 * s), (70 * s, 6 * s)], fill=(100, 70, 30), width=5 * s)
    save(im, f'fruit_{name}.png', w, h)


def market_bg():
    W_, H_ = 1280, 720
    im = Image.new('RGB', (W_, H_)); d = ImageDraw.Draw(im)
    for y in range(H_):
        d.line([(0, y), (W_, y)], fill=mix((255, 214, 170), (255, 240, 220), y / H_))
    for i in range(0, W_, 128):  # awning stripes
        d.polygon([(i, 0), (i + 64, 0), (i + 64, 110), (i, 130)], fill=(236, 72, 100))
        d.polygon([(i + 64, 0), (i + 128, 0), (i + 128, 130), (i + 64, 110)], fill=(255, 250, 245))
    for x in range(0, W_, 64):
        d.pieslice([x, 100, x + 64, 160], 0, 180, fill=(236, 72, 100) if (x // 64) % 2 == 0 else (255, 250, 245))
    for y in range(520, H_, 40):  # wood table
        d.rectangle([0, y, W_, y + 38], fill=mix((150, 95, 50), (120, 75, 40), (y - 520) / 200))
        d.line([(0, y + 38), (W_, y + 38)], fill=(90, 55, 30), width=2)
    im.save(f'{OUT}/bg_market.jpg', quality=82, optimize=True)


def basket():
    w, h = 260, 150; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    d.polygon([(10 * s, 40 * s), (250 * s, 40 * s), (220 * s, 145 * s), (40 * s, 145 * s)], fill=(170, 110, 50))
    for y in range(50, 145, 16): d.line([(14 * s, y * s), (246 * s, y * s)], fill=(120, 75, 35), width=3 * s)
    for x in range(30, 240, 22): d.line([(x * s, 42 * s), ((x + (125 - x) * 0.12) * s, 144 * s)], fill=(140, 90, 40), width=3 * s)
    d.rounded_rectangle([4 * s, 30 * s, 256 * s, 50 * s], radius=10 * s, fill=(140, 85, 40))
    save(im, 'basket.png', w, h)


# ------------------------------------------------------------------ pizza
def pizza():
    w = h = 400; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    d.ellipse([4 * s, 4 * s, 396 * s, 396 * s], fill=(200, 130, 60))
    d.ellipse([24 * s, 24 * s, 376 * s, 376 * s], fill=(240, 190, 90))
    d.ellipse([34 * s, 34 * s, 366 * s, 366 * s], fill=(220, 60, 40))
    d.ellipse([44 * s, 44 * s, 356 * s, 356 * s], fill=(255, 220, 120))
    random.seed(3)
    for _ in range(14):
        a, r = random.uniform(0, 6.28), random.uniform(30, 130)
        x, y = 200 + r * math.cos(a), 200 + r * math.sin(a)
        d.ellipse([(x - 20) * s, (y - 20) * s, (x + 20) * s, (y + 20) * s], fill=(190, 40, 40))
        d.ellipse([(x - 14) * s, (y - 16) * s, (x - 4) * s, (y - 8) * s], fill=(230, 90, 80))
    for _ in range(18):
        a, r = random.uniform(0, 6.28), random.uniform(20, 140)
        x, y = 200 + r * math.cos(a), 200 + r * math.sin(a)
        d.ellipse([(x - 6) * s, (y - 3) * s, (x + 6) * s, (y + 3) * s], fill=(60, 140, 50))
    save(im, 'pizza.png', w, h)


def table_bg():
    W_, H_ = 1280, 720
    im = Image.new('RGB', (W_, H_), (240, 240, 240)); d = ImageDraw.Draw(im)
    sq = 80
    for y in range(0, H_, sq):
        for x in range(0, W_, sq):
            if (x // sq + y // sq) % 2 == 0: d.rectangle([x, y, x + sq, y + sq], fill=(214, 60, 60))
            else: d.rectangle([x, y, x + sq, y + sq], fill=(250, 245, 240))
    im = im.filter(ImageFilter.GaussianBlur(1.2))
    im.save(f'{OUT}/bg_table.jpg', quality=80, optimize=True)


# ------------------------------------------------------------------ owl runner
def owl(frame):
    w, h = 160, 180; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    lift = [0, 6][frame]
    # feet
    for fx in (58, 102):
        dy = (8 if (fx == 58) == (frame == 0) else 0)
        d.ellipse([(fx - 14) * s, (160 - dy) * s, (fx + 14) * s, (176 - dy) * s], fill=(249, 115, 22))
    # body
    for i in range(50):
        t = i / 50
        d.ellipse([(22 + 12 * t) * s, (40 + 10 * t - lift) * s, (138 - 16 * t) * s, (166 - 8 * t - lift) * s], fill=mix((90, 60, 160), (160, 120, 240), t))
    d.ellipse([46 * s, (96 - lift) * s, 114 * s, (162 - lift) * s], fill=(235, 220, 250))
    for y in range(108, 150, 12):
        for x in (62, 80, 98):
            d.arc([(x - 7) * s, (y - lift) * s, (x + 7) * s, (y + 10 - lift) * s], 20, 160, fill=(170, 150, 200), width=2 * s)
    # ears
    d.polygon([(34 * s, (52 - lift) * s), (42 * s, (14 - lift) * s), (62 * s, (44 - lift) * s)], fill=(90, 60, 160))
    d.polygon([(126 * s, (52 - lift) * s), (118 * s, (14 - lift) * s), (98 * s, (44 - lift) * s)], fill=(90, 60, 160))
    # eyes
    for ex in (58, 102):
        d.ellipse([(ex - 22) * s, (50 - lift) * s, (ex + 22) * s, (94 - lift) * s], fill=(255, 255, 255))
        d.ellipse([(ex - 10) * s, (62 - lift) * s, (ex + 10) * s, (82 - lift) * s], fill=(30, 30, 50))
        d.ellipse([(ex - 5) * s, (64 - lift) * s, (ex + 1) * s, (70 - lift) * s], fill=(255, 255, 255))
    d.polygon([(72 * s, (88 - lift) * s), (88 * s, (88 - lift) * s), (80 * s, (102 - lift) * s)], fill=(251, 191, 36))
    # wings
    wa = 10 if frame else -6
    d.ellipse([10 * s, (86 + wa - lift) * s, 40 * s, (140 + wa - lift) * s], fill=(70, 45, 135))
    d.ellipse([120 * s, (86 - wa - lift) * s, 150 * s, (140 - wa - lift) * s], fill=(70, 45, 135))
    save(im, f'owl{frame}.png', w, h)


def road_bg():
    W_, H_ = 720, 1280
    im = Image.new('RGB', (W_, H_)); d = ImageDraw.Draw(im)
    for y in range(H_):
        d.line([(0, y), (W_, y)], fill=mix((60, 150, 80), (80, 180, 100), y / H_))
    d.rectangle([90, 0, 630, H_], fill=(70, 72, 90))
    d.rectangle([84, 0, 96, H_], fill=(240, 240, 240)); d.rectangle([624, 0, 636, H_], fill=(240, 240, 240))
    for x in (270, 450):
        for y in range(0, H_, 120): d.rectangle([x - 5, y, x + 5, y + 64], fill=(250, 220, 90))
    random.seed(11)
    for _ in range(40):
        x = random.choice([random.uniform(5, 75), random.uniform(645, 715)]); y = random.uniform(0, H_)
        r = random.uniform(14, 30)
        d.ellipse([x - r, y - r, x + r, y + r], fill=(40, 120, 60)); d.ellipse([x - r * 0.6, y - r * 0.7, x + r * 0.3, y], fill=(70, 160, 90))
    im.save(f'{OUT}/bg_road.jpg', quality=80, optimize=True)


def gate():
    w, h = 200, 120; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    d.rectangle([14 * s, 60 * s, 24 * s, 120 * s], fill=(120, 120, 140)); d.rectangle([176 * s, 60 * s, 186 * s, 120 * s], fill=(120, 120, 140))
    d.rounded_rectangle([4 * s, 4 * s, 196 * s, 76 * s], radius=18 * s, fill=(30, 32, 60))
    d.rounded_rectangle([10 * s, 10 * s, 190 * s, 70 * s], radius=14 * s, fill=(255, 255, 255))
    save(im, 'gate.png', w, h)


def coin():
    w = h = 80; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    for i in range(40):
        t = i / 40; r = 38 * (1 - t * 0.85)
        d.ellipse([(40 - 8 * t - r) * s, (40 - 8 * t - r) * s, (40 - 8 * t + r) * s, (40 - 8 * t + r) * s], fill=mix((200, 140, 10), (255, 240, 140), t))
    d.ellipse([16 * s, 16 * s, 64 * s, 64 * s], outline=(190, 130, 10), width=3 * s)
    save(im, 'coin.png', w, h)


# ------------------------------------------------------------------ cards + hud
def card_back():
    w, h = 200, 260; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    d.rounded_rectangle([2 * s, 2 * s, 198 * s, 258 * s], radius=22 * s, fill=(91, 70, 201))
    d.rounded_rectangle([12 * s, 12 * s, 188 * s, 248 * s], radius=16 * s, outline=(255, 255, 255), width=3 * s)
    for i in range(-6, 12):
        d.line([((i * 30) * s, 12 * s), ((i * 30 + 236) * s, 248 * s)], fill=(120, 100, 220), width=6 * s)
    d.rounded_rectangle([12 * s, 12 * s, 188 * s, 248 * s], radius=16 * s, outline=(255, 255, 255), width=3 * s)
    d.ellipse([60 * s, 90 * s, 140 * s, 170 * s], fill=(249, 115, 22))
    d.text((100 * s, 130 * s), 'M', fill=(255, 255, 255), anchor='mm', font=__import__('PIL.ImageFont', fromlist=['x']).truetype('/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf', 56 * s))
    save(im, 'card_back.png', w, h)


def card_front():
    w, h = 200, 260; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    d.rounded_rectangle([2 * s, 2 * s, 198 * s, 258 * s], radius=22 * s, fill=(255, 255, 255), outline=(220, 220, 235), width=3 * s)
    d.rounded_rectangle([12 * s, 12 * s, 188 * s, 248 * s], radius=16 * s, outline=(235, 230, 250), width=2 * s)
    save(im, 'card_front.png', w, h)


def heart():
    w = h = 64; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    d.ellipse([4 * s, 8 * s, 34 * s, 38 * s], fill=(239, 68, 68)); d.ellipse([30 * s, 8 * s, 60 * s, 38 * s], fill=(239, 68, 68))
    d.polygon([(6 * s, 28 * s), (58 * s, 28 * s), (32 * s, 58 * s)], fill=(239, 68, 68))
    d.ellipse([12 * s, 14 * s, 22 * s, 24 * s], fill=(255, 180, 180))
    save(im, 'heart.png', w, h)




# ------------------------------------------------------------------ 1.0.10 sprites
def bomb():
    w = h = 200; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    for i in range(50):
        t = i / 50; r = 78 * (1 - t * 0.85)
        d.ellipse([(96 - 26 * t - r) * s, (112 - 26 * t - r) * s, (96 - 26 * t + r) * s, (112 - 26 * t + r) * s], fill=mix((12, 12, 20), (90, 95, 120), t ** 1.6))
    d.rounded_rectangle([118 * s, 28 * s, 152 * s, 52 * s], radius=6 * s, fill=(70, 72, 90))
    save(im, 'bomb.png', w, h)


def castle():
    w, h = 240, 260; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    stone, dark = (148, 156, 172), (100, 108, 124)
    d.rectangle([20 * s, 90 * s, 220 * s, 260 * s], fill=stone)
    for x in (0, 160):
        d.rectangle([x * s, 50 * s, (x + 80) * s, 260 * s], fill=mix(stone, (255, 255, 255), 0.08))
        for bx in range(x, x + 80, 22): d.rectangle([bx * s, 30 * s, (bx + 14) * s, 52 * s], fill=mix(stone, (255, 255, 255), 0.08))
    for bx in range(80, 160, 22): d.rectangle([bx * s, 72 * s, (bx + 14) * s, 92 * s], fill=stone)
    for y in range(110, 260, 24):
        for x in range(20 if (y // 24) % 2 else 32, 220, 48): d.rectangle([x * s, y * s, (x + 40) * s, (y + 2) * s], fill=dark)
    d.pieslice([90 * s, 160 * s, 150 * s, 220 * s], 180, 360, fill=(60, 40, 30)); d.rectangle([90 * s, 190 * s, 150 * s, 260 * s], fill=(60, 40, 30))
    for x in (30, 190): d.rounded_rectangle([x * s, 100 * s, (x + 20) * s, 130 * s], radius=10 * s, fill=(40, 40, 60))
    d.line([(40 * s, 50 * s), (40 * s, 4 * s)], fill=(90, 60, 30), width=4 * s); d.polygon([(42 * s, 4 * s), (78 * s, 14 * s), (42 * s, 26 * s)], fill=(249, 115, 22))
    save(im, 'castle.png', w, h)


def monster(n, col):
    w, h = 140, 150; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    for i in range(40):
        t = i / 40
        d.ellipse([(14 + 10 * t) * s, (20 + 8 * t) * s, (126 - 14 * t) * s, (140 - 12 * t) * s], fill=mix(mix(col, (0, 0, 0), 0.3), mix(col, (255, 255, 255), 0.35), t))
    for x in (34, 92): d.polygon([(x * s, 34 * s), ((x + 8) * s, 4 * s), ((x + 18) * s, 32 * s)], fill=mix(col, (0, 0, 0), 0.25))
    for x in (50, 90):
        d.ellipse([(x - 16) * s, 46 * s, (x + 16) * s, 78 * s], fill=(255, 255, 255)); d.ellipse([(x - 7) * s, 56 * s, (x + 7) * s, 70 * s], fill=(20, 20, 30))
    d.chord([40 * s, 80 * s, 100 * s, 118 * s], 0, 180, fill=(40, 10, 20))
    for x in (52, 72): d.polygon([(x * s, 82 * s), ((x + 8) * s, 82 * s), ((x + 4) * s, 92 * s)], fill=(255, 255, 255))
    for x in (34, 92): d.rounded_rectangle([(x - 2) * s, 126 * s, (x + 18) * s, 148 * s], radius=6 * s, fill=mix(col, (0, 0, 0), 0.35))
    save(im, f'monster{n}.png', w, h)


def car(name, col):
    w, h = 220, 110; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    for x in (40, 150):
        for y in (8, 82): d.rounded_rectangle([x * s, y * s, (x + 36) * s, (y + 20) * s], radius=6 * s, fill=(20, 20, 26))
    d.rounded_rectangle([14 * s, 18 * s, 206 * s, 92 * s], radius=26 * s, fill=col)
    d.rounded_rectangle([24 * s, 28 * s, 196 * s, 82 * s], radius=20 * s, fill=mix(col, (255, 255, 255), 0.18))
    d.rounded_rectangle([110 * s, 34 * s, 150 * s, 76 * s], radius=10 * s, fill=(40, 60, 90))
    d.rounded_rectangle([60 * s, 34 * s, 92 * s, 76 * s], radius=10 * s, fill=(40, 60, 90))
    d.rectangle([4 * s, 30 * s, 16 * s, 80 * s], fill=mix(col, (0, 0, 0), 0.4))
    for y in (26, 74): d.ellipse([196 * s, y * s, 210 * s, (y + 10) * s], fill=(255, 240, 160))
    d.rectangle([150 * s, 52 * s, 206 * s, 58 * s], fill=(255, 255, 255))
    save(im, f'car_{name}.png', w, h)


def stone():
    w, h = 200, 110; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    for i in range(40):
        t = i / 40
        d.ellipse([(8 + 20 * t) * s, (10 + 10 * t) * s, (192 - 26 * t) * s, (100 - 14 * t) * s], fill=mix((70, 75, 85), (190, 195, 205), t ** 1.4))
    for (x, y, r) in [(60, 40, 6), (130, 60, 5), (100, 30, 4)]: d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=(120, 126, 138))
    save(im, 'stone.png', w, h)


def snake_icon():
    w, h = 200, 160; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    pts = [(20, 120), (50, 120), (80, 120), (80, 90), (80, 60), (110, 60), (140, 60), (140, 90)]
    for i, (x, y) in enumerate(pts):
        c = (34, 197, 94) if i % 2 else (22, 163, 74)
        d.rounded_rectangle([(x - 14) * s, (y - 14) * s, (x + 14) * s, (y + 14) * s], radius=9 * s, fill=c)
    hx, hy = 140, 100
    d.rounded_rectangle([(hx - 18) * s, (hy - 16) * s, (hx + 18) * s, (hy + 20) * s], radius=12 * s, fill=(134, 239, 172))
    for ex in (-7, 7): d.ellipse([(hx + ex - 4) * s, (hy + 2) * s, (hx + ex + 4) * s, (hy + 10) * s], fill=(17, 17, 17))
    d.line([(hx * s, (hy + 20) * s), (hx * s, (hy + 32) * s)], fill=(239, 68, 68), width=3 * s)
    save(im, 'snake.png', w, h)


def bolt():
    w, h = 160, 200; im = canvas(w, h); d = ImageDraw.Draw(im); s = K
    pts = [(96, 6), (24, 112), (74, 112), (56, 194), (138, 78), (86, 78), (112, 6)]
    d.polygon([(x * s, y * s) for x, y in pts], fill=(250, 204, 21))
    d.polygon([((x + 6) * s, (y + 4) * s) for x, y in pts[:3]], fill=(253, 230, 138))
    save(im, 'bolt.png', w, h)


if __name__ == '__main__':
    space_bg(); ship(); [asteroid(i) for i in range(3)]
    sky_bg(); [balloon(n, c) for n, c in [('red', (239, 68, 68)), ('blue', (59, 130, 246)), ('green', (34, 197, 94)), ('yellow', (250, 204, 21)), ('purple', (168, 85, 247)), ('pink', (236, 72, 153))]]
    market_bg(); basket(); [fruit(n) for n in ('apple', 'orange', 'watermelon', 'lemon', 'grapes')]
    pizza(); table_bg()
    owl(0); owl(1); road_bg(); gate(); coin()
    card_back(); card_front(); heart()
    bomb(); castle(); [monster(i, c) for i, c in enumerate([(168, 85, 247), (34, 197, 94), (239, 68, 68)])]
    car('orange', (249, 115, 22)); car('purple', (139, 92, 246)); stone(); snake_icon(); bolt()
    tot = sum(os.path.getsize(f'{OUT}/{f}') for f in os.listdir(OUT))
    print(len(os.listdir(OUT)), 'images', round(tot / 1024), 'KB')
