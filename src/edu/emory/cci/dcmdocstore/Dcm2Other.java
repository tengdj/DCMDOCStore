package edu.emory.cci.dcmdocstore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.SAXWriter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class Dcm2Other {

	XMLTrimer trimer = new XMLTrimer();
	File curFile = null;
    public static void main(String[] args) throws InterruptedException
    {
    	File[] fs = new File("F:\\test3").listFiles();
    	for(int i = 0;i<fs.length;i++)
    	{
    		if(fs[i].getAbsolutePath().endsWith(".dcm"))
    			System.out.println(new Dcm2Other().dcm2Json(fs[i].getAbsolutePath()));
    	}
			//System.out.println(new Dcm2Other().dcm2Xml("F:\\Dropbox\\New folder\\HdicomNCRIFPH1132.16.124.113543.6003.990192198.38331.16523.3665049472000000\\000001.dcm"));
    }
    public String dcm2Xml(String file){
    	return dcm2Xml(new File(file));
    }
    public String dcm2Xml(File infile){
    	DicomInputStream dis = null;
        FileOutputStream fos = null;
        StringWriter stringOut = new StringWriter();
        try {
        	dis = new DicomInputStream(infile);
        	SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
            TransformerHandler th = tf.newTransformerHandler();
            th.getTransformer().setOutputProperty(OutputKeys.INDENT,"yes");
            th.setResult(new StreamResult(stringOut));
            //th.setResult(new StreamResult(System.out));
            //th.setResult(new StreamResult(new FileOutputStream(new File("/home/db2inst1/Dropbox/test.xml"))));
            final SAXWriter writer = new SAXWriter(th, null);
            writer.setBaseDir(infile.getParentFile());
            writer.setExclude(new int[] {Tag.PixelData});
            dis.setHandler(writer);
            dis.readDicomObject(new BasicDicomObject(), -1);
        } catch(Exception e)
        {
        	return null;
        }
        finally {
            if (fos != null)
				try {
					fos.close();
					dis.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
        }
        String result=null;
		try {
			result = trimer.trimXML(stringOut.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			result=null;
			e.printStackTrace();
		}
        return result;
    }
  
  public String dcm2Json(String filepath)
  {
	  this.curFile = new File(filepath);
	  return xml2Json(dcm2Xml(filepath));
  }
  
  public String dcm2Json(File file)
  {
	  this.curFile=file;
	  return xml2Json(dcm2Xml(file));
  }  
    
  @SuppressWarnings("unchecked")
public String xml2Json(String xml){
       
         try {
        	   JSONObject obj = new JSONObject();
               InputStream is = new ByteArrayInputStream(xml.getBytes("utf-8"));
               SAXBuilder sb = new SAXBuilder();
               Document doc = sb.build(is);
               Element root = doc.getRootElement();
               List<Element> attrs =  root.getChildren();
               for(int i = 0;i<attrs.size();i++) 
               {
            	  // System.out.println(attrs.get(i).getName());
            	  List<Element> values = attrs.get(i).getChildren();
            	  if(values.size()>1)
            	  {
            	  JSONArray valuesJson=null;
            	  for(int j = 0;j<values.size();j++)
            	    {
            		  if(j==0)valuesJson = new JSONArray("[\""+values.get(j).getText()+"\"]");
            		  else {
            			  valuesJson.put(j, values.get(j).getText());
                		 // System.out.println(attrs.get(i).getName()+"{value=\""+values.get(j).getText()+"\"}");
            		  }
            	    }

            	  obj.put(attrs.get(i).getName(),valuesJson);
            	  }
            	  else if(values.size()==1)
            	  obj.put(attrs.get(i).getName(),values.get(0).getText());
               }
               return obj.toString();
          }catch (Exception e){
           e.printStackTrace();
           return null;
         }
   }
  /*
   @SuppressWarnings({ "rawtypes", "unchecked" })
  public Map<String, List> iterateElement(Element element){
        List<Element> node = element.getChildren();
        Element et = null;
        Map<String, List> obj = new HashMap<String, List>();
        List<Object> list = null;
        for (int i = 0; i < node.size(); i++) 
        {
           list = new LinkedList<Object>();
           et = node.get(i); 
           if(et.getTextTrim().equals(""))
           {
              if(et.getChildren().size()==0)
                 continue;
              if(obj.containsKey(et.getName()))
                  list = obj.get(et.getName());      
              list.add(iterateElement(et));
              obj.put(et.getName(),list);
           }
           else
           {
            if(obj.containsKey(et.getName()))
                list = obj.get(et.getName());
            list.add(et.getTextTrim());
            obj.put(et.getName() ,list);
           }
         }
    return obj;
  }

    public static String readStrFromFile(File file){
             try{
    		  BufferedReader bf = new BufferedReader(new FileReader(file));
    		  String content = "";
    		  StringBuilder sb = new StringBuilder();
    		  while(content != null){
    		   content = bf.readLine();
    		   if(content == null){
    		    break;
    		   }	   
    		   sb.append(content.trim());
    		   sb.append("\n");
    		  }  
    		  bf.close();
        	  return sb.toString();
             }
             catch(Exception e){
              return null;
           } 
    }
    public String convertde(File infile)
    {
    	String [] cmd = new String[3];
        cmd[0]="/home/db2inst1/Dropbox/LoadData/TOOLS/dcm2xml";
        cmd[1]=infile.getAbsolutePath();
        cmd[2]=System.getProperty("java.io.tmpdir")+"/"+infile.getName();
        
        File ofile = new File(cmd[2]);
    	Process pro;
		try {
			pro = Runtime.getRuntime().exec(cmd);
			pro.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String xml=null;
	    xml = readStrFromFile(ofile);
		ofile.delete();
		return xml;
    }*/
}
