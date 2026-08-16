package grazioso;

	public class Monkey extends RescueAnimal {
		// Instance variables
			private String tailLength; // Length of Monkeys tail
			private String height; // Height of Monkeys
			private String bodyLength; // Length of Monkeys Body
			private String species; // Species of Monkeys
			
			public Monkey(String name, String species, String tailLength, String height, String bodyLength, String gender, String age, String weight, boolean reserved,
					String acquisitionCountry, String trainingStatus, String acquisitionDate, String inServiceCountry) {
				
			setName(name);
		    setSpecies(species);
		    setTailLength(tailLength);
		    setHeight(height);
		    setBodyLength(bodyLength);
		    setGender(gender);
		    setAge(age);
		    setWeight(weight);
		    setAcquisitionDate(acquisitionDate);
		    setAcquisitionCountry(acquisitionCountry);
		    setTrainingStatus(trainingStatus);
		    setReserved(reserved);
		    setInServiceCountry(inServiceCountry);
			}
			private void setAcquisitionCountry(String acquisitionCountry) {
				return;
				
				
				
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
		        return getName() + ",       " + getTrainingStatus() + ",       " + getAcquisitionCountry() + ",       " + getReserved() + "\n";
			}
			public boolean getReserved() {
				
				return false;
			}
			public void setReserved(boolean b) {
				
				
			}

			}

			



