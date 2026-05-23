# Duress Alarm Management System

A high-performance, resilient multi-module safety infrastructure engineered with Quarkus 3.35.3 and Java 25.

This project isolates cross-cutting telemetry data models from specific deployment contexts, orchestrating an AWS cloud orchestration layer and specialized background remote computer client drivers.

---

## 🏗️ System Architecture

The project is structured as a decoupled Maven Reactor multi-module ecosystem:

```text
safety-alert/
│
├── duress-common/      <- Pure data definitions, serialized DTO payloads, and YAML config structures.
├── duress-server/      <- Cloud orchestration engine featuring reactive WebSockets Next and PostgreSQL/SQLite.
└── duress-client/      <- Thin desktop client application with localized SQLite offline caching drivers.
```

---

## 🛠️ Global Validation Check

Before spinning up individual services, validate the integrity of cross-module injection schemas by running a baseline compile check from the system root directory:

```bash
./mvnw clean install -DskipTests
```

---

## 🚀 Running Applications in Development Mode

You can launch both applications concurrently using the explicit module positioning (`-pl`) flags. Open separate terminal windows or use your integrated IntelliJ IDEA terminal sessions:

### 1. Fire up the Cloud Core Server:
```bash
./mvnw -pl duress-server quarkus:dev
```
> **Note:** The server boots up with an **Agroal Connection Pool capped at 8 connections** (with a `busy_timeout=5000ms` file buffer) and handles underlying table generation routines safely via the `drop-and-create` strategy. Access its Dev UI dashboard at: `http://localhost:8080/q/dev/`.

### 2. Fire up the Remote Client Service:
```bash
./mvnw -pl duress-client quarkus:dev
```
> **Note:** Access the Client's isolated Dev UI tool wrapper at: `http://localhost:8081/q/dev/`.

### 🧪 Continuous Test Execution
While running the server in dev mode, press **`r`** inside your console panel to execute the entire server test suite. Continuous background scanning will track your code adjustments inside IntelliJ in real time.

---

## 📦 Packaging and Deployment Pipelines

Because these modules are deployed onto entirely separate target infrastructures, package them independently from the root repository directory:

### A. AWS Server Production Build
Compile your core cloud service artifact using the `-am` (also make) flag to guarantee that common serialization libraries are automatically packed into the distribution archive:

```bash
./mvnw package -pl duress-server -am
```
* **Output Runner:** `duress-server/target/quarkus-app/quarkus-run.jar`
* **Execution Command:** `java -jar duress-server/target/quarkus-app/quarkus-run.jar`

### B. Remote Desktop Native Executable
Compile a memory-dense, standalone native executable tailored for host operating systems without requiring a local Java Runtime Environment (JRE):

```bash
./mvnw package -pl duress-client -am -Pnative
```
* **Output Runner:** `duress-client/target/duress-client-1.0.0-SNAPSHOT-runner`

If you do not have GraalVM installed locally, leverage standard container virtualization engines to build the native Linux/Unix binary:

```bash
./mvnw package -pl duress-client -am -Pnative -Dquarkus.native.container-build=true
```

---

## 📑 Strategic Extensions Used

* **WebSockets Next ([Guide](https://quarkus.io/guides/websockets-next-reference)):** Ultra-low latency, asynchronous endpoint architecture facilitating real-time duress alerts.
* **REST Jackson ([Guide](https://quarkus.io/guides/rest#json-serialisation)):** High-performance JSON serialization processing framework with reflection-free capabilities.
* **Hibernate ORM with Panache ([Guide](https://quarkus.io/guides/hibernate-orm-panache)):** Streamlined Active Record layer for rapid object persistence.
* **Quarkus SQLite JDBC Driver ([Guide](https://quarkus.io/guides/datasource)):** Lightweight, zero-admin database filesystem integration for localized caching and log histories.
