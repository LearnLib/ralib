package de.learnlib.ralib.utils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.SymbolicDataValue;
import de.learnlib.ralib.data.SymbolicDataValue.Constant;
import de.learnlib.ralib.data.SymbolicDataValue.Register;
import de.learnlib.ralib.data.SymbolicDataValue.SuffixValue;
import de.learnlib.ralib.oracles.mto.SDT;
import de.learnlib.ralib.oracles.mto.SDTLeaf;
import de.learnlib.ralib.theory.SDTGuard;
import de.learnlib.ralib.theory.SDTTrueGuard;
import de.learnlib.ralib.theory.equality.DisequalityGuard;
import de.learnlib.ralib.theory.equality.EqualityGuard;
import de.learnlib.ralib.theory.inequality.IntervalGuard;

/**
 * A builder meant to ease SDT construction. 
 * 
 * @author Paul
 */
public class SDTBuilder {
	private static String SDV_EXP = "[scr][1-9][0-9]*";
	
	private DataType dataType;
	
	private final Map<SDTGuard, SDTBuilder> children;
	private SDT leaf;
	
	// the parent of this build and the guard by which it is connected
	private SDTBuilder parent;
	private SDTGuard guard;
	private SuffixValue sVal;
	private int depth;
	
	public SDTBuilder(DataType dataType) {
		children = new LinkedHashMap<>();
		this.dataType = dataType;
		sVal = new SuffixValue(dataType, 1);
	}
	
	private SDTBuilder(SDTBuilder parent, SDTGuard guard) {
		children = new LinkedHashMap<>();
		dataType = parent.dataType;
		this.parent = parent;
		this.guard = guard;
		sVal = new SuffixValue(parent.dataType, parent.sVal.getId() + 1);
		parent.children.put(guard, this);
		depth = parent.depth + 1;
	}
	
	public SDTBuilder eq(String expr) {
		return new SDTBuilder(this, new EqualityGuard(sVal, sdv(expr)));
	}
	
	public SDTBuilder deq(String expr) {
		return new SDTBuilder(this, new DisequalityGuard(sVal, sdv(expr)));
	}
	
	public SDTBuilder lsr(String expr) {
		return new SDTBuilder(this, new IntervalGuard(sVal, null, sdv(expr)));
	}
	
	public SDTBuilder lsrEq(String expr) {
		return new SDTBuilder(this, new IntervalGuard(sVal, null, true, sdv(expr), false));
	}
	
	public SDTBuilder grt(String expr) {
		return new SDTBuilder(this, new IntervalGuard(sVal, sdv(expr), null));
	}
	
	public SDTBuilder grtEq(String expr) {
		return new SDTBuilder(this, new IntervalGuard(sVal, sdv(expr), false, null, true));
	}
	
	public SDTBuilder ineq(String leftExpr, boolean leftEq, String rightExpr, boolean rightEq) {
		return new SDTBuilder(this, new IntervalGuard(sVal, leftExpr != null ? sdv(leftExpr) : null, !leftEq, rightExpr != null ? sdv(rightExpr) : null, !rightEq));
	}
	
	public SDTBuilder tru() {
		return new SDTBuilder(this, new SDTTrueGuard(sVal));
	}
	
	public SDTBuilder accept() {
		leaf = SDTLeaf.ACCEPTING;
		return this;
	}
	
	public SDTBuilder reject() {
		leaf = SDTLeaf.REJECTING;
		return this;
	}
	
	/**
	 * Return the parent builder 
	 */
	public SDTBuilder up() {
		if (parent == null) {
			throw new InternalError("Cannot go up as the SDT under construction does not have a parent");
		}
		
		return parent;
	}
	
	/**
	 * Return ancestor builders of a given level (1 is parent, ..)
	 */
	public SDTBuilder up(int level) {
		if (level < 0) {
			throw new InternalError("Level cannot be negative");
		} else if (level > 0) {
			if (parent == null) {
				throw new InternalError("Cannot go up as the SDT under construction does not have a parent");
			}
			return parent.up(level-1);
		}
		return this;
	}
	
	/**
	 * Returns the ancestor builder at the given depth in the tree (0 is root, ..)  
	 */
	public SDTBuilder down(int depth) {
		if (depth < 0) {
			throw new InternalError("Depth cannot be negative");
		} else if (this.depth == depth) {
			return this;
		} else {
			if (this.depth < depth) {
				throw new InternalError("Solicited depth cannot be greater than builder depth");
			} else {
				return parent.down(depth);
			}
		}
	}
	
	/**
	 * Returns the root builder
	 */
	public SDTBuilder root() {
		if (parent == null)
			return this;
		else
			return parent.root();
	}
	
	
	/**
	 * Builds the SDT corresponding to this builder.
	 */
	public SDT build() {
		if (leaf != null) {
			return leaf;
		} else {
			Map<SDTGuard, SDT> sdtChildren = new LinkedHashMap<>();
			for (SDTGuard guard : children.keySet()) {
				sdtChildren.put(guard, children.get(guard).build());
			}
			return new SDT(sdtChildren);
		} 
		
	}
	
	private SymbolicDataValue sdv(String str) {
		if (!str.matches(SDV_EXP)) {
			throw new RuntimeException("Symbolic data value has invalid format. Expected: " + SDV_EXP);
		}
		char type = str.charAt(0);
		int index = Integer.valueOf(str.substring(1));
		switch(type) {
		case 'r':
			return new Register(dataType, index);
		case 's':
			return new SuffixValue(dataType, index);
		case 'c':
			return new Constant(dataType, index);
		}
		
		throw new InternalError("Error in generating an SDV for string " + str);
	}
	
}
