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
package sasc.smartcard.common;

import sasc.iso7816.MasterFile;
import sasc.iso7816.BERTLV;
import sasc.iso7816.AID;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import sasc.iso7816.ATR;
import sasc.emv.DDF;
import sasc.emv.EMVApplication;
import sasc.iso7816.Application;
import sasc.terminal.KnownAIDList;
import sasc.util.Log;
import sasc.util.Util;

/**
 * A representation of all the data discovered on a smart card.
 * A card may adhere partly or fully to ISO7816-4, and/or use proprietary selection methods/communication commands.
 * Thus, a card may:
 * -contain Applications that are selectable using AIDs (selectable according to ISO7816-4)
 * -contain a Master File (according to ISO7816-4)
 * -contain a default selected Application (ISO7816-4 or proprietary)
 * -contain other selectable files, either proprietary or according to ISO7816
 *
 * This class should not contain any transient state, like connection state. Its just a POJO
 *
 * @author sasc
 *
 */
public class SmartCard {

    private Map<AID, Application> otherApplicationsMap = new LinkedHashMap<AID, Application>();
    private Map<AID, EMVApplication> emvApplicationsMap = new LinkedHashMap<AID, EMVApplication>();

    private Set<AID> allAIDs = new LinkedHashSet<AID>();

    private EMVApplication selectedApp = null;
    private DDF pse = null;
    private List<BERTLV> unhandledRecords = new ArrayList<BERTLV>();

    private Set<ATR> atrSet = new LinkedHashSet<ATR>();
    private MasterFile mf = null;
    private Type type = Type.UNKNOWN; //default

    private boolean allKnownAidsProbed = false;

    //Use info from ATR, PSE/PPSE presence etc to guess interface type
    public enum Type{
        CONTACTED, CONTACTLESS, UNKNOWN;
    }

    public SmartCard(ATR atr) {
        if(atr == null){
            throw new IllegalArgumentException("Argument atr cannot be null");
        }
        atrSet.add(atr);
    }

    public void setType(Type type){
        this.type = type;
    }

    public Type getType(){
        return type;
    }

    public boolean allKnownAidsProbed(){
        return allKnownAidsProbed;
    }

    public void setAllKnownAidsProbed(){
        allKnownAidsProbed = true;
    }

    /**
     *
     * @return the final ATR (cold or warm)
     */
    public ATR getATR(){
        ATR last = null;
        for(ATR atr : atrSet){
            last = atr;
        }
        return last;
    }

    /**
     * TODO
     * The first item is the cold ATR
     * Any subsequent ATRs (if present) are warm ATRs
     * @return Set of ATRs (cold and warm (if present))
     */
    public Set<ATR> getATRs(){
        return Collections.unmodifiableSet(atrSet);
    }

    public void setMasterFile(MasterFile mf) {
        this.mf = mf;
    }

    public MasterFile getMasterFile() {
        return this.mf;
    }

    //TODO this vs addAID ?
    public void addEMVApplication(EMVApplication app) {
//        if (applicationsMap.containsKey(app.getAID())) {
//            throw new IllegalArgumentException("EMVApplication already added: " + app.getAID() + " " + app.getPreferredName());
//        }
        if(app.getAID() == null){
            throw new IllegalArgumentException("Invalid EMVApplication object: AID == null");
        }
        Log.debug("ADDING EMV aid: "+Util.prettyPrintHexNoWrap(app.getAID().getAIDBytes()));
        if(app.getCard() == null){
            app.setCard(this);
        }
        emvApplicationsMap.put(app.getAID(), app);
    }

    public void addApplication(Application app) {
        if(app == null) {
            throw new IllegalArgumentException("Param app cannot be null");
        }
        if(app.getAID() == null){
            throw new IllegalArgumentException("Invalid Application object: AID == null");
        }
        if(app instanceof EMVApplication) {
            addEMVApplication((EMVApplication)app);
        } else {
            otherApplicationsMap.put(app.getAID(), app);
        }
    }

    public EMVApplication getSelectedApplication() {
        return selectedApp;
    }

    public void setSelectedApplication(EMVApplication app) {
        this.selectedApp = app;
    }

