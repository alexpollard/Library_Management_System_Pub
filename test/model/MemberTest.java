package model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.util.List;

public class MemberTest {

    private Member member;

    @Before
    public void setUp() {
        member = new Member(101, "Alex", "Turner");
    }

    @Test
    public void testGetMemberId() {
        assertEquals(101, member.getMemberId());
    }

    @Test
    public void testGetFirstName() {
        assertEquals("Alex", member.getFirstName());
    }

    @Test
    public void testGetSurname() {
        assertEquals("Turner", member.getSurname());
    }

    @Test
    public void testGetFullName() {
        assertEquals("Alex Turner", member.getFullName());
    }

    @Test
    public void testGetLoansInitiallyEmpty() {
        List<Loan> loans = member.getLoans();
        assertNotNull(loans);
        assertTrue(loans.isEmpty());
    }

    @Test
    public void testAddLoan() {
        Loan loan = new Loan(
            1,
            "123",
            LocalDate.parse("2026-01-08"),
            LocalDate.parse("2026-01-15")
        );
        member.addLoan(loan);
        assertEquals(1, member.getLoans().size());
        assertEquals("123", member.getLoans().get(0).getIsbn());
    }

    @Test
    public void testRemoveLoanByISBN() {
        Loan loan = new Loan(
            1,
            "123",
            LocalDate.parse("2026-01-08"),
            LocalDate.parse("2026-01-15")
        );
        member.addLoan(loan);
        boolean removed = member.removeLoanByISBN("123");
        assertTrue(removed);
        assertTrue(member.getLoans().isEmpty());
    }

    @Test
    public void testAddOverdueFine() {
        member.addOverdueFine(2.50);
        assertEquals(2.50, member.getOverdueFines(), 0.001);
    }

    @Test
    public void testAddDamageFine() {
        member.addDamageFine(1.75);
        assertEquals(1.75, member.getDamageFines(), 0.001);
    }

    @Test
    public void testGetTotalFines() {
        member.addOverdueFine(2.50);
        member.addDamageFine(1.75);
        assertEquals(4.25, member.getTotalFines(), 0.001);
    }

    @Test
    public void testPayFinePartial() {
        member.addOverdueFine(5.00);
        boolean success = member.payFine(2.00);
        assertTrue(success);
        assertEquals(3.00, member.getTotalFines(), 0.001);
    }

    @Test
    public void testPayFineFull() {
        member.addOverdueFine(5.00);
        boolean success = member.payFine(5.00);
        assertTrue(success);
        assertEquals(0.00, member.getTotalFines(), 0.001);
    }

    @Test
    public void testSetFirstName() {
        member.setFirstName("Jamie");
        assertEquals("Jamie", member.getFirstName());
    }

    @Test
    public void testSetSurname() {
        member.setSurname("Cook");
        assertEquals("Cook", member.getSurname());
    }
}
