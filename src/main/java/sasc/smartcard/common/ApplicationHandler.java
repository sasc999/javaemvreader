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

import sasc.iso7816.AID;
import sasc.terminal.CardConnection;
import sasc.terminal.TerminalException;

/**
 *
 * @author sasc
 */
public interface ApplicationHandler {
    public boolean process(AID aid, SmartCard card, CardConnection cardConnection) throws TerminalException;
}
