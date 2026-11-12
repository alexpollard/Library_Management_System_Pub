package ui;

import model.Book;
import service.LibrarySystem;

/**
 *
 * @author alex pollard - s2342835
 */

public class Main {
    public static void main(String[] args) {
        // Create the system and preload members
        LibrarySystem system = new LibrarySystem();
        system.preloadDemoData();
        
        // Launch the login window
        java.awt.EventQueue.invokeLater(() -> new Login(system).setVisible(true));
    }
}
