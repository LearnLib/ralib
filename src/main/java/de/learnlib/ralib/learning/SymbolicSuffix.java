package de.learnlib.ralib.learning;

import static de.learnlib.ralib.theory.DataRelation.EQ;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.data.WordValuation;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import net.automatalib.words.Word;

public interface SymbolicSuffix {
	 public Word<ParameterizedSymbol> getActions();
	 public SuffixValue getValue(int i);
	 

		public static ParameterizedSymbol computeSymbol(GeneralizedSymbolicSuffix suffix, int pId) {
			int idx = 0;
			for (ParameterizedSymbol a : suffix.getActions()) {
				idx += a.getArity();
				if (idx >= pId) {
					return a;
				}
			}
			return suffix.getActions().size() > 0 ? suffix.getActions().firstSymbol() : null;
		}

		public static int computeLocalIndex(GeneralizedSymbolicSuffix suffix, int pId) {
			int idx = 0;
			for (ParameterizedSymbol a : suffix.getActions()) {
				idx += a.getArity();
				if (idx >= pId) {
					return pId - (idx - a.getArity()) - 1;
				}
			}
			return pId - 1;
		}

		public static Word<PSymbolInstance> buildQuery(Word<PSymbolInstance> prefix, GeneralizedSymbolicSuffix suffix,
				WordValuation values) {

			Word<PSymbolInstance> query = prefix;
			int base = 0;
			for (ParameterizedSymbol a : suffix.getActions()) {
				if (base + a.getArity() > values.size()) {
					break;
				}
				DataValue[] vals = new DataValue[a.getArity()];
				for (int i = 0; i < a.getArity(); i++) {
					vals[i] = values.get(base + i + 1);
				}
				query = query.append(new PSymbolInstance(a, vals));
				base += a.getArity();
			}
			return query;
		}
		
		public static int findLeftMostEqual(GeneralizedSymbolicSuffix suffix, int pId) {        
	        //System.out.println("findLeftMostEqual (" + pId + "): " + suffix);
	        DataType t = suffix.getDataValue(pId).getType();
	        for (int i=1; i<pId; i++) {
	            if (!t.equals(suffix.getDataValue(i).getType())) {
	                continue;
	            }            
	            if (suffix.getSuffixRelations(i, pId).contains(EQ)) return i;
	        }
	        return pId;
	    }
}
