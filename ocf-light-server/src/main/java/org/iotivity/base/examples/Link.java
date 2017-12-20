/*
 * //******************************************************************
 * //
 * // Copyright 2017 Intel Corporation All Rights Reserved.
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
 * Link
 */
public class Link {

    static private final String HREF_KEY = "href";
    static private final String REL_KEY = "rel";
    static private final String RT_KEY = "rt";
    static private final String DEFAULT_REL = "contains";

    private String href;
    private String rel;
    private String[] rt;

    public Link(String href, String[] rt) {
        this(href, DEFAULT_REL, rt);
    }

    public Link(String href, String rel, String[] rt) {
        this.href = href;
        this.rel = rel;
        this.rt = rt;
    }

    public void setOcRepresentation(OcRepresentation ocRep) throws OcException {
        if (ocRep.hasAttribute(HREF_KEY)) {
            href = ocRep.getValue(HREF_KEY);
        }
        if (ocRep.hasAttribute(REL_KEY)) {
            rel = ocRep.getValue(REL_KEY);
        }
        if (ocRep.hasAttribute(RT_KEY)) {
            rt = ocRep.getValue(RT_KEY);
        }
    }

    public OcRepresentation getOcRepresentation() throws OcException {
        OcRepresentation ocRep = new OcRepresentation();
        ocRep.setValue(HREF_KEY, href);
        ocRep.setValue(REL_KEY, rel);
        ocRep.setValue(RT_KEY, rt);
        return ocRep;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String type : rt) {
            if (!first) {
                sb.append(", ");
            }
            if (type != null) {
                sb.append(type.toString());
            }
            first = false;
        }
        sb.append("]");

        return "[" + HREF_KEY + ": " + href + ", " + REL_KEY + ": " + rel + ", " + RT_KEY + ": " + sb + "]";
    }
}
