package model;

import java.time.LocalDate;

/**
 * Represents  a loan of a book to a library member
 * Tracks which member borrowed which book, when it was checked out,
 * and when it is due to be returned.
 * 
 * Provides a method to implement demo data so overdue checks can be tested.
 *
 * @author alex pollard - s2342835
 */

public class Loan {
    // ID of the member who borrowed the book
    private final int MEMBER_ID;
    
    // ISBN of the borrowed book
    private final String ISBN;
    
    // Dates when the book was checked out and is due to be returned
    private final LocalDate CHECKOUT_DATE;
    private final LocalDate DUE_DATE;
    
    /**
     * Constructs a Loan
     * @param memberId of the borrowing member
     * @param isbn of the borrowed book
     * @param checkoutDate date book was borrowed
     * @param dueDate  date book is due
     */
    public Loan(int memberId, String isbn, LocalDate checkoutDate, LocalDate dueDate) {
        this.MEMBER_ID = memberId;
        this.ISBN = isbn;
        this.CHECKOUT_DATE = checkoutDate;
        this.DUE_DATE = dueDate;
    }
    
    /**
     * Convenience constructor for demo data.
     * Calculates the due date automatically by adding weeks to pre-set  checkout date.
     * @param memberId of borrowing member
     * @param isbn of borrowed book
     * @param checkoutDate date book was borrowed
     * @param weeks until due
     */
    public Loan(int memberId, String isbn, LocalDate checkoutDate, int weeks) { 
        this.MEMBER_ID = memberId;
        this.ISBN = isbn; 
        this.CHECKOUT_DATE = checkoutDate; 
        this.DUE_DATE = checkoutDate.plusWeeks(weeks); }
    
    
    // ------------------- Getters -------------------
    public int getMemberId() { return MEMBER_ID; }
    public String getIsbn() { return ISBN; }
    public LocalDate getCheckoutDate() { return CHECKOUT_DATE; }
    public LocalDate getDueDate() { return DUE_DATE; }
    
    // ------------------- Utlity -------------------
    /**
     * Checks if the loan is overdue compared to today's date.
     * @param today the date to compare against
     * @return true if today is after the due date, false otherwise
     */
    // Returns true/false depending on if book is overdue o return
    public boolean isOverdue(LocalDate today) {
        return today.isAfter(DUE_DATE);
    }
}
