package svsync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Sval
 */
public class Profile {
    
    private List<SourceInfo> sources = new ArrayList<>();
    private TargetInfo target;
    
    private Profile() {
    }
    
    public static Profile load(String profilePath) throws Exception {

	File xmlFile = new File(profilePath);
        if (!xmlFile.exists()) {
            throw new Exception("Profile file [" + profilePath + "] not found");
        }
        
        Profile profile = new Profile();
        
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder;
        
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            
            // Parse archives
            NodeList sourceNodes = doc.getElementsByTagName("source");
            if (sourceNodes.getLength() == 0) throw new Exception("Incorrect file format: should have at least one archive");

            for (int i=0; i<sourceNodes.getLength(); i++) {
                Node sourceNode = sourceNodes.item(i);
                Map<String, String> params = new HashMap<>();
                
                for (int j=0; j<sourceNode.getAttributes().getLength(); j++) {
                    String key = sourceNode.getAttributes().item(j).getNodeName().trim();
                    String value = sourceNode.getAttributes().item(j).getTextContent().trim();
                    params.put(key, value);
                }                                

                List<String> excludes = new ArrayList<>();
                List<String> filters = new ArrayList<>();
                
                for (int j=0; j<sourceNode.getChildNodes().getLength(); j++) {
                    Node currentNode = sourceNode.getChildNodes().item(j);
                    
                    switch (currentNode.getNodeName()) {
                        case "exclude":
                            Node pathExclude = currentNode.getAttributes().getNamedItem("path");
                            if (pathExclude != null) {
                                excludes.add(pathExclude.getTextContent());
                            }
                            Node filter = currentNode.getAttributes().getNamedItem("filter");
                            if (filter != null) {
                                filters.add(filter.getTextContent());
                            }
                        break;
                    }
                }

                SourceInfo source = SourceInfo.createSource(params, excludes, filters);
                profile.sources.add(source);
            }
            
            // Parse target
            NodeList targetNodes = doc.getElementsByTagName("target");
            if (targetNodes.getLength() == 0) throw new Exception("Incorrect file format: should have at least one target");

            Node targetNode = targetNodes.item(0);

            Map<String, String> params = new HashMap<>();
            for (int i=0; i<targetNode.getAttributes().getLength(); i++) {
                String key = targetNode.getAttributes().item(i).getNodeName().trim();
                String value = targetNode.getAttributes().item(i).getTextContent().trim();
                params.put(key, value);
            }
            
            profile.target = TargetInfo.createTarget(params);
        }
        catch (IOException | ParserConfigurationException | SAXException ex) {
            throw new Exception(ex);
        } 
        
        return profile;
    }    
    
    public List<SourceInfo> getSources() {
        return sources;
    }
    
    public TargetInfo getTarget() {
        return target;
    }
}
