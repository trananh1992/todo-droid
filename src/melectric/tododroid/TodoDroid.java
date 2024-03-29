package melectric.tododroid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import melectric.todoxmlparser.BaseFeedParser;
import melectric.todoxmlparser.Task;

import org.xmlpull.v1.XmlSerializer;

import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.TreeBuilder;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import pl.polidea.treeview.TreeViewList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Main Tree Activity
 * 
 */
public class TodoDroid extends Activity {
    private enum TreeType implements Serializable {
        SIMPLE,
        FANCY
    }

    private final Set<Long> selected = new HashSet<Long>();

    private static final String TAG = TodoDroid.class.getSimpleName();
    private TreeViewList treeView;

    // TODO: Set the correct value for the number of levels
    private static final int LEVEL_NUMBER = 40;

    protected static Context Activity = null;
    protected static Bundle SavedInstanceState = null;
    private TreeStateManager<Long> manager = null;
    private SimpleStandardAdapter simpleAdapter;
    private TreeType treeType;
    private boolean collapsible;
    private int highestTaskId;

    private TodoDroidDatabase db;
    
    private final String MY_DATABASE_NAME = "TaskListDatabase";
    private final String MY_DATABASE_TABLE = "t_Tasks";
    private final String MY_DATABASE_ATTRIBUTETABLE = "t_Attributes";
    private final String MY_DATABASE_XMLNODETABLE = "t_XmlNode";
    private final String MY_DATABASE_XMLNODEATTRIBUTETABLE = "t_XmlNodeAttribute";
    private final String MY_DATABASE_XMLPROJECTATTRIBUTETABLE = "t_XmlProjectAttribute";
    private final String MY_DATABASE_TODODROIDSETTINGS = "t_Settings";
    
    private SQLiteDatabase myDB = null;
    
    /**
     * Check if the database exist
     * 
     * @return true if it exists, false if it doesn't
     */
    private boolean DataBaseExists() {
        SQLiteDatabase checkDB = null;
        boolean exists = false;
        try {
            checkDB = this.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
            Cursor c = checkDB.rawQuery("SELECT * FROM " + MY_DATABASE_TODODROIDSETTINGS, null);
            int NameColumn = c.getColumnIndex("Name");     
            int ValueColumn = c.getColumnIndex("Value");     
            
            c.moveToFirst();
            if (c != null) {
             do {
            	 if(c.getString(NameColumn).equals("mChosenFile"))
            	 {
            		 mChosenFile = c.getString(ValueColumn);
            	 }
            	 if(c.getString(NameColumn).equals("mPath"))
            	 {
            		 mPath = new File(c.getString(ValueColumn));
            	 }
            	 
            	 exists = true;
             }while(c.moveToNext());
            }
            checkDB.close();
        } catch (SQLiteException e) {
            // database doesn't exist yet.
        	Log.d(TAG, "Database Not Found");
        }
        return exists;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		db = new TodoDroidDatabase(this,
				MY_DATABASE_NAME,
				MY_DATABASE_TABLE,
				MY_DATABASE_ATTRIBUTETABLE,
				MY_DATABASE_XMLNODETABLE,
				MY_DATABASE_XMLNODEATTRIBUTETABLE,
				MY_DATABASE_XMLPROJECTATTRIBUTETABLE,
				MY_DATABASE_TODODROIDSETTINGS
				);        
        Activity = this;
        SavedInstanceState = savedInstanceState;

        try {
        	boolean dbExists = DataBaseExists();
        	if(dbExists)
        	{
        		populateTree();
                TdlFileObserver fileOb = new TdlFileObserver(mPath.getAbsolutePath() + "//" +  mChosenFile + "//");
                fileOb.startWatching();

        	}
        	else
        	{
        		SelectFile();
        	}
        } catch (Exception e) {
        	writeErrorToFile(e, "Load File Error");
        }
    }

