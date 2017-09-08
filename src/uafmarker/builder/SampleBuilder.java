package uafmarker.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class SampleBuilder extends IncrementalProjectBuilder {

	//called in incremental build
	class SampleDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				mark(resource);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				mark(resource); 
				break;
			}
			//return true to continue visiting children.
			return true;
		}
	}

	//called in full build
	class SampleResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			//checkXML(resource); 
			mark(resource);
			//return true to continue visiting children.
			return true;
		}
	}

	public static final String BUILDER_ID = "uafmarker.sampleBuilder";

	private static final String MARKER_TYPE_USE = "uafmarker.usePoint";
	
	private static final String MARKER_TYPE_FREE = "uafmarker.freePoint";
	
	private static final String MARKER_TYPE_USE_STRING = "uafmarker.usePointCallString";
	
	private static final String MARKER_TYPE_FREE_STRING = "uafmarker.freePointCallString";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	//build options
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		// delete markers set and files created
		getProject().deleteMarkers(MARKER_TYPE_USE, true, IResource.DEPTH_INFINITE);
		getProject().deleteMarkers(MARKER_TYPE_FREE, true, IResource.DEPTH_INFINITE);
		getProject().deleteMarkers(MARKER_TYPE_USE_STRING, true, IResource.DEPTH_INFINITE);
		getProject().deleteMarkers(MARKER_TYPE_FREE_STRING, true, IResource.DEPTH_INFINITE);
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE_USE, false, IResource.DEPTH_ZERO);
			file.deleteMarkers(MARKER_TYPE_FREE, false, IResource.DEPTH_ZERO);
			file.deleteMarkers(MARKER_TYPE_USE_STRING, false, IResource.DEPTH_ZERO);
			file.deleteMarkers(MARKER_TYPE_FREE_STRING, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {
			readInputFromFile();
			getProject().accept(new SampleResourceVisitor());
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new SampleDeltaVisitor());
	}
	
	//MARKER OBJECTS AND FUNCTIONS

	private class point {
		private int pointID;
		private String fileName;
		private int lineNumber;
		private String directory;
		private String context;
		
		public int getPointID() {
			return pointID;
		}
		public void setPointID(int pointID) {
			this.pointID = pointID;
		}
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		public int getLineNumber() {
			return lineNumber;
		}
		public void setLineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
		}
		public String getDirectory() {
			return directory;
		}
		public void setDirectory(String directory) {
			this.directory = directory;
		}
		public String getContext() {
			return context;
		}
		public void setContext(String context) {
			this.context = context;
		}
	}
	
	private class UsePoint extends point {
		private String argPos;
		private FreePoint freePoint;
		
		public String getArgPos() {
			return argPos;
		}
		public void setArgPos(String argPos) {
			this.argPos = argPos;
		}
		public FreePoint getFreePoint() {
			return freePoint;
		}
		public void setFreePoint(FreePoint freePoint) {
			this.freePoint = freePoint;
		}
	}
	
	private class FreePoint extends point {
		private UsePoint usePoint;
		
		public UsePoint getUsePoint() {
			return usePoint;
		}
		public void setUsePoint(UsePoint usePoint) {
			this.usePoint = usePoint;
		}
	}
	
	private Map<String, ArrayList<UsePoint>> usePointMap;
	private Map<String, ArrayList<FreePoint>> freePointMap;
	
	//read the uaf txt
	private void readInputFromFile() throws IOException, CoreException { // Hua
		
		usePointMap = new HashMap<String, ArrayList<UsePoint>>();
		freePointMap = new HashMap<String, ArrayList<FreePoint>>();

		//set up reader
		BufferedReader inputStream = null;
		try {
			//get file and open file stream
			IFile ifile = this.getProject().getFile("UAF.txt");
			String fileName = ifile.getLocation().toString();
			File file = new File(fileName);
			
			if(!file.exists())
				return;
			inputStream = new BufferedReader(new FileReader(file));
			
			String str;
			int issueID = 1;
			
			//for each line in UAF.txt (loop for each UAF instance)
			//1: ## Use_After_Free ## ....
			while ((str = inputStream.readLine()) != null) {
				//no warning issued
				if (str.equals("No warning issued.")) {
					break;
				}

				//no new Use After Free (white space checking)
				if(!str.contains("## Use_After_Free")) {
					continue; 
				}
				
				//-----------------------------------------------------------------------
				//USE POINT
				//-----------------------------------------------------------------------
				UsePoint usePoint = new UsePoint();
				usePoint.setPointID(issueID);
				
				//2: Use point header 
				//## Use....
				inputStream.readLine(); 
				
				//3: line number
				//line: 32
				Integer lineNum = Integer.valueOf(inputStream.readLine().replaceFirst("line: ", ""));
				usePoint.setLineNumber(lineNum);
				
				//4: file name
				//file: ./benchmark/useCorrelation/uc5.c
				String tmpString = inputStream.readLine().replaceFirst("file: ", "");
				String tmp[] = tmpString.split("/");
				str = tmp[tmp.length -1]; //file name is last token
				usePoint.setFileName(str);
								
				if(usePointMap.get(str) == null)
					//if file is not in the map yet, add to map
					usePointMap.put(str, new ArrayList<UsePoint>());
				usePointMap.get(str).add(usePoint);

				//5: Directory
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", ""); //5 use point file directory
				usePoint.setDirectory(str);

				//6: Call Stack String
				//CXT : ==>sch(ln: 32)  ==> $$$
				str = inputStream.readLine().replaceFirst("CXT :", "");
				usePoint.setContext(str);

				//7: Argument Position (no idea what this means tbh)
				//Arg Pos: -1
				str = inputStream.readLine().replaceFirst("Arg Pos: ", "");//argument position (if call or invoke, otherwise -1)
				usePoint.setArgPos(str);
				
				//headers
				inputStream.readLine();//8 ## Use....
				//-----------------------------------------------------------------------
				//END OF USE POINT
				//-----------------------------------------------------------------------
				
				//-----------------------------------------------------------------------
				//FREE POINT
				//-----------------------------------------------------------------------
				FreePoint freePoint = new FreePoint();
				freePoint.setPointID(issueID);
				
				inputStream.readLine();//9 ## Free....
				
				//10: Free point line number
				//line: 5
				lineNum = Integer.valueOf(inputStream.readLine().replaceFirst("line: ", ""));//10 free point line num
				freePoint.setLineNumber(lineNum);
				
				//11: Free point file name
				//file: ./benchmark/useCorrelation/uc5.c
				tmpString = inputStream.readLine().replaceFirst("file: ", "");
				tmp = tmpString.split("/");
				str = tmp[tmp.length -1];
				freePoint.setFileName(str);
				
				if(freePointMap.get(str) == null)
					freePointMap.put(str, new ArrayList<FreePoint>());
				freePointMap.get(str).add(freePoint);
				
				//12: Free point directory
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", "");//12 free point file directory
				freePoint.setDirectory(str);
				
				//13: free point call string
				// ## CXT : ==>sch(ln: 29) ==>f(ln: 5)  ==> $$$
				str = inputStream.readLine().replaceFirst("CXT :", "");
				freePoint.setContext(str);

				inputStream.readLine();//14 ## ## Free ############ }
				inputStream.readLine();//15 ## ## Use_After_Free ## 1 ## }
				//-----------------------------------------------------------------------
				//END OF FREE POINT
				//-----------------------------------------------------------------------
				
				usePoint.setFreePoint(freePoint);
				freePoint.setUsePoint(usePoint);
				issueID++;
			}
		}catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}	

	//use info to mark stuff
	void mark(IResource resource) {
		if(!(resource instanceof IFile)) return;
		
		//if the UAF file has not been read yet
		if(usePointMap == null) {
			try {
				readInputFromFile();
			} catch (IOException | CoreException e) {
				e.printStackTrace();
			} 
		}
		
		IFile file = (IFile) resource;
		deleteMarkers(file);

		String fileName = resource.getName();
		
		//use point marking
		if(resource instanceof IFile && usePointMap.containsKey(fileName)){
			try {
				IDocumentProvider provider = new TextFileDocumentProvider();
				provider.connect(file);
				IDocument doc = provider.getDocument(file);
				for(UsePoint i:usePointMap.get(fileName)){
					int IssueID = i.getPointID();
					int lineNumber = i.getLineNumber();
					FreePoint thisFreePoint = i.getFreePoint();
					
					String mesg = "Use Point"+ System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "Free Point at : " +  System.getProperty("line.separator");
					mesg = mesg + thisFreePoint.getFileName() + ", line number: " + thisFreePoint.getLineNumber() +System.getProperty("line.separator"); 
					mesg = mesg + "File directory: " + thisFreePoint.getDirectory() + System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "Argument Position (for callInst): " + i.getArgPos() + System.getProperty("line.separator");
					mesg = mesg + "Call Stack:" + System.getProperty("line.separator");
					
					String[] callString = i.getContext().split("==>");
					for(int j = 0; j < callString.length; j++){
						if(j == 0) continue;
						
						callString[j] = callString[j].trim();
						
						mesg = mesg + j + ": "+ callString[j];
						if(callString[j+1].contains("$$$")) {
							mesg = mesg + " (here)" + System.getProperty("line.separator");
							break;
						}
						mesg = mesg + System.getProperty("line.separator");
						
						String[] lineNumArry = callString[j].split(" ");
						String line = lineNumArry[lineNumArry.length -1];
						int lineNum = Integer.parseInt(line.replaceAll("\\D+",""));
						
						IMarker tmp = file.createMarker(MARKER_TYPE_USE_STRING);
						String tmpMsg = "Use Point Call Stack " + j + System.getProperty("line.separator");
						tmpMsg += "For the use point of issue " + IssueID + " at line " + lineNumber;
						tmp.setAttribute(IMarker.MESSAGE, tmpMsg);
						tmp.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						tmp.setAttribute(IMarker.CHAR_START, doc.getLineOffset(lineNum-1));
						tmp.setAttribute(IMarker.CHAR_END, doc.getLineOffset(lineNum)-1);
						tmp.setAttribute(IMarker.LINE_NUMBER, lineNum-1);
						tmp.setAttribute("IssueID", IssueID);
					}
					IMarker marker = file.createMarker(MARKER_TYPE_USE);
					marker.setAttribute(IMarker.MESSAGE, mesg);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.CHAR_START, doc.getLineOffset(lineNumber-1));
					marker.setAttribute(IMarker.CHAR_END, doc.getLineOffset(lineNumber)-1);
					marker.setAttribute(IMarker.LINE_NUMBER, lineNumber-1);
					marker.setAttribute("IssueID", IssueID);
				}
			} catch (CoreException | BadLocationException e) {
			}
		}
		
		//free point marking
		if(resource instanceof IFile && freePointMap.containsKey(fileName)){
			//deleteMarkers(file);
			try {
				IDocumentProvider provider = new TextFileDocumentProvider();
				provider.connect(file);
				IDocument doc = provider.getDocument(file);
				for(FreePoint i:freePointMap.get(fileName)){
					int IssueID = i.getPointID();
					int lineNumber = i.getLineNumber();
					UsePoint thisUsePoint = i.getUsePoint();

					String mesg = "Free Point"+ System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "Use Point at : " + System.getProperty("line.separator");
					mesg = mesg + thisUsePoint.getFileName() + ", line number: " + thisUsePoint.getLineNumber() +System.getProperty("line.separator"); 
					mesg = mesg + "File directory: " + thisUsePoint.getDirectory() + System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "Call Stack:" + System.getProperty("line.separator");
					
					String[] callString = i.getContext().split("==>");
					for(int j = 0; j < callString.length; j++){
						if(j == 0) continue;
						
						callString[j] = callString[j].trim();
						
						mesg = mesg + j + ": "+ callString[j];
						if(callString[j+1].contains("$$$")) {
							mesg = mesg + " (here)" + System.getProperty("line.separator");
							break;
						}
						mesg = mesg + System.getProperty("line.separator");
						
						String[] lineNumArry = callString[j].split(" ");
						String line = lineNumArry[lineNumArry.length -1];
						int lineNum = Integer.parseInt(line.replaceAll("\\D+",""));
						
						IMarker tmp = file.createMarker(MARKER_TYPE_FREE_STRING);
						String tmpMsg = "Free Point Call Stack " + j + System.getProperty("line.separator");
						tmpMsg += "For the free point of issue " + IssueID + " at line " + lineNumber;
						tmp.setAttribute(IMarker.MESSAGE, tmpMsg);
						tmp.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						tmp.setAttribute(IMarker.CHAR_START, doc.getLineOffset(lineNum-1));
						tmp.setAttribute(IMarker.CHAR_END, doc.getLineOffset(lineNum)-1);
						tmp.setAttribute(IMarker.LINE_NUMBER, lineNum-1);
						tmp.setAttribute("IssueID", IssueID);
					}
					IMarker marker = file.createMarker(MARKER_TYPE_FREE);
					marker.setAttribute(IMarker.MESSAGE, mesg);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.CHAR_START, doc.getLineOffset(lineNumber-1));
					marker.setAttribute(IMarker.CHAR_END, doc.getLineOffset(lineNumber)-1);
					marker.setAttribute(IMarker.LINE_NUMBER, lineNumber-1);
					marker.setAttribute("IssueID", IssueID);
				}
			} catch (CoreException | BadLocationException e) {
			}
		}
	}
}

