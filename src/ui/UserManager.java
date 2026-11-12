package ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import model.Member;
import service.LibrarySystem;

/**
 * User Manager window.
 * Provides a graphical interface for staff to:
 * - Browse and search members
 * - Edit selected member info
 * - Delete members
 * - Add new members
 *
 * @author alex pollard - s2342835
 */
public class UserManager extends javax.swing.JFrame {
    
    // ------------------- Fields -------------------
    private LibrarySystem system;   // central library system reference
    
    private DefaultTableModel memberModel;  // table model for browsing books
    private TableRowSorter<TableModel> sorter;  // sorter for sorting the user table

    // ------------------- Constructor ---------------------
    /**
     * Constructs the UserManager form
     * Initialises UI components, populates table
     */
    public UserManager(LibrarySystem system) {
        this.system = system;
        initComponents();
        
        // Initialise table models
        memberModel = (DefaultTableModel) memberTable.getModel();
        sorter = new TableRowSorter<>(memberModel);
        memberTable.setRowSorter(sorter);
        
        // Populate table
        fillMemberTable(system.getAllMembers());
    }
    
    // ------------------- Browse / Search / Editing -------------------
    
    /** Fills the member browse table with a collection of members. */
    private void fillMemberTable(Collection<Member> members) {
        memberModel.setRowCount(0);
        // Iterates through all members
        for (Member currentMember : members) {
            memberModel.addRow(new Object[]{
                currentMember.getMemberId(),
                currentMember.getFirstName(),
                currentMember.getSurname(),
                currentMember.getLoans().size()
            });
        }
        // Sorts the members by the selected option
        applySortOnMembers();
    }
    
    /** Performs a search based on user input and selected type. */
    private void performSearch() {
        String type = (String) searchByCombo.getSelectedItem();
        String query = searchEntry.getText().trim().toLowerCase();
        
        // Performs the search. No prefix searches for members.
        List<Member> results = new ArrayList<>();

        switch (type) {
            case "MemberID" -> {
                try {
                    int id = Integer.parseInt(query);
                    Member found = system.getMember(id);
                    if (found != null) results.add(found);
                } catch (NumberFormatException e){
                    // Invalid input
                }
            }
            case "Firstname" -> {
                for (Member currentMember : system.getAllMembers()){
                    if (currentMember.getFirstName().toLowerCase().equals(query)) results.add(currentMember);
                }
            }
            case "Surname" -> {
                for (Member currentMember : system.getAllMembers()){
                    if (currentMember.getSurname().toLowerCase().equals(query)) results.add(currentMember);
                }
            }
        }
        fillMemberTable(results);
    }
    
    /** Applies sorting to the browse table based on the selected criteria. */
    private void applySortOnMembers() {
        String sort = (String) sortByCombo.getSelectedItem();
        int columnIndex;
        
        switch (sort) {
            case "Firstname":
                columnIndex = 1;
                break;
            case "Surname":
                columnIndex = 2;
                break;
            case "MemberID":
            default:
                columnIndex = 0;
                break;
        }
        
        sorter.setSortKeys(List.of(new RowSorter.SortKey(columnIndex, SortOrder.ASCENDING)));
    }
    
    /** Fills the edit fields when a record is selected from the table. */
    private void tableTraversalLoad() {
        int viewRow = memberTable.getSelectedRow();
        if (viewRow != -1){
            int modelRow = memberTable.convertRowIndexToModel(viewRow);
            memberIdField.setText(String.valueOf(memberModel.getValueAt(modelRow, 0)));
            firstnameField.setText(String.valueOf(memberModel.getValueAt(modelRow, 1)));
            surnameField.setText(String.valueOf(memberModel.getValueAt(modelRow, 2)));
        }
    }
    
