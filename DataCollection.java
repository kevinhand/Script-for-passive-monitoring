import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileOutputStream;

/**
*
* @author Hande
*/


public class DataCollection {

	private static boolean IsSendToServer = true;
	private static HashMap<String, String> ouiDB = new HashMap<String, String>();
	private static Socket socket = null;
	private static String macPi = "";
	private static ProbeRequest lastProbe = null ;
	private static String path = "/home/honghande/Research/scriptForMonitor/";
//	private static String path = "/root/";

	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("sudo java DataCollection IP_address Port wlanX");
		} else {
//			macPi = getEtherMac();
			ouiDB = ReadOuiFromFile(path+"smart_device.txt");
			System.out.println(ouiDB.size());
			runTcpDumpClient(args[0], Integer.parseInt(args[1]), args[2]);
		}
	}

	public static void runTcpDumpClient(String host, int port, String wlanInterface) {
		try {
			String Cmd = "/usr/sbin/tcpdump -tttt -i " + wlanInterface + " -e -s 256 \" type mgt subtype probe-req || type data subtype null\"";
			ProcessBuilder mProcessBuilder = new ProcessBuilder("/bin/bash", "-c", Cmd);
			
			Process mprocess = mProcessBuilder.start();
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(mprocess.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(mprocess.getErrorStream()));
			
			File FileWriter = new File(path + "Apdata.txt");
			FileWriter.setReadable(true);
			FileWriter.setWritable(true);
			FileOutputStream fos = new FileOutputStream(FileWriter, true);
		
			while(true){
				try{	
				
					socket = new Socket(host, port);
					OutputStream os = socket.getOutputStream();
					OutputStreamWriter osw = new OutputStreamWriter(os);
					BufferedWriter bw = new BufferedWriter(osw);				
		
					String strLine;
					while ((strLine = stdInput.readLine()) != null) {
						if (strLine.contains("SA:") && strLine.contains("dB ")) {
							System.out.println(strLine);
							// Print the content on the console
							String[] eProbeRequestString = strLine.split(" ");
							String msourceMAC = null;
							int mRssi = 0;
							Calendar cal = Calendar.getInstance();
							String[] edate = eProbeRequestString[0].split("-");
							String eTime = eProbeRequestString[1].substring(0, 8);
							String[] eTimeCut = eTime.split(":");
							cal.clear();
							cal.set(Integer.parseInt(edate[0]), Integer.parseInt(edate[1]) - 1, Integer.parseInt(edate[2]),
									Integer.parseInt(eTimeCut[0]), Integer.parseInt(eTimeCut[1]),
									Integer.parseInt(eTimeCut[2]));
		
							for (int i = 0; i < eProbeRequestString.length; i++) {
								if (eProbeRequestString[i].contains("SA:"))
									msourceMAC = eProbeRequestString[i].substring(3);
								if (eProbeRequestString[i].contains("dB"))
									mRssi = -1 * Integer.parseInt(eProbeRequestString[i].replaceAll("[\\D]", ""));
							}
							String prefix = msourceMAC.substring(0, 8);
							lastProbe = new ProbeRequest(cal, msourceMAC, mRssi);
							String info ="";
							if (ouiDB.containsKey(prefix.toUpperCase()) ) {
								info= "1 "+ CalendarToString(lastProbe.getTime()) + " "
										+ lastProbe.getSourceMAC() + " " + lastProbe.getAveragerssiWithoutOutliar()+" "
										+ ouiDB.get(prefix.toUpperCase())
										+"\n";						
							}else{
                        		info = "1 "+ CalendarToString(lastProbe.getTime()) + " "
				                         + lastProbe.getSourceMAC() + " " + lastProbe.getAveragerssiWithoutOutliar()+" "
				                         + "unknown"
				                         +"\n";
							}
							fos.write((info).getBytes());
							if (IsSendToServer) {
								bw.write(info);
								bw.flush();
							}
						}
					}
		
					while ((strLine = stdError.readLine()) != null) {
						System.out.println(strLine);
					}
				}catch(IOException e){				
					Thread.sleep(3000);		     
				}
			
			}//while end 
		} catch (Exception exception) {
			exception.printStackTrace();
		} finally {
			// Closing the socket
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	public static String getEtherMac() {
		String command = "/sbin/ifconfig";

		String sOsName = System.getProperty("os.name");
		if (sOsName.startsWith("Windows")) {
			command = "ipconfig";
		} else {

			if ((sOsName.startsWith("Linux")) || (sOsName.startsWith("Mac")) || (sOsName.startsWith("HP-UX"))) {
				command = "/sbin/ifconfig";
			} else {
//				System.out.println("The current operating system '" + sOsName + "' is not supported.");
			}
		}

		Pattern p = Pattern.compile("([a-fA-F0-9]{1,2}(-|:)){5}[a-fA-F0-9]{1,2}");
		String mac = null;
		try {
			Process pa = Runtime.getRuntime().exec(command);
			pa.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(pa.getInputStream()));

			String line;
			Matcher m;
			while ((line = reader.readLine()) != null) {

				m = p.matcher(line);

				if (!m.find())
					continue;
				line = m.group();
				break;

			}
			mac = line;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mac;
	}

	public static String CalendarToString(Calendar c) {
		return c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH)+1) + "-" + c.get(Calendar.DAY_OF_MONTH) + " "
				+ ((c.get(Calendar.HOUR_OF_DAY) < 10) ? "0" + c.get(Calendar.HOUR_OF_DAY) : c.get(Calendar.HOUR_OF_DAY))
				+ ":" + ((c.get(Calendar.MINUTE) < 10) ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE)) + ":"
				+ ((c.get(Calendar.SECOND) < 10) ? "0" + c.get(Calendar.SECOND) : c.get(Calendar.SECOND));
	}

	public static HashMap<String, String> ReadOuiFromFile(String filename) {
		// Open the file
		HashMap<String, String> ouiDB = new HashMap<String, String>();
		FileInputStream fstream;
		ouiDB = new HashMap<String, String>();
		try {
			fstream = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			String strLine;

			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// System.out.println(strLine);
				String[] tokens = strLine.split(" ");
//				System.out.println(tokens[0]+" "+tokens[1]);
				ouiDB.put(tokens[0], tokens[1]);
			}

			// System.out.println(ouiDB.size());

			// Close the input stream
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ouiDB;
		// System.out.println(signalDB.toString());
	}

	public static class ProbeRequest {
	    private  Calendar time;
	    private String sourceMAC;
	    private ArrayList<Integer> rssiList =null;
	    
	    public ProbeRequest(Calendar s, String sourceMAC, int rssi)
	    {
	        this.setTime(s);
	        this.sourceMAC = sourceMAC;
	        rssiList = new ArrayList<Integer>();
	        rssiList.add(rssi);   	
	    }
	    
	    public String getSourceMAC() {return this.sourceMAC;}
	    public ArrayList<Integer> getRssi() {return this.rssiList;}
	    
	    
	    public int getAveragerssi(){
	    	if(rssiList.size()>0){
	    		int sum =0;
	    		for(int i=0;i<rssiList.size();i++){
	    			sum += rssiList.get(i);
	    		}
	    		return sum/rssiList.size();
	    	}
	    	else
	    		return 0;   	
	    }
	    
	    public int getAveragerssiWithoutOutliar(){
	    	int avg = getAveragerssi();
	    	double dev = getDeviationrssi();
	    	if(rssiList.size()>0){
	    		int sum =0,cc=0;
	    		for(int i=0;i<rssiList.size();i++){
	    			if(Math.abs(rssiList.get(i)-avg)<=dev){
	    				sum += rssiList.get(i);
	    				cc++;
	    			}
	    				
	    		}
	    		return sum/cc;
	    	}
	    	else
	    		return 0;   	
	    }
	    	    
	    public double getDeviationrssi(){
	    	if(rssiList.size()>0){
	    		int avg = getAveragerssi();
	    		
	    		double var =0;
	    		for(int i=0;i<rssiList.size();i++){
	    			var +=(rssiList.get(i)-avg)*(rssiList.get(i)-avg);
	    		}
	    		return Math.sqrt((var/rssiList.size()));   		
	    	}
	    	else
	    		return 0;   	
	    }
	    
	    public double getDeviationrssiWithoutOutliar(){
	    	if(rssiList.size()>0){	    
	    		int avg = getAveragerssiWithoutOutliar();
	    		double dev = getDeviationrssi();
	    		double var =0;
	    		int cc=0;
	    		for(int i=0;i<rssiList.size();i++){
	    			if(Math.abs(rssiList.get(i)-avg)<=dev){
	    				var +=(rssiList.get(i)-avg)*(rssiList.get(i)-avg);
	    				cc++;
	    			}
	    		}
	    		return Math.sqrt((var/cc));   		
	    	}
	    	else
	    		return 0;   	
	    }
	    
	    public boolean IsInSameBurst(ProbeRequest probe)
	    {        
	        if (probe == null)
	            return false;
	        else if (probe == this)
	            return true;
	        
	        if ( probe.IsInSameStaying(this,2)&&
	        		probe.getSourceMAC().equals(this.sourceMAC))
	        {

	        	return true;
	        }
	           
	        else
	            return false;
	    }
	   
	    public boolean IsInSameStaying(ProbeRequest probe, int second)
	    {        
	        if (probe == null)
	            return false;
	        else if (probe == this)
	            return true;
	        
	        int diff = Math.abs(GetTimediffInSecond(probe));
	        if (diff>=0 && diff <second)
	            return true;
	        else
	            return false;
	    }
	    
	    
	    public int  GetTimediffInSecond(ProbeRequest probe)
	    {        
	       if (probe == this)
	            return 0;
	        return (int) ((probe.getTime().getTimeInMillis()-this.getTime().getTimeInMillis())/1000);
	  
	    }
	    
	    
	    public void PrintProbeRequest(){
	    	System.out.println(this.getTime().get(Calendar.YEAR)+"-"
	    						+this.getTime().get(Calendar.MONTH)+"-"
	    						+this.getTime().get(Calendar.DAY_OF_MONTH)+" "
	    						+this.getTime().get(Calendar.HOUR_OF_DAY)+":"
	    						+this.getTime().get(Calendar.MINUTE)+":"
	    						+this.getTime().get(Calendar.SECOND)+" "
	    						+(this.getTime().getTimeInMillis()-this.getTime().getTimeInMillis())+" "
	    						+ this.getRssi()+" "+this.sourceMAC);
	    }

		public Calendar getTime() {
			return time;
		}

		public void setTime(Calendar time) {
			this.time = time;
		}
	}
	
	
}

