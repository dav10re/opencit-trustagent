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
	
	private String ima_file_path = "/sys/kernel/security/ima/ascii_runtime_measurements";
	
	public IMAMeasurements getIMAMeasurements(){
		
		IMAMeasurements imaMeasurements = new IMAMeasurements();
		
		try{
			
			/*String[] cmdArray = {"cp",
					"/sys/kernel/security/ima/ascii_runtime_measurements",
					ima_file_path};
	    	Runtime.getRuntime().exec(cmdArray);
			*/
             
			FileReader ima_file = new FileReader(ima_file_path);
			BufferedReader bufRead = new BufferedReader(ima_file);
			String line = null;
			
			List<FileMeasurementType> measurements_list = imaMeasurements.getImameasurements();
			
			while ( (line = bufRead.readLine()) != null)
			{    
                String[] token = line.split("\\s+");
                
                //Get the template-hash value because this is used for the PCR 10 extension process
                //String sha1_value = token[1]; //only if you want template-hash
                
                String[] subtoken = token[3].split("sha1:"); //get filedata-hash
                
                String sha1_value = subtoken[1];
                
                //This is necessary because some hash values are equal to 0..0 and in the extension process
                //they are set to 1..1 (see file measure.c in ltp-ima-standalone-v2.tar on IMA website
                if (sha1_value.equals("0000000000000000000000000000000000000000"))
                    sha1_value = "ffffffffffffffffffffffffffffffffffffffff";

			    
			    String file_path = token[4];
			    
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
