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

import sasc.lookup.ATR_DB;
import sasc.lookup.IIN_DB;
import sasc.lookup.RID_DB;

/**
 *
 * @author sasc
 */
public class Context {
    private Context(){
        throw new UnsupportedOperationException("Not allowed to instantiate");
    }
    
    public static synchronized void init(){
        IIN_DB.initialize();
        ATR_DB.initialize();
        RID_DB.initialize();
        try{
            Class.forName("sasc.util.ISO3166_1");
            Class.forName("sasc.util.ISO4217_Numeric");
        }catch(ClassNotFoundException ex){
            throw new RuntimeException(ex);
        }
    }
}
