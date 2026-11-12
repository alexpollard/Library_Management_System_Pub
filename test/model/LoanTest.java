package model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;

public class LoanTest {

    private Loan loan;

    @Before
    public void setUp() {
        loan = new Loan(
            101,
            "9781234567890",
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2026-01-15")
        );
    }

    @Test
    public void testGetMemberId() {
        assertEquals(101, loan.getMemberId());
    }

    @Test
    public void testGetIsbn() {
        assertEquals("9781234567890", loan.getIsbn());
    }

    @Test
    public void testGetCheckoutDate() {
        assertEquals(LocalDate.parse("2026-01-01"), loan.getCheckoutDate());
    }

    @Test
    public void testGetDueDate() {
        assertEquals(LocalDate.parse("2026-01-15"), loan.getDueDate());
    }

    @Test
    public void testIsOverdueBeforeDueDate() {
        LocalDate today = LocalDate.parse("2026-01-10");
        assertFalse(loan.isOverdue(today));
    }

    @Test
    public void testIsOverdueAfterDueDate() {
        LocalDate today = LocalDate.parse("2026-01-20");
        assertTrue(loan.isOverdue(today));
    }

    @Test
    public void testIsOverdueOnDueDate() {
        LocalDate today = LocalDate.parse("2026-01-15");
        assertFalse(loan.isOverdue(today)); // assuming due date is inclusive
    }
}
