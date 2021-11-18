/*
 *
 * Created on: 8/24/2021
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
import com.actian.di.designsdk.UserVariable;
import com.actian.di.designsdk.process.Component;
import com.actian.di.designsdk.process.Step;
import com.actian.di.designsdk.process.VariablesContainer;
import com.actian.di.designsdk.process.steps.ActionType;
import com.actian.di.designsdk.process.steps.AggregatorStep;
import com.actian.di.designsdk.process.steps.DecisionStep;
import com.actian.di.designsdk.process.steps.QueueStep;
import com.actian.di.designsdk.process.steps.ScriptingStep;
import com.actian.di.designsdk.process.steps.StartStep;
import com.actian.di.designsdk.process.steps.StopStep;

import java.lang.invoke.MethodHandles;
import java.util.logging.Logger;

import static com.actian.di.designsdk.ArtifactVersion.V10;

/**
 * This sample demonstrates how to work with components and decision steps.  It will use a File Folder
 * Queue component to read a select set of files from a directory, and will use a Zip Aggregator to
 * put them all in a zip file.
 */
public class ProcessWithComponentsSample extends Sample {
    private static final Logger logger = LogUtil.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROCESSNAME = "ProcessWithComponents";

    @Override
    protected boolean run() {
        try {
            // First, we create and configure the process
            ProcessRep process = configureProcess();
            // Create the components.  Configure the ffqueue, but the zip aggregator doesn't need
            // configuration.  We could do this in configureProcess, but we'll need access to the
            // components when configuring the steps that use them.
            Component ffqueueComponent = configureFFQueue(process);
            Component zipComponent = process.addComponent("Zip", Component.ComponentCategory.AGGREGATOR,
                                                          "Zip Aggregator", "1.0.0");
            // Now configure the steps.  We'll do start and stop separately as well, since they will
            // both have EZscript expressions.
            Step startStep = configureStartStep(process);
            Step stopStep = configureStopStep(process);
            Step queueStep = configureQueueStep(process, ffqueueComponent);
            Step decisionStep = configureDecisionStep(process, queueStep);
            Step scriptStep = configureScriptStep(process);
            Step zipPutStep = configureZipPutStep(process, zipComponent);
            Step zipGetStep = configureZipGetStep(process, zipComponent);

            // Link the steps
            process.addLink(startStep.getName(), queueStep.getName(), null);
            process.addLink(queueStep.getName(), decisionStep.getName(), null);
            process.addLink(scriptStep.getName(), zipPutStep.getName(), null);
            process.addLink(zipPutStep.getName(), queueStep.getName(), null);
            // Links from the decision step need to indicate whether they are for the true
            // branch or false branch
            process.addLink(decisionStep.getName(), scriptStep.getName(), true);
            process.addLink(decisionStep.getName(), zipGetStep.getName(), false);
            process.addLink(zipGetStep.getName(), stopStep.getName(), null);

            // Save the process, and execute it
            process.save(createdArtifactPath(PROCESSNAME + ".process"));
            process.execute(null);
            process.close();
        }
        catch (DesignSdkException e) {
            e.printStackTrace();
            logger.severe("Error running sample " + this.getClass().getSimpleName() + ": "
                          + e.getLocalizedMessage());
            return false;

        }
        return true;
    }

    private AggregatorStep configureZipGetStep(ProcessRep process, Component zip) throws DesignSdkException {
        AggregatorStep step = (AggregatorStep) process.addStep(Step.StepType.Aggregator);
        step.setName("Write Zip to Message");
        step.setComponent(zip);
        // The Zip Aggregator uses the Put action to add entries to the zip.  The Get action then
        // closes the zip and writes its contents to the supplied message.  Remember we will actually
        // write the message contents to the zip file in the filesystem in the Stop step expression.
        step.setActionType(ActionType.GetMessage);
        step.getAction().setParameterValue("Message", "ZipMessage");
        // The zip aggregator by default encrypts the zip.  We want to turn that off.
        step.setOption("EncryptionMethod", "NONE");
        step.getPresentationInfo().setX(564);
        step.getPresentationInfo().setY(288);
        return step;
    }

    private DecisionStep configureDecisionStep(ProcessRep process, Step keyStep) throws DesignSdkException {
        DecisionStep step = (DecisionStep) process.addStep(Step.StepType.Decision);
        step.setName("Got a File?");
        // Here the decision is very easy, we just want to check the return code for the queue
        // get step.  We could have also handled the "fix message properties" step in this expression
        // and eliminated that step.
        step.setExpression("Project(\"" + keyStep.getName() + "\").ReturnCode == 0");
        step.getPresentationInfo().setX(420);
        step.getPresentationInfo().setY(288);
        return step;
    }

    private AggregatorStep configureZipPutStep(ProcessRep process, Component zip) throws DesignSdkException {
        AggregatorStep step = (AggregatorStep) process.addStep(Step.StepType.Aggregator);
        step.setName("Write File to Zip");
        // Set component, action type, and configure the parameter.  The PutMessage action on the Zip
        // Aggregator adds a file entry to the zip file.  It can come from a message (as we're configuring
        // here), or by specifying the file name option.
        step.setComponent(zip);
        step.setActionType(ActionType.PutMessage);
        step.getAction().setParameterValue("Message", "SrcMessage");
        // We will put the files we are adding under the (relative) directory "test"
        step.setOption("TargetFolder", "test");
        step.getPresentationInfo().setX(210);
        step.getPresentationInfo().setY(120);
        return step;
    }

