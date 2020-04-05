package de.learnlib.ralib.sul;

import java.util.Map;
import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.mapper.MultiTheoryDeterminizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;

public class DeterminzerDataWordSUL extends DataWordSUL{
	
	private final DataWordSUL sul;
	private final Supplier<MultiTheoryDeterminizer> canonizerFactory;
	
	private MultiTheoryDeterminizer canonizer;
	
	public DeterminzerDataWordSUL(Map<DataType, Theory> teachers, Constants constants, DataWordSUL sul) {
		this.canonizerFactory = () -> MultiTheoryDeterminizer.buildNew(teachers, constants);
		this.sul = sul;
	}

	@Override
	public void pre() {
		 countResets(1);
		 this.sul.pre();
		 this.canonizer = canonizerFactory.get();
	}

	@Override
	public void post() {
		this.sul.post();
	}

	@Override
	public PSymbolInstance step(PSymbolInstance input) throws SULException {
        countInputs(1);
        // de-canonize input before sending it to the SUL
        input = this.canonizer.canonize(input, true);
        
        PSymbolInstance output = this.sul.step(input);
       
        // canonize output 
        output = this.canonizer.canonize(output, false);
      

        return output;
	}
	
}
