package de.learnlib.ralib.sul;

import java.util.function.Supplier;

import de.learnlib.api.SULException;
import de.learnlib.ralib.words.PSymbolInstance;

public class DeterminedDataWordSUL extends DataWordSUL{
	
	private final DataWordSUL sul;
	private final Supplier<ValueCanonizer> canonizerFactory;
	
	private ValueCanonizer canonizer;
	
	public DeterminedDataWordSUL(Supplier<ValueCanonizer> canonizedSupplier, DataWordSUL sul) {
		this.canonizerFactory = canonizedSupplier;
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
