package melectric.tododroid;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import melectric.todoxmlparser.Task;

public class TodoDroidDatabase {
	public String dbName;
	public Activity Activity;
	public String TaskTable;
	public String AttributeTable;
	public String XmlNodeTable;
	public String XmlNodeAttributeTable;
	public String XmlProjectAttributeTable;
	public String TodoDroidSettingsTable;
	
	private SQLiteDatabase myDB;
	
	public TodoDroidDatabase(Activity context,
			String databaseName,
			String TaskTable,
			String AttributeTable,
			String XmlNodeTable,
			String XmlNodeAttributeTable,
			String XmlProjectAttributeTable,
			String TodoDroidSettingsTable){
		this.Activity = context;
		this.dbName = databaseName;
		this.TaskTable = TaskTable;
		this.AttributeTable = AttributeTable;
		this.XmlNodeTable = XmlNodeTable;
		this.XmlNodeAttributeTable = XmlNodeAttributeTable;
		this.XmlProjectAttributeTable = XmlProjectAttributeTable;
		this.TodoDroidSettingsTable = TodoDroidSettingsTable;
	}
	
	public List<Task> GetTasks()
	{
		List<Task> tasks =  new ArrayList<Task>();
        myDB = Activity.openOrCreateDatabase("TaskListDatabase", Activity.MODE_PRIVATE, null);
        Cursor cursor = myDB.rawQuery("SELECT * FROM t_Tasks" , null);
        
        cursor.moveToFirst();
        if (cursor != null) {
        	do {
        		Task task = new Task(cursor, myDB);
        		tasks.add(task);
            }while(cursor.moveToNext());
        }
        
		return tasks;
	}
	
	public SQLiteDatabase PrepareBlankDatabase() {
		
		Activity.deleteDatabase(dbName);
		SQLiteDatabase database = Activity.openOrCreateDatabase(dbName, Activity.MODE_PRIVATE, null);
		
		database.execSQL("CREATE TABLE IF NOT EXISTS " + TaskTable
		    + " (Id INT(3), Title VARCHAR, ParentId INT(3), Level INT(3), Completed INT(3), COMMENTS VARCHAR, COMMENTSTYPE VARCHAR, PRIORITY INT(3), UNUSEDATTRIBUTES VARCHAR);");
		    
		database.execSQL("CREATE TABLE IF NOT EXISTS " + AttributeTable
		        + " (TaskId INT(3), Name VARCHAR, Value VARCHAR);");
		
		database.execSQL("CREATE TABLE IF NOT EXISTS " + XmlNodeTable
		        + " (Name VARCHAR);");
		
		database.execSQL("CREATE TABLE IF NOT EXISTS " + XmlNodeAttributeTable
		        + " (NodeName VARCHAR, Name VARCHAR, Value VARCHAR);");
		
		database.execSQL("CREATE TABLE IF NOT EXISTS " + XmlProjectAttributeTable
		        + " (Name VARCHAR, Value VARCHAR);");
		
		database.execSQL("CREATE TABLE IF NOT EXISTS " + TodoDroidSettingsTable
		        + " (Name VARCHAR, Value VARCHAR);");
		return database;
	}
}
