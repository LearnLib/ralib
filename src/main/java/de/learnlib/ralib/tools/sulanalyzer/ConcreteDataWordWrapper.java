package de.learnlib.ralib.tools.sulanalyzer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.classanalyzer.FieldConfig;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class ConcreteDataWordWrapper extends DataWordSUL {
	private final Class<? extends ConcreteSUL> sulClass;

    private final Map<String, ParameterizedSymbol> outputLookup;

    private ConcreteSUL sul = null;

    private FieldConfig fieldConfigurator;

    public ConcreteDataWordWrapper(Class<? extends ConcreteSUL> sulClass, ParameterizedSymbol [] outputs) {
        this(sulClass, outputs, null);
    }
    
    public ConcreteDataWordWrapper(Class<? extends ConcreteSUL> sulClass, ParameterizedSymbol [] outputs, FieldConfig fieldConfiguration) {
    	this.sulClass = sulClass;
        this.outputLookup = new LinkedHashMap<>();
        Arrays.asList(outputs).forEach(out ->  
        this.outputLookup.put(out.getName(), out));
        this.fieldConfigurator = fieldConfiguration;
    }

    @Override
    public void pre() {
        //System.out.println("----------");
        countResets(1);
        try {
            sul = sulClass.newInstance();
            if (this.fieldConfigurator != null)
            	this.fieldConfigurator.setFields(sul);
            	
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void post() {
        sul = null;
    }

    @Override
    public PSymbolInstance step(PSymbolInstance i) throws SULException {
        countInputs(1);
        ConcreteInput input = toConcreteInput(i);
        ConcreteOutput output = this.sul.step(input);
        PSymbolInstance o = toPSymbolInstance(output);
        return o;

    }
    
    private ConcreteInput toConcreteInput(PSymbolInstance input) {
    	Object[] inputParams = Arrays.stream(input.getParameterValues()).map(dv -> dv.getId()).toArray();
    	return new ConcreteInput(input.getBaseSymbol().getName(), inputParams);
    }
    
    public PSymbolInstance toPSymbolInstance(ConcreteOutput output) {
    	ParameterizedSymbol outSym = this.outputLookup.get(output.getMethodName());
    	DataValue [] dvs = new DataValue [outSym.getArity()];
    	for (int i=0; i<outSym.getArity(); i++) 
    		dvs[i] = new DataValue(outSym.getPtypes()[i], output.getParameterValues()[i]);
    	return new PSymbolInstance(outSym, dvs);
    }
}
