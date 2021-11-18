/*
 *
 * Created on: 8/17/2021
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
import com.actian.di.designsdk.MapRep;
import com.actian.di.designsdk.map.ControlLink;
import com.actian.di.designsdk.map.DatatypeProperties;
import com.actian.di.designsdk.map.DatatypeRep;
import com.actian.di.designsdk.map.EventHandler;
import com.actian.di.designsdk.map.FieldRep;
import com.actian.di.designsdk.map.FieldType;
import com.actian.di.designsdk.map.SortInterface;
import com.actian.di.designsdk.map.SourceRep;
import com.actian.di.designsdk.map.TargetRep;

import java.util.List;

/**
 * @author wbunton
 */
public class WorkingWithDatatypesSample extends MapWithEventsAndRejectsSample {

    public WorkingWithDatatypesSample() {
        ACCFILE = "$(TRG)/StateAccountsDT.asc";
        REJFILE = "$(TRG)/StateAccountsRejectDT.asc";
        MAPNAME = "Datatypes";
    }

    // We are going to override the base setupSource.  We will call the base version, but then we're
    // going to change the datatype of the "Balance" field to Decimal, so that we don't need to do
    // the CDEC function in the event handler conditional expression.  We'll also go ahead and sort
    // by the "Balance" field.
    @Override
    protected SourceRep setupSource(MapRep map, String file) throws DesignSdkException {
        SourceRep src = super.setupSource(map, file);
        src.getRecords().setRecordPosition(0);
        setupSrcDatatype(src);
        setupSorting(src);
        return src;
    }

    // Change the source datatype to Decimal.  We're not going to do anything fancy with the datatype
    // handling, we just want to ensure it gets read as a numeric rather than text value.
    private void setupSrcDatatype(SourceRep src) throws DesignSdkException {
        FieldRep sFields = src.getRecords().getFields();
        sFields.setFieldPositionByName("Balance");
        DatatypeRep dt = sFields.getDataType();
        dt.setDataType("Decimal");
        dt.setLength(16);
    }

    // This is really the minimum to set up sorting.  You can get fancier, but we don't to reverse
    // the sort order.  We do want to ensure duplicates are allowed, however.
    private void setupSorting(SourceRep src) throws DesignSdkException {
        FieldRep sFields = src.getRecords().getFields();
        sFields.setFieldPositionByName("Balance");
        SortInterface sl = src.getSortLogic();
        sl.insertKeyAfter();
        sl.setPosition(0);      // Position to first (and only) key
        sl.setKeyType(FieldType.DECIMAL);
        sl.setKeyExpression("FieldAt(\"" + sFields.getFullPath() + "\")");
        sl.setAllowDuplicateRecords(true);
    }

    // We override setupEvents, call the base version, then change the event condition to skip the call
    // to CDEC, instead directly referencing the field.  Without the CDEC and without changing the
    // datatype, the comparison is done as text so the primary output, which should be values >= 500,
    // will contain all values whose first character is >= "5" (i.e. 60.73).
    @Override
    protected void setupEvents(SourceRep src, TargetRep trg, TargetRep rej) throws DesignSdkException {
        super.setupEvents(src, trg, rej);
        // Now change the conditional expression
        ControlLink link = src.getControlLink(0);
        EventHandler eh = link.getEvents().getEvent(0);
        // Records should still be positioned properly
        FieldRep sFields = src.getRecords().getFields();
        sFields.setFieldPositionByName("Balance");
        // The new conditional does not do the CDEC conversion, since the source field is a numeric
        // data type.  If it were still text, the second record in the primary output file would have
        // a balance of 75.09.
        eh.setCondition("FieldAt(\"" + sFields.getFullPath() + "\") >= 500");
    }

    // Override copyFields so we can modify the "Balance" field's datatype.  Note we will do different
    // changes for the primary vs the secondary target.
    @Override
    protected void copyFields(SourceRep src, TargetRep trg, List<String> fields) throws DesignSdkException {
        super.copyFields(src, trg, fields);
        trg.getRecords().setRecordPosition(0);
        FieldRep tFields = trg.getRecords().getFields();
        tFields.setFieldPositionByName("Balance");
        DatatypeRep dt = tFields.getDataType();
        if (trg.getName().equals("TARGET_1")) {
            dt.setDataType("Decimal");
            dt.setDecimalPlaces(4);
        }
        else {
            dt.setAlias("Number");
            dt.setAlign(DatatypeProperties.AlignType.Left);
        }
    }
}
