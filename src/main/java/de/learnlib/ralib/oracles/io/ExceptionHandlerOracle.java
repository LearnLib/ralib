package de.learnlib.ralib.oracles.io;
import java.util.Collection;

import de.learnlib.api.Query;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.exceptions.NonDeterminismException;
import de.learnlib.ralib.exceptions.SULRestartException;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;


/**
 * A wrapper class for the exceptional cases
 */
public class ExceptionHandlerOracle extends IOOracle implements DataWordOracle, ExceptionHandler{
	public DataWordOracle oracle;
	public <T extends DataWordOracle,IOOracle> ExceptionHandlerOracle(T oracle) {
		this.oracle = oracle;
	}
	

	public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
		int nonDet = 0, sulRest = 0;
		DecoratedRuntimeException lastExc = null;
		
		while (nonDet < NON_DET_ATTEMPTS && sulRest < SUL_RESTART_ATTEMPTS) {
			try {
				this.oracle.processQueries(queries);
				return;
			} catch(NonDeterminismException exc) {
				nonDet ++;
				lastExc = exc;
			} catch(SULRestartException exc) {
				sulRest ++;
				System.out.println("SUL issued restart");
				lastExc = exc;
			}
		}
		throw lastExc;
	}

	public Boolean answerQuery(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix) {
		int nonDet = 0, sulRest = 0;
		DecoratedRuntimeException lastExc = null;
		
		while (nonDet < NON_DET_ATTEMPTS && sulRest < SUL_RESTART_ATTEMPTS) {
			try {
				Boolean ret = this.oracle.answerQuery(prefix, suffix);
				return ret;
			} catch(NonDeterminismException exc) {
				nonDet ++;
				lastExc = exc;
			} catch(SULRestartException exc) {
				sulRest ++;
				System.out.println("SUL issued restart");
				lastExc = exc;
			}
		}
		throw lastExc;
	}


	@Override
	public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
		Word<PSymbolInstance> trace = ((IOOracle) this.oracle).trace(query);
		return trace;
	}

}
