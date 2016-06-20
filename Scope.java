import java.util.HashSet;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

// Dr.scient. Frank Thomas Tveter, 2016

public class Scope {
    Scope parent=null;
    //
    public List<Program>    programs    = new ArrayList<Program>();
    List<Block>      blocks      = new ArrayList<Block>();
    // subroutines and functions in this scope only
    HashMap<String,Function>    functions = new HashMap<String,Function>();
    HashMap<String,Subroutine>  subroutines = new HashMap<String,Subroutine>();
    // global list of unlinked subroutines and functions
    HashMap<String,List<Call>>  unlinkedFunctionCallLists; // only defined for global/top scope...
    HashMap<String,List<Call>>  unlinkedSubroutineCallLists; // only defined for global/top scope...
    //
    public Scope() { // global/top scope
	this.parent=null;
	unlinkedFunctionCallLists = new HashMap<String,List<Call>>();
	unlinkedSubroutineCallLists = new HashMap<String,List<Call>>();
    }
    public Scope(Scope parent) {
	this.parent=parent;
    }
    public void link(RegexNode nodeTree) {
	//
	HashMap<String,Subroutine>  globalSubroutines = new HashMap<String,Subroutine>();
	HashMap<String,Function>    globalFunctions   = new HashMap<String,Function>();
	System.out.format("looping programs.\n");
	// loop over programs	    
	RegexNode programNode=nodeTree.getNode("$","*","Program"); // one level(filenode) below top-level
	while (programNode != null) {		
	    Program program =new Program(programNode);
	    programNode.setAttribute("dataStructure",program);
	    programs.add(program);
	    programNode=nodeTree.getNode("$","*","Program"); // one level(filenode) below top-legel
	}
	
	System.out.format("looping subroutines.\n");
	// loop over subroutines
	RegexNode subroutineNode=nodeTree.getNode("$","*","Subroutine"); // one level(filenode) below top-level
	while (subroutineNode != null) {		
	    Subroutine subroutine =new Subroutine(subroutineNode,null);
	    subroutineNode.setAttribute("dataStructure",subroutine);
	    globalSubroutines.put(subroutine.name.toLowerCase(),subroutine);
	    subroutineNode=nodeTree.getNode("$","*","Subroutine");
	}
	
	System.out.format("looping functions.\n");
	// loop over functions
	RegexNode functionNode=nodeTree.getNode("$","*","Function"); // one level(filenode) below top-level
	while (functionNode != null) {		
	    Function function =new Function(functionNode,null);
	    functionNode.setAttribute("dataStructure",function);
	    globalFunctions.put(function.name.toLowerCase(),function);
	    System.out.format("************Archiving global function: %s\n",function.name);
	    functionNode=nodeTree.getNode("$","*","Function");
	}
	
	System.out.format("looping block statements.\n");
	// loop over block data statements
	RegexNode blockNode=nodeTree.getNode("$","*","Block"); // one level(filenode) below top-level
	while (blockNode != null) {		
	    System.out.format("\nFound block data\n");
	    Block block =new Block(blockNode);
	    blocks.add(block);
	    blockNode.setAttribute("dataStructure",block);
	    blockNode=nodeTree.getNode("$","*","Block");
	}
	
	System.out.format("linking.\n");
	
	addSubroutine(globalSubroutines);
	addFunction(globalFunctions);
	link();
	link(programs);
	clear();

	
	System.out.format("Printing unlinked subroutines:\n%s\n",toString());
    }
    public void addSubroutine(HashMap<String,Subroutine> subroutines) {
	for (Map.Entry<String, Subroutine> entry : subroutines.entrySet()) {
	    String name = entry.getKey();
	    Subroutine subroutine = entry.getValue();
	    addSubroutine(subroutine);
	}
    }
    public void addSubroutine(Subroutine subroutine) {
	// a subroutine may have many entries
	for (Map.Entry<String, RegexNode> subroutineEntry : subroutine.entries.entrySet()) {
	    String name = subroutineEntry.getKey();
	    subroutines.put(name.toLowerCase(),subroutine);
	}
    }
    public void addFunction(HashMap<String,Function> functions) {
	for (Map.Entry<String, Function> entry : functions.entrySet()) {
	    String name = entry.getKey();
	    Function function = entry.getValue();
	    addFunction(function);
	}
    }
    public void addFunction(Function function) {
	// a function may have many entries
	for (Map.Entry<String, RegexNode> functionEntry : function.entries.entrySet()) {
	    String name = functionEntry.getKey();
	    functions.put(name.toLowerCase(),function);
	}
    }
    public void addUnlinkedFunction(Call functionCall) {
	String name=functionCall.name;
	List<Call> functionCallList = unlinkedFunctionCallLists.get(name);
	if (functionCallList==null) {
	    functionCallList=new ArrayList<Call>();
	    //System.out.format("unlinkedFunctionCallLists Adding: %s\n",name);
	    unlinkedFunctionCallLists.put(name,functionCallList);
	}
	functionCallList.add(functionCall);
    }
    public void addUnlinkedSubroutine(Call subroutineCall) {
	String name=subroutineCall.name;
	List<Call> subroutineCallList = unlinkedSubroutineCallLists.get(name);
	if (subroutineCallList==null) {
	    subroutineCallList=new ArrayList<Call>();
	    //System.out.format("unlinkedSubroutineCallLists Adding: %s\n",name);
	    unlinkedSubroutineCallLists.put(name,subroutineCallList);
	}
	subroutineCallList.add(subroutineCall);
    }
    public void link() {
	// link scope functions
	for (Map.Entry<String, Function> entry : functions.entrySet()) {
	    String name = entry.getKey();
	    Function function = entry.getValue();
	    // link global calls to this function
	    if (unlinkedFunctionCallLists != null) { // global scope
		List<Call> functionCallList = unlinkedFunctionCallLists.get(name);
		if (functionCallList!=null) {
		    for (Call functionCall: functionCallList) {
			functionCall.setTargetProcedure(function);
		    }
		    unlinkedFunctionCallLists.remove(name);
		}
	    }
	    // link calls in function
	    if (name.equals(function.name.toLowerCase())) {
		function.link(this);
	    }
	}
	// link scope subroutines
	for (Map.Entry<String, Subroutine> entry : subroutines.entrySet()) {
	    String name = entry.getKey();
	    Subroutine subroutine = entry.getValue();
	    // link global calls to this subroutine
	    if (unlinkedSubroutineCallLists != null) { // global scope
		List<Call> subroutineCallList = unlinkedSubroutineCallLists.get(name);
		if (subroutineCallList!=null) {
		    for (Call subroutineCall: subroutineCallList) {
			subroutineCall.setTargetProcedure(subroutine);
		    }
		    unlinkedSubroutineCallLists.remove(name);
		}
	    }
	    // link calls in subroutine
	    if (name.equals(subroutine.name.toLowerCase())) {
		subroutine.link(this);
	    }
	}
    }
    public void link(Program program) {
	program.link(this);
    }
    public void link(List<Program> programs) {
	for (Program program : programs) {
	    link(program);
	}
    }
    public void clear() {
	subroutines = new HashMap<String,Subroutine>();
	functions   = new HashMap<String,Function>();
    }
    
