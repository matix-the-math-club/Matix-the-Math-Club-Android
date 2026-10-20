#!/bin/sh
# usage: render_fractals.sh mode w h frames crf out [ss]
cd "$(dirname "$0")"
enc() { ffmpeg -y -loglevel error -f rawvideo -pix_fmt rgb24 -s ${2}x${3} -r 30 -i - -vf scale=out_color_matrix=bt709:out_range=tv,format=yuv420p -c:v libx264 -preset medium -crf $5 -profile:v high -level 4.1 -colorspace bt709 -color_primaries bt709 -color_trc bt709 -g 60 -threads 2 -an -movflags +faststart out/$6; }
./c/fractal $1 $2 $3 $4 ${7:-1} 2 | enc "$@"
ls -la out/$6
