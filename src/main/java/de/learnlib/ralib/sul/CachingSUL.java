package de.learnlib.ralib.sul;

import de.learnlib.api.SULException;
import de.learnlib.ralib.oracles.io.IOCache;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

/**
 * A SUL with caching functionality.
 */
public class CachingSUL extends DataWordSUL{

    private DataWordSUL sul;
    private IOCache cache;
    private Word<PSymbolInstance> trace;
    private boolean answerFromCache = false;

    public CachingSUL(DataWordSUL sul, IOCache cache) {
        this.sul = sul;
        this.cache = cache;
    }

    @Override
    public void pre() {
        answerFromCache = true;
        trace = Word.epsilon();
    }

    @Override
    public void post() {
        if (!answerFromCache) {
            sul.post();
        }
    }

    @Override
    public PSymbolInstance step(PSymbolInstance in) throws SULException {
        PSymbolInstance out = null;
        trace = trace.append(in);
        if (answerFromCache) {
            Word<PSymbolInstance> trWithResp = cache.traceFromCache(trace.append(new PSymbolInstance(new OutputSymbol("__cache_dummy"))));
            if (trWithResp != null) {
                out = trWithResp.lastSymbol();
            } else {
                answerFromCache = false;
                sul.pre();
                for (int i=0; i< trace.length(); i=i+2) {
                    out = sul.step(trace.getSymbol(i));
                }
            }
        }
        else {
            out = sul.step(in);
        }
        trace = trace.append(out);
        if (!answerFromCache) {
            cache.addToCache(trace);
        }
        return out;
    }

}
