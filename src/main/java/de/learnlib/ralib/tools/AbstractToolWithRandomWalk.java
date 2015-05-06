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

import de.learnlib.logging.Category;
import de.learnlib.logging.filter.CategoryFilter;
import de.learnlib.ralib.tools.config.Configuration;
import de.learnlib.ralib.tools.config.ConfigurationException;
import de.learnlib.ralib.tools.config.ConfigurationOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author falk
 */
public abstract class AbstractToolWithRandomWalk implements RaLibTool {

    protected static final ConfigurationOption.LongOption OPTION_RANDOM_SEED = 
            new ConfigurationOption.LongOption("random.seed", "Seed for RNG", 0L, true);
        
    protected static final ConfigurationOption<Level> OPTION_LOGGING_LEVEL =
            new ConfigurationOption<Level>("logging.level", "Log Level", Level.INFO, true) {

            @Override
            public Level parse(Configuration c) throws ConfigurationException {
                if (!c.containsKey(this.getKey())) {
                    if (!this.isOptional()) {
                        throw new ConfigurationException("Missing config value for " + this.getKey());
                    }
                    return this.getDefaultValue();
                }
                Level lvl = Level.parse(c.getProperty(this.getKey()));
                return lvl;
            }
        };
    
    protected static final ConfigurationOption<EnumSet<Category>> OPTION_LOGGING_CATEGORY =
            new ConfigurationOption<EnumSet<Category>>("logging.category", "Log category", 
                    EnumSet.allOf(Category.class), true) {

            @Override
            public EnumSet<Category> parse(Configuration c) throws ConfigurationException {
                if (!c.containsKey(this.getKey())) {
                    if (!this.isOptional()) {
                        throw new ConfigurationException("Missing config value for " + this.getKey());
                    }
                    return this.getDefaultValue();
                }
                String[] names = c.getProperty(this.getKey()).split(",");
                List<Category> list = new ArrayList<>();
                for (String n : names) {
                    list.add(parseCategory(n));
                }
                EnumSet<Category> ret = EnumSet.copyOf(list);
                return ret;
            }

            private Category parseCategory(String n) throws ConfigurationException {
                n = n.toUpperCase();
                switch (n) {
                    case "CONFIG": return Category.CONFIG;
                    case "COUNTEREXAMPLE": return Category.COUNTEREXAMPLE;
                    case "DATASTRUCTURE": return Category.DATASTRUCTURE;
                    case "EVENT": return Category.EVENT;
                    case "MODEL": return Category.MODEL;
                    case "PHASE": return Category.PHASE;
                    case "PROFILING": return Category.PROFILING;
                    case "QUERY": return Category.QUERY;
                    case "STATISTIC": return Category.STATISTIC;
                    case "SYSTEM": return Category.SYSTEM;
                }
                throw new ConfigurationException("can not parse " + this.getKey() + ": " + n);
            }
        };

    protected static final ConfigurationOption.BooleanOption OPTION_USE_RWALK =
            new ConfigurationOption.BooleanOption("use.rwalk", 
                    "Use random walk for finding counterexamples. "
                    + "This will override any ces produced by the eq test", Boolean.FALSE, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_USE_CEOPT =
            new ConfigurationOption.BooleanOption("use.ceopt", 
                    "Use counterexample optimizers", Boolean.FALSE, true);

    protected static final ConfigurationOption.BooleanOption OPTION_USE_FRESH_VALUES =
            new ConfigurationOption.BooleanOption("use.fresh", 
                    "Allow fresh values in output", Boolean.FALSE, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_EXPORT_MODEL =
            new ConfigurationOption.BooleanOption("export.model", 
                    "Export final model to model.xml", Boolean.FALSE, true);
    
    protected static final ConfigurationOption.BooleanOption OPTION_USE_SUFFIXOPT =
            new ConfigurationOption.BooleanOption("use.suffixopt", 
                    "Do only use fresh values for non-free suffix values", Boolean.FALSE, true);

    protected static final ConfigurationOption.LongOption OPTION_TIMEOUT =
            new ConfigurationOption.LongOption("max.time.millis", 
                    "Maximal run time for experiment in milliseconds", -1L, true);
    
    protected static final ConfigurationOption.IntegerOption OPTION_MAX_ROUNDS =
            new ConfigurationOption.IntegerOption("max.rounds", 
                    "Maximum number of rounds", -1, true);

    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_DRAW =
            new ConfigurationOption.BooleanOption("rwalk.draw.uniform", 
                    "Draw next input uniformly", null, false);
    
    protected static final ConfigurationOption.BooleanOption OPTION_RWALK_RESET =
            new ConfigurationOption.BooleanOption("rwalk.reset.count", 
                    "Reset limit counters after each counterexample", null, false);

    protected static final ConfigurationOption.DoubleOption OPTION_RWALK_RESET_PROB = 
            new ConfigurationOption.DoubleOption("rwalk.prob.reset", 
                    "Probability of performing reset instead of step", null, false);
    
    protected static final ConfigurationOption.DoubleOption OPTION_RWALK_FRESH_PROB = 
            new ConfigurationOption.DoubleOption("rwalk.prob.fresh", 
                    "Probability of using a fresh data value", null, false);

    protected static final ConfigurationOption.LongOption OPTION_RWALK_MAX_RUNS = 
            new ConfigurationOption.LongOption("rwalk.max.runs", 
                    "Maximum number of random walks", null, false);
    
    protected static final ConfigurationOption.IntegerOption OPTION_RWALK_MAX_DEPTH =
            new ConfigurationOption.IntegerOption("rwalk.max.depth", 
                    "Maximum length of each random walk", null, false);
    

    protected Random random = null;
        
    protected boolean useCeOptimizers;
        
    protected boolean findCounterexamples;

    protected boolean useSuffixOpt;
    
    protected int maxRounds = -1; 
    
    protected long timeoutMillis = -1L;
    
    protected boolean exportModel = false;
    
    protected boolean useFresh = false;
    
    @Override
    public void setup(Configuration config) throws ConfigurationException {
        
        config.list(System.out);
        
        // logging
        Logger root = Logger.getLogger("");
        Level lvl = OPTION_LOGGING_LEVEL.parse(config);
        root.setLevel(lvl);
        EnumSet<Category> cat = OPTION_LOGGING_CATEGORY.parse(config);
        for (Handler h : root.getHandlers()) {
            h.setLevel(lvl);
            h.setFilter(new CategoryFilter(cat));
        }
        

        // random
        Long seed = (new Random()).nextLong();
        if (config.containsKey(OPTION_RANDOM_SEED.getKey())) {
            seed = OPTION_RANDOM_SEED.parse(config);
        }
        System.out.println("RANDOM SEED=" + seed);
        this.random = new Random(seed);
        config.setProperty("__seed", "" + seed);
        
        this.useCeOptimizers = OPTION_USE_CEOPT.parse(config);
        this.findCounterexamples = OPTION_USE_RWALK.parse(config);      
        this.maxRounds = OPTION_MAX_ROUNDS.parse(config);        
        this.useSuffixOpt = OPTION_USE_SUFFIXOPT.parse(config);
        this.timeoutMillis = OPTION_TIMEOUT.parse(config);
        this.exportModel = OPTION_EXPORT_MODEL.parse(config);
        this.useFresh = OPTION_USE_FRESH_VALUES.parse(config);
    }    
}
