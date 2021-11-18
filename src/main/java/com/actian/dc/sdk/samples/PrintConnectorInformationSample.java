/*
 *
 * Created on: 7/28/2021
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
import com.actian.di.designsdk.OptionInfo;
import com.actian.di.designsdk.map.ConnectInfoEntry;
import com.actian.di.designsdk.map.ConnectionCapabilities;
import com.actian.di.designsdk.map.ControlInterface;
import com.actian.di.designsdk.map.DatatypeInfo;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * Displays information about a connector (capabilities, datatypes, connector parts and options).
 * This can easily be modified to use a different connector type, or a target instead of a source.
 * Note that setting the "file" connection part and option "header" are not necessary, they are only
 * done here so there is a value to display for the connection part, and so that the value of the
 * "header" option will be different than the default value.
 */
public class PrintConnectorInformationSample extends Sample{
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());
    /**
     * This sample shows how to display connector capabilities, datatypes, connection parts and options.
     */
    @Override
    protected boolean run() {
        try {
            ControlInterface ci = EngineFactory.instance().createSource(V10);
            ci.setConnectionTypeName("ASCII (Delimited)");
            ci.setConnectionPart("file", "$(SRC)/Accounts.txt");
            ci.setOption("header", "true");        // First record is a header
            // A session must be established to retrieve the proper datatypes, if the connector supports
            // user defined data types.  Calling connect() will implicitly start a session, but
            // startSession does not require a table (in the case of a database).
            ci.startSession();

            showConnectors(ci);
            showCapabilities(ci);
            showDatatypes(ci);
            showParts(ci);
            showOptions(ci);
        }
        catch (DesignSdkException e) {
            logger.severe("Error displaying capabilities, parts or options: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    private String yesOrNo(boolean b) {
        return b ? "Yes" : "No";
    }

    private void displaySection(String name) {
        System.out.println("\n---- " + name);
    }

    private void showConnectors(ControlInterface ci) throws DesignSdkException {
        displaySection("Connector Types");
        for (String type : ci.getConnections()) {
            if (type.contains("Delimited"))
                System.out.println("  " + type);
        }
    }

    private void showDatatypes(ControlInterface ci) throws DesignSdkException {
        displaySection("Datatypes");
        DatatypeInfo info = ci.getDatatypes();
        System.out.println("  Index  Type             Code  Alias            Generic");
        System.out.println("  _____  __________       ____  __________       __________");
        for (int dt = 0; dt < info.size(); ++dt) {
            info.setIndex(dt);
            System.out.printf("  %5d  %-15.15s  %4d  %-15.15s  %-15.15s%n",
                              dt, info.getTypeName(), info.getTypeCode(), info.getAlias(),
                              info.getGenericTypeName());
        }
    }

    /**
     * Displays some info about each connection part, including its current value.  The part name
     * (@see com.actian.di.designsdk.map.ConnectInfoEntry#getPartName) is the name used when setting/getting
     * the connection part value, while the prompt
     * (@see com.actian.di.designsdk.map.ConnectInfoEntry#getPrompt) contains the "display name" the
     * connector expects to be shown to the user.  The "name" property
     * (@see com.actian.di.designsdk.map.ConnectInfoEntry#getName) actually returns an enum referring to
     * which part of the connection info this entry refers to (i.e. the database name, server name, etc.)
     */
    private void showParts(ControlInterface ci) throws DesignSdkException {
        displaySection("Connection Parts");
        System.out.println("  Partname    Prompt                Name        Type        Value");
        System.out.println("  __________  __________            __________  __________  __________");
        for (ConnectInfoEntry info : ci.getConnectionEntries()) {
            System.out.printf("  %-10.10s  %-20.20s  %-10.10s  %-10.10s  %s%n",
                              info.getPartName(), info.getPrompt(), info.getName(),
                              info.getType(), ci.getConnectionPart(info.getPartName()));
        }
    }

    /**
     * Displays some info about each connection option, including its current value.
     */
    private void showOptions(ControlInterface ci) throws DesignSdkException {
        displaySection("Connector Options");
        System.out.println("  Name                            Alias                          Type         Default Value         Value");
        System.out.println("  __________                      __________                     __________   __________            __________");
        for (OptionInfo info : ci.getOptions()) {
            String value = info.isVisible() ? ci.getOption(info.getName()) : "---";
            value = escapeString(value);
                System.out.printf("  %-30.30s  %-30.30s  %-10.10s  %-20.20s  %s%n",
                                  info.getName(), info.getAlias(), info.getType().name(),
                                  escapeString(info.getDefaultValue()), value);
        }
    }
    /**
     * Display the capabilities of a connector.  Note the connector is specified as a ControlInterface, so
     * this method can handle a source, target or intermediate target.  Note this is not a complete list
     * of capabilities, see the javadocs for ConnectionCapabilities.
     * @param ci The connector (control) whose capabilities should be displayed
     */
    private void showCapabilities(ControlInterface ci) {
        ConnectionCapabilities caps = ci.getCapabilities();
        displaySection("Capabilities");
        if (caps.mayDefineExternalSchema())
            System.out.println("  May provide schema");
        else if (caps.mustDefineFields())
            System.out.println("  Must provide schema");
        else
            System.out.println("  Schema defined solely by connector");
        System.out.println("  Supports tables: " + yesOrNo(caps.supportsTables()));
        System.out.println("  User defined data types: " + yesOrNo(caps.supportsUserDefinedDataTypes()));
        System.out.print("  Multiple record types: ");
        if (caps.definesMultipleRecordTypes())
            System.out.println("Defines multiple record types");
        else
            System.out.println(caps.supportsMultipleRecordTypes() ? "Supported" : "Not Supported");
        System.out.println("  Hierarchical records: " + yesOrNo(caps.supportsHierarchicalRecords()));
        if (caps.supportsSql()) {
            System.out.println("  SQL Connector: Yes");
            System.out.println("  SQL Listerals: " + yesOrNo(caps.supportsSqlLiterals()));
        }
        System.out.println("  Multimode: " + yesOrNo(caps.supportsMultimode()));
    }
}