    /** Handles the saving of the changes made to the selected user */
    private void saveMemberChanges() {
        // Checks to see if a member has been selected from the table. If not, an error is shown.
        if (memberIdField.getText().equals("Member ID")) {
            JOptionPane.showMessageDialog(this, "No member selected.");
            return;
        }
        
        // If selected, extra check to make sure member ID exists. Member not found error shown if not.
        int memberId = Integer.parseInt(memberIdField.getText());
        Member selectedMember = system.getMember(memberId);
        if (selectedMember == null) {
            JOptionPane.showMessageDialog(this, "Member not found.");
            return;
        }
        
        // Gets the changed fields from the entries.
        String firstName = firstnameField.getText().trim();
        String surname = surnameField.getText().trim();
        
        // Checks to see if new values are not default, or empty.
        if (firstName.equals("Enter Firstname") || firstName.isEmpty() ||
                surname.equals("Enter Surname") || surname.isEmpty()){
            JOptionPane.showMessageDialog(this, "Make sure all fields are complete.");
            return;
        }
        
        // Sets the members attributes with the new values.
        selectedMember.setFirstName(firstnameField.getText().trim());
        selectedMember.setSurname(surnameField.getText().trim());
        
        JOptionPane.showMessageDialog(this, "Member information updated successfully.");
        
        // Member table refreshed and entry fields set to their defaults.
        fillMemberTable(system.getAllMembers());
        memberIdField.setText("Member ID");
        firstnameField.setText("Enter Firstname");
        surnameField.setText("Enter Surname");
        memberTable.clearSelection();
    }
    
    
    /** Handles the deletion of the selected member. */
    private void deleteSelectedMember() {
        // Gets the selected row from the table
        // Error message if there isn't a selection
        int viewRow = memberTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a member to delete.");
            return;
        }
        
        int modelRow = memberTable.convertRowIndexToModel(viewRow);
        
        // Gets the member object for the selected member ID.
        // Checks to make sure member exists as could have been deleted 
        // by another staff member since it was loaded into this table.
        int memberId = Integer.parseInt(String.valueOf(memberModel.getValueAt(modelRow, 0)));
        Member selectedMember = system.getMember(memberId);
        if (selectedMember == null) {
            JOptionPane.showMessageDialog(this, "Member not found.");
            return;
        }
        
