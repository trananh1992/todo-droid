package melectric.tododroid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xmlpull.v1.XmlSerializer;

import melectric.todoxmlparser.BaseFeedParser;
import melectric.todoxmlparser.Task;
import melectric.todoxmlparser.Utilities;
import pl.polidea.treeview.InMemoryTreeStateManager;
import pl.polidea.treeview.R;
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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
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

/**
 * Demo activity showing how the tree view can be used.
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

    private final String MY_DATABASE_NAME = "TaskListDatabase";
    private final String MY_DATABASE_TABLE = "t_Tasks";
    private final String MY_DATABASE_ATTRIBUTETABLE = "t_Attributes";
    private final String MY_DATABASE_XMLNODETABLE = "t_XmlNode";
    private final String MY_DATABASE_XMLNODEATTRIBUTETABLE = "t_XmlNodeAttribute";
    private final String MY_DATABASE_XMLPROJECTATTRIBUTETABLE = "t_XmlProjectAttribute";
    
    private SQLiteDatabase myDB = null;
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Activity = this;
        SavedInstanceState = savedInstanceState;

        try {
        	SelectFile();
        } catch (Exception e) {
            e.printStackTrace();
            //TODO: Load XML Failed
            new AlertDialog.Builder(Activity)
            .setTitle("Error")
            .setMessage("Sorry An Error Has Occured.")
            .setPositiveButton(android.R.string.yes, null)
                .setNegativeButton(android.R.string.no, null).show();
        }
    }

    @SuppressWarnings("unchecked")
	private void populateTree(SQLiteDatabase myDB) {
        List<Integer> titles = new ArrayList<Integer>(); 
        myDB = this.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
        Cursor cursor = myDB.rawQuery("SELECT * FROM t_Tasks" , null);
        
        int Column1 = cursor.getColumnIndex("Id");               
        
        // Check if our result was valid.
        cursor.moveToFirst();
        if (cursor != null) {
         // Loop through all Results
         do {
             Integer Id = cursor.getInt(Column1);
             titles.add(Id);
         }while(cursor.moveToNext());
        }
        
        TreeType newTreeType = null;
        boolean newCollapsible;
        if (SavedInstanceState == null) {
            manager = new InMemoryTreeStateManager<Long>();
            final TreeBuilder<Long> treeBuilder = new TreeBuilder<Long>(manager);
            treeBuilder.clear();
            for (int i = 0; i < titles.size(); i++) {
                try{
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
    }

    private void importXML() throws Exception {
        // final ProgressDialog dialog = ProgressDialog.show(this, "", "Loading. Please wait...", true);
        
        
        SQLiteDatabase database = null;
        try {
        	this.deleteDatabase(MY_DATABASE_NAME);
            database = this.openOrCreateDatabase(MY_DATABASE_NAME, MODE_PRIVATE, null);
            
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MY_DATABASE_TABLE
                + " (Id INT(3), Title VARCHAR, ParentId INT(3), Level INT(3), Completed INT(3), COMMENTS VARCHAR, COMMENTSTYPE VARCHAR, PRIORITY INT(3), UNUSEDATTRIBUTES VARCHAR);");
                
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MY_DATABASE_ATTRIBUTETABLE
                    + " (TaskId INT(3), Name VARCHAR, Value VARCHAR);");
            
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MY_DATABASE_XMLNODETABLE
                    + " (Name VARCHAR);");
            
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MY_DATABASE_XMLNODEATTRIBUTETABLE
                    + " (NodeName VARCHAR, Name VARCHAR, Value VARCHAR);");
            
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MY_DATABASE_XMLPROJECTATTRIBUTETABLE
                    + " (Name VARCHAR, Value VARCHAR);");
        	
        	// Environment.getExternalStorageDirectory()+"/TaskList.tdl"
            List<Task> tasks = BaseFeedParser.parse2(mPath.getAbsolutePath()+"//" + mChosenFile, database);
                
                        
                for (int i = 0; i < tasks.size(); i++) {
                	Task task = tasks.get(i);
                	task.SaveToDatabase(database, MY_DATABASE_TABLE, MY_DATABASE_ATTRIBUTETABLE);                 
                }
            }
           catch(Exception e) {
               Log.e("Error", "Error", e);
               throw e;
           } finally {
               if (database != null)
                   database.close();
           }
        // dialog.cancel();
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
        switch (newTreeType) {
        case SIMPLE:
            treeView.setAdapter(simpleAdapter);
            break;
        default:
            treeView.setAdapter(simpleAdapter);
        }
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
                e.printStackTrace();
                //TODO: Load XML Failed
                new AlertDialog.Builder(Activity)
	                .setTitle("Error")
	                .setMessage("Sorry An Error Has Occured.")
	                .setPositiveButton(android.R.string.yes, null)
	                .setNegativeButton(android.R.string.no, null).show();
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
                        
                        Cursor projectAttributesCursor = myDB.rawQuery("SELECT * FROM " + MY_DATABASE_XMLPROJECTATTRIBUTETABLE, null);
                        int nameColumn = projectAttributesCursor.getColumnIndex("Name");
                        int valueColumn = projectAttributesCursor.getColumnIndex("Value");
                        projectAttributesCursor.moveToFirst();
                        if (projectAttributesCursor != null && projectAttributesCursor.getCount() > 0) {
                            do {
                            	serializer.attribute(null, projectAttributesCursor.getString(nameColumn), projectAttributesCursor.getString(valueColumn));
                            } while (projectAttributesCursor.moveToNext());
                        }
                                                
                        SQLiteDatabase myDB = this.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
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
                    	
                        new AlertDialog.Builder(Activity)
                        .setTitle("Success")
                        .setMessage("The File has been created")
                        .setPositiveButton(android.R.string.yes, null)
                            .setNegativeButton(android.R.string.no, null).show();
                } catch (Exception e) {
                        Log.e("Exception","");
                        
                        new AlertDialog.Builder(Activity)
	                        .setTitle("Sorry!")
	                        .setMessage("Error occurred while creating xml file")
	                        .setPositiveButton(android.R.string.yes, null)
	                        .setNegativeButton(android.R.string.no, null).show();
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
            manager.removeNodeRecursively(id);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    private void SelectFile()
    {
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
    				} catch (Exception e) {
    	                e.printStackTrace();
    	                //TODO: Load XML Failed
    	                new AlertDialog.Builder(Activity)
    	                .setTitle("Error")
    	                .setMessage("Sorry An Error Has Occured.")
    	                .setPositiveButton(android.R.string.yes, null)
    	                    .setNegativeButton(android.R.string.no, null).show();
    				}

                  populateTree(myDB);
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
}

