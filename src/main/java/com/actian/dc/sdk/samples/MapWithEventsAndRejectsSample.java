/*
 *
 * Created on: 7/29/2021
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
import com.actian.di.designsdk.map.SourceRep;
import com.actian.di.designsdk.map.TargetRep;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * This sample demonstrates adding a second target, and using events.  It uses some of the setup from
 * CreateSimpleMapSample
 */
public class MapWithEventsAndRejectsSample extends CreateSimpleMapSample {
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());
    String MAPNAME = "EventsAndRejects";
    String ACCFILE = "$(TRG)/StateAccounts.asc";
    String REJFILE = "$(TRG)/StateAccountsReject.asc";
    @Override
    protected boolean run() {
        List<String> fieldsToCopy = Arrays.asList("Account Number", "Company", "City", "State", "Zip",
                                                  "Balance");
        MapRep map;
        try {
            map = EngineFactory.instance().createMap(V10);
            // Set the log file
            map.getOptions().getLogSettings().setLogFile("$(LOG)/MapWithEventsAndRejectsSample.log");
            SourceRep src = setupSource(map, ACCOUNTS_SRC);
            TargetRep trg = setupTarget(map, ACCFILE, "TARGET_1");
            TargetRep rej = setupTarget(map, REJFILE, "TARGET_2");
            // We're copying a limited number of fields.
            copyFields(src, trg, fieldsToCopy);
            copyFields(src, rej, fieldsToCopy);

            // Go set up the events
            setupEvents(src, trg, rej);

            // Go ahead and save it, then execute
            map.saveAs(createdArtifactPath(MAPNAME + ".map.rtc"), createdArtifactPath(MAPNAME + ".map"));
            map.execute(null);
        }
        catch (DesignSdkException e) {
            logger.severe("Error running sample: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    protected ControlLink createLink(SourceRep src, TargetRep trg) throws DesignSdkException {
        src.getRecords().setRecordPosition(0);
        trg.getRecords().setRecordPosition(0);
        return src.addControlLink(src.getRecords().getPath(),
                                  "/" + trg.getName() + "/" + trg.getRecords().getPath());
    }

    protected void setupEvents(SourceRep src, TargetRep trg, TargetRep rej) throws DesignSdkException {
        ControlLink link = createLink(src, trg);
        // Add the RecordStarted event, set the condition and add 'if' and 'else' actions
        EventInfo info = link.getEvents().getEventInfoByName("RecordStarted");
        EventHandler eh = link.getEvents().addEvent(info.getEventType());
        FieldRep sFields = src.getRecords().getFields();
        sFields.setFieldPositionByName("Balance");
        eh.setCondition("CDec(FieldAt(\"" + sFields.getFullPath() + "\")) >= 500");
        EventAction action = eh.addAction();
        action.setType("OutputRecord");
        action = eh.addElseAction();
        action.setType("Reject");

        // We now have the condition set up to reject.  We now need to write those records
        // to the reject target
        link = createLink(src, rej);
        info = link.getEvents().getEventInfoByName("RecordRejected");
        eh = link.getEvents().addEvent(info.getEventType());
        action = eh.addAction();
        action.setType("OutputRecord");
    }
}
