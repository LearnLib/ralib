package de.learnlib.ralib.tools.sockanalyzer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.SULFactory;
import de.learnlib.ralib.words.ParameterizedSymbol;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class SocketAnalyzerSULFactory implements SULFactory{
	private SocketWrapper sock;
	private int maxDepth;
	private LinkedHashSet<ParameterizedSymbol> inputs;
	private Map<String, ParameterizedSymbol> outputSymbolsMap;
	
	public SocketAnalyzerSULFactory(String ipAddress, int portNumber, int maxDepth, List<ParameterizedSymbol> inputs,
			List<ParameterizedSymbol> outputs) {
		this.maxDepth = maxDepth;
		this.sock = new SocketWrapper(ipAddress, portNumber);
		this.inputs = new LinkedHashSet<>(inputs);
		this.outputSymbolsMap = new LinkedHashMap<>();
		outputs.forEach(out -> this.outputSymbolsMap.put(out.getName(), out));
		
	}

	public DataWordSUL newSUL() {
		SocketAnalyzerSUL socketSUL = new SocketAnalyzerSUL(this.sock, this.maxDepth, this.inputs, this.outputSymbolsMap);
		return socketSUL;
	}

	public boolean isParallelizable() {
		return false;
	}

	public DataWordSUL[] newIndependentSULs(int numInstances) {
		throw new NotImplementedException();
	}

}
