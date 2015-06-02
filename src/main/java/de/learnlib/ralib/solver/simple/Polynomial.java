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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Polynomial extends Constraint {
	
	public static Polynomial FALSE = new Polynomial(Collections.<Monomial>emptySet());
	
	
	static Constraint fromSet(Set<Monomial> constraints) {
		int siz = constraints.size();
		if(siz == 0)
			return FALSE;
		else if(siz == 1)
			return constraints.iterator().next();
		return new Polynomial(constraints);
	}
	
	private final Set<Monomial> constraints;
	
	private Polynomial(Set<Monomial> constraints) {
		this.constraints = constraints;
	}
	
	
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
	
	public Polynomial shift(int[] numVars, int thisIdx) {
		Set<Monomial> cs = new HashSet<Monomial>(constraints.size());
		for(Monomial c : constraints)
			cs.add(c.shift(numVars, thisIdx));
		return new Polynomial(cs);
	}
	
	public Polynomial shift(int myVars, int base, int total) {
		Set<Monomial> cs = new HashSet<Monomial>(constraints.size());
		for(Monomial c : constraints)
			cs.add(c.shift(myVars, base, total));
		return new Polynomial(cs);
	}


	public boolean isFalse() {
		return constraints.isEmpty();
	}
	
	public boolean isTrue() {
		return negate().isFalse();
	}

	public Polynomial substitute(int[] subst) {
		Set<Monomial> cs = new HashSet<Monomial>();
		for(Monomial c : constraints)
			cs.add(c.substitute(subst));
		return new Polynomial(cs);
	}
	
	public Polynomial shift(int shift) {
		Set<Monomial> cs = new HashSet<Monomial>(constraints.size());
		for(Monomial c : constraints)
			cs.add(c.shift(shift));
		return new Polynomial(cs);
	}

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