    public String toString() {
	String s="";
	if (unlinkedFunctionCallLists != null) {
	    for (Map.Entry<String,List<Call>> entry : unlinkedFunctionCallLists.entrySet()) {
		String name = entry.getKey();
		s=s+"=>:"+name+":\n";
		List<Call> procedureCallList = entry.getValue();
		if (procedureCallList != null) {
		    for (Call procedureCall: procedureCallList) {
			s=s+procedureCall.toString("   ");
		    }
		}
	    }
	}
	if (unlinkedSubroutineCallLists != null) {
	    for (Map.Entry<String,List<Call>> entry : unlinkedSubroutineCallLists.entrySet()) {
		String name = entry.getKey();
		s=s+"=>:"+name+":\n";
		List<Call> procedureCallList = entry.getValue();
		if (procedureCallList != null) {
		    for (Call procedureCall: procedureCallList) {
			s=s+procedureCall.toString("   ");
		    }
		}
	    }
	}
	return s;
    }
    public class Call {
	String name=null;           // name of procedure
	RegexNode callerNode=null;      // link to caller procedure
	Procedure caller=null;      // link to caller procedure
	Procedure target=null;      // link to procedure
	public boolean bExternal=false; // is procedure declared?
	public boolean bLocal=false;    // is procedure declared?
	public boolean bCalled=false;   // is procedure called?
	public Call(Procedure caller, String name) {
	    this.callerNode=null;
	    this.caller=caller;
	    this.name=name;
	}
	public Call(RegexNode callerNode, Procedure caller, String name) {
	    this.callerNode=callerNode;
	    this.caller=caller;
	    this.name=name;
	}
	public String getName(){
	    return name;
	}
	public void setTargetProcedure(Procedure procedure) {
	    this.target=procedure;
	}
	public String toString(String prefix) {
	    return toString(prefix,-1);
	}
	public String toString(String prefix, int printid) {
	    String l=prefix+"¤"+String.format("%-"+Math.max(10,name.length())+"s","\""+name+"\"");
	    String s="[";
	    if (bCalled) {
		s=s+"c";
	    } else {
		s=s+"-";
	    }
	    if (bLocal) {
		s=s+"l";
	    } else if (target != null) {
		s=s+"g";
	    } else {
		s=s+"-";
	    }
	    if (bExternal) {
		s=s+"e";
	    } else {
		s=s+"-";
	    }
	    if (target instanceof Subroutine) {
		s=s+"s";
	    } else if (target instanceof Function) {
		s=s+"f";
	    } else {
		s=s+"-";
	    }
	    s=s+"]";
	    s=String.format("%-20s   %-5s",l,s);
	    if (target != null) {
		s=s+"\n"+target.toString(prefix+" ",printid);
	    } else {
		s=s+" => <no target>\n";
	    }
	    return s;
	}
    }

    public class CommonBlock {
	RegexNode   declaration;
	Variable[]    variables;
    }
    
    private class CommonParent extends CommonBlock{
	String Name;
	CommonBlock[] commonBlocks;
	Block block;
    }
    public class Block extends Procedure{
	public Block(RegexNode node) {
	    super(node);
	    name=node.getText("$","Block","start","name");
	    System.out.format("%s Found block\n",name);
	    analyseDeclarations(node);     // analyse declarations
	}
    }

    public class Program extends Procedure {
	public Program(RegexNode node) {
	    super(node);
	    name=node.getText("$","Program","start","name");
	    System.out.format("\n%s Found program\n",name);
	    analyseDeclarations(node);     // analyse declarations
	    System.out.format("%s Contains\n",name);
	    analyseContains(node); // analyse contains section
	    System.out.format("%s Main\n",name);
	    analyseMain(node);     // analyse main contents
	}
    }

