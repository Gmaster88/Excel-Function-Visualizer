package core;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.formula.ptg.AttrPtg;
import org.apache.poi.ss.formula.ptg.OperationPtg;

public class OperationToken extends FormulaToken {
  private FormulaToken[] children;          //The arguments of this function.
  private String op;                        //A string representation of this token without
                                            //  any information about arguments.
  /**
   * An Operation token represents a function in the overall formula which itself takes 
   * other tokens as arguments. For example, IF() can take 2 or 3 arguments: so `tok`
   * is the Ptg token that refers to IF() individually, and `args` is an array of the 
   * tokens that serve as the 2 or 3 arguments.
   * 
   * Arithmetical operations, like + - * /, also count as operations which take arguments.
   * 
   * @param tok   Spreadsheet operation.
   * @param args    All the arguments in the function defined by token.
   */
  public OperationToken(OperationPtg tok, FormulaToken[] args) {
    int len = args.length;
    if (tok.getNumberOfOperands() != len)
      throw new UnsupportedOperationException("OperationToken: not enough arguments to the operation.");   
    
    this.token = tok;
    
    String[] sArgs = Arrays.stream(args).map(s -> s.toString()).toArray(String[]::new);
    this.tokenStr = tok.toFormulaString(sArgs);
    this.op = extractOp(tok, len);
        
    addChildren(len, args);
  }
  
  /**
   * Constructor used primarily for single-arg SUM. We expect only one arg.
   * Vararged for automatic conversion to array. (TODO: Is that good form?)
   * 
   * @param tok   String representation of the function, including arguments.
   * @param args    Individual FormulaToken arguments.
   */
  public OperationToken(AttrPtg tok, FormulaToken... args) {
    if (args.length != 1)
      throw new UnsupportedOperationException("OperationToken: not enough arguments to the operation."); 
    
    this.token = tok;  
    this.tokenStr = "SUM(" + args[0] + ")";
    this.op = "SUM()";
    addChildren(args.length, args);
  }

  /**
   * Make and populate the children of this token.
   * @param len   Number of expected children for this function.
   * @param args  The array of children to this node.
   */
  private void addChildren(int len, FormulaToken[] args) {
    children = new FormulaToken[len];
    for (int i = 0; i < len; ++i)
      children[i] = args[i];
  }
  

  /**
   * Extracts the operator from the string. Uses an array of blank Strings so all operands
   * are represented as string "null" and thus easier to manipulate.
   * @param func    The operation token.
   * @param len     Number of arguments the argument expects.
   * @return        The simplest string representation of this operation, either just FOO() or the binary symbol 
   *                (+-/*& etc)
   */
  private String extractOp(OperationPtg func, int len) {
    String[] nulls = new String[len];
    String funcOp = func.toFormulaString(nulls);
    funcOp = NULL.reset(funcOp).replaceAll("");    
    return funcOp;    
  }
  private static final Matcher NULL = Pattern.compile("null,?").matcher("");

  public String wrap() {
    this.tokenStr = "(" + tokenStr + ")";
    return tokenStr;
  }
  
  public FormulaToken[] getChildren() {
    return children;
  }

  public String toString() {
    return tokenStr;
  }
  
  /**
   * Like toString but ignores all the arguments of the function.
   * @return    the function name
   */
  public String toSimpleString() {
    return op;
  }
  
  /**
   * Builds a hierarchical string of this node and it's children.
   * @param sb    The stringbuilder passed on from a higher level which contains the whole string so far.
   * @param depth How many levels down the tree we are now.
   */
  public StringBuilder toTreeString(StringBuilder sb, int depth) {    
    tabs(sb, depth);
    sb.append(this.op);
    sb.append("\n");
    
    for (FormulaToken child : children) {
      child.toTreeString(sb, depth + 1);
    }
    
    return sb;
  }
}