# 🏦 Online Banking System (Core Java & MySQL)

A secure, console-based banking management application built with **Core Java**, **JDBC**, and **MySQL**.

## 📌 Features
- **Account Management:** User registration, PIN validation, and balance inquiries.
- **Transactions:** Deposits, withdrawals, and secure atomic fund transfers using SQL transactions (commit/rollback).
- **Audit Logging:** Full transaction history tracking with foreign key relational integrity.
- **Security:** PIN verification and SQL injection prevention using `PreparedStatement`.

## 🛠️ Technologies
- **Language:** Core Java (JDK 17)
- **Database:** MySQL
- **Connectivity:** JDBC (Java Database Connectivity)
- **Concepts:** Object-Oriented Programming (OOP), Exception Handling, ACID Transactions

## 🚀 How to Run
1. Run `schema.sql` in MySQL Workbench or terminal to set up tables.
2. Update the DB credentials in `OnlineBankingSystem.java`.
3. Compile and run:
   ```bash
   javac OnlineBankingSystem.java
   java OnlineBankingSystem