        // Checks to see if member has any books checked out.
        // If not, asks for confirmation of deletion and deletes.
        // If they do, error message saying cannot be deleted as they have books checked out.
        boolean deleteable = system.canDeleteMember(memberId);
        if (deleteable) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete \"" + selectedMember.getFullName() +
                            "\"?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                system.deleteMember(memberId);
                JOptionPane.showMessageDialog(this, "Member deleted successfully.");
                fillMemberTable(system.getAllMembers());
                memberIdField.setText("Member ID");
                firstnameField.setText("Enter Firstname");
                surnameField.setText("Enter Surname");
                memberTable.clearSelection();
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete member \"" + selectedMember.getFullName() + 
                            "\" because they have books checked out or are in reservation queue..");
        }
    }

    /** Handles the addition of a new member to the system. */ 
    private void addMemberHandler() {
        // Gets the details entered for the new member.
        String firstName = newMemberFirstname.getText().trim();
        String surname = newMemberSurname.getText().trim();
        
        // Checks the fields have been changed from default and aren't empty.
        if (firstName.equals("Enter Firstname") || firstName.isEmpty() || surname.equals("Enter Surname") || surname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both firstname and surname.");
        }else{
            // Member is added to systems memory
            Member m = system.registerMember(firstName, surname);

            JOptionPane.showMessageDialog(this, "New member created: " + m.getFullName() + " (ID: " + m.getMemberId() + ".");

            // Member table is refreshed and entry fields set to their defaults.
            fillMemberTable(system.getAllMembers());
            newMemberFirstname.setText("Enter Firstname");
            newMemberSurname.setText("Enter Surname");
        }
        
        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        searchByCombo = new javax.swing.JComboBox<>();
        searchEntry = new javax.swing.JTextField();
        sortByCombo = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        memberTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        addMemberBtn = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        newMemberFirstname = new javax.swing.JTextField();
        newMemberSurname = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        searchBtn = new javax.swing.JButton();
        clearSearchBtn = new javax.swing.JButton();
        goBackBtn = new javax.swing.JButton();
        deleteUserBtn = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        firstnameField = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        surnameField = new javax.swing.JTextField();
        saveChangesBtn = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        memberIdField = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1175, 635));

        jPanel1.setBackground(new java.awt.Color(246, 246, 246));
        jPanel1.setPreferredSize(new java.awt.Dimension(1175, 635));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("Tw Cen MT Condensed Extra Bold", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("User Management Dashboard");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(29, 0, 0, 0);
        jPanel1.add(jLabel1, gridBagConstraints);

        jPanel2.setBackground(new java.awt.Color(0, 153, 153));
        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel2.setMinimumSize(new java.awt.Dimension(1175, 100));
        jPanel2.setPreferredSize(new java.awt.Dimension(1175, 100));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1173, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 98, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.weighty = 0.2;
        jPanel1.add(jPanel2, gridBagConstraints);

        searchByCombo.setMaximumRowCount(4);
        searchByCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "MemberID", "Firstname", "Surname" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(28, 63, 0, 6);
        jPanel1.add(searchByCombo, gridBagConstraints);

        searchEntry.setText("Enter Search");
        searchEntry.setMinimumSize(new java.awt.Dimension(120, 25));
        searchEntry.setPreferredSize(new java.awt.Dimension(120, 25));
        searchEntry.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                searchEntryFocusLost(evt);
            }
        });
        searchEntry.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                searchEntryMouseClicked(evt);
            }
        });
        searchEntry.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchEntryKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        jPanel1.add(searchEntry, gridBagConstraints);

        sortByCombo.setMaximumRowCount(3);
        sortByCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "MemberID", "Firstname", "Surname" }));
        sortByCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortByComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(28, 0, 0, 0);
        jPanel1.add(sortByCombo, gridBagConstraints);

        jLabel2.setText("Sort by:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 69, 4, 7);
        jPanel1.add(jLabel2, gridBagConstraints);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(500, 150));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(500, 150));

        memberTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "MemberID", "Firstname", "Surname", "No. Loaned Books"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        memberTable.setMinimumSize(new java.awt.Dimension(500, 150));
        memberTable.setRowHeight(25);
        memberTable.setRowMargin(2);
        memberTable.setSelectionBackground(new java.awt.Color(51, 153, 255));
        memberTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        memberTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        memberTable.setShowHorizontalLines(true);
        memberTable.setShowVerticalLines(true);
        memberTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                memberTableMouseReleased(evt);
            }
        });
        memberTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                memberTableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(memberTable);
        if (memberTable.getColumnModel().getColumnCount() > 0) {
            memberTable.getColumnModel().getColumn(0).setResizable(false);
            memberTable.getColumnModel().getColumn(1).setResizable(false);
            memberTable.getColumnModel().getColumn(2).setResizable(false);
            memberTable.getColumnModel().getColumn(3).setResizable(false);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(10, 63, 0, 62);
        jPanel1.add(jScrollPane1, gridBagConstraints);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel3.setMinimumSize(new java.awt.Dimension(550, 535));
        jPanel3.setPreferredSize(new java.awt.Dimension(550, 535));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 153, 153));
        jLabel3.setText("Create New Member");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 40, 0);
        jPanel3.add(jLabel3, gridBagConstraints);

        addMemberBtn.setText("Add Member");
        addMemberBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addMemberBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(38, 0, 15, 0);
        jPanel3.add(addMemberBtn, gridBagConstraints);

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel13.setText("Firstname");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 30);
        jPanel3.add(jLabel13, gridBagConstraints);

        jLabel18.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel18.setText("Surname");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 30, 0, 0);
        jPanel3.add(jLabel18, gridBagConstraints);

        newMemberFirstname.setText("Enter Firstname");
        newMemberFirstname.setMinimumSize(new java.awt.Dimension(110, 30));
        newMemberFirstname.setPreferredSize(new java.awt.Dimension(110, 30));
        newMemberFirstname.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                newMemberFirstnameFocusLost(evt);
            }
        });
        newMemberFirstname.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newMemberFirstnameMouseClicked(evt);
            }
        });
        newMemberFirstname.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                newMemberFirstnameKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 30);
        jPanel3.add(newMemberFirstname, gridBagConstraints);

        newMemberSurname.setText("Enter Surname");
        newMemberSurname.setMinimumSize(new java.awt.Dimension(110, 30));
        newMemberSurname.setPreferredSize(new java.awt.Dimension(110, 30));
        newMemberSurname.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                newMemberSurnameFocusLost(evt);
            }
        });
        newMemberSurname.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newMemberSurnameMouseClicked(evt);
            }
        });
        newMemberSurname.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                newMemberSurnameKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(5, 30, 0, 0);
        jPanel3.add(newMemberSurname, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel1.add(jPanel3, gridBagConstraints);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setText("Edit Existing Members");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        jPanel1.add(jLabel14, gridBagConstraints);

        searchBtn.setText("Search");
        searchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(searchBtn, gridBagConstraints);

        clearSearchBtn.setText("Clear");
        clearSearchBtn.setMargin(new java.awt.Insets(2, 1, 3, 1));
        clearSearchBtn.setMaximumSize(new java.awt.Dimension(60, 19));
        clearSearchBtn.setMinimumSize(new java.awt.Dimension(60, 19));
        clearSearchBtn.setPreferredSize(new java.awt.Dimension(60, 19));
        clearSearchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSearchBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 0, 0);
        jPanel1.add(clearSearchBtn, gridBagConstraints);

        goBackBtn.setText("Go Back");
        goBackBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                goBackBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        jPanel1.add(goBackBtn, gridBagConstraints);

        deleteUserBtn.setText("Delete Selected Member");
        deleteUserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteUserBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 62);
        jPanel1.add(deleteUserBtn, gridBagConstraints);

        jPanel4.setBackground(new java.awt.Color(246, 246, 246));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel15.setText("Firstname");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel4.add(jLabel15, gridBagConstraints);

        firstnameField.setText("Enter Firstname");
        firstnameField.setMinimumSize(new java.awt.Dimension(110, 30));
        firstnameField.setPreferredSize(new java.awt.Dimension(110, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 35, 0, 35);
        jPanel4.add(firstnameField, gridBagConstraints);

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel16.setText("Surname");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 35, 0, 0);
        jPanel4.add(jLabel16, gridBagConstraints);

        surnameField.setText("Enter Surname");
        surnameField.setMinimumSize(new java.awt.Dimension(110, 30));
        surnameField.setPreferredSize(new java.awt.Dimension(110, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(5, 35, 0, 0);
        jPanel4.add(surnameField, gridBagConstraints);

        saveChangesBtn.setText("Save Changes");
        saveChangesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveChangesBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(51, 0, 16, 0);
        jPanel4.add(saveChangesBtn, gridBagConstraints);

        jLabel17.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel17.setText("MemberID");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 35);
        jPanel4.add(jLabel17, gridBagConstraints);

        memberIdField.setEditable(false);
        memberIdField.setText("Member ID");
        memberIdField.setMinimumSize(new java.awt.Dimension(110, 30));
        memberIdField.setPreferredSize(new java.awt.Dimension(110, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 35);
        jPanel4.add(memberIdField, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel1.add(jPanel4, gridBagConstraints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void clearSearchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSearchBtnActionPerformed
        fillMemberTable(system.getAllMembers());
        searchEntry.setText("Enter Search");
    }//GEN-LAST:event_clearSearchBtnActionPerformed

    private void goBackBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goBackBtnActionPerformed
        dispose();
        new StaffDashboard(system).setVisible(true);
    }//GEN-LAST:event_goBackBtnActionPerformed

    private void sortByComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortByComboActionPerformed
        applySortOnMembers();
    }//GEN-LAST:event_sortByComboActionPerformed

    private void searchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchBtnActionPerformed
        performSearch();
    }//GEN-LAST:event_searchBtnActionPerformed

    private void searchEntryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchEntryMouseClicked
        if (searchEntry.getText().equals("Enter Search")) {
            searchEntry.setText("");
        }
    }//GEN-LAST:event_searchEntryMouseClicked

    private void searchEntryKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchEntryKeyTyped
        if (searchEntry.getText().equals("Enter Search")) {
            searchEntry.setText("");
        }
    }//GEN-LAST:event_searchEntryKeyTyped

    private void searchEntryFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_searchEntryFocusLost
        if (searchEntry.getText().equals("")) {
            searchEntry.setText("Enter Search");
        }
    }//GEN-LAST:event_searchEntryFocusLost

    private void deleteUserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteUserBtnActionPerformed
        deleteSelectedMember();
    }//GEN-LAST:event_deleteUserBtnActionPerformed

    private void memberTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_memberTableKeyReleased
        tableTraversalLoad();
    }//GEN-LAST:event_memberTableKeyReleased

    private void saveChangesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveChangesBtnActionPerformed
        saveMemberChanges();
    }//GEN-LAST:event_saveChangesBtnActionPerformed

    private void addMemberBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addMemberBtnActionPerformed
        addMemberHandler();
    }//GEN-LAST:event_addMemberBtnActionPerformed

    private void newMemberFirstnameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_newMemberFirstnameKeyTyped
        if (newMemberFirstname.getText().equals("Enter Firstname")) {
            newMemberFirstname.setText("");
        }
    }//GEN-LAST:event_newMemberFirstnameKeyTyped

    private void newMemberFirstnameMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newMemberFirstnameMouseClicked
        if (newMemberFirstname.getText().equals("Enter Firstname")) {
            newMemberFirstname.setText("");
        }
    }//GEN-LAST:event_newMemberFirstnameMouseClicked

    private void newMemberFirstnameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_newMemberFirstnameFocusLost
        if (newMemberFirstname.getText().equals("")) {
            newMemberFirstname.setText("Enter Firstname");
        }
    }//GEN-LAST:event_newMemberFirstnameFocusLost

    private void newMemberSurnameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_newMemberSurnameKeyTyped
        if (newMemberSurname.getText().equals("Enter Surname")) {
            newMemberSurname.setText("");
        }
    }//GEN-LAST:event_newMemberSurnameKeyTyped

    private void newMemberSurnameMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newMemberSurnameMouseClicked
        if (newMemberSurname.getText().equals("Enter Surname")) {
            newMemberSurname.setText("");
        }
    }//GEN-LAST:event_newMemberSurnameMouseClicked

    private void newMemberSurnameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_newMemberSurnameFocusLost
        if (newMemberSurname.getText().equals("")) {
            newMemberSurname.setText("Enter Surname");
        }
    }//GEN-LAST:event_newMemberSurnameFocusLost

    private void memberTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_memberTableMouseReleased
        tableTraversalLoad();
    }//GEN-LAST:event_memberTableMouseReleased

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addMemberBtn;
    private javax.swing.JButton clearSearchBtn;
    private javax.swing.JButton deleteUserBtn;
    private javax.swing.JTextField firstnameField;
    private javax.swing.JButton goBackBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField memberIdField;
    private javax.swing.JTable memberTable;
    private javax.swing.JTextField newMemberFirstname;
    private javax.swing.JTextField newMemberSurname;
    private javax.swing.JButton saveChangesBtn;
    private javax.swing.JButton searchBtn;
    private javax.swing.JComboBox<String> searchByCombo;
    private javax.swing.JTextField searchEntry;
    private javax.swing.JComboBox<String> sortByCombo;
    private javax.swing.JTextField surnameField;
    // End of variables declaration//GEN-END:variables
}
