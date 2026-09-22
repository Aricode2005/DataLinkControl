<div align="center">
  <h1>🌐 Data Link Layer Flow Control Simulator</h1>
  <p><i>A full-stack, multithreaded ARQ Protocol Simulator visualizing Stop-and-Wait, Go-Back-N, and Selective Repeat algorithms.</i></p>
  
  [![Live Demo](https://img.shields.io/badge/Live_Demo-Render-46E3B7?style=for-the-badge&logo=render)](https://datalink-simulator.onrender.com/)
  [![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://java.com)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io)
  [![WebSockets](https://img.shields.io/badge/WebSockets-010101?style=for-the-badge&logo=socket.io&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)
</div>

<br/>

## 🚀 Live Demo
Experience the simulation in real-time straight from your browser:
**👉 [https://datalink-simulator.onrender.com/](https://datalink-simulator.onrender.com/)**

*Open the link on two different devices (or browser tabs) to act as the Sender and Receiver simultaneously!*

---

## 🛠️ Tech Stack
* **Backend Core:** Pure `Java 17` (Multithreading, DatagramSockets, OOP Design Patterns)
* **Web Server:** `Spring Boot 3` & `Maven`
* **Real-time Communication:** `Spring WebSockets`
* **Frontend UI:** Vanilla `HTML5`, `CSS3` (Dark-Mode SaaS aesthetic), and `JavaScript`
* **Cloud Deployment:** `Docker` & `Render.com`

---

## ✨ Features
* **Three Flow Control Protocols:** Fully implemented Stop-and-Wait (SAW), Go-Back-N (GBN), and Selective Repeat (SR).
* **Live Network Simulator:** Injects exact probabilities of Packet Loss (via Extreme Latency) and Packet Corruption (via Single-Bit Error injection) on the fly.
* **Real-time Dashboard:** Watch the mathematical window pointers (`base`, `expectedSeqNo`) slide in real-time via the WebSocket-powered hacker terminal.
* **Document Reassembly:** Upload a `.txt` file on the Sender side, and watch the Receiver flawlessly reconstruct the document at the end of the simulation!
* **Deep Telemetry:** Calculates theoretical Efficiency (%) and Average RTT (ms) based on real network transit times.

---

## 🌐 How to Run (Web Dashboard Mode)

This project is deployed to the cloud via Docker. If you want to run the web server locally:

1. **Compile & Run with Maven:**
   ```bash
   ./mvnw spring-boot:run
   ```
2. Open `localhost:8080` in two separate tabs.
3. In Tab 1, click **Receiver Mode** and generate a Session Code.
4. In Tab 2, click **Sender Mode**, input the Session Code, and hit "Start Transmission"!

---

## 💻 How to Run (Classic CLI Mode)

If you prefer the original, raw UDP Terminal experience without the Spring Boot web server:

### 1. Compile the Core Project (Excluding Web Server)
```powershell
javac -d target (Get-ChildItem -Recurse -File src\main\java\*.java | Where-Object { $_.DirectoryName -notmatch "web" }).FullName
```

### 2. Interactive Visual Simulation
Open **two separate terminals**.
```bash
# Terminal 1 (Receiver):
java -cp target com.network.Main receiver SR

# Terminal 2 (Sender):
java -cp target com.network.Main sender SR
```

---

## 📊 Automated Analytics

The project includes two CLI test runner scripts that rapidly simulate tens of thousands of packets in the background to generate empirical data reports.

### 1. Comparative Analysis (Efficiency vs. Error/Delay)
```bash
java -cp target com.network.TestRunner
```
* **Output:** Generates a highly detailed `comparative_analysis.txt` file testing Cases 2, 3, and 4 automatically.

### 2. Window Size Analysis
```bash
java -cp target com.network.WindowSizeAnalyzer
```
* **Output:** Generates a `window_size_metrics.csv` spreadsheet for graphing, proving protocol failures when $N \ge 2^m$.

---

## 🧩 Design Patterns Used
1. **Template Method Pattern**: The abstract `Sender` defines the backbone (timers, framing, UDP listening loop), delegating exact `Recv` logic to subclasses.
2. **Strategy Pattern**: The simulation can easily switch ARQ models seamlessly via polymorphism.
3. **Observer/Listener Pattern**: The WebSocket Logger and UDP Socket Listeners observe incoming traffic and dispatch it asynchronously.
4. **Proxy Pattern**: The `NetworkSimulator` proxies all outgoing packets, intentionally corrupting them without the `Sender`'s direct knowledge.