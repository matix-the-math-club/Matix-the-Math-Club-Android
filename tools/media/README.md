# Matix video course generator

Every video in the app is a Math Learn lesson in `app/src/main/assets/media/lessons`, drawn frame by frame
in code — no stock footage, no downloaded images. 103 lessons, about one minute each, Grade 1 to college.

Requirements: Python 3.9+, Pillow, and an ffmpeg with libx264 (`pip install pillow imageio-ffmpeg` gives you both).
Fonts come from macOS (Arial Rounded Bold, Arial Bold, Arial Unicode); set `MX_FONT_DIR` elsewhere.

| File | What it is |
| --- | --- |
| `course/pkit.py` | drawing kit: 2x supersampled frames, kinetic text with font fallback for math symbols, shapes, x264 pipe |
| `course/lessons.py` | early-grade scenes: counting, adding, column maths, times tables, sharing, clocks, shapes |
| `course/scenes2.py` | later scenes: worked steps, graphs, tangents, Riemann sums, Pythagoras, trig, circles, probability, matrices, vectors, "Your turn" practice |
| `course/curriculum.py` | the course: every lesson by grade, with its scenes, recap and quiz |
| `course/render_course.py` | renders all lessons in parallel |
| `course/build_course.py` | copies them into the app, makes posters and writes the course data into `js/matix-course.js` |

```sh
python3 -m venv venv && ./venv/bin/pip install pillow imageio-ffmpeg
cd tools/media/course
../../../venv/bin/python curriculum.py list                         # lessons and lengths
../../../venv/bin/python curriculum.py stills slope 10,25,40         # preview PNGs in ./out
../../../venv/bin/python render_course.py ./out 620 5                # render (constant 620 kbps ≈ 480 MB, 5 workers)
../../../venv/bin/python build_course.py ../../.. ./out              # install into the app
```

Each lesson is padded to about a minute with "Your turn!" practice scenes taken from its quiz.
To add a lesson, add an `L(...)` entry in `curriculum.py`, render it, and rebuild.
