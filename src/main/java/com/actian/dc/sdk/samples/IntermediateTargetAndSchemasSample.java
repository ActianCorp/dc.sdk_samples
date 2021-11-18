/*
 * Created on: 8/30/2021
 *
 * Copyright (c) 2021 by Actian Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.actian.dc.sdk.samples;

import com.actian.di.designsdk.DesignSdkException;
import com.actian.di.designsdk.EngineFactory;
import com.actian.di.designsdk.MapRep;
import com.actian.di.designsdk.map.ControlLink;
import com.actian.di.designsdk.map.EventAction;
import com.actian.di.designsdk.map.EventHandler;
import com.actian.di.designsdk.map.EventInfo;
import com.actian.di.designsdk.map.FieldRep;
import com.actian.di.designsdk.map.IntermediateTarget;
import com.actian.di.designsdk.map.RecordRep;
import com.actian.di.designsdk.map.SortInterface;
import com.actian.di.designsdk.map.SourceInterface;
import com.actian.di.designsdk.map.SourceRep;
import com.actian.di.designsdk.map.TargetInterface;
import com.actian.di.designsdk.map.TargetRep;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * This sample will demonstrate how to build, save and use schemas, and how to work with an
 * intermediate target.  The sample will read the Accounts.txt file, sorting the source by the
 * state abbreviation field.  It will use an incore lookup to convert the state abbreviations to'
 * full state names.  The target will be XML; we will manually create the schema on the fly, save
 * it, and then use it in the target.  The final output will look like:
 * <States>
 *  <State name="Alaska">
 *   <Company name="Terry Friel">
 *    <Address>
 *     <Street>PO Box 756480</Street>
 *     <City>Fairbanks</City>
 *     <Zip>99775-6480</Zip>
 *    </Address>
 *    <Financials>
 *     <Standard_Payment>112.00</Standard_Payment>
 *     <Payments>120.85</Payments>
 *     <Balance>120.85</Balance>
 *    </Financials>
 *   </Company>
 *  </State>
 * </States>
 */
