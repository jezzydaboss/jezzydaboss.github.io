import java.lang.String;

/***********************************************************
 *  RescueAnimal
 *
 *  ENHANCEMENT NOTES (Milestone Three, CS 499):
 *  This class previously had a getter/setter naming mismatch
 *  that broke the build entirely: the field was set via
 *  setInServiceCountry() but could only be read back via
 *  getInServiceLocation() - a different name. Driver.java
 *  called getInServiceCountry() (matching the setter, not the
 *  getter), which does not exist on the original class, so the
 *  project failed to compile. The same mismatch pattern existed
 *  for acquisition location/country. Both accessor pairs are
 *  now consistently named so the setter and getter for the same
 *  field always share the same name.
 ***********************************************************/
public class RescueAnimal {

    // Instance variables
    private String name;
    private String animalType;
    private String gender;
    private String age;
    private String weight;
    private String acquisitionDate;
    private String acquisitionCountry;
    private String trainingStatus;
    private boolean reserved;
    private String inServiceCountry;

    // Constructor
    public RescueAnimal() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnimalType() {
        return animalType;
    }

    public void setAnimalType(String animalType) {
        this.animalType = animalType;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(String acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    // ENHANCEMENT: renamed from getAcquisitionLocation() to
    // getAcquisitionCountry() so it matches setAcquisitionCountry()
    // below (previously named setAcquisitionLocation()) - callers
    // throughout the codebase already expected "Country" naming.
    public String getAcquisitionCountry() {
        return acquisitionCountry;
    }

    public void setAcquisitionCountry(String acquisitionCountry) {
        this.acquisitionCountry = acquisitionCountry;
    }

    public boolean getReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    // ENHANCEMENT: renamed from getInServiceLocation() to
    // getInServiceCountry() so it matches setInServiceCountry() -
    // this mismatch is what made the original project fail to
    // compile, since Driver.java called a getter name that did
    // not exist on this class.
    public String getInServiceCountry() {
        return inServiceCountry;
    }

    public void setInServiceCountry(String inServiceCountry) {
        this.inServiceCountry = inServiceCountry;
    }

    public String getTrainingStatus() {
        return trainingStatus;
    }

    public void setTrainingStatus(String trainingStatus) {
        this.trainingStatus = trainingStatus;
    }
}
