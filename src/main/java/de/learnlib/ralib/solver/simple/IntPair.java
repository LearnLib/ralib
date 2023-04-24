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
