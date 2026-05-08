package de.learnlib.ralib.example.palindrome;

import static de.learnlib.ralib.example.palindrome.PalindromeOracle.IN;
import static de.learnlib.ralib.example.palindrome.PalindromeOracle.TYPE;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.word.WordBuilder;

public class PalindromeGenerator {

	/**
	 * Tree structure for compactly specifying {@link RegisterAutomaton} instances with equality.
	 * Nodes in the tree are converted to locations, edges to transitions.
	 */
	public static class Node {
		private Integer maxMemV;
		private BiMap<Integer, Node> children;
		private Node parent;
		private boolean accept;

		public Node(Node parent, int maxMemV) {
			this.parent = parent;
			this.maxMemV = maxMemV;
			this.children = HashBiMap.create();
		}

		public List<Integer> getPrefix() {
			Node parent = this.parent, child = this;
			List<Integer> prefix = new LinkedList<Integer>();
			while (parent != null) {
				Integer trans = parent.children.inverse().get(child);
				prefix.addFirst(trans);
				child = parent;
				parent = parent.parent;
			}
			return prefix;
		}

		public void setAccept(boolean accept) {
			this.accept = accept;
		}

		public Node createChildIfAbsent(Integer edge) {
			Node child = children.get(edge);
			if (child == null) {
				child = new Node(this, maxMemV);
				children.put(edge, child);
			}
			return child;
		}

		public Integer determineMaxDownstreamEdge() {
			Integer maxEdge = 0;
			for (Entry<Integer, Node> entry : children.entrySet()) {
				maxEdge = Math.max(maxEdge, entry.getValue().determineMaxDownstreamEdge());
				maxEdge = Math.max(maxEdge, entry.getKey());
			}
			return maxEdge;
		}

		private Integer determineMaxMemV () {
				Integer futureMaxMemV = this.determineMaxDownstreamEdge();
				for (Entry<Integer, Node> entry : children.entrySet()) {
					entry.getValue().determineMaxMemV();
				}
				Integer pastMaxMemV = parent == null? 0 : Collections.max(this.getPrefix());
				maxMemV = Math.min(futureMaxMemV, pastMaxMemV);
				return maxMemV;
		}

		public Integer getMaxMemV() {
			return maxMemV;
		}

		public String toString() {
			return toString(0, 2);
		}

		public Map<Integer, Node> getChildren() {
			return children;
		}

		public boolean getAccept() {
			return accept;
		}

		public String toString(int indent, int indentIncrease) {
			StringBuilder builder = new StringBuilder();
			builder.append(getPrefix()).
			append(", maxMemV: ").append(maxMemV).append(", acc: ").append(accept).append("{").append(System.lineSeparator());
			for (Entry<Integer, Node> entry : children.entrySet()) {
				builder.repeat(" ", indent).append(entry.getKey()).append(" -> ").append(entry.getValue()
						.toString(indent + indentIncrease, indentIncrease));
			}
			builder.append("}");
			return builder.toString();
		}
	}


	static record WorkItem (Node node) {};


