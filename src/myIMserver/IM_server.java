package myIMserver;

import java.awt.List;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.Element;

//几个总结吧：
//1. 应该先设计一个状态图，搞清楚各个功能的转换关系，设置好状态标签；
//2. 应该尽可能使代码清晰，避免繁重的条件判断；
//3. 在代码解析部分应该更加精炼；
//4. 此次设计状态标签应该统一在命令解析部分完成；
public class IM_server {
	private static int Port = 8888;
	// 端口号
	// ---------------jdbc连接数据库---------------------
	private ServerSocket server;
	// 驱动程序名
	private String driver = "com.mysql.jdbc.Driver";
	// URL指向要访问的数据库名scutcs
	private String url = "jdbc:mysql://127.0.0.1/myIM";
	// MySQL配置时的用户名
	private String user = "root";
	// Java连接MySQL配置时的密码
	private String password = "19921112";
	// private ArrayList<Socket> clientlist;
	// =================================================
	private Map<String, Socket> clientdict;
	// 建立IP_Port与socket的对应，各线程共享该数据
	private Map<String, String> nm_IPP;

	// 建立用户名和IP_Port的对应，各线程共享该数据

	public IM_server() throws IOException, SQLException, ClassNotFoundException {
		// TODO Auto-generated constructor stub

		// ---------------连接数据库------------------
		// 加载驱动程序
		Class.forName(driver);
		// 连续数据库
		Connection conn = DriverManager.getConnection(url, user, password);
		if (!conn.isClosed())
			System.out.println("Succeeded connecting to the Database!");
		// statement用来执行SQL语句
		Statement statement = conn.createStatement();
		// 要执行的SQL语句
		// String sql = "select * from usrid";
		// ==========================================

		server = new ServerSocket(Port);
		// clientlist = new ArrayList<Socket>();
		clientdict = new HashMap<String, Socket>();
		nm_IPP = new HashMap<String, String>();
		while (true) {
			System.out.println("wait for connect");
			Socket client = server.accept();
			// clientlist.add(client);
			String IP_Port = client.getInetAddress().getHostAddress() + ":"
					+ ((Integer) client.getPort()).toString();
			System.out.println(IP_Port + "已经连接.");
			clientdict.put(IP_Port, client);
			new Thread(new Getorder(client, statement, clientdict, nm_IPP))
					.start();
		}
	}

	public static void main(String[] args) throws IOException, SQLException,
			ClassNotFoundException {
		new IM_server();
	}
}

// //这个类用来存储IP_Port与其对应的socket，以便于进行名字与其一一对应的结构书写。用于检测重复登陆。
// class IP_port_socket{
// private String IP_Port;
// private Socket sckt;
// public IP_port_socket(String ipport,Socket sc){
// IP_Port=ipport;
// sckt=sc;
// }
// public String getIP_Port() {
// return IP_Port;
// }
// public Socket getSckt() {
// return sckt;
// }
// }

class Getorder implements Runnable {
	private Socket client;// 当前线程的socket
	private BufferedReader sop;// 当前socket的input缓存
	private PrintWriter re_client;// 当前socket的output接口
	// private List id;
	private Statement mysqlst;// 当前现场执行sql语句的申明
	// 是否已经登陆的状态变量
	private boolean isLogin;// 是否登录的状态变量
	// 是否结束连接；
	private boolean isQuit;// 是否退出的状态变量，这里的退出不是指logout，而是整个程序的退出
	private String Clientnm;// 该线程登录后的绑定用户名
	private ArrayList<String> friendlist;// 该用户的朋友名单
	private ArrayList<String> onlinefriends;// 该用户在线好友名单
	private Map<String, Socket> socketmap;// 所有用户的IP_Port和套接字对应map
	private Socket s_friend;// 正在聊天的朋友套接字
	private boolean isChatting;// 该用户是否正在聊天
	private String msg;// 传入的消息
	private String IP;// IP地址
	private Integer Port;// 接口号
	private String IP_Port;// 和在一起，用于数据库存储和socket对应时的操作方便
	private Map<String, String> nm_IPPORT;// 登录用户和其IP_Port的对应
	private String frichatwith;// 正在聊天的朋友名字，便于进行特殊状态的对话：比如突然中断、突然删除好友
	private boolean isdelete;// 是否删除好友
	private boolean isadd;// 是否添加好友
	private static final String code = "19921112";// 超级用户的密码

