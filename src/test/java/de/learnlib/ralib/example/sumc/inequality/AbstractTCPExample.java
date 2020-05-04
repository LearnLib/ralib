package de.learnlib.ralib.example.sumcineq;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstractTCPExample extends AbstractWindowProtocol implements TCPExample{
	protected State state;
	protected Set<Option> options;
	
	public AbstractTCPExample(Double win) {
		super(win);
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
		state = State.CLOSED;
	}
	
	public AbstractTCPExample() {
		super();
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
		state = State.CLOSED;
	}
	
	public void configure (Option ... options) {
		this.options = Arrays.asList(options).stream().collect(Collectors.toSet());
	}
}
