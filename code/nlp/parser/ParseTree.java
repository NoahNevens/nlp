package nlp.parser;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing a syntactic parse tree.  The parse tree is a recursive structure.
 * A parse tree consists of a label and, if it is not a terminal, children that are also
 * parse trees.
 * 
 * @author Dave Kauchak
 * @version 2/26/2011
 *
 */
public class ParseTree {
	private static Pattern tagSplitPattern = Pattern.compile("(\\S+) (.+)");
	
	private boolean terminal; // whether or not this is a terminal node in the tree
	private String label; // the label of this node of the tree, e.g. S, NP, VP, etc.
	
	private ArrayList<ParseTree> children; // the sub parse trees of this node if it's a non-terminal
	
	/**
	 * Construct a new parse tree from a single line, parenthesized version of the parse tree.
	 * 
	 * @param treeString A single line containing a parenthesized version of a parse tree
	 */
	public ParseTree(String treeString){
		if( treeString.charAt(0) != '(' ||
			treeString.charAt(treeString.length()-1) != ')' ){
			throw new RuntimeException("Malformed tree: " + treeString);
		}
		
		// splice off the beginning and trailing parentheses
		treeString = treeString.substring(1, treeString.length()-1);
		
		// if this still had parenthesis in it, it's a non-terminal
		if( treeString.contains("(") ){
			Matcher m = tagSplitPattern.matcher(treeString);
		
			// make sure this subtree is of the proper form
			if( m.matches() ){
				label = m.group(1);
				String childrenString = m.group(2);
				
				terminal = false;
				children = new ArrayList<ParseTree>();
				
				int leftParenCount = 0;
				int start = 0;
				
				// find the subchildren and recursively create them
				for( int i = 0; i < childrenString.length(); i++ ){
					if( childrenString.charAt(i) == '(' ){
						leftParenCount++;
					}else if( childrenString.charAt(i) == ')' ){
						leftParenCount--;
						
						if( leftParenCount == 0 ){
							children.add(new ParseTree(childrenString.substring(start, i+1)));
							
							// the i+1th character should be a space (or we're off the end)
							start = i+2;
							
						}else if( leftParenCount < 0 ){
							throw new RuntimeException("Malformed subtree: " + treeString);
						}
					}
				}
			}else{
				throw new RuntimeException("Malformed subtree: " + treeString);
			}
			
		}else{
			// this is a part of speech
			// when we get down the the part of speech tag, create the POS node as a non-terminal
			// and then link in the terminal node
			terminal = false;
			
			Matcher m = tagSplitPattern.matcher(treeString);
			
			if( m.matches() ){
				label = m.group(1);
				children = new ArrayList<ParseTree>(1);
				children.add(new ParseTree(m.group(2), true));
			}else{
				throw new RuntimeException("Malformed tree leaf: " + treeString);
			}
		}  
	}
	
	/**
	 * Construct a new parse tree without any children (though it still may be a non-terminal
	 * with children yet unattached)
	 * 
	 * @param label the constituent label for this node (or the word if it's a terminal)
	 * @param terminal whether or not this is a terminal
	 */
	public ParseTree(String label, boolean terminal){
		this.label = label;
		this.terminal = terminal;

		if( !terminal ){
			children = new ArrayList<ParseTree>();
		}
	}
	
	/**
	 * Adds a child to this TreeNode from left to right
	 * 
	 * @param newChild the child to be added.
	 */
	public void addChild(ParseTree newChild){
		children.add(newChild);
	}
	
	/**
	 * Get the children of this parse tree.
	 * 
	 * @return an iterable object over the children parse trees
	 */
	public Iterable<ParseTree> getChildren(){
		return children;
	}
	
	/**
	 * Get the constituent labels of the children of this parse tree.
	 * 
	 * @return an ArrayList of the labels
	 */
	public ArrayList<String> getChildrenLabels(){
		ArrayList<String> labels = new ArrayList<String>(children.size());
		
		if( !terminal ){		
			for(ParseTree t: children){
				labels.add(t.getLabel());
			}
		}
		
		return labels;
	}
	
	/**
	 * Get the child at index of this parse tree
	 * 
	 * @param index the index of the child to obtain
	 * @return the child at index
	 */
	public ParseTree getChild(int index){
		return children.get(index);
	}
	
	/**
	 * Get the number of children/sub-trees for this parse tree
	 * 
	 * @return the number of children/sub-trees
	 */
	public int numChildren(){
		return children.size();
	}
	
