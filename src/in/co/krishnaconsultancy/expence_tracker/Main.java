package in.co.krishnaconsultancy.expence_tracker;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sun.mail.util.MailSSLSocketFactory;

import in.co.krishnaconsultancy.expence_tracker.Parser.StatementTypes;

public class Main {

	private static Connection con;
	private static JSONArray rules;
	private static String[] subjects;
	private static Parser.Logger logger = new Parser.Logger() {
		@Override
		public void log(Entries entry) {
			PreparedStatement stmt;
			try {
				stmt = con.prepareStatement(
						"INSERT INTO `entries`(`datetime`, `particulars`, `amount`, `transaction_type`, `source`) "
						+ "VALUES (?,?,?,?,?)");
				stmt.setObject(1, entry.date.getTime(), Types.BIGINT);
				stmt.setString(2, entry.desc);
				stmt.setDouble(3, entry.amt);
				stmt.setString(4, entry.transactionType);
				stmt.setString(5, entry.source);
				stmt.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}  
			System.out.println(entry);
		}
	};
	
	public static void main(String[] args) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject) parser.parse(new FileReader(new File("config.json")));

		JSONObject mysql = (JSONObject) obj.get("mysql"); 
		Class.forName("com.mysql.jdbc.Driver");  
		con=DriverManager.getConnection(
				String.format("jdbc:mysql://%s:%s/%s", mysql.get("host"), mysql.get("port"), mysql.get("database")),
				(String) mysql.get("username"),(String) mysql.get("password"));  

		JSONObject mailAccount = (JSONObject) obj.get("mail_account");
		rules = (JSONArray) obj.get("rules");
		int len = rules.size();
		subjects = new String[len];
		for(int i = 0; i < len; i++){
			subjects[i] = (String) ((JSONObject)rules.get(i)).get("subject");
		}
		String[] folders = null;
		if(mailAccount.containsKey("folders")){
			JSONArray fs = (JSONArray) mailAccount.get("folders");
			folders = new String[fs.size()];
			for(int i = 0; i < fs.size(); i++){
				folders[i] = (String) fs.get(i);
			}
		}
		fetch((String)mailAccount.get("host"), (String)mailAccount.get("username"), (String)mailAccount.get("password"), folders);
	}
	
	public static void fetch(String host, String username, String password, String... folders) throws Exception {
        Store store = null;
        try {
			MailSSLSocketFactory socketFactory= new MailSSLSocketFactory();
			socketFactory.setTrustAllHosts(true);
			Properties props = System.getProperties();
			props.setProperty("mail.store.protocol", "imaps");
			props.put("mail.imaps.ssl.socketFactory", socketFactory);
			Session session = Session.getDefaultInstance(props, null);
			//session.setDebug(true);
          
			store = session.getStore("imaps");
			store.connect(host, username, password);
			if(folders == null){
				Folder[] fo = store.getDefaultFolder().list("*");
				folders = new String[fo.length];
				for(int i = 0; i < fo.length; i++){
					folders[i] = fo[i].getFullName();
				}
			}
			Calendar calendar = Calendar.getInstance();
			PreparedStatement stmt = con.prepareStatement("SELECT MAX(date) FROM statement_list");
			ResultSet rs = stmt.executeQuery();
			rs.next();
			if(rs.getLong(1) == 0){
				calendar.set(2015, 3, 1);
			}
			else {
				calendar.setTimeInMillis(rs.getLong(1));
			}
			stmt.close();
			Message[] messages;
			Message msg;
			Folder folder;
			for(String folderName : folders){
				folder = store.getFolder(folderName);
				if(!folder.isOpen()) folder.open(Folder.READ_ONLY);
				messages = folder.getMessages();
				for (int i=messages.length - 1; i >= 0; i--) {
					msg = messages[i];
					if(msg.getReceivedDate().before(calendar.getTime())){
						break;
					}
					for(String sub : subjects){
						if(msg.getSubject().matches(sub)){
							writePart(msg, msg.getReceivedDate());
							break;
						}
					}
				}
	    		if (folder != null && folder.isOpen()) { folder.close(false); }
			}
    	} finally {
    		if (store != null) { store.close(); }
    	}
	}
   
   /*
   * This method checks for content-type 
   * based on which, it processes and
   * fetches the content of the message
   */
   public static void writePart(Part p, Date date) throws Exception {
	  //if (p instanceof Message) writeEnvelope((Message) p);
      //check if the content has attachment
      if (p.isMimeType("multipart/*")) {
         Multipart mp = (Multipart) p.getContent();
         int count = mp.getCount();
         for (int i = 0; i < count; i++)
            writePart(mp.getBodyPart(i), date);
      } 
      //check if the content is a nested message
      else if (p.isMimeType("message/rfc822")) {
         //writePart((Part) p.getContent(), date);
		  writePart((Part) p.getContent(), ((Message)p).getReceivedDate());
      } 
      else {
    	  Object o = p.getContent();
    	  if (o instanceof InputStream && p.getFileName() != null) {
    		  String fileName = p.getFileName(), statement_type;
    		  JSONObject obj;
    		  for(int i = 0; i < rules.size(); i++){
       			  obj = (JSONObject)rules.get(i);
       			  if(Pattern.matches((String)obj.get("file_name"), fileName)){
       				  statement_type = (String)obj.get("statement_type");
    				  PreparedStatement stmt = con.prepareStatement(
    						  "SELECT count(id) FROM `statement_list` WHERE `date` = ? AND `statement_type` = ?");
    				  stmt.setObject(1, date.getTime(), Types.BIGINT);
    				  stmt.setString(2, statement_type);
    				  ResultSet rs = stmt.executeQuery();
    				  rs.next();
    				  if(rs.getInt(1) == 0){
        				  Parser.parseStatements((InputStream)o, (String)obj.get("password"), 
        						  StatementTypes.valueOf(statement_type), logger);
        				  stmt.close();
        				  stmt = con.prepareStatement(
        						  "INSERT INTO `statement_list`(`date`, `statement_type`, `path`) VALUES (?,?,?)");
        				  stmt.setObject(1, date.getTime(), Types.BIGINT);
        				  stmt.setString(2, statement_type);
        				  stmt.setString(3, "");
        				  stmt.execute();
    				  }
    				  else {
    					  System.out.println("Statement already parsed, skipped");
    				  }
    				  stmt.close();
    				  return;
    			  }
    		  }
    	  }
      }
   }

   /*
    * This method would print FROM,TO and SUBJECT of the message
    */
    public static void writeEnvelope(Message m) throws Exception {
    	System.out.println("This is the message envelope");
    	System.out.println("---------------------------");
    	System.out.println("Subject: " + m.getSubject());
    	System.out.println("Date: "+m.getReceivedDate());
		System.out.println("Size: "+m.getSize());
		System.out.println("Content Type:" + m.getContentType());
    	Address[] a;
    	// FROM
    	if ((a = m.getFrom()) != null) {
    		for (int j = 0; j < a.length; j++)
    			System.out.println("FROM: " + a[j].toString());
    	}
    	// TO
    	if ((a = m.getRecipients(Message.RecipientType.TO)) != null) {
    		for (int j = 0; j < a.length; j++)
    			System.out.println("TO: " + a[j].toString());
    	}
    }
}
