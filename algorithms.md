# Algorithms and Data Structures — Rescue Animal Management System

[Home](index.md) | [Software Engineering](software-engineering.md) | [Algorithms](algorithms.md) | [Databases](databases.md) | [Code Review](code-review.md)

---

## Artifact Description

The artifact is a Java console application for managing rescue dogs and monkeys, originally built for IT 145, Foundation in Application Development. It's a menu-driven program supporting animal intake, reservation by service country, and filtered listing, built around a `RescueAnimal` parent class with `Dog` and `Monkey` subclasses.

**Download the full code:** original and enhanced source files are in this repository under `/artifacts/algorithms/`.

## Justification for Inclusion

I selected this as a replacement for my original CS 260 artifact because it gave me a much stronger algorithms and data structures story — real linear-search collection usage, and a real opportunity to move to a hash-based structure. During my code review I documented several defects: a Scanner newline bug, a broken reservation check, a `printAnimals()` method that ignored its own parameter, and a scrambled `Monkey` constructor call.

When I actually sat down to perform the enhancement, I found the project **did not compile at all**. `Monkey` was declared in a package called `grazioso` while `RescueAnimal`, `Dog`, and `Driver` were all in Java's default, unnamed package — Java does not allow a named package to extend a class from the unnamed package. `Driver.java` also called `getInServiceCountry()` and `getAcquisitionCountry()`, methods that didn't exist on `RescueAnimal`, which only defined `getInServiceLocation()` and `getAcquisitionLocation()`. Neither defect was visible from reading the code — only from actually compiling it.

### What I Fixed

- Fixed the cross-package compile failure and the mismatched getter/setter names
- Fixed a Scanner newline bug that corrupted the first field read after every menu selection — found by actually running the program, not just reading it
- Fixed the reservation logic, the `printAnimals()` option branching, and an inverted "available animals" filter
- Fixed the `Monkey` constructor's parameter order, which had been silently scrambling every monkey's data
- Replaced `ArrayList` with a **custom hash table** (`NameIndex`) using separate chaining, with real observable collision counting and automatic resizing once load factor crosses 0.75
- Added real timing instrumentation and a live HTTP metrics endpoint (Java's built-in `HttpServer`, no external dependencies)

### Figure 1 — Custom Hash Table

![NameIndex collision tracking](assets/screenshots/artifact2_shot1.png)

*Real, observable collision tracking as part of the custom hash table, replacing `java.util.HashMap`'s black-box behavior.*

### Figure 2 — Scanner Bug Fix

![Driver.java Scanner newline bug fix](assets/screenshots/artifact2_shot2.png)

*The menu-selection Scanner newline bug, found by actually running the program rather than only reading it, and fixed by consuming the leftover newline before dispatching to a handler.*

## Course Outcomes

This enhancement demonstrates progress toward **Outcomes 3 and 4**: diagnosing and correcting defects hidden behind a cascading compiler failure, then upgrading from sequential array storage to a hash-based structure with real, tested collision and load-factor data, required managing real trade-offs between correctness, memory overhead, and lookup speed.

## Reflection

The clearest lesson here was that even after code compiles, you still have to run it. I found a second, more fundamental version of a Scanner bug I thought I'd already fixed only by actually executing the program with scripted input. I also chose to fix the `Monkey` constructor itself rather than the call site that used it, since the constructor's parameter order was the one inconsistent with the rest of the codebase — a more defensible engineering decision even though it meant touching more code. Verified with a real `javac` compile and multiple scripted test runs covering intake, duplicate rejection, reservation, and all three print modes.
