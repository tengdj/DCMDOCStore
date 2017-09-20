package edu.emory.cci.dcmdocstore;
 
import java.io.File;
import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;
public class DataInsert implements Runnable{
	final static int FILELISTREADER = 1;
	final static int JSONPARSER = 2;
	final static int UPLOADER = 3;
	final static int FILEWRITER = 4;
	
	public static Queue<String[]> jsonQueue;
	public static Queue<String> pathQueue;
	public static Mongo mongo=null;
	public static WriteConcern writeConcern = WriteConcern.NORMAL;

	private int workType = 0;
	private String database;
	private String collumn;
	private String path;
	
	public static int filenumber = 0;
	public static String[] worker = {"","FILELISTREADER","JSONPARSER","UPLOADER","FILEWRITER"};
	public boolean stopped = false;
	public DataInsert()
	{
		if(mongo==null)
		{
			try {
				mongo = new Mongo("pais1.cci.emory.edu", 27031);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}

	
	public DataInsert(int worktype,String host,int port,String database,String collumn,String path)
	{
		    this.workType = worktype;
		    this.database = database;
		    this.collumn = collumn;
		    this.path = path;
		    if(mongo==null)
			{
			  try {
				  mongo = new Mongo(host, port);
			   } catch (UnknownHostException e) {
				System.out.println("unknown host!");
				e.printStackTrace();
			   }
			 }
	}
	
	@Override
	public void run() {
		long start = System.currentTimeMillis();
        if(workType==FILELISTREADER)//read filepath
        {
           pathQueue = new LinkedList<String>();
           jsonQueue = new LinkedList<String[]>();
           readFilelist(new File(this.path));
        }
        else if(workType==JSONPARSER)//parse dicom image
        {
        	while(pathQueue==null)
        	{
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        	parseJson(this.path);
        }
        else if(workType==UPLOADER)//upload into database
        {
        	
        	while(jsonQueue==null)
        	{
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        	insertDicom(this.collumn);	     	
        }
        long end = System.currentTimeMillis();
        System.out.println("my workType is:  "+worker[workType]+" I'm died. totally use : " +(end-start)+"  milliseconds");	
	}
	//parse the directory and put all name into one queue
	public void readFilelist(File file)
	{
		int pathsize = 0;
   		if(file.isDirectory())
   		{
   		   File[] filelist = file.listFiles();
   		   for(File f:filelist)
   			readFilelist(f);
   		}
   	    else if(file.isFile()&&file.getName().endsWith(".dcm"))
   	    {
   	    	synchronized(pathQueue)
   	       {
   	    	pathQueue.offer(file.getAbsolutePath());
   	       }
   	    	
   	    	if(pathsize>1000)
   		    {
   		    	try {
   					Thread.sleep((pathsize)/1000);
   				} catch (InterruptedException e) {
   				}
   		    }
   	    }
   		if(file.getAbsolutePath().equalsIgnoreCase(path))
   			pathQueue.offer("stop!");
   		//if(filenumber%10000==0)
		//System.out.println("file number now is: "+(filenumber++));
	}
	//parse the json object from dicom image
	public void parseJson(String collName)
	{
        Dcm2Other dcmTool = new Dcm2Other();
		String tmppath = null;
		int jsonsize=0;
		String[] pathandjson = new String[2];
		while(true)
		{
		
		jsonsize = jsonQueue.size();
	    if(jsonsize>1000)
		  {
		   try {
					Thread.sleep(1000);
					System.out.println("jsonsize is too large, I need to take a rest!");
					continue;
				} catch (InterruptedException e) {}
		  }
		synchronized(pathQueue)
		{
			tmppath = pathQueue.peek();
			if(tmppath==null)continue;
			if(tmppath.equalsIgnoreCase("stop!"))
				{
				    if(stopped == false)
				      {
				    	synchronized(jsonQueue)
				    	{
				    	 jsonQueue.offer(new String[]{tmppath,""});
				    	}
				    	 stopped = true;
				      }
				    return;
				}
			else tmppath = pathQueue.poll();
		}
		
		if(tmppath==null)
		{
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				continue;
			}
			continue;
		}
		String json = dcmTool.dcm2Json(tmppath);
		pathandjson[0] = tmppath;
    	pathandjson[1] = json;
	    synchronized(jsonQueue)
	    {
	    	jsonQueue.offer(pathandjson);
	    }
	    
	  }
	 
}
	//read from isonQueue and poll one from it and put it into database
	public void insertDicom(String collName)
	{
		DB db = mongo.getDB(this.database);
		GridFS fs = new GridFS(db);
		DBCollection collection = db.getCollection(collName);
		collection.setWriteConcern(writeConcern);
		db.getCollection("fs.files").setWriteConcern(writeConcern);
		db.getCollection("fs.chunks").setWriteConcern(writeConcern);
		String[] pathandjson = null;
		WriteResult wr=null;
		while(true)
		{
		  synchronized(jsonQueue)
		  {
			pathandjson = jsonQueue.peek();
			if(pathandjson!=null&&pathandjson[0].equalsIgnoreCase("stop!")){
				return;
			}
			else pathandjson=jsonQueue.poll();
		  }
		  if(pathandjson==null)
		  {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				continue;
				}
			continue;
		  }
		  DBObject dbObject = (DBObject)JSON.parse(pathandjson[1]);
          
		  GridFSInputFile in;
		  try {
			  in = fs.createFile( new FileInputStream(pathandjson[0]),pathandjson[0]);
			  dbObject.put("file_path", in.getFilename());
			  dbObject.put("files_id", in.getId());
			  in.save();//save the file itself into database
	          wr = collection.insert(dbObject);//save the json object into database
	          if(wr.getError()==null)
	          {
	        	 System.out.println(pathandjson[0]+" is inserted ");
	          }
	          else
	          {
	        	 System.out.println(pathandjson[0]+" inserted failed, error "+wr.getError()+" ");
	          }
		  }catch(Exception e)
		  {
			e.printStackTrace();
			System.out.println("insert file "+pathandjson[0]+" failed!");
		  }
	    }//end while
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	public static void importFromDir(String host,int port, String database,String collumn,String path,int numberofparser,int numberofuploader)
	{
		Thread fileReader;
		fileReader = new Thread(new DataInsert(FILELISTREADER,host,port,database,collumn,path));
		fileReader.start();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
		}
		for(int i = 0;i<numberofparser;i++)
		{
		Thread worker = new Thread(new DataInsert(JSONPARSER,host,port,database,collumn,path));
		worker.start();
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int i = 0;i<numberofuploader;i++)
		{
			Thread uploader = new Thread(new DataInsert(UPLOADER,host,port,database,collumn,path));
		    uploader.start();
		}
		
	}
	
	public static void main(String[] args)
	{
		
		Option infile = new Option("i", "input", true, "input file");
		infile.setRequired(false);
		infile.setArgName("input");
		
		Option host = new Option("h", "host", true, "IP for host");
		host.setRequired(false);
		host.setArgName("host");
		
		
		Option port = new Option("p", "port", true, "port");
		port.setRequired(false);
		port.setArgName("port");
		

		
		Options options = new Options();
		options.addOption(infile);
		options.addOption(host);
		options.addOption(port);
		
		CommandLineParser CLIparser = new GnuParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine line = null;
		try {
			line = CLIparser.parse(options, args);
		} catch(org.apache.commons.cli.ParseException e) {
			formatter.printHelp("JSON Insert", options, true);
			System.exit(1);
		}	
		String inputpath = line.getOptionValue("i");
		String hoststring = line.getOptionValue("h");
		String portstring = line.getOptionValue("p");
		
		if(inputpath==null)
			inputpath = "/home/dteng2/dicomdata/dicom/NCRIandRoswellStrong";
		if(hoststring==null)
			hoststring = "localhost";
		if(portstring==null)
			portstring="27031";
		System.out.println(inputpath);
		writeConcern = WriteConcern.NONE;
		importFromDir(hoststring,Integer.parseInt(portstring),"dicomdb","dicom",inputpath,4,2);
	}
	
	
		
//another file storage method, base64 method
	/*
	public static byte[] readFileByte(File file)
	{
		byte[] bin = null;
		try {
			InputStream in = new FileInputStream(file);
			bin = new byte[in.available()];
			in.read(bin);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return bin;
	}
	
	public static byte[] readFileByte(String file)
	{
		return readFileByt(new File(file));
	}
	

	 	public void insertDicomsGribFS(File file,String collName)
	{
		DBCollection collection = db.getCollection(collName);
		if(file.isDirectory())
			{
			   File[] filelist = file.listFiles();
			   for(File f:filelist)
			       insertDicomsGribFS(f,collName);
			}
		else if(file.isFile()&&file.getName().endsWith(".dcm"))
		{
			String json = dcmTool.dcm2Json(file).toString();
			DBObject dbObject = (DBObject)JSON.parse(json);
			
		    GridFSInputFile in = fs.createFile( readFileByte(file) );
		    in.save();
		    dbObject.put("file_id", new ObjectId(in.getId().toString()));
            collection.insert(dbObject);	
            System.out.println(MongoDB.count++);
		}
		
	}
	
	public void insertDicomsBase64(File file,String collName)
	{
		DBCollection collection = db.getCollection(collName);
		if(file.isDirectory())
		{
		   File[] filelist = file.listFiles();
		   for(File f:filelist)
		       insertDicomsBase64(f,collName);
		}
	   else if(file.isFile()&&file.getName().endsWith(".dcm"))
	  {
		String json = dcmTool.dcm2Json(file).toString();
		DBObject dbObject = (DBObject)JSON.parse(json);
	    dbObject.put("file_Object", readFileBase64(file.getAbsolutePath()));
        collection.insert(dbObject);	
        System.out.println(MongoDB.count++);
	  }
	}
		public static String readFileBase64(String filepath)
	{
		String result = null;
	    try {
			InputStream in = new FileInputStream(filepath);
			byte[] bin = new byte[in.available()];
			in.read(bin);
			in.close();
		   result =  new BASE64Encoder().encode(bin);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return result;
	    
	}
	
	public static void writeBase64(String content, String filepath)
	{
		try {
			OutputStream out = new FileOutputStream(filepath);
			byte[] bout = new BASE64Decoder().decodeBufferToByteBuffer(content).array();
			out.write(bout);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
*/
}