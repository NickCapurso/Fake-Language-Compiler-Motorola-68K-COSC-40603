

//*************************************************************************************************
//Begin Symbol Table Entry Class
//	This class represents an entry in the symbol table
//
//*************************************************************************************************
public class SymbolTableEntry {
	
	//*********************************************************************************************
	// Begin Location Enumeration
	//		Because the location of an entry can only be a register or memory, it lends itself
	//		to an enumeration type
	//*********************************************************************************************
	public enum Location{ 
			REMOVED (-2), //For temporaries (they don't get moved to memory)
			MEMORY (-1), 
			D0 (0), 
			D1 (1), 
			D2 (2), 
			D3 (3), 
			D4 (4), 
			D5 (5), 
			D6 (6), 
			D7 (7);
			
			//Each enumeration is assigned a value - for registers, this corresponds to their
			//index into the register table
			public final int value;
			Location(int value){
				this.value = value;
			}
			
			//Given a number (i.e. an index into the register table), will return
			//the according Location. (e.x. 0 => D0
			public static Location getLocationFromValue(int i){
				switch(i){
					case -2: return REMOVED;
					case -1: return MEMORY; 
					case 0: return D0;
					case 1: return D1;
					case 2: return D2;
					case 3: return D3;
					case 4: return D4;
					case 5: return D5;
					case 6: return D6;
					case 7: return D7;
				}
				return null;
			}
		}
	//*********************************************************************************************
	// End Location Enumeration
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	//
	// Class Variables
	//
	// Variables		Type			Description
	// ---------		--------		-------------------------------------------
	// identifier		String			The variable name			
	// nextUse			int				The next use of this entry (in terms of quad number)
	// location			Location		The location of this entry - memory, register, etc
	//
	//*********************************************************************************************
	private String identifier;
	private int nextUse;
	private Location location;
	
	
	//*********************************************************************************************
	// Begin Constructor 
	//		Initializes class variables
	//
	//*********************************************************************************************
	public SymbolTableEntry(String identifier, int nextUse, Location location){
		this.identifier = identifier;
		this.nextUse = nextUse;
		this.location = location;
	}
	//*********************************************************************************************
	// End Constructor
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Get/Set Methods
	//*********************************************************************************************
	public void setNextUse(int nextUse){
		this.nextUse = nextUse;
	}
	
	public int getNextUse(){
		return nextUse;
	}
	
	public void setLocation(Location location){
		this.location = location;
	}
	
	public Location getLocation(){
		return location;
	}

	
	public String getIdentifier(){
		return identifier;
	}
	//*********************************************************************************************
	// End Get/Set Methods
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin toString Method
	//*********************************************************************************************
	@Override
	public String toString(){
		return ("ID: " + identifier + 
				", NextUse: " + nextUse + ", Location: " + location);
	}
	//*********************************************************************************************
	// End toString Methods
	//*********************************************************************************************
}
//*********************************************************************************************
//End Symbol Table Entry Class
//*********************************************************************************************
