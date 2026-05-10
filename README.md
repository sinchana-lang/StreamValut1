---

## 🗄️ Database Schema

```sql
CREATE TABLE IF NOT EXISTS users (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(100) NOT NULL UNIQUE,
    phone    VARCHAR(20),
    age      VARCHAR(10),
    password VARCHAR(100) NOT NULL
);
```

---

## 📡 API Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/` | GET | Serves frontend index.html |
| `/api/register` | POST | Register new user |
| `/api/login` | POST | Login existing user |
| `/api/videos` | GET | List all videos as JSON |
| `/stream/{id}` | GET | Stream video with Range support |
| `/upload` | POST | Upload video file |

---

## 📋 Requirements

- JDK 11 or above
- MySQL 8.0
- MySQL Connector/J JAR file

---

## 👩‍💻 Author

**Sinchana J**
1GA24CI101
Dept. of CSE (AI & ML)
Global Academy of Technology, Bengaluru

---

## 📄 License

This project is licensed under the MIT License.
