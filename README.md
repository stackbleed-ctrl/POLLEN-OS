![Pollen-OS Banner](./pollen-os-banner.png)

🌼 Pollen OS

**The private AI layer that turns Android phones into autonomous, cooperative agents.**

> Runs on-device • Works offline • Mesh-native autonomous agents

---

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>POLLEN-OS — Offline-First Mesh Coordination</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Share+Tech+Mono&family=Barlow:wght@300;400;500;600&family=Barlow+Condensed:wght@700;900&display=swap');

  * { box-sizing: border-box; margin: 0; padding: 0; }
  html { scroll-behavior: smooth; }

  body {
    background: #060504;
    color: #d4c4a0;
    font-family: 'Barlow', sans-serif;
    font-weight: 300;
    line-height: 1.7;
    min-height: 100vh;
    overflow-x: hidden;
  }

  body::before {
    content: '';
    position: fixed;
    inset: 0;
    background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.04'/%3E%3C/svg%3E");
    pointer-events: none;
    z-index: 0;
    opacity: 0.5;
  }

  body::after {
    content: '';
    position: fixed;
    inset: 0;
    background-image:
      linear-gradient(rgba(201,168,76,0.07) 1px, transparent 1px),
      linear-gradient(90deg, rgba(201,168,76,0.07) 1px, transparent 1px);
    background-size: 40px 40px;
    pointer-events: none;
    z-index: 0;
    opacity: 0.5;
  }

  .wrapper {
    position: relative;
    z-index: 1;
    max-width: 860px;
    margin: 0 auto;
    padding: 0 24px 80px;
  }

  header {
    padding: 64px 0 48px;
    border-bottom: 1px solid #2a2215;
    margin-bottom: 56px;
  }

  .tag-line {
    font-family: 'Share Tech Mono', monospace;
    font-size: 11px;
    color: #c9a84c;
    letter-spacing: 0.18em;
    text-transform: uppercase;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .tag-line::before {
    content: '';
    display: inline-block;
    width: 24px;
    height: 1px;
    background: #c9a84c;
  }

  h1 {
    font-family: 'Barlow Condensed', sans-serif;
    font-weight: 900;
    font-size: clamp(56px, 10vw, 96px);
    line-height: 0.9;
    letter-spacing: -0.02em;
    color: #ffffff;
  }

  h1 .dash { color: #c9a84c; }

  .subtitle {
    margin-top: 20px;
    font-size: 15px;
    color: #6b5a38;
    font-weight: 400;
    max-width: 420px;
  }

  .badges { display: flex; gap: 8px; margin-top: 28px; flex-wrap: wrap; }

  .badge {
    font-family: 'Share Tech Mono', monospace;
    font-size: 10px;
    letter-spacing: 0.12em;
    padding: 4px 10px;
    border: 1px solid #4a3c20;
    color: #6b5a38;
    text-transform: uppercase;
  }

  .badge.active {
    border-color: #c9a84c;
    color: #c9a84c;
    background: rgba(201,168,76,0.08);
  }

  section { margin-bottom: 52px; animation: fadeUp 0.5s ease both; }

  @keyframes fadeUp {
    from { opacity: 0; transform: translateY(16px); }
    to   { opacity: 1; transform: translateY(0); }
  }

  section:nth-child(1) { animation-delay: 0.05s; }
  section:nth-child(2) { animation-delay: 0.10s; }
  section:nth-child(3) { animation-delay: 0.15s; }
  section:nth-child(4) { animation-delay: 0.20s; }
  section:nth-child(5) { animation-delay: 0.25s; }

  h2 {
    font-family: 'Barlow Condensed', sans-serif;
    font-weight: 700;
    font-size: 11px;
    letter-spacing: 0.2em;
    text-transform: uppercase;
    color: #c9a84c;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
  }

  h2::after { content: ''; flex: 1; height: 1px; background: #2a2215; }

  .callout {
    border-left: 3px solid #c9a84c;
    padding: 18px 24px;
    background: rgba(201,168,76,0.05);
    margin-bottom: 24px;
  }

  .callout p {
    font-family: 'Share Tech Mono', monospace;
    font-size: 13px;
    color: #c9a84c;
    line-height: 1.6;
  }

  .use-cases {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 1px;
    background: #2a2215;
    border: 1px solid #2a2215;
    margin-top: 4px;
  }

  .use-case {
    background: #0e0c09;
    padding: 16px 18px;
    display: flex;
    align-items: flex-start;
    gap: 12px;
    transition: background 0.2s;
  }

  .use-case:hover { background: #150f07; }

  .use-case-icon { font-size: 16px; flex-shrink: 0; margin-top: 2px; }

  .use-case-text { font-size: 13px; color: #d4c4a0; line-height: 1.45; }

  .cap-list { display: grid; grid-template-columns: 1fr 1fr; gap: 2px; }

  @media (max-width: 540px) { .cap-list { grid-template-columns: 1fr; } }

  .cap-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 14px;
    background: #0e0c09;
    border: 1px solid #2a2215;
    font-size: 13px;
    color: #d4c4a0;
    transition: border-color 0.2s, color 0.2s, background 0.2s;
  }

  .cap-item:hover { border-color: #4a3c20; color: #ffffff; background: #150f07; }

  .cap-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #c9a84c;
    flex-shrink: 0;
    box-shadow: 0 0 6px rgba(201,168,76,0.6);
  }

  .flow {
    background: #0e0c09;
    border: 1px solid #2a2215;
    padding: 28px;
    font-family: 'Share Tech Mono', monospace;
    font-size: 12.5px;
    color: #d4c4a0;
    line-height: 1.9;
    position: relative;
  }

  .flow::before {
    content: 'CORE PRIMITIVE';
    position: absolute;
    top: 10px;
    right: 16px;
    font-size: 9px;
    letter-spacing: 0.16em;
    color: #4a3c20;
  }

  .flow-step { display: flex; gap: 14px; align-items: baseline; padding: 3px 0; }
  .flow-step:hover .flow-label { color: #c9a84c; }
  .flow-num { color: #4a3c20; width: 18px; text-align: right; flex-shrink: 0; font-size: 10px; }
  .flow-arrow { color: #e8d5a0; flex-shrink: 0; }
  .flow-label { color: #d4c4a0; }
  .flow-label strong { color: #ffffff; font-weight: 400; }
  .flow-label .node-a { color: #c9a84c; }
  .flow-label .node-b { color: #e8d5a0; }

  .disclaimer {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 16px 20px;
    background: rgba(201,168,76,0.04);
    border: 1px solid rgba(201,168,76,0.2);
    margin-top: 4px;
  }

  .disclaimer-icon { font-size: 18px; flex-shrink: 0; margin-top: 1px; }

  .disclaimer p { font-size: 12.5px; color: #6b5a38; line-height: 1.6; }
  .disclaimer strong { color: #e8d5a0; font-weight: 500; }

  footer {
    margin-top: 64px;
    padding-top: 24px;
    border-top: 1px solid #2a2215;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-family: 'Share Tech Mono', monospace;
    font-size: 10px;
    color: #4a3c20;
    letter-spacing: 0.1em;
    gap: 12px;
    flex-wrap: wrap;
  }

  footer span { text-transform: uppercase; }

  .pulse {
    display: inline-block;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #c9a84c;
    box-shadow: 0 0 0 0 rgba(201,168,76,0.5);
    animation: glow 2s infinite;
    vertical-align: middle;
    margin-right: 6px;
  }

  @keyframes glow {
    0%   { box-shadow: 0 0 0 0   rgba(201,168,76,0.5); }
    70%  { box-shadow: 0 0 0 8px rgba(201,168,76,0);   }
    100% { box-shadow: 0 0 0 0   rgba(201,168,76,0);   }
  }

  p { margin-bottom: 12px; }
  p:last-child { margin-bottom: 0; }
</style>
</head>
<body>
<div class="wrapper">

  <header>
    <div class="tag-line">Android Alpha · Experimental</div>
    <h1>POLLEN<span class="dash">-</span>OS</h1>
    <p class="subtitle">Offline-first mesh coordination for Android devices.</p>
    <div class="badges">
      <span class="badge active">Alpha</span>
      <span class="badge">Android</span>
      <span class="badge">Mesh</span>
      <span class="badge">Offline-First</span>
      <span class="badge">Research Prototype</span>
    </div>
  </header>

  <section>
    <h2>What it is</h2>
    <p>POLLEN-OS is a mobile mesh coordination layer for Android. It is designed for field testing, resilience research, and offline coordination experiments — turning nearby phones into local mesh nodes capable of discovering peers, exchanging task packets, recording results, and running an onboard AI decision layer to assess mesh health.</p>
    <div class="callout">
      <p>Ordinary phones should be able to coordinate locally when cloud,<br>Wi-Fi, or cellular infrastructure is unavailable.</p>
    </div>
  </section>

  <section>
    <h2>Target Use Cases</h2>
    <div class="use-cases">
      <div class="use-case"><span class="use-case-icon">🔍</span><span class="use-case-text">Search &amp; rescue coordination</span></div>
      <div class="use-case"><span class="use-case-icon">⛏</span><span class="use-case-text">Underground &amp; low-connectivity teams</span></div>
      <div class="use-case"><span class="use-case-icon">🌪</span><span class="use-case-text">Disaster response testing</span></div>
      <div class="use-case"><span class="use-case-icon">📡</span><span class="use-case-text">Rural &amp; off-grid communication experiments</span></div>
      <div class="use-case"><span class="use-case-icon">🏭</span><span class="use-case-text">Industrial field teams</span></div>
      <div class="use-case"><span class="use-case-icon">🔀</span><span class="use-case-text">Local device-to-device task routing</span></div>
      <div class="use-case"><span class="use-case-icon">🧠</span><span class="use-case-text">AI-assisted mesh health monitoring</span></div>
    </div>
  </section>

  <section>
    <h2>Alpha Capabilities</h2>
    <div class="cap-list">
      <div class="cap-item"><span class="cap-dot"></span>Android peer discovery</div>
      <div class="cap-item"><span class="cap-dot"></span>Nearby mesh status tracking</div>
      <div class="cap-item"><span class="cap-dot"></span>Task packet creation</div>
      <div class="cap-item"><span class="cap-dot"></span>Task result history</div>
      <div class="cap-item"><span class="cap-dot"></span>ACK-style result handling</div>
      <div class="cap-item"><span class="cap-dot"></span>Latency recording</div>
      <div class="cap-item"><span class="cap-dot"></span>Debug &amp; event logging</div>
      <div class="cap-item"><span class="cap-dot"></span>Tester log export</div>
      <div class="cap-item"><span class="cap-dot"></span>AI mesh health scoring</div>
      <div class="cap-item"><span class="cap-dot"></span>AI recommended action state</div>
      <div class="cap-item"><span class="cap-dot"></span>Field &amp; range probe testing</div>
      <div class="cap-item"><span class="cap-dot"></span>Task compatibility fallback</div>
      <div class="cap-item"><span class="cap-dot"></span>Sensitive task blocking for untrusted peers</div>
      <div class="cap-item"><span class="cap-dot"></span>Alpha dashboard UI</div>
    </div>
  </section>

  <section>
    <h2>Core Primitive</h2>
    <div class="flow">
      <div class="flow-step"><span class="flow-num">01</span><span class="flow-arrow">→</span><span class="flow-label"><strong class="node-a">Phone A</strong> detects <strong class="node-b">Phone B</strong></span></div>
      <div class="flow-step"><span class="flow-num">02</span><span class="flow-arrow">→</span><span class="flow-label"><strong class="node-a">Phone A</strong> creates a task packet</span></div>
      <div class="flow-step"><span class="flow-num">03</span><span class="flow-arrow">→</span><span class="flow-label"><strong class="node-a">Phone A</strong> sends the task through the local mesh</span></div>
      <div class="flow-step"><span class="flow-num">04</span><span class="flow-arrow">→</span><span class="flow-label"><strong class="node-b">Phone B</strong> receives the task</span></div>
      <div class="flow-step"><span class="flow-num">05</span><span class="flow-arrow">→</span><span class="flow-label"><strong class="node-b">Phone B</strong> executes &amp; responds</span></div>
      <div class="flow-step"><span class="flow-num">06</span><span class="flow-arrow">→</span><span class="flow-label"><strong class="node-a">Phone A</strong> receives ACK / result</span></div>
      <div class="flow-step"><span class="flow-num">07</span><span class="flow-arrow">→</span><span class="flow-label"><strong class="node-a">Phone A</strong> records latency, status, and result</span></div>
      <div class="flow-step"><span class="flow-num">08</span><span class="flow-arrow">→</span><span class="flow-label">AI layer evaluates mesh health</span></div>
    </div>
  </section>

  <section>
    <h2>Status</h2>
    <div class="disclaimer">
      <span class="disclaimer-icon">⚡</span>
      <p><strong>This is not an emergency replacement system.</strong> POLLEN-OS is an alpha research prototype focused on proving core primitives. It is under active development and not recommended for production or life-safety use.</p>
    </div>
  </section>

  <footer>
    <span><span class="pulse"></span>Active Alpha Development</span>
    <span>POLLEN-OS · Offline-First Mesh</span>
  </footer>

</div>
</body>
</html>
