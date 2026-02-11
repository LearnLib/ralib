package de.learnlib.ralib.ct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.ParameterValuation;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.util.RemappingIterator;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator.ParameterGenerator;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.mto.OptimizedSymbolicSuffixBuilder;
import de.learnlib.ralib.oracles.mto.SLLambdaRestrictionBuilder;
import de.learnlib.ralib.oracles.mto.SymbolicSuffixRestrictionBuilder;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.smt.ReplacingValuesVisitor;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import gov.nasa.jpf.constraints.api.Expression;
import net.automatalib.word.Word;

/**
 * Data structure for a classification tree. Implements methods for sifting new prefixes into the tree
 * and refining the tree with additional symbolic suffixes, as well as closedness and consistency checks.
 *
 * @author fredrik
 */
public class ClassificationTree {
	private final CTInnerNode root;

	private final Map<Word<PSymbolInstance>, CTLeaf> prefixes;
	private final Set<Word<PSymbolInstance>> shortPrefixes;

	private final ConstraintSolver solver;
	private final TreeOracle oracle;
	private final SymbolicSuffixRestrictionBuilder restrBuilder;
	private final OptimizedSymbolicSuffixBuilder suffixBuilder;

	private final ParameterizedSymbol[] inputs;
	private final List<SymbolicSuffix> outputs;

	private final Constants consts;

	private boolean ioMode;

	public ClassificationTree(TreeOracle oracle,
			ConstraintSolver solver,
			SymbolicSuffixRestrictionBuilder restrBuilder,
			OptimizedSymbolicSuffixBuilder suffixBuilder,
			Constants consts,
			boolean ioMode,
			ParameterizedSymbol ... inputs) {
		this.oracle = oracle;
		this.solver = solver;
		this.ioMode = ioMode;
		this.inputs = inputs;
		this.restrBuilder = restrBuilder;
		this.suffixBuilder = suffixBuilder;
		this.consts = consts;

		prefixes = new LinkedHashMap<>();
		shortPrefixes = new LinkedHashSet<>();
		outputs = outputSuffixes(inputs);

		root = new CTInnerNode(null, RaStar.EMPTY_SUFFIX);
	}

	public Set<CTLeaf> getLeaves() {
		return new LinkedHashSet<>(prefixes.values());
	}

	public CTLeaf getLeaf(Word<PSymbolInstance> u) {
		return prefixes.get(u);
	}

	public Set<Word<PSymbolInstance>> getPrefixes() {
		return new LinkedHashSet<>(prefixes.keySet());
	}

	public Set<ShortPrefix> getShortPrefixes() {
		Set<ShortPrefix> sp = new LinkedHashSet<>();
		for (CTLeaf leaf : getLeaves()) {
			sp.addAll(leaf.getShortPrefixes());
		}
		assert sp.size() == shortPrefixes.size();
		assert sp.containsAll(shortPrefixes);
		return sp;
	}

	/**
	 * Get all one-symbol extensions of prefix {@code u}.
	 *
	 * @param u
	 * @return set of prefixes which are one-symbol extensions of {@code u}
	 */
	public Set<Prefix> getExtensions(Word<PSymbolInstance> u) {
		Set<Prefix> extensions = new LinkedHashSet<>();
		for (ParameterizedSymbol action : inputs) {
			extensions.addAll(getExtensions(u, action)
					.stream()
					.map(w -> getLeaf(w).getPrefix(w))
					.collect(Collectors.toList()));
		}
		return extensions;
	}

	/**
	 * Get the sink node of the classification tree (IO mode only). The sink node of an IO classification
	 * tree is the leaf node in the rejecting branch of the tree (i.e., the branch for the empty symbolic suffix
	 * which is rejecting. Note that in IO mode, there should only be one rejecting leaf.
	 *
	 * @return an {@code Optional} containing the sink node if one exists, or an empty {@code Optional} otherwise
	 */
	public Optional<CTLeaf> getSink() {
		if (!ioMode) {
			return Optional.empty();
		}

		for (CTBranch branch : root.getBranches()) {
			if (!branch.getRepresentativePath().isAccepting()) {
				CTNode node = branch.getChild();
				while (!node.isLeaf()) {
					CTInnerNode inner = (CTInnerNode) node;
					List<CTBranch> children = inner.getBranches();
					if (children.isEmpty()) {
						return Optional.empty();
					}
					node = children.stream().findFirst().get().getChild();
				}
				return Optional.of((CTLeaf) node);
			}
		}
		return Optional.empty();
	}

