package de.learnlib.ralib.automata.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.testng.Assert;
import org.testng.annotations.Test;

import de.learnlib.ralib.RaLibTestSuite;
import de.learnlib.ralib.TestUtil;
import de.learnlib.ralib.automata.RegisterAutomaton;
import de.learnlib.ralib.data.Constants;
import de.learnlib.ralib.example.login.LoginAutomatonExample;

/**
 * Tests that {@link RegisterAutomatonExporter} and {@link RegisterAutomatonImporter} understand each other.
 */

public class RegisterAutomatonExportImportTest extends RaLibTestSuite {

	@Test
	public void testExportImportLogin() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		RegisterAutomatonExporter.write(LoginAutomatonExample.AUTOMATON, new Constants(), bos);
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		RegisterAutomatonImporter importer = new RegisterAutomatonImporter(bis);
		assertMatches(importer, LoginAutomatonExample.AUTOMATON, new Constants());
	}
	
	@Test
	public void testExportImportDtls() {
		RegisterAutomatonImporter loader = TestUtil.getLoader("/de/learnlib/ralib/automata/xml/dtls/scandium-server.xml");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		RegisterAutomatonExporter.write(loader.getRegisterAutomaton(), loader.getConstants(), bos);
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		RegisterAutomatonImporter importer = new RegisterAutomatonImporter(bis);
		assertMatches(importer, loader.getRegisterAutomaton(), loader.getConstants());
	}
	
	private void assertMatches(RegisterAutomatonImporter result, RegisterAutomaton expectedRa, Constants expectedConsts) {
		RegisterAutomaton actualRa = result.getRegisterAutomaton();
		Constants consts = result.getConstants();
		Assert.assertEquals(actualRa.getTransitions().size(), expectedRa.getTransitions().size(), "Wrong number of transitions");
		Assert.assertEquals(actualRa.getInputStates().size(), expectedRa.getInputStates().size(), "Wrong number of input locations");
		Assert.assertEquals(consts.entrySet(), expectedConsts.entrySet());
		
	}
}
