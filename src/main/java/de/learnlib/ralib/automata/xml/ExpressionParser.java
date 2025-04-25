/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.automata.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import de.learnlib.ralib.data.SymbolicDataValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

/**
 *
 * @author fh
 */
public class ExpressionParser {

    private final String expLine;
    private final Map<String, SymbolicDataValue> pMap;

    private Expression<Boolean> predicate;

    public ExpressionParser(String exp, Map<String, SymbolicDataValue> pMap) {
        expLine = exp.trim();
        this.pMap = pMap;
        buildExpression();
    }

    private void buildExpression() {
        this.predicate = buildDisjunction(expLine);
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
        return ExpressionUtil.or(disjuncts.toArray(new Expression[] {}));
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
        return ExpressionUtil.and(conjuncts.toArray(new Expression[] {}));
    }

    private Expression<Boolean> buildPredicate(String pred) {
        pred = pred.replace("!=", "!!");
        if (pred.trim().length() < 1) {
            return ExpressionUtil.TRUE;
        }

        NumericComparator relation = null;
        String[] related = null;

        if (pred.contains("==")) {
            related = pred.split("==");
            relation = NumericComparator.EQ;
        }
        else if (pred.contains("!!")) {
            related = pred.split("!!");
            relation = NumericComparator.NE;
        }
        else if (pred.contains("<=")) {
        	related = pred.split("<=");
        	relation = NumericComparator.LE;
        }
        else if (pred.contains("<")) {
            related = pred.split("<");
            relation = NumericComparator.LT;
        }
        else if (pred.contains(">=")) {
        	related = pred.split(">=");
        	relation = NumericComparator.GE;
        }
        else if (pred.contains(">")) {
            related = pred.split(">");
            relation = NumericComparator.GT;
        }

        if (relation == null) {
            throw new IllegalStateException(
                    "this should not happen!!! " + pred + " in " + expLine);
        }

        SymbolicDataValue left = pMap.get(related[0].trim());
        SymbolicDataValue right = pMap.get(related[1].trim());
        return new NumericBooleanExpression(left, relation, right);
    }

    /**
     * @return the predicate
     */
    public Expression<Boolean> getPredicate() {
        return predicate;
    }

}
