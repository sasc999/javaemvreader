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
package sasc.smartcard.app.globalplatform;

import java.io.PrintWriter;
import sasc.iso7816.AID;
import sasc.iso7816.Application;
import sasc.smartcard.common.SmartCard;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class ISDApplication implements Application {
    
    SmartCard card;
    AID aid;
    SecurityDomainFCI securityDomainFCI;
    CPLC cplc;
    CardRecognitionData cardRecognitionData;
    KeyInformationTemplate keyInformationTemplate;
    byte[] sequenceCounterOfTheDefaultKeyVersionNumber;
    byte[] confirmationCounter;
    byte[] cardImageNumber;
    byte[] issuerIdentificationNumber;
    byte[] credentsysJ_preIssuanceData;
    
    public ISDApplication(AID aid, SmartCard card) {
        this.aid = aid;
        this.card = card;
    }
    
    public void setFCI(SecurityDomainFCI fci) {
        this.securityDomainFCI = fci;
    }
    
    public void setKeyInformationTemplate(KeyInformationTemplate template) {
        this.keyInformationTemplate = template;
    }
    
    public void setSequenceCounterOfTheDefaultKeyVersionNumber(byte[] counter) {
        this.sequenceCounterOfTheDefaultKeyVersionNumber = counter;
    }
    
    public void setConfirmationCounter(byte[] counter) {
        this.confirmationCounter = counter;
    }
    
    public void setCardRecognitionData(CardRecognitionData data) {
        this.cardRecognitionData = data;
    }
    
    public void setCardImageNumber(byte[] number) {
        this.cardImageNumber = number;
    }
    
    public void setIssuerIdentificationNumber(byte[] iin) {
        this.issuerIdentificationNumber = iin;
    }
    
    public void setCredentsysJ_preIssuanceData(byte[] data) {
        this.credentsysJ_preIssuanceData = data;
    }
    
    public void setCPLC(CPLC cplc) {
        this.cplc = cplc;
    }

    @Override
    public AID getAID() {
        return aid;
    }

    @Override
    public SmartCard getCard() {
        return card;
    }

    @Override
    public void dump(PrintWriter pw, int indent) {
        pw.println(Util.getSpaces(indent) + "Security Domain Application");

        String indentStr = Util.getSpaces(indent + Log.INDENT_SIZE);
        
        if (aid != null) {
            aid.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        if (securityDomainFCI != null) {
            securityDomainFCI.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        if (cplc != null) {
            cplc.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        if (cardRecognitionData != null) {
            cardRecognitionData.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        if (keyInformationTemplate != null) {
            keyInformationTemplate.dump(pw, indent + Log.INDENT_SIZE);
        }
        
        if (sequenceCounterOfTheDefaultKeyVersionNumber != null) {
            pw.println(indentStr+"Sequence Counter Of The Default Key Version Number: " + Util.byteArrayToInt(sequenceCounterOfTheDefaultKeyVersionNumber));
        }
        
        if (confirmationCounter != null) {
            pw.println(indentStr+"Confirmation Counter: " + Util.prettyPrintHexNoWrap(confirmationCounter));
        }
        
        if (cardImageNumber != null) {
            pw.println(indentStr+"Card Image Number: " + Util.prettyPrintHexNoWrap(cardImageNumber));
        }
        
        if (issuerIdentificationNumber != null) {
            pw.println(indentStr+"Issuer Identification Number: " + Util.prettyPrintHexNoWrap(issuerIdentificationNumber));
        }

        if (credentsysJ_preIssuanceData != null) {
            pw.println(indentStr+"Credentsys-J Pre-Issuance Data: " + Util.prettyPrintHexNoWrap(credentsysJ_preIssuanceData));
        }
    }
}
