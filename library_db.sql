-- 1. Create and select the database
CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

-- 2. Books Table
CREATE TABLE books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,  
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    borrowed BOOLEAN DEFAULT FALSE,
    borrowing_date DATE,
    borrowing_period INT,
    return_date DATE
);

-- 3. Users Table (Master table for all authentication)
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(15) NOT NULL,
    last_name VARCHAR(15) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(15) DEFAULT NULL,
    role ENUM('user', 'employee') DEFAULT 'user',
    registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Members Table (Only for role = 'user')
CREATE TABLE members (
    member_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,  
    member_code VARCHAR(20) NOT NULL UNIQUE,
    total_books_borrowed INT DEFAULT 0,
    current_fines DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 5. Employees Table (Only for role = 'employee')
CREATE TABLE employees (
    employee_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    position ENUM('librarian','library_assistant','technician','manager','admin') NOT NULL,
    hire_date DATE NOT NULL,
    salary DECIMAL(10,2) DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 6. Fines Table (Linked to the member)
CREATE TABLE fines (
    fine_id INT PRIMARY KEY AUTO_INCREMENT,
    member_id INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    fine_date DATE NOT NULL,
    reason ENUM('overdue','damage','lost') DEFAULT 'overdue',
    status ENUM('pending','paid','waived') DEFAULT 'pending',
    paid_date DATE DEFAULT NULL,
    days_overdue INT DEFAULT 0,
    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
);

-- 7. Borrowings Table (Tracks borrowed books)
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
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE  
);

-- First employee to add
INSERT INTO users (first_name, last_name, email, password, phone_number, role)
VALUES ('John', 'Doe', 'johndoe707@gmail.com', '0a19533d8eae0719d0e75b3cfb2d80808111b7612756418145cc7103e621f352', '01008756488', 'employee');
-- password = temp123

INSERT INTO employees (user_id, employee_code, position, hire_date, salary)
VALUES (1, 'EMP001', 'admin', CURDATE(), 50000.00);
