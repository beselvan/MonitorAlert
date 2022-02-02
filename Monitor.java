import java.io.StringWriter;
import java.io.PrintWriter;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

class Monitor {

    String line = "";
    String host = "";
    String user = "";
    String password = "";
    String command = "";
    int i = 0;


    void ConnectShell(String hostname, String username, String pwd, int port, String monitor, String email, String mail_password) {
        line = null;
        host = hostname;
        user = username;
        password = pwd;
        port = port;
        monitor = monitor;
        email = email;
        mail_password = mail_password;

        try {
            String command = "df .|awk '{print $5}'|grep [0-9]|cut -d'%' -f1";
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
            session.setConfig(config);;
            session.setPassword(password);
            session.connect();
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            InputStream input = channel.getInputStream();
            channel.connect();

            InputStreamReader inputReader = new InputStreamReader(input);
            BufferedReader bufferedReader = new BufferedReader(inputReader);
            while ((line = bufferedReader.readLine()) != null) {
                i = Integer.parseInt(line);
                bufferedReader.close();
                inputReader.close();
				System.out.println(monitor+ " : \t Connected: " + host + " \t " + user + " \t " +i + "% consumption");
                if (i >= 90) {
                    SendMail(email, mail_password, host, user, "diskSpace", "");
                }

                channel.disconnect();
                session.disconnect();
            }
        } catch (IOException ex) {} catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String ex1 = sw.toString();
            monitor = "diskSpace1";
            System.out.println(monitor+ " : \t Disconnected: " + host + " \t " + user + " : \t " +ex);
            SendMail(email, mail_password, host, user, monitor, ex1);
            ex.printStackTrace();


        }


    }

    void SendMail(String email, String mail_password, String host, String user, String monitor, String ex) {


        String smtp_host = "smtp.gmail.com";
        String from_user = email;
        String password = mail_password;
        String to_user = email;
        Properties props = new Properties();
        props.put("mail.smtp.host", smtp_host);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.ssl.enable", "true");

        javax.mail.Session mailsession = javax.mail.Session.getDefaultInstance(props,
            new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from_user, password);
                }
            });
        try {

            MimeMessage message = new MimeMessage(mailsession);
            message.setFrom(new InternetAddress(from_user));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to_user));

            if (monitor.equals("diskSpace")) {

                message.setText("Hi Team, \n \n" +
                    "The below Host with User is consuming more than " + i + "%. Kindly clear some space \n\n" +
                    "Host: " + host + "\n" +
                    "User: " + user + "\n\n" +
                    "This is the auto genereated alert. Do not reply to this mail \n\n"

                );
                message.setSubject("[Alert] [Warning] [" + host + "] [" + user + "] [File System Reached " + i + "%]");
            } else if (monitor.equals("diskSpace1")) {

                message.setText("Hi Team, \n \n" +
                    "The below Host not reachable \n\n" +
                    "Host: " + host + "\n" +
                    "User: " + user + "\n\n" +
                    "Error: " + ex + "\n\n" +
                    "This is the auto genereated alert. Do not reply to this mail \n\n"

                );
                message.setSubject("[Alert] [Warning] [" + host + "] [" + user + "] [Host is not reachable]");
            } else if (monitor.equals("Database")) {

                message.setText("Hi Team, \n \n" +
                    "The below Database is not reachable\n\n" +
                    "Host: " + host + "\n" +
                    "SID: " + user + "\n\n" +
                    "Error: " + ex + "\n\n" +
                    "This is the auto genereated alert. Do not reply to this mail \n\n"

                );
                message.setSubject("[Alert] [Warning] [" + host + "] [" + user + "] [DataBase is not reachable]");
            }
                        Transport.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }




    void ConnectDB(String DBHost, String DBUser, String DBPassword, int DBPort, String SID, String monitor, String email, String mail_password) {

        Connection conn = null;

        try {
            Class.forName("oracle.jdbc.OracleDriver");

            String dbURL = "jdbc:oracle:thin:@" + DBHost + ":" + DBPort + ":" + SID;
            conn = DriverManager.getConnection(dbURL, DBUser, DBPassword);
            System.out.println(monitor+ "  : \t Connected: " + DBHost + " \t " + SID);
        } catch (Exception ex) {

            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String ex1 = sw.toString();

            SendMail(email, mail_password, DBHost, SID, monitor, ex1);
            System.out.println(monitor+ "  : \t NotConnected: " + DBHost + " \t " + SID + " : \t " +ex);
            ex.printStackTrace();
        }
    }

    public String decodePassword(String password) {
        password = password;
        byte[] password1 = Base64.getDecoder().decode(password);
        String password2 = new String(password1);
        return password2;

    }
    public static void main(String[] args) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Date : " + dtf.format(now));

        Monitor mon = new Monitor();
        Properties prop = new Properties();
        try {

            prop.load(new FileInputStream("file.properties"));
            String email_user = prop.getProperty("email_user");
            String email_password = prop.getProperty("email_password");
            byte[] mail_password1 = Base64.getDecoder().decode(email_password);
            String mail_password = new String(mail_password1);
            String count = prop.getProperty("count");
            int i = Integer.parseInt(count);

            for (int loop = 1; loop <= i; loop++) {


                String host = prop.getProperty("host" + loop);
                String user = prop.getProperty("user" + loop);
                String password = prop.getProperty("password" + loop);
                String cport = prop.getProperty("port" + loop);
                String monitor = prop.getProperty("monitor" + loop);
                String sid = prop.getProperty("sid" + loop);
                String email = prop.getProperty("email" + loop);
                int port = Integer.parseInt(cport);
                String DBpassword = mon.decodePassword(password);

                System.out.print(loop + " : ");

                if (monitor.equals("diskSpace")) {
                    mon.ConnectShell(host, user, password, port, monitor, email_user, mail_password);
                } else if (monitor.equals("Database")) {
                    mon.ConnectDB(host, user, DBpassword, port, sid, monitor, email_user, mail_password);
                }
            }
        } catch (Exception e) {}
               System.out.println("-------------------------------------------------------------------------");
    }
}