package uafmarker.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.resources.IContainer;
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
			if (delta == null || usePointMap==null) { //usePointMap is null when starting, here forcing a full build of everything at startup
				fullBuild(monitor);						//not sure why startup wont automatically call here
			} else {
				incrementalBuild(delta, monitor);		//but calls here instead? lol
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

	private class contextPoint {
		private String fileName;
		private int lineNumber;
		private String functionName;
		
		public contextPoint(String fileName, int lineNumber, String functionName) {
			this.fileName = fileName;
			this.lineNumber = lineNumber;
			this.functionName = functionName;
		}
		
		public String getFileName() {
			return fileName;
		}
		public int getLineNumber() {
			return lineNumber;
		}
		public String getFunctionName() {
			return functionName;
		}
	}
	
	private class point {
		private int pointID;
		private String fileName;
		private int lineNumber;
		private String directory;
		private ArrayList<contextPoint> contextPoint;
		
		public point () {
			contextPoint = new ArrayList<contextPoint>();
		}
		
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
		public ArrayList<contextPoint> getContextPoint() {
			return contextPoint;
		}
	}
	
	private class UsePoint extends point {
		private String argPos;
		private FreePoint freePoint;
		
		public UsePoint() {
			super();
		}
		
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
		
		public FreePoint() {
			super();
		}
		
		public UsePoint getUsePoint() {
			return usePoint;
		}
		public void setUsePoint(UsePoint usePoint) {
			this.usePoint = usePoint;
		}
	}
	
	private Map<String, ArrayList<UsePoint>> usePointMap;
	private Map<String, ArrayList<FreePoint>> freePointMap;
	/**
	 * A map, with the file name as the key and the content of the file (as a string) as the value
	 */
	private Map<String, String> fileStreams;
	private boolean streamMapSet;
	
	//read the uaf txt
	private void readInputFromFile() throws IOException, CoreException { // Hua
		
		usePointMap = new HashMap<String, ArrayList<UsePoint>>();
		freePointMap = new HashMap<String, ArrayList<FreePoint>>();
		fileStreams = new HashMap<String, String>();
		streamMapSet = false;

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
				String thisFName = tmp[tmp.length -1]; //file name is last token
				usePoint.setFileName(thisFName);
								
				if(usePointMap.get(thisFName) == null)
					//if file is not in the map yet, add to map
					usePointMap.put(thisFName, new ArrayList<UsePoint>());
				usePointMap.get(thisFName).add(usePoint);
				
				if(!fileStreams.containsKey(thisFName)) {
					makeStream(this.getProject().getFile(thisFName));
				}
				
				//5: Directory
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", ""); //5 use point file directory
				usePoint.setDirectory(str);

				//6: Call Stack String
				//CXT : ==>sch(ln: 32)  ==> $$$
				String callString[] = inputStream.readLine().replaceFirst("CXT :", "").split("==>");
				for(int j = 0; j < callString.length; j++){
					if(j == 0) continue;
					
					callString[j] = callString[j].trim();
					
					String[] callStrArry = callString[j].split(" ");
					String functionName = callStrArry[0].replace("(ln:", "");
					String stackFileName = "";
					if (!functionExist(functionName, fileStreams.get(thisFName))) {
						if(!streamMapSet) {
							setupStreams(this.getProject());
							streamMapSet = true;
						}
						for(Map.Entry<String, String> entry: fileStreams.entrySet()) {
							if(!entry.getKey().equals(thisFName)) {
								if(functionExist(functionName, entry.getValue())) {
									stackFileName = entry.getKey();
									break;
								}
							}
						}
					} else {
						stackFileName = thisFName;
					}
										
					String line = callStrArry[callStrArry.length -1];
					int stackLineNumber = Integer.parseInt(line.replaceAll("\\D+",""));
					
					usePoint.getContextPoint().add(new contextPoint(stackFileName, stackLineNumber, functionName));
					
					if(callString[j+1].contains("$$$")) {
						break;
					}
				}

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
				thisFName = tmp[tmp.length -1];
				freePoint.setFileName(thisFName);
				
				if(freePointMap.get(thisFName) == null)
					freePointMap.put(thisFName, new ArrayList<FreePoint>());
				freePointMap.get(thisFName).add(freePoint);
				
				if(!fileStreams.containsKey(thisFName)) {
					makeStream(this.getProject().getFile(thisFName));
				}
				
				//12: Free point directory
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", "");//12 free point file directory
				freePoint.setDirectory(str);
				
				//13: free point call string
				// ## CXT : ==>sch(ln: 29) ==>f(ln: 5)  ==> $$$
				callString = inputStream.readLine().replaceFirst("CXT :", "").split("==>");
				for(int j = 0; j < callString.length; j++){
					if(j == 0) continue;
					
					callString[j] = callString[j].trim();
					
					String[] callStrArry = callString[j].split(" ");
					String functionName = callStrArry[0].replace("(ln:", "");
					String stackFileName = "";
					if (!functionExist(functionName, fileStreams.get(thisFName))) {
						if(!streamMapSet) {
							setupStreams(this.getProject());
							streamMapSet = true;
						}
						for(Map.Entry<String, String> entry: fileStreams.entrySet()) {
							if(!entry.getKey().equals(thisFName)) {
								if(functionExist(functionName, entry.getValue())) {
									stackFileName = entry.getKey();
									break;
								}
							}
						}
					} else {
						stackFileName = thisFName;
					}
										
					String line = callStrArry[callStrArry.length -1];
					int stackLineNumber = Integer.parseInt(line.replaceAll("\\D+",""));
					
					freePoint.getContextPoint().add(new contextPoint(stackFileName, stackLineNumber, functionName));
					
					if(callString[j+1].contains("$$$")) {
						break;
					}
				}

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
					
					for(int k = 0; k < i.getContextPoint().size(); k++) {
						int j = k+1;
						contextPoint c = i.getContextPoint().get(k);
						String functionName = c.getFunctionName();
						String stackFileName = c.getFileName();
						int lineNum = c.getLineNumber();
						
						if(stackFileName.equals("")) {
							mesg = mesg + j + ": "+ functionName + "(ln: " + lineNum + "), location not found";
						} else {
							mesg = mesg + j + ": "+ functionName + "(ln: " + lineNum + ") in file " + stackFileName;
						}
						if(k+1 == i.getContextPoint().size()) {
							mesg += " (here)" + System.getProperty("line.separator");
							break;
						}
						mesg = mesg + System.getProperty("line.separator");
						
						IFile stackFile = file;
						IDocument stackDoc = doc;
						if(!(stackFileName.equals("") || stackFileName.equals(file.getName()))) {
							stackFile = this.getProject().getFile(stackFileName); 
							IDocumentProvider tmpprovider = new TextFileDocumentProvider();
							tmpprovider.connect(stackFile);
							stackDoc = tmpprovider.getDocument(stackFile);
						}
						IMarker tmp = stackFile.createMarker(MARKER_TYPE_USE_STRING);
						String tmpMsg = "Use Point Call Stack " + j;
						if(stackFileName.equals("")) {
							tmpMsg += " (location not found)";
						}
						tmpMsg += System.getProperty("line.separator") + System.getProperty("line.separator") + "For the use point of issue " + IssueID + " at line " + lineNumber;
						tmp.setAttribute(IMarker.MESSAGE, tmpMsg);
						tmp.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						if(!stackFileName.equals("")) {
							tmp.setAttribute(IMarker.CHAR_START, stackDoc.getLineOffset(lineNum-1));
							tmp.setAttribute(IMarker.CHAR_END, stackDoc.getLineOffset(lineNum)-1);
							tmp.setAttribute(IMarker.LINE_NUMBER, lineNum-1);
						}
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
					
					for(int k = 0; k < i.getContextPoint().size(); k++) {
						int j = k+1;
						contextPoint c = i.getContextPoint().get(k);
						String functionName = c.getFunctionName();
						String stackFileName = c.getFileName();
						int lineNum = c.getLineNumber();
						
						if(stackFileName.equals("")) {
							mesg = mesg + j + ": "+ functionName + "(ln: " + lineNum + "), location not found";
						} else {
							mesg = mesg + j + ": "+ functionName + "(ln: " + lineNum + ") in file " + stackFileName;
						}
						if(k+1 == i.getContextPoint().size()) {
							mesg += " (here)" + System.getProperty("line.separator");
							break;
						}
						mesg = mesg + System.getProperty("line.separator");
						
						IFile stackFile = file;
						IDocument stackDoc = doc;
						if(!(stackFileName.equals("") || stackFileName.equals(file.getName()))) {
							stackFile = this.getProject().getFile(stackFileName); 
							IDocumentProvider tmpprovider = new TextFileDocumentProvider();
							tmpprovider.connect(stackFile);
							stackDoc = tmpprovider.getDocument(stackFile);
						}
						IMarker tmp = stackFile.createMarker(MARKER_TYPE_FREE_STRING);
						String tmpMsg = "Free Point Call Stack " + j;
						if(stackFileName.equals("")) {
							tmpMsg += " (location not found)";
						}
						tmpMsg += System.getProperty("line.separator") + System.getProperty("line.separator") + "For the free point of issue " + IssueID + " at line " + lineNumber;
						tmp.setAttribute(IMarker.MESSAGE, tmpMsg);
						tmp.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						if(!stackFileName.equals("")) {
							tmp.setAttribute(IMarker.CHAR_START, stackDoc.getLineOffset(lineNum-1));
							tmp.setAttribute(IMarker.CHAR_END, stackDoc.getLineOffset(lineNum)-1);
							tmp.setAttribute(IMarker.LINE_NUMBER, lineNum-1);
						}
						tmp.setAttribute("IssueID", IssueID);
						j++;
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
	
	/**
	 * This function checks a file stream (not really a stream, more like a string that contains the file content)
	 * and then returns true if the function definition is in the file
	 * 
	 * @param functionName
	 * @param fileStream the content of the file
	 * @return true if function exist in file, false otherwise
	 */
	boolean functionExist(String functionName, String fileStream) {
		Pattern pattern = Pattern.compile(""+functionName+"\\s*\\({1}[^\\)]*\\){1}\\s*\\{");
		return pattern.matcher(fileStream).find();
	}
	
	/**
	 * Sets up all the file streams that have yet to be set up in this project by calling setupStreams()
	 * on all the files in this project. Iterates through all folders too. 
	 * 
	 * @param container
	 */
	void setupStreams (IContainer container) {
		IResource[] members;
		try {
			members = container.members();
			for (IResource member : members)
			{
				if (member instanceof IContainer) 
				{
					setupStreams((IContainer)member);
				}
				else if (member instanceof IFile)
				{
					makeStream(member);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * For the file member, checks if it is a c or cpp file, then adds it's content to the 
	 * fileStreams map if it is not already in the map
	 * 
	 * @param member
	 */
	void makeStream(IResource member) {
		String fileEx = member.getFileExtension();
		if(fileEx==null) return;
		if(fileEx.equals("c") || fileEx.equals("cpp")) {
			String fileName = member.getName();
			if(!fileStreams.containsKey(fileName)) {
				Stream<String> stream;
				try {
					stream = Files.lines(Paths.get(member.getLocation().toString()));
					StringBuilder b = new StringBuilder();
					stream.forEach(b::append);
					String str = b.toString();
					fileStreams.put(fileName, str);
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}