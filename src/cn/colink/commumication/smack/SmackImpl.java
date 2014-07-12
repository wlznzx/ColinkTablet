package cn.colink.commumication.smack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import cn.colink.commumication.db.provider.ChatProvider;
import cn.colink.commumication.db.provider.RosterProvider;
import cn.colink.commumication.db.provider.ChatProvider.ChatConstants;
import cn.colink.commumication.db.provider.RosterProvider.RosterConstants;
import cn.colink.commumication.exception.XXException;
import cn.colink.commumication.service.XXService;
import cn.colink.commumication.util.L;
import cn.colink.commumication.util.PreferenceConstants;
import cn.colink.commumication.util.PreferenceUtils;
import cn.colink.commumication.util.StatusMode;
import cn.colink.commumication.R;

public class SmackImpl implements Smack {
	public static final String XMPP_IDENTITY_NAME = "xx";
	public static final String XMPP_IDENTITY_TYPE = "phone";
	private static final int PACKET_TIMEOUT = 30000;
	private static final int JOIN_MAXSTANZA = 10;
	private static final int FIRST_JOIN_MAXSTANZA = 20;
	final static private String[] SEND_OFFLINE_PROJECTION = new String[] {
			ChatConstants._ID, ChatConstants.JID, ChatConstants.MESSAGE,
			ChatConstants.DATE, ChatConstants.PACKET_ID };
	final static private String SEND_OFFLINE_SELECTION = ChatConstants.DIRECTION
			+ " = "
			+ ChatConstants.OUTGOING
			+ " AND "
			+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	static {
		registerSmackProviders();
	}

