package com.intel.mountwilson.trustagent.commands;

import com.intel.dcsg.cpg.xml.JAXB;
import com.intel.mountwilson.common.ErrorCode;
import com.intel.mountwilson.common.ICommand;
import com.intel.mountwilson.common.TAException;
import com.intel.mountwilson.trustagent.data.TADataContext;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.io.IOUtils;
import com.intel.mountwilson.trustagent.imameasurement.IMAMeasurements;

/**
 *
 * @author dav10re
 */
public class RetrieveImaMeasurement implements ICommand {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RetrieveImaMeasurement.class);
    private TADataContext context;

    public RetrieveImaMeasurement(TADataContext context) {
        this.context = context;
    }

    /**
     * Retrieves the measurement log from the TA node.
     * 
     * @throws TAException 
     */
    @Override
    public void execute() throws TAException {
    	
    	IMAReader imaReader = new IMAReader();
    	
    	
    	try {

        	IMAMeasurements imaMeasurements = imaReader.getIMAMeasurements();
        	if (imaMeasurements == null){
        		log.warn("There are problems to get IMA measurements");
				throw new IOException("There are problems getting IMA measurements");
        	}
    		
    		InputStream schemaStream = Crea_xml.class.getResourceAsStream("/xsd/IMA_Schema.xsd");
			if (schemaStream == null) {
				log.warn("Schema not found");
				throw new IOException("Schema not found");
			}
            
            JAXBContext jaxbContext = JAXBContext.newInstance(IMAMeasurements.class);
            
            SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
            Schema schema = sf.newSchema(new StreamSource(schemaStream));
            
     
            Marshaller m = jaxbContext.createMarshaller();
            m.setSchema(schema);
            StringWriter sw = new StringWriter();
            m.marshal(imaMeasurements, sw);
            log.debug("Marshalled IMAMeasurement: {}", sw.toString());
            context.setImaMeasurement(sw.toString());
       
        } catch (IOException e) {
            log.warn("IOException, invalid measurement.xml: {}", e.getMessage());
            throw new TAException(ErrorCode.BAD_REQUEST, "Invalid ima.txt file. Cannot unmarshal/marshal object using jaxb.");
        } catch (Exception e) {
            log.warn("Exception, invalid measurement.xml: {}", e.getMessage());
            throw new TAException(ErrorCode.BAD_REQUEST, "Invalid ima.txt file. Cannot unmarshal/marshal object using jaxb.");
        }
    }
}
