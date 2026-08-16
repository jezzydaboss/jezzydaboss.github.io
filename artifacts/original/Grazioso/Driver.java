import java.util.ArrayList;
import java.util.Scanner;

import grazioso.Monkey;

public class Driver {
    private static ArrayList<Dog> dogList = new ArrayList<Dog>();
    private static ArrayList<Monkey> monkeyList = new ArrayList<Monkey>();
    // Instance variables (if needed)

    public static void main(String[] args) {


        initializeDogList();
        initializeMonkeyList();

        // Add a loop that displays the menu, accepts the users input
        // and takes the appropriate action.
	// For the project submission you must also include input validation
        // and appropriate feedback to the user.
        // Hint: create a Scanner and pass it to the necessary
        // methods 
	// Hint: Menu options 4, 5, and 6 should all connect to the printAnimals() method.
        Scanner input = new Scanner(System.in); // scanner class object
       char option;
        
        do // loop until user exits application
        {
            displayMenu();
            option = input.next().charAt(0);
            if (option == '1') { // intakeNewDog method is called
                intakeNewDog(input);
                input.nextLine();
            }
            
            else if (option == '2') { // intakeNewMonkey method is called
                intakeNewMonkey(input);
                input.nextLine();
            }
            
            else if (option == '3') { // reserveAnimal method is called
                reserveAnimal(input);
                input.nextLine();
            }
            
            else if (option == '4') { // printAnimals method is called to print the dog list
                printAnimals(option);
                input.nextLine();
            }
            
            else if (option == '5') { // printAnimals method is called to print the monkey list
                printAnimals(option);
                input.nextLine();
            }
            
            else if (option == '6') { // printAnimals method is called to print all available animals
                printAnimals(option);
                input.nextLine();
            }
            
            else if (option == 'q') { // exit message prints and application stops running
                System.out.print("You have exited the application.");
                break;
                input.nextLine();
            }
            
            else { // in the event of an invalid input, tells user and prompts for a new input
                System.out.print("You have entered an invalid input. Please enter a valid input.");
                input.nextLine();
            }
        }
        while (option != 'q');
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
        Dog dog1 = new Dog("Spot", "German Shepherd", "male", "1", "25.6", "05-12-2019", "United States", "intake", false, "United States");
        Dog dog2 = new Dog("Rex", "Great Dane", "male", "3", "35.2", "02-03-2020", "United States", "Phase I", false, "United States");
        Dog dog3 = new Dog("Bella", "Chihuahua", "female", "4", "25.6", "12-12-2019", "Canada", "in service", true, "Canada");

        dogList.add(dog1);
        dogList.add(dog2);
        dogList.add(dog3);
    }


    // Adds monkeys to a list for testing
    //Optional for testing
    public static void initializeMonkeyList() {

    }


