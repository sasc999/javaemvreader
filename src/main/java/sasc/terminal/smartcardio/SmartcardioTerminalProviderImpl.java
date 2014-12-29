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
package sasc.terminal.smartcardio;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import sasc.smartcard.pcsc.WinErrorCodes;
import sasc.terminal.CardConnection;
import sasc.terminal.NoTerminalsAvailableException;
import sasc.terminal.Terminal;
import sasc.terminal.TerminalException;
import sasc.terminal.TerminalProvider;
import sasc.util.Log;
import sasc.util.Util;

/**
 *
 * WARNING: This class must not be instantiated, referenced or loaded in any way
 * except via reflection.
 *
 * @author sasc
 */
public class SmartcardioTerminalProviderImpl implements TerminalProvider {

    private String providerInfo = "";
    CardTerminals terminals = null;

    SmartcardioTerminalProviderImpl() {
        TerminalFactory factory = TerminalFactory.getDefault();
        providerInfo = "SmartcardIO[" + factory.getProvider() + "]";
        terminals = factory.terminals();
    }
    
    SmartcardioTerminalProviderImpl(CardTerminals cardTerminals, String providerInfo){
        this.providerInfo = providerInfo;
        this.terminals = cardTerminals;
    }
    
    public static TerminalProvider createFromCardTerminals(Object cardTerminals, String providerInfo){
        return new SmartcardioTerminalProviderImpl((CardTerminals)cardTerminals, providerInfo);
    }

    @Override
    public List<Terminal> listTerminals() throws TerminalException {
        List<Terminal> list = new ArrayList<Terminal>();
        try {
            for (CardTerminal terminal : terminals.list()) {
                list.add(new TerminalImpl(terminal));
            }
        } catch (CardException ex) {
            if(!SmartcardioUtils.isNoCardReadersAvailable(ex)){ //No card readers available is not an exception, just return empty list
                throw new TerminalException(ex);
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public CardConnection connectAnyTerminal() throws TerminalException {
        return connectAnyTerminal("*");
    }
    
	/**
	 * For this to work, all other cards in all other attached readers must be removed first
	 *
	 */
    @Override
    public CardConnection connectAnyTerminalWithCardPresent(String protocol) throws TerminalException {
        try {
            while (true) {
                for (CardTerminal smartCardIOTerminal : terminals.list(javax.smartcardio.CardTerminals.State.CARD_PRESENT)) {
                    Log.debug("SmartcardioProvider Terminal: "+smartCardIOTerminal.getName());
                    Card _card = smartCardIOTerminal.connect(protocol); //if proto, eg T=1, is specified and not supported by card: throws PCSCException SCARD_E_PROTO_MISMATCH
                    Log.debug("Connected to card using protocol: "+_card.getProtocol());

                    return new SmartcardioCardConnection(_card, smartCardIOTerminal);
                }
                boolean changeOccurred = terminals.waitForChange(200);
                if(!changeOccurred){ //Timeout
                    if(Thread.currentThread().isInterrupted()) {
                        return null;
                    }
                    continue; //waitForChange again
                }
            }
        } catch (CardException ex) {
            if(SmartcardioUtils.isNoCardReadersAvailable(ex)){ //No card readers available
                throw new NoTerminalsAvailableException(ex);
            }
            throw new TerminalException(SmartcardioUtils.getPCSCErrorDescription(ex), ex);
        } catch (IllegalStateException ex){
            throw new TerminalException(ex);
        }
    }

    @Override
    public CardConnection connectAnyTerminal(String protocol) throws TerminalException {
        try {
            //Do not use State.CARD_PRESENT. Some systems might have a card present at all times in a specific termial 
            //(for example a 3G mobile card with SIM slot that is listed as a PC/SC reader on the host system)
            //wait for a card to be inserted
            while (true) {
                boolean changeOccurred = terminals.waitForChange(200);
                if(!changeOccurred){ //Timeout
                    if(Thread.currentThread().isInterrupted()){
                        return null;
                    }
                    continue; //waitForChange again
                }
                for (CardTerminal smartCardIOTerminal : terminals.list(javax.smartcardio.CardTerminals.State.CARD_INSERTION)) {
                    Card _card = smartCardIOTerminal.connect(protocol); //if proto, eg T=1, is specified and not supported by card: throws PCSCException SCARD_E_PROTO_MISMATCH
                    Log.debug("Connected to card using protocol: "+_card.getProtocol());
                    Log.debug("Terminal: "+smartCardIOTerminal.getName());
                    return new SmartcardioCardConnection(_card, smartCardIOTerminal);
                }
            }
        } catch (CardException ex) {
            if(SmartcardioUtils.isNoCardReadersAvailable(ex)){ //No card readers available 
                throw new NoTerminalsAvailableException(ex);
            }
            throw new TerminalException(SmartcardioUtils.getPCSCErrorDescription(ex), ex);
        } catch (IllegalStateException ex){
            throw new TerminalException(ex);
        }
    }

    @Override
    public CardConnection connectTerminal(String name) throws TerminalException {
        try {
            CardTerminal smartCardIOTerminal = terminals.getTerminal(name);
            Card _card = smartCardIOTerminal.connect("*");
            return new SmartcardioCardConnection(_card, smartCardIOTerminal);
        } catch (CardException ex) {
            throw new TerminalException(ex);
        } 
    }

    @Override
    public CardConnection connectTerminal(int index) throws TerminalException {
        try {
            CardTerminal smartCardIOTerminal = terminals.list().get(index);
            Card _card = smartCardIOTerminal.connect("*");
            return new SmartcardioCardConnection(_card, smartCardIOTerminal);
        } catch (CardException ex) {
            throw new TerminalException(ex);
        } catch (IndexOutOfBoundsException ex){
            throw new TerminalException(ex);
        }
    }

    @Override
    public String getProviderInfo() {
        return providerInfo;
    }

    private class TerminalImpl implements Terminal {

        CardTerminal smartCardIOTerminal;
        Card card = null;

        public TerminalImpl(CardTerminal smartCardIOTerminal) {
            this.smartCardIOTerminal = smartCardIOTerminal;
        }

        @Override
        public CardConnection connect() throws TerminalException {
            try {
                card = smartCardIOTerminal.connect("*");
                return new SmartcardioCardConnection(card, smartCardIOTerminal);
            } catch (CardException ex) {
                throw new TerminalException(ex);
            }
        }
        
        @Override
        public boolean isCardPresent() throws TerminalException {
            try{
                return smartCardIOTerminal.isCardPresent();
            }catch(CardException ex){
                throw new TerminalException(ex);
            }catch (IllegalStateException ex){
                throw new TerminalException(ex);
            }
        }
        
        @Override
        public String getName(){
            return smartCardIOTerminal.getName();
        }

        @Override
        public String getTerminalInfo() {
            String cardPresent = null;
            try{
                cardPresent = smartCardIOTerminal.isCardPresent()?"Card Present":"No card present";
            }catch(CardException ex){
                //Ignore
            }
            return "Name: "+smartCardIOTerminal.getName() + " (Description: "+smartCardIOTerminal.toString()+") "+cardPresent;
        }
    }    
}
