package jmri.jmrit.logix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

import jmri.Block;
import jmri.InstanceManager;
import jmri.JmriException;
import jmri.NamedBean;
import jmri.SignalHead;
import jmri.SignalMast;

/**
 * A Portal is a boundary between two Blocks.
 * 
 * <P>
 * A Portal has Lists of the OPaths that connect through it.
 * The direction of trains passing through the portal is managed from the BlockOrders of the Warrant
 * the train is running under.  The Portal fires a PropertyChangeEvent that a PortIcon can listen for
 * to set direction arrows for a given route.
 * 
 * The Portal also supplies speed information from any signals set at its location that the Warrant 
 * passes on the Engineer.
 *
 * @author	Pete Cressman  Copyright (C) 2009
 */
public class Portal extends jmri.implementation.AbstractNamedBean {

    private ArrayList <OPath> _fromPaths = new ArrayList <OPath>();
    private OBlock      _fromBlock;
    private NamedBean   _fromSignal;          // may be either SignalHead or SignalMast
    private long        _fromSignalDelay;
    private ArrayList <OPath> _toPaths = new ArrayList <OPath>();
    private OBlock      _toBlock;
    private NamedBean   _toSignal;          // may be either SignalHead or SignalMast
    private long        _toSignalDelay;
    //private String      _portalName;
    private int 		_state = UNKNOWN;

    //public static final int UNKNOWN      = 0x01;
    //public static final int INCONSISTENT = 0x08;
    public static final int ENTER_TO_BLOCK = 0x02;
    public static final int ENTER_FROM_BLOCK = 0x04;
    
    public Portal(String sName, String uName) {
        super(sName.toUpperCase(), uName);
    }
    
    public Portal(OBlock fromBlock, String portalName, OBlock toBlock) {
    	super(portalName, portalName);
        _fromBlock = fromBlock;
 //       _portalName = portalName;
        _toBlock = toBlock;
        if (_fromBlock!=null) _fromBlock.addPortal(this);
        if (_toBlock!=null) _toBlock.addPortal(this);
        //if (log.isDebugEnabled()) log.debug("Ctor: name= "+_portalName+", fromBlock= "+
        //           getFromBlockName()+", toBlock= "+getToBlockName()); 
    }

    /**
    * Determine which list the Path belongs to and add it to the list
    * @return false if Path does not have a matching block for this Portal
    */
    public boolean addPath(OPath path) {
        Block block = path.getBlock();
        if (block==null) {
            log.error("Path \""+path.getName()+"\" has no block.");
            return false;
        }
        if (!this.equals(path.getFromPortal()) &&
                !this.equals(path.getToPortal()) ){
            return false;
        }
        if (_fromBlock != null && _fromBlock.equals(block)) {
            return addPath(_fromPaths, path);
        } else if (_toBlock != null && _toBlock.equals(block)) {
            return addPath(_toPaths, path);
        }
        // portal is incomplete or path block not in this portal
        // to do!!! fix this so it may return false.  Need true for bogus load
        return true;
    }

    /**
    *  Utility for both path lists
    */
    private boolean addPath(List <OPath> list, OPath path) {
        if (!list.contains(path))  {
            String pName =path.getName();
            for (int i=0; i<list.size(); i++) {
                if (pName.equals(list.get(i).getName())) { log.error("Path \""+path.getName()+
                    "\" is duplicate name for another path in Portal \""+getUserName()+"\".");
                    return false; 
                }
            }
            list.add(path);
        }
        return true;
    }

    public void removePath(OPath path) {
        Block block = path.getBlock();
        if (block==null) {
            log.error("Path \""+path.getName()+"\" has no block.");
            return;
        }
        //if (log.isDebugEnabled()) log.debug("removePath: "+toString());
        if (!this.equals(path.getFromPortal()) &&
                !this.equals(path.getToPortal()) ){
            return;
        }
        if (_fromBlock != null && _fromBlock.equals(block)) {
            _fromPaths.remove(path);
        } else if (_toBlock != null && _toBlock.equals(block)) {
            _toPaths.remove(path);
        }
    }

    /**
    * Check for duplicate name in either block
    * @return return error message, return null if name change is OK 
    */
    public String setName(String name) {
        if (name == null || name.length()==0) { return null; }
        if (getUserName().equals(name)) { return null; }

        String msg = checkName(name, _fromBlock);
        if (msg==null) {
            msg = checkName(name, _toBlock);
        }
        if (msg==null) {
        	setUserName(name);
        } else {
            msg = Bundle.getMessage("DuplicatePortalName", msg, name); 
        }
        return msg;
    }
    private String checkName(String name, OBlock block) {
        List<Portal> list = block.getPortals();
        for (int i=0; i<list.size(); i++) {
            if (name.equals(list.get(i).getName())) {
                return list.get(i).getName(); 
            }
        }
        return null;
    }

