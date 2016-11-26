package de.learnlib.ralib.automata.guards;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

/**
 * TODO 
 * This could be merged with AtomicGuardExpression class, and both Left and Right should be made expressions.
 */
public class SumCAtomicGuardExpression<Left extends SymbolicDataValue, Right extends SymbolicDataValue> extends AtomicGuardExpression<Left, Right>{
	
	private DataValue<?> lConst;

	private DataValue<?> rConst;
    
    public SumCAtomicGuardExpression(Left left, DataValue<?> lConst, Relation relation, Right right, DataValue<?> rConst) {
       super(left, relation, right);
        this.lConst = lConst;
        this.rConst = rConst;
    }
    
    
    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {        
        
        DataValue lv = DataValue.add(val.get(this.getLeft()), lConst);
        DataValue rv = DataValue.add(val.get(this.getRight()), rConst);

        assert lv != null && rv != null;
                        
       return super.isSatisfied(lv, rv, super.relation);
    }
               
    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        SymbolicDataValue newLeft = (SymbolicDataValue) relabelling.get(left);
        if (newLeft == null) {
            newLeft = getLeft();
        }
        SymbolicDataValue newRight = (SymbolicDataValue) relabelling.get(right);
        if (newRight == null) {
            newRight = right;
        }
        
        return new SumCAtomicGuardExpression<SymbolicDataValue, SymbolicDataValue>
        (newLeft, lConst, relation, newRight, rConst);
    }

    
    @Override
    public String toString() {
        return "(" + left + (lConst!=null? (" + " + lConst):"" ) + relation + right + 
        		(rConst!=null?(" + " + rConst):"") + ")";
    }

    public DataValue<?> getLeftConst() {
    	return lConst;
    }
    
    public DataValue<?> getRightConst() {
    	return rConst;
    }

    public Relation getRelation() {
        return relation;
    }
}