public class IntermediateTargetAndSchemasSample extends CreateSimpleMapSample {
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());
    private static final String MAPNAME = "AccountsToXML";
    private static final String TFILE = "$(TRG)/Accounts.xml";


    @Override
    protected boolean run() {
        try {
            MapRep map = EngineFactory.instance().createMap(V10);
            // Set the log file
            map.getOptions().getLogSettings().setLogFile("$(LOG)/IntermediateTargetAndSchemasSample.log");
            SourceRep src = setupSource(map, ACCOUNTS_SRC);
            setupSorting(src);
            IntermediateTarget lookup = setupLookup(map);
            TargetRep trg = setupTarget(map, TFILE, "TARGET_1");

            src.getRecords().setRecordPosition(0);
            lookup.getRecords().setRecordPosition(0);
            mapFields(trg, src.getRecords().getFields(), lookup.getRecords().getFields());
            setupEvents(src, lookup, trg);
            map.saveAs(createdArtifactPath(MAPNAME + ".map.rtc"), createdArtifactPath(MAPNAME + ".map"));
            map.execute(null);
        }
        catch (DesignSdkException e) {
            e.printStackTrace();
            logger.severe("Error running sample: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    // We want to sort the source on the State field, so we can use GroupStarted
    private void setupSorting(SourceRep src) throws DesignSdkException {
        FieldRep sFields = src.getRecords().getFields();
        src.getRecords().setRecordPosition(0);
        sFields.setFieldPositionByName("State");
        SortInterface sl = src.getSortLogic();
        sl.insertKeyAfter();
        sl.setPosition(0);      // Position to first (and only) key
        sl.setKeyExpression("FieldAt(\"" + sFields.getFullPath() + "\")");
        sl.setAllowDuplicateRecords(true);
    }

    // Presume src and target have their fields positioned properly.  Create a control link, and add
    // the specified event and action
    private void setEventAndAction(SourceInterface src, TargetInterface trg,
                                   String event) throws DesignSdkException {
        ControlLink link = src.addControlLink(src.getRecords().getPath(),
                                              "/" + trg.getName() + "/" + trg.getRecords().getPath());
        EventInfo info = link.getEvents().getEventInfoByName(event);
        EventHandler eh = link.getEvents().addEvent(info.getEventType());
        if (event.equals("GroupStarted")) {
            eh.setParameterValue(0, "field_list");  // groupby_type
            eh.setParameterValue(1, "State");       // State field is the group
        }
        EventAction action = eh.addAction();
        action.setType("OutputRecord");
    }

    private void setupEvents(SourceRep src, IntermediateTarget lookup, TargetRep trg)
                             throws DesignSdkException {
        src.getRecords().setRecordPosition(0);
        lookup.getRecords().setRecordPosition(0);
        trg.getRecords().setRecordPositionByPath("States");
        // Write States element at start
        setEventAndAction(src, trg, "SourceStarted");
        // Each time the state changes:
        setEventAndAction(src, lookup, "GroupStarted");     // Do the lookup
        trg.getRecords().setRecordPositionByPath("States/State");
        setEventAndAction(src, trg, "GroupStarted");        // And write the state element
        // Now, write the Company, Address and Financials for each record
        trg.getRecords().setRecordPositionByPath("States/State/Company");
        setEventAndAction(src, trg, "RecordStarted");
        trg.getRecords().setRecordPositionByPath("States/State/Company/Address");
        setEventAndAction(src, trg, "RecordStarted");
        trg.getRecords().setRecordPositionByPath("States/State/Company/Financials");
        setEventAndAction(src, trg, "RecordStarted");
    }

    // This just makes it a little easier
    private void mapField(FieldRep trg, String tname, FieldRep src, String sname) throws DesignSdkException {
        trg.setFieldPositionByName(tname);
        src.setFieldPositionByName(sname);
        trg.addLink(src.getFullPath(), false);
    }

    // Do special mapping of the company name
    private void mapCompanyName(FieldRep trg, FieldRep src) throws DesignSdkException {
        // This says check to see if the "Company" field is empty.  If so, we want to use the "Name" field,
        // otherwise the "Company" field.
        trg.setFieldPositionByName("name");
        StringBuilder exp = new StringBuilder("if Len(FieldAt(\"");
        src.setFieldPositionByName("Company");
        exp.append(src.getFullPath());
        exp.append("\"))== 0 then \r\n    FieldAt(\"");
        src.setFieldPositionByName("Name");
        exp.append(src.getFullPath());
        exp.append("\")\r\nelse\r\n    FieldAt(\"");
        src.setFieldPositionByName("Company");
        exp.append(src.getFullPath()).append("\")\r\nend if");
        trg.setExpression(exp.toString());
    }

    private void mapFields(TargetRep trg, FieldRep srcFields, FieldRep lookupFields) throws
                                                                                     DesignSdkException {
        // First, set up the lookup action key
        mapField(lookupFields, "ST", srcFields, "State");
        RecordRep records = trg.getRecords();
        FieldRep trgFields = records.getFields();
        records.setRecordPositionByPath("States/State");
        mapField(trgFields, "name", lookupFields, "STATE");
        records.setRecordPositionByPath("States/State/Company");
        mapCompanyName(trgFields, srcFields);
        records.setRecordPositionByPath("States/State/Company/Address");
        mapField(trgFields, "Street", srcFields, "Street");
        mapField(trgFields, "City", srcFields, "City");
        mapField(trgFields, "Zip", srcFields, "Zip");
        records.setRecordPositionByPath("States/State/Company/Financials");
        mapField(trgFields, "Standard_Payment", srcFields, "Standard Payment");
        mapField(trgFields, "Payments", srcFields, "Payments");
        mapField(trgFields, "Balance", srcFields, "Balance");
    }

    private IntermediateTarget setupLookup(MapRep map) throws DesignSdkException {
        IntermediateTarget lookup = map.addIntermediateTarget(IntermediateTarget.IntermediateType.IncoreLookup,
                                                              "LOOKUP");
        lookup.setConnectionTypeName("ASCII (Delimited)");
        lookup.setConnectionPart("file", "$(SRC)/StateCodeStateData.txt");
        lookup.setOption("header", "true");        // First record is a header
        lookup.connect();           // Connect so we build the schema
        return lookup;
    }

    @Override
    protected TargetRep setupTarget(MapRep map, String file, String name) throws DesignSdkException {
        // First, build the schema we're going to use
        String sname = new SchemaBuilder().build();
        TargetRep trg = map.addTarget(name);
        trg.setConnectionTypeName("XML-DMS");
        trg.setConnectionPart("file", file);
        // Now, set and load the schema.  Note that these are two separate things.  We need to set the
        // name so saving will reference the external schema, we need to load it so the target schema
        // is actually set (loaded).  With an external schema reference set, loading the transformation
        // will load the schema.  When loading, it's not in an encrypted djar, so no key is necessary.
        trg.getSchemaRef().setName(sname);
        trg.getSchemaRef().load(sname, null);
        return trg;
    }
}
