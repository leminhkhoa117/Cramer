import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const frontendRoot = path.resolve(scriptDir, '..');
const cssRoot = path.join(frontendRoot, 'src', 'css');
const adminCssRoot = path.join(frontendRoot, 'src', 'admin', 'css');
const maxLines = 999;

const touchedFiles = new Set();

function absolute(relativePath, base = cssRoot) {
  return path.join(base, ...relativePath.split('/'));
}

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

function readLines(filePath) {
  const text = readText(filePath).replace(/\r\n/g, '\n');
  return text.endsWith('\n') ? text.slice(0, -1).split('\n') : text.split('\n');
}

function writeLines(filePath, lines) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, `${lines.join('\n')}\n`, 'utf8');
  touchedFiles.add(filePath);
}

function lineCount(filePath) {
  const text = readText(filePath).replace(/\r\n/g, '\n');
  if (text.length === 0) return 0;
  return text.endsWith('\n') ? text.slice(0, -1).split('\n').length : text.split('\n').length;
}

function isImportHub(filePath, imports) {
  if (!fs.existsSync(filePath)) return false;
  const text = readText(filePath);
  return imports.every((importPath) => text.includes(`@import '${importPath}';`));
}

function sliceRange(lines, startLine, endLine) {
  const startIndex = Math.max(startLine - 1, 0);
  const endIndex = endLine == null ? lines.length : Math.min(endLine, lines.length);
  return lines.slice(startIndex, endIndex);
}

function writeHub(filePath, title, imports) {
  const lines = [
    '/* ===================================',
    `   ${title}`,
    '   Import order matters.',
    '   =================================== */',
    ...imports.map((importPath) => `@import '${importPath}';`),
  ];
  writeLines(filePath, lines);
}

function splitLineRanges({ source, base = cssRoot, shardDir, title, shards, imports }) {
  const sourcePath = absolute(source, base);
  const importPaths = imports ?? shards.map((shard) => `${shardDir}/${shard.file}`);

  if (!isImportHub(sourcePath, importPaths)) {
    const lines = readLines(sourcePath);
    for (const shard of shards) {
      writeLines(absolute(`${shardDir}/${shard.file}`, base), sliceRange(lines, shard.start, shard.end));
    }
    writeHub(sourcePath, title, importPaths);
  }

  touchedFiles.add(sourcePath);
  for (const importPath of importPaths) {
    touchedFiles.add(absolute(importPath, base));
  }
}

function extractLeadingRootBlock(lines) {
  const rootIndex = lines.findIndex((line) => line.trim() === ':root {');
  if (rootIndex === -1) return [];

  let startIndex = rootIndex;
  if (rootIndex > 0 && lines[rootIndex - 1].trim().startsWith('/* ===')) {
    startIndex = rootIndex - 1;
  }

  let depth = 0;
  for (let index = rootIndex; index < lines.length; index += 1) {
    for (const char of lines[index]) {
      if (char === '{') depth += 1;
      if (char === '}') depth -= 1;
    }
    if (depth === 0) {
      return lines.slice(startIndex, index + 1);
    }
  }

  throw new Error('Could not find the end of the leading :root block.');
}

function writeRootTokenHub({ source, tokenShard, pageHub, title }) {
  const sourcePath = absolute(source);
  const imports = [tokenShard, pageHub];

  if (!isImportHub(sourcePath, imports)) {
    const tokenLines = extractLeadingRootBlock(readLines(sourcePath));
    if (tokenLines.length === 0) {
      throw new Error(`No leading :root token block found in ${source}`);
    }
    writeLines(absolute(tokenShard), tokenLines);
    writeHub(sourcePath, title, imports);
  }

  touchedFiles.add(sourcePath);
  touchedFiles.add(absolute(tokenShard));
}