	public Getorder(Socket cl, Statement st, Map<String, Socket> clientdict,
			Map<String, String> nm_IPP) throws IOException {
		// TODO Auto-generated constructor stub
		client = cl;
		sop = new BufferedReader(new InputStreamReader(client.getInputStream()));
		// id = new List();
		mysqlst = st;
		isLogin = false;
		isQuit = false;
		socketmap = clientdict;
		Clientnm = "";
		friendlist = new ArrayList<String>();
		onlinefriends = new ArrayList<String>();
		s_friend = null;
		isChatting = false;
		msg = "";
		re_client = new PrintWriter(client.getOutputStream());
		IP = client.getInetAddress().getHostAddress();
		Port = client.getPort();
		IP_Port = IP + ":" + Port.toString();
		nm_IPPORT = nm_IPP;
		frichatwith = "";
		isdelete = false;
		isadd = false;
	}

	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
		quit();
		client.close();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			re_client.println("欢迎使用鹏鹏的IM，更多帮助，请尝试/help。");
			re_client.flush();
			while (!isQuit) {
				msg = sop.readLine();
				chechstatus();
				if (!msg.isEmpty()) {
					msghandle();
					re_client.flush();
				}
			}
			re_client.println("谢谢您的使用，再见。");
			re_client.flush();
			client.close();// 关闭socket
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 每次执行之前先检查当前状态，以保持与数据库状态一致
	private void chechstatus() {
		// TODO Auto-generated method stub
		String sql = "select * from userid where IP_port ='" + IP_Port + "'";
		try {
			ResultSet rs_ = mysqlst.executeQuery(sql);
			if (rs_.next()) {
				boolean _isonline = rs_.getBoolean("isOnline");
				if (!_isonline) {
					isChatting = false;
					isLogin = false;
				}
			}
			else {
				isChatting = false;
				isLogin = false;
			}
		}
		catch (Exception e) {
			// TODO: handle exception
		}
		flushfriend();
	}

