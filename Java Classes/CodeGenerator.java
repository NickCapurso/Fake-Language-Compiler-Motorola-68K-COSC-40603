

import java.util.ArrayList;

import java.util.LinkedList;
//*************************************************************************************************
// Begin CodeGenerator Class
//	This class handles code generation for a set of quads. It takes a set of quads and a symbol
//	table and outputs the machine instructions to Standard Out.
//
//*************************************************************************************************
public class CodeGenerator {
	
	//*********************************************************************************************
	//
	// Class Variables
	//
	// Variables		Type							Description
	// ---------		----------------------------	-------------------------------------------
	// quadList			ArrayList<Quad>					The list of generated quads
	// symbolTable		ArrayList<SymbolTableEntry>		The symbol table (SymbolTableEntry is 
	//													described in its respective class)
	// registerTable	ArrayList<LinkedList<String>>	An array of LinkedLists representing
	//													the data registers
	// tempStorage		ArrayList<String>				Storage instructions for temporary vars
	//													if they need an allocated memory location
	// codeList			ArrayList<String>				Instructions generated for the main program
	// dataStorage		ArrayList<String>				Storage instructions for nontemporaries
	// programCounter	int								Holds the current instruction address		
	// tempStgCounter	int								Holds the current temp storage address
	// dataStgCounter	int								Holds the current nontemp storage address
	//
	//*********************************************************************************************
	private  ArrayList<Quad> quadList = new ArrayList<Quad >();
	private  ArrayList<SymbolTableEntry> symbolTable = new ArrayList<SymbolTableEntry >();
	private  ArrayList<LinkedList<String>> registerTable = new ArrayList<LinkedList<String> >(8);
	private ArrayList<String> tempStorage = new ArrayList<String>();
	private ArrayList<String> codeList = new ArrayList<String>();
	private ArrayList<String> dataStorage = new ArrayList<String>();
	private int programCounter;
	private int tempStgCounter;
	private int dataStgCounter;
	
	
	//*********************************************************************************************
	// Begin Constructor 
	//		Initializes class variables. Adds ORG instructions to each of the instruction lists
	//
	//*********************************************************************************************
	public CodeGenerator(ArrayList<Quad> quadList, ArrayList<SymbolTableEntry> symbolTable){
		this.quadList = quadList;
		this.symbolTable = symbolTable;
		
		for(int i = 0; i < 8; i++)
			registerTable.add(new LinkedList<String>());
		
		tempStorage.add("\tORG\t\t$4000");
		tempStgCounter = 0x4000;
		codeList.add("\tORG\t\t$1000");
		programCounter = 0x1000;
		dataStorage.add("\tORG\t\t$3000");
		dataStgCounter = 0x3000;
	}
	//*********************************************************************************************
	// End Constructor
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Generate Code Method 
	//		The entry point to begin code generation. Handles calling the appropriate method
	//		to handle the operation.
	//
	// Variables		Type			Description
	// ---------		-----------		-------------------------------------------
	// quad				Quad			The current quad
	// operation		String			The operation of the current quad 
	// BBStart			int				The quad that marks the beginning of a basic blok
	// BBEnd			int				The quad that marks the end of a basic block
	// curr				int				The current quad number
	// finished			boolean			Have we reached the last basic block?
	//
	//*********************************************************************************************
	public void genCode(){
		Quad quad;
		String operation;
		int BBStart = 0;
		int BBEnd = 0;
		int curr = 0;
		boolean finished = false;

		setupDataStorage();		//Allocate storage for non-temporaries
		doBasicBlockAnalysis(); //Mark leaders
		
		do{
			//The end of the current block is the quad preceeding the next leader
			BBEnd = getNextLeader(BBStart) - 1;
			
			//Is this the last basic block (getNextLeader returned -1)?
			if(BBEnd == -1){
				BBEnd = quadList.size()-1;
				finished = true;
			}
		
			//Live variable analysis to set up next use information for current basic block
			doLiveVarAnalysis(BBStart, BBEnd);
			
			//For each quad in the current basic block, call the appropriate method
			for(curr = BBStart; curr <= BBEnd; curr ++){
				quad = quadList.get(curr);
				operation = quad.getOperation();
				
				//If this quad has an address already before it has been assigned one
				//	indicates that this is the target of some forward jump
				if(!quad.getAddress().equals("0"))
					backpatchJump(quad);
				
				//Assign an address
				quad.setAddress(Integer.toHexString(programCounter));

				//Arthmetic & Unary
				if(operation.equals("+")){
					if(!quad.getArg2Name().equals(" "))
						genArithLogicCode("ADD", quad, curr);
				}else if(operation.equals("-")){
					if(quad.getArg2Name().equals(" "))
						genUnaryCode("NEG", quad, curr);
					else
						genArithLogicCode("SUB", quad, curr);
				}else if(operation.equals("*")){
					genArithLogicCode("MUL", quad, curr);
				}else if(operation.equals("/")){
					genArithLogicCode("DIV", quad, curr);
				}else if(operation.equals("^")){
					genUnaryCode("NOT", quad, curr);
				}else if(operation.equals("&")){
					genArithLogicCode("AND", quad, curr);
				}else if(operation.equals("|")){
					genArithLogicCode("OR", quad, curr);
				}else if(operation.equals(":=")){
					genAssignCode(quad, curr);
				}
				
				
				//Relational
				else if(operation.equals("<")){
					genRelationalCode("BLT", quad, curr);
				}else if(operation.equals("<=")){
					genRelationalCode("BLE", quad, curr);
				}else if(operation.equals("=")){
					genRelationalCode("BEQ", quad, curr);
				}else if(operation.equals("/=")){
					genRelationalCode("BNE", quad, curr);
				}else if(operation.equals(">")){
					genRelationalCode("BGT", quad, curr);
				}else if(operation.equals(">=")){
					genRelationalCode("BGE", quad, curr);
				}
				
				
				//Branches
				else if(operation.equals("jeqz")){
					genConditionalJumpCode(quad, curr);
				}else if(operation.equals("jump")){
					genUnconditionalJumpCode(quad, curr);
				}
				
				
				//I/O
				else if(operation.equals("putInt")){
					genPutCode("putInt", quad, curr);
				}else if(operation.equals("putString")){
					genPutCode("putString", quad, curr);
				}else if(operation.equals("get")){
					genGetCode(quad);
				}
				
				else if(operation.equals("SQRT") || operation.equals("sqrt")){
					genUnaryCode("SQRT", quad, curr);
				}else if(operation.equals("ABS") || operation.equals("abs")){
					genUnaryCode("ABS", quad, curr);
				}
			}
			
			//End of Basic Block, move live variables to memory
			moveEverythingToMemory();
			
			//New start
			BBStart = BBEnd + 1;
		}while(!finished);
		
		//Instructions to halt the machine
		codeList.add(Integer.toHexString(programCounter)+"\tMOVE.B\t#9,D0\t\t;Set up halt trap");
		programCounter += 2;
		codeList.add(Integer.toHexString(programCounter)+"\tTRAP\t\t#5\t;Halt program");
		programCounter += 2;
		
		//Print out all instructions to standard out
		System.out.println("\n;-------------------Program Start------------------");
		for(String s : codeList)
			System.out.println(s);
		System.out.println("\n;----------Non-Temporary & String Storage----------");
		for(String s : dataStorage)
			System.out.println(s);
		System.out.println("\n;-----------------Temporary Storage----------------");
		for(String s : tempStorage)
			System.out.println(s);
	}
	//*********************************************************************************************
	// End Generate Code Method
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Generate Arithmetic & Logical Operator Code Method 
	//		The method handles the generation of code for quads with an arithmetic or logical
	//		operator.
	//
	//		Parameters: the machine language operator (i.e. "ADD"), the quad, and the quad number
	//
	// Variables		Type			Description
	// ---------		-----------		-------------------------------------------
	// arg1				String			The first operand
	// arg2				String			The second operand
	// result			String			The result/destination
	// reg				int				The register returned from getReg
	// reg2				int				The possible register that the second operand resides in
	//
	//*********************************************************************************************
	private void genArithLogicCode(String op, Quad quad, int quadNum){
		String arg1 = quad.getArg1Name();
		String arg2 = quad.getArg2Name();
		String result = quad.getResultName();
		int reg = getReg(arg1, quad.getArg1NextUse(), quadNum);
		int reg2;
		
		//If operand1 is not in REG
		if(!registerTable.get(reg).contains(arg1)){
			//If operand1 is in another register, copy it to REG
			//else move it into REG from memory
			if((reg2=findVariableInReg(arg1)) != -1){
				copyRegister(reg, reg2, arg1);
			}else{
				moveToRegister(reg, arg1);
			}
		}
		
		//If the operand2 = operand1, do the operation with the same register for source/dest.,
		//else if operand2 is in another register, do the operation with the two registers
		//else, if operand2 is a constant generate the immediate/quick version of the instruction
		//	else, generate the appropriate machine instruction
		if(arg1.equals(arg2)){
			codeList.add(Integer.toHexString(programCounter)+"\t"+op + ".L\t\tD"+reg+",D"+reg);
		}else if((reg2=findVariableInReg(arg2)) != -1){
			codeList.add(Integer.toHexString(programCounter)+"\t"+op + ".L\t\tD"+reg2+",D"+reg);
		}else{
			if(isConstant(arg2)){
				codeList.add(Integer.toHexString(programCounter)+"\t"+op + "I.L\t\t#"+arg2+",D"+reg);
			}else{
				codeList.add(Integer.toHexString(programCounter)+"\t"+op + ".L\t\t"+arg2+",D"+reg);
			}
		}
		programCounter += 2;
		
		//If operand1 is dead and in a register, move it to memory
		if(quad.getArg1NextUse() == 0 && (reg2=findVariableInReg(arg1)) != -1)
			moveToMemory(reg2, arg1);

		//If operand2 is dead and in a register, move it to memory
		if(quad.getArg2NextUse() == 0 && (reg2=findVariableInReg(arg2)) != -1)
			moveToMemory(reg2, arg2);
		
		//Update the symbol and register tables to reflect the result's new location
		updateTablesWithResult(result, reg);
	}
	//*********************************************************************************************
	// End Generate Arithmetic & Logical Operator Code Method 
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Generate Assignment Operator Code Method 
	//		The method handles the generation of code for quads with an assignment operator.
	//
	//		Parameters: the quad and the quad number
	//
	// Variables		Type			Description
	// ---------		-----------		-------------------------------------------
	// arg1				String			The first operand
	// reg				int				The register returned from getReg
	// reg2				int				The possible register that the second operand resides in
	//
	//*********************************************************************************************
	private void genAssignCode(Quad quad, int quadNum){
		String arg1 = quad.getArg1Name();
		int reg = getReg(arg1, quad.getArg1NextUse(), quadNum);
		int reg2;
		
		//If operand1 isn't in a register, move it to REG
		if(findVariableInReg(arg1) == -1)
			moveToRegister(reg, arg1);
		
		//If operand1 is dead and in a register, move it to memory
		if(quad.getArg1NextUse() == 0 && (reg2 = findVariableInReg(arg1)) != -1)
			moveToMemory(reg2, arg1);
		
		//Update the symbol and register tables to reflect the result's new location
		updateTablesWithResult(quad.getResultName(), reg);
	}
	//*********************************************************************************************
	// End Generate Assignment Operator Code Method  
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Generate Unary Operator Code Method 
	//		The method handles the generation of code for quads with an unary operator.
	//
	//		Parameters: the machine language operator (i.e. "ADD"), the quad, and the quad number
	//
	// Variables		Type			Description
	// ---------		-----------		-------------------------------------------
	// arg1				String			The first operand
	// reg				int				The register returned from getReg
	// reg2				int				The possible register that the second operand resides in
	//
	//*********************************************************************************************
	private void genUnaryCode(String op, Quad quad, int quadNum){
		String arg1 = quad.getArg1Name();
		int reg = getReg(arg1, quad.getArg1NextUse(), quadNum);
		int reg2;
		
		//If operand1 is not in REG
		if(!registerTable.get(reg).contains(arg1)){
			//If operand1 is in another register, copy it to REG
			//else move it from memory into REG
			if((reg2=findVariableInReg(arg1)) != -1){
				copyRegister(reg, reg2, arg1);
			}else{
				moveToRegister(reg, arg1);
			}
		}
		codeList.add(Integer.toHexString(programCounter)+"\t"+op + "\t\tD"+reg);
		programCounter += 2;
		
		//If operand1 is dead and in a register, move it to memory
		if(quad.getArg1NextUse() == 0 && (reg2 = findVariableInReg(arg1)) != -1)
			moveToMemory(reg2, arg1);
		
		//Update the symbol and register tables to reflect the result's new location
		updateTablesWithResult(quad.getResultName(), reg);
	}
	//*********************************************************************************************
	// End Generate Unary Operator Code Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Generate Relational Operator Code Method 
	//		The method handles the generation of code for quads with a relational operator.
	//
	//		Parameters: the machine language operator (i.e. "BGT"), the quad, and the quad number
	//
	// Variables		Type			Description
	// ---------		-----------		-------------------------------------------
	// arg1				String			The first operand
	// arg2				String			The second operand
	// reg				int				The register returned from getReg
	// reg2				int				The possible register that the second operand resides in
	//
	//*********************************************************************************************
	private void genRelationalCode(String op, Quad quad, int quadNum){
		String arg1 = quad.getArg1Name();
		String arg2 = quad.getArg2Name();
		int reg = getReg(arg1, quad.getArg1NextUse(),quadNum);
		int reg2;
		
		//If operand1 is not in REG
		if(!registerTable.get(reg).contains(arg1)){
			//If operand1 is in another register, copy it to REG
			//else move it from memory into REG
			if((reg2=findVariableInReg(arg1)) != -1){
				copyRegister(reg, reg2, arg1);
			}else{
				moveToRegister(reg, arg1);
			}
		}
		
		//If operand2 is a constant, generate the immediate compare instruction
		//elseif operand2 is in another register, generate the compare instruction with the two
		//else generate the compare instruction using operand2 from memory
		if(isConstant(arg2))
			codeList.add(Integer.toHexString(programCounter)+"\tCMPI.L\t\t#"+ arg2 + ",D"  + reg);
		else if((reg2=findVariableInReg(arg2)) != -1)
			codeList.add(Integer.toHexString(programCounter)+"\tCMP.L\t\t"+ "D"+ reg2 + ",D"  + reg);
		else
			codeList.add(Integer.toHexString(programCounter)+"\tCMP.L\t\t"+ arg2 + ",D"  + reg);
		
		//Generate success/failure branches and corresponding instructions
		programCounter += 2;
		codeList.add(Integer.toHexString(programCounter)+"\t"+op+"\t\t"+ Integer.toHexString((programCounter + 6)));
		programCounter += 2;
		codeList.add(Integer.toHexString(programCounter)+"\tCLR.L\t\t"+ "D"+reg);
		programCounter += 2;
		codeList.add(Integer.toHexString(programCounter)+"\tBRA\t\t"+ Integer.toHexString((programCounter + 4)));
		programCounter += 2;
		codeList.add(Integer.toHexString(programCounter)+"\tMOVEQ.L\t\t#1,D"+ reg);
		programCounter += 2;
		
		//If operand1 is dead and in a register, move it to memory
		if(quad.getArg1NextUse() == 0 && (reg2 = findVariableInReg(arg1)) != -1)
			moveToMemory(reg2, arg1);
		
		//Update the symbol and register tables to reflect the result's new location
		updateTablesWithResult(quad.getResultName(), reg);
	}
	//*********************************************************************************************
	// End Generate Relational Operator Code Method
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Generate Unconditional Jump Code Method 
	//		The method handles the generation of code for an unconditional jump
	//
	//		Parameters: the quad and the quad number
	//
	// Variables		Type			Description
	// ---------		-----------		-------------------------------------------
	// jumpTarget		int				The target quad number
	// jumpQuad			Quad			The target quad
	//
	//*********************************************************************************************
	private void genUnconditionalJumpCode(Quad quad, int quadNum){
		int jumpTarget = Integer.parseInt(quad.getResultName());
		Quad jumpQuad = quadList.get(jumpTarget); 
		
		//Before a jump, move everything live to memory
		moveEverythingToMemory();
		
		//If a backwards jump, then just generate the JMP instruction with the address of the target
		//quad.
		//Else put the current program counter in the target quad's address field so that the
		//JMP instruction can be backpatched
		if(jumpTarget <= quadNum){
			codeList.add(Integer.toHexString(programCounter)+"\tJMP\t\t"+ jumpQuad.getAddress());
		}else{
			codeList.add(Integer.toHexString(programCounter)+"\tJMP\t\t"+jumpQuad.getAddress());
			quadList.get(jumpTarget).setAddress(Integer.toHexString(programCounter));
		}
		programCounter += 2;
	}
	//*********************************************************************************************
	// End Generate Unconditional Jump Code Method
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Generate Conditional Jump Code Method 
	//		The method handles the generation of code for a conditional jump
	//
	//		Parameters: the quad and the quad number
	//
	// Variables		Type			Description
	// ---------		-----------		-------------------------------------------
	// arg1				String			The first operand
	// reg				int				The register returned from getReg
	// jumpTarget		int				The target quad number
	// jumpQuad			Quad			The target quad
	//
	//*********************************************************************************************
	private void genConditionalJumpCode(Quad quad, int quadNum){
		String arg1 = quad.getArg1Name();
		int reg = getReg(arg1, quad.getArg1NextUse(),quadNum);
		int jumpTarget = Integer.parseInt(quad.getResultName());
		Quad jumpQuad = quadList.get(jumpTarget); 
		
		//If operand1 is not in a register, move it to a REG
		if(findVariableInReg(arg1) == -1)
			moveToRegister(reg, arg1);
		
		//Prior to a jump, move all live variables to memory
		moveEverythingToMemory();
		
		//Generate test instruction
		codeList.add(Integer.toHexString(programCounter)+"\tTST\t\tD"+reg);
		programCounter+=2;
		
		//If a backwards jump, then just generate the BEQ instruction with the address of the target
		//quad.
		//Else put the current program counter in the target quad's address field so that the
		//BEQ instruction can be backpatched
		if(jumpTarget <= quadNum){
			codeList.add(Integer.toHexString(programCounter)+"\tBEQ\t\t"+ jumpQuad.getAddress());
		}else{
			codeList.add(Integer.toHexString(programCounter)+"\tBEQ\t\t"+jumpQuad.getAddress());
			quadList.get(jumpTarget).setAddress(Integer.toHexString(programCounter));
		}
		programCounter += 2;
	}
	//*********************************************************************************************
	// End Generate Conditional Jump Code Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Generate Put Code Method 
	//		The method handles the generation of code for a put statement
	//
	//		Parameters: the quad operator (i.e. "putInt"), the quad, and the quad number
	//
	//*********************************************************************************************
	private void genPutCode(String op, Quad quad, int quadNum){
		//If D0 is occupied, move its contents to memory
		for(int i = 0; i < registerTable.get(0).size(); i++)
			moveToMemory(0, registerTable.get(i).remove());
		
		//If putInt, then move operand1 into D0 and generate the trap 
		//else if putString, memory label with be the string with no spaces, thus 
		//do that and generate the move address into A0 and the trap
		if(op.equals("putInt")){
			codeList.add(Integer.toHexString(programCounter)+"\tMOVE.L\t\t"+quad.getArg1Name()+",D0");
			programCounter += 2;
			codeList.add(Integer.toHexString(programCounter)+"\tTRAP\t\t#2");
			programCounter += 2;
		}else{
			//Derive the string's memory-label by removing spaces and quotes
			String noSpaces = quad.getArg1Name();
			noSpaces = noSpaces.substring(1, noSpaces.length()-1).replaceAll(" ", "");
			
			codeList.add(Integer.toHexString(programCounter)+"\tMOVEA.L\t\t"+noSpaces+",A0");
			programCounter += 2;
			codeList.add(Integer.toHexString(programCounter)+"\tTRAP\t\t#3");
			programCounter += 2;
		}
	}
	//*********************************************************************************************
	// End Generate Put Code Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Generate Get Code Method 
	//		The method handles the generation of code for a get statement
	//
	//		Parameters: the quad 
	//
	//*********************************************************************************************
	private void genGetCode(Quad quad){
		//If D0 is occupied, move its contents to memory
		for(int i = 0; i < registerTable.get(0).size(); i++)
			moveToMemory(0, registerTable.get(i).remove());
		
		//Clear D0 and generate the trap instruction
		codeList.add(Integer.toHexString(programCounter)+"\tCLR.L\t\tD0");
		programCounter += 2;
		codeList.add(Integer.toHexString(programCounter)+"\tTRAP\t\t#1");
		programCounter += 2;
		
		//Update the symbol and register tables to reflect the result's new location
		updateTablesWithResult(quad.getResultName(), 0);
	}
	//*********************************************************************************************
	// End Generate Get Code Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Move To Register Method 
	//		The generates the instruction to move a variable from memory in a register
	//
	//		Parameters: the quad 
	//
	//*********************************************************************************************
	private void moveToRegister(int register, String var){
		if(isConstant(var)) //Generate move-quick instruction for constants.
			codeList.add(Integer.toHexString(programCounter)+"\tMOVEQ.L\t\t#"+ var + ",D"  + register);
		else
			codeList.add(Integer.toHexString(programCounter)+"\tMOVE.L\t\t"+ var + ",D"  + register);
		
		programCounter += 2;
	}
	//*********************************************************************************************
	// End Move To Register Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Copy Register Method 
	//		The generates the instruction to copy a variable from one register to another
	//
	//		Parameters: the source register, the destination register, and the variable
	//
	//*********************************************************************************************
	private void copyRegister(int toRegister, int fromRegister, String var){
		codeList.add(Integer.toHexString(programCounter)+"\tMOVE.L\t\tD" + fromRegister + ",D" + toRegister);
		programCounter += 2;
	}
	//*********************************************************************************************
	// End Copy Register Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Move To Memory Method 
	//		The generates the instruction to move a variable from a register to memory
	//
	//		Parameters: the register and the variable 
	//
	//*********************************************************************************************
	private void moveToMemory(int register, String var){
		//If the variable is a temporary variable, don't move it to memory, just remove
		//it from the register and update its location in the symbol table
		if(var.charAt(0) == '$'){
			registerTable.get(register).remove(var);
			symbolTable.get(findSTEntryById(var))
				.setLocation(SymbolTableEntry.Location.REMOVED);
			
		//Else generate the appropriate move instruction and update the register/symbol table	
		}else{				
			codeList.add(Integer.toHexString(programCounter)+"\tMOVE.L\t\t"+ "D"  + register + "," + var);
			programCounter += 2;
			registerTable.get(register).remove(var);
			symbolTable.get(findSTEntryById(var))
				.setLocation(SymbolTableEntry.Location.MEMORY);
		}
	}
	//*********************************************************************************************
	// End Move To Memory Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Update Tables With Result Method 
	//		Updates the register and symbol table to reflect a variable in a register
	//
	//		Parameters: the register and the variable 
	//
	//*********************************************************************************************
	private void updateTablesWithResult(String result, int register){
		//Make sure it is in no other registers
		removeVarFromAllRegs(result);
		
		//Update tables
		if (!registerTable.get(register).contains(result))
			registerTable.get(register).add(result);
		symbolTable.get(findSTEntryById(result))
			.setLocation(SymbolTableEntry.Location.getLocationFromValue(register));
	}
	//*********************************************************************************************
	// End Update Tables With Result Method 
	//*********************************************************************************************	
	
	
	//*********************************************************************************************
	// Begin Remove Variable From All Registers Method 
	//		Removes the specified variable from all registers
	//
	//		Parameters: the variable 
	//
	//*********************************************************************************************
	private void removeVarFromAllRegs(String var){
		for(LinkedList<String> list : registerTable)
			list.remove(var);
	}
	//*********************************************************************************************
	// End Remove Variable From All Registers Method 
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Do Basic Block Analysis Method 
	//		Marks leaders in order to define basic blocks
	//
	//*********************************************************************************************
	private void doBasicBlockAnalysis(){
		String op;
		Quad quad;
		int len = quadList.size();
		
		//First quad is a leader
		quadList.get(0).setLeader(true);
		
		//Also setting last quad to be a leader, since this will be the instruction that
		//halts the machine (thus things need to be moved to memory beforehand)
		quadList.get(quadList.size()-1).setLeader(true);
		
		for(int i = 0; i < len; i ++){
			quad = quadList.get(i);
			op = quad.getOperation();
			
			//If unconditional, mark target and following quad
			if(op.equals("jeqz")){
				//In case the jump target is to the end of the program (outside of quad list)
				try{
					quadList.get(Integer.parseInt(quad.getResultName())).setLeader(true);
				}catch (IndexOutOfBoundsException e){	}
				
				//Check to make sure that there is actually a quad following this one
				if(i != len - 1)
					quadList.get(i+1).setLeader(true);
			
			//If conditional, mark target	
			}else if(op.equals("jump")){
				//In case the jump target is to the end of the program (outside of quad list)
				try{
					quadList.get(Integer.parseInt(quad.getResultName())).setLeader(true);
				}catch (IndexOutOfBoundsException e){	}
			}
		}
	}
	//*********************************************************************************************
	// End Do Basic Block Analysis Method 
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Do Live Variable Analysis Method 
	//		Performs live variable analysis and sets up next use information
	//
	//*********************************************************************************************
	private void doLiveVarAnalysis(int start, int end){
		int curr = end;
		int arg1Index, arg2Index, resultIndex; //Indexes into the symbol table
		Quad quad;
		
		//Initialize next use to 0 or MAX VALUE
		initializeSTNextUse();
		
		while((curr-start) >= 0){
			quad = quadList.get(curr);
			arg1Index = findSTEntryById(quad.getArg1Name());
			arg2Index = findSTEntryById(quad.getArg2Name());
			resultIndex = findSTEntryById(quad.getResultName());
			
			//If there is a first operand, copy next use from symbol table and
			//set next use in the symbol table to the current quad#
			if(arg1Index != -1){
				quad.setArg1NextUse(symbolTable.get(arg1Index).getNextUse());	
				symbolTable.get(arg1Index).setNextUse(curr);
			}
			
			//If there is a second operand, copy next use from symbol table and
			//set next use in the symbol table to the current quad#
			if(arg2Index != -1){
				quad.setArg2NextUse(symbolTable.get(arg2Index).getNextUse());
				symbolTable.get(arg2Index).setNextUse(curr);
			}
			
			//If there is a second operand, copy next use from symbol table and
			//set next use in the symbol table to 0
			if(resultIndex != -1){
				quad.setResultNextUse(symbolTable.get(resultIndex).getNextUse());
				symbolTable.get(resultIndex).setNextUse(0);
			}

			curr --;
		}
	}
	//*********************************************************************************************
	// End Do Live Variable Analysis Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Get Next Leader Method 
	//		Returns the next basic block leader
	//
	//*********************************************************************************************
	private int getNextLeader(int curr){
		for(int i = curr + 1; i < quadList.size(); i ++){
			if(quadList.get(i).isLeader())
				return i;
		}
		return 0;
	}
	//*********************************************************************************************
	// End Get Next Leader Method 
	//*********************************************************************************************
	
	
	//*********************************************************************************************
	// Begin Initialize Symbol Table Next Use Method 
	//		Sets all constants and temporary next uses to 0 and sets non-temporaries to MAX_VALUE
	//
	//*********************************************************************************************
	private void initializeSTNextUse(){
		for(SymbolTableEntry entry : symbolTable){
			if(entry.getIdentifier().charAt(0) <= '9'){
				if(entry.getNextUse() == 0)
					continue;
				entry.setNextUse(0);
			}else{
				if(entry.getNextUse() == Integer.MAX_VALUE)
					continue;
				entry.setNextUse(Integer.MAX_VALUE);
			}
		}
	}
	//*********************************************************************************************
	// End Initialize Symbol Table Next Use Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Find Symbol Table Entry By Id Method 
	//		Searches the symbol table for an entry matching the passed in identifier.
	//		Returns either the index into the symbol table, if found, or -1 if not found.
	//
	//*********************************************************************************************
	private int findSTEntryById(String id)
	{
		String test = id.toUpperCase();
		int index = 0;
		for(SymbolTableEntry current : symbolTable){
			if(current.getIdentifier().equals(test))
				return index;

			index++;	
		}
		return -1;
	}
	//*********************************************************************************************
	// End Find Symbol Table Entry By Id Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Find Variable In Register Method
	//		Searches the register table for a specific variable. Returns the register number
	//		if found or -1 if not found
	//
	//*********************************************************************************************
	private int findVariableInReg(String var){
		for(int i = 0; i < registerTable.size(); i++)
			if(registerTable.get(i).contains(var))
				return i;
		return -1;
	}
	//*********************************************************************************************
	// End Find Variable In Register Method
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Get Register Method 
	//		Returns a register based on the argument, next use, and the current quad
	//
	//*********************************************************************************************
	private int getReg(String argument, int nextUse, int currQuad){
		SymbolTableEntry arg1 = symbolTable.get(findSTEntryById(argument));
		SymbolTableEntry.Location location = arg1.getLocation();
		int reg;
		String head;

		//If the operand is in a register by itself and is dead, return that register 
		if(isInRegWithNoOthers(location) && nextUse == 0){
			return location.value;
			
		//Else if, return an empty register, if there is one
		}else if((reg =findEmptyReg()) != -1){
			return reg;
		
		//All Registers are Full
		}else{
			//Choosing D1 by default to empty out
			while(!registerTable.get(1).isEmpty()){
				head = registerTable.get(1).remove();
				
				//If there is a temporary variable, then move it to memory as it could still be live
				//in the middle of a basic block. Dynamically allocate it a storage space
				if(head.charAt(0) == '$'){
					tempStorage.add(Integer.toHexString(tempStgCounter)+"\t"+head+"\tDC.W\t0");
					tempStgCounter += 2;
				}
				
				//Move the variable to storage
				codeList.add(Integer.toHexString(programCounter)+"\tMOVE.L\t\tD1," + head);
				programCounter += 2;
				symbolTable.get(findSTEntryById(head)).setLocation(SymbolTableEntry.Location.MEMORY);			  
			}
			//Return D1
			return 1;
		}
	}
	//*********************************************************************************************
	// End Get Register Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Is In Register With No Others Method 
	//		Return true if a given register has only one item in it
	//
	//*********************************************************************************************
	private boolean isInRegWithNoOthers(SymbolTableEntry.Location l){
		if(l != SymbolTableEntry.Location.MEMORY){
			if(l == SymbolTableEntry.Location.REMOVED)
				return false;
			if(registerTable.get(l.value).size() == 1)
				return true;
		}
		return false;
	}
	//*********************************************************************************************
	// End Is In Register With No Others Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Find Empty Register Method 
	//		Return the index of an empty register, or -1 if there isn't one available.
	//
	//*********************************************************************************************
	private int findEmptyReg(){
		for(int i = 1; i < registerTable.size(); i++)
			if(registerTable.get(i).isEmpty())
				return i;
		return -1;
	}
	//*********************************************************************************************
	// End Find Empty Register Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Move Everything To Memory Method 
	//		Moves all variables in a register to memory
	//
	//*********************************************************************************************
	private void moveEverythingToMemory(){
		for(int i = 0; i < registerTable.size(); i++){
			while(!registerTable.get(i).isEmpty()){
				moveToMemory(i, registerTable.get(i).remove());
			}
		}
	}
	//*********************************************************************************************
	// End Move Everything To Memory Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Is Constant Method 
	//		Returns true if the given variable is a constant (only constants may begin with a number)
	//
	//*********************************************************************************************
	private boolean isConstant(String test){
		return test.charAt(0) >= '0' && test.charAt(0) <= '9';
	}
	//*********************************************************************************************
	// End Is Constant Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Backpatch Jump Method 
	//		Goes through forward jump instructions and fixes their target address to be the current
	//		address
	//
	//*********************************************************************************************
	private void backpatchJump(Quad quad){
		String newAddress = Integer.toHexString(programCounter); //Program counter
		String address = ""+quad.getAddress(); //The address of the current quad
		String curr;
		int codeListIndex = 0;

		do{
			//Get the index into the code list based on the address in the current quad
			codeListIndex = findInstructionByAddress(address);
			curr = codeList.get(codeListIndex);
			
			//Get the address being referenced by the current instruction
			address = getJumpTargetAddress(curr);
			
			//Cut out the target's target address and replace it with the current address
			curr = curr.substring(0, curr.lastIndexOf("\t")+1);
			curr = curr.concat(newAddress);
			
			//Replace the old instruction with the new instruction
			codeList.set(codeListIndex, curr); 
		}while(!address.equals("0"));
	}
	//*********************************************************************************************
	// End Backpatch Jump Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Find Instruction By Address Method 
	//		Goes through the code list searching for the instruction at the given address
	//
	//*********************************************************************************************
	private int findInstructionByAddress(String address){
		for(int i = 0; i < codeList.size(); i++)
			if(getInstructionAddress(codeList.get(i)).equals(address))
				return i;

		return -1;
	}
	//*********************************************************************************************
	// End Find Instruction By Address Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Get Instruction Address Method 
	//		Returns the address of the given instruction (the characters before the first tab)
	//
	//*********************************************************************************************
	private String getInstructionAddress(String instruction){
		return instruction.substring(0, instruction.indexOf("\t"));
	}
	//*********************************************************************************************
	// End Get Instruction Address Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Get Jump Target Address Method 
	//		Returns the target jump address of the given jump instruction (the last
	//		characters after the last tab)
	//
	//*********************************************************************************************
	private String getJumpTargetAddress(String instruction){
		return instruction.substring(instruction.lastIndexOf("\t")+1, instruction.length());
	}
	//*********************************************************************************************
	// End Begin Get Jump Target Address Method 
	//*********************************************************************************************

	
	//*********************************************************************************************
	// Begin Setup Data Storage Method 
	//		Sets up memory allocation for variables references in the symbol table. Temporaries
	//		and constants are not allocated storage, programmer-defined variables are allocated
	//		storage and their memory location is labeled with the variable name. Strings are
	//		treated in the same way, their label is the string with spaces removed; a null 
	//		character is placed after the string.
	//
	//*********************************************************************************************
	private void setupDataStorage(){
		String id;
		for(SymbolTableEntry entry : symbolTable){
			id = entry.getIdentifier();
			
			//If the current entry is a constant or temporary, do nothing
			if(id.charAt(0) == '$' || isConstant(id)){
				continue;
				
			//If the current entry is a programmer-defined variable, allocate storage	
			}else if(!(id.charAt(0) == '\"')){
				dataStorage.add(Integer.toHexString(dataStgCounter)+"\t"+id+"\tDC.W\t0");
				dataStgCounter += 2;
				
			//If the current entry is a string, allocate storage. The label will be the
			//string with spaces removed.
			}else{
				String noSpaces = id.substring(1, id.length()-1).replaceAll(" ", "");
				dataStorage.add(Integer.toHexString(dataStgCounter)+"\t"+noSpaces+"\tDC.B\t"+
						"\'"+id.substring(1, id.length()-1)+"\'");
				dataStgCounter += id.length();
				
				//Place a null terminator so the trap instruction will know when to stop writing
				dataStorage.add(Integer.toHexString(dataStgCounter)+"\t\tDC.B\t0\t;Null terminator");
				dataStgCounter ++;
			}

		}
	}
	//*********************************************************************************************
	// End Setup Data Storage Method 
	//*********************************************************************************************
}
//*************************************************************************************************
// End CodeGenerator Class
//*************************************************************************************************