    public class Subroutine extends Procedure {
	Integer narguments=0;
	public Subroutine(RegexNode node, Procedure parent) {
	    super(node);
	    System.out.format("\nFound subroutine: %s\n",name);
	    importVariables(parent); // import variables from the parent procedure
	    System.out.format("%s Declarations\n",name);
	    analyseDeclarations(parent,node);     // analyse declarations
	    System.out.format("%s Interface\n",name);
	    analyseInterface(parent,node);     // analyse main contents
	    System.out.format("%s Contains\n",name);
	    analyseContains(parent,node); // analyse contains section
	    System.out.format("%s Main\n",name);
	    analyseMain(parent,node);     // analyse main contents
	    declared=true;
	}
    }

    public class Function extends Procedure {
	Integer narguments=null;
	public Function(RegexNode node, Procedure parent) {
	    super(node);
	    System.out.format("\nFound function: %s\n",name);
	    importVariables(parent); // import variables from the parent procedure
	    Variable variable=defineFunctionVariable(parent,node);
	    System.out.format("Defined local variable: %s\n",variable.toString());
	    System.out.format("%s Declarations\n",name);
	    analyseDeclarations(parent,node);     // analyse declarations
	    System.out.format("%s Interface\n",name);
	    analyseInterface(parent,node);     // analyse main contents
	    System.out.format("%s Contains\n",name);
	    analyseContains(parent,node); // analyse contains section
	    System.out.format("%s Main\n",name);
	    analyseMain(parent,node);     // analyse main contents
	    narguments=(Integer) node.getAttribute("narguments","$","Function","start");
	    declared=true;
	}
	public Variable defineFunctionVariable(Procedure parent, RegexNode node) {
	    // define name of function as a local variable
	    Variable variable = new Variable(this.name);
	    node.setAttribute("dataStructure",variable);
	    localVariables.put(name.toLowerCase(),variable);
	    String type=node.getText("$","Function","start","type");
	    if (type == null && parent != null) {
		type=parent.implicitList.getType(null,name);
	    }
	    variable.type=type;
	    return variable;
	}
    }
    abstract class Procedure {
	protected String          name;
	protected RegexNode     declareNode;
	protected HashMap<String,RegexNode>  entries  = new HashMap<String,RegexNode>();
	//
	protected ImplicitList implicitList=new ImplicitList();
	protected String type=null;
	protected Boolean declared=false;
	//
	protected HashMap<String,Variable>          localVariables  = new HashMap<String,Variable>();
	protected HashMap<String,Subroutine>        localSubroutines= new HashMap<String,Subroutine>();
	protected HashMap<String,Function>          localFunctions  = new HashMap<String,Function>();
	//
	protected List<Call>  subroutineCalls     = new ArrayList<Call>();
	protected List<Call>  functionCalls       = new ArrayList<Call>();
	protected List<Call>  calledBy            = new ArrayList<Call>();
	//
	protected HashSet<String>          externalFunctions  = new HashSet<String>();
	protected HashSet<String>          declaredFunctions  = new HashSet<String>();
	//
	protected HashMap<String,RegexNode>       commonBlocks    = new HashMap<String,RegexNode>();
	//
	int printid=-1;
	//
	public Procedure(RegexNode node) {
	    declareNode=node;
	    RegexNode entryNode=node.getNode("$","*","start");
	    node.getNodeReset("$","*","start");
	    name=entryNode.getText("$","*","name");
	    if (name == null) name ="null";
	    entries.put(name.toLowerCase(),entryNode);
	    entryNode=node.getNode("$","*","Main","entry");
	    while (entryNode != null) {
		String ename=entryNode.getText("$","*","name");
		if (ename == null) ename="null";
		entries.put(ename.toLowerCase(),entryNode);
		entryNode=node.getNode("$","*","Main","entry");
	    }
	}
	public String getName() {
	    return name;
	}
	public String toString() {
	    return toString("",-1);
	}
	public String toString(String prefix) {
	    return toString(prefix, -1);
	}
	public String toString(int printid) {
	    return toString("",printid);
	}
	public String toString(String prefix, int printid) {
	    String s="";
	    if (printid==-1 || printid!=this.printid) {
		this.printid=printid;
		if (subroutineCalls.size() > 0) {
		    s=s+prefix+"\""+name+"\"\n";
		    for (Call subroutineCall : subroutineCalls) {
			String name = subroutineCall.getName();
			s=s+subroutineCall.toString(prefix+"   ",printid);
		    }
		}
		if (functionCalls.size() > 0) {
		    s=s+prefix+"\""+name+"\"\n";
		    for (Call functionCall : functionCalls) {
			String name = functionCall.getName();
			s=s+functionCall.toString(prefix+"   ",printid);
		    }
		}
	    } else if (printid == this.printid) {
		s=s+prefix+"   ...\n";
	    }
	    return s;
	}
	public void link(Scope scope) {
	    Scope localScope=new Scope(scope);
	    localScope.addFunction(localFunctions);
	    localScope.addSubroutine(localSubroutines);
	    // link local scope
	    localScope.link();
	    linkSubroutineCalls(scope);
	    linkFunctionCalls(scope);
	}
	public void linkFunctionCalls (Scope scope) {
	    // link function pointers
	    for (Call functionCall : functionCalls) {
		String name = functionCall.getName();
		// look for match
		Scope currentScope=scope;
		Scope topScope=currentScope;
		while (currentScope != null) {
		    Function target=currentScope.functions.get(name);
		    if (target!=null) {
			functionCall.target=target;
			target.calledBy.add(functionCall);
			currentScope=null;
		    } else {
			topScope=currentScope;
			currentScope=currentScope.parent;
		    }
		}
		// add to global unlinked list if no match was found...
		if (functionCall.target == null & topScope != null) {
		    topScope.addUnlinkedFunction(functionCall);
		}
	    }
	}
	public void linkSubroutineCalls (Scope scope) {
	    // link subroutine pointers
	    for (Call subroutineCall : subroutineCalls) {
		String name = subroutineCall.getName();
		// look for match
		Scope currentScope=scope;
		Scope topScope=currentScope;
		while (currentScope != null) {
		    Subroutine target=currentScope.subroutines.get(name);
		    if (target!=null) {
			subroutineCall.target=target;
			target.calledBy.add(subroutineCall);
			currentScope=null;
		    } else {
			topScope=currentScope;
			currentScope=currentScope.parent;
		    }
		}
		// add to global unlinked list if no match was found...
		if (subroutineCall.target == null & topScope != null) {
		    topScope.addUnlinkedSubroutine(subroutineCall);
		}
	    }
	}
	public void importVariables(Procedure parent) {
	    // link to variables in parent procedure...
	    if (parent != null ) {
		for (Map.Entry<String, Variable> entry : parent.localVariables.entrySet()) {
		    String name = entry.getKey();
		    Variable variable = entry.getValue();
		    localVariables.put(name.toLowerCase(),variable);
		}
	    }
	}
	public void analyseDeclarations (RegexNode procedureNode) {
	    analyseDeclarations(null,procedureNode);
	}
	public void analyseDeclarations (Procedure parent, RegexNode procedureNode) {
	    String prefix="   ";
	    RegexNode declarationsNode=procedureNode.getNode("$","*","Declarations"); // only analyse first occurence of Main
	    if (declarationsNode != null) {
		// analyse implicit rules
		RegexNode implicitNode=declarationsNode.getNode("$","Declarations","implicit");
		while (implicitNode != null ) {
		    RegexNode typeNode=implicitNode.getNode("$","implicit","arguments","argument","type");
		    while (typeNode != null) {
			String type=canonical(typeNode.getText("type","name"));
			if (type.equals("none")) {
			    int from=-100000000;
			    int to=100000000;
			    implicitList.add(type,from,to);
			    System.out.format("%s[implicit] none...\n",prefix);
			} else {
			    Integer arguments=(Integer) typeNode.getAttribute("narguments");
			    RegexNode rangeNode=typeNode.getNode("$","type","arguments","argument");
			    int from=0;
			    int to=0;
			    while (rangeNode != null) {
				RegexNode variableNode=rangeNode.getNode("$","argument","variable"); // e.g. implicit real(a,b,c)
				if (variableNode != null) {
				    from=(int) variableNode.getText("name").substring(0,1).charAt(0);
				    to=from;
				} else { // argument may contain intrinsic "-"...
				    variableNode=rangeNode.getNode("$","argument","intrinsic","argument","variable"); // e.g. implicit real(a-b,b-c)
				    if (variableNode != null) {
					from=(int) variableNode.getText("name").substring(0,1).charAt(0);
					variableNode=rangeNode.getNode("$","argument","intrinsic","argument","variable");
					if (variableNode != null) {
					    to=(int) variableNode.getText("name").substring(0,1).charAt(0);
					} else {
					    to=from;
					}
				    }
				    rangeNode.getNodeReset("$","argument","intrinsic","argument","variable");
				}
				rangeNode.getNodeReset("$","argument","variable");
				implicitList.add(type,from,to);
				System.out.format("%s[implicit] %s (%s - %s)\n",prefix,type,(char)from,(char)to);
				rangeNode=typeNode.getNode("$","type","arguments","argument");
			    }
			}
			typeNode=implicitNode.getNode("$","implicit","arguments","argument","type");
		    }
		    implicitNode=declarationsNode.getNode("$","Declarations","implicit");
		}
		// analyse externals
		RegexNode externalNode=declarationsNode.getNode("$","Declarations","external");
		while (externalNode != null ) {
		    RegexNode variableNode=externalNode.getNode("$","external","arguments","argument","variable");
		    while (variableNode != null) {
			String name=variableNode.getText("name");
			name=canonical(name);
			externalFunctions.add(name.toLowerCase());
			variableNode=externalNode.getNode("$","external","arguments","argument","variable");
		    }
		    externalNode=declarationsNode.getNode("$","Declarations","external");
		}
		// analyse  dimensions.
		RegexNode dimensionNode=declarationsNode.getNode("$","Declarations","dimension");
		while (dimensionNode != null ) {
		    RegexNode variableNode=dimensionNode.getNode("$","dimension","arguments","argument","variable");
		    while (variableNode != null) {
			String name=variableNode.getText("name");
			name=canonical(name);
			Variable variable = new Variable(name);
			variableNode.setAttribute("dataStructure",variable);
			localVariables.put(name.toLowerCase(),variable);
			variable.ndimensions=(Integer) variableNode.getAttribute("narguments");
			Integer dim=0;
			RegexNode argumentNode = variableNode.getNode("$","variable","arguments","argument");
			while (argumentNode != null) {
			    dim=dim+1;
			    variable.dimensions.put(dim,argumentNode);
			    argumentNode = variableNode.getNode("$","variable","arguments","argument");
			}
			String type=implicitList.getType(parent,name);
			variable.type=type;
			System.out.format("%s[dimension]   %s %d(%d) %s\n",prefix,name,variable.ndimensions,dim, type);
			variableNode=dimensionNode.getNode("$","dimension","arguments","argument","variable");
		    }
		    dimensionNode=declarationsNode.getNode("$","Declarations","dimension");
		}
		// analyse declarations
		RegexNode declarationNode=declarationsNode.getNode("$","Declarations","declaration");
		while (declarationNode != null ) {
		    String type=declarationNode.getText("type");
		    RegexNode variableNode=declarationNode.getNode("$","declaration","variables","variable");
		    while (variableNode != null) {
			String name=variableNode.getText("name");
			name=canonical(name);
			Variable variable=new Variable(name);
			variableNode.setAttribute("dataStructure",variable);
			localVariables.put(name.toLowerCase(),variable);
			variable.type=type;
			variable.ndimensions=(Integer) variableNode.getAttribute("narguments");
			Integer dim=0;
			RegexNode argumentNode = variableNode.getNode("$","variable","arguments","argument");
			while (argumentNode != null) {
			    dim=dim+1;
			    variable.dimensions.put(dim,argumentNode);
			    argumentNode = variableNode.getNode("$","variable","arguments","argument");
			}
			variable.declaration=variableNode;
			System.out.format("%s[declaration] %s %s\n",prefix,type,name);
			variableNode=declarationNode.getNode("$","declaration","variables","variable");
		    }
		    declarationNode=declarationsNode.getNode("$","Declarations","declaration");
		}
		// analyse  common blocks
		RegexNode commonNode=declarationsNode.getNode("$","Declarations","common");
		while (commonNode != null ) {
		    RegexNode variableNode=commonNode.getNode("$","common","variables","variable");
		    while (variableNode != null) {
			String name=variableNode.getText("name");
			name=canonical(name);
			Variable variable=localVariables.get(name.toLowerCase());
			if (variable == null) {
			    variable = new Variable(name);
			    localVariables.put(name.toLowerCase(),variable);
			}
			variableNode.setAttribute("dataStructure",variable);
			variable.commonBlock=variableNode;
			System.out.format("%s[common] %s\n",prefix,name);
			variableNode=commonNode.getNode("$","common","arguments","argument","variable");
		    }
		    commonNode=declarationsNode.getNode("$","Declarations","common");
		}
		// analyse data statements
		RegexNode dataNode=declarationsNode.getNode("$","Declarations","data");
		while (dataNode != null ) {
		    RegexNode itemNode=dataNode.getNode("$","data","list","item");
		    while (itemNode != null) {
			RegexNode valueNode=itemNode.getNode("$","item","expressions","expression");
			RegexNode variableNode=itemNode.getNode("$","item","variables","variable");
			while (variableNode != null) {
			    String name=variableNode.getText("name");
			    System.out.format("%s[data] %s\n",prefix,name);
			    name=canonical(name);
			    Variable variable=localVariables.get(name.toLowerCase());
			    if (variable == null) {
				variable = new Variable(name);
				localVariables.put(name.toLowerCase(),variable);
			    }
			    variableNode.setAttribute("dataStructure",variable);
			    Integer ndimensions=(Integer) itemNode.getAttribute("narguments");
			    if (ndimensions == null) ndimensions=0;
			    // ********************
			    if (ndimensions > 0) { // if we have array arguments, we may be trying to store a single element-value...
				Integer dim=0;
				RegexNode argumentNode = itemNode.getNode("$","variable","arguments","argument");
				while (argumentNode != null) {
				    dim=dim+1;
				    variable.dimensions.put(dim,argumentNode);
				    argumentNode = variableNode.getNode("$","variable","arguments","argument");
				}			    }
			    // *************** Here we assume 1 to 1 relationship between variable and value...
			    variable.data=valueNode;
			    valueNode=itemNode.getNode("$","item","expressions","expression");
			    // ********************
			    variableNode=itemNode.getNode("$","item","variables","variable");
			}
			itemNode=dataNode.getNode("$","data","list","item");
		    }
		    dataNode=declarationsNode.getNode("$","Declarations","data");
		}
	    }
	    procedureNode.getNodeReset("$","*","Declarations");
	}
	