    // Complete the intakeNewDog method
    // The input validation to check that the dog is not already in the list
    // is done for you
    public static void intakeNewDog(Scanner scnr) {
        System.out.println("What is the dog's name?");
        String name = scnr.nextLine();
        for(Dog dog: dogList) {
            if(dog.getName().equalsIgnoreCase(name)) {
                System.out.println("\n\nThis dog is already in our system\n\n");
                return; //returns to menu

            }
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

        System.out.println("What is the dog's in service country?");
        String inServiceCountry = scnr.nextLine(); //gets dog in service country from input
      
        Dog dog = new Dog(name, breed, gender, age, weight, acquisitionDate, acquisitionCountry, 
                          trainingStatus, reserved, inServiceCountry);//dog strings for array to store
        dogList.add(dog);
    
      
    }


        // Complete intakeNewMonkey
	//Instantiate and add the new monkey to the appropriate list
        // For the project submission you must also  validate the input
	// to make sure the monkey doesn't already exist and the species type is allowed
        public static void intakeNewMonkey(Scanner scanner) {
                System.out.println("What is the monkey's name?");
                String name = scanner.nextLine();
                for(Monkey monkey: monkeyList) {
                    if(monkey.getName().equalsIgnoreCase(name)) {
                        System.out.println("\n\nThis monkey is already in our system\n\n");
                        return;
                      //returns to menu
                    }
                }
          System.out.println("What is the monkey's gender? is it a male or female?");
          String gender = scanner.nextLine();//gets monkey gender from input
          
          System.out.println("What is the monkey's age?");
          String age = scanner.nextLine();//gets monkey age from input
          
          System.out.println("What is the monkey's weight?");
          String weight = scanner.nextLine();//gets monkey weight from input
         
          System.out.println("What is the monkey's acquisition date?");
          String acquisitionDate = scanner.nextLine();//gets monkey acquisition date from input
         
          System.out.println("What is the monkey's acquisition country?");
          String acquisitionCountry = scanner.nextLine();//gets monkey acquisition country from input
          
          System.out.println("What is the monkey's training status?");
          String trainingStatus = scanner.nextLine(); //gets monkey training status from input
          
          System.out.println("is the monkey reserved?");
          boolean reserved = scanner.nextBoolean();//gets monkey reserved status from input
          
          System.out.println("What is the monkey's in service country?");
          String inServiceCountry = scanner.nextLine();//gets monkey in service country from input
          
          System.out.println("What is the monkey's tail length?");
          String tailLength = scanner.nextLine(); //gets monkey tail length from input
          
          System.out.println("What is the monkey's height?");
          String height = scanner.nextLine();//gets monkey height from input
          
          System.out.println("What is the monkey's bodylength?");
          String bodyLength = scanner.nextLine(); //gets monkey body length from input
         
          System.out.println("What is the monkey's species?");
          String species = scanner.nextLine();//gets monkey species from input
          
          Monkey monkey = new Monkey(name, species, gender, age, weight, acquisitionDate, acquisitionCountry, trainingStatus,
                                     reserved, inServiceCountry, tailLength, height, bodyLength);//monkey strings to add to array
        	    monkeyList.add(monkey);//adds monkey to list
                         
    }

        // Complete reserveAnimal
        // You will need to find the animal by animal type and in service country
        public static void reserveAnimal(Scanner scnr) {
          System.out.println("Is the animal a dog or a monkey?");
          String dogMonkey = scnr.nextLine();
          System.out.println("What is the animals in service country");
          String inServiceCountry = scnr.nextLine();
        
          if (dogMonkey.equalsIgnoreCase("Dog")){//if dog is typed chekcs dogList
            	
            for(Dog dog: dogList){
                if (inServiceCountry.equalsIgnoreCase(dog.getInServiceCountry()) && ( !dog.getReserved() ) &&//getter for in service country and reserved status
                    dog.getInServiceCountry().equals("in service")) {
                    dog.setReserved(true);//sets reserved status
                    System.out.println("This dog is now reserved.");
                    return;
                }
            }
          }
           
          else if (dogMonkey.equalsIgnoreCase("Monkey")){//if monkey is typed checks monkey list
            	
            for(Monkey monkey: monkeyList){//if monkey is typed checks monkey list
                if (inServiceCountry.equalsIgnoreCase(monkey.getInServiceCountry()) && ( !monkey.getReserved() ) &&//gets reserved status and in service country
                    monkey.getInServiceCountry().equals("in service")) {
                    monkey.setReserved(true);//sets reserved status
                    System.out.println("This monkey is now reserved.");
                    return;
                }
            }
          }
          
        

        
        }

        // Complete printAnimals
     
        // Include the animal name, status, acquisition country and if the animal is reserved.
	// Remember that this method connects to three different menu items.
        // The printAnimals() method has three different outputs
        // based on the listType parameter
        // dog - prints the list of dogs
        // monkey - prints the list of monkeys
        // available - prints a combined list of all animals that are
        // fully trained ("in service") but not reserved 
	// Remember that you only have to fully implement ONE of these lists. 
	// The other lists can have a print statement saying "This option needs to be implemented".
	// To score "exemplary" you must correctly implement the "available" list.
        public static void printAnimals(char option) {
            System.out.println("Name - " + "Training Status - " + "Acquisition Country - " + " Reservation Status");
            for (int i = 0; i < dogList.size(); ++i) {
                if (dogList.get(i).getReserved() && dogList.get(i).getTrainingStatus().equals("in service"))
                    System.out.println(dogList.get(i));
            }

            for (int i = 0; i < monkeyList.size(); ++i) {
                if (monkeyList.get(i).getReserved() && monkeyList.get(i).getTrainingStatus().equals("in service"))
                    System.out.println(monkeyList.get(i));
            }
        }
    }
