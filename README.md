# Library Management System

A Java Swing library-management application developed for a data structures and algorithms unit. The application keeps its data in memory and supports member registration, book catalogue management, loans and returns, reservations, fines, and book search.

## Data Structures

- `HashMap` registries for members, books, and reservations
- `TreeMap` secondary indexes for title, author, and genre searches, including prefix search
- FIFO queues to manage book reservations
- An LRU cache to track recently borrowed books

## Build and Run

Requires JDK 21 and Apache Ant. Open the project in NetBeans or run:

```sh
ant run
```

Run the unit tests with:

```sh
ant test
```

The tests use JUnit 4.13.2 and Hamcrest Core 1.3. For the NetBeans Ant build, place those JARs in `~/JUnit4` or update their paths in `nbproject/project.properties`.

## Scope

This is an educational prototype: data is not persisted between runs, and staff login is not backed by account authentication.