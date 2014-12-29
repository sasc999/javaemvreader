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
package sasc.emv;

import sasc.iso7816.SmartCardException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class LanguagePreference {

    private List<Locale> prefs;

    public LanguagePreference(byte[] data) {
        if (data.length < 2 || data.length > 8 || data.length % 2 != 0) {
            throw new SmartCardException("Array length must be an even number between 2 (inclusive) and 8 (inclusive). Length=" + data.length);
        }
        prefs = new ArrayList<Locale>();

        int numLang = data.length / 2;

        for (int i = 0; i < numLang; i++) {
            String s = String.valueOf((char) data[i * 2]) + String.valueOf((char) data[i * 2 + 1]);
            prefs.add(new Locale(s));
        }

    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }
    
    public List<Locale> getLocales(){
        return prefs;
    }
    
    public Locale getPreferredLocale(){
        return prefs.get(0);
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Language Preference (in order of preference):");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        for (Locale lang : prefs) {
            String postfix = "";
            String displayLanguage = lang.getDisplayLanguage(Locale.ENGLISH);
            if (!"".equals(displayLanguage)) {
                postfix = " (" + displayLanguage + ")";
            }
            pw.println(indentStr + "Language: " + lang + postfix);
        }
    }
}
