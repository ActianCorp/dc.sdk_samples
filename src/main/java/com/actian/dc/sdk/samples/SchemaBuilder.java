/*
 *
 * Created on: 9/1/2021
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
import com.actian.di.designsdk.map.DatatypeProperties;
import com.actian.di.designsdk.map.FieldRep;
import com.actian.di.designsdk.map.RecordRep;
import com.actian.di.designsdk.map.TargetRep;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * @author wbunton
 */
public class SchemaBuilder {
    public static final String STATES = "States";
    public static final String STATE = "State";
    public static final String COMPANY = "Company";
    public static final String ADDRESS = "Address";
    public static final String FINANCIALS = "Financials";

    private static final String SCHEMANAME = "AccountsXMLSchema.schema";

    public String build() throws DesignSdkException {
        TargetRep trg = EngineFactory.instance().createTarget(V10);
        trg.setConnectionTypeName("XML-DMS");
        RecordRep schema = trg.getRecords();
        addStatesToSchema(schema);
        addStateToSchema(schema);
        addCompanyToSchema(schema);
        addAddressToSchema(schema);
        addFinancialsToSchema(schema);

        String sname = Sample.createdArtifactPath(SCHEMANAME);
        trg.getSchemaRef().save(sname);
        return sname;
    }

    private void addStateToSchema(RecordRep schema) throws DesignSdkException {
        // Position to the parent, add a record reference.
        schema.setRecordPositionByPath(STATES);
        addRecordReference(schema, STATE, makePath(STATES, STATE), true);
        FieldRep fields = schema.getFields();
        fields.insertAfter();           // Add the "name" field
        fields.setFieldPosition(0);
        fields.setName("name");
        fields.getDataType().setAlias("String Attribute");
    }

    private void addStatesToSchema(RecordRep schema) throws DesignSdkException {
        // Here we position to the unlisted "Root Defs" typedef.  Doing this allows adding new
        // root record references.
        schema.setRecordTypePosition("Root Defs");
        addRecordReference(schema, STATES, STATES, false);
    }

    private void addCompanyToSchema(RecordRep schema) throws DesignSdkException {
        schema.setRecordPositionByPath(makePath(STATES, STATE));
        addRecordReference(schema, COMPANY, makePath(STATES, STATE, COMPANY), true);
        // Now add our name field
        FieldRep fields = schema.getFields();
        fields.insertAfter();           // Add the "name" field
        fields.setFieldPosition(0);
        fields.setName("name");
        fields.getDataType().setAlias("String Attribute");
    }

    private void addAddressToSchema(RecordRep schema) throws DesignSdkException {
        schema.setRecordPositionByPath(makePath(STATES, STATE, COMPANY));
        String mypath = makePath(STATES, STATE, COMPANY, ADDRESS);
        addRecordReference(schema, ADDRESS, mypath, false);
        // Now add the data fields
        FieldRep fields = schema.getFields();
        addStringField(fields, "Street", 35);
        addStringField(fields, "City", 16);
        addStringField(fields, "Zip", 10);
    }

    private void addFinancialsToSchema(RecordRep schema) throws DesignSdkException {
        schema.setRecordPositionByPath(makePath(STATES, STATE, COMPANY));
        String mypath = makePath(STATES, STATE, COMPANY, FINANCIALS);
        addRecordReference(schema, FINANCIALS, mypath, false);
        // Now add the data fields
        FieldRep fields = schema.getFields();
        addStringField(fields, "Standard_Payment", 6);
        addStringField(fields, "Payments", 7);
        addStringField(fields, "Balance", 6);
    }

    private void addRecordReference(RecordRep schema, String type, String path, boolean unbounded) throws DesignSdkException {
        schema.addRecordType(makeTypename(type));
        FieldRep fields = schema.getFields();
        if (fields.fieldCount() > 0)
            fields.setFieldPosition(fields.fieldCount() - 1);
        fields.insertAfter();
        fields.setFieldPosition(fields.fieldCount() - 1);
        fields.setName(type);
        fields.getDataType().setDataType(DatatypeProperties.DT_STRUCT);
        if (unbounded)
            fields.setMaxOccurs(Long.MAX_VALUE);
        fields.setRecordReference(makeTypename(type));
        // We've added the record reference to the parent.  Now switch to the child record type
        // and fix the group name
        schema.setRecordPositionByPath(path);
        schema.getGroups().setGroupPosition(0);
        schema.getGroups().setName(makeGroupname(type));
    }

    private void addStringField(FieldRep fields, String name, int length) throws DesignSdkException {
        fields.insertAfter();
        fields.setFieldPosition(fields.fieldCount() - 1);
        fields.setName(name);
        fields.getDataType().setAlias("String");
        fields.getDataType().setLength(length);

    }
    public static String makePath(String... segments) {
        StringBuilder path = new StringBuilder();
        for (String seg : segments) {
            if (path.length() > 0)
                path.append("/");
            path.append(seg);
        }
        return path.toString();
    }

    public static String makeTypename(String type) {
        return type + "_Type";
    }
    public static String makeGroupname(String group) {
        return group + "_Group";
    }

}
