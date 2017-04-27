package de.learnlib.ralib.tools.sulanalyzer;

class DefaultSULInstantiator implements ConcreteSULInstantiator{
	private Class<? extends ConcreteSUL> cls;

	public DefaultSULInstantiator(Class<? extends ConcreteSUL> cls) {
		this.cls = cls;
	}

	public ConcreteSUL newSUL() {
		ConcreteSUL newSUL = null;
   		try {
   	          newSUL = cls.newInstance();
   	    } catch (InstantiationException | IllegalAccessException ex) {
   	         throw new RuntimeException(ex);
   	    }
		return newSUL;
	}
}
