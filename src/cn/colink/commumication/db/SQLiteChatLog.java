package cn.colink.commumication.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import cn.colink.commumication.model.ModelChatLog;

public class SQLiteChatLog extends SQLiteDALBase{
//	private String TableName;
	
	public SQLiteChatLog(Context p_Context,String pTableName) {
		super(p_Context,new SQLiteHelperChatLog(p_Context));
//		TableName = pTableName;
	}
	/**
	 * 插入表；
	 * @author wl
	 * @param ModelChatLog
	 * @return long
	 */
	public long insertChatLog(ModelChatLog pModelChatLog,String pTableName) {
		if(!isTableExists(pTableName)){
			createTable(pTableName);
		}
		long _NewID =  getDataBase().insert(pTableName, null, createParms(pModelChatLog));
		notifyChange(pTableName);
		return _NewID;
	}
	
	public int deleteChatLog(String pCondition,String pTableName) {
		return delete(pTableName,pCondition);
	}
	
	public boolean updateChatLog(String pCondition,ModelChatLog pModelRelatives,String pTableName) {
		ContentValues _ContentValues = createParms(pModelRelatives);
		return getDataBase().update(pTableName , _ContentValues, pCondition, null) > 0;
	}
	
	public int UpdateChatLog(String p_Condition,ContentValues pContentValues,String pTableName){
		return getDataBase().update(pTableName, pContentValues, p_Condition, null);
	}
	
	
	/**
	 * 设置聊天记录是否已读；
	 * @author wl
	 * @param 
	 * @return void
	 */
	public void updateIsRead(int p_ID,int pRead,String pTableName){
		String _SqlText = "update "+pTableName+" set Read='"+pRead+"'"+" where _ID="+p_ID;
		Log.v("tt","_SqlText = " + _SqlText);
		getDataBase().execSQL(_SqlText);
	}
	
	/**
	 * 获取表内的全部信息；
	 * @author wl
	 * @param 
	 * @return List<ModelChatLog>
	 */
	public List<ModelChatLog> GetAllChatLogs(String pTableName)
	{
		String _SqlText = "Select * From " +  pTableName + " Where 1=1";
		return getList(_SqlText);
	}
	
	/**
	 * 获取表内的全部信息(传入条件)；
	 * @author wl
	 * @param String 如：Dect = 0
	 * @return List<ModelChatLog>
	 */
	public List<ModelChatLog> GetChatLogs(String pCondition,String pTableName){
		String _SqlText = "Select * From " +  pTableName + " Where " + pCondition;
		return getList(_SqlText);
	}
	
	/**
	 * 判断数据库中是否存在这个表；
	 * @author wl
	 * @param pTableName
	 * @return
	 */
	public boolean isTableExists(String pTableName){
		Cursor cursor = ExecSql("select count(*) as c from sqlite_master where type ='table' and name ='" + pTableName + "'");
		if(cursor.moveToNext()){
            int count = cursor.getInt(0);
            if(count>0){
                return true;
            }
		}
		return false;
	}
	
	/**
	 * 获取数据库中所有的表名；
	 * @author wl
	 * @param
	 * @return List<String>
	 */
	public List<String> getAllTables(){
		List<String> resultList = new ArrayList<String>();
		Cursor cursor = ExecSql("select name from sqlite_master where type='table';");
		  while(cursor.moveToNext()){
		   String name = cursor.getString(0);
		   resultList.add(name);
		  }
		 return resultList;
	}
	
	/**
	 * 查询表中JID；
	 * @author wl
	 * @param 
	 * @return List
	 */
	public List<String> getJIDs(String pfield,String pTableName){
		String _SqlText = "Select * From " +  pTableName + " Where JID='" + pfield + "'";
		return getStringList(_SqlText,"JID");
	}
	
	/**
	 * 查询表中的某一列；
	 * @author wl
	 * @param 
	 * @return List
	 */
	private List getStringList(String pSqlText,String field){
		Cursor _Cursor = ExecSql(pSqlText);
		List _List = new ArrayList();
		while(_Cursor.moveToNext()){
			String str = _Cursor.getString(_Cursor.getColumnIndex(field));
			_List.add(str);
		}
		return _List;
	}
	
	public void createTable(String pTableName) {
		StringBuilder s_CreateTableScript = new StringBuilder();
		s_CreateTableScript.append("Create TABLE "+ pTableName +" (");
		s_CreateTableScript.append("[_ID] integer PRIMARY KEY AUTOINCREMENT NOT NULL");
		s_CreateTableScript.append(",[Date] ID NOT NULL");
		s_CreateTableScript.append(",[Dect] integer");
		s_CreateTableScript.append(",[JID] TEXT");
		s_CreateTableScript.append(",[MessageType] integer");
		s_CreateTableScript.append(",[MessageBody] TEXT");
		s_CreateTableScript.append(",[Read] integer");
		s_CreateTableScript.append(")");
		getDataBase().execSQL(s_CreateTableScript.toString());
	}

	@Override
	protected String[] GetTableNameAndPK() {
	//	return new String[]{TableName,"_ID"};
		return null;
	}

	@Override
	protected Object findModel(Cursor pCursor) {
		ModelChatLog _ModelChatLog = new ModelChatLog();
		_ModelChatLog.set_ID(pCursor.getInt(pCursor.getColumnIndex("_ID")));
		_ModelChatLog.setDate(pCursor.getLong(pCursor.getColumnIndex("Date")));
		_ModelChatLog.setDect(pCursor.getInt(pCursor.getColumnIndex("Dect")));
		_ModelChatLog.setJID(pCursor.getString(pCursor.getColumnIndex("JID")));
		_ModelChatLog.setMessageType((pCursor.getInt(pCursor.getColumnIndex("MessageType"))));
		_ModelChatLog.setMessageBody(pCursor.getString(pCursor.getColumnIndex("MessageBody")));
		_ModelChatLog.setRead(pCursor.getInt(pCursor.getColumnIndex("Read")));
		return _ModelChatLog;
	}
	
	public ContentValues createParms(ModelChatLog pInfo){
		ContentValues _ContentValues = new ContentValues();
		//_ContentValues.put("_ID", pInfo.get_ID());
		_ContentValues.put("Date",pInfo.getDate());
		_ContentValues.put("Dect",pInfo.getDect());
		_ContentValues.put("JID",pInfo.getJID());
		_ContentValues.put("MessageType",pInfo.getMessageType());
		_ContentValues.put("MessageBody",pInfo.getMessageBody());
		_ContentValues.put("Read",pInfo.getRead());
		return _ContentValues;
	}
}

class SQLiteHelperChatLog extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "chatlog.db";
	private static final int VERSION = 1;
	public SQLiteHelperChatLog(Context pContext){
		super(pContext, DATABASE_NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase pDataBase) {
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int old_version, int new_version) {
			
	}
}