	// 用于解析命令---第一层次的解析，用于注册登陆登出退出和帮助；
	public void msghandle() throws SQLException, IOException {
		// TODO Auto-generated method stub
		msg.trim();
		if (msg.charAt(0) == '/') {
			String[] msglist = msg.split(" ");
			// 这样的register设计保证了除了在广播模式下，其他任意模式都能进行注册
			// 注册完成后并不是登录状态，而是需要键入登录命令进行登录
			if (msglist[0].equals("/register")) {
				if (!msglist[2].equals(msglist[3])) {
					re_client.println("两次设置密码不一致！，请重新输入");
				}
				else {
					// System.out.println("/register");
					// 初始情况下设置数据库内状态为不在线，IP为0.0.0.0:0
					String sql = "insert into userid value('" + msglist[1]
							+ "','" + msglist[2]
							+ "',false,'0.0.0.0:0000','0.0.0.0',0)";
					try {
						mysqlst.execute(sql);
						re_client.println("注册成功，请登录！");
					}
					catch (Exception e) {
						// TODO: handle exception
						re_client.println("该用户名已被占用，请修改后重新注册。");
					}
					// System.out.println(rst);
				}
			}
			// 如此复杂的条件，思路主要是:未登录->登录，已登录->注销->登录，异地登陆->验证->登陆，其中还包括是否在线的检查，是否注册的检查
			else if (msglist[0].equals("/login")) {
				if (isLogin) {
					re_client.println("该客户端已经登陆一个账户，请先执行logout");
				}
				else {
					String sql = "select * from userid where userid.usernm ='"
							+ msglist[1] + "'";
					boolean isrelogin = false;
					// System.out.println(sql);
					try {
						ResultSet rs = mysqlst.executeQuery(sql);
						String pswd = "";
						if (rs.next()) {
							pswd = rs.getString("password");
							isrelogin = rs.getBoolean("isOnline");
						}
						else {
							re_client.println("该用户没有被注册。");
						}
						if (pswd.equals(msglist[2])) {
							if (isrelogin) {
								re_client
										.println("该用户已经登陆，继续操作，请输入：y－－确认登陆，n－－取消操作");
								re_client.flush();
								msg = sop.readLine();
								while (true) {
									if (msg.equals("y")) {
										isLogin = true;
										Clientnm = msglist[1];
										flushsocketmap(Clientnm);
										login();
										break;
									}
									else if (msg.equals("n")) {
										break;
									}
									else {
										re_client
												.println("输入错误，请重新输入。该用户已经登陆，继续操作，请输入：y－－确认登陆，n－－取消操作");
										msg = sop.readLine();
									}
								}
							}
							else {
								isLogin = true;
								Clientnm = msglist[1];
								login();
							}
							// msghandle2(msglist);
						}
						else {
							re_client.println("密码输入错误！");
						}
					}
					catch (Exception e) {
						// TODO: handle exception
						logout(msglist[1]);
						re_client.println("login failed!");
					}
				}
			}
			// 退出线程、关闭连接
			else if (msglist[0].equals("/quit")) {
				quit();
			}
			// 注销，需要将状态重置，期间还要检查一些情况：诸如本来还没有登录的、本来
			else if (msglist[0].equals("/logout")) {
				isChatting = false;
				frichatwith = "";
				if (isLogin) {
					isLogin = false;
					logout(Clientnm);
					re_client.println("成功logout");
				}
				else {
					re_client.println("你还没有login。");
				}
			}

			else if (msglist[0].equals("/help")) {
				String help_ = "功能指令格式如下：\n"
						+ "1. 注册: /register name Password Password\n"
						+ "2. 登录: /login name Password\n" + "3. 登出: /logout\n"
						+ "4. 退出客户端: /quit\n" + "5. 添加指定好友: /add name\n"
						+ "6. 删除指定好友: /delete name\n"
						+ "7. 与指定好友聊天: /chat name\n"
						+ "8. 停止向好友发送消息: /stopchat name\n" + "9. 帮助: /help\n"
						+ "10. 刷新朋友和在线朋友列表: /flush\n"
						+ "11. 广播功能、退出广播功能: /super Password ;/quitsuper\n"
						+ "12. 修改密码: /ch_pswd name oldPassword newPassword\n";
				re_client.println(help_);
			}
			// 连接指令，采用由服务器消息转发机制，已经开始聊天的用户需要
			else if (msglist[0].equals("/chat")) {
				if (isLogin) {
					String sql = "select * from userid where usernm= '"
							+ msglist[1] + "'";
					ResultSet rs__ = mysqlst.executeQuery(sql);
					if (rs__.next()) {
						if (friendlist.contains(msglist[1])) {
							if (onlinefriends.contains(msglist[1])) {
								// 这里很纠结，是采用该客户端直接连接指定用户呢，还是拷贝用户对服务器的连接；暂时使用后者实现；
								String IP_Port_ = rs__.getString("IP_Port");
								s_friend = socketmap.get(IP_Port_);
								// new Thread(new Msg2fri(client,link)).start();
								// new Thread(new Msg2fri(link,client)).start();
								isChatting = true;
								frichatwith = msglist[1];
								re_client.println("成功连接，可以开始聊天～");
							}
							else {
								re_client.println("你的朋友不在线");
							}
						}
						else {
							re_client.println(msglist[1] + " 还不是你的朋友");
						}
					}
					else {
						re_client.println("该用户不存在！");
					}
				}
			}
			// 增加一个停止发消息的指令，仅仅需要改变一些状态标签就行；
			else if (msglist[0].equals("/stopchat")) {
				if (isChatting) {
					re_client.println("将不再向" + frichatwith + "发送消息");
					isChatting = false;
					frichatwith = "";
					s_friend = null;
				}
				else {
					re_client.println("你现在并没有处于聊天状态");
				}

			}
			// 刷新朋友列表
			else if (msglist[0].equals("/flush")) {
				if (isLogin) {
					showfriends();
				}
				else {
					re_client.println("请先登录");
				}
			}
			// 添加朋友，本来想实现权限授予，但是需要在线进行授权，逻辑上有一些不方便，还是取消了验证步骤
			else if (msglist[0].equals("/add")) {
				if (isLogin) {// 两边同时加为好友
					addfri(Clientnm, msglist[1]);
					addfri(msglist[1], Clientnm);
					if (isadd) {
						noticefri(msglist[1]);
						isadd = false;
					}
					// add之后应该刷新好友列表
					re_client.println("正在刷新好友列表");
					showfriends();
				}
				else {
					re_client.println("请先登录");
				}
			}
			// 删除好友
			else if (msglist[0].equals("/delete")) {
				if (isLogin) {// 两边同时删除
					deletefri(Clientnm, msglist[1]);
					deletefri(msglist[1], Clientnm);
					if (isdelete) {
						deletenoticefri(msglist[1]);
						isdelete = false;
					}
					// delete之后应该刷新好友列表
					re_client.println("正在刷新好友列表");
					showfriends();
				}
				else {
					re_client.println("请先登录");
				}
			}
			// 广播功能，退出广播需要输入 /quitsuper
			else if (msglist[0].equals("/super")) {
				if (msglist[1].equals(code)) {
					supernotice();
					re_client.println("已经退出广播模式。");
				}
			}
			else if (msglist[0].equals("/ch_pswd")) {
				changepswd(msglist[1], msglist[2], msglist[3]);
			}
			else {
				re_client.println("请输入正确的指令");
			}
		}
		// 非指令模式将会进入此，如果在聊天，将会直接发送聊天消息。
		else {
			if (isChatting)
				chat2fri();
			else if (isLogin) {
				re_client.println("请输入你要聊天的人。");
			}
			else {
				re_client.println("请先登录或者注册！");
			}
		}
	}

