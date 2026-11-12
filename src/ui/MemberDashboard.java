package ui;

import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import model.*;
import service.LibrarySystem;

/**
 * Member dashboard window.
 * Provides a graphical interface for members to:
 * - Browse and search books
 * - Checkout and return books
 * - View popular books
 * - Manage reservations and waitlists
 * - Track fines
 *
 * @author alex pollard - s2342835
 */

public class MemberDashboard extends javax.swing.JFrame {
    
    // ------------------- Fields -------------------
    private final LibrarySystem SYSTEM;     // central library system reference
    private final Member member;            // currently logged-in member
    
    private DefaultTableModel browseModel;      // table model for browsing books
    private DefaultTableModel borrowedModel;    // tables model for borrowed books
    private TableRowSorter<TableModel> sorter;  // sorter for sorting the browsing table

    // ------------------- Constructor ---------------------
    /**
     * Constructs the MemberDashboard form
     * Initialises UI components, sets welcome labels,
     * and populates tables and fines panel
     */
    public MemberDashboard(LibrarySystem system, Member member) {
        this.SYSTEM = system;
        this.member = member;
        
        initComponents();
        
        // Display welcome message and member ID
        welcomeLabel.setText("Welcome, " + member.getFullName());
        idLabel.setText("Member ID: " + member.getMemberId());
        
        // Initialise table models and sorters
        browseModel = (DefaultTableModel) browseTable.getModel();
        sorter = new TableRowSorter<>(browseModel);
        browseTable.setRowSorter(sorter);
        borrowedModel = (DefaultTableModel) borrowedTable.getModel();
        
        
        // Populate tables and fines panel
        fillTable(new ArrayList<>(system.getAllBooks()));
        updateFinesPanel();
        fillBorrowedTable();
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
        String type = (String) searchTypeCombo.getSelectedItem();
        String query = searchEntry.getText().trim();
        
        // Performs the search if the entry has been changed from the default
        if (!query.equals("Enter Search")) {
            List<Book> results;
        
            // Tries to find by prefix first
            if (!query.isEmpty() && (type.equals("Title") || type.equals("Author") || type.equals("Genre"))) {
                results = SYSTEM.searchByPrefix(type, query);

                if (results.isEmpty()) {
                    // fallback to exact-key index lookup (lowercased key match)
                    results = SYSTEM.searchBooks(type, query);
                }
            } else {
                results = SYSTEM.searchBooks(type, query);
            }

            fillTable(results);
        } else {
            JOptionPane.showMessageDialog(this, "Must enter search query.");
            fillTable(new ArrayList<>(SYSTEM.getAllBooks()));
        }
    }
    
    /** Displays popular books borrowed in the last 2 weeks. */
    private void viewPopular() {
        LocalDate cutoff = LocalDate.now().minusWeeks(2);
        List<Book> popular = SYSTEM.getPopularBooksSince(cutoff, 10);
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
        int viewRow = browseTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to checkout.");
            return;
        }
        int modelRow = browseTable.convertRowIndexToModel(viewRow);
        String isbn = String.valueOf(browseModel.getValueAt(modelRow, 0));
        String title = String.valueOf(browseModel.getValueAt(modelRow, 1));
        
