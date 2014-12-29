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

import sasc.iso7816.ShortFileIdentifier;
import sasc.iso7816.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Directory Definition File
 *
 * @author sasc
 */
public class DDF implements File{ // implements DF {

    private byte[] name;
    private ShortFileIdentifier sfi;
    private LanguagePreference languagePreference = null;
    private int issuerCodeTableIndex = -1;

    public DDF() {
    }

    public void setSFI(ShortFileIdentifier sfi) {
        this.sfi = sfi;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    public byte[] getName() {
        return name;
    }

    public ShortFileIdentifier getSFI() {
        return sfi;
    }

    public void setLanguagePreference(LanguagePreference languagePreference) {
        this.languagePreference = languagePreference;
    }

    public LanguagePreference getLanguagePreference() {
        return languagePreference;
    }

    public void setIssuerCodeTableIndex(int index){
        issuerCodeTableIndex = index;
    }

    public int getIssuerCodeTableIndex() {
        return issuerCodeTableIndex;
    }

    public String getIssuerCodeTable(){
        return "ISO-8859-"+issuerCodeTableIndex;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Directory Definition File");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);

        pw.println(indentStr + "Name: " + Util.byteArrayToHexString(name) + " (=" + Util.getSafePrintChars(name) + ")");
        if(issuerCodeTableIndex != -1){
            pw.println(indentStr + "Issuer Code Table Index: " + issuerCodeTableIndex + " (ISO-8859-"+issuerCodeTableIndex+")");
        }

        if (sfi != null) {
            sfi.dump(pw, indent + Log.INDENT_SIZE);
        }

        if (languagePreference != null) {
            languagePreference.dump(pw, indent + Log.INDENT_SIZE);
        }

    }
}