	/**
	 * Get all one-symbol {@code action}-extensions of prefix {@code u}.
	 *
	 * @param u
	 * @param action
	 * @return set of prefixes which are one-symbol extensions of {@code u} with symbol {@code action}
	 */
	public Set<Word<PSymbolInstance>> getExtensions(Word<PSymbolInstance> u, ParameterizedSymbol action) {
		return prefixes.keySet()
				.stream()
				.filter(w -> w.length() == u.length() + 1)
				.filter(w -> w.prefix(w.length() - 1).equals(u) && w.lastSymbol().getBaseSymbol().equals(action))
				.collect(Collectors.toSet());
	}

	/**
	 * Initialize the classification tree by sifting the empty prefix.
	 */
	public void initialize() {
		sift(RaStar.EMPTY_PREFIX);
	}

	///////////////////////////////////////////
	// OPERATIONS ON THE CLASSIFICATION TREE //
	///////////////////////////////////////////

	/**
	 * Sift prefix into the tree. If prefix sifts to a new leaf, it becomes the representative prefix
	 * for that leaf.
	 *
	 * @param u prefix to sift
	 * @return the leaf into which {@code u} has been sifted
	 */
	public CTLeaf sift(Word<PSymbolInstance> u) {
		Prefix prefix = new Prefix(u, new CTPath(ioMode));
		CTLeaf leaf = root.sift(prefix, oracle, solver, ioMode);
		prefixes.put(u, leaf);
		return leaf;
	}

	/**
	 * Expands a prefix by turning it into a short prefix. The new short prefix will have
	 * branching information initialized from the initial guards of the conjunction of all its SDTs.
	 *
	 * @param u the prefix to be expanded
	 */
	public void expand(Word<PSymbolInstance> u) {
		CTLeaf leaf = prefixes.get(u);
		// sift u into leaf, if not already present
		if (leaf == null) {
			leaf = sift(u);
		}
		ShortPrefix prefix = leaf.elevatePrefix(u, oracle, inputs);
		shortPrefixes.add(u);

		// sift one-symbol extensions of u
		for (ParameterizedSymbol ps : inputs) {
			Branching b = prefix.getBranching(ps);
			for (Word<PSymbolInstance> ua : b.getBranches().keySet()) {
				CTLeaf l = sift(ua);
				prefixes.put(ua, l);
			}
		}
	}

	/**
	 * Refine a {@code leaf} by adding a new inner node with above it. After the new inner node is added,
	 * all prefixes of {@code leaf} will be sifted into the classification tree with a call to
	 * {@link ClassificationTree#sift(Word)}. Any short prefix in {@code leaf} will have its branching
	 * updated.
	 *
	 * @param leaf the leaf to refine
	 * @param suffix the symbolic suffix to be contained within the new inner node
	 */
	public void refine(CTLeaf leaf, SymbolicSuffix suffix) {
		CTInnerNode parent = (CTInnerNode) leaf.getParent();
		assert parent != null;
		Map<Word<PSymbolInstance>, CTLeaf> leaves = parent.refine(leaf, suffix, oracle, solver, ioMode, inputs);
		prefixes.putAll(leaves);

		// make sure all short prefixes are still short (this may not be the case if a new leaf was created)
		for (Word<PSymbolInstance> sp : shortPrefixes) {
			CTLeaf l = prefixes.get(sp);
			Prefix p = l.getPrefix(sp);
			if (!(p instanceof ShortPrefix)) {
				l.elevatePrefix(sp, oracle, inputs);
			}
		}
	}

	///////////////////////
	// CLOSEDNESS CHECKS //
	///////////////////////

	/**
	 * Checks for output closedness, i.e., whether a symbolic suffix for each output symbol is
	 * present for each leaf. If not output closed, add one missing output suffix as a new inner
	 * node with a call to {@link ClassificationTree#refine(CTLeaf, SymbolicSuffix)}.
	 *
	 * @return {@code true} if output closed
	 */
	public boolean checkOutputClosed() {
		if (!ioMode) {
			return true;
		}
		return checkOutputClosed(root);
	}

