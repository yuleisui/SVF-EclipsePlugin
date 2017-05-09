# SVF-EclipsePlugin
This is an Eclipse Plugin for examining bugs reported by [SVF](https://github.com/unsw-corg/SVF/)-based detectors.

To confirm or fix a bug reported by detectors, one needs to first examine it. This process is usually difficult in large projects because a bug may involve multiple lines of code scattered in different files. To make this process easier, we implemented a tool called UAF-Marker for bug-detectors based on SVF. UAF-Marker, which is a simple Eclipse plugin, targets at use-after-free bugs. Specifically, it takes Eclipse as a code viewer and uses markers and colors to highlight the potential buggy code lines so that the user can clearly see the bugs.


To display the bugs found by detectors, UAF-Marker requires a file named UAF.txt as input.
The format of UAF.txt is as follows.
    
    Use-After-Free
    Use:				# tag
    1719				# line number of the use point
    main.c				# file of the use point
    /wget-1.16/src		# directory of the use point
    CALL=url_error;;RET_TO=main;;	# call string of the use point
    0 					# argument position of the use point (if the use is a call)
    Free:				# tag
    962					# line number of the free point
    url.c				# file of the free point
    wget-1.16/src	 	# directory of the free point
Each use-after-free corresponds to 11 lines in UAF.txt. The first line is the indication of the start of a use after free, the next 6 lines are the inforamtion about the use point, while the next following 4 lines are the information about the associated free point.

Below is the usage of UAF-Marker.
1. Create your project
* Launch Eclipse CPP
* Create a new project and add existing code/project that is being tested to the project
* Copy UAF.txt to the directory of the project
2. Install UAF-Marker
* Close Eclipse
* Copy from plugins uafmarker_1.0.0.201705100246.jar to your_eclipse_dir/dropins directory.
* In your project directory, open up the .project file and add the following:
    
    <buildSpec>
        <!-- Other, existing builders for your project -->
        <buildCommand>
            <name>uafmarker.sampleBuilder</name>
            <arguments>
            </arguments>
        </buildCommand>
    </buildSpec>
    <natures>
        <!-- Other, existing natures for your project -->
        <nature>uafmarker.sampleNature</nature>
    </natures>
3. Use UAF-Marker
* UAF Marker should work right away. If it doesn't work, refresh the project or force a build by changing a small part of the project and saving it. 
* If configured correctly, when you right click the project name, you should be able to disable/enable the Sample Builder under Configure

Now you can see where the use-after-free bugs are in the problem panel of eclipse and track the bugs in the code view by clicking them.

To recreate the project/edit the project:
1. Launch Eclipse RCP
2. Import the project into your workspace
3. To test your changes, run as an Eclipse application and create your projects with the nesscasary files in the Eclipse environment that apprears. You should be able to enable/disbale the plugin in the testing environment through the Configure option that is avalible when you right click the project name. 
4. To export your plugin, right click on your project name, go to export, as deployable plugin, and export. Install and use as specified above

