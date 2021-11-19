/*
 * Copyright 2021 Actian Corporation
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
import com.actian.di.designsdk.SystemContext;
import com.actian.di.designsdk.SystemContextAccess;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

public class SamplesRunner {
    static final String macrosFile = "target/runtime/data/macrodef.json";

    // Where to find the pre-defined artifacts included with the samples
    static final String artifactsPath = new File("target/runtime/artifacts").getAbsolutePath();

    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<String, String> directoryMacros = new HashMap<>();

    // Returns the value of the named directory macro, or null if it isn't a standard directory macro
    @SuppressWarnings("SameParameterValue")
    static String directoryFor(String macro) {
        return directoryMacros.get(macro);
    }

    public static void main(String[] args) throws Exception {
        // Must set a license key before trying to execute a process or transformation.  Any other
        // operation is allowed.  This actual call is not stricly necessary, as by default the
        // key "ENG" is used.
        EngineFactory.instance().setLicenseKey("ENG");
        // Load our macrodefs file.  A null list of macrosets means include them all.
        EngineFactory.instance().setMacrodefs(macrosFile, null);
        Objects.requireNonNull(getSystemContext(null)).getMacroConfiguration().addMacro("TEST_BASE",
                                                                                        System.getProperty(
                                                                                                "user.dir")
                                                                                        + "/");
        prepareOutputDirectories();

        List<Sample> samples = buildSamplesList(args);
        for (Sample sample : samples) {
            String sampleName = sample.getClass().getSimpleName();
            logger.info("Starting " + sampleName);
            boolean ok = sample.run();
            String status = ok ? "OK" : "ERROR";
            logger.info(sampleName + " finished " + status + "\n");
            if (!ok)
                break;
        }
    }

    /**
     * Build the list of samples to be run
     *
     * @param args Command line args
     * @return list of samples to run
     * @throws Exception if any error occurs
     */
    private static List<Sample> buildSamplesList(String[] args) throws Exception {
        List<Sample> samples = new ArrayList<>();
        if (args != null && args.length > 0 && args[0].trim().length() > 0) {
            Class<?> clazz;
            String sampleClassToRun = args[0].trim();
            try {
                clazz = Class.forName(sampleClassToRun);
            }
            catch (ClassNotFoundException ex) {
                // didn't find the class using the name provided.  Maybe the
                // package was left off.  Check to see if any dots are in the
                // name.  If not, add the current package and try again.
                if (!sampleClassToRun.contains(".")) {
                    String pkgName = SamplesRunner.class.getPackage().getName();
                    clazz = Class.forName(pkgName + "." + sampleClassToRun);
                }
                else
                    throw ex;
            }
            // Add only the provided sample for execution
            // Assumes instance of ConnectionUser and null public constructor
            samples.add((Sample) clazz.getDeclaredConstructor().newInstance());
        }
        else {
            // Queue all samples for execution
            samples.add(new LoadAndExecuteSample());
            samples.add(new PrintConnectorInformationSample());
            samples.add(new CreateSimpleMapSample());
            samples.add(new MapWithEventsAndRejectsSample());
            samples.add(new WorkingWithDatatypesSample());
            samples.add(new IntermediateTargetAndSchemasSample());
            samples.add(new CreateSimpleProcessSample());
            samples.add(new ProcessWithTransformationSample());
            samples.add(new ProcessWithAlternateTransformationSample());
            samples.add(new ProcessWithComponentsSample());
        }
        return samples;
    }

    static void prepareOutputDirectories() throws IOException {
        SystemContext systemContext = getSystemContext(null);
        assert systemContext != null;
        for (String mac : new String[]{"TRG", "LOG", "ARTIFACTS"}) {
            String dir = systemContext.expandMacros("$(" + mac + ")");
            directoryMacros.put(mac, dir);
            if (!dir.isEmpty() && !dir.equals(".") && !dir.equals("..")) {
                File fdir = new File(dir);
                if (fdir.exists()) {
                    FileUtils.cleanDirectory(fdir);
                    FileUtils.deleteDirectory(fdir);
                }
                FileUtils.forceMkdir(fdir);
            }
        }
    }

    private static SystemContext globalSystemContext = null;

    @SuppressWarnings("SameParameterValue")
    static SystemContext getSystemContext(SystemContextAccess sca) {
        if (sca == null) {
            if (globalSystemContext == null) {
                try {
                    // Sneaky way to get a generic system context.  This will contain the macros container
                    // owned by the EngineFactory system context.
                    MapRep map = EngineFactory.instance().createMap(V10);
                    globalSystemContext = map.getSystemContext();
                    map.close();
                }
                catch (DesignSdkException e) {
                    logger.severe("Unable to retrieve global context: " + e.getLocalizedMessage());
                    return null;
                }
            }
            return globalSystemContext;
        }
        else
            return sca.getSystemContext();
    }
}
