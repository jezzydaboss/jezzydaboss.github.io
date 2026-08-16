/***********************************************************
 *  Dog
 *
 *  ENHANCEMENT NOTE (Milestone Three, CS 499): the constructor
 *  called setAcquisitionLocation(), which no longer exists now
 *  that RescueAnimal's accessor was renamed to
 *  setAcquisitionCountry() for naming consistency updated to
 *  match.
 ***********************************************************/
public class Dog extends RescueAnimal {

    // Instance variable
    private String breed;

    // Constructor
    public Dog(String name, String breed, String gender, String age,
    String weight, String acquisitionDate, String acquisitionCountry,
	String trainingStatus, boolean reserved, String inServiceCountry) {
        setName(name);
        setBreed(breed);
        setGender(gender);
        setAge(age);
        setWeight(weight);
        setAcquisitionDate(acquisitionDate);
        setAcquisitionCountry(acquisitionCountry);
        setTrainingStatus(trainingStatus);
        setReserved(reserved);
        setInServiceCountry(inServiceCountry);
    }

    // Accessor Method
    public String getBreed() {
        return breed;
    }

    // Mutator Method
    public void setBreed(String dogBreed) {
        breed = dogBreed;
    }

    // ENHANCEMENT: added for consistent, readable output when printing
    // dogs (used by the enhanced printAnimals() in Driver)
    @Override
    public String toString() {
        return getName() + " (Dog, " + breed + ") - " + getTrainingStatus()
                + " - " + getAcquisitionCountry() + " - reserved: " + getReserved();
    }
}
