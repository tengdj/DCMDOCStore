package edu.emory.cci.dcmdocstore;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;  
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;  
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;  
import org.w3c.dom.Element;  
import org.w3c.dom.NodeList;  

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
public class XMLTrimer {
	
    private HashMap<String,String> map = new HashMap<String,String>(); 
    public XMLTrimer()
    {
    	try {
    		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("dicom_dict.txt")));
    		String line=null;
    		while((line = reader.readLine())!=null)
    		{
    			map.put(line.split(" ")[0], line.split(" ")[1]);
    	    }
    		reader.close();
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    public ArrayList<Attribute> readXml(String xml){  
    	  ArrayList<Attribute> attrs = new ArrayList<Attribute>();
		  try{    
		        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();   
		        DocumentBuilder db = dbf.newDocumentBuilder(); 
		        Document doc=db.parse(new ByteArrayInputStream(xml.getBytes("utf-8")));
		        //Document doc=db.parse(fileName);  
		        Element  els=doc.getDocumentElement();  
		        
		        NodeList list=els.getElementsByTagName("attr"); 

		         for(int i=0;i<list.getLength();i++){  

		        	 Attribute attr = new Attribute();
		        	 attr.setTag(list.item(i).getAttributes().getNamedItem("tag").getNodeValue());
		        	 attr.setDescription(getDescriptionBYTag(list.item(i).getAttributes().getNamedItem("tag").getNodeValue()));
		        	 attr.setVr(list.item(i).getAttributes().getNamedItem("vr").getNodeValue());
		        	 if(list.item(i).getFirstChild()!=null)
		        	 {
		        		String value = list.item(i).getFirstChild().getNodeValue();
		        		
		        		String[] attrsValue=null;
		        		if(value.contains("\\"))
		        		attrsValue = value.split("\\\\");
		        		else
		        		{
		        			attrsValue = new String[1];
		        			attrsValue[0]=value;
		        		}
		        		 ArrayList<String> attrvalues = new ArrayList<String>();
		        		 for(int k = 0;k<attrsValue.length;k++)
		        			 attrvalues.add(attrsValue[k]);
		        		 attr.setAttr(attrvalues);
		        	 }
		        	 else attr.setAttr(new ArrayList<String>());
		             attrs.add(attr);
		         }     
		        }catch(Exception e){  
		             e.printStackTrace();   
		      }  
		  return attrs;
	}
		    
	public String getDescriptionBYTag(String tag){
         
		 return (String) map.get(tag);
	 }
    
    public String trimXML(String xml) throws IOException
    {
      ArrayList<Attribute> attrs = readXml(xml);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();   
      DocumentBuilder db=null;
	  try{
		db = dbf.newDocumentBuilder();
	  }catch (ParserConfigurationException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	  }
      Document newDoc=db.newDocument();  
      Element  root=newDoc.createElement("dicom");  
      /*
      for(int i = 0;i<attrs.size();i++)
      {
    	if(attrs.get(i).getDescription()!=null&&!attrs.get(i).getDescription().equalsIgnoreCase(""))
        for(int k = 0;k<attrs.get(i).getAttr().size();k++)
        {
          Element attr=newDoc.createElement(attrs.get(i).getDescription());
          attr.appendChild(newDoc.createTextNode(attrs.get(i).getAttr().get(k)));
          root.appendChild(attr);     
        } 
      }
      */
      for(int i = 0;i<attrs.size();i++)
      {
    	if(attrs.get(i).getDescription()!=null&&!attrs.get(i).getDescription().equalsIgnoreCase(""))
    	{
    		Element attr=newDoc.createElement(attrs.get(i).getDescription());
            for(int k = 0;k<attrs.get(i).getAttr().size();k++)
            {
              Element value = newDoc.createElement("value");
              value.appendChild(newDoc.createTextNode(attrs.get(i).getAttr().get(k)));
              attr.appendChild(value);
              } 
        root.appendChild(attr);
    	}
      }
      
      newDoc.appendChild(root);
      OutputFormat    format  = new OutputFormat( newDoc , "GBK" , true );    
      StringWriter  stringOut = new StringWriter();   
      XMLSerializer    serial = new XMLSerializer(stringOut, format );     
      serial.asDOMSerializer();      
      serial.serialize( newDoc.getDocumentElement() );     
      //PrintStream ps = new PrintStream(new FileOutputStream(fileName));    
      return stringOut.toString();
    }      
    
}
