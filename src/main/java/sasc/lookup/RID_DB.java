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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import sasc.iso7816.RID;
import sasc.util.Util;

/**
 * Summary of the Register of Issued Numbers Report (RMG)
 *
 * @author sasc
 */
public class RID_DB {

    private static final CountDownLatch initLatch = new CountDownLatch(1);
    private static final Map<String, RID> ridMap = new ConcurrentHashMap<String, RID>();
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
                    InputStream is2 = null;
                    InputStream is3 = null;
                    BufferedReader br = null;

                    try {
                        is1 = Util.loadResource(RID_DB.class, "/rid_list_rmg.txt");
                        is2 = Util.loadResource(RID_DB.class, "/rid_list_other.txt");
                        is3 = Util.loadResource(RID_DB.class, "/rid_list_country.txt");
                        ArrayList<InputStream> a = new ArrayList<InputStream>();
                        a.add(is1);
                        a.add(is2);
                        a.add(is3);
                        br = new BufferedReader(new InputStreamReader(new SequenceInputStream(Collections.enumeration(a)), "UTF-8"));

                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("#") || line.trim().length() == 0) {
                                continue;
                            } else {
                                StringTokenizer st = new StringTokenizer(line, ";");
                                if (st.countTokens() != 3) {
                                    throw new RuntimeException("RID lists should contain three values pr line separated by \";\" . "+line);
                                }
                                String ridStr = st.nextToken().trim();
                                String applicant = st.nextToken().trim();
                                String country = st.nextToken().trim();
                                if (ridMap.containsKey(ridStr)) { //Should not happen
                                    throw new RuntimeException("RID: Duplicate value \"" + ridStr + "\" found");
                                }
                                RID rid = new RID(ridStr, applicant, country);
                                ridMap.put(ridStr, rid);
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
                        if (is2 != null) {
                            try {
                                is2.close();
                            } catch (IOException ex) {
                                //Ignore
                            }
                        }
                        if (is3 != null) {
                            try {
                                is3.close();
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

    public static Map<String, RID> getAll() {
		awaitInit();
	    return Collections.unmodifiableMap(ridMap);
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
    public static RID searchRID(byte[] rid) {
        awaitInit();
        return ridMap.get(Util.byteArrayToHexString(rid).toUpperCase());
    }

    public static void main(String[] args) {
        initialize();
        System.out.println(RID_DB.searchRID(new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01}));
        System.out.println(RID_DB.searchRID(new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x10}));
        System.out.println(RID_DB.searchRID(new byte[]{(byte) 0xD5, (byte) 0x78, (byte) 0x00, (byte) 0x00, (byte) 0x02}));
        System.out.println(RID_DB.searchRID(Util.fromHexString("D276000010")));
    }
}