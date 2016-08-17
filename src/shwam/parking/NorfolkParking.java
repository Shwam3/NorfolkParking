package shwam.parking;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.xml.bind.DatatypeConverter;
import org.json.JSONObject;
import org.json.XML;

public class NorfolkParking
{
    private static TrayIcon icon;
    private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    
    public static void main(String[] args)
    {
        if (args.length == 1 && "-runonce".equalsIgnoreCase(args[0]))
        {
            String xmlData = null;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL("http://datex.norfolk.cdmf.info/carparks/content.xml").openStream())))
            {
                xmlData = "";
                String line;
                while ((line = br.readLine()) != null)
                    xmlData += line;
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                icon.setToolTip("Norfolk Parking FTP thing\nLast update: Failed\n" + String.valueOf(ex));
            }

            if (xmlData != null)
            {
                JSONObject obj = XML.toJSONObject(xmlData);
                obj = new JSONObject(obj.toString().replace("d2lm:", ""));
                obj = obj.getJSONObject("d2LogicalModel").getJSONObject("payloadPublication");
                obj = new JSONObject(obj, Arrays.asList("publicationTime","situation").toArray(new String[0]));

                if (TimeZone.getDefault().inDaylightTime(new Date()))
                    obj.put("publicationTime", obj.getString("publicationTime") + "+0100");
                else
                    obj.put("publicationTime", obj.getString("publicationTime") + "+0000");
                
                obj.put("uploader", System.getProperty("user.name", "unknown"));

                try
                {
                    File ftpLoginFile = new File("ftpLogin.properties");
                    Properties ftpLogin = new Properties();
                    ftpLogin.load(new FileReader(ftpLoginFile));
                    URLConnection conn = new URL("ftp://ftp.shwam3.altervista.org/parking/data.json").openConnection();
                    conn.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((ftpLogin.getProperty("Username", "") + ":" + ftpLogin.getProperty("Password", "")).getBytes()));

                    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream())))
                    {
                        bw.write(obj.toString());

                        System.out.println("Last update: " + sdf.format(new Date()));
                    }
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();

                    System.out.println("Last update: Failed\n" + String.valueOf(ex));
                }
                
                File file = new File(System.getProperty("user.home"), "web" + File.separator + "parking" + File.separator + "data.json");
                try
                {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                catch (IOException ex)
                {
                    Logger.getLogger(NorfolkParking.class.getName()).
                        log(Level.SEVERE, null, ex);
                }
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file)))
                {
                    bw.write(obj.toString());
                }
                catch (IOException ex) { ex.printStackTrace(); }
            }
        }
        else
        {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) { ex.printStackTrace(); }

            if (SystemTray.isSupported())
            {
                try
                {
                    Image image = ImageIO.read(NorfolkParking.class.getResource("/shwam/parking/res/Icon.png"));
                    PopupMenu menu = new PopupMenu();
                    MenuItem exit = new MenuItem("Exit");
                    exit.addActionListener(e -> System.exit(0));
                    menu.add(exit);

                    icon = new TrayIcon(image);
                    icon.setImageAutoSize(true);
                    icon.setToolTip("Norfolk Parking FTP thing");
                    icon.setPopupMenu(menu);
                    icon.addActionListener(e ->
                    {
                        try { Desktop.getDesktop().browse(new URI("http://shwam3.altervista.org/parking/")); }
                        catch (IOException | URISyntaxException ex) { ex.printStackTrace(); }
                    });
                    SystemTray.getSystemTray().add(icon);
                }
                catch (AWTException | IOException ex) { ex.printStackTrace(); }
            }

            new Timer().scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    String xmlData = null;

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL("http://datex.norfolk.cdmf.info/carparks/content.xml").openStream())))
                    {
                        xmlData = "";
                        String line;
                        while ((line = br.readLine()) != null)
                            xmlData += line;
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace();
                        icon.setToolTip("Norfolk Parking FTP thing\nLast update: Failed\n" + String.valueOf(ex));
                    }

                    if (xmlData != null)
                    {
                        JSONObject obj = XML.toJSONObject(xmlData);
                        obj = new JSONObject(obj.toString().replace("d2lm:", ""));
                        obj = obj.getJSONObject("d2LogicalModel").getJSONObject("payloadPublication");
                        obj = new JSONObject(obj, Arrays.asList("publicationTime","situation").toArray(new String[0]));

                        if (TimeZone.getDefault().inDaylightTime(new Date()))
                            obj.put("publicationTime", obj.getString("publicationTime") + "+0100");
                        else
                            obj.put("publicationTime", obj.getString("publicationTime") + "+0000");
                        
                        obj.put("uploader", System.getProperty("user.name", "unknown"));

                        try
                        {
                            File ftpLoginFile = new File("ftpLogin.properties");
                            Properties ftpLogin = new Properties();
                            ftpLogin.load(new FileReader(ftpLoginFile));
                            URLConnection conn = new URL("ftp://ftp.shwam3.altervista.org/parking/data.json").openConnection();
                            conn.setRequestProperty("Authorization", "Basic " + DatatypeConverter.printBase64Binary((ftpLogin.getProperty("Username", "") + ":" + ftpLogin.getProperty("Password", "")).getBytes()));

                            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream())))
                            {
                                bw.write(obj.toString());

                                if (SystemTray.isSupported())
                                    icon.setToolTip("Norfolk Parking FTP thing\nLast update: " + sdf.format(new Date()));
                                System.out.println("Last update: " + sdf.format(new Date()));
                            }
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();

                            if (SystemTray.isSupported())
                                icon.setToolTip("Norfolk Parking FTP thing\nLast update: Failed\n" + String.valueOf(ex));
                            System.out.println("Last update: Failed\n" + String.valueOf(ex));
                        }
                    }
                }
            }, 10, 120000);
        }
    }
}