	private void changepswd(String nm, String oldpswd, String newpswd)
			throws SQLException {
		// TODO Auto-generated method stub
		String sql = "select * from userid where usernm='" + nm
				+ "' and password='" + oldpswd + "'";
		ResultSet rs_ = mysqlst.executeQuery(sql);
		if (rs_.next()) {
			sql = "update userid set password ='" + newpswd
					+ "' where usernm ='" + nm + "'";
			mysqlst.execute(sql);
			re_client.println("密码成功修改!");
		}
		else {
			re_client.println("用户名或密码错误!");
		}
	}

	// 广播函数，迭代向所有socket发送消息，不论该客户端的用户是否登录
	private void supernotice() throws IOException {
		// TODO Auto-generated method stub
		re_client.println("进入广播模式，使用‘/quitsuper’退出广播模式");
		re_client.flush();
		Iterator<Entry<String, Socket>> iter = socketmap.entrySet().iterator();
		BufferedReader supermsg_b = new BufferedReader(new InputStreamReader(
				client.getInputStream()));
		while (true) {
			String msg_ = supermsg_b.readLine();
			if (!msg_.equals("/quitsuper")) {
				iter = socketmap.entrySet().iterator();
				while (iter.hasNext()) {
					@SuppressWarnings("rawtypes")
					Map.Entry entry = (Map.Entry) iter.next();
					// String key = (String) entry.getKey();
					Socket e_s = (Socket) entry.getValue();
					PrintWriter super_p = new PrintWriter(e_s.getOutputStream());
					super_p.println(msg_);
					super_p.flush();
				}
			}
			else {
				break;
			}
		}
	}

