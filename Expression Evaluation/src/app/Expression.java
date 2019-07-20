package app;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	public static String delims = " \t*+-/()[]";
			
    /**
     * Populates the vars list with simple variables, and arrays lists with arrays
     * in the expression. For every variable (simple or array), a SINGLE instance is created 
     * and stored, even if it appears more than once in the expression.
     * At this time, values for all variables and all array items are set to
     * zero - they will be loaded from a file in the loadVariableValues method.
     * 
     * @param expr The expression
     * @param vars The variables array list - already created by the caller
     * @param arrays The arrays array list - already created by the caller
     */
    public static void 
    makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	/** COMPLETE THIS METHOD **/
    	/** DO NOT create new vars and arrays - they are already created before being sent in
    	 ** to this method - you just need to fill them in.
    	 **/
    	StringTokenizer st = new StringTokenizer(expr, delims);
    	String temp = "";
    	
    	while(st.hasMoreTokens()) {
    		temp = st.nextToken();
    		int endOfString = expr.indexOf(temp) + temp.length();
    		
    		if(Character.isLetter(temp.charAt(0))) {
    			
	    		if(endOfString >= expr.length()) {
	    			vars.add(new Variable(temp));
	    			break;
	    		}
	    		else if(expr.charAt(endOfString) == '[') {
	    			Array tempArr = new Array(temp);
	    			if(!arrays.contains(tempArr)) {
	    				arrays.add(tempArr);
	    			}
	    		}
	    		else {
	    			Variable tempVar = new Variable(temp);
	    			if(!vars.contains(tempVar)) {
	    				vars.add(tempVar);
	    			}
	    		}
    		}
    	} 
    }
    

    /**
     * Loads values for variables and arrays in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     * @param vars The variables array list, previously populated by makeVariableLists
     * @param arrays The arrays array list - previously populated by makeVariableLists
     */
    public static void 
    loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String tok = st.nextToken();
            Variable var = new Variable(tok);
            Array arr = new Array(tok);
            int vari = vars.indexOf(var);
            int arri = arrays.indexOf(arr);
            if (vari == -1 && arri == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                vars.get(vari).value = num;
            } else { // array symbol
            	arr = arrays.get(arri);
            	arr.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    arr.values[index] = val;              
                }
            }
        }
    }
    
    /**
     * Evaluates the expression.
     * 
     * @param vars The variables array list, with values for all variables in the expression
     * @param arrays The arrays array list, with values for all array items
     * @return Result of evaluation
     */
    public static float evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	Stack<Float> values = new Stack<Float>();
    	Stack<Character> operators = new Stack<Character>();
    	Stack<Array> arrs = new Stack<Array>();
    	Stack<Integer> arrPos = new Stack<Integer>();
    	
    	expr = deleteWhiteSpaces(expr);
    	char[] exp = expr.toCharArray();
    	
    	for(int i = 0; i < exp.length; i++) {
    		//Check to see if it is a number, push it onto values stack
    		if(Character.isDigit(exp[i])) {
    			String num = "";
    			do {
    				num += exp[i];
    				i++;
    			}while(i < exp.length && Character.isDigit(exp[i]));
    			values.push(Float.parseFloat(num.toString()));
    			if(i < exp.length)
    				i--;
    		}
    		
    		//Check to see if it is a letter, parse and push it onto values stack
    		else if(Character.isLetter(exp[i])) {
    			String var = "";
    			do {
    				var += exp[i];
    				i++;
    			}while(i < exp.length && Character.isLetter(exp[i]));
    			Variable tempVar = new Variable(var);
    			if(i < exp.length)
    				i--;
    			if(isVariable(tempVar, vars)) {
    				for(int x = 0; x < vars.size();x++) {
    		    		if(vars.get(x).equals(tempVar))
    		    			values.push((float)vars.get(x).value);
    		    	}
    			}
    			else {
    				Array tempArr = new Array(var);
    				for(int y = 0; y < arrays.size(); y++) {
    					if(arrays.get(y).equals(tempArr)) {
    						arrs.push(arrays.get(y));
    					}
    				}
    			}
    			
    		}
    		
    		//Check to see if it is a left bracket, push it on
    		else if(exp[i] == '[') {
    			operators.push(exp[i]);
    			arrPos.push(i);
    		}
    		
    		//Check if it is a right bracket, solve the previous expression
    		else if(exp[i] == ']') {
				if(expr.substring(arrPos.peek(),i).contains("+") || expr.substring(arrPos.peek(),i).contains("-") || expr.substring(arrPos.peek(),i).contains("*") || expr.substring(arrPos.peek(),i).contains("/")) {
					while(operators.peek() != '[') {
						values.push(math(operators.pop(), values.pop(), values.pop()));
					}
				}
				values.push((float)arrs.peek().values[(int)(values.pop()*100/100)]);
    			
    			operators.pop();
    			arrPos.pop();
    			arrs.pop();
    		}
    		//Check to see if it is left parentheses, push it on, starting a new expression
    		else if(exp[i] == '(') {
    			operators.push(exp[i]);
    		}
    		
    		//Check to see if it is a right parentheses, and solve the previous expression
    		else if(exp[i] == ')') {
    			while(operators.peek() != '(') {
    				values.push(math(operators.pop(), values.pop(), values.pop()));
    			}
    			operators.pop();
    		}
    		
    		//Check to see if it is an operator and do order of operations
    		else if(isOp(exp[i])) {
    			//As long as the top of ops has same or greater priority, keep doing math using the top op
    			while(!operators.isEmpty() && orderOfOps(exp[i], operators.peek())) {
    				values.push(math(operators.pop(), values.pop(), values.pop()));
    			}
    			operators.push(exp[i]);
    		}
    	}
    	while(!operators.isEmpty()) {
			values.push(math(operators.pop(), values.pop(), values.pop()));
		}
    	
    	return values.pop();
    }
    
    private static boolean isVariable(Variable var, ArrayList<Variable> vars) {
		for(int i = 0; i < vars.size(); i++) {
			if(vars.get(i).equals(var)) {
				return true;
			}
		}
		return false;
	}


	private static float math(char op, float y, float x) {
    	if(op == '+') {
    		return x+y;
    	}
    	else if(op == '-') {
    		return x-y;
    	}
    	else if(op == '*') {
    		return x*y;
    	}
    	else if(op == '/') {
    		return x/y;
    	}
		return x;
    }
    private static boolean isOp(char op) {
    	if(op == '+' || op == '-' || op == '/' || op == '*') {
    		return true;
    	}
    	return false;
    }
    private static boolean orderOfOps(char op, char op1) {
        if(op1 == '(' || op1 == ')') 
            return false; 
        if(op1 == '[' || op1 == ']')
        	return false;
        if((op == '*' || op == '/') && (op1 == '+' || op1 == '-')) 
            return false; 
        else
            return true; 
    }
    private static String deleteWhiteSpaces(String expr) {
    	expr.trim();
    	String exp = expr.replaceAll("\\s+", "");
    	return exp;
    }
}
