/*
 *
 * Created on: 7/27/2021
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
import com.actian.di.designsdk.map.FieldRep;
import com.actian.di.designsdk.map.SourceRep;
import com.actian.di.designsdk.map.TargetRep;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * Create a simple transformation.  The transformation simply copies data from the source to the target,
 * where the source is delimited ASCII and the target contains the same data, but in fixed ASCII format.
 * This transformation meets the requirements to add a default output event/action, so the code never
 * sets up events/actions.  After creating the transformation, it is saved and then executed.
 */
public class CreateSimpleMapSample extends Sample {
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());
    private static final String MAPNAME = "SimpleMap";
    static final String TFILE = "$(TRG)/AccountsFixed.asc";
    @Override
    protected boolean run() {
        try {
            MapRep map = EngineFactory.instance().createMap(V10);
            // Set the log file
            map.getOptions().getLogSettings().setLogFile("$(LOG)/CreateSimpleMap.log");
            SourceRep src = setupSource(map, ACCOUNTS_SRC);
            TargetRep trg = setupTarget(map, TFILE, "TARGET_1");

            // Connecting the source should give us a schema, since the delimited ASCII connector does not
            // require a schema be set (see src.getCapabilities)
            copyFields(src, trg, null);
            // Now save it
            map.saveAs(createdArtifactPath(MAPNAME + ".map.rtc"), createdArtifactPath(MAPNAME + ".map"));
            // At this point, since we only have a single source and target record type, and the target is
            // not multimode, we can execute the transformation.  We didn't add any action that would
            // cause a record to be output, but the engine detects that and adds a default event/action
            // that will write a record.
            map.execute(null);

            // Verify
            File tfile = new File(map.getSystemContext().expandMacros(TFILE));
            if (tfile.length() != 44702) {
                logger.severe("Target file should be 44702 bytes, was actually " + tfile.length() + " bytes");
                return false;
            }
        }
        catch (DesignSdkException e) {
            logger.severe("Error running sample: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    /**
     * Copies fields from the source control to the target control, and sets the map expression to copy.
     * If <code>fields</code> is not empty/null, then only fields named in the list will be copied
     * @param src source control
     * @param trg target control to add fields
     * @param fields list of fields to copy
     * @throws DesignSdkException if an error occurs
     */
    protected void copyFields(SourceRep src, TargetRep trg, List<String> fields) throws DesignSdkException {
        if (fields != null && fields.isEmpty())
            fields = null;
        src.getRecords().setRecordPosition(0);
        FieldRep srcFields = src.getRecords().getFields();
        trg.getRecords().setRecordPosition(0);
        FieldRep trgFields = trg.getRecords().getFields();
        // Copy source fields to target, and set map expression
        for (int i = 0; i < srcFields.fieldCount(); ++i) {
            srcFields.setFieldPosition(i);
            if (fields == null || fields.contains(srcFields.getName())) {
                trgFields.insertAfter();
                trgFields.setFieldPosition(trgFields.fieldCount() - 1);
                trgFields.copy(srcFields, true);
                // Make the length a little longer, to make it more readable to humans
                trgFields.getDataType().setLength(trgFields.getDataType().getLength() + 2);
                trgFields.setExpression("fieldat(\"/SOURCE_1/" + src.getRecords().getName() + "/"
                                        + srcFields.getName() + "\")");
            }
        }
    }

    protected SourceRep setupSource(MapRep map, String file) throws DesignSdkException {
        // Add the new source to the transformation
        SourceRep src = map.addSource("SOURCE_1");

        // Set the type and the needed connection parts and options
        src.setConnectionTypeName("ASCII (Delimited)");
        src.setConnectionPart("file", file);
        src.setOption("header", "true");        // First record is a header

        // Connect.  This will make the connector open the file and read the header, and create a schema
        // based on the data found.
        src.connect();

        return src;
    }

    protected TargetRep setupTarget(MapRep map, String file, String name) throws DesignSdkException {
        TargetRep trg = map.addTarget(name);

        // Configure and connect the target
        trg.setConnectionTypeName("ASCII (Fixed)");
        trg.setConnectionPart("file", file);
        // Note that "CR-LF" is in the allowed value list, so will get translated properly into
        // "\r\n" when being outputl
        trg.setOption("recsep", "CR-LF");

        trg.connect();
        return trg;
    }
}
