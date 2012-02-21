package melectric.todoxmlparser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlpull.v1.XmlSerializer;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Task {
    public Integer Id;
    public String Title;
    public Integer ParentId;
    public List<Task> Children;
    public Integer Level;
    public boolean Completed;
	private String COMMENTS;
	private String COMMENTSTYPE;
	private Integer PRIORITY;
	List<Attribute> Attributes;
    
	public Task() {
	}
	
	public Task(Element child, Integer parentId, Integer parentLevel) {
		NamedNodeMap attributes = child.getAttributes();
		this.Attributes = new ArrayList<Attribute>();
		for (int i = 0; i < attributes.getLength(); i++) { 
			Node attribute = attributes.item(i);
		    String attributeName = attribute.getNodeName();
		    String attributeValue = attribute.getNodeValue();
		    
		    
		    
		    if("ID".equals(attributeName))
		    {
	            Integer id = new Integer(attributeValue);
	            this.Id = new Integer(id);
		    }
		    else if("TITLE".equals(attributeName))
		    {
		        String title = attributeValue;
		        this.Title = title;
		    }
		    else if("PERCENTDONE".equals(attributeName))
		    {
	            try{
	                Integer completedPercent = new Integer(attributeValue);
	                boolean completed = completedPercent == 100;
	                this.Completed = completed;
	            }
	            catch(Exception e){
	                // PercentDone might not exist so set to false
	            	this.Completed = false;
	            }
		    }
		    else if("COMMENTS".equals(attributeName))
		    {
		        this.COMMENTS = attributeValue;
		    }
		    else if("COMMENTSTYPE".equals(attributeName))
		    {
		        this.COMMENTSTYPE = attributeValue;
		    }
		    else if("PRIORITY".equals(attributeName))
		    {
	            String priority =  attributeValue;
	            if(priority != "")
	            {
	            	this.PRIORITY = new Integer(priority);
	            }
		    }
		    else if("DONEDATESTRING".equals(attributeName))
		    {
		    	
		    }
		    else if("DONEDATE".equals(attributeName))
		    {
		    	
		    }
		    else
		    {
				Attribute attributeObject = new Attribute();
				attributeObject.Name = attributeName;
				attributeObject.Value = attributeValue;
				this.Attributes.add(attributeObject);
		    }
		}
	
        this.ParentId = parentId;              
        this.Level = parentLevel + 1;
	}

	public Task(Cursor c, SQLiteDatabase db) {
        int IdColumn = c.getColumnIndex("Id");
        int TitleColumn = c.getColumnIndex("Title");
        int CompletedColumn = c.getColumnIndex("Completed");
        int COMMENTSColumn = c.getColumnIndex("COMMENTS");
        int COMMENTSTYPEColumn = c.getColumnIndex("COMMENTSTYPE");
        int PRIORITYColumn = c.getColumnIndex("PRIORITY");
        
		this.Id = c.getInt(IdColumn);
		this.Title = c.getString(TitleColumn);
        int completedint = c.getInt(CompletedColumn);
        this.Completed = completedint == 1;
        this.COMMENTS = c.getString(COMMENTSColumn);
        this.COMMENTSTYPE = c.getString(COMMENTSTYPEColumn);
        this.PRIORITY = c.getInt(PRIORITYColumn);
        
        this.Attributes = new ArrayList<Attribute>();
        Cursor attributeCursor = db.rawQuery("SELECT * FROM t_Attributes WHERE TaskId = " + this.Id, null);      
    	int NameColumn = attributeCursor.getColumnIndex("Name");
    	int ValueColumn = attributeCursor.getColumnIndex("Value");
        attributeCursor.moveToFirst();
        if (attributeCursor != null && attributeCursor.getCount() > 0) {
            do {            	
        		Attribute attributeObject = new Attribute();
        		attributeObject.Name = attributeCursor.getString(NameColumn);
        		attributeObject.Value = attributeCursor.getString(ValueColumn);
        		this.Attributes.add(attributeObject);
            } while (attributeCursor.moveToNext());
        }
	}

	public void SaveToDatabase(SQLiteDatabase database, String tableName, String attributesTableName) {
        int completed = 0;
        if(this.Completed)
        {
            completed = 1;
        }
        
        ContentValues args = new ContentValues();
        args.put("Id", this.Id);
        args.put("Title", this.Title);
        args.put("ParentId", this.ParentId);
        args.put("Level", this.Level);
        args.put("Completed", completed);
        args.put("COMMENTS", this.COMMENTS);
        args.put("COMMENTSTYPE", this.COMMENTSTYPE);
        args.put("PRIORITY", this.PRIORITY); 
        database.insert(tableName, "", args);
        
        for(Attribute attribute: this.Attributes ) {
        	args = new ContentValues();
        	args.put("TaskId", this.Id);
        	args.put("Name", attribute.Name);
        	args.put("Value", attribute.Value);
        	database.insert(attributesTableName, "", args);
        }   
	}

	public void SaveAttributesToFile(XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.attribute(null, "ID", this.Id.toString());
        serializer.attribute(null, "TITLE", this.Title);
        if(this.Completed)
        {
        	serializer.attribute(null, "PERCENTDONE", "100");
        	
        	//TODO: DONEDATE and DONEDATESTRING - needs to load from database(when the field has been implemented), and needs to implement time
        	Date doneDate = new Date();
        	
        	SimpleDateFormat dateformatYYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");
        	StringBuilder nowYYYYMMDD = new StringBuilder( dateformatYYYYMMDD.format(doneDate));
        	serializer.attribute(null, "DONEDATESTRING", nowYYYYMMDD.toString());
        	
        	long diffInDays = (doneDate.getTime() - new Date(0,0,1).getTime())/1000/60/60/24;
        	
        	serializer.attribute(null, "DONEDATE", String.valueOf(diffInDays));
        }	
        if(this.COMMENTS != null)
        {
            serializer.attribute(null, "COMMENTS", this.COMMENTS);
        }
        if(this.COMMENTSTYPE != null)
        {
        	
        	serializer.attribute(null, "COMMENTSTYPE", this.COMMENTSTYPE);
        }
        if(this.PRIORITY != null)
        {
        	serializer.attribute(null, "PRIORITY", this.PRIORITY.toString());
        }
        
        for(Attribute attribute: this.Attributes ) {
        	serializer.attribute(null, attribute.Name, attribute.Value);
        }   
	}
}