    public String getName() { return getUserName(); }

    /**
    * Set block name. Verify that all toPaths are contained in the block.
    * @return false if paths are not in the block
    */
    public boolean setToBlock(OBlock block, boolean changePaths) {
        if ((block!=null && block.equals(_toBlock)) || (block==null && _toBlock==null)) {
            return true;
        }
        if (changePaths) {
            //Switch paths to new block.  User will need to verify connections
            for (int i=0; i<_toPaths.size(); i++) {
                    _toPaths.get(i).setBlock(block);
            }
        } else if (!verify(_toPaths, block)) {
            return false;
        }
        if (log.isDebugEnabled()) log.debug("setToBlock: oldBlock= \""+getToBlockName()
                  +"\" newBlock \""+(block!=null ? block.getDisplayName() : null)+"\".");
        if (_toBlock!=null) { _toBlock.removePortal(this); }
        _toBlock = block;
        if (_toBlock!=null) { _toBlock.addPortal(this); }
        return true;
    }
    public OBlock getToBlock() { return _toBlock;  }
    public String getToBlockName() { return (_toBlock!=null ? _toBlock.getDisplayName() : null); }
    public List <OPath> getToPaths() { return _toPaths; }

    /**
    * Set block name. Verify that all fromPaths are contained in the block.
    * @return false if paths are not in the block
    */
    public boolean setFromBlock(OBlock block, boolean changePaths) {
        if ((block!=null && block.equals(_fromBlock)) || (block==null && _fromBlock==null)) {
            return true;
        }
        if (changePaths) {
            //Switch paths to new block.  User will need to verify connections
            for (int i=0; i<_fromPaths.size(); i++) {
                    _fromPaths.get(i).setBlock(block);
            }
        } else if (!verify(_fromPaths, block)) {
            return false;
        }
        if (log.isDebugEnabled()) log.debug("setFromBlock: oldBlock= \""+getFromBlockName()+
                  "\" newBlock \""+(block!=null ? block.getDisplayName() : null)+"\".");
        if (_fromBlock!=null) { _fromBlock.removePortal(this); }
        _fromBlock = block;
        if (_fromBlock!=null) { _fromBlock.addPortal(this); }
        return true;
    }
    public OBlock getFromBlock() { return _fromBlock;  }
    public String getFromBlockName() { return (_fromBlock!=null ? _fromBlock.getDisplayName() : null);  }
    public List <OPath> getFromPaths() { return _fromPaths;  }

    public boolean setProtectSignal(NamedBean signal, long time, OBlock protectedBlock) {
        if (protectedBlock==null) return false;
        if (_fromBlock.equals(protectedBlock)) {
            _toSignal = signal;
            _toSignalDelay = time;
            return true;
            //log.debug("setSignal: _toSignal= \""+name+", protectedBlock= "+protectedBlock);
        }
        if (_toBlock.equals(protectedBlock)) {
            _fromSignal = signal;
            _fromSignalDelay = time;
            return true;
            //log.debug("setSignal: _fromSignal= \""+name+", protectedBlock= "+protectedBlock);
        }
        return false;
    }

    public NamedBean getFromSignal() {
        return _fromSignal;
    }
    public String getFromSignalName() {
        return (_fromSignal!=null ? _fromSignal.getDisplayName() : null);
    }
    public long getFromSignalDelay() {
        return _fromSignalDelay;
    }
    public NamedBean getToSignal() {
        return _toSignal;
    }
    public String getToSignalName() {
        return (_toSignal!=null ? _toSignal.getDisplayName() : null); 
    }
    public long getToSignalDelay() {
        return _toSignalDelay;
    }
    public void deleteSignal(NamedBean signal) {
        if (signal.equals(_toSignal)) {
            _toSignal = null;
        } else if (signal.equals(_fromSignal)) {
            _fromSignal = null;
        }
    }

    static public NamedBean getSignal(String name) {
        NamedBean signal = InstanceManager.signalMastManagerInstance().getSignalMast(name);
        if (signal==null) {
            signal = InstanceManager.signalHeadManagerInstance().getSignalHead(name);
        }
        return signal;
    }

    /**
    * Get the paths to the portal within the connected Block
    * i.e. the paths in this (the param) block through the Portal
    * @param block 
    * @return null if portal does not connect to block
    */
    public List <OPath> getPathsWithinBlock(OBlock block) { 
        if (block == null) { return null; }
        if (block.equals(_fromBlock)) {
            return _fromPaths;
        } else if (block.equals(_toBlock)) {
            return _toPaths;
        }
        return null; 
    }