	public void analyseInterface(RegexNode node) {
	    analyseInterface(null,node);
	}
	public void analyseInterface(Procedure parent, RegexNode procedureNode) {
	    String prefix="   ";
	    RegexNode startNode=procedureNode.getNode("$","*","start");
	    while (startNode != null) {
		Integer narguments=(Integer) startNode.getAttribute("narguments");
		System.out.format("%s[arguments] %s %dD (",prefix,name,narguments);
		int narg=0;
		RegexNode variableNode=startNode.getNode("$","start","arguments","argument","variable");
		while (variableNode != null) {
		    String vname=variableNode.getText("name");
		    narg++;
		    Variable variable=localVariables.get(vname.toLowerCase());
		    if (variable == null) {
			variable = new Variable(vname);
			String type=implicitList.getType(parent, name);
		    }
		    variableNode.setAttribute("dataStructure",variable);
		    localVariables.put(vname.toLowerCase(),variable);
		    variable.setArgumentNumber(narg,variableNode);
		    System.out.format(" %s ",vname);
		    variableNode=startNode.getNode("$","start","arguments","argument","variable");
		}
		type=startNode.getText("$","start","type");
		System.out.format(")\n");
		startNode=procedureNode.getNode("$","*","start");
	    }
	}

	public void analyseMain (RegexNode procedureNode) {
	    analyseMain (null,procedureNode);
	}
	public void analyseMain (Procedure parent, RegexNode procedureNode) {
	    String prefix="   ";
	    RegexNode mainNode=procedureNode.getNode("$","*","Main"); // only analyse first occurence of Main
	    if (mainNode != null) {
		// analyse all variables
		RegexNode variableNode=mainNode.getNode("$","...","variable");
		while (variableNode != null) {		
		    // get variable name, and number of dimensions
		    String name=variableNode.getText("name");
		    if (name == null) {
			System.out.format("Invalid structure: a variable is missing name attribute: %s\n",
					  variableNode.toString());
		    }
		    Integer ndimensions=(Integer) variableNode.getAttribute("narguments");
		    //
		    name=canonical(name);
		    Variable variable=localVariables.get(name.toLowerCase());
		    boolean processed=false;
		    // check for allocation statement (must be declared)
		    if (! processed) {
			RegexNode node=variableNode.getParent("allocate","variables","variable");
			if (node != null ) {
			    System.out.format("%s[allocate] %s",prefix,name);
			    if (variable == null) {
				System.out.format(" **not declared**");
			    } else {
				variable.assigned=true;
			    }
			    System.out.format("\n");
			    processed=true;
			};
		    }
		
		    // check for deallocation statement (must be declared)
		    if (! processed) {
			RegexNode node=variableNode.getParent("deallocate","variables","variable");
			if (node != null ) {
			    System.out.format("%s[deallocate] %s",prefix,name);
			    if (variable == null) {
				System.out.format(" **not declared**");
			    } else {
				variable.assigned=true;
			    }
			    System.out.format("\n");
			    processed=true;
			};
		    }
		
		    // check for call statement (must be declared or implicit)
		    if (! processed) {
			RegexNode node=variableNode.getParent("call","arguments","argument","variable");
			if (node != null ) {
			    System.out.format("%s[call] %s",prefix,name);
			    if (ndimensions != null) {
				System.out.format(" %dD",ndimensions);
			    }
			    if (variable == null) { // implicit variable or function
				if (externalFunctions.contains(name.toLowerCase()) || 
				    declaredFunctions.contains(name.toLowerCase())) { // check if declared (external/contains) function
				    Call functionCall=new Call(variableNode,this,name);
				    functionCall.bCalled=true;
				    functionCalls.add(functionCall);
				    variableNode.setAttribute("dataStructure",functionCall);
				    System.out.format(" **existing function**");
				} else {
				    if (ndimensions == null) { // no dimensions: implicit variable
					variable=createVariable(name,variableNode); // implicit variable
					variableNode.setAttribute("dataStructure",variable);
					localVariables.put(name.toLowerCase(),variable);
					variable.type=implicitList.getType(parent,name);
					variable.calls.add(variableNode);
				    } else {   // must be an unknown function
					Call functionCall=new Call(variableNode,this,name);
					functionCall.bCalled=true;
					functionCalls.add(functionCall);
					variableNode.setAttribute("dataStructure",functionCall);
					System.out.format(" function(%d)",ndimensions);
					System.out.format(" **new function**");	
				    };
				}
			    } else {      // this is a local variable
				variableNode.setAttribute("dataStructure",variable);
				variable.calls.add(variableNode);
			    }
			    System.out.format("\n");
			    processed=true;
			};
		    }
		
		    // check for write statement (must be declared or implicit)
		    if (! processed) {
			RegexNode node=variableNode.getParent("write","arguments","argument","variable");
			if (node != null ) {
			    System.out.format("%s[write] %s",prefix,name);
			    if (variable == null) {
				if (externalFunctions.contains(name.toLowerCase()) || 
				    declaredFunctions.contains(name.toLowerCase())) { // check if declared (external/contains) function
				    // found declared function
				    Call functionCall=new Call(variableNode,this,name);
				    functionCall.bCalled=true;
				    functionCalls.add(functionCall);
				    variableNode.setAttribute("dataStructure",functionCall);
				} else {
				    if (ndimensions == null) { // no dimensions: implicit variable
					variable=createVariable(name,variableNode); // implicit variable
					variableNode.setAttribute("dataStructure",variable);
					localVariables.put(name.toLowerCase(),variable);
					variable.type=implicitList.getType(parent,name);
				    } else {   // must be an unknown function
					Call functionCall=new Call(variableNode,this,name);
					functionCall.bCalled=true;
					functionCalls.add(functionCall);
					variableNode.setAttribute("dataStructure",functionCall);
				    };
				}
			    } else {
				// found declared variable
				variableNode.setAttribute("dataStructure",variable);
			    }
			    System.out.format("\n");
			    processed=true;
			};
		    }
		
		    // check for read statement (must be declared or implicit)
		    if (! processed) {
			RegexNode node=variableNode.getParent("read","expressions","expression","variable");
			if (node != null ) {
			    System.out.format("%s[read] %s",prefix,name);
			    if (variable == null) {
				if (ndimensions == null) {
				    variable=createVariable(name,variableNode);
				    variableNode.setAttribute("dataStructure",variable);
				    localVariables.put(name.toLowerCase(),variable);
				    variable.type=implicitList.getType(parent,name);
				    variable.assigned=true;
				} else {   // "variable" is a function...
				    System.out.format(" ** error: can not write to function **");
				};
			    } else {
				variableNode.setAttribute("dataStructure",variable);
				variable.assigned=true;
			    }
			    System.out.format("\n");
			    processed=true;
			};
		    }
		
		    // check for assignment statement (must be declared or implicit)
		    if (! processed) {
			RegexNode node=variableNode.getParent("assignment","variable");
			if (node != null ) {
			    System.out.format("%s[assignment] %s",prefix,name);
			    if (variable == null) {
				if (ndimensions == null) {
				    variable=createVariable(name,variableNode);
				    variableNode.setAttribute("dataStructure",variable);
				    localVariables.put(name.toLowerCase(),variable);
				    variable.type=implicitList.getType(parent,name);
				    variable.assigned=true;
				} else {
				    System.out.format(" ** error: can not write to function **");
				}
			    } else {
				variableNode.setAttribute("dataStructure",variable);
				variable.assigned=true;
			    }
			    System.out.format("\n");
			    processed=true;
			};
		    }
		
		    // check for do statement (must be declared or implicit)
		    if (! processed) {
			RegexNode node=variableNode.getParent("do","variable");
			if (node != null ) {
			    System.out.format("%s[do] %s",prefix,name);
			    if (variable == null) {
				if (ndimensions == null) {
				    variable=createVariable(name,variableNode);
				    variableNode.setAttribute("dataStructure",variable);
				    localVariables.put(name.toLowerCase(),variable);
				    variable.type=implicitList.getType(parent,name);
				    variable.assigned=true;
				} else {
				    System.out.format(" ** error: can not use function as iterator **");
				}
			    } else {
				variableNode.setAttribute("dataStructure",variable);
				variable.assigned=true;
			    }
			    System.out.format("\n");
			    processed=true;
			};
		    }
		
		    // if not processed so far, this is "access only" variable
		    if ( ! processed) {
			if (variable == null) {
			    if (externalFunctions.contains(name.toLowerCase()) || 
				declaredFunctions.contains(name.toLowerCase())) { // check if declared (external/contains) function
				// found declared function
				Call functionCall=new Call(variableNode,this,name);
				functionCall.bCalled=true;
				functionCalls.add(functionCall);
				variableNode.setAttribute("dataStructure",functionCall);
			    } else {
				if (ndimensions == null) { // no dimensions: implicit variable
				    variable=createVariable(name,variableNode); // implicit variable
				    variableNode.setAttribute("dataStructure",variable);
				    localVariables.put(name.toLowerCase(),variable);
				    variable.type=implicitList.getType(parent,name);
				} else {   // must be an unknown function
				    Call functionCall=new Call(variableNode,this,name);
				    functionCall.bCalled=true;
				    functionCalls.add(functionCall);
				    variableNode.setAttribute("dataStructure",functionCall);
				};
			    }
			    System.out.format("%s[undefined] %s",prefix,name);
			} else {
			    variableNode.setAttribute("dataStructure",variable);
			    System.out.format("%s[peeked] %s",prefix,name);
			};
			System.out.format("\n");
		    }
		
		    // check how variable is used
		
		    // access to value
		
		    //System.out.format("  %s\n",variable.toString());
		
		    variableNode=mainNode.getNode("$","...","variable");
		}
		// analyse subroutine calls
		RegexNode callNode=mainNode.getNode("$","Main","call");
		while (callNode != null) {		
		    // get call name, and number of dimensions
		    String name=callNode.getText("name");
		    Integer narguments=(Integer) callNode.getAttribute("narguments");
		    name=canonical(name);
		    Call subroutineCall=new Call(callNode,this,name);
		    subroutineCalls.add(subroutineCall);
		    subroutineCall.bCalled=true;
		    callNode.setAttribute("dataStructure",subroutineCall);
		    callNode=mainNode.getNode("$","Main","call");
		}

	    }
	    procedureNode.getNodeReset("$","*","Main");
	}
	
