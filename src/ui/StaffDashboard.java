package ui;

import java.time.LocalDate;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import model.*;
import service.LibrarySystem;

/**
 * Staff dashboard window.
 * Provides a graphical interface for members to:
 * - Browse and search books
 * - Checkout and return books
 * - View popular books
 * - Manage reservations and waitlists
 * - Track fines
 * - Open Book Manager
 * - Open User Manager
 *
 * @author alex pollard - s2342835
 */

public class StaffDashboard extends javax.swing.JFrame {
    
    // ------------------- Fields -------------------
    private LibrarySystem system;       // central library system reference
    
    private DefaultTableModel browseModel;      // table model for browsing books
    private DefaultTableModel borrowedModel;    // tables model for borrowed books
    private TableRowSorter<TableModel> sorter;  // sorter for sorting the browsing table

    // ------------------- Constructor ---------------------
    /**
     * Constructs the StaffDashboard form
     * Initialises UI components
     */
    public StaffDashboard(LibrarySystem system) {
        this.system = system;
        initComponents();
        
        // Initialise table models
        browseModel = (DefaultTableModel) bookInvTable.getModel();
        sorter = new TableRowSorter<>(browseModel);
        bookInvTable.setRowSorter(sorter);
        borrowedModel = (DefaultTableModel) borrowedBooksTable.getModel();
        
        // Populates the browsing table
        fillTable(new ArrayList<>(system.getAllBooks()));
    }
    
    // ------------------- Browse / Search / Popular -------------------
    
    /** Fills the browse table with a collection of books. */
    private void fillTable(Collection<Book> books) {
        browseModel.setRowCount(0);
        // Iterates through all books
        for (Book currentBook : books) {
            int inStock = currentBook.getAvailableCopies();
            int loaned = currentBook.getBorrowCount();
            browseModel.addRow(new Object[] {
                currentBook.getISBN(),
                currentBook.getTitle(),
                currentBook.getAuthor(),
                currentBook.getGenre(),
                inStock,
                loaned
            });
        }
        // Sorts the books by the selected option
        applySortOnBrowse();
    }
    
    /** Performs a search based on user input and selected type. */
    private void performSearch() {
        String type = (String) bookSearchTypeCombo.getSelectedItem();
        String query = bookSearchEntry.getText().trim();
        
        // Performs the search if the entry has been changed from the default
        if (!query.equals("Enter Search")) {
            List<Book> results;
        
            // Tries to find by prefix first
            if (!query.isEmpty() && (type.equals("Title") || type.equals("Author") || type.equals("Genre"))) {
                results = system.searchByPrefix(type, query);
                if (results.isEmpty()) {
                    // fallback to exact-key index lookup (lowercased key match)
                    results = system.searchBooks(type, query);
                }
            } else {
                results = system.searchBooks(type, query);
            }

            fillTable(results);
        } else {
            JOptionPane.showMessageDialog(this, "Must enter search query.");
            fillTable(new ArrayList<>(system.getAllBooks()));
        }
        
    }
    
    /** Displays popular books borrowed in the last 4 weeks. */
    private void viewPopular() {
        LocalDate cutoff = LocalDate.now().minusWeeks(4);
        List<Book> popular = system.getPopularBooksSince(cutoff, 10);
        fillTable(popular);
    }
    
    /** Applies sorting to the browse table based on the selected criteria. */
    private void applySortOnBrowse() {
        String sort = (String) sortByCombo.getSelectedItem();
        int columnIndex;
        
        switch (sort) {
            case "Author":
                columnIndex = 2;
                break;
            case "Genre":
                columnIndex = 3;
                break;
            case "Title":
            default: 
                columnIndex = 1;
                break;
        }
        
        sorter.setSortKeys(List.of(new RowSorter.SortKey(columnIndex, SortOrder.ASCENDING)));
    }
    
    
    // ------------------- Checkout & Return -------------------
    
    /** Handles checkout of the selected book. */
    private void checkoutSelected() {
        // Gets the selected row from the table
        // Error message if there isn't a selection
        int viewRow = bookInvTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to checkout.");
            return;
        } 
        
        int modelRow = bookInvTable.convertRowIndexToModel(viewRow);
        
        String isbn = String.valueOf(browseModel.getValueAt(modelRow, 0));
        String title = String.valueOf(browseModel.getValueAt(modelRow, 1));
        
        // Prompts member ID to be entered to checkout the book for a member
        String memberIdStr = JOptionPane.showInputDialog(this,
                "Enter Member ID to checkout \"" + title + "\" for:");
        if (memberIdStr == null || memberIdStr.trim().isEmpty()) return;
        
