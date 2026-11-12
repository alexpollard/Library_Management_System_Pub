package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a book in the library system
 * Each books tracks its metadata (ISBN, title, author, genre),
 * number of copies, borrow history and popularity statistics.
 * 
 * Enforces validation rules, not allowing empty attributes or negative
 * copy counts, providing methods for borrowing, returning, 
 * and recording their use.
 * 
 * @author alex pollard - s2342835
 */

public class Book {
    // Unique, immutable book identifier
    private final String ISBN;
    
    // Core book descriptive metadata
    private String title, author, genre;
    
    // Copy counts: total owned vs currently available
    private int totalCopies, availableCopies;
    
    // Records dates when the book was borrowed to measure popularity
    private final List<LocalDate> BORROW_DATES = new ArrayList<>();
    
    /**
     * Constructs an new Book object.
     * @param isbn unique identifier
     * @param title book title
     * @param author book author
     * @param genre book genre
     * @param totalCopies number of copies owned by library
    */
    public Book(String isbn, String title, String author, String genre, int totalCopies) {
        this.ISBN = isbn;
        setTitle(title);
        setAuthor(author);
        setGenre(genre);
        setTotalCopies(totalCopies);
        this.availableCopies = totalCopies; // initially all copies are available
    }
    
    // ------------------- Getters -------------------
    public String getISBN() { return ISBN; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getGenre() { return genre; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public int getBorrowCount(){ return totalCopies - availableCopies; }
    
    // ------------------- Setters -------------------
    /**
     * Setting the book title (must not be empty).
     * @param title
     */
    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title required");
        this.title = title.trim();
    }
    
    /**
     * Sets the author name (must not be empty).
     * @param author 
     */
    public void setAuthor(String author) {
        if (author == null || author.trim().isEmpty()) throw new IllegalArgumentException("Author required");
        this.author = author.trim();
    }
    
    /**
     * Sets the genre (must not be empty).
     * @param genre 
     */
    public void setGenre(String genre) {
        if (genre == null || genre.trim().isEmpty()) throw new IllegalArgumentException("Genre required");
        this.genre = genre.trim();
    }
    
    /**
     * Adjusts the total number of copies.
     * Also updates available copies to keep consistency.
     * Ensures availableCopies never exceeds totalCopies or drops below zero.
     * @param totalCopies 
     */
    public void setTotalCopies(int totalCopies) {
        if (totalCopies < 0) throw new IllegalArgumentException("Total copies cannot be negative");
        int delta = totalCopies - this.totalCopies;
        this.totalCopies = totalCopies;
        this.availableCopies += delta;
        
        // Make sure availableCopies remains in a valid range
        if (this.availableCopies < 0) this.availableCopies = 0;
        if (this.availableCopies > this.totalCopies) this.availableCopies = this.totalCopies;
    }
    
    // ------------------- Borrowing Logic -------------------
    /** @return true if at least one copy is available to borrow */
    public boolean canBorrow() { 
        return availableCopies > 0; 
    }
    
    /**
     * Borrow a copy of the book.
     * Decreases availableCopies and increments borrowCount.
     * Throws an exception if there a no copies available.
     */
    public void borrowCopy() {
        if (!canBorrow()) throw new IllegalStateException("No Copies Available");
        availableCopies--;
    }
    
    /**
     * Return a copy of the book
     * Increases availableCopies but never beyond totalCopies to correct
     * incorrect values
     */
    public void returnCopy() {
        if (availableCopies < totalCopies) availableCopies++;
    }
    
    // ------------------- Borrow History -------------------
    /**
     * Records the date when a copy was borrowed.
     * Used for tracking of popular books.
     * @param date 
     */
    public void recordBorrowDate(LocalDate date) {
        LocalDate cutoff = LocalDate.now().minusWeeks(2);
        BORROW_DATES.removeIf(d -> d.isBefore(cutoff)); // prunes old entries
        BORROW_DATES.add(date);
    }
    
    /**
     * Counts how many times the book has been borrowed since a given cutoff date.
     * @param cutoff date to compare against list
     * @return the number of borrow events since cutoff
     */
    public int getBorrowCountSince(LocalDate cutoff) {
        return (int) BORROW_DATES.stream().filter(d -> d.isAfter(cutoff)).count();
    }
}