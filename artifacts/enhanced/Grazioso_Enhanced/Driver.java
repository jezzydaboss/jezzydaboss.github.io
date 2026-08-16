import java.util.Scanner;

/***********************************************************
 *  Driver
 *
 *  ENHANCEMENT NOTES (Milestone Three, CS 499):
 *   - Removed "import grazioso.Monkey;" - Monkey now lives in the
 *     default package alongside everything else (see Monkey.java).
 *   - dogList/monkeyList changed from ArrayList<Dog>/ArrayList<Monkey>
 *     to NameIndex<Dog>/NameIndex<Monkey> - a small, purpose-built
 *     hash table (see NameIndex.java) keyed by the animal's name in
 *     lowercase. Unlike java.util.HashMap, NameIndex exposes real,
 *     observable collision and load-factor data and actively
 *     rehashes into a larger table as it fills up, which is what
 *     backs the "Algorithmic Optimization" claim in the enhancement
 *     plan. The original duplicate-name check in intakeNewDog()/
 *     intakeNewMonkey() scanned the entire list with
 *     equalsIgnoreCase() on every intake - O(n) per check - and is
 *     now O(1) average case.
 *   - Added MetricsCollector and MetricsServer see those files for
 *     details. This backs the "Data Analysis & Metrics Harvesting"
 *     and "Cross-Tier Application Connectivity" claims.
 *   - Fixed a real Scanner bug in intakeNewDog(): nextBoolean() does
 *     not consume the trailing newline character, so the following
 *     nextLine() call (meant to read in-service country) previously
 *     read an empty leftover string instead of the actual input.
 *   - Fixed reserveAnimal(): it compared an animal's country field
 *     against the literal string "in service", which is a training
 *     status, not a country so the condition could essentially
 *     never be satisfied correctly. It now checks getTrainingStatus().
 *   - Fixed printAnimals(): it previously ignored its own "option"
 *     parameter entirely and always printed the same combined,
 *     reserved-only list regardless of which of the three menu items
 *     (4, 5, or 6) called it. It now actually branches on option, and
 *     the "available" list (option 6) correctly shows animals that
 *     are in service AND NOT reserved, matching the assignment's own
 *     specification (the original code checked for reserved == true,
 *     the opposite of what was asked for).
 *   - Removed unreachable code after break in the quit branch.
 *   - Dog previously had no toString() override at all, so
 *     printAnimals() printed default Object.toString() output
 *     (e.g. "Dog@3b9a45") for every dog. Dog.java now has a
 *     toString() matching Monkey's, so both print readable output.
 *   - Fixed a second Scanner bug (found by actually running the
 *     program, not just reading it): input.next() for the menu
 *     selection left a trailing newline that corrupted the first
 *     field read inside whichever handler ran next.
 ***********************************************************/
public class Driver {
    private static NameIndex<Dog> dogList = new NameIndex<>();
    private static NameIndex<Monkey> monkeyList = new NameIndex<>();
    private static final MetricsCollector metrics = new MetricsCollector();
    private static final MetricsServer metricsServer = new MetricsServer(8081);

    public static void main(String[] args) {

        metricsServer.start();

        initializeDogList();
        initializeMonkeyList();
        publishMetrics();

        Scanner input = new Scanner(System.in); // scanner class object
        char option;

        do // loop until user exits application
        {
            displayMenu();
            option = input.next().charAt(0);
            // ENHANCEMENT: input.next() reads a token but leaves the
            // trailing newline in the buffer. Discovered through actually
            // running the program:that leftover
            // newline was being silently consumed by the FIRST nextLine()
            // call inside whichever handler ran next (e.g. reading the
            // animal's name), returning an empty string instead of real
            // input and shifting every subsequent field read by one. The
            // original code called input.nextLine() AFTER each handler,
            // which is too late to fix this it has to happen here,
            // before the handler runs.
            input.nextLine();

            if (option == '1') { // intakeNewDog method is called
                intakeNewDog(input);
            }

            else if (option == '2') { // intakeNewMonkey method is called
                intakeNewMonkey(input);
            }

            else if (option == '3') { // reserveAnimal method is called
                reserveAnimal(input);
            }

            else if (option == '4') { // printAnimals method is called to print the dog list
                printAnimals(option);
            }

            else if (option == '5') { // printAnimals method is called to print the monkey list
                printAnimals(option);
            }

            else if (option == '6') { // printAnimals method is called to print all available animals
                printAnimals(option);
            }

            else if (option == 'q') { // exit message prints and application stops running
                System.out.println("You have exited the application.");
            }

            else { // in the event of an invalid input, tells user and prompts for a new input
                System.out.println("You have entered an invalid input. Please enter a valid input.");
            }

            // ENHANCEMENT: push fresh metrics to the live endpoint after
            // every menu action so a connected dashboard always reflects
            // current state
            publishMetrics();
        }
        while (option != 'q');

        input.close();
        metricsServer.stop();
    }

