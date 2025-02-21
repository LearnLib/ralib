package de.learnlib.ralib.smt;

import com.google.common.base.Function;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.Mapping;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.ExpressionVisitor;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class SMTUtils {

    public static Valuation compose(Mapping<? extends SymbolicDataValue, DataValue>... varVals) {
        Valuation val = new Valuation();
        //System.out.println(Arrays.toString(varVals));
        Arrays.stream(varVals).sequential().flatMap( vv -> vv.entrySet().stream() ).forEach( e -> {
            if (val.containsValueFor(e.getKey())) {
                assert false;
            }
            val.setValue(e.getKey(), e.getValue().getValue());
        });
        return val;
    }

    public static Constant constantFor(DataValue sv) {
        return new Constant(BuiltinTypes.DECIMAL, sv.getValue());
    }

    public static Collection<SymbolicDataValue> getSymbolicDataValues(
            Expression<Boolean> expr) {
        ArrayList<SymbolicDataValue> list = new ArrayList<>();
        for (Variable v : ExpressionUtil.freeVariables(expr)) {
            list.add( (SymbolicDataValue) v);
        }
        return list;
    }

    public static Expression<Boolean> renameVars(Expression<Boolean> expr,
                                              final VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue> relabelling) {
        final ReplacingVarsVisitor replacer = new ReplacingVarsVisitor();
        return replacer.apply(expr, relabelling);
    }
}