    public Collection<EMVApplication> getEmvApplications() {
        return Collections.unmodifiableCollection(emvApplicationsMap.values());
    }

    public Collection<Application> getOtherApplications() {
        return Collections.unmodifiableCollection(otherApplicationsMap.values());
    }

    public Collection<Application> getAllApplications() {
        List<Application> apps = new ArrayList<Application>();
        apps.addAll(emvApplicationsMap.values());
        apps.addAll(otherApplicationsMap.values());
        return Collections.unmodifiableCollection(apps);
    }

    public void addAID(AID aid){
        allAIDs.add(aid);

        KnownAIDList.KnownAID knownAID = KnownAIDList.searchAID(aid.getAIDBytes());
        if(knownAID != null){
            String type = knownAID.getType();
            if("EMV".equalsIgnoreCase(type)){
                EMVApplication emvApp = new EMVApplication();
                emvApp.setAID(aid);
                emvApp.setCard(this);
                addEMVApplication(emvApp);
            }else if("GP".equalsIgnoreCase(type)){
                //TODO
            }
        } else {
            //TODO unhandled AIDs list
        }
    }

    public Set<AID> getAllAIDs(){
        return Collections.unmodifiableSet(allAIDs);
    }

    public void setPSE(DDF pse) {
        this.pse = pse;
    }

    public DDF getPSE() {
        return pse;
    }

    public void addUnhandledRecord(BERTLV bertlv) {
        unhandledRecords.add(bertlv);
    }

    public List<BERTLV> getUnhandledRecords() {
        return Collections.unmodifiableList(unhandledRecords);
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        dump(new PrintWriter(sw), 0);
        return sw.toString();
    }

    //Dump all information read from the card
    public void dump(PrintWriter pw, int indent) {

        for(ATR atr : atrSet){
            atr.dump(pw, indent);
        }

        pw.println("");

        pw.println(Util.getSpaces(indent+Log.INDENT_SIZE) + "Interface Type: "+type);

        if (mf != null) {
            mf.dump(pw, indent + Log.INDENT_SIZE);
        }

        pw.println("");

        if (pse != null) {
            pse.dump(pw, indent + Log.INDENT_SIZE);
        }

        if (!unhandledRecords.isEmpty()) {
            pw.println(Util.getSpaces(indent + Log.INDENT_SIZE) + "UNHANDLED GLOBAL RECORDS (" + unhandledRecords.size() + " found):");

            for (BERTLV tlv : unhandledRecords) {
                pw.println(Util.getSpaces(indent + Log.INDENT_SIZE*2) + tlv.getTag() + " " + tlv);
            }
        }
        pw.println("");

        pw.println("");
        pw.println(Util.getSpaces(indent + Log.INDENT_SIZE*2) + "EMV applications (" + getEmvApplications().size() + " found):");
        pw.println("");

        for (EMVApplication app : getEmvApplications()) {
            app.dump(pw, indent + Log.INDENT_SIZE*3);
        }

        if(otherApplicationsMap.size() > 0){
            pw.println("");
            pw.println(Util.getSpaces(indent + Log.INDENT_SIZE*2) + "Other Applications (" + otherApplicationsMap.size() + " found):");
            pw.println("");

            for (Application app : otherApplicationsMap.values()){
                app.dump(pw, indent + Log.INDENT_SIZE*3);
                pw.println("");
            }
        }
        Set<AID> unhandledAIDs = new HashSet<AID>();
		for(AID aid : allAIDs) {
			if(!emvApplicationsMap.containsKey(aid) && !otherApplicationsMap.containsKey(aid)) {
				unhandledAIDs.add(aid);
			}
		}
		if(unhandledAIDs.size() > 0){
			pw.println("");
			pw.println(Util.getSpaces(indent + Log.INDENT_SIZE*2) + "Unhandled AIDs (" + otherApplicationsMap.size() + " found):");
			pw.println("");

			for (AID unhandledAID : unhandledAIDs){
				unhandledAID.dump(pw, indent + Log.INDENT_SIZE*3);
				pw.println("");
			}
        }
        pw.flush();
    }
}
