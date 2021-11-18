/*
 *
 * Created on: 7/22/2021
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
import com.actian.di.designsdk.ProcessRep;
import com.actian.di.designsdk.map.ConversionEventListener;
import com.actian.di.designsdk.process.ProcessEventListener;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * Load and execute a transformation and process.
 */
public class LoadAndExecuteSample extends Sample {
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * This demonstrates how simple it is to load and run existing transformations and processes.
     * @return True if ran successfully (and output as expected), false otherwise
     * @see Sample#run()
     */
    @Override
    public boolean run() {
        return runTransformation() && runProcess();
    }

    private boolean runTransformation() {
        MapRep map = null;
        MapRep dup = null;
        String mapname = suppliedArtifactPath("AccountsExample.map.rtc");
        try {
            map = EngineFactory.instance().createMap(V10);
            if (!map.load(mapname)) {
                logger.severe("There was a problem loading the transformation");
                return false;
            }
            logger.info("Running " + mapname);
            dup = map.duplicate();
            dup.execute(new ConversionEventListener() {
                @Override
                public boolean progress(long recordNumber, long estCount, long readCount, long writeCount) {
                    logger.info("Progress: Record " + recordNumber + " of (estimated) " + estCount + ", " + readCount + " read, " + writeCount + " written");
                    return true;
                }

                @Override
                public boolean onError(long recordNumber, int errorCode, String message) {
                    logger.severe("Error " + errorCode + " on record " + recordNumber + ": " + message);
                    return false;
                }

                @Override
                public boolean sortProgress(long l, long l1) {
                    return true;
                }

                @Override
                public boolean validationProgress(int i, int i1) {
                    return true;
                }

                @Override
                public boolean onValidationError(int i, int i1, String s) {
                    return true;
                }
            });
        }
        catch (DesignSdkException e) {
            logger.severe("Error executing " + mapname + ": " + e.getLocalizedMessage());
            return false;
        }
        finally {
            if (map != null)
                map.close();
            if (dup != null)
                dup.close();
        }
        return true;
    }

    private boolean runProcess() {
        ProcessRep process = null;
        String processname = suppliedArtifactPath("SimpleProcessSample.process.rtc");
        try {
            process = EngineFactory.instance().createProcess(V10);
            if (!process.load(processname)) {
                logger.severe("There was a problem loading the process");
                return false;
            }
            process.setForceUnloadMapsAndProcesses(true);
            logger.info("Running " + processname);
            process.execute(new ProcessEventListener() {
                @Override
                public boolean progress(String step, String item, long recordCount, int percentComplete) {
                    if (!step.isEmpty())
                        logger.info("Progress: " + step + ", " + item + ": " + recordCount
                                           + " records, " + percentComplete + "% complete");
                    return true;
                }

                @Override
                public boolean result(String item, String resultStatus, long returnCode, String lastMessage,
                                      long errorCount, long rejectCount, long outputCount) {
                    logger.info("Result: step " + item + " status " + resultStatus);
                    return true;
                }

                @Override
                public boolean onError(String s, long l, String s1) {
                    return false;
                }

                @Override
                public boolean status(String s, String s1) {
                    return true;
                }

                @Override
                public boolean transformationProgress(String s, long l, long l1, long l2) {
                    return true;
                }

                @Override
                public boolean onTransformationError(String s, long l, long l1, String s1) {
                    return true;
                }

                @Override
                public int pauseStatus(String s, HashMap<String, String> hashMap) {
                    return 0;
                }
            });
        }
        catch (DesignSdkException e) {
            logger.severe("Error executing " + processname + ": " + e.getLocalizedMessage());
            return false;
        }
        finally {
            if (process != null)
                process.close();
        }
        return true;
    }
}
