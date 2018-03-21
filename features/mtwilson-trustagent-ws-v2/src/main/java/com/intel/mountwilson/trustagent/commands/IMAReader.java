package com.intel.mountwilson.trustagent.commands;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import com.intel.mountwilson.trustagent.imameasurement.*;

/**
*
* @author dav10re
*/


/*This class read IMA measurements and create a IMAMeasurements object
 ready for the TpmQuoteResponse class
*/

public class IMAReader {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IMAReader.class);
	
	private String ima_file_path = "/var/log/trustagent/ima.txt";
	
	public IMAMeasurements getIMAMeasurements(){
		
		IMAMeasurements imaMeasurements = new IMAMeasurements();
		
		try{
			
			String[] cmdArray = {"cp",
					"/sys/kernel/security/ima/ascii_runtime_measurements",
					ima_file_path};
	    	Runtime.getRuntime().exec(cmdArray);
			
			FileReader ima_file = new FileReader(ima_file_path);
			BufferedReader bufRead = new BufferedReader(ima_file);
			String line = null;
			
			List<FileMeasurementType> measurements_list = imaMeasurements.getImameasurements();
			
			while ( (line = bufRead.readLine()) != null)
			{    
			    if(line.indexOf("sha1:") == -1){

			    	bufRead.close();
			    	ima_file.close();
			    	throw new Exception();
			    	
			    }
				
				String[] token = line.split("sha1:");
			    
			    String[] subtoken = token[1].split(" ");
			    
			    String sha1_value = subtoken[0];
			    
			    String file_path = subtoken[1];
			    
			    FileMeasurementType measured_file = new FileMeasurementType();
			    measured_file.setPath(file_path);
			    measured_file.setValue(sha1_value);
			    
			    measurements_list.add(measured_file);
			    
			}
			
			bufRead.close();
	    	ima_file.close();
			
		}catch (FileNotFoundException e){
				
			log.warn("The file containing IMA measurements doesn't exist: {}", e.getMessage());
			return null;
		
		}catch (Exception e){
			
			log.warn("Don't be able to parse the IMA file {}", e.getMessage());
			return null;
		}
		
		imaMeasurements.setDigestAlg("sha1");
		return imaMeasurements;
		
	}
	
	
}
