package de.learnlib.ralib.ct;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.RegisterAssignment;
import de.learnlib.ralib.data.util.SymbolicDataValueGenerator;
import de.learnlib.ralib.learning.PrefixContainer;
import de.learnlib.ralib.learning.SymbolicSuffix;
import de.learnlib.ralib.theory.SDT;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.word.Word;

/**
 * Data structure for a prefix stored within a leaf of a {@link ClassificationTree}.
 * Along with the prefix itself, also stores the SDTs for each suffix along the path
 * through which the prefix was sifted, as well as the {@code Bijection} under which
 * the SDTs along the path are equivalent to those of the leaf's representative prefix
 * (the RP bijection). If this prefix is the representative prefix, the RP bijection
 * should be the identity mapping.
 *
 * @author fredrik
 * @see CTLeaf
 * @see CTPath
 * @see Bijection
 */
public class Prefix extends Word<PSymbolInstance> implements PrefixContainer {
	private final Word<PSymbolInstance> prefix;
	private Bijection<DataValue> rpBijection;
	private final CTPath path;
	public final Map<SymbolicSuffix, Bijection<DataValue>> pathBijections;  // tracks bijections to the RP at each ancestor node

	public Prefix(Word<PSymbolInstance> u, Bijection<DataValue> rpRenaming, CTPath path) {
		this.prefix = u instanceof Prefix ? ((Prefix) u).getPrefix() : u;
		this.rpBijection = rpRenaming;
		this.path = path;
		pathBijections = new LinkedHashMap<>();
	}

	public Prefix(Word<PSymbolInstance> u, Bijection<DataValue> rpRenaming, CTPath path, Map<SymbolicSuffix, Bijection<DataValue>> pathBijections) {
		this(u, rpRenaming, path);
		this.pathBijections.putAll(pathBijections);
	}

	public Prefix(Word<PSymbolInstance> prefix, CTPath path) {
		this(prefix, Bijection.identity(path.getMemorable()), path);
		if (prefix instanceof Prefix p) {
			pathBijections.putAll(p.pathBijections);
		}
	}

	public Prefix(Prefix prefix, Bijection<DataValue> rpRenaming) {
		this(prefix.prefix, rpRenaming, prefix.path, prefix.pathBijections);
	}

	public void setRpBijection(Bijection<DataValue> rpBijection) {
		this.rpBijection = rpBijection;
	}

	/**
	 * Add bijection mapping {@code this} to the representative prefix of the node containing {@code suffix}.
	 *
	 * @param suffix
	 * @param bijection
	 */
	public void putBijection(SymbolicSuffix suffix, Bijection<DataValue> bijection) {
		pathBijections.put(suffix, bijection);
	}

	public Map<SymbolicSuffix, Bijection<DataValue>> getBijections() {
		return pathBijections;
	}

	/**
	 * Add identity bijection mapping {@code this} to the representative prefix of the node containing {@code suffix}
	 *
	 * @param suffix
	 */
	public void putBijection(SymbolicSuffix suffix) {
		putBijection(suffix, Bijection.identity(getRegisters()));
	}

	/**
	 * @param suffix
	 * @return bijection mapping {@code this} to the representative prefix of the ancestor node containing {@code suffix}
	 */
	public Bijection<DataValue> getBijection(SymbolicSuffix suffix) {
		return pathBijections.get(suffix);
	}

	public SDT[] getSDTs(ParameterizedSymbol ps) {
		List<SDT> list = new ArrayList<>();
		for (Map.Entry<SymbolicSuffix, SDT> e : path.getSDTs().entrySet()) {
			Word<ParameterizedSymbol> acts = e.getKey().getActions();
			if (acts.length() > 0 && acts.firstSymbol().equals(ps)) {
				list.add(e.getValue());
			}
		}
		return list.toArray(new SDT[list.size()]);
	}

	public SDT getSDT(SymbolicSuffix s) {
		return path.getSDT(s);
	}

	public Bijection<DataValue> getRpBijection() {
		return rpBijection;
	}

	public MemorableSet getRegisters() {
		return path.getMemorable();
	}

	public CTPath getPath() {
		return path;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Prefix) {
			return ((Prefix) other).prefix.equals(prefix);
		}
		return prefix.equals(other);
	}

	@Override
	public Word<PSymbolInstance> getPrefix() {
		return prefix;
	}

	@Override
	public RegisterAssignment getAssignment() {
		RegisterAssignment ra = new RegisterAssignment();
		SymbolicDataValueGenerator.RegisterGenerator regGen =
				new SymbolicDataValueGenerator.RegisterGenerator();

		this.getRegisters().forEach(
				dv -> ra.put(dv, regGen.next(dv.getDataType()))
		);

		return ra;
	}

	@Override
	public int hashCode() {
		return prefix.hashCode();
	}

	@Override
	public String toString() {
		return prefix.toString();
	}

	@Override
	public int length() {
		return prefix.length();
	}

	@Override
	public PSymbolInstance getSymbol(int index) {
		return prefix.getSymbol(index);
	}

	@Override
	public Iterator<PSymbolInstance> iterator() {
		return prefix.iterator();
	}

	@Override
	public Spliterator<PSymbolInstance> spliterator() {
		return prefix.spliterator();
	}

	@Override
    public void writeToArray(int offset, @Nullable Object[] array, int tgtOffset, int length) {
		prefix.writeToArray(offset, array, tgtOffset, length);
	}

	@Override
	public List<PSymbolInstance> asList() {
		return prefix.asList();
	}

	@Override
	public PSymbolInstance lastSymbol() {
		return prefix.lastSymbol();
	}

	@Override
	public Word<PSymbolInstance> append(PSymbolInstance symbol) {
		return prefix.append(symbol);
	}

	@Override
	public Word<PSymbolInstance> prepend(PSymbolInstance symbol) {
		return prefix.prepend(symbol);
	}

	@Override
	public boolean isPrefixOf(Word<?> other) {
		return prefix.isPrefixOf(other);
	}

	@Override
    public Word<PSymbolInstance> longestCommonPrefix(Word<?> other) {
		return prefix.longestCommonPrefix(other);
	}

	@Override
    public boolean isSuffixOf(Word<?> other) {
		return prefix.isSuffixOf(other);
	}

    @Override
    public Word<PSymbolInstance> longestCommonSuffix(Word<?> other) {
    	return prefix.longestCommonSuffix(other);
    }

    @Override
    public Word<PSymbolInstance> flatten() {
    	return prefix.flatten();
    }

    @Override
    public Word<PSymbolInstance> trimmed() {
    	return prefix.trimmed();
    }

    @Override
    public <T> Word<T> transform(Function<? super PSymbolInstance, ? extends T> transformer) {
    	return prefix.transform(transformer);
    }
}
