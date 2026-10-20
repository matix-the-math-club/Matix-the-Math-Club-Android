# Matix media generator

Every video in `app/src/main/assets/media` is generated from math by these scripts — no stock footage, no downloaded images.
Run everything from this folder (`cd tools/media`). Rendered masters go to `./out/`, which is not part of the app.

Requirements: Python 3.10+, `pycairo`, `numpy`, `fonttools`, `ffmpeg` with libx264, a C compiler, and the
Noto Sans / Noto Sans Arabic / Arial fonts (used for symbols in the animations).

| Script | Renders |
| --- | --- |
| `scenes_brand.py intro` / `scenes_brand.py welcome` | boot intro film and the welcome-screen loop (portrait + landscape) |
| `scenes_hero.py <kind>` (or no argument for all) | 1920×480 looping header animations, one per section |
| `scenes_fx.py` | confetti / stars / rings / badge celebration effects (portrait + landscape) |
| `math1.py <name>` | fourier, lissajous, unit_circle, pythagoras, phyllotaxis, cardioid, tesseract, koch_hilbert |
| `math2.py <name>` | chaos_game, galton, primes, monte_carlo, life, cellular, double_pendulum, lorenz |
| `render_fractals.sh` + `c/fractal.c` | Mandelbrot deep zoom (mode 0), Julia morph (1), Newton z³−1 (2) |
| `build_media.py` | copies/encodes masters into the app within a size budget, makes posters + highlight reel, writes `media/manifest.json` and the gallery list inside `js/matix-media.js` |

Fractals:

```sh
cc -O3 -o c/fractal c/fractal.c -lm -lpthread
./render_fractals.sh 0 1280 720 720 16 math_mandelbrot.mp4
./render_fractals.sh 1 1920 1080 600 17 math_julia.mp4
./render_fractals.sh 2 1920 1080 450 17 math_newton.mp4
```

Size budget: `TARGET_MEDIA=188e6 CAP_KBPS=3600 python3 build_media.py` fills about 188 MB of media; the bitrate of the
biggest fractal films is tuned so the total hits the target. Keep the whole APK/AAB base under Google Play's
200 MB limit (the app code + SVGs add about 3 MB), or move `media/gallery` into a Play Asset Delivery pack
if you want to go bigger.
