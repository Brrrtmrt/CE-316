# Integrated Assignment Environment (IAE)

## Overview
The Integrated Assignment Environment (IAE) is a lightweight, standalone desktop application built with JavaFX and Java 17. It is designed to help instructors streamline the management, compilation, and evaluation of programming assignments. By automating the extraction, execution, and comparison of student code against expected outputs, IAE drastically reduces manual grading overhead.

## Key Features
* **Automated Batch Processing:** Automatically extracts and processes student submissions from `.zip` archives.
* **Language Agnostic:** Supports both compiled (C, C++, Java) and interpreted (Python, JavaScript) languages through customizable configurations.
* **Secure Execution:** Protects against malicious or poorly written student code (e.g., infinite loops, memory exhaustion) using strict timeouts and stdout truncation.
* **Multiple Comparison Strategies:** Evaluates student outputs using strategies like *Exact Match*, *Ignore Whitespace*, and *Trim Lines*.
* **Persistent State:** Uses a local SQLite database (`iae.db`) to save projects, configurations, and evaluation results for future review.
* **Import/Export Configurations:** Share and back up assignment environments using JSON files.
* **Standalone Windows Installer:** Generates a native `.exe` installer bundled with the required JRE via `jpackage`, removing the need for end-users to pre-install Java.

## Prerequisites
* **Java Development Kit (JDK):** Version 17 or higher.
* **Apache Maven:** For dependency management and building.
* *(Optional)* **WiX Toolset:** Required on Windows if you intend to build the native `.exe` installer using the `jpackage` tool.

## Build & Run Instructions

### 1. Running in Development Mode
To compile and launch the application directly from the source code, use the JavaFX Maven plugin:
```bash
mvn clean compile javafx:run