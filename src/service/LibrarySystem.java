package service;

import java.time.LocalDate;
import java.util.*;

import model.*;
import util.LRUCache;

/**
 * Core service class for the library system
 * Manages members, boobs, loans, reservations, fines, and search indexes
 * Provides staff and member operations such as registration, checkout,
 * return, reserve, and search.
 * 
 * Acts as the central hub connecting the models with higher level logic
 *
 * @author Alex Pollard
 */

public class LibrarySystem {
    // ------------------- Core Data Stores -------------------
    /**
     * Primary Registry of all Members, keyed by their ID.
     * HashMap chosen for O(1) average lookup time.
     */
    private final Map<Integer, Member> MEMBERS_BY_ID = new HashMap<>();
    
    // Member ID generator, starting at 1000.
    private int nextMemberId = 1000;
    
    // Catalogue of all books, each keyed by ISBN
    private final Map<String, Book> BOOKS_BY_ISBN = new HashMap<>();
    
    // FIFO reservation queue, keyed by ISBN
    private final Map<String, Queue<Integer>> RESERVATIONS = new HashMap<>();
    
    private final Map<Integer, Set<String>> RESERVATIONS_BY_MEMBER = new HashMap<>();
    
    // Cache of recently borrowed books for tracking popularity witha fixed capacity of 50
    private final LRUCache POPULAR_CACHE = new LRUCache(50);
    
    // Secondary indexes enabling fast, case‑insensitive searches.
    // TreeMap chosen to maintain keys in sorted order, supporting efficient
    // prefix queries and alphabetical browsing in the UI.
    private final TreeMap<String, Set<String>> TITLE_INDEX = new TreeMap<>();
    private final TreeMap<String, Set<String>> AUTHOR_INDEX = new TreeMap<>();
    private final TreeMap<String, Set<String>> GENRE_INDEX = new TreeMap<>();
    
    
    // ------------------- Member Management -------------------
    
    /** Registers a new member with auto-generated ID. */
    public Member registerMember(String firstName, String surname) {
        Member regMember = new Member(nextMemberId++, firstName, surname);
        MEMBERS_BY_ID.put(regMember.getMemberId(), regMember);
        return regMember;
    }

    /** Retrieves a member by ID for login. */
    public Member loginMember(int memberId) {
        return MEMBERS_BY_ID.get(memberId);
    }
    
    /** Gets a member by ID. */
    public Member getMember(int memberId) {
        return MEMBERS_BY_ID.get(memberId);
    }
    
    /** Returns all members in the system. */
    public Collection<Member> getAllMembers() {
        return new ArrayList<>(MEMBERS_BY_ID.values());
    }
    
    //** Updates a member's name. */
    public boolean updateMemberName(int memberId, String firstName, String surname) {
        Member selectedMember = MEMBERS_BY_ID.get(memberId);
        if (selectedMember == null) return false;
        selectedMember.setFirstName(firstName);
        selectedMember.setSurname(surname);
        return true;
    }
    
    /**
     * Checks if a member can be safely deleted.
     * A member cannot be deleted if they have active loans or reservations.
     */
    public boolean canDeleteMember(int memberId) {
        Member selectedMember = MEMBERS_BY_ID.get(memberId);
        if (selectedMember == null) return false;
        if (!selectedMember.getLoans().isEmpty()) return false;
        
        // Checking if they are in any reservation queues
        Set<String> reservedIsbns = RESERVATIONS_BY_MEMBER.getOrDefault(memberId, Set.of());
        if (!reservedIsbns.isEmpty()) return false;
        
        // Returns true if they exist, have no loans or reservations
        return true;
    }
    
    /** Deletes a member record. */
    public boolean deleteMember(int memberId) {
        MEMBERS_BY_ID.remove(memberId);
        return true;
    }
    
    // ------------------- Book Management -------------------
    
    /** Adds a book and indexes it by title, author, and genre. (Staff Only) */
    public void addBook(Book newBook) {
        BOOKS_BY_ISBN.put(newBook.getISBN(), newBook);
        indexValue(TITLE_INDEX, newBook.getTitle().toLowerCase(), newBook.getISBN());
        indexValue(AUTHOR_INDEX, newBook.getAuthor().toLowerCase(), newBook.getISBN());
        indexValue(GENRE_INDEX, newBook.getGenre().toLowerCase(), newBook.getISBN());
    }
    
    /** Deletes a book by ISBN. (Staff Only) */
    public void deleteBook(String isbn) {
        BOOKS_BY_ISBN.remove(isbn);
    }
    
