package melectric.todoxmlparser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Element;
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
    
	public Task() {
	}
	
	public Task(Element child, Integer parentId, Integer parentLevel) {

        String title = child.getAttribute("TITLE");

        this.Title = title;

            Integer id = new Integer(child.getAttribute("ID"));
            this.Id = new Integer(id);
            this.ParentId = parentId;
            try{
                Integer completedPercent = new Integer(child.getAttribute("PERCENTDONE"));
                boolean completed = completedPercent == 100;
                this.Completed = completed;
            }
            catch(Exception e){
                // PercentDone might not exist so set to false
            	this.Completed = false;
            }
            this.COMMENTS = child.getAttribute("COMMENTS");
            this.COMMENTSTYPE = child.getAttribute("COMMENTSTYPE");
            
            String priority =  child.getAttribute("PRIORITY");
            if(priority != "")
            {
            	this.PRIORITY = new Integer(priority);
            }
            
            this.Level = parentLevel + 1;

	}

	public Task(Cursor c) {
        int IdColumn = c.getColumnIndex("Id");
        int TitleColumn = c.getColumnIndex("Title");
        int CompletedColumn = c.getColumnIndex("Completed");
        int COMMENTSColumn = c.getColumnIndex("COMMENTS");
        int COMMENTSTYPEColumn = c.getColumnIndex("COMMENTSTYPE");
        int PRIORITYColumn = c.getColumnIndex("PRIORITY");
        
        
        
		this.Id = c.getInt(IdColumn);
		this.Title = c.getString(TitleColumn);
        int completedint = new Integer(c.getString(CompletedColumn));
        this.Completed = completedint == 1;
        this.COMMENTS = c.getString(COMMENTSColumn);
        this.COMMENTSTYPE = c.getString(COMMENTSTYPEColumn);
        this.PRIORITY = new Integer(c.getString(PRIORITYColumn));
	}

	public void SaveToDatabase(SQLiteDatabase database, String tableName) {
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
        serializer.attribute(null, "COMMENTS", this.COMMENTS);
        serializer.attribute(null, "COMMENTSTYPE", this.COMMENTSTYPE);
        serializer.attribute(null, "PRIORITY", this.PRIORITY.toString());
	}
}
