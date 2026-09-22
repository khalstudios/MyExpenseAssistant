# Play Store assets

| File | Play Console field | Spec |
| --- | --- | --- |
| `play-icon-512.png` | App icon | 512 × 512, 32-bit PNG with alpha |
| `feature-graphic-1024x500.png` | Feature graphic | 1024 × 500 PNG, no alpha |
| `listing.md` | Full description | Up to 4000 characters |

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