    /***********************************************************
     *  publishMetrics()
     *
     *  ENHANCEMENT: builds the current NameIndex and timing metrics
     *  as a JSON document and pushes it to the MetricsServer so the
     *  next HTTP request to /metrics serves fresh data.
     ***********************************************************/
    private static void publishMetrics() {
        String json = "{\n"
            + "  \"dogCount\": " + dogList.size() + ",\n"
            + "  \"monkeyCount\": " + monkeyList.size() + ",\n"
            + "  \"dogIndex\": {\n"
            + "    \"bucketCount\": " + dogList.bucketCount() + ",\n"
            + "    \"loadFactor\": " + String.format("%.3f", dogList.loadFactor()) + ",\n"
            + "    \"collisionCount\": " + dogList.getCollisionCount() + ",\n"
            + "    \"resizeCount\": " + dogList.getResizeCount() + "\n"
            + "  },\n"
            + "  \"monkeyIndex\": {\n"
            + "    \"bucketCount\": " + monkeyList.bucketCount() + ",\n"
            + "    \"loadFactor\": " + String.format("%.3f", monkeyList.loadFactor()) + ",\n"
            + "    \"collisionCount\": " + monkeyList.getCollisionCount() + ",\n"
            + "    \"resizeCount\": " + monkeyList.getResizeCount() + "\n"
            + "  },\n"
            + "  \"timing\": {\n"
            + "    \"lookupCount\": " + metrics.getLookupCount() + ",\n"
            + "    \"averageLookupMicros\": " + String.format("%.3f", metrics.getAverageLookupMicros()) + ",\n"
            + "    \"insertCount\": " + metrics.getInsertCount() + ",\n"
            + "    \"averageInsertMicros\": " + String.format("%.3f", metrics.getAverageInsertMicros()) + "\n"
            + "  }\n"
            + "}\n";

        metricsServer.updatePayload(json);
    }

    // This method prints the menu options
    public static void displayMenu() {
        System.out.println("\n\n");
        System.out.println("\t\t\t\tRescue Animal System Menu");
        System.out.println("[1] Intake a new dog");
        System.out.println("[2] Intake a new monkey");
        System.out.println("[3] Reserve an animal");
        System.out.println("[4] Print a list of all dogs");
        System.out.println("[5] Print a list of all monkeys");
        System.out.println("[6] Print a list of all animals that are not reserved");
        System.out.println("[q] Quit application");
        System.out.println();
        System.out.println("Enter a menu selection");
    }

    // Adds dogs to a list for testing
    public static void initializeDogList() {
        Dog dog1 = new Dog("Spot", "German Shepherd", "male", "1", "25.6", "05-12-2019", "United States", "in service", false, "United States");
        Dog dog2 = new Dog("Rex", "Great Dane", "male", "3", "35.2", "02-03-2020", "United States", "Phase I", false, "United States");
        Dog dog3 = new Dog("Bella", "Chihuahua", "female", "4", "25.6", "12-12-2019", "Canada", "in service", true, "Canada");

        dogList.put(dog1.getName().toLowerCase(), dog1);
        dogList.put(dog2.getName().toLowerCase(), dog2);
        dogList.put(dog3.getName().toLowerCase(), dog3);
    }