    /**
    * Return the block on the other side of the portal
    * from this (the param) block
    */
    public OBlock getOpposingBlock(OBlock block) {
        if (block.equals(_fromBlock)) {
            return _toBlock;
        } else if (block.equals(_toBlock)) {
            return _fromBlock;
        }
        return null; 
    }
    
    /**
    * Get the paths from the portal in the next connected Block
    * i.e. paths in the block on the other side of the portal
    * from this (the param) block
    * @param block 
    * @return null if portal does not connect to block
    */
    public List <OPath> getPathsFromOpposingBlock(OBlock block) { 
        if (block.equals(_fromBlock)) {
            return _toPaths;
        } else if (block.equals(_toBlock)) {
            return _fromPaths;
        }
        return null; 
    }

    /**
     * Call is from BlockOrder when setting the path
     * @param block
     */
    protected void setEntryState(OBlock block) {
    	try {
        	if (block==null) {
        		_state = UNKNOWN;
        	}
        	else if (block.equals(_fromBlock)) {
        		setState(ENTER_FROM_BLOCK);
            } else if (block.equals(_toBlock)) {
            	setState(ENTER_TO_BLOCK);
            }    		
    	} catch (jmri.JmriException ex) {}
    }
    public void setState(int s) throws JmriException {
    	int old = _state;
    	_state = s;
    	firePropertyChange("Direction", old, _state);
    }    
    public int getState() {
    	return _state;
    }

    /**
    * @param block is the direction of entry
    * @return signal protecting block
    */
    public NamedBean getSignalProtectingBlock(OBlock block) {
        if (block.equals(_toBlock)) {
            return _fromSignal;
        } else if (block.equals(_fromBlock)) {
            return _toSignal;
        }
        return null;
    }

    /**
    * Check signals, if any, for speed into the block. The signal that protects the
    * "to" block is the signal facing the "from" Block, i.e. the "from" signal.
    * (and vice-versa) 
    * @param block is the direction of entry, "from" block
    * @return permissible speed, null if no signal
    */
    public String getPermissibleEntranceSpeed(OBlock block) {
        String speed = null;
    	String blockName = block.getDisplayName();
        if (block.equals(_toBlock)) {
            if (_fromSignal!=null) {
                if (_fromSignal instanceof SignalHead) {
                    speed = getPermissibleSignalEntranceSpeed((SignalHead)_fromSignal);
                } else {
                    speed = getPermissibleSignalEntranceSpeed((SignalMast)_fromSignal);
                }
            }
        } else if (block.equals(_fromBlock)) {
            if (_toSignal!=null) {
                if (_toSignal instanceof SignalHead) {
                    speed = getPermissibleSignalEntranceSpeed((SignalHead)_toSignal);
                } else {
                    speed = getPermissibleSignalEntranceSpeed((SignalMast)_toSignal);
                }
            }
        } else {
            	log.error("Block \""+blockName+"\" is not in Portal \""+getUserName()+"\".");
        }
        if (log.isDebugEnabled() && speed!=null) log.debug("Portal \""+getUserName()+"\"," +
        		" has ENTRANCE speed= "+speed+" into \""+blockName+"\" from signal."); 
        // no signals, proceed at recorded speed
        return speed;
    }

    public long getEntranceSpeedChangeWaitForBlock(OBlock block) {
        if (block.equals(_toBlock)) {
            if (_fromSignal!=null) {
                return _fromSignalDelay;
            }
        } else if (block.equals(_fromBlock)) {
            if (_toSignal!=null) {
                return _toSignalDelay;
            }
        }
        return 0;
    }
    /**
    * Check signals, if any, for speed out of the block. The signal that protects the
    * "to" block is the signal facing the "from" Block, i.e. the "from" signal.
    * (and vice-versa) 
    * @param block is the direction of entry, "from" block
    * @return permissible speed, null if no signal
    */
    public String getPermissibleExitSpeed(OBlock block) {
        String speed = null;
    	String blockName = block.getDisplayName();
        if (block.equals(_toBlock)) {
            if (_fromSignal!=null) {
                if (_fromSignal instanceof SignalHead) {
                    speed = getPermissibleSignalExitSpeed((SignalHead)_fromSignal);
                } else {
                    speed = getPermissibleSignalExitSpeed((SignalMast)_fromSignal);
                }
            }
        } else if (block.equals(_fromBlock)) {
            if (_toSignal!=null) {
                if (_toSignal instanceof SignalHead) {
                    speed = getPermissibleSignalExitSpeed((SignalHead)_toSignal);
                } else {
                    speed = getPermissibleSignalExitSpeed((SignalMast)_toSignal);
                }
            }
        } else {
            log.error("Block \""+blockName+"\" is not in Portal \""+getUserName()+"\".");
        }
        if (log.isDebugEnabled() && speed!=null) log.debug("Portal \""+getUserName()+"\"," +
        		" has EXIT speed= "+speed+" into \""+blockName+"\" from signal."); 
        // no signals, proceed at recorded speed
        return speed;
    }

