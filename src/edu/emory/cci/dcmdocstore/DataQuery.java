package edu.emory.cci.dcmdocstore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.types.ObjectId;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

public class DataQuery implements Runnable{

	    public static Mongo mongo;
	    public static DBCursor cursor;
	    public static String root = "d:\\";
		public static int numberofWriter = 8;

	    String database;
	    
	    
	    public DataQuery(String host,int port,String database)
	    {
	    	if(mongo==null)
	    	{
	    	try {
				mongo = new Mongo(host,port);
				System.out.println("connect to "+host+":"+port+" successfully!");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
	    	}
	    	this.database = database;
	    }
	    
		@Override
		public void run() {
			writeCursor();
		}
	    
		public void writeCursor()
		{
			
			long start  = System.currentTimeMillis();
			DBObject obj;
			DB db = mongo.getDB(this.database);
			GridFS fs = new GridFS(db);
			String filepath = null;
			int counter=0;
			while(true)
			{
				 synchronized(cursor)
				 {
					 if(cursor.hasNext())
						 obj = cursor.next();
					 else break;
				 }
				 counter++;
				 filepath = obj.get("file_path").toString();
				 filepath = filepath.substring(filepath.indexOf("\\"),filepath.length() );
				 //System.out.println(Thread.currentThread()+" writing file "+DataQuery.root+File.separator+filepath);
				 savaFileByID(fs,obj.get("files_id"), DataQuery.root+File.separator+filepath);		 
			 }
			 long end = System.currentTimeMillis();
	         System.out.println("write " + counter+" files, totally cost "+(end-start)+" millisecond");
		}

		
		public void clearColl(String database,String collName)
		{
			DB db = mongo.getDB(database);
			GridFS fs = new GridFS(db);
			DBCollection collection = db.getCollection(collName);
			DBCursor cursor = collection.find();
			while(cursor.hasNext())
			 {
				 DBObject dbobject = cursor.next();
				 fs.remove((ObjectId)dbobject.get("files_id"));
				 collection.remove(dbobject);
			 }
			 DBCursor fc = fs.getFileList();
			 while(fc.hasNext())
			 {
				 fs.remove(fc.next());
			 } 
		}
		
		public  void savaFileByID(GridFS fs,Object object,String path)
		{
			GridFSDBFile out =  fs.findOne( new BasicDBObject( "_id" , object ) );	
			 //Save loaded image from database into new image file
	        FileOutputStream outputImage;
	        File parentDir = new File(path).getParentFile();
	        if(parentDir.exists()==false)
	           parentDir.mkdirs();
			try {
				outputImage = new FileOutputStream(path);
				out.writeTo( outputImage );
		        outputImage.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		
		public static int count = 1;
		public static void main(String[] args){
         
			query2();
			
		}
		public static void query1()
		{  
			new DataQuery("pais1.cci.emory.edu",27031,"dicomdb");
			DBCollection c = mongo.getDB("dicomdb").getCollection("dicom");
		    cursor = c.find(new BasicDBObject("PatientName","1.3.6.1.4.1.9328.50.2.0155"));
		    for(int i = 0;i<numberofWriter;i++)
		    {
		    	Thread thread = new Thread(new DataQuery("pais1.cci.emory.edu",27017,"dicomdb"));
		    	thread.start();
		    }
			
		}

		public static void query2()
		{
		    new DataQuery("pais1.cci.emory.edu",27031,"dicomdb");
		    DBCollection c = mongo.getDB("dicomdb").getCollection("dicom");
		    BasicDBObject match = new BasicDBObject("$match",new BasicDBObject("PatientAge",new BasicDBObject("$gt","075Y")));
		    BasicDBObject project = new BasicDBObject("$project",new BasicDBObject("PatientName",1));
		    BasicDBObject group = new BasicDBObject("$group",new BasicDBObject("_id",new BasicDBObject("patient","$PatientName")).append("number", new BasicDBObject("$sum",1)));
		    BasicDBObject sort = new BasicDBObject("$sort",new BasicDBObject("number",-1));
		    BasicDBObject limit = new BasicDBObject("$limit",100);
		    
		    Long starttime = System.currentTimeMillis();
		    AggregationOutput aggre = c.aggregate(match,project,group,sort,limit);
			Long endtime = System.currentTimeMillis();
			System.out.println(endtime-starttime);
			
		    //AggregationOutput aggre = c.aggregate(new BasicDBObject("$group",new BasicDBObject("_id",new BasicDBObject("PatientAge","$PatientAge")).append("number",new BasicDBObject("$sum",1))) );
		    //Iterator<DBObject> it = aggre.results().iterator();
		    //while(it.hasNext())
		    //{
			    //System.out.println(it.next().toString());
		    //}
		}
		
		public static void query3()
		{
			new DataQuery("pais1.cci.emory.edu",27031,"dicomdb");
		    DBCollection c = mongo.getDB("dicomdb").getCollection("dicom");
		    ArrayList<String> imagetype = new ArrayList<String>();
		    imagetype.add("ORIGINAL");
		    imagetype.add("PRIMARY");
		    imagetype.add("AXIAL");
		    BasicDBObject query  = new BasicDBObject("ImageType",new BasicDBObject("$all",imagetype));
		    
		    Long starttime = System.currentTimeMillis();
		    DBCursor cur = c.find(query);
			Long endtime = System.currentTimeMillis();
			System.out.println(endtime-starttime+" "+cur.count());
		    
		}
		@SuppressWarnings("unchecked")
		public static void query4()
		{
			new DataQuery("pais1.cci.emory.edu",27031,"dicomdb");
		    DBCollection c = mongo.getDB("dicomdb").getCollection("dicom");
			 Long starttime = System.currentTimeMillis();
			 List<String> list = c.distinct("PatientName");
			 Long endtime = System.currentTimeMillis();
			 System.out.println(endtime-starttime+" "+list.size());
		}
}