    @SuppressWarnings("unchecked")
	private void populateTree() {
        List<Integer> titles = new ArrayList<Integer>(); 
        
        List<Task> tasks = db.GetTasks();
        highestTaskId = 0;
        for(Task task : tasks)
        {
        	if(task.Id > highestTaskId)
			{
        		highestTaskId = task.Id;
			}
        	if(!task.Completed)
        	{
                titles.add(task.Id);
            }
        }
        
        TreeType newTreeType = null;
        boolean newCollapsible;
        if (SavedInstanceState == null) {
            manager = new InMemoryTreeStateManager<Long>();
            final TreeBuilder<Long> treeBuilder = new TreeBuilder<Long>(manager);
            treeBuilder.clear();
            for (int i = 0; i < titles.size(); i++) {
                try{
                	myDB = this.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
                    Cursor c = myDB.rawQuery("SELECT * FROM t_Tasks WHERE Id = " + titles.get(i).longValue(), null);
   
                    int levelColumn = c.getColumnIndex("Level");
                    c.moveToFirst();
                    treeBuilder.sequentiallyAddNextNode(titles.get(i).longValue(), c.getInt(levelColumn));
                }
                catch(Exception e)
                {
                    Log.e("Error", "Error", e);
                }
            }
       
            Log.d(TAG, manager.toString());
            newTreeType = TreeType.SIMPLE;
            newCollapsible = true;
        } else {
            manager = (TreeStateManager<Long>) SavedInstanceState
                    .getSerializable("treeManager");
            newTreeType = (TreeType) SavedInstanceState
                    .getSerializable("treeType");
            newCollapsible = SavedInstanceState.getBoolean("collapsible");
        }
        if (myDB != null){
           myDB.close();
        } 
        setContentView(R.layout.main_demo);
        treeView = (TreeViewList) findViewById(R.id.mainTreeView);
        simpleAdapter = new SimpleStandardAdapter(this, selected, manager, LEVEL_NUMBER, this);
        setTreeAdapter(newTreeType);
        setCollapsible(newCollapsible);
        registerForContextMenu(treeView);
        manager.collapseChildren(null);
    }

    private void importXML() throws Exception {
        SQLiteDatabase database = null;
        try {
	        	database = prepareBlankDatabase();
	        	
	        	database.beginTransaction();
	        	
			    ContentValues args = new ContentValues();
	        	args.put("Name", "mChosenFile");
	        	args.put("Value", mChosenFile);
	        	database.insert(MY_DATABASE_TODODROIDSETTINGS, "", args);
	        	
			    args = new ContentValues();
	        	args.put("Name", "mPath");
	        	args.put("Value", mPath.getAbsolutePath());
	        	database.insert(MY_DATABASE_TODODROIDSETTINGS, "", args);
	        	
	            List<Task> tasks = BaseFeedParser.parse2(mPath.getAbsolutePath()+"//" + mChosenFile, database);
	                
	            int numberOfTasks = tasks.size();
	            
	            for (int i = 0; i < numberOfTasks; i++) {
	            	Log.d(TAG, "Adding task To Database:" + i + " of " + numberOfTasks);
	            	Task task = tasks.get(i);
	            	task.SaveToDatabase(database, MY_DATABASE_TABLE, MY_DATABASE_ATTRIBUTETABLE);                 
	            }
	            database.setTransactionSuccessful();
	            
            }
           catch(Exception e) {
               Log.e("Error", "Error", e);
               throw e;
           } finally {
               if (database != null)
               {
            	   database.endTransaction();
                   database.close();
               }
           }
    }


