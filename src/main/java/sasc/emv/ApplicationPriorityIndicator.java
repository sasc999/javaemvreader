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

import java.io.PrintWriter;
import java.io.StringWriter;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Application Priority Indicator
 * Indicates the priority of a given application or group of applications in a directory
 *
 * @author sasc
 */
public class ApplicationPriorityIndicator {
    private byte apiByte;

    public ApplicationPriorityIndicator(byte apiByte){
        this.apiByte = apiByte;
    }

    public boolean mayBeselectedWithoutCardholderConfirmation(){
        return (apiByte & 0xFF & 0x80) == 0;
    }

    public int getSelectionPriority(){
        return (apiByte & 0x0F);
    }

    public boolean isPriorityAssigned(){
        return (apiByte & 0x0F) > 0;
    }

    public String getMayBeselectedWithoutCardholderConfirmationString(){
        if(mayBeselectedWithoutCardholderConfirmation()){
            return "Application may be selected without confirmation of cardholder";
        }else{
            return "Application cannot be selected without confirmation of cardholder";
        }
    }

    @Override
    public String toString(){
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    public void dump(PrintWriter pw, int indent){
        pw.println(Util.getSpaces(indent)+"Application Priority Indicator");
        String indentStr = Util.getSpaces(indent+Log.INDENT_SIZE);
        pw.println(indentStr+getMayBeselectedWithoutCardholderConfirmationString());
        if(isPriorityAssigned()){
            pw.println(indentStr+"Selection Priority: "+getSelectionPriority() + " (1 is highest)");
        }else{
            pw.println(indentStr+"No Priority Assigned");
        }
    }

    public static void main(String[] args){
        ApplicationPriorityIndicator api = new ApplicationPriorityIndicator((byte)0x83);
        System.out.println(api.toString());
        System.out.println("");
        System.out.println("mayBeselectedWithoutCardholderConfirmation(): "+api.mayBeselectedWithoutCardholderConfirmation());
        System.out.println("selectionPriority: "+api.getSelectionPriority());
    }
}