    private String getPermissibleSignalEntranceSpeed(SignalHead signal) {
        int appearance = signal.getAppearance();
        String speed = Warrant.getSpeedMap().getAppearanceSpeed(signal.getAppearanceName(appearance));
        if (speed==null) {
            log.error("SignalHead \""+ signal.getDisplayName()+"\" has no speed specified for appearance \""+
                            signal.getAppearanceName(appearance)+"\"! - Restricting Movement!");
            speed = "Restricted";
        }
        if (log.isDebugEnabled()) log.debug(signal.getDisplayName()+" has speed notch= "+speed+" from appearance \""+
                                                signal.getAppearanceName(appearance)+"\""); 
        return speed;
    }

    private String getPermissibleSignalEntranceSpeed(SignalMast signal) {
        String aspect = signal.getAspect();
        String speed = Warrant.getSpeedMap().getAspectSpeed(aspect, signal.getSignalSystem());
        if (speed==null) {
            log.error("SignalMast \""+ signal.getDisplayName()+"\" has no speed specified for aspect \""+
                                                aspect+"\"! - Restricting Movement!");
            speed = "Restricted";
        }
        if (log.isDebugEnabled()) log.debug(signal.getDisplayName()+" has speed notch= "+speed+
        			" from aspect \""+aspect+"\"");
        return speed;
    }
    
    private String getPermissibleSignalExitSpeed(SignalHead signal) {
        int appearance = signal.getAppearance();
        String speed = Warrant.getSpeedMap().getAppearanceSpeed(signal.getAppearanceName(appearance));
        if (speed==null) {
            log.error("SignalHead \""+ signal.getDisplayName()+"\" has no (exit) speed specified for appearance \""+
                            signal.getAppearanceName(appearance)+"\"! - Restricting Movement!");
            speed = "Restricted";
        }
        if (log.isDebugEnabled()) log.debug(signal.getDisplayName()+" has exit speed notch= "+speed+
        		" from appearance \""+signal.getAppearanceName(appearance)+"\""); 
        return speed;
    }

    private String getPermissibleSignalExitSpeed(SignalMast signal) {
        String aspect = signal.getAspect();
        String speed = Warrant.getSpeedMap().getAspectExitSpeed(aspect, signal.getSignalSystem());
        if (speed==null) {
            log.error("SignalMast \""+ signal.getDisplayName()+"\" has no exit speed specified for aspect \""+
                                                aspect+"\"! - Restricting Movement!");
            speed = "Restricted";
        }
        if (log.isDebugEnabled()) log.debug(signal.getDisplayName()+" has exit speed notch= "+
        				speed+" from aspect \""+aspect+"\"");
        return speed;
    }
    
    private boolean verify(List <OPath> paths, OBlock block) {
        if (block==null) {
            if (paths.size()==0) {
                return true;
            } else {
                return false;
            }
        }
        String name = block.getSystemName();
        for (int i=0; i<paths.size(); i++) {
            String pathName = paths.get(i).getBlock().getSystemName();
            if (!pathName.equals(name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if path connects to Portal
     * @param path
     */
    public boolean isValidPath(OPath path) {
    	String name = path.getName();
    	for (int i=0; i<_toPaths.size(); i++) {
    		if (_toPaths.get(i).getName().equals(name)) {
    			return true;
    		}
    	}
    	for (int i=0; i<_fromPaths.size(); i++) {
    		if (_fromPaths.get(i).getName().equals(name)) {
    			return true;
    		}
    	}
    	return false;
    }
    /**
     * Check portal has both blocks
     */
    public boolean isValid() {
        return (_fromBlock!=null && _toBlock!=null);
    }

    public void dispose() {
        if (_fromBlock!=null) _fromBlock.removePortal(this);
        if (_toBlock!=null) _toBlock.removePortal(this);
    }

    public String getDescription() {
        return Bundle.getMessage("PortalDescription",
        		getUserName(), getFromBlockName(), getToBlockName());
    }
    
    public String toString() {
        return ("Portal \""+getUserName()+"\" from block \""+getFromBlockName()+"\" to block \""+getToBlockName()+"\""); 
    }
    
    static Logger log = LoggerFactory.getLogger(Portal.class.getName());
}