	private SQLiteDatabase prepareBlankDatabase() {		
		return db.PrepareBlankDatabase();
	}
 
    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        outState.putSerializable("treeManager", manager);
        outState.putSerializable("treeType", treeType);
        outState.putBoolean("collapsible", this.collapsible);
        super.onSaveInstanceState(outState);
    }

    protected final void setTreeAdapter(final TreeType newTreeType) {
        this.treeType = newTreeType;
        treeView.setAdapter(simpleAdapter);

    }

    protected final void setCollapsible(final boolean newCollapsible) {
        this.collapsible = newCollapsible;
        treeView.setCollapsible(this.collapsible);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.fancy_menu_item) {
            try {
            	
            	SelectFile();
            } catch (Exception e) {
                writeErrorToFile(e, "Load File Error");
            }
        } else if (item.getItemId() == R.id.export_menu_item) {
            Export();
        } else if (item.getItemId() == R.id.expand_all_menu_item) {
            manager.expandEverythingBelow(null);
        } else if (item.getItemId() == R.id.collapse_all_menu_item) {
            manager.collapseChildren(null);
        } else {
            return false;
        }
        return true;
    }

    private void Export() {
    	//create a new file called "new.xml" in the SD card
        File newxmlfile = new File(mPath.getAbsolutePath()+"//" + mChosenFile);
        try{
                newxmlfile.createNewFile();
        }catch(IOException e){
                Log.e("IOException", "exception in createNewFile() method");
        }
        //we have to bind the new file with a FileOutputStream
        FileOutputStream fileos = null;        
        try{
                fileos = new FileOutputStream(newxmlfile);
        }catch(FileNotFoundException e){
                Log.e("FileNotFoundException", "can't create FileOutputStream");
        }
        //we create a XmlSerializer in order to write xml data
        XmlSerializer serializer = Xml.newSerializer();
        try {
                //we set the FileOutputStream as output for the serializer, using UTF-8 encoding
                        serializer.setOutput(fileos, "UTF-8");
                        //Write <?xml declaration with encoding (if encoding not null) and standalone flag (if standalone not null)
                        serializer.startDocument("windows-1252", Boolean.valueOf(true));
                        //set indentation option
                        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                        //start a tag called "root"
                        serializer.startTag(null, "TODOLIST");
                        SQLiteDatabase myDB = this.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
                        Cursor projectAttributesCursor = myDB.rawQuery("SELECT * FROM " + MY_DATABASE_XMLPROJECTATTRIBUTETABLE, null);
                        int nameColumn = projectAttributesCursor.getColumnIndex("Name");
                        int valueColumn = projectAttributesCursor.getColumnIndex("Value");
                        projectAttributesCursor.moveToFirst();
                        if (projectAttributesCursor != null && projectAttributesCursor.getCount() > 0) {
                            do {
                            	serializer.attribute(null, projectAttributesCursor.getString(nameColumn), projectAttributesCursor.getString(valueColumn));
                            } while (projectAttributesCursor.moveToNext());
                        }
                                                
                        
                        Cursor c = myDB.rawQuery("SELECT * FROM t_Tasks WHERE ParentId  IS NULL", null);

                        c.moveToFirst();
                        if (c != null && c.getCount() > 0) {
                            do {
                            	Task task = new Task(c, myDB);
                            	
                               	serializer.startTag(null, "TASK");
                                task.SaveAttributesToFile(serializer);
                                CreateChildTask(myDB, serializer, task.Id);
                                serializer.endTag(null, "TASK");                
                            } while (c.moveToNext());
                        }
                        
                        Cursor xmlNodeCursor = myDB.rawQuery("SELECT * FROM " + MY_DATABASE_XMLNODETABLE, null);
                        int nodeNameColumn = xmlNodeCursor.getColumnIndex("Name");
                        xmlNodeCursor.moveToFirst();
                        if (xmlNodeCursor != null && xmlNodeCursor.getCount() > 0) {
                            do {
                            	String nodeName = xmlNodeCursor.getString(nodeNameColumn);
                            	serializer.startTag(null,  nodeName);
                            	
                            	Cursor xmlNodeAttributeCursor = myDB.rawQuery("SELECT * FROM " + MY_DATABASE_XMLNODEATTRIBUTETABLE + " WHERE NodeName = '" + nodeName + "'", null);
                            	int nodeAttributeNameColumn = xmlNodeAttributeCursor.getColumnIndex("Name");
                            	int nodeAttributeValueColumn = xmlNodeAttributeCursor.getColumnIndex("Value");
                            	xmlNodeAttributeCursor.moveToFirst();
                                if (xmlNodeAttributeCursor != null && xmlNodeAttributeCursor.getCount() > 0) {
                                    do {
                            	serializer.attribute(null, xmlNodeAttributeCursor.getString(nodeAttributeNameColumn), xmlNodeAttributeCursor.getString(nodeAttributeValueColumn));
                                    } while (xmlNodeAttributeCursor.moveToNext());
                                }
                            	serializer.endTag(null,  nodeName);
                            } while (xmlNodeCursor.moveToNext());
                        }
                               
                        serializer.endTag(null, "TODOLIST");
                        serializer.endDocument();
                        //write xml data into the FileOutputStream
                        serializer.flush();
                        //finally we close the file stream
                        fileos.close();
                        //Notify the User
                        Toast.makeText(this, "TaskList Saved To File", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    writeErrorToFile(e, "Save Tasklist To File Failed");
                }
}

	private void CreateChildTask(SQLiteDatabase myDB, XmlSerializer serializer, Integer parentId) throws IllegalArgumentException, IllegalStateException, IOException {
        Cursor c = myDB.rawQuery("SELECT * FROM t_Tasks WHERE ParentId = " + parentId, null);      
        
        c.moveToFirst();
        if (c != null && c.getCount() > 0) {
            do {
            	Task task = new Task(c, myDB);
                
            	serializer.startTag(null, "TASK");
                task.SaveAttributesToFile(serializer);
                CreateChildTask(myDB, serializer, task.Id);
                serializer.endTag(null, "TASK");
                
            } while (c.moveToNext());
        }
	}

	@Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
            final ContextMenuInfo menuInfo) {
        final AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
        final long id = adapterInfo.id;
        final TreeNodeInfo<Long> info = manager.getNodeInfo(id);
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.context_menu, menu);
        if (info.isWithChildren()) {
            if (info.isExpanded()) {
                menu.findItem(R.id.context_menu_expand_item).setVisible(false);
                menu.findItem(R.id.context_menu_expand_all).setVisible(false);
            } else {
                menu.findItem(R.id.context_menu_collapse).setVisible(false);
            }
        } else {
            menu.findItem(R.id.context_menu_expand_item).setVisible(false);
            menu.findItem(R.id.context_menu_expand_all).setVisible(false);
            menu.findItem(R.id.context_menu_collapse).setVisible(false);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        final long id = info.id;
        if (item.getItemId() == R.id.context_menu_collapse) {
            manager.collapseChildren(id);
            return true;
        } else if (item.getItemId() == R.id.context_menu_expand_all) {
            manager.expandEverythingBelow(id);
            return true;
        } else if (item.getItemId() == R.id.context_menu_expand_item) {
            manager.expandDirectChildren(id);
            return true;
        } else if (item.getItemId() == R.id.context_menu_delete) {
        	  myDB = TodoDroid.Activity.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
        	  String strFilter = "Id=" + id;
        	  myDB.delete(MY_DATABASE_TABLE, strFilter, null);
            manager.removeNodeRecursively(id);
            return true;
        } else if (item.getItemId() == R.id.context_menu_addchild) {
        	highestTaskId = highestTaskId + 1;
        	
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Add Task");
            alert.setMessage("Name:");

            // Set an EditText view to get user input 
            final EditText input = new EditText(this);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              String value = input.getText().toString();
              // Do something with value!
          	  int level = 0;
          	  myDB = TodoDroid.Activity.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
              Cursor c = myDB.rawQuery("SELECT * FROM " +  MY_DATABASE_TABLE+ " WHERE Id = " + id, null);      
          	  int LevelColumn = c.getColumnIndex("Level");
              c.moveToFirst();
              if (c != null && c.getCount() > 0) {
                  do {            	
                  	level = c.getInt(LevelColumn);
                  } while (c.moveToNext());
              }
              
              ContentValues args = new ContentValues();
              args.put("Id", highestTaskId);
              args.put("Title", value);
              args.put("ParentId", id);
              args.put("Level", level + 1);
              args.put("Completed", 0);
              args.put("COMMENTS", "");
              args.put("COMMENTSTYPE", "");
              args.put("PRIORITY", 5); 
              myDB.insert(MY_DATABASE_TABLE, "", args);
              
              manager.addBeforeChild(id, (long)highestTaskId, null);
              }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
              }
            });

            alert.show();

            return true;
        }  else if (item.getItemId() == R.id.context_menu_addbelow) {
        	highestTaskId = highestTaskId + 1;
        	
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Add Task");
            alert.setMessage("Name:");

            // Set an EditText view to get user input 
            final EditText input = new EditText(this);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              String value = input.getText().toString();
              // Do something with value!
          	  int level = 0;
          	  int ParentId = 0;
          	  myDB = TodoDroid.Activity.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
              Cursor c = myDB.rawQuery("SELECT * FROM " +  MY_DATABASE_TABLE+ " WHERE Id = " + id, null);      
          	  int LevelColumn = c.getColumnIndex("Level");
          	int ParentIdColumn = c.getColumnIndex("ParentId");
              c.moveToFirst();
              if (c != null && c.getCount() > 0) {
                  do {            	
                  	level = c.getInt(LevelColumn);
                  	ParentId = c.getInt(ParentIdColumn);
                  } while (c.moveToNext());
              }
              
              ContentValues args = new ContentValues();
              args.put("Id", highestTaskId);
              args.put("Title", value);
              if(ParentId == 0)
              {
            	  args.putNull("ParentId");
              }
              else
              {
            	  args.put("ParentId", ParentId);
              }
              
              
              args.put("Level", level);
              args.put("Completed", 0);
              args.put("COMMENTS", "");
              args.put("COMMENTSTYPE", "");
              args.put("PRIORITY", 5); 
              myDB.insert(MY_DATABASE_TABLE, "", args);
              if(ParentId == 0)
              {
                  manager.addAfterChild(null, (long)highestTaskId, id);
              }
              else
              {
                  manager.addAfterChild((long)ParentId, (long)highestTaskId, id);
              }
              }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
              }
            });

            alert.show();

            return true;
        }  else if (item.getItemId() == R.id.context_menu_view) {
            Intent myIntent = new Intent(this,TaskDetails.class);
            myIntent.putExtra("TaskId", id);           
            startActivityForResult(myIntent, 0);
            return true;
        }  else if (item.getItemId() == R.id.context_menu_edit) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Add Task");
            alert.setMessage("Name:");

            // Set an EditText view to get user input 
            final EditText input = new EditText(this);
            myDB = TodoDroid.Activity.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
            Cursor c = myDB.rawQuery("SELECT * FROM t_Tasks WHERE Id = " + id, null);

            int Column2 = c.getColumnIndex("Title");
            String Title = "";
            c.moveToFirst();
            if (c != null) {
                do {
                    Title = c.getString(Column2);
                } while (c.moveToNext());
            }
            input.setText(Title);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              String value = input.getText().toString();
              // Do something with value!
          		  String strFilter = "Id=" + id;
	              ContentValues args = new ContentValues();
	              args.put("Title", value);
	              
	              myDB.update("t_Tasks", args, strFilter, null);  
	              manager.refresh();
              }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
              }
            });

            alert.show();

            return true;
        }
        else {
            return super.onContextItemSelected(item);
        }
		
    }

    
    
    private void SelectFile()
    {
    	mChosenFile = "";
    	mPath = new File(Environment.getExternalStorageDirectory() + "//");;
    	loadFileList();
    	showDialog(DIALOG_LOAD_FILE);
    }
    //In an Activity
    private String[] mFileList;
    private File mPath = new File(Environment.getExternalStorageDirectory() + "//");
    private String mChosenFile;
    private static final String FTYPE = ".tdl";    
    private static final int DIALOG_LOAD_FILE = 1000;

    private void loadFileList(){
      try{
         mPath.mkdirs();
      }
      catch(SecurityException e){
         Log.e(TAG, "unable to write on the sd card " + e.toString());
      }
      if(mPath.exists()){
         FilenameFilter filter = new FilenameFilter(){
             public boolean accept(File dir, String filename){
                 File sel = new File(dir, filename);
                 return filename.contains(FTYPE) || sel.isDirectory();
             }
         };
         mFileList = mPath.list(filter);
      }
      else{
        mFileList= new String[0];
      }
    }

    protected Dialog onCreateDialog(int id){
      Dialog dialog = null;
      AlertDialog.Builder builder = new Builder(this);

      switch(id){
      case DIALOG_LOAD_FILE:
       builder.setTitle("Choose your file");
       if(mFileList == null){
         Log.e(TAG, "Showing file picker before loading the file list");
         dialog = builder.create();
         return dialog;
       }
         builder.setItems(mFileList, new DialogInterface.OnClickListener(){
           public void onClick(DialogInterface dialog, int which){
              mChosenFile = mFileList[which];
              
              if(!mChosenFile.endsWith(".tdl"))
              {        
            	  if(mChosenFile == "Go Back")
            	  {
            		//TODO: Bug if you go back too many levels to a non existant directory
            		 int lastindex = mPath.getAbsolutePath().lastIndexOf("/");
            		 mChosenFile = mPath.getAbsolutePath().substring(0, lastindex);
            		 
            		 mPath = new File(mChosenFile); 
            	  }
            	  else
            	  {
            		  mPath = new File(mPath.getAbsolutePath() + "//" +  mChosenFile + "//"); 
            	  }
            	  
                  FilenameFilter filter = new FilenameFilter(){
                      public boolean accept(File dir, String filename){
                          File sel = new File(dir, filename);
                          return filename.contains(FTYPE) || sel.isDirectory();
                      }
                  };
                  mFileList = mPath.list(filter);
                  mFileList = (String[]) prepend(mFileList, "Go Back");
                 // mFileList = (String[]) ArrayUtils.addAll(mPath.list(filter), mPath.list(filter));
            	  
            	  onCreateDialog(DIALOG_LOAD_FILE);
              }
              else
              {
            	  //File Selected
                  try {
    				importXML();
    				populateTree();
    				} catch (Exception e) {
    	                writeErrorToFile(e, "Load File Error");
    				}
              }

           }
          });
      break;
      }
      dialog = builder.show();
      return dialog;
     } 
      
    public static Object[]  prepend(Object[] oldArray, Object  o)
      {
        Object[] newArray = (Object[])Array.newInstance(oldArray.getClass().getComponentType(), oldArray.length + 1);

        System.arraycopy(oldArray, 0, newArray, 1, oldArray.length);

        newArray[0] = o;

        return newArray;
      }
    
    private void writeErrorToFile(Exception e, String message) {
        try {
        	final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            e.printStackTrace(printWriter);
            String stacktrace = result.toString();
          
        	File root = new File(Environment.getExternalStorageDirectory(), "TODODROID");
            if (!root.exists()) {
                root.mkdirs();
            }
            File errorFile = new File(root, "ErrorLog.txt");
            FileWriter writer = new FileWriter(errorFile);
            writer.append(stacktrace);
            writer.flush();
            writer.close();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Saved Error Log To SD Card TODODROID Folder", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}

