/*
 * //******************************************************************
 * //
 * // Copyright 2016 Intel Corporation All Rights Reserved.
 * //
 * //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * //
 * // Licensed under the Apache License, Version 2.0 (the "License");
 * // you may not use this file except in compliance with the License.
 * // You may obtain a copy of the License at
 * //
 * //      http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing, software
 * // distributed under the License is distributed on an "AS IS" BASIS,
 * // WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * // See the License for the specific language governing permissions and
 * // limitations under the License.
 * //
 * //-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

package org.iotivity.base.examples;

import org.iotivity.base.OcException;
import org.iotivity.base.OcRepresentation;

/**
 * Resource
 * 
 * This class is used by IotivityClient to create an object representation of a remote resource
 * and update the values depending on the server response
 */
abstract public class Resource {

    public static final String NAME_KEY = "name";
    public static final String URI_KEY = "uri";
    
    private String mName;
    private String mUri;

    public Resource() {
        mName = "";
        mUri = "";
    }

    public void setOcRepresentation(OcRepresentation rep) throws OcException {
        if (rep.hasAttribute(URI_KEY)) {
            setUri((String) rep.getValue(URI_KEY));
        }
        if (rep.hasAttribute(NAME_KEY)) {
            String nameToUse = null;
            // Fake for UPnP Bridge... use name from property file
            if (NamesPropertyFile.getInstance().hasUri(mUri)) {
                nameToUse = NamesPropertyFile.getInstance().getNameForUri(mUri);
            }

            if (nameToUse != null) {
                setName(nameToUse);

            } else {
                setName((String) rep.getValue(NAME_KEY));
            }
        }
    }

    public OcRepresentation getOcRepresentation() throws OcException {
        OcRepresentation rep = new OcRepresentation();
        if ((mUri != null) && (!mUri.isEmpty())) {
            rep.setValue(URI_KEY, mUri);
            rep.setUri(mUri);
        }
        if ((mName != null) && (!mName.isEmpty())) {
            rep.setValue(NAME_KEY, mName);
        }

        return rep;
    }

    public String getUri() {
        return mUri;
    }

    public void setUri(String uri) {
        mUri = (uri != null) ? uri : "";
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = (name != null) ? name : "";
    }

    @Override
    public String toString() {
        return "[" + URI_KEY + ": " + getUri() +
                ", " + NAME_KEY + ": " + getName() + "]";
    }
}
