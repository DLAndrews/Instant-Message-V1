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
import java.util.Map;
import javax.lang.model.element.Element;

public class IM_server {
	private static int Port = 8888;
	private ServerSocket server;
	// 驱动程序名
	private String driver = "com.mysql.jdbc.Driver";
	// URL指向要访问的数据库名scutcs
	private String url = "jdbc:mysql://127.0.0.1/myIM";
	// MySQL配置时的用户名
	private String user = "root";
	// Java连接MySQL配置时的密码
	private String password = "19921112";
	private ArrayList<Socket> clientlist;
	private Map<String, Socket> clientdict;

	public IM_server() throws IOException, SQLException, ClassNotFoundException {
		// TODO Auto-generated constructor stub

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

		server = new ServerSocket(Port);
		clientlist = new ArrayList<Socket>();
		clientdict = new HashMap<String, Socket>();
		while (true) {
			System.out.println("wait for connect");
			Socket client = server.accept();
			clientlist.add(client);
			String IP_Port = client.getInetAddress().getHostAddress() + ":"
					+ ((Integer) client.getPort()).toString();
			clientdict.put(IP_Port, client);
			new Thread(new Getorder(client, statement, clientdict)).start();

		}
	}

	public static void main(String[] args) throws IOException, SQLException,
			ClassNotFoundException {
		new IM_server();
	}
}

class Getorder implements Runnable {
	private Socket client;
	private BufferedReader sop;
	private PrintWriter re_client;
	// private List id;
	private Statement mysqlst;
	// 是否已经登陆的状态变量
	private boolean isLogin;
	// 是否结束连接；
	private boolean isQuit;
	private Map<String, Socket> socketmap;
	private String Clientnm;
	private ArrayList<String> friendlist;
	private ArrayList<String> onlinefriends;
	private Socket s_friend;
	private boolean isChatting;
	private String msg;
	private String IP;
	private Integer Port;
	private String IP_Port;

	public Getorder(Socket cl, Statement st, Map<String, Socket> clientdict)
			throws IOException {
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
				if (!msg.isEmpty()) {
					msghandle();
					re_client.flush();
				}
			}
			re_client.println("谢谢您的使用，再见。");
			re_client.flush();
			client.close();
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

