package melectric.todoxmlparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Environment;
import android.util.Log;

public class BaseFeedParser {
    static List<String> list;
    static List<Task> tasks;
    public static List<Task> parse2(String filePath) throws Exception
    {
        list = new ArrayList<String>();
        tasks = new ArrayList<Task>();  
        
        try{
            File f = new File(filePath);
          
            FileInputStream fileIS = new FileInputStream(f);

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        
	        builderFactory.setNamespaceAware(false);
	        builderFactory.setValidating(false);
	        builderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
	        builderFactory.setFeature("http://xml.org/sax/features/validation", false);

            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            
            Document document = builder.parse(fileIS);
            org.w3c.dom.Element rootElement = document.getDocumentElement();
             
            getChildNodes(rootElement, null, -1);
            
            return tasks;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  
            throw e;
        } catch (SAXException e) {
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch(Exception e)
        {
            Log.e("Error", "Error", e);   
            throw e;
        }
    }

    private static void getChildNodes(Element parent, Integer parentId, Integer parentLevel) throws Exception {
        NodeList childNodes = parent.getChildNodes();
        for(int j=0; j<childNodes.getLength(); j++){
            try{
                Node childNode = childNodes.item(j);
                if(childNode instanceof Element){
                    Element child = (Element) childNode;
                    try
                    {
                    	String name = child.getNodeName();
                    	if("TASK".equals(name))
                    	{
                            Task task = new Task(child, parentId, parentLevel);
                            tasks.add(task);
                            list.add(task.Title);
                            getChildNodes(child, task.Id, parentLevel + 1);
                    	}
                    }
                    catch(NumberFormatException e){
                        // Not a Task Node so Ignore
                    }

                }
            }
            catch(Exception e){
                Log.e("Error", "Error", e);  
                throw e;
            }
        }
    }

}
