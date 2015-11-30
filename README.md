# SVF-EclipsePlugin
This is an Eclipse Plugin for examining bugs reported by SVF-based detectors.

To confirm or fix a bug reported by detectors, one needs to first examine it. This process is usually difficult in large projects because a bug may involve multiple lines of code scattered in different files. To make this process easier, we implemented a tool called UAF-Marker for bug-detectors based on SVF. UAF-Marker, which is a simple Eclipse plugin, targets at use-after-free bugs. Specifically, it takes Eclipse as a code viewer and uses markers and colors to highlight the potential buggy code lines so that the user can clearly see the bugs.


To display the bugs found by detectors, UAF-Marker requires a file named UAF.txt as input.
The format of UAF.txt is as follows.

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
Each use-after-free coresponds to 10 lines in UAF.txt. The first 6 lines are the inforamtion about the use point, while the following 4 lines are the information about the associated free point.

Below is the usage of UAF-Marker.
1. Install UAF-Marker
* Copy uafmarker_1.0.0.201511251655.jar to your_eclipse_dir/plugins.
2. Use UAF-Marker
* Copy UAF.txt to the directory of the project under test.
* Launch your eclipse.
* In eclipse, new an eclipse project with existing code of the project under test.
* Right click the project name, in the menu, tick Configure/enable Sample Builder

Now you can see where the use-after-free bugs are in the problem panel of eclipse and track the bugs in the code view by clicking them.

