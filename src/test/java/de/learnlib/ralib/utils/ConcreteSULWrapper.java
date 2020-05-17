package de.learnlib.ralib.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteInput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteOutput;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteSUL;

/**
 * Convenient wrapper to generate a {@link ConcreteSUL} out of any basic SUL class. 
 */
public class ConcreteSULWrapper<T> extends ConcreteSUL{
	public static final String OK = "OK";
	public static final String NOK = "NOK";
	public static final String OUTPUT = "OUTPUT";
	public static final String VOID = "VOID";
	public static final String NULL = "NULL";
	
	
	private Class<T> klass;
	private Supplier<T> generator;
	private T sut;
	
	
	public ConcreteSULWrapper(Class<T> klass, Supplier<T> generator) {
		this.klass = klass;
		this.generator = generator;
	}
	
	@Override
	public void pre() {
		sut = generator.get();
	}
	
	@Override
	public void post() {
	}
	
	private ConcreteOutput createOutput(Method method, Object returnedValue) {
		if (method.getReturnType() == null) {
			return new ConcreteOutput(VOID);
		} else if (returnedValue == null) {
			return new ConcreteOutput(NULL);
		} else if (returnedValue instanceof Boolean) {
			if (Boolean.TRUE.equals(returnedValue)) {
				return new ConcreteOutput("OK");
			} else {
				return new ConcreteOutput("NOK");
			}
		} else {
			return new ConcreteOutput("OUTPUT", returnedValue);
		}
	}

	@Override
	public ConcreteOutput step(ConcreteInput in) throws SULException {
		Method method;
		try {
			method = getMethod(in); 
			Object result = method.invoke(sut, in.getParameterValues());
			ConcreteOutput output = createOutput(method, result);
			return output;
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/* 
	 * We could instead get the method via Class::getMethod(name,array of class types), however that will not work
	 * if the class type of a parameter in a ConcreteInput is a subclass of the corresponding parameter in the method's signature. 
	 */
	private Method getMethod(ConcreteInput in) throws NoSuchMethodException {
		Optional<Method> result = Arrays.stream(klass.getMethods())
				.filter(m -> m.getName().equals(in.getMethodName()))
				.filter(m -> m.getParameterCount() == in.getParameterValues().length)
				.findFirst();
		if (!result.isPresent()) {
			throw new NoSuchMethodException(in.getMethodName());
		} else {
			return result.get();
		}
	}
}
