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
 * Links
 */
public class Links {

    public static final String LINKS_KEY = "links";

    private Link[] links;

    public Links(Link[] links) {
        this.links = (links != null) ? links : new Link[0];
    }

    public void setOcRepresentation(OcRepresentation[] ocRepLinks) throws OcException {
        if ((ocRepLinks != null) && (ocRepLinks.length > 0)) {
            links = new Link[ocRepLinks.length];
            int index = 0;
            for (OcRepresentation ocRepLink : ocRepLinks) {
                links[index] = new Link("", new String[0]);
                links[index].setOcRepresentation(ocRepLink);
                ++index;
            }
        }
    }

    public OcRepresentation[] getOcRepresentation() throws OcException {
        OcRepresentation[] ocRepLinks = new OcRepresentation[links.length];
        int index = 0;
        for (Link link : links) {
            OcRepresentation ocRepLink = link.getOcRepresentation();
            ocRepLinks[index] = ocRepLink;
            ++index;
        }
        return ocRepLinks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Link link : links) {
            if (!first) {
                sb.append(", ");
            }
            if (link != null) {
                sb.append(link.toString());
            }
            first = false;
        }
        sb.append("]");

        return sb.toString();
    }
}
