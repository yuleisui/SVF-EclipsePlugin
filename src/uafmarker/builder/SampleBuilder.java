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

	//MARKER LOGIC

	private Map<String, ArrayList<Integer>> toMark_UsePoint; //filename 2 usepoint list
	private Map<String, ArrayList<Integer>> toMark_FreePoint;//filename 2 freepoint list
	private Map<String, String> use2free; //use 2 free
	private Map<String, String> use2callString; //use 2 call string
	private Map<String, String> use2argPos; //use 2 argument position
	private Map<String, String> free2use; //free2use
	private Map<String, String> free2callString; //free 2 call string
	private Map<String, Integer> use2id;
	private Map<String, Integer> free2id;

	//read the uaf txt
	private void readInputFromFile() throws IOException, CoreException { // Hua
		//set up maps
		toMark_UsePoint = new HashMap<String, ArrayList<Integer> >();
		toMark_FreePoint = new HashMap<String, ArrayList<Integer> >();
		use2free = new HashMap<String, String>();
		use2callString = new HashMap<String, String>();
		use2argPos = new HashMap<String, String>();
		free2use = new HashMap<String, String>();
		free2callString = new HashMap<String, String>();
		use2id = new HashMap<String, Integer>();
		free2id = new HashMap<String, Integer>();

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
			//read the first line here
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

				//2: Use point header 
				//## Use....
				inputStream.readLine(); 
				
				//3: line number
				//line: 32
				Integer lineNum = Integer.valueOf(inputStream.readLine().replaceFirst("line: ", ""));
				
				//4: file name
				//file: ./benchmark/useCorrelation/uc5.c
				String tmpString = inputStream.readLine().replaceFirst("file: ", "");
				String tmp[] = tmpString.split("/");
				str = tmp[tmp.length -1]; //file name is last token
				
				if(toMark_UsePoint.get(str) == null)
					//if file is not in the map yet, add to map
					toMark_UsePoint.put(str, new ArrayList<Integer>());

				// the value with the file name as the key is an array with the lines at which there are use points
				toMark_UsePoint.get(str).add(lineNum);  

				//unique identifier for this use point (eg: 32 : uc5.c)
				String usePoint = "" + lineNum + " : " + str; //str here is the file name 
				
				String usePointInfo = str + ", line nuber: " + lineNum;
				
				//6: Directory
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", ""); //5 use point file directory
				
				usePointInfo = System.getProperty("line.separator") + usePointInfo + System.getProperty("line.separator")+"File Directory: " + str;

				//7: Call Stack String
				//CXT : ==>sch(ln: 32)  ==> $$$
				String callString = inputStream.readLine().replaceFirst("CXT :", "");

				use2callString.put(usePoint,  callString);

				//8: Argument Position (no idea what this means tbh)
				//Arg Pos: -1
				String argPos = inputStream.readLine().replaceFirst("Arg Pos: ", "");//argument position (if call or invoke, otherwise -1)
				use2argPos.put(usePoint, argPos);

				//headers
				inputStream.readLine();//9 ## Use....
				inputStream.readLine();//10 ## Free....
				
				//11: Free point line number
				//line: 5
				lineNum = Integer.valueOf(inputStream.readLine().replaceFirst("line: ", ""));//10 free point line num
				
				//12: Free point file name
				//file: ./benchmark/useCorrelation/uc5.c
				tmpString = inputStream.readLine().replaceFirst("file: ", "");
				tmp = tmpString.split("/");
				str = tmp[tmp.length -1];
				if(toMark_FreePoint.get(str) == null)
					toMark_FreePoint.put(str, new ArrayList<Integer>());
				toMark_FreePoint.get(str).add(lineNum);
				
				//free point unique identifier
				String freePoint = lineNum + " : " + str;
				
				//free point information
				String freePointInfo = str + ", line number: " + lineNum;
				
				//13: Free point directory
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", "");//12 free point file directory
				
				freePointInfo = System.getProperty("line.separator") + freePointInfo + System.getProperty("line.separator")+"File Directory: " + str;
				use2free.put(usePoint, freePointInfo);
				free2use.put(freePoint,  usePointInfo);
				
				//14: free point call string
				// ## CXT : ==>sch(ln: 29) ==>f(ln: 5)  ==> $$$
				callString = inputStream.readLine().replaceFirst("CXT :", "");
				free2callString.put(freePoint,  callString);
				
				inputStream.readLine();//14 ## ## Free ############ }
				inputStream.readLine();//15 ## ## Use_After_Free ## 1 ## }
				
				use2id.put(usePoint, issueID);
				free2id.put(freePoint, issueID);
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

		String fileName = resource.getName();//.getLocationURI().toString();
		System.out.println(fileName);
		
		//use point marking
		if(resource instanceof IFile && toMark_UsePoint.containsKey(fileName)){
			//deleteMarkers(file);
			try {
				IDocumentProvider provider = new TextFileDocumentProvider();
				provider.connect(file);
				IDocument doc = provider.getDocument(file);
				for(Integer i:toMark_UsePoint.get(fileName)){
					int IssueID = use2id.get("" + i + " : " + fileName);
					
					String mesg = "Use Point"+ System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "Free Point at : ";
					mesg = mesg + use2free.get("" + i + " : " + fileName) + System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "Argument Position (for callInst): " + use2argPos.get("" + i + " : " + fileName) + System.getProperty("line.separator");
					mesg = mesg + "CallString:" + System.getProperty("line.separator");
					
					String[] callString = use2callString.get("" + i + " : " + fileName).split("==>");
					for(int j = 0; j < callString.length; j++){
						if(j == 0) continue;
						
						callString[j] = callString[j].trim();
						
						if(callString[j+1].contains("$$$"))
							break;
						
						mesg = mesg + j + ": "+ callString[j] + System.getProperty("line.separator");
						
						String[] lineNumArry = callString[j].split(" ");
						String line = lineNumArry[lineNumArry.length -1];
						int lineNum = Integer.parseInt(line.replaceAll("\\D+",""));
						
						IMarker tmp = file.createMarker(MARKER_TYPE_USE_STRING);
						String tmpMsg = "Use Point Call Stack " + j + System.getProperty("line.separator");
						tmpMsg += "For the use point of issue " + IssueID + " at line " + i;
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
					marker.setAttribute(IMarker.CHAR_START, doc.getLineOffset(i.intValue()-1));
					marker.setAttribute(IMarker.CHAR_END, doc.getLineOffset(i.intValue())-1);
					marker.setAttribute(IMarker.LINE_NUMBER, i.intValue()-1);
					marker.setAttribute("IssueID", IssueID);
				}
			} catch (CoreException | BadLocationException e) {
			}
		}
		
		//free point marking
		if(resource instanceof IFile && toMark_FreePoint.containsKey(fileName)){
			//deleteMarkers(file);
			try {
				IDocumentProvider provider = new TextFileDocumentProvider();
				provider.connect(file);
				IDocument doc = provider.getDocument(file);
				for(Integer i:toMark_FreePoint.get(fileName)){
					int IssueID = free2id.get("" + i + " : " + fileName);

					String mesg = "Free Point"+ System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "Use Point at : ";
					mesg = mesg + free2use.get("" + i + " : " + fileName) + System.getProperty("line.separator")+ System.getProperty("line.separator");
					mesg = mesg + "CallString:" + System.getProperty("line.separator");
					
					String[] callString = free2callString.get("" + i + " : " + fileName).split("==>");
					for(int j = 0; j < callString.length; j++){
						if(j == 0) continue;
						
						callString[j] = callString[j].trim();
						if(callString[j+1].contains("$$$"))
							break;
						
						mesg = mesg + j + ": "+ callString[j] + System.getProperty("line.separator");
						
						String[] lineNumArry = callString[j].split(" ");
						String line = lineNumArry[lineNumArry.length -1];
						int lineNum = Integer.parseInt(line.replaceAll("\\D+",""));
						
						IMarker tmp = file.createMarker(MARKER_TYPE_FREE_STRING);
						String tmpMsg = "Free Point Call Stack " + j + System.getProperty("line.separator");
						tmpMsg += "For the free point of issue " + IssueID + " at line " + i;
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
					marker.setAttribute(IMarker.CHAR_START, doc.getLineOffset(i.intValue()-1));
					marker.setAttribute(IMarker.CHAR_END, doc.getLineOffset(i.intValue())-1);
					marker.setAttribute(IMarker.LINE_NUMBER, i.intValue()-1);
					marker.setAttribute("IssueID", IssueID);
				}
			} catch (CoreException | BadLocationException e) {
			}
		}
	}
}
