# QueLess India

**QueLess India** is a Java-based desktop application designed to reduce long queues in places like **hospitals, banks, and government offices** by allowing users to digitally generate queue tokens.

Instead of standing in line physically, users can **book their position in the queue digitally** and receive a **token number with estimated waiting time**.

This project is built using **Java Swing for GUI and MySQL for database management**.

---

## Version

**Current Version:** v1.0
This is the **first version** of the project. More improvements and features will be added in future updates.

---

## Features

* User Signup and Login system
* Queue token generation
* Department selection (Hospital / Bank / Post Office / Other)
* Time slot selection
* Automatic token number generation
* Estimated waiting time calculation
* MySQL database integration
* Simple and user-friendly interface

---

## Tech Stack

**Frontend (GUI)**

* Java Swing
* Java AWT

**Backend**

* Java

**Database**

* MySQL

**Connectivity**

* JDBC (Java Database Connectivity)

---

## Project Structure

```
QueLessIndia.java
```

Main modules inside the program:

* Database Connection
* User Authentication (Login / Signup)
* Queue Booking System
* Token Generation Logic
* GUI Interface

---

## Database Schema

### Users Table

| Column   | Type              |
| -------- | ----------------- |
| user_id  | INT (Primary Key) |
| name     | VARCHAR           |
| email    | VARCHAR           |
| password | VARCHAR           |

### Queues Table

| Column       | Type              |
| ------------ | ----------------- |
| token_id     | INT (Primary Key) |
| user_id      | INT               |
| dept_type    | VARCHAR           |
| dept_name    | VARCHAR           |
| slot_time    | VARCHAR           |
| token_number | INT               |

---

## How It Works

1. User creates an account using **Signup**
2. User logs into the system
3. Selects department type (Hospital, Bank, etc.)
4. Enters department name
5. Selects a time slot
6. System generates a **queue token**
7. Estimated waiting time is displayed



Tables will automatically be created when the program runs.

### 3. Add MySQL Connector

Download MySQL JDBC driver and add it to your project classpath.


## Future Updates

Planned improvements:

* Admin dashboard
* Live queue tracking
* Token cancellation
* Notification system
* Better UI design
* Multi-branch support
* Web / Mobile integration

---

## Project Goal

The goal of this project is to **digitize queue management** and reduce unnecessary waiting time in crowded public places.

---

## Author

Atharva Chauhan
BTech CSE Student


⭐ More updates coming soon.
