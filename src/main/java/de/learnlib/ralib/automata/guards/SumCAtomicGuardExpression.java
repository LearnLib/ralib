package de.learnlib.ralib.automata.guards;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Set;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;

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

            case BIGGER:
            case SMALLER:
                return numCompare(lv, rv, relation);
           
            case SUCC:
            case IN_WIN:
            case NOT_IN_WIN:
                return succ(lv, rv, relation);
                
            default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not supoorted in guards");
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
        
        return new SumCAtomicGuardExpression(newLeft, lConst, relation, newRight, rConst);
    }

    
    @Override
    public String toString() {
        return "(" + left + rConst + relation + right + rConst + ")";
    }

    public Left getLeft() {
        return left;
    }

    public Right getRight() {
        return right;
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
            case SMALLER:
                return result < 0;
            case BIGGER:
                return result > 0;
               default:
                throw new UnsupportedOperationException(
                        "Relation " + relation + " is not supoorted in guards");   
        }
    }
    
    private boolean succ(DataValue lv, DataValue rv, Relation relation) {
        if (!(lv.getId() instanceof Number) || !(rv.getId() instanceof Number)) {
			return false;
    	}	else {
    		int val1 = ((Number) lv.getId()).intValue();
    		int val2 = ((Number) rv.getId()).intValue();
    		switch(relation) {
    		case IN_WIN: return val2 > val1 + 1 && val2 <= val1 + 100;
    		case NOT_IN_WIN: return val2 <= val1 + 1 && val2 > val1 + 100;
    		case SUCC: return val2 == val1+1;
    		case NOT_SUCC: return val2 != val1+1;
    		default:
    	    throw new UnsupportedOperationException(
                    "Relation " + relation + " is not suported in succ guards");
    		}
    }
        }	
	
	
}