function rewriteHomePageHub() {
  const sourcePath = absolute('pages/home.css');
  const imports = [
    '../home/home-base.css',
    '../home/home-hero.css',
    '../home/home-features.css',
    '../home/home-guide.css',
    '../home/home-testimonials.css',
    '../home/home-demo.css',
    '../home/home-faq.css',
    '../home/home-signup.css',
    '../home/home-zigzag.css',
    '../home/home-responsive.css',
  ];

  if (!isImportHub(sourcePath, imports)) {
    writeHub(sourcePath, 'CRAMER HOMEPAGE - page import hub', imports);
  }

  touchedFiles.add(sourcePath);
}

function splitWritingResult() {
  splitLineRanges({
    source: 'test/writing-result.css',
    shardDir: 'test/writing-result-shards',
    title: 'Writing Result Page Styles - import hub',
    shards: [
      { file: '01-base.css', start: 1, end: 42 },
      { file: '02-header-note.css', start: 43, end: 46 },
      { file: '03-task-scores-content.css', start: 47, end: 639 },
      { file: '04-feedback-word-analysis.css', start: 640, end: 1222 },
      { file: '05-footer-grading-base.css', start: 1223, end: 1861 },
      { file: '06-grading-progress.css', start: 1862, end: 2427 },
      { file: '07-comparison.css', start: 2428, end: 2815 },
      { file: '08-responsive.css', start: 2816, end: null },
    ],
  });

  const sourcePath = absolute('writing-result-page.css');
  const imports = [
    './test/writing-result-shards/01-base.css',
    './writing-result-page-shards/02-review-header.css',
    './test/writing-result-shards/03-task-scores-content.css',
    './test/writing-result-shards/04-feedback-word-analysis.css',
    './writing-result-page-shards/05-highlight-flash.css',
    './test/writing-result-shards/05-footer-grading-base.css',
    './test/writing-result-shards/06-grading-progress.css',
    './test/writing-result-shards/07-comparison.css',
    './test/writing-result-shards/08-responsive.css',
  ];

  if (!isImportHub(sourcePath, imports)) {
    const lines = readLines(sourcePath);
    writeLines(absolute('writing-result-page-shards/02-review-header.css'), sliceRange(lines, 43, 245));
    writeLines(absolute('writing-result-page-shards/05-highlight-flash.css'), sliceRange(lines, 1422, 1438));
    writeHub(sourcePath, 'Writing Result Page Styles - legacy import hub', imports);
  }

  touchedFiles.add(sourcePath);
  for (const importPath of imports) {
    touchedFiles.add(absolute(importPath));
  }
}

function validate() {
  const rows = [...touchedFiles]
    .filter((filePath) => fs.existsSync(filePath))
    .sort((left, right) => left.localeCompare(right))
    .map((filePath) => ({
      filePath,
      relative: path.relative(frontendRoot, filePath).replace(/\\/g, '/'),
      lines: lineCount(filePath),
    }));

  const offenders = rows.filter((row) => row.lines > maxLines);

  console.log('Large CSS split line counts:');
  for (const row of rows) {
    console.log(`${String(row.lines).padStart(4, ' ')}  ${row.relative}`);
  }

  if (offenders.length > 0) {
    console.error('\nFiles still above the line limit:');
    for (const offender of offenders) {
      console.error(`${offender.lines}  ${offender.relative}`);
    }
    process.exitCode = 1;
  }
}

rewriteHomePageHub();

splitLineRanges({
  source: 'home/home-features.css',
  shardDir: 'home/home-features-shards',
  title: 'SECTION: FEATURES - import hub',
  shards: [
    { file: '01-grid-and-stacked-intro.css', start: 1, end: 578 },
    { file: '02-stacked-and-zigzag.css', start: 579, end: 1192 },
    { file: '03-showcase-cards-1-2.css', start: 1193, end: 1700 },
    { file: '04-showcase-cards-3-6.css', start: 1701, end: 2400 },
    { file: '05-premium-tuning.css', start: 2401, end: null },
  ],
});

splitLineRanges({
  source: 'home/home-demo.css',
  shardDir: 'home/home-demo-shards',
  title: 'SECTION: INTERACTIVE DEMO - import hub',
  shards: [
    { file: '01-demo-window.css', start: 1, end: 566 },
    { file: '02-audio-questions-and-cta.css', start: 567, end: null },
  ],
});