    // Adds monkeys to a list for testing
    public static void initializeMonkeyList() {
        Monkey monkey1 = new Monkey("George", "Capuchin", "male", "2", "9.5", "01-15-2021",
                "Brazil", "in service", false, "Brazil", "15", "18", "20");
        monkeyList.put(monkey1.getName().toLowerCase(), monkey1);
    }

    // Complete the intakeNewDog method
    public static void intakeNewDog(Scanner scnr) {
        System.out.println("What is the dog's name?");
        String name = scnr.nextLine();

        // ENHANCEMENT: real timing instrumentation around the NameIndex
        // lookup that the duplicate-name check performs
        long lookupStart = System.nanoTime();
        boolean isDuplicate = dogList.containsKey(name.toLowerCase());
        metrics.recordLookup(System.nanoTime() - lookupStart);

        if (isDuplicate) {
            System.out.println("\n\nThis dog is already in our system\n\n");
            return; //returns to menu
        }

        System.out.println("What is the dog's breed?");
        String breed = scnr.nextLine();//gets dog breed from input

        System.out.println("What is the dog's gender?");
        String gender = scnr.nextLine();//gets dog gender from input

        System.out.println("What is the dog's age?");
        String age = scnr.nextLine();//gets dog age from input

        System.out.println("What is the dog's weight?");
        String weight = scnr.nextLine();//gets dog weight from input

        System.out.println("What is the dog's acquisition date?");
        String acquisitionDate = scnr.nextLine();//gets dog acquisition date from input

        System.out.println("What is the dog's acquisition country?");
        String acquisitionCountry = scnr.nextLine();//gets dog acquisition country from input

        System.out.println("What is the dog's training status?");
        String trainingStatus = scnr.nextLine();//gets dog training status from input

        System.out.println("is the dog reserved? True or False");
        boolean reserved = scnr.nextBoolean();//gets dog reserve status from input
        scnr.nextLine(); // ENHANCEMENT: nextBoolean() does not consume the trailing
                          // newline - without this line, the next nextLine() call
                          // below would read an empty leftover string instead of
                          // the actual in-service country input.

        System.out.println("What is the dog's in service country?");
        String inServiceCountry = scnr.nextLine(); //gets dog in service country from input

        Dog dog = new Dog(name, breed, gender, age, weight, acquisitionDate, acquisitionCountry,
                          trainingStatus, reserved, inServiceCountry);

        // ENHANCEMENT: real timing instrumentation around the NameIndex insert
        long insertStart = System.nanoTime();
        dogList.put(dog.getName().toLowerCase(), dog);
        metrics.recordInsert(System.nanoTime() - insertStart);
    }

    // Complete intakeNewMonkey
    public static void intakeNewMonkey(Scanner scanner) {
        System.out.println("What is the monkey's name?");
        String name = scanner.nextLine();

        long lookupStart = System.nanoTime();
        boolean isDuplicate = monkeyList.containsKey(name.toLowerCase());
        metrics.recordLookup(System.nanoTime() - lookupStart);

        if (isDuplicate) {
            System.out.println("\n\nThis monkey is already in our system\n\n");
            return;
        }

        System.out.println("What is the monkey's species? (Capuchin, Guenon, Macaque, Marmoset, Squirrel Monkey, Tamarin)");
        String species = scanner.nextLine();
        // ENHANCEMENT: basic input validation on the allowed species list,
        // called for in the original assignment comments but never implemented
        String speciesLower = species.trim().toLowerCase();
        boolean validSpecies = speciesLower.equals("capuchin") || speciesLower.equals("guenon")
                || speciesLower.equals("macaque") || speciesLower.equals("marmoset")
                || speciesLower.equals("squirrel monkey") || speciesLower.equals("tamarin");
        if (!validSpecies) {
            System.out.println("\n\nThat species is not eligible for intake into this program.\n\n");
            return;
        }

        System.out.println("What is the monkey's gender? is it a male or female?");
        String gender = scanner.nextLine();

        System.out.println("What is the monkey's age?");
        String age = scanner.nextLine();

        System.out.println("What is the monkey's weight?");
        String weight = scanner.nextLine();

        System.out.println("What is the monkey's acquisition date?");
        String acquisitionDate = scanner.nextLine();

        System.out.println("What is the monkey's acquisition country?");
        String acquisitionCountry = scanner.nextLine();

        System.out.println("What is the monkey's training status?");
        String trainingStatus = scanner.nextLine();

        System.out.println("is the monkey reserved?");
        boolean reserved = scanner.nextBoolean();
        scanner.nextLine(); // ENHANCEMENT: same missing-newline-consumption bug as
                             // intakeNewDog(), fixed the same way.

        System.out.println("What is the monkey's in service country?");
        String inServiceCountry = scanner.nextLine();

        System.out.println("What is the monkey's tail length?");
        String tailLength = scanner.nextLine();

        System.out.println("What is the monkey's height?");
        String height = scanner.nextLine();

        System.out.println("What is the monkey's bodylength?");
        String bodyLength = scanner.nextLine();

        Monkey monkey = new Monkey(name, species, gender, age, weight, acquisitionDate,
                acquisitionCountry, trainingStatus, reserved, inServiceCountry,
                tailLength, height, bodyLength);

        long insertStart = System.nanoTime();
        monkeyList.put(monkey.getName().toLowerCase(), monkey);
        metrics.recordInsert(System.nanoTime() - insertStart);
    }