        // Attempts to checkout book for current member using the service package
        boolean ok = SYSTEM.checkoutBook(member.getMemberId(), isbn, 2);
        // Book is checked out if return is true
        if (ok){
            JOptionPane.showMessageDialog(this,
                    "You have checked out \"" + title + "\" for 2 weeks.");
            fillBorrowedTable();
        } 
        // Return is false, provides reasoning for why it can't be checked out
        else{ 
            Book selectedBook = SYSTEM.getBookByISBN(isbn);
            
            boolean alreadyBorrowed = member.getLoans().stream()
                    .anyMatch(l -> l.getIsbn().equals(isbn));
            
            boolean hasQueue = SYSTEM.hasReservationQueue(isbn);
            boolean isFirst = SYSTEM.isFirstInQueue(member.getMemberId(), isbn);
            
            // If member already has the book
            if (alreadyBorrowed) {
                JOptionPane.showMessageDialog(this, "Checkout failed. You already have"
                        + " this book on loan.");
            }   
            
            // If the queue exists and the member is not first
            
            else if (hasQueue && !isFirst) {
                int choice = JOptionPane.showConfirmDialog(
                        this, 
                        "There is an existing reservation queue for \"" + title + "\".\n" + 
                        "Would you like to join the queue?", 
                        "Reservation", 
                        JOptionPane.YES_NO_OPTION 
                ); 

                if (choice == JOptionPane.YES_OPTION) {
                    boolean reserved = SYSTEM.reserveBook(member.getMemberId(), isbn);
                    if (!reserved) {
                        JOptionPane.showMessageDialog(this, "You are already in the reservation queue for this book.");
                    } else {
                        JOptionPane.showMessageDialog(this, "You have been added to the reservation queue for \"" + title + "\"."); 
                    }
                }
            }
            
            // No copies available
            else if (selectedBook != null && selectedBook.getAvailableCopies() ==0) {
                int choice = JOptionPane.showConfirmDialog(
                        this, 
                        "There are currently no copies of \"" + title + "\" available.\n" +
                                "Would you like to join the reservation queue?",
                        "Reservation",
                        JOptionPane.YES_NO_OPTION
                );
                // If they have chosen to choice reservaion queue, it checks if they are already in
                // If not, they are added to the queue
                if (choice == JOptionPane.YES_OPTION) {
                    boolean reserved = SYSTEM.reserveBook(member.getMemberId(), isbn);
                    if (!reserved){
                        JOptionPane.showMessageDialog(this,
                                "You are already in the reservation queue for this book.");
                    }else{
                        JOptionPane.showMessageDialog(this,
                                "You have been added to the reservation queue for \"" + title + "\".");
                    }
                }
            } else{
                JOptionPane.showMessageDialog(this,
                        "Checkout failed. You may already have this book.");
            }
        }
        // When checkout complete, table is refilled with default data.
        fillTable(SYSTEM.getAllBooks()); // refresh stock
        searchEntry.setText("Enter Search");
    }
    
    /** Handles return of the selected book. */
    private void returnSelected() {
        // Gets selected book from borrowed table
        // If no selection, message presented asking to select a book
        int viewRow = borrowedTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to return.");
            return;
        }
        
        int modelRow = borrowedTable.convertRowIndexToModel(viewRow);
                
        String isbn = String.valueOf(borrowedModel.getValueAt(modelRow, 0));

        // Asks if it was damaged in option pane
        int damaged = JOptionPane.showConfirmDialog(
                this,
                "Is the book in similar condition to when checked out?\n£5 damage fee if not.",
                "Damage check",
                JOptionPane.YES_NO_OPTION
        );
        // Attempts to return book, informs member of their fees, or informs them the return was failed
        boolean ok = SYSTEM.returnBook(member.getMemberId(), isbn, damaged == JOptionPane.NO_OPTION);
        JOptionPane.showMessageDialog(this, ok ? "Returned.\nPentalty charge of £0.50 per day overdue." : "Return failed.");

        // Refresh table and fines panel
        fillTable(SYSTEM.getAllBooks());
        updateFinesPanel();
        fillBorrowedTable();
    }
    
    // ------------------- Reservations / Waitlist -------------------
    
    /** Displays the member's current reservation queues and positions */
    private void viewWaitlist() {
        Map<Book, Integer> waitlist = SYSTEM.getWaitlistForMember(member.getMemberId());
        if (waitlist.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You are not currently in any reservation queues.");
            return;
        }
        
        StringBuilder sb = new StringBuilder("You are in the following reservation queues:\n\n");
        for (Map.Entry<Book, Integer> entry : waitlist.entrySet()) {
            Book b = entry.getKey();
            int pos = entry.getValue();
            sb.append("\"").append(b.getTitle()).append("\" (Position ").append(pos).append(")\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString());
    }
    
    // ------------------- Borrowed Books & Fines -------------------
    
    /** Fills the borrowed table with the member's current loans. */
    private void fillBorrowedTable() {
        borrowedModel.setRowCount(0);
        for(var loan : member.getLoans()) {
            Book currentBook = SYSTEM.getBookByISBN(loan.getIsbn());
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

    /** Updates the fines panel with members current total fines, broken down by overdue and damage */
    private void updateFinesPanel() {
        overdueLabel.setText(String.format("£%.2f", member.getOverdueFines()));
        damageLabel.setText(String.format("£%.2f", member.getDamageFines()));
        totalLabel.setText(String.format("£%.2f", member.getTotalFines()));
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
        titleLabel = new javax.swing.JLabel();
        idLabel = new javax.swing.JLabel();
        welcomeLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        searchTypeCombo = new javax.swing.JComboBox<>();
        searchEntry = new javax.swing.JTextField();
        sortByCombo = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        browseTable = new javax.swing.JTable();
        checkoutBtn = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        borrowedTable = new javax.swing.JTable();
        returnSelectedBtn = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        overdueLabel = new javax.swing.JLabel();
        damageLabel = new javax.swing.JLabel();
        totalLabel = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        viewPopularBtn = new javax.swing.JButton();
        bookSearchBtn = new javax.swing.JButton();
        clearBtn = new javax.swing.JButton();
        signOutBtn = new javax.swing.JButton();
        viewWaitlistBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1175, 635));

        jPanel1.setBackground(new java.awt.Color(246, 246, 246));
        jPanel1.setMinimumSize(new java.awt.Dimension(1175, 635));
        jPanel1.setPreferredSize(new java.awt.Dimension(1175, 635));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        titleLabel.setFont(new java.awt.Font("Tw Cen MT Condensed Extra Bold", 0, 36)); // NOI18N
        titleLabel.setForeground(new java.awt.Color(255, 255, 255));
        titleLabel.setText("Greenfields Community Library Dashboard");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_START;
        gridBagConstraints.insets = new java.awt.Insets(29, 0, 0, 0);
        jPanel1.add(titleLabel, gridBagConstraints);

        idLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        idLabel.setForeground(new java.awt.Color(255, 255, 255));
        idLabel.setText("MemberID");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 7, 15);
        jPanel1.add(idLabel, gridBagConstraints);

        welcomeLabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        welcomeLabel.setForeground(new java.awt.Color(255, 255, 255));
        welcomeLabel.setText("Welcome, Firstname Surname");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        jPanel1.add(welcomeLabel, gridBagConstraints);

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

        searchTypeCombo.setMaximumRowCount(4);
        searchTypeCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Title", "Author", "Genre", "ISBN" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(28, 50, 0, 6);
        jPanel1.add(searchTypeCombo, gridBagConstraints);

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

        browseTable.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        browseTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
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
        browseTable.setMinimumSize(new java.awt.Dimension(475, 380));
        browseTable.setRowHeight(25);
        browseTable.setRowMargin(2);
        browseTable.setSelectionBackground(new java.awt.Color(51, 153, 255));
        browseTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        browseTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        browseTable.setShowHorizontalLines(true);
        browseTable.setShowVerticalLines(true);
        browseTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(browseTable);
        if (browseTable.getColumnModel().getColumnCount() > 0) {
            browseTable.getColumnModel().getColumn(0).setResizable(false);
            browseTable.getColumnModel().getColumn(0).setPreferredWidth(75);
            browseTable.getColumnModel().getColumn(1).setResizable(false);
            browseTable.getColumnModel().getColumn(1).setPreferredWidth(125);
            browseTable.getColumnModel().getColumn(2).setResizable(false);
            browseTable.getColumnModel().getColumn(2).setPreferredWidth(125);
            browseTable.getColumnModel().getColumn(3).setResizable(false);
            browseTable.getColumnModel().getColumn(3).setPreferredWidth(100);
            browseTable.getColumnModel().getColumn(4).setResizable(false);
            browseTable.getColumnModel().getColumn(4).setPreferredWidth(50);
            browseTable.getColumnModel().getColumn(5).setResizable(false);
            browseTable.getColumnModel().getColumn(5).setPreferredWidth(50);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(10, 50, 0, 0);
        jPanel1.add(jScrollPane1, gridBagConstraints);

        checkoutBtn.setText("Checkout Selected Book");
        checkoutBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkoutBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(20, 50, 20, 0);
        jPanel1.add(checkoutBtn, gridBagConstraints);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel3.setMinimumSize(new java.awt.Dimension(475, 535));
        jPanel3.setPreferredSize(new java.awt.Dimension(475, 535));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 153, 153));
        jLabel3.setText("Your Info");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 17, 0);
        jPanel3.add(jLabel3, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setText("Borrowed Books:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 7, 0);
        jPanel3.add(jLabel4, gridBagConstraints);

        jScrollPane2.setMinimumSize(new java.awt.Dimension(370, 150));
        jScrollPane2.setPreferredSize(new java.awt.Dimension(370, 150));

        borrowedTable.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        borrowedTable.setModel(new javax.swing.table.DefaultTableModel(
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
                {null, null, null, null}
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
        borrowedTable.setMinimumSize(new java.awt.Dimension(365, 150));
        borrowedTable.setRowHeight(25);
        borrowedTable.setRowMargin(2);
        borrowedTable.setSelectionBackground(new java.awt.Color(51, 153, 255));
        borrowedTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        borrowedTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        borrowedTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        borrowedTable.setShowHorizontalLines(true);
        borrowedTable.setShowVerticalLines(true);
        borrowedTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(borrowedTable);
        if (borrowedTable.getColumnModel().getColumnCount() > 0) {
            borrowedTable.getColumnModel().getColumn(0).setResizable(false);
            borrowedTable.getColumnModel().getColumn(0).setPreferredWidth(75);
            borrowedTable.getColumnModel().getColumn(1).setResizable(false);
            borrowedTable.getColumnModel().getColumn(1).setPreferredWidth(110);
            borrowedTable.getColumnModel().getColumn(2).setResizable(false);
            borrowedTable.getColumnModel().getColumn(2).setPreferredWidth(110);
            borrowedTable.getColumnModel().getColumn(3).setResizable(false);
            borrowedTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        jPanel3.add(jScrollPane2, gridBagConstraints);

        returnSelectedBtn.setText("Return Selected Book");
        returnSelectedBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                returnSelectedBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        jPanel3.add(returnSelectedBtn, gridBagConstraints);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Fees:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel3.add(jLabel5, gridBagConstraints);

        jLabel6.setText("Outstanding Fees:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 56, 5, 0);
        jPanel3.add(jLabel6, gridBagConstraints);

        jLabel7.setText("Breakdown:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(0, 60, 0, 0);
        jPanel3.add(jLabel7, gridBagConstraints);

        jLabel8.setText("Overdue:");
        jLabel8.setPreferredSize(new java.awt.Dimension(54, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 94, 0, 0);
        jPanel3.add(jLabel8, gridBagConstraints);

        jLabel9.setText("Damaged:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(0, 94, 0, 0);
        jPanel3.add(jLabel9, gridBagConstraints);

        overdueLabel.setText("jLabel10");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 51);
        jPanel3.add(overdueLabel, gridBagConstraints);

        damageLabel.setText("jLabel11");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 51);
        jPanel3.add(damageLabel, gridBagConstraints);

        totalLabel.setText("jLabel12");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 51);
        jPanel3.add(totalLabel, gridBagConstraints);

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 0, 0));
        jLabel13.setText("Please see librarian to pay");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 41, 0);
        jPanel3.add(jLabel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel1.add(jPanel3, gridBagConstraints);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setText("Book Browser");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(30, 50, 0, 0);
        jPanel1.add(jLabel14, gridBagConstraints);

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

        bookSearchBtn.setText("Search");
        bookSearchBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bookSearchBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(bookSearchBtn, gridBagConstraints);

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

        signOutBtn.setText("Sign Out");
        signOutBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                signOutBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        jPanel1.add(signOutBtn, gridBagConstraints);

        viewWaitlistBtn.setText("View My Waitlist");
        viewWaitlistBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewWaitlistBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 25, 0);
        jPanel1.add(viewWaitlistBtn, gridBagConstraints);

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
        fillTable(new ArrayList<>(SYSTEM.getAllBooks()));
        searchEntry.setText("Enter Search");
    }//GEN-LAST:event_clearBtnActionPerformed

    private void signOutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_signOutBtnActionPerformed
        // Close current dashboard
        dispose();

        // Return to login screen
           new Login(SYSTEM).setVisible(true);
    }//GEN-LAST:event_signOutBtnActionPerformed

    private void bookSearchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bookSearchBtnActionPerformed
        performSearch();
    }//GEN-LAST:event_bookSearchBtnActionPerformed

    private void viewPopularBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewPopularBtnActionPerformed
        viewPopular();
    }//GEN-LAST:event_viewPopularBtnActionPerformed

    private void checkoutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkoutBtnActionPerformed
        checkoutSelected();
    }//GEN-LAST:event_checkoutBtnActionPerformed

    private void returnSelectedBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_returnSelectedBtnActionPerformed
        returnSelected();
    }//GEN-LAST:event_returnSelectedBtnActionPerformed

    private void viewWaitlistBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewWaitlistBtnActionPerformed
        viewWaitlist();
    }//GEN-LAST:event_viewWaitlistBtnActionPerformed

    private void sortByComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sortByComboActionPerformed
        applySortOnBrowse();
    }//GEN-LAST:event_sortByComboActionPerformed

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

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bookSearchBtn;
    private javax.swing.JTable borrowedTable;
    private javax.swing.JTable browseTable;
    private javax.swing.JButton checkoutBtn;
    private javax.swing.JButton clearBtn;
    private javax.swing.JLabel damageLabel;
    private javax.swing.JLabel idLabel;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
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
    private javax.swing.JLabel overdueLabel;
    private javax.swing.JButton returnSelectedBtn;
    private javax.swing.JTextField searchEntry;
    private javax.swing.JComboBox<String> searchTypeCombo;
    private javax.swing.JButton signOutBtn;
    private javax.swing.JComboBox<String> sortByCombo;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JLabel totalLabel;
    private javax.swing.JButton viewPopularBtn;
    private javax.swing.JButton viewWaitlistBtn;
    private javax.swing.JLabel welcomeLabel;
    // End of variables declaration//GEN-END:variables
}
