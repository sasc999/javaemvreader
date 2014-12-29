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
package sasc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * ISO 3166-1
 * ISO Country Code (3 digit Numeric)
 *
 * java.util.Locale doesn't support the 3-digit code variant of 3166 (part 1),
 * so we must use our own list
 *
 * @author sasc
 */
public class ISO3166_1 {
    
    private final static HashMap<String, String> map;
    
    static{
        map = new HashMap<String, String>();

        BufferedReader br = null;

        try{
            br = new BufferedReader(new InputStreamReader(Util.loadResource(ISO3166_1.class, "/iso3166_1_numeric.txt"), "UTF-8"));

            String line;
            while((line = br.readLine()) != null){
                if(line.trim().length() < 4 || line.startsWith("#")){
                    continue;
                }
                map.put(line.substring(0, 3), line.substring(4));
            }
        }catch(IOException e){
            throw new RuntimeException(e);
        }finally{
            if(br != null){
                try {
                    br.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }
        
    }

    public static String getCountryForCode(int code){
        return getCountryForCode(String.valueOf(code));
    }

    public static String getCountryForCode(String code){

        return map.get(code);
    }
    
}