	// 删除好友的提示函数，向对方发送已被删除
	private void deletenoticefri(String clienttodelete) throws IOException {
		// TODO Auto-generated method stub
		String ipp = nm_IPPORT.get(clienttodelete);
		if (ipp == null) {
			re_client.println("对方不在线，将不做通知。");
		}
		else {
			Socket s_toadd = socketmap.get(ipp);
			PrintWriter p_toadd = new PrintWriter(s_toadd.getOutputStream());
			p_toadd.println(Clientnm + "从好友列表中删除了您");
			p_toadd.flush();
			re_client.println("已经通知" + clienttodelete + "被删除");
		}
	}

	// 删除好友，同时改变一些状态标签；
	private void deletefri(String clientwant2delete, String clienttodelete) {
		// TODO Auto-generated method stub
		String sql = "";
		sql = "select * from userid where usernm ='" + clienttodelete + "'";
		try {
			ResultSet rs_ = mysqlst.executeQuery(sql);
			if (rs_.next()) {// 此时说明该用户存在
				// boolean isfriOnline=rs_.getBoolean("isOnline");
				sql = "select * from " + clientwant2delete
						+ " where friendnm ='" + clienttodelete + "'";
				try {
					rs_ = mysqlst.executeQuery(sql);
					if (rs_.next()) {// 此时说明查询结果为有，即已经在朋友列表中；不需要再验证
						sql = "delete from " + clientwant2delete
								+ " where friendnm = '" + clienttodelete + "'";
						mysqlst.execute(sql);
						re_client.println(clientwant2delete + " 成功删除好友 "
								+ clienttodelete);
						isdelete = true;
						if (frichatwith.equals(clienttodelete)) {
							isChatting = false;
							frichatwith = "";
						}
					}
					else {// 这表明没有在朋友列表中
						re_client.println("你们还不是朋友，无法删除");
					}
				}
				catch (Exception e) {
					// TODO: handle exception
					re_client.println("你们还不是朋友，无法删除");
				}
			}
		}
		catch (Exception e) {
			// TODO: handle exception
			re_client.println("该用户不存在！");
		}
	}

	// 这个函数用来处理同一账号异地登陆，将会把异地账号进行登出，并作出提醒。
	private void flushsocketmap(String loginnm) throws IOException {
		// TODO Auto-generated method stub
		// String IPP_new = IP_Port;
		String IPP_old = nm_IPPORT.get(loginnm);
		Socket sckt_old = socketmap.get(IPP_old);
		// System.out.println("-------flshsocketmap---------");
		// System.out.println(nm_IPPORT);
		// System.out.println(IPP_old);
		// System.out.println(sckt_old);
		PrintWriter warning = new PrintWriter(sckt_old.getOutputStream());
		warning.println("您的账户在:" + IP_Port
				+ " 登陆，本地账号将登出，若非本人操作，请注意账户安全（还没做-_-）");
		warning.flush();
		logout(loginnm);
	}

	// 退出客户端，将会进行logout操作
	private void quit() {
		// TODO Auto-generated method stub
		logout(Clientnm);
		// isChatting = false;
		frichatwith = "";
		// isLogin = false;
		isQuit = true;
	}

