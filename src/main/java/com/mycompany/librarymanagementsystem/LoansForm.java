// LoansForm.java
package com.mycompany.librarymanagementsystem;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Calendar;

public class LoansForm extends javax.swing.JFrame {

    private Connection conn;
    private PreparedStatement pst;
    private ResultSet rs;
    private DefaultTableModel dtm;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public LoansForm() {
        initComponents();
        connect();
        if (conn != null) {
            loadLoansData();
        } else {
             JOptionPane.showMessageDialog(this, "Database Connection Failed. Cannot load data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        // Clear placeholder text if needed
        // txtLoanID.setText("");
        // txtMemberID.setText("");
        // txtBookID.setText("");
        // txtLoanDate.setText("");
        // txtDueDate.setText("");
        // txtReturnDate.setText("");

        // Set Loan Date to current date by default for new loans
        txtLoanDate.setText(dateFormat.format(new java.util.Date()));
        // Calculate default Due Date (e.g., 14 days from today)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 14);
        txtDueDate.setText(dateFormat.format(cal.getTime()));

        // Loan ID is likely auto-increment, disable for Add but enable for Search/Return
        // txtLoanID.setEnabled(false); // Let's keep it enabled like the example for Search/Return
    }

    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/librarymanagementsystem", // Ensure DB name matches SQL file
                "root",
                ""); // Use your DB password if set
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            conn = null; // Ensure conn is null if connection fails
        }
    }

    private void loadLoansData() {
        try {
            // Join tables to get meaningful names/titles if desired, or just show IDs
            // Let's show IDs to keep it simple like the example
            pst = conn.prepareStatement("SELECT * FROM loans ORDER BY LoanID");
            rs = pst.executeQuery();

            dtm = (DefaultTableModel) tblLoans.getModel();
            dtm.setRowCount(0); // Clear existing rows

            // Ensure columns are set
            if (dtm.getColumnCount() == 0) {
                dtm.addColumn("LoanID");
                dtm.addColumn("MemberID");
                dtm.addColumn("BookID");
                dtm.addColumn("Loan Date");
                dtm.addColumn("Due Date");
                dtm.addColumn("Return Date");
            }

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("LoanID"));
                row.add(rs.getInt("MemberID"));
                row.add(rs.getInt("BookID"));
                Date loanDateSql = rs.getDate("LoanDate");
                row.add(loanDateSql != null ? dateFormat.format(loanDateSql) : "");
                Date dueDateSql = rs.getDate("DueDate");
                row.add(dueDateSql != null ? dateFormat.format(dueDateSql) : "");
                Date returnDateSql = rs.getDate("ReturnDate");
                row.add(returnDateSql != null ? dateFormat.format(returnDateSql) : "");
                dtm.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading loans data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void clearForm() {
        txtLoanID.setText("");
        txtMemberID.setText("");
        txtBookID.setText("");
        // Reset dates to default for a new loan
        txtLoanDate.setText(dateFormat.format(new java.util.Date()));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 14);
        txtDueDate.setText(dateFormat.format(cal.getTime()));
        txtReturnDate.setText(""); // Clear return date
        tblLoans.clearSelection();
    }

    // Issue Loan (Add)
    private void issueLoan() {
        String memberIDStr = txtMemberID.getText();
        String bookIDStr = txtBookID.getText();
        String loanDateStr = txtLoanDate.getText();
        String dueDateStr = txtDueDate.getText();

        if (memberIDStr.isEmpty() || bookIDStr.isEmpty() || loanDateStr.isEmpty() || dueDateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Member ID, Book ID, Loan Date, and Due Date are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.sql.Date loanDate = null;
        java.sql.Date dueDate = null;
        try {
            loanDate = new java.sql.Date(dateFormat.parse(loanDateStr).getTime());
            dueDate = new java.sql.Date(dateFormat.parse(dueDateStr).getTime());
        } catch (ParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection transactionConn = null;
        try {
            int memberID = Integer.parseInt(memberIDStr);
            int bookID = Integer.parseInt(bookIDStr);

            // Use transaction for atomicity
            transactionConn = conn; // Use the existing connection
            transactionConn.setAutoCommit(false);

            // 1. Check Book Availability & Member Existence (Important!)
            boolean bookAvailable = false;
            boolean memberExists = false;

            // Check member
            pst = transactionConn.prepareStatement("SELECT MemberID FROM members WHERE MemberID = ?");
            pst.setInt(1, memberID);
            rs = pst.executeQuery();
            memberExists = rs.next();
            rs.close();
            pst.close();

            if (!memberExists) {
                JOptionPane.showMessageDialog(this, "Member ID does not exist.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                transactionConn.rollback();
                return;
            }

            // Check book and lock row
            pst = transactionConn.prepareStatement("SELECT CopiesAvailable FROM books WHERE BookID = ? FOR UPDATE");
            pst.setInt(1, bookID);
            rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getInt("CopiesAvailable") > 0) {
                    bookAvailable = true;
                }
            } else {
                 JOptionPane.showMessageDialog(this, "Book ID does not exist.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                 transactionConn.rollback();
                 return;
            }
            rs.close();
            pst.close();

            if (!bookAvailable) {
                JOptionPane.showMessageDialog(this, "Book is not available (no copies left).", "Availability Error", JOptionPane.WARNING_MESSAGE);
                transactionConn.rollback();
                return;
            }

            // 2. Insert Loan (LoanID is auto-increment, so we don't insert it)
            pst = transactionConn.prepareStatement("INSERT INTO loans (MemberID, BookID, LoanDate, DueDate) VALUES (?, ?, ?, ?)");
            pst.setInt(1, memberID);
            pst.setInt(2, bookID);
            pst.setDate(3, loanDate);
            pst.setDate(4, dueDate);
            int loanResult = pst.executeUpdate();
            pst.close();

            // 3. Decrement Book Copies
            pst = transactionConn.prepareStatement("UPDATE books SET CopiesAvailable = CopiesAvailable - 1 WHERE BookID = ?");
            pst.setInt(1, bookID);
            int bookUpdateResult = pst.executeUpdate();
            pst.close();

            if (loanResult > 0 && bookUpdateResult > 0) {
                transactionConn.commit();
                JOptionPane.showMessageDialog(this, "Loan issued successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadLoansData(); // Refresh table
                clearForm();
            } else {
                transactionConn.rollback();
                JOptionPane.showMessageDialog(this, "Failed to issue loan. Transaction rolled back.", "Transaction Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Member ID or Book ID format. Must be numbers.", "Input Error", JOptionPane.WARNING_MESSAGE);
            try { if (transactionConn != null) transactionConn.rollback(); } catch (SQLException ignored) {}
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error during loan issue: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            try { if (transactionConn != null) transactionConn.rollback(); } catch (SQLException ignored) {}
            ex.printStackTrace();
        } finally {
            try { if (transactionConn != null) transactionConn.setAutoCommit(true); } catch (SQLException finalEx) { finalEx.printStackTrace(); }
        }
    }

    // Return Book (Update)
    private void returnBook() {
        String loanIDStr = txtLoanID.getText();
        if (loanIDStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter or select the Loan ID to mark as returned.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.sql.Date returnDate = new java.sql.Date(new java.util.Date().getTime()); // Today's date

        Connection transactionConn = null;
        try {
            int loanID = Integer.parseInt(loanIDStr);

            transactionConn = conn;
            transactionConn.setAutoCommit(false);

            // 1. Get BookID and check if already returned
            int bookID = -1;
            boolean alreadyReturned = false;
            pst = transactionConn.prepareStatement("SELECT BookID, ReturnDate FROM loans WHERE LoanID = ? FOR UPDATE");
            pst.setInt(1, loanID);
            rs = pst.executeQuery();
            if (rs.next()) {
                bookID = rs.getInt("BookID");
                if (rs.getDate("ReturnDate") != null) {
                    alreadyReturned = true;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Loan ID not found.", "Error", JOptionPane.ERROR_MESSAGE);
                transactionConn.rollback();
                return;
            }
            rs.close();
            pst.close();

            if (alreadyReturned) {
                JOptionPane.showMessageDialog(this, "This loan has already been marked as returned.", "Info", JOptionPane.INFORMATION_MESSAGE);
                transactionConn.rollback(); // No changes needed
                return;
            }

            // 2. Update Loan with Return Date
            pst = transactionConn.prepareStatement("UPDATE loans SET ReturnDate = ? WHERE LoanID = ?");
            pst.setDate(1, returnDate);
            pst.setInt(2, loanID);
            int loanUpdateResult = pst.executeUpdate();
            pst.close();

            // 3. Increment Book Copies
            pst = transactionConn.prepareStatement("UPDATE books SET CopiesAvailable = CopiesAvailable + 1 WHERE BookID = ?");
            pst.setInt(1, bookID);
            int bookUpdateResult = pst.executeUpdate();
            pst.close();

            if (loanUpdateResult > 0 && bookUpdateResult > 0) {
                transactionConn.commit();
                JOptionPane.showMessageDialog(this, "Book returned successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadLoansData(); // Refresh table
                clearForm();
            } else {
                transactionConn.rollback();
                JOptionPane.showMessageDialog(this, "Failed to process return. Transaction rolled back.", "Transaction Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Loan ID format.", "Input Error", JOptionPane.WARNING_MESSAGE);
             try { if (transactionConn != null) transactionConn.rollback(); } catch (SQLException ignored) {}
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error during book return: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
             try { if (transactionConn != null) transactionConn.rollback(); } catch (SQLException ignored) {}
            ex.printStackTrace();
        } finally {
             try { if (transactionConn != null) transactionConn.setAutoCommit(true); } catch (SQLException finalEx) { finalEx.printStackTrace(); }
        }
    }

     // Search Loan by LoanID (like the example's searchBook)
    private void searchLoan() {
        String loanIDStr = txtLoanID.getText();
        if (loanIDStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the Loan ID to search.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int loanID = Integer.parseInt(loanIDStr);
            pst = conn.prepareStatement("SELECT * FROM loans WHERE LoanID=?");
            pst.setInt(1, loanID);
            rs = pst.executeQuery();

            if (rs.next()) {
                txtMemberID.setText(String.valueOf(rs.getInt("MemberID")));
                txtBookID.setText(String.valueOf(rs.getInt("BookID")));
                Date loanDateSql = rs.getDate("LoanDate");
                txtLoanDate.setText(loanDateSql != null ? dateFormat.format(loanDateSql) : "");
                Date dueDateSql = rs.getDate("DueDate");
                txtDueDate.setText(dueDateSql != null ? dateFormat.format(dueDateSql) : "");
                Date returnDateSql = rs.getDate("ReturnDate");
                txtReturnDate.setText(returnDateSql != null ? dateFormat.format(returnDateSql) : "");
            } else {
                JOptionPane.showMessageDialog(this, "Loan not found.", "Search Result", JOptionPane.INFORMATION_MESSAGE);
                // Optionally clear fields if not found
                // clearForm(); // Or just clear related fields
                 txtMemberID.setText("");
                 txtBookID.setText("");
                 txtLoanDate.setText("");
                 txtDueDate.setText("");
                 txtReturnDate.setText("");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Loan ID format.", "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error during search: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblLoanID = new javax.swing.JLabel();
        txtLoanID = new javax.swing.JTextField();
        lblMemberID = new javax.swing.JLabel();
        txtMemberID = new javax.swing.JTextField();
        lblBookID = new javax.swing.JLabel();
        txtBookID = new javax.swing.JTextField();
        lblLoanDate = new javax.swing.JLabel();
        txtLoanDate = new javax.swing.JTextField();
        lblDueDate = new javax.swing.JLabel();
        txtDueDate = new javax.swing.JTextField();
        lblReturnDate = new javax.swing.JLabel();
        txtReturnDate = new javax.swing.JTextField();
        btnIssueLoan = new javax.swing.JButton();
        btnReturnBook = new javax.swing.JButton();
        btnSearch = new javax.swing.JButton();
        btnViewAll = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblLoans = new javax.swing.JTable();
        btnClear = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Manage Loans");

        lblLoanID.setText("Loan ID:");

        txtLoanID.setToolTipText("Enter Loan ID for Search or Return");

        lblMemberID.setText("Member ID:");

        txtMemberID.setToolTipText("Enter Member ID for issuing a new loan");

        lblBookID.setText("Book ID:");

        txtBookID.setToolTipText("Enter Book ID for issuing a new loan");

        lblLoanDate.setText("Loan Date:");

        txtLoanDate.setToolTipText("YYYY-MM-DD (Defaults to today)");

        lblDueDate.setText("Due Date:");

        txtDueDate.setToolTipText("YYYY-MM-DD (Defaults to 14 days from Loan Date)");

        lblReturnDate.setText("Return Date:");

        txtReturnDate.setToolTipText("YYYY-MM-DD (Set automatically on return)");
        txtReturnDate.setEnabled(false); // Cannot be manually edited

        btnIssueLoan.setText("Issue Loan");
        btnIssueLoan.setToolTipText("Add a new loan record (Requires Member ID, Book ID, Loan Date, Due Date)");
        btnIssueLoan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIssueLoanActionPerformed(evt);
            }
        });

        btnReturnBook.setText("Return Book");
        btnReturnBook.setToolTipText("Mark the loan specified by Loan ID as returned");
        btnReturnBook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReturnBookActionPerformed(evt);
            }
        });

        btnSearch.setText("Search by Loan ID");
        btnSearch.setToolTipText("Find loan details using the Loan ID");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnViewAll.setText("View All / Refresh");
        btnViewAll.setToolTipText("Reload all loan records from the database");
        btnViewAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewAllActionPerformed(evt);
            }
        });

        tblLoans.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "LoanID", "MemberID", "BookID", "Loan Date", "Due Date", "Return Date"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblLoans.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblLoansMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblLoans);

        btnClear.setText("Clear Form");
        btnClear.setToolTipText("Clear all input fields");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(lblReturnDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblDueDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblLoanDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblBookID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblMemberID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblLoanID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtLoanID, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtMemberID, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtBookID, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtLoanDate, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtDueDate, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtReturnDate, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnIssueLoan)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnReturnBook)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSearch)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnViewAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnClear)))
                        .addGap(0, 138, Short.MAX_VALUE))) // Adjust spacing
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblLoanID)
                    .addComponent(txtLoanID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMemberID)
                    .addComponent(txtMemberID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBookID)
                    .addComponent(txtBookID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblLoanDate)
                    .addComponent(txtLoanDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblDueDate)
                    .addComponent(txtDueDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblReturnDate)
                    .addComponent(txtReturnDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIssueLoan)
                    .addComponent(btnReturnBook)
                    .addComponent(btnSearch)
                    .addComponent(btnViewAll)
                    .addComponent(btnClear))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addGap(20, 20, 20))
        );

        pack();
        setLocationRelativeTo(null); // Center the form
    }// </editor-fold>//GEN-END:initComponents

    private void btnIssueLoanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIssueLoanActionPerformed
        issueLoan();
    }//GEN-LAST:event_btnIssueLoanActionPerformed

    private void btnReturnBookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReturnBookActionPerformed
        returnBook();
    }//GEN-LAST:event_btnReturnBookActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        searchLoan();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnViewAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewAllActionPerformed
        loadLoansData();
    }//GEN-LAST:event_btnViewAllActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearActionPerformed

    private void tblLoansMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblLoansMouseClicked
        // Populate fields when a row is clicked
        int selectedRow = tblLoans.getSelectedRow();
        if (selectedRow != -1) {
            dtm = (DefaultTableModel) tblLoans.getModel();
            txtLoanID.setText(dtm.getValueAt(selectedRow, 0).toString());
            txtMemberID.setText(dtm.getValueAt(selectedRow, 1).toString());
            txtBookID.setText(dtm.getValueAt(selectedRow, 2).toString());
            txtLoanDate.setText(dtm.getValueAt(selectedRow, 3) != null ? dtm.getValueAt(selectedRow, 3).toString() : "");
            txtDueDate.setText(dtm.getValueAt(selectedRow, 4) != null ? dtm.getValueAt(selectedRow, 4).toString() : "");
            txtReturnDate.setText(dtm.getValueAt(selectedRow, 5) != null ? dtm.getValueAt(selectedRow, 5).toString() : "");
        }
    }//GEN-LAST:event_tblLoansMouseClicked

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(LoansForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LoansForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnIssueLoan;
    private javax.swing.JButton btnReturnBook;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnViewAll;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblBookID;
    private javax.swing.JLabel lblDueDate;
    private javax.swing.JLabel lblLoanDate;
    private javax.swing.JLabel lblLoanID;
    private javax.swing.JLabel lblMemberID;
    private javax.swing.JLabel lblReturnDate;
    private javax.swing.JTable tblLoans;
    private javax.swing.JTextField txtBookID;
    private javax.swing.JTextField txtDueDate;
    private javax.swing.JTextField txtLoanDate;
    private javax.swing.JTextField txtLoanID;
    private javax.swing.JTextField txtMemberID;
    private javax.swing.JTextField txtReturnDate;
    // End of variables declaration//GEN-END:variables
}

