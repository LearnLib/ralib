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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Constraint {
	
	public static Constraint TRUE = Monomial.TRUE;
	public static Constraint FALSE = Polynomial.FALSE;
	
	public static Constraint disjunction(List<? extends Constraint> constraints) {
		Set<Monomial> cset = new HashSet<Monomial>();
		for(Constraint c : constraints)
			cset.addAll(c.monomials());
		
		return Polynomial.fromSet(cset);
	}
	
	public static Monomial makeEquality(int lhs, int rhs) {
		return Monomial.create(Collections.singletonList(IntPair.makeOrdered(lhs, rhs)),
				Collections.<IntPair>emptyList());
	}
	
	public static Monomial makeInequality(int lhs, int rhs) {
		return Monomial.create(Collections.<IntPair>emptyList(),
				Collections.singletonList(IntPair.makeOrdered(lhs, rhs)));
	}
	
	public static Constraint disjunction(Constraint ...constraints) {
		return disjunction(Arrays.asList(constraints));
	}
	
	public static Constraint conjunction(List<? extends Constraint> constraints) {
		Set<Monomial> cset = new HashSet<Monomial>();
		makeConjunction(cset, constraints);
		return Polynomial.fromSet(cset);
	}
	
	public static Constraint conjunction(Constraint ...constraints) {
		return conjunction(Arrays.asList(constraints));
	}
	
	private static void makeConjunction(Set<Monomial> constraints, List<? extends Constraint> csetList) {
		makeConjunction(constraints, csetList, Monomial.TRUE, 0);
	}
	
	private static void makeConjunction(Set<Monomial> constraints, List<? extends Constraint> csetList, Monomial curr, int idx) {
		if(curr == null)
			return;
		if(idx >= csetList.size()) {
			constraints.add(curr);
			return;
		}
		Constraint cset = csetList.get(idx);
		if(cset.isFalse())
			return;
		
		for(Monomial m : cset.monomials())
			makeConjunction(constraints, csetList, Monomial.conjoin(curr, m), idx+1);
	}

	public abstract Constraint restrict(int newDomSize);

	public abstract Constraint negate();

	public abstract void print(Appendable a, String[] varNames)
			throws IOException;

	public abstract Constraint shift(int[] numVars, int thisIdx);

	public abstract Constraint shift(int myVars, int base, int total);

	public abstract Constraint substitute(int[] subst);

	public abstract Constraint shift(int shift);
	
	public abstract Collection<Monomial> monomials();
	
	public abstract boolean isTrue();
	
	public abstract boolean isFalse();
	
	public boolean implies(Constraint other) {
		return conjunction(this, other.negate()).isFalse();
	}
	
	public Relation compare(Constraint other) {
		boolean implies = implies(other);
		boolean impliedBy = other.implies(this);
		if(implies) {
			if(impliedBy)
				return Relation.EQUIVALENT;
			return Relation.IMPLIES;
		}
		else if(impliedBy)
			return Relation.IMPLIED_BY;
		
		if(conjunction(this, other).isFalse())
			return Relation.DISJOINT;
		return Relation.INTERSECT;
	}
	
	public boolean equivalent(Constraint other) {
		return implies(other) && other.implies(this);
	}
	
	public abstract Constraint normalize();
	
}