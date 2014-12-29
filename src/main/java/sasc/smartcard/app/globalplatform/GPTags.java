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

import sasc.iso7816.Tag;
import sasc.iso7816.TagImpl;
import sasc.iso7816.TagValueType;

/**
 * Global Platform tags
 * 
 * @author sasc
 */
public class GPTags {

    public static final Tag CARD_MANAGEMENT_TYPE_AND_VERSION_OID   = new TagImpl("60",   TagValueType.BINARY, "Card Management Type And Version OID", "'Application Tag 0'. {globalPlatform 2 v} - GP version in last 3 bytes");
    public static final Tag CARD_IDENTIFICATION_SCHEME_OID         = new TagImpl("63",   TagValueType.BINARY, "Card Identification Scheme OID", "{globalPlatform 3} - Indicates a GP card that is uniquely identified by the Issuer Identification Number (IIN) and Card Image Number (CIN)");
    public static final Tag SECURE_CHANNEL_OID                     = new TagImpl("64",   TagValueType.BINARY, "Secure Channel Protocol and implementation options OID", "{globalPlatform 4 scp i}");
    public static final Tag CARD_CONFIGURATION_DETAILS             = new TagImpl("65",   TagValueType.BINARY, "Card Configuration Details", "");
    public static final Tag CARD_CHIP_DETAILS                      = new TagImpl("66",   TagValueType.BINARY, "Card / Chip Details", "");

    public static final Tag SECURITY_DOMAIN_MANAGEMENT_DATA        = new TagImpl("73",   TagValueType.BINARY, "Security domain management data", "");

    public static final Tag KEY_INFO_DATA                          = new TagImpl("c0",   TagValueType.BINARY, "Key Information Data", "");
    public static final Tag KEY_INFO_TEMPLATE                      = new TagImpl("e0",   TagValueType.BINARY, "Key Information Template", "");

    public static final Tag APPLICATION_PRODUCTION_LIFECYCLE_DATA  = new TagImpl("9f6e", TagValueType.BINARY, "Application production life cycle data", "");
    public static final Tag MAXIMUM_LENGTH_COMMAND_DATA_FIELD      = new TagImpl("9f65", TagValueType.BINARY, "Max length of data field in command message", "");
    public static final Tag CPLC                                   = new TagImpl("9f7f", TagValueType.BINARY, "Card Production Life Cycle Data", "");
}
