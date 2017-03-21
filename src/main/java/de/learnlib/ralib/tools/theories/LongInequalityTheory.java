package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.DiscreteInequalityMerger;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;

public class LongInequalityTheory  extends NumberInequalityTheory<Long> implements TypedTheory<Long>{

    public LongInequalityTheory() {
    	super(new DiscreteInequalityMerger());
    }

    public LongInequalityTheory(DataType<Long> t) {
    	this();
    	super.setType(t);
    }
    
    @Override
    protected List<Range<Long>> generateRangesFromPotential(List<DataValue<Long>> potential) {
		int potSize = potential.size();
		List<Range<Long>> ranges = new ArrayList<Range<Long>>(potential.size());
		for (int i = 1; i < potSize; i++)   {
			if (potential.get(i).getId() - potential.get(i-1).getId() > 1)
				ranges.add(new Range<Long>(potential.get(i-1), potential.get(i)));
		}
		return ranges;
	} 
}
