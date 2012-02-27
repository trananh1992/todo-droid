package melectric.tododroid;

import melectric.todoxmlparser.Task;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.EditText;

public class TaskDetails extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.taskdetails);
         
        int taskId = getIntent().getIntExtra("TaskId", 0);

        SQLiteDatabase myDB = this.openOrCreateDatabase("TaskListDatabase", MODE_PRIVATE, null);
        Cursor c = myDB.rawQuery("SELECT * FROM t_Tasks WHERE Id = " + taskId, null);

        c.moveToFirst();
        if (c != null && c.getCount() > 0) {
            do {
            	Task task = new Task(c, myDB);
            	EditText title =(EditText) findViewById(R.id.TitleTextBox);
            	title.setText(task.Title);
               
            } while (c.moveToNext());
        }  
    }
   


}
