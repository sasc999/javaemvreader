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
package sasc.iso7816;

import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Short File Identifier
 * 
 * @author sasc
 */
public class ShortFileIdentifier {

    private int sfi;

    public ShortFileIdentifier(int sfi) {
        this.sfi = sfi;
    }

    public int getValue() {
        return sfi;
    }

    public static String getDescription(int sfi) {
        if (sfi < 1 || sfi > 30) {
            throw new IllegalArgumentException("Illegal SFI value. SFI must be in the range 1-30. sfi=" + sfi);
        }
        if (sfi <= 10) { //1-10
            return "Governed by the EMV specification";
        } else if (sfi <= 20) { //11-20
            return "Payment system-specific";
        } else { //21-30
            return "Issuer-specific";
        }
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Short File Identifier:");
        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        pw.println(indentStr + sfi + " (" + ShortFileIdentifier.getDescription(sfi) + ")");
    }
}
