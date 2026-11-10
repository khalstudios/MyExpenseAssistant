// Renders <name>.html to ../reel/<name>.mp4 (1080x1920, 30 fps, H.264 with a silent audio track).
// Needs puppeteer-core and ffmpeg-static (npm i puppeteer-core ffmpeg-static in any folder, then
// point NODE_PATH at its node_modules) and Microsoft Edge.
// Usage: node render-reel.js [name, default reel] [fps, default 30]
const path = require('path');
const fs = require('fs');
const { spawn } = require('child_process');
const puppeteer = require('puppeteer-core');
const ffmpeg = require('ffmpeg-static');

const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const name = process.argv[2] || 'reel';
const fps = Number(process.argv[3] || 30);
const page = 'file:///' + path.resolve(__dirname, name + '.html').replace(/\\/g, '/') + '?render';
const out = path.resolve(__dirname, '../reel/' + name + '.mp4');

(async () => {
  fs.mkdirSync(path.dirname(out), { recursive: true });
  const browser = await puppeteer.launch({ executablePath: EDGE, headless: true, args: ['--hide-scrollbars'] });
  const tab = await browser.newPage();
  await tab.setViewport({ width: 1080, height: 1920, deviceScaleFactor: 1 });
  await tab.goto(page, { waitUntil: 'load' });
  await tab.evaluate(() => document.fonts.ready);
  const seconds = await tab.evaluate(() => window.REEL_SECONDS);

  const enc = spawn(ffmpeg, [
    '-y', '-f', 'image2pipe', '-c:v', 'mjpeg', '-framerate', String(fps), '-i', '-',
    '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=stereo',
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-r', String(fps),
    '-c:a', 'aac', '-b:a', '128k', '-shortest', '-movflags', '+faststart', out,
  ], { stdio: ['pipe', 'ignore', 'inherit'] });

  const frames = Math.round(seconds * fps);
  for (let i = 0; i < frames; i++) {
    await tab.evaluate(t => window.renderAt(t), i / fps);
    const jpg = await tab.screenshot({ type: 'jpeg', quality: 95 });
    if (!enc.stdin.write(jpg)) await new Promise(r => enc.stdin.once('drain', r));
    if (i % fps === 0) process.stdout.write(`\r${i / fps}s / ${seconds}s`);
  }
  enc.stdin.end();
  await new Promise(r => enc.on('close', r));
  await browser.close();
  console.log(`\nWrote ${out}`);
})();
