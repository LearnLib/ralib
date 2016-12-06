package de.learnlib.ralib.learning;

import java.util.LinkedHashSet;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.DataWords;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public class ParamSignature {
	public static final ParamSignature ANY = new AnySignature();
	
	static class AnySignature extends ParamSignature {

		private AnySignature() {
			super(null, -1);
		}
		
		public String toString() {
			return "ANY";
		}
		
		public  Set<DataValue> getDataValuesWithSignature(Word<PSymbolInstance> prefix){
			return DataWords.valSet(prefix);
		}
	}
	
	public ParamSignature(ParameterizedSymbol symbol, int index) {
		super();
		this.symbol = symbol;
		this.index = index;
	}
	
	public String toString() {
		return this.symbol.getName() + ".p" + this.index;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParamSignature other = (ParamSignature) obj;
		if (index != other.index)
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		return true;
	}
	private final ParameterizedSymbol symbol;
	private final int index;
	
	public Set<DataValue> getDataValuesWithSignature(Word<PSymbolInstance> prefix){
		Set<DataValue> dvs = new LinkedHashSet<>();
		for (PSymbolInstance psym : prefix) {
			if (psym.getBaseSymbol().equals(this.symbol))
				dvs.add(psym.getParameterValues()[this.index]);
		}
		return dvs;
	} 

}