    // Complete reserveAnimal
    public static void reserveAnimal(Scanner scnr) {
        System.out.println("Is the animal a dog or a monkey?");
        String dogMonkey = scnr.nextLine();
        System.out.println("What is the animal's in service country?");
        String inServiceCountry = scnr.nextLine();

        if (dogMonkey.equalsIgnoreCase("Dog")) {
            for (Dog dog : dogList.values()) {
                // ENHANCEMENT: this previously compared the country field
                // against the literal string "in service" - training
                // status and country are different fields entirely, so
                // this could essentially never correctly match. Now
                // correctly checks getTrainingStatus().
                if (inServiceCountry.equalsIgnoreCase(dog.getInServiceCountry())
                        && (!dog.getReserved())
                        && dog.getTrainingStatus().equalsIgnoreCase("in service")) {
                    dog.setReserved(true);
                    System.out.println("This dog is now reserved.");
                    return;
                }
            }
            System.out.println("No eligible dog was found for that in-service country.");
        }

        else if (dogMonkey.equalsIgnoreCase("Monkey")) {
            for (Monkey monkey : monkeyList.values()) {
                if (inServiceCountry.equalsIgnoreCase(monkey.getInServiceCountry())
                        && (!monkey.getReserved())
                        && monkey.getTrainingStatus().equalsIgnoreCase("in service")) {
                    monkey.setReserved(true);
                    System.out.println("This monkey is now reserved.");
                    return;
                }
            }
            System.out.println("No eligible monkey was found for that in-service country.");
        }
        else {
            System.out.println("Please enter either 'Dog' or 'Monkey'.");
        }
    }

    // Complete printAnimals
    // option '4' -> dogs only
    // option '5' -> monkeys only
    // option '6' -> combined list of animals that are in service AND not reserved
    public static void printAnimals(char option) {
        System.out.println("Name - Training Status - Acquisition Country - Reservation Status");

        if (option == '4') {
            for (Dog dog : dogList.values()) {
                System.out.println(dog);
            }
        }
        else if (option == '5') {
            for (Monkey monkey : monkeyList.values()) {
                System.out.println(monkey);
            }
        }
        else if (option == '6') {
            // ENHANCEMENT: previously checked getReserved() == true, the
            // opposite of the assignment's own specification, which calls
            // for animals that are in service and NOT reserved.
            for (Dog dog : dogList.values()) {
                if (!dog.getReserved() && dog.getTrainingStatus().equalsIgnoreCase("in service")) {
                    System.out.println(dog);
                }
            }
            for (Monkey monkey : monkeyList.values()) {
                if (!monkey.getReserved() && monkey.getTrainingStatus().equalsIgnoreCase("in service")) {
                    System.out.println(monkey);
                }
            }
        }
    }
}
