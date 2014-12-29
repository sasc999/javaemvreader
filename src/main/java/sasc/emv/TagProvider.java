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
package sasc.emv;

import sasc.iso7816.Tag;

/**
 *
 * Contains Tags specific to a Tag Authority
 * The authority is identified either by Issuer Identification Number or RID
 * 
 * Example authorities:
 * VISA
 * MasterCard
 * GlobalPlatform
 * ISO7816
 * EMV
 * 
 * @author sasc
 */
public interface TagProvider {
    
    /**
     * If the tag is not found, this method returns the "[UNHANDLED TAG]" containing 'tagBytes'
     *
     * @param tagBytes
     * @return
     */
    public Tag getNotNull(byte[] tagBytes);

    /**
     * Returns null if Tag not found
     */
    public Tag find(byte[] tagBytes);
}
