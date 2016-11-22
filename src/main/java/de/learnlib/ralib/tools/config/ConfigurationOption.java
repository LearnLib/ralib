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

    @Override
    public String toString() {
        return "" + this.key +  (optional ? "(optional)" : "") + ": " + this.description +
                (optional ? ", default: " + this.defaultValue : "");
    }
    
    public abstract T parse(Configuration c) throws ConfigurationException;

    public static final class BooleanOption extends ConfigurationOption<Boolean> {

        public BooleanOption(String key, String description, Boolean defaultValue, boolean optional) {
            super(key, description, defaultValue, optional);
        }

        @Override
        public Boolean parse(Configuration c) throws ConfigurationException {
            if (!c.containsKey(getKey())) {
                if (!this.isOptional()) {
                    throw new ConfigurationException("Missing config value for " + this.getKey());
                }
                return this.getDefaultValue();
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
                if (!this.isOptional()) {
                    throw new ConfigurationException("Missing config value for " + this.getKey());
                }
                return this.getDefaultValue();
            }

            String value = c.getProperty(getKey());
            Integer i = null;
            try {
                i = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Could not parse " + getKey()
                        + ": " + ex.getMessage());
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
                if (!this.isOptional()) {
                    throw new ConfigurationException("Missing config value for " + this.getKey());
                }
                return this.getDefaultValue();
            }

            String value = c.getProperty(getKey());
            Double d = null;
            try {
                d = Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Could not parse " + getKey()
                        + ": " + ex.getMessage());
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
                if (!this.isOptional()) {
                    throw new ConfigurationException("Missing config value for " + this.getKey());
                }
                return this.getDefaultValue();
            }

            String value = c.getProperty(getKey());
            Long l = null;
            try {
                l = Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Could not parse " + getKey()
                        + ": " + ex.getMessage());
            }

            return l;
        }
    }

    public static class StringOption extends ConfigurationOption<String> {

        public StringOption(String key, String description, String defaultValue, boolean optional) {
            super(key, description, defaultValue, optional);
        }

        @Override
        public String parse(Configuration c) throws ConfigurationException {
            if (!c.containsKey(getKey())) {
                if (!this.isOptional()) {
                    throw new ConfigurationException("Missing config value for " + this.getKey());
                }
                return this.getDefaultValue();
            }
            return c.getProperty(getKey());
        }
    }
    
//    public static class ArrayOption<T extends int[] > extends ConfigurationOption<T> {
//
//    	private Class<T> cls;
//
//
//		public ArrayOption(String key, String description, T defaultValue, boolean optional, Class<T> cls) {
//			super(key, description, defaultValue, optional);
//			this.cls = cls;
//		}
//
//    	
//		public   T [] parse(Configuration c) throws ConfigurationException {
//            if (!c.containsKey(getKey())) {
//                if (!this.isOptional()) {
//                    throw new ConfigurationException("Missing config value for " + this.getKey());
//                }
//                return null;
//            }
//            return c.getProperty(getKey());
//		}
//    	
//    }

}
