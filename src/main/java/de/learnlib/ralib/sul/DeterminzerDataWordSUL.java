package de.learnlib.ralib.sul;

import java.util.Map;

import de.learnlib.api.SULException;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.mapper.MultiTheoryDeterminizer;
import de.learnlib.ralib.theory.Theory;
import de.learnlib.ralib.words.PSymbolInstance;

public class DeterminzerDataWordSUL extends DataWordSUL{
	
	private final DataWordSUL sul;
	
	private MultiTheoryDeterminizer canonizer;
	private Map<DataType, Theory> teachers;
	private Constants constants;
	
	public DeterminzerDataWordSUL(Map<DataType, Theory> teachers, Constants constants, DataWordSUL sul) {
		this.teachers = teachers;
		this.sul = sul;
		this.constants = constants;
	}

	@Override
	public void pre() {
		 countResets(1);
		 this.sul.pre();
		 this.canonizer = new MultiTheoryDeterminizer(this.teachers, constants);
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
