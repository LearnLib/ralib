package de.learnlib.ralib.tools;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;

public class SULAnalyzerTest extends RaLibTestSuite {
	@Test
    public void testSULAnalyzerWithFreshMultilogin() {
		 final String[] options = new String[] {
		     "sul-analyzer",     
		     "target=de.learnlib.ralib.example.login.FreshMultiLoginConcreteSUL;" +
		     "inputs=" +
		     "IRegister(java.lang.Integer:uid)+" +
		     "ILogin(java.lang.Integer:uid,java.lang.Integer:pwd);" +
		     "outputs=" +
		     "OK()+" +
		     "NOK()+" +
		     "OUTPUT(java.lang.Integer:pwd);" +
		     "random.seed=6521023071547789;" +
		     "logging.level=WARNING;" +
		     "max.time.millis=600000;" +
		     "use.ceopt=false;" +
		     "use.suffixopt=true;" +
		     "use.fresh=true;" +
		     "use.rwalk=true;" +
		     "export.model=false;" +
		     "rwalk.prob.fresh=0.3;" +
		     "rwalk.prob.reset=0.1;" +
		     "rwalk.max.depth=6;" +
		     "rwalk.max.runs=10000;" +
		     "rwalk.reset.count=false;" +
		     "rwalk.draw.uniform=false;" +
		     "teachers=uid:de.learnlib.ralib.tools.theories.IntegerEqualityTheory+" +
		      "pwd:de.learnlib.ralib.tools.theories.IntegerEqualityTheory;"};
		 
		 try {
		     ConsoleClient cl = new ConsoleClient(options);
		     int ret = cl.run();
		     Assert.assertEquals(ret, 0);
		     
		 } catch (Throwable t) {
		     t.printStackTrace();
		     Assert.fail(t.getClass().getName());
		 }
	}
	
	@Test
    public void testSULAnalyzerWithPriorityQueue() {
		final String[] options = new String[] {
			     "sul-analyzer",     
			     "target=de.learnlib.ralib.example.priority.PriorityQueueConcreteSUL;" +
			     "inputs=" +
			     "offer(java.lang.Object:double)+" +
			     "poll();" +
			     "outputs=" +
			     "OUTPUT(java.lang.Object:double)+" +
			     "OK()+" +
			     "NOK()+" +
			     "NULL();" +
			     "random.seed=6521023071547789;" +
			     "solver=z3;" +
			     "logging.level=WARNING;" +
			     "max.time.millis=600000;" +
			     "use.ceopt=false;" +
			     "use.suffixopt=true;" +
			     "use.fresh=true;" +
			     "use.rwalk=true;" +
			     "export.model=false;" +
			     "rwalk.prob.fresh=0.3;" +
			     "rwalk.prob.reset=0.1;" +
			     "rwalk.max.depth=6;" +
			     "rwalk.max.runs=10000;" +
			     "rwalk.reset.count=false;" +
			     "rwalk.draw.uniform=false;" +
			     "teachers=double:de.learnlib.ralib.tools.theories.DoubleInequalityTheory;"};
			 
			 try {
			     ConsoleClient cl = new ConsoleClient(options);
			     int ret = cl.run();
			     Assert.assertEquals(ret, 0);
			     
			 } catch (Throwable t) {
			     t.printStackTrace();
			     Assert.fail(t.getClass().getName());
			 }
	}
}
