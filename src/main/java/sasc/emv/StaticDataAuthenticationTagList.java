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

import sasc.iso7816.Tag;
import sasc.iso7816.SmartCardException;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sasc.iso7816.TLVUtil;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Static Data Authentication Tag List
 * List of tags of primitive data objects defined in the EMV specification
 * whose value fields are to be included in the Signed Static or Dynamic Application Data
 * @author sasc
 */
public class StaticDataAuthenticationTagList {
    private List<Tag> tagList = new ArrayList<Tag>();

    public StaticDataAuthenticationTagList(byte[] data){
        //Parse tags and lengths
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        while(stream.available() > 0){
            this.tagList.add(EMVTags.getNotNull(TLVUtil.readTagIdBytes(stream)));
        }
        if(!(tagList.size() == 1 && tagList.get(0).equals(EMVTags.APPLICATION_INTERCHANGE_PROFILE))){
            //If the Static Data Authentication Tag List exists, it shall contain
            //only the tag for the Application Interchange Profile.
            throw new SmartCardException("Only ApplicationInterchangeProfile is allowed in the Static Data Authentication Tag List. List="+tagList);
        }
    }

    public List<Tag> getTagList(){
        return Collections.unmodifiableList(tagList);
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent) + "Static Data Authentication Tag List");
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);

        for(Tag tag : tagList){
            pw.println(indentStr+tag.getName());
        }
    }
}