    /** Retrieves a book by ISBN. */
    public Book getBookByISBN(String isbn) {
        return BOOKS_BY_ISBN.get(isbn);
    }
    
    /** Returns all books in the system */
    public Collection<Book> getAllBooks() {
        return BOOKS_BY_ISBN.values();
    }
    
    /** Fast Search */
    private void indexValue(TreeMap<String, Set<String>> index, String key , String isbn) {
        String k = key == null ? "" : key.toLowerCase();
        index.computeIfAbsent(k, x -> new HashSet<>()).add(isbn);
    }
    
    // ------------------- Checkout & Return -------------------
    
    /**
     * Attempts to checkout a book for a member.
     * Prevents duplicate loans of the same book by the same member.
     * Records borrow date and updates popularity cache.
     */
    public boolean checkoutBook(int memberId, String isbn, int weeks) {
        Member selectedMember = MEMBERS_BY_ID.get(memberId);
        Book selectedBook = BOOKS_BY_ISBN.get(isbn);
        
        // Returns false if either member or book don't exist, or if there no more books available
        if (selectedMember == null || selectedBook == null || !selectedBook.canBorrow()) return false; 
        
        // Returns false if they have already borrowed the book
        boolean alreadyBorrowed = selectedMember.getLoans().stream()
                .anyMatch(l -> l.getIsbn().equals(isbn));
        if (alreadyBorrowed) return false;
        
        // Queue enforcement 
        Queue<Integer> queue = RESERVATIONS.get(isbn);
        if (queue != null && !queue.isEmpty()) { 
            // If member is NOT first → block checkout
            if (queue.peek() != memberId) {
                return false;
            } 
            // If member IS first → remove them from queue 
            queue.poll(); 
            RESERVATIONS_BY_MEMBER.get(memberId).remove(isbn);
        } 

        if (!selectedBook.canBorrow()) return false;
        
        selectedBook.borrowCopy();
        selectedBook.recordBorrowDate(LocalDate.now());
        
        // New loan is created and assigned to the member
        Loan loan = new Loan(memberId, isbn, LocalDate.now(), LocalDate.now().plusWeeks(weeks));
        selectedMember.addLoan(loan);
        
        POPULAR_CACHE.put(isbn, selectedBook);
        
        return true;
    }
    
    /**
     * Returns a book for a member.
     * Applies overdue fines (£0.50 per day late) and damage fines.
     * Updates member loans and book availability.
     */
    public boolean returnBook(int memberId, String isbn, boolean damaged) {
        Member selectedMember = MEMBERS_BY_ID.get(memberId);
        Book selectedBook = BOOKS_BY_ISBN.get(isbn);
        // Returns false if member or book don't exist
        if (selectedMember == null || selectedBook == null) return false;
        
        // Returns false if the member hasn't got the selected book checked out.
        Loan loan = selectedMember.getLoans().stream()
                .filter(l -> l.getIsbn().equals(isbn))
                .findFirst().orElse(null);
        if (loan == null) return false;
        
        // Calculates the fines and assigns them to the member
        if (loan.isOverdue(LocalDate.now())) {
            long daysLate = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            selectedMember.addOverdueFine(daysLate * 0.5);
        }
        if (damaged) {
            selectedMember.addDamageFine(5.00);
        }
        
        selectedMember.removeLoanByISBN(isbn);
        selectedBook.returnCopy();
        return true;
    }
    
    // ------------------- Reservations -------------------

    //** Adds a member to the reservation queue for a book. Prevents duplicates */
    public boolean reserveBook(int memberId, String isbn) {
        Member member = MEMBERS_BY_ID.get(memberId);
        
        // Prevent reserving a book already checked out
        boolean alreadyBorrowed = member.getLoans().stream()
                .anyMatch(l -> l.getIsbn().equals(isbn));
        if (alreadyBorrowed) return false;
        
        Queue<Integer> queue = RESERVATIONS.computeIfAbsent(isbn, k -> new LinkedList<>());
        if (queue.contains(memberId)) {
            return false;
        }
        queue.add(memberId);
        RESERVATIONS_BY_MEMBER
                .computeIfAbsent(memberId, k -> new HashSet<>())
                .add(isbn);

        return true;
    }
    
    /** Gets the reservation queue for a book. */
    public Queue<Integer> getReservationQueue(String isbn) {
        return RESERVATIONS.getOrDefault(isbn, new LinkedList<>());
    }
    
