package uafmarker.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;

public class SampleBuilder extends IncrementalProjectBuilder {

	class SampleDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.
		 * core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				checkXML(resource);
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				checkXML(resource);
				break;
			}
			// return true to continue visiting children.
			return true;
		}
	}

	class SampleResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			mark(resource);
			//simpleMark(resource);
			
			
			// checkXML(resource);
			// return true to continue visiting children.
			return true;
		}
	}

	///////////////////////////////////////

	void simpleMark(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			IFile file = (IFile) resource;
			deleteMarkers(file);

			/*
			 * http://www.vogella.com/tutorials/EclipseCodeAccess/article.html
			 * org.eclipse.ui.ide.IDE.openEditor(IWorkbenchPage,IMarker);
			 */

			try {
				// maybe useful, examples
				// http://www.programcreek.com/java-api-examples/index.php?api=org.eclipse.ui.texteditor.MarkerUtilities

				// How to get CHAR_START from line number
				// http://stackoverflow.com/questions/10980082/get-line-number-within-a-eclipse-plugin
				// How to get IDocument from IFile
				// http://stackoverflow.com/questions/16896538/eclipse-cdt-ifile-ipath-to-idocument
				IDocumentProvider provider = new TextFileDocumentProvider();
				provider.connect(file);
				IDocument doc = provider.getDocument(file);

				IMarker marker = file.createMarker(MARKER_TYPE);
				marker.setAttribute(IMarker.MESSAGE, resource.getName());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.LINE_NUMBER, 10);
				// marker.setAttribute(IMarker.TEXT, resource.getName()+"text");

				IMarker m2 = file.createMarker(MARKER_TYPE);
				m2.setAttribute(IMarker.MESSAGE, resource.getName() + " testing 2");
				m2.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
				// m2.setAttribute(IMarker.TEXT, resource.getName()+"text");
				// m2.setAttribute(IMarker.CHAR_START, 1);
				// m2.setAttribute(IMarker.CHAR_END, 5);
				m2.setAttribute(IMarker.LINE_NUMBER, 25);

				IMarker m3 = file.createMarker(MARKER_TYPE);
				m3.setAttribute(IMarker.MESSAGE, resource.getName() + " testing 3");
				m3.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				// m3.setAttribute(IMarker.TEXT, resource.getName()+"text");

				m3.setAttribute(IMarker.CHAR_START, doc.getLineOffset(20));
				m3.setAttribute(IMarker.CHAR_END, doc.getLineOffset(21) - 1);
				m3.setAttribute(IMarker.LINE_NUMBER, 20);

				IMarker m6 = file.createMarker(MARKER_TYPE);
				m6.setAttribute(IMarker.MESSAGE, resource.getName() + " testing 6");
				m6.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				// m6.setAttribute(IMarker.TEXT, resource.getName()+"text");
				// m6.setAttribute(IMarker.CHAR_START, 0);
				// m6.setAttribute(IMarker.CHAR_END, 30);
				m6.setAttribute(IMarker.LINE_NUMBER, 30);
			} catch (CoreException | BadLocationException e) {
			}

		}
	}

	/////////////////////////////////////////////////////

	class XMLErrorHandler extends DefaultHandler {

		private IFile file;

		public XMLErrorHandler(IFile file) {
			this.file = file;
		}

		private void addMarker(SAXParseException e, int severity) {
			SampleBuilder.this.addMarker(file, e.getMessage(), e.getLineNumber(), severity);
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

	private void addMarker(IFile file, String message, int lineNumber, int severity) {
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
	 * java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
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

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		try {
			readInputFromFile(); // Hua
			getProject().accept(new SampleResourceVisitor());
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readInputFromFile() throws IOException, CoreException { // Hua
		toMark_UsePoint = new HashMap<String, ArrayList<Integer> >();
		toMark_FreePoint = new HashMap<String, ArrayList<Integer> >();
		use2free = new HashMap<String, String>();
		use2callString = new HashMap<String, String>();
		use2argPos = new HashMap<String, String>();
		BufferedReader inputStream = null;
		try {
			IFile ifile = this.getProject().getFile("UAF.txt");
			String fileName = ifile.getLocation().toString();
			File file = new File(fileName);
			if(!file.exists())
				return;
			inputStream = new BufferedReader(new FileReader(file));
			String str;
			while ((str = inputStream.readLine()) != null) {//1 Use_After_Free
				inputStream.readLine();//2 Use:
				Integer lineNum = Integer.valueOf(inputStream.readLine())-1;//3 use point line num
				str = inputStream.readLine();//4 use point file name
				if(toMark_UsePoint.get(str) == null)
					toMark_UsePoint.put(str, new ArrayList<Integer>());
				toMark_UsePoint.get(str).add(lineNum);
				String usePoint = "" + lineNum + " : " + str;
				str = inputStream.readLine(); //5 use point file directory
				//usePoint = usePoint + " dir: " + str;
				
				
				String callString = inputStream.readLine();//6 Call Stack (String)
				//callString = callString.replaceAll(";;", "<br>");
				use2callString.put(usePoint,  callString);
				
				String argPos = inputStream.readLine();//7 argument position (if call or invoke, otherwise -1)
				use2argPos.put(usePoint, argPos);
				
				inputStream.readLine();//8 Free:
				lineNum = Integer.valueOf(inputStream.readLine())-1;//9 free point line num
				str = inputStream.readLine();//10 free point file name
				if(toMark_FreePoint.get(str) == null)
					toMark_FreePoint.put(str, new ArrayList<Integer>());
				toMark_FreePoint.get(str).add(lineNum);
				String freePoint = "ln: " + lineNum + " fl: " + str;
				str = inputStream.readLine();//11 free point file directory
				freePoint = System.getProperty("line.separator") + freePoint + " dir: " + str;
				use2free.put(usePoint, freePoint);
			}
		}catch (FileNotFoundException ex) {
		    ex.printStackTrace();
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
	private Map<String, ArrayList<Integer> > toMark_UsePoint; //filename 2 usepoint list
	private Map<String, ArrayList<Integer> > toMark_FreePoint;//filename 2 freepoint list
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
					String[] callString = use2callString.get("" + i + " : " + fileName).split(";;");
					for(int j = 0; j < callString.length; j++){
						if(j ==0)
							callString[j] = callString[j].replace("CALL"  , "Free__in___Function:  ");
						if(callString[j].contains("CALL"))
							callString[j] = callString[j].replace("CALL"  , "Call_______Function:  ");
						if(callString[j].contains("RET_TO"))
							callString[j] = callString[j].replace("RET_TO", "Return_to_Function:  ");
						callString[j] = callString[j].replace('=', ' ');
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
	
	private SAXParser getParser() throws ParserConfigurationException, SAXException {
		if (parserFactory == null) {
			parserFactory = SAXParserFactory.newInstance();
		}
		return parserFactory.newSAXParser();
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new SampleDeltaVisitor());
	}
}
