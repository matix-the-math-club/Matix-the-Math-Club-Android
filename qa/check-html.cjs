// Run from the project root: node qa/check-html.cjs
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const html = fs.readFileSync(path.join(__dirname, '../app/src/main/assets/app.html'), 'utf8');
let checked = 0;
for (const match of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script\s*>/gi)) {
    if (/\bsrc\s*=|\btype\s*=\s*["'](?:module|application\/json)/i.test(match[1])) continue;
    new vm.Script(match[2], { filename: `app.html:inline-${++checked}` });
}
if (!checked) throw new Error('No inline app scripts found');
console.log(`PASS: ${checked} inline JavaScript blocks parse successfully.`);
