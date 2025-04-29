package de.learnlib.ralib.dt;

import de.learnlib.ralib.data.Bijection;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.data.util.RemappingIterator;

public class DTBranch {

    private final PathResult urap;

    private DTNode child;

    public DTBranch(DTNode child, PathResult row) {
        //this.sdt = sdt;
        this.child = child;
        this.urap = row;
        child.setParentBranch(this);
    }

    public DTBranch(DTBranch b) {
        child = b.child.copy();
        urap = b.urap.copy();
        child.setParentBranch(this);
    }

    public void setChild(DTNode child) {
        this.child = child;
        child.setParentBranch(this);
    }

    public DTNode getChild() {
        return child;
    }

    /**
     * null if there is no match
     *
     * @param r
     * @return
     */
    public Bijection<DataValue> matches(PathResult r) {
        if (!urap.couldBeEquivalentTo(r)) {
            return null;
        }

        RemappingIterator<DataValue> iterator = new RemappingIterator<>(
                r.memorableValues(), urap.memorableValues());

        for (Bijection<DataValue> m : iterator) {
            //System.out.println("m: " + m);
            if (r.isEquivalentTo(urap, m)) {
                return m;
            }
        }
        return null;
    }

    PathResult getUrap() {
        return urap;
    }
}
