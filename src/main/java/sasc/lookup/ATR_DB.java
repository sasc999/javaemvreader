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

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import sasc.util.Log;
import sasc.util.Util;

/**
 * Ludovic Rousseau's smartcard list
 * http://ludovic.rousseau.free.fr/softwares/pcsc-tools/smartcard_list.txt
 *
 * + some additional ATRs
 *
 *
 * @author sasc
 */
public class ATR_DB {

    private static final CountDownLatch initLatch = new CountDownLatch(1);
    private static final Map<String, PublicATR> atrMap = new ConcurrentHashMap<String, PublicATR>();
    private static final AtomicBoolean initCalled = new AtomicBoolean(false);

	public synchronized static void initialize() {
        if(initCalled.getAndSet(true)){
            return;
        }
		new Thread(new Runnable(){
				@Override
				public void run() {

                    InputStream is1 = null;
                    InputStream is2 = null;
                    BufferedReader br = null;

                    try{
                        is1 = Util.loadResource(ATR_DB.class, "/smartcard_list.txt");
                        is2 = Util.loadResource(ATR_DB.class, "/smartcard_list_additional_atrs.txt");
                        br = new BufferedReader(new InputStreamReader(new SequenceInputStream(is1, is2), "UTF-8"));

                        int lineNumber = 0;
                        String line;
                        String currentATR = null;
                        while((line = br.readLine()) != null){
                            ++lineNumber;
                            if(line.startsWith("#")  || line.trim().length() == 0){ //comment ^#/ empty line ^$/
                                continue;
                            }else if(line.startsWith("\t") && currentATR != null){
                                atrMap.get(currentATR).addDescriptiveText(line.replace("\t", "").trim());
            //                    Log.debug("Adding descriptive text for ATR="+currentATR+" "+line.replace("\t", ""));
                            }else if(line.startsWith("3")){ // ATR hex
                                currentATR = line.toUpperCase().trim();
                                if(!atrMap.containsKey(currentATR)){
                                    atrMap.put(currentATR, new PublicATR(line));
                                }else{
            //                        Log.debug("Found existing ATR: "+currentATR);
                                }
                            }else{
                                Log.debug("Encountered unexpected line in atr list: currentATR="+currentATR+" Line("+lineNumber+")="+line);
                                //Just skip
                                //throw new RuntimeException("Encountered unexpected line in atr list: currentATR="+currentATR+" Line="+line);
                            }
                        }
                    }catch(IOException e){
                        throw new RuntimeException(e);
                    }finally{
                        if(is1 != null){
                            try {
                                is1.close();
                            } catch (IOException ex) {
                                //Ignore
                            }
                        }
                        if(is2 != null){
                            try {
                                is2.close();
                            } catch (IOException ex) {
                                //Ignore
                            }
                        }
                        if(br != null){
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
    
    public static Map<String, PublicATR> getAll() {
		awaitInit();
	    return Collections.unmodifiableMap(atrMap);
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

    public static class PublicATR{
        String atr;
        List<String> descriptiveText = new ArrayList<String>();

        private PublicATR(String atr){
            this.atr = atr; //With spaces between bytes
        }

        private void addDescriptiveText(String text){
            descriptiveText.add(text);
        }

        public List<String> getDescriptiveText(){
            return Collections.unmodifiableList(descriptiveText);
        }

    }

    public static List<String> searchATR(byte[] atr){
        awaitInit();
        String atrStr = Util.prettyPrintHexNoWrap(atr).toUpperCase();
        for(String atrPatternStr : atrMap.keySet()){
            if(atrStr.matches("^"+atrPatternStr+"$")){
                PublicATR publicATR = atrMap.get(atrPatternStr);
                if(publicATR != null){
                    return publicATR.getDescriptiveText();
                }
            }
        }
        return null;
    }

    public static void main(String[] args){
        initialize();
        System.out.println(ATR_DB.searchATR(new byte[]{(byte)0x3B, (byte)0x90, (byte)0x95, (byte)0x80, (byte)0x1F, (byte)0xC3, (byte)0x59}));
        System.out.println(ATR_DB.searchATR(new byte[]{(byte)0x3B, (byte)0x04, (byte)0xA2, (byte)0x13, (byte)0x10, (byte)0x91}));
        System.out.println(ATR_DB.searchATR(new byte[]{(byte)0x3B, (byte)0x67, (byte)0x00, (byte)0x00, (byte)0xa6, (byte)0x40, (byte)0x40, (byte)0x00, (byte)0x09, (byte)0x90, (byte)0x00}));
        System.out.println(ATR_DB.searchATR(new byte[]{(byte)0x3B, (byte)0x24, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x45}));
        System.out.println(ATR_DB.searchATR(Util.fromHexString("3F 6A 00 00 00 64 01 50 01 0C 82 01 01 A9")));
//        3b 67 00 00 a6 40 40 00 09 90 00 
    }
}
