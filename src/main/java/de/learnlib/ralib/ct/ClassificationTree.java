package de.learnlib.ralib.ct;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

	private static List<SymbolicSuffix> outputSuffixes(ParameterizedSymbol[] inputs) {
		List<SymbolicSuffix> ret = new ArrayList<>();
		for (ParameterizedSymbol ps : inputs) {
			if (ps instanceof OutputSymbol) {
				ret.add(new SymbolicSuffix(ps));
			}
		}
		return ret;
	}

	public Set<CTLeaf> getLeaves() {
		return new LinkedHashSet<>(prefixes.values());
	}

	public Set<Word<PSymbolInstance>> getPrefixes() {
		return new LinkedHashSet<>(prefixes.keySet());
	}

	public CTLeaf getLeaf(Word<PSymbolInstance> u) {
		return prefixes.get(u);
	}

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

	public CTLeaf sift(Word<PSymbolInstance> u) {
		Prefix prefix = new Prefix(u, new CTPath(ioMode));
		CTLeaf leaf = root.sift(prefix, oracle, solver, ioMode);
		prefixes.put(u, leaf);
		return leaf;
	}

	public void expand(Word<PSymbolInstance> u) {
		CTLeaf leaf = prefixes.get(u);
		if (leaf == null) {
			leaf = sift(u);
		}
		ShortPrefix prefix = leaf.elevatePrefix(u, oracle, inputs);
		shortPrefixes.add(u);

		for (ParameterizedSymbol ps : inputs) {
			Branching b = prefix.getBranching(ps);
			for (Word<PSymbolInstance> ua : b.getBranches().keySet()) {
				CTLeaf l = sift(ua);
				prefixes.put(ua, l);
			}
		}
	}

	public void refine(CTLeaf leaf, SymbolicSuffix suffix) {
		CTInnerNode parent = (CTInnerNode) leaf.getParent();
		assert parent != null;
		Map<Word<PSymbolInstance>, CTLeaf> leaves = parent.refine(leaf, suffix, oracle, solver, ioMode, inputs);
		prefixes.putAll(leaves);

		for (Word<PSymbolInstance> sp : shortPrefixes) {
			CTLeaf l = prefixes.get(sp);
			Prefix p = l.getPrefix(sp);
			if (!(p instanceof ShortPrefix)) {
				l.elevatePrefix(sp, oracle, inputs);
			}
		}
	}

	public void initialize() {
		sift(RaStar.EMPTY_PREFIX);
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

	public CTInnerNode lca(CTNode n1, CTNode n2) {
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

	// CLOSEDNESS CHECKS

	public boolean checkLocationClosedness() {
		for (CTLeaf leaf : getLeaves()) {
			if (leaf.getShortPrefixes().isEmpty()) {
				expand(leaf.getRepresentativePrefix());
				return false;
			}
		}
		return true;
	}

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

	public boolean checkRegisterClosedness() {
		for (Map.Entry<Word<PSymbolInstance>, CTLeaf> e : prefixes.entrySet()) {
			Word<PSymbolInstance> ua = e.getKey();
			CTLeaf leaf = e.getValue();
			if (ua.length() < 1) {
				continue;
			}
			Word<PSymbolInstance> u = ua.prefix(ua.size() - 1);
			Prefix ua_pref = leaf.getPrefix(ua);
			CTLeaf u_leaf = prefixes.get(u);

			Set<DataValue> ua_mem = leaf.getPrefix(ua).getRegisters();
			Set<DataValue> u_mem = prefixes.get(u).getPrefix(u).getRegisters();
			Set<DataValue> a_mem = actionRegisters(ua);

			if (!consistentMemorable(ua_mem, u_mem, a_mem)) {
				for (SymbolicSuffix v : leaf.getSuffixes()) {
					Set<DataValue> s_mem = ua_pref.getSDT(v).getDataValues();
					if (!consistentMemorable(s_mem, u_mem, a_mem)) {
						DataValue[] missingRegs = missingRegisters(s_mem, u_mem, a_mem);
						SymbolicSuffix av = extendSuffix(ua, v, missingRegs);
						refine(u_leaf, av);
						break;
					}
				}
				return false;
			}
		}
		return true;
	}

	private boolean consistentMemorable(Set<DataValue> ua_mem, Set<DataValue> u_mem, Set<DataValue> a_mem) {
		Set<DataValue> union = new LinkedHashSet<>();
		union.addAll(u_mem);
		union.addAll(a_mem);
		return union.containsAll(ua_mem);
	}

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

	private DataValue[] missingRegisters(Set<DataValue> s_mem, Set<DataValue> u_mem, Set<DataValue> a_mem) {
		Set<DataValue> union = new LinkedHashSet<>(u_mem);
		union.addAll(a_mem);
		Set<DataValue> difference = new LinkedHashSet<>(s_mem);
		difference.removeAll(union);
		return difference.toArray(new DataValue[difference.size()]);
	}

	private SymbolicSuffix extendSuffix(Word<PSymbolInstance> ua, SymbolicSuffix v, DataValue[] missingRegs) {
		if (suffixBuilder == null) {
			PSymbolInstance a = ua.lastSymbol();
			Word<PSymbolInstance> u = ua.prefix(ua.length() - 1);
			SymbolicSuffix alpha = new SymbolicSuffix(u, Word.fromSymbols(a), restrBuilder);
			return alpha.concat(v);
		}

		SDT u_sdt = prefixes.get(ua).getPrefix(ua).getSDT(v);
		assert u_sdt != null : "SDT for symbolic suffix " + v + " does not exist for prefix " + ua;

		return suffixBuilder.extendSuffix(ua, u_sdt, v, missingRegs);
	}

	// CONSISTENCY CHECKS

	public boolean checkLocationConsistency() {
		for (CTLeaf l : getLeaves()) {
			Iterator<ShortPrefix> sp = l.getShortPrefixes().iterator();
			if (!sp.hasNext()) {
				continue;
			}
			ShortPrefix u = sp.next();
			while (sp.hasNext()) {
				ShortPrefix uPrime = sp.next();
				for (ParameterizedSymbol action : inputs) {
					for (Map.Entry<Word<PSymbolInstance>, Expression<Boolean>> uEntry : u.getBranching(action).getBranches().entrySet()) {
						Word<PSymbolInstance> ua = uEntry.getKey();
						Expression<Boolean> g = uEntry.getValue();
						Bijection<DataValue> gamma = u.getRpBijection().compose(uPrime.getRpBijection().inverse());

						ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
						Expression<Boolean> gammaG = rvv.apply(g, gamma.toVarMapping());

						Optional<Word<PSymbolInstance>> uPrimeA = uPrime.getBranching(action).getPrefix(gammaG, solver);
						assert uPrimeA.isPresent();

						CTLeaf uALeaf = getLeaf(ua);
						CTLeaf uPrimeALeaf = getLeaf(uPrimeA.get());
						if (uALeaf != uPrimeALeaf) {
							SymbolicSuffix v = lca(uALeaf, uPrimeALeaf).getSuffix();
							SymbolicSuffix av = extendSuffix(ua, uPrimeA.get(), v);
							refine(l, av);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public boolean checkTransitionConsistency() {
		for (ShortPrefix u : getShortPrefixes()) {
			for (ParameterizedSymbol action : inputs) {
				Set<Word<PSymbolInstance>> extensions = getExtensions(u, action);
				for (Map.Entry<Word<PSymbolInstance>, Expression<Boolean>> e : u.getBranching(action).getBranches().entrySet()) {
					Word<PSymbolInstance> uA = e.getKey();
					Expression<Boolean> g = e.getValue();
					for (Word<PSymbolInstance> uB : extensions) {
						if (uB.equals(uA)) {
							continue;
						}
						Mapping<SymbolicDataValue, DataValue> mapping = new Mapping<>();
						mapping.putAll(actionValuation(uB));
						mapping.putAll(consts);
						if (solver.isSatisfiable(g, mapping)) {
							Optional<SymbolicSuffix> av = transitionConsistentA(uA, uB);
							if (av.isEmpty()) {
								av = transitionConsistentB(uA, uB);
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

	private Optional<SymbolicSuffix> transitionConsistentA(Word<PSymbolInstance> uA, Word<PSymbolInstance> uB) {
		Word<PSymbolInstance> u = uA.prefix(uA.length() - 1);
		CTLeaf uALeaf = getLeaf(uA);
		CTLeaf uBLeaf = getLeaf(uB);
		if (uALeaf != uBLeaf) {
			CTLeaf uLeaf = getLeaf(u);
			assert uLeaf != null : "Prefix is not short: " + u;
			SymbolicSuffix v = lca(uALeaf, uBLeaf).getSuffix();
			SymbolicSuffix av = extendSuffix(uA, uB, v);
			return Optional.of(av);
		}
		return Optional.empty();
	}

	private Optional<SymbolicSuffix> transitionConsistentB(Word<PSymbolInstance> uA, Word<PSymbolInstance> uB) {
		Prefix pA = getLeaf(uA).getPrefix(uA);
		Prefix pB = getLeaf(uB).getPrefix(uB);
		for (SymbolicSuffix v : getLeaf(uB).getSuffixes()) {
			SDT sdtA = pA.getSDT(v).toRegisterSDT(uA, consts);
			SDT sdtB = pB.getSDT(v).toRegisterSDT(uB, consts);
			if (!SDT.equivalentUnderId(sdtA, sdtB)) {
				CTLeaf uLeaf = getLeaf(uA.prefix(uA.length() - 1));
				assert uLeaf != null;
				Register[] regs = inequivalentMapping(rpRegBijection(pA.getRpBijection(), pA), rpRegBijection(pB.getRpBijection(), pB));
				DataValue[] regVals = regsToDvs(regs, uA);
				SymbolicSuffix av = extendSuffix(uA, v, regVals);
				if (suffixRevealsNewGuard(av, getLeaf(uA.prefix(uA.length() - 1)))) {
					return Optional.of(av);
				}
			}
		}
		return Optional.empty();
	}

	private boolean suffixRevealsNewGuard(SymbolicSuffix av, CTLeaf leaf) {
		Word<PSymbolInstance> u = leaf.getRepresentativePrefix();
		SDT sdt = oracle.treeQuery(u, av);
		ParameterizedSymbol a = av.getActions().firstSymbol();
		Branching branching = leaf.getBranching(a);
		Branching newBranching = oracle.updateBranching(u, a, branching, sdt);
		for (Expression<Boolean> guard : newBranching.getBranches().values()) {
			if (!branching.getBranches().values().contains(guard)) {
				return true;
			}
		}
		return false;
	}

	private Bijection<Register> rpRegBijection(Bijection<DataValue> bijection, Word<PSymbolInstance> prefx) {
		return Bijection.DVtoRegBijection(bijection, prefx, getLeaf(prefx).getRepresentativePrefix());
	}

	private DataValue[] regsToDvs(Register[] regs, Word<PSymbolInstance> prefix) {
		DataValue[] vals = DataWords.valsOf(prefix);
		DataValue[] ret = new DataValue[regs.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = vals[regs[i].getId()-1];
		}
		return ret;
	}

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
					Prefix ua = extensions.next();

					RemappingIterator<DataValue> rit = new RemappingIterator<>(u.getRegisters(), u.getRegisters());
					for (Bijection<DataValue> gamma : rit) {
						CTPath uPath = u.getPath();
						if (uPath.isEquivalent(uPath, gamma, solver)) {
							for (Map.Entry<SymbolicSuffix, SDT> e : ua.getPath().getSDTs().entrySet()) {
								SymbolicSuffix v = e.getKey();
								SDT uaSDT = e.getValue();
								if (SDT.equivalentUnderBijection(uaSDT, uaSDT, gamma) == null) {
									DataValue[] regs = gamma.keySet().toArray(new DataValue[gamma.size()]);
									SymbolicSuffix av = extendSuffix(ua, v, regs);
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

	private SymbolicSuffix extendSuffix(Word<PSymbolInstance> u1, Word<PSymbolInstance> u2, SymbolicSuffix v) {
		SDT sdt1 = getLeaf(u1).getPrefix(u1).getSDT(v);
		SDT sdt2 = getLeaf(u2).getPrefix(u2).getSDT(v);
		return suffixBuilder.extendDistinguishingSuffix(u1, sdt1, u2, sdt2, v);
	}

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

	public Set<Word<PSymbolInstance>> getExtensions(Word<PSymbolInstance> u, ParameterizedSymbol action) {
		return prefixes.keySet()
				.stream()
				.filter(w -> w.length() == u.length() + 1)
				.filter(w -> w.prefix(w.length() - 1).equals(u) && w.lastSymbol().getBaseSymbol().equals(action))
				.collect(Collectors.toSet());
	}

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

	/*
	 * Returns the sink node for IO automata
	 */
	public Optional<CTLeaf> getSink() {
		if (!ioMode) {
			return Optional.empty();
		}

		for (CTBranch branch : root.getBranches()) {
			if (!branch.getPath().isAccepting()) {
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
}