	// 添加朋友，以本人和朋友用户名为参数，其中会检查是否重复添加，数据库方面维护者一张以用户名命名的表，里面记录着用户的好友名字，这里实现的时候在这里进行了这张表的创建和维护，其实创建应该在一开始注册的时候就可以发生，这样会简单好多！！
	private void addfri(String clientwant2add, String clienttoadd) {
		// TODO Auto-generated method stub
		String sql = "";
		sql = "select * from userid where usernm ='" + clienttoadd + "'";
		try {
			ResultSet rs_ = mysqlst.executeQuery(sql);
			if (rs_.next()) {// 此时说明该用户存在
				// boolean isfriOnline=rs_.getBoolean("isOnline");
				sql = "select * from " + clientwant2add + " where friendnm ='"
						+ clienttoadd + "'";
				try {
					rs_ = mysqlst.executeQuery(sql);
					if (rs_.next()) {// 此时说明查询结果为有，即已经在朋友列表中；不需要再验证
						// String fnm = rs_.getString("friendnm");
						// if (!fnm.equals(friendnm)) {
						// sql = "insert into " + Clientnm + " value('"
						// + friendnm + "')";
						// mysqlst.execute(sql);
						// re_client.println("成功添加" + friendnm + "为好友。");
						// }
						// else {
						re_client.println("你们已经是朋友了，不需要重复添加。");
						// }
					}
					else {// 这表明没有在朋友列表中
							// if (isfriOnline) {
							// boolean isAgreed=getpermission(clientwant2add,
							// clienttoadd);
						sql = "insert into " + clientwant2add + " value('"
								+ clienttoadd + "')";
						mysqlst.execute(sql);
						re_client.println(clientwant2add + " 成功添加"
								+ clienttoadd + "为好友。");
						isadd = true;
						// }else{
						// re_client.println("该用户不在线，请在线时添加");
						// }
					}
				}
				catch (Exception e) {
					// TODO: handle exception
					// if (isfriOnline) {
					sql = "create table " + clientwant2add
							+ "(friendnm varchar(255))";
					try {
						mysqlst.execute(sql);
						sql = "insert into " + clientwant2add + " value('"
								+ clienttoadd + "')";
						mysqlst.execute(sql);
						re_client.println(clientwant2add + "成功添加" + clienttoadd
								+ "为好友。");
						isadd = true;
					}
					catch (Exception e2) {
						// TODO: handle exception
						System.out.println("add 失败!");
					}
					// }else{
					// re_client.println("该用户不在线，请在线时添加");
					// }

				}
			}
		}
		catch (Exception e) {
			// TODO: handle exception
			re_client.println("该用户不存在！");
		}

	}

	// 暂时放弃，因为与聊天逻辑不符合，万一对方正在聊天，需要断开对方的连接？
	// private boolean getpermission(String clientwant2add, String clienttoadd)
	// throws IOException {
	// // TODO Auto-generated method stub
	// // String IPP_add=IP_Port;
	// String IPP_toadd=nm_IPPORT.get(clienttoadd);
	// Socket s_toadd=socketmap.get(IPP_toadd);
	// PrintWriter p_toadd=new PrintWriter(s_toadd.getOutputStream());
	// p_toadd.println(Clientnm+"想添加你为好友，同意吗？同意请输入y，否则请输入n");
	//
	//
	// return false;
	// }
	// 添加好友以后的双方提示，对方不在线的话就不会提示了
	private void noticefri(String clienttoadd) throws IOException {
		// TODO Auto-generated method stub
		String ipp = nm_IPPORT.get(clienttoadd);
		if (ipp == null) {
			re_client.println("对方不在线，将不做通知。");
		}
		else {
			Socket s_toadd = socketmap.get(ipp);
			PrintWriter p_toadd = new PrintWriter(s_toadd.getOutputStream());
			p_toadd.println(Clientnm + "添加您为好友！");
			p_toadd.flush();
			re_client.println("已经通知" + clienttoadd + "成为您的好友");
		}
	}

	// 聊天函数，允许聊天过程中一方突然断线（退出）或者删除好友，主要是由于每次线程轮回时都会检查mysql表并刷新朋友列表，这样做感觉牺牲了很多效率
	private void chat2fri() throws IOException {
		// TODO Auto-generated method stub
		if (isChatting) {
			PrintWriter tofriend;
			String ipp_ = nm_IPPORT.get(frichatwith);
			if (ipp_ != null) {
				if (onlinefriends.contains(frichatwith)) {
					tofriend = new PrintWriter(s_friend.getOutputStream());
					tofriend.println(Clientnm + ":" + msg);
					tofriend.flush();
				}
				else {
					re_client.println("你们还不是朋友");
					isChatting = false;
					frichatwith = "";
				}
			}
			else {
				re_client.println("您的朋友已经掉线");
				isChatting = false;
				frichatwith = "";
			}
		}
	}

