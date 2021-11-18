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
import com.actian.di.designsdk.process.steps.TransformationStep;

import static com.actian.di.designsdk.ArtifactVersion.V9;

/**
 * @author wbunton
 */
public class ProcessWithAlternateTransformationSample extends ProcessWithTransformationSample {

    public ProcessWithAlternateTransformationSample() {
        TFILE = "$(TRG)/AlternateAccountsFixed.asc";
        PROCESSNAME = "ProcessWithAltTransformation";
    }

    // Note we use the same run() as ProcessWithTransformationSample, but different transformation
    // creation and step configuration routines.

    @Override
    protected Step createTransformationStep(String tfname, ProcessRep process) throws DesignSdkException {
        // For a V10 process, Transformation returns a V10TransformationStep and AlternateTransformation
        // returns a TransformationStep.  In a V9 process, these are reversed.
        TransformationStep tstep =
                (TransformationStep) process.addStep(Step.StepType.AlternateTransformation);
        tstep.setName("Transformation_1");
        // Instead of supplying a configuration, we supply the map file.  We then set an option to
        // determine if the configuration is found by using MAPNAME.tf.xml, or if an "override"
        // configuration is used instead, which would be named processname$stepname.tf.xml
        tstep.setMapFile(tfname.replace("tf.xml", "map.xml"));
        tstep.setOverrideTransformationFile(false);
        return tstep;
    }

    // The only real difference is we start by creating a V9 MapRep, load from a tf.xml, and save to
    // tf.xml and map.xml.
    @Override
    protected String createTransformation() throws DesignSdkException {
        String mapname = suppliedArtifactPath("AccountsExample.tf.xml");
        MapRep map = EngineFactory.instance().createMap(V9);
        map.load(mapname);
        TargetRep trg = map.getTarget(map.getTargetNames().get(0));
        trg.setConnectionPart("file", TFILE);
        map.saveAs(createdArtifactPath(MAPNAME + ".tf.xml"), createdArtifactPath(MAPNAME + ".map.xml"));
        String tfname = map.getTransformationFileName();
        map.close();
        return tfname;
    }
}
