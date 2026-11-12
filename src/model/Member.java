package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a library member.
 * Tracks personal details, active loans, and fines.
 * Provides methods for managing loans and paying fines.
 *
 * @author alex pollard - s2342835
 */

public class Member {
    // Immutable unique member identifier
    private final int MEMBER_ID;
    
    // Members personal details
    private String firstName;
    private String surname;
    
    // List of active loans for this member
    private final List<Loan> LOANS = new ArrayList<>();
    
    // Fine balances    
    private double overdueFines = 0.0;
    private double damageFines = 0.0;
    
    /**
     * Constructs a new member object.
     * @param memberID unique identifier
     * @param firstName of member
     * @param surname of member
     */
    public Member(int memberID, String firstName, String surname) {
        this.MEMBER_ID = memberID;
        setFirstName(firstName);
        setSurname(surname);
    }
    
    // ------------------- Getters -------------------
    public int getMemberId(){ return MEMBER_ID; }
    public String getFirstName(){ return firstName; }
    public String getSurname(){ return surname; }
    public String getFullName() { return firstName + " " + surname; }
    public List<Loan> getLoans() { return LOANS; }
    public double getOverdueFines() {return overdueFines;}
    public double getDamageFines() {return damageFines;}
    public double getTotalFines() {return overdueFines + damageFines;}
    
    
    // ------------------- Loan Management -------------------
    /**
     * Adds new loan to the member's active loan list
     * @param loan the loan to add
     */
    public void addLoan(Loan loan) { 
        LOANS.add(loan); 
    }
    
    /**
     * Removes a loan by ISBN if present.
     * @param isbn the ISBN of the book to remove
     * @return true if a loan was removed
     */
    public boolean removeLoanByISBN(String isbn) {
        return LOANS.removeIf(l -> l.getIsbn().equals(isbn));
    }
    
    // ------------------- Fine management -------------------
    /**
     * Adds an overdue fine to the member's balance
     * @param amount fine amount
     */
    public void addOverdueFine(double amount) {
        if (amount > 0) overdueFines += amount;
    }
    
    /**
     * Adds a damage fine to the member's balance
     * @param amount fine amount
     */
    public void addDamageFine(double amount) {
        if (amount > 0) damageFines += amount;
    }
    
    // ------------------- Staff Functions -------------------
    /**
     * Updates the member's first name if not empty
     * @param firstName 
     */
    public void setFirstName(String firstName) {
        if(firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name required");
        }
        this.firstName = firstName.trim();
    }
    
    /**
     * Updates the member's surname if not empty
     * @param surname 
     */
    public void setSurname(String surname) {
        if(surname == null || surname.trim().isEmpty()) {
            throw new IllegalArgumentException("Surname required");
        }
        this.surname = surname.trim();
    }
    
    /**
     * Pays down fines in order: overdue fines first, then damage fines.
     * @param amount amount to pay (must be positive and not exceed total fines)
     * @return true if the payment was applied
     */
    public boolean payFine(double amount) {
        if (amount <= 0) return false;
        double total = getTotalFines();
        if (amount > total) return false;
        
        double remaining = amount;
        
        // Pay overdue fines first
        if (overdueFines > 0) {
            double pay = Math.min(remaining, overdueFines);
            overdueFines -= pay;
            remaining -= pay;
        }
        // Then pay damage fine if anything remains from amount
        if (remaining > 0 && damageFines > 0) {
        double pay = Math.min(remaining, damageFines);
        damageFines -= pay;
        }
        return true;
    }
}