package de.learnlib.ralib.sul.examples;

public class Test extends AbstractWindowProtocol{
	private Double a;

	public Double in(Double a, Double b) {
		if (a == null) {
			this.a = a;
		} else {
			if (equ(b, this.a)) {
				this.a = a;
			} else {
				if (succ(this.a, b) || inWin(this.a, b)) {
					// do nothing
				} 
				else
					this.a = null;
			}
		}
		
		return this.a;
	}
}
