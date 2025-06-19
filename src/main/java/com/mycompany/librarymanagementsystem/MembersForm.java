// MembersForm.java
package com.mycompany.librarymanagementsystem;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class MembersForm extends javax.swing.JFrame {

    private Connection conn;
    private PreparedStatement pst;
    private ResultSet rs;
    private DefaultTableModel dtm;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public MembersForm() {
        initComponents();
        connect();
        if (conn != null) {
            loadMembersData();
        } else {
             JOptionPane.showMessageDialog(this, "Database Connection Failed. Cannot load data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        // Optional: Clear placeholder text if using NetBeans GUI builder defaults
        // txtMemberID.setText("");
        // txtFirstName.setText("");
        // txtLastName.setText("");
        // txtMembershipDate.setText("");
        // txtEmail.setText("");
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

    private void loadMembersData() {
        try {
            pst = conn.prepareStatement("SELECT * FROM members ORDER BY MemberID");
            rs = pst.executeQuery();

            dtm = (DefaultTableModel) tblMembers.getModel();
            dtm.setRowCount(0); // Clear existing rows

            // Ensure columns are set (if table model was empty initially)
            if (dtm.getColumnCount() == 0) {
                dtm.addColumn("MemberID");
                dtm.addColumn("First Name");
                dtm.addColumn("Last Name");
                dtm.addColumn("Membership Date");
                dtm.addColumn("Email");
            }

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("MemberID"));
                row.add(rs.getString("FirstName"));
                row.add(rs.getString("LastName"));
                // Format date for display
                Date sqlDate = rs.getDate("MembershipDate");
                row.add(sqlDate != null ? dateFormat.format(sqlDate) : "");
                row.add(rs.getString("Email") != null ? rs.getString("Email") : "");
                dtm.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading members data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void clearForm() {
        txtMemberID.setText(""); // Assuming MemberID might be manually entered based on LibraryForm pattern
        txtFirstName.setText("");
        txtLastName.setText("");
        txtMembershipDate.setText("");
        txtEmail.setText("");
        tblMembers.clearSelection();
        // Reset button states if needed (e.g., enable Add, disable Update/Delete)
    }

    private void addMember() {
        String memberIDStr = txtMemberID.getText();
        String firstName = txtFirstName.getText();
        String lastName = txtLastName.getText();
        String memDateStr = txtMembershipDate.getText();
        String email = txtEmail.getText();

        if (memberIDStr.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Member ID, First Name, and Last Name are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.sql.Date memDate = null;
        if (!memDateStr.isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(memDateStr);
                memDate = new java.sql.Date(parsedDate.getTime());
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            int memberID = Integer.parseInt(memberIDStr);

            pst = conn.prepareStatement("INSERT INTO members (MemberID, FirstName, LastName, MembershipDate, Email) VALUES (?, ?, ?, ?, ?)");
            pst.setInt(1, memberID);
            pst.setString(2, firstName);
            pst.setString(3, lastName);
            if (memDate != null) {
                 pst.setDate(4, memDate);
            } else {
                 pst.setNull(4, Types.DATE);
            }
            if (email != null && !email.isEmpty()) {
                 pst.setString(5, email);
            } else {
                 pst.setNull(5, Types.VARCHAR);
            }

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Member added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadMembersData(); // Refresh table
            clearForm();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Member ID format. Must be a number.", "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            if (ex.getSQLState().equals("23000")) { // Integrity constraint violation (likely duplicate MemberID)
                 JOptionPane.showMessageDialog(this, "Error adding member: Member ID already exists.", "Database Error", JOptionPane.ERROR_MESSAGE);
            } else {
                 JOptionPane.showMessageDialog(this, "Database error during insert: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
            ex.printStackTrace();
        }
    }

    private void updateMember() {
         String memberIDStr = txtMemberID.getText();
        String firstName = txtFirstName.getText();
        String lastName = txtLastName.getText();
        String memDateStr = txtMembershipDate.getText();
        String email = txtEmail.getText();

        if (memberIDStr.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Member ID, First Name, and Last Name are required to update.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.sql.Date memDate = null;
        if (!memDateStr.isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(memDateStr);
                memDate = new java.sql.Date(parsedDate.getTime());
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            int memberID = Integer.parseInt(memberIDStr);

            pst = conn.prepareStatement("UPDATE members SET FirstName=?, LastName=?, MembershipDate=?, Email=? WHERE MemberID=?");
            pst.setString(1, firstName);
            pst.setString(2, lastName);
             if (memDate != null) {
                 pst.setDate(3, memDate);
            } else {
                 pst.setNull(3, Types.DATE);
            }
            if (email != null && !email.isEmpty()) {
                 pst.setString(4, email);
            } else {
                 pst.setNull(4, Types.VARCHAR);
            }
            pst.setInt(5, memberID);

            int result = pst.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Member updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadMembersData(); // Refresh table
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update member. MemberID might not exist.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Member ID format.", "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error during update: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void deleteMember() {
        String memberIDStr = txtMemberID.getText();
        if (memberIDStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter or select the Member ID to delete.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Member ID: " + memberIDStr + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int memberID = Integer.parseInt(memberIDStr);
                pst = conn.prepareStatement("DELETE FROM members WHERE MemberID=?");
                pst.setInt(1, memberID);

                int result = pst.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Member deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadMembersData(); // Refresh table
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete member. MemberID might not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                 JOptionPane.showMessageDialog(this, "Invalid Member ID format.", "Input Error", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                 if (ex.getSQLState().startsWith("23")) { // Integrity constraint violation (e.g., member has loans)
                    JOptionPane.showMessageDialog(this, "Cannot delete member. They might have active loans.", "Deletion Failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Database error during delete: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
                ex.printStackTrace();
            }
        }
    }

    private void searchMember() {
        String memberIDStr = txtMemberID.getText();
        if (memberIDStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the Member ID to search.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int memberID = Integer.parseInt(memberIDStr);
            pst = conn.prepareStatement("SELECT * FROM members WHERE MemberID=?");
            pst.setInt(1, memberID);

            rs = pst.executeQuery();

            if (rs.next()) {
                txtFirstName.setText(rs.getString("FirstName"));
                txtLastName.setText(rs.getString("LastName"));
                Date sqlDate = rs.getDate("MembershipDate");
                txtMembershipDate.setText(sqlDate != null ? dateFormat.format(sqlDate) : "");
                txtEmail.setText(rs.getString("Email") != null ? rs.getString("Email") : "");
            } else {
                JOptionPane.showMessageDialog(this, "Member not found.", "Search Result", JOptionPane.INFORMATION_MESSAGE);
                // Optionally clear other fields if member not found
                // txtFirstName.setText("");
                // txtLastName.setText("");
                // txtMembershipDate.setText("");
                // txtEmail.setText("");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Member ID format.", "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error during search: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblMemberID = new javax.swing.JLabel();
        txtMemberID = new javax.swing.JTextField();
        lblFirstName = new javax.swing.JLabel();
        txtFirstName = new javax.swing.JTextField();
        lblLastName = new javax.swing.JLabel();
        txtLastName = new javax.swing.JTextField();
        lblMembershipDate = new javax.swing.JLabel();
        txtMembershipDate = new javax.swing.JTextField();
        lblEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        btnAdd = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnSearch = new javax.swing.JButton();
        btnViewAll = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblMembers = new javax.swing.JTable();
        btnClear = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE); // Dispose on close, not exit
        setTitle("Manage Members");

        lblMemberID.setText("Member ID:");

        txtMemberID.setToolTipText("Enter Member ID (required for Add, Update, Delete, Search)");

        lblFirstName.setText("First Name:");

        lblLastName.setText("Last Name:");

        lblMembershipDate.setText("Membership Date:");

        txtMembershipDate.setToolTipText("YYYY-MM-DD");

        lblEmail.setText("Email:");

        btnAdd.setText("Add");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnUpdate.setText("Update");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });

        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnSearch.setText("Search by ID");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        btnViewAll.setText("View All / Refresh");
        btnViewAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewAllActionPerformed(evt);
            }
        });

        tblMembers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "MemberID", "First Name", "Last Name", "Membership Date", "Email"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblMembers.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblMembersMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblMembers);

        btnClear.setText("Clear Form");
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
                                    .addComponent(lblMembershipDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblLastName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblFirstName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblMemberID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblEmail, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtMemberID, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtMembershipDate, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnAdd) // Adjusted button sizes/spacing
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnUpdate)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnDelete)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSearch)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnViewAll)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnClear)))
                        .addGap(0, 119, Short.MAX_VALUE))) // Adjust spacing if needed
                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMemberID)
                    .addComponent(txtMemberID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblFirstName)
                    .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblLastName)
                    .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblMembershipDate)
                    .addComponent(txtMembershipDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblEmail)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAdd)
                    .addComponent(btnUpdate)
                    .addComponent(btnDelete)
                    .addComponent(btnSearch)
                    .addComponent(btnViewAll)
                    .addComponent(btnClear))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE) // Allow table to grow
                .addGap(20, 20, 20))
        );

        pack();
        setLocationRelativeTo(null); // Center the form
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        addMember();
    }//GEN-LAST:event_btnAddActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        updateMember();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        deleteMember();
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        searchMember();
    }//GEN-LAST:event_btnSearchActionPerformed

    private void btnViewAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewAllActionPerformed
        loadMembersData();
    }//GEN-LAST:event_btnViewAllActionPerformed

    private void tblMembersMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblMembersMouseClicked
        // Get data from selected row and populate the fields
        int selectedRow = tblMembers.getSelectedRow();
        if (selectedRow != -1) {
            dtm = (DefaultTableModel) tblMembers.getModel();
            txtMemberID.setText(dtm.getValueAt(selectedRow, 0).toString());
            txtFirstName.setText(dtm.getValueAt(selectedRow, 1).toString());
            txtLastName.setText(dtm.getValueAt(selectedRow, 2).toString());
            txtMembershipDate.setText(dtm.getValueAt(selectedRow, 3) != null ? dtm.getValueAt(selectedRow, 3).toString() : "");
            txtEmail.setText(dtm.getValueAt(selectedRow, 4) != null ? dtm.getValueAt(selectedRow, 4).toString() : "");
            // Optionally disable Add button, enable Update/Delete
        }
    }//GEN-LAST:event_tblMembersMouseClicked

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearActionPerformed

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(MembersForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MembersForm().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnClear;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JButton btnViewAll;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblEmail;
    private javax.swing.JLabel lblFirstName;
    private javax.swing.JLabel lblLastName;
    private javax.swing.JLabel lblMemberID;
    private javax.swing.JLabel lblMembershipDate;
    private javax.swing.JTable tblMembers;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtFirstName;
    private javax.swing.JTextField txtLastName;
    private javax.swing.JTextField txtMemberID;
    private javax.swing.JTextField txtMembershipDate;
    // End of variables declaration//GEN-END:variables
}

