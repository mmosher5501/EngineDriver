package jmri.managers.configurexml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jdom.Element;
import jmri.managers.InternalSensorManager;

/**
 * Provides load and store functionality for
 * configuring InternalSensorManagers.
 * <P>
 * Uses the store method from the abstract base class, but
 * provides a load method here.
 *
 * @author Bob Jacobsen Copyright: Copyright (c) 2006
 * @version $Revision$
 */
public class InternalSensorManagerXml extends jmri.managers.configurexml.AbstractSensorManagerConfigXML {

    public InternalSensorManagerXml() {
        super();
    }

    public void setStoreElementClass(Element sensors) {
        sensors.setAttribute("class",this.getClass().getName());
    }

    public void load(Element element, Object o) {
        log.error("Invalid method called");
    }
    
    public Element store(Object o) {
        Element sensors = new Element("sensors");
        
        String defaultState;
        switch(InternalSensorManager.getDefaultStateForNewSensors()){
            case jmri.Sensor.ACTIVE : defaultState = "active"; break;
            case jmri.Sensor.INACTIVE : defaultState = "inactive"; break;
            case jmri.Sensor.INCONSISTENT : defaultState = "inconsistent"; break;
            default : defaultState = "unknown";
        }
        
        sensors.addContent(new Element("defaultInitialState").addContent(defaultState));
        
        return store(o, sensors);
    
    }

    public boolean load(Element sensors) throws jmri.configurexml.JmriConfigureXmlException {
        // load individual sensors
        if (sensors.getChild("defaultInitialState")!=null){
            int defaultState = jmri.Sensor.UNKNOWN;
            String state = sensors.getChild("defaultInitialState").getText();
            if(state.equals("active")){
                defaultState = jmri.Sensor.ACTIVE;
            } else if (state.equals("inactive")){
                defaultState = jmri.Sensor.INACTIVE;
            } else if (state.equals("inconsistent")){
                defaultState  = jmri.Sensor.INCONSISTENT;
            }
            InternalSensorManager.setDefaultStateForNewSensors(defaultState);
        }
        boolean load = loadSensors(sensors);
        
        return load;
    }

    static Logger log = LoggerFactory.getLogger(InternalSensorManagerXml.class.getName());
}
