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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * ISO 4217
 * ISO 3-digit Currency Code
 *
 * http://www.iso.org/iso/support/faqs/faqs_widely_used_standards/widely_used_standards_other/currency_codes/currency_codes_list-1.htm
 *
 * java.util.Currency is pretty useless in java 1.6. Must wait for java 1.7 to get the methods:
 * getDisplayName()
 * getNumericCode()
 * getAvailableCurrencies()
 *
 * @author sasc
 */
public class ISO4217_Numeric {

    private static final HashMap<String, Currency> code2CurrencyMap;
    private static final HashMap<String, Integer> currencyCode2NumericMap;

    static {
        code2CurrencyMap = new HashMap<String, Currency>();
        currencyCode2NumericMap = new HashMap<String, Integer>();

        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(Util.loadResource(ISO3166_1.class, "/iso4217_numeric.txt"), "UTF-8"));

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() <= 0 || line.startsWith("#")) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(line, ",");
                String numericCodeStr = st.nextToken();
                String currencyCodeStr = st.nextToken();
                String displayName = st.nextToken();
                int numericCode = Integer.parseInt(numericCodeStr);
                code2CurrencyMap.put(numericCodeStr, new Currency(numericCode, currencyCodeStr, displayName));
                currencyCode2NumericMap.put(currencyCodeStr, numericCode);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    //Ignore
                }
            }
        }

    }

    public static String getCurrencyNameForCode(int code) {

        return getCurrencyNameForCode(String.valueOf(code));
    }

    public static String getCurrencyNameForCode(String code) {
        Currency c = code2CurrencyMap.get(code);
        if (c == null) {
            return null;
        }
        return c.getDisplayName();
    }

    public static Currency getCurrencyForCode(int code) {
        return code2CurrencyMap.get(String.valueOf(code));
    }

    public static Integer getNumericCodeForCurrencyCode(String currencyCode) {
        return currencyCode2NumericMap.get(currencyCode);
    }

    public static List<Integer> getNumericCodeForLocale(final Locale locale) {
        List<Integer> codeList = new ArrayList<Integer>();
        if (locale.getCountry() == null || locale.getCountry().length() != 2) {
            //We have no country! Might find more than 1 match
            for (Locale l : Locale.getAvailableLocales()) {
                if (l.getLanguage().equals(locale.getLanguage()) && l.getCountry() != null && l.getCountry().length() == 2) {
                    String currencyCode = java.util.Currency.getInstance(l).getCurrencyCode();
                    codeList.add(ISO4217_Numeric.getNumericCodeForCurrencyCode(currencyCode));
                }
            }
        }else if (locale.getCountry() != null && locale.getCountry().length() == 2) {
            String currencyCode = java.util.Currency.getInstance(locale).getCurrencyCode();
            codeList.add(ISO4217_Numeric.getNumericCodeForCurrencyCode(currencyCode));
        }
        return codeList;
    }

    public static class Currency {

        int numericCode;
        String code;
        String displayName;

        Currency(int numericCode, String code, String displayName) {
            this.numericCode = numericCode;
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static void main(String[] args) {
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(578));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(955));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(999));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(998));
        System.out.println(ISO4217_Numeric.getCurrencyNameForCode(1000));
        System.out.println(ISO4217_Numeric.getNumericCodeForCurrencyCode("USD"));
    }
}
