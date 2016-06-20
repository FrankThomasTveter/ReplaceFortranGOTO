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

public class FortranAttributes {
    //
    static final int noChild=RegexNode.noSubLevels; 
    static final int allChild=RegexNode.allSubLevels; 
    static int maxcnt=0;
    //
    public FortranAttributes() {
    }
    static public void addAttributes(RegexNode regex,String... nodes) {
	addAttributes(regex, null,nodes);
    }
    static private void addAttributes(RegexNode regex, HashMap<String,String> globalTypeMap,String... nodes) {
	// assign location array attribute to all nodes in the node-tree
	
	//regex.debugOff();
	RegexNode subNode=regex.getNode(nodes);
	while (subNode != null) {
	    //System.out.format("Looping addAttributes subNode: %s\n",subNode.getIdentification());
	    subNode.setLocationAll();
	    
	    // assign "name-attribute" to all parents of "name"
	    RegexNode node=subNode.getNode("name");
	    while (node != null) {		
		String varName=node.getText();
		RegexNode parent=node.getParent();
		parent.setAttribute("name",varName);
		node=subNode.getNode("name");
	    }
	    // assign "Number of arguments" to all "arguments" nodes
	    node=subNode.getNode("arguments");
	    while (node != null) {		
		Integer nArguments=node.countChildren();
		RegexNode parent=node.getParent();
		parent.setAttribute("narguments",nArguments);
		node=subNode.getNode("arguments");
	    }
	    //System.out.format("Debug:\n===============\n%s\n===============\n",subNode.toString());
	    HashMap<String,String>  typeMap    = new HashMap<String,String>();
	    if (globalTypeMap != null) {
		for (Map.Entry<String, String> entry : globalTypeMap.entrySet()) {
		    String addressName = entry.getKey();
		    String addressType = entry.getValue();
		    typeMap.put(addressName,addressType);
		}
	    }
	    // process references, identify functions and arrays...
	    RegexNode mainNode=subNode.getNode("$","*","Main"); // loop over all "main" nodes...
	    while (mainNode != null) {
		//System.out.format("Looping addAttributes MainNode: %s\n",mainNode.getIdentification());
		if (subNode.getNodeName().equals("Function")) {
		    String name=mainNode.getText("$","Main","start","name");
		    typeMap.put(name,"function");
		}
		RegexNode declarationsNode=mainNode.getNode("$","Main","Declarations");
		// make hash of arrays and external function declarations...
		while(declarationsNode != null ) {
		    RegexNode declarationNode=declarationsNode.getNode("$","Declarations","declaration");
		    while ( declarationNode!=null)  {
			String type=declarationNode.getText("type");
			RegexNode referenceNode=declarationNode.getNode("$","declaration","references","reference");
			while (referenceNode != null) {
			    String name=canonical(referenceNode.getText("$","reference","name"));
			    name=canonical(name);
			    // find number of arguments...
			    Integer nArguments=0;
			    RegexNode argumentsNode=referenceNode.getFirstNode("$","reference","arguments");
			    if (argumentsNode != null) {
				nArguments=argumentsNode.countChildren();
			    }
			    if (nArguments == 0) {
				typeMap.put(name,"variable");
				referenceNode.setAttribute("type",new String("scalar"));
			    } else {
				typeMap.put(name,"array");
				referenceNode.setAttribute("type",new String("array"));
			    }
			    referenceNode=declarationNode.getNode("$","declaration","references","reference");
			}
			declarationNode=declarationsNode.getNode("$","Declarations","declaration");
		    }
		    RegexNode externalNode=declarationsNode.getNode("$","Declarations","external");
		    while ( externalNode!=null)  {
			RegexNode referenceNode=externalNode.getNode("$","external","arguments","argument","reference");
			while (referenceNode != null) {
			    String name=canonical(referenceNode.getText("$","reference","name"));
			    name=canonical(name);
			    // store function name...
			    typeMap.put(name,"function");
			    referenceNode.setAttribute("type",new String("function"));
			    referenceNode=externalNode.getNode("$","external","arguments","argument","reference");
			}
			externalNode=declarationsNode.getNode("$","Declarations","external");
		    }
		    RegexNode dimensionNode=declarationsNode.getNode("$","Declarations","dimension");
		    while ( dimensionNode!=null)  {
			RegexNode referenceNode=dimensionNode.getNode("$","dimension","arguments","argument","reference");
			while (referenceNode != null) {
			    String name=canonical(referenceNode.getText("$","reference","name"));
			    name=canonical(name);
			    // store reference name...
			    typeMap.put(name,"array");
			    referenceNode.setAttribute("type",new String("array"));
			    referenceNode=dimensionNode.getNode("$","dimension","arguments","argument","reference");
			}
			dimensionNode=declarationsNode.getNode("$","Declarations","dimension");
		    }
		    declarationsNode=mainNode.getNode("$","Main","Declarations");
		}
		RegexNode statementsNode=mainNode.getNode("$","Main","Statements");
		while(statementsNode != null ) {
		    // loop over references, 
		    
		    RegexNode referenceNode=statementsNode.getNode("reference");
		    while (referenceNode != null ) {
			String name=canonical(referenceNode.getText("$","reference","name"));
			name=canonical(name);
			String type=typeMap.get(name);
			if (type != null ) {
			    if (type.equals("function")) {
				referenceNode.setAttribute("type",new String("function"));
			    } else if (type.equals("array")) {
				referenceNode.setAttribute("type",new String("array"));
			    }
			} else { 
			    // check if reference has dimensions => check if it is external or declared array or, otherwise its a "functionCall"
			    // if it has no dimensions , check if it is external, then it is a function... change nodeName to "functionCall"
			    // find number of arguments...
			    Integer nArguments=0;
			    RegexNode argumentsNode=referenceNode.getFirstNode("$","reference","arguments");
			    if (argumentsNode != null) {
				nArguments=argumentsNode.countChildren();
			    }
			    if (nArguments > 0) { // this is a function (else it is an implicit reference)
				referenceNode.setAttribute("type",new String("function"));
			    } else {
				referenceNode.setAttribute("type",new String("implicit"));
			    }
			}
			referenceNode=statementsNode.getNode("reference");
		    }
		    statementsNode=mainNode.getNode("$","Main","Statements");
		}
		mainNode=subNode.getNode("$","*","Main");
	    }
	    
	    String s="";
	    for (String ss : nodes) {
		s=s+":"+ss;
	    }
	    
	    RegexNode subroutineNode=subNode.getNode("$","*","Contains","Subroutine");
	    while (subroutineNode != null) {
		//System.out.format("   Loop addAttributes subroutineNode: %s\n",subroutineNode.getIdentification());
		addAttributes(subroutineNode, typeMap,"$","Subroutine");
		//System.out.format("   Done addAttributes subroutineNode: %s\n",subroutineNode.getIdentification());
		subroutineNode=subNode.getNode("$","*","Contains","Subroutine");
	    }
	    RegexNode functionNode=subNode.getNode("$","*","Contains","Function");
	    while (functionNode != null) {
		//System.out.format("Looping addAttributes functionNode: %s\n",functionNode.getIdentification());
		addAttributes(functionNode, typeMap,"$","Function");
		functionNode=subNode.getNode("$","*","Contains","Function");
	    }
	    subNode=regex.getNode(nodes);
	}
	// process contains subroutines (may use global variables)...
    }
    public static String canonical (String name) {
	if (name != null) {
	    return name.toLowerCase();
	} else {
	    return name;
	    }
    }
}
