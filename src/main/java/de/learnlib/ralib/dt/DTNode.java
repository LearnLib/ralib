package de.learnlib.ralib.dt;

public abstract class DTNode {
	protected DTInnerNode parent;
	protected DTBranch parentBranch;
	
	public DTNode() {
		parent = null;
		parentBranch = null;
	}
	
	public void setParent(DTInnerNode parent) {
		this.parent = parent;
	}
	
	public void setParentBranch(DTBranch b) {
		parentBranch = b;
	}
	
	public DTInnerNode getParent() {
		return parent;
	}
	
	public DTBranch getParentBranch() {
		return parentBranch;
	}
	
	public abstract boolean isLeaf();
}
