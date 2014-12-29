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
package sasc.lookup;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import sasc.util.Util;

/**
 * IIN (Issuer Identification Number), aka BIN - Bank Identification Number. 
 * 
 * ISO/IEC 7812
 *
 * @author sasc
 */
public class IIN_DB {

    private static final CountDownLatch initLatch = new CountDownLatch(1);
    private static final Map<String, IIN> iinMap = new ConcurrentHashMap<String, IIN>();
    private static final AtomicBoolean initCalled = new AtomicBoolean(false);

	public synchronized static void initialize() {
        if(initCalled.getAndSet(true)){
            return;
//            throw new IllegalStateException("initialize() already called");
        }
		new Thread(new Runnable(){
				@Override
				public void run() {

                    InputStream is1 = null;
                    BufferedReader br = null;

                    try {
                        is1 = Util.loadResource(IIN_DB.class, "/iin_bin_list.txt");
                        br = new BufferedReader(new InputStreamReader(is1));

                        String line;

                        //Skip first line
                        line = br.readLine();

                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("#") || line.trim().length() == 0) {
                                continue;
                            } else {
                                StringTokenizer st = new StringTokenizer(line, ";");

                                String iinStr = null;
                                String location = "";
                                String type = "";
                                String issuerName = "";
                                String phoneNumber = "";

                                iinStr = st.nextToken();
                                if (st.hasMoreTokens()) {
                                    location = st.nextToken();
                                }
                                if (st.hasMoreTokens()) {
                                    type = st.nextToken();
                                }
                                if (st.hasMoreTokens()) {
                                    issuerName = st.nextToken();
                                }
                                if (st.hasMoreTokens()) {
                                    phoneNumber = st.nextToken();
                                }

                                if (iinMap.containsKey(iinStr)) {
                                    throw new RuntimeException("IIN/BIN: Duplicate value \"" + iinStr + "\" found");
                                }
                                IIN iin = new IIN(iinStr, location, type, issuerName, phoneNumber);
                                iinMap.put(iinStr, iin);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (is1 != null) {
                            try {
                                is1.close();
                            } catch (IOException ex) {
                                //Ignore
                            }
                        }
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException ex) {
                                //Ignore
                            }
                        }
						initLatch.countDown();
					}
				}
			}).start();

	}
    
    /**
     * Disable this database 
     * (for example on memory restricted devices)
     */
    public synchronized static void disable() {
        initCalled.set(true);
        initLatch.countDown();
    }
    
    public static Map<String, IIN> getAll() {
		awaitInit();
	    return Collections.unmodifiableMap(iinMap);
    }
    
    public static boolean awaitInit(){
        if(!initCalled.get()){
            throw new IllegalStateException("Not initalized. Call initialize() first");
        }
		try{
			initLatch.await();
			return true;
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
		return false;
	}

    public static IIN searchIIN(int iin) {
        awaitInit();
        return iinMap.get(String.valueOf(iin));
    }

    public static void main(String[] args) throws Throwable {
        initialize();
        System.out.println(IIN_DB.searchIIN(492564));

    }

    public static class IIN {

        String iin;
        String location;
        String type;
        String issuerName;
        String phoneNumber;

        public IIN(String iin, String location, String type, String issuerName, String phoneNumber) {
            if (iin == null) {
                throw new IllegalArgumentException("iin cannot be null");
            }
            this.iin = iin;
            this.location = location;
            this.type = type;
            this.issuerName = issuerName;
            this.phoneNumber = phoneNumber;
        }

        public String getIIN() {
            return iin;
        }

        public String getLocation() {
            return location;
        }

        public String getType() {
            return type;
        }

        public String getIssuerName() {
            return issuerName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public String getDescription(){
            StringBuilder buf = new StringBuilder();
            if(issuerName != null && !issuerName.isEmpty()){
                buf.append(issuerName);
            }
            if(type != null && !type.isEmpty()){
                if(buf.length() > 0){
                    buf.append(", ");
                }
                buf.append(type);
            }
            if(location != null && !location.isEmpty()){
                if(buf.length() > 0){
                    buf.append(", ");
                }
                buf.append(location);
            }
            
            return buf.toString();
        }

        @Override
        public String toString() {
            return "IIN(" + iin + "," + location + "," + type + "," + issuerName + "," + phoneNumber + ")";
        }
    }
}
