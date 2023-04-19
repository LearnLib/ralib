/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.config.Configuration;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author falk
 */
public class ConsoleClient {

    private static final Map<String, Class<? extends RaLibTool>> tools = new LinkedHashMap<>();

    static {
        tools.put("iosimulator", IOSimulator.class);
        tools.put("class-analyzer", ClassAnalyzer.class);
    }

    private final String[] args;

    private String toolname = null;

    private Configuration config = null;

    private RaLibTool tool = null;

    public ConsoleClient(String[] args) {
        this.args = args;
    }

    public int run() {
        try {
            parseTool();
            if (tool == null) {
                System.err.println("Could not find tool.");
                usage();
                return 1;
            }
  
            parseConfig();
            if (config == null) {
                System.err.println("Could not parse configuration.");
                usage();
                return 1;
            }
        
            tool.setup(config);

        } catch (Throwable ex) {
            System.err.println("Execution terminated abnormally: " + ex.getMessage());
            ex.printStackTrace();
            usage();
            return 1;
        }

        try {
            System.err.println("Running " + args[0]);
            tool.run();
        } catch (Throwable ex) {
            System.err.println("Execution terminated abnormally: " + ex.getMessage());
            ex.printStackTrace(System.err);
            return 1;
        }
        
        System.err.println("Random seed: " + config.getProperty("__seed"));
        return 0;
    }

    private void usage() {

        String usage = "\nUsage: ralib <tool> [-f <file>] [<arg>], where \n"
                + "    - <tool> is the name of the tool to run\n"
                + "    - if -f is provided, <file> has to be a file name of the configuration\n"
                + "    - <arg> has to contain configuration options, separated by ;.\n\n"
                + "    Implemented Tools:\n"
                + "    " + Arrays.toString(tools.keySet().toArray());

        System.err.println(usage);

        if (tool != null && toolname != null) {
            System.err.println();
            System.err.println("Info on " + toolname);
            System.err.println();
            System.err.println(tool.description());
            System.err.println();
            System.err.println("Options");
            System.err.println();
            System.err.println(tool.help());
        }
    }

    private void parseTool() throws InstantiationException, IllegalAccessException {
        if (args.length < 1) {
            return;
        }

        toolname = args[0];
        Class<? extends RaLibTool> toolClass = tools.get(toolname);
        this.tool = toolClass.newInstance();
    }

    private void parseConfig() throws IOException {
        if (args.length >= 4 && args[1].equals("-f")) {
            this.config = new Configuration(new File(args[2]));
            for (int i=3; i<args.length; i++) {
                Configuration temp = new Configuration(args[i].replaceAll(";", System.lineSeparator()));
                for (String s : temp.stringPropertyNames()) {
                    this.config.put(s, temp.get(s));
                }
            }
        }
        else if (args.length == 3 && args[1].equals("-f")) {
            this.config = new Configuration(new File(args[2]));
        } else if (args.length == 2) {
            this.config = new Configuration(args[1].replaceAll(";", System.lineSeparator()));
        }
    }

}
