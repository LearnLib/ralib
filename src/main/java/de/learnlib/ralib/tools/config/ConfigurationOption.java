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

package de.learnlib.ralib.tools.config;

/**
 *
 * @author falk
 * @param <T>
 */
public abstract class ConfigurationOption<T> {
    
    private final String key;
    
    private final String description;
    
    private final T defaultValue;
    
    private final boolean optional;

    public ConfigurationOption(String key, String description, T defaultValue, boolean optional) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.optional = optional;
    }


    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    public T getDefaultValue() {
        return defaultValue;
    }
    
    public boolean isOptional() {
        return optional;
    }

    
    public abstract T parse(Configuration c) throws ConfigurationException;
        
    public static final class BooleanOption extends ConfigurationOption<Boolean> {

        public BooleanOption(String key, String description, Boolean defaultValue, boolean optional) {
            super(key, description, defaultValue, optional);
        }

        @Override
        public Boolean parse(Configuration c) throws ConfigurationException {
            if (!c.containsKey(getKey())) {
                return null;
            }
            
            String value = c.getProperty(getKey());            
            return !(value.toLowerCase().equals("false"));
        }        
    }
    
    public static final class IntegerOption extends ConfigurationOption<Integer> {

        public IntegerOption(String key, String description, Integer defaultValue, boolean optional) {
            super(key, description, defaultValue, optional);
        }

        @Override
        public Integer parse(Configuration c) throws ConfigurationException {
            if (!c.containsKey(getKey())) {
                return null;
            }
            
            String value = c.getProperty(getKey());            
            Integer i = null;
            try {
                i = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Could not parse " + getKey() + 
                        ": " + ex.getMessage());
            }
            
            return i;  
        }  
    }
    
    public static final class DoubleOption extends ConfigurationOption<Double> {

        public DoubleOption(String key, String description, Double defaultValue, boolean optional) {
            super(key, description, defaultValue, optional);
        }

        @Override
        public Double parse(Configuration c) throws ConfigurationException {
            if (!c.containsKey(getKey())) {
                return null;
            }
            
            String value = c.getProperty(getKey());            
            Double d = null;
            try {
                d = Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Could not parse " + getKey() + 
                        ": " + ex.getMessage());
            }
            
            return d;  
        }  
    }    

    public static final class LongOption extends ConfigurationOption<Long> {

        public LongOption(String key, String description, Long defaultValue, boolean optional) {
            super(key, description, defaultValue, optional);
        }

        @Override
        public Long parse(Configuration c) throws ConfigurationException {
            if (!c.containsKey(getKey())) {
                return null;
            }
            
            String value = c.getProperty(getKey());            
            Long l = null;
            try {
                l = Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Could not parse " + getKey() + 
                        ": " + ex.getMessage());
            }
            
            return l;  
        }  
    }
    
}
