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

public class IntPair implements Comparable<IntPair> {
	
	public static IntPair makeOrdered(int fst, int snd) {
		if(fst > snd)
			return new IntPair(snd, fst);
		return new IntPair(fst, snd);
	}
	
	private final int fst;
	private final int snd;
	
	public IntPair() {
		fst = snd = 0;
	}
	
	public IntPair(int fst, int snd) {
		this.fst = fst;
		this.snd = snd;
	}
	
	public int getFst() {
		return fst;
	}
	
	public int getSnd() {
		return snd;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fst;
		result = prime * result + snd;
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
		IntPair other = (IntPair) obj;
		if (fst != other.fst)
			return false;
		if (snd != other.snd)
			return false;
		return true;
	}

	@Override
	public int compareTo(IntPair o) {
		int d = fst - o.fst;
		if(d != 0)
			return d;
		return snd - o.snd;
	}

}