	static void registerSmackProviders() {
		ProviderManager pm = ProviderManager.getInstance();
		// add IQ handling
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());
		// add delayed delivery notifications
		pm.addExtensionProvider("delay", "urn:xmpp:delay",
				new DelayInfoProvider());
		pm.addExtensionProvider("x", "jabber:x:delay", new DelayInfoProvider());
		// add carbons and forwarding
		pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE,
				new Forwarded.Provider());
		pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
		pm.addExtensionProvider("received", Carbon.NAMESPACE,
				new Carbon.Provider());
		// add delivery receipts
		pm.addExtensionProvider(DeliveryReceipt.ELEMENT,
				DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
		pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT,
				DeliveryReceipt.NAMESPACE,
				new DeliveryReceiptRequest.Provider());
		// add XMPP Ping (XEP-0199)
		pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());
		// MUC User
				pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
						new MUCUserProvider());
				// MUC Admin
				pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
						new MUCAdminProvider());
				// MUC Owner
				pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
						new MUCOwnerProvider());
		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());
		// FileTransfer
				pm.addIQProvider("si", "http://jabber.org/protocol/si",
						new StreamInitiationProvider());
		// SharedGroupsInfo
				pm.addIQProvider("sharedgroup",
						"http://www.jivesoftware.org/protocol/sharedgroup",
						new SharedGroupsInfo.Provider());

		ServiceDiscoveryManager.setIdentityName(XMPP_IDENTITY_NAME);
		ServiceDiscoveryManager.setIdentityType(XMPP_IDENTITY_TYPE);
	}

	private ConnectionConfiguration mXMPPConfig;
	private XMPPConnection mXMPPConnection;
	private XXService mService;
	private Roster mRoster;
	private final ContentResolver mContentResolver;
	private RosterListener mRosterListener;
	private PacketListener mPacketListener;
	private PacketListener mSendFailureListener;
	private PacketListener mPongListener;

	// ping-pong服务器
	private String mPingID;
	private long mPingTimestamp;
	private PendingIntent mPingAlarmPendIntent;
	private PendingIntent mPongTimeoutAlarmPendIntent;
	private static final String PING_ALARM = "cn.colink.commumication.PING_ALARM";
	private static final String PONG_TIMEOUT_ALARM = "cn.colink.commumication.PONG_TIMEOUT_ALARM";
	private Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private BroadcastReceiver mPingAlarmReceiver = new PingAlarmReceiver();

	// ping-pong服务器

	public SmackImpl(XXService service) {
		String customServer = PreferenceUtils.getPrefString(service,
				PreferenceConstants.CUSTOM_SERVER, "");
		int port = PreferenceUtils.getPrefInt(service,
				PreferenceConstants.PORT, PreferenceConstants.DEFAULT_PORT_INT);
		String server = PreferenceUtils.getPrefString(service,
				PreferenceConstants.Server, PreferenceConstants.GMAIL_SERVER);
		boolean smackdebug = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.SMACKDEBUG, false);
		boolean requireSsl = PreferenceUtils.getPrefBoolean(service,
				PreferenceConstants.REQUIRE_TLS, false);
		if (customServer.length() > 0
				|| port != PreferenceConstants.DEFAULT_PORT_INT)
			this.mXMPPConfig = new ConnectionConfiguration(customServer, port,
					server);
		else
			this.mXMPPConfig = new ConnectionConfiguration(server); // use SRV

		this.mXMPPConfig.setReconnectionAllowed(false);
		this.mXMPPConfig.setSendPresence(false);
		this.mXMPPConfig.setCompressionEnabled(false); // disable for now
		this.mXMPPConfig.setDebuggerEnabled(smackdebug);
		this.mXMPPConfig.setSASLAuthenticationEnabled(false); 
		if (requireSsl)
			this.mXMPPConfig
					.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		this.mXMPPConnection = new XMPPConnection(mXMPPConfig);
		this.mService = service;
		mContentResolver = service.getContentResolver();
	}

	@Override
	public boolean login(String account, String password) throws XXException {
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mXMPPConnection.disconnect();
				} catch (Exception e) {
					L.d("conn.disconnect() failed: " + e);
				}
			}
			SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
			SmackConfiguration.setKeepAliveInterval(-1);
			SmackConfiguration.setDefaultPingInterval(0);
			registerRosterListener();// 监听联系人动态变化
			mXMPPConnection.connect();
			if (!mXMPPConnection.isConnected()) {
				throw new XXException("SMACK connect failed without exception!");
			}
			mXMPPConnection.addConnectionListener(new ConnectionListener() {
				public void connectionClosedOnError(Exception e) {
					mService.postConnectionFailed(e.getMessage());
				}

				public void connectionClosed() {
				}

				public void reconnectingIn(int seconds) {
				}

				public void reconnectionFailed(Exception e) {
				}

				public void reconnectionSuccessful() {
				}
			});
			initServiceDiscovery();// 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
			// SMACK auto-logins if we were authenticated before
			if (!mXMPPConnection.isAuthenticated()) {
				String ressource = PreferenceUtils.getPrefString(mService,
						PreferenceConstants.RESSOURCE, "XX");
				mXMPPConnection.login(account, password, ressource);
			}
			setStatusFromConfig();// 更新在线状态

		} catch (XMPPException e) {
			throw new XXException(e.getLocalizedMessage(),
					e.getWrappedThrowable());
		} catch (Exception e) {
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			L.e(SmackImpl.class, "login(): " + Log.getStackTraceString(e));
			throw new XXException(e.getLocalizedMessage(), e.getCause());
		}
		registerAllListener();// 注册监听其他的事件，比如新消息
		return mXMPPConnection.isAuthenticated();
	}

	private void registerAllListener() {
		// actually, authenticated must be true now, or an exception must have
		// been thrown.
		if (isAuthenticated()) {
			registerMessageListener();
			registerMessageSendFailureListener();
			registerPongListener();
			registerMultiUserChatInviteListener();
			//joinMultiUserChat(mXMPPConnection,"co");
			joinMultiUserChatWhenLogin(mXMPPConnection);
			//joinMultiUserChat();
			sendOfflineMessages();
			if (mService == null) {
				mXMPPConnection.disconnect();
				return;
			}
			// we need to "ping" the service to let it know we are actually
			// connected, even when no roster entries will come in
			mService.rosterChanged();
		}
	}
	/**
	 * colink 第一次加入请亲请圈（会话组）
	 * @author wl
	 * @param XMPPConnection，String
	 * */
	private void joinMultiUserChat(XMPPConnection pXMPPConnection,String pGroupJID){
		MultiUserChat muc = new MultiUserChat(pXMPPConnection, pGroupJID);
		try {
			DiscussionHistory history = new DiscussionHistory();
			history.setMaxStanzas(FIRST_JOIN_MAXSTANZA);															// 新群，读20条；
			muc.join(pXMPPConnection.getUser());
			saveGroupInfo(pXMPPConnection,pGroupJID);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	/**
	 * colink 在登陆的时候加入情亲圈（会话组）；
	 * @author wl
	 * @param XMPPConnection
	 * */
	private void joinMultiUserChatWhenLogin(XMPPConnection pXMPPConnection){
		List<String> joinGroups = getGroupList(pXMPPConnection, null);
		for(String str : joinGroups){
			MultiUserChat muc = new MultiUserChat(pXMPPConnection, str);
			Log.v("wlpe","join "+str);
			try {
				DiscussionHistory history = new DiscussionHistory();
				history.setMaxStanzas(JOIN_MAXSTANZA);															// 设置读取历史记录的条数.
				muc.join(pXMPPConnection.getUser(),null,history,300);
			} catch (XMPPException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * colink 获取保存已经添加会话组的字段；
	 * @author wl
	 * @return List<String>
	 */
	private List<String> getGroupList(XMPPConnection pXMPPConnection,String pJID){
		List<String> result = new ArrayList<String>();
		VCard vcard = new VCard();
		try {
			vcard.load(pXMPPConnection);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		int i = 0;
		/**/
		String str = vcard.getField("colink"+i);
		Log.v("wlpe","" + str);
		while(str != null){
			result.add(str);
			Log.v("wlpe",vcard.getField("colink"+i));
			i++;
			str = vcard.getField("colink"+i);
		}
		return result;
	}
	/**
	 *	 colink 将已经添加的会话组名，加入到用户的Filed字段中去，下次Login的时候，重新添加该组；
	 */
	private void saveGroupInfo(XMPPConnection pXMPPConnection,String pGroupName){
		VCard vcard = new VCard();
		try {
			vcard.load(pXMPPConnection);
			int i = 0;
			String str = vcard.getField("colink"+i);
			while(str != null){
				i++;
				str = vcard.getField("colink"+i);
			}
			vcard.setField("colink"+i, pGroupName);
			vcard.save(pXMPPConnection);
			Log.v("wlpe","setField " + "colink"+i + " " + pGroupName);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	private void joinMultiUserChat(){
//		Presence presence = new Presence(Presence.Type.available); 
//		presence.setStatus("ai");
//		mXMPPConnection.sendPacket(presence);
//		Presence presence2 = mXMPPConnection.getRoster().getPresence("admin@114.215.209.75");
//		//Presence presence = new Presence(Presence.Type.available);
//		String str = presence2.getStatus();
//		VCard vcard = new VCard();
//		try {
//			vcard.load(mXMPPConnection);
//			for(int i=0;i<6;i++){
//				Log.v("wlpe","vcard.getAddressFieldHome"+i + vcard.getField("wl"+i));
//			}
//			vcard.save(mXMPPConnection);
//		} catch (XMPPException e1) {
//			e1.printStackTrace();
//		}
//		muc = new MultiUserChat(mXMPPConnection, "colink@conference.114.215.209.75");
//        try {
//			muc.join("admin", null);
//		} catch (XMPPException e) {
//			e.printStackTrace();
//		}   
//        //监听消息   
//        muc.addMessageListener(new PacketListener() {   
//            @Override   
//            public void processPacket(Packet packet) {
//                Message message = (Message) packet;   
//                DelayInformation inf = (DelayInformation)  message.getExtension("x", "jabber:x:delay");
//                if (inf == null) {  
//                    System.out.println("new message");
//                    Log.v("wl","message = " + message.getFrom() + " : " + message.getBody());
//                } else {  
//                    System.out.println("old message...."+ message.getBody());
//                }
//            }   
//        });
		
		VCard vcard = new VCard();
		try {
			vcard.load(mXMPPConnection);
			for(int i=0;i<6;i++){
				Log.v("wlpe","vcard.getAddressFieldHome"+i + vcard.getField("wl"+i));
				
				vcard.setField("colink"+i, null);
			}
			vcard.save(mXMPPConnection);
		} catch (XMPPException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * 监听聊天室邀请
	 * @author wl
	 */
	private void registerMultiUserChatInviteListener(){
		Log.v("wl","registerMultiUserChatInviteListener()");
		 MultiUserChat.addInvitationListener(mXMPPConnection, new InvitationListener() {
			@Override
			public void invitationReceived(Connection arg0, String GroupJID,
					String arg2, String arg3, String arg4, Message arg5) {
				joinMultiUserChat(mXMPPConnection,GroupJID);
			}   
         }); 
	}

	/************ start 新消息处理 ********************/
	private void registerMessageListener() {
		// do not register multiple packet listeners
		if (mPacketListener != null)
			mXMPPConnection.removePacketListener(mPacketListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mPacketListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();
						
						// try to extract a carbon
						Carbon cc = CarbonManager.getCarbon(msg);
						if (cc != null
								&& cc.getDirection() == Carbon.Direction.received) {
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded()
									.getForwardedPacket();
							chatMessage = msg.getBody();
							// fall through
						} else if (cc != null
								&& cc.getDirection() == Carbon.Direction.sent) {
							L.d("carbon: " + cc.toXML());
							msg = (Message) cc.getForwarded()
									.getForwardedPacket();
							chatMessage = msg.getBody();
							if (chatMessage == null)
								return;
							String fromJID = getJabberID(msg.getTo());

							addChatMessageToDB(ChatConstants.OUTGOING, fromJID,
									chatMessage, ChatConstants.DS_SENT_OR_READ,
									System.currentTimeMillis(),
									msg.getPacketID());
							// always return after adding
							return;
						}

						if (chatMessage == null) {
							return;
						}

						if (msg.getType() == Message.Type.error) {
							chatMessage = "<Error> " + chatMessage;
						}

						long ts;
						DelayInfo timestamp = (DelayInfo) msg.getExtension(
								"delay", "urn:xmpp:delay");
						if (timestamp == null)
							timestamp = (DelayInfo) msg.getExtension("x",
									"jabber:x:delay");
						if (timestamp != null)
							ts = timestamp.getStamp().getTime();
						else
							ts = System.currentTimeMillis();

						String fromJID = getJabberID(msg.getFrom());

						addChatMessageToDB(ChatConstants.INCOMING, fromJID,
								chatMessage, ChatConstants.DS_NEW, ts,
								msg.getPacketID());
						mService.newMessage(fromJID, chatMessage);
						Log.v("wl","chatMessage = " + chatMessage);
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, filter);
	}

	private void addChatMessageToDB(int direction, String JID, String message,
			int delivery_status, long ts, String packetID) {
		ContentValues values = new ContentValues();

		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, JID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);

		mContentResolver.insert(ChatProvider.CONTENT_URI, values);
	}

	/************ end 新消息处理 ********************/

	/***************** start 处理消息发送失败状态 ***********************/
	private void registerMessageSendFailureListener() {
		// do not register multiple packet listeners
		if (mSendFailureListener != null)
			mXMPPConnection
					.removePacketSendFailureListener(mSendFailureListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mSendFailureListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					if (packet instanceof Message) {
						Message msg = (Message) packet;
						String chatMessage = msg.getBody();

						Log.d("SmackableImp",
								"message "
										+ chatMessage
										+ " could not be sent (ID:"
										+ (msg.getPacketID() == null ? "null"
												: msg.getPacketID()) + ")");
						changeMessageDeliveryStatus(msg.getPacketID(),
								ChatConstants.DS_NEW);
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from
					// processPacket :(
					L.e("failed to process packet:");
					e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketSendFailureListener(mSendFailureListener,
				filter);
	}

	public void changeMessageDeliveryStatus(String packetID, int new_status) {
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME);
		mContentResolver.update(rowuri, cv, ChatConstants.PACKET_ID
				+ " = ? AND " + ChatConstants.DIRECTION + " = "
				+ ChatConstants.OUTGOING, new String[] { packetID });
	}

	/***************** end 处理消息发送失败状态 ***********************/
	/***************** start 处理ping服务器消息 ***********************/
	private void registerPongListener() {
		// reset ping expectation on new connection
		mPingID = null;

		if (mPongListener != null)
			mXMPPConnection.removePacketListener(mPongListener);

		mPongListener = new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				if (packet == null)
					return;

				if (packet.getPacketID().equals(mPingID)) {
					L.i(String.format(
							"Ping: server latency %1.3fs",
							(System.currentTimeMillis() - mPingTimestamp) / 1000.));
					mPingID = null;
					((AlarmManager) mService
							.getSystemService(Context.ALARM_SERVICE))
							.cancel(mPongTimeoutAlarmPendIntent);
				}
			}

		};

		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(
				IQ.class));
		mPingAlarmPendIntent = PendingIntent.getBroadcast(
				mService.getApplicationContext(), 0, mPingAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(
				mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(
				PING_ALARM));
		mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(
				PONG_TIMEOUT_ALARM));
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
				.setInexactRepeating(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis()
								+ AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						AlarmManager.INTERVAL_FIFTEEN_MINUTES,
						mPingAlarmPendIntent);
	}

	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			L.d("Ping: timeout for " + mPingID);
			mService.postConnectionFailed(XXService.PONG_TIMEOUT);
			logout();// 超时就断开连接
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 */
	private class PingAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			if (mXMPPConnection.isAuthenticated()) {
				sendServerPing();
			} else
				L.d("Ping: alarm received, but not connected to server.");
		}
	}

	/***************** end 处理ping服务器消息 ***********************/

	/***************** start 发送离线消息 ***********************/
	public void sendOfflineMessages() {
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION, null, null);
		final int _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
		final int JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int PACKETID_COL = cursor
				.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS,
				ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext()) {
			int _id = cursor.getInt(_ID_COL);
			String toJID = cursor.getString(JID_COL);
			String message = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			long ts = cursor.getLong(TS_COL);
			L.d("sendOfflineMessages: " + toJID + " > " + message);
			final Message newMessage = new Message(toJID, Message.Type.chat);
			newMessage.setBody(message);
			DelayInformation delay = new DelayInformation(new Date(ts));
			newMessage.addExtension(delay);
			newMessage.addExtension(new DelayInfo(delay));
			newMessage.addExtension(new DeliveryReceiptRequest());
			if ((packetID != null) && (packetID.length() > 0)) {
				newMessage.setPacketID(packetID);
			} else {
				packetID = newMessage.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}
			Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
					+ ChatProvider.TABLE_NAME + "/" + _id);
			mContentResolver.update(rowuri, mark_sent, null, null);
			mXMPPConnection.sendPacket(newMessage); // must be after marking
													// delivered, otherwise it
													// may override the
													// SendFailListener
		}
		cursor.close();
	}

	public static void sendOfflineMessage(ContentResolver cr, String toJID,
			String message) {
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, toJID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	/***************** end 发送离线消息 ***********************/
	/******************************* start 联系人数据库事件处理 **********************************/
	private void registerRosterListener() {
		mRoster = mXMPPConnection.getRoster();
		mRosterListener = new RosterListener() {
			private boolean isFristRoter;

			@Override
			public void presenceChanged(Presence presence) {
				L.i("presenceChanged(" + presence.getFrom() + "): " + presence);
				String jabberID = getJabberID(presence.getFrom());
				RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				updateRosterEntryInDB(rosterEntry);
				mService.rosterChanged();
			}

			@Override
			public void entriesUpdated(Collection<String> entries) {
				// TODO Auto-generated method stub
				L.i("entriesUpdated(" + entries + ")");
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				mService.rosterChanged();
			}

			@Override
			public void entriesDeleted(Collection<String> entries) {
				L.i("entriesDeleted(" + entries + ")");
				for (String entry : entries) {
					deleteRosterEntryFromDB(entry);
				}
				mService.rosterChanged();
			}

			@Override
			public void entriesAdded(Collection<String> entries) {
				L.i("entriesAdded(" + entries + ")");
				ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
				}
				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				if (isFristRoter) {
					isFristRoter = false;
					mService.rosterChanged();
				}
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}

	private String getJabberID(String from) {
		String[] res = from.split("/");
		return res[0].toLowerCase();
	}

	private void updateRosterEntryInDB(final RosterEntry entry) {
		final ContentValues values = getContentValuesForRosterEntry(entry);

		if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
				RosterConstants.JID + " = ?", new String[] { entry.getUser() }) == 0)
			addRosterEntryToDB(entry);
	}

	private void addRosterEntryToDB(final RosterEntry entry) {
		ContentValues values = getContentValuesForRosterEntry(entry);
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		L.i("addRosterEntryToDB: Inserted " + uri);
	}

	private void deleteRosterEntryFromDB(final String jabberID) {
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
				RosterConstants.JID + " = ?", new String[] { jabberID });
		L.i("deleteRosterEntryFromDB: Deleted " + count + " entries");
	}

	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));

		Presence presence = mRoster.getPresence(entry.getUser());
		values.put(RosterConstants.STATUS_MODE, getStatusInt(presence));
		values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));

		return values;
	}

	private String getGroup(Collection<RosterGroup> groups) {
		for (RosterGroup group : groups) {
			return group.getName();
		}
		return "";
	}

	private String getName(RosterEntry rosterEntry) {
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0) {
			return name;
		}
		return rosterEntry.getUser();
	}

	private StatusMode getStatus(Presence presence) {
		if (presence.getType() == Presence.Type.available) {
			if (presence.getMode() != null) {
				return StatusMode.valueOf(presence.getMode().name());
			}
			return StatusMode.available;
		}
		return StatusMode.offline;
	}

	private int getStatusInt(final Presence presence) {
		return getStatus(presence).ordinal();
	}

	public void setStatusFromConfig() {
		boolean messageCarbons = PreferenceUtils.getPrefBoolean(mService,
				PreferenceConstants.MESSAGE_CARBONS, true);
		String statusMode = PreferenceUtils.getPrefString(mService,
				PreferenceConstants.STATUS_MODE, PreferenceConstants.AVAILABLE);
		String statusMessage = PreferenceUtils.getPrefString(mService,
				PreferenceConstants.STATUS_MESSAGE,
				mService.getString(R.string.status_online));
		int priority = PreferenceUtils.getPrefInt(mService,
				PreferenceConstants.PRIORITY, 0);
		if (messageCarbons)
			CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(
					true);

		Presence presence = new Presence(Presence.Type.available);
		Mode mode = Mode.valueOf(statusMode);
		presence.setMode(mode);
		presence.setStatus(statusMessage);
		presence.setPriority(priority);
		mXMPPConnection.sendPacket(presence);
	}

	/******************************* end 联系人数据库事件处理 **********************************/

	/**
	 * 与服务器交互消息监听,发送消息需要回执，判断是否发送成功
	 */
	private void initServiceDiscovery() {
		// register connection features
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager
				.getInstanceFor(mXMPPConnection);
		if (sdm == null)
			sdm = new ServiceDiscoveryManager(mXMPPConnection);

		sdm.addFeature("http://jabber.org/protocol/disco#info");

		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(
				10 * 1000);
		// reference DeliveryReceiptManager, add listener

		DeliveryReceiptManager dm = DeliveryReceiptManager
				.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.registerReceiptReceivedListener(new DeliveryReceiptManager.ReceiptReceivedListener() {
			public void onReceiptReceived(String fromJid, String toJid,
					String receiptId) {
				L.d(SmackImpl.class, "got delivery receipt for " + receiptId);
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);
			}
		});
	}

	@Override
	public boolean isAuthenticated() {
		if (mXMPPConnection != null) {
			return (mXMPPConnection.isConnected() && mXMPPConnection
					.isAuthenticated());
		}
		return false;
	}

	@Override
	public void addRosterItem(String user, String alias, String group)
			throws XXException {
		// TODO Auto-generated method stub
		addRosterEntry(user, alias, group);
	}

	private void addRosterEntry(String user, String alias, String group)
			throws XXException {
		mRoster = mXMPPConnection.getRoster();
		try {
			mRoster.createEntry(user, alias, new String[] { group });
		} catch (XMPPException e) {
			throw new XXException(e.getLocalizedMessage());
		}
	}

	@Override
	public void removeRosterItem(String user) throws XXException {
		// TODO Auto-generated method stub
		L.d("removeRosterItem(" + user + ")");

		removeRosterEntry(user);
		mService.rosterChanged();
	}

	private void removeRosterEntry(String user) throws XXException {
		mRoster = mXMPPConnection.getRoster();
		try {
			RosterEntry rosterEntry = mRoster.getEntry(user);

			if (rosterEntry != null) {
				mRoster.removeEntry(rosterEntry);
			}
		} catch (XMPPException e) {
			throw new XXException(e.getLocalizedMessage());
		}
	}

	@Override
	public void renameRosterItem(String user, String newName)
			throws XXException {
		// TODO Auto-generated method stub
		mRoster = mXMPPConnection.getRoster();
		RosterEntry rosterEntry = mRoster.getEntry(user);

		if (!(newName.length() > 0) || (rosterEntry == null)) {
			throw new XXException("JabberID to rename is invalid!");
		}
		rosterEntry.setName(newName);
	}

	@Override
	public void moveRosterItemToGroup(String user, String group)
			throws XXException {
		// TODO Auto-generated method stub
		tryToMoveRosterEntryToGroup(user, group);
	}

	private void tryToMoveRosterEntryToGroup(String userName, String groupName)
			throws XXException {

		mRoster = mXMPPConnection.getRoster();
		RosterGroup rosterGroup = getRosterGroup(groupName);
		RosterEntry rosterEntry = mRoster.getEntry(userName);

		removeRosterEntryFromGroups(rosterEntry);

		if (groupName.length() == 0)
			return;
		else {
			try {
				rosterGroup.addEntry(rosterEntry);
			} catch (XMPPException e) {
				throw new XXException(e.getLocalizedMessage());
			}
		}
	}

	private void removeRosterEntryFromGroups(RosterEntry rosterEntry)
			throws XXException {
		Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

		for (RosterGroup group : oldGroups) {
			tryToRemoveUserFromGroup(group, rosterEntry);
		}
	}

	private void tryToRemoveUserFromGroup(RosterGroup group,
			RosterEntry rosterEntry) throws XXException {
		try {
			group.removeEntry(rosterEntry);
		} catch (XMPPException e) {
			throw new XXException(e.getLocalizedMessage());
		}
	}

	private RosterGroup getRosterGroup(String groupName) {
		RosterGroup rosterGroup = mRoster.getGroup(groupName);

		// create group if unknown
		if ((groupName.length() > 0) && rosterGroup == null) {
			rosterGroup = mRoster.createGroup(groupName);
		}
		return rosterGroup;

	}

	@Override
	public void renameRosterGroup(String group, String newGroup) {
		// TODO Auto-generated method stub
		L.i("oldgroup=" + group + ", newgroup=" + newGroup);
		mRoster = mXMPPConnection.getRoster();
		RosterGroup groupToRename = mRoster.getGroup(group);
		if (groupToRename == null){
			return;
		}
		groupToRename.setName(newGroup);
	}

	@Override
	public void requestAuthorizationForRosterItem(String user) {
		// TODO Auto-generated method stub
		Presence response = new Presence(Presence.Type.subscribe);
		response.setTo(user);
		mXMPPConnection.sendPacket(response);
	}

	@Override
	public void addRosterGroup(String group) {
		// TODO Auto-generated method stub
		mRoster = mXMPPConnection.getRoster();
		mRoster.createGroup(group);
	}

	@Override
	public void sendMessage(String toJID, String message) {
		// TODO Auto-generated method stub
		final Message newMessage = new Message(toJID, Message.Type.chat);
		newMessage.setBody(message);
		newMessage.addExtension(new DeliveryReceiptRequest());
		if (isAuthenticated()) {
			addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
					ChatConstants.DS_SENT_OR_READ, System.currentTimeMillis(),
					newMessage.getPacketID());
			mXMPPConnection.sendPacket(newMessage);
		} else {
			// send offline -> store to DB
			addChatMessageToDB(ChatConstants.OUTGOING, toJID, message,
					ChatConstants.DS_NEW, System.currentTimeMillis(),
					newMessage.getPacketID());
		}
	}

	@Override
	public void sendServerPing() {
		if (mPingID != null) {
			L.d("Ping: requested, but still waiting for " + mPingID);
			return; // a ping is still on its way
		}
		Ping ping = new Ping();
		ping.setType(Type.GET);
		ping.setTo(PreferenceUtils.getPrefString(mService,
				PreferenceConstants.Server, PreferenceConstants.GMAIL_SERVER));
		mPingID = ping.getPacketID();
		mPingTimestamp = System.currentTimeMillis();
		L.d("Ping: sending ping " + mPingID);
		mXMPPConnection.sendPacket(ping);

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE)).set(
				AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
						+ PACKET_TIMEOUT + 3000, mPongTimeoutAlarmPendIntent);
	}

	@Override
	public String getNameForJID(String jid) {
		if (null != this.mRoster.getEntry(jid)
				&& null != this.mRoster.getEntry(jid).getName()
				&& this.mRoster.getEntry(jid).getName().length() > 0) {
			return this.mRoster.getEntry(jid).getName();
		} else {
			return jid;
		}
	}
	/**
	 * colink 创建聊天室
	 * 
	 */
	private MultiUserChat muc;
	@Override
	public void createMultiChat(String username){
		List<String> col = null;
	     Collection<HostedRoom> rooms = null;
	     HostedRoom colinkRoom = null;
			try {
				col = getConferenceServices(mXMPPConnection.getServiceName(), mXMPPConnection);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(col.isEmpty()){
		        for (Object aCol : col) {   
		            String service = (String) aCol;   
		             //查询服务器上的聊天室   
					try {
						rooms = MultiUserChat.getHostedRooms(mXMPPConnection, service);
					} catch (XMPPException e) {
						e.printStackTrace();
					}  
		        }
		        for (HostedRoom room : rooms) {
		        	if(room.getName().equals(username)){
		        		colinkRoom = room;
		        	}else{
		        		
		        	}
				}
			}
	        if(colinkRoom == null){
	        	this.muc = new MultiUserChat(mXMPPConnection, username + "@conference.114.215.209.75");
	        	try {  
	                // 创建聊天室  
	                muc.create(username);  
	                // 获得聊天室的配置表单  
	                Form form = muc.getConfigurationForm();  
	                // 根据原始表单创建一个要提交的新表单。  
	                Form submitForm = form.createAnswerForm();  
	                // 向要提交的表单添加默认答复  
	                for (Iterator fields = form.getFields(); fields.hasNext();) {  
	                    FormField field = (FormField) fields.next();  
	                    if (!FormField.TYPE_HIDDEN.equals(field.getType())
	                            && field.getVariable() != null) {  
	                        // 设置默认值作为答复  
	                        submitForm.setDefaultAnswer(field.getVariable());  
	                    }  
	                }  
	                // 设置聊天室的新拥有者  
	                // List owners = new ArrayList();  
	                // owners.add("liaonaibo2\\40slook.cc");  
	                // owners.add("liaonaibo1\\40slook.cc");  
	                // submitForm.setAnswer("muc#roomconfig_roomowners", owners);  
	                // 设置聊天室是持久聊天室，即将要被保存下来  
	                submitForm.setAnswer("muc#roomconfig_persistentroom", true);  
	                // 房间仅对成员开放  
	                submitForm.setAnswer("muc#roomconfig_membersonly", false);  
	                // 允许占有者邀请其他人  
	                submitForm.setAnswer("muc#roomconfig_allowinvites", true);  
	                // 能够发现占有者真实 JID 的角色  
	                // submitForm.setAnswer("muc#roomconfig_whois", "anyone");  
	                // 登录房间对话  
	                submitForm.setAnswer("muc#roomconfig_enablelogging", true);
	                // 仅允许注册的昵称登录  
	                submitForm.setAnswer("x-muc#roomconfig_reservednick", true);  
	                // 允许使用者修改昵称  
	                submitForm.setAnswer("x-muc#roomconfig_canchangenick", false);
	                // 允许用户注册房间  
	                submitForm.setAnswer("x-muc#roomconfig_registration", false);
	                // 发送已完成的表单（有默认值）到服务器来配置聊天室  
	                muc.sendConfigurationForm(submitForm);  
	            } catch (XMPPException e) {  
	                e.printStackTrace();  
	            }  
	        }else{
	        	this.muc = new MultiUserChat(mXMPPConnection, colinkRoom.getJid());
	        }
	}
	public List<String> getConferenceServices(String server,
			XMPPConnection connection) throws Exception {
		List<String> answer = new ArrayList<String>();
		ServiceDiscoveryManager discoManager = ServiceDiscoveryManager
				.getInstanceFor(connection);
		DiscoverItems items = discoManager.discoverItems(server);
		for (Iterator<DiscoverItems.Item> it = items.getItems(); it.hasNext();) {
			DiscoverItems.Item item = (DiscoverItems.Item) it.next();
			if (item.getEntityID().startsWith("conference")
					|| item.getEntityID().startsWith("private")) {
				answer.add(item.getEntityID());
			} else {
				try {
					DiscoverInfo info = discoManager.discoverInfo(item
							.getEntityID());
					if (info.containsFeature("http://jabber.org/protocol/muc")) {
						answer.add(item.getEntityID());
					}
				} catch (XMPPException e) {
				}
			}
		}
		return answer;
	}

	@Override
	public boolean logout() {
		L.d("unRegisterCallback()");
		// remove callbacks _before_ tossing old connection
		try {
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection
					.removePacketSendFailureListener(mSendFailureListener);
			mXMPPConnection.removePacketListener(mPongListener);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
					.cancel(mPingAlarmPendIntent);
			((AlarmManager) mService.getSystemService(Context.ALARM_SERVICE))
					.cancel(mPongTimeoutAlarmPendIntent);
			mService.unregisterReceiver(mPingAlarmReceiver);
			mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		} catch (Exception e) {
			// ignore it!
			return false;
		}
		if (mXMPPConnection.isConnected()) {
			// work around SMACK's #%&%# blocking disconnect()
			new Thread() {
				public void run() {
					L.d("shutDown thread started");
					mXMPPConnection.disconnect();
					L.d("shutDown thread finished");
				}
			}.start();
		}
		setStatusOffline();
		this.mService = null;
		return true;
	}

	private void setStatusOffline() {
		ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, StatusMode.offline.ordinal());
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}
}
