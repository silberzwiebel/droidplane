package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.acra.ACRA;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.analytics.tracking.android.EasyTracker;

import android.net.Uri;
import android.util.Log;

/**
 * Mindmap handles the loading and storing of a mind map document.
 */
public class Mindmap {
	
	/**
	 * the XML DOM document, the mind map
	 */
	public Document document;

	/**
	 * The currently loaded Uri
	 */
	private Uri uri;
	
	/**
	 * The root node of the document.
	 */
	private MindmapNode rootNode;

	/**
	 * a hash map that resolves node IDs to Node objects
	 */
	private HashMap<String, MindmapNode> ids;

	/**
	 * Returns the Uri which is currently loaded in document.
	 * @return Uri
	 */
	public Uri getUri() {
		return this.uri;
	}

	/**
	 * Set the Uri after loading a new document.
	 * @param uri
	 */
	public void setUri(Uri uri) {
		this.uri = uri;
	}
	
	/**
	 * Loads a mind map (*.mm) XML document into its internal DOM tree
	 * @param inputStream the inputStream to load
	 */
	public void loadDocument(InputStream inputStream) {
		
        // start measuring the document load time
		long loadDocumentStartTime = System.currentTimeMillis();
		
		// XML document builder
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;

        // load the Mindmap from the InputStream
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			document = docBuilder.parse(inputStream);
		} catch (ParserConfigurationException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "ParserConfigurationException");
			e.printStackTrace();
			return;
		} catch (SAXException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "SAXException");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "IOException");
			e.printStackTrace();
			return;
		}


		rootNode = new MindmapNode(MainApplication.getStaticApplicationContext(), document.getDocumentElement().getElementsByTagName("node").item(0));

		// fill the hash map (TODO: is this a good place to do this? Should / Could this be done in a more efficient way?)
		ids = new HashMap<String, MindmapNode>();
		this.fillHashMap(rootNode);
		
		long loadDocumentEndTime = System.currentTimeMillis();
	    EasyTracker.getTracker().sendTiming("document", loadDocumentEndTime-loadDocumentStartTime, "loadDocument", "loadTime");
		Log.d(MainApplication.TAG, "Document loaded");
	    
		long numNodes = document.getElementsByTagName("node").getLength();
		EasyTracker.getTracker().sendEvent("document", "loadDocument", "numNodes", numNodes);

	}
	
	/**
	 * Returns the root node of the currently loaded mind map
	 * @return the root node
	 */
	public MindmapNode getRootNode() {
		return rootNode;
	}

	private void fillHashMap(MindmapNode n){

		this.ids.put(n.id, n);
		Log.d(MainApplication.TAG, "Added " + n.id + " to hashmap.");

		ArrayList<MindmapNode> children = n.getChildNodes();
		Iterator<MindmapNode> iter = children.iterator();

		while (iter.hasNext()) {
			this.fillHashMap(iter.next());
		}

	}

	public MindmapNode getNodeFromID(String id) {

		MindmapNode node = this.ids.get(id);

		Log.d(MainApplication.TAG, "Returning node with ID " + id);

		return node;
	}

}
