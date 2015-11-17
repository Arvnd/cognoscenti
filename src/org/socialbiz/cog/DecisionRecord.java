/*
 * Copyright 2013 Keith D Swenson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors Include: Shamim Quader, Sameer Pradhan, Kumar Raja, Jim Farris,
 * Sandia Yang, CY Chen, Rajiv Onat, Neal Wang, Dennis Tam, Shikha Srivastava,
 * Anamika Chaudhari, Ajay Kakkar, Rajeev Rastogi
 */

package org.socialbiz.cog;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.workcast.json.JSONObject;

public class DecisionRecord extends DOMFace {

    //these codes match those for History record
    public final static int SOURCE_TYPE_TOPIC        = 4;
    public final static int SOURCE_TYPE_MEETING      = 7;
    public final static int SOURCE_TYPE_DECISION     = 8;

    public DecisionRecord(Document nDoc, Element nEle, DOMFace p) {
        super(nDoc, nEle, p);
    }

    public int getNumber() throws Exception {
        return getAttributeInt("num");
    }
    public void setNumber(int newVal) throws Exception {
        setAttributeInt("num", newVal);
    }

    public String getDecision() throws Exception {
        return getScalar("decision");
    }
    public void setDecision(String newVal) throws Exception {
        setScalar("decision", newVal);
    }
    public String getContentHtml(AuthRequest ar)  throws Exception {
        return WikiConverterForWYSIWYG.makeHtmlString(ar, getDecision());
    }
    public void setContentHtml(AuthRequest ar, String newHtml) throws Exception {
        setDecision(HtmlToWikiConverter.htmlToWiki(ar.baseURL, newHtml));
    }


    public long getTimestamp() throws Exception {
        return getAttributeLong("timestamp");
    }
    public void setTimestamp(long newVal) throws Exception {
        setAttributeLong("timestamp", newVal);
    }

    //Where did this decision come from???
    public int getSourceType() throws Exception {
        return getAttributeInt("sourceType");
    }
    public void setSourceType(int newVal) throws Exception {
        setAttributeInt("sourceType", newVal);
    }
    public String getSourceId() throws Exception {
        return getAttribute("sourceId");
    }
    public void setSourceId(String newVal) throws Exception {
        setAttribute("sourceId", newVal);
    }
    public long getSourceCmt() throws Exception {
        return getAttributeLong("sourceCmt");
    }
    public void setSourceCmt(long newVal) throws Exception {
        setAttributeLong("sourceCmt", newVal);
    }

    public String getSourceUrl(AuthRequest ar, NGPage ngp) throws Exception {
        return HistoryRecord.lookUpResourceURL(ar, ngp, getSourceType(), getSourceId())
                + "#cmt" + getSourceCmt();
    }


    /**
     * the universal id is a globally unique ID for this decision, composed of the
     * id for the server, the project, and the action item. This is set at the point
     * where the action item is created and remains with the note as it is carried
     * around the system as long as it is moved as a clone from a project to a
     * clone of a project. If it is copied or moved to another project for any
     * other reason, then the universal ID should be reset.
     */
    public String getUniversalId() throws Exception {
        return getScalar("universalid");
    }

    public void setUniversalId(String newID) throws Exception {
        setScalar("universalid", newID);
    }


    /**
     * get the labels on a document -- only labels valid in the project,
     * and no duplicates
     */
    public List<NGLabel> getLabels(NGPage ngp) throws Exception {
        Vector<NGLabel> res = new Vector<NGLabel>();
        for (String name : getVector("labels")) {
            NGLabel aLabel = ngp.getLabelRecordOrNull(name);
            if (aLabel!=null) {
                if (!res.contains(aLabel)) {
                    res.add(aLabel);
                }
            }
        }
        return res;
    }

    /**
     * set the list of labels on a document
     */
    public void setLabels(List<NGLabel> values) throws Exception {
        Vector<String> labelNames = new Vector<String>();
        for (NGLabel aLable : values) {
            labelNames.add(aLable.getName());
        }
        //Since this is a 'set' type vector, always sort them so that they are
        //stored in a consistent way ... so files are more easily compared
        Collections.sort(labelNames);
        setVector("labels", labelNames);
    }


    public JSONObject getJSON4Decision(NGPage ngp, AuthRequest ar) throws Exception {
        JSONObject thisDecision = new JSONObject();
        thisDecision.put("universalid", getUniversalId());
        thisDecision.put("num", getNumber());
        thisDecision.put("timestamp", getTimestamp());
        JSONObject labelMap = new JSONObject();
        for (NGLabel lRec : getLabels(ngp) ) {
            labelMap.put(lRec.getName(), true);
        }
        thisDecision.put("labelMap",  labelMap);
        thisDecision.put("html", getContentHtml(ar));
        thisDecision.put("sourceType", getSourceType());
        thisDecision.put("sourceId", getSourceId());
        thisDecision.put("sourceCmt", getSourceCmt());
        thisDecision.put("sourceUrl", getSourceUrl(ar, ngp));
        return thisDecision;
    }

    public void updateDecisionFromJSON(JSONObject decisionObj, NGPage ngp, AuthRequest ar) throws Exception {
        String universalid = decisionObj.getString("universalid");
        if (!universalid.equals(getUniversalId())) {
            //just checking, this should never happen
            throw new Exception("Error trying to update the record for a decision with UID ("
                    +getUniversalId()+") with post from decision with UID ("+universalid+")");
        }
        if (decisionObj.has("html")) {
            setContentHtml(ar, decisionObj.getString("html"));
        }
        if (decisionObj.has("timestamp")) {
            setTimestamp(decisionObj.getLong("timestamp"));
        }
        if (decisionObj.has("labelMap")) {
            JSONObject labelMap = decisionObj.getJSONObject("labelMap");
            Vector<NGLabel> selectedLabels = new Vector<NGLabel>();
            for (NGLabel stdLabel : ngp.getAllLabels()) {
                String labelName = stdLabel.getName();
                if (labelMap.optBoolean(labelName)) {
                    selectedLabels.add(stdLabel);
                }
            }
            setLabels(selectedLabels);
        }

        if (decisionObj.has("sourceType")) {
            setSourceType(decisionObj.getInt("sourceType"));
        }
        if (decisionObj.has("sourceId")) {
            setSourceId(decisionObj.getString("sourceId"));
        }
        if (decisionObj.has("sourceCmt")) {
            setSourceCmt(decisionObj.getLong("sourceCmt"));
        }

    }

}