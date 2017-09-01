# SVF-EclipsePlugin

This is an Eclipse Plugin for examining bugs reported by [SVF](https://github.com/unsw-corg/SVF/)-based detectors.

To confirm or fix a bug reported by detectors, one needs to first examine it. This process is usually difficult in large projects because a bug may involve multiple lines of code scattered in different files. To make this process easier, we implemented a tool called UAF-Marker for bug-detectors based on SVF. UAF-Marker, which is a simple Eclipse plugin, targets at use-after-free bugs. Specifically, it takes Eclipse as a code viewer and uses markers and colors to highlight the potential buggy code lines so that the user can clearly see the bugs.

## Installation
Copy from the plugins folder the jar file uafmarker_1.0.0.<version_number>.jar to your *eclipseCPP_installation_dir/dropins* directory.

## Requirement
To display the bugs found by detectors, UAF-Marker requires a file named UAF.txt as input for the plugin, placed in the root of the project directory (see **Usage** for more detailed instructions). The format of UAF.txt is as output by the SVF tool, as follows.
```
## Use_After_Free ## 1 ## {
## Use  ############ {                          # tag
line: 10                                        # line number of the use point
file: ./benchmark/array/array1.c                # file of the use point
dir : /home/stc/stc/test/testMicroBenchmark     # directory of the use point
CXT : ==>main(ln: 10)  ==> $$$                  # call string of the use point
Arg Pos: -1                                     # argument position of the use point (if the use is a call)
## Use  ############ }
## Free ############ {                          # tag
line: 9                                         # line number of the free point
file: ./benchmark/array/array1.c                # file of the free point
dir : /home/stc/stc/test/testMicroBenchmark     # directory of the free point
CXT : ==>main(ln: 9)  ==> $$$
## Free ############ }
## Use_After_Free ## 1 ## }
```
Each use-after-free corresponds to 15 lines in UAF.txt. The first line is the tag of the start of a use after free, the next 7 lines are the inforamtion about the use point, after which the next following 6 lines are the information about the associated free point, followed by an ending tag to mark the end of that use after free. 

## Usage
1. Create your C/C++ project
* Launch Eclipse CPP
* Create a new project and add existing code/project that is being tested to the project
* Copy UAF.txt to the directory of the project
* To activate the plugin, right click on the project name, and activate/deactive the plugin using the disable/enable Sample Builder option under the Configure option, as shown below. 

2. Use UAF-Marker
* UAF Marker should work right away. If it doesn't work, refresh the project or force a build by changing a small part of the project and saving it. 
* Now you can see where the use-after-free bugs are in the problem panel of eclipse and track the bugs in the code view by clicking them.

## Troubleshooting
If right clicking on the project name does not show the disable/enable the Sample Builder option under Configure, you can manually configure your project by going to your project directory and editing the .project file to add the following:

```
<buildSpec>
    <!-- Other, existing builders for your project -->
    <!-- Add this -->
    <buildCommand>
        <name>uafmarker.sampleBuilder</name>
        <arguments>
        </arguments>
    </buildCommand>
</buildSpec>
<natures>
    <!-- Other, existing natures for your project -->
    <!-- Add this -->
    <nature>uafmarker.sampleNature</nature>
</natures>
```
If the above does not work, check the project properties by right clicking on the project name. The Sample Builder should be listed under the Builders option. If it is not there or if it says "Missing Builder", the plugin may not be configured correctly. Either check the configuration or re-export the plugin from this project using your own machine. 

To recreate the project/edit the project:
1. Download and launch Eclipse RCP
2. Import the project from this repo into your workspace
3. To test your changes, run as an Eclipse application and create your projects with the necessary files in the Eclipse environment that appears. You should be able to enable/disable the plugin in the testing environment through the Configure option that is available when you right click the project name. 
4. To export your plugin, right click on your project name, go to export, as deployable plugin, and export. Install and use as specified above
