package de.learnlib.ralib.equivalence.wmethod.partref;

import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_POP;
import static de.learnlib.ralib.example.stack.StackAutomatonExample.I_PUSH;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.example.stack.IOStackAutomatonExample;
import de.learnlib.ralib.example.stack.PureIOStackAutomatonExample;
import de.learnlib.ralib.example.stack.StackAutomatonExample;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class TestPartitionRefinement extends RaLibTestSuite {

    @Test
    public void testPartitionRefinementOnStack() {
        testRunner(StackAutomatonExample.AUTOMATON, I_PUSH, I_POP);
    }

    @Test
    public void testPartitionRefinementOnIOStack() {
        testRunner(IOStackAutomatonExample.AUTOMATON, I_PUSH, I_POP);
    }

    @Test
    public void testPartitionRefinementOnPureIOStack() {
        testRunner(PureIOStackAutomatonExample.AUTOMATON, I_PUSH, I_POP);
    }

    private void testRunner(RegisterAutomaton ra, ParameterizedSymbol ... inputs) {
        PartitionRefinement partref = new PartitionRefinement(ra, inputs);

        Partition part = null;
        while (partref.hasNext()) {
            part = partref.next();
        }

        Assert.assertNotNull(part);
        Assert.assertTrue(part.getFullyIdentifiedRALocations().containsAll(ra.getInputStates()));
    }
}