	// 用于解析命令---第一层次的解析，用于注册登陆登出退出和帮助；
	public void msghandle() throws SQLException, IOException {
		// TODO Auto-generated method stub
		msg.trim();
		if (msg.charAt(0) == '/') {
			String[] msglist = msg.split(" ");
			if (msglist[0].equals("/register")) {
				if (!msglist[2].equals(msglist[3])) {
					re_client.println("两次设置密码不一致！，请重新输入");
				}
				else {
					// System.out.println("/register");
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
			else if (msglist[0].equals("/login")) {
				String sql = "select * from userid where userid.usernm ='"
						+ msglist[1] + "'";
				// System.out.println(sql);
				try {
					ResultSet rs = mysqlst.executeQuery(sql);
					String pswd = "";
					if (rs.next()) {
						pswd = rs.getString("password");
					}
					else {
						re_client.println("该用户没有被注册。");
					}
					if (pswd.equals(msglist[2])) {
						isLogin = true;
						Clientnm = msglist[1];
						login();
						showfriends();
						// msghandle2(msglist);
					}
					else {
						re_client.println("密码输入错误！");
					}
				}
				catch (Exception e) {
					// TODO: handle exception
					re_client.println("login failed!");
				}
			}
			else if (msglist[0].equals("/quit")) {
				quit();
			}
			else if (msglist[0].equals("/logout")) {
				isChatting = false;
				if (isLogin) {
					isLogin = false;
					logout();
					re_client.println("成功logout");
				}
				else {
					re_client.println("你还没有login。");
				}
			}
			else if (msglist[0].equals("/help")) {
				re_client.println("未编辑帮助内容。");
			}
			else if (msglist[0].equals("/connect")) {
				if (isLogin) {
					String sql = "select * from userid where usernm= '"
							+ msglist[1] + "'";
					ResultSet rs__ = mysqlst.executeQuery(sql);
					if (rs__.next()) {
						if (onlinefriends.contains(msglist[1])) {
							// 这里很纠结，是采用该客户端直接连接指定用户呢，还是拷贝用户对服务器的连接；暂时使用后者实现；
							String IP_Port_ = rs__.getString("IP_Port");
							s_friend = socketmap.get(IP_Port_);
							// new Thread(new Msg2fri(client,link)).start();
							// new Thread(new Msg2fri(link,client)).start();
							isChatting = true;
							re_client.println("成功连接，可以开始聊天～");
						}
						else {
							re_client.println("你的朋友不在线，或者请刷新好友列表。");
						}
					}
					else {
						re_client.println("该用户不存在！");
					}
				}
			}
			else if (msglist[0].equals("/flush")) {
				if (isLogin) {
					showfriends();
				}
				else {
					re_client.println("请先登录");
				}
			}
			else if (msglist[0].equals("/add")) {
				if (isLogin) {// 两边同时加为好友
					addfri(Clientnm, msglist[1]);
					addfri(msglist[1], Clientnm);
					// add之后应该刷新好友列表
					re_client.println("正在刷新好友列表");
					showfriends();
				}
				else {
					re_client.println("请先登录");
				}
			}
			else {

			}
		}
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

	private void quit() {
		// TODO Auto-generated method stub
		logout();
		isChatting = false;
		isLogin = false;
		isQuit = true;
	}

	private void addfri(String clientwant2add, String clienttoadd) {
		// TODO Auto-generated method stub
		String sql = "";
		sql = "select * from userid where usernm ='" + clienttoadd + "'";
		try {
			ResultSet rs_ = mysqlst.executeQuery(sql);
			if (rs_.next()) {// 此时说明该用户存在
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
						System.out.println("你们已经是朋友了，不需要重复添加。");
						// }
					}
					else {// 这表明没有在朋友列表中
						sql = "insert into " + clientwant2add + " value('"
								+ clienttoadd + "')";
						mysqlst.execute(sql);
						re_client.println(clientwant2add + " 成功添加"
								+ clienttoadd + "为好友。");
					}
				}
				catch (Exception e) {
					// TODO: handle exception
					sql = "create table " + clientwant2add
							+ "(friendnm varchar(255))";
					try {
						mysqlst.execute(sql);
						sql = "insert into " + clientwant2add + " value('"
								+ clienttoadd + "')";
						mysqlst.execute(sql);
						re_client.println(clientwant2add + "成功添加" + clienttoadd
								+ "为好友。");
					}
					catch (Exception e2) {
						// TODO: handle exception
						System.out.println("add 失败!");
					}

				}
			}
		}
		catch (Exception e) {
			// TODO: handle exception
			re_client.println("该用户不存在！");
		}

	}

	private void chat2fri() throws IOException {
		// TODO Auto-generated method stub
		if (isChatting) {
			PrintWriter tofriend = new PrintWriter(s_friend.getOutputStream());
			tofriend.println(Clientnm + ":" + msg);
			tofriend.flush();
		}
	}

	private void logout() {
		// TODO Auto-generated method stub
		String sql = "update userid set isOnline= false, IP_port='0.0.0.0:0000',IP='0.0.0.0',Port=0000 where usernm ='"
				+ Clientnm + "'";
		try {
			mysqlst.execute(sql);
		}
		catch (Exception e) {
			// TODO: handle exception
			re_client.println("logout失败!");
		}

	}

	private void login() {
		// TODO Auto-generated method stub
		// String IP_port = client.getInetAddress().getHostAddress()
		// + ((Integer) client.getPort()).toString();
		String sql = "update userid set isOnline= true, IP_port='" + IP_Port
				+ "',IP='" + IP + "',Port=" + Port + " where usernm ='"
				+ Clientnm + "'";
		try {
			mysqlst.execute(sql);
		}
		catch (Exception e) {
			// TODO: handle exception
			re_client.println("login失败!");
		}
	}

	private void showfriends() {
		// TODO Auto-generated method stub
		String sql = "select * from " + Clientnm;
		friendlist = new ArrayList<String>();
		onlinefriends = new ArrayList<String>();
		try {
			ResultSet rs = mysqlst.executeQuery(sql);
			re_client.println("您的好友如下：");
			while (rs.next()) {
				String friendnm = rs.getString("friendnm");
				if (!friendnm.isEmpty()) {
					re_client.println(friendnm);
					friendlist.add(friendnm);
				}
				else {
					break;
				}
			}
			if (friendlist.size() == 0) {
				re_client.println("原来您还没有好友，请添加好友！");
			}
			else {
				re_client.println("您在线的好友有：");
				for (int i = 0; i < friendlist.size(); i++) {
					String nm = friendlist.get(i);
					String sql_ = "select * from userid where usernm ='" + nm
							+ "'";
					ResultSet rs_ = mysqlst.executeQuery(sql_);
					boolean isOnline = false;
					if (rs_.next()) {
						isOnline = rs_.getBoolean("isOnline");
						if (isOnline) {
							onlinefriends.add(nm);
							re_client.println(nm);
						}
					}
				}

			}
		}
		catch (SQLException e) {
			// TODO Auto-generated catch block
			re_client.println("您还没有好友，请添加好友！");
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