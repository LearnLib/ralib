package de.learnlib.ralib.example.llambda;

import de.learnlib.ralib.automata.Assignment;
import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.TransitionGuard;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.words.InputSymbol;

public class LLambdaAutomatonExample {

    public static final InputSymbol A = 
            new InputSymbol("a", new DataType[] {}); 
    
    public static final InputSymbol B = 
            new InputSymbol("b", new DataType[] {});
    
    public static final RegisterAutomaton AUTOMATON = buildAutomaton();
    
    private LLambdaAutomatonExample() {      
    }

    private static RegisterAutomaton buildAutomaton() {
        MutableRegisterAutomaton ra = new MutableRegisterAutomaton();        
        
        // locations
        RALocation l0 = ra.addInitialState(true);
        RALocation l1 = ra.addState(false);
        RALocation l2 = ra.addState(false);
        RALocation l3 = ra.addState(false);
        RALocation l4 = ra.addState(true);
        RALocation l5 = ra.addState(false);
        RALocation l6 = ra.addState(false);
        
        // registers and parameters
        
        // guards
        TransitionGuard trueGuard   = new TransitionGuard();
        
        // assignments
        VarMapping<Register, SymbolicDataValue> noMapping = new VarMapping<Register, SymbolicDataValue>();
        
        Assignment noAssign     = new Assignment(noMapping);
        
        // initial location
        ra.addTransition(l0, A, new InputTransition(trueGuard, A, l0, l1, noAssign));
        ra.addTransition(l0, B, new InputTransition(trueGuard, B, l0, l5, noAssign));
        

        ra.addTransition(l1, A, new InputTransition(trueGuard, A, l1, l2, noAssign));
        ra.addTransition(l1, B, new InputTransition(trueGuard, B, l1, l5, noAssign));

        ra.addTransition(l2, A, new InputTransition(trueGuard, A, l2, l3, noAssign));
        ra.addTransition(l2, B, new InputTransition(trueGuard, B, l2, l6, noAssign));

        ra.addTransition(l3, A, new InputTransition(trueGuard, A, l3, l4, noAssign));
        ra.addTransition(l3, B, new InputTransition(trueGuard, B, l3, l3, noAssign));

        ra.addTransition(l4, A, new InputTransition(trueGuard, A, l4, l4, noAssign));
        ra.addTransition(l4, B, new InputTransition(trueGuard, B, l4, l4, noAssign));

        ra.addTransition(l5, A, new InputTransition(trueGuard, A, l5, l2, noAssign));
        ra.addTransition(l5, B, new InputTransition(trueGuard, B, l5, l6, noAssign));

        ra.addTransition(l6, A, new InputTransition(trueGuard, A, l6, l2, noAssign));
        ra.addTransition(l6, B, new InputTransition(trueGuard, B, l6, l0, noAssign));
        
        return ra;
    }
    
}
