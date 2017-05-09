package uafmarker.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SampleBuilder extends IncrementalProjectBuilder {

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

	class SampleResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			//checkXML(resource); 
			mark(resource);
			//return true to continue visiting children.
			return true;
		}
	}

	//not needed anymore
	class XMLErrorHandler extends DefaultHandler {
		
		private IFile file;

		public XMLErrorHandler(IFile file) {
			this.file = file;
		}

		private void addMarker(SAXParseException e, int severity) {
			SampleBuilder.this.addMarker(file, e.getMessage(), e
					.getLineNumber(), severity);
		}

		public void error(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		public void fatalError(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		public void warning(SAXParseException exception) throws SAXException {
			addMarker(exception, IMarker.SEVERITY_WARNING);
		}
	}

	public static final String BUILDER_ID = "uafmarker.sampleBuilder";

	private static final String MARKER_TYPE = "uafmarker.xmlProblem";

	private SAXParserFactory parserFactory;

	private void addMarker(IFile file, String message, int lineNumber,
			int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
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
		getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
	}

	// not needed anymore
	void checkXML(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".xml")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);
			XMLErrorHandler reporter = new XMLErrorHandler(file);
			try {
				getParser().parse(file.getContents(), reporter);
			} catch (Exception e1) {
			}
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
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

	private SAXParser getParser() throws ParserConfigurationException,
			SAXException {
		if (parserFactory == null) {
			parserFactory = SAXParserFactory.newInstance();
		}
		return parserFactory.newSAXParser();
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
	
	class Loc{
		public Loc(){}
		public Loc(String fileName, int lineNum){
			this.fileName = fileName;
			this.lineNum = lineNum;
		}
		public String fileName;
		public int lineNum;
	}
	
	
	private void readInputFromFile() throws IOException, CoreException { // Hua
		//set up maps
		toMark_UsePoint = new HashMap<String, ArrayList<Integer> >();
		toMark_FreePoint = new HashMap<String, ArrayList<Integer> >();
		use2free = new HashMap<String, String>();
		use2callString = new HashMap<String, String>();
		use2argPos = new HashMap<String, String>();
		
		//set up reader
		BufferedReader inputStream = null;
		try {
			IFile ifile = this.getProject().getFile("UAF.txt");
			String fileName = ifile.getLocation().toString();
			File file = new File(fileName);
			if(!file.exists())
				return;
			inputStream = new BufferedReader(new FileReader(file));
			String str;
			
			//for each line in UAF.txt (loop for each UAF instance)
			while ((str = inputStream.readLine()) != null) {//1 ## Use_After_Free ## ....
				//no warning issued
				if (str.equals("No warning issued.")) {
					break;
				}
				
				//no new Use After Free
				if(!str.contains("## Use_After_Free")) {
					continue; 
				}
				
				//## Use....
				inputStream.readLine();//2 
				//line: 32
				Integer lineNum = Integer.valueOf(inputStream.readLine().replaceFirst("line: ", ""))-1;//3 use point line num
				//file: ./benchmark/useCorrelation/uc5.c
				String tmp[] = inputStream.readLine().split("/");//4 use point file name
				str = tmp[tmp.length -1];
				if(toMark_UsePoint.get(str) == null)
					toMark_UsePoint.put(str, new ArrayList<Integer>());
				
				toMark_UsePoint.get(str).add(lineNum);  // key is the file name, array of lines with error
				
				String usePoint = "" + lineNum + " : " + str; //str here is the file name (eg: 32 : uc5.c)
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", ""); //5 use point file directory
				
				//usePoint = usePoint + " dir: " + str;
				
				//CXT : ==>sch(ln: 32)  ==> $$$
				String callString = inputStream.readLine().replaceFirst("CXT :", "");//6 Call Stack (String)
				
				//callString = callString.replaceAll(";;", "<br>");
				use2callString.put(usePoint,  callString);
				
				//Arg Pos: -1
				String argPos = inputStream.readLine().replaceFirst("Arg Pos: ", "");//7 argument position (if call or invoke, otherwise -1)
				use2argPos.put(usePoint, argPos);
				
				inputStream.readLine();//8 ## Use....
				inputStream.readLine();//9 ## Free....
				//line: 5
				lineNum = Integer.valueOf(inputStream.readLine().replaceFirst("line: ", ""))-1;//10 free point line num
				//file: ./benchmark/useCorrelation/uc5.c
				tmp = inputStream.readLine().split("/");//11 free point file name
				str = tmp[tmp.length -1];
				if(toMark_FreePoint.get(str) == null)
					toMark_FreePoint.put(str, new ArrayList<Integer>());
				toMark_FreePoint.get(str).add(lineNum);
				String freePoint = "ln: " + lineNum + " fl: " + str;
				//dir : /home/stc/stc/test/testMicroBenchmark
				str = inputStream.readLine().replaceFirst("dir : ", "");//12 free point file directory
				freePoint = System.getProperty("line.separator") + freePoint + " dir: " + str;
				use2free.put(usePoint, freePoint);
				inputStream.readLine();//13 ## CXT : ==>sch(ln: 29) ==>f(ln: 5)  ==> $$$
				inputStream.readLine();//14 ## ## Free ############ }
				inputStream.readLine();//15 ## ## Use_After_Free ## 1 ## }
			}
		}catch (FileNotFoundException ex) {
		    ex.printStackTrace();
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}	
	
	void mark(IResource resource) {
		if(!(resource instanceof IFile)) return;
		IFile file = (IFile) resource;
		deleteMarkers(file);
		
		String fileName = resource.getName();//.getLocationURI().toString();
		System.out.println(fileName);
		if(resource instanceof IFile && toMark_UsePoint.containsKey(fileName)){
			//deleteMarkers(file);
			try {
				IDocumentProvider provider = new TextFileDocumentProvider();
				provider.connect(file);
				IDocument doc = provider.getDocument(file);

				for(Integer i:toMark_UsePoint.get(fileName)){
					IMarker marker = file.createMarker(MARKER_TYPE);
					String mesg = "Use Point (Use_After_Free) "+ System.getProperty("line.separator");
					mesg = mesg + " Free Point @ : ";
					mesg = mesg + use2free.get("" + i + " : " + fileName) + System.getProperty("line.separator");
					mesg = mesg + "Argument Position (for callInst): " + use2argPos.get("" + i + " : " + fileName) + System.getProperty("line.separator");
					mesg = mesg + "CallString:" + System.getProperty("line.separator");
					String[] callString = use2callString.get("" + i + " : " + fileName).split("==>");
					for(int j = 0; j < callString.length; j++){
//						if(j ==0)
//							callString[j] = callString[j].replace("CALL"  , "Free__in___Function:  ");
//						if(callString[j].contains("CALL"))
//							callString[j] = callString[j].replace("CALL"  , "Call_______Function:  ");
//						if(callString[j].contains("RET_TO"))
//							callString[j] = callString[j].replace("RET_TO", "Return_to_Function:  ");
//						callString[j] = callString[j].replace('=', ' ');
						if(j == 0) continue;
						callString[j] = callString[j].trim();
						if(callString[j].contains("$$$"))
							break;
						mesg = mesg + j + ": "+ callString[j] + System.getProperty("line.separator");
					}
					System.out.println("" + i + " : " + fileName);
					marker.setAttribute(IMarker.MESSAGE, mesg);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.CHAR_START, doc.getLineOffset(i.intValue()));
					marker.setAttribute(IMarker.CHAR_END, doc.getLineOffset(i.intValue()+1)-1);
					marker.setAttribute(IMarker.LINE_NUMBER, i.intValue());
				}
			} catch (CoreException | BadLocationException e) {
			}
		}
		
		if(resource instanceof IFile && toMark_FreePoint.containsKey(fileName)){
			//deleteMarkers(file);
			try {
				IDocumentProvider provider = new TextFileDocumentProvider();
				provider.connect(file);
				IDocument doc = provider.getDocument(file);

				for(Integer i:toMark_FreePoint.get(fileName)){
					IMarker marker = file.createMarker(MARKER_TYPE);
					marker.setAttribute(IMarker.MESSAGE, "Free Point (Use_After_Free)");
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
					marker.setAttribute(IMarker.CHAR_START, doc.getLineOffset(i.intValue()));
					marker.setAttribute(IMarker.CHAR_END, doc.getLineOffset(i.intValue()+1)-1);
					marker.setAttribute(IMarker.LINE_NUMBER, i.intValue());
				}
			} catch (CoreException | BadLocationException e) {
			}
		}
	}
}
