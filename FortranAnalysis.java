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

public class FortranAnalysis {
    //
    static final int noChild=RegexNode.noSubLevels; 
    static final int allChild=RegexNode.allSubLevels; 
    //
    static boolean debug = false;
    static boolean first = true;
    static Long tstart = time();
    //
    //
    public FortranAnalysis(String file) {
	readFile(file);
    }
    static RegexNode readFile(String file) {
	RegexNode regex=null;
	if (first) {
	    define();        // define anchors and basic replacement strings
	    first=false;
	}
	try{
	    regex=new RegexNode(readFileToString(file));
	    regex.setNodeName(file);        // identify this node as a "file"

	    if (debug) System.out.format("FortranAnalysis Processing file %s at %s\n",file,time(tstart));
	    if (debug) System.out.format("%s FortranAnalysis  Hiding basics .\n",time(tstart));

	    hideBasics(regex,"$","*");
	    
	    //regex.dumpToFile("analyseA.tree");

	    if (debug) System.out.format("%s FortranAnalysis  Hiding structure .\n",time(tstart));
	    hideStructure(regex,"$","*");
	    
	    //regex.dumpToFile("analyseB.tree");

	    if (debug) System.out.format("%s FortranAnalysis  Hiding commands .\n",time(tstart));
	    hideCommands(regex,"$","*");

	    if (debug) System.out.format("%s FortranAnalysis  Hiding If/Do-blocks .\n",time(tstart));
	    hideBlocks(regex,"$","*");

	    if (debug) System.out.format("%s FortranAnalysis  Hiding If-commands .\n",time(tstart));
	    hideCommands(regex,"$","...","ifCommand");

	    regex.unignoreAll();    // everything processed so far stays hidden
	    regex.setLabelAll("-");
	    
	    //regex.dumpToFile("analyseC.tree");

	    if (debug) System.out.format("%s FortranAnalysis  Hiding primitives .\n",time(tstart));
	    hidePrimitives(regex,"$","*");
	    
	    //regex.dumpToFile("after.tree");
	    
	    //System.out.format("Beginning:\n===============\n%s\n===============\n",regex.toString());
	    
	    if (debug) System.out.format("FortranAnalysis Adding attributes at %s.\n",time(tstart));
	    
	    //addAttributes(regex,"Program");
	    //addAttributes(regex,"Subroutine");
	    //addAttributes(regex,"Function");
	    
	    //regex.dumpToFile("analyseC.tree");
	    
	    //regex.unignoreAll();
	    //regex.unhideAll();
	    
	    //System.out.format("Final:\n===============\n%s\n===============\n",regex.getText());
	    
	    if (debug) System.out.format("FortranAnalysis Done at %s.\n",time(tstart));

	} catch (IOException io) {
	    System.out.format("File error: %s, %s\n",file,io);
	}
	return regex;
    }
    static private String readFileToString( String file ) throws IOException {
	BufferedReader reader = new BufferedReader( new FileReader (file));
	String         line = null;
	StringBuilder  stringBuilder = new StringBuilder();
	String         ls = System.getProperty("line.separator");
	
	while( ( line = reader.readLine() ) != null ) {
	    stringBuilder.append( line );
	    stringBuilder.append( ls );
	}
	
	return stringBuilder.toString();
    }    

    static Long time() {
	return (System.currentTimeMillis()/1000);
    }
    static String time(Long tstart) {
	return String.format("%d",(System.currentTimeMillis()/1000)-tstart);
    }
    static void define() {
	// define anchors
	RegexNode.define("<name>",     "[a-zA-Z][a-zA-Z0-9_]*");
	RegexNode.define("<attribute>", "[a-zA-Z0-9_\\*]+");
	RegexNode.define("<Attribute>");
	RegexNode.define("<expression>", "[a-zA-Z0-9_\\*\\/\\+\\-<Brackets>\\ \\.<Op>]*");
	RegexNode.define("<Expression>");
	RegexNode.define("<atom>",     "[^ \\t,\\n\\(\\)=+-/\\*%!\\|&<Op>]");
	RegexNode.define("<noArgument>","[^ \\t,\\n\\(\\)<Argument>]");
	RegexNode.define("<noPointer>","[^ \\t,\\n\\(\\)=+-/\\*%!\\|&<Op><Pointer><Name>]");
	RegexNode.define("< >",        "[ \\t]*");
	RegexNode.define("<d>",        "[\\d]*");
	RegexNode.define("<#>",        "[^\\n]*");
	RegexNode.define("<#/>",       "[^\\n\\/]*");
	RegexNode.define("<String>");
	RegexNode.define("<Brackets>");
	RegexNode.define("<Content>");
	RegexNode.define("<Rule>");
	RegexNode.define("<Fold>");

	RegexNode.define("<If>");
	RegexNode.define("<Elseif>");
	RegexNode.define("<Else>");
	RegexNode.define("<Endif>");
	RegexNode.define("<Do>");
	RegexNode.define("<Enddo>");
	RegexNode.define("<Goto>");

	RegexNode.define("<Block>");
	RegexNode.define("<Program>");
	RegexNode.define("<Subroutine>");
	RegexNode.define("<Entry>");
	RegexNode.define("<Function>");
	RegexNode.define("<Call>");
	RegexNode.define("<Contains>");
	RegexNode.define("<End>");
	RegexNode.define("<Return>");
	RegexNode.define("<CR>");
	RegexNode.define("<Processed>");
	RegexNode.define("<Declaration>");
	RegexNode.define("<Statements>");
	RegexNode.define("<Continue>");
	RegexNode.define("<Format>");
	RegexNode.define("<Name>");
	RegexNode.define("<Argument>");
	RegexNode.define("<Pointer>");
	RegexNode.define("<Op>");
	RegexNode.define("<Operator>");
	RegexNode.define("<Indentation>");
	RegexNode.define("<Anchor>");
	RegexNode.define("<Command>");
	RegexNode.define("<Garbage>");

	//RegexNode.define("<Control>");
	//RegexNode.define("<Label>");
	//RegexNode.define("<Arguments>");
	//RegexNode.define("<Command>");

    }
    static private void hideBasics(RegexNode regex, String... node) { // strings and comments
	regex.hideAll( "string",       "([\\\"\\\'])[^\\\"\\\'\\n]*\\1",    "<String>", node); // hide strings
	regex.hideAll( "comment",      "(?m)^[cC]<#>\n",                    "\n", node);  // hide line comments
	regex.hideAll( "comment",      "(?m)^< >!.*\n",                     "\n", node);  // hide comments at end of line
	regex.hideAll( "comment",      "(?m)!.*\n",                         "\n", node);  // hide comments at end of line
	//regex.dumpToFile("analysis1.tree");
	regex.hideAll( "continuation", "(?m)\\n[ ]{5}[^\\d ]",             " ", node);  // hide comments at end of line
	regex.hideAll( "continuation", "(?m)\\&[ \t]*\\n[ \t]*\\&",         " ", node);  // hide comments at end of line
	regex.hideAll( "semicolon",    ";",                                 "\n",node); 

	regex.ignoreAll("string");
	regex.ignoreAll("comment");

	 // hide brackets and contents
	// regex.ignoreAll(node,"*");  // everything so far stays hidden
	while(regex.hideAll( "_Brackets", "(?m)(?i)\\(([^\\(\\)<Content>]*)\\)", "<Brackets>",node)) {
	    regex.hideNodeGroup("content", "<Content>", 1, node,"_Brackets");
	    regex.setNodeNameAll("Brackets",node,"_Brackets");
	};
    }


