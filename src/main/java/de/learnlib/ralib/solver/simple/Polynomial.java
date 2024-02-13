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

public class Polynomial extends Constraint {

	static Constraint fromSet(Set<Monomial> constraints) {
		int size = constraints.size();
		if(size == 0)
		    return new Polynomial(constraints);
		else if(size == 1)
		    return constraints.iterator().next();
		return new Polynomial(constraints);
	}

	private final Set<Monomial> constraints;

	private Polynomial(Set<Monomial> constraints) {
		this.constraints = constraints;
	}

        @Override
	public Constraint negate() {
		List<Constraint> csets = new ArrayList<Constraint>(constraints.size());

		for(Monomial m : constraints)
			csets.add(m.negate());

		return Constraint.conjunction(csets);
	}

	public boolean implies(Polynomial other) {
		return conjunction(Arrays.asList(this, other.negate())).isFalse();
	}

	public boolean equivalent(Polynomial other) {
		return implies(other) && other.implies(this);
	}

	public Set<Monomial> getConstraints() {
		return Collections.unmodifiableSet(constraints);
	}

        @Override
	public void print(Appendable a, String[] varNames) throws IOException {
		if(constraints.isEmpty()) {
			a.append("false");
			return;
		}
		boolean first = true;
		for(Constraint c : constraints) {
			if(first)
				first = false;
			else
				a.append(" || ");
			c.print(a, varNames);
		}
	}

        @Override
	public Polynomial shift(int[] numVars, int thisIdx) {
		Set<Monomial> cs = new HashSet<Monomial>(constraints.size());
		for(Monomial c : constraints)
			cs.add(c.shift(numVars, thisIdx));
		return new Polynomial(cs);
	}

        @Override
	public Polynomial shift(int myVars, int base, int total) {
		Set<Monomial> cs = new HashSet<Monomial>(constraints.size());
		for(Monomial c : constraints)
			cs.add(c.shift(myVars, base, total));
		return new Polynomial(cs);
	}

        @Override
	public boolean isFalse() {
		return constraints.isEmpty();
	}

        @Override
	public boolean isTrue() {
		return negate().isFalse();
	}

        @Override
	public Polynomial substitute(int[] subst) {
		Set<Monomial> cs = new HashSet<Monomial>();
		for(Monomial c : constraints)
			cs.add(c.substitute(subst));
		return new Polynomial(cs);
	}

        @Override
	public Polynomial shift(int shift) {
		Set<Monomial> cs = new HashSet<Monomial>(constraints.size());
		for(Monomial c : constraints)
			cs.add(c.shift(shift));
		return new Polynomial(cs);
	}

        @Override
	public Polynomial restrict(int numVars) {
		Set<Monomial> cs = new HashSet<Monomial>();
		for(Monomial c : constraints)
			cs.add(c.restrict(numVars));
		return new Polynomial(cs);
	}

        @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((constraints == null) ? 0 : constraints.hashCode());
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
		Polynomial other = (Polynomial) obj;
		if (constraints == null) {
			if (other.constraints != null)
				return false;
		} else if (!constraints.equals(other.constraints))
			return false;
		return true;
	}

        @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			print(sb, new String[0]);
		}
		catch(IOException e) {}
		return sb.toString();
	}

        @Override
	public Collection<Monomial> monomials() {
		return constraints;
	}


        @Override
	public Constraint normalize() {
		return negate().negate();
	}

}