    /** Fulfils the next reservation in line by checking out the book */
    public void fulfillReservation(String isbn) {
        Queue<Integer> queue = RESERVATIONS.get(isbn);
        if (queue != null && !queue.isEmpty()) {
            int nextMemberId = queue.poll();
            checkoutBook(nextMemberId, isbn, 2);
        }
    }
    
    /**
     * Checks reservations for a member.
     * If they are first in line and a copy is available, book is auto-checked out.
     * Called on login
     */
    public List<Book> checkReservationsForMember(int memberId) {
        Set<String> reservedIsbns = RESERVATIONS_BY_MEMBER.getOrDefault(memberId, Set.of());
        List<Book> readyBooks = new ArrayList<>();
        
        for (String isbn : reservedIsbns) {
            Queue<Integer> queue = RESERVATIONS.get(isbn);
            Book book = BOOKS_BY_ISBN.get(isbn);
            
            // Checks out the book on login if they are in a queue and at the front.
            if (queue != null && !queue.isEmpty() && queue.peek() == memberId &&
                    book != null && book.getAvailableCopies() > 0) {
                
                if (checkoutBook(memberId, isbn, 2)){
                    readyBooks.add(book);
                    RESERVATIONS_BY_MEMBER.get(memberId).remove(isbn);
                }
            }
        }
        return readyBooks;
    }
    
    /** Checks if a book has a reserve queue. */
    public boolean hasReservationQueue(String isbn) {
        Queue<Integer> queue = RESERVATIONS.get(isbn);
        return queue != null && !queue.isEmpty();
    }
    
    public boolean isFirstInQueue(int memberId, String isbn) {
        Queue<Integer> q = RESERVATIONS.get(isbn);
        return q != null && !q.isEmpty() && q.peek() == memberId; 
    }
    
    /** Clears the reservation queue for a book. */
    public void clearReservationQueue(String isbn) {
        RESERVATIONS.remove(isbn);
    }
    
    /**
     * Gets the waitlist position for a member across all reservations.
     * @return map of Book -> position in queue
     */
    public Map<Book, Integer> getWaitlistForMember(int memberId) {
        Map<Book, Integer> waitlist = new LinkedHashMap<>();
        for (Map.Entry<String, Queue<Integer>> entry : RESERVATIONS.entrySet()) {
            String isbn = entry.getKey();
            Queue<Integer> queue = entry.getValue();
            if (queue != null && queue.contains(memberId)) {
                int position = 1;
                for (int queueId : queue) {
                    if (queueId == memberId) {
                        Book selectedBook = BOOKS_BY_ISBN.get(isbn);
                        if (selectedBook != null) waitlist.put(selectedBook, position);
                        break;
                    }
                    position++;
                }
            }
        }
        return waitlist;
    }
    
    // ------------------- Search & Popularity -------------------
    
    /**
     * Searches books by type and query
     * Supported types: "Title", "Author", "Genre", "ISBN".
     * Uses prebuilt indexes (TreeMap) for fast lookups (except direct ISBN)
     */
    public List<Book> searchBooks(String type, String query) {
        if (query == null) query = "";
        query = query.toLowerCase();
        Set<String> isbns = new HashSet<>();
        
        switch (type) {
            case "Title":
                isbns.addAll(TITLE_INDEX.getOrDefault(query, Set.of()));
                break;
            case "Author":
                isbns.addAll(AUTHOR_INDEX.getOrDefault(query, Set.of()));
                break;
            case "Genre":
                isbns.addAll(GENRE_INDEX.getOrDefault(query, Set.of()));
                break;
            case "ISBN":
                if (BOOKS_BY_ISBN.containsKey(query)) isbns.add(query);
                break;
            default:
                isbns.addAll(BOOKS_BY_ISBN.keySet());
        }
        
        List<Book> results = new ArrayList<>();
        for (String isbn : isbns) {
            Book book = BOOKS_BY_ISBN.get(isbn);
            if (book != null) results.add(book);
        }
        return results;
    }
    
