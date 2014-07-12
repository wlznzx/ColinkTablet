package cn.colink.commumication.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import cn.colink.commumication.model.ModelChatLogList;

public class SQLiteChatLogList extends SQLiteDALBase{
	public final static String TableName = "ChatLogList";
	
	public SQLiteChatLogList(Context p_Context) {
		super(p_Context,new SQLiteHelperChatLogList(p_Context));
	}
	/**
	 * 插入表；
	 * @author wl
	 * @param ModelChatLogList
	 * @return long
	 */
	public long insertChatLogList(ModelChatLogList pModelChatLogList) {
		long _NewID =  getDataBase().insert(GetTableNameAndPK()[0], null, createParms(pModelChatLogList));
		return _NewID;
	}
	
	/**
	 * 根据UserJID来删除；
	 * @author wl
	 * @param ModelChatLogList
	 * @return long
	 */
	public int deleteChatLogListByUserJID(String pUserJID) {
		return delete(GetTableNameAndPK()[0],"And UserJID = '" + pUserJID+ "'");
	}
	
	public boolean updateChatLogList(String pCondition,ModelChatLogList pModelChatLogList) {
		ContentValues _ContentValues = createParms(pModelChatLogList);
		return getDataBase().update(GetTableNameAndPK()[0], _ContentValues, pCondition, null) > 0;
	}
	
	/**
	 * 获取表内的全部信息；
	 * @author wl
	 * @param 
	 * @return List<ModelChatLogList>
	 */
	public List<ModelChatLogList> getAllChatLogLists()
	{
		String _SqlText = "Select * From " +  TableName + " Where 1=1";
		return getList(_SqlText);
	}
	
	/**
	 * 获取表内的全部信息(传入条件)；
	 * @author wl
	 * @param String 如：Dect = 0
	 * @return List<ModelChatLogList>
	 */
	public List<ModelChatLogList> getChatLogs(String pCondition){
		String _SqlText = "Select * From " +  TableName + " Where " + pCondition;
		return getList(_SqlText);
	}
	
	/**
	 * 根据用户名来查找对应的ChatLog表名；
	 * @author wl
	 * @param 
	 * @return List
	 */
	public String getChatTableNameByJID(String pUserJID){
		String _SqlText = "Select ChatTableName From " +  TableName + " Where UserJID='" + pUserJID + "'";
		Cursor _Cursor = ExecSql(_SqlText);
		String resultStr = null;
		if(_Cursor != null && _Cursor.getCount() > 0){
			_Cursor.moveToNext();
			resultStr = _Cursor.getString(_Cursor.getColumnIndex("ChatTableName"));
		}
		return resultStr;
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
	
	/*
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
	*/

	@Override
	protected String[] GetTableNameAndPK() {
		return new String[]{TableName,"_ID"};
	}

	@Override
	protected Object findModel(Cursor pCursor) {
		ModelChatLogList _ModelChatLogList = new ModelChatLogList();
		_ModelChatLogList.set_ID(pCursor.getInt(pCursor.getColumnIndex("_ID")));
		_ModelChatLogList.setUserJID(pCursor.getString(pCursor.getColumnIndex("UserJID")));
		_ModelChatLogList.setChatTableName(pCursor.getString(pCursor.getColumnIndex("ChatTableName")));
		return _ModelChatLogList;
	}
	
	public ContentValues createParms(ModelChatLogList pInfo){
		ContentValues _ContentValues = new ContentValues();
		//_ContentValues.put("_ID", pInfo.get_ID());
		_ContentValues.put("UserJID",pInfo.getUserJID());
		_ContentValues.put("ChatTableName",pInfo.getChatTableName());
		return _ContentValues;
	}
}

class SQLiteHelperChatLogList extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "chatloglist.db";
	private static final int VERSION = 1;
	public SQLiteHelperChatLogList(Context pContext){
		super(pContext, DATABASE_NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase pDataBase) {
		StringBuilder s_CreateTableScript = new StringBuilder();
		s_CreateTableScript.append("Create TABLE "+ SQLiteChatLogList.TableName +" (");
		s_CreateTableScript.append("[_ID] integer PRIMARY KEY AUTOINCREMENT NOT NULL");
		s_CreateTableScript.append(",[UserJID] TEXT UNIQUE");
		s_CreateTableScript.append(",[ChatTableName] TEXT UNIQUE");
		s_CreateTableScript.append(")");
		pDataBase.execSQL(s_CreateTableScript.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int old_version, int new_version) {
			
	}
}
