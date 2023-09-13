package de.learnlib.ralib.dt;

import de.learnlib.ralib.data.VarMapping;
import de.learnlib.ralib.data.util.PIVRemappingIterator;

public class DTBranch {

    private PathResult urap;

    private DTNode child;

    public DTBranch(DTNode child, PathResult row) {
        //this.sdt = sdt;
        this.child = child;
        this.urap = row;
        child.setParentBranch(this);
    }

    public DTBranch(DTBranch b) {
        child = b.child.copy();
        urap = b.urap.copy(); //todo: should we copy?
        child.setParentBranch(this);
    }

    public void setChild(DTNode child) {
        this.child = child;
        child.setParentBranch(this);
    }

    public DTNode getChild() {
        return child;
    }

    public boolean matches(PathResult r) {
        if (!urap.couldBeEquivalentTo(r)) {
            return false;
        }

        PIVRemappingIterator iterator = new PIVRemappingIterator(
                r.getParsInVars(), urap.getParsInVars());

        for (VarMapping m : iterator) {
            if (r.isEquivalentTo(urap, m)) {
                return true;
            }
        }
        return false;
    }

    PathResult getUrap() {
        return urap;
    }
}