    /**
     * Prefix search across Title/Author.Genre indexes
     * Returns all books where the indexed key starts with the provided prefix.
     * Eg: "har" matches "harry potter" and "hard times".
     */
    public List<Book> searchByPrefix(String type, String prefix) {
        prefix = prefix == null ? "" : prefix.toLowerCase();
        TreeMap<String, Set<String>> index;
        switch (type) {
            case "Title":  index = TITLE_INDEX; break;
            case "Author": index = AUTHOR_INDEX; break;
            case "Genre":  index = GENRE_INDEX; break;
            default: return Collections.emptyList();
        }

        String fromKey = prefix;
        String toKey   = prefix + Character.MAX_VALUE;

        NavigableMap<String, Set<String>> sub = index.subMap(fromKey, true, toKey, true);
        
        List<Book> results = new ArrayList<>();
        for (Set<String> set : sub.values()) {
            for (String isbn : set) {
                Book book = BOOKS_BY_ISBN.get(isbn);
                if (book != null) results.add(book);
            }
        }
        return results;
    }
    
    /**
     * Returns popular books since a cutoff date, sorted by recent borrow activity.
     * Uses the LRU popularCache and ranks by Book.getBorrowCountSince(cutoff).
     * @param cutoff
     * @param limit
     * @return 
     */
    public List<Book> getPopularBooksSince(LocalDate cutoff, int limit) {
        List<Book> cached = new ArrayList<>(POPULAR_CACHE.values());
        cached.sort(Comparator.comparingInt(
                book -> -book.getBorrowCountSince(cutoff)
        ));
        return cached.stream()
                .filter(book -> book.getBorrowCountSince(cutoff) > 0)
                .limit(limit)
                .toList();
    }
    
