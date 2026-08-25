# Data Link Flow Control Simulation (Java)

This project simulates the Data Link Layer flow control mechanisms inside a simulated network environment. It implements **Stop and Wait**, **Go-Back-N ARQ**, and **Selective Repeat ARQ** protocols using Java DatagramSockets (UDP). 

It is designed with low-level design patterns to ensure extensibility, maintainability, and clean architecture.

## 🚀 How to Run

1. Open your terminal and navigate to the project directory:
   ```bash
   cd C:\Users\aritr\.gemini\antigravity\scratch\data_link_flow_control
   ```
2. Compile all Java files:
   ```powershell
   javac (Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
   ```
3. Run the Main simulation:
   ```bash
   java -cp src/main/java com.network.Main
   ```

## 🏗️ Project Architecture

The project has been separated into cohesive packages adhering to standard Java conventions:

### `com.network.model`
- **`Frame.java`**: Represents a Data Frame conforming to the assignment structure (Header, Payload, FCS trailer). Uses encapsulation to store source MAC, destination MAC, length, and sequence numbers.
- **`Ack.java`**: Represents Acknowledgement frames (ACK/NAK) and their checksums.

### `com.network.sender` and `com.network.receiver`
- **`Sender.java`** / **`Receiver.java`**: Abstract base classes that provide the standard `Framing()`, `Channel()`, `Timer()`, and socket binding logic.
- **Template Method Pattern**: The base classes define standard flows and delegate protocol-specific behaviors to abstract `Send()` and `Recv()` methods that subclasses implement.

### `com.network.protocol`
Implements the Strategy / State behavior for specific Flow Control schemes.
- **`StopAndWaitSender` / `StopAndWaitReceiver`**: Sends one frame at a time and waits for an explicit ACK.
- **`GoBackNSender` / `GoBackNReceiver`**: Uses a sliding window (size `N`) and cumulative acknowledgements. Retransmits the entire window if a timeout occurs.
- **`SelectiveRepeatSender` / `SelectiveRepeatReceiver`**: Uses independent ACKs/NAKs and a sliding window on both sides, retransmitting only selectively lost frames.

### `com.network.util`
- **`CRCUtils.java`**: Provides static utility methods for computing and verifying standard CRC32 checksums (FCS) for data integrity.
- **`NetworkSimulator.java`**: Acts as a **Proxy** over the DatagramSockets. It intentionally intercepts outgoing traffic to simulate network unreliability (delay, packet loss, and bit corruption).

## 💡 Design Patterns Used

1. **Template Method Pattern**: The abstract `Sender` defines the backbone (timers, framing, UDP listening loop), but delegates exact handling of `Recv` and `Send` logic to subclasses.
2. **Strategy Pattern**: The simulation can easily switch out ARQ models seamlessly via polymorphism (`RunSimulation(Sender, Receiver)`).
3. **Observer/Listener Pattern**: Multithreading is heavily used for the socket `startListening()` loops, which observe incoming UDP packets and dispatch them to the correct handler asynchronously.
4. **Proxy Pattern**: The `NetworkSimulator` proxies all outgoing packets, adding delays or intentionally corrupting them without the `Sender`'s direct knowledge, cleanly separating logic from simulation noise.
