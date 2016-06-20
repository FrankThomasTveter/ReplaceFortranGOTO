import java.util.HashSet;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

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

public class FortranProcessor {
    //
    static final int noChild=RegexNode.noSubLevels; 
    static final int allChild=RegexNode.allSubLevels; 
    static int maxcnt=0;
    static boolean debug = false;
    //
    public FortranProcessor() {
    }
    //
    static public void standardise(RegexNode regex) {
	preProcessIf(regex);
	preProcessLoops(regex);
	postProcessContinue(regex);
	postProcessIfExec(regex);
	postProcessIndentation(regex);
    }
    static public void postProcessIndentation(RegexNode regex) {
	RegexNode indentationNode=regex.getNode("indentation");
	while (indentationNode != null) {
	    int level=0;
	    RegexNode pNode=indentationNode.getParent();
	    while (pNode != null) {
		// find out which level we are at...
		String pNodeName=pNode.getNodeName();
		if (pNodeName.equals("IfStatements") || 
		    pNodeName.equals("DoStatements")) {
		    level=level+1;
		}
		// keep sub-nodes intact, shifting them to the front of the indentation...
		indentationNode.setLabelAll(' ',"*"); // make white-space for the sub-nodes 
		indentationNode.unignoreAll("*"); // make sure no sub-node is ignored
		indentationNode.hideTheRestAll("rest",""); // hide area between sub-nodes with node-name "rest"
		indentationNode.rmNodeAll("","rest"); // remove "rest"-nodes
		String indentation=indentationNode.getText(); // get minimum indentation
		int indentationLength=indentation.length(); // get minimum length of indentation
		int deltaLength=Math.max(1,(6+level*3)-indentationLength); // get indentation increment
		String newIndentation= String.format("%1$"+deltaLength+ "s", ""); // make indentation increment
		indentationNode.appendText(newIndentation); // add increment to indentation
		pNode=pNode.getParent();
	    }
	    indentationNode=regex.getNode("indentation");
	}
    }
    static public void postProcessIfExec(RegexNode regex) {
	// transform Single ifblocks to ifexec statements
	//regex.dumpToFile("   dumpX1_%d.tree");
	int it=0; // search for IfBlock first, and then delayedIfBlock
	RegexNode ifBlockNode=regex.getNode("IfBlock");
	if (ifBlockNode==null) {it++;ifBlockNode=regex.getNode("delayedIfBlock");};
	while (ifBlockNode != null) {
	    if(debug)System.out.format("Processing IfBlock: %s\n",ifBlockNode.getIdentification());
	    RegexNode nextNode=null;
	    if (it==0) {nextNode=regex.getNode("IfBlock");if (nextNode==null) it++;}
	    if (it==1) {nextNode=regex.getNode("delayedIfBlock");if (nextNode==null) it++;}
	    if (ifBlockNode.hasStructure("if","IfStatements","endif")) {
		RegexNode ifNode           = ifBlockNode.getFirstNode("$","*","if");
		RegexNode ifStatementsNode = ifBlockNode.getFirstNode("$","*","IfStatements");
		RegexNode endifNode        = ifBlockNode.getFirstNode("$","*","endif");
		RegexNode indNode          = ifNode.getFirstNode("$","if","indentation");
		RegexNode expNode          = ifNode.getFirstNode("$","if","expression");
		RegexNode linNode          = ifNode.getFirstNode("$","if","linefeed");
		RegexNode exeNode          = ifStatementsNode.getFirstNode("$","IfStatements","*");
		if(debug)System.out.format("   Number of children %d\n",ifStatementsNode.countChildren());
		if (ifStatementsNode.countChildren() == 1 & indNode!= null & expNode!= null & linNode!= null & exeNode!= null) {
		    RegexNode rmindNode=exeNode.getFirstNode("$","*","indentation");
		    RegexNode rmlinNode=exeNode.getFirstNode("$","*","linefeed");
		    if(debug)System.out.format("      ExeNode %s\n",exeNode.getNodeName());
		    if (rmindNode != null & 
			(exeNode.getNodeName().equals("assignment") ||
			 exeNode.getNodeName().equals("cycle") ||
			 exeNode.getNodeName().equals("exit") ||
			 exeNode.getNodeName().equals("return")) ) {
			if(debug)System.out.format("         Indentation count %d\n",indNode.countChildren());
			if (rmindNode.countChildren() == 0 & linNode != null) {
			    if(debug)System.out.format("            Processing.\n");
			    rmindNode.rmNode("");
			    rmlinNode.rmNode("");
			    indNode.rmNode("");
			    expNode.rmNode("");
			    linNode.rmNode("");
			    exeNode.rmNode("");
			    RegexNode ifExecNode=new RegexNode("ifExec:@if (@) ¤@;"
								  +   "ifCommand:@;",
								  ':',';','¤','@',
								  indNode,expNode,linNode,exeNode);
			    ifExecNode.replace(ifBlockNode);
			}
		    }
		} else if (ifStatementsNode.countChildren() == 0) {
		    ifBlockNode.rmNode("");
		}
	    }
	    ifBlockNode=nextNode;
	}
	//regex.dumpToFile("   dumpX2_%d.tree");
    }
    // remove lingering continue statements
    static public void postProcessContinue(RegexNode regex) {
	RegexNode continueNode=regex.getNode("continue");
	while (continueNode != null) {
	    if(debug)System.out.format("Processing Continue: %s\n",continueNode.getIdentification());
	    RegexNode nextNode=regex.getNode("continue");
	    RegexNode indentationNode=continueNode.getFirstNode("indentation");
	    if (indentationNode != null) {
		if (indentationNode.countChildren()  == 0) {
		    continueNode.rmNode("");
		}
	    }
	    continueNode=nextNode;
	}
    }	    
    //
    static private void preProcessIf(RegexNode regex) {
	System.out.format("Processing IF statements.\n");
	// loop through program statements
	RegexNode statementsNode=regex.getNode("Statements");
	while (statementsNode != null) {
	    //
	    // loop over ifExec statements (if() exec), and transform them to if-endif statements).
	    //
	    RegexNode ifExecNode=statementsNode.getNode("ifExec");
	    while (ifExecNode != null) {	
		RegexNode nextIfExecNode=statementsNode.getNode("ifExec");
		preProcessIfExec(ifExecNode);
		//
		ifExecNode=nextIfExecNode;
	    }
	    //
	    // loop over ifControl statements (if(i)L1,L2,L3), and transform them to if-else-endif goto-statements).
	    //
	    RegexNode ifControlNode=statementsNode.getNode("ifControl");
	    while (ifControlNode != null) {	
		RegexNode nextIfControlNode=statementsNode.getNode("ifControl");
		preProcessIfControl(ifControlNode);
		ifControlNode=nextIfControlNode;
	    }
	    //
	    statementsNode=regex.getNode("Statements"); // loop over all "main" nodes...
	}
    }
    static private void preProcessIfExec(RegexNode ifExecNode) {
	if (debug)System.out.format("   Replacing ifExec (%s) by ifBlock\n",ifExecNode.getIdentification());
	// analyse contents
	RegexNode indentationNode=ifExecNode.getFirstNode("$","ifExec","indentation");
	RegexNode expressionNode=ifExecNode.getFirstNode("$","ifExec","expression");
	RegexNode commandNode=ifExecNode.getFirstNode("$","ifExec","ifCommand","*");
	String in=getIndentation(indentationNode);
	//
	RegexNode newIfNode=new 
	    RegexNode ("if:@if (@) then¤;"
			 +  "linefeed:\n;",
			 ':',';','¤','@',
			 indentationNode,expressionNode);
	indentationNode.setLabel("<Indentation>");
	expressionNode.setLabel("<Expression>");
	//
	RegexNode newIndentationNode=new RegexNode("indentation:"+in+"   ;",':',';');
	RegexNode oldIndentationNode = commandNode.getFirstNode("$","...","indentation");
	if (oldIndentationNode != null) {
	    oldIndentationNode.rmNode("");
	}
	commandNode.prependChild(newIndentationNode,"<Indentation>");
	//
	RegexNode newIfStatementsNode=new 
	    RegexNode ("IfStatements:@;",
			 ':',';','@',
			 commandNode);
	//
	// ....make endif statement
	RegexNode newEndifNode=new
	    RegexNode ("endif:¤endif¤;"
			 +   "indentation:"+in+";"
			 +   "linefeed:\n;",
			 ':',';','¤');
	newEndifNode.getFirstNode("indentation").setLabel("<Indentation>");
	newEndifNode.getFirstNode("linefeed").setLabel("<CR>");
	//
	RegexNode newIfBlockNode=new 
	    RegexNode ("IfBlock:@@@;",
			 ':',';','@',
			 newIfNode,newIfStatementsNode,newEndifNode);
	//
	newIfBlockNode.replace(ifExecNode);
    }
    static private void preProcessIfControl(RegexNode ifControlNode) {
	System.out.format("   Replacing ifControl (%s) by ifBlock\n",ifControlNode.getIdentification());
	RegexNode indentationNode=ifControlNode.getFirstNode("$","ifControl","indentation");
	Integer indentation=indentationNode.getText().length();
	if (indentation < 3) indentation=3;
	String in=whiteSpace(indentation);
	RegexNode newIndentationName2=new RegexNode("indentation:"+in+"   ;",':',';');
	RegexNode newIndentationNode3=new RegexNode("indentation:"+in+"   ;",':',';');
	RegexNode ifExpressionNode=ifControlNode.getFirstNode("$","ifControl","expression");
	RegexNode elseifExpressionNode=new RegexNode(ifExpressionNode);
	RegexNode elseExpressionNode=new RegexNode(ifExpressionNode);
	
	// manipulate expression (X) => (X.le.0), (X.eq.0), (X.gt.0)
	
	RegexNode newIfNode=new 
	    RegexNode ("if:¤if (¤) then¤;"
			 +   "indentation:"+in+";"
			 +   "expression:@¤0;"
			 +      "operator:¤;"
			 +         "lt:.lt.;"
			 +   "linefeed:\n;",
			 ':',';','¤','@',
			 ifExpressionNode);
	RegexNode newElseifNode=new 
	    RegexNode ("elseif:¤elseif (¤) then¤;"
			 +   "indentation:"+in+";"
			 +   "expression:@¤0;"
			 +      "operator:¤;"
			 +         "eq:.eq.;"
			 +   "linefeed:\n;",
			 ':',';','¤','@',
			 elseifExpressionNode);
	RegexNode newElseNode=new 
	    RegexNode ("elseif:¤elseif (¤) then¤;"
			 +   "indentation:"+in+";"
			 +   "expression:@¤0;"
			 +      "operator:¤;"
			 +         "gt:.gt.;"
			 +   "linefeed:\n;",
			 ':',';','¤','@',
			 elseExpressionNode);
	RegexNode newEndifNode=new 
	    RegexNode ("endif:¤end if¤;"
			 +   "indentation:"+in+";"
			 +   "linefeed:\n;",
			 ':',';','¤');
	RegexNode ifControlLabelNode=ifControlNode.getNode("control");
	RegexNode elseifControlLabelNode=ifControlNode.getNode("control");
	RegexNode elseControlLabelNode=ifControlNode.getNode("control");
	ifControlNode.getNodeReset("control");    // clean up
	
	RegexNode newIfStatementsNode=new 
	    RegexNode ("IfStatements:¤;"
			 +   "goto:¤goto @¤;"
			 +      "indentation:"+in+"   ;"
			 +      "linefeed:\n;",
			 ':',';','¤','@',
			 ifControlLabelNode);
	RegexNode newElseifStatementsNode=new 
	    RegexNode ("IfStatements:¤;"
			 +   "goto:¤goto @¤;"
			 +      "indentation:"+in+"   ;"
			 +      "linefeed:\n;",
			 ':',';','¤','@',
			 elseifControlLabelNode);
	RegexNode newElseStatementsNode=new 
	    RegexNode ("IfStatements:¤;"
			 +   "goto:¤goto @¤;"
			 +      "indentation:"+in+"   ;"
			 +      "linefeed:\n;",
			 ':',';','¤','@',
			 elseControlLabelNode);
	//
	RegexNode newIfBlockNode=new 
	    RegexNode ("IfBlock:@@@@@@@;",
			 ':',';','¤','@',
			 newIfNode,newIfStatementsNode,
			 newElseifNode,newElseifStatementsNode,
			 newElseNode,newElseStatementsNode,
			 newEndifNode);
	//
	newIfBlockNode.replace(ifControlNode);
	//
    }
    static private void preProcessLoops(RegexNode regex) {
	System.out.format("Processing Loops.\n");
	RegexNode.define("<Control>");
	RegexNode.define("<Target>");
	RegexNode.define("<Passive>");
	// loop through program statements
	RegexNode statementsNode=regex.getNode("Statements");
	while (statementsNode != null) {
	    ArrayList<String> newVariablesList = new ArrayList<String>(); // is updated so it must exist
	    ArrayList<String> initVariablesList = new ArrayList<String>(); // is updated so it must exist
	    ArrayList<String> newLabelsList    = new ArrayList<String>(); // is updated so it must exist
	    ArrayList<String> obsoleteVariablesList = new ArrayList<String>(); // is updated so it must exist
	    // make list of original references
	    HashMap<String,ArrayList<RegexNode>> originalReferenceMap = null;
	    RegexNode declarationsNode=statementsNode.getPrevSibling();
	    if (declarationsNode != null) { // we have some declarations
		if (declarationsNode.getNodeName().equals("Declarations")) {
		    originalReferenceMap = declarationsNode.makeMap("declaration","references","reference");
		}
	    } else {
		originalReferenceMap = new HashMap<String,ArrayList<RegexNode>>();
	    }
	    HashMap<String,ArrayList<RegexNode>> labels = null;
	    HashMap<String,ArrayList<RegexNode>> originalTargetMap = statementsNode.makeMap("target");
	    HashMap<String,ArrayList<RegexNode>> allTargetMap;
	    HashMap<String,ArrayList<RegexNode>> doControlMap;
	    boolean changed=true;
	    while (changed) {
		changed=false;
		statementsNode.unignoreAll(); // process all nodes 
		RegexNode codeNode;
		//
		// ********************* PROCESS LABELLED DO LOOPS ***************************
		//
		allTargetMap = statementsNode.makeMap("target");
		doControlMap = statementsNode.makeMap("do","control");
		doControlMap = statementsNode.makeMap(doControlMap,"dofor","control");
		doControlMap = statementsNode.makeMap(doControlMap,"dowhile","control");
		for (Map.Entry<String,ArrayList<RegexNode> > entry : allTargetMap.entrySet()) {
		    String name = entry.getKey();
		    ArrayList<RegexNode> doNodes = doControlMap.get(name);
		    if (doNodes != null) {
			changed=(processDoLoops(statementsNode,name,doNodes,originalReferenceMap,originalTargetMap,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
		    }
		}
		//
		// ********************* PROCESS GOTO STATEMENTS ***************************
		//
		allTargetMap = statementsNode.makeMap("target");
		doControlMap = statementsNode.makeMap("do","control");
		doControlMap = statementsNode.makeMap(doControlMap,"dofor","control");
		doControlMap = statementsNode.makeMap(doControlMap,"dowhile","control");
		for (Map.Entry<String,ArrayList<RegexNode> > entry : allTargetMap.entrySet()) {
		    String name = entry.getKey();
		    ArrayList<RegexNode> doNodes = doControlMap.get(name);
		    if (doNodes == null) { // this is not a do-control variable...
			changed=(processGoto(statementsNode,name,originalReferenceMap,originalTargetMap,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
		    }
		}
	    } // changed loop
	    // fold structure...
	    statementsNode.foldAll();

	    findObsoleteVariables(statementsNode,newVariablesList,initVariablesList,obsoleteVariablesList);
	    
	    if(debug)statementsNode.dumpToFile(String.format("   debugObsolete.tree"));

	    removeObsoleteVariablesFromVarLists(statementsNode,obsoleteVariablesList);

	    if(debug)statementsNode.dumpToFile(String.format("   debugDelay.tree"));

	    processDelayedCode(statementsNode,newVariablesList,initVariablesList,obsoleteVariablesList);

	    if(debug)statementsNode.dumpToFile(String.format("   debugMake.tree"));

	    makeCode(statementsNode,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList);

	    if(debug)statementsNode.dumpToFile(String.format("   debugRm.tree"));

	    // remove targets...
	    allTargetMap = statementsNode.makeMap("target");
	    doControlMap = statementsNode.makeMap("do","control");
	    doControlMap = statementsNode.makeMap(doControlMap,"dofor","control");
	    doControlMap = statementsNode.makeMap(doControlMap,"dowhile","control");
	    for (Map.Entry<String,ArrayList<RegexNode> > entry : allTargetMap.entrySet()) {
		String name = entry.getKey();
		ArrayList<RegexNode> doNodes = doControlMap.get(name);
		if (doNodes == null) { // this is not a do-control variable...
		    changed=(processGotoTarget(statementsNode,name,originalReferenceMap,originalTargetMap,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
		}
	    }
	    if(debug)statementsNode.dumpToFile("   dumpX4_%d.tree");
	    // add declarations...


	    addDeclarations(statementsNode,newVariablesList,initVariablesList,obsoleteVariablesList);
	    if(debug)statementsNode.dumpToFile(String.format("   debugLast.tree"));

	    statementsNode=regex.getNode("Statements"); // loop over all "main" nodes...
	}

	//regex.check();
    }
    // make "delayedDoLoop" with attribute "variable" and "label" containing details on variable and label
    // make "delayedIfBlock" with attribute "varList" and "enterIfTrue", use HashMap {variable} -> "enterIfTrue"...
    // make "delayedAssignment" with attributes "reference" and "value"
    // make "delayedExit" with attribute "label" (duplicate last statement before exit, if statement is not passive...)
    // make "delayedDelete" - this is where original code is stored before it is removed

    private static boolean processDoLoops(RegexNode statementsNode, String name, ArrayList<RegexNode> doNodes,
					  HashMap<String,ArrayList<RegexNode>> originalReferenceMap,
					  HashMap<String,ArrayList<RegexNode>> originalTargetMap,
					  ArrayList<String> newVariablesList,
					  ArrayList<String> initVariablesList,
					  ArrayList<String> newLabelsList,
					  ArrayList<String> obsoleteVariablesList) {
	if (debug) System.out.format("processDoLoops Entering with %s\n",name);
	boolean changed=false;
	HashMap<String,ArrayList<RegexNode>> unknownControlMap = statementsNode.makeMap("unknownControl");
	HashMap<String,ArrayList<RegexNode>> oldControlMap = statementsNode.makeMap("oldControl");
	HashMap<String,ArrayList<RegexNode>> controlMap    = statementsNode.makeMap("control");
	HashMap<String,ArrayList<RegexNode>> targetMap     = statementsNode.makeMap("target");
	ArrayList<RegexNode> unknownControlList = unknownControlMap.get(name); // control-labels
	ArrayList<RegexNode> oldControlList     = oldControlMap.get(name); // control-labels
	ArrayList<RegexNode> controlList        = controlMap.get(name); // control-labels
	ArrayList<RegexNode> targetList         = targetMap.get(name);     // target-labels
	resetNodeForSeek(statementsNode,controlList,targetList);
	if (debug) statementsNode.dumpToFile("   dumpX5_%d.tree");
	statementsNode.setLabelAll(' ',"target"); // make white-space in the indentation...
	if (controlList == null || targetList == null) { // missing control or destination labels...
	    if (debug & controlList == null) System.out.format("processDoLoops No control.\n");
	    if (debug & targetList == null) System.out.format("processDoLoops No target.\n");
	    return false; // nothing to do
	}
	statementsNode.markLabelAll(controlList,"¤","#",-2,"$","Statements","...","control");
	statementsNode.markLabelAll(targetList,"@","#",-2,"$","Statements","...");
	while (statementsNode.seek("(?m)(?i)(¤<Do>#)((?!¤<Do>#).*)(@-#)")) {
	    if (debug) System.out.format("   %s (c=%d l=%d): %s\n",name,controlList.size(),targetList.size(),statementsNode.replaceAnchors(statementsNode.getText()));
	    System.out.format("Found \"Labelled do-loop\" with label: %s.\n",name);
	    RegexNode startTempNode=statementsNode.hide("startNode","do",1);
	    RegexNode bodyTempNode=statementsNode.hide("bodyNode","body",2);
	    RegexNode endTempNode=statementsNode.hide("endNode","enddo",3);
	    RegexNode startDoNode=startTempNode.getFirstNode("$","startNode","*");
	    RegexNode bodyDoNode=bodyTempNode.getFirstNode("$","bodyNode","*");
	    RegexNode endDoNode=endTempNode.getFirstNode("$","endNode","*");
	    String endType=endDoNode.getNodeName();
	    boolean passiveEnd = (endType.equals("continue") || endType.equals("format"));
	    String vname=getNewName("V"+name,originalReferenceMap,newVariablesList);  // name of the variable used to transform this label
	    String lname=getNewName("L"+name,originalTargetMap,newLabelsList);  // name of the variable used to transform this label
	    if (debug) statementsNode.dumpToFile("   dumpX7_%d.tree");
	    RegexNode startIndentationNode=startTempNode.getFirstNode("indentation");
	    RegexNode startControlNode=startTempNode.getFirstNode("control");
	    startControlNode.rmNode("");
	    RegexNode newLabel=new RegexNode ("newLabel:"+lname+";",':',';');
	    startTempNode.appendText(": ",startIndentationNode);
	    startIndentationNode.appendSibling(newLabel,"<Anchor>");
	    newLabelsList.add(lname);
	    HashMap<String,ArrayList<RegexNode>> bodyControlMap = bodyTempNode.makeMap("control");
	    ArrayList<RegexNode> bodyControlList = bodyControlMap.get(name);
	    boolean varUsed=false;
	    if (bodyControlList != null)  {
		if (debug) statementsNode.dumpToFile("   dumpX11_%d.tree");
		int controlLabelCount = bodyTempNode.count(vname,"control");
		if (debug) statementsNode.dumpToFile("   dumpX13_%d.tree");
		resetNodeForSeek(bodyTempNode,bodyControlList); 
		bodyTempNode.markLabelAll(bodyControlList,"¤","#",-2,"$","...","control");
		if (debug) statementsNode.dumpToFile("   dumpX14_%d.tree");
		if ( passiveEnd  || controlLabelCount == 0) { // we can replace GOTOs by CYCLE statements...
		    bodyTempNode.hideAll( "preCode", "(?m)(?i)(¤-#)","-","$","bodyNode");
		    bodyTempNode.hideNodeGroup("newCycle","-",1,"preCode");
		    bodyTempNode.setAttributeAll("label",lname,"$","bodyNode","preCode","newCycle");
		    bodyTempNode.unhideAll("preCode");
		} else { // after a goto, we must skip to last end statement...
		    while (bodyTempNode.hideAll( "preCode", "(?m)(?i)(¤-#)([^¤#]*)$","-","bodyNode")) {
			if (debug) statementsNode.dumpToFile("   dumpX15_%d.tree");
			bodyTempNode.hideNodeGroup("newAssignment","-",1,"preCode");
			bodyTempNode.hideNodeGroup("newSkip","-",2,"preCode");
			
			// should consider copying end statement and adding a cycle instead...XXXXX
			// copyEndNode=endTempNode.copy();
			// copyLabelNode=copyEndNode.getFirstnode("target");
			// copyLabelNode.rmNode("");
			// newAssignment -> newCycleNode, newCycleNode.prependSibling(copyEndNode);
			
			varUsed=true;
			bodyTempNode.setAttributeAll("variable",vname,"$","bodyNode","preCode","newAssignment");
			bodyTempNode.setAttributeAll("value","operator:¤;false:.false.;","$","bodyNode","preCode","newAssignment");
			bodyTempNode.setNodeNameAll("oldControl","preCode","newAssignment","...","control"); // make sure we do not re-process
			if (skipCodeUsingIf(bodyTempNode,vname,lname,"newSkip"))varUsed=true;
			bodyTempNode.unhideAll("newSkip");
			bodyTempNode.unhideAll("preCode");
			bodyControlMap = bodyTempNode.makeMap("control");
			bodyControlList = bodyControlMap.get(name);
			resetNodeForSeek(bodyTempNode,bodyControlList); 
			bodyTempNode.markLabelAll(bodyControlList,"¤","#",-2,"$","...","control");
			if (debug) statementsNode.dumpToFile("   dumpX16_%d.tree");
		    }
		}
	    }
	    if (debug) statementsNode.dumpToFile("   dumpX17_%d.tree");
	    RegexNode newEndDoNode=null;
	    RegexNode indentationNode=endTempNode.getFirstNode("indentation"); // should contain the target...
	    Integer indentation=indentationNode.getText().length();
	    String ein=whiteSpace(indentation);
	    if (passiveEnd) {
		newEndDoNode = endTempNode.prependSibling(new RegexNode("newEnddo:¤;indentation:       ;",':',';','¤'),"-");
		newEndDoNode.setAttribute("label",lname);
	    } else {
		// make new indentation for original target statement...
		RegexNode newIndentationNode= new RegexNode("indentation:"+ein+";",':',';');
		newIndentationNode.replace(indentationNode);
		newIndentationNode.setLabel("-");
		// make new passive statement with original indentationNode (containing the target)...
		RegexNode newEndNode = endTempNode.appendSibling(new RegexNode("continue:@continue¤;"
									     +   "linefeed:\n;",
									     ':',';','¤','@',
									     indentationNode),
							     "-");
		newEndDoNode = endTempNode.appendSibling(new RegexNode("newEnddo:¤;indentation:       ;",
									       ':',';','¤'),
							       "-");
		newEndDoNode.setAttribute("label",lname);
	    }
	    if (endType.equals("continue")) endDoNode.rmNode("");
	    statementsNode.addUnFolded("DoLoop","-",startTempNode,newEndDoNode);
	    RegexNode s = startTempNode.getNextSibling();
	    RegexNode e = newEndDoNode.getPrevSibling();
	    if ( s != newEndDoNode) {
		statementsNode.addUnFolded("DoStatements","-",s,e);
	    }
	    if (debug) statementsNode.dumpToFile("   dumpX8_%d.tree");
	    if (varUsed) { // variable must be declared and set
		if (debug)System.out.format("%s %s A:Making Assignment after dobody\n",bodyTempNode.getIdentification(),bodyTempNode.getNodeName());
		if (! newVariablesList.contains(vname) ) newVariablesList.add(vname);
		RegexNode newAssignmentNode=new RegexNode("newAssignment:;",':',';');
		newAssignmentNode.setAttribute("indentation",getIndentation(bodyTempNode));
		newAssignmentNode.setAttribute("variable",vname);
		newAssignmentNode.setAttribute("value","operator:¤;true:.true.;");
		bodyTempNode.prependChild(newAssignmentNode,"-");
	    }
	    if (debug) statementsNode.dumpToFile("   dumpX9_%d.tree");
	    statementsNode.unhideAll("startNode");
	    statementsNode.unhideAll("bodyNode");
	    statementsNode.unhideAll("endNode");
	    if (debug) statementsNode.dumpToFile("   dumpX10_%d.tree");
	}	    
	statementsNode.foldAll();
	return changed; 
    }

    private static boolean processGoto(RegexNode statementsNode, String name,
				       HashMap<String,ArrayList<RegexNode>> originalReferenceMap,
				       HashMap<String,ArrayList<RegexNode>> originalTargetMap,
				       ArrayList<String> newVariablesList,
				       ArrayList<String> initVariablesList,
				       ArrayList<String> newLabelsList,
				       ArrayList<String> obsoleteVariablesList) {
	if (debug) System.out.format("processGoto Entering with %s\n",name);

	boolean changed=false;
	HashMap<String,ArrayList<RegexNode>> oldControlMap = statementsNode.makeMap("oldControl");
	HashMap<String,ArrayList<RegexNode>> controlMap    = statementsNode.makeMap("control");
	HashMap<String,ArrayList<RegexNode>> targetMap     = statementsNode.makeMap("target");
	ArrayList<RegexNode> oldControlList = oldControlMap.get(name); // control-labels
	ArrayList<RegexNode> controlList    = controlMap.get(name); // control-labels
	ArrayList<RegexNode> targetList     = targetMap.get(name);     // destination-labels
	if (debug) statementsNode.dumpToFile("   dumpX19_%d.tree");
	statementsNode.foldPaired();
	if (controlList == null || targetList == null) {
	    if (debug & controlList == null) System.out.format("processGoto No control.\n");
	    if (debug & targetList == null) System.out.format("processGoto No target.\n");
	    return false; // nothing to do
	}
	if (targetList.size() != 1) {
	    System.out.format("Found more than one label: %s\n",name);
	    return false;
	}

	// strategy: set all initialisation etc first, and remove later if variable is obsolete...
	boolean initTarget=true; // we need to put vname=true just before the target...
	boolean initVar=false; // initialise variable at start of statementsNode?

	String vname=getNewName("V"+name,originalReferenceMap,newVariablesList);  // name of the variable used to transform this label
	String lname=getNewName("L"+name,originalTargetMap,newLabelsList);  // name of the variable used to transform this label

	System.out.format("Processing %s\n",vname);

	RegexNode currentTargetNode=targetList.get(0);
	DoLoopPlan dp = new DoLoopPlan(statementsNode, name, currentTargetNode, controlList);

	// use existing do-loop if available
	if (dp.startLoopNode != null & dp.endLoopNode != null) {
	    // at-gotos that start+stop at a do-block, should cycle to that do-block 
	    for (RegexNode cNode : dp.atList) {
		RegexNode tParentNode=dp.targetParentNodes.get(cNode);
		RegexNode cParentNode=dp.controlParentNodes.get(cNode);
		if (tParentNode.getNodeName().equals("DoLoop")) {
		    String label=makeSureDoLoopHasLabel(tParentNode,name,originalTargetMap,newLabelsList);
		    turnGotoIntoCycle(statementsNode,vname,label,cNode); // use existing doloop, add label if necessary
		} else {
		    // add to afterList (make new do-loop)
		    dp.afterList.add(cNode);
		}
	    }
	}
	if (debug) statementsNode.dumpToFile("   dumpX20_%d.tree");
	RegexNode doLoopNode=null; // new do-loop
	// make new do-loop, add exit before enddo-statement, make if-blocks between do-loop-start and target
	if (dp.startLoopNode != null & dp.endLoopNode != null) {
	    if (dp.afterList.size() > 0) {
		doLoopNode=makeDoLoop(statementsNode,dp.startLoopNode,dp.endLoopNode,vname,lname); // make do-loop from startLoopNode and endLoopNode
		if (makeIfBlocks(statementsNode,dp.startLoopNode,currentTargetNode,vname,lname)) { // make inner if-blocks between startLoopNode and currentTargetNode
		    initTarget=false; // already done by makeIfBlocks
		    initVar=true; // variable is used
		}
		for (RegexNode cNode : dp.afterList) {// process afterList, turn goto -> cycle
		    System.out.format("%s %s Processing goto.\n",cNode.getIdentification(),cNode.getNodeName());

		    if (turnGotoIntoCycle(statementsNode, vname,lname, cNode)) {  // use new doloop
			if (! initTarget) { //we have inner if-blocks
			    initVar=true; // variable is used
			}
		    }
		}
	    }
	    doLoopNode.prependChild("-","$","DoLoop","...","target"); // move the target label to the do-loop-block (old or new)
	}
	if (debug) statementsNode.dumpToFile("   dumpX21_%d.tree");
	// make if-blocks between control and do-loop-start
	if (dp.beforeList.size() > 0) {
	    boolean mustAssign=false;
	    // process beforeList (goto->ifblocks)
	    for (RegexNode cNode : dp.beforeList) {
		// make if-blocks between control and target...
		if (makeIfBlocks(statementsNode,cNode,currentTargetNode,vname,lname)) { // make if-blocks between startLoopNode and currentTargetNode
		    initVar=true;// variable is used
		}
		if (initVar) { // we need to make an assignment
		    turnGotoIntoAssignment(statementsNode,vname,lname,cNode); // use existing doloop, add label if necessary
		} else { // no assignment needed, delete the goto statement
		    cNode.getParent().makeParent("deleteCode","-");
		}
	    }
	}
	dp.debugOutput();
	if (debug) statementsNode.dumpToFile("   dumpX22_%d.tree");
	// initialise VNAME=true before the do-loop-start
	if (doLoopNode != null & initTarget) {
	    System.out.format("Adding resetting of %s.\n",vname); 
	    doLoopNode.addAttribute("addTrueAssignmentBeforeVarList",vname); // add VLABEL=true before do-loop...
	    if (! newVariablesList.contains(vname) ) newVariablesList.add(vname);
	} else {
	    System.out.format("Adding resetting of %s.\n",vname); 
	    currentTargetNode.getParentOf("indentation").addAttribute("addTrueAssignmentBeforeVarList",vname); // add VLABEL=true before do-loop...
	    if (! newVariablesList.contains(vname) ) newVariablesList.add(vname);
	}
	if (initVar) {
	    System.out.format("Adding initialisation of %s.\n",vname); 
	    statementsNode.addAttribute("addTrueAssignmentVarList",vname); // make VLABEL=true at start of statementsNode...
	    if (! newVariablesList.contains(vname) ) newVariablesList.add(vname);
	    if (! initVariablesList.contains(vname) ) initVariablesList.add(vname);
	}
	if (debug) statementsNode.dumpToFile("   dumpX23_%d.tree");
	return false;
    }

    private static boolean processGotoTarget(RegexNode statementsNode, String name,
					    HashMap<String,ArrayList<RegexNode>> originalReferenceMap,
					    HashMap<String,ArrayList<RegexNode>> originalTargetMap,
					    ArrayList<String> newVariablesList,
					    ArrayList<String> initVariablesList,
					    ArrayList<String> newLabelsList,
					    ArrayList<String> obsoleteVariablesList) {
	if (debug) System.out.format("processGotoTarget Entering with %s\n",name);
	boolean changed=false;
	HashMap<String,ArrayList<RegexNode>> unknownControlMap = statementsNode.makeMap("unknownControl");
	HashMap<String,ArrayList<RegexNode>> oldControlMap = statementsNode.makeMap("oldControl");
	HashMap<String,ArrayList<RegexNode>> controlMap    = statementsNode.makeMap("control");
	HashMap<String,ArrayList<RegexNode>> targetMap     = statementsNode.makeMap("target");
	ArrayList<RegexNode> unknownControlList = unknownControlMap.get(name); // control-labels
	ArrayList<RegexNode> oldControlList = oldControlMap.get(name); // control-labels
	ArrayList<RegexNode> controlList    = controlMap.get(name); // control-labels
	ArrayList<RegexNode> targetList     = targetMap.get(name);     // destination-labels
	//
	// check if we have any remaining targets and no controls 
	unknownControlList = unknownControlMap.get(name); // control-labels
	oldControlList = oldControlMap.get(name); // control-labels
	controlList    = controlMap.get(name); // control-labels
	targetList     = targetMap.get(name);     // target-labels
	if (unknownControlList == null & oldControlList == null & controlList == null & targetList != null) {
	    if (debug) System.out.format("   Removing target %s\n",name);
	    for (RegexNode cTargetNode : targetList) { // loop over the controlNodes, handling each seperately
		// finally remove the target node...
		if (cTargetNode != null) {
		    RegexNode targetParentNode=cTargetNode.getParent(2); // we assume "target" is in the "indentation"...
		    String endType=targetParentNode.getNodeName();
		    if (endType.equals("format")) {
			// do nothing
		    } else if (endType.equals("continue")) {
			if (debug) System.out.format("Removing target statement\n");
			targetParentNode.rmNode("");
		    } else {
			// remove target node anyways
			targetParentNode.setLabelAll(' ',"control"); // make white-space in the indentation...
			cTargetNode.rmNode();
		    }
		}		
	    }
	}
	// re-fold unfolded blocks...
	statementsNode.foldAll();

	//statementsNode.setNodeNameAll("control","oldControl");

	if (debug) statementsNode.dumpToFile("   dumpX46_%d.tree");

	if (debug) System.out.format("processGotoTarget Done with %s %b\n",name,changed);

	return changed;
    }

    static private boolean skipCodeUsingIf(RegexNode node,
					   String vname,
					   String lname,
					   String... nodes) {
	System.out.format("Skipping code using if\n");
	boolean changed=false;
	// add vname to if-attribute "enterIfFalseVarList"
	if (debug) node.dumpToFile("   dumpX90_%d.tree");
	if(addVarAttributeToIf(node, vname, nodes)) {
	    changed=true;
	}
	// make sure we enter exposed <Do> statements...
	// add vname to do-attribute "enterIfFalseVarList"
	if (debug) node.dumpToFile("   dumpX91_%d.tree");
	if(addVarAttributeToDo(node, vname, nodes)) {
	    changed=true;
	}
	if (debug) node.dumpToFile("   dumpX92_%d.tree");
	// add new if-blocks to skip-code
	if (addNewIfBlock(node, vname, nodes)) {
	    changed=true;
	}
	if (debug) node.dumpToFile("   dumpX93_%d.tree");
	return changed;
    }
    static private boolean addVarAttributeToIf(RegexNode node, String vname, String... nodes) {
	boolean changed=false;
	RegexNode cNode=node.getNode(nodes);
	while (cNode != null) {
	    boolean first=true;
	    RegexNode child=cNode.getLastChild().getPrevSibling();
	    while (child != cNode.getFirstChild()) {
		String name=child.getNodeName();
		if (name.equals("if") ||
		    name.equals("elseif") ||
		    name.equals("else") ) {
		    if (first) {
			if (! name.equals("else")) {
			    child.addAttribute("enterIfFalseVarList",vname);
			}
			first=false;
		    } else {
			child.addAttribute("enterIfTrueVarList",vname);
		    }
		    changed=true;
		}
		child=child.getPrevSibling();
	    }
	    cNode=node.getNode(nodes);
	}
	return changed;
    }

    static private boolean addVarAttributeToDo(RegexNode node, String vname, String... nodes) {
	boolean changed=false;
	if (node.hideAll( "xdoNode", "(?m)(?i)(<Do>)","X",nodes) ) {
	    if (debug)System.out.format("   xdoNode:%s\n",node.replaceAnchors(node.getText()));
	    // split up newSkip into new If-blocks around code we need to skip
	    RegexNode xdoNode=node.getNode("xdoNode");
	    while (xdoNode != null) {
		RegexNode doNode=xdoNode.getFirstNode("$","*","*");
		if (doNode != null) {
		    doNode.addAttribute("enterIfFalseVarList",vname);
		    if(debug)System.out.format("%s %s Added enterIfFalseVarList\n",doNode.getIdentification(),doNode.getNodeName());
		}
		xdoNode=node.getNode("xdoNode");
	    }
	    node.unhideAll("xdoNode");
	    changed=true;
	}
	return changed;
    }

    static private boolean addNewIfBlock(RegexNode node, String vname, String... nodes) {
	boolean changed=false;
	// make sure wi omit ordinary exposed statements 
	if (node.hideAll( "xnewIfBlock", "(?m)(?i)([^<Fold><Passive><If><Else><Elseif><Endif><Do><Enddo>][^<Fold><If><Else><Elseif><Endif><Do><Enddo>]*)",
			       "<If>",nodes)  ||
	    node.hideAll( "xnewIfBlock", "(?m)(?i)([^<Fold><If><Else><Elseif><Endif><Do><Enddo>]*[^<Fold><Passive><If><Else><Elseif><Endif><Do><Enddo>])",
			       "<If>",nodes)) {
	    // make sure we do not nest simple if-blocks
	    RegexNode xnewIfBlockNode=node.getNode("xnewIfBlock");
	    while (xnewIfBlockNode != null) {
		RegexNode nextNode=node.getNode("xnewIfBlock");
		RegexNode childNode=xnewIfBlockNode.getOnlyChild();
		if (childNode != null) {
		    if (childNode.getNodeName().equals("newIfBlock") || 
			childNode.getNodeName().equals("delayedIfBlock")) {
			if (debug)System.out.format("%s %s C:Adding variable %s\n",childNode.getIdentification(),childNode.getNodeName(),vname);
			childNode.addAttribute("enterIfTrueVarList",vname);
			xnewIfBlockNode.unhide();
			xnewIfBlockNode=null;
		    }
		}
		if (xnewIfBlockNode != null) {
		    if (debug)System.out.format("%s %s E:Added ifBlock variable %s\n",xnewIfBlockNode.getIdentification(),
						xnewIfBlockNode.getNodeName(),vname);
		    xnewIfBlockNode.addAttribute("enterIfTrueVarList",vname);
		}
		xnewIfBlockNode=nextNode;
	    }
	    if (debug) node.dumpToFile("   dumpX27_%d.tree");
	    if (debug)System.out.format("%s %s E:Adding variable %s\n",node.getIdentification(),node.getNodeName(),vname);
	    node.setNodeNameAll("newIfBlock","xnewIfBlock"); // make sure we do not re-process
	    changed=true;
	}
	// make the if-blocks
	RegexNode codeNode=node.getNode("newIfBlock");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=node.getNode("newIfBlock");
	    changed=(processNewIfBlock(codeNode) || changed);
	    codeNode=nextCodeNode;
	}
	// reset iteration nodes
	return changed;
    }
    static public String whiteSpace(Integer length) {
	if (length == null) length=0;
	if (length==0) return "";
	return String.format("%1$"+length+ "s", "");
    }
    static private boolean processNewIfBlock(RegexNode codeNode) {
	// newIfBlock: delay all if-block creation, maintain attribute for later use
	boolean changed=true;
	if (codeNode.isunfolded()) {
	    throw new IllegalStateException(String.format("Invalid attempt to create if-block: %s.",codeNode.getIdentification()));
	}
	//if(debug)System.out.format(">>>>>>>> Making delayedIfBlock: %s %s\n",codeNode.getIdentification(),codeNode.getNodeName());
	codeNode.setNodeName("delayedIfBlock");
	// create everything but the if-expression
	RegexNode indentationNode=codeNode.getFirstNode("$","...","indentation");
	Integer indentation=0;
	if (indentationNode != null) indentation=indentationNode.getText().length();
	if (indentation < 3) indentation=3;
	String in=whiteSpace(indentation);
	//
	codeNode.hideAll("IfStatements","(?m)(?i)^.*$","-","$","delayedIfBlock");
	// increase indentation
	indentationNode=codeNode.getNode("indentation");
	while (indentationNode != null) {
	    indentationNode.prependText("   ");
	    indentationNode=codeNode.getNode("indentation");
	}
	//
	RegexNode newIfNode=new 
	    RegexNode ("if:¤if (¤) then¤;"
			 +  "indentation:"+in+";"
			 +  "expression:;" 
			 +  "linefeed:\n;",
			 ':',';','¤');
	//
	codeNode.prependChild(newIfNode,"-");
	//
	// ....make endif statement
	RegexNode newEndifNode=new
	    RegexNode ("endif:¤endif¤;"
			 +   "indentation:"+in+";"
			 +   "linefeed:\n;",
			 ':',';','¤');
	//
	codeNode.appendChild(newEndifNode,"-");
	return changed;
    }

    static private boolean processNewCycle(RegexNode codeNode, 
					   ArrayList<String> newVariablesList,
					   ArrayList<String> initVariablesList,
					   ArrayList<String> newLabelsList,
					   ArrayList<String> obsoleteVariablesList) {
	// newCycle: goto 100-> cycle L100
	boolean changed=false;
	// only process "goto"-nodes at this point
	boolean match=true;
	RegexNode sourceNode = codeNode.getFirstChild().getNextSibling();
	if (sourceNode != codeNode.getLastChild()) {
	    match=(sourceNode.getNodeName().equals("goto"));
	}
	if (match) {
	    // analyse contents
	    RegexNode indentationNode=codeNode.getFirstNode("$","...","indentation");
	    Integer indentation=indentationNode.getText().length();
	    if (indentation < 3) indentation=3;
	    String in=whiteSpace(indentation);
	    //
	    String lab=(String) codeNode.getAttribute("label");
	    RegexNode newCycleNode=new 
		RegexNode ("cycle:@cycle ¤¤;"
			     +  "newlabel:"+lab+";"
			     +  "linefeed:\n;",
			     ':',';','¤','@',
			     indentationNode);
	    indentationNode.setLabel("<Anchor>");
	    //XXXXXX controlNode.setLabel("<Control>");
	    //
	    newCycleNode.replace(codeNode);
	    changed=true;
	} else {
	    System.out.format("   Unable to process goto (%s) %s\n",codeNode.getIdentification(),codeNode.toString());
	    codeNode.setNodeNameAll("unknownControl","oldControl");
	}
	return changed;
    }
    static private boolean processNewExit(RegexNode codeNode, 
					  ArrayList<String> newVariablesList,
					  ArrayList<String> initVariablesList,
					  ArrayList<String> newLabelsList,
					  ArrayList<String> obsoleteVariablesList) {
	// newExit: goto 100-> exit L100
	boolean changed=false;
	// only process "goto"-nodes at this point
	boolean match=true;
	RegexNode sourceNode = codeNode.getFirstChild().getNextSibling();
	if (sourceNode != codeNode.getLastChild()) {
	    match=(sourceNode.getNodeName().equals("goto"));
	}
	if (match) {
	    // analyse contents
	    String in=getIndentation(codeNode);
	    String lab=(String) codeNode.getAttribute("label");
	    if (debug)System.out.format("Using EXIT label %s.\n",lab);

	    RegexNode newExitNode=new 
		RegexNode ("exit:¤exit ¤¤;"
			     +  "indentation:"+in+";"
			     +  "newlabel:"+lab+";"
			     +  "linefeed:\n;",
			     ':',';','¤');
	    //XXXXXX controlNode.setLabel("<Control>");
	    //
	    newExitNode.replace(codeNode);
	    changed=true;
	} else {
	    System.out.format("   Unable to process goto (%s) %s\n",codeNode.getIdentification(),codeNode.toString());
	    codeNode.setNodeNameAll("unknownControl","oldControl");
	}
	return changed;
    }
    static private boolean processNewAssign(RegexNode codeNode, 
					    ArrayList<String> newVariablesList,
					    ArrayList<String> initVariablesList,
					    ArrayList<String> newLabelsList,
					    ArrayList<String> obsoleteVariablesList) {
	boolean changed=false;
	if (debug)System.out.format("%s %s X:Found assignment.\n",codeNode.getIdentification(),codeNode.getNodeName());
	// newAssign; assign "false" to label variable
	// analyse contents
	String in = getIndentation(codeNode);
	String var=(String) codeNode.getAttribute("variable");
	String val=(String) codeNode.getAttribute("value");
	if (var!=null & val!=null) {
	    boolean ok=true;
	    if (codeNode.countChildren()==0) {
		ok=true;
	    } else {
		RegexNode sourceNode = codeNode.getFirstChild().getNextSibling();
		ok=(sourceNode.getNodeName().equals("goto"));
	    }
	    if (obsoleteVariablesList.contains(var)) {
		ok=false;
		if (debug) System.out.format("   Ignoring obsolete %s\n",var);
	    } else {
		if (ok) {
		    if (debug)System.out.format("%s %s L:Making assignment of %s\n",codeNode.getIdentification(),codeNode.getNodeName(),var);
		    RegexNode newAssignmentNode=new 
			RegexNode ("assignment:¤¤=¤¤;"
				     +  "indentation:"+in+";"
				     +  "reference:¤;"
				     +     "name:"+var+";"
				     +  "expression:¤;"
				     +     val
				     +  "linefeed:\n;",
				     ':',';','¤');
		    //
		    newAssignmentNode.replace(codeNode);
		    changed=true;
		} else { // remove newAssignment
		    System.out.format("Removing strange newAssignment node %s\n",codeNode.toString());
		    codeNode.rmNode("");
		    //throw new IllegalStateException(String.format("Strange newAssignmentNode %s\n",
		    //						   codeNode.getIdentification()));
		}
	    }
	} else {
	    throw new IllegalStateException(String.format("Missing \"variable\" and \"value\" attributes in %s: %s\n",
							  codeNode.getIdentification(),codeNode.toString()));
	}
	return changed;
    }
    static private boolean processNewDo(RegexNode codeNode, 
					ArrayList<String> newVariablesList,
					ArrayList<String> initVariablesList,
					ArrayList<String> newLabelsList,
					ArrayList<String> obsoleteVariablesList) {
	boolean changed=false;
	// newAssign; assign "false" to label variable
	// analyse contents
	String in = "   ";
	RegexNode indentationNode=codeNode.getFirstNode("$","...","indentation");
	if (indentationNode != null)  {
	    Integer indentation=indentationNode.getText().length();
	    if (indentation < 3) indentation=3;
	    in=whiteSpace(indentation);
	} else {
	    in = (String) codeNode.getAttribute("indentation");
	}
	//
	String lab=(String) codeNode.getAttribute("label");
	RegexNode expressionNode=codeNode.getFirstNode("expression");
	RegexNode targetTempNode=(RegexNode) codeNode.getAttribute("targetNode");
	System.out.format("   Making Do\n");
	RegexNode newDoNode;
	boolean bex=(expressionNode != null);
	if (debug & ! bex) System.out.format("No expression in newDo-node:%s\n",codeNode.getIdentification());
	if (bex) bex=(expressionNode.countChildren() > 0);
	if (! bex) {
	    newDoNode=new 
		RegexNode ("do=¤¤: Do¤;"
			     +  "indentation="+in+";"
			     +  "newlabel="+lab+";"
			     +  "linefeed=\n;",
			     '=',';','¤');
	} else {
	    newDoNode=new 
		RegexNode ("dowhile=¤¤: Do while (¤)¤;"
			     +  "indentation="+in+";"
			     +  "newlabel="+lab+";"
			     +  "expression=@;"
			     +  "linefeed=\n;",
			     '=',';','¤','@',expressionNode);
	}
	if (targetTempNode != null) {
	    newDoNode.getFirstNode("indentation").prependChild(targetTempNode);
	} else {
	    if (debug) System.out.format("%s %s Do is not a target\n",codeNode.getIdentification(),codeNode.getNodeName());
	}
	codeNode.copy(newDoNode); // keep attributes, external pointers etc.
	changed=true;
	return changed;
    }
    static private boolean processNewEndDo(RegexNode codeNode, 
					   ArrayList<String> newVariablesList,
					   ArrayList<String> initVariablesList,
					   ArrayList<String> newLabelsList,
					   ArrayList<String> obsoleteVariablesList) {
	boolean changed=false;
	// newAssign; assign "false" to label variable
	// analyse contents
	String in = "   ";
	RegexNode indentationNode=codeNode.getFirstNode("$","...","indentation");
	if (indentationNode != null)  {
	    Integer indentation=indentationNode.getText().length();
	    if (indentation < 3) indentation=3;
	    in=whiteSpace(indentation);
	} else {
	    in = (String) codeNode.getAttribute("indentation");
	}
	//
	String lab=(String) codeNode.getAttribute("label");
	if (debug)System.out.format("   Making EndDo of %s\n",lab);
	RegexNode newEndDoNode=new 
	    RegexNode ("enddo:¤End do ¤¤;"
			 +  "indentation:"+in+";"
			 +  "newlabel:"+lab+";"
			 +  "linefeed:\n;",
			 ':',';','¤');
	codeNode.copy(newEndDoNode); // keep attributes, external pointers etc.
	RegexNode doStatementsNode=codeNode.getPrevSibling();
	@SuppressWarnings("unchecked")
	    ArrayList<String> lnameList=(ArrayList<String>) codeNode.getAttribute("addExitBeforeLabelList");
	if (lnameList != null & doStatementsNode != null) {
	    codeNode.removeAttribute("addExitBeforeLabelList");
	    for (String lname : lnameList) {
		RegexNode newExitNode=new 
		    RegexNode ("exit:¤exit ¤¤;"
				 +  "indentation:"+in+";"
				 +  "newlabel:"+lname+";"
				 +  "linefeed:\n;",
				 ':',';','¤');
		doStatementsNode.appendChild(newExitNode,"-");
	    }
	}
	changed=true;
	return changed;
    }
    static private boolean processDeleteCode(RegexNode codeNode, 
					    ArrayList<String> newVariablesList,
					    ArrayList<String> initVariablesList,
					    ArrayList<String> newLabelsList,
					     ArrayList<String> obsoleteVariablesList) {
	boolean changed=false;
	codeNode.rmNode("");
	changed=true;
	return changed;
    }
    static private String getIndentation(RegexNode node) {
	String in = (String) node.getAttribute("indentation");
	if (in != null) return in;
	RegexNode indentationNode=node.getFirstNode("$","...","indentation");
	if (indentationNode == null) {
	    if (node.getParent()!=null) {
		if (node.getPrevSibling() != node.getParent().getFirstChild()) {
		    return getIndentation(node.getPrevSibling());
		} else {
		    return getIndentation(node.getParent());
		}
	    } else {
		return "   ";
	    }
	} else {
	    Integer indentation=indentationNode.getText().length();
	    return whiteSpace(indentation);
	}
    }

    static private boolean makeCode(RegexNode node,
				    ArrayList<String> newVariablesList,
				    ArrayList<String> initVariablesList,
				    ArrayList<String> newLabelsList,
				    ArrayList<String> obsoleteVariablesList,String... nodes) {
	boolean changed=false;
	// code for "newIfBlock" -> new if-block around current node
	// code for "newCycle"   -> new cycle (attribute) replaces current node
	// code for "newDo" -> new do while replaces current node
	RegexNode codeNode=node.getNode(nodes,"newDo");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=node.getNode(nodes,"newDo");
	    changed=(processNewDo(codeNode,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
	    codeNode=nextCodeNode;
	}
	// code for "newEnddo"   -> new end do replaces current node
	codeNode=node.getNode(nodes,"newEnddo");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=node.getNode(nodes,"newEnddo");
	    changed=(processNewEndDo(codeNode,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
	    codeNode=nextCodeNode;
	}
	codeNode=node.getNode(nodes,"newCycle");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=node.getNode(nodes,"newCycle");
	    changed=(processNewCycle(codeNode,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
	    codeNode=nextCodeNode;
	}
	// code for "newExit"   -> new exit (attribute) replaces current node
	codeNode=node.getNode(nodes,"newExit");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=node.getNode(nodes,"newExit");
	    changed=(processNewExit(codeNode,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
	    codeNode=nextCodeNode;
	}
	// code for "newAssignment"  -> new assign (attributes) replaces current node
	codeNode=node.getNode(nodes,"newAssignment");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=node.getNode(nodes,"newAssignment");
	    changed=(processNewAssign(codeNode,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
	    codeNode=nextCodeNode;
	}
	// code for "deleteCode"
	codeNode=node.getNode(nodes,"deleteCode");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=node.getNode(nodes,"deleteCode");
	    changed=(processDeleteCode(codeNode,newVariablesList,initVariablesList,newLabelsList,obsoleteVariablesList) || changed);
	    codeNode = nextCodeNode;
	}
	return changed;
    }
    
    // if continue is before goto, we insert a do-while loop...
    
    // L100=.false.
    // ...
    // IF() L100=.true.    // GOTO STATEMENT WAS HERE INSIDE IF-STATEMENT
    // IF (! L100) THEN
    // ...
    // END IF // L100
    // END IF // SOME OTHER ENDIF
    // IF (!L100) THEN
    // ...
    // IF() L100=.true.    // GOTO STATEMENT WAS HERE
    // IF (!L100) THEN
    // ...
    // IF () L100=.true. // GOTO INSIDE DO/WHILE LOOP 
    // CYCLE LABEL
    //...
    // END DO LABEL
    // IF (!L100) THEN
    // ...
    // L100=.true.
    // END IF
    // L100: do while(L100) // CONTINUE STATEMENT WAS HERE
    // L100=.false.
    // ... 
    // IF() L100=.true.     // GOTO STATEMENT WAS HERE 
    // CYCLE
    // ... 
    // IF() L100=.true.     // GOTO STATEMENT WAS HERE 
    // CYCLE
    // ... 
    // end do L100          // MUST BE SAME LEVEL AS CONTINUE STATEMENT, 
    //                         ...BUT IT IS NEVER EXECUTED, SO POSITION IS OTHERWISE ARBITRARY
    private static void addDeclarations(RegexNode statementsNode,
					ArrayList<String> newVariablesList,
					ArrayList<String> initVariablesList,
					ArrayList<String> obsoleteVariablesList) {
	System.out.format("Proposed declarations: %d\n", newVariablesList.size());
	ArrayList<String> createVariablesList=new ArrayList<String>();
	for (String var : newVariablesList) {
	    if (! obsoleteVariablesList.contains(var)) {
		createVariablesList.add(var);
	    }
	}
	if (createVariablesList.size() > 0) {
	    RegexNode declarationsNode=statementsNode.getPrevSibling();
	    int count=0;
	    if ( ! declarationsNode.getNodeName().equals("Declarations")) {
		System.out.format("No declarations found.\n");
		declarationsNode=new RegexNode("Declarations:;",':',';');
		//statementsNode.setLabelAll(' ',"label"); // make white-space in the indentation...
		String in = getIndentation(statementsNode);
		if (debug) statementsNode.dumpToFile("   dumpX47_%d.tree");
		declarationsNode.setAttribute("indentation",in);
		statementsNode.prependSibling(declarationsNode,"-");
	    }
	    String references=";";
	    for (String vname : createVariablesList) {
		count=count+1;
		if (count == 1) {
		    references="¤"+references
			+ "reference:¤;"
			+    "name:"+vname+";";
		} else {
		    references="¤, "+references
			+ "reference:¤;"
			+    "name:"+vname+";";
		}
		if (count > 2) {
		    String in=getIndentation(declarationsNode);
		    //
		    RegexNode newDeclarationNode=new 
			RegexNode ("declaration:¤¤ ¤¤;"
				     +  "indentation:"+in+";"
				     +  "type:logical;"
				     +  "references:"
				     +     references
				     +  "linefeed:\n;",
				     ':',';','¤');
		    declarationsNode.appendChild(newDeclarationNode,"-");
		    count=0;
		    references=";";
		}
	    }
	    if (count > 0) {
		String in=getIndentation(declarationsNode);
		//
		RegexNode newDeclarationNode=new 
		    RegexNode ("declaration:¤¤ ¤¤;"
				 +  "indentation:"+in+";"
				 +  "type:logical;"
				 +  "references:"
				 +     references
				 +  "linefeed:\n;",
				 ':',';','¤');
		declarationsNode.appendChild(newDeclarationNode,"-");
	    }
	}
    }
    private static void findObsoleteVariables(RegexNode statementsNode,
					      ArrayList<String> newVariablesList,
					      ArrayList<String> initVariablesList,
					      ArrayList<String> obsoleteVariablesList) {
	for (String vname : newVariablesList) {
	    if (! obsoleteVariablesList.contains(vname)) {	    
		int cnt=countUsedVariable(statementsNode,vname);
		if (cnt == 0) {
		    if (debug) System.out.format("   Variable %s is obsolete\n",vname);
		    obsoleteVariablesList.add(vname);
		} else {
		    if (debug) System.out.format("   Variable %s is used %d time(s).\n",vname,cnt);
		}
	    }
	}
    }
    private static void removeObsoleteVariablesFromVarLists(RegexNode statementsNode,ArrayList<String> obsoleteVariablesList) {
	ArrayList<String> attributes = new ArrayList<String>() {
	    private static final long serialVersionUID = 12345678L;
	    {
		add("enterIfTrueVarList");
		add("enterIfFalseVarList");
		add("addTrueAssignmentVarList");
		add("addTrueAssignmentBeforeVarList");
		add("addTrueAssignmentAfterVarList");
	    }};
	int cnt=0;
	RegexNode cNode=statementsNode.getNode("*");
	while (cNode != null) {
	    for (String att : attributes) {
		@SuppressWarnings("unchecked")
		    ArrayList<String> vnameList=(ArrayList<String>) cNode.getAttribute("enterIfTrueVarList");
		if (vnameList != null) {
		    for (String vname : obsoleteVariablesList) {
			vnameList.remove(vname);
		    }
		}
	    }
	    cNode=statementsNode.getNode("*");
	}
    }
    private static void processDelayedCode(RegexNode statementsNode,
					   ArrayList<String> newVariablesList,
					   ArrayList<String> initVariablesList,
					   ArrayList<String> obsoleteVariablesList) {
	simplifyCode(statementsNode,obsoleteVariablesList);
	processDelayedEnterIf(statementsNode,obsoleteVariablesList,"enterIfTrueVarList",true,"and");
	processDelayedEnterIf(statementsNode,obsoleteVariablesList,"enterIfFalseVarList",false,"or");
	processDelayedAddTrueAssignmentVarList(statementsNode,newVariablesList,initVariablesList,obsoleteVariablesList);
	processDelayedAddTrueAssignmentBeforeVarList(statementsNode,obsoleteVariablesList);
	processDelayedAddFalseAssignmentBeforeVarList(statementsNode,obsoleteVariablesList);
	processDelayedAddTrueAssignmentAfterVarList(statementsNode,obsoleteVariablesList);
    }
    private static void simplifyCode(RegexNode statementsNode,ArrayList<String> obsoleteVariablesList) {
	System.out.format("Simplifying code\n");

	// check if we have cycle just before end-do
	RegexNode codeNode=statementsNode.getNode("newEnddo");
	while (codeNode != null) {	
	    String eLabel=(String) codeNode.getAttribute("label");
	    RegexNode nextCodeNode=statementsNode.getNode("newEnddo");
	    RegexNode doStatementsNode=codeNode.getPrevSibling("DoStatements");

	    if (doStatementsNode != null) {
		if (debug)System.out.format("Simplifying last DoStatements: %s %s\n",
					    doStatementsNode.getIdentification(),
					    doStatementsNode.getNodeName());
		
		RegexNode lastNode=doStatementsNode.getLastChild().getPrevSibling();
		boolean bdone=(lastNode == doStatementsNode.getFirstChild());
		while (! bdone) {
		    if (lastNode.getNodeName().equals("newCycle")) {
			String cLabel=(String) lastNode.getAttribute("label");
			if (eLabel.equals(cLabel)) {
			    RegexNode nextLastNode=lastNode.getPrevSibling();
			    lastNode.rmNode("");
			    codeNode.removeAttribute("addExitBeforeLabelList");
			    lastNode=nextLastNode;
			    bdone=(lastNode == doStatementsNode.getFirstChild());
			} else {
			    bdone=true;
			}
		    } else {
			bdone=true;
		    }
		}
	    } else {
		System.out.format("DoLoop does not have DoStatements %s\n",codeNode.getParent().toString());
	    }
	    codeNode=nextCodeNode;
	}
    }
    private static void processDelayedEnterIf(RegexNode statementsNode,ArrayList<String> obsoleteVariablesList,
					    String att,boolean val,String op) {
	RegexNode codeNode=statementsNode.getNode("*");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=statementsNode.getNode("*");
	    @SuppressWarnings("unchecked")
		ArrayList<String> vnameList=(ArrayList<String>) codeNode.getAttribute(att);
	    if (vnameList != null) {
		if (debug) System.out.format("%s %s >>>> Found %s\n",codeNode.getIdentification(),codeNode.getNodeName(),att);
		RegexNode expressionNode=codeNode.getFirstNode("$","...","expression");
		if (expressionNode == null & codeNode.getNodeName().equals("else")) {
			codeNode.setNodeName("else if");
			RegexNode indentationNode=codeNode.getFirstNode("indentation");
			RegexNode linefeedNode=codeNode.getFirstNode("linefeed");
			RegexNode newElseifNode=new 
			RegexNode ("elseif:@elseif (¤) then@;"
				     +   "expression:;",
				     ':',';','¤','@',
				     indentationNode,linefeedNode);
			codeNode.copy(newElseifNode);
			expressionNode=codeNode.getFirstNode("$","...","expression");
		}
		if (expressionNode != null) {
		    if(debug)System.out.format("%s %s >>>> Found expression\n",codeNode.getIdentification(),codeNode.getNodeName());
		    codeNode.removeAttribute(att);
		    String exp="";
		    String nodes="";
		    for (String vname : vnameList) {
			if (! exp.equals("")) {
			    exp=exp+"¤";
			    nodes=nodes+"operator:¤;" 
				+        op+":."+op+".;";
			}
			if (! val) {
			    exp=exp+"¤";
			    nodes=nodes+"operator:¤;"
				+        "not:.not.;";
			}
			exp=exp+"¤";
			nodes=nodes+"reference:¤;"
			    +        "name:"+vname+";";
		    }
		    int cnt=expressionNode.countChildren();
		    if (cnt == 0) {
			// do nothing
		    } else if (cnt == 1) {
			if (! exp.equals("")) {
			    exp=exp+"¤";
			    nodes=nodes+"operator:¤;" 
				+        op+":."+op+".;";
			}
			exp=exp+"¤";
			nodes=nodes+"oldExpression:;";
		    } else {
			if (! exp.equals("")) {
			    exp=exp+"¤";
			    nodes=nodes+"operator:¤;" 
				+        op+":."+op+".;";
			}
			exp=exp+"¤";
			nodes=nodes+"Brackets:(¤);"
			    +        "content:¤;"
			    +           "oldExpression:;";
		    }
		    RegexNode newExpressionNode=new 
			RegexNode ("expression:"+exp+";"+nodes,':',';','¤');
		    newExpressionNode.replace(expressionNode);

		    //System.out.format("NewExpression: %s\n",newExpressionNode.toString());

		    if (cnt != 0) {
			expressionNode.replace(newExpressionNode.getFirstNode("oldExpression"));
		    }
		} else {
		    if(debug)System.out.format("%s %s >>>> Did not find expression %s\n",codeNode.getIdentification(),codeNode.getNodeName(),codeNode.toString());
		}
	    }
	    codeNode=nextCodeNode;
	}
    }
    private static void processDelayedAddTrueAssignmentVarList(RegexNode statementsNode,
							       ArrayList<String> newVariablesList,
							       ArrayList<String> initVariablesList,
							       ArrayList<String> obsoleteVariablesList) {
	ArrayList<String> vnameList=initVariablesList;
	if (vnameList != null) {
	    int cnt=0;
	    for (String vname : vnameList) {
		cnt++;
		if (debug)System.out.format("%s %s X:Making init-assignment %s\n",statementsNode.getIdentification(),statementsNode.getNodeName(),vname);
		String in=getIndentation(statementsNode);
		RegexNode newAssignmentNode=new RegexNode("newAssignment:;",':',';');
		newAssignmentNode.setAttribute("indentation",in);
		newAssignmentNode.setAttribute("variable",vname);
		newAssignmentNode.setAttribute("value","operator:¤;true:.true.;");
		statementsNode.prependChild(newAssignmentNode,"-");
	    }
	    if (debug & cnt==0) {
		System.out.format("%s %s M:No Assignment before do\n",statementsNode.getIdentification(),statementsNode.getNodeName());
	    }
	} else {
	    if (debug)System.out.format("%s %s N:No Assignment necessary before do\n",statementsNode.getIdentification(),statementsNode.getNodeName());
	}
    }
    private static void processDelayedAddTrueAssignmentBeforeVarList(RegexNode statementsNode,ArrayList<String> obsoleteVariablesList) {
	// add assignment before new do-loops
	RegexNode codeNode=statementsNode.getNode("*");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=statementsNode.getNode("*");
	    @SuppressWarnings("unchecked")
		ArrayList<String> vnameList=(ArrayList<String>) codeNode.getAttribute("addTrueAssignmentBeforeVarList");
	    if (vnameList != null) {
		if (debug)System.out.format("%s %s O:Making true Assignment after\n",codeNode.getIdentification(),codeNode.getNodeName());
		codeNode.removeAttribute("addTrueAssignmentBeforeVarList");
		// find closest node before that is not a block
		RegexNode cNode=codeNode;
		RegexNode pNode=codeNode.getParent();
		String pName=pNode.getNodeName();
		if (pName.equals("IfBlock") || pName.equals("DoLoop")) cNode=pNode; // use parent if cNode is "do", "if", "elseif"...
		int cnt=0;
		for (String vname : vnameList) {
		    cnt++;
		    String in=getIndentation(codeNode);
		    RegexNode newAssignmentNode=new RegexNode("newAssignment:;",':',';');
		    newAssignmentNode.setAttribute("indentation",in);
		    newAssignmentNode.setAttribute("variable",vname);
		    newAssignmentNode.setAttribute("value","operator:¤;true:.true.;");
		    cNode.prependSibling(newAssignmentNode,"-");
		}
		if (debug & cnt==0) {
		    System.out.format("%s %s M:No Assignment before do\n",codeNode.getIdentification(),codeNode.getNodeName());
		}
	    }
	    codeNode=nextCodeNode;
	}
    }
    private static void processDelayedAddFalseAssignmentBeforeVarList(RegexNode statementsNode,ArrayList<String> obsoleteVariablesList) {
	// add assignment before new end do statements
	RegexNode codeNode=statementsNode.getNode("*");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=statementsNode.getNode("*");
	    @SuppressWarnings("unchecked")
		ArrayList<String> vnameList=(ArrayList<String>) codeNode.getAttribute("addFalseAssignmentBeforeVarList");
	    if (vnameList != null) {
		if (debug)System.out.format("%s %s O:Making false Assignment before\n",codeNode.getIdentification(),codeNode.getNodeName());
		codeNode.removeAttribute("addFalseAssignmentBeforeVarList");
		RegexNode cNode=codeNode;
		RegexNode pNode=codeNode.getParent();
		String cName=cNode.getNodeName();
		String pName=pNode.getNodeName();
		if (cName.equals("endDo")) {
		    cNode=cNode.getPrevSibling().getLastChild().getPrevSibling();
		} else if (pName.equals("IfBlock") || pName.equals("DoLoop")) {
		    cNode=pNode; // use parent if cNode is "do", "if", "elseif"...
		}
		for (String vname : vnameList) {
		    String in=getIndentation(codeNode);
		    RegexNode newAssignmentNode=new RegexNode("newAssignment:;",':',';');
		    newAssignmentNode.setAttribute("indentation",in);
		    newAssignmentNode.setAttribute("variable",vname);
		    newAssignmentNode.setAttribute("value","operator:¤;false:.false.;");
		    cNode.prependSibling(newAssignmentNode,"-");
		}
	    }
	    codeNode=nextCodeNode;
	}
    }
    private static void processDelayedAddTrueAssignmentAfterVarList(RegexNode statementsNode,ArrayList<String> obsoleteVariablesList) {
	// add assignment before new end do statements
	RegexNode codeNode=statementsNode.getNode("*");
	while (codeNode != null) {	
	    RegexNode nextCodeNode=statementsNode.getNode("*");
	    @SuppressWarnings("unchecked")
		ArrayList<String> vnameList=(ArrayList<String>) codeNode.getAttribute("addTrueAssignmentAfterVarList");
	    if (vnameList != null) {
		if (debug)System.out.format("%s %s O:Making Assignment before enddo\n",codeNode.getIdentification(),codeNode.getNodeName());
		codeNode.removeAttribute("addTrueAssignmentAfterVarList");
		RegexNode cNode=codeNode;
		RegexNode pNode=codeNode.getParent();
		String pName=pNode.getNodeName();
		if (pName.equals("IfBlock") || pName.equals("DoLoop")) cNode=pNode; // use parent if cNode is "do", "if", "elseif"...
		for (String vname : vnameList) {
		    if (debug)System.out.format("%s %s P:Making Assignment %s at enddo\n",codeNode.getIdentification(),codeNode.getNodeName(),vname);
		    String in=getIndentation(codeNode);
		    RegexNode newAssignmentNode=new RegexNode("newAssignment:;",':',';');
		    newAssignmentNode.setAttribute("indentation",in);
		    newAssignmentNode.setAttribute("variable",vname);
		    newAssignmentNode.setAttribute("value","operator:¤;true:.true.;");
		    cNode.appendChild(newAssignmentNode,"-");
		}
	    }
	    codeNode=nextCodeNode;
	}
    }
    private static String getNewName(String name,  HashMap<String,ArrayList<RegexNode>> originalMap,ArrayList<String> newList) {
	String cname=String.format("%s",name);
	boolean unique=true;
	if (unique & originalMap != null) unique=(originalMap.get(cname)==null);
	if (unique & newList != null) unique=(!newList.contains(cname));
	int cnt=0;
	while (! unique) {
	    cname=String.format("%sB%d",name,cnt++);
	    unique=true;
	    if (unique & originalMap != null) unique=(originalMap.get(cname)==null);
	    if (unique & newList != null) unique=(!newList.contains(cname));
	}
	return cname;
    }
    private static int countUsedVariable(RegexNode pNode, String varName) {
	int cnt=0;
	cnt=cnt+countEnterIfFalseVarList(pNode, varName);
	cnt=cnt+countEnterIfTrueVarList(pNode, varName);
	return cnt;
    }
    private static int countEnterIfFalseVarList(RegexNode pNode, String varName) {
	int cnt=0;
	RegexNode cNode=pNode.getNode("*");
	while (cNode != null) {
	    @SuppressWarnings("unchecked")
		ArrayList<String> vnameList=(ArrayList<String>) cNode.getAttribute("enterIfFalseVarList");
	    if (vnameList != null) {
		for (String vname : vnameList) {
		    //System.out.format("***********Found delayed if: %s\n",vname);
		    if (vname.equals(varName)) {
			cnt++;
		    }
		}
	    }
	    cNode=pNode.getNode("*");
	}
	return cnt;
    }
    private static int countEnterIfTrueVarList(RegexNode pNode, String varName) {
	int cnt=0;
	RegexNode cNode=pNode.getNode("*");
	while (cNode != null) {
	    @SuppressWarnings("unchecked")
		ArrayList<String> vnameList=(ArrayList<String>) cNode.getAttribute("enterIfTrueVarList");
	    if (vnameList != null) {
		for (String vname : vnameList) {
		    if(debug)System.out.format("%s %s ***********Found delayed if: %s\n",cNode.getIdentification(),cNode.getNodeName(),vname);
		    if (vname.equals(varName)) {
			cnt++;
		    }
		}
	    }
	    cNode=pNode.getNode("*");
	}
	return cnt;
    }
    private static void resetNodeLabelsForSeek(RegexNode node) {
	node.markLabelAll();
	node.foldPaired();
	node.setLabelAll("-");
	node.setLabelAll("<If>",          "if");
	node.setLabelAll("<Elseif>",      "elseif");
	node.setLabelAll("<Else>",        "else");
	node.setLabelAll("<Endif>",       "endif");
	node.setLabelAll("<Do>",          "do");
	node.setLabelAll("<Do>",          "dofor");
	node.setLabelAll("<Do>",          "dowhile");
	node.setLabelAll("<Enddo>",       "enddo");
	node.setLabelAll("<Do>",          "newDo");
	node.setLabelAll("<Enddo>",       "newEnddo");
	node.setLabelAll("<Passive>",     "blank");
	node.setLabelAll("<Passive>",     "format");
	node.setLabelAll("<Passive>",     "IfStatements"); // do not include if-test around IfStatements if it is unfolded...
	node.setLabelAll("<Passive>",     "DoStatements"); // dont think this is ever used as DoStatements are either hidden by DoLoop or unfolded..
	node.setLabelAll("<Block>",       "delayedIfBlock"); // exposed ifstatements are unfolded
	node.setLabelAll("<Block>",       "IfBlock"); // exposed ifstatements are unfolded
	node.setLabelAll("<Block>",       "DoLoop"); // exposed ifstatements are unfolded
	// handle unfolded nodes...
	node.setLabelUnPaired("<Fold>","<Fold>","delayedIfBlock");
	node.setLabelUnPaired("<Fold>","<Fold>","IfBlock");
	node.setLabelUnPaired("<Fold>","<Fold>","IfStatements");
	node.setLabelUnPaired("<Fold>","<Fold>","DoLoop");
	node.setLabelUnPaired("<Fold>","<Fold>","DoStatements");
    }
    private static void resetNodeFoldForSeek(RegexNode node,RegexNode cNode) {
	node.unfoldParentAll(cNode,"<Fold>","<Fold>","delayedIfBlock","...");
	node.unfoldParentAll(cNode,"<Fold>","<Fold>","IfBlock","...");
	node.unfoldParentAll(cNode,"<Fold>","<Fold>","IfStatements","...");
	node.unfoldParentAll(cNode,"<Fold>","<Fold>","DoLoop","...");
	node.unfoldParentAll(cNode,"<Fold>","<Fold>","DoStatements","...");
    }
    private static void resetNodeFoldForSeek(RegexNode node,String target, ArrayList<RegexNode> list) {
	node.unfoldParentAll(list,"<Fold>","<Fold>","delayedIfBlock","...",target);
	node.unfoldParentAll(list,"<Fold>","<Fold>","IfBlock","...",target);
	node.unfoldParentAll(list,"<Fold>","<Fold>","IfStatements","...",target);
	node.unfoldParentAll(list,"<Fold>","<Fold>","DoLoop","...",target);
	node.unfoldParentAll(list,"<Fold>","<Fold>","DoStatements","...",target);
    }
    private static void resetNodeForSeek(RegexNode node,ArrayList<RegexNode> controlList) {
	resetNodeLabelsForSeek(node);
	resetNodeFoldForSeek(node,"control",controlList);
    }
    private static void resetNodeForSeek(RegexNode node,ArrayList<RegexNode> controlList, ArrayList<RegexNode> targetList) {
	resetNodeLabelsForSeek(node);
	resetNodeFoldForSeek(node,"control",controlList);
	resetNodeFoldForSeek(node,"target",targetList);
    }
    private static void flagParents(RegexNode statementsNode,
				    String name,
				    RegexNode targetNode,
				    RegexNode controlNode) {
	RegexNode cNode=targetNode;
	boolean bdone=false;
	while (! bdone) {
	    bdone=(cNode == statementsNode);
	    cNode.setAttribute("targetName",new String(name));
	    cNode.setAttribute("controlCount",new Integer(1));
	    cNode=cNode.getParent();
	}
	cNode=controlNode;
	bdone= (cNode == statementsNode);
	while (! bdone) {
	    RegexNode pNode=cNode.getParent();
	    String  s=(String)  pNode.getAttribute("targetName");
	    Integer c=(Integer) pNode.getAttribute("controlCount");
	    String pName=pNode.getNodeName();
	    boolean match=true;
	    if (match) match=(s != null & c != null);
	    if (match) match=s.equals(name);
	    if (match) {

		if (pName.equals("IfBlock") ||
		    pName.equals("DoLoop")) {
		    if(debug)System.out.format("%s %s Omitting parent\n",pNode.getIdentification(),pNode.getNodeName());
		} else {
		    c--;
		    cNode.setAttribute("controlName",new String(name));
		    pNode.setAttribute("controlCount",c);
		}
	    }
	    cNode=pNode;
	    bdone=(cNode == statementsNode);
	}
    }
    private static RegexNode getParentNode(RegexNode statementsNode, String name, RegexNode targetNode) {
	RegexNode cNode=targetNode;
	boolean bdone= (cNode == statementsNode);
	while (! bdone) {
	    RegexNode pNode=cNode.getParent();
	    if (pNode != null) {
		String s = (String) pNode.getAttribute("targetName");
		Integer c = (Integer) pNode.getAttribute("controlCount");
		boolean match=true;
		if (match) match=(s != null & c != null);
		if (match) match=(s.equals(name));
		if (match) match=(c==0);
		if (match) {
		    return cNode;
		}
		cNode=pNode;
		if (cNode == statementsNode) return cNode;
	    } else {
		bdone=true;
		throw new IllegalStateException(String.format("Invalid parent target: %s.\n%s\n",cNode.getIdentification(),cNode.toString()));
	    }
	}
	return null;
    }
    static private class DoLoopPlan {
	public RegexNode statementsNode;
	public RegexNode currentTargetNode;
	public ArrayList<RegexNode> controlList;
	public RegexNode startLoopNode=null;
	public RegexNode endLoopNode=null;
	public ArrayList<RegexNode> beforeList=new ArrayList<RegexNode>();
	public ArrayList<RegexNode> atList=new ArrayList<RegexNode>();
	public ArrayList<RegexNode> afterList=new ArrayList<RegexNode>();

	HashMap<RegexNode,RegexNode> controlParentNodes = new HashMap<RegexNode,RegexNode>();
	HashMap<RegexNode,RegexNode> targetParentNodes = new HashMap<RegexNode,RegexNode>();
	public DoLoopPlan(RegexNode statementsNode, String name, RegexNode currentTargetNode, ArrayList<RegexNode> controlList) {
	    this.statementsNode=statementsNode;
	    this.currentTargetNode=currentTargetNode;
	    this.controlList=controlList;
	    this.startLoopNode=null;
	    this.endLoopNode=null;
	    // make list of control-parent nodes, before/after the target node...
	    for (RegexNode cNode : controlList) {
		flagParents(statementsNode,name,currentTargetNode,cNode);
		// find common parent of target and control...
		RegexNode tParentNode=getParentNode(statementsNode,name,currentTargetNode);
		RegexNode cParentNode=getParentNode(statementsNode,name,cNode);
		targetParentNodes.put(cNode,tParentNode);
		controlParentNodes.put(cNode,cParentNode);
		Boolean before=cParentNode.isBefore(tParentNode);
		if (before) { // make if-blocks <goto 100> ... <100 continue> ...
		    if (debug)System.out.format("%s %s Found BEFORE-node\n",cNode.getIdentification(),cNode.getNodeName());
		    beforeList.add(cNode);
		} else {  // make do loop, <100 continue> ...<goto 100> ... 
		    if (startLoopNode == null) {
			startLoopNode = tParentNode;
			endLoopNode = cParentNode;
		    } else if (startLoopNode == tParentNode) {
			if (endLoopNode.isBefore(cParentNode)) {
			    endLoopNode=cParentNode;
			}
		    } else if (tParentNode.contains(startLoopNode)) {
			startLoopNode = tParentNode;
			endLoopNode = cParentNode;
		    }
		    if (cParentNode==tParentNode) { // before and after, i.e. <if> <goto 100> <else> <100 continue> <endif>, need do-loop
			System.out.format("%s %s Found AT-node\n",cNode.getIdentification(),cNode.getNodeName());
			atList.add(cNode);
		    } else  {
			System.out.format("%s %s Found AFTER-node\n",cNode.getIdentification(),cNode.getNodeName());
			afterList.add(cNode);
		    }
		}
	    }
	}
	public void debugOutput() {
	    if (startLoopNode != null & endLoopNode != null) {
		System.out.format("%s %s Start Loop\n",startLoopNode.getIdentification(),startLoopNode.getNodeName());
		Boolean before=endLoopNode.isBefore(startLoopNode);
		if (before==null) {
		    if (debug)System.out.format(">>>>>>>>>>>>>>>>>>Parent mismatch %s %s\n",startLoopNode.getIdentification(),endLoopNode.getIdentification());
		} else if (endLoopNode.isBefore(startLoopNode)) {
		    System.out.format("%s %s End Loop\n",endLoopNode.getIdentification(),endLoopNode.getNodeName());
		    endLoopNode=startLoopNode;
		}
		System.out.format("%s %s End Loop\n",endLoopNode.getIdentification(),endLoopNode.getNodeName());
	    } else {
		if (debug) System.out.format("No loop necessary...\n");
	    }
	}
    }
    static private String makeSureDoLoopHasLabel(RegexNode doLoopNode,
					  String name,
					  HashMap<String,ArrayList<RegexNode>> originalTargetMap,
					  ArrayList<String> newLabelsList) {
	String ret="";
	RegexNode labelNode=doLoopNode.getFirstNode("$","DoLoop","*","newLabel");
	RegexNode oldLabelNode=doLoopNode.getFirstNode("$","DoLoop","*","indentation","label");
	if (labelNode != null) {
	    ret=labelNode.getText();
	} else if (oldLabelNode != null) {
	    ret=oldLabelNode.getText();
	} else if (labelNode == null & oldLabelNode==null) { // make new label
	    String lab=getNewName("L"+name,originalTargetMap,newLabelsList);  // name of the variable used to transform this label
	    RegexNode doNode=doLoopNode.getFirstNode("$","DoLoop","*");
	    RegexNode indentationNode=doLoopNode.getFirstNode("$","DoLoop","*","indentation");
	    RegexNode newLabelNode=new RegexNode("newlabel="+lab+";",'=',';','¤');
	    doNode.appendText(": ",indentationNode);
	    indentationNode.appendSibling(newLabelNode,"-");
	    ret=lab;
	}
	return ret;
    }
    static private boolean turnGotoIntoCycle(RegexNode statementsNode,String vname,String lname,RegexNode cNode) {
	boolean ret=false;
	RegexNode pNode=cNode.getParentOf("indentation");
	System.out.format("%s %s Turning goto into cycle.\n",pNode.getIdentification(),pNode.getNodeName());
	String pName=pNode.getNodeName();
	cNode.setNodeName("oldControl"); // make sure we do not re-process this control-node...
	if (pName.equals("goto")) {
	    RegexNode newCycleNode=pNode.makeParent("newCycle","-");
	    newCycleNode.setAttribute("label",lname);
	    newCycleNode.addAttribute("addFalseAssignmentBeforeVarList",vname);
	} else {
	    System.out.format("Unknown control %s\n",pName);
	}
	return ret;
    }
    static private boolean turnGotoIntoAssignment(RegexNode statementsNode,String vname,String lname,RegexNode cNode) {
	boolean ret=false;
	RegexNode pNode=cNode.getParentOf("indentation");
	System.out.format("%s %s Turning goto into assignment.\n",pNode.getIdentification(),pNode.getNodeName());
	String pName=pNode.getNodeName();
	cNode.setNodeName("oldControl"); // make sure we do not re-process this control-node...
	if (pName.equals("goto")) {
	    RegexNode deleteCodeNode=pNode.makeParent("deleteCode","-");
	    RegexNode newAssignmentNode=new RegexNode("newAssignment:;",':',';');
	    newAssignmentNode.setAttribute("variable",vname);
	    newAssignmentNode.setAttribute("value","operator:¤;false:.false.;");
	    deleteCodeNode.prependSibling(newAssignmentNode,"-");
	} else {
	    System.out.format("Unknown control %s\n",pName);
	}
	return ret;
    }
    static private RegexNode makeDoLoop(RegexNode statementsNode,
			       RegexNode startLoopNode,
			       RegexNode endLoopNode,
			       String vname,
			       String lname) {// make do-loop from startLoopNode and endLoopNode
	RegexNode newDoNode=new RegexNode("newDo:;",':',';');
	newDoNode.setAttribute("label",lname);
	RegexNode newEnddoNode=new RegexNode("newEnddo:;",':',';');
	newEnddoNode.setAttribute("label",lname);
	//blockNode.addAttribute("addTrueAssignmentBeforeVarList",vname);
	newEnddoNode.addAttribute("addExitBeforeLabelList",lname);
	startLoopNode.prependSibling(newDoNode,"-");
	endLoopNode.appendSibling(newEnddoNode,"-");
	RegexNode blockNode=statementsNode.addUnFolded("DoLoop","-",newDoNode,newEnddoNode);
	statementsNode.addUnFolded("DoStatements","-",startLoopNode,endLoopNode);
	return blockNode;
    }
    static private boolean makeIfBlocks(RegexNode statementsNode,
				 RegexNode sNode,
				 RegexNode eNode,
				 String vname,
				 String lname) { // make if-blocks between startLoopNode and currentTargetNode
	boolean ret=false;
	resetNodeLabelsForSeek(statementsNode);
	resetNodeFoldForSeek(statementsNode,sNode);
	resetNodeFoldForSeek(statementsNode,eNode);
	statementsNode.markLabelAll(sNode,"¤","#",-2,"$","Statements","...");
	statementsNode.markLabelAll(eNode,"@","#",-2,"$","Statements","...");
	if (statementsNode.hideAll( "preCode", "(?m)(?i)¤.#(.*)@.#","-","$","Statements")) {
	    if (statementsNode.hideNodeGroup("newSkip","-",1,"preCode")) {
		String label=getLastEnddoLabel(statementsNode,"preCode","newSkip");
		if (label != null) {
		    if (debug)System.out.format("Using existing enddo %s for %s %s\n",label,sNode.getIdentification(),sNode.getNodeName());
		    addExitToCode(statementsNode,label,sNode.getParentOf("indentation"));
		}
		if (skipCodeUsingIf(statementsNode,vname,lname,"newSkip")) ret=true;
		statementsNode.unhideAll("newSkip");
	    }
	    statementsNode.unhideAll("preCode");
	}
	return ret;
    }
    static private String getLastEnddoLabel(RegexNode node,String... nodes) {
	String label=null;
	if (node.hideAll( "cycleIfSkip", "(?m)(?i)(<Enddo>)([^<Enddo>]*)$",
			       "<Block>",nodes)) {
	    node.hideNodeGroup("lastEndDo","-",1,"cycleIfSkip");  // "preCode/newSkip/cyleIfSkip/lastEndDo" created
	    node.hideNodeGroup("newSkip","-",2,"cycleIfSkip");    // "preCode/newSkip/cyleIfSkip/newSkip" created
	    // make cycle to lastEndDo (there can only be one)
	    RegexNode lastEndDoNode = node.getFirstNode("lastEndDo");
	    RegexNode indentationNode = lastEndDoNode.getFirstNode("$","lastEndDo","indentation");
	    label="";
	    RegexNode labelNode = lastEndDoNode.getFirstNode("enddo","newlabel"); // old label
	    if (labelNode != null) {
		label=labelNode.getText();
	    } else { // new label?
		labelNode = lastEndDoNode.getFirstNode("$","*","newEnddo");
		if (labelNode != null) {
		    label=(String) labelNode.getAttribute("label");
		    if (label == null) label="";
		}
	    }
	    if (debug)System.out.format("Found <Enddo>, using Exit %s.\n",label);

	    if (debug & label.equals(""))System.out.format("%s %s Enddo without label.\n%s",
							   lastEndDoNode.getIdentification(),
							   lastEndDoNode.getNodeName(),
							   lastEndDoNode.toString());
	    node.unhideAll(-1,"newSkip","cycleIfSkip"); // remove newskip: "preCode/cyleIfSkip"
	    node.unhideAll("cycleIfSkip"); // remove cycleIfSkip: "preCode/lastEndDo"
	    node.unhideAll("lastEndDo"); // remove cycleIfSkip: "preCode"
	}
	return label;
    }
    static private boolean addExitToCode(RegexNode node,String label,RegexNode controlNode) {
	RegexNode newExitNode=new RegexNode("newExit:;",':',';');
	newExitNode.setAttribute("label",label);
	controlNode.appendSibling(newExitNode,"-");
	return true;
    }
 }