	private boolean checkOutputClosed(CTNode node) {
		if (node.isLeaf()) {
			CTLeaf leaf = (CTLeaf) node;
			for (SymbolicSuffix v : outputs) {
				if (!leaf.getSuffixes().contains(v)) {
					refine(leaf, v);
					return false;
				}
			}
			return true;
		} else {
			CTInnerNode n = (CTInnerNode) node;
			for (CTBranch b : n.getBranches()) {
				if (!checkOutputClosed(b.getChild())) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Checks for location closedness, i.e., whether each leaf has at least one short prefix. If
	 * not location closed, this method expands, with a call to {@link ClassificationTree#expand(Word)},
	 * the representative prefix of one leaf which does not have a short prefix.
	 *
	 * @return {@code true} if location closed
	 */
	public boolean checkLocationClosedness() {
		for (CTLeaf leaf : getLeaves()) {
			if (leaf.getShortPrefixes().isEmpty()) {
				expand(leaf.getRepresentativePrefix());
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks for transition closedness, i.e., whether each short prefix has a one-symbol extension
	 * for each guard. If not transition closed, one new one-symbol prefix will be sifted for one
	 * short prefix missing an extension.
	 *
	 * @return {@code true} if transition closed
	 */
	public boolean checkTransitionClosedness() {
		for (CTLeaf leaf : getLeaves()) {
			for (ShortPrefix u : leaf.getShortPrefixes()) {
				for (ParameterizedSymbol a : inputs) {
					for (Word<PSymbolInstance> ua : u.getBranching(a).getBranches().keySet()) {
						if (!prefixes.containsKey(ua)) {
							sift(ua);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Checks for register closedness, i.e., whether for each short prefix {@code u}, the memorable parameters
	 * of {@code u} contain all the memorable parameters of its one-symbol extensions {@code ua(d)}. If not register
	 * closed, a new symbolic suffix {@code av}, formed by prepending the symbol {@code a} of {@code ua(d)} with a
	 * symbolic suffix {@code v} which reveals a missing memorable parameter of {@code ua(d)}, will be added to
	 * the leaf of {@code u}. Note that only one new symbolic suffix will be added, even if there are multiple
	 * short prefixes for which a memorable parameter is missing.
	 *
	 * @return {@code true} if register closed
	 */
	public boolean checkRegisterClosedness() {
		for (Map.Entry<Word<PSymbolInstance>, CTLeaf> e : prefixes.entrySet()) {
			Word<PSymbolInstance> ua = e.getKey();
			CTLeaf leaf = e.getValue();
			if (ua.length() < 1) {
				continue;
			}
			Word<PSymbolInstance> u = ua.prefix(ua.size() - 1);
			Prefix u_pref = getLeaf(u).getPrefix(u);
			Prefix ua_pref = leaf.getPrefix(ua);
			CTLeaf u_leaf = prefixes.get(u);

			Set<DataValue> ua_mem = leaf.getPrefix(ua).getRegisters();
			Set<DataValue> u_mem = prefixes.get(u).getPrefix(u).getRegisters();
//			Set<DataValue> a_mem = actionRegisters(ua);

			Set<DataValue> missingRegs = missingMemorable(ua_mem, u_mem, u);
			if (!missingRegs.isEmpty()) {
				for (SymbolicSuffix v : leaf.getSuffixes()) {
					Set<DataValue> v_mem = ua_pref.getSDT(v).getDataValues();
					Set<DataValue> vMissingRegs = missingMemorable(v_mem, u_mem, u);
					if (!vMissingRegs.isEmpty()) {
						// found a suffix with missing memorables
						SymbolicSuffix av = extendSuffixRegister(ua, v, vMissingRegs);
						SDT sdt = oracle.treeQuery(u, av.relabel(u_pref.getRpBijection().inverse().toVarMapping()));
						if (Collections.disjoint(vMissingRegs, sdt.getDataValues())) {
							continue;
						}
						refine(u_leaf, av);
						return false;
					}
				}
			}

//			if (!consistentMemorable(ua_mem, u_mem, a_mem)) {
//				// memorables are missing, find suffix which reveals missing memorables
//				boolean found = false;
//				for (SymbolicSuffix v : leaf.getSuffixes()) {
//					Set<DataValue> s_mem = ua_pref.getSDT(v).getDataValues();
//					if (!consistentMemorable(s_mem, u_mem, a_mem)) {
//						DataValue[] missingRegs = missingRegisters(s_mem, u_mem, a_mem);   // registers to not optimize away
//						SymbolicSuffix av = extendSuffixRegister(ua, v, missingRegs);
//
////						SDT sdt = oracle.treeQuery(u, av);
////						if (Collections.disjoint(Set.of(missingRegs), sdt.getDataValues())) {
////							continue;
////						}
//						refine(u_leaf, av);
////						found = true;
////						break;
//						return false;
//					}
//				}
////				if (found) {
////					return false;
////				}
//			}
		}
		return true;
	}

	////////////////////////
	// CONSISTENCY CHECKS //
	////////////////////////

	/**
	 * Checks for location consistency. This property states that, for each pair of short prefixes {@code u} and {@code v},
	 * each one-symbol extension {@code ua(d)} and {@code va(d)} of {@code u} and {@code v}, corresponding to the same
	 * guard, is in the same leaf. If the classification tree is not location consistent, this method refines the tree with a
	 * new symbolic suffix formed by prepending the symbolic suffix of the lowest common ancestor of {@code u} and {@code v}
	 * by the symbol {@code a} of the one-symbol extensions revealing the inconsistency. Note that only one location inconsistency
	 * will be resolved by this method. To resolve multiple inconsistencies, call the method multiple times.
	 *
	 * @return {@code true} if location consistent
	 */
	public boolean checkLocationConsistency() {
		for (CTLeaf l : getLeaves()) {
			Iterator<ShortPrefix> sp = l.getShortPrefixes().iterator();
			if (!sp.hasNext()) {
				continue;
			}
			ShortPrefix u = sp.next();
			while (sp.hasNext()) {
				ShortPrefix uOther = sp.next();
				for (ParameterizedSymbol action : inputs) {
					// loop over one-symbol extensions
					for (Map.Entry<Word<PSymbolInstance>, Expression<Boolean>> uEntry : u.getBranching(action).getBranches().entrySet()) {
						// rename guard to make parameters consistent with RP
						Word<PSymbolInstance> uExtension = uEntry.getKey();
						Expression<Boolean> uGuard = uEntry.getValue();
						Bijection<DataValue> uRenaming = u.getRpBijection().compose(uOther.getRpBijection().inverse());

						ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
						Expression<Boolean> uGuardRenamed = rvv.apply(uGuard, uRenaming.toVarMapping());

						// find equivalent one-symbol extension for RP
						Optional<Word<PSymbolInstance>> uOtherExtension = uOther.getBranching(action).getPrefix(uGuardRenamed, solver);
						assert uOtherExtension.isPresent();

						CTLeaf uExtLeaf = getLeaf(uExtension);
						CTLeaf uOtherExtLeaf = getLeaf(uOtherExtension.get());
						if (uExtLeaf != uOtherExtLeaf) {
							// inconsistent, refine leaf with extended suffix
							SymbolicSuffix v = lca(uExtLeaf, uOtherExtLeaf).getSuffix();
							SymbolicSuffix av = extendSuffixLocation(uExtension, uOtherExtension.get(), v);
							refine(l, av);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Checks for transition consistency. There are two types of transition consistency, (a) and (b).
	 * This method checks for both. The transition consistency (a) property states that all one-symbol
	 * extensions {@code ua(d)} of a prefix {@code u} which satisfy the same guard are in the same leaf.
	 * Transition consistency (b) states that all one-symbol extensions {@code ua(d)} of a prefix {@code u}
	 * that satisfy the same guard and are in the same leaf should have equivalent SDTs under identity renaming.
	 * If either property is not satisfied, the classification tree will be refined by adding a new inner
	 * node to the leaf of {@code u} with a symbolic suffix formed by prepending the symbolic suffix
	 * revealing the inconsistency by the symbol {@code a} of the one-symbol extension.
	 * Note that only one inconsistency will be resolved. Multiple inconsistencies can be resolved through
	 * multiple calls to this method.
	 *
	 * @return {@code true} if transition consistent
	 */
	public boolean checkTransitionConsistency() {
		for (ShortPrefix u : getShortPrefixes()) {
			for (ParameterizedSymbol action : inputs) {
				Set<Word<PSymbolInstance>> extensions = getExtensions(u, action);
				for (Map.Entry<Word<PSymbolInstance>, Expression<Boolean>> e : u.getBranching(action).getBranches().entrySet()) {
					Word<PSymbolInstance> uElse = e.getKey();
					Expression<Boolean> g = e.getValue();
					for (Word<PSymbolInstance> uIf : extensions) {
						if (uIf.equals(uElse)) {
							continue;
						}

						// check if guard for uA is satisfiable under mapping of uB
						Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
						mapping.putAll(actionValuation(uIf));
						mapping.putAll(consts);
						if (solver.isSatisfiable(g, mapping)) {
							// check transition consistency A
							Optional<SymbolicSuffix> av = transitionConsistentA(uIf, uElse);
							if (av.isEmpty()) {
								// check transition consistency B
								av = transitionConsistentB(uIf, uElse);
							}
							if (av.isPresent()) {
								refine(getLeaf(u), av.get());
								return false;
							}
						}
					}
				}
			}
		}
		return true;
	}

	private Optional<SymbolicSuffix> transitionConsistentA(Word<PSymbolInstance> uIf, Word<PSymbolInstance> uElse) {
		Word<PSymbolInstance> u = uIf.prefix(uIf.length() - 1);
		CTLeaf uALeaf = getLeaf(uIf);
		CTLeaf uBLeaf = getLeaf(uElse);
		if (uALeaf != uBLeaf) {
			CTLeaf uLeaf = getLeaf(u);
			assert uLeaf != null : "Prefix is not short: " + u;
			SymbolicSuffix v = lca(uALeaf, uBLeaf).getSuffix();
			SymbolicSuffix av = extendSuffixTransition(uIf, uElse, v);
			return Optional.of(av);
		}
		return Optional.empty();
	}

	private Optional<SymbolicSuffix> transitionConsistentB(Word<PSymbolInstance> uIf, Word<PSymbolInstance> uElse) {
		Prefix pA = getLeaf(uIf).getPrefix(uIf);
		Prefix pB = getLeaf(uElse).getPrefix(uElse);
		for (SymbolicSuffix v : getLeaf(uElse).getSuffixes()) {
			SDT sdtA = pA.getSDT(v).toRegisterSDT(uIf, consts);
			SDT sdtB = pB.getSDT(v).toRegisterSDT(uElse, consts);
			if (!SDT.equivalentUnderId(sdtA, sdtB)) {
				CTLeaf uLeaf = getLeaf(uIf.prefix(uIf.length() - 1));
				assert uLeaf != null;

				// find registers that should not be removed through optimization
//				Register[] regs = inequivalentMapping(rpRegBijection(pA.getRpBijection(), pA), rpRegBijection(pB.getRpBijection(), pB));
//				DataValue[] regVals = regsToDvs(regs, uA);
//
//				SymbolicSuffix av = extendSuffix(uA, v, regVals);
				SymbolicSuffix av = extendSuffixTransition(uIf, uElse, v);
				if (suffixRevealsNewGuard(av, getLeaf(uIf.prefix(uIf.length() - 1)))) {
					return Optional.of(av);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Checks for register consistency. This property states that if there are any symmetries between
	 * memorable parameters of any prefix, then these symmetries must be present also in its
	 * one-symbol extensions. If this property does not hold for some prefix {@code u}, a new inner
	 * node will be added to break the symmetry of {@code u}. This node will contain a symbolic suffix
	 * formed by prepending the symbolic suffix breaking the symmetry in the one-symbol extension
	 * {@code ua(d)} by the symbol {@code a}. Not that only one inconsistency will be resolved in this
	 * manner. Multiple inconsistencies should be resolved with multiple calls to this method.
	 *
	 * @return {@code true} if register consistent
	 */
	public boolean checkRegisterConsistency() {
		for (Prefix u : getShortPrefixes()) {
			if (u.length() < 2) {
				continue;
			}
			for (ParameterizedSymbol action : inputs) {
				Iterator<Prefix> extensions = getExtensions(u, action)
						.stream()
						.map(w -> getLeaf(w).getPrefix(w))
						.iterator();
				while (extensions.hasNext()) {
					Prefix uExtended = extensions.next();

					// loop over all bijections exhibiting symmetry
					RemappingIterator<DataValue> rit = new RemappingIterator<>(u.getRegisters(), u.getRegisters());
					for (Bijection<DataValue> gamma : rit) {
						CTPath uPath = u.getPath();
						if (uPath.isEquivalent(uPath, gamma, solver)) {
							// u exhibits symmetry under gamma
							for (Map.Entry<SymbolicSuffix, SDT> e : uExtended.getPath().getSDTs().entrySet()) {
								SymbolicSuffix v = e.getKey();
								SDT uaSDT = e.getValue();
								if (SDT.equivalentUnderBijection(uaSDT, uaSDT, gamma) == null) {
									// one-symbol extension uExtended does not exhibit symmetry under gamma
//									DataValue[] regs = gamma.keySet().toArray(new DataValue[gamma.size()]);
//									SymbolicSuffix av = extendSuffixRegister(uExtended, v, regs);
									SymbolicSuffix av = new SymbolicSuffix(DataWords.concatenate(Word.fromSymbols(uExtended.lastSymbol().getBaseSymbol()), v.getActions()));
									refine(getLeaf(u), av);
									return false;
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	////////////////////
	// HELPER METHODS //
	////////////////////

	/**
	 *
	 * @param n1
	 * @param n2
	 * @return lowest common ancestor of {@code n1} and {@code n2}
	 */
	private CTInnerNode lca(CTNode n1, CTNode n2) {
		if (n1 == n2) {
			if (n1.isLeaf()) {
				return (CTInnerNode) n1.getParent();
			}
			return (CTInnerNode) n1;
		}
		int h1 = height(n1);
		int h2 = height(n2);

		return lca(n1, h1, n2, h2);
	}

	private CTInnerNode lca(CTNode n1, int h1, CTNode n2, int h2) {
		if (n1 == n2) {
			assert n1 instanceof CTInnerNode;
			return (CTInnerNode) n1;
		}
		if (h1 < h2) {
			return lca(n1, h1, n2.getParent(), h2 - 1);
		}
		if (h1 > h2) {
			return lca(n1.getParent(), h1 - 1, n2, h2);
		}
		return lca(n1.getParent(), h1 - 1, n2.getParent(), h2 - 1);
	}

	private int height(CTNode n) {
		int h = 0;
		while (n.getParent() != null) {
			h++;
			n = n.getParent();
		}
		return h;
	}

	private Set<DataValue> missingMemorable(Set<DataValue> ua_mem, Set<DataValue> u_mem, Word<PSymbolInstance> u) {
		Set<DataValue> uVals = new LinkedHashSet<>();
		uVals.addAll(Arrays.asList(DataWords.valsOf(u)));
		Set<DataValue> diff = new LinkedHashSet<>(ua_mem);
		diff.removeAll(u_mem);
		uVals.removeAll(u_mem);
		return Sets.intersection(uVals, diff);
	}

	/**
	 *
	 * @param ua_mem
	 * @param u_mem
	 * @param a_mem
	 * @return {@code true} if {@code ua_mem} contains all of {@code u_mem} and {@code a_mem}
	 */
	private boolean consistentMemorable(Set<DataValue> ua_mem, Set<DataValue> u_mem, Set<DataValue> a_mem) {
		Set<DataValue> union = new LinkedHashSet<>();
		union.addAll(u_mem);
		union.addAll(a_mem);
		return union.containsAll(ua_mem);
	}

	/**
	 * @param ua
	 * @return the set of data values in the last symbol instance of {@code ua}
	 */
	private Set<DataValue> actionRegisters(Word<PSymbolInstance> ua) {
		int ua_arity = DataWords.paramLength(DataWords.actsOf(ua));
		int u_arity = ua_arity - ua.lastSymbol().getBaseSymbol().getArity();
		DataValue[] vals = DataWords.valsOf(ua);

		Set<DataValue> regs = new LinkedHashSet<>();
		for (int i = u_arity; i < ua_arity; i++) {
			regs.add(vals[i]);
		}
		return regs;
	}

	/**
	 *
	 * @param s_mem
	 * @param u_mem
	 * @param a_mem
	 * @return an array containing the data values of {@code s_mem} not contained in either {@code u_mem} or {@code a_mem}
	 */
	private DataValue[] missingRegisters(Set<DataValue> s_mem, Set<DataValue> u_mem, Set<DataValue> a_mem) {
		Set<DataValue> union = new LinkedHashSet<>(u_mem);
		union.addAll(a_mem);
		Set<DataValue> difference = new LinkedHashSet<>(s_mem);
		difference.removeAll(union);
		return difference.toArray(new DataValue[difference.size()]);
	}

	/**
	 * Form a new symbolic suffix by prepending {@code v} by the last symbol of {@code ua},
	 * using suffix optimization.
	 *
	 * @param ua
	 * @param v
	 * @param missingRegs the register which should not be removed through suffix optimizations
	 * @return the last symbol of {@code ua} concatenated with {@code v}
	 */
	private SymbolicSuffix extendSuffixRegister(Word<PSymbolInstance> ua, SymbolicSuffix v, Set<DataValue> missingRegs) {
		Word<PSymbolInstance> u = ua.prefix(ua.length() - 1);
		if (suffixBuilder == null) {
			PSymbolInstance a = ua.lastSymbol();
			SymbolicSuffix alpha = new SymbolicSuffix(u, Word.fromSymbols(a), restrBuilder);
			return alpha.concat(v);
		}

		SDT u_sdt = prefixes.get(ua).getPrefix(ua).getSDT(v);
		assert u_sdt != null : "SDT for symbolic suffix " + v + " does not exist for prefix " + ua;

		if (restrBuilder instanceof SLLambdaRestrictionBuilder sllambdaRestrBuilder) {
			Prefix uPref = getLeaf(u).getPrefix(u);
			Prefix uExtPref = getLeaf(ua).getPrefix(ua);
			Prefix uRepr = getLeaf(u).getRepresentativePrefix();
			return sllambdaRestrBuilder.extendSuffix(uPref, uExtPref, uRepr, v, u_sdt, missingRegs);
		}

		return suffixBuilder.extendSuffix(ua, u_sdt, v, missingRegs.toArray(new DataValue[missingRegs.size()]));
	}

	/**
	 * Perform a tree query for the representative prefix of {@code leaf} with {@code av} to check
	 * whether {@code av} reveals additional guards.
	 *
	 * @param av
	 * @param leaf
	 * @return {@code true} if {@code av} reveals additional guards
	 */
	private boolean suffixRevealsNewGuard(SymbolicSuffix av, CTLeaf leaf) {
		assert !leaf.getShortPrefixes().isEmpty() : "No short prefix in leaf " + leaf;
		ShortPrefix u = leaf.getShortPrefixes().iterator().next();
		SDT sdt = oracle.treeQuery(u, av.relabel(u.getRpBijection().inverse().toVarMapping()));
		ParameterizedSymbol a = av.getActions().firstSymbol();
		Branching branching = u.getBranching(a);
		Branching newBranching = oracle.updateBranching(u, a, branching, sdt);
		for (Expression<Boolean> guard : newBranching.getBranches().values()) {
			if (!branching.getBranches().values().contains(guard)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Convert {@code Bijection<DataValue>} to {@code Bijection<Register>} using the
	 * data values of {@code prefix} to determine register ids.
	 *
	 * @param bijection
	 * @param prefx
	 * @return
	 */
	private Bijection<Register> rpRegBijection(Bijection<DataValue> bijection, Word<PSymbolInstance> prefx) {
		return Bijection.dvToRegBijection(bijection, prefx, getLeaf(prefx).getRepresentativePrefix());
	}

	/**
	 * Convert array of {@code Register} to array of {@code DataValue} by matching {@link Register#getId()}
	 * values to data value positions in {@code prefix}.
	 *
	 * @param regs
	 * @param prefix
	 * @return
	 */
	private DataValue[] regsToDvs(Register[] regs, Word<PSymbolInstance> prefix) {
		DataValue[] vals = DataWords.valsOf(prefix);
		DataValue[] ret = new DataValue[regs.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = vals[regs[i].getId()-1];
		}
		return ret;
	}

	/**
	 * Form a {@code SymbolicSuffix} by prepending {@code v} by the last symbol of {@code u1} and {@code u2}.
	 * The new suffix will be optimized for separating {@code u1} and {@code u2}.
	 * Note that {@code u1} and {@code u2} must have the same last symbol.
	 *
	 * @param u1
	 * @param u2
	 * @param v
	 * @return
	 */
	private SymbolicSuffix extendSuffixLocation(Word<PSymbolInstance> u1Ext, Word<PSymbolInstance> u2Ext, SymbolicSuffix v) {
		SDT sdt1 = getLeaf(u1Ext).getPrefix(u1Ext).getSDT(v);
		SDT sdt2 = getLeaf(u2Ext).getPrefix(u2Ext).getSDT(v);
//		return suffixBuilder.extendDistinguishingSuffix(u1, sdt1, u2, sdt2, v);
		if (restrBuilder != null && restrBuilder instanceof SLLambdaRestrictionBuilder sllambdaRestrBuilder) {
			Word<PSymbolInstance> u1 = u1Ext.prefix(u1Ext.size() - 1);
			Word<PSymbolInstance> u2 = u2Ext.prefix(u2Ext.size() - 1);
			CTLeaf leaf = getLeaf(u1);
			assert leaf == getLeaf(u2);
			Prefix u1Pref = leaf.getPrefix(u1);
			Prefix u2Pref = leaf.getPrefix(u2);
			Prefix u1ExtPref = getLeaf(u1Ext).getPrefix(u1Ext);
			Prefix u2ExtPref = getLeaf(u2Ext).getPrefix(u2Ext);
			Prefix reprPref = leaf.getRepresentativePrefix();
			return sllambdaRestrBuilder.extendSuffix(u1Pref, u1ExtPref, u2Pref, u2ExtPref, reprPref, v, sdt1, sdt2);
		}

		Word<ParameterizedSymbol> actions = v.getActions();
		Word<ParameterizedSymbol> extended = DataWords.concatenate(Word.fromSymbols(u1Ext.lastSymbol().getBaseSymbol()), actions);
		return new SymbolicSuffix(extended);
	}

	private SymbolicSuffix extendSuffixTransition(Word<PSymbolInstance> uIf, Word<PSymbolInstance> uElse, SymbolicSuffix v) {
		CTLeaf leafIf = getLeaf(uIf);
		CTLeaf leafElse = getLeaf(uElse);
		Prefix uIfPref = leafIf.getPrefix(uIf);
		Prefix uElsePref = leafElse.getPrefix(uElse);
		SDT sdtIf = uIfPref.getSDT(v);
		SDT sdtElse = uElsePref.getSDT(v);
		if (restrBuilder != null && restrBuilder instanceof SLLambdaRestrictionBuilder sllambdaRestrBuilder) {
			Word<PSymbolInstance> u = uIf.prefix(uIf.size() - 1);
			CTLeaf uLeaf = getLeaf(u);
			Prefix uPref = uLeaf.getPrefix(u);
			Prefix reprPref = uLeaf.getRepresentativePrefix();
			return sllambdaRestrBuilder.extendSuffix(uPref, uIfPref, uElsePref, reprPref, v, sdtIf, sdtElse);
		}

		Word<ParameterizedSymbol> actions = v.getActions();
		Word<ParameterizedSymbol> extended = DataWords.concatenate(Word.fromSymbols(uIf.lastSymbol().getBaseSymbol()), actions);
		return new SymbolicSuffix(extended);
	}

	/**
	 * {@code ParameterValuation} of the last symbol instance of {@code ua}.
	 *
	 * @param ua
	 * @return
	 */
	private ParameterValuation actionValuation(Word<PSymbolInstance> ua) {
		ParameterGenerator pgen = new ParameterGenerator();
		DataValue[] vals = ua.lastSymbol().getParameterValues();
		ParameterValuation valuation = new ParameterValuation();
		for (int i = 0; i < vals.length; i++) {
			Parameter p = pgen.next(vals[i].getDataType());
			valuation.put(p, vals[i]);
		}
		return valuation;
	}

	/**
	 *
	 * @param a
	 * @param b
	 * @return array of registers in {@code a} and {@code b} which are not mapped the same
	 */
	private Register[] inequivalentMapping(Bijection<Register> a, Bijection<Register> b) {
		Set<Register> ret = new LinkedHashSet<>();
		for (Map.Entry<Register, Register> ea : a.entrySet()) {
			Register key = ea.getKey();
			Register val = b.get(key);
			if (val == null) {
				ret.add(key);
				ret.add(ea.getValue());
			} else if (!val.equals(ea.getValue())) {
				ret.add(key);
				ret.add(val);
			}
		}
		return ret.toArray(new Register[ret.size()]);
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CT: {");
        buildTreeString(builder, root, "", "   ", " -- ");
        builder.append("}");
        return builder.toString();
    }

    private void buildTreeString(StringBuilder builder, CTNode node, String currentIndentation, String indentation, String sep) {
        if (node.isLeaf()) {
            builder.append("\n").append(currentIndentation).append("Leaf: ").append(node);
        } else {
            CTInnerNode inner = (CTInnerNode) node;
            builder.append("\n").append(currentIndentation).append("Inner: ").append(inner.getSuffix());
            if (!inner.getBranches().isEmpty()) {
                Iterator<CTBranch> iter = inner.getBranches().iterator();
                while (iter.hasNext()) {
                    builder.append("\n").append(currentIndentation);
                    CTBranch branch = iter.next();
                    builder.append("Branch: ").append(branch.getRepresentativePath());
                    buildTreeString(builder, branch.getChild(), indentation + currentIndentation, indentation, sep);
                }
            }
        }
    }

	private static List<SymbolicSuffix> outputSuffixes(ParameterizedSymbol[] inputs) {
		List<SymbolicSuffix> ret = new ArrayList<>();
		for (ParameterizedSymbol ps : inputs) {
			if (ps instanceof OutputSymbol) {
				ret.add(new SymbolicSuffix(ps));
			}
		}
		return ret;
	}

}
