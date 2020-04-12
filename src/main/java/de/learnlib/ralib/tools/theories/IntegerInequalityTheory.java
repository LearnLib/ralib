package de.learnlib.ralib.tools.theories;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.theory.inequality.DiscreteDomainInequalityMerger;
import de.learnlib.ralib.tools.classanalyzer.TypedTheory;

public abstract class IntegerInequalityTheory  extends NumberInequalityTheory<Integer> implements TypedTheory<Integer>{

    public IntegerInequalityTheory() {
    	super(new DiscreteDomainInequalityMerger());
    }

    public IntegerInequalityTheory(DataType<Integer> t) {
    	this();
    	super.setType(t);
    }
    
    @Override
    protected List<Range<Integer>> generateRangesFromPotential(List<DataValue<Integer>> potential) {
		int potSize = potential.size();
		List<Range<Integer>> ranges = new ArrayList<Range<Integer>>(potential.size());
		for (int i = 1; i < potSize; i++)   {
			if (potential.get(i).getId() - potential.get(i-1).getId() > 1)
				ranges.add(new Range<Integer>(potential.get(i-1), potential.get(i)));
		}
		return ranges;
	} 
}
