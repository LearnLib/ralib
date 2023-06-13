package de.learnlib.ralib.sul;

import de.learnlib.api.SULException;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * A SUL with caching functionality.
 */
public class CachingSUL extends DataWordSUL{

    private IOCache cache;
    private Word<PSymbolInstance> trace;

    public CachingSUL(IOCache cache) {
        this.cache = cache;
    }

    @Override
    public void pre() {
        this.trace = Word.epsilon();
    }

    @Override
    public void post() {
    }

    @Override
    public PSymbolInstance step(PSymbolInstance in) throws SULException {
        PSymbolInstance out;
        Word<PSymbolInstance> extendedTrace = trace.append(in);
        trace = cache.trace(extendedTrace);
        out = trace.lastSymbol();
        return out;
    }

}
