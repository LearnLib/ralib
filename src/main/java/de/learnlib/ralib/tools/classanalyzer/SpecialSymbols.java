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
package de.learnlib.ralib.tools.classanalyzer;

import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.words.OutputSymbol;

/**
 *
 * @author falk
 */
public class SpecialSymbols {

    static final class ErrorSymbol extends OutputSymbol {

        private final Throwable error;

        ErrorSymbol(Throwable error) {
            super("__ERR");
            this.error = error;
        }


        @Override
        public String toString() {
            return "E_" + this.error.getClass().getSimpleName();
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            return getClass() == obj.getClass();
        }
    };

    public static final ErrorSymbol ERROR = new ErrorSymbol(new Exception("__dummy"));

    public static final OutputSymbol NULL = new OutputSymbol("NULL");

    public static final OutputSymbol VOID = new OutputSymbol("V");

    public static final OutputSymbol DEPTH = new OutputSymbol("MAXD");

    public static final OutputSymbol TRUE = new OutputSymbol("TRUE");

    public static final OutputSymbol FALSE = new OutputSymbol("FALSE");

    public static final DataType BOOLEAN_TYPE = new DataType("boolean", boolean.class);
}
