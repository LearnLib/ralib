package de.learnlib.ralib.example.sumc.inequality;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AbstractIntTCPExample extends AbstractIntWindowProtocol implements TCPExample{
	protected State state;
	protected Set<Option> options;
	
	public AbstractIntTCPExample(Integer win) {
		super(win);
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
		state = State.CLOSED;
	}
	
	public AbstractIntTCPExample() {
		super();
		configure(Option.WIN_SYNRECEIVED_TO_CLOSED, Option.WIN_SYNSENT_TO_CLOSED);
		state = State.CLOSED;
	}
	
	public void configure (Option ... options) {
		this.options = Arrays.asList(options).stream().collect(Collectors.toSet());
	}
}
