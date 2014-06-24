package com.turner.lmdb.feed.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.turner.lmdb.feed.LmdbFeed;

public class LmdbXml extends LmdbFeed {

	public LmdbXml(String url) {
		super(url);
	}

	Node getElemNode( NodeList nodes ) {
		Node node=null;
		for (int i=0; i < nodes.getLength(); i++) {
			if ( nodes.item(i).getNodeType()==Node.ELEMENT_NODE)
				return nodes.item(i);
		}
		return node;
	}

	public static Element getFirstElement( Element element, String name ) {
		NodeList nl = element.getElementsByTagName( name );
		if ( nl.getLength() < 1 )
			throw new RuntimeException(
					"Element: "+element+" does not contain: "+name);
		return (Element)nl.item(0);
	}

	public static String getSimpleElementText( Element node, String name ) 
	{
		Element namedElement = getFirstElement( node, name );
		return getSimpleElementText( namedElement );
	}

	public static String getSimpleElementText( Element node ) 
	{
		StringBuffer sb = new StringBuffer();
		NodeList children = node.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if ( child instanceof Text )
				sb.append( child.getNodeValue() );
		}
		return sb.toString();
	}

	public static String decodeNodeType( short s ) {
		String nodeType = "";
		switch( s ) {
		case Node.ELEMENT_NODE:	nodeType = "Node.ELEMENT_NODE " + Node.ELEMENT_NODE;
		break;
		case Node.ATTRIBUTE_NODE:	nodeType = "Node.ATTRIBUTE_NODE" + Node.ATTRIBUTE_NODE;
		break;
		case Node.ENTITY_NODE:	nodeType = "Node.ENTITY_NODE " + Node.ENTITY_NODE;
		break;
		case Node.TEXT_NODE:	nodeType = "Node.TEXT_NODE " + Node.TEXT_NODE;
		break;
		default:				nodeType = "Some other node";
		break;
		}
		return nodeType;
	}
	
	static String schedItemsToString( Document doc) {
		String schedItemData = "";
		NodeList nList = doc.getElementsByTagName("SchedItem");
		for (int i=0; i<nList.getLength(); i++){
			Node n = nList.item(i);
			if ( n.getNodeType()==Node.ELEMENT_NODE) {
				Node airingID = n.getAttributes().getNamedItem("AiringID");
				schedItemData += "AiringID:" + airingID.getNodeValue() + " | ";
				NodeList schedItemChildren = n.getChildNodes();
				Element programChildren=null;
				for (int j=0; j<n.getChildNodes().getLength(); j++)
					if ( schedItemChildren.item(j).getNodeName().equals("Program") )
						programChildren = (Element)schedItemChildren.item(j).getChildNodes();
				schedItemData += "Name:" + getSimpleElementText(programChildren, "Name") + " | ";
				schedItemData += "FranchiseName:" + getSimpleElementText(programChildren, "FranchiseName") + " | ";
				schedItemData += "StartTime:" + getSimpleElementText(programChildren, "StartTime") + " | ";
				schedItemData += "EndTime:" + getSimpleElementText(programChildren, "EndTime");
			}
			schedItemData += "\n";
		}
		return schedItemData;
	}

	public Map<String, String> read(){
		String xml = readUrl();
		if ( errors.length()>0) lmdbMap.put("errors", errors);
		if ( xml != null && xml.length()>0 ) {
			lmdbMap.put("lmdbFile", xml);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
				Element docElement = doc.getDocumentElement();
				docElement.normalize();
				Node liveFeedNode = doc.getFirstChild();
				if (liveFeedNode.getNodeName().equals("LiveFeed")) {
					lmdbMap.put("LiveFeed", "LiveFeed");
					Node tveLiveSchedNode = getElemNode( liveFeedNode.getChildNodes() ); 
					if (tveLiveSchedNode != null && tveLiveSchedNode.getNodeName().equals("tveLiveSched")) {
						lmdbMap.put("tveLiveSched", "tveLiveSched");
						Node timeStampAttr = tveLiveSchedNode.getAttributes().getNamedItem("TimeStamp");
						if (timeStampAttr != null) lmdbMap.put("TimeStamp", timeStampAttr.getNodeValue());
						Node startAttr = tveLiveSchedNode.getAttributes().getNamedItem("Start");
						if (startAttr != null) lmdbMap.put("Start", startAttr.getNodeValue());
						Node endAttr = tveLiveSchedNode.getAttributes().getNamedItem("End");
						if (endAttr != null) lmdbMap.put("End", endAttr.getNodeValue());
						NodeList nList = doc.getElementsByTagName("SchedItem");
						if (nList != null) {
							lmdbMap.put("SchedItem", String.valueOf(nList.getLength()));
							lmdbMap.put("SchedItemData", schedItemsToString(doc));
						}
					}
				}
			} catch (ParserConfigurationException e) {
				errors += e.getMessage();
				lmdbMap.put("errors", errors);
				return lmdbMap;
			} catch (IOException ioe) {
				errors += ioe.getMessage();
				lmdbMap.put("errors", errors);
				return lmdbMap;
			} catch (SAXException e) {
				errors += "Error parsing xml:  " + e.getMessage();
				lmdbMap.put("errors", errors);
				return lmdbMap;
			} catch (Exception e) {
				errors += e.getMessage();
				lmdbMap.put("errors", errors);
				return lmdbMap;
			}
		}
		return lmdbMap;
	}
}
//Test data for testing xml parsing:
//String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?> <tveLiveSched Provider=\"TBSE\" TimeStamp=\"2013-07-09T09:24:01.2619954-04:00\" Start=\"2013-07-09T08:29:55.0000000-04:00\" End=\"2013-07-09T13:00:20.0000000-04:00\"> <SchedItem AiringID=\"472165036\"> <Program/> </SchedItem> <SchedItem AiringID=\"472165999\"> <Program/> </SchedItem> <SchedItem AiringID=\"472165999\"> <Program/> </SchedItem> </tveLiveSched> </LiveFeed>";
//String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?> <SchedItem AiringID=\"472165036\"> <Program/> </SchedItem> <SchedItem AiringID=\"472165999\"> <Program/> </SchedItem> <SchedItem AiringID=\"472165999\"> <Program/> </SchedItem>";
//String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?> <LiveFeed Version=\"1.0\"> <tveLiveSched Provider=\"TBSE\" TimeStamp=\"2012-07-09T09:24:01.2619954-04:00\" Start=\"2013-07-01T08:29:55.0000000-04:00\" End=\"2013-07-09T13:00:20.0000000-04:00\"> <SchedItem AiringID=\"472165036\"> <Program/> </SchedItem> <SchedItem AiringID=\"472165999\"> <Program/> </SchedItem> <SchedItem AiringID=\"472165999\"> <Program/> </SchedItem> </tveLiveSched> </LiveFeed>";
//String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?> <LiveFeed Version=\"1.0\"> <tveLiveSched Provider=\"TBSE\" TimeStamp=\"2013-07-09T09:24:01.2619954-04:00\" Start=\"2013-07-09T08:29:55.0000000-04:00\"> </tveLiveSched> </LiveFeed>";
//String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
//String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><A><b/><b/></A>";
//String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><A><tveLiveSched Provider=\"TBSE\" TimeStamp=\"2012-07-09T09:24:01.2619954-04:00\" Start=\"2013-07-01T08:29:55.0000000-04:00\" End=\"2013-07-09T13:00:20.0000000-04:00\"/><b/><b/></A>";