    private Step configureScriptStep(ProcessRep process) throws DesignSdkException {
        ScriptingStep step = (ScriptingStep) process.addStep(Step.StepType.Scripting);
        step.setName("Fix Message Properties");
        // The old ffqueue component sets a couple message properties regarding the source file name.
        // The new zip aggregator requires a file name, but the property name is different.  So we
        // want to use the source file info to create the zip entry name.
        step.setExpression("SrcMessage.Properties(\"FileName\")"
                           + " = SrcMessage.Properties(\"DJFT SourceName\") & \".txt\"");
        step.getPresentationInfo().setX(386);
        step.getPresentationInfo().setY(120);
        return step;
    }

    private QueueStep configureQueueStep(ProcessRep process, Component queue) throws DesignSdkException {
        // Create the step, and let it know the queue component it will be using
        QueueStep step = (QueueStep) process.addStep(Step.StepType.Queue);
        step.setName("Read File");
        step.setComponent(queue);
        // Configure the action.  We need to set the action type, then configure the single (in
        // this case) parameter.
        step.setActionType(ActionType.GetMessage);
        step.getAction().setParameterValue("Message", "SrcMessage");
        //   To see the available parameters:
//        ComponentAction action = step.getAction();
//        for (int i = 0; i < action.getParameterCount(); ++i) {
//            ComponentActionParameterInfo info = action.getParameterInfo(i);
//            System.out.println("Parameter info:" + info.getName() + " (" + info.getPrompt() + ") has type "
//                               + info.getType());
//        }

        // Since this is a queue, we don't want the error that comes with EOF to terminate the
        // process.
        step.setIgnoreErrors(true);
        step.getPresentationInfo().setX(228);
        step.getPresentationInfo().setY(288);
        return step;
    }

    private Step configureStopStep(ProcessRep process) throws DesignSdkException {
        StopStep step = (StopStep) process.addStep(Step.StepType.Stop);
        // Write the message body containing the zip file out.  Note we specify ISO8859-1 as
        // the encoding, to ensure it remains binary.
        step.setExpression("FileWrite(\"$(TRG)/test.zip\", ZipMessage.Body, ENC_ISO8859_1)");
        step.getPresentationInfo().setX(600);
        step.getPresentationInfo().setY(145);
        return step;
    }

    private Step configureStartStep(ProcessRep process) throws DesignSdkException {
        StartStep step = (StartStep) process.addStep(Step.StepType.Start);
        // Use the start step expression to initialize the two global message variables.  Best
        // practice when dealing with message variables is to try findMessage, and if that
        // doesn't return a message then create a new one.  This way. the process can be easily
        // used as a subprocess, or messages can be managed before the process executes.
        step.setExpression("set SrcMessage = findMessage(\"SrcMessage\")\r\n"
                           + "if SrcMessage is nothing then\r\n"
                           + "\tset SrcMessage = new DJMessage(\"SrcMessage\")\r\n"
                           + "end if\r\n"
                           + "set ZipMessage = findMessage(\"ZipMessage\")\r\n"
                           + "if ZipMessage is nothing then\r\n"
                           + "\tset ZipMessage = new DJMessage(\"ZipMessage\")\r\n"
                           + "end if\r\n");
        step.getPresentationInfo().setX(77);
        step.getPresentationInfo().setY(145);
        return step;
    }

    private Component configureFFQueue(ProcessRep process) throws DesignSdkException {
        Component qc = process.addComponent("FFQueue", Component.ComponentCategory.MESSAGE_QUEUE,
                                            "File Folder Queue", "1.0.0");
        qc.setOption("Directory", "$(SRC)");
        qc.setOption("GetMsgClass", "asc");
        qc.setOption("BrowseMode", "True");
        return qc;
    }

    private ProcessRep configureProcess() throws DesignSdkException {
        ProcessRep process = EngineFactory.instance().createProcess(V10);
        String logname = "$(LOG)/" + PROCESSNAME + ".log";
        process.getOptions().getLogSettings().setLogFile(logname);
        process.getOptions().setEnableLogging(true);
        // Normally, we like to break when an error is encountered.  In this case, however, the step
        // that reads file generates an error when EOF is hit.  We want to keep processing, so we need
        // to turn off the break on first error property.  By default, steps that error will still
        // terminate the process, we will also need to turn off a step property.
        process.getOptions().setBreakOnFirstError(false);

        // Now add variables to access the two messages we'll use, one for the files we read and
        // one for the body of the zip file.
        VariablesContainer vars = process.getGlobalVariables();
        UserVariable srcMsg = vars.add("SrcMessage");
        srcMsg.setDatatype(UserVariable.Datatype.DJMessage);
        UserVariable zipMsg = vars.add("ZipMessage");
        zipMsg.setDatatype(UserVariable.Datatype.DJMessage);

        return process;
    }
}
