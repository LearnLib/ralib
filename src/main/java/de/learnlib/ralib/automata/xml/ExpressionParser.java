/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.learnlib.ralib.automata.xml;


import de.learnlib.ralib.automata.guards.DataExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 *
 * @author fh
 */
public class ExpressionParser {
    
    
    private final String expLine;
    private final Map<String, SymbolicDataValue> pMap;
    private final Map<SymbolicDataValue, gov.nasa.jpf.constraints.api.Variable> vars = 
            new LinkedHashMap<>();    
    private DataExpression<Boolean> predicate;
    
    public ExpressionParser(String exp, Map<String, SymbolicDataValue> pMap) {
        expLine = exp.trim();
        this.pMap = pMap;
        
        buildExpression();
    }
    
    private void buildExpression() 
    {
        Expression<Boolean> disjunction = buildDisjunction(expLine);
        this.predicate = new DataExpression<>(disjunction, vars);
    }
    
    private Expression<Boolean> buildDisjunction(String dis) {
        StringTokenizer tok = new StringTokenizer(dis, "||");
        if (tok.countTokens() < 2) {
            return buildConjunction(dis);
        }
        List<Expression<Boolean>> disjuncts = new ArrayList<>();        
        while (tok.hasMoreTokens()) {
            disjuncts.add(buildConjunction(tok.nextToken().trim()));
        }
        return ExpressionUtil.or(disjuncts);
    }

    private Expression<Boolean> buildConjunction(String con) {
        StringTokenizer tok = new StringTokenizer(con, "&&");
        if (tok.countTokens() < 2) {
            return buildPredicate(con);
        }
        List<Expression<Boolean>> conjuncts = new ArrayList<>();        
        while (tok.hasMoreTokens()) {
            conjuncts.add(buildPredicate(tok.nextToken().trim()));
        }
        return ExpressionUtil.and(conjuncts);            
    }

    private Expression<Boolean> buildPredicate(String pred) 
    {
        pred = pred.replace("!=", "<>");
        
        if (pred.contains("==")) {
            String[] related = pred.split("==");
            SymbolicDataValue left = pMap.get(related[0].trim());
            SymbolicDataValue right = pMap.get(related[1].trim());
            return new NumericBooleanExpression(
                    getOrCreate(left), NumericComparator.EQ, getOrCreate(right));            
        } 
        else if (pred.contains("<>")) {
            String[] related = pred.split("<>");
            SymbolicDataValue left = pMap.get(related[0].trim());
            SymbolicDataValue right = pMap.get(related[1].trim());
            return new NumericBooleanExpression(
                    getOrCreate(left), NumericComparator.NE, getOrCreate(right));            
        }
        throw new IllegalStateException(
                "this should not happen!!! " + pred + " in " + expLine);
    }
    
    /**
     * @return the predicate
     */
    public DataExpression<Boolean> getPredicate() {
        return predicate;
    }
    
    private gov.nasa.jpf.constraints.api.Variable getOrCreate(SymbolicDataValue key) {
        gov.nasa.jpf.constraints.api.Variable var = vars.get(key);
        if (var == null) {
            var = new gov.nasa.jpf.constraints.api.Variable(
                    BuiltinTypes.DOUBLE, key.toString());
            vars.put(key, var);
        }
        return var;
    }
    
}
