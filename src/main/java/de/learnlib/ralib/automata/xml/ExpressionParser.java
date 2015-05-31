/*
 * Copyright (C) 2014 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.learnlib.ralib.automata.xml;


import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Relation;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;
import java.util.ArrayList;
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
      
    private GuardExpression predicate;
    
    public ExpressionParser(String exp, Map<String, SymbolicDataValue> pMap) {
        expLine = exp.trim();
        this.pMap = pMap;
        
        buildExpression();
    }
    
    private void buildExpression() 
    {
        this.predicate = buildDisjunction(expLine);
    }
    
    private GuardExpression buildDisjunction(String dis) {
        StringTokenizer tok = new StringTokenizer(dis, "||");
        if (tok.countTokens() < 2) {
            return buildConjunction(dis);
        }
        List<GuardExpression> disjuncts = new ArrayList<>();        
        while (tok.hasMoreTokens()) {
            disjuncts.add(buildConjunction(tok.nextToken().trim()));
        }
        return new Disjunction(disjuncts.toArray(new GuardExpression[] {}));
    }

    private GuardExpression buildConjunction(String con) {
        StringTokenizer tok = new StringTokenizer(con, "&&");
        if (tok.countTokens() < 2) {
            return buildPredicate(con);
        }
        List<GuardExpression> conjuncts = new ArrayList<>();        
        while (tok.hasMoreTokens()) {
            conjuncts.add(buildPredicate(tok.nextToken().trim()));
        }
        return new Conjunction(conjuncts.toArray(new GuardExpression[] {}));            
    }

    private GuardExpression buildPredicate(String pred) 
    {
        pred = pred.replace("!=", "!!");
        if (pred.trim().length() < 1) {
            return new TrueGuardExpression();
        }
        else if (pred.contains("==")) {
            String[] related = pred.split("==");
            SymbolicDataValue left = pMap.get(related[0].trim());
            SymbolicDataValue right = pMap.get(related[1].trim());
            return new AtomicGuardExpression(left, Relation.EQUALS, right);            
        } 
        else if (pred.contains("!!")) {
            String[] related = pred.split("!!");
            SymbolicDataValue left = pMap.get(related[0].trim());
            SymbolicDataValue right = pMap.get(related[1].trim());
            return new AtomicGuardExpression(left, Relation.NOT_EQUALS, right);            
        }
        
        throw new IllegalStateException(
                "this should not happen!!! " + pred + " in " + expLine);
    }
    
    /**
     * @return the predicate
     */
    public GuardExpression getPredicate() {
        return predicate;
    }
    
}
