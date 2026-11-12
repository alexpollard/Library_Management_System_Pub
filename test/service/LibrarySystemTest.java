package service;

import model.*;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.*;

public class LibrarySystemTest {

    private LibrarySystem system;

    @Before
    public void setUp() {
        system = new LibrarySystem();
    }

    // ---------------- MEMBER TESTS ----------------

    @Test
    public void testRegisterAndGetMember() {
        Member m = system.registerMember("Alex", "Turner");
        assertNotNull(m);
        assertEquals("Alex", m.getFirstName());
        assertEquals("Turner", m.getSurname());

        Member fetched = system.getMember(m.getMemberId());
        assertEquals(m, fetched);
    }

    @Test
    public void testUpdateMemberName() {
        Member m = system.registerMember("Old", "Name");
        boolean updated = system.updateMemberName(m.getMemberId(), "New", "Name");
        assertTrue(updated);
        assertEquals("New", system.getMember(m.getMemberId()).getFirstName());
    }

    @Test
    public void testCanDeleteMember_NoLoansOrReservations() {
        Member m = system.registerMember("Test", "User");
        assertTrue(system.canDeleteMember(m.getMemberId()));
    }

    @Test
    public void testDeleteMember() {
        Member m = system.registerMember("Test", "User");
        assertTrue(system.deleteMember(m.getMemberId()));
        assertNull(system.getMember(m.getMemberId()));
    }

    // ---------------- BOOK TESTS ----------------

    @Test
    public void testAddAndGetBook() {
        Book b = new Book("123", "Test Book", "Author", "Genre", 3);
        system.addBook(b);

        Book fetched = system.getBookByISBN("123");
        assertNotNull(fetched);
        assertEquals("Test Book", fetched.getTitle());
    }

    @Test
    public void testDeleteBook() {
        Book b = new Book("123", "Test Book", "Author", "Genre", 3);
        system.addBook(b);

        system.deleteBook("123");
        assertNull(system.getBookByISBN("123"));
    }

    // ---------------- CHECKOUT & RETURN ----------------

    @Test
    public void testCheckoutBookSuccess() {
        Member m = system.registerMember("Alex", "Turner");
        Book b = new Book("123", "Test Book", "Author", "Genre", 1);
        system.addBook(b);

        boolean ok = system.checkoutBook(m.getMemberId(), "123", 2);
        assertTrue(ok);
        assertEquals(0, b.getAvailableCopies());
        assertEquals(1, m.getLoans().size());
    }

    @Test
    public void testCheckoutFailsIfAlreadyBorrowed() {
        Member m = system.registerMember("Alex", "Turner");
        Book b = new Book("123", "Test Book", "Author", "Genre", 1);
        system.addBook(b);

        system.checkoutBook(m.getMemberId(), "123", 2);
        boolean secondAttempt = system.checkoutBook(m.getMemberId(), "123", 2);

        assertFalse(secondAttempt);
    }

    @Test
    public void testReturnBookSuccess() {
        Member m = system.registerMember("Alex", "Turner");
        Book b = new Book("123", "Test Book", "Author", "Genre", 1);
        system.addBook(b);

        system.checkoutBook(m.getMemberId(), "123", 1);
        boolean ok = system.returnBook(m.getMemberId(), "123", false);

        assertTrue(ok);
        assertEquals(1, b.getAvailableCopies());
        assertTrue(m.getLoans().isEmpty());
    }

    // ---------------- RESERVATIONS ----------------

    @Test
    public void testReserveBook() {
        Member m = system.registerMember("Alex", "Turner");
        Book b = new Book("123", "Test Book", "Author", "Genre", 1);
        system.addBook(b);

        boolean reserved = system.reserveBook(m.getMemberId(), "123");
        assertTrue(reserved);

        Queue<Integer> q = system.getReservationQueue("123");
        assertEquals(1, q.size());
        assertEquals((Integer)m.getMemberId(), q.peek());
    }

    @Test
    public void testReservationBlocksCheckoutForOthers() {
        Member m1 = system.registerMember("A", "One");
        Member m2 = system.registerMember("B", "Two");
        Book b = new Book("123", "Test Book", "Author", "Genre", 1);
        system.addBook(b);

        system.reserveBook(m1.getMemberId(), "123");

        boolean m2Checkout = system.checkoutBook(m2.getMemberId(), "123", 1);
        assertFalse(m2Checkout);
    }

    @Test
    public void testFirstInQueueCanCheckout() {
        Member m1 = system.registerMember("A", "One");
        Book b = new Book("123", "Test Book", "Author", "Genre", 1);
        system.addBook(b);

        system.reserveBook(m1.getMemberId(), "123");

        boolean ok = system.checkoutBook(m1.getMemberId(), "123", 1);
        assertTrue(ok);
    }

    // ---------------- SEARCH ----------------

    @Test
    public void testSearchByTitle() {
        Book b = new Book("123", "Head First Java", "Author", "Programming", 1);
        system.addBook(b);

        List<Book> results = system.searchBooks("Title", "head first java");
        assertEquals(1, results.size());
        assertEquals("123", results.get(0).getISBN());
    }

    @Test
    public void testPrefixSearch() {
        Book b = new Book("123", "Harry Potter", "Rowling", "Fantasy", 1);
        system.addBook(b);

        List<Book> results = system.searchByPrefix("Title", "har");
        assertEquals(1, results.size());
        assertEquals("123", results.get(0).getISBN());
    }

    // ---------------- POPULARITY ----------------

    @Test
    public void testPopularBooksSince() {
        Book b = new Book("123", "Test Book", "Author", "Genre", 1);
        system.addBook(b);

        // Simulate borrow activity
        b.recordBorrowDate(LocalDate.now().minusDays(1));

        // Add to popularity cache manually
        system.checkoutBook(system.registerMember("Test", "User").getMemberId(), "123", 1);

        List<Book> results = system.getPopularBooksSince(LocalDate.now().minusDays(7), 5);
        assertTrue(results.stream().anyMatch(book -> book.getISBN().equals("123")));
    }
}
