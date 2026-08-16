/***********************************************************
 *  Monkey
 *
 *  ENHANCEMENT NOTES (Milestone Three, CS 499):
 *   - Removed "package grazioso;" - Monkey was the only class in
 *     this package while RescueAnimal, Dog, and Driver were all in
 *     Java's default (unnamed) package. A named package cannot
 *     import or extend a class in the unnamed package, so the
 *     original project failed to compile with "cannot find symbol:
 *     class RescueAnimal" and a cascade of related errors. Moving
 *     Monkey into the default package alongside its dependencies
 *     resolves this.
 *   - The constructor's parameter order previously did not match
 *     how Driver.intakeNewMonkey() actually calls it (gender was
 *     passed into the tailLength slot, age into height, etc.),
 *     silently scrambling every monkey's data with no compiler
 *     error, since every parameter happened to be the same type
 *     (String) or coincidentally aligned (reserved). The
 *     constructor signature below matches Driver's actual call
 *     order and follows the same convention Dog.java already
 *     uses: shared RescueAnimal fields first, subclass-specific
 *     fields (species, tailLength, height, bodyLength) last.
 *   - Removed the private setAcquisitionCountry(String) stub that
 *     took its parameter and did nothing with it - a dead method
 *     that silently shadowed the real, working setter inherited
 *     from RescueAnimal (now correctly named setAcquisitionCountry()
 *     there too - see RescueAnimal.java).
 *   - Removed the broken getReserved()/setReserved(boolean)
 *     overrides, which unconditionally returned false and did
 *     nothing, respectively - meaning a monkey could never
 *     actually be reserved no matter what the rest of the program
 *     did. Removing them lets Monkey use RescueAnimal's real,
 *     working implementation instead.
 ***********************************************************/

public class Monkey extends RescueAnimal {
	// Instance variables
	private String tailLength; // Length of Monkeys tail
	private String height; // Height of Monkeys
	private String bodyLength; // Length of Monkeys Body
	private String species; // Species of Monkeys

	public Monkey(String name, String species, String gender, String age, String weight,
			String acquisitionDate, String acquisitionCountry, String trainingStatus,
			boolean reserved, String inServiceCountry,
			String tailLength, String height, String bodyLength) {

		setName(name);
		setSpecies(species);
		setGender(gender);
		setAge(age);
		setWeight(weight);
		setAcquisitionDate(acquisitionDate);
		setAcquisitionCountry(acquisitionCountry);
		setTrainingStatus(trainingStatus);
		setReserved(reserved);
		setInServiceCountry(inServiceCountry);
		setTailLength(tailLength);
		setHeight(height);
		setBodyLength(bodyLength);
	}

	public String getTailLength() { // Accessor method for tailLength
		return tailLength;
	}
	public void setTailLength(String tailLength) { // Mutator method for tailLength
		this.tailLength = tailLength;
	}
	public String getHeight() { // Accessor method for height
		return height;
	}
	public void setHeight(String height) { // Mutator method for height
		this.height = height;
	}
	public String getBodyLength() { // Accessor method for bodyLength
		return bodyLength;
	}
	public void setBodyLength(String bodyLength) { // Mutator method for bodyLength
		this.bodyLength = bodyLength;
	}

	public String getSpecies() { // Accessor method for species
		return species;
	}
	public void setSpecies(String species) { // Mutator method for species
		this.species = species;
	}

	@Override
	public String toString() { // Overrides the toString method
		return getName() + " (Monkey, " + species + ") - " + getTrainingStatus()
				+ " - " + getAcquisitionCountry() + " - reserved: " + getReserved();
	}
}