    // ------------------- Demo Data -------------------
    public void preloadDemoData() {
        Member alex = registerMember("Alex", "Pollard");
        Member john = registerMember("John", "Smith");
        Member mary = registerMember("Mary", "Brown");
        Member robert = registerMember("Robert", "Jones");
        Member barbara = registerMember("Barbara", "Davis");
        Member richard = registerMember("Richard", "Wilson");
        Member nancy = registerMember("Nancy", "Taylor");
        Member kenneth = registerMember("Kenneth", "Martin");
        
        addBook(new Book("9780140268867", "The Odyssey", "Homer", "Epic", 5));
        addBook(new Book("9780596009205", "Head First Java", "Kathy Sierra", "Programming", 2));        
        addBook(new Book("9780743273565","The Great Gatsby","F. Scott Fitzgerald","Classic",3));
        addBook(new Book("9780451524935","1984","George Orwell","Dystopian",5));
        addBook(new Book("9780061120084","To Kill a Mockingbird","Harper Lee","Classic",4));
        addBook(new Book("9780141439518","Pride and Prejudice","Jane Austen","Romance",3));
        addBook(new Book("9781503280786","Moby Dick","Herman Melville","Adventure",2));
        addBook(new Book("9780199232765","War and Peace","Leo Tolstoy","Historical",2));
        addBook(new Book("9780316769488","The Catcher in the Rye","J.D. Salinger","Classic",3));
        addBook(new Book("9780060850524","Brave New World","Aldous Huxley","Dystopian",4));
        addBook(new Book("9780547928227","The Hobbit","J.R.R. Tolkien","Fantasy",5));
        addBook(new Book("9781408855652","Harry Potter and the Philosopher's Stone","J.K. Rowling","Fantasy",6));
        addBook(new Book("9780261103573","The Fellowship of the Ring","J.R.R. Tolkien","Fantasy",4));
        addBook(new Book("9780261103580","The Two Towers","J.R.R. Tolkien","Fantasy",4));
        addBook(new Book("9780261103597","The Return of the King","J.R.R. Tolkien","Fantasy",4));
        addBook(new Book("9780132350884","Clean Code","Robert C. Martin","Programming",3));
        addBook(new Book("9780201633610","Design Patterns","Erich Gamma","Programming",2));
        addBook(new Book("9780134685991","Effective Java","Joshua Bloch","Programming",3));
        addBook(new Book("9780201616224","The Pragmatic Programmer","Andrew Hunt","Programming",3));
        addBook(new Book("9780140449136","Crime and Punishment","Fyodor Dostoevsky","Classic",2));
        addBook(new Book("9780374528379","The Brothers Karamazov","Fyodor Dostoevsky","Classic",2));
        addBook(new Book("9780140449174","Anna Karenina","Leo Tolstoy","Classic",2));
        addBook(new Book("9780061122415","The Alchemist","Paulo Coelho","Philosophical",4));
        addBook(new Book("9780307474278","The Da Vinci Code","Dan Brown","Thriller",5));
        addBook(new Book("9780743493468","Angels & Demons","Dan Brown","Thriller",4));
        addBook(new Book("9780307949486","The Girl with the Dragon Tattoo","Stieg Larsson","Crime",4));
        addBook(new Book("9780307588371","Gone Girl","Gillian Flynn","Thriller",3));
        addBook(new Book("9780307743657","The Shining","Stephen King","Horror",4));
        addBook(new Book("9781501142970","It","Stephen King","Horror",3));
        addBook(new Book("9780441172719","Dune","Frank Herbert","Science Fiction",5));
        addBook(new Book("9780553293357","Foundation","Isaac Asimov","Science Fiction",4));
        addBook(new Book("9780441569595","Neuromancer","William Gibson","Science Fiction",3));
        
        
        Book b1 = BOOKS_BY_ISBN.get("9780307474278");
        b1.borrowCopy();
        b1.recordBorrowDate(LocalDate.now().minusWeeks(4));
        Loan loan1 = new Loan(alex.getMemberId(), b1.getISBN(), LocalDate.now().minusWeeks(4), LocalDate.now().minusWeeks(2));
        alex.addLoan(loan1);
        POPULAR_CACHE.put(b1.getISBN(), b1);
        
        Book b2 = BOOKS_BY_ISBN.get("9780140449136");
        b2.borrowCopy();
        b2.recordBorrowDate(LocalDate.now().minusWeeks(3));
        Loan loan2 = new Loan(alex.getMemberId(), b2.getISBN(), LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(1));
        alex.addLoan(loan2);
        POPULAR_CACHE.put(b2.getISBN(), b2);
        
        Book b3 = BOOKS_BY_ISBN.get("9780061120084");
        b1.borrowCopy();
        b1.recordBorrowDate(LocalDate.now().minusWeeks(4));
        Loan loan3 = new Loan(alex.getMemberId(), b3.getISBN(), LocalDate.now().minusWeeks(4), LocalDate.now().minusWeeks(2));
        alex.addLoan(loan3);
        POPULAR_CACHE.put(b3.getISBN(), b3);

        Book b4 = BOOKS_BY_ISBN.get("9780441172719");
        b4.borrowCopy();
        b4.recordBorrowDate(LocalDate.now().minusWeeks(3));
        Loan loan4 = new Loan(john.getMemberId(), b4.getISBN(), LocalDate.now().minusWeeks(3), LocalDate.now().minusWeeks(1));
        john.addLoan(loan4);
        POPULAR_CACHE.put(b4.getISBN(), b4);

        Book b5 = BOOKS_BY_ISBN.get("9781408855652");
        b5.borrowCopy();
        b5.recordBorrowDate(LocalDate.now().minusWeeks(1));
        Loan loan5 = new Loan(mary.getMemberId(), b5.getISBN(), LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(1));
        mary.addLoan(loan5);
        POPULAR_CACHE.put(b5.getISBN(), b5);

        Book b6 = BOOKS_BY_ISBN.get("9780132350884");
        b6.borrowCopy();
        b6.recordBorrowDate(LocalDate.now().minusWeeks(5));
        Loan loan6 = new Loan(robert.getMemberId(), b6.getISBN(), LocalDate.now().minusWeeks(5), LocalDate.now().minusWeeks(2));
        robert.addLoan(loan6);
        POPULAR_CACHE.put(b6.getISBN(), b6);

        Book b7 = BOOKS_BY_ISBN.get("9780061122415");
        b7.borrowCopy();
        b7.recordBorrowDate(LocalDate.now().minusWeeks(2));
        Loan loan7 = new Loan(richard.getMemberId(), b7.getISBN(), LocalDate.now().minusWeeks(2), LocalDate.now());
        richard.addLoan(loan7);
        POPULAR_CACHE.put(b7.getISBN(), b7);

        Book b8 = BOOKS_BY_ISBN.get("9780307588371");
        b8.borrowCopy();
        b8.recordBorrowDate(LocalDate.now().minusWeeks(6));
        Loan loan8 = new Loan(nancy.getMemberId(), b8.getISBN(), LocalDate.now().minusWeeks(6), LocalDate.now().minusWeeks(3));
        nancy.addLoan(loan8);
        POPULAR_CACHE.put(b8.getISBN(), b8);

        Book b9 = BOOKS_BY_ISBN.get("9780441569595");
        b9.borrowCopy();
        b9.recordBorrowDate(LocalDate.now().minusWeeks(1));
        Loan loan9 = new Loan(kenneth.getMemberId(), b9.getISBN(), LocalDate.now().minusWeeks(1), LocalDate.now().plusWeeks(1));
        kenneth.addLoan(loan9);
        POPULAR_CACHE.put(b9.getISBN(), b9);
    }
        
    
    
}