//-------------------------------------------------------------------
//DEBUGGING CODE - USE TO OUTPUT DEBUGGING INFO TO A FILE
//-------------------------------------------------------------------
//FOR IF THE OUTPUT FILE ALREADY EXIST
//
//IFile logFile = this.getProject().getFile("pluginLog.txt");
//String logFileName = logFile.getLocation().toString();
//File logFileFile = new File(logFileName);
//if(!logFileFile.exists())
//	return;
//try {
//	FileWriter fw = new FileWriter(logFileFile, true);
//	BufferedWriter writer = new BufferedWriter(fw);
//	if(fileName == null) {
//		writer.write("Oh no file is null!"+ "\n");
//	} else if (toMark_UsePoint == null) {
//		writer.write("Ah ha!\n");
//	}else if (toMark_UsePoint.containsKey(fileName)==false) {
//		writer.write("Well theres your problem " + fileName + "\n");
//	}else {
//		writer.write("Now marking file " + fileName + "\n");
//	}
//	writer.close();
//} catch (FileNotFoundException e1) {
//	
//	e1.printStackTrace();
//} catch (IOException e) {
//	
//	e.printStackTrace();
//}
//
//FOR IF THE OUTPUT FILE IS YET TO EXIST
//
//IProject project = getProject();
//IFile logFile = project.getFile("pluginLog.txt");
//
//String contents = "Now marking " + fileName;
//InputStream source = new ByteArrayInputStream(contents.getBytes());
//try {
//	logFile.create(source, false, null);
//} catch (CoreException e1) {
//	
//	e1.printStackTrace();
//}