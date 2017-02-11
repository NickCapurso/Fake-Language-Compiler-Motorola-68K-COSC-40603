

//*************************************************************************************************
// Begin Quad Class
//	This class represents a quad
//
//*************************************************************************************************
public class Quad {
	//*********************************************************************************************
	//
	// Class Variables
	//
	// Variables		Type			Description
	// ---------		--------		-------------------------------------------
	// operation		String			The operation for the quad (e.g. +,-,jeqz,<, etc ...)
	// arg1				TwoTuple		The first operand and its next use value
	// arg2				TwoTuple		The second operand and its next use value
	// result			TwoTuple		The result/destination and its next use value
	// isLeader			boolean			Does this quad start a basic block?
	// address			String			The address of this quad during codeGen
	//										Using Integer.toHexString(...)
	//
	//*********************************************************************************************
	private String operation;
	private TwoTuple arg1;
	private TwoTuple arg2;
	private TwoTuple result;
	private boolean isLeader;
	private String address;

	
	//*********************************************************************************************
	// Begin Constructor 
	//		Initializes class variables
	//
	//*********************************************************************************************
	public Quad (String operation, String arg1, String arg2, String result){
		this.operation = operation;
		this.arg1 = new TwoTuple(arg1, 0);
		this.arg2 = new TwoTuple(arg2, 0);
		this.result = new TwoTuple(result, 0);
		isLeader = false;
		address = "0";
	}
	//*********************************************************************************************
	// End Constructor
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Get/Set Methods
	//*********************************************************************************************
	public void setOperation(String operation){
		this.operation = operation;
	}
	
	public String getOperation(){
		return operation;
	}
	
	public void setArg1Name(String name){
		this.arg1.name = name;
	}
	
	public void setArg1NextUse(int nextUse){
		this.arg1.nextUse = nextUse;
	}
	
	public String getArg1Name(){
		return arg1.name;
	}
	
	public int getArg1NextUse(){
		return arg1.nextUse;
	}
	
	public void setArg2Name(String name){
		this.arg2.name = name;
	}
	
	public void setArg2NextUse(int nextUse){
		this.arg2.nextUse = nextUse;
	}
	
	public String getArg2Name(){
		return arg2.name;
	}
	
	public int getArg2NextUse(){
		return arg2.nextUse;
	}
	
	public void setResultName(String name){
		this.result.name = name;
	}
	
	public void setResultNextUse(int nextUse){
		this.result.nextUse = nextUse;
	}
	
	public String getResultName(){
		return result.name;
	}
	
	public int getResultNextUse(){
		return result.nextUse;
	}
	
	public void setLeader(boolean leader){
		isLeader = leader;
	}
	
	public boolean isLeader(){
		return isLeader;
	}
	
	public void setAddress(String a){
		address = a;
	}
	
	public String getAddress(){
		return address;
	}
	//*********************************************************************************************
	// End Get/Set Methods
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin toString Methods
	//*********************************************************************************************
	@Override
	public String toString(){
		return (operation + "," + arg1.name + "," + arg2.name + "," + result.name);
	}
	
	public String formattedToString(){
		return String.format("%4s,%3s,%3s,%3s", operation, arg1.name, arg2.name, result.name);
	}
	
	public String formattedFullToString(){
		return String.format("%4s,%3s / %4d,%3s / %4d,%3s / %4d, Addr %5d", operation, arg1.name, arg1.nextUse, 
				arg2.name, arg2.nextUse, result.name, result.nextUse, address);
	}
	//*********************************************************************************************
	// End toString Methods
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// TwoTuple Class
	//		Used to hold an operand and its next use value
	//*********************************************************************************************
	class TwoTuple {
		public String name;
		public int nextUse;
		
		public TwoTuple(String n, int nU){
			name = n;
			nextUse = nU;
		}
	}
	//*********************************************************************************************
	// End TwoTuple Class
	//*********************************************************************************************
}
//*********************************************************************************************
// End Quad Class
//*********************************************************************************************
