// ShortAddrVariableValue.java

package jmri.jmrit.symbolicprog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;

import java.util.Vector;

/**
 * Representation of a short address (CV1).
 * <P>
 * This is a decimal value, extended to modify the other CVs when
 * written.  The CVs to be modified and there new values are
 * stored in two arrays for simplicity.
 * <P>
 * 
 * The NMRA has decided that writing CV1 causes other CVs to update
 * within the decoder (CV19 for consisting, CV29 for short/long
 * address). We want DP to overwrite those _after_ writing CV1,
 * so that the DP values are forced to be the correct ones.
 * 
 * @author	    Bob Jacobsen   Copyright (C) 2001, 2006, 2007
 * @version     $Revision$
 *
 */
public class ShortAddrVariableValue extends DecVariableValue {

    public ShortAddrVariableValue(String name, String comment, String cvName,
                                  boolean readOnly, boolean infoOnly, boolean writeOnly, boolean opsOnly,
                                  int cvNum, String mask,
                                  Vector<CvValue> v, JLabel status, String stdname) {
        // specify min, max value explicitly
        super(name, comment, cvName, readOnly, infoOnly, writeOnly, opsOnly, cvNum, mask, 1, 127, v, status, stdname);

        // add default overwrites as per NMRA spec
        firstFreeSpace = 0;
        setModifiedCV(19);         // consisting
        setModifiedCV(29);         // control bits
    }

    /**
     * Register a CV to be modified regardless of
     * current value
     */
    public void setModifiedCV(int cvNum) {
        if (firstFreeSpace>=maxCVs) {
            log.error("too many CVs registered for changes!");
            return;
        }
        cvNumbers[firstFreeSpace] = cvNum;
        newValues[firstFreeSpace] = -10;
        firstFreeSpace++;
    }

    /**
     * Change CV values due to change in short address
     */
    private void updateCvForAddrChange() {
        for (int i=0; i<firstFreeSpace; i++) {
            CvValue cv = _cvVector.elementAt(cvNumbers[i]);
            if (cv == null) continue;  // if CV not present this decoder...
            if (cvNumbers[i]!=cv.number())
                log.error("CV numbers don't match: "
                          +cvNumbers[i]+" "+cv.number());
            cv.setToWrite(true);
            cv.setState(EDITED);
            if(log.isDebugEnabled()) log.debug("Mark to write " +cv.number());
        }
    }

    int firstFreeSpace = 0;
    static final int maxCVs = 20;
    int[] cvNumbers = new int[maxCVs];
    int[] newValues = new int[maxCVs];

    public void writeChanges() {
        if (getReadOnly()) log.error("unexpected writeChanges operation when readOnly is set");
        setBusy(true);  // will be reset when value changes
        // mark other CVs as possibly needing write
        updateCvForAddrChange();
        // and change the value of this one
        _cvVector.elementAt(getCvNum()).write(_status);
    }

    public void writeAll() {
        if (getReadOnly()) log.error("unexpected writeAll operation when readOnly is set");
        setBusy(true);  // will be reset when value changes
        // mark other CVs as possibly needing write
        updateCvForAddrChange();
        // and change the value of this one
        _cvVector.elementAt(getCvNum()).write(_status);
    }

    // clean up connections when done
    public void dispose() {
        super.dispose();
    }

    // initialize logging
    static Logger log = LoggerFactory.getLogger(ShortAddrVariableValue.class.getName());

}