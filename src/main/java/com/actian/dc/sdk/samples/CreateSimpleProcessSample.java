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
import com.actian.di.designsdk.ProcessRep;
import com.actian.di.designsdk.process.Step;
import com.actian.di.designsdk.process.steps.ScriptingStep;
import org.apache.commons.io.FileUtils;

import static com.actian.di.designsdk.ArtifactVersion.V10;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

/**
 * This sample will create a simple process that simply logs a message (similarly to the
 * supplied SimpleProcessSample).
 *
 * @author wbunton
 */
public class CreateSimpleProcessSample extends Sample {
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROCESSNAME = "SimpleProcess";
    private static final String MESSAGE = "This is logged from the created process sample";

    @Override
    protected boolean run() {
        String logname = "$(LOG)/CreateSimpleProcess.log";
        ProcessRep process = null;
        try {
            process = EngineFactory.instance().createProcess(V10);
            process.getOptions().getLogSettings().setLogFile(logname);

            // Add the required start and stop steps
            Step startStep = process.addStep(Step.StepType.Start);
            Step stopStep = process.addStep(Step.StepType.Stop);

            // Now create a script step, and set the script
            ScriptingStep scriptingStep = (ScriptingStep) process.addStep(Step.StepType.Scripting);
            scriptingStep.setName("Scripting");
            scriptingStep.setExpression("LogMessage(\"INFO\", \"" + MESSAGE + "\")");

            // Now create the step links.  None of the steps are decision steps, so the third argument
            // can always be null.
            process.addLink(startStep.getName(), scriptingStep.getName(), null);
            process.addLink(scriptingStep.getName(), stopStep.getName(), null);

            // Save the process, and execute it
            process.save(createdArtifactPath(PROCESSNAME + ".process"));
            process.execute(null);

            // Verify the process actually logged the message
            File lfile = new File(process.getSystemContext().expandMacros(logname));
            String log = FileUtils.readFileToString(lfile);
            if (!log.contains(MESSAGE)) {
                logger.severe("The log file did not contain the expected message");
                return false;
            }
        }
        catch (DesignSdkException e) {
            logger.severe("Error running sample: " + e.getLocalizedMessage());
            return false;
        }
        catch (IOException e) {
            logger.severe("Error reading log: " + e.getMessage());
            return false;
        }
        finally {
            if (process != null)
                process.close();
        }
        return true;
    }
}
