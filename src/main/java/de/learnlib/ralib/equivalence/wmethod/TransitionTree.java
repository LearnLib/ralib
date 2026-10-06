package de.learnlib.ralib.equivalence.wmethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.learnlib.ralib.automata.InputTransition;
import de.learnlib.ralib.automata.MutableRegisterAutomaton;
import de.learnlib.ralib.automata.RALocation;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.automata.Transition;
import de.learnlib.ralib.automata.output.OutputTransition;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class TransitionTree {

    private class TransitionTreeConstructor {

        private final MutableRegisterAutomaton tree;

        private final Map<RALocation, RALocation> nodeToRaLoc;

        public TransitionTreeConstructor() {
            this.tree = new MutableRegisterAutomaton();
            this.nodeToRaLoc = new LinkedHashMap<>();
        }

        public Map<RALocation, RALocation> getNodeToRaLocMap() {
            return nodeToRaLoc;
        }

        public RegisterAutomaton constructTransitionTree(RALocation initial) {
            RALocation root = tree.addInitialState(initial.isAccepting());
            nodeToRaLoc.put(root, initial);

            addTransitions(root, List.of(initial));

            return tree;
        }

        private void addTransitions(RALocation node, List<RALocation> raPath) {
            for (Transition raTransition : raPath.getLast().getOut()) {
                RALocation dest = raTransition.getDestination();
                ParameterizedSymbol ps = raTransition.getLabel();
                RALocation nextNode = addNode(dest, raPath);
                Transition edge = createTransition(node, nextNode, raTransition);
                tree.addTransition(node, ps, edge);
            }
        }

        private Transition createTransition(RALocation sourceNode, RALocation destNode, Transition raTransition) {
            return raTransition instanceof OutputTransition ot ?
                    new OutputTransition(ot.getGuard(), ot.getOutput(), ot.getLabel(), sourceNode, destNode, ot.getAssignment()) :
                        new InputTransition(raTransition.getGuard(), (InputSymbol) raTransition.getLabel(), sourceNode, destNode, raTransition.getAssignment());
        }

        private RALocation addNode(RALocation raLoc, List<RALocation> raPath) {
            RALocation node = tree.addState(raLoc.isAccepting());
            nodeToRaLoc.put(node, raLoc);
            if (!raPath.contains(raLoc)) {
                List<RALocation> next = new ArrayList<>(raPath);
                next.add(raLoc);
                addTransitions(node, next);
            }
            return node;
        }
    }

    private final RegisterAutomaton tree;

    private final Map<RALocation, RALocation> nodeToRaLoc;

    public TransitionTree(RegisterAutomaton model, RALocation initial) {
        TransitionTreeConstructor treeConstr = new TransitionTreeConstructor();
        this.tree = treeConstr.constructTransitionTree(initial);
        this.nodeToRaLoc = treeConstr.getNodeToRaLocMap();
    }

    public TransitionTree(RegisterAutomaton model) {
        this(model, model.getInitialState());
    }

    public RALocation getRoot() {
        return tree.getInitialState();
    }

    public Collection<Transition> getTransitons(RALocation loc) {
        return loc.getOut();
    }

    public RALocation getRaLocation(RALocation node) {
        return nodeToRaLoc.get(node);
    }
}