splitLineRanges({
  source: 'home/home-responsive.css',
  shardDir: 'home/home-responsive-shards',
  title: 'HOMEPAGE RESPONSIVE - import hub',
  shards: [
    { file: '01-tablet.css', start: 1, end: 487 },
    { file: '02-mobile.css', start: 488, end: 1429 },
    { file: '03-motion-and-small-mobile.css', start: 1430, end: null },
  ],
});

splitLineRanges({
  source: 'pages/pricing.css',
  shardDir: 'pages/pricing-shards',
  title: 'PRICING PAGE STYLES - import hub',
  shards: [
    { file: '01-root-and-tiers.css', start: 1, end: 400 },
    { file: '02-demo.css', start: 401, end: 950 },
    { file: '03-sections.css', start: 951, end: 1290 },
    { file: '04-responsive.css', start: 1291, end: null },
  ],
});

writeRootTokenHub({
  source: 'pricing-page.css',
  tokenShard: './pricing-page-shards/00-tokens.css',
  pageHub: './pages/pricing.css',
  title: 'PRICING PAGE STYLES - legacy import hub',
});

splitLineRanges({
  source: 'pages/subscription.css',
  shardDir: 'pages/subscription-shards',
  title: 'SUBSCRIPTION PAGE STYLES - import hub',
  shards: [
    { file: '01-overview-and-wallet.css', start: 1, end: 846 },
    { file: '02-history-and-sessions.css', start: 847, end: 1167 },
    { file: '03-responsive-and-cancel.css', start: 1168, end: null },
  ],
});

writeRootTokenHub({
  source: 'subscription-page.css',
  tokenShard: './subscription-page-shards/00-tokens.css',
  pageHub: './pages/subscription.css',
  title: 'SUBSCRIPTION PAGE STYLES - legacy import hub',
});

splitLineRanges({
  source: 'pages/about.css',
  shardDir: 'pages/about-shards',
  title: 'ABOUT PAGE STYLES - import hub',
  shards: [
    { file: '01-hero-through-story.css', start: 1, end: 783 },
    { file: '02-comparison-through-interactive.css', start: 784, end: 1257 },
    { file: '03-responsive.css', start: 1258, end: null },
  ],
});

writeRootTokenHub({
  source: 'about.css',
  tokenShard: './about-page-shards/00-tokens.css',
  pageHub: './pages/about.css',
  title: 'ABOUT PAGE STYLES - legacy import hub',
});

splitLineRanges({
  source: 'pages/vocabulary.css',
  shardDir: 'pages/vocabulary-shards',
  title: 'VOCABULARY PAGE STYLES - import hub',
  shards: [
    { file: '01-layout-toolbar-and-list.css', start: 1, end: 656 },
    { file: '02-modal-and-card-states.css', start: 657, end: 1134 },
    { file: '03-responsive.css', start: 1135, end: null },
  ],
});

writeRootTokenHub({
  source: 'vocabulary-page.css',
  tokenShard: './vocabulary-page-shards/00-tokens.css',
  pageHub: './pages/vocabulary.css',
  title: 'VOCABULARY PAGE STYLES - legacy import hub',
});

splitWritingResult();

splitLineRanges({
  source: 'test/test-review.css',
  shardDir: 'test/test-review-shards',
  title: 'TestReview.css - import hub',
  shards: [
    { file: '01-layout-and-question-rendering.css', start: 1, end: 785 },
    { file: '02-writing-review-and-results.css', start: 786, end: 1428 },
    { file: '03-explanations-and-print.css', start: 1429, end: null },
  ],
});

splitLineRanges({
  source: 'pages/users/UserDetailPage.css',
  base: adminCssRoot,
  shardDir: 'pages/users/UserDetailPage-shards',
  title: 'UserDetailPage.css - import hub',
  shards: [
    { file: '01-profile-and-activity.css', start: 1, end: 549 },
    { file: '02-attempts-and-modals.css', start: 550, end: 1118 },
    { file: '03-admin-modal-overrides.css', start: 1119, end: null },
  ],
});

validate();