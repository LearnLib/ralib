package de.learnlib.ralib.tools;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.words.ParameterizedSymbol;

/**
 * Reads all SUL related configurations such as I/O alphabet, constants and theories
 */
public abstract class SULParser {
	public abstract void parseConfig(Configuration config) throws ConfigurationException;
	
	public abstract ParameterizedSymbol [] getInputs();
	public abstract ParameterizedSymbol [] getOutput();
	public final ParameterizedSymbol [] getAlphabet() {
		return Stream.concat(Arrays.stream(this.getInputs()), Arrays.stream(this.getOutput())).toArray(ParameterizedSymbol []:: new);
	} 
	public abstract Map<String, DataType> getTypes();
	//public abstract Map<DataType, Theory> getTeachers();
	/**
	 * Instantiates a naked SUL.
	 */
	public abstract DataWordSUL newSUL();
	public String targetName() {
		return "SUL";
	}
}
