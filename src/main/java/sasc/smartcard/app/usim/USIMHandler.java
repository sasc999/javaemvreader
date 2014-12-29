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
package sasc.smartcard.app.usim;

import sasc.emv.EMVUtil;
import sasc.iso7816.AID;
import sasc.iso7816.Iso7816Commands;
import sasc.smartcard.app.globalplatform.ISDApplication;
import sasc.smartcard.app.globalplatform.SecurityDomainFCI;
import sasc.smartcard.common.ApplicationHandler;
import sasc.smartcard.common.SmartCard;
import sasc.terminal.CardConnection;
import sasc.terminal.CardResponse;
import sasc.terminal.TerminalException;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * @author sasc
 */
public class USIMHandler implements ApplicationHandler {

    @Override
    public boolean process(AID aid, SmartCard card, CardConnection terminal) throws TerminalException {
        int SW1;
        int SW2;
        byte[] command;
        byte[] data;
        CardResponse response;
        USIM app = new USIM(aid, card);

//        if (card != null) {
//            card.addApplication(app);
//        }

        Log.commandHeader("Select USIM Application");
        command = Iso7816Commands.selectByDFName(aid.getAIDBytes(), true, (byte) 0x00);
        response = EMVUtil.sendCmd(terminal, command);

        SW1 = (byte) response.getSW1();
        SW2 = (byte) response.getSW2();
        data = response.getData();

        if (SW1 == (byte) 0x90 && SW2 == (byte) 0x00) {
            try {

//                Log.info(securityDomainFCI.toString());
            } catch (RuntimeException ex) {
                Log.debug(Util.getStackTrace(ex));
            }
        }
        
        return false;
    }
    
}
