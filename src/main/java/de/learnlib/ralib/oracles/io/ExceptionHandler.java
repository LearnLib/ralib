package de.learnlib.ralib.oracles.io;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.exceptions.NonDeterminismException;
import de.learnlib.ralib.exceptions.SULRestartException;

//TODO this could be made more general.
public interface ExceptionHandler {
	public static final int NON_DET_ATTEMPTS =3; // how many attempts are made before non determinism is signaled
	public static final int SUL_RESTART_ATTEMPTS =20; // sul restart is normal and should never be a problem
	
	public default <O> O exceptionHandler(Supplier<O> fun) {
		int nonDet = 0, sulRest = 0;
		DecoratedRuntimeException lastExc = null;
		
		while (nonDet < NON_DET_ATTEMPTS && sulRest < SUL_RESTART_ATTEMPTS) {
			try {
				//o..processQueries(queries);
				O result = fun.get();
				return result;
			} catch(NonDeterminismException exc) {
				System.out.println("Non determinism:" + exc);
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
}
