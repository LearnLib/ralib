package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.config.ConfigurationOption;

public class Debugger {
    protected static final ConfigurationOption.StringOption OPTION_DEBUG_TRACES
    = new ConfigurationOption.StringOption("debug.traces",
            "Debug traces are run on the system at start with printing of the output, followed by exit. No learning is done."
            + "Debug traces format: test1; test2; ...", null, true);
    
    protected static final ConfigurationOption.StringOption OPTION_DEBUG_SUFFIXES
    = new ConfigurationOption.StringOption("debug.suffixes",
            "For the debug traces given, run the given suffixes exhaustively and exit. No learning is done."
            + "Debug suffixes format: suff1; suff2; ...", null, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_DEBUG_EXCLUDE
    = new ConfigurationOption.BooleanOption("debug.update",
            "Excludes the specified traces from the cache (. ", false, true);
    
}
