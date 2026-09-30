const http = require("http");
const fs = require("fs");
const path = require("path");

const PORT = process.env.PORT || 3000;

const server = http.createServer((req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    res.writeHead(204);
    return res.end();
  }

  if (req.method === "POST" && req.url === "/api/internal/dispatch") {
    let body = "";
    req.on("data", chunk => (body += chunk));
    req.on("end", () => {
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(
        JSON.stringify({
          ok: true,
          status: "DISPATCHED",
          event: "zaldi_ping",
          matched_drivers: 3,
          latency_ms: 8,
          timestamp: new Date().toISOString()
        })
      );
    });
    return;
  }

  const filePath = path.join(__dirname, "index.html");
  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(500);
      return res.end("Error loading index.html");
    }
    res.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
    res.end(content);
  });
});

server.listen(PORT, () => {
  console.log(`\n=============================================================`);
  console.log(`🚀 ZALDI 3-APP SUITE RUNNING IN VS CODE: http://localhost:${PORT}`);
  console.log(`   1. Customer App  •  2. Driver App  •  3. Admin App`);
  console.log(`=============================================================\n`);
});
