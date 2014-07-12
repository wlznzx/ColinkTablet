package cn.colink.commumication.db;

import java.util.ArrayList;
import java.util.List;

public abstract class SubjectChatLogDB {
	private List<ObserverForChatLogDB> observers = new ArrayList<ObserverForChatLogDB>();

    public void attach(ObserverForChatLogDB observer){
        observers.add(observer);
    }

    public void Detach(ObserverForChatLogDB observer){
        observers.remove(observer);
    }
    
    public void notifyChange(String pTableName){
        for (ObserverForChatLogDB observer : observers){
        	observer.onChange(pTableName);
        }
    }
}

