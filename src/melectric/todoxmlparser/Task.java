package melectric.todoxmlparser;

import java.util.List;

import org.w3c.dom.Element;

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
            this.Level = parentLevel + 1;

	}

	public Task(Cursor c) {
        int IdColumn = c.getColumnIndex("Id");
        int TitleColumn = c.getColumnIndex("Title");
        int CompletedColumn = c.getColumnIndex("Completed");
        
		this.Id = c.getInt(IdColumn);
		this.Title = c.getString(TitleColumn);
        int completedint = new Integer(c.getString(CompletedColumn));
        this.Completed = completedint == 1;
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
        database.insert(tableName, "", args);
		
	}
}
