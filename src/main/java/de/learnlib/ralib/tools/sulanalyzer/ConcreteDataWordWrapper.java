package de.learnlib.ralib.tools.sulanalyzer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.exceptions.DecoratedRuntimeException;
import de.learnlib.ralib.sul.DataWordSUL;
import de.learnlib.ralib.tools.classanalyzer.FieldConfig;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class ConcreteDataWordWrapper extends DataWordSUL {
	private final Class<? extends ConcreteSUL> sulClass;

    private final Map<String, ParameterizedSymbol> outputLookup;

    private ConcreteSUL sul = null;

    private FieldConfig fieldConfigurator;
    

    public ConcreteDataWordWrapper(Class<? extends ConcreteSUL> sulClass,
			LinkedHashMap<String, ParameterizedSymbol> outputLookup, FieldConfig fieldConfigurator) {
    	this.sulClass = sulClass;
    	this.fieldConfigurator = fieldConfigurator;
    	this.outputLookup = outputLookup;
	}

	@Override
    public void pre() {
        //System.out.println("----------");
        countResets(1);
        try {
            sul = sulClass.newInstance();
            if (this.fieldConfigurator != null)
            	this.fieldConfigurator.setFields(sul);
            sul.pre();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void post() {
    	sul.post();
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
    	if (outSym == null) 
    		throw new DecoratedRuntimeException("Undefined output received. Should be added to output alphabet.")
    		.addDecoration("output", output);
    	DataValue [] dvs = new DataValue [outSym.getArity()];
    	for (int i=0; i<outSym.getArity(); i++) 
    		dvs[i] = new DataValue(outSym.getPtypes()[i], output.getParameterValues()[i]);
    	return new PSymbolInstance(outSym, dvs);
    }
}
