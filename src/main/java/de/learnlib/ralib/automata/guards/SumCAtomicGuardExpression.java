package de.learnlib.ralib.automata.guards;

import java.util.Collection;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;

/**
 * TODO 
 * This could be merged with AtomicGuardExpression class, and both Left and Right should be made expressions.
 */
public class SumCAtomicGuardExpression<Left extends SymbolicDataValue, Right extends SymbolicDataValue> extends GuardExpression {
	
    private final Left left; 
        
    private final Relation relation;

    private final Right right;

	private DataValue<?> lConst;

	private DataValue<?> rConst;
    
    public SumCAtomicGuardExpression(Left left, DataValue<?> lConst, Relation relation, Right right, DataValue<?> rConst) {
        this.left = left;
        this.relation = relation;
        this.lConst = lConst;
        this.rConst = rConst;
        this.right = right;
    }
    
    
    @Override
    public boolean isSatisfied(Mapping<SymbolicDataValue, DataValue<?>> val) {        
        
        DataValue lv = DataValue.add(val.get(left), lConst);
        DataValue rv = DataValue.add(val.get(right), rConst);

        assert lv != null && rv != null;
                        
        switch (relation) {
            case EQUALS: 
                return lv.equals(rv);
            case NOT_EQUALS: 
                return !lv.equals(rv);

            case GREATER:
            case LESSER:
            case LSREQUALS:
            case GREQUALS:
                return numCompare(lv, rv, relation);
            
            case SUCC:
            case IN_WIN:
            case NOT_IN_WIN:
                return succ(lv, rv, relation);
                
            default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not suported in guards");
        }
    }
               
    @Override
    public GuardExpression relabel(VarMapping relabelling) {
        SymbolicDataValue newLeft = (SymbolicDataValue) relabelling.get(left);
        if (newLeft == null) {
            newLeft = left;
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

    public Left getLeft() {
        return left;
    }

    public Right getRight() {
        return right;
    }    
    
    public DataValue<?> getLeftConst() {
    	return lConst;
    }
    
    public DataValue<?> getRightConst() {
    	return rConst;
    }

    @Override
    protected void getSymbolicDataValues(Set<SymbolicDataValue> vals) {
        vals.add(left);
        vals.add(right);
    }

    public Relation getRelation() {
        return relation;
    }

    private boolean numCompare(DataValue l, DataValue r, Relation relation) {        
        if (!l.getType().equals(r.getType())) {
            return false;
        }
        Comparable lc = (Comparable) l.getId();
        int result = lc.compareTo(r.getId());        
        switch (relation) {
            case LESSER:
                return result < 0;
            case GREATER:
                return result > 0;
            case LSREQUALS:
                return result <= 0;
            case GREQUALS:
                return result >= 0;
            default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not supoorted in guards");   
        }
    }
    
    private boolean succ(DataValue lv, DataValue rv, Relation relation) {
        if (!(lv.getId() instanceof Number) || !(rv.getId() instanceof Number)) {
			return false;
    	}	else {
    		double val1 = ((Number) lv.getId()).doubleValue();
    		double val2 = ((Number) rv.getId()).doubleValue();
    		switch(relation) {
    		case IN_WIN: return val2 > val1 + 1 && val2 <= val1 + 100;
    		case NOT_IN_WIN: return val2 <= val1 + 1 || val2 > val1 + 100;
    		case SUCC: return val2 == val1+1;
    		case NOT_SUCC: return val2 != val1+1;
    		default:
    	    throw new UnsupportedOperationException(
                    "Relation " + relation + " is not suported in succ guards");
    		}
    }
        }


	@Override
	protected void getAtoms(Collection<AtomicGuardExpression> vals) {
		// TODO Auto-generated method stub
		
	}	
	
	
}