        try {
            int memberId = Integer.parseInt(memberIdStr.trim());
            // Attempts to checkout book for selected member using the service package
            boolean ok = system.checkoutBook(memberId, isbn, 2);
            // Book is checked out if return is true
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Book \"" + title + "\" checked out to Member " + memberId + " for 2 weeks.");
                fillTable(system.getAllBooks());
            }
            // Return is false, provides reasoning for why it can't be checked out
            else {
                Book selectedBook = system.getBookByISBN(isbn);
                
                Member member = system.loginMember(memberId);
                boolean alreadyBorrowed = member.getLoans().stream()
                        .anyMatch(l -> l.getIsbn().equals(isbn));
                
                boolean hasQueue = system.hasReservationQueue(isbn);
                boolean isFirst = system.isFirstInQueue(memberId, isbn);
                
                // If member already has the book
                if (alreadyBorrowed) {
                    JOptionPane.showMessageDialog(this, "Checkout failed. Member " + memberId + 
                            "already has this book on loan.");
                }   
                
                // If the queue exists and the member is not first

                else if (hasQueue && !isFirst) {
                    int choice = JOptionPane.showConfirmDialog(
                            this, 
                            "There is an existing reservation queue for \"" + title + "\".\n" + 
                            "Would Member " + memberId + " like to join the queue?", 
                            "Reservation", 
                            JOptionPane.YES_NO_OPTION 
                    ); 

                    if (choice == JOptionPane.YES_OPTION) {
                        boolean reserved = system.reserveBook(memberId, isbn);
                        if (!reserved) {
                            JOptionPane.showMessageDialog(this, "Member " + memberId + " is already in the reservation queue for this book.");
                        } else {
                            JOptionPane.showMessageDialog(this, "Member " + memberId + " has been added to the reservation queue for \"" + title + "\"."); 
                        }
                    }
                }
                
                // No copies available
                else if (selectedBook != null && selectedBook.getAvailableCopies() ==0) {
                    int choice = JOptionPane.showConfirmDialog(
                            this, 
                            "The are currently no copies of \"" + title + "\" available.\n" +
                                    "Would Member " + memberId + " like to join the reservation queue?",
                            "Reservation",
                            JOptionPane.YES_NO_OPTION
                    );
                    // If they have chosen to choice reservaion queue, it checks if they are already in
                    // If not, they are added to the queue
                    if (choice == JOptionPane.YES_OPTION) {
                        boolean reserved = system.reserveBook(member.getMemberId(), isbn);
                        if (!reserved){
                            JOptionPane.showMessageDialog(this,
                                    "Member " + memberId + " is already in the reservation queue for this book.");
                        }else{
                            JOptionPane.showMessageDialog(this,
                                    "Member " + memberId + " has been added to the reservation queue for \"" + title + "\".");
                        }
                    }
                } else{
                    JOptionPane.showMessageDialog(this,
                            "Checkout failed. Member mau already have this book.");
                }
            }
        } catch (NumberFormatException ex) {
            // If the member ID entered was invalid, error message is shown
            JOptionPane.showMessageDialog(this, "Invalid Member ID.");
        }
    }
    
    /** Handles return of the selected book. */
    private void returnSelected() {
        // Gets selected book from borrowed table
        // If no selection, message presented asking to select a book
        int viewRow = borrowedBooksTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to return.");
            return;
        }
        
        int modelRow = borrowedBooksTable.convertRowIndexToModel(viewRow);
        
        try {
            // Prompt to enter member ID with error message if not found.
            int memberId = Integer.parseInt(memberIdEntry.getText().trim());
            Member selectedMember = system.getMember(memberId);
            if (selectedMember == null) {
                JOptionPane.showMessageDialog(this, "Member not found.");
                return;
            }
            
            String isbn = String.valueOf(borrowedModel.getValueAt(modelRow, 0));
            
            // Asks if it was damaged in option pane
            int damaged = JOptionPane.showConfirmDialog(
                    this,
                    "Is the book in similar condition to when checked out?\n£5 damage fee if not.",
                    "Damage check",
                    JOptionPane.YES_NO_OPTION
            );
            // Attempts to return book, informs member of their fees, or informs them the return was failed
            boolean ok = system.returnBook(memberId, isbn, damaged == JOptionPane.NO_OPTION);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Book returned successfully for Member " + memberId + ".\nPenalty charge of £0.50 per day overdue.");
                // Refresh table and fines panel
                fillBorrowedTable(selectedMember);
                fillTable(system.getAllBooks());
                updateFinesPanel(selectedMember);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Return failed.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric Member ID.");
        }
    }
    
    // ------------------- Member Search and Loading -------------------
    
    /** Fills the borrowed table with the member's current loans. */
    private void fillBorrowedTable(Member selectedMember) {
        borrowedModel.setRowCount(0);
        for(var loan : selectedMember.getLoans()) {
            Book currentBook = system.getBookByISBN(loan.getIsbn());
            if (currentBook != null) {
                borrowedModel.addRow(new Object[]{
                    currentBook.getISBN(),
                    currentBook.getTitle(),
                    currentBook.getAuthor(),
                    loan.getDueDate()
                });
            }
        }
    }
    
    // Gets the entered member ID from the entry field and searches for the member
    // If member is found, their borrowed books are loaded into table
    // If not, an error message is provided for the reason they weren't found
    private void findMemberBorrowedBooks() {
        try {
            int memberId = Integer.parseInt(memberIdEntry.getText().trim());
            Member selectedMember = system.getMember(memberId);
            if (selectedMember == null) {
                JOptionPane.showMessageDialog(this, "Member not found.");
                return;
            }
            fillBorrowedTable(selectedMember);
            updateFinesPanel(selectedMember);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid numeric Member ID.");
        }
    }
    
    /** Updates the fines panel with members current total fines, broken down by overdue and damage */
    private void updateFinesPanel(Member m) {
        overdueFineField.setText(String.format("£%.2f", m.getOverdueFines()));
        damageFineField.setText(String.format("£%.2f", m.getDamageFines()));
        totalFineField.setText(String.format("£%.2f", m.getTotalFines()));
    }
    
    /** Allows the staff member to enter amount member is paying */
    private void payFines() {
        String memberIdStr = memberIdEntry.getText().trim();
        // Only allows them to pay fine if they have entered a member ID
        if (memberIdStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a Member ID first.");
            return;
        }

        try {
            // Checks member ID exists
            int memberId = Integer.parseInt(memberIdStr);
            Member selectedMember = system.getMember(memberId);
            if (selectedMember == null) {
                JOptionPane.showMessageDialog(this, "Member not found.");
                return;
            }
            
            // If they do exist, checks to see if they have any fines, retuns if not
            // after showing message thaat there is nothing to pay
            double totalFines = selectedMember.getTotalFines();
            if (totalFines <= 0) {
                JOptionPane.showMessageDialog(this, "This member has no outstanding fines.");
                return;
            }

            // Window to enter the amount being paid
            String amountStr = JOptionPane.showInputDialog(this,
                "Enter amount to pay (Total fines: £" + String.format("%.2f", totalFines) + "):");
            if (amountStr == null || amountStr.trim().isEmpty()) return;

            double amount = Double.parseDouble(amountStr.trim());
            // Attempts to pay fine using the logic in the member model
            boolean ok = selectedMember.payFine(amount);

            // Message informing it was successful with how much they still owe, or
            // failed payment failure with potential reasoning
            if (ok) {
                JOptionPane.showMessageDialog(this,
                    "Payment successful. Remaining fines: £" + String.format("%.2f", selectedMember.getTotalFines()));
            } else {
                JOptionPane.showMessageDialog(this,
                    "Payment failed. Amount must be positive and not exceed total fines.");
            }

            fillBorrowedTable(selectedMember);
            updateFinesPanel(selectedMember);
        } catch (NumberFormatException ex) {
            // Payment failed message
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter numeric values.");

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
        dashboardLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        bookSearchTypeCombo = new javax.swing.JComboBox<>();
        bookSearchEntry = new javax.swing.JTextField();
        sortByCombo = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        bookInvTable = new javax.swing.JTable();
        checkoutBookBtn = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        borrowedBooksTable = new javax.swing.JTable();
        returnMembersBookBtn = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        overdueFineField = new javax.swing.JLabel();
        damageFineField = new javax.swing.JLabel();
        totalFineField = new javax.swing.JLabel();
        memberIdEntry = new javax.swing.JTextField();
        findMemberBtn = new javax.swing.JButton();
        payFeesBtn = new javax.swing.JButton();
        bookBrowserLabel = new javax.swing.JLabel();
        viewPopularBtn = new javax.swing.JButton();
        findMemberbtn = new javax.swing.JButton();
        clearBtn = new javax.swing.JButton();
        signOutbtn = new javax.swing.JButton();
        openBookManagerBtn = new javax.swing.JButton();
        openUserManagementBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1175, 635));

        jPanel1.setBackground(new java.awt.Color(246, 246, 246));
        jPanel1.setPreferredSize(new java.awt.Dimension(1175, 635));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        dashboardLabel.setFont(new java.awt.Font("Tw Cen MT Condensed Extra Bold", 0, 36)); // NOI18N
        dashboardLabel.setForeground(new java.awt.Color(255, 255, 255));
        dashboardLabel.setText("Staff Dashboard");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(29, 0, 0, 0);
        jPanel1.add(dashboardLabel, gridBagConstraints);

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

        bookSearchTypeCombo.setMaximumRowCount(4);
        bookSearchTypeCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Title", "Author", "Genre", "ISBN" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(28, 50, 0, 6);
        jPanel1.add(bookSearchTypeCombo, gridBagConstraints);

        bookSearchEntry.setText("Enter Search");
        bookSearchEntry.setMinimumSize(new java.awt.Dimension(120, 25));
        bookSearchEntry.setPreferredSize(new java.awt.Dimension(120, 25));
        bookSearchEntry.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                bookSearchEntryFocusLost(evt);
            }
        });
        bookSearchEntry.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                bookSearchEntryMouseClicked(evt);
            }
        });
        bookSearchEntry.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                bookSearchEntryKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        jPanel1.add(bookSearchEntry, gridBagConstraints);

        sortByCombo.setMaximumRowCount(3);
        sortByCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Title", "Author", "Genre" }));
        sortByCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sortByComboActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(28, 0, 0, 0);
        jPanel1.add(sortByCombo, gridBagConstraints);

        jLabel2.setText("Sort by:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 218, 4, 0);
        jPanel1.add(jLabel2, gridBagConstraints);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(600, 300));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(600, 300));

        bookInvTable.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        bookInvTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ISBN", "Title", "Author", "Genre", "In Stock", "Loan Qty"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        bookInvTable.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        bookInvTable.setMinimumSize(new java.awt.Dimension(475, 380));
        bookInvTable.setRowHeight(25);
        bookInvTable.setRowMargin(2);
        bookInvTable.setSelectionBackground(new java.awt.Color(51, 153, 255));
        bookInvTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        bookInvTable.setShowHorizontalLines(true);
        bookInvTable.setShowVerticalLines(true);
        jScrollPane1.setViewportView(bookInvTable);
        if (bookInvTable.getColumnModel().getColumnCount() > 0) {
            bookInvTable.getColumnModel().getColumn(0).setResizable(false);
            bookInvTable.getColumnModel().getColumn(0).setPreferredWidth(75);
            bookInvTable.getColumnModel().getColumn(1).setResizable(false);
            bookInvTable.getColumnModel().getColumn(1).setPreferredWidth(125);
            bookInvTable.getColumnModel().getColumn(2).setResizable(false);
            bookInvTable.getColumnModel().getColumn(2).setPreferredWidth(125);
            bookInvTable.getColumnModel().getColumn(3).setResizable(false);
            bookInvTable.getColumnModel().getColumn(3).setPreferredWidth(100);
            bookInvTable.getColumnModel().getColumn(4).setResizable(false);
            bookInvTable.getColumnModel().getColumn(4).setPreferredWidth(50);
            bookInvTable.getColumnModel().getColumn(5).setResizable(false);
            bookInvTable.getColumnModel().getColumn(5).setPreferredWidth(50);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(10, 50, 0, 0);
        jPanel1.add(jScrollPane1, gridBagConstraints);

        checkoutBookBtn.setText("Checkout Selected Book");
        checkoutBookBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkoutBookBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(19, 50, 8, 0);
        jPanel1.add(checkoutBookBtn, gridBagConstraints);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel3.setMinimumSize(new java.awt.Dimension(475, 535));
        jPanel3.setPreferredSize(new java.awt.Dimension(475, 535));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 153, 153));
        jLabel3.setText("User Info");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        jPanel3.add(jLabel3, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("Borrowed Books:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 7, 0);
        jPanel3.add(jLabel4, gridBagConstraints);

        jScrollPane2.setMinimumSize(new java.awt.Dimension(370, 150));
        jScrollPane2.setPreferredSize(new java.awt.Dimension(370, 150));

        borrowedBooksTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ISBN", "Title", "Author", "Date Due"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        borrowedBooksTable.setMinimumSize(new java.awt.Dimension(300, 150));
        borrowedBooksTable.setRowHeight(25);
        borrowedBooksTable.setRowMargin(2);
        borrowedBooksTable.setSelectionBackground(new java.awt.Color(51, 153, 255));
        borrowedBooksTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        borrowedBooksTable.setShowHorizontalLines(true);
        borrowedBooksTable.setShowVerticalLines(true);
        jScrollPane2.setViewportView(borrowedBooksTable);
        if (borrowedBooksTable.getColumnModel().getColumnCount() > 0) {
            borrowedBooksTable.getColumnModel().getColumn(0).setResizable(false);
            borrowedBooksTable.getColumnModel().getColumn(0).setPreferredWidth(75);
            borrowedBooksTable.getColumnModel().getColumn(1).setResizable(false);
            borrowedBooksTable.getColumnModel().getColumn(1).setPreferredWidth(110);
            borrowedBooksTable.getColumnModel().getColumn(2).setResizable(false);
            borrowedBooksTable.getColumnModel().getColumn(2).setPreferredWidth(110);
            borrowedBooksTable.getColumnModel().getColumn(3).setResizable(false);
            borrowedBooksTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        jPanel3.add(jScrollPane2, gridBagConstraints);

        returnMembersBookBtn.setText("Return Selected Book");
        returnMembersBookBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                returnMembersBookBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        jPanel3.add(returnMembersBookBtn, gridBagConstraints);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Fees:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel3.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Outstanding Fees:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 56, 5, 0);
        jPanel3.add(jLabel6, gridBagConstraints);

        jLabel7.setText("Breakdown:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(0, 60, 0, 0);
        jPanel3.add(jLabel7, gridBagConstraints);

        jLabel8.setText("Overdue:");
        jLabel8.setPreferredSize(new java.awt.Dimension(54, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 94, 0, 0);
        jPanel3.add(jLabel8, gridBagConstraints);

        jLabel9.setText("Damaged:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 94, 0, 0);
        jPanel3.add(jLabel9, gridBagConstraints);

        overdueFineField.setText("£0.00");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 51);
        jPanel3.add(overdueFineField, gridBagConstraints);

        damageFineField.setText("£0.00");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 51);
        jPanel3.add(damageFineField, gridBagConstraints);

        totalFineField.setText("£0.00");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 51);
        jPanel3.add(totalFineField, gridBagConstraints);

        memberIdEntry.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        memberIdEntry.setText("Enter MemberID");
        memberIdEntry.setMinimumSize(new java.awt.Dimension(100, 25));
        memberIdEntry.setPreferredSize(new java.awt.Dimension(100, 25));
        memberIdEntry.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                memberIdEntryMouseClicked(evt);
            }
        });
        memberIdEntry.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                memberIdEntryKeyTyped(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 2, 0);
        jPanel3.add(memberIdEntry, gridBagConstraints);

        findMemberBtn.setText("Find");
        findMemberBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findMemberBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel3.add(findMemberBtn, gridBagConstraints);

        payFeesBtn.setText("Pay Fees");
        payFeesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                payFeesBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 14, 0);
        jPanel3.add(payFeesBtn, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel1.add(jPanel3, gridBagConstraints);

        bookBrowserLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        bookBrowserLabel.setText("Book Browser");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(30, 50, 0, 0);
        jPanel1.add(bookBrowserLabel, gridBagConstraints);

        viewPopularBtn.setText("View Popular");
        viewPopularBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewPopularBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 58, 0, 0);
        jPanel1.add(viewPopularBtn, gridBagConstraints);

        findMemberbtn.setText("Search");
        findMemberbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findMemberbtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(findMemberbtn, gridBagConstraints);

        clearBtn.setText("Clear");
        clearBtn.setMargin(new java.awt.Insets(2, 1, 3, 1));
        clearBtn.setMaximumSize(new java.awt.Dimension(60, 19));
        clearBtn.setMinimumSize(new java.awt.Dimension(60, 19));
        clearBtn.setPreferredSize(new java.awt.Dimension(60, 19));
        clearBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 0, 0);
        jPanel1.add(clearBtn, gridBagConstraints);

        signOutbtn.setText("Sign Out");
        signOutbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                signOutbtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        jPanel1.add(signOutbtn, gridBagConstraints);

        openBookManagerBtn.setText("Manage Books");
        openBookManagerBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openBookManagerBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 170, 0, 0);
        jPanel1.add(openBookManagerBtn, gridBagConstraints);

        openUserManagementBtn.setText("Manage Staff / Members");
        openUserManagementBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openUserManagementBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 120);
        jPanel1.add(openUserManagementBtn, gridBagConstraints);

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

    private void clearBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearBtnActionPerformed
        fillTable(new ArrayList<>(system.getAllBooks()));
        bookSearchEntry.setText("Enter Search");
    }//GEN-LAST:event_clearBtnActionPerformed

    private void signOutbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_signOutbtnActionPerformed
        // Close current dashboard
        dispose();

        // Return to login screen
        new Login(system).setVisible(true);
    }//GEN-LAST:event_signOutbtnActionPerformed

    private void memberIdEntryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_memberIdEntryMouseClicked
        memberIdEntry.setText("");
        borrowedModel.setRowCount(0);
        totalFineField.setText("£0.00");
        overdueFineField.setText("£0.00");
        damageFineField.setText("£0.00");
    }//GEN-LAST:event_memberIdEntryMouseClicked

    private void memberIdEntryKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_memberIdEntryKeyTyped
        if (memberIdEntry.getText().equals("Enter MemberID")) {
            memberIdEntry.setText("");
        }
    }//GEN-LAST:event_memberIdEntryKeyTyped

    private void openBookManagerBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openBookManagerBtnActionPerformed
        new BookManager(system).setVisible(true);
            dispose();
    }//GEN-LAST:event_openBookManagerBtnActionPerformed

    private void openUserManagementBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openUserManagementBtnActionPerformed
        new UserManager(system).setVisible(true);
            dispose();
    }//GEN-LAST:event_openUserManagementBtnActionPerformed

    private void findMemberBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findMemberBtnActionPerformed
        findMemberBorrowedBooks();
    }//GEN-LAST:event_findMemberBtnActionPerformed

    private void checkoutBookBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkoutBookBtnActionPerformed
        checkoutSelected();
    }//GEN-LAST:event_checkoutBookBtnActionPerformed

    private void returnMembersBookBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_returnMembersBookBtnActionPerformed
        returnSelected();
    }//GEN-LAST:event_returnMembersBookBtnActionPerformed

    private void viewPopularBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewPopularBtnActionPerformed
        viewPopular();
    }//GEN-LAST:event_viewPopularBtnActionPerformed

    private void payFeesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_payFeesBtnActionPerformed
        payFines();
    }//GEN-LAST:event_payFeesBtnActionPerformed

    private void findMemberbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findMemberbtnActionPerformed
        performSearch();
    }//GEN-LAST:event_findMemberbtnActionPerformed

    private void sortByComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortByComboActionPerformed
        applySortOnBrowse();
    }//GEN-LAST:event_sortByComboActionPerformed

    private void bookSearchEntryKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_bookSearchEntryKeyTyped
        if (bookSearchEntry.getText().equals("Enter Search")) {
            bookSearchEntry.setText("");
        }
    }//GEN-LAST:event_bookSearchEntryKeyTyped

    private void bookSearchEntryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bookSearchEntryMouseClicked
        if (bookSearchEntry.getText().equals("Enter Search")) {
            bookSearchEntry.setText("");
        }
    }//GEN-LAST:event_bookSearchEntryMouseClicked

    private void bookSearchEntryFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_bookSearchEntryFocusLost
        if (bookSearchEntry.getText().equals("")) {
            bookSearchEntry.setText("Enter Search");
        }
    }//GEN-LAST:event_bookSearchEntryFocusLost

    /**
     * @param args the command line arguments
     */


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel bookBrowserLabel;
    private javax.swing.JTable bookInvTable;
    private javax.swing.JTextField bookSearchEntry;
    private javax.swing.JComboBox<String> bookSearchTypeCombo;
    private javax.swing.JTable borrowedBooksTable;
    private javax.swing.JButton checkoutBookBtn;
    private javax.swing.JButton clearBtn;
    private javax.swing.JLabel damageFineField;
    private javax.swing.JLabel dashboardLabel;
    private javax.swing.JButton findMemberBtn;
    private javax.swing.JButton findMemberbtn;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField memberIdEntry;
    private javax.swing.JButton openBookManagerBtn;
    private javax.swing.JButton openUserManagementBtn;
    private javax.swing.JLabel overdueFineField;
    private javax.swing.JButton payFeesBtn;
    private javax.swing.JButton returnMembersBookBtn;
    private javax.swing.JButton signOutbtn;
    private javax.swing.JComboBox<String> sortByCombo;
    private javax.swing.JLabel totalFineField;
    private javax.swing.JButton viewPopularBtn;
    // End of variables declaration//GEN-END:variables
}