	// 注销，状态恢复初始状态
	private void logout(String nm_Client) {
		// TODO Auto-generated method stub
		String sql = "update userid set isOnline= false, IP_port='0.0.0.0:0000',IP='0.0.0.0',Port=0000 where usernm ='"
				+ nm_Client + "'";
		try {
			mysqlst.execute(sql);
			onlinefriends.remove(nm_Client);
			nm_IPPORT.remove(nm_Client);
			friendlist = new ArrayList<String>();
			onlinefriends = new ArrayList<String>();
			// isadd = false;
			// isdelete = false;
			// isChatting = false;
			// isLogin = false;
			// isQuit = false;
		}
		catch (Exception e) {
			// TODO: handle exception
			re_client.println("logout失败!");
		}

	}

	// 登录，数据库中登记，状态变量改变
	private void login() throws SQLException {
		// TODO Auto-generated method stub
		// String IP_port = client.getInetAddress().getHostAddress()
		// + ((Integer) client.getPort()).toString();
		String sql = "update userid set isOnline= true, IP_port='" + IP_Port
				+ "',IP='" + IP + "',Port=" + Port + " where usernm ='"
				+ Clientnm + "'";
		// try {
		mysqlst.execute(sql);
		nm_IPPORT.put(Clientnm, IP_Port);
		showfriends();
		// isOnline=true;
		// isLogin=true;
		// }
		// catch (Exception e) {
		// // TODO: handle exception
		// re_client.println("login失败!");
		// }
	}

	// 刷新好友列表和在线好友列表，服务于聊天函数（防止中途掉线无应答）、添加、删除好友函数
	private void flushfriend() {
		// TODO Auto-generated method stub
		String sql = "select * from " + Clientnm;
		friendlist = new ArrayList<String>();
		onlinefriends = new ArrayList<String>();
		try {
			ResultSet rs = mysqlst.executeQuery(sql);
			while (rs.next()) {
				String friendnm = rs.getString("friendnm");
				if (!friendnm.isEmpty()) {
					friendlist.add(friendnm);
				}
				else {
					break;
				}
			}
			if (friendlist.size() != 0) {
				for (String friendnm : friendlist) {
					String sql_ = "select * from userid where usernm ='"
							+ friendnm + "'";
					ResultSet rs_ = mysqlst.executeQuery(sql_);
					boolean isOnline = false;
					if (rs_.next()) {
						isOnline = rs_.getBoolean("isOnline");
						if (isOnline) {
							onlinefriends.add(friendnm);
						}
					}
				}
			}
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			// re_client.println("您还没有好友，请添加好友！");
		}
	}

	// 显示好友及在线好友情况
	private void showfriends() {
		// TODO Auto-generated method stub
		flushfriend();
		if (friendlist.size() == 0) {
			re_client.println("您还没有好友，请添加好友！");
		}
		else {
			re_client.println("您的好友如下：");
			for (String friendnm : friendlist) {
				re_client.println(friendnm);
			}
			if (onlinefriends.size() > 0) {
				re_client.println("您在线的好友有：");
				for (String nm : onlinefriends) {
					re_client.println(nm);
				}
			}
			else {
				re_client.println("您没有好友在线！");
			}
		}
	}
}

// class Msg2fri implements Runnable{
// private Socket s_client;
// private Socket s_friend;//client to friend;
// BufferedReader sop;
//
// public Msg2fri(Socket cli,Socket fri) throws IOException {
// // TODO Auto-generated constructor stub
// s_client=cli;
// s_friend=fri;
// sop=new BufferedReader(new InputStreamReader(s_client.getInputStream()));
// }
// @Override
// public void run() {
// // TODO Auto-generated method stub
// String msg=sop.readLine();
// while()
// }
//
// }