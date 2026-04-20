# QuelessIndia ⚡

Skip the queue. Save your time.

## Overview

QuelessIndia is a web-based queue management system that allows users to book tokens online and track their position in real time. It helps reduce waiting time and overcrowding in places like hospitals, banks, and service centers.

## Features

* User login and registration
* Token booking system
* Real-time queue tracking
* Smart reporting time
* Admin panel for managing users and tokens

## Tech Stack

* Frontend: HTML, CSS, JavaScript
* Backend: Java (Server)
* Database: MySQL

## Folder Structure

```
QuelessIndia/
├── Server.java
├── mysql-connector-j-9.6.0.jar
└── web/
    ├── index.html
    ├── dashboard.html
    ├── admin.html
    └── style.css
```

## How to Run

1. Clone the repository
2. Place the MySQL connector JAR in the root folder
3. Start your MySQL server
4. Run the backend:

   ```
   java -cp ".;mysql-connector-j-9.6.0.jar" Server
   ```
5. Open `web/index.html` in your browser

## Admin Login

```
username: admin  
password: admin123
```

## Future Improvements

* Mobile application
* SMS/Email notifications
* AI-based queue prediction

---

Built to solve real-world waiting problems 🚀