    static private void hideStructure(RegexNode regex, String... node) {

	// hide procedural definition statements
	regex.hideAll( "blank",             "(?m)(?i)^< >\\n","\n",                               node);    // hide block data statements
	regex.hideAll( "blockStart",        "(?m)(?i)^(< >)block< >data<#>\\n","\n",              node);    // hide block data statements
	regex.hideAll( "programStart",      "(?m)(?i)^(< >)program< >(\\w*)<#>\\n","\n",          node);    // hide program statements
	regex.hideAll( "subroutineStart",   "(?m)(?i)^(< >)subroutine< >(\\w+)< >(<Brackets>)< >\\n","\n",node);// hide subroutine statements
	regex.hideAll( "subroutineStart",   "(?m)(?i)^(< >)recursive< >subroutine< >(\\w+)(<Brackets>)< >\\n","\n",node);// hide subroutine statements
	regex.hideAll( "entry",        "(?m)(?i)^(< >)entry< >(\\w+)(<Brackets>)< >\\n","\n",     node);// hide entry statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(integer)(<#>::)< >function< >(<name>)< >(<Brackets>)< >\\n",     "\n",node);  // hide integer statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(integer)(\\*\\d)< >function< >(<name>)< >(<Brackets>)< >\\n",    "\n",node);  // hide integer statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(integer)()< >function< >(<name>)< >(<Brackets>)< >\\n",          "\n",node);  // hide integer statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(real)(<#>::)< >function< >(<name>)< >(<Brackets>)< >\\n",        "\n",node);  // hide real statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(real)(\\*\\d)< >function< >(<name>)< >(<Brackets>)< >\\n",       "\n",node);  // hide real statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(real)()< >function< >(<name>)< >(<Brackets>)< >\\n",             "\n",node);  // hide real statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(double)< >(precision)< >function< >(<name>)< >(<Brackets>)< >\\n",             "\n",node);  // hide real statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(double)()< >function< >(<name>)< >(<Brackets>)< >\\n",             "\n",node);  // hide real statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(logical)(\\*\\d)< >function< >(<name>)< >(<Brackets>)< >\\n",    "\n",node);  // hide logical statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(logical)()< >function< >(<name>)< >(<Brackets>)< >\\n",          "\n",node);  // hide logical statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(character)(\\*\\d+)< >function< >(<name>)< >(<Brackets>)< >\\n", "\n",node);  // hide character statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)(character)()< >function< >(<name>)< >(<Brackets>)< >\\n",        "\n",node);  // hide character statements
	regex.hideAll( "functionStart",     "(?m)(?i)^(< >)()()< >function< >(<name>)< >(<Brackets>)< >\\n",     "\n",node);  // hide integer statements
	regex.hideAll( "contains",     "(?m)(?i)^(< >)contains<#>\\n","\n",                       node);    // hide contains statements
	regex.hideAll( "end",          "(?m)(?i)^(< ><d>< >)end< >program< >(<#>)\\n","\n",       node);    // hide end statements
	regex.hideAll( "end",          "(?m)(?i)^(< ><d>< >)end< >subroutine< >(<#>)\\n","\n",    node);    // hide end statements
	regex.hideAll( "end",          "(?m)(?i)^(< ><d>< >)end< >function< >(<#>)\\n","\n",      node);    // hide end statements
	regex.hideAll( "end",          "(?m)(?i)^(< ><d>< >)end< >block< >data< >(<#>)\\n","\n",  node);    // hide end statements
	regex.hideAll( "end",          "(?m)(?i)^(< ><d>< >)end< >()\\n","\n",                    node);    // hide end statements
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"blockStart");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"programStart");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"subroutineStart");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"entry");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"functionStart");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"contains");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"end");
	regex.hideNodeGroup("name",       "<Name>",       2, node,"programStart");
	regex.hideNodeGroup("name",       "<Name>",       2, node,"subroutineStart");
	regex.hideNodeGroup("name",       "<Name>",       2, node,"entry");
	regex.hideNodeGroup("(arguments)","<Anchor>",  3, node,"subroutineStart");
	regex.hideNodeGroup("(arguments)","<Anchor>",  3, node,"entry");
	regex.hideNodeGroup("type",       "<Anchor>",     2, node,"functionStart");
	regex.hideNodeGroup("attributes", "<Anchor>",     3, node,"functionStart");
	regex.hideNodeGroup("name",       "<Name>",       4, node,"functionStart");
	regex.hideNodeGroup("(arguments)","<Anchor>",  5, node,"functionStart");
	regex.hideNodeGroup("name",       "<Name>",       2, node,"end");

	// hide declarations
	regex.hideAll( "implicit",    "(?m)(?i)^(< >)implicit< >(<#>)\\n",             "\n",node);  // hide implicit statements
	regex.hideAll( "external",    "(?m)(?i)^(< >)external(<#>)\\n",                "\n",node);  // hide external statements
	regex.hideAll( "equivalence", "(?m)(?i)^(< >)equivalence(<#>)\\n",             "\n",node);  // hide external statements
	regex.hideAll( "dimension",   "(?m)(?i)^(< >)dimension(<#>)\\n",               "\n",node);  // hide external statements
	regex.hideAll( "parameter",   "(?m)(?i)^(< >)parameter< >(<Brackets>)< >\\n",  "\n",node);  // hide character statements
	regex.hideAll( "format",      "(?m)(?i)^(< ><d>< >)format< >(<Brackets>)< >\\n",     "\n",node);  // hide character statements
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"implicit");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"external");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"equivalence");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"dimension");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"parameter");
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"format");
	regex.hideNodeGroup("list",   "<Anchor>", 2, node,"implicit");
	regex.hideNodeGroup("list",   "<Anchor>", 2, node,"external");
	regex.hideNodeGroup("list",   "<Anchor>", 2, node,"equivalence");
	regex.hideNodeGroup("list",   "<Anchor>", 2, node,"dimension");
	regex.hideNodeGroup("list",   "<Anchor>", 2, node,"parameter");
	regex.hideNodeGroup("expression", "<Anchor>", 2, node,"format");

	regex.hideAll("item",   "(?m)(?i)(none)()",                                   "<Argument>",  node,"implicit","list");
	regex.hideAll("item",   "(?m)(?i)(double< >precision)< >(<Brackets>)",        "<Argument>",  node,"implicit","list");
	regex.hideAll("item",   "(?m)(?i)(real[^<Brackets>\n]*)< >(<Brackets>)",      "<Argument>",  node,"implicit","list");
	regex.hideAll("item",   "(?m)(?i)(integer[^<Brackets>\n]*)< >(<Brackets>)",   "<Argument>",  node,"implicit","list");
	regex.hideAll("item",   "(?m)(?i)(logical[^<Brackets>\n]*)< >(<Brackets>)",   "<Argument>",  node,"implicit","arguments");
	regex.hideAll("item",   "(?m)(?i)(character[^<Brackets>\n]*)< >(<Brackets>)", "<Argument>",  node,"implicit","arguments");
	regex.hideNodeGroup("type",  "<Argument>", 1, node,"implicit","list","item");
	regex.hideNodeGroup("(arguments)",  "<Anchor>", -2, node,"implicit","list","item");
	
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(integer)(<#>::)< >(<#>)\\n",      "\n",node);  // hide integer statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(integer)(\\*\\d)< >(<#>)\\n",     "\n",node);  // hide integer statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(integer)()< >(<#>)\\n",           "\n",node);  // hide integer statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(real)(<#>::)< >(<#>)\\n",         "\n",node);  // hide real statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(real)(\\*\\d)< >(<#>)\\n",        "\n",node);  // hide real statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(real)()< >(<#>)\\n",              "\n",node);  // hide real statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(double< >precision)()< >(<#>)\\n","\n",node);  // hide real statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(double)()< >(<#>)\\n",            "\n",node);  // hide real statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(logical)(\\*\\d)< >([^\\n]*)\\n", "\n",node);  // hide logical statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(logical)()< >([^\\n]*)\\n",       "\n",node);  // hide logical statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(character)(\\*\\d+)([^\\n]*)\\n", "\n",node);  // hide character statements
	regex.hideAll( "declaration", "(?m)(?i)^(< >)(character)()([^\\n]*)\\n",        "\n",node);  // hide character statements
	regex.hideNodeGroup("indentation", "<Anchor>", 1, node,"declaration");
	regex.hideNodeGroup("type",       "<Anchor>",    2, node,"declaration");
	regex.hideNodeGroup("attributes", "<Anchor>",    -3, node,"declaration");
	regex.hideNodeGroup("references",  "<Anchor>", 4, node,"declaration");

	regex.hideAll( "common",   "(?m)(?i)^(< >)common< >(\\/)([^\\/\\n]+)(\\/)(<#>)\\n",                    "\n",node);  // hide character statements
	regex.hideAll( "common",   "(?m)(?i)^(< >)common< >()()()(<#>)\\n",                    "\n",node);  // hide character statements
	regex.hideNodeGroup("indentation","<Anchor>",  1,     node,"common");
	regex.hideNodeGroup("delimiter",  "<Anchor>",        2,    node,"common");
	regex.hideNodeGroup("name",       "<Name>",           3,   node,"common");
	regex.hideNodeGroup("delimiter",  "<Anchor>",          4,  node,"common");
	regex.hideNodeGroup("references",  "<Anchor>",        5, node,"common");

	regex.hideAll( "data",     "(?m)(?i)^(< >)data< >(<#>)< >\\n", "\n",node);  // hide data statements
	regex.hideNodeGroup("indentation", "<Anchor>", 1,   node,"data");
	regex.hideNodeGroup("list",  "<Anchor>",     2,  node,"data");

	while (regex.hideAll( "item",     "(?m)(?i)< >([a-zA-Z][^ \\t\\n\\/]*)< >(\\/)< >(<#/>)< >(\\/*)", "<Declaration>",node,"data","list")) {  // hide data statements
	    regex.hideNodeGroup("references",  "<Pointer>",           1,   node,"data","list","item");
	    regex.hideNodeGroup("delimiter",  "<Anchor>",              2,  node,"data","list","item");
	    regex.hideNodeGroup("expressions","<Anchor>",           3, node,"data","list","item");
	    regex.hideNodeGroup("delimiter",  "<Anchor>",                4,node,"data","list","item");
	};

	regex.unhideAll("data", "list", "item");

	// hide if statements
	regex.hideAll( "if",          "(?m)(?i)^(< ><d>< >)if< >(<Brackets>)< >then<#>\\n", "\n",           node);      // hide if statements
	regex.hideAll( "elseif",      "(?m)(?i)^(< ><d>< >)else< >if< >(<Brackets>)< >then<#>\\n", "\n",node);  // hide else if statements
	regex.hideAll( "endif",       "(?m)(?i)^(< ><d>< >)end< >if<#>\\n",        "\n",                 node);  // hide end if statements
	regex.hideAll( "ifControl",     "(?m)(?i)^(< ><d>< >)if< >(<Brackets>)< >(<d>)< >,< >(<d>)< >,< >(<d>)< >\\n", "\n",           node);      // hide if statements
	regex.hideNodeGroup("indentation", "<Indentation>", 1, node,"if");
	regex.hideNodeGroup("indentation", "<Indentation>", 1, node,"elseif");
	regex.hideNodeGroup("indentation", "<Indentation>", 1, node,"endif");
	regex.hideNodeGroup("indentation", "<Indentation>", 1, node,"ifControl");
	regex.hideNodeGroup("(expression)", "<Expression>", 2, node,"if");
	regex.hideNodeGroup("(expression)", "<Expression>", 2, node,"elseif");
	regex.hideNodeGroup("(expression)", "<Expression>", 2, node,"ifControl");
	regex.hideNodeGroup("control", "<Anchor>", 3, node,"ifControl");
	regex.hideNodeGroup("control", "<Anchor>", 4, node,"ifControl");
	regex.hideNodeGroup("control", "<Anchor>", 5, node,"ifControl");

	regex.hideAll( "else",        "(?m)(?i)^(< ><d>< >)else<#>\\n",                "\n",              node);   // hide else statements
	regex.hideNodeGroup("indentation", "<Indentation>", 1, node,"else");

	regex.hideAll( "ifExec",      "(?m)(?i)^(< ><d>< >)if< >(<Brackets>)< >(<#>< >\\n)", "\n",           node);      // hide if statements
	regex.hideNodeGroup("indentation", "<Indentation>", 1, node,"ifExec");
	regex.hideNodeGroup("(expression)", "<Expression>", 2, node,"ifExec");
	regex.hideNodeGroup("ifCommand", "<Command>", 3, node,"ifExec");



	// hide do statements
	regex.hideAll( "do",           "(?m)(?i)^(< ><d>< >)()< >do< >\\n",   "\n",node);    // hide do statements
	regex.hideAll( "do",           "(?m)(?i)^(< ><d>< >)(\\w+):< >do< >\\n",   "\n",node);    // hide do statements
	regex.hideAll( "dofor",        "(?m)(?i)^(< ><d>< >)()< >do< >(<d>)< >([^\\n=, ]+)< >=(<#>)\\n",   "\n",node);    // hide do statements
	regex.hideAll( "dofor",        "(?m)(?i)^(< ><d>< >)(\\w+):< >do< >(<d>)< >([^\\n=, ]+)< >=(<#>)\\n",   "\n",node);    // hide do statements
	regex.hideAll( "dowhile",      "(?m)(?i)^(< ><d>< >)()< >do< >(<d>)< >while< >(<Brackets>)< >\\n",         "\n",node);    // hide do statements
	regex.hideAll( "dowhile",      "(?m)(?i)^(< ><d>< >)(\\w+):< >do< >(<d>)< >while< >(<Brackets>)< >\\n",         "\n",node);    // hide do statements
	regex.hideAll( "enddo",        "(?m)(?i)^(< ><d>< >)end< >do<#>\\n",                          "\n",node); // hide end do statements
	regex.hideAll( "continue",     "(?m)(?i)^(< ><d>< >)continue<#>\\n",                          "\n",node);      // hide continue statements
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"do");
	regex.hideNodeGroup("newLabel",    "<Anchor>",   2,    node,"do");
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"dofor");
	regex.hideNodeGroup("newLabel",    "<Anchor>",    2,   node,"dofor");
	regex.hideNodeGroup("control",     "<Anchor>",     3,  node,"dofor");
	regex.hideNodeGroup("pointer",     "<Anchor>",      4, node,"dofor");
	regex.hideNodeGroup("arguments",   "<Anchor>",       5,node,"dofor");
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"dowhile");
	regex.hideNodeGroup("newLabel",    "<Anchor>",    2,   node,"dowhile");
	regex.hideNodeGroup("control",     "<Anchor>",     3,  node,"dowhile");
	regex.hideNodeGroup("(expression)","<Anchor>",      4, node,"dowhile");
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"enddo");
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"continue");

	if (debug) System.out.format("%s HideStructure Done .\n",time(tstart));
    }
    static private void hideCommands(RegexNode regex, String... node) {
	regex.unignoreAll();
	regex.ignoreAll("string");
	regex.ignoreAll("comment");

	regex.hideAll( "_call",         "(?m)(?i)^(< ><d>< >)call< >(\\w+)< >([<Brackets>]?)< >[\\n<CR>]",      "\n",node); // hide call statements
	regex.hideNodeGroup("indentation", "<Anchor>", 1,     node,"_call");
	regex.hideNodeGroup("name",        "<Name>",    2,    node,"_call");
	regex.hideNodeGroup("(arguments)", "<Anchor>",   3,   node,"_call");	
	regex.setNodeNameAll("call",node,"_call");

	regex.hideAll( "_assignment",   "(?m)(?i)^(< ><d>< >)(\\w[^\\n<CR>=<Brackets>]*[<Brackets>]?)< >=< >([^\\n<CR>,]*)< >[\\n<CR>]", "\n",node); // can not contain comma (this seperates them from "do"-lines)
	regex.hideNodeGroup("indentation", "<Anchor>",1,     node,"_assignment"); // 
	regex.hideNodeGroup("pointer",     "<Anchor>", 2,    node,"_assignment");
	regex.hideNodeGroup("expression",  "<Anchor>",  3,   node,"_assignment");
	regex.setNodeNameAll("assignment", node,"_assignment");

	regex.hideAll( "open",         "(?m)(?i)^(< ><d>< >)open< >(<Brackets>)< ><#>[\\n<CR>]",         "\n",node);   // hide open statements
	regex.hideAll( "read",         "(?m)(?i)^(< ><d>< >)read< >(<Brackets>)< >(<#>)[\\n<CR>]",       "\n",node);   // hide read statements
	regex.hideAll( "write",        "(?m)(?i)^(< ><d>< >)write< >(<Brackets>)< >(<#>)[\\n<CR>]",      "\n",node);   // hide write statements
	regex.hideAll( "close",        "(?m)(?i)^(< ><d>< >)close< >(<Brackets>)< ><#>[\\n<CR>]",        "\n",node);   // hide close statements
	regex.hideAll( "return",       "(?m)(?i)^(< ><d>< >)return<#>[\\n<CR>]",                         "\n",node);  // hide return statements
	regex.hideNodeGroup("indentation",  "<Anchor>",1,    node,"open");
	regex.hideNodeGroup("(arguments)",  "<Anchor>", 2,   node,"open");
	regex.hideNodeGroup("indentation",  "<Anchor>",1,    node,"read");
	regex.hideNodeGroup("(arguments)",  "<Anchor>", 2,   node,"read");
	regex.hideNodeGroup("expressions",  "<Anchor>",  3,  node,"read");
	regex.hideNodeGroup("indentation",  "<Anchor>",1,    node,"write");
	regex.hideNodeGroup("(arguments)",  "<Anchor>", 2,   node,"write");
	regex.hideNodeGroup("expressions",  "<Anchor>",  3,  node,"write");
	regex.hideNodeGroup("indentation",  "<Anchor>",1,    node,"close");
	regex.hideNodeGroup("(arguments)",  "<Anchor>", 2,   node,"close");
	regex.hideNodeGroup("indentation",  "<Anchor>",1,    node,"return");

	regex.hideAll( "goto",         "(?m)(?i)^(< ><d>< >)go< >to< >(<#>)< >[\\n<CR>]","\n",node);              // hide goto statements
	regex.hideAll( "cycle",        "(?m)(?i)^(< ><d>< >)cycle< >()[\\n<CR>]","\n",node);              // hide goto statements
	regex.hideAll( "cycle",        "(?m)(?i)^(< ><d>< >)cycle< >(<#>)< >[\\n<CR>]","\n",node);              // hide goto statements
	regex.hideAll( "exit",         "(?m)(?i)^(< ><d>< >)exit< >()[\\n<CR>]","\n",node);              // hide goto statements
	regex.hideAll( "exit",         "(?m)(?i)^(< ><d>< >)exit< >(<#>)< >[\\n<CR>]","\n",node);              // hide goto statements
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"goto");
	regex.hideNodeGroup("control",     "<Anchor>",    2,   node,"goto");
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"cycle");
	regex.hideNodeGroup("newlabel",    "<Anchor>",    2,   node,"cycle");
	regex.hideNodeGroup("indentation", "<Anchor>",   1,    node,"exit");
	regex.hideNodeGroup("newlabel",    "<Anchor>",    2,   node,"exit");

	//regex.dumpToFile("jnkC.tree");

	// io statements
	regex.hideAll( "gotoIO",        "(?m)(?i)err< >=< >(<#>)","<Anchor>" ,  node, "open",  "(arguments)","..." );    // hide end statements in IO statements
	regex.hideAll( "gotoIO",        "(?m)(?i)err< >=< >(<#>)","<Anchor>" ,  node, "read",  "(arguments)", "...");    // hide end statements in IO statements
	regex.hideAll( "gotoIO",        "(?m)(?i)end< >=< >(<#>)","<Anchor>" ,  node, "read",  "(arguments)", "...");    // hide end statements in IO statements
	regex.hideAll( "gotoIO",        "(?m)(?i)err< >=< >(<#>)","<Anchor>" ,  node, "write", "(arguments)", "...");    // hide end statements in IO statements
	regex.hideAll( "gotoIO",        "(?m)(?i)end< >=< >(<#>)","<Anchor>" ,  node, "write", "(arguments)", "...");    // hide end statements in IO statements
	regex.hideNodeGroup("control",   "<Anchor>",  1, node, "*", "(arguments)", "...","gotoIO");

	// make sure we do not hide top-labels...
	regex.hideAll( "linefeed",       "(?m)(?i)\\n","<CR>",node,"...","*");
    }
    static private void hideBlocks(RegexNode regex, String... node) {
	// clean start
	regex.unignoreAll(node,"*");    // everything processed so far stays hidden
	regex.setLabelAll("-",node,"*");

	// hide procedural blocks
	regex.ignoreAll(node,"*");    // everything processed so far stays hidden
	//regex.dumpToFile("jnkA.tree");

	regex.unignoreAll(node,"blockStart");
	regex.unignoreAll(node,"programStart");
	regex.unignoreAll(node,"subroutineStart");
	regex.unignoreAll(node,"functionStart");
	regex.unignoreAll(node,"contains");
	regex.unignoreAll(node,"end");

	regex.setLabelAll("<Block>",     "blockStart");
	regex.setLabelAll("<Program>",   "programStart");
	regex.setLabelAll("<Subroutine>","subroutineStart");
	regex.setLabelAll("<Function>",  "functionStart");
	regex.setLabelAll("<Contains>",  "contains");
	regex.setLabelAll("<End>",       "end");

	regex.hideTheRestAll("rest","<Garbage>",node);    // hide the rest

	//regex.dumpToFile("jnkB.tree");

	while(regex.hideAll( "Block",     "(?m)(?i)<Block>([^<Program><Subroutine><Function><End>]*)<End>","<Processed>",node)||
	      regex.hideAll( "Program",   "(?m)(?i)<Program>([^<Program><Subroutine><Function><End><Contains>]*)()<End>","<Processed>",node)||
	      regex.hideAll( "Program",   "(?m)(?i)<Program>([^<Program><Subroutine><Function><End><Contains>]*)(<Contains>[<CR><Processed>]*)<End>","<Processed>",node)||
	      regex.hideAll( "Subroutine","(?m)(?i)<Subroutine>([^<Program><Subroutine><Function><End><Contains>]*)()<End>","<Processed>",node)||
	      regex.hideAll( "Subroutine","(?m)(?i)<Subroutine>([^<Program><Subroutine><Function><End><Contains>]*)(<Contains>[<CR><Processed>]*)<End>","<Processed>",node)||
	      regex.hideAll( "Function",  "(?m)(?i)<Function>([^<Program><Subroutine><Function><End><Contains>]*)()<End>","<Processed>",node) ||
	      regex.hideAll( "Function",  "(?m)(?i)<Function>([^<Program><aSubroutine><Function><End><Contains>]*)(<Contains>[<CR><Processed>]*)<End>","<Processed>",node)
	      ) {
	    if (debug) System.out.format("%s HideStructure inside iteration .\n",time(tstart));
	};          // hide procedural blocks 

	regex.hideNodeGroup("Main",             "<Anchor>",       1, node,"Program");
	regex.hideNodeGroup("Contains",         "<Anchor>",       2, node,"Program");
	regex.hideNodeGroup("Main",             "<Anchor>",       1, node,"...","Function");
	regex.hideNodeGroup("Contains",         "<Anchor>",       2, node,"...","Function");
	regex.hideNodeGroup("Main",             "<Anchor>",       1, node,"...","Subroutine");
	regex.hideNodeGroup("Contains",         "<Anchor>",       2, node,"...","Subroutine");

	if (debug) System.out.format("%s HideStructure Writing to file .\n",time(tstart));
	//regex.dumpToFile("jnkC.tree");

	regex.unhideAll("rest");

	//regex.setLabelAll("<Anchor>",node,"Block");
	//regex.setLabelAll("<Anchor>",node,"Program");
	//regex.setLabelAll("<Anchor>",node,"Subroutine");
	//regex.setLabelAll("<Anchor>",node,"Function");

	regex.unignoreAll(node,"...","declaration");
	regex.unignoreAll(node,"...","implicit");
	regex.unignoreAll(node,"...","external");
	regex.unignoreAll(node,"...","equivalence");
	regex.unignoreAll(node,"...","dimension");
	regex.unignoreAll(node,"...","parameter");
	regex.unignoreAll(node,"...","common");
	regex.unignoreAll(node,"...","data");

	regex.setLabelAll("<Declaration>",node,"...","declaration");
	regex.setLabelAll("<Declaration>",node,"...","implicit");
	regex.setLabelAll("<Declaration>",node,"...","external");
	regex.setLabelAll("<Declaration>",node,"...","equivalence");
	regex.setLabelAll("<Declaration>",node,"...","dimension");
	regex.setLabelAll("<Declaration>",node,"...","parameter");
	regex.setLabelAll("<Declaration>",node,"...","common");
	regex.setLabelAll("<Declaration>",node,"...","data");

	regex.hideAll( "Statements",       "(?m)(?i)[^<Declaration>]+$","<Statements>",node,"...","Main");
	regex.hideAll( "Declarations",     "(?m)(?i)^[^<Statements>]+","<Processed>",node,"...","Main");

	regex.setNodeNameAll("start",node,"Block","blockStart");
	regex.setNodeNameAll("start",node,"Program","programStart");
	regex.setNodeNameAll("start",node,"...","Function","functionStart");
	regex.setNodeNameAll("start",node,"...","Subroutine","subroutineStart");

	//regex.unignoreAll(node,"...","linefeed");
	//regex.unhideAll(node,"...","linefeed");

	regex.unignoreAll(node,"...","entry");

	//   *********************** identify blocks
	regex.ignoreAll();                                 // first ignore everything
	regex.unignoreAll("Program");                      // unignore relevant nodes...
	regex.unignoreAll("Function");                     // unignore relevant nodes...
	regex.unignoreAll("Subroutine");                   // unignore relevant nodes...
	regex.unignoreAll("Main");                         // unignore relevant nodes...
	regex.unignoreAll("Statements");                   // unignore relevant nodes...
	regex.unignoreAll("Contains");                   // unignore relevant nodes...
	//
	regex.unignoreAll("if");                           // unignore relevant nodes...
	regex.unignoreAll("elseif");
	regex.unignoreAll("else");
	regex.unignoreAll("endif");
	regex.unignoreAll("do");
	regex.unignoreAll("dofor");
	regex.unignoreAll("dowhile");
	regex.unignoreAll("enddo");

	regex.setLabelAll("<If>",     "if");
	regex.setLabelAll("<Elseif>", "elseif");
	regex.setLabelAll("<Else>",   "else");
	regex.setLabelAll("<Endif>",  "endif");
	regex.setLabelAll("<Do>",     "do");
	regex.setLabelAll("<Do>",     "dofor");
	regex.setLabelAll("<Do>",     "dowhile");
	regex.setLabelAll("<Enddo>",  "enddo");

	if (regex.hideTheRestAll("garbage","<Processed>","Statements")) {
	    RegexNode garbage=regex.getFirstNode("Statements","garbage");
	    System.out.format("*** Found garbage:\n%s\n",garbage.getText("garbage"));
	}; // hide the rest

	// regex.dumpToFile("debugXX_%d.tree");

	// match block start and end
	while(regex.hideAll( "IfBlock", "(?m)(?i)<If>[^<If><Endif><Do><Enddo>]*<Endif>","<Processed>","Statements")||
	      regex.hideAll( "DoLoop",  "(?m)(?i)<Do>[^<If><Endif><Do><Enddo>]*<Enddo>","<Processed>","Statements")) {
	};

	regex.hideAll( "IfStatements", "(?m)(?i)[^<If><Elseif><Else><Endif><Do><Enddo>]+","<Processed>","IfBlock");
	regex.hideAll( "DoStatements",  "(?m)(?i)[^<If><Elseif><Else><Endif><Do><Enddo>]+","<Processed>","DoLoop");

	regex.unignoreAll();         // first un-ignore everything

	//regex.unhideAll("garbage");
    }
    static private void hidePrimitives(RegexNode regex, String... node) {

	regex.unignoreAll();
	regex.ignoreAll("string");
	regex.ignoreAll("comment");
	regex.ignoreAll("type");

	// identify labels in the indentation...
	regex.hideAll("target",  "([\\d]+)", "<Anchor>",  "indentation");

	// create attributes
	regex.hideAll("attribute", "(<attribute>)",  "<Attribute>",  "attributes");
	regex.hideAll("type",      "(<name>)",       "<Pointer>",    "attribute","...");

	// create expressions at the "top level" 
	regex.setNodeNameAll("expression", "(expression)","Brackets","content");

	regex.hideAll("expression", "([^,]+)",  "<Rule>",    "expressions");

	regex.hideAll("argument", "([^,]+)",  "<Rule>",    "arguments");
	regex.hideAll("argument", "([^,]+)",  "<Rule>",    "(arguments)","Brackets","content");
	regex.hideAll("rule", "(<name>)=(<expression>)",  "<Rule>",    "argument");
	regex.hideNodeGroup("name",         "<Pointer>", 1,  "rule");
	regex.hideNodeGroup("expression",   "<Anchor>",    2, "rule");
	regex.hideAll("expression",  "([^\\,<Rule>]+)","<Pointer>",  "arguments","argument"); // top level only
	regex.hideAll("expression",  "([^\\,<Rule>]+)","<Pointer>",  "(arguments)","Brackets","content","argument"); // top level only

	// search all bracket-contents for expressions (=anything not containing , or a rule)
	regex.hideAll("expression",  "([^\\,<Rule><Pointer>]+)","<Pointer>",  "content");

	// hide logical operators
        regex.hideAll( "true",        "(?m)(?i)\\.true\\.",  "<Operator>","expression","...");
        regex.hideAll( "false",       "(?m)(?i)\\.false\\.", "<Operator>","expression","...");
        regex.hideAll( "not",         "(?m)(?i)\\.not\\.",   "<Operator>","expression","...");
        regex.hideAll( "and",         "(?m)(?i)\\.and\\.",   "<Operator>","expression","...");
        regex.hideAll( "and",         "(?m)(?i)&&",          "<Operator>","expression","...");
        regex.hideAll( "or",          "(?m)(?i)\\.or\\.",    "<Operator>","expression","...");
        regex.hideAll( "or",          "(?m)(?i)\\|\\|",      "<Operator>","expression","...");
        regex.hideAll( "eq",          "(?m)(?i)\\.eq\\.",    "<Operator>","expression","...");
        regex.hideAll( "eq",          "(?m)(?i)==",          "<Operator>","expression","...");
        regex.hideAll( "ne",          "(?m)(?i)\\.ne\\.",    "<Operator>","expression","...");
        regex.hideAll( "le",          "(?m)(?i)\\.le\\.",    "<Operator>","expression","...");
        regex.hideAll( "le",          "(?m)(?i)<=",          "<Operator>","expression","...");
        regex.hideAll( "lt",          "(?m)(?i)\\.lt\\.",    "<Operator>","expression","...");
        regex.hideAll( "lt",          "(?m)(?i)<",           "<Operator>","expression","...");
        regex.hideAll( "ge",          "(?m)(?i)\\.ge\\.",    "<Operator>","expression","...");
        regex.hideAll( "ge",          "(?m)(?i)>=",          "<Operator>","expression","...");
        regex.hideAll( "gt",          "(?m)(?i)\\.gt\\.",    "<Operator>","expression","...");
        regex.hideAll( "gt",          "(?m)(?i)>",           "<Operator>","expression","...");
	regex.hideAll( "operator",    "(?m)(?i)<Operator>",        "<Op>","expression","...");
	regex.ignoreAll("operator"); // ignore operator statements

	// hide arithmetic operations and real numbers
	regex.hideAll( "number", "(?m)(?i)(\\d*\\.\\d+[dDeE][+-]*\\d+)",  "<Anchor>","expression","...");
	regex.hideAll( "number", "(?m)(?i)(\\d+\\.\\d*[dDeE][+-]*\\d+)",  "<Anchor>","expression","...");
	regex.ignoreAll("number");
	// hide arithmetic operations (recursively)
	while(regex.hideAll( "arithmetic", "(?m)(?i)(<atom>+)< >(\\*\\*)< >(<atom>+)",  "<Anchor>","expression","...") ||
	      regex.hideAll( "arithmetic", "(?m)(?i)(<atom>+)< >([\\*\\+\\-\\/])< >(<atom>+)",  "<Anchor>","expression","...")) {
	    if (debug) System.out.format("%s inside arithmetic iteration .\n",time(tstart));
	    regex.hideNodeGroup("argument", "<Argument>", 1, node,"...","arithmetic");
	    regex.hideNodeGroup("type", "<Argument>", 2, node,"...","arithmetic");
	    regex.hideNodeGroup("argument", "<Argument>", 3, node,"...","arithmetic");
	    regex.ignoreAll(node,"...","arithmetic");
	};

	// identify pointers that may point to intrinsic functions, functions or variables...
	regex.hideAll("pointer",  "<name>< >[<Brackets>]", "<Pointer>", "expression","...");
	regex.hideAll("pointer",  "<name>", "<Pointer>", "expression","...");
	regex.hideAll("pointer",  "<name>< >[<Brackets>]", "<Pointer>", "references","...");
	regex.hideAll("pointer",  "<name>", "<Pointer>", "references","...");

	//regex.dumpToFile("analyseA.tree");

	// identify intrinsic functions
	regex.hideAll( "intrinsic", "(?m)(?i)^< >(max|min|asin|acos|atan|sin|cos|tan)< >(<Brackets>)$",  "<Anchor>","pointer");
	regex.hideNodeGroup("type",        "<Argument>",  1,  "intrinsic");
	regex.hideNodeGroup("(arguments)", "<Argument>",   2, "intrinsic");
	regex.ignoreAll("intrinsic","type");

	// identify intrinsic functions
	regex.hideAll( "intrinsic", "(?m)(?i)^(err|end)$",  "<Anchor>","gotoIO","pointer");
	regex.hideNodeGroup("type",        "<Argument>",  1,  "gotoIO","pointer","intrinsic");
	regex.ignoreAll("intrinsic","type");

	// identify references (variables/functions)
	regex.hideAll( "reference", "(?m)(?i)^(<name>)< >(<Brackets>)$",  "<Anchor>","pointer");
	regex.hideAll( "reference", "(?m)(?i)^(<name>)()$",  "<Anchor>","pointer");
	regex.hideNodeGroup("name",         "<Pointer>", 1,  "reference");
	regex.hideNodeGroup("(arguments)",  "<Pointer>", -2,  "reference");

	// clean up
	regex.setNodeNameAll("expression", "(expression)","Brackets","content");
	regex.unhideAll(1,"(expression)", "Brackets", "expression"); // unhide "Brackets"
	regex.unhideAll(1,"(expression)", "expression");             // unhide "(expression)"

	regex.setNodeNameAll("arguments", "(arguments)","Brackets","content");
	regex.unhideAll(1,"(arguments)", "Brackets","arguments"); // unhide "Brackets" 
	regex.unhideAll(1,"(arguments)", "arguments");            // unhide "(arguments)"

	regex.unhideAll(1,"pointer", "intrinsic");                // unhide "pointer"
	regex.unhideAll(1,"pointer", "reference");                // unhide "reference"

	regex.unhideAll(1,"expression","gotoIO");                // unhide "pointer"
	regex.unhideAll(0,"gotoIO", "intrinsic");                // unhide "pointer"

	regex.setNodeNameAll("argument", "arguments","expression");

	regex.unhideAll("data", "list", "item");

	/*
	int cnt=0;
	regex.ignoreAll("name");
	// process arguments, pointers and pointers with arguments
	boolean bchange=true;
	while ( bchange) {
	    bchange = ( regex.hideAll("argument", "(<noArgument>+)", "<Argument>",    "arguments"));
	    bchange = ( regex.setNodeNameAll("_arguments","arguments") || bchange);
	    bchange = ( regex.hideAll( "_intrinsic", "(?m)(?i)([max|min|asin|acos|atan|sin|cos|tan])< >(>Brackets>)",  "<Anchor>","argument","...") || bchange);
	    bchange = ( regex.hideNodeGroup("type", "<Argument>", 1, "_intrinsic") || bchange);
	    bchange = ( regex.hideNodeGroup("(arguments)", "<Argument>", 2, "_intrinsic") || bchange);
	    bchange = ( regex.setNodeNameAll("intrinsic","_intrinsic") || bchange);
	    regex.ignoreAll("type");
	    bchange = ( regex.hideAll("pointer", "([a-zA-Z]<noPointer>*)", "<Pointer>",    "argument","...") || bchange);
	    bchange = ( regex.setNodeNameAll("_argument","argument") || bchange );
	    bchange = ( regex.hideAll("(arguments)", "([<Brackets>])$", "<Argument>",  "pointer") || bchange);
	    bchange = ( regex.hideAll("name", "([^ <Brackets><Name><Pointer><Argument>]+)", "<Name>",    "pointer") || bchange );
	    bchange = ( regex.setNodeNameAll("_pointer","pointer") || bchange );
	    bchange = ( regex.unhideAll("(arguments)","Brackets") || bchange );
	    bchange = ( regex.setNodeNameAll("arguments","(arguments)") || bchange );
	    regex.ignoreAll("name");
	    //bchange=false;
	    cnt++;
	    //System.out.format("Pointer loop %d\n",cnt);
	};
	regex.setNodeNameAll("arguments","_arguments");
	regex.setNodeNameAll("argument","_argument");
	regex.setNodeNameAll("pointer","_pointer");
	regex.unignoreAll("name");

	*/
    }
}
