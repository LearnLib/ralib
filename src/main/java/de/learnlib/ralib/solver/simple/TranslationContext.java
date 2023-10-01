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

	private Constraint translate(TrueGuardExpression unused) {
		return Constraint.TRUE;
	}

	private Constraint translate(FalseGuardExpression unused) {
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
