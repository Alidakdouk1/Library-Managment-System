/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.librarymanagementsystem;

/**
 *
 * @author Toshiba
 */

    // StaffForm.java

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class StaffForm extends javax.swing.JFrame {

    private Connection conn;
    private PreparedStatement pst;
    private ResultSet rs;
    private DefaultTableModel dtm;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public StaffForm() {
        initComponents();
        connect();
        if (conn != null) {
            loadStaffData();
        } else {
             JOptionPane.showMessageDialog(this, "Database Connection Failed. Cannot load data.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        // Optional: Clear placeholder text if using NetBeans GUI builder defaults
        // txtStaffID.setText("");
        // txtFirstName.setText("");
        // txtLastName.setText("");
        // txtHireDate.setText("");
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

    
    private void loadStaffData() {
        try {
            pst = conn.prepareStatement("SELECT StaffID, FirstName, LastName, Position, Role, Email, Phone, HireDate FROM staff ORDER BY StaffID");
            rs = pst.executeQuery();

            dtm = (DefaultTableModel) tblStaff.getModel();
            dtm.setRowCount(0); // Clear existing rows

            // Ensure columns are set correctly for staff
            if (dtm.getColumnCount() < 8 || !dtm.getColumnName(0).equals("StaffID")) { // Check if columns need reset
                dtm.setColumnCount(0); // Clear existing columns first
                dtm.addColumn("StaffID");
                dtm.addColumn("First Name");
                dtm.addColumn("Last Name");
                dtm.addColumn("Position");
                dtm.addColumn("Role");
                dtm.addColumn("Email");
                dtm.addColumn("Phone");
                dtm.addColumn("Hire Date");
            }

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("StaffID"));
                row.add(rs.getString("FirstName"));
                row.add(rs.getString("LastName"));
                row.add(rs.getString("Position") != null ? rs.getString("Position") : "");
                row.add(rs.getString("Role") != null ? rs.getString("Role") : "");
                row.add(rs.getString("Email") != null ? rs.getString("Email") : "");
                row.add(rs.getString("Phone") != null ? rs.getString("Phone") : "");
                // Format date for display
                Date sqlDate = rs.getDate("HireDate");
                row.add(sqlDate != null ? dateFormat.format(sqlDate) : "");
                dtm.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading staff data: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    
    private void clearForm() {
        txtStaffID.setText("");
        txtFirstName.setText("");
        txtLastName.setText("");
        // Assuming UI components exist: txtPosition, cmbRole, txtEmail, txtPhone, txtHireDate
        // txtPosition.setText("");
        // cmbRole.setSelectedIndex(0); // Reset role dropdown
        txtEmail.setText("");
        // txtPhone.setText("");
        txtHireDate.setText("");
        tblStaff.clearSelection();
        txtStaffID.setEditable(true); // Re-enable StaffID field
        // Reset button states if needed (e.g., enable Add, disable Update/Delete)
        // btnAdd.setEnabled(true);
        // btnUpdate.setEnabled(false);
        // btnDelete.setEnabled(false);
    }

    
    private void addStaff() {
        String staffIDStr = txtStaffID.getText();
        String firstName = txtFirstName.getText();
        String lastName = txtLastName.getText();
        // String position = txtPosition.getText(); // Placeholder - Needs UI element
        // String role = cmbRole.getSelectedItem().toString(); // Placeholder - Needs UI element (JComboBox)
        String email = txtEmail.getText();
        // String phone = txtPhone.getText(); // Placeholder - Needs UI element
        String hireDateStr = txtHireDate.getText();

        // Basic validation
        if (staffIDStr.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Staff ID, First Name, and Last Name are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Add placeholders for new fields until UI is updated
        String position = "Librarian"; // Temporary default
        String role = "Librarian"; // Temporary default
        String phone = ""; // Temporary default

        java.sql.Date hireDate = null;
        if (!hireDateStr.isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(hireDateStr);
                hireDate = new java.sql.Date(parsedDate.getTime());
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format for Hire Date. Please use YYYY-MM-DD.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            int staffID = Integer.parseInt(staffIDStr);

            // Updated SQL INSERT statement
            pst = conn.prepareStatement("INSERT INTO staff (StaffID, FirstName, LastName, Position, Role, Email, Phone, HireDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            pst.setInt(1, staffID);
            pst.setString(2, firstName);
            pst.setString(3, lastName);
            pst.setString(4, position); // Use placeholder
            pst.setString(5, role);     // Use placeholder
            if (email != null && !email.isEmpty()) {
                 pst.setString(6, email);
            } else {
                 pst.setNull(6, Types.VARCHAR);
            }
             if (phone != null && !phone.isEmpty()) { // Use placeholder
                 pst.setString(7, phone);
            } else {
                 pst.setNull(7, Types.VARCHAR);
            }
            if (hireDate != null) {
                 pst.setDate(8, hireDate);
            } else {
                 pst.setNull(8, Types.DATE);
            }

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Staff added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadStaffData(); // Refresh table
            clearForm();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Staff ID format. Must be a number.", "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            if (ex.getSQLState().equals("23000")) { // Integrity constraint violation
                 if (ex.getMessage().toLowerCase().contains("email")) {
                     JOptionPane.showMessageDialog(this, "Error adding staff: Email already exists.", "Database Error", JOptionPane.ERROR_MESSAGE);
                 } else {
                     JOptionPane.showMessageDialog(this, "Error adding staff: Staff ID already exists.", "Database Error", JOptionPane.ERROR_MESSAGE);
                 }
            } else {
                 JOptionPane.showMessageDialog(this, "Database error during insert: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
            ex.printStackTrace();
        }
    }

    
    private void updateStaff() {
         String staffIDStr = txtStaffID.getText(); // ID comes from the field, should be non-editable after selection
        String firstName = txtFirstName.getText();
        String lastName = txtLastName.getText();
        // String position = txtPosition.getText(); // Placeholder - Needs UI element
        // String role = cmbRole.getSelectedItem().toString(); // Placeholder - Needs UI element
        String email = txtEmail.getText();
        // String phone = txtPhone.getText(); // Placeholder - Needs UI element
        String hireDateStr = txtHireDate.getText();

        if (staffIDStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a staff staff to update.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
         if (firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "First Name and Last Name are required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Add placeholders for new fields until UI is updated
        String position = "Librarian"; // Temporary default
        String role = "Librarian"; // Temporary default
        String phone = ""; // Temporary default

        java.sql.Date hireDate = null;
        if (!hireDateStr.isEmpty()) {
            try {
                java.util.Date parsedDate = dateFormat.parse(hireDateStr);
                hireDate = new java.sql.Date(parsedDate.getTime());
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format for Hire Date. Please use YYYY-MM-DD.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            int staffID = Integer.parseInt(staffIDStr);

            // Updated SQL UPDATE statement
            pst = conn.prepareStatement("UPDATE staff SET FirstName=?, LastName=?, Position=?, Role=?, Email=?, Phone=?, HireDate=? WHERE StaffID=?");
            pst.setString(1, firstName);
            pst.setString(2, lastName);
            pst.setString(3, position); // Placeholder
            pst.setString(4, role);     // Placeholder
            if (email != null && !email.isEmpty()) {
                 pst.setString(5, email);
            } else {
                 pst.setNull(5, Types.VARCHAR);
            }
             if (phone != null && !phone.isEmpty()) { // Placeholder
                 pst.setString(6, phone);
            } else {
                 pst.setNull(6, Types.VARCHAR);
            }
            if (hireDate != null) {
                 pst.setDate(7, hireDate);
            } else {
                 pst.setNull(7, Types.DATE);
            }
            pst.setInt(8, staffID); // WHERE clause

            int result = pst.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Staff updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadStaffData(); // Refresh table
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update staff. StaffID might not exist.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Staff ID format.", "Input Error", JOptionPane.WARNING_MESSAGE); // Should not happen if ID field is non-editable
        } catch (SQLException ex) {
             if (ex.getSQLState().equals("23000") && ex.getMessage().toLowerCase().contains("email")) { // Check for unique constraint violation on Email
                 JOptionPane.showMessageDialog(this, "Error updating staff: Email already exists for another staff staff.", "Database Error", JOptionPane.ERROR_MESSAGE);
             } else {
                JOptionPane.showMessageDialog(this, "Database error during update: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
             }
            ex.printStackTrace();
        }
    }

    
    private void deleteStaff() {
        String staffIDStr = txtStaffID.getText(); // Get ID from the field
        if (staffIDStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select the Staff ID to delete.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Staff ID: " + staffIDStr + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int staffID = Integer.parseInt(staffIDStr);
                // Updated SQL DELETE statement
                pst = conn.prepareStatement("DELETE FROM staff WHERE StaffID=?");
                pst.setInt(1, staffID);

                int result = pst.executeUpdate();
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Staff deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadStaffData(); // Refresh table
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete staff. StaffID might not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                 JOptionPane.showMessageDialog(this, "Invalid Staff ID format.", "Input Error", JOptionPane.WARNING_MESSAGE); // Should not happen if ID field is non-editable
            } catch (SQLException ex) {
                 if (ex.getSQLState().startsWith("23")) { // Integrity constraint violation (e.g., staff processed loans)
                    JOptionPane.showMessageDialog(this, "Cannot delete staff. They might be associated with existing loans.", "Deletion Failed", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Database error during delete: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
                ex.printStackTrace();
            }
        }
    }

    
    private void searchStaff() {
        String staffIDStr = txtStaffID.getText();
        if (staffIDStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the Staff ID to search.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int staffID = Integer.parseInt(staffIDStr);
            // Updated SQL SELECT statement
            pst = conn.prepareStatement("SELECT * FROM staff WHERE StaffID=?");
            pst.setInt(1, staffID);

            rs = pst.executeQuery();

            if (rs.next()) {
                txtFirstName.setText(rs.getString("FirstName"));
                txtLastName.setText(rs.getString("LastName"));
                // txtPosition.setText(rs.getString("Position") != null ? rs.getString("Position") : ""); // Placeholder - Needs UI element
                // cmbRole.setSelectedItem(rs.getString("Role") != null ? rs.getString("Role") : "Librarian"); // Placeholder - Needs UI element
                txtEmail.setText(rs.getString("Email") != null ? rs.getString("Email") : "");
                // txtPhone.setText(rs.getString("Phone") != null ? rs.getString("Phone") : ""); // Placeholder - Needs UI element
                Date sqlDate = rs.getDate("HireDate");
                txtHireDate.setText(sqlDate != null ? dateFormat.format(sqlDate) : "");

                // Select the found staff in the table (optional)
                // findAndSelectStaffInTable(staffID);

                // Disable StaffID field after search
                 txtStaffID.setEditable(false);
                 // Enable Update/Delete buttons (Assuming buttons exist: btnUpdate, btnDelete)
                 // btnUpdate.setEnabled(true);
                 // btnDelete.setEnabled(true);

            } else {
                JOptionPane.showMessageDialog(this, "Staff not found.", "Search Result", JOptionPane.INFORMATION_MESSAGE);
                // Optionally clear other fields if staff not found
                 clearForm(); // Clear form if not found
                 txtStaffID.setText(staffIDStr); // Keep the searched ID
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Staff ID format.", "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error during search: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    // initComponents needs to be updated for Staff fields (Position, Role, Phone)
    private void initComponents() {

        lblStaffID = new javax.swing.JLabel();
        txtStaffID = new javax.swing.JTextField();
        lblFirstName = new javax.swing.JLabel();
        txtFirstName = new javax.swing.JTextField();
        lblLastName = new javax.swing.JLabel();
        txtLastName = new javax.swing.JTextField();
        lblHireDate = new javax.swing.JLabel();
        txtHireDate = new javax.swing.JTextField();
        lblEmail = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        btnAdd = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnSearch = new javax.swing.JButton();
        btnViewAll = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblStaff = new javax.swing.JTable();
        btnClear = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE); // Dispose on close, not exit
        setTitle("Manage Staff");

        lblStaffID.setText("Staff ID:");

        txtStaffID.setToolTipText("Enter Staff ID (required for Add, Update, Delete, Search)");

        lblFirstName.setText("First Name:");

        lblLastName.setText("Last Name:");

        lblHireDate.setText("Membership Date:");

        txtHireDate.setToolTipText("YYYY-MM-DD");

        lblEmail.setText("Email:");

        btnAdd.setText("Add");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddStaffActionPerformed(evt);
            }
        });

        btnUpdate.setText("Update");
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateStaffActionPerformed(evt);
            }
        });

        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteStaffActionPerformed(evt);
            }
        });

        btnSearch.setText("Search by ID");
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchStaffActionPerformed(evt);
            }
        });

        btnViewAll.setText("View All / Refresh");
        btnViewAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewAllStaffActionPerformed(evt);
            }
        });

        tblStaff.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "StaffID", "First Name", "Last Name", "Position", "Role", "Email", "Phone", "Hire Date"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblStaff.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblStaffMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblStaff);

        btnClear.setText("Clear Form");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearFormActionPerformed(evt);
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
                                    .addComponent(lblHireDate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblLastName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblFirstName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblStaffID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblEmail, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtStaffID, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtHireDate, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                    .addComponent(lblStaffID)
                    .addComponent(txtStaffID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
                    .addComponent(lblHireDate)
                    .addComponent(txtHireDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

    private void btnAddStaffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddStaffActionPerformed
        addStaff();
    }//GEN-LAST:event_btnAddStaffActionPerformed

    private void btnUpdateStaffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateStaffActionPerformed
        updateStaff();
    }//GEN-LAST:event_btnUpdateStaffActionPerformed

    private void btnDeleteStaffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteStaffActionPerformed
        deleteStaff();
    }//GEN-LAST:event_btnDeleteStaffActionPerformed

    private void btnSearchStaffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchStaffActionPerformed
        searchStaff();
    }//GEN-LAST:event_btnSearchStaffActionPerformed

    private void btnViewAllStaffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewAllStaffActionPerformed
        loadStaffData();
    }//GEN-LAST:event_btnViewAllStaffActionPerformed

    
    private void tblStaffMouseClicked(java.awt.event.MouseEvent evt) {
        int selectedRow = tblStaff.getSelectedRow();
        if (selectedRow >= 0) {
            dtm = (DefaultTableModel) tblStaff.getModel();
            txtStaffID.setText(dtm.getValueAt(selectedRow, 0).toString());
            txtFirstName.setText(dtm.getValueAt(selectedRow, 1).toString());
            txtLastName.setText(dtm.getValueAt(selectedRow, 2).toString());
            // Assuming UI components exist for these fields: txtPosition, cmbRole, txtEmail, txtPhone, txtHireDate
            // Need to add these components first. For now, just populate existing ones + HireDate + Email.
            // txtPosition.setText(dtm.getValueAt(selectedRow, 3).toString());
            // cmbRole.setSelectedItem(dtm.getValueAt(selectedRow, 4).toString()); // Assuming a JComboBox for Role
            txtEmail.setText(dtm.getValueAt(selectedRow, 5) != null ? dtm.getValueAt(selectedRow, 5).toString() : "");
            // txtPhone.setText(dtm.getValueAt(selectedRow, 6).toString());
            txtHireDate.setText(dtm.getValueAt(selectedRow, 7) != null ? dtm.getValueAt(selectedRow, 7).toString() : "");

            // Disable StaffID field for editing after selection
            txtStaffID.setEditable(false);
            // Enable Update/Delete buttons, disable Add (Assuming buttons exist: btnAdd, btnUpdate, btnDelete)
            // btnAdd.setEnabled(false);
            // btnUpdate.setEnabled(true);
            // btnDelete.setEnabled(true);
        }
    }//GEN-LAST:event_tblStaffMouseClicked

    private void btnClearFormActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearFormActionPerformed
        clearForm();
    }//GEN-LAST:event_btnClearFormActionPerformed

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
    private javax.swing.JLabel lblStaffID;
    private javax.swing.JLabel lblHireDate;
    private javax.swing.JTable tblStaff;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtFirstName;
    private javax.swing.JTextField txtLastName;
    private javax.swing.JTextField txtStaffID;
    private javax.swing.JTextField txtHireDate;
    
}



    // --- UI Components (Placeholders - Need to be added in initComponents or .form file) ---
    // private javax.swing.JLabel lblPosition;
    // private javax.swing.JTextField txtPosition;
    // private javax.swing.JLabel lblRole;
    // private javax.swing.JComboBox<String> cmbRole; // Use JComboBox for roles
    // private javax.swing.JLabel lblPhone;
    // private javax.swing.JTextField txtPhone;
    // --- End UI Placeholders ---

