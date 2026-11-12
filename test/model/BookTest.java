package model;

import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class BookTest {

    @Test
    public void testConstructorAndGetters() {
        Book b = new Book("123", "Test Title", "Author Name", "Fantasy", 5);

        assertEquals("123", b.getISBN());
        assertEquals("Test Title", b.getTitle());
        assertEquals("Author Name", b.getAuthor());
        assertEquals("Fantasy", b.getGenre());
        assertEquals(5, b.getTotalCopies());
        assertEquals(5, b.getAvailableCopies());
        assertEquals(0, b.getBorrowCount());
    }

    @Test
    public void testBorrowCopyReducesAvailableCopies() {
        Book b = new Book("123", "Test", "Author", "Genre", 3);
        b.borrowCopy();
        assertEquals(2, b.getAvailableCopies());
    }

    @Test
    public void testBorrowCopyIncrementsBorrowCount() {
        Book b = new Book("123", "Test", "Author", "Genre", 3);
        b.borrowCopy();
        assertEquals(1, b.getBorrowCount());
    }

    @Test
    public void testCannotBorrowWhenNoCopies() {
        Book b = new Book("123", "Test", "Author", "Genre", 0);
        assertFalse(b.canBorrow());
    }

    @Test
    public void testReturnCopyIncreasesAvailableCopies() {
        Book b = new Book("123", "Test", "Author", "Genre", 1);
        b.borrowCopy();   // now 0 available
        b.returnCopy();   // should go back to 1
        assertEquals(1, b.getAvailableCopies());
    }

    @Test
    public void testRecordBorrowDateAndCountSince() {
        Book b = new Book("123", "Test", "Author", "Genre", 3);

        LocalDate cutoff = LocalDate.now().minusDays(14);

        b.recordBorrowDate(LocalDate.now().minusDays(3));
        b.recordBorrowDate(LocalDate.now().minusDays(1));

        assertEquals(2, b.getBorrowCountSince(cutoff));
    }
}
