package melectric.todoxmlparser;

import java.util.List;

public class Task {
    public Integer Id;
    public String Title;
    public Integer ParentId;
    public List<Task> Children;
    public Integer Level;
    public boolean Completed;
}
