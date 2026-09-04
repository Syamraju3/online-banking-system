import java.sql.*;
import java.util.Scanner;

public class OnlineBankingSystem {
    // Database credentials (adjust as needed for your local MySQL)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/banking_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password";

    private static Connection connection;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            // Establishing Database Connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("========================================");
            System.out.println("   WELCOME TO ONLINE BANKING SYSTEM     ");
            System.out.println("========================================");

            boolean exit = false;
            while (!exit) {
                System.out.println("\n1. Create New Account");
                System.out.println("2. Deposit Money");
                System.out.println("3. Withdraw Money");
                System.out.println("4. Transfer Funds");
                System.out.println("5. Check Balance");
                System.out.println("6. View Transaction History");
                System.out.println("7. Exit");
                System.out.print("Enter your choice: ");

                int choice = scanner.nextInt();
                switch (choice) {
                    case 1: createAccount(); break;
                    case 2: depositMoney(); break;
                    case 3: withdrawMoney(); break;
                    case 4: transferFunds(); break;
                    case 5: checkBalance(); break;
                    case 6: viewTransactionHistory(); break;
                    case 7:
                        exit = true;
                        System.out.println("\nThank you for banking with us. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 1. Create Account
    private static void createAccount() throws SQLException {
        scanner.nextLine(); // Clear newline
        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine();
        System.out.print("Set 4-Digit Security PIN: ");
        int pin = scanner.nextInt();
        System.out.print("Enter Initial Deposit Amount: ");
        double initialDeposit = scanner.nextDouble();

        String query = "INSERT INTO accounts (account_holder_name, pin, balance) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setInt(2, pin);
            stmt.setDouble(3, initialDeposit);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int accNo = rs.getInt(1);
                System.out.println("\nAccount successfully created! Your Account Number is: " + accNo);
                recordTransaction(accNo, "INITIAL_DEPOSIT", initialDeposit, initialDeposit);
            }
        }
    }

    // 2. Deposit Money
    private static void depositMoney() throws SQLException {
        System.out.print("Enter Account Number: ");
        int accNo = scanner.nextInt();
        System.out.print("Enter Deposit Amount: ");
        double amount = scanner.nextDouble();

        if (amount <= 0) {
            System.out.println("Amount must be greater than 0.");
            return;
        }

        String query = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, accNo);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                double newBalance = getBalance(accNo);
                recordTransaction(accNo, "DEPOSIT", amount, newBalance);
                System.out.println("Deposit successful! New Balance: Rs. " + newBalance);
            } else {
                System.out.println("Account not found.");
            }
        }
    }

    // 3. Withdraw Money
    private static void withdrawMoney() throws SQLException {
        System.out.print("Enter Account Number: ");
        int accNo = scanner.nextInt();
        System.out.print("Enter Security PIN: ");
        int pin = scanner.nextInt();
        System.out.print("Enter Withdrawal Amount: ");
        double amount = scanner.nextDouble();

        if (!validatePin(accNo, pin)) {
            System.out.println("Invalid PIN or Account Number.");
            return;
        }

        double currentBalance = getBalance(accNo);
        if (currentBalance < amount) {
            System.out.println("Insufficient funds! Current balance: Rs. " + currentBalance);
            return;
        }

        String query = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, accNo);
            stmt.executeUpdate();

            double newBalance = currentBalance - amount;
            recordTransaction(accNo, "WITHDRAWAL", amount, newBalance);
            System.out.println("Withdrawal successful! Remaining Balance: Rs. " + newBalance);
        }
    }

    // 4. Fund Transfer (Atomic Transaction)
    private static void transferFunds() throws SQLException {
        System.out.print("Enter Your Account Number: ");
        int senderAcc = scanner.nextInt();
        System.out.print("Enter Security PIN: ");
        int pin = scanner.nextInt();

        if (!validatePin(senderAcc, pin)) {
            System.out.println("Authentication failed. Invalid Account or PIN.");
            return;
        }

        System.out.print("Enter Beneficiary Account Number: ");
        int receiverAcc = scanner.nextInt();
        System.out.print("Enter Amount to Transfer: ");
        double amount = scanner.nextDouble();

        double senderBalance = getBalance(senderAcc);
        if (senderBalance < amount) {
            System.out.println("Transfer failed: Insufficient balance.");
            return;
        }

        // Begin Transaction
        connection.setAutoCommit(false);
        try {
            // Deduct from sender
            String deductQuery = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deductQuery)) {
                stmt.setDouble(1, amount);
                stmt.setInt(2, senderAcc);
                stmt.executeUpdate();
            }

            // Credit to receiver
            String creditQuery = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
            try (PreparedStatement stmt = connection.prepareStatement(creditQuery)) {
                stmt.setDouble(1, amount);
                stmt.setInt(2, receiverAcc);
                stmt.executeUpdate();
            }

            connection.commit(); // Commit transaction

            recordTransaction(senderAcc, "TRANSFER_SENT", amount, senderBalance - amount);
            recordTransaction(receiverAcc, "TRANSFER_RECEIVED", amount, getBalance(receiverAcc));

            System.out.println("Transfer of Rs. " + amount + " to Account " + receiverAcc + " was successful!");
        } catch (SQLException e) {
            connection.rollback(); // Rollback in case of error
            System.out.println("Transfer failed. Transaction rolled back.");
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // 5. Check Balance
    private static void checkBalance() throws SQLException {
        System.out.print("Enter Account Number: ");
        int accNo = scanner.nextInt();
        System.out.print("Enter PIN: ");
        int pin = scanner.nextInt();

        if (validatePin(accNo, pin)) {
            System.out.println("Current Balance: Rs. " + getBalance(accNo));
        } else {
            System.out.println("Invalid Account or PIN.");
        }
    }

    // 6. View Transaction History
    private static void viewTransactionHistory() throws SQLException {
        System.out.print("Enter Account Number: ");
        int accNo = scanner.nextInt();
        System.out.print("Enter PIN: ");
        int pin = scanner.nextInt();

        if (!validatePin(accNo, pin)) {
            System.out.println("Authentication failed.");
            return;
        }

        String query = "SELECT transaction_type, amount, balance_after, transaction_date FROM transactions WHERE account_number = ? ORDER BY transaction_date DESC";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accNo);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n--- TRANSACTION HISTORY FOR ACCOUNT #" + accNo + " ---");
            System.out.printf("%-18s | %-10s | %-14s | %-20s\n", "Type", "Amount", "Balance After", "Date");
            System.out.println("----------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-18s | Rs. %-7.2f | Rs. %-11.2f | %-20s\n",
                        rs.getString("transaction_type"),
                        rs.getDouble("amount"),
                        rs.getDouble("balance_after"),
                        rs.getTimestamp("transaction_date").toString());
            }
        }
    }

    // Helper: Validate PIN
    private static boolean validatePin(int accNo, int pin) throws SQLException {
        String query = "SELECT pin FROM accounts WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accNo);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("pin") == pin;
        }
    }

    // Helper: Get Balance
    private static double getBalance(int accNo) throws SQLException {
        String query = "SELECT balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accNo);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getDouble("balance") : 0.0;
        }
    }

    // Helper: Record Audit Transaction
    private static void recordTransaction(int accNo, String type, double amount, double balanceAfter) throws SQLException {
        String query = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, accNo);
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.setDouble(4, balanceAfter);
            stmt.executeUpdate();
        }
    }
}
