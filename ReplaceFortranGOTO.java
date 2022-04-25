import java.util.HashSet;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
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

//
// The ReplaceFortranGOTO replaces goto-statements in FORTRAN code
// using IF-blocks and DO-loops.
//
// requires:: RegexNode.java
// main:: ReplaceFortranGOTO.java
// sources:: FortranAnalysis.java FortranProcessor.java FortranAttributes.java Scope.java 
//
// usage:: java ReplaceFortranGOTO file1.f file2.f ...
//
// output:: xfile1.f xfile2.f ...
//
// No guarantees provided.

public class ReplaceFortranGOTO {
    static HashSet<String>           files             = new HashSet<String>();
    static HashSet<RegexNode>     nodeTreeSet = new HashSet<RegexNode>();
    static final boolean debug=true;
    static String outputPath="";
    // usage:: ReplaceFortranGOTO file1.f file2.f ...

    public static void main(String[] args){
	ReplaceFortranGOTO f=new ReplaceFortranGOTO(args);
    }

    public ReplaceFortranGOTO(String[] args) {
	// all source code
	// process arguments and make list of input files
	int type=0;               // argument is unspecified
        for (String arg: args) {
	    if (type==0) {
		if (arg.equals("-p")) {
		    type=1;       // next argument will be the output path...
		} else if (arg.equals("-i")) {
		} else {
		    files.add(arg); // argument is a file name
		};
	    } else if (type==1) { // argument is the output path
		outputPath=arg;
		type=0;
	    }
        }

	// make Fortran nodeTree list
        for (String file: files) {
	    nodeTreeSet.add(FortranAnalysis.readFile(file));
	} // for file loop

	// make Global scope
	Scope globalScope=new Scope();

	// loop over fortranStructures, and create data-structure
	for (RegexNode nodeTree: nodeTreeSet) {
	    String file=nodeTree.getNodeName();

	    String dumpfile=file+".start.tree";
	    if (debug)nodeTree.dumpToFile(dumpfile);

	    // remove Goto statements etc.
	    FortranProcessor.standardise(nodeTree);

	    // add attributes to nodeTree
	    FortranAttributes.addAttributes(nodeTree);

	    dumpfile=file+".end.tree";
	    if (debug) nodeTree.dumpToFile(dumpfile);

	    //System.out.format("ReplaceFortranGOTO Linking.\n");
	    // link new procedures together
	    //globalScope.link(nodeTree);

	    // Print out call tree...
	    
	    //System.out.format("ReplaceFortranGOTO printing call structure.\n");
	    //for (Scope.Program program : globalScope.programs) {
	    //		System.out.format("\nProgram %s:\n%s\n",program.getName(),program.toString(1));
	    //}

	    nodeTree.unignoreAll();
	    nodeTree.unhideAll();

	    nodeTree.writeToFile( file + ".output" );

	    System.out.format("ReplaceFortranGOTO printing done.\n");
	}

    }
}
