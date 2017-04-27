package de.learnlib.ralib.sul.tcp;

import java.util.Arrays;
import java.util.LinkedHashMap;

import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.ParallelizableSULFactory;
import de.learnlib.ralib.tools.SULParser;
import de.learnlib.ralib.tools.classanalyzer.FieldConfig;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteDataWordWrapper;
import de.learnlib.ralib.tools.sulanalyzer.ConcreteSUL;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class TCPSULFactory implements ParallelizableSULFactory {

	private Class<? extends ConcreteSUL> sulClass;
	private LinkedHashMap<String, ParameterizedSymbol> outputLookup;
	private FieldConfig fieldConfigurator;

	public TCPSULFactory(SULParser parser) {
		this.sulClass = TCPAdapterSUL.class;
		this.outputLookup = new LinkedHashMap<>();
		ParameterizedSymbol[] outputs = parser.getOutput();
		Arrays.asList(outputs).forEach(out -> this.outputLookup.put(out.getName(), out));
		this.fieldConfigurator = new FieldConfig(this.sulClass, new String[] {});
	}

	public DataWordSUL newSUL() {
		ConcreteDataWordWrapper sul = new ConcreteDataWordWrapper(this.sulClass, this.outputLookup,
				this.fieldConfigurator);
		return sul;
	}

	/**
	 * Instantiates SULs with incremental sender port numbers starting from the port in the properties file. 
	 */
	public DataWordSUL [] newIndependentSULs(int numInstances) {
		try {
			DataWordSUL [] suls = new DataWordSUL[numInstances];
			TCPConfig nextConfig = TCPConfig.readPropFile();
			for (int i=0; i<numInstances; i++) {
				final TCPConfig config = nextConfig; 
				suls[i] = new ConcreteDataWordWrapper(() -> new TCPAdapterSUL(config), this.outputLookup,
						this.fieldConfigurator);
				nextConfig = new TCPConfig(config.senderAddress, config.senderPortNumber+1);
				
			}
			return suls;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		
	}

}
