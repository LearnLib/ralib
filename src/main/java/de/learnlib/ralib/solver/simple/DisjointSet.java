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
