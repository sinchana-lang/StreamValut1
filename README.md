# 🎬 StreamVault1 — Video Streaming Server

![Java](https://img.shields.io/badge/Java-11%2B-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License](https://img.shields.io/badge/License-MIT-green)

🚀 A lightweight, full-featured Video Streaming Server built entirely 
in Java — no external frameworks required. Users can register, login, 
upload and stream videos directly from any browser — just like a mini YouTube!

---

## ✨ Features

- ✅ User Registration and Login with MySQL database
- ✅ Video Upload — supports MP4, MKV, WEBM, AVI, MOV, M4V
- ✅ HTTP Range Request streaming for smooth seek and playback
- ✅ Handles files up to 4 GB
- ✅ 10 concurrent users via thread pool
- ✅ HTML/CSS/JavaScript frontend served from Java
- ✅ CORS support for browser API calls

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| ☕ Java 11+ | Main server and backend logic |
| 🌐 com.sun.net.httpserver | Built-in HTTP server |
| 🗄️ MySQL 8.0 | Permanent user data storage |
| 🔌 MySQL Connector/J 9.7.0 | JDBC driver for MySQL |
| 🎨 HTML / CSS / JavaScript | Frontend web interface |

---

## 📁 Project Structure

    StreamVault1/
    ├── 🚀 VideoStreamingServer.java   (Entry point)
    ├── 🔐 AuthHandler.java            (Register & Login)
    ├── 🗄️ DatabaseManager.java        (MySQL connection)
    ├── 📋 VideoListHandler.java       (Video list API)
    ├── 🎥 VideoStreamHandler.java     (HTTP streaming)
    ├── 📤 VideoUploadHandler.java     (File upload)
    ├── 🌐 StaticFileHandler.java      (Frontend server)
    ├── 🎨 index.html                  (Web interface)
    ├── 🔌 mysql-connector-j-9.7.0.jar (MySQL driver)
    └── 📂 videos/                     (Uploaded videos)

---

## ⚙️ How to Run

🔹 Step 1 — Clone the repository

    git clone https://github.com/sinchana-lang/StreamValut1.git
    cd StreamValut1

🔹 Step 2 — Setup MySQL

    CREATE DATABASE IF NOT EXISTS streamvault;

🔹 Step 3 — Compile

    javac -cp ".;mysql-connector-j-9.7.0.jar" *.java

🔹 Step 4 — Run

    java -cp ".;mysql-connector-j-9.7.0.jar" VideoStreamingServer

🔹 Step 5 — Open browser

    http://localhost:9090

---

## 🗄️ Database Schema

    CREATE TABLE IF NOT EXISTS users (
        id       INT PRIMARY KEY AUTO_INCREMENT,
        name     VARCHAR(100) NOT NULL,
        email    VARCHAR(100) NOT NULL UNIQUE,
        phone    VARCHAR(20),
        age      VARCHAR(10),
        password VARCHAR(100) NOT NULL
    );

---

## 📡 API Endpoints

| Endpoint | Method | Description |
|---|---|---|
| 🏠 / | GET | Serves frontend index.html |
| 📝 /api/register | POST | Register new user |
| 🔐 /api/login | POST | Login existing user |
| 🎬 /api/videos | GET | List all videos as JSON |
| 📺 /stream/{id} | GET | Stream video with Range support |
| 📤 /upload | POST | Upload video file |

---

## 📋 Requirements

- ☕ JDK 11 or above
- 🗄️ MySQL 8.0
- 🔌 MySQL Connector/J JAR file

---

## 👩‍💻 Author

**Sinchana J**
🎓 1GA24CI101
🏫 Dept. of CSE (AI & ML)
🏛️ Global Academy of Technology, Bengaluru

---

## 📄 License

This project is licensed under the MIT License.

---

⭐ If you found this project helpful, please give it a star!
