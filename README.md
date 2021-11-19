
# DataConnect SDK Samples
---

This project provides code samples that demonstrate how to use the DataConnect SDK. These samples are intended to be used in conjunction with the DataConnect SDK Developers Guide (included in PDF format as DataConnectSDKDeveloperGuide.pdf).

The DataConnect SDK is a direct interface to the DataConnect engine, and is used for both design and execution by the [DataConnect Studio IDE](https://www.actian.com/data-integration/dataconnect-integration/).

The samples in this package provide examples of using the SDK to design, load, edit, save and execute various artifacts.  They specifically demonstrate:

* Load and execute an existing transformation

---
## About the SDK



---
## Prerequisites 

Before running these samples, you will need:

1. Apache Maven 3.5.0 or higher [maven.apache.org](https://maven.apache.org/)
2. JDK version 11. Note: a JRE is not sufficient to build the samples 
3. Licensed DataConnect 12 installation with either the DataConnect Studio IDE or a standalone DataConnect Runtime Engine
   
Note: the sample code must be on the same machine as the DataConnect install.

---
## Preparing Your Environment

The SDK samples project builds and executes using Apache Maven.

1. Start a command shell
1. Ensure that the Apache Maven bin directory is in the path using the command **`mvn -v`** to print the maven version, Java version, and other relevant info
1. Add the **`runtime/di9`** directory relative to the DataConnect engine installation to your **`PATH`** environment variable
1. Set the **`DJLIB`** environment variable to the location of the directory that contains your **`cosmos.ini`** file.  This will typically be in
**`\ProgramData\Actian\DataConnect\dc-rcp-64-bit-<version>`** on Windows
1. From the command shell, run `djengine -?` to verify a valid license file and to see command line Help

Note: for more information about installing and configuring the DataConnect Runtime Engine, see the Help topic of the same name in the [DataConnect User Guide on-line documentation](http://docs.actian.com/).

---
## Building And Running the Samples

After you have completed the setup above, open a command shell and change directory to the root of the samples.

To build and execute all samples, run the following command: **`mvn verify`**
	
To build and execute a single sample, specify the name of the sample: **`mvn verify -Dsample.to.run=LoadAndExecuteSample`**

---
## Verifying Sample Results

* The console will display the integration project name, FINISHED_OK, and BUILD SUCCESSFUL messages
* Target/output files are created under the folder  **`target/runtime/data`** 
* Sample-specific log files are created under **`target/runtime/logs`** 
* Artifacts are saved under **`target/runtime/artifacts`**

---
## Key Concepts

See the developer guide for complete discussion of the samples

---
## Project Structure
```
README.TXT:  This file
pom.xml:  Maven build script
src/main/artifacts:
  AccountsExample.map: Simple transformation used to demonstrate loading and running a transformation 
  AccountsExample.map.rtc:  Runtime configuration used to configure and run AccountsExample.map
  AccountsExample.map.xml: Simple V9 transformation used to demonstrate loading and running a transformation 
  AccountsExample.tf.xml:  Runtime configuration used to configure and run AccountsExample.map
  SimpleProcessSample.process: A simple process used to demonstrate loading and running a process
  SimpleProcessSample.process.rtc: Runtime configuration used to configure and run SimpleProcessSample.process
src/main/assemblies:
  stage-artifacts-and-data.xml:  Maven assembly used to stage sample artifacts and data for execution
src/main/data:
  invoices_src.txt:  Source data used by sample artifacts
  Accounts.txt:  Source data used by the sample artifacts
  File1.txt:  Source data used by the sample artifacts
  File2.txt:  Source data used by the sample artifacts
  File3.txt:  Source data used by the sample artifacts
  StateCodeStateData.txt:  Source data used by the sample artifacts
  macrodef.json: Macrodefs file used by the samples
src/main/java/com/actian/dc/sdk/samples:
  SamplesRunner.java:  Main class used to execute all of the samples
  Sample.java: Base class for all samples
  LogUtil.java:  Utility class used to implement logging for the samples
  LoadAndExecuteSample.java: Simple example of loading and executing a transformation and a process
  PrintConnectorInformationSample.java: Example of displaying information about a connector
  CreateSimpleMapSample.java: Creates, saves and executes a simple transformation
  MapWithEventsAndRejectsSample.java: Creates, saves and executes a more complex transformation (multiple targets, and none-default events/actions)
  IntermediateTargetAndSchemasSample.java: Creates an extenral schema, then uses it in a complex transformation using a lookup intermediate target
  SchemaBuilder.java: Class used by IntermediateTargetAndSchemasSample to create the external schema
  WorkingWithDatatypesSample.java: Demonstrates how to manipulate datatypes
  CreateSimpleProcessSample.java: Creates, saves and executes a simple process
  ProcessWithTransformationSample.java: Creates, saves and executes a simple process that runs a transformation in a transformation step
  ProcessWithAlternateTransformationSample.java: Creates, saves and executes a simple process that runs a V9 transformation in an alternate transformation step
  ProcessWithComponentsSample.java: Creates, saves and executes a complex process that uses a queue component to read files and an aggregator component to write those files to a zip file.
```
---
## Support

Free support is available for registered users of the [Actian Community](https://communities.actian.com/s/?_ga=2.42990309.1976004892.1553019528-1750363259.1548967681)  
Paid plans are also available at [Actian Support Services](https://www.actian.com/support-services/)



---
## Contributing

Actian Corporation welcomes contributions to the DataConnect SDK Samples project.  To contribute, please follow these steps:

* When submitting your pull request, please provide full contact info (Name, company, email, phone)
* Submit your pull request to the dev branch. We will review and test the requested change.  
* Once approved, we will perform the merge to dev.  Your change will be available immediately after our next merge to master.
