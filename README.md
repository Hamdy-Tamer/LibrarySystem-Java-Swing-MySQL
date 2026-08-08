# 📚 Library Management System – Java + Swing + MySQL

![Java](https://img.shields.io/badge/Java-21-blue?style=flat&logo=java)
![Swing](https://img.shields.io/badge/GUI-Swing-orange?style=flat)
![MySQL](https://img.shields.io/badge/Database-MySQL-4479A1?style=flat&logo=mysql)
![JDBC](https://img.shields.io/badge/Driver-JDBC-4B8BBE?style=flat)
![Version](https://img.shields.io/badge/Version-2.0-brightgreen)

---

## 📖 Description

A complete **Library Management System** built with **Java Swing** for the user interface and **MySQL** for persistent data storage. The system features **role‑based dashboards** (Employee & User), **fine management** ($15/day for overdue books), **user/employee management**, and **real‑time borrowing history**.

> **Data is stored permanently** – no data loss after restart.  
> **Fines are calculated automatically** – $15 per day for overdue books.

---

## ✨ Features

### 👨‍💼 Employee Dashboard
- 📚 **Book Management** – Add, edit, remove books (single copies or entire titles).
- 🔍 **Search** – Filter books by title, category, or copy ID.
- 📋 **Tabbed Views** – All, Available, Borrowed.
- 🗑️ **Delete All Copies** – Remove every copy of a title at once.
- 👥 **User Management** – View all registered users (customers) with:
  - Number of books currently borrowed
  - Total fines (including live overdue calculations)
- 💲 **Settle Fines** – Collect fines at the desk and clear user balances.
- 📖 **View Borrowing History** – See complete history for any user with fine amounts.
- 👨‍💼 **Employee Management** – Add, view, and delete staff members.
- 📊 **Library Status** – Real‑time counts of total books, borrowed books, and per‑category totals.

### 👤 User Dashboard
- 📖 **Browse Books** – View all available books.
- 📥 **Borrow Books** – Borrow a copy (max 15 days).
- 📤 **Return Books** – Return books with automatic fine calculation.
- ⏰ **Overdue Detection** – Real‑time overdue status and fine display.
- 📜 **Borrowing History** – View personal borrowing history with fines.
- 🚪 **Logout** – Securely logout and return to login screen.

### 🔐 Authentication & Role Management
- **Registration** – New users can register (first name, last name, email, phone, password).
- **Login** – Secure login with email and password.
- **Role‑Based Access**:
  - **Employees** – Full management (books, users, employees, fines).
  - **Users** – Borrow, return, and view personal history.

### 💰 Fine System
- **$15 per day** late fee for overdue books.
- **Real‑time fine calculation** – displayed in user tables and history.
- **Fine settlement** – Employees can collect fines at the desk.
- **Automatic fine recording** – Fines are stored in both `borrowings` and `members` tables.

---

## 🛠️ Technologies Used

| Technology | Purpose |
|------------|---------|
| ☕ **Java 21** | Core language |
| 🖼️ **Swing** | GUI framework |
| 🗄️ **MySQL 8** | Relational database |
| 🔌 **JDBC** | Database connectivity |
| 📦 **Maven (optional)** | Dependency management |

---

## 🚀 Getting Started

### 1️⃣ Prerequisites

- **Java JDK 21** or higher installed.
- **MySQL Server 8** installed and running.
- **MySQL Workbench** (or any SQL client) to create the database.

---

### 2️⃣ Clone the Repository

```bash
git clone https://github.com/your-username/Library-Management-System-Java-MySQL.git
cd Library-Management-System-Java-MySQL
```

### 3️⃣ Create the Database

Open your MySQL client (e.g., MySQL Workbench) and run the following script to create the database and all required tables:

```sql
CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

-- Books table
CREATE TABLE books (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    borrowed BOOLEAN DEFAULT FALSE,
    borrowing_date DATE,
    borrowing_period INT,
    return_date DATE
);

-- Users table
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(15) NOT NULL,
    last_name VARCHAR(15) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(15) DEFAULT NULL UNIQUE,
    role ENUM('user', 'employee') DEFAULT 'user',
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Members (users with role='user')
CREATE TABLE members (
    member_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    member_code VARCHAR(20) NOT NULL UNIQUE,
    total_books_borrowed INT DEFAULT 0,
    current_fines DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Employees (users with role='employee')
CREATE TABLE employees (
    employee_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    position ENUM('librarian','assistant_librarian','library_assistant','technician','manager','director','admin') NOT NULL,
    hire_date DATE NOT NULL,
    salary DECIMAL(10,2) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Borrowings (borrowing history)
CREATE TABLE borrowings (
    borrowing_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    borrowing_date DATE NOT NULL,
    return_date DATE NOT NULL,
    actual_return_date DATE DEFAULT NULL,
    borrowing_period INT NOT NULL,
    status ENUM('borrowed', 'returned', 'overdue') DEFAULT 'borrowed',
    fine_amount DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- Insert default admin employee
INSERT INTO users (first_name, last_name, email, password, phone_number, role)
VALUES ('Admin', 'System', 'admin404@gmail.com', 'Admin#1234', '01000000000', 'employee');

INSERT INTO employees (user_id, employee_code, position, hire_date, salary)
VALUES (1, 'EMP001', 'admin', CURDATE(), 0.00);
```

### 4️⃣ Configure Database Credentials

Edit the file `src/db/DBConnection.java` and set your MySQL username and password:

```java
private static final String USER = "your_username";
private static final String PASSWORD = "your_password";
```

### 5️⃣ Add MySQL Connector/J

Download the latest **MySQL Connector/J** (JAR) from  
[MySQL official site](https://dev.mysql.com/downloads/connector/j/).

Add it to your project's classpath:
- **IntelliJ IDEA**: *File → Project Structure → Libraries → + → Java* and select the JAR.
- **Eclipse**: Right‑click project → *Build Path → Configure Build Path → Add External JARs*.
- **NetBeans**: Right‑click project → *Properties → Libraries → Add JAR/Folder*.

---

### 6️⃣ Run the Application

Open the project in your IDE, locate the **`Main.java`** file in the `src` folder, and run it – the GUI will appear.

> **Command line (after compilation):**
> ```bash
> javac -cp ".;lib/mysql-connector-j-9.7.0.jar" src/**/*.java Main.java
> java -cp ".;lib/mysql-connector-j-9.7.0.jar" Main
> ```
> *(Replace `lib/mysql-connector-j-9.7.0.jar` with the actual path to your JAR.)*
