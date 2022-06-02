package de.learnlib.ralib.dt;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class ShortPrefix {
	private Word<PSymbolInstance> prefix;
	private PIV registers;
	
	public ShortPrefix(Word<PSymbolInstance> prefix, PIV registers) {
		this.prefix = prefix;
		this.registers = registers;
	}
	
	public Word<PSymbolInstance> getPrefix() {
		return this.prefix;
	}
	
	public PIV getRegisters() {
		return this.registers;
	}
}