	public void analyseContains (RegexNode procedureNode) {
	    analyseContains(null,procedureNode);
	}
	public void analyseContains (Procedure parent, RegexNode procedureNode) {
	    RegexNode containsNode=procedureNode.getNode("$","*","Contains");
	    while (containsNode != null) {
		// analyse subroutines in Contains statement
		RegexNode subroutineNode=containsNode.getNode("$","*","Subroutine");
		while (subroutineNode != null) {		
		    String name=canonical(subroutineNode.getText("$","Subroutine","start","name"));
		    if (name == null) {
			System.out.format("Strange name s=%s\n",subroutineNode.toString(2));
		    }
		    System.out.format("Here s=%s\n",name);
		    Subroutine subroutine=localSubroutines.get(name.toLowerCase());
		    if (subroutine == null) {
			System.out.format("Here A1 s=%s\n",name);
			subroutine = new Subroutine(subroutineNode,this);
			System.out.format("Here A2 s=%s\n",name);
			localSubroutines.put(name.toLowerCase(),subroutine);
			for (Map.Entry<String, RegexNode> subroutineEntry : subroutine.entries.entrySet()) {
			    name = subroutineEntry.getKey();
			    Call subroutineCall=new Call(this,name);
			    subroutineCall.bLocal=true;
			    subroutineCall.target=subroutine;
			    subroutineCalls.add(subroutineCall);
			}
		    } else {
			System.out.format("Subroutine already declared: %s\n",name);
		    }
		    subroutineNode.setAttribute("dataStructure",subroutine);
		    subroutineNode=containsNode.getNode("$","*","Subroutine");
		}
		// analyse functions in Contains statement
		RegexNode functionNode=containsNode.getNode("$","*","Function");
		while (functionNode != null) {		
		    String name=canonical(functionNode.getText("$","Function","start","name"));
		    System.out.format("Here f=%s \n",name);
		    Function function=localFunctions.get(name.toLowerCase());
		    if (function == null) {
			function = new Function(functionNode,this);
			localFunctions.put(name.toLowerCase(),function);
			for (Map.Entry<String, RegexNode> functionEntry : function.entries.entrySet()) {
			    name = functionEntry.getKey();
			    declaredFunctions.add(name.toLowerCase());
			}
		    } else {
			System.out.format("Function already declared: %s\n",name);
		    }
		    functionNode.setAttribute("dataStructure",function);
		    functionNode=containsNode.getNode("$","*","Function");
		}
		containsNode=procedureNode.getNode("$","*","Contains");
	    }
	}
	public Variable createVariable(String name, RegexNode variableNode) {
	    Variable variable = new Variable(name);
	    variableNode.setAttribute("dataStructure",variable);
	    Integer dim=0;
	    RegexNode argumentNode = variableNode.getNode("$","variable","arguments","argument");
	    while (argumentNode != null) {
		dim=dim+1;
		variable.dimensions.put(dim,argumentNode);
		argumentNode = variableNode.getNode("$","variable","arguments","argument");
	    }
	    variable.declaration=variableNode;
	    return variable;
	} 
	private class ImplicitList {
	    List<Implicit>   list      = new ArrayList<Implicit>();
	    public void add(String type, int from, int to) {
		Implicit implicit=new Implicit(type,from,to);
		list.add(0,implicit); // fortran standard searches last entries first, and exits if match is found...
	    }
	    public String getType(String name) {
		return getType(null,name);
	    }
	    public String getType(Procedure parent, String name) {
		int n=(int) name.substring(0,1).charAt(0);
		for (Implicit implicit: list) {
		    if ( implicit.from  <= n && n <= implicit.to) {
			return implicit.type;
		    };
		}
		if (parent != null) {
		    String type=parent.implicitList.getType(name);
		    if (type==null) type="none";
		    return type;
		}
		return "none";
	    }
	}
	private class Implicit {
	    String type;
	    int from;
	    int to;
	    public Implicit(String type, int from, int to) {
		this.type=type;
		this.from=from;
		this.to=to;
	    }
	}
	private String canonical (String name) {
	    if (name != null) {
		return name.toLowerCase();
	    } else {
		return name;
	    }
	}
    }
    public class Variable {
	private String          name; // name of variable
	private Procedure       procedure; // the procedure where this variable belongs
	private Integer         argumentNumber;
	private RegexNode     argument; // argument node in node tree
	private Integer         arguments;
	private RegexNode     declaration; // declaration node in node tree
	private RegexNode     commonBlock; // commonblock node in node tree
	private RegexNode     data; // data node in node tree
	private String          type;
	private String          attribute;
	private String          init;
	private Boolean         assigned=false; // does the variable receive any value within the procedure?
	private Boolean         caller=false;   // is the variable used in a call?
	private Boolean         external=false; // is this an external function?
	private Boolean         isFunction=false; // is this a function?
	private Integer         ndimensions = null;
	private Variable        parent; 
	private HashMap<Integer,RegexNode>  dimensions=new HashMap<Integer,RegexNode>();
	List<RegexNode> calls = new ArrayList<RegexNode>();
	public Variable(Variable parent) {
	    this.name          = parent.name;           // name of variable
	    this.procedure     = parent.procedure; // the procedure where this variable belongs
	    this.argumentNumber= parent.argumentNumber;
	    this.argument      = parent.argument; // argument node in node tree
	    this.arguments     = parent.arguments;
	    this.declaration   = parent.declaration; // declaration node in node tree
	    this.commonBlock   = parent.commonBlock; // commonblock node in node tree
	    this.data          = parent.data; // data node in node tree
	    this.type          = parent.type;
	    this.attribute     = parent.attribute;
	    this.init          = parent.init;
	    this.assigned      = parent.assigned; // does the variable receive any value within the procedure?
	    this.external      = parent.external; // is this an external function?
	    this.ndimensions   = parent.ndimensions;
	    this.parent        = parent.parent; 
	    this.name          = parent.name;
	}
	public Variable(String name) {
	    this.name=name;
	}
	public void setArgumentNumber(Integer num, RegexNode node) {
	    argumentNumber=num;
	    argument=node;
	}
	public void setDeclaration(Integer num, RegexNode node) {
	    arguments=num;
	    declaration=node;
	}
	public void setDeclaration(RegexNode dec) {
	    declaration=dec;
	}
	public void setCommon(RegexNode comm) {
	    commonBlock=comm;
	}
	public void setAssigned() {
	    assigned=true;
	}
	public String toString() {
	    String s=name;
	    if (arguments !=null){
		s=s+String.format("(%d)",arguments);
	    }
	    if (type!=null) {
		s=s+String.format("[%s]",type);
	    }
	    return s;
	}
    }
}
