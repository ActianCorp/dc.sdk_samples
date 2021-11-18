/*
 *
 * Created on: 8/23/2021
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
import com.actian.di.designsdk.map.TargetRep;
import com.actian.di.designsdk.process.Step;
import com.actian.di.designsdk.process.steps.V10TransformationStep;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * A simple example showing how to create a process with a transformation step.
 */
public class ProcessWithTransformationSample extends Sample {
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());
    String PROCESSNAME = "ProcessWithTransformation";
    String MAPNAME = "InProcessAccountsExample";
    String TFILE = "$(TRG)/ProcessAccountsFixed.asc";
    @Override
    protected boolean run() {
        String logname = "$(LOG)/" + PROCESSNAME + ".log";
        try {
            ProcessRep process = EngineFactory.instance().createProcess(V10);
            process.getOptions().getLogSettings().setLogFile(logname);

            // Add the required start and stop steps
            Step startStep = process.addStep(Step.StepType.Start);
            Step stopStep = process.addStep(Step.StepType.Stop);

            // Create the transformation step, and add links
            Step tstep = createTransformationStep(createTransformation(), process);
            process.addLink(startStep.getName(), tstep.getName(), null);
            process.addLink(tstep.getName(), stopStep.getName(), null);

            // Save the process, and execute it
            process.save(createdArtifactPath(PROCESSNAME + ".process"));
            process.execute(null);
            process.close();
        }
        catch (DesignSdkException e) {
            logger.severe("Error running sample: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    protected Step createTransformationStep(String tfname, ProcessRep process) throws DesignSdkException {
        V10TransformationStep tstep = (V10TransformationStep) process.addStep(Step.StepType.Transformation);
        tstep.setName("Transformation_1");
        tstep.setConfigFile(tfname);
        return tstep;
    }

    // This method hides the work done to create the transformation artifacts we'll be using in this
    // sample.  All we do is load the supplied AccountsExample transformation, change the output file,
    // and save with a new name.
    protected String createTransformation() throws DesignSdkException {
        String mapname = suppliedArtifactPath("AccountsExample.map.rtc");
        MapRep map = EngineFactory.instance().createMap(V10);
        map.load(mapname);
        TargetRep trg = map.getTarget(map.getTargetNames().get(0));
        trg.setConnectionPart("file", TFILE);
        map.saveAs(createdArtifactPath(MAPNAME + ".map.rtc"), createdArtifactPath(MAPNAME + ".map"));
        String tfname = map.getTransformationFileName();
        map.close();
        return tfname;
    }
}
