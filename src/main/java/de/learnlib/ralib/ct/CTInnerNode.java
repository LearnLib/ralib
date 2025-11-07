package de.learnlib.ralib.ct;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.smt.ConstraintSolver;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

public class CTInnerNode extends CTNode {
	private final SymbolicSuffix suffix;
	private final List<CTBranch> branches;

	public CTInnerNode(CTNode parent, SymbolicSuffix suffix) {
		super(parent);
		this.suffix = suffix;
		branches = new ArrayList<>();
	}

	public SymbolicSuffix getSuffix() {
		return suffix;
	}

	public List<CTBranch> getBranches() {
		return branches;
	}

	protected CTBranch getBranch(CTNode child) {
		for (CTBranch b : getBranches()) {
			if (b.getChild() == child) {
				return b;
			}
		}
		return null;
	}

	@Override
	protected CTLeaf sift(Prefix prefix, TreeOracle oracle, ConstraintSolver solver, boolean ioMode) {
		CTPath path = CTPath.computePath(oracle, prefix, getSuffixes(), ioMode);
//		SDT sdt = path.getSDT(suffix);
//		prefix.putSDT(suffix, sdt);

		for (CTBranch b : branches) {
			Bijection vars = b.matches(path, solver);
			if (vars != null) {
//				prefix.setRpBijection(vars);
				prefix = new Prefix(prefix, vars, path);
				return b.getChild().sift(prefix, oracle, solver, ioMode);
			}
		}

		// no child with equivalent SDTs, create a new leaf
		prefix = new Prefix(prefix, path);
		CTLeaf leaf = new CTLeaf(prefix, this);
		CTBranch branch = new CTBranch(path, leaf);
		branches.add(branch);
		return leaf;
	}

	protected Map<Word<PSymbolInstance>, CTLeaf> refine(CTLeaf leaf, SymbolicSuffix suffix, TreeOracle oracle, ConstraintSolver solver, boolean ioMode, ParameterizedSymbol[] inputs) {
		CTBranch b = getBranch(leaf);
		assert b != null : "Node is not the parent of leaf " + leaf;
		assert !getSuffixes().contains(suffix) : "Duplicate suffix: " + suffix;

		Set<ShortPrefix> shorts = leaf.getShortPrefixes();

		CTInnerNode newNode = new CTInnerNode(this, suffix);
		CTBranch newBranch = new CTBranch(b.getPath(), newNode);
		branches.remove(b);
		branches.add(newBranch);

		Map<Word<PSymbolInstance>, CTLeaf> leaves = new LinkedHashMap<>();
		CTLeaf l = sift(leaf.getRepresentativePrefix(), oracle, solver, ioMode);
		leaves.put(leaf.getRepresentativePrefix(), l);

		Set<Prefix> prefixes = new LinkedHashSet<>(leaf.getPrefixes());
		prefixes.remove(leaf.getRepresentativePrefix());

		for (Prefix u : prefixes) {
			l = sift(u, oracle, solver, ioMode);
			leaves.put(u, l);
		}
		for (ShortPrefix u : shorts) {
			if (!(u instanceof ShortPrefix)) {
				leaves.get(u).elevatePrefix(u, oracle, inputs);
			}
		}

		return leaves;
	}

	@Override
	public List<SymbolicSuffix> getSuffixes() {
		List<SymbolicSuffix> suffixes = new ArrayList<>();
		suffixes.add(suffix);
		if (getParent() == null) {
			return suffixes;
		}

		suffixes.addAll(getParent().getSuffixes());
		return suffixes;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public String toString() {
		return "(" + suffix + ")";
	}

//	@Override
//	protected void sift(Prefix u, Map<SymbolicSuffix, SDT> rpSDTs, ConstraintSolver solver, TreeOracle oracle) {
//		TreeQueryResult tqr = oracle.treeQuery(u.getPrefix(), suffix);
//		SDT sdt = tqr.getSdt().relabel(tqr.getPiv().getRenaming().toVarMapping());
//
//		Set<Register> regs = new LinkedHashSet<>();
//		regs.addAll(u.getRegisters());
//		regs.addAll(sdt.getRegisters());
//
//		Set<Register> rpRegs = new LinkedHashSet<>();
//		for (SDT sdt : rpSDTs.values()) {
//			rpRegs.addAll(sdt.getRegisters());
//		}
//
//		for (CTBranch b : branches) {
//			Set<Register> branchRegs = new LinkedHashSet<>(rpRegs);
//			branchRegs.addAll(b.getSDT().getRegisters());
//
//			RemappingIterator it = new RemappingIterator(regs, branchRegs);
//
//			while(it.hasNext()) {
//				Bijection renaming = it.next();
//				if (!b.equivalentUnderRenaming(u, rpSDTs, renaming, solver, oracle)) {
//					continue;
//				}
//
//				// equivalent for all SDTs up to this point
//				u = new Prefix(u, new Bijection(renaming));
//				u.putSDT(suffix, sdt);
//				rpSDTs.put(suffix, sdt);
//				b.getChild().sift(u, rpSDTs, solver, oracle);
//				return;
//			}
//		}
//
//		// no branch with equivalent SDT, create new leaf
//		u.putSDT(suffix, sdt);
//		CTLeaf leaf = new CTLeaf(u, this);
//	}

}
