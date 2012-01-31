package melectric.tododroid;

import java.util.Set;

import melectric.todoxmlparser.Task;

import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.R;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This is a very simple adapter that provides very basic tree view with a
 * check-box and simple item description.
 */
class SimpleStandardAdapter extends AbstractTreeViewAdapter<Long> {
    private final Context mContext;
    private final SQLiteDatabase myDB;
    
    public SimpleStandardAdapter(final TodoDroid treeViewListDemo,
            final Set<Long> selected,
            final TreeStateManager<Long> treeStateManager,
            final int numberOfLevels, Context context) {
        super(treeViewListDemo, treeStateManager, numberOfLevels);
        
        mContext = context;
        this.selected = selected;
        
        myDB = mContext.openOrCreateDatabase("TaskListDatabase", Context.MODE_PRIVATE, null);

    }

    private final Set<Long> selected;

    private final OnCheckedChangeListener onCheckedChange = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
            final Long id = (Long) buttonView.getTag();
            changeSelected(isChecked, id);
        }
    };

    private void changeSelected(final boolean isChecked, final Long id) {
        if (isChecked) {
            selected.add(id);
        } else {
            selected.remove(id);
        }
    }

    private String getDescription(final long id) {
        String Title = "";

        try {
           Cursor c = myDB.rawQuery("SELECT * FROM t_Tasks WHERE Id = " + id, null);

            int Column2 = c.getColumnIndex("Title");

            c.moveToFirst();
            if (c != null) {
                do {
                    Title = c.getString(Column2);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e("Error", "Error", e);
        }

        return Title;
    }
    
    private Task getTask(final long id) {
        Task task = new Task();

        try {
           Cursor c = myDB.rawQuery("SELECT * FROM t_Tasks WHERE Id = " + id, null);

            int Column2 = c.getColumnIndex("Title");
            int CompletedColumn = c.getColumnIndex("Completed");

            c.moveToFirst();
            if (c != null) {
                do {
                    task.Title = c.getString(Column2);
                    int completedint = new Integer(c.getString(CompletedColumn));
                    task.Completed = completedint == 1;
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e("Error", "Error", e);
        }

        return task;
    }

    @Override
    public View getNewChildView(final TreeNodeInfo<Long> treeNodeInfo) {
        final LinearLayout viewLayout = (LinearLayout) getActivity()
                .getLayoutInflater().inflate(R.layout.demo_list_item, null);
        return updateView(viewLayout, treeNodeInfo);
    }

    @Override
    public LinearLayout updateView(final View view,
            final TreeNodeInfo<Long> treeNodeInfo) 
    {
        
        final LinearLayout viewLayout = (LinearLayout) view;
        final TextView descriptionView = (TextView) viewLayout.findViewById(R.id.demo_list_item_description);
        
        Task task = getTask(treeNodeInfo.getId());
        descriptionView.setText(task.Title);
        final CheckBox box = (CheckBox) viewLayout.findViewById(R.id.demo_list_checkbox);
        box.setTag(treeNodeInfo.getId());
        box.setChecked(task.Completed);
        box.setVisibility(View.VISIBLE);
        
        if (treeNodeInfo.isWithChildren()) {
            // box.setVisibility(View.GONE);
        } else {
            
        }
        box.setOnCheckedChangeListener(onCheckedChange);
        return viewLayout;
    }

    @Override
    public void handleItemClick(final View view, final Object id) {
        final Long longId = (Long) id;
        final TreeNodeInfo<Long> info = getManager().getNodeInfo(longId);
        if (info.isWithChildren()) {
            super.handleItemClick(view, id);
        } else {
            final ViewGroup vg = (ViewGroup) view;
            final CheckBox cb = (CheckBox) vg
                    .findViewById(R.id.demo_list_checkbox);
            cb.performClick();
        }
    }

    @Override
    public long getItemId(final int position) {
        return getTreeId(position);
    }
}