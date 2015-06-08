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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisjointSet<T> {
	
	private static class Node<T> {
		T x;
		Node<T> parent;
		int rank;
		
		public Node(T x) {
			this.x = x;
			this.parent = null;
			this.rank = 0;
		}
	}
	
	
	
	private final Map<T,Node<T>> nodes
		= new HashMap<T,Node<T>>();
	
	public DisjointSet() {
	}
	
	private Node<T> getNode(T x) {
		Node<T> n = nodes.get(x);
		if(n == null) {
			n = new Node<T>(x);
			nodes.put(x, n);
		}
		return n;
	}
	
	public T find(T x) {
		Node<T> n = getNode(x);
		return findRoot(n).x;
	}
	
	public T union(T a, T b) {
		return union(findRoot(a), findRoot(b)).x;
	}
	
	private Node<T> findRoot(T x) {
		return findRoot(getNode(x));
	}
	
	private Node<T> findRoot(Node<T> n) {
		if(n.parent == null)
			return n;
		Node<T> root = findRoot(n.parent);
		n.parent = root;
		return root;
	}
	
	private Node<T> union(Node<T> a, Node<T> b) {
		if(a == b)
			return a;
		
		if(a.rank < b.rank) {
			a.parent = b;
			a = b;
		}
		else if(a.rank > b.rank)
			b.parent = a;
		else {
			b.parent = a;
			a.rank++;
		}
		return a;
	}
	
	
	public Collection<List<T>> partition() {
		Map<Node<T>,List<T>> result = new HashMap<Node<T>,List<T>>();
		
		
		for(Node<T> n : nodes.values()) {
			Node<T> root = findRoot(n);
			List<T> l = result.get(root);
			if(l == null) {
				l = new ArrayList<T>();
				result.put(root, l);
			}
			l.add(n.x);
		}
		
		return result.values();
	}
	
}
