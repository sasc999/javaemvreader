/*
 * Copyright 2010 sasc
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
package sasc.util;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements a simple logging facility. This class should be replace by slf4j/log4j or simimlar
 * @author sasc
 */
public class Log {
    
    public static final String COMMAND_HEADER_FRAMING = "------------------------------------------------";//---------------------";

    public static final int INDENT_SIZE = 2;
    
    private static AtomicInteger stepNo = new AtomicInteger(1);
    private static PrintWriter printWriter = null;
    private static Level level = Level.INFO;

    public enum Level {

        ALL(0), TRACE(1), DEBUG(2), PROCEDUREBYTE(3), INFO(5), COMMAND(6), ERROR(8), OFF(10);
        private int value;

        private Level(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static void setLevel(Level level) {
        Log.level = level;
    }

//    public static void setLevels(Level... levels){
//        EnumSet<Level> set = EnumSet.copyOf(Arrays.asList(levels));
//    }
    public static Level getLevel() {
        return level;
    }
    
    public static void resetStepNo(){
        stepNo.set(1);
    }

    public static void debug(String msg) {
        logInternal("DEBUG: " + msg, Level.DEBUG);
    }

    public static void info(String msg) {
        logInternal(msg, Level.INFO);
    }

    public static void procedureByte(String msg) {
        logInternal(msg, Level.PROCEDUREBYTE);
    }

    public static void command(String msg) {
        logInternal(msg, Level.COMMAND);
    }

    public static void commandHeader(String msg) {
        logInternal("\n"+COMMAND_HEADER_FRAMING
                + "\n[Step " + stepNo.getAndIncrement() + "] " + msg
                + "\n"+COMMAND_HEADER_FRAMING, Level.COMMAND);
    }

    private static void logInternal(String msg, Level level) {
        if (level.getValue() >= Log.level.getValue()) {
            if (printWriter != null) {
                printWriter.println(msg);
                printWriter.flush();
            } else {
                System.out.println(msg);
            }
        }
    }

    public static void setPrintWriter(PrintWriter printWriter) {
        if (printWriter == null) {
            throw new IllegalArgumentException("Parameter 'printWriter' cannot be null");
        }
        Log.printWriter = printWriter;
    }

    public static PrintWriter getPrintWriter() {
        if (printWriter == null) {
            return new PrintWriter(System.out);
        }
        return printWriter;
    }
}
