package ui;

import java.time.LocalDate;
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
import model.Book;
import service.LibrarySystem;

/**
 * Book Manager window.
 * Provides a graphical interface for staff to:
 * - Browse and search books
 * - Edit selected book info
 * - Delete books
 * - Add new books
 *
 * @author alex pollard - s2342835
 */
public class BookManager extends javax.swing.JFrame {
    
    // ------------------- Fields -------------------
    private LibrarySystem system;   // central library system reference
    
    private DefaultTableModel bookTableModel;   // table model for browsing books
    private TableRowSorter<TableModel> sorter;  // sorter for sorting the browsing table

    // ------------------- Constructor ---------------------
    /**
     * Constructs the BookManager form
     * Initialises UI components, populates tables
     */
    public BookManager(LibrarySystem system) {
        this.system = system;
        initComponents();
        
        // Initialise table model
        bookTableModel = (DefaultTableModel) bookSelectorTable.getModel();
        sorter = new TableRowSorter<>(bookTableModel);
        bookSelectorTable.setRowSorter(sorter);
        
        // Populate table
        fillBookTable(new ArrayList<>(system.getAllBooks()));
    }
    
    // ------------------- Browse / Search / Editing -------------------
    
    /** Fills the browse table with a collection of books. */
    private void fillBookTable(Collection<Book> books) {
        bookTableModel.setRowCount(0);
        // Iterates through all books
        for (Book currentBook : books) {
            int totalCopies = currentBook.getTotalCopies();
            bookTableModel.addRow(new Object[] {
                currentBook.getISBN(),
                currentBook.getTitle(),
                currentBook.getAuthor(),
                currentBook.getGenre(),
                totalCopies,
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
                results = system.searchByPrefix(type, query);
                if (results.isEmpty()) {
                    // fallback to exact-key index lookup (lowercased key match)
                    results = system.searchBooks(type, query);
                }
            } else {
                results = system.searchBooks(type, query);
            }

            fillBookTable(results);
        } else {
            JOptionPane.showMessageDialog(this, "Must enter search query.");
            fillBookTable(new ArrayList<>(system.getAllBooks()));
        }
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
    
    /** Fills the edit fields when a record is selected from table. */
    private void fillFields() {
        // Checks if a row has been selected before loading record into edit fields
        int viewRow = bookSelectorTable.getSelectedRow();
        if (viewRow != -1){
            int modelRow = bookSelectorTable.convertRowIndexToModel(viewRow);
            isbnEditField.setText(String.valueOf(bookTableModel.getValueAt(modelRow, 0)));
            titleEditField.setText(String.valueOf(bookTableModel.getValueAt(modelRow, 1)));
            authorEditField.setText(String.valueOf(bookTableModel.getValueAt(modelRow, 2)));
            genreEditField.setText(String.valueOf(bookTableModel.getValueAt(modelRow, 3)));
            copiesEditSpinner.setValue(bookTableModel.getValueAt(modelRow, 4));
        }
        
    }
    
    /** Handles the saving of the changes made to the selected book. */
    private void saveChanges() {
        // Gets the selected ISBN, if the search fails, book not found error is shown.
        String isbn = isbnEditField.getText().trim();
        Book selectedBook = system.getBookByISBN(isbn);
        if (selectedBook == null) {
            JOptionPane.showMessageDialog(this, "Book not found.");
            return;
        }
        
        // Gets the changed fields from the entries        
        String title = titleEditField.getText().trim();
        String author = authorEditField.getText().trim();
        String genre = genreEditField.getText().trim();
        int newTotal = (int) copiesEditSpinner.getValue();
        int checkedOut = selectedBook.getBorrowCount();
        
        // Checks to see if new values are not default, empty, or number of copies is less than 0.
        if (newTotal < checkedOut) {
            JOptionPane.showMessageDialog(this, "Cannot reduce total copies below number currently checked out (" + checkedOut + ").");
            return;
        }else if (isbn.equals("Selected Book Title") || isbn.isEmpty() || 
                title.equals("Selected Book Title") || title.isEmpty() || 
                author.equals("Selected Book Title") || author.isEmpty() || 
                genre.equals("Selected Book Title") || genre.isEmpty()){
            JOptionPane.showMessageDialog(this, "Make sure all fields are complete.");
            return;
        }
        
        // Sets the attributes of record to their new values
        selectedBook.setTitle(title);
        selectedBook.setAuthor(author);
        selectedBook.setGenre(genre);
        selectedBook.setTotalCopies(newTotal);
        
        JOptionPane.showMessageDialog(this, "Book updated successfully.");
        
        // Book table refreshed and entry fields set to their defaults.
        fillBookTable(system.getAllBooks());
        isbnEditField.setText("Selected Book ISBN");
        titleEditField.setText("Selected Book Title");
        authorEditField.setText("Selected Book Author");
        genreEditField.setText("Selected Book Genre");
        copiesEditSpinner.setValue(0);
        bookSelectorTable.clearSelection();
    }
    
    /** Handles the deletion of the selected book. */
    private void deleteSelectedBook() {
        // Gets the selected row from the table
        // Error message if there isn't a selection
        int viewRow = bookSelectorTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a book to delete.");
            return;
        }
        
        int modelRow = bookSelectorTable.convertRowIndexToModel(viewRow);
        
        String isbn = String.valueOf(bookTableModel.getValueAt(modelRow, 0));
        Book selectedBook = system.getBookByISBN(isbn);
        if (selectedBook == null) {
            JOptionPane.showMessageDialog(this, "Book not found.");
            return;
        }
        
        // Checks to see if the selected book is currently checked out by any members
        // If it is, cannot be deleted.
        int checkedOut = selectedBook.getBorrowCount();
        if (checkedOut > 0) {
            JOptionPane.showMessageDialog(this, "Cannot delete \"" + selectedBook.getTitle() + "\" because " + 
                    checkedOut + " copies are currently checked out.");
            return;
        }
        
        // Asks if they are sure they want to delete the book
        int choice = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete \"" + selectedBook.getTitle() + "\"?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);
        
        // If they answer yes, the book is deleted and any reservations are cleared.
        if (choice == JOptionPane.YES_OPTION) {
            if (system.hasReservationQueue(isbn)) {
                system.clearReservationQueue(isbn);
            }
            system.deleteBook(isbn);
            JOptionPane.showMessageDialog(this, "Book deleted successfully.");
            fillBookTable(system.getAllBooks());
            isbnEditField.setText("Selected Book ISBN");
            titleEditField.setText("Selected Book Title");
            authorEditField.setText("Selected Book Author");
            genreEditField.setText("Selected Book Genre");
            copiesEditSpinner.setValue(0);
            bookSelectorTable.clearSelection();
        }
    }
    
    /** Handles the addition of new book to the system. */
    private void addBookHandler() {
        // Gets the details entered for the new book
        String isbn = newBookIsbnField.getText().trim();
        String title = newBookTitleField.getText().trim();
        String author = newBookAuthorField.getText().trim();
        String genre = newBookGenreField.getText().trim();
        int copies = (int) newBookCopiesSpinner.getValue();
        
        // Checks the fields have been changed from default and aren't empty.
        if (isbn.isEmpty() || isbn.equals("Enter Book ISBN") || 
                title.isEmpty() || title.equals("Enter Book Title") ||
                author.isEmpty() || author.equals("Enter Book Author") ||
                genre.isEmpty() || genre.equals("Enter Book Genre")) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.");
        } else if (system.getBookByISBN(isbn) != null){
            // Won't let a book with the same ISBN as another be added
            JOptionPane.showMessageDialog(this, "A book with this ISBN already exists.");
        } else if (copies < 0) {
            // Ensures number of copies is not negative.
            JOptionPane.showMessageDialog(this, "Number of book copies must be Zero or greater.");
        } else {
            // Book is added to systems memory
            Book newBook = new Book(isbn, title, author, genre, copies);
            system.addBook(newBook);
            
            JOptionPane.showMessageDialog(this, "Book added successfully.");
            
            // Book table is refreshed and entry fields set to their defaults.
            fillBookTable(system.getAllBooks());
            newBookIsbnField.setText("Enter Book ISBN");
            newBookTitleField.setText("Enter Book Title");
            newBookAuthorField.setText("Enter Book Author");
            newBookGenreField.setText("Enter Book Genre");
            newBookCopiesSpinner.setValue(0);
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
        searchTypeCombo = new javax.swing.JComboBox<>();
        searchEntry = new javax.swing.JTextField();
        sortByCombo = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        bookSelectorTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        addBookBtn = new javax.swing.JButton();
        jLabel20 = new javax.swing.JLabel();
        newBookIsbnField = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        newBookTitleField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        newBookAuthorField = new javax.swing.JTextField();
        newBookGenreField = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        newBookCopiesSpinner = new javax.swing.JSpinner();
        jLabel14 = new javax.swing.JLabel();
        searchBtn = new javax.swing.JButton();
        clearBtn = new javax.swing.JButton();
        goBackBtn = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        deleteBookBtn = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        isbnEditField = new javax.swing.JTextField();
        titleEditField = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        authorEditField = new javax.swing.JTextField();
        genreEditField = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        saveBookBtn = new javax.swing.JButton();
        copiesEditSpinner = new javax.swing.JSpinner();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1175, 635));

