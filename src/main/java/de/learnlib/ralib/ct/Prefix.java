package de.learnlib.ralib.ct;

import java.util.ArrayList;
import java.util.Iterator;
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

public class Prefix extends Word<PSymbolInstance> implements PrefixContainer {
	private final Word<PSymbolInstance> prefix;
	private Bijection<DataValue> rpBijection;
//	private final Map<SymbolicSuffix, SDT> sdts;
	private final CTPath path;

//	public Prefix(Word<PSymbolInstance> prefix, Bijection rpBijection) {
	public Prefix(Word<PSymbolInstance> u, Bijection<DataValue> rpRenaming, CTPath path) {
		this.prefix = u instanceof Prefix ? ((Prefix) u).getPrefix() : u;
		this.rpBijection = rpRenaming;
		this.path = path;
//		sdts = new LinkedHashMap<>();
	}

	public Prefix(Word<PSymbolInstance> prefix, CTPath path) {
		this(prefix, Bijection.identity(path.getMemorable()), path);
	}

	public Prefix(Prefix prefix, Bijection<DataValue> rpRenaming) {
		this(prefix.prefix, rpRenaming, prefix.path);
//		sdts.putAll(u.sdts);
	}

	public Prefix(Prefix other) {
		this(other.prefix, other.rpBijection, other.path);
	}

	public void setRpBijection(Bijection<DataValue> rpBijection) {
		this.rpBijection = rpBijection;
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

//	public void putSDT(SymbolicSuffix s, SDT sdt) {
//		sdts.put(s, sdt);
//	}

//	public Word<PSymbolInstance> getPrefix() {
//		return prefix;
//	}

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
//		if (!(other instanceof Prefix)) {
//			return false;
//		}
//		return ((Prefix) other).prefix.equals(prefix);
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
