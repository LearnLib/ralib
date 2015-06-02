/*
 * Copyright (C) 2015 malte.
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
package de.learnlib.ralib.solver.simple;

import java.util.HashMap;
import java.util.Map;

import de.learnlib.ralib.automata.guards.AtomicGuardExpression;
import de.learnlib.ralib.automata.guards.Conjunction;
import de.learnlib.ralib.automata.guards.Disjunction;
import de.learnlib.ralib.automata.guards.FalseGuardExpression;
import de.learnlib.ralib.automata.guards.GuardExpression;
import de.learnlib.ralib.automata.guards.Negation;
import de.learnlib.ralib.automata.guards.TrueGuardExpression;
import de.learnlib.ralib.data.SymbolicDataValue;

public class TranslationContext {

	private final Map<SymbolicDataValue,Integer> dvMap = new HashMap<>();
	
	public int getDataValueIndex(SymbolicDataValue dataValue) {
		Integer i = dvMap.get(dataValue);
		if (i == null) {
			return -1;
		}
		return i.intValue();
	}
	
	public int translateDataValue(SymbolicDataValue dataValue) {
		Integer i = dvMap.get(dataValue);
		if (i == null) {
			i = dvMap.size();
			dvMap.put(dataValue, i);
		}
		return i.intValue();
	}
	
	public Constraint translateGuard(GuardExpression e) {
		if (e instanceof TrueGuardExpression) {
			return translate((TrueGuardExpression) e);
		}
		if (e instanceof FalseGuardExpression) {
			return translate((FalseGuardExpression) e);
		}
		if (e instanceof Negation) {
			return translate((Negation) e);
		}
		if (e instanceof Conjunction) {
			return translate((Conjunction) e);
		}
		if (e instanceof Disjunction) {
			return translate((Disjunction) e);
		}
		if (e instanceof AtomicGuardExpression) {
			return translate((AtomicGuardExpression<?,?>) e);
		}
		throw new IllegalArgumentException();
	}
	
	private Constraint translate(TrueGuardExpression e) {
		return Constraint.TRUE;
	}
	
	private Constraint translate(FalseGuardExpression e) {
		return Constraint.FALSE;
	}
	
	private Constraint translate(Negation e) {
		return translateGuard(e.getNegated()).negate();
	}
	
	private Constraint translate(Conjunction e) {
		GuardExpression[] conjuncts = e.getConjuncts();
		Constraint[] translated = new Constraint[conjuncts.length];
		for (int i = 0; i < conjuncts.length; i++) {
			translated[i] = translateGuard(conjuncts[i]);
		}
		return Constraint.conjunction(translated);
	}
	
	private Constraint translate(Disjunction e) {
		GuardExpression[] disjuncts = e.getDisjuncts();
		Constraint[] translated = new Constraint[disjuncts.length];
		for (int i = 0; i < disjuncts.length; i++) {
			translated[i] = translateGuard(disjuncts[i]);
		}
		return Constraint.disjunction(translated);
	}
	
	private Monomial translate(AtomicGuardExpression<?, ?> e) {
		int lhs = translateDataValue(e.getLeft());
		int rhs = translateDataValue(e.getRight());
		switch (e.getRelation()) {
		case EQUALS:
			return Constraint.makeEquality(lhs, rhs);
		case NOT_EQUALS:
			return Constraint.makeInequality(lhs, rhs);
		}
		throw new AssertionError();
	}

}