        jPanel1.setBackground(new java.awt.Color(246, 246, 246));
        jPanel1.setPreferredSize(new java.awt.Dimension(1175, 635));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setFont(new java.awt.Font("Tw Cen MT Condensed Extra Bold", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Book Manager Dashboard");
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
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(28, 0, 0, 0);
        jPanel1.add(sortByCombo, gridBagConstraints);

        jLabel2.setText("Sort by:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 218, 4, 0);
        jPanel1.add(jLabel2, gridBagConstraints);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(600, 150));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(600, 150));

        bookSelectorTable.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        bookSelectorTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ISBN", "Title", "Author", "Genre", "No. Copies"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        bookSelectorTable.setMinimumSize(new java.awt.Dimension(375, 150));
        bookSelectorTable.setRowHeight(25);
        bookSelectorTable.setRowMargin(2);
        bookSelectorTable.setSelectionBackground(new java.awt.Color(51, 153, 255));
        bookSelectorTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        bookSelectorTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        bookSelectorTable.setShowHorizontalLines(true);
        bookSelectorTable.setShowVerticalLines(true);
        bookSelectorTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                bookSelectorTableMouseReleased(evt);
            }
        });
        bookSelectorTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                bookSelectorTableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(bookSelectorTable);
        if (bookSelectorTable.getColumnModel().getColumnCount() > 0) {
            bookSelectorTable.getColumnModel().getColumn(0).setResizable(false);
            bookSelectorTable.getColumnModel().getColumn(0).setPreferredWidth(100);
            bookSelectorTable.getColumnModel().getColumn(1).setResizable(false);
            bookSelectorTable.getColumnModel().getColumn(1).setPreferredWidth(150);
            bookSelectorTable.getColumnModel().getColumn(2).setResizable(false);
            bookSelectorTable.getColumnModel().getColumn(2).setPreferredWidth(125);
            bookSelectorTable.getColumnModel().getColumn(3).setResizable(false);
            bookSelectorTable.getColumnModel().getColumn(3).setPreferredWidth(125);
            bookSelectorTable.getColumnModel().getColumn(4).setResizable(false);
            bookSelectorTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(10, 50, 0, 50);
        jPanel1.add(jScrollPane1, gridBagConstraints);

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jPanel3.setMinimumSize(new java.awt.Dimension(475, 535));
        jPanel3.setPreferredSize(new java.awt.Dimension(475, 535));
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(0, 153, 153));
        jLabel3.setText("Add New Book To System");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 63, 0);
        jPanel3.add(jLabel3, gridBagConstraints);

        addBookBtn.setText("Add Book");
        addBookBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBookBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(70, 0, 36, 0);
        jPanel3.add(addBookBtn, gridBagConstraints);

        jLabel20.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel20.setText("ISBN");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 20);
        jPanel3.add(jLabel20, gridBagConstraints);

        newBookIsbnField.setText("Enter Book ISBN");
        newBookIsbnField.setMinimumSize(new java.awt.Dimension(125, 27));
        newBookIsbnField.setPreferredSize(new java.awt.Dimension(125, 27));
        newBookIsbnField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newBookIsbnFieldMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        jPanel3.add(newBookIsbnField, gridBagConstraints);

        jLabel21.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel21.setText("Title");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 3, 0);
        jPanel3.add(jLabel21, gridBagConstraints);

        newBookTitleField.setText("Enter Book Title");
        newBookTitleField.setMinimumSize(new java.awt.Dimension(125, 27));
        newBookTitleField.setPreferredSize(new java.awt.Dimension(125, 27));
        newBookTitleField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newBookTitleFieldMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel3.add(newBookTitleField, gridBagConstraints);

        jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel22.setText("Author");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 3, 20);
        jPanel3.add(jLabel22, gridBagConstraints);

        jLabel23.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel23.setText("Genre");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 3, 0);
        jPanel3.add(jLabel23, gridBagConstraints);

        newBookAuthorField.setText("Enter Book Author");
        newBookAuthorField.setMinimumSize(new java.awt.Dimension(125, 27));
        newBookAuthorField.setPreferredSize(new java.awt.Dimension(125, 27));
        newBookAuthorField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newBookAuthorFieldMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 20);
        jPanel3.add(newBookAuthorField, gridBagConstraints);

        newBookGenreField.setText("Enter Book Genre");
        newBookGenreField.setMinimumSize(new java.awt.Dimension(125, 27));
        newBookGenreField.setPreferredSize(new java.awt.Dimension(125, 27));
        newBookGenreField.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                newBookGenreFieldMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel3.add(newBookGenreField, gridBagConstraints);

        jLabel24.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel24.setText("No. Copies");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(21, 0, 0, 20);
        jPanel3.add(jLabel24, gridBagConstraints);

        newBookCopiesSpinner.setMinimumSize(new java.awt.Dimension(50, 22));
        newBookCopiesSpinner.setPreferredSize(new java.awt.Dimension(50, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 20);
        jPanel3.add(newBookCopiesSpinner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        jPanel1.add(jPanel3, gridBagConstraints);

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel14.setText("Edit Existing Books");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.insets = new java.awt.Insets(30, 50, 0, 0);
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

        jPanel4.setBackground(new java.awt.Color(246, 246, 246));
        jPanel4.setInheritsPopupMenu(true);
        jPanel4.setMinimumSize(new java.awt.Dimension(700, 270));
        jPanel4.setPreferredSize(new java.awt.Dimension(700, 270));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        deleteBookBtn.setText("Delete Selected Book");
        deleteBookBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteBookBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 29, 0);
        jPanel4.add(deleteBookBtn, gridBagConstraints);

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel15.setText("ISBN");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 1, 0);
        jPanel4.add(jLabel15, gridBagConstraints);

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel16.setText("Title");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 1, 0);
        jPanel4.add(jLabel16, gridBagConstraints);

        isbnEditField.setEditable(false);
        isbnEditField.setText("Selected Book ISBN");
        isbnEditField.setMinimumSize(new java.awt.Dimension(125, 27));
        isbnEditField.setPreferredSize(new java.awt.Dimension(125, 27));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 10, 50);
        jPanel4.add(isbnEditField, gridBagConstraints);

        titleEditField.setText("Selected Book Title");
        titleEditField.setMinimumSize(new java.awt.Dimension(125, 27));
        titleEditField.setPreferredSize(new java.awt.Dimension(125, 27));
        titleEditField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                titleEditFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 0);
        jPanel4.add(titleEditField, gridBagConstraints);

        jLabel17.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel17.setText("Author");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 1, 0);
        jPanel4.add(jLabel17, gridBagConstraints);

        jLabel18.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel18.setText("Genre");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        jPanel4.add(jLabel18, gridBagConstraints);

        authorEditField.setText("Selected Book Author");
        authorEditField.setMinimumSize(new java.awt.Dimension(125, 27));
        authorEditField.setPreferredSize(new java.awt.Dimension(125, 27));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 10, 50);
        jPanel4.add(authorEditField, gridBagConstraints);

        genreEditField.setText("Selected Book Genre");
        genreEditField.setMinimumSize(new java.awt.Dimension(125, 27));
        genreEditField.setPreferredSize(new java.awt.Dimension(125, 27));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 10, 0);
        jPanel4.add(genreEditField, gridBagConstraints);

        jLabel19.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel19.setText("No. Copies");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        jPanel4.add(jLabel19, gridBagConstraints);

        saveBookBtn.setText("Save Book");
        saveBookBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveBookBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 35, 0);
        jPanel4.add(saveBookBtn, gridBagConstraints);

        copiesEditSpinner.setMinimumSize(new java.awt.Dimension(50, 22));
        copiesEditSpinner.setPreferredSize(new java.awt.Dimension(50, 22));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(1, 0, 10, 0);
        jPanel4.add(copiesEditSpinner, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
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

    private void clearBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearBtnActionPerformed
        fillBookTable(new ArrayList<>(system.getAllBooks()));
        searchEntry.setText("Enter Search");
    }//GEN-LAST:event_clearBtnActionPerformed

    private void titleEditFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_titleEditFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_titleEditFieldActionPerformed

    private void goBackBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_goBackBtnActionPerformed
        dispose();
        new StaffDashboard(system).setVisible(true);
    }//GEN-LAST:event_goBackBtnActionPerformed

    private void searchBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchBtnActionPerformed
        performSearch();
    }//GEN-LAST:event_searchBtnActionPerformed

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

    private void saveBookBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBookBtnActionPerformed
        saveChanges();
    }//GEN-LAST:event_saveBookBtnActionPerformed

    private void addBookBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBookBtnActionPerformed
        addBookHandler();
    }//GEN-LAST:event_addBookBtnActionPerformed

    private void deleteBookBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteBookBtnActionPerformed
        deleteSelectedBook();
    }//GEN-LAST:event_deleteBookBtnActionPerformed

    private void newBookIsbnFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newBookIsbnFieldMouseClicked
        if (newBookIsbnField.getText().equals("Enter Book ISBN")) {
            newBookIsbnField.setText("");
        }
    }//GEN-LAST:event_newBookIsbnFieldMouseClicked

    private void newBookTitleFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newBookTitleFieldMouseClicked
        if (newBookTitleField.getText().equals("Enter Book Title")) {
            newBookTitleField.setText("");
        }
    }//GEN-LAST:event_newBookTitleFieldMouseClicked

    private void newBookAuthorFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newBookAuthorFieldMouseClicked
        if (newBookAuthorField.getText().equals("Enter Book Author")) {
            newBookAuthorField.setText("");
        }
    }//GEN-LAST:event_newBookAuthorFieldMouseClicked

    private void newBookGenreFieldMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_newBookGenreFieldMouseClicked
        if (newBookGenreField.getText().equals("Enter Book Genre")) {
            newBookGenreField.setText("");
        }
    }//GEN-LAST:event_newBookGenreFieldMouseClicked

    private void bookSelectorTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_bookSelectorTableMouseReleased
        fillFields();
    }//GEN-LAST:event_bookSelectorTableMouseReleased

    private void bookSelectorTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_bookSelectorTableKeyReleased
        fillFields();
    }//GEN-LAST:event_bookSelectorTableKeyReleased

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addBookBtn;
    private javax.swing.JTextField authorEditField;
    private javax.swing.JTable bookSelectorTable;
    private javax.swing.JButton clearBtn;
    private javax.swing.JSpinner copiesEditSpinner;
    private javax.swing.JButton deleteBookBtn;
    private javax.swing.JTextField genreEditField;
    private javax.swing.JButton goBackBtn;
    private javax.swing.JTextField isbnEditField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField newBookAuthorField;
    private javax.swing.JSpinner newBookCopiesSpinner;
    private javax.swing.JTextField newBookGenreField;
    private javax.swing.JTextField newBookIsbnField;
    private javax.swing.JTextField newBookTitleField;
    private javax.swing.JButton saveBookBtn;
    private javax.swing.JButton searchBtn;
    private javax.swing.JTextField searchEntry;
    private javax.swing.JComboBox<String> searchTypeCombo;
    private javax.swing.JComboBox<String> sortByCombo;
    private javax.swing.JTextField titleEditField;
    // End of variables declaration//GEN-END:variables
}
