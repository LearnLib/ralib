package de.learnlib.ralib.dt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.data.SymbolicDataValue.Parameter;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PIVRemappingIterator;
import de.learnlib.ralib.learning.LocationComponent;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicDecisionTree;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.learning.rastar.RaStar;
import de.learnlib.ralib.learning.rattt.DiscriminationTree;
import de.learnlib.ralib.oracles.Branching;
import de.learnlib.ralib.oracles.TreeOracle;
import de.learnlib.ralib.oracles.TreeQueryResult;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class DTLeaf extends DTNode implements LocationComponent {
	
	private MappedPrefix access;
	private PrefixSet shortPrefixes;
	private PrefixSet otherPrefixes;
	
	private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<ParameterizedSymbol, Branching>();
	
	public DTLeaf() {
		super();
		access = null;
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
	}
	
	public DTLeaf(Word<PSymbolInstance> p) {
		super();
		access = new MappedPrefix(p, new PIV());
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
		//shortPrefixes.add(new MappedPrefix(p, new PIV()));
	}
	
	public DTLeaf(MappedPrefix as) {
		super();
		access = as;
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
		//shortPrefixes.add(as);
	}
	
	public void addPrefix(Word<PSymbolInstance> p) {
		otherPrefixes.add(new MappedPrefix(p));
	}
	
	public void addPrefix(MappedPrefix p) {
		otherPrefixes.add(p);
	}
	
	public void addShortPrefix(Word<PSymbolInstance> prefix, PIV registers) {
		if (access == null) 
			access = new MappedPrefix(prefix, registers);
		else
			addShortPrefix(new MappedPrefix(prefix, registers));
	}
	
	public void addShortPrefix(MappedPrefix prefix) {
		if (otherPrefixes.contains(prefix))
			otherPrefixes.remove(prefix);
		if (access == null)
			access = prefix;
		else
			shortPrefixes.add(prefix);
	}
	
	public boolean removeShortPrefix(MappedPrefix p) {
		return shortPrefixes.remove(p);
	}
	
	public boolean removeShortPrefix(Word<PSymbolInstance> p) {
		return shortPrefixes.remove(p);
	}
	
	public void clear() {
		shortPrefixes = new PrefixSet();
		otherPrefixes = new PrefixSet();
	}
	
	public PrefixSet getShortPrefixes() {
		return shortPrefixes;
	}
	
	public PrefixSet getPrefixes() {
		return otherPrefixes;
	}
	
	public boolean isLeaf() {
		return true;
	}

	@Override
	public boolean isAccepting() {
		TreeQueryResult tqr = access.getTQRs().get(RaStar.EMPTY_SUFFIX);
		return tqr.getSdt().isAccepting();
	}
	
	MappedPrefix get(Word<PSymbolInstance> p) {
		if (access.getPrefix().equals(p))
			return access;
		MappedPrefix ret = null;
		ret = shortPrefixes.get(p);
		if (ret != null)
			return ret;
		ret = otherPrefixes.get(p);
		return ret;
	}

	@Override
	public Word<PSymbolInstance> getAccessSequence() {
		return access.getPrefix();
	}

	@Override
	public VarMapping getRemapping(PrefixContainer r) {
		if (r.getPrefix().equals(this.getAccessSequence()))
//			return this.access.getParsInVars();
			return null;
		PIVRemappingIterator it = new PIVRemappingIterator(
				otherPrefixes.get(r.getPrefix()).getParsInVars(),
				this.access.getParsInVars());
//		return otherPrefixes.get(r.getPrefix()).getParsInVars();
		return it.next();
	}

	@Override
	public Branching getBranching(ParameterizedSymbol action) {
		return branching.get(action);
	}

	@Override
	public MappedPrefix getPrimePrefix() {
		return access;
	}
	
	Collection<Word<PSymbolInstance>> getAllPrefixes() {
		Collection<Word<PSymbolInstance>> prefs = new ArrayList<Word<PSymbolInstance>>();
		prefs.add(this.getAccessSequence());
		Iterator<MappedPrefix> it = shortPrefixes.iterator();
		while (it.hasNext())
			prefs.add(it.next().getPrefix());
		it = otherPrefixes.iterator();
		while (it.hasNext())
			prefs.add(it.next().getPrefix());
		return prefs;
	}
	
	@Override
	public DTInnerNode getParent() {
		return parent;
	}
	
	public MappedPrefix getPrefix(Word<PSymbolInstance> p) {
		return otherPrefixes.get(p);
	}
	
	void addTQRs(PIV primePIV, SymbolicSuffix s, TreeOracle oracle) {
		TreeQueryResult tqr;
		//TreeQueryResult tqr = oracle.treeQuery(getAccessSequence(), s);
		//access.addTQR(s, tqr);
		access.updateMemorable(primePIV);
		Iterator<MappedPrefix> it = shortPrefixes.iterator();
		while (it.hasNext()) {
			MappedPrefix mp = it.next();
			tqr = oracle.treeQuery(mp.getPrefix(), s);
			mp.addTQR(s, tqr);
		}
		it = otherPrefixes.iterator();
		while (it.hasNext()) {
			MappedPrefix mp = it.next();
			tqr = oracle.treeQuery(mp.getPrefix(), s);
			mp.addTQR(s, tqr);
		}
	}
	
	void addTQRs(PIV primePIV, SymbolicSuffix s, TreeOracle oracle, boolean addToAccess) {
		if (addToAccess) {
			TreeQueryResult tqr = oracle.treeQuery(access.getPrefix(), s);
			this.access.addTQR(s, tqr);
		}
		addTQRs(primePIV, s, oracle);
	}
	
	@Override
	public Collection<PrefixContainer> getOtherPrefixes() {
		return otherPrefixes.get().stream().collect(Collectors.toList());
	}
	
//	public boolean expandPrefix(Word<PSymbolInstance> p, DT dt) {
//		MappedPrefix mp = this.getPrefix(p);
//		if (mp == null) {
//			dt.sift(p, true);
//			return true;
//		}
//		this.addShortPrefix(mp);
//		
//		boolean refinement = startPrefix(dt, mp, dt.getOracle());
//		return refinement;
//	}
	
	private boolean startPrefix(DT dt, MappedPrefix mp, TreeOracle oracle) {
		boolean refinement = false;
		boolean input = isInputComponent();
		for (ParameterizedSymbol ps : dt.getInputs()) {
			if (dt.getIoMode() && (input ^ isInput(ps)))
				continue;
			
			SymbolicDecisionTree[] sdtsP = this.getSDTsForInitialSymbol(mp, ps);
			Branching prefixBranching = oracle.getInitialBranching(mp.getPrefix(), ps, mp.getParsInVars(), sdtsP);
			
			SymbolicDecisionTree[] sdtsAS = this.getSDTsForInitialSymbol(this.getPrimePrefix(), ps);
			Branching accessBranching = oracle.getInitialBranching(getAccessSequence(), ps, this.access.getParsInVars(), sdtsAS);
			
			assert prefixBranching.getBranches().size() == accessBranching.getBranches().size();
			
			Iterator<Word<PSymbolInstance>> itB = prefixBranching.getBranches().keySet().iterator();
			Iterator<Word<PSymbolInstance>> itA = accessBranching.getBranches().keySet().iterator();
			
			while (itB.hasNext()) {
				assert itA.hasNext();
				Word<PSymbolInstance> p = itB.next();
				Word<PSymbolInstance> a = itA.next();
				DTLeaf leaf = dt.sift(p, true);
				if (!dt.getLeaf(a).equals(leaf))
					refinement = true;
			}
//			for (Word<PSymbolInstance> prefix : b.getBranches().keySet()) {
//				dt.sift(prefix, true);
//			}
		}
		return refinement;
	}
	
	void start(DT dt, TreeOracle oracle, boolean ioMode, ParameterizedSymbol ... inputs) {
		boolean input = isInputComponent();
		for (ParameterizedSymbol ps : inputs) {
			if (ioMode && (input ^ isInput(ps))) {
	            continue;
	        }
			
			SymbolicDecisionTree[] sdts = this.getSDTsForInitialSymbol(ps);
			Branching b = oracle.getInitialBranching(getAccessSequence(), ps, access.getParsInVars(), sdts);
			branching.put(ps, b);
			for (Word<PSymbolInstance> prefix : b.getBranches().keySet()) {
				dt.sift(prefix, true);
			}
		}
	}

	boolean updateBranching(TreeOracle oracle, DiscriminationTree dt) {
		boolean ret = true;
		for (ParameterizedSymbol p : branching.keySet()) {
			boolean ub = updateBranching(p, oracle, dt);
			ret = ret && ub;
		}
		return ret;
	}
	
	private boolean updateBranching(ParameterizedSymbol ps, TreeOracle oracle, DiscriminationTree dt) {
		Branching b = branching.get(ps);
		SymbolicDecisionTree[] sdts = getSDTsForInitialSymbol(ps);
		Branching newB = oracle.updateBranching(access.getPrefix(), ps, b, access.getParsInVars(), sdts);
		boolean ret = true;
		
		for (Word<PSymbolInstance> prefix : newB.getBranches().keySet()) {
			if (!b.getBranches().containsKey(prefix)) {
				dt.sift(prefix, true);
				ret = false;
			}
		}
		
		branching.put(ps, newB);
		return ret;
	}
	
	private SymbolicDecisionTree[] getSDTsForInitialSymbol(ParameterizedSymbol p) {
		return getSDTsForInitialSymbol(this.getPrimePrefix(), p);
//		List<SymbolicDecisionTree> sdts = new ArrayList<>();
//		for (Entry<SymbolicSuffix, TreeQueryResult> e : this.access.getTQRs().entrySet()) {
//			Word<ParameterizedSymbol> acts = e.getKey().getActions();
//			if (acts.length() > 0 && acts.firstSymbol().equals(p)) {
//				sdts.add(makeConsistent(e.getValue().getSdt(), e.getValue().getPiv(), this.getPrimePrefix().getParsInVars()));
//			}
//		}
//		return sdts.toArray(new SymbolicDecisionTree[]{});
	}
	
	private SymbolicDecisionTree[] getSDTsForInitialSymbol(MappedPrefix mp, ParameterizedSymbol p) {
		List<SymbolicDecisionTree> sdts = new ArrayList<>();
		for (Entry<SymbolicSuffix, TreeQueryResult> e : mp.getTQRs().entrySet()) {
			Word<ParameterizedSymbol> acts = e.getKey().getActions();
			if (acts.length() > 0 && acts.firstSymbol().equals(p)) {
				sdts.add(makeConsistent(e.getValue().getSdt(), e.getValue().getPiv(), mp.getParsInVars()));
			}
		}
		return sdts.toArray(new SymbolicDecisionTree[]{});
	}
	
	private SymbolicDecisionTree makeConsistent(SymbolicDecisionTree sdt, PIV piv, PIV memorable) {
		VarMapping relabeling = new VarMapping();
		for (Entry<Parameter, Register> e : piv.entrySet()) {
			Register r = memorable.get(e.getKey());
			relabeling.put(e.getValue(), r);
		}
		return sdt.relabel(relabeling);
	}
	
	public boolean checkVariableConsistency(DT dt, TreeOracle oracle, Constants consts) {
		if (!checkVariableConsistency(access, dt, oracle, consts)) {
			return false;
		}
		assert(shortPrefixes.length() == 0);
		Iterator<MappedPrefix> it = otherPrefixes.iterator();
		while (it.hasNext()) {
			if (!checkVariableConsistency(it.next(), dt, oracle, consts))
				return false;
		}
		return true;
	}

	private boolean checkVariableConsistency(MappedPrefix mp, DT dt, TreeOracle oracle, Constants consts) {
		if (mp.getPrefix().length() < 2)
			return true;
		
		Word<PSymbolInstance> prefix = mp.getPrefix().prefix(mp.getPrefix().length() - 1);
		MappedPrefix prefixMapped = dt.getLeaf(prefix).getPrimePrefix();
		
		PIV memPrefix = prefixMapped.getParsInVars();
		PIV memMP = mp.getParsInVars();
		
		int max = DataWords.paramLength(DataWords.actsOf(prefix));
		
		for (Parameter p : memMP.keySet()) {
			if (!memPrefix.containsKey(p) && p.getId() <= max) {
				SymbolicSuffix suffix = mp.getSuffixForMemorable(p);
				SymbolicSuffix newSuffix = new SymbolicSuffix(
						mp.getPrefix(), suffix, consts);
				
				dt.addSuffix(newSuffix, dt.getLeaf(prefixMapped.getPrefix()));
				return false;
			}
		}
		return true;
	}

    private boolean isInputComponent() {
        if (this.getAccessSequence().length() == 0)
            return true;
        
        ParameterizedSymbol ps = this.getAccessSequence().lastSymbol().getBaseSymbol();
        return !isInput(ps);
    }

    private boolean isInput(ParameterizedSymbol ps) {
        return (ps instanceof InputSymbol);
    }
}
