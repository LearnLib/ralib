package de.learnlib.ralib.oracles.io;

import de.learnlib.api.SULException;
import de.learnlib.ralib.exceptions.NonDeterminismException;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;


/**
 * A very inefficient cache SUL implementation. However inefficient, it doesn't break IOCache encapsulation.
 */
public class CachingSUL extends DataWordSUL{
	
	private DataWordSUL sul;
	private IOCache cache;
	private Word<PSymbolInstance> trace;
	private boolean found = true;

	public CachingSUL(DataWordSUL sul, IOCache cache) {
		this.sul = sul;
		this.cache = cache;
	}

	@Override
	public void pre() {
		this.sul.pre();
		this.trace = Word.epsilon();
		this.found = true;
	}

	@Override
	public void post() {
		this.sul.post();
	}

	@Override
	public PSymbolInstance step(PSymbolInstance in) throws SULException {
		PSymbolInstance out = null;
		trace = this.trace.append(in);
		if (found) {
			Word<PSymbolInstance> trWithResp = this.cache.traceFromCache(trace.append(CanonizingIOCacheOracle.CACHE_DUMMY));
			if (trWithResp != null)
				out = trWithResp.lastSymbol();
			else {
				found = false;
				for (int i=0; i< trace.length(); i=i+2) {
					out = sul.step(trace.getSymbol(i));
					if (i+1 < trace.length()) {
						PSymbolInstance expected = trace.getSymbol(i+1);
						if (!out.equals(expected)) 
							throw new NonDeterminismException(trace.prefix(i), expected, out);
					}
				}
			}
		}
		else
			out = sul.step(in);
		trace = trace.append(out);
		if (!found) {
			this.cache.addToCache(trace);
		}
		return out;
	}

}
