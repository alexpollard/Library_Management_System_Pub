package service;

import model.Book;
import model.Member;

public class PerformanceBenchmarkTest {

    public static void main(String[] args) {
        LibrarySystem system = new LibrarySystem();

        benchmark("Register 1000 members", () -> {
            for (int i = 0; i < 1000; i++) {
                system.registerMember("User", "Number" + i);
            }
        });

        benchmark("Add 1000 books", () -> {
            for (int i = 0; i < 1000; i++) {
                system.addBook(new Book(
                        "ISBN" + i,
                        "Title" + i,
                        "Author" + i,
                        "Genre" + (i % 5),
                        3
                ));
            }
        });

        benchmark("Search by prefix (Title: 'Tit')", () -> {
            system.searchByPrefix("Title", "Tit");
        });

        benchmark("Checkout + return loop (100 iterations)", () -> {
            Member m = system.registerMember("Bench", "Mark");
            system.addBook(new Book("999", "Benchmark Book", "Author", "Test", 1));

            for (int i = 0; i < 100; i++) {
                system.checkoutBook(m.getMemberId(), "999", 1);
                system.returnBook(m.getMemberId(), "999", false);
            }
        });

        benchmark("Reservation queue stress test (1000 adds)", () -> {
            Member m = system.registerMember("Queue", "Tester");
            system.addBook(new Book("555", "Queue Book", "Author", "Test", 1));

            for (int i = 0; i < 1000; i++) {
                system.reserveBook(m.getMemberId(), "555");
            }
        });
    }

    private static void benchmark(String name, Runnable task) {
        long start = System.nanoTime();
        task.run();
        long end = System.nanoTime();

        long ms = (end - start) / 1_000_000;
        System.out.println(name + ": " + ms + " ms");
    }
}
