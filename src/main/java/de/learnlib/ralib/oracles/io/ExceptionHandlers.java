package de.learnlib.ralib.oracles.io;
import java.util.Collection;
import java.util.List;

import de.learnlib.api.Query;
import de.learnlib.ralib.oracles.DataWordOracle;
import de.learnlib.ralib.oracles.QueryCounter;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;


/**
 * Wrapper classes for the exceptional cases
 */
public class ExceptionHandlers{
	
	public static IOOracle wrapIOOracle(IOOracle oracle) {
		return new IOOracleWrapper(oracle);
	}
	
	public static DataWordIOOracle wrapDataWordIOOracle(DataWordIOOracle oracle) {
		return new DataWordIOOracleWrapper(oracle);
	}
	
	
	static class DataWordIOOracleWrapper implements DataWordIOOracle, ExceptionHandler {
		private  DataWordIOOracle oracle;
		public DataWordIOOracleWrapper(DataWordIOOracle oracle) {
			this.oracle = oracle;
			
		}
	
		public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
			this.exceptionHandler( ()-> { 
				this.oracle.processQueries(queries);
				return null;
			}); 
		}
		
		public Boolean answerQuery(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix) {
			Boolean result = this.exceptionHandler(() -> oracle.answerQuery(prefix, suffix));
			return result;
		}
		
		@Override
		public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
			Word<PSymbolInstance> trace = this.exceptionHandler(() -> ((IOOracle) this.oracle).trace(query));
			return trace;
		}
	}
	
	
	static class DataWordOracleWrapper extends QueryCounter implements DataWordOracle, ExceptionHandler {
		public DataWordOracleWrapper(DataWordOracle oracle) {
			this.oracle = oracle;
		}
	
		private DataWordOracle oracle;

		public void processQueries(Collection<? extends Query<PSymbolInstance, Boolean>> queries) {
			this.exceptionHandler( ()-> { 
				this.oracle.processQueries(queries);
				return null;
			}); 
		}
		
		public Boolean answerQuery(Word<PSymbolInstance> prefix, Word<PSymbolInstance> suffix) {
			Boolean result = this.exceptionHandler(() -> oracle.answerQuery(prefix, suffix));
			return result;
		}
	}



	static class IOOracleWrapper implements IOOracle, ExceptionHandler{
		private IOOracle oracle;

		public IOOracleWrapper(IOOracle oracle) {
			this.oracle = oracle;
		}
		
		@Override
		public Word<PSymbolInstance> trace(Word<PSymbolInstance> query) {
			Word<PSymbolInstance> trace = this.exceptionHandler(() -> ((IOOracle) this.oracle).trace(query));
			return trace;
		}
		
		@Override
		public List<Word<PSymbolInstance>> traces(List<Word<PSymbolInstance>> query) {
			List<Word<PSymbolInstance>> traces = this.exceptionHandler(() -> ((IOOracle) this.oracle).traces(query));
			return traces;
		}

	}

}
