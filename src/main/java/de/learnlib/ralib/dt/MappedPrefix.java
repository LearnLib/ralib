package de.learnlib.ralib.dt;

import de.learnlib.ralib.data.PIV;
import de.learnlib.ralib.words.PSymbolInstance;
import net.automatalib.words.Word;

public class MappedPrefix {
	private Word<PSymbolInstance> prefix;
	private PIV registers;
	
	public MappedPrefix(Word<PSymbolInstance> prefix) {
		this.prefix = prefix;
		registers = new PIV();
	}
	
	public MappedPrefix(Word<PSymbolInstance> prefix, PIV registers) {
		this.prefix = prefix;
		this.registers = registers;
	}
	
	public Word<PSymbolInstance> getPrefix() {
		return this.prefix;
	}
	
	public PIV getRegisters() {
		return this.registers;
	}
	
	public String toString() {
		return "{" + prefix.toString() + ", " + registers.toString() + "}";
	}
}
