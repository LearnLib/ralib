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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Monomial extends Constraint {

	private static List<IntPair> aggregateEqualities(List<Monomial> constraints) {
		List<IntPair> result = new ArrayList<IntPair>();
		for(Monomial c : constraints)
			result.addAll(c.getEqualities());
		return result;
	}

	private static List<IntPair> aggregateInequalities(List<Monomial> constraints) {
		List<IntPair> result = new ArrayList<IntPair>();
		for(Monomial c : constraints)
			result.addAll(c.getInequalities());
		return result;
	}

	private static IntPair shift(IntPair p, int myVars, int base, int total) {
		int fst = p.getFst(), snd = p.getSnd();
		if(fst < myVars)
			fst += base;
		else
			fst = fst - myVars + total;
		if(snd < myVars)
			snd += base;
		else
			snd = snd - myVars + total;
		return new IntPair(fst, snd);
	}

	public static Monomial conjoin(Monomial ...constraints) {
		return conjoin(Arrays.asList(constraints));
	}

	public static Monomial conjoin(List<Monomial> constraints) {
		List<IntPair> eqs = aggregateEqualities(constraints);
		List<IntPair> neqs = aggregateInequalities(constraints);
		return create(eqs, neqs);
	}

	public static Monomial create(List<IntPair> equalities, List<IntPair> inequalities) {
		DisjointSet<Integer> ds = new DisjointSet<Integer>();

		for(IntPair eq : equalities)
			ds.union(eq.getFst(), eq.getSnd());

		IntPair[] ineqs = new IntPair[inequalities.size()];

		{
			int i = 0;
			for(IntPair neq : inequalities) {
				int a = ds.find(neq.getFst());
				int b = ds.find(neq.getSnd());
				if(a == b)
					return null;

				ineqs[i++] = IntPair.makeOrdered(a, b);
			}
		}

		int pos = 0;

		if(ineqs.length > 0) {
			Arrays.sort(ineqs);
			IntPair last = ineqs[0];
			pos = 1;
			for(int i = 1; i < ineqs.length; i++) {
				IntPair neq = ineqs[i];
				if(!last.equals(neq)) {
					if(pos != i)
						ineqs[pos] = neq;
					last = neq;
					pos++;
				}
			}

		}

		List<IntPair> eqs = new ArrayList<IntPair>();

		for(List<Integer> set : ds.partition()) {
			if(set.size() < 2)
				continue;
			int min = Integer.MAX_VALUE;
			for(int i : set) {
				if(i < min)
					min = i;
			}
			for(int i : set) {
				if(i != min)
					eqs.add(new IntPair(min, i));
			}
		}

		return new Monomial(eqs, (pos <= 0) ? Collections.<IntPair>emptyList() : Arrays.asList(ineqs).subList(0, pos));
	}

	private final List<IntPair> equalities;
	private final List<IntPair> inequalities;

	private Monomial(List<IntPair> equalities, List<IntPair> inequalities) {
		this.equalities = equalities;
		this.inequalities = inequalities;
	}

	public List<IntPair> getEqualities() {
		return equalities;
	}

	public List<IntPair> getInequalities() {
		return inequalities;
	}
	/*
	public boolean implies(Constraint other) {
		if(equalities.size() < other.equalities.size())
			return false;
		else if(equalities.size() == other.equalities.size()) {
			if(inequalities.size() < other.inequalities.size())
				return false;
		}

		DisjointSet ds = partition();

		for(IntPair oeq : other.equalities) {
			if(oeq.)
		}

	}*/

	@Override
	public Monomial restrict(int newDomSize) {
		List<IntPair> eqs = new ArrayList<IntPair>();
		List<IntPair> neqs = new ArrayList<IntPair>();
		for(IntPair eq : equalities) {
			if(eq.getSnd() < newDomSize)
				eqs.add(eq);
		}
		for(IntPair neq : inequalities) {
			if(neq.getSnd() < newDomSize)
				neqs.add(neq);
		}

		return new Monomial(eqs, neqs);
	}

	@Override
	public Constraint negate() {
		if(isTrue())
			return Constraint.FALSE;
		Set<Monomial> mons = new HashSet<Monomial>(negated());

		return Polynomial.fromSet(mons);
	}

	public List<Monomial> negated() {
		List<Monomial> result = new ArrayList<Monomial>(equalities.size() + inequalities.size());

		for(IntPair eq : equalities)
			result.add(new Monomial(Collections.<IntPair>emptyList(), Collections.singletonList(eq)));
		for(IntPair neq : inequalities)
			result.add(new Monomial(Collections.singletonList(neq), Collections.<IntPair>emptyList()));

		return result;
	}

	@Override
	public void print(Appendable a, String[] varNames) throws IOException {
		if(equalities.isEmpty() && inequalities.isEmpty()) {
			a.append("true");
			return;
		}

		boolean first = true;
		for(IntPair eq : equalities) {
			if(first)
				first = false;
			else
				a.append(" && ");

			int lhs = eq.getFst(), rhs = eq.getSnd();

			printIndex(a, varNames, lhs);
			a.append(" = ");
			printIndex(a, varNames, rhs);
		}

		for(IntPair neq : inequalities) {
			if(first)
				first = false;
			else
				a.append(" && ");

			int lhs = neq.getFst();
			int rhs = neq.getSnd();

			printIndex(a, varNames, lhs);
			a.append(" != ");
			printIndex(a, varNames, rhs);
		}
	}

	public static void printIndex(Appendable a, String[] varNames, int index) throws IOException {
		if(index < varNames.length) {
			a.append(varNames[index]);
			return;
		}
		index -= varNames.length;

		a.append('p');
		a.append(Integer.toString(index+1));
	}

	@Override
	public Monomial shift(int[] numVars, int thisIdx) {
		int base = 0;
		int total = 0;
		for(int i = 0; i < numVars.length; i++) {
			if(i == thisIdx)
				base = total;
			total += numVars[i];
		}

		int myVars = numVars[thisIdx];

		List<IntPair> eqs = new ArrayList<IntPair>(equalities.size());
		for(IntPair ip : equalities)
			eqs.add(shift(ip, myVars, base, total));
		List<IntPair> neqs = new ArrayList<IntPair>(inequalities.size());
		for(IntPair ip : inequalities)
			neqs.add(shift(ip, myVars, base, total));

		return new Monomial(eqs, neqs);
	}

	@Override
	public Monomial shift(int myVars, int base, int total) {
		List<IntPair> eqs = new ArrayList<IntPair>(equalities.size());
		for(IntPair ip : equalities)
			eqs.add(shift(ip, myVars, base, total));
		List<IntPair> neqs = new ArrayList<IntPair>(inequalities.size());
		for(IntPair ip : inequalities)
			neqs.add(shift(ip, myVars, base, total));

		return new Monomial(eqs, neqs);
	}

	@Override
	public Monomial substitute(int[] subst) {
		List<IntPair> eqs = new ArrayList<IntPair>(equalities.size());
		for(IntPair ip : equalities)
			eqs.add(new IntPair(subst[ip.getFst()], subst[ip.getSnd()]));
		List<IntPair> neqs = new ArrayList<IntPair>(inequalities.size());
		for(IntPair ip : inequalities)
			neqs.add(new IntPair(subst[ip.getFst()], subst[ip.getSnd()]));

		return Monomial.create(eqs, neqs);
	}

	@Override
	public Monomial shift(int shift) {
		List<IntPair> eqs = new ArrayList<IntPair>(equalities.size());
		for(IntPair ip : equalities)
			eqs.add(new IntPair(ip.getFst() + shift, ip.getSnd() + shift));

		List<IntPair> neqs = new ArrayList<IntPair>(inequalities.size());
		for(IntPair ip : inequalities)
			neqs.add(new IntPair(ip.getFst() + shift, ip.getSnd() + shift));

		return new Monomial(eqs, neqs);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((equalities == null) ? 0 : equalities.hashCode());
		result = prime * result
				+ ((inequalities == null) ? 0 : inequalities.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Monomial other = (Monomial) obj;
		if (equalities == null) {
			if (other.equalities != null)
				return false;
		} else if (!equalities.equals(other.equalities))
			return false;
		if (inequalities == null) {
			if (other.inequalities != null)
				return false;
		} else if (!inequalities.equals(other.inequalities))
			return false;
		return true;
	}

	@Override
	public Collection<Monomial> monomials() {
		return Collections.singletonList(this);
	}

	@Override
	public boolean isTrue() {
		return equalities.isEmpty() && inequalities.isEmpty();
	}

	@Override
	public boolean isFalse() {
		return false;
	}

	@Override
	public Constraint normalize() {
		return this;
	}

}