	private static boolean isPalindrome(List<Integer> word) {
		for (int i=0; i<word.size()/2; i++) {
			if (word.get(i) != word.get(word.size() - (i + 1))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Converts a tree with root {@code root} into a corresponding {@link RegisterAutomaton} with alphabet containing only {@code in}.
	 *
	 * @param root root {@link Node} of the tree
	 * @param in the (only) symbol used in the constructed RegisterAutomaton
	 * @return the constructed RegisterAutomaton
	 */
	private static RegisterAutomaton convertToRA(Node root, ParameterizedSymbol in) {
		MutableRegisterAutomaton ra = new MutableRegisterAutomaton();
		DataType type = in.getPtypes()[0];
		Queue<Node> q = new ArrayDeque<Node>();
		Map<Node, RALocation> map = new HashMap<>();
		q.add(root);
		map.put(root, ra.addState(root.getAccept()));
		ra.setInitialState(map.get(root));
		RALocation sink = ra.addState(false);
		while (!q.isEmpty()) {
			Node srcNode = q.poll();
			RALocation srcLoc = map.get(srcNode);

			// create srcNode registers and parameter
			SymbolicDataValueGenerator.RegisterGenerator srcRgen = new SymbolicDataValueGenerator.RegisterGenerator();
			Register [] srcRegs = new Register [srcNode.getMaxMemV()];
			for (int i=0; i<srcNode.getMaxMemV(); i ++) {
				srcRegs[i] = srcRgen.next(type);
			}
			SymbolicDataValueGenerator.ParameterGenerator pgen = new SymbolicDataValueGenerator.ParameterGenerator();
			Parameter param = pgen.next(type);
			List<Register> eqRegs = new ArrayList<>();
			for (int edge=1; edge<=srcNode.getMaxMemV() + 1; edge++) {
				Node destNode = srcNode.getChildren().get(edge);
				if (destNode == null) {
					continue;
				}
				RALocation destLoc = ra.addState(destNode.getAccept());
				map.put(destNode, destLoc);

				// create assignment
				SymbolicDataValueGenerator.RegisterGenerator destRgen = new SymbolicDataValueGenerator.RegisterGenerator();
				VarMapping<SymbolicDataValue.Register, SymbolicDataValue> assignmentMapping = new VarMapping<>();
				for (int i=1; i<=srcNode.getMaxMemV(); i++) {
					if (destNode.getMaxMemV() >= i) {
						assignmentMapping.put(destRgen.next(type), srcRegs[i-1]);
					}
				}
				if (destNode.getMaxMemV() > srcNode.getMaxMemV()) {
					assignmentMapping.put(destRgen.next(type), param);
				}

				// create guard
				Expression<Boolean> guard = null;
				if (edge <= srcNode.getMaxMemV()) { // equality case
					guard = new NumericBooleanExpression(srcRegs[edge-1], NumericComparator.EQ, param);
					// update list of registers over which we have equality
					eqRegs.add(srcRegs[edge-1]);
				} else { // fresh case
					List<Expression<Boolean>> conjuncts = new ArrayList<>(srcRegs.length);
					for (Register r : eqRegs) {
						conjuncts.add(new NumericBooleanExpression(r, NumericComparator.NE, param));
					}
					guard = ExpressionUtil.and(conjuncts);
				}

				Transition transition = new Transition(in, guard, srcLoc, destLoc, new Assignment(assignmentMapping));
				ra.addTransition(srcLoc, in, transition);
				q.add(destNode);
			}

			// add sink transition to sink
			if (!srcNode.getChildren().containsKey(srcNode.getMaxMemV()+1)) {
				VarMapping<SymbolicDataValue.Register, SymbolicDataValue> assignmentMapping = new VarMapping<>();
				// create guard for the fresh case
				List<Expression<Boolean>> conjuncts = new ArrayList<>(srcRegs.length);
				for (Register r : eqRegs) {
					conjuncts.add(new NumericBooleanExpression(r, NumericComparator.NE, param));
				}
				Expression<Boolean> guard = ExpressionUtil.and(conjuncts);
				Transition transition = new Transition(in, guard, srcLoc, sink, new Assignment(assignmentMapping));
				ra.addTransition(srcLoc, in, transition);
			}
		}

		// add sink self-loop transition
		VarMapping<SymbolicDataValue.Register, SymbolicDataValue> assignmentMapping = new VarMapping<>();
		Expression<Boolean> guard = ExpressionUtil.TRUE;
		Transition transition = new Transition(in, guard, sink, sink, new Assignment(assignmentMapping));
		ra.addTransition(sink, in, transition);

		return ra;
	}

	/**
	 * Returns a {@link RegisterAutomaton} of a language which accepts palindromes up to a length {code maxLen}.
	 * @param maxLen The maximum length of the palindromes
	 * @return the constructed RegisterAutomaton
	 */
	public static RegisterAutomaton generate(int maxLen) {
		Node root = new Node(null, 0);
		Queue<WorkItem> q = new ArrayDeque<WorkItem>();
		q.add(new WorkItem(root));
		while (!q.isEmpty()) {
			WorkItem i = q.poll();
			Node node = i.node;
			List<Integer> p = node.getPrefix();
			if (2*p.size() <= maxLen) {
				Node last = node;
				for (int idx = p.size()-1; idx>=0; idx--) {
					last = last.createChildIfAbsent(p.get(idx));
					q.add(new WorkItem(last));
				}
				last.setAccept(true);
				if (2*p.size() + 1 <= maxLen) {
					Integer maxVal = p.stream().max((i1, i2) -> i1.compareTo(i2)).orElse(0);
					for (int middleVal = 1; middleVal <= maxVal+1; middleVal++) {
						last = node.createChildIfAbsent(middleVal);
							q.add(new WorkItem(last));
							for (int idx = p.size()-1; idx>=0; idx--) {
								last = last.createChildIfAbsent(p.get(idx));
								q.add(new WorkItem(last));
							}
							last.setAccept(true);
					}
				}
			}
		}
		root.determineMaxMemV();
		RegisterAutomaton ra = convertToRA(root, IN);
		return ra;
	}

	private static int TEST_MAX_LEN = 8;
	/*
	 *  Tests generator for numbers up to TEST_MAX_LEN size. Note that  this method scales badly with increasing TEST_MAX_LEN.
	 */

	@Test(enabled=false)
	public void testPalindromeGenerator() {
		RegisterAutomaton ra = generate(TEST_MAX_LEN);
		Queue<List<Integer>> q = new ArrayDeque<>();
		q.add(Collections.emptyList());
		while (!q.isEmpty()) {
			List<Integer> valWord = q.poll();
			WordBuilder<PSymbolInstance> wb = new WordBuilder<PSymbolInstance>();
			valWord.forEach(d ->
			wb.add(new PSymbolInstance(IN,
					new DataValue(TYPE, BigDecimal.valueOf(d)))));
			Assert.assertEquals(ra.accepts(wb.toWord()), isPalindrome(valWord), "Mismatch for word " + valWord);
			if (valWord.size() < TEST_MAX_LEN) {
				if (valWord.isEmpty()) {
					q.add(Arrays.asList(1));
				} else {
					Integer maxVal = valWord.stream().max((i1, i2) -> i1.compareTo(i2)).get();
					for (int i=1; i<= maxVal+1; i++) {
						List<Integer> next = new ArrayList<>(valWord);
						next.add(i);
						q.add(next);
					}
				}
			}
		}
	}
}
