/*
 * Copyright (C) 2015 falk.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package de.learnlib.ralib.tools;

import de.learnlib.ralib.tools.config.Configuration;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author falk
 */
public class ConsoleClient {
    
    private static final Map<String, Class<? extends RaLibTool>> tools = new HashMap<>();
    
    static {
        tools.put("iosimulator", IOSimulator.class);
    }
    
    private final String[] args;
    
    private Configuration config = null;
    
    private RaLibTool tool = null;
    
    public ConsoleClient(String[] args) {
        this.args = args;
    }
    
    public void run() {
        try {
            parseTool();
            if (tool == null) {
                System.err.println("Could not find tool.");
                usage();
                return;                                
            }
            
            parseConfig();
            if (config == null) {
                System.err.println("Could not parse configuration.");
                usage();
                return;                
            }
        } catch (Throwable ex) {
            usage();    
            return;
        }
            
        try {
            System.err.println("Running " + args[0]);
            tool.setup(config);
            tool.run();            
        } catch (Throwable ex) {
            System.err.println("Execution terminated abnormally: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
    
    private void usage() {
        
        String usage = "\nUsage: ralib <tool> [-f] <arg> where \n" +
                "    - <tool> is the name of the tool to run\n" + 
                "    - if -f is provided, <arg> has to be a file name of the configuration\n" +
                "    - otherwise <arg> has to contain the configuration options.\n\n" +
                "    Implemented Tools:\n" +
                "    " + Arrays.toString(tools.keySet().toArray());
        
        System.err.println(usage);
    }
    
    private void parseTool() throws InstantiationException, IllegalAccessException {
        if (args.length < 1) {
            return;
        }
        
        String toolname = args[0];
        Class<? extends RaLibTool> toolClass = tools.get(toolname);
        this.tool = toolClass.newInstance();
    }
    
    private void parseConfig() throws IOException {
        if (args.length == 3 && args[1].equals("-f")) {
            this.config =  new Configuration(new File(args[2]));
        }
        else if (args.length == 2) {
            this.config = new Configuration(args[1].replaceAll(";", "\n"));
        }
    }
    

    public static void main(String[] args) {
        
        ConsoleClient cl = new ConsoleClient(new String[] {
            "iosimulator",
            "x=y;z=p;random.seed=36842364238534534"
        });
        
        cl.run();
    }


}