	/**
	 * Checks if this parse tree is a terminal node or not
	 * 
	 * @return whether or not this parse tree is a terminal node
	 */
	public boolean isTerminal(){
		return terminal;
	}
	
	/**
	 * Get the constituent label for this parse tree.  If it is a terminal,
	 * this is the word.
	 * 
	 * @return the constituent label
	 */
	public String getLabel(){
		return label;
	}
	
	/**
	 * Get a string representation of this parse tree in parenthesized form
	 */
	public String toString(){
		if( terminal ){
			return label;
		}else{
			StringBuffer buffer = new StringBuffer();
		
			buffer.append("(");
			buffer.append(label);
			
			for(ParseTree child: children){
				buffer.append(" ");
				buffer.append(child.toString());
			}
			
			buffer.append(")");
			
			return buffer.toString();
		}
	}

	public static void main(String[] args) {
		Hashtable<String, Hashtable<String, Integer>> cfgCount = null;
		Hashtable<String, Integer> unigramCount = null;
		Hashtable<String, Hashtable<String, Double>> pcfg = null;
		cfgCount = new Hashtable<String, Hashtable<String, Integer>>(); 
		unigramCount = new Hashtable<String, Integer>();
		pcfg = new Hashtable<String, Hashtable<String, Double>>();
		List<String> sentences = new ArrayList<String>();
		BufferedReader objReader = null;
		try {
		 String strCurrentLine;
	  
		 objReader = new BufferedReader(new FileReader("example/example.parsed"));
	  
		 while ((strCurrentLine = objReader.readLine()) != null) {
	  
		  sentences.add(strCurrentLine);
		 }
	  
		} catch (IOException e) {
	  
		 e.printStackTrace();
	  
		} finally {
	  
		 try {
		  if (objReader != null)
		   objReader.close();
		 } catch (IOException ex) {
		  ex.printStackTrace();
		 }
		}

		for (String sentence: sentences) {
			ParseTree PT = new ParseTree(sentence);
			cfgCount = CFGMaker(PT, cfgCount);
			unigramCount = unigramMaker(PT, unigramCount);
		}

		for (Map.Entry<String, Hashtable<String, Integer>> entry : cfgCount.entrySet()) {
			String lhs = entry.getKey();
			Hashtable<String, Integer> countMap = entry.getValue();
			Hashtable<String, Double> probMap = new Hashtable<String, Double>();
			for (Map.Entry<String, Integer> entry2 : countMap.entrySet()) {
				String rhs = entry2.getKey();
				probMap.put(rhs, (double) entry2.getValue()/unigramCount.get(lhs));
			}
			pcfg.put(lhs, probMap);
		}
		System.out.println(pcfg);
	   }

	public static Hashtable<String, Hashtable<String, Integer>> CFGMaker(ParseTree pt, Hashtable<String, Hashtable<String, Integer>> cfgCount) {
		   if (!pt.isTerminal()) {
			Iterable<ParseTree> children = pt.getChildren();
			String currLabel = pt.label;
			ArrayList<String> childrenLabels = pt.getChildrenLabels();
			if (!cfgCount.containsKey(currLabel)) {
				cfgCount.put(currLabel, new Hashtable<String, Integer>());
			}
			Hashtable<String, Integer> map = cfgCount.get(currLabel);
			String childrenString = "";
			for (String child: childrenLabels) {
				childrenString = childrenString + child + " ";
			}
			childrenString = childrenString.substring(0, childrenString.length() - 1);
			if (map.containsKey(childrenString)) {
				int count = map.get(childrenString); 
				map.put(childrenString, count + 1);
			} else {
				map.put(childrenString, 1);
			}

			for (ParseTree child: children) {
				cfgCount = CFGMaker(child, cfgCount);
			}
		}
			return cfgCount;
		}

		public static Hashtable<String, Integer> unigramMaker(ParseTree pt, Hashtable<String, Integer> unigramCount) {
			if (!pt.isTerminal()) {
				Iterable<ParseTree> children = pt.getChildren();
				String currLabel = pt.label;
				if (!unigramCount.containsKey(currLabel)) {
					unigramCount.put(currLabel, 1);
				} else {
					int count = unigramCount.get(currLabel); 
					unigramCount.put(currLabel, count + 1);
				}
				for (ParseTree child: children) {
					unigramCount = unigramMaker(child, unigramCount);
				}
			}
			return unigramCount;
		}
	}
