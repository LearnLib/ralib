//package de.learnlib.ralib.learning.ralambda;
//
//import java.util.Collection;
//import java.util.LinkedHashSet;
//import java.util.LinkedList;
//import java.util.Set;
//
//import de.learnlib.ralib.ct.CTLeaf;
//import de.learnlib.ralib.ct.Prefix;
//import de.learnlib.ralib.ct.ShortPrefix;
//import de.learnlib.ralib.data.Bijection;
//import de.learnlib.ralib.data.PIV;
//import de.learnlib.ralib.data.VarMapping;
//import de.learnlib.ralib.learning.LocationComponent;
//import de.learnlib.ralib.learning.PrefixContainer;
//import de.learnlib.ralib.oracles.Branching;
//import de.learnlib.ralib.words.PSymbolInstance;
//import de.learnlib.ralib.words.ParameterizedSymbol;
//import net.automatalib.word.Word;
//
//public class LeafComponent implements LocationComponent {
//	
//	public class PC implements PrefixContainer {
//		Word<PSymbolInstance> prefix;
//		PIV piv;
//		
//		public PC(Word<PSymbolInstance> prefix, PIV piv) {
//			this.prefix = prefix;
//			this.piv = piv;
//		}
//		
//		@Override
//		public Word<PSymbolInstance> getPrefix() {
//			return prefix;
//		}
//
//		@Override
//		public PIV getParsInVars() {
//			return piv;
//		}
//	}
//	
//	private final CTLeaf leaf;
//	
//	public LeafComponent(CTLeaf leaf) {
//		this.leaf = leaf;
//	}
//
//	@Override
//	public boolean isAccepting() {
//		return leaf.isAccepting();
//	}
//
//	@Override
//	public Word<PSymbolInstance> getAccessSequence() {
//		return leaf.getRepresentativePrefix();
//	}
//
//	@Override
//	public VarMapping getRemapping(PrefixContainer r) {
//		Bijection remapping = leaf.getPrefix(r.getPrefix()).getRpBijection();
//		return remapping.toVarMapping();
//	}
//
//	@Override
//	public Branching getBranching(ParameterizedSymbol action) {
//		Prefix rp = leaf.getRepresentativePrefix();
//		assert rp instanceof ShortPrefix : "Prefix is not a short prefix: " + rp;
//		return ((ShortPrefix) rp).getBranching(action);
//	}
//
//	@Override
//	public PrefixContainer getPrimePrefix() {
//		Prefix rp = leaf.getRepresentativePrefix();
//		return new PC(rp, PIV.defaultPiv(rp.getRegisters()));
//	}
//
//	@Override
//	public Collection<PrefixContainer> getOtherPrefixes() {
//		Set<Prefix> prefixes = new LinkedHashSet<>(leaf.getPrefixes());
//		prefixes.remove(leaf.getRepresentativePrefix());
//		Collection<PrefixContainer> container = new LinkedList<>();
//		for (Prefix u : prefixes) {
//			container.add(new PC(u, PIV.defaultPiv(u.getRegisters())));
//		}
//		return container;
//	}
//
//}
