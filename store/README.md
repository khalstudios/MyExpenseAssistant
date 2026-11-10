# Play Store assets

| File | Play Console field | Spec |
| --- | --- | --- |
| `play-icon-512.png` | App icon | 512 × 512, 32-bit PNG with alpha |
| `feature-graphic-1024x500.png` | Feature graphic | 1024 × 500 PNG, no alpha |
| `listing.md` | Full description | Up to 4000 characters |

## Social media

| Files | Use | Spec |
| --- | --- | --- |
| `feed/post-1.png` … `post-4.png` | Instagram / Facebook single feed posts | 1080 × 1350 (4:5) |
| `feed/post-5.png` … `post-8.png` | Instagram / Facebook carousel, in order | 1080 × 1350 (4:5) |
| `reel/reel.mp4`, `reel/cover.jpg` | Instagram / Facebook Reel and its cover | 1080 × 1920, 22 s, 30 fps, H.264 |
| `feed/diwali-1.png`, `diwali-2.png` | Diwali feed posts (from `src/feed-diwali.html#1`, `#2`) | 1080 × 1350 (4:5) |
| `reel/reel-diwali.mp4`, `reel/cover-diwali.jpg` | Diwali Reel (from `src/reel-diwali.html`) and its cover | 1080 × 1920, 19.5 s, 30 fps, H.264 |
| `reel/reel-2026.mp4`, `reel/cover-2026.jpg` | Year-in-review / New Year Reel (from `src/reel-2026.html`) and its cover | 1080 × 1920, 22.5 s, 30 fps, H.264 |
| `reel/reel-christmas.mp4`, `reel/cover-christmas.jpg` | Christmas Reel (from `src/reel-christmas.html`) and its cover | 1080 × 1920, 19.5 s, 30 fps, H.264 |
| `social-posts.md` | Captions, hashtags and alt text for the feed posts and reel | |

`src/feed.html` lays out the feed posts; open `feed.html#1` … `#8` to see each one. Render each
with the same Edge command as the feature graphic, at `--window-size=1080,1350`, passing the page as
a Windows path (`file:///D:/…/src/feed.html#N`).

`src/reel.html` animates the reel; open it in a browser to watch it loop, or add `#t=5` to hold one
moment. `src/render-reel.js` renders it to `reel/reel.mp4` through Edge. It needs Node.js and two
packages installed in a folder outside the repo:

```bash
npm i puppeteer-core@23.11.1 ffmpeg-static@5.2.0        # in, say, ~/reel-tools
NODE_PATH=~/reel-tools/node_modules node src/render-reel.js               # reel.html
NODE_PATH=~/reel-tools/node_modules node src/render-reel.js reel-diwali   # reel-diwali.html
NODE_PATH=~/reel-tools/node_modules node src/render-reel.js reel-2026     # reel-2026.html
NODE_PATH=~/reel-tools/node_modules node src/render-reel.js reel-christmas # reel-christmas.html
```

Take the cover from the finished video with
`ffmpeg -ss 2.2 -i reel/reel.mp4 -frames:v 1 -q:v 2 reel/cover.jpg`.

## Sources

- `src/icon.svg` is the icon design on the 108-unit adaptive icon canvas. The launcher icon
  (`app/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`,
  `ic_launcher_monochrome.xml`) and the notification icon (`ic_notification.xml`) mirror its shapes,
  so change them together.
- `src/icon-play.svg` is `icon.svg` cropped to the visible 72 units, used for the Play icon and the
  feature graphic.
- `src/feature-graphic.html` lays out the feature graphic. Its figures are sample data.
- `src/icon-preview.html` shows the icon under circle and squircle masks at small sizes.

## Re-rendering

With Microsoft Edge installed, from this folder:

```bash
sed 's|viewBox="0 0 108 108" width="108" height="108"|viewBox="18 18 72 72" width="512" height="512"|' src/icon.svg > src/icon-play.svg
EDGE="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"
"$EDGE" --headless=new --hide-scrollbars --force-device-scale-factor=1 --window-size=1024,500 \
  --screenshot="$PWD/feature-graphic-1024x500.png" "file:///$PWD/src/feature-graphic.html"
"$EDGE" --headless=new --hide-scrollbars --force-device-scale-factor=1 --window-size=512,512 \
  --screenshot="$PWD/play-icon-512.png" "file:///$PWD/src/icon-play.svg"
```

Edge writes RGB PNGs; Play wants the icon with an alpha channel, so convert `play-icon-512.png` to
32-bit RGBA afterwards (for example with `System.Drawing` in PowerShell, or any